package com.nova.assistant.domain

import com.nova.assistant.domain.context.ConversationContext
import com.nova.assistant.domain.entity.DeviceAction
import com.nova.assistant.domain.entity.DeviceTarget
import com.nova.assistant.domain.entity.EntityExtractor
import com.nova.assistant.domain.intent.Intent
import com.nova.assistant.domain.intent.IntentClassifier
import com.nova.assistant.domain.normalizer.PersianNormalizer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * Command Understanding Engine — replaces the old if/else chain with a
 * two-tier architecture:
 *
 *   Tier 1 — directMatch(): unambiguous commands bypass classification
 *   Tier 2 — classify + extract + map: intent-based commands
 *
 * Public API preserved: fun parse(text: String): Command
 */
class CommandParser @Inject constructor(
    private val normalizer: PersianNormalizer,
    private val classifier: IntentClassifier,
    private val extractor: EntityExtractor
) {
    private var lastContext: ConversationContext? = null

    fun parse(input: String): Command {
        if (input.isBlank()) return Command.Unknown
        val text = normalizer.normalize(input)
        if (text.isEmpty()) return Command.Unknown

        // ── Tier 1: Direct Commands (no ambiguity, no classification) ──
        directMatch(text)?.let { return it }

        // ── Tier 2: Intent-based Commands ──
        val result = classifier.classify(text)
        android.util.Log.d("NovaParser", "Intent=${result.intent} conf=${result.confidence} keywords=${result.matchedKeywords}")
        if (result.isUnknown) return Command.Unknown

        val entities = extractor.extract(text, result.intent, lastContext)
        val command = mapToCommand(result.intent, entities)

        lastContext = ConversationContext(
            lastIntent = result.intent,
            lastEntities = entities,
            timestamp = Instant.now()
        )

        return command
    }

    // ═══════════════════════ TIER 1: DIRECT COMMANDS ═══════════════════════

    private val imperativeVerbs = listOf("بگو", "بگین", "نشون بده", "ببین", "ببینم", "چنده", "چند", "چند است", "چیه", "چطوره")
    private val timeSynonyms = listOf("ساعت", "ساعته", "زمان", "وقت", "تایم", "time")

    private fun directMatch(text: String): Command? = when {
        // Time — recognizes time synonyms + question/imperative patterns
        timeSynonyms.any { text.contains(it) } && imperativeVerbs.any { text.contains(it) } -> Command.GetTime
        timeSynonyms.any { text.contains(it) } && text.contains("الان") -> Command.GetTime
        text.contains("الان") && containsAny(text, "چنده", "چند وقته", "چند") -> Command.GetTime
        text == "ساعت" || text == "ساعت چنده" || text == "ساعت چند" -> Command.GetTime
        // Date
        containsAny(text, "امروز چندمه", "تاریخ امروز", "چه روزی", "چندمه", "چندم", "چند شنبه") -> Command.GetDate
        text.contains("امروز") && text.contains("چند") -> Command.GetDate
        text.contains("تاریخ") && containsAny(text, "بگو", "نشون بده", "چیه", "چندمه") -> Command.GetDate
        // Help
        text == "راهنما" || text == "کمک" || text == "help" || text == "؟" ||
        containsAny(text, "چه کارایی", "چه کارا", "چیکار", "چی کار", "چکار", "چیا میتونی") -> Command.Help
        // Settings
        text.contains("تنظیمات") || text.contains("setting") || text.contains("ستینگ") -> Command.OpenSettings
        // Clear history
        containsAny(text, "پاک کن تاریخچه", "حذف تاریخچه", "حذف گفتگو", "پاک گفتگو", "تاریخچه پاک", "گفتگو حذف") -> Command.ClearHistory
        text.contains("تاریخچه") && containsAny(text, "پاک", "حذف") -> Command.ClearHistory
        // Reading controls
        text == "بس کن" || text == "قطع کن" || text == "خاموش کن" || text == "بسه" ||
        text == "قطع" || text == "خاموش" || text == "ساکت" || text == "stop" || text == "دیگه بسه" -> Command.StopReading
        containsAny(text, "تندتر", "سریعتر", "سرعت بیشتر", "تندتر بخون", "تند بخون") -> Command.ReadFaster
        containsAny(text, "یواشتر", "آرومتر", "آهسته تر", "کندتر", "یواشتر بخون", "یواش بخون", "آهسته بخون") -> Command.ReadSlower
        // Brightness (informational only)
        containsAny(text, "نور صفحه", "روشنایی صفحه", "نور", "brightness") &&
        containsAny(text, "زیاد", "بالا", "ببر", "کم", "پایین") -> Command.DeviceToggle("brightness")
        // Airplane mode (informational only)
        containsAny(text, "حالت پرواز", "airplane") -> Command.DeviceToggle("airplane")
        // Battery
        containsAny(text, "باتری", "باطری", "شارژ") &&
        containsAny(text, "چند", "چقدر", "وضعیت", "چنده", "چقدره", "چند درصده") -> Command.DeviceToggle("battery")
        else -> null
    }

    // ═══════════════════════ TIER 2: MAPPER (private) ═══════════════════════

    private fun mapToCommand(intent: Intent, e: com.nova.assistant.domain.entity.ExtractedEntities): Command {
        return when (intent) {
            // ── Alarms ──
            Intent.SET_ALARM -> {
                val time = e.time ?: return Command.Unknown
                Command.SetAlarm(time, e.taskContent)
            }
            Intent.SET_REMINDER -> {
                val time = e.time ?: return Command.Unknown
                val date = e.date ?: LocalDate.now()
                Command.SetReminder(e.taskContent ?: "یادآوری", LocalDateTime.of(date, time))
            }
            Intent.LIST_ALARMS -> Command.ListAlarms
            Intent.CANCEL_ALARM -> {
                val id = e.number?.toLong() ?: return Command.Unknown
                Command.CancelAlarm(id)
            }
            Intent.CANCEL_ALL_ALARMS -> Command.CancelAllAlarms
            Intent.SNOOZE -> Command.Snooze(e.duration ?: 10)

            // ── Timer ──
            Intent.SET_TIMER -> {
                val mins = e.duration ?: return Command.Unknown
                Command.SetTimer(mins)
            }
            Intent.CANCEL_TIMER -> Command.CancelTimer

            // ── Notes ──
            Intent.CREATE_NOTE -> {
                val content = e.taskContent ?: return Command.Unknown
                Command.CreateNote(content)
            }
            Intent.LIST_NOTES -> Command.ListNotes
            Intent.DELETE_NOTE -> {
                val id = e.number?.toLong() ?: return Command.Unknown
                Command.DeleteNote(id)
            }
            Intent.SEARCH_NOTES -> {
                val query = e.taskContent ?: return Command.Unknown
                Command.SearchNotes(query)
            }

            // ── Device ──
            Intent.TOGGLE_WIFI -> {
                val action = e.deviceAction ?: DeviceAction.TOGGLE
                val setting = when (action) {
                    DeviceAction.ON -> "wifi_on"
                    DeviceAction.OFF -> "wifi_off"
                    else -> "wifi_toggle"
                }
                Command.DeviceToggle(setting)
            }
            Intent.TOGGLE_BLUETOOTH -> {
                val action = e.deviceAction ?: DeviceAction.TOGGLE
                val setting = when (action) {
                    DeviceAction.ON -> "bt_on"
                    DeviceAction.OFF -> "bt_off"
                    else -> "bt_toggle"
                }
                Command.DeviceToggle(setting)
            }
            Intent.TOGGLE_FLASHLIGHT -> Command.DeviceToggle("flash_toggle")

            // ── Notifications ──
            Intent.READ_NOTIFICATIONS -> Command.ReadNotifications
            Intent.READ_LAST_MESSAGE -> Command.ReadLastMessage

            Intent.UNKNOWN -> Command.Unknown
        }
    }

    // ═══════════════════════ HELPERS ═══════════════════════

    private fun containsAny(text: String, vararg words: String): Boolean =
        words.any { text.contains(it) }
}
