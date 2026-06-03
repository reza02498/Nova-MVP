package com.nova.assistant.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class CommandParser @Inject constructor() {

    fun parse(input: String): Command {
        val text = normalize(input.trim())
        if (text.isEmpty()) return Command.Unknown

        // Simple keyword-based matching (much more reliable than complex regex)
        return parsePersian(text)
    }

    private fun normalize(input: String): String {
        return input
            .replace("ي", "ی").replace("ك", "ک").replace("ة", "ه")
            .replace("ؤ", "و").replace("أ", "ا").replace("إ", "ا")
            .replace("٠", "0").replace("١", "1").replace("٢", "2")
            .replace("٣", "3").replace("٤", "4").replace("٥", "5")
            .replace("٦", "6").replace("٧", "7").replace("٨", "8").replace("٩", "9")
            .replace("۰", "0").replace("۱", "1").replace("۲", "2")
            .replace("۳", "3").replace("۴", "4").replace("۵", "5")
            .replace("۶", "6").replace("۷", "7").replace("۸", "8").replace("۹", "9")
            .replace(Regex("\\s+"), " ").trim()
    }

    private fun parsePersian(text: String): Command {
        // === ALARMS ===

        // Set alarm: "آلارم ۷ صبح" / "ساعت ۷ بیدارم کن" / "زنگ بزن ۷"
        if (anyWord(text, "آلارم", "زنگ", "بیدارم کن", "بیدار کن", "کوک کن")) {
            val time = extractTime(text)
            if (time != null) {
                val label = text.replace(Regex("آلارم|زنگ|بیدارم? کن|بذار|بزن|کوک کن|برای|ساعت|صبح|ظهر|عصر|شب|\\d.*"), "").trim()
                return Command.SetAlarm(time, label.ifEmpty { null })
            }
        }

        // Reminder: "یادآوری کن نان بخر فردا ساعت ۱۰" / "یادآوری نان بخر برای فردا ۱۰"
        if (text.contains("یادآوری") || text.contains("یاداوری") || text.contains("reminder")) {
            val time = extractTime(text)
            val date = extractRelativeDate(text)
            if (time != null) {
                val dt = LocalDateTime.of(date ?: LocalDate.now(), time)
                val task = text.replace(Regex("یادآوری|یاداوری|کن|برای|رو|ساعت|صبح|ظهر|عصر|شب|\\d.*|فردا|امروز|پس فردا"), "").trim()
                return Command.SetReminder(task.ifEmpty { "یادآوری" }, dt)
            }
        }

        // Show alarms: "آلارما رو نشون بده" / "چه آلارمایی داری"
        if (anyWord(text, "آلارما", "آلارم ها", "چه آلارم", "لیست آلارم") && anyWord(text, "نشون", "بده", "داری", "بگو", "چی", "لیست"))
            return Command.ListAlarms

        // Cancel all: "همه آلارما رو پاک کن"
        if (anyWord(text, "همه", "تموم", "کل") && anyWord(text, "آلارم", "زنگ") && anyWord(text, "پاک", "حذف", "کنسل", "لغو"))
            return Command.CancelAllAlarms

        // Cancel one: "آلارم ۲ رو حذف کن" / "آلارم شماره ۳ پاک"
        if (anyWord(text, "آلارم", "زنگ") && anyWord(text, "حذف", "پاک", "کنسل", "لغو", "قطع")) {
            val num = extractNumber(text)
            if (num != null) return Command.CancelAlarm(num.toLong())
        }

        // Snooze: "چرت ۱۰ دقیقه" / "اسنوز ۵"
        if (anyWord(text, "چرت", "اسنوز", "snooze")) {
            val mins = extractNumber(text) ?: 10
            return Command.Snooze(mins)
        }

        // === NOTIFICATIONS ===
        if (text.contains("پیام") && anyWord(text, "بخون", "نشون", "بده", "چک", "چی دارم", "چه پیام"))
            return Command.ReadNotifications

        if (text.contains("آخرین پیام") || text.contains("پیام آخر") || (text.contains("آخر") && text.contains("پیام")))
            return Command.ReadLastMessage

        // === READING CONTROLS ===
        if (text == "بس کن" || text == "قطع کن" || text == "خاموش کن" || text == "بسه" || text == "قطع" || text == "خاموش" || text == "ساکت" || text == "stop")
            return Command.StopReading
        if (anyWord(text, "تندتر", "سریعتر", "سرعت بیشتر") || text.contains("تندتر بخون"))
            return Command.ReadFaster
        if (anyWord(text, "یواشتر", "آرومتر", "آهسته تر", "کندتر") || text.contains("یواشتر بخون"))
            return Command.ReadSlower

        // === GENERAL ===
        if (text.contains("ساعت") && anyWord(text, "چنده", "چند", "چند است", "چنده الان", "الان"))
            return Command.GetTime

        if (anyWord(text, "امروز چندمه", "تاریخ امروز", "چه روزی", "چندمه", "چندم است") || (text.contains("امروز") && text.contains("چند")))
            return Command.GetDate

        if (text.contains("راهنما") || text == "راهنما" || text == "کمک" || text.contains("چه کارایی") || text.contains("چیکار") || text == "help" || text == "؟")
            return Command.Help

        if (text.contains("تنظیمات") || text.contains("setting"))
            return Command.OpenSettings

        if (anyWord(text, "پاک کن تاریخچه", "حذف تاریخچه", "حذف گفتگو", "پاک گفتگو", "clear history", "پاک کردن تاریخچه"))
            return Command.ClearHistory

        // === NUMERIC ALARM CANCEL (when user just says "حذف ۲") ===
        if (anyWord(text, "حذف", "پاک کن", "پاک", "کنسل") && extractNumber(text) != null)
            return Command.CancelAlarm(extractNumber(text)!!.toLong())

        return Command.Unknown
    }

    // ─── HELPERS ───

    private fun anyWord(text: String, vararg words: String): Boolean {
        return words.any { text.contains(it) }
    }

    private fun extractNumber(text: String): Int? {
        return Regex("(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractTime(text: String): LocalTime? {
        // Match Persian time patterns
        val match = Regex("(\\d{1,2})\\s*(?:[:.؛:]\\s*(\\d{1,2}))?\\s*(صبح|ظهر|عصر|بعدازظهر|شب)?").find(text)
            ?: return null

        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: 0
        val period = match.groupValues[3]

        val adjustedHour = when {
            period.contains("صبح") -> if (hour == 12) 0 else hour
            period.contains("ظهر") || period.contains("عصر") || period.contains("بعدازظهر") ->
                if (hour == 12) 12 else hour + 12
            period.contains("شب") -> if (hour == 12) 0 else hour + 12
            hour in 0..5 -> hour + 12  // 1-5 means PM in Persian context
            else -> hour.coerceIn(0, 23)
        }
        return LocalTime.of(adjustedHour, minute.coerceIn(0, 59))
    }

    private fun extractRelativeDate(text: String): LocalDate? {
        val now = LocalDate.now()
        return when {
            text.contains("امروز") -> now
            text.contains("فردا") -> now.plusDays(1)
            text.contains("پس فردا") -> now.plusDays(2)
            text.contains("دیروز") -> now.minusDays(1)
            text.contains("شنبه") -> nextDayOfWeek(now, DayOfWeek.SATURDAY)
            text.contains("یکشنبه") || text.contains("یک شنبه") -> nextDayOfWeek(now, DayOfWeek.SUNDAY)
            text.contains("دوشنبه") || text.contains("دو شنبه") -> nextDayOfWeek(now, DayOfWeek.MONDAY)
            text.contains("سه شنبه") -> nextDayOfWeek(now, DayOfWeek.TUESDAY)
            text.contains("چهارشنبه") || text.contains("چهار شنبه") -> nextDayOfWeek(now, DayOfWeek.WEDNESDAY)
            text.contains("پنجشنبه") || text.contains("پنج شنبه") -> nextDayOfWeek(now, DayOfWeek.THURSDAY)
            text.contains("جمعه") -> nextDayOfWeek(now, DayOfWeek.FRIDAY)
            else -> null
        }
    }

    private fun nextDayOfWeek(from: LocalDate, target: DayOfWeek): LocalDate {
        val daysUntil = (target.value - from.dayOfWeek.value + 7) % 7
        return if (daysUntil == 0) from.plusDays(7) else from.plusDays(daysUntil.toLong())
    }
}
