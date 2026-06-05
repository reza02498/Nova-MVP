package com.nova.assistant.domain.nlu.tokenizer

import javax.inject.Inject

/**
 * Converts a normalized Persian string into a list of typed Tokens.
 *
 * Strategy:
 *   1. Split text by whitespace into raw words
 *   2. Try compound-word matching first (e.g., "روشن کن" as one token)
 *   3. Fall back to single-word lexicon lookup
 *   4. Try number/time/date pattern matching for unknown words
 *   5. Any remaining → UNKNOWN
 */
class PersianTokenizer @Inject constructor(
    private val lexicon: TokenLexicon
) {

    fun tokenize(text: String): List<Token> {
        if (text.isBlank()) return emptyList()

        val rawWords = text.trim().split(Regex("\\s+"))
        val tokens = mutableListOf<Token>()
        var i = 0
        var pos = 0

        while (i < rawWords.size) {
            val word = rawWords[i]

            // Try 2-word compound first (e.g., "روشن کن", "وای فای")
            if (i + 1 < rawWords.size) {
                val compound = "$word ${rawWords[i + 1]}"
                val compoundType = lexicon.typeOf(compound)
                if (compoundType != TokenType.UNKNOWN) {
                    tokens.add(Token(compound, compound, compoundType, pos++, 1.0f))
                    i += 2
                    continue
                }
            }

            // Try single word
            val type = lexicon.typeOf(word)
            if (type != TokenType.UNKNOWN) {
                tokens.add(Token(word, word, type, pos++, 1.0f))
                i++
                continue
            }

            // Pattern matching for unknown words
            when {
                isTimePattern(word) -> {
                    tokens.add(Token(word, word, TokenType.TIME_VALUE, pos++, 0.9f))
                }
                isDatePattern(word) -> {
                    tokens.add(Token(word, word, TokenType.DATE_VALUE, pos++, 0.9f))
                }
                isDurationPattern(word) -> {
                    tokens.add(Token(word, word, TokenType.DURATION_VALUE, pos++, 0.9f))
                }
                isNumberPattern(word) -> {
                    tokens.add(Token(word, word, TokenType.NUMBER_VALUE, pos++, 0.9f))
                }
                // Negation prefix check: "نـ" + verb stem
                word.startsWith("ن") && word.length > 2 && lexicon.isKnown(word.substring(1)) -> {
                    tokens.add(Token(word, word, TokenType.NEGATION, pos++, 0.85f))
                }
                // Question word ending or starting
                word.endsWith("؟") || word == "؟" -> {
                    tokens.add(Token(word, word, TokenType.QUESTION, pos++, 1.0f))
                }
                else -> {
                    tokens.add(Token(word, word, TokenType.UNKNOWN, pos++, 0f))
                }
            }
            i++
        }

        return tokens
    }

    private fun isTimePattern(word: String): Boolean {
        // "۷", "07:00", "۷:۳۰", "7:30"
        if (Regex("^\\d{1,2}$").matches(word) && word.toIntOrNull() in 0..24) return true
        if (Regex("^\\d{1,2}[:.؛:]\\d{2}$").matches(word)) return true
        // Word numbers followed by time period: "هفت صبح" is 2 tokens, but "هفت" alone with context
        if (word in TokenLexicon.WORD_NUMBERS && word !in listOf("یک", "دو", "سه")) return true
        return false
    }

    private fun isDatePattern(word: String): Boolean =
        word in TokenLexicon.DATE_WORDS

    private fun isDurationPattern(word: String): Boolean {
        // "5", "۱۰" — potentially a duration if context supports it
        val num = word.toIntOrNull()
        if (num != null && num in 1..120) return true
        // "نیم", "ربع" — duration words
        if (word in listOf("نیم", "ربع", "یک ربع")) return true
        return false
    }

    private fun isNumberPattern(word: String): Boolean {
        if (Regex("^\\d+$").matches(word)) return true
        if (word in TokenLexicon.WORD_NUMBERS) return true
        return false
    }
}
