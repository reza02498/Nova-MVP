package com.nova.assistant.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class CommandParser @Inject constructor() {

    fun parse(input: String): Command {
        val text = normalize(input)

        // Detect language: Persian or English
        val isPersian = text.any { it in '\u0600'..'\u06FF' || it in '\uFB50'..'\uFDFF' }

        return if (isPersian) parsePersian(text) else parseEnglish(text)
    }

    // ─── Normalization ───

    private fun normalize(input: String): String {
        return input.trim().lowercase()
            .replace("ي", "ی")     // Arabic Yeh → Persian Yeh
            .replace("ك", "ک")     // Arabic Kaf → Persian Kaf
            .replace("ة", "ه")     // Teh Marbuta → Heh
            .replace("ؤ", "و")     // Waw with Hamza
            .replace("أ", "ا")     // Alef with Hamza
            .replace("إ", "ا")
            .replace("٠", "0").replace("١", "1").replace("٢", "2")
            .replace("٣", "3").replace("٤", "4").replace("٥", "5")
            .replace("٦", "6").replace("٧", "7").replace("٨", "8").replace("٩", "9")
            .replace("۰", "0").replace("۱", "1").replace("۲", "2")
            .replace("۳", "3").replace("۴", "4").replace("۵", "5")
            .replace("۶", "6").replace("۷", "7").replace("۸", "8").replace("۹", "9")
            .replace(Regex("\\s+"), " ")
    }

    // ─── PERSIAN PARSING ───

    private fun parsePersian(text: String): Command {
        // Alarms first (most specific patterns)

        // "آلارم بذار برای ساعت ۷ صبح" / "ساعت ۷ صبح بیدارم کن" / "زنگ بزن ۷"
        val setAlarmPattern = Regex("(?:آلارم|زنگ).*?(?:ساعت\\s*)?(\\d{1,2})(?:\\s*[:.؛:]\\s*(\\d{1,2}))?\\s*(صبح|ظهر|عصر|شب)?.*?(?:بذار|بزن|تنظیم|کوک|بیدارم? ?کن)")
        val setAlarmMatch = setAlarmPattern.find(text)
        if (setAlarmMatch != null) {
            val hour = parsePersianHour(setAlarmMatch.groupValues[1].toIntOrNull() ?: 0,
                                         setAlarmMatch.groupValues[3])
            val minute = setAlarmMatch.groupValues[2].toIntOrNull() ?: 0
            val label = extractLabel(text, listOf("بذار", "بزن", "کن", "برای"))
            return Command.SetAlarm(LocalTime.of(hour, minute), label)
        }

        // "یادآوری کن نون بخرم رو فردا ساعت ۱۰"
        val reminderPattern = Regex("یادآوری\\s+(?:کن\\s+)?(.+?)\\s+(?:برای|رو|روی)\\s+(.+)|یادآوری\\s+(?:کن\\s+)?(.+?)\\s+(?:ساعت|برای)\\s+(.+)")
        val reminderMatch = reminderPattern.find(text)
        if (reminderMatch != null) {
            val task = reminderMatch.groupValues[1].ifEmpty { reminderMatch.groupValues[3] }.trim()
            val timeStr = reminderMatch.groupValues[2].ifEmpty { reminderMatch.groupValues[4] }.trim()
            val dateTime = parsePersianDateTime(timeStr)
            if (task.isNotEmpty() && dateTime != null) {
                return Command.SetReminder(task, dateTime)
            }
        }

        // "آلارما رو نشون بده" / "لیست آلارما"
        if (text.contains(Regex("آلارما.*نشون|لیست.*آلارم|آلارم.*لیست|آلارم.*چی دارم")))
            return Command.ListAlarms

        // "همه آلارما رو پاک کن"
        if (text.contains(Regex("همه.*آلارم.*پاک|آلارما.*حذف.*همه|کانسل.*همه")))
            return Command.CancelAllAlarms

        // "آلارم شماره ۳ رو کنسل کن" / "حذف آلارم ۳"
        val cancelPattern = Regex("(?:آلارم|زنگ)\\s*(?:شماره\\s*)?(\\d+)\\s*(?:رو|را)?\\s*(?:کنسل|حذف|پاک|لغو|قطع)")
        val cancelMatch = cancelPattern.find(text)
        if (cancelMatch != null) {
            return Command.CancelAlarm(cancelMatch.groupValues[1].toLongOrNull() ?: return Command.Unknown)
        }

        // "چرت ۱۰ دقیقه" / "اسنوز ۵"
        val snoozePattern = Regex("(?:چرت|اسنوز)\\s*(\\d+)|(\\d+)\\s*دقیقه\\s*(?:دیگه\\s*)?(?:چرت|اسنوز|بزن|زنگ)")
        val snoozeMatch = snoozePattern.find(text)
        if (snoozeMatch != null) {
            val mins = (snoozeMatch.groupValues[1].toIntOrNull() ?: snoozeMatch.groupValues[2].toIntOrNull()) ?: 10
            return Command.Snooze(mins)
        }

        // Notification Reading
        if (text.contains(Regex("پیام.*بخون|پیام.*چک|کی.*پیام.*داد|اعلان.*بخون|نوتیفیکیشن")))
            return Command.ReadNotifications

        if (text.contains(Regex("آخرین.*پیام|پیام.*آخر|آخرین.*اعلان")))
            return Command.ReadLastMessage

        // Reading Controls
        if (text.contains(Regex("^(بس|قطع|خاموش|ساکت)$|بس ?کن|قطع ?کن|خاموش ?کن|بس ?دیگه")))
            return Command.StopReading

        if (text.contains(Regex("تندتر|سریعتر|سرعت.*بیشتر|بسرعت|تند.*بخون")))
            return Command.ReadFaster

        if (text.contains(Regex("یواشتر|آرومتر|آهسته|کندتر|آهسته.*بخون")))
            return Command.ReadSlower

        // General
        if (text.contains(Regex("ساعت.*چند|الان.*ساعت|ساعت.*الان")))
            return Command.GetTime

        if (text.contains(Regex("امروز.*چند|تاریخ.*امروز|چندمه|چه روزی")))
            return Command.GetDate

        if (text.contains(Regex("راهنما|کمک|چیکار.*میتونی|چه.*کارا")))
            return Command.Help

        if (text.contains(Regex("تنظیمات|تنظیمات.*باز|برو.*تنظیمات")))
            return Command.OpenSettings

        if (text.contains(Regex("پاک.*تاریخچه|حذف.*گفتگو|تاریخچه.*پاک|گفتگو.*حذف")))
            return Command.ClearHistory

        return Command.Unknown
    }

    // ─── ENGLISH PARSING ───

    private fun parseEnglish(text: String): Command {
        // Alarms
        val setAlarmEn = Regex("(?:set\\s+(?:an?\\s+)?)?alarm\\s+(?:for\\s+)?(?:at\\s+)?(\\d{1,2})(?:[:.](\\d{2}))?\\s*(am|pm)?|wake\\s+me\\s+up\\s+at\\s+(\\d{1,2})(?:[:.](\\d{2}))?\\s*(am|pm)?")
        val alarmEnMatch = setAlarmEn.find(text)
        if (alarmEnMatch != null) {
            val isFirstPattern = alarmEnMatch.groupValues[1].isNotEmpty()
            val hour = if (isFirstPattern) {
                parseEnglishHour(alarmEnMatch.groupValues[1].toIntOrNull() ?: 0, alarmEnMatch.groupValues[3])
            } else {
                parseEnglishHour(alarmEnMatch.groupValues[4].toIntOrNull() ?: 0, alarmEnMatch.groupValues[6])
            }
            val minute = (alarmEnMatch.groupValues[2].toIntOrNull() ?: alarmEnMatch.groupValues[5].toIntOrNull()) ?: 0
            return Command.SetAlarm(LocalTime.of(hour, minute), null)
        }

        val reminderEn = Regex("remind\\s+me\\s+(?:to\\s+)?(.+?)\\s+(?:at|for|on)\\s+(.+)")
        val reminderEnMatch = reminderEn.find(text)
        if (reminderEnMatch != null) {
            val dt = parseEnglishDateTime(reminderEnMatch.groupValues[2].trim())
            if (dt != null) return Command.SetReminder(reminderEnMatch.groupValues[1].trim(), dt)
        }

        if (text.contains(Regex("show\\s+alarms?|list\\s+alarms?|what\\s+alarms?")))
            return Command.ListAlarms

        if (text.contains(Regex("cancel\\s+all\\s+alarms?|delete\\s+all\\s+alarms?|clear\\s+alarms?")))
            return Command.CancelAllAlarms

        val cancelEn = Regex("(?:cancel|delete|remove)\\s+alarm\\s+(\\d+)")
        val cancelEnMatch = cancelEn.find(text)
        if (cancelEnMatch != null) {
            return Command.CancelAlarm(cancelEnMatch.groupValues[1].toLongOrNull() ?: return Command.Unknown)
        }

        val snoozeEn = Regex("snooze\\s+(\\d+)")
        val snoozeEnMatch = snoozeEn.find(text)
        if (snoozeEnMatch != null) {
            return Command.Snooze(snoozeEnMatch.groupValues[1].toIntOrNull() ?: 10)
        }

        // Notifications
        if (text.contains(Regex("read\\s+(?:my\\s+)?messages?|check\\s+messages?|any\\s+(?:new\\s+)?messages?")))
            return Command.ReadNotifications

        if (text.contains(Regex("read\\s+last\\s+message|last\\s+(?:message|notification)")))
            return Command.ReadLastMessage

        // Reading Controls
        if (text.contains(Regex("^(stop|shut\\s*up|quiet)$|stop\\s+reading|stop\\s+speaking")))
            return Command.StopReading

        if (text.contains(Regex("faster|speed\\s*up|read\\s+faster")))
            return Command.ReadFaster

        if (text.contains(Regex("slower|slow\\s*down|read\\s+slower")))
            return Command.ReadSlower

        // General
        if (text.contains(Regex("what\\s+time|current\\s+time|time\\s+is\\s+it")))
            return Command.GetTime

        if (text.contains(Regex("what.*date|today.*date|date\\s+today")))
            return Command.GetDate

        if (text.contains(Regex("^help$|what\\s+can\\s+you\\s+do|commands")))
            return Command.Help

        if (text.contains(Regex("settings|open\\s+settings")))
            return Command.OpenSettings

        if (text.contains(Regex("clear\\s+history|clear\\s+chat")))
            return Command.ClearHistory

        return Command.Unknown
    }

    // ─── TIME / DATE HELPERS ───

    private fun parsePersianHour(hour24: Int, period: String): Int {
        return when {
            period.contains("صبح") -> if (hour24 == 12) 0 else hour24
            period.contains("ظهر") -> if (hour24 == 12) 12 else hour24 + 12
            period.contains("عصر") || period.contains("بعدازظهر") -> if (hour24 == 12) 12 else hour24 + 12
            period.contains("شب") -> if (hour24 == 12) 0 else hour24 + 12
            hour24 in 0..6 -> hour24 + 12 // if not specified, assume PM for low numbers (common Persian pattern)
            else -> hour24.coerceIn(0, 23)
        }
    }

    private fun parseEnglishHour(hour12: Int, period: String): Int {
        return when {
            period == "am" -> if (hour12 == 12) 0 else hour12
            period == "pm" -> if (hour12 == 12) 12 else hour12 + 12
            hour12 in 0..6 -> hour12 + 12
            else -> hour12.coerceIn(0, 23)
        }
    }

    private fun parsePersianDateTime(input: String): LocalDateTime? {
        val now = LocalDate.now(ZoneId.of("Asia/Tehran"))
        val time = extractTime(input) ?: return null
        val date = when {
            input.contains("امروز") || input.contains("همین امروز") -> now
            input.contains("فردا") -> now.plusDays(1)
            input.contains("پس فردا") -> now.plusDays(2)
            input.contains("دیروز") -> now.minusDays(1)
            // Day of week
            input.contains("شنبه") -> nextDayOfWeek(now, DayOfWeek.SATURDAY)
            input.contains("یکشنبه") || input.contains("یک شنبه") -> nextDayOfWeek(now, DayOfWeek.SUNDAY)
            input.contains("دوشنبه") || input.contains("دو شنبه") -> nextDayOfWeek(now, DayOfWeek.MONDAY)
            input.contains("سه شنبه") -> nextDayOfWeek(now, DayOfWeek.TUESDAY)
            input.contains("چهارشنبه") || input.contains("چهار شنبه") -> nextDayOfWeek(now, DayOfWeek.WEDNESDAY)
            input.contains("پنجشنبه") || input.contains("پنج شنبه") -> nextDayOfWeek(now, DayOfWeek.THURSDAY)
            input.contains("جمعه") -> nextDayOfWeek(now, DayOfWeek.FRIDAY)
            else -> now // default to today
        }
        return LocalDateTime.of(date, time)
    }

    private fun parseEnglishDateTime(input: String): LocalDateTime? {
        val now = LocalDate.now()
        val time = extractTime(input) ?: return null
        val date = when {
            input.contains("today") -> now
            input.contains("tomorrow") -> now.plusDays(1)
            input.contains("day after tomorrow") -> now.plusDays(2)
            input.contains("monday") -> nextDayOfWeek(now, DayOfWeek.MONDAY)
            input.contains("tuesday") -> nextDayOfWeek(now, DayOfWeek.TUESDAY)
            input.contains("wednesday") -> nextDayOfWeek(now, DayOfWeek.WEDNESDAY)
            input.contains("thursday") -> nextDayOfWeek(now, DayOfWeek.THURSDAY)
            input.contains("friday") -> nextDayOfWeek(now, DayOfWeek.FRIDAY)
            input.contains("saturday") -> nextDayOfWeek(now, DayOfWeek.SATURDAY)
            input.contains("sunday") -> nextDayOfWeek(now, DayOfWeek.SUNDAY)
            else -> now
        }
        return LocalDateTime.of(date, time)
    }

    private fun extractTime(input: String): LocalTime? {
        // Match "ساعت ۷ صبح" / "ساعت ۷:۳۰" / "at 7 pm" / "7:30"
        val timePattern = Regex("(?:ساعت\\s*)?(\\d{1,2})\\s*[:.؛:]?\\s*(\\d{0,2})\\s*(صبح|ظهر|عصر|بعدازظهر|شب|am|pm)?")
        val match = timePattern.find(input) ?: return null
        val h = match.groupValues[1].toIntOrNull() ?: return null
        val m = match.groupValues[2].toIntOrNull() ?: 0
        val p = match.groupValues[3]

        val hour = when {
            p.contains("صبح") || p == "am" -> if (h == 12) 0 else h
            p.contains("ظهر") -> if (h == 12) 12 else h + 12
            p.contains("عصر") || p.contains("بعدازظهر") || p == "pm" -> if (h == 12) 12 else h + 12
            p.contains("شب") -> if (h == 12) 0 else h + 12
            h in 0..6 -> h + 12 // common Persian: bare "7" usually means 7 AM when context suggests morning
            else -> h.coerceIn(0, 23)
        }
        return LocalTime.of(hour, m.coerceIn(0, 59))
    }

    private fun nextDayOfWeek(from: LocalDate, targetDay: DayOfWeek): LocalDate {
        val daysUntil = (targetDay.value - from.dayOfWeek.value + 7) % 7
        return if (daysUntil == 0) from.plusDays(7) else from.plusDays(daysUntil.toLong())
    }

    private fun extractLabel(text: String, delimiters: List<String>): String? {
        for (delim in delimiters) {
            val idx = text.indexOf(delim)
            if (idx >= 0) {
                val after = text.substring(idx + delim.length).trim()
                if (after.isNotEmpty() && after.length < 50) return after
            }
        }
        return null
    }

    // Public helpers used by CommandExecutor
    companion object {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val persianTimeFormatter = DateTimeFormatter.ofPattern("ساعت HH:mm")
    }
}
