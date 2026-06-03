package com.nova.assistant.domain.entity

import java.time.LocalDate
import java.time.LocalTime

enum class DeviceTarget { WIFI, BLUETOOTH, FLASHLIGHT }
enum class DeviceAction { ON, OFF, TOGGLE, STATUS }

data class ExtractedEntities(
    val time: LocalTime? = null,
    val date: LocalDate? = null,
    val number: Int? = null,
    val duration: Int? = null,
    val taskContent: String? = null,
    val deviceTarget: DeviceTarget? = null,
    val deviceAction: DeviceAction? = null
) {
    val hasTime: Boolean get() = time != null
    val hasNumber: Boolean get() = number != null || duration != null
}
