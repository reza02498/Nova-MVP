package com.nova.assistant.domain.nlu.entity

import com.nova.assistant.domain.nlu.tokenizer.Token
import com.nova.assistant.domain.nlu.tokenizer.TokenLexicon

object DurationExtractor {

    /**
     * Extracts duration in minutes from tokenized input.
     * Intent-independent — works for timers, alarms, reminders, etc.
     */
    fun extractMinutes(tokens: List<Token>): Int? {
        for (i in tokens.indices) {
            val token = tokens[i]

            // Pattern 1: NUMBER followed by unit word
            val num = parseNumber(token.normalized)
            if (num != null && i + 1 < tokens.size) {
                val next = tokens[i + 1].normalized
                val multiplier = TokenLexicon.DURATION_UNITS[next]
                if (multiplier != null) {
                    return num * (multiplier / 60) // convert seconds to minutes
                }
            }

            // Pattern 2: "نیم ساعت" → 30 min, "یک ساعت" → 60 min
            if (token.normalized == "نیم" && i + 1 < tokens.size && tokens[i + 1].normalized == "ساعت") return 30
            if (token.normalized == "ربع" && i + 1 < tokens.size && tokens[i + 1].normalized == "ساعت") return 15
            if (token.normalized == "یک" && i + 1 < tokens.size && tokens[i + 1].normalized == "ساعت") return 60

            // Pattern 3: Standalone with context — "دقیقه" after a word number
            if (token.normalized in TokenLexicon.DURATION_UNITS) {
                if (i > 0) {
                    val prev = tokens[i - 1].normalized
                    val prevNum = parseNumber(prev)
                    if (prevNum != null) {
                        val multiplier = TokenLexicon.DURATION_UNITS[token.normalized] ?: 60
                        return prevNum * (multiplier / 60)
                    }
                }
            }
        }
        return null
    }

    private fun parseNumber(text: String): Int? {
        text.toIntOrNull()?.let { return it }
        return TokenLexicon.WORD_NUMBERS[text]
    }
}
