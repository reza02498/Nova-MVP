package com.nova.assistant.domain.nlu.entity

import com.nova.assistant.domain.nlu.tokenizer.Token
import com.nova.assistant.domain.nlu.tokenizer.TokenLexicon

object NumberExtractor {

    fun extract(tokens: List<Token>): Int? {
        for (token in tokens) {
            // Digit: "3", "15"
            token.normalized.toIntOrNull()?.let { return it }
            // Word: "سه", "پنجم"
            TokenLexicon.WORD_NUMBERS[token.normalized]?.let { return it }
        }
        return null
    }
}
