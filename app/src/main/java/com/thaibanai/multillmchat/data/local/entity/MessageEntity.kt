package com.thaibanai.multillmchat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val role: String, // "user" or "assistant"
    val provider: String? = null, // "claude", "openai", "deepseek", null for user messages
    val model: String? = null, // model name used, null for user messages
    val content: String,
    val attachmentsJson: String? = null, // JSON array of attachment metadata
    val isStreaming: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdAt: Long,
    val orderIndex: Int = 0 // ordering within a conversation
)
