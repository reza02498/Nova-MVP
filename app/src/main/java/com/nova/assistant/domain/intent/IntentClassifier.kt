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

        return if (bestScore >= getThreshold(bestResult.intent)) bestResult
        else IntentResult.unknown()
    }

    private fun calculateScore(text: String, def: IntentDefinition): Float {
        // Check structural requirements first
        if (def.requiresNumber && !hasNumber(text)) return 0f
        if (def.requiresTime && !hasTime(text)) return 0f

        // Positive keywords (weight 1.0)
        var matched = 0f
        val matchedWords = mutableListOf<String>()
        for (kw in def.positiveKeywords) {
            if (text.contains(kw)) {
                matched += 1.0f
                matchedWords.add(kw)
            }
        }

        // Synonyms (weight 0.8)
        var synonymScore = 0f
        var totalSynonyms = 0
        for (group in def.synonyms) {
            totalSynonyms += group.variations.size
            for (v in group.variations) {
                if (text.contains(v)) {
                    synonymScore += 0.8f
                    matchedWords.add(v)
                }
            }
        }

        // Negative penalty
        var penalty = 0f
        for (nw in def.negativeKeywords) {
            if (text.contains(nw)) penalty += 1.5f
        }

        val maxScore = def.positiveKeywords.size.toFloat() + totalSynonyms.toFloat() * 0.8f
        if (maxScore == 0f) return 0f

        return ((matched + synonymScore - penalty) / maxScore).coerceIn(0f, 1f)
    }

    private fun collectKeywords(text: String, def: IntentDefinition): List<String> {
        val words = mutableListOf<String>()
        for (kw in def.positiveKeywords) if (text.contains(kw)) words.add(kw)
        for (g in def.synonyms) for (v in g.variations) if (text.contains(v)) words.add(v)
        return words
    }

    private fun getThreshold(intent: Intent): Float =
        IntentDefinitions.forIntent(intent).threshold

    private fun hasNumber(text: String): Boolean =
        Regex("\\d+").containsMatchIn(text) || hasWordNumber(text)

    private fun hasWordNumber(text: String): Boolean {
        val words = listOf("یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه", "ده",
            "اول", "دوم", "سوم", "چهارم", "پنجم", "نیم", "one", "two", "three", "four", "five")
        return words.any { text.contains(it) }
    }

    private fun hasTime(text: String): Boolean =
        Regex("\\d{1,2}\\s*(?:[:.؛:]\\s*\\d{1,2})?\\s*(صبح|ظهر|عصر|بعدازظهر|شب|بعد از ظهر|am|pm)?").containsMatchIn(text) ||
        text.contains("ساعت") || text.contains("نیم") || text.contains("ربع")
}
