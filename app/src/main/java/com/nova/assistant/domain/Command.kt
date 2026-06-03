package com.nova.assistant.domain

import java.time.LocalDateTime
import java.time.LocalTime

sealed class Command {
    // --- Alarms ---
    data class SetAlarm(val time: LocalTime, val label: String?) : Command()
    data class SetReminder(val task: String, val dateTime: LocalDateTime) : Command()
    object ListAlarms : Command()
    data class CancelAlarm(val alarmId: Long) : Command()
    object CancelAllAlarms : Command()
    data class Snooze(val minutes: Int) : Command()

    // --- Notifications ---
    object ReadNotifications : Command()
    object ReadLastMessage : Command()

    // --- Reading Controls ---
    object StopReading : Command()
    object ReadFaster : Command()
    object ReadSlower : Command()

    // --- General ---
    object GetTime : Command()
    object GetDate : Command()
    object Help : Command()
    object OpenSettings : Command()
    object ClearHistory : Command()

    object Unknown : Command()
}
