package com.nova.assistant.domain.nlu.entity

import com.nova.assistant.domain.nlu.tokenizer.Token
import com.nova.assistant.domain.nlu.tokenizer.TokenType

enum class DeviceTarget { WIFI, BLUETOOTH, FLASHLIGHT, UNKNOWN }
enum class DeviceAction { ON, OFF, TOGGLE, UNKNOWN }

object DeviceExtractor {

    private val wifiWords = setOf("وای فای", "وایفای", "وای‌فای", "وایرلس", "اینترنت", "نت", "شبکه", "wifi")
    private val btWords = setOf("بلوتوث", "بلوتوس", "بلوتوت", "bluetooth", "bt")
    private val flWords = setOf("چراغ قوه", "چراغقوه", "فلش", "فلش لایت", "flashlight", "torch", "چراغ")
    private val onWords = setOf("روشن", "فعال", "وصل", "باز", "on", "enable", "start", "روشن کن", "وصل کن")
    private val offWords = setOf("خاموش", "قطع", "ببند", "off", "disable", "stop", "خاموش کن", "قطع کن", "ببندش")

    fun extractTarget(tokens: List<Token>): DeviceTarget {
        for (token in tokens) {
            if (token.normalized in wifiWords) return DeviceTarget.WIFI
            if (token.normalized in btWords) return DeviceTarget.BLUETOOTH
            if (token.normalized in flWords) return DeviceTarget.FLASHLIGHT
        }
        return DeviceTarget.UNKNOWN
    }

    fun extractAction(tokens: List<Token>): DeviceAction {
        for (token in tokens) {
            if (token.normalized in onWords) return DeviceAction.ON
            if (token.normalized in offWords) return DeviceAction.OFF
        }
        return DeviceAction.TOGGLE
    }
}
