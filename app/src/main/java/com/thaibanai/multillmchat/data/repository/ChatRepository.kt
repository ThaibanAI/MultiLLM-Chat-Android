package com.thaibanai.multillmchat.data.repository

import com.thaibanai.multillmchat.data.local.SecureStorage
import com.thaibanai.multillmchat.data.local.dao.ConversationDao
import com.thaibanai.multillmchat.data.local.dao.MessageDao
import com.thaibanai.multillmchat.data.local.entity.ConversationEntity
import com.thaibanai.multillmchat.data.local.entity.MessageEntity
import com.thaibanai.multillmchat.data.remote.StreamEvent
import com.thaibanai.multillmchat.data.remote.StreamingService
import com.thaibanai.multillmchat.data.remote.model.*
import com.thaibanai.multillmchat.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val streamingService: StreamingService,
    private val secureStorage: SecureStorage
) {

    // ========== Conversations ==========

    fun getAllConversations(): Flow<List<Conversation>> = conversationDao.getAllConversations()
        .map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun createNewConversation(title: String = "New Chat"): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val entity = ConversationEntity(
            id = id,
            title = title,
            createdAt = now,
            updatedAt = now
        )
        conversationDao.insert(entity)
        return id
    }

    suspend fun renameConversation(id: String, title: String) {
        conversationDao.updateTitle(id, title, System.currentTimeMillis())
    }

    suspend fun deleteConversation(id: String) {
        conversationDao.deleteById(id)
    }

    suspend fun deleteAllConversations() {
        conversationDao.deleteAll()
    }

    // ========== Messages ==========

    fun getMessages(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesByConversation(conversationId)

    suspend fun saveUserMessage(
        conversationId: String,
        content: String,
        attachments: List<PendingAttachment> = emptyList()
    ): MessageEntity {
        val now = System.currentTimeMillis()
        val orderIndex = (messageDao.getMaxOrderIndex(conversationId) ?: -1) + 1
        val entity = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "user",
            content = content,
            attachmentsJson = if (attachments.isNotEmpty()) {
                com.google.gson.Gson().toJson(attachments.map { it.fileName })
            } else null,
            createdAt = now,
            orderIndex = orderIndex
        )
        messageDao.insert(entity)
        conversationDao.touchTimestamp(conversationId, now)

        // Update conversation title from first message
        if (orderIndex == 0) {
            val title = content.take(50).trim().let {
                if (it.length >= 50) "$it…" else it
            }
            conversationDao.updateTitle(conversationId, title, now)
        }

        return entity
    }

    suspend fun createAssistantMessage(
        conversationId: String,
        provider: LLMProvider,
        model: String,
        orderIndex: Int
    ): MessageEntity {
        val now = System.currentTimeMillis()
        val entity = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "assistant",
            provider = provider.id,
            model = model,
            content = "",
            isLoading = true,
            isStreaming = false,
            createdAt = now,
            orderIndex = orderIndex
        )
        messageDao.insert(entity)
        return entity
    }

    suspend fun updateStreamingMessage(messageId: String, content: String) {
        messageDao.updateMessageContent(messageId, content, isStreaming = true, isLoading = false, error = null)
    }

    suspend fun completeMessage(messageId: String, content: String) {
        messageDao.updateMessageContent(messageId, content, isStreaming = false, isLoading = false, error = null)
    }

    suspend fun failMessage(messageId: String, error: String) {
        messageDao.updateMessageContent(messageId, "", isStreaming = false, isLoading = false, error = error)
    }

    suspend fun deleteMessage(messageId: String) {
        messageDao.deleteById(messageId)
    }

    // ========== Provider Config ==========

    fun getProviderConfig(provider: LLMProvider): ProviderConfigState {
        return when (provider) {
            LLMProvider.CLAUDE -> ProviderConfigState(
                provider = provider,
                apiKey = secureStorage.getAnthropicApiKey() ?: "",
                model = secureStorage.getAnthropicModel(),
                isEnabled = secureStorage.isAnthropicEnabled(),
                hasSavedKey = !secureStorage.getAnthropicApiKey().isNullOrBlank()
            )
            LLMProvider.OPENAI -> ProviderConfigState(
                provider = provider,
                apiKey = secureStorage.getOpenAiApiKey() ?: "",
                model = secureStorage.getOpenAiModel(),
                isEnabled = secureStorage.isOpenAiEnabled(),
                hasSavedKey = !secureStorage.getOpenAiApiKey().isNullOrBlank()
            )
            LLMProvider.DEEPSEEK -> ProviderConfigState(
                provider = provider,
                apiKey = secureStorage.getDeepSeekApiKey() ?: "",
                model = secureStorage.getDeepSeekModel(),
                isEnabled = secureStorage.isDeepSeekEnabled(),
                hasSavedKey = !secureStorage.getDeepSeekApiKey().isNullOrBlank()
            )
        }
    }

    fun getAllProviderStatuses(): List<ProviderStatus> {
        return LLMProvider.entries.map { provider ->
            val config = getProviderConfig(provider)
            ProviderStatus(
                provider = provider,
                isEnabled = config.isEnabled && config.hasSavedKey,
                hasApiKey = config.hasSavedKey,
                selectedModel = config.model,
                isSelected = false
            )
        }
    }

    fun saveProviderConfig(config: ProviderConfigState) {
        when (config.provider) {
            LLMProvider.CLAUDE -> {
                secureStorage.setAnthropicApiKey(config.apiKey.trim())
                secureStorage.setAnthropicModel(config.model.trim())
                secureStorage.setAnthropicEnabled(config.isEnabled)
            }
            LLMProvider.OPENAI -> {
                secureStorage.setOpenAiApiKey(config.apiKey.trim())
                secureStorage.setOpenAiModel(config.model.trim())
                secureStorage.setOpenAiEnabled(config.isEnabled)
            }
            LLMProvider.DEEPSEEK -> {
                secureStorage.setDeepSeekApiKey(config.apiKey.trim())
                secureStorage.setDeepSeekModel(config.model.trim())
                secureStorage.setDeepSeekEnabled(config.isEnabled)
            }
        }
    }

    // ========== Streaming Send ==========

    fun sendToProvider(
        provider: LLMProvider,
        prompt: String,
        conversationHistory: List<MessageEntity>,
        attachments: List<PendingAttachment> = emptyList()
    ): Flow<StreamEvent> {
        val apiKey = when (provider) {
            LLMProvider.CLAUDE -> secureStorage.getAnthropicApiKey()?.trim()
            LLMProvider.OPENAI -> secureStorage.getOpenAiApiKey()?.trim()
            LLMProvider.DEEPSEEK -> secureStorage.getDeepSeekApiKey()?.trim()
        }

        if (apiKey.isNullOrBlank()) {
            return flowOf(StreamEvent.Error("API key not configured for ${provider.displayName}"))
        }

        val model = when (provider) {
            LLMProvider.CLAUDE -> secureStorage.getAnthropicModel()
            LLMProvider.OPENAI -> secureStorage.getOpenAiModel()
            LLMProvider.DEEPSEEK -> secureStorage.getDeepSeekModel()
        }

        return when (provider) {
            LLMProvider.CLAUDE -> {
                val request = buildAnthropicRequest(model, prompt, conversationHistory, attachments, apiKey)
                streamingService.streamAnthropic(request, apiKey)
            }
            LLMProvider.OPENAI -> {
                val request = buildOpenAIRequest(model, prompt, conversationHistory, attachments)
                streamingService.streamOpenAI(request, apiKey)
            }
            LLMProvider.DEEPSEEK -> {
                val request = buildDeepSeekRequest(model, prompt, conversationHistory)
                streamingService.streamDeepSeek(request, apiKey)
            }
        }
    }

    private fun buildAnthropicRequest(
        model: String,
        prompt: String,
        history: List<MessageEntity>,
        attachments: List<PendingAttachment>,
        apiKey: String
    ): AnthropicRequest {
        val messages = mutableListOf<AnthropicMessage>()

        // Add history
        for (msg in history) {
            when (msg.role) {
                "user" -> {
                    val content = mutableListOf(
                        AnthropicContentBlock(type = "text", text = msg.content)
                    )
                    messages.add(AnthropicMessage(role = "user", content = content))
                }
                "assistant" -> {
                    val content = listOf(
                        AnthropicContentBlock(type = "text", text = msg.content)
                    )
                    messages.add(AnthropicMessage(role = "assistant", content = content))
                }
            }
        }

        // Add current prompt with attachments
        val currentContent = mutableListOf<AnthropicContentBlock>()
        currentContent.add(AnthropicContentBlock(type = "text", text = prompt))

        // Add image attachments (Claude supports vision)
        val imageAttachments = attachments.filter { it.type == AttachmentType.IMAGE && !it.base64Data.isNullOrBlank() }
        for (img in imageAttachments) {
            val mediaType = when {
                img.mimeType.contains("png") -> "image/png"
                img.mimeType.contains("webp") -> "image/webp"
                else -> "image/jpeg"
            }
            currentContent.add(
                AnthropicContentBlock(
                    type = "image",
                    source = AnthropicImageSource(
                        mediaType = mediaType,
                        data = img.base64Data!!
                    )
                )
            )
        }

        messages.add(AnthropicMessage(role = "user", content = currentContent))

        return AnthropicRequest(
            model = model,
            messages = messages,
            stream = true
        )
    }

    private fun buildOpenAIRequest(
        model: String,
        prompt: String,
        history: List<MessageEntity>,
        attachments: List<PendingAttachment>
    ): OpenAIRequest {
        val messages = mutableListOf<OpenAIMessage>()

        // Add history - skip messages with provider info (we're building fresh)
        for (msg in history) {
            when (msg.role) {
                "user" -> {
                    messages.add(
                        OpenAIMessage(
                            role = "user",
                            content = listOf(
                                OpenAIContentPart(type = "text", text = msg.content)
                            )
                        )
                    )
                }
                "assistant" -> {
                    messages.add(
                        OpenAIMessage(
                            role = "assistant",
                            content = listOf(
                                OpenAIContentPart(type = "text", text = msg.content)
                            )
                        )
                    )
                }
            }
        }

        // Add current prompt with attachments
        val currentContent = mutableListOf<OpenAIContentPart>()
        currentContent.add(OpenAIContentPart(type = "text", text = prompt))

        // Add image attachments (GPT-4o supports vision)
        val imageAttachments = attachments.filter { it.type == AttachmentType.IMAGE && !it.base64Data.isNullOrBlank() }
        for (img in imageAttachments) {
            val mediaType = when {
                img.mimeType.contains("png") -> "image/png"
                img.mimeType.contains("webp") -> "image/webp"
                else -> "image/jpeg"
            }
            currentContent.add(
                OpenAIContentPart(
                    type = "image_url",
                    imageUrl = OpenAIImageUrl(
                        url = "data:$mediaType;base64,${img.base64Data}",
                        detail = "high"
                    )
                )
            )
        }

        messages.add(OpenAIMessage(role = "user", content = currentContent))

        // Add document text as a separate system note if present
        val docAttachments = attachments.filter {
            it.type != AttachmentType.IMAGE && !it.extractedText.isNullOrBlank()
        }
        if (docAttachments.isNotEmpty()) {
            val docText = docAttachments.joinToString("\n\n---\n\n") {
                "**Document: ${it.fileName}**\n\n${it.extractedText}"
            }
            messages.add(
                OpenAIMessage(
                    role = "user",
                    content = listOf(
                        OpenAIContentPart(
                            type = "text",
                            text = "[Attached document content]\n\n$docText"
                        )
                    )
                )
            )
        }

        // Reasoning models (GPT-5.x) require reasoning_effort and max_completion_tokens,
        // NOT max_tokens or temperature
        val isReasoningModel = model.startsWith("gpt-5")

        return OpenAIRequest(
            model = model,
            messages = messages,
            stream = true,
            max_tokens = if (isReasoningModel) null else 4096,
            maxCompletionTokens = if (isReasoningModel) 4096 else null,
            temperature = if (isReasoningModel) null else 0.7,
            reasoningEffort = if (isReasoningModel) "medium" else null
        )
    }

    private fun buildDeepSeekRequest(
        model: String,
        prompt: String,
        history: List<MessageEntity>
    ): DeepSeekRequest {
        val messages = mutableListOf<DeepSeekMessage>()

        for (msg in history) {
            // DeepSeek only supports text
            if (msg.role == "user" || msg.role == "assistant") {
                messages.add(DeepSeekMessage(role = msg.role, content = msg.content))
            }
        }

        // Add current prompt
        messages.add(DeepSeekMessage(role = "user", content = prompt))

        return DeepSeekRequest(
            model = model,
            messages = messages,
            stream = true
        )
    }

    fun getThemeMode(): String = secureStorage.getThemeMode()
    fun setThemeMode(mode: String) = secureStorage.setThemeMode(mode)

    companion object {
        private fun ConversationEntity.toDomain() = Conversation(
            id = id,
            title = title,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
