package com.nova.assistant.domain.nlu.entity

import com.nova.assistant.domain.nlu.tokenizer.Token
import com.nova.assistant.domain.nlu.tokenizer.TokenLexicon
import java.time.DayOfWeek
import java.time.LocalDate

object DateExtractor {

    fun extract(tokens: List<Token>): LocalDate? {
        val now = LocalDate.now()

        for (token in tokens) {
            val word = token.normalized
            when {
                word == "امروز" -> return now
                word == "فردا" -> return now.plusDays(1)
                word in listOf("پس فردا", "پسفردا") -> return now.plusDays(2)
                word == "دیروز" -> return now.minusDays(1)
                word == "شنبه" -> return nextDayOfWeek(now, DayOfWeek.SATURDAY)
                word in listOf("یکشنبه", "یک شنبه") -> return nextDayOfWeek(now, DayOfWeek.SUNDAY)
                word in listOf("دوشنبه", "دو شنبه") -> return nextDayOfWeek(now, DayOfWeek.MONDAY)
                word in listOf("سه شنبه", "سه\u200Cشنبه") -> return nextDayOfWeek(now, DayOfWeek.TUESDAY)
                word in listOf("چهارشنبه", "چهار شنبه", "چارشنبه") -> return nextDayOfWeek(now, DayOfWeek.WEDNESDAY)
                word in listOf("پنجشنبه", "پنج شنبه") -> return nextDayOfWeek(now, DayOfWeek.THURSDAY)
                word == "جمعه" -> return nextDayOfWeek(now, DayOfWeek.FRIDAY)
            }
        }
        return null
    }

    private fun nextDayOfWeek(from: LocalDate, target: DayOfWeek): LocalDate {
        val daysUntil = (target.value - from.dayOfWeek.value + 7) % 7
        return if (daysUntil == 0) from.plusDays(7) else from.plusDays(daysUntil.toLong())
    }
}
