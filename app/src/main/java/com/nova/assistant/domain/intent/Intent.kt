package com.nova.assistant.domain.intent

/**
 * Functional intents that require classification.
 * Direct/unambiguous commands (GetTime, Help, etc.) bypass classification entirely
 * and are handled by CommandParser.directMatch().
 */
enum class Intent {
    // Device Controls (3)
    TOGGLE_WIFI,
    TOGGLE_BLUETOOTH,
    TOGGLE_FLASHLIGHT,

    // Alarms (6)
    SET_ALARM,
    SET_REMINDER,
    LIST_ALARMS,
    CANCEL_ALARM,
    CANCEL_ALL_ALARMS,
    SNOOZE,

    // Timer (2)
    SET_TIMER,
    CANCEL_TIMER,

    // Notes (4)
    CREATE_NOTE,
    LIST_NOTES,
    DELETE_NOTE,
    SEARCH_NOTES,

    // Notifications (2)
    READ_NOTIFICATIONS,
    READ_LAST_MESSAGE,

    // Fallback
    UNKNOWN
}
