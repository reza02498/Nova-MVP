package com.nova.assistant.domain.intent

/**
 * Configuration-only. No business logic. No Android dependencies.
 * All 18 intent definitions with keywords, synonyms, thresholds, and examples.
 */
object IntentDefinitions {

    val all: List<IntentDefinition> = listOf(
        // ═══ DEVICE CONTROLS ═══
        IntentDefinition(
            intent = Intent.TOGGLE_WIFI,
            threshold = 0.30f,
            positiveKeywords = listOf("وای فای", "وایفای", "wifi"),
            synonyms = listOf(
                SynonymGroup("وای فای", listOf("وای‌فای", "وایفای", "وایرلس", "بی‌سیم", "اینترنت", "نت", "شبکه")),
                SynonymGroup("روشن", listOf("فعال", "وصل", "باز", "روشن کن", "روشنش کن", "وصلش کن", "بزن", "on", "enable")),
                SynonymGroup("خاموش", listOf("قطع", "ببند", "ببندش", "بستن", "خاموش کن", "قطع کن", "off", "disable"))
            ),
            negativeKeywords = listOf("بلوتوث", "دیتا", "موبایل", "باتری"),
            examples = listOf(
                "وای فای رو روشن کن", "وایفای روشن بشه", "نت رو وصل کن",
                "شبکه رو روشن کن", "اینترنت گوشی رو فعال کن", "وای فای رو خاموش کن",
                "وایفای رو قطع کن", "نت رو ببند", "اینترنت رو قطع کن"
            )
        ),
        IntentDefinition(
            intent = Intent.TOGGLE_BLUETOOTH,
            threshold = 0.30f,
            positiveKeywords = listOf("بلوتوث", "bluetooth"),
            synonyms = listOf(
                SynonymGroup("بلوتوث", listOf("بلوتوس", "بلوتوت", "bluetooth", "bt")),
                SynonymGroup("روشن", listOf("فعال", "وصل", "باز", "روشن کن", "on", "enable", "connect")),
                SynonymGroup("خاموش", listOf("قطع", "ببند", "off", "disable", "disconnect"))
            ),
            negativeKeywords = listOf("وای فای", "wifi", "چراغ قوه", "فلش"),
            examples = listOf(
                "بلوتوث رو روشن کن", "بلوتوث رو فعال کن", "بلوتوث رو وصل کن",
                "bluetooth on", "بلوتوث گوشی رو باز کن", "bt رو روشن کن",
                "بلوتوث رو خاموش کن", "bluetooth off"
            )
        ),
        IntentDefinition(
            intent = Intent.TOGGLE_FLASHLIGHT,
            threshold = 0.30f,
            positiveKeywords = listOf("چراغ قوه", "فلش", "flashlight"),
            synonyms = listOf(
                SynonymGroup("چراغ قوه", listOf("چراق قوه", "چراغقوه", "فلش", "فلش لایت", "flashlight", "torch", "چراغ", "نور گوشی", "روشنایی گوشی")),
                SynonymGroup("روشن", listOf("فعال", "باز", "on", "enable", "بزن", "روشن کن"))
            ),
            negativeKeywords = listOf("صفحه", "نور صفحه", "بلوتوث", "باتری"),
            examples = listOf(
                "چراغ قوه رو روشن کن", "فلش رو باز کن", "فلش لایت رو روشن کن",
                "چراغ قوه رو فعال کن", "torch on", "چراغ گوشی رو روشن کن", "نور گوشی رو باز کن"
            )
        ),

        // ═══ ALARMS ═══
        IntentDefinition(
            intent = Intent.SET_ALARM,
            threshold = 0.30f,
            positiveKeywords = listOf("آلارم", "زنگ", "بیدار", "کوک"),
            synonyms = listOf(
                SynonymGroup("آلارم", listOf("الارم", "زنگ", "هشدار", "یادآور")),
                SynonymGroup("تنظیم", listOf("بذار", "بزن", "کوک کن", "تنظیم کن", "set"))
            ),
            negativeKeywords = listOf("لیست", "نشون", "همه", "حذف", "پاک", "کنسل", "لغو", "تایمر"),
            requiresTime = true,
            examples = listOf(
                "آلارم بذار برای ساعت ۷ صبح", "زنگ بزن ۷", "ساعت ۶ بیدارم کن",
                "الارم رو کوک کن برای ۸", "یه زنگ بذار برای ۵",
                "ساعت ۷ صبح بیدارم کن", "آلارم تنظیم کن ۱۰ شب"
            )
        ),
        IntentDefinition(
            intent = Intent.SET_REMINDER,
            threshold = 0.30f,
            positiveKeywords = listOf("یادآوری", "یادم بنداز", "یادم بیار"),
            synonyms = listOf(
                SynonymGroup("یادآوری", listOf("یاداوری", "یاد اوری", "یادم بنداز", "یادم بیار", "reminder", "یادم باشه")),
                SynonymGroup("کن", listOf("بکن", "انجام بده", "بگو"))
            ),
            negativeKeywords = listOf("آلارم", "تایمر", "یادداشت"),
            requiresTime = true,
            examples = listOf(
                "یادآوری کن نان بخر فردا ساعت ۱۰", "یادم بنداز جلسه دارم ساعت ۳",
                "یادم بیار فردا بهش زنگ بزنم", "reminder call mom at 5"
            )
        ),
        IntentDefinition(
            intent = Intent.LIST_ALARMS,
            threshold = 0.30f,
            positiveKeywords = listOf("آلارم", "زنگ"),
            synonyms = listOf(
                SynonymGroup("لیست", listOf("نشون بده", "بده", "داری", "بگو", "چی", "چیا", "کدوم", "برنامه", "لیست کن", "نمایش"))
            ),
            negativeKeywords = listOf("حذف", "پاک", "کنسل", "بذار", "بزن", "کوک", "یادداشت"),
            examples = listOf(
                "آلارما رو نشون بده", "چه آلارمایی داری", "لیست آلارم‌ها",
                "زنگایی که تنظیم کردم رو بگو", "آلارم‌ها رو نمایش بده", "برنامه آلارم‌ها چیه"
            )
        ),
        IntentDefinition(
            intent = Intent.CANCEL_ALARM,
            threshold = 0.30f,
            positiveKeywords = listOf("آلارم", "زنگ"),
            synonyms = listOf(
                SynonymGroup("حذف", listOf("پاک کن", "کنسل", "لغو", "قطع", "بردار", "remove"))
            ),
            negativeKeywords = listOf("همه", "تموم", "کل"),
            requiresNumber = true,
            examples = listOf(
                "آلارم ۳ رو حذف کن", "زنگ ۱ رو کنسل کن", "آلارم شماره ۲ رو پاک کن",
                "الارم ۵ رو لغو کن", "حذف آلارم ۱"
            )
        ),
        IntentDefinition(
            intent = Intent.CANCEL_ALL_ALARMS,
            threshold = 0.5f,
            positiveKeywords = listOf("آلارم", "زنگ"),
            synonyms = listOf(
                SynonymGroup("همه", listOf("تموم", "کل", "همشون", "همشونو")),
                SynonymGroup("حذف", listOf("پاک کن", "کنسل", "لغو", "بردار"))
            ),
            examples = listOf(
                "همه آلارما رو پاک کن", "تموم زنگا رو حذف کن", "آلارما رو کلا لغو کن",
                "همشونو کنسل کن", "کل آلارم‌ها رو پاک کن"
            )
        ),
        IntentDefinition(
            intent = Intent.SNOOZE,
            threshold = 0.3f,
            positiveKeywords = listOf("چرت", "اسنوز", "snooze"),
            synonyms = listOf(
                SynonymGroup("چرت", listOf("اسنوز", "snooze", "خواب", "باز مزاحم", "عقب بنداز", "دیرتر"))
            ),
            examples = listOf(
                "چرت ۱۰ دقیقه", "اسنوز ۵", "چرت بزن",
                "snooze 10", "ده دقیقه دیرتر"
            )
        ),

        // ═══ TIMER ═══
        IntentDefinition(
            intent = Intent.SET_TIMER,
            threshold = 0.30f,
            positiveKeywords = listOf("تایمر", "timer"),
            synonyms = listOf(
                SynonymGroup("تایمر", listOf("تایمز", "تایم", "زمانسنج", "کرنومتر", "شمارشگر", "timer")),
                SynonymGroup("تنظیم", listOf("بذار", "بزن", "set", "start", "شروع", "تنظیم کن"))
            ),
            negativeKeywords = listOf("قطع", "کنسل", "حذف", "خاموش", "آلارم", "زنگ"),
            requiresNumber = true,
            examples = listOf(
                "تایمر ۱۰ دقیقه", "تایمر بذار برای ۵ دقیقه",
                "یه تایمر ۲۰ دقیقه ای تنظیم کن", "تایمز ۱۵",
                "زمانسنج ۳۰ دقیقه", "timer set 10", "شروع تایمر ۸ دقیقه"
            )
        ),
        IntentDefinition(
            intent = Intent.CANCEL_TIMER,
            threshold = 0.30f,
            positiveKeywords = listOf("تایمر", "timer"),
            synonyms = listOf(
                SynonymGroup("حذف", listOf("قطع", "کنسل", "لغو", "خاموش کن", "استاپ", "بسه", "بس کن", "stop"))
            ),
            examples = listOf(
                "تایمر رو قطع کن", "تایمر رو کنسل کن", "تایمر رو خاموش کن",
                "stop timer", "تایمر بسه", "قطع تایمر"
            )
        ),

        // ═══ NOTES ═══
        IntentDefinition(
            intent = Intent.CREATE_NOTE,
            threshold = 0.30f,
            positiveKeywords = listOf("یادداشت", "بنویس", "ثبت کن"),
            synonyms = listOf(
                SynonymGroup("یادداشت", listOf("یاداشت", "نوشته", "note", "memo", "یادگاری")),
                SynonymGroup("ذخیره", listOf("کن", "بذار", "بزن", "ثبت", "بنویس", "save", "write"))
            ),
            negativeKeywords = listOf("نشون", "بده", "لیست", "حذف", "پاک", "بخون", "جستجو"),
            examples = listOf(
                "یادداشت کن شماره حساب ۶۰۳۷", "بنویس فردا دکتر دارم",
                "یادداشت جدید بذار", "ثبت کن جلسه ساعت ۳",
                "یه یادداشت بزن قرار ملاقات", "یاداشت کن شیر بخر"
            )
        ),
        IntentDefinition(
            intent = Intent.LIST_NOTES,
            threshold = 0.30f,
            positiveKeywords = listOf("یادداشت", "یاداشت"),
            synonyms = listOf(
                SynonymGroup("لیست", listOf("نشون بده", "بده", "بیار", "بگو", "چی", "چیا", "دارم", "نوشتم", "نمایش"))
            ),
            negativeKeywords = listOf("حذف", "پاک", "بخون", "جستجو", "کن", "بذار", "جدید", "بنویس"),
            examples = listOf(
                "یادداشتامو نشون بده", "چه یادداشتایی دارم", "یادداشت‌ها رو بیار",
                "نوشته‌هام رو نمایش بده", "یاداشتا رو بگو"
            )
        ),
        IntentDefinition(
            intent = Intent.DELETE_NOTE,
            threshold = 0.30f,
            positiveKeywords = listOf("یادداشت", "یاداشت"),
            synonyms = listOf(
                SynonymGroup("حذف", listOf("پاک", "بردار", "حذف کن", "پاک کن", "remove"))
            ),
            negativeKeywords = listOf("همه", "جستجو", "لیست"),
            requiresNumber = true,
            examples = listOf(
                "حذف یادداشت ۳", "یادداشت ۲ رو پاک کن", "یادداشت شماره ۱ رو حذف کن",
                "پاک کن یادداشت ۴", "حذف یادداشت ۱"
            )
        ),
        IntentDefinition(
            intent = Intent.SEARCH_NOTES,
            threshold = 0.30f,
            positiveKeywords = listOf("یادداشت", "یاداشت"),
            synonyms = listOf(
                SynonymGroup("جستجو", listOf("بگرد", "پیدا کن", "سرچ", "search", "find", "بگرد تو"))
            ),
            examples = listOf(
                "تو یادداشتا بگرد شماره", "جستجوی یادداشت حساب",
                "پیدا کن تو یادداشت‌ها", "سرچ یادداشت دکتر"
            )
        ),

        // ═══ NOTIFICATIONS ═══
        IntentDefinition(
            intent = Intent.READ_NOTIFICATIONS,
            threshold = 0.30f,
            positiveKeywords = listOf("پیام", "اعلان", "نوتیف"),
            synonyms = listOf(
                SynonymGroup("پیام", listOf("پیغام", "اعلان", "نوتیفیکیشن", "پیامک", "message", "notification", "sms")),
                SynonymGroup("بخوان", listOf("بخون", "نشون بده", "بگو", "چک کن", "بررسی کن", "ببین", "چی اومده"))
            ),
            negativeKeywords = listOf("بفرست", "ارسال", "جواب", "آخرین"),
            examples = listOf(
                "پیامامو بخون", "اعلان‌ها رو نشون بده", "چه پیامایی دارم",
                "کی پیام داده", "پیام‌های جدید رو بگو", "چک کن کی بهم پیام داده"
            )
        ),
        IntentDefinition(
            intent = Intent.READ_LAST_MESSAGE,
            threshold = 0.30f,
            positiveKeywords = listOf("آخرین پیام", "پیام آخر", "آخرین اعلان"),
            synonyms = listOf(
                SynonymGroup("آخرین", listOf("آخر", "اخر", "جدیدترین", "تازه‌ترین", "last", "latest"))
            ),
            negativeKeywords = listOf("همه", "بفرست", "ارسال", "جواب"),
            examples = listOf(
                "آخرین پیام رو بخون", "پیام آخر رو نشون بده",
                "آخرین اعلان چی بود", "آخر پیام رو بگو", "جدیدترین پیام"
            )
        )
    )

    fun forIntent(intent: Intent): IntentDefinition =
        all.first { it.intent == intent }
}
