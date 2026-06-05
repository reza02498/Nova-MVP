package com.nova.assistant.domain.nlu.tokenizer

data class Token(
    val text: String,
    val normalized: String,
    val type: TokenType,
    val position: Int,
    val confidence: Float = 1.0f
) {
    val isAction: Boolean get() = type == TokenType.ACTION
    val isTarget: Boolean get() = type.name.startsWith("TARGET_")
    val isNegation: Boolean get() = type == TokenType.NEGATION
    val isQuestion: Boolean get() = type == TokenType.QUESTION
    val isValue: Boolean get() = type.name.endsWith("_VALUE")
}
