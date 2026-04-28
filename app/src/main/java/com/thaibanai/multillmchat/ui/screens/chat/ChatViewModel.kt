package com.thaibanai.multillmchat.ui.screens.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thaibanai.multillmchat.data.local.SecureStorage
import com.thaibanai.multillmchat.data.local.entity.MessageEntity
import com.thaibanai.multillmchat.data.remote.StreamEvent
import com.thaibanai.multillmchat.data.remote.model.LLMProvider
import com.thaibanai.multillmchat.data.repository.ChatRepository
import com.thaibanai.multillmchat.domain.model.*
import com.thaibanai.multillmchat.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val conversationId: String? = null,
    val conversationTitle: String = "New Chat",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val selectedProviders: Set<LLMProvider> = emptySet(),
    val providerStatuses: List<ProviderStatus> = emptyList(),
    val pendingAttachments: List<PendingAttachment> = emptyList(),
    val isLoading: Boolean = false,
    val streamingMessages: Map<String, String> = emptyMap(), // messageId -> current text
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val repository: ChatRepository,
    private val secureStorage: SecureStorage
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJobs = mutableMapOf<String, Job>()

    fun initialize(conversationId: String?) {
        if (conversationId == null || conversationId == "new") {
            viewModelScope.launch {
                val newId = repository.createNewConversation("New Chat")
                _uiState.update { it.copy(conversationId = newId, conversationTitle = "New Chat") }
                loadProviderStatuses()
            }
        } else {
            _uiState.update { it.copy(conversationId = conversationId) }
            loadMessages(conversationId)
            loadProviderStatuses()
        }
    }

    private fun loadMessages(conversationId: String) {
        repository.getMessages(conversationId).onEach { entities ->
            val messages = entities.map { it.toChatMessage() }
            _uiState.update { state ->
                state.copy(
                    messages = messages,
                    conversationTitle = state.conversationTitle.takeIf { it.isNotBlank() }
                        ?: entities.firstOrNull()?.let { it.content.take(40) } ?: "New Chat"
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun loadProviderStatuses() {
        val statuses = repository.getAllProviderStatuses()
        _uiState.update { state ->
            state.copy(
                providerStatuses = statuses,
                selectedProviders = statuses.filter { it.isEnabled }.map { it.provider }.toSet()
            )
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun toggleProvider(provider: LLMProvider) {
        _uiState.update { state ->
            val updated = state.selectedProviders.toMutableSet()
            if (updated.contains(provider)) {
                updated.remove(provider)
            } else {
                updated.add(provider)
            }
            state.copy(selectedProviders = updated)
        }
    }

    fun addImageAttachment(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = FileUtils.getFileName(context, uri)
            val fileSize = FileUtils.getFileSize(context, uri)
            val base64 = FileUtils.imageUriToBase64(context, uri)

            val attachment = PendingAttachment(
                id = UUID.randomUUID().toString(),
                type = AttachmentType.IMAGE,
                fileName = fileName,
                fileSize = fileSize,
                uri = uri.toString(),
                mimeType = context.contentResolver.getType(uri) ?: "image/jpeg",
                base64Data = base64,
                thumbnailUri = uri.toString()
            )

            _uiState.update { state ->
                state.copy(pendingAttachments = state.pendingAttachments + attachment)
            }
        }
    }

    fun addDocumentAttachment(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = FileUtils.getFileName(context, uri)
            val fileSize = FileUtils.getFileSize(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

            val attachmentType = when {
                mimeType.contains("pdf") -> AttachmentType.PDF
                mimeType.contains("text") || fileName.endsWith(".txt") -> AttachmentType.TXT
                fileName.endsWith(".docx") || mimeType.contains("document") -> AttachmentType.DOCX
                else -> AttachmentType.TXT
            }

            val extractedText = when (attachmentType) {
                AttachmentType.PDF -> FileUtils.extractPdfText(context, uri)
                AttachmentType.TXT -> FileUtils.readText(context, uri)
                AttachmentType.DOCX -> extractDocxText(context, uri)
                else -> null
            }

            val attachment = PendingAttachment(
                id = UUID.randomUUID().toString(),
                type = attachmentType,
                fileName = fileName,
                fileSize = fileSize,
                uri = uri.toString(),
                mimeType = mimeType,
                extractedText = extractedText
            )

            _uiState.update { state ->
                state.copy(pendingAttachments = state.pendingAttachments + attachment)
            }
        }
    }

    private fun extractDocxText(context: android.content.Context, uri: Uri): String? {
        // Basic DOCX text extraction (read raw XML from the zip)
        return try {
            val bytes = FileUtils.readBytes(context, uri) ?: return null
            val zipInputStream = java.util.zip.ZipInputStream(bytes.inputStream())
            var text = StringBuilder()
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xml = zipInputStream.readBytes().toString(Charsets.UTF_8)
                    // Strip XML tags
                    text.append(xml.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim())
                }
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
            text.toString().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    fun removeAttachment(attachmentId: String) {
        _uiState.update { state ->
            state.copy(pendingAttachments = state.pendingAttachments.filter { it.id != attachmentId })
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        val conversationId = state.conversationId ?: return
        val text = state.inputText.trim()
        val selectedProviders = state.selectedProviders.toList()

        if (text.isBlank() && state.pendingAttachments.isEmpty()) return
        if (selectedProviders.isEmpty()) {
            _uiState.update { it.copy(error = "Select at least one provider") }
            return
        }

        _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

        viewModelScope.launch {
            // Save user message
            val userMessage = repository.saveUserMessage(conversationId, text, state.pendingAttachments)
            val attachments = state.pendingAttachments.toList()

            // Get existing messages for context
            val existingMessages = repository.getMessages(conversationId).first()

            // For each selected provider, create an assistant message and start streaming
            for ((index, provider) in selectedProviders.withIndex()) {
                val model = when (provider) {
                    LLMProvider.CLAUDE -> secureStorage.getAnthropicModel()
                    LLMProvider.OPENAI -> secureStorage.getOpenAiModel()
                    LLMProvider.DEEPSEEK -> secureStorage.getDeepSeekModel()
                }

                val assistantMsg = repository.createAssistantMessage(
                    conversationId = conversationId,
                    provider = provider,
                    model = model,
                    orderIndex = (repository.getMessages(conversationId).first().size + index)
                )

                val apiKey = when (provider) {
                    LLMProvider.CLAUDE -> secureStorage.getAnthropicApiKey()
                    LLMProvider.OPENAI -> secureStorage.getOpenAiApiKey()
                    LLMProvider.DEEPSEEK -> secureStorage.getDeepSeekApiKey()
                }

                if (apiKey.isNullOrBlank()) {
                    repository.failMessage(assistantMsg.id, "API key not configured")
                    continue
                }

                // Check vision support for image attachments
                val filteredAttachments = if (provider == LLMProvider.DEEPSEEK) {
                    // DeepSeek doesn't support vision
                    attachments.filter { it.type != AttachmentType.IMAGE }
                } else {
                    attachments
                }

                // Start streaming
                val streamJob = launch(Dispatchers.IO) {
                    buildString {
                        repository.sendToProvider(provider, text, existingMessages, filteredAttachments)
                            .catch { e ->
                                repository.failMessage(assistantMsg.id, e.message ?: "Unknown error")
                            }
                            .collect { event ->
                                when (event) {
                                    is StreamEvent.Delta -> {
                                        append(event.text)
                                        repository.updateStreamingMessage(assistantMsg.id, toString())
                                    }
                                    is StreamEvent.Complete -> {
                                        append(event.fullText)
                                        repository.completeMessage(assistantMsg.id, toString())
                                    }
                                    is StreamEvent.Error -> {
                                        repository.failMessage(assistantMsg.id, event.message)
                                    }
                                }
                            }
                    }
                }
                streamJobs[assistantMsg.id] = streamJob
            }

            // Clear pending attachments after sending
            _uiState.update { it.copy(pendingAttachments = emptyList(), isLoading = false) }
        }
    }

    fun resendToProviders(
        messageContent: String,
        providers: List<LLMProvider>,
        existingMessages: List<MessageEntity>
    ) {
        val conversationId = _uiState.value.conversationId ?: return

        for (provider in providers) {
            viewModelScope.launch {
                val model = when (provider) {
                    LLMProvider.CLAUDE -> secureStorage.getAnthropicModel()
                    LLMProvider.OPENAI -> secureStorage.getOpenAiModel()
                    LLMProvider.DEEPSEEK -> secureStorage.getDeepSeekModel()
                }

                val assistantMsg = repository.createAssistantMessage(
                    conversationId = conversationId,
                    provider = provider,
                    model = model,
                    orderIndex = (repository.getMessages(conversationId).first().size)
                )

                val apiKey = when (provider) {
                    LLMProvider.CLAUDE -> secureStorage.getAnthropicApiKey()
                    LLMProvider.OPENAI -> secureStorage.getOpenAiApiKey()
                    LLMProvider.DEEPSEEK -> secureStorage.getDeepSeekApiKey()
                }

                if (apiKey.isNullOrBlank()) {
                    repository.failMessage(assistantMsg.id, "API key not configured")
                    return@launch
                }

                launch(Dispatchers.IO) {
                    buildString {
                        repository.sendToProvider(provider, messageContent, existingMessages, emptyList())
                            .catch { e ->
                                repository.failMessage(assistantMsg.id, e.message ?: "Unknown error")
                            }
                            .collect { event ->
                                when (event) {
                                    is StreamEvent.Delta -> {
                                        append(event.text)
                                        repository.updateStreamingMessage(assistantMsg.id, toString())
                                    }
                                    is StreamEvent.Complete -> {
                                        append(event.fullText)
                                        repository.completeMessage(assistantMsg.id, toString())
                                    }
                                    is StreamEvent.Error -> {
                                        repository.failMessage(assistantMsg.id, event.message)
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun clearConversation() {
        val conversationId = _uiState.value.conversationId ?: return
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            val newId = repository.createNewConversation("New Chat")
            _uiState.update { it.copy(conversationId = newId, messages = emptyList()) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        streamJobs.values.forEach { it.cancel() }
        streamJobs.clear()
    }

    private fun MessageEntity.toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            role = if (role == "user") MessageRole.USER else MessageRole.ASSISTANT,
            provider = provider?.let { p ->
                try { LLMProvider.valueOf(p.uppercase()) } catch (e: Exception) { null }
            },
            model = model,
            content = content,
            isStreaming = isStreaming,
            isLoading = isLoading,
            error = error,
            timestamp = createdAt
        )
    }
}
