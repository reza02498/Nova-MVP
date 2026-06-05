package com.nova.assistant.domain.nlu.entity

import com.nova.assistant.domain.nlu.tokenizer.Token
import com.nova.assistant.domain.nlu.tokenizer.TokenLexicon
import java.time.LocalTime

object TimeExtractor {

    /**
     * Extracts time from tokenized input.
     * Intent-independent — returns the time regardless of what the user's intent is.
     */
    fun extract(tokens: List<Token>): LocalTime? {
        // Strategy 1: Explicit TIME_VALUE token followed by period word
        for (i in tokens.indices) {
            val token = tokens[i]
            if (token.type.name == "TIME_VALUE" || token.normalized.toIntOrNull() in 0..24) {
                val hour = parseHour(token.normalized)
                if (hour == null) continue

                val minute = extractMinute(tokens, i)
                val period = findPeriod(tokens, i)

                val adjusted = adjustHour(hour, period)
                return LocalTime.of(adjusted, minute)
            }
        }

        // Strategy 2: Explicit HH:MM pattern anywhere in tokens
        for (token in tokens) {
            val match = Regex("(\\d{1,2})[:.؛:](\\d{2})").find(token.normalized)
            if (match != null) {
                val hour = match.groupValues[1].toIntOrNull() ?: continue
                val minute = match.groupValues[2].toIntOrNull() ?: 0
                return LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            }
        }

        return null
    }

    private fun parseHour(text: String): Int? {
        // Digit: "7", "07"
        text.toIntOrNull()?.let { return it.coerceIn(0, 24) }
        // Word: "هفت"
        TokenLexicon.WORD_NUMBERS[text]?.let { return it }
        return null
    }

    private fun extractMinute(tokens: List<Token>, hourIdx: Int): Int {
        // Check next token for minute pattern
        if (hourIdx + 1 < tokens.size) {
            val next = tokens[hourIdx + 1].normalized
            // "و نیم" pattern
            if (next == "و" && hourIdx + 2 < tokens.size && tokens[hourIdx + 2].normalized == "نیم") return 30
            // HH:MM format already parsed in strategy 2
        }
        return 0
    }

    private fun findPeriod(tokens: List<Token>, hourIdx: Int): String? {
        // Check tokens after hour for period word
        for (i in hourIdx + 1 until minOf(hourIdx + 3, tokens.size)) {
            if (tokens[i].normalized in TokenLexicon.TIME_PERIODS) return tokens[i].normalized
        }
        return null
    }

    private fun adjustHour(hour: Int, period: String?): Int = when {
        period == "صبح" -> if (hour == 12) 0 else hour
        period == "ظهر" || period == "عصر" || period == "بعدازظهر" || period == "بعد از ظهر" ->
            if (hour == 12) 12 else hour + 12
        period == "شب" -> if (hour == 12) 0 else hour + 12
        hour in 0..5 -> hour + 12   // Common Persian: bare numbers 1-5 are PM
        else -> hour.coerceIn(0, 23)
    }
}
