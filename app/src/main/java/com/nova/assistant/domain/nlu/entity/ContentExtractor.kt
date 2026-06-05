package com.nova.assistant.domain.nlu.entity

import com.nova.assistant.domain.nlu.tokenizer.Token
import com.nova.assistant.domain.nlu.tokenizer.TokenType

object ContentExtractor {

    /**
     * Extracts free-text content from a command.
     * Content = tokens that are NOT structural (not action, target, filler, marker, etc.)
     *
     * Example: "یادداشت کن خرید نان از نونوایی"
     *   → ACTION: "کن" (part of compound "یادداشت کن")
     *   → TARGET_NOTE: "یادداشت"
     *   → CONTENT: "خرید نان از نونوایی"
     */
    fun extract(tokens: List<Token>): String {
        val contentWords = mutableListOf<String>()

        for (token in tokens) {
            // Skip structural tokens
            if (token.type == TokenType.ACTION) continue
            if (token.type.name.startsWith("TARGET_")) continue
            if (token.type == TokenType.OBJECT_MARKER) continue
            if (token.type == TokenType.FILLER) continue
            if (token.type == TokenType.NEGATION) continue
            if (token.type == TokenType.QUESTION) continue
            if (token.type == TokenType.CONFIRM) continue
            if (token.type == TokenType.CANCEL) continue
            // Values might be content in some contexts (e.g., "شماره ۶۰۳۷")
            // Keep UNKNOWN, CONTENT, and NUMBER_VALUE tokens
            contentWords.add(token.text)
        }

        return contentWords.joinToString(" ").trim()
    }

    /**
     * Extracts content AFTER the last structural token.
     * More precise for commands like "یادداشت کن خرید نان"
     */
    fun extractAfterStructural(tokens: List<Token>): String {
        var lastStructuralIdx = -1
        for (i in tokens.indices) {
            val t = tokens[i]
            if (t.type == TokenType.ACTION || t.type.name.startsWith("TARGET_") ||
                t.type == TokenType.OBJECT_MARKER || t.type == TokenType.FILLER) {
                lastStructuralIdx = i
            }
        }

        if (lastStructuralIdx >= 0 && lastStructuralIdx < tokens.size - 1) {
            return tokens.subList(lastStructuralIdx + 1, tokens.size)
                .joinToString(" ") { it.text }.trim()
        }
        return ""
    }
}
