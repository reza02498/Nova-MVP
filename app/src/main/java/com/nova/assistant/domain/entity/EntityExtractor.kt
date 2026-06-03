package com.nova.assistant.domain.entity

import com.nova.assistant.domain.context.ConversationContext
import com.nova.assistant.domain.intent.Intent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class EntityExtractor @Inject constructor() {

    fun extract(text: String, intent: Intent, context: ConversationContext? = null): ExtractedEntities {
        return when (intent) {
            // ── Alarms ──
            Intent.SET_ALARM -> ExtractedEntities(
                time = extractTime(text),
                taskContent = extractContent(text, "آلارم|زنگ|بیدار|کوک|بذار|بزن|ساعت|صبح|ظهر|عصر|شب")
            )
            Intent.SET_REMINDER -> ExtractedEntities(
                time = extractTime(text),
                date = extractRelativeDate(text),
                taskContent = extractContent(text, "یادآوری|یاداوری|یادم|reminder|کن|برای|رو|ساعت|فردا|امروز")
            )
            Intent.CANCEL_ALARM -> ExtractedEntities(number = extractNumber(text))
            Intent.SNOOZE -> ExtractedEntities(duration = extractNumber(text) ?: 10)
            Intent.LIST_ALARMS, Intent.CANCEL_ALL_ALARMS -> ExtractedEntities()

            // ── Timer ──
            Intent.SET_TIMER -> ExtractedEntities(duration = extractNumber(text) ?: 5)
            Intent.CANCEL_TIMER -> ExtractedEntities()

            // ── Notes ──
            Intent.CREATE_NOTE -> ExtractedEntities(
                taskContent = extractContent(text, "یادداشت|یاداشت|بنویس|ثبت|کن|جدید|بذار|بزن|save|write")
            )
            Intent.DELETE_NOTE -> ExtractedEntities(number = extractNumber(text))
            Intent.SEARCH_NOTES -> ExtractedEntities(
                taskContent = extractContent(text, "جستجو|بگرد|پیدا|سرچ|search|find|تو|یادداشت|یاداشت")
            )
            Intent.LIST_NOTES -> ExtractedEntities()

            // ── Device ──
            Intent.TOGGLE_WIFI -> ExtractedEntities(
                deviceTarget = DeviceTarget.WIFI,
                deviceAction = extractDeviceAction(text)
            )
            Intent.TOGGLE_BLUETOOTH -> ExtractedEntities(
                deviceTarget = DeviceTarget.BLUETOOTH,
                deviceAction = extractDeviceAction(text)
            )
            Intent.TOGGLE_FLASHLIGHT -> ExtractedEntities(
                deviceTarget = DeviceTarget.FLASHLIGHT,
                deviceAction = extractDeviceAction(text)
            )

            // ── Notifications ──
            Intent.READ_NOTIFICATIONS, Intent.READ_LAST_MESSAGE -> ExtractedEntities()

            Intent.UNKNOWN -> ExtractedEntities()
        }
    }

    // ═══════════════════════ EXTRACTION HELPERS ═══════════════════════

    fun extractTime(text: String): LocalTime? {
        val match = Regex("(\\d{1,2})\\s*(?:[:.؛:]\\s*(\\d{1,2}))?\\s*(صبح|ظهر|عصر|بعدازظهر|شب|بعد از ظهر)?")
            .find(text) ?: return null

        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: 0
        val period = match.groupValues[3]

        val adjustedHour = when {
            period.contains("صبح") -> if (hour == 12) 0 else hour
            period.contains("ظهر") || period.contains("عصر") ||
            period.contains("بعدازظهر") || period.contains("بعد از ظهر") ->
                if (hour == 12) 12 else hour + 12
            period.contains("شب") -> if (hour == 12) 0 else hour + 12
            hour in 0..5 -> hour + 12
            else -> hour.coerceIn(0, 23)
        }
        return LocalTime.of(adjustedHour, minute.coerceIn(0, 59))
    }

    fun extractNumber(text: String): Int? {
        val wordNums = mapOf(
            "یک" to 1, "دو" to 2, "سه" to 3, "چهار" to 4, "پنج" to 5,
            "شش" to 6, "هفت" to 7, "هشت" to 8, "نه" to 9, "ده" to 10,
            "اول" to 1, "دوم" to 2, "سوم" to 3, "چهارم" to 4, "پنجم" to 5,
            "نیم" to 30
        )
        for ((word, num) in wordNums) {
            if (text.contains(word)) return num
        }
        return Regex("(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun extractRelativeDate(text: String): LocalDate? {
        val now = LocalDate.now()
        return when {
            text.contains("امروز") -> now
            text.contains("فردا") -> now.plusDays(1)
            text.contains("پس فردا") || text.contains("پسفردا") -> now.plusDays(2)
            text.contains("دیروز") -> now.minusDays(1)
            text.contains("شنبه") -> nextDayOfWeek(now, DayOfWeek.SATURDAY)
            text.contains("یکشنبه") || text.contains("یک شنبه") -> nextDayOfWeek(now, DayOfWeek.SUNDAY)
            text.contains("دوشنبه") || text.contains("دو شنبه") -> nextDayOfWeek(now, DayOfWeek.MONDAY)
            text.contains("سه شنبه") || text.contains("سه‌شنبه") -> nextDayOfWeek(now, DayOfWeek.TUESDAY)
            text.contains("چهارشنبه") || text.contains("چهار شنبه") || text.contains("چارشنبه") ->
                nextDayOfWeek(now, DayOfWeek.WEDNESDAY)
            text.contains("پنجشنبه") || text.contains("پنج شنبه") -> nextDayOfWeek(now, DayOfWeek.THURSDAY)
            text.contains("جمعه") -> nextDayOfWeek(now, DayOfWeek.FRIDAY)
            else -> null
        }
    }

    private fun extractContent(text: String, removePattern: String): String {
        return text.replace(Regex(removePattern), "").replace(Regex("\\s+"), " ").trim()
    }

    private fun extractDeviceAction(text: String): DeviceAction {
        val onWords = listOf("روشن", "فعال", "وصل", "باز", "on", "enable", "روشن کن", "وصلش کن", "بزن")
        val offWords = listOf("خاموش", "قطع", "ببند", "off", "disable", "خاموش کن", "قطع کن", "ببندش", "بستن")

        if (onWords.any { text.contains(it) }) return DeviceAction.ON
        if (offWords.any { text.contains(it) }) return DeviceAction.OFF
        return DeviceAction.TOGGLE
    }

    private fun nextDayOfWeek(from: LocalDate, target: DayOfWeek): LocalDate {
        val daysUntil = (target.value - from.dayOfWeek.value + 7) % 7
        return if (daysUntil == 0) from.plusDays(7) else from.plusDays(daysUntil.toLong())
    }
}
