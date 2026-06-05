package com.nova.assistant.domain.nlu.tokenizer

enum class TokenType {
    // Structural
    ACTION,
    NEGATION,
    OBJECT_MARKER,

    // Targets (nouns that indicate WHAT the user wants to operate on)
    TARGET_TIME,
    TARGET_DATE,
    TARGET_DEVICE,
    TARGET_NOTE,
    TARGET_ALARM,
    TARGET_TIMER,
    TARGET_MESSAGE,

    // Values (extracted entities)
    TIME_VALUE,
    DATE_VALUE,
    DURATION_VALUE,
    NUMBER_VALUE,

    // Question/Command markers
    QUESTION,
    CONFIRM,
    CANCEL,

    // Content
    CONTENT,
    FILLER,
    UNKNOWN
}
