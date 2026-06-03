package com.nova.assistant.domain.context

import com.nova.assistant.domain.entity.ExtractedEntities
import com.nova.assistant.domain.intent.Intent
import java.time.Instant

data class ConversationContext(
    val lastIntent: Intent? = null,
    val lastEntities: ExtractedEntities? = null,
    val timestamp: Instant = Instant.now()
)
