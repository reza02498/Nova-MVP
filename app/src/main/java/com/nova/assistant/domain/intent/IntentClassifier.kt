package com.nova.assistant.domain.intent

import javax.inject.Inject

/**
 * Rule-based intent classifier.
 *
 * Scoring algorithm (per IntentDefinition):
 *   matchedPositiveWeight  = count(matched positiveKeywords) × 1.0
 *   matchedSynonymWeight   = sum(matched synonym variations) × 0.8
 *   negativePenalty        = count(matched negativeKeywords) × 1.5
 *   maxPossibleScore       = positiveKeywords.size + totalSynonymsCount × 0.8
 *   rawScore               = (matchedPositive + matchedSynonym - negativePenalty) / maxPossible
 *
 *   if requiresNumber and no number in text → score = 0
 *   if requiresTime and no time in text → score = 0
 *
 *   confidence = rawScore.coerceIn(0f, 1f)
 */
class IntentClassifier @Inject constructor() {

    fun classify(normalizedText: String): IntentResult {
        if (normalizedText.isBlank()) return IntentResult.unknown()

        var bestResult = IntentResult.unknown()
        var bestScore = 0f

        for (def in IntentDefinitions.all) {
            val score = calculateScore(normalizedText, def)
            if (score > bestScore) {
                bestScore = score
                bestResult = IntentResult.of(def.intent, score, collectKeywords(normalizedText, def))
            }
        }

        android.util.Log.d("NovaClassifier", "Best: ${bestResult.intent} score=$bestScore threshold=${getThreshold(bestResult.intent)}")
        return if (bestScore >= getThreshold(bestResult.intent)) bestResult
        else IntentResult.unknown()
    }

    private fun calculateScore(text: String, def: IntentDefinition): Float {
        // Check structural requirements first
        if (def.requiresNumber && !hasNumber(text)) return 0f
        if (def.requiresTime && !hasTime(text)) return 0f

        // Positive keywords (weight 1.0) — word boundary matching
        var matched = 0f
        val matchedWords = mutableListOf<String>()
        for (kw in def.positiveKeywords) {
            if (matchesWord(text, kw)) {
                matched += 1.0f
                matchedWords.add(kw)
            }
        }

        // Synonyms (weight 0.8 per GROUP — word boundary matching)
        var synonymScore = 0f
        for (group in def.synonyms) {
            for (v in group.variations) {
                if (matchesWord(text, v)) {
                    synonymScore += 0.8f
                    matchedWords.add(v)
                    break
                }
            }
        }

        // Negative penalty — word boundary matching
        var penalty = 0f
        for (nw in def.negativeKeywords) {
            if (matchesWord(text, nw)) penalty += 1.5f
        }

        // Max score: positiveKeywords count + one 0.8 per synonym group
        val maxScore = def.positiveKeywords.size.toFloat() + def.synonyms.size.toFloat() * 0.8f
        if (maxScore == 0f) return 0f

        return ((matched + synonymScore - penalty) / maxScore).coerceIn(0f, 1f)
    }

    private fun collectKeywords(text: String, def: IntentDefinition): List<String> {
        val words = mutableListOf<String>()
        for (kw in def.positiveKeywords) if (matchesWord(text, kw)) words.add(kw)
        for (g in def.synonyms) for (v in g.variations) if (matchesWord(text, v)) words.add(v)
        return words
    }

    private fun getThreshold(intent: Intent): Float =
        IntentDefinitions.forIntent(intent).threshold

    private fun hasNumber(text: String): Boolean =
        Regex("\\d+").containsMatchIn(text) || hasWordNumber(text)

    private fun hasWordNumber(text: String): Boolean {
        val words = listOf("یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه", "ده",
            "اول", "دوم", "سوم", "چهارم", "پنجم", "نیم", "one", "two", "three", "four", "five")
        return words.any { matchesWord(text, it) }
    }

    private fun hasTime(text: String): Boolean =
        // Clock time: "۷ صبح", "07:00", "۷:۳۰"
        Regex("\\d{1,2}\\s*(?:[:.؛:]\\s*\\d{1,2})?\\s*(صبح|ظهر|عصر|بعدازظهر|شب|بعد از ظهر|am|pm)?").containsMatchIn(text) ||
        matchesWord(text, "ساعت") || matchesWord(text, "نیم") || matchesWord(text, "ربع") ||
        // Relative duration: "۲ دقیقه دیگه", "5 دقیقه", "۱۰ دقیقه دیگه"
        hasDuration(text)

    private fun hasDuration(text: String): Boolean =
        Regex("\\d+\\s*دقیقه|\\d+\\s*ساعت|\\d+\\s*ثانیه|نیم ساعت|ربع ساعت|\\d+\\s*دقه").containsMatchIn(text)

    /** Word boundary match — prevents substring false positives like "نت" inside "نتونستم" */
    private fun matchesWord(text: String, keyword: String): Boolean {
        val idx = text.indexOf(keyword)
        if (idx == -1) return false
        val before = if (idx == 0) ' ' else text[idx - 1]
        val after = if (idx + keyword.length >= text.length) ' ' else text[idx + keyword.length]
        return !before.isLetterOrDigit() && !after.isLetterOrDigit()
    }

    /** Check if any word in the list matches as a whole word */
    private fun containsAnyWord(text: String, words: List<String>): Boolean =
        words.any { matchesWord(text, it) }
}
