package com.nova.assistant.util

import android.content.Context
import java.util.Locale

object PhoneLanguage {

    fun isPersian(context: Context): Boolean {
        val locale = context.resources.configuration.locales[0]
        return locale.language == "fa" || locale.language == "ar" || locale.country == "IR" || locale.country == "AF"
    }

    fun getCurrentLocale(context: Context): Locale {
        return context.resources.configuration.locales[0]
    }

    /**
     * Returns the path to enable notification listener in the user's language.
     */
    fun notificationListenerPath(context: Context): String {
        return if (isPersian(context)) {
            "تنظیمات گوشی ← دسترسی‌پذیری (Accessibility) ← Nova ← فعال کردن"
        } else {
            "Settings → Accessibility → Installed apps → Nova → Turn ON"
        }
    }

    /**
     * Returns the path to download offline speech recognition in the user's language.
     */
    fun offlineSpeechPath(context: Context): String {
        return if (isPersian(context)) {
            "تنظیمات ← مدیریت عمومی ← زبان و ورودی ← تشخیص گفتار Google برگه All ← فارسی ← دانلود"
        } else {
            "Settings → Language & Input → Google Voice Typing → Offline speech recognition → All → Persian → Download"
        }
    }

    /**
     * Returns the path to enable exact alarm permission in the user's language.
     */
    fun exactAlarmPath(context: Context): String {
        return if (isPersian(context)) {
            "تنظیمات ← برنامه‌ها ← Nova ← مجوزها ← آلارم‌ها و یادآوری‌ها ← فعال"
        } else {
            "Settings → Apps → Nova → Permissions → Alarms & reminders → Allow"
        }
    }
}
