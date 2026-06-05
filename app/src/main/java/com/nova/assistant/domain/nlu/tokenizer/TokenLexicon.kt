package com.nova.assistant.domain.nlu.tokenizer

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps Persian words to their TokenType.
 * ~300 high-frequency Persian command words with type annotations.
 */
@Singleton
class TokenLexicon @Inject constructor() {

    private val map: Map<String, TokenType> = buildMap()

    fun typeOf(word: String): TokenType = map[word] ?: TokenType.UNKNOWN

    fun isKnown(word: String): Boolean = word in map

    // ═══════════════════════════════════════════
    //  LEXICON (data — no business logic)
    // ═══════════════════════════════════════════

    private fun buildMap(): Map<String, TokenType> {
        val m = mutableMapOf<String, TokenType>()

        // ── ACTIONS (verbs — what the user wants to DO) ──
        val actions = listOf(
            // Enable/Turn on
            "روشن", "روشن کن", "فعال", "فعال کن", "وصل", "وصل کن", "باز کن",
            "enable", "start", "on",
            // Disable/Turn off
            "خاموش", "خاموش کن", "قطع", "قطع کن", "ببند", "ببندش", "بستن",
            "disable", "stop", "off",
            // Set/Create
            "بذار", "بگذار", "تنظیم", "تنظیم کن", "کوک", "کوک کن", "set", "create",
            "بزن", "بنداز",
            // Query/Tell
            "بگو", "بگین", "نشون بده", "ببین", "ببینم", "بخون", "اعلام کن",
            "tell", "show", "read",
            // Delete
            "حذف", "حذف کن", "پاک", "پاک کن", "بردار", "لغو", "کنسل", "کن",
            "delete", "remove", "cancel",
            // Wake/Remind
            "بیدار", "بیدار کن", "بیدارم کن", "صدا کن", "خبر کن", "یادآوری کن",
            "wake", "remind",
            // Search
            "بگرد", "پیدا کن", "جستجو", "سرچ", "search", "find",
            // Write
            "بنویس", "ثبت کن", "یادداشت کن", "write", "save"
        )
        for (a in actions) m[a] = TokenType.ACTION

        // ── TARGETS (nouns — WHAT the user wants to operate on) ──

        // Time targets
        val timeTargets = listOf("ساعت", "زمان", "وقت", "تایم", "time", "clock")
        for (t in timeTargets) m[t] = TokenType.TARGET_TIME

        // Date targets
        val dateTargets = listOf("تاریخ", "تقویم", "امروز", "date", "calendar")
        for (t in dateTargets) m[t] = TokenType.TARGET_DATE

        // Device targets — WiFi
        val wifiTargets = listOf(
            "وای فای", "وایفای", "وای‌فای", "وایرلس", "اینترنت", "نت", "شبکه", "wifi"
        )
        for (t in wifiTargets) m[t] = TokenType.TARGET_DEVICE

        // Device targets — Bluetooth
        val btTargets = listOf("بلوتوث", "بلوتوس", "بلوتوت", "bluetooth", "bt")
        for (t in btTargets) m[t] = TokenType.TARGET_DEVICE

        // Device targets — Flashlight
        val flTargets = listOf("چراغ قوه", "چراغقوه", "فلش", "فلش لایت", "flashlight", "torch")
        for (t in flTargets) m[t] = TokenType.TARGET_DEVICE

        // Note targets
        val noteTargets = listOf("یادداشت", "یاداشت", "نوشته", "نوت", "note", "memo")
        for (t in noteTargets) m[t] = TokenType.TARGET_NOTE

        // Alarm targets
        val alarmTargets = listOf("آلارم", "الارم", "زنگ", "هشدار", "بیدارباش", "alarm")
        for (t in alarmTargets) m[t] = TokenType.TARGET_ALARM

        // Timer targets
        val timerTargets = listOf("تایمر", "تایمز", "زمانسنج", "کرنومتر", "شمارشگر", "timer")
        for (t in timerTargets) m[t] = TokenType.TARGET_TIMER

        // Message targets
        val msgTargets = listOf("پیام", "اعلان", "نوتیفیکیشن", "نوتیف", "پیامک", "message", "notification", "sms")
        for (t in msgTargets) m[t] = TokenType.TARGET_MESSAGE

        // ── NEGATION ──
        val negations = listOf("نکن", "نخون", "نذار", "نبین", "ننویس", "نمیخوام", "نمی‌خوام", "نخواستم", "لازم نیست", "not", "don't")
        for (n in negations) m[n] = TokenType.NEGATION

        // ── QUESTION WORDS ──
        val questions = listOf("چنده", "چند", "چطور", "چطوره", "کدوم", "چی", "چیه", "کی", "کجا", "چرا", "چه", "آیا", "چقدر", "چقدره", "چند وقته", "چندمه", "؟")
        for (q in questions) m[q] = TokenType.QUESTION

        // ── CONFIRM ──
        val confirms = listOf("آره", "بله", "باشه", "اوکی", "ok", "yes", "درسته", "حتما", "چشم")
        for (c in confirms) m[c] = TokenType.CONFIRM

        // ── CANCEL ──
        val cancels = listOf("نه", "بیخیال", "لغو", "منصرف", "ولش کن", "no", "cancel", "نمیخوام")
        for (c in cancels) m[c] = TokenType.CANCEL

        // ── OBJECT MARKERS ──
        val markers = listOf("رو", "را")
        for (om in markers) m[om] = TokenType.OBJECT_MARKER

        // ── FILLERS ──
        val fillers = listOf("خب", "ببین", "ببینم", "لطفا", "لطفاً", "میشه", "می‌شه", "یه", "یکی", "برام", "واسم", "راستی", "ضمنا", "آخه", "دیگه", "اصلا", "اصلاً", "اینکه", "اونکه", "please", "just")
        for (f in fillers) m[f] = TokenType.FILLER

        // ── CONTENT MARKERS (words that indicate free-text content follows) ──
        val contentMarkers = listOf("که", "تا", "برای", "واسه", "درباره", "راجع به", "about", "for")
        for (cm in contentMarkers) m[cm] = TokenType.CONTENT

        return m
    }

    companion object {
        /** Known time indicators (not in lexicon map — used by EntityExtractors) */
        val TIME_PERIODS = setOf("صبح", "ظهر", "عصر", "شب", "بعدازظهر", "بعد از ظهر", "نیمه شب", "am", "pm")

        /** Known date words */
        val DATE_WORDS = setOf("امروز", "فردا", "دیروز", "پس فردا", "پسفردا",
            "شنبه", "یکشنبه", "یک شنبه", "دوشنبه", "دو شنبه", "سه شنبه", "سه‌شنبه",
            "چهارشنبه", "چهار شنبه", "چارشنبه", "پنجشنبه", "پنج شنبه", "جمعه")

        /** Known duration units */
        val DURATION_UNITS = mapOf(
            "ثانیه" to 1, "دقیقه" to 60, "ساعت" to 3600,
            "دقه" to 60, "ثانیه" to 1, "دقیقست" to 60
        )

        /** Persian word numbers */
        val WORD_NUMBERS = mapOf(
            "یک" to 1, "دو" to 2, "سه" to 3, "چهار" to 4, "پنج" to 5,
            "شش" to 6, "هفت" to 7, "هشت" to 8, "نه" to 9, "ده" to 10,
            "یازده" to 11, "دوازده" to 12, "سیزده" to 13, "چهارده" to 14, "پانزده" to 15,
            "شانزده" to 16, "هفده" to 17, "هجده" to 18, "نوزده" to 19, "بیست" to 20,
            "سی" to 30, "چهل" to 40, "پنجاه" to 50, "شصت" to 60,
            "نیم" to 30, "ربع" to 15,
            "اول" to 1, "دوم" to 2, "سوم" to 3, "چهارم" to 4, "پنجم" to 5,
            "ششم" to 6, "هفتم" to 7, "هشتم" to 8, "نهم" to 9, "دهم" to 10,
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10
        )
    }
}
