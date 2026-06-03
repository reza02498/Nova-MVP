package com.nova.assistant.domain.intent

data class IntentResult(
    val intent: Intent,
    val confidence: Float,
    val matchedKeywords: List<String> = emptyList()
) {
    val isUnknown: Boolean get() = intent == Intent.UNKNOWN

    companion object {
        fun unknown() = IntentResult(Intent.UNKNOWN, 0f)
        fun of(intent: Intent, confidence: Float, keywords: List<String> = emptyList()) =
            IntentResult(intent, confidence.coerceIn(0f, 1f), keywords)
    }
}
