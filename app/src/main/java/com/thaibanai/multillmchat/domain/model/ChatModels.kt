package com.thaibanai.multillmchat.domain.model

import com.thaibanai.multillmchat.data.remote.model.LLMProvider

/**
 * Represents a pending attachment in the input area before sending.
 */
data class PendingAttachment(
    val id: String,
    val type: AttachmentType,
    val fileName: String,
    val fileSize: Long,
    val uri: String,
    val mimeType: String,
    val base64Data: String? = null, // for images sent to API
    val extractedText: String? = null, // for documents
    val thumbnailUri: String? = null // for images
)

enum class AttachmentType {
    IMAGE,
    PDF,
    TXT,
    DOCX
}

/**
 * Represents a message in the chat UI, combining data from multiple providers' responses.
 */
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val provider: LLMProvider? = null,
    val model: String? = null,
    val content: String,
    val attachments: List<PendingAttachment> = emptyList(),
    val isStreaming: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val timestamp: Long
)

enum class MessageRole {
    USER,
    ASSISTANT
}

data class ProviderStatus(
    val provider: LLMProvider,
    val isEnabled: Boolean,
    val hasApiKey: Boolean,
    val selectedModel: String,
    val isSelected: Boolean = false
)

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastMessage: String? = null,
    val messageCount: Int = 0
)

/**
 * Result of sending a prompt to a single provider.
 */
data class ProviderResponseResult(
    val provider: LLMProvider,
    val messageId: String,
    val model: String,
    val content: String,
    val isComplete: Boolean,
    val error: String? = null
)

data class ProviderConfigState(
    val provider: LLMProvider,
    val apiKey: String = "",
    val model: String = "",
    val isEnabled: Boolean = true,
    val hasSavedKey: Boolean = false
)
