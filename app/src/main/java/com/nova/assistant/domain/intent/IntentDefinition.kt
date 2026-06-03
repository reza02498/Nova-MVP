package com.nova.assistant.domain.intent

data class SynonymGroup(
    val canonical: String,
    val variations: List<String>
)

data class IntentDefinition(
    val intent: Intent,
    val threshold: Float = 0.4f,
    val positiveKeywords: List<String>,
    val synonyms: List<SynonymGroup> = emptyList(),
    val negativeKeywords: List<String> = emptyList(),
    val requiresNumber: Boolean = false,
    val requiresTime: Boolean = false,
    val examples: List<String>
)
