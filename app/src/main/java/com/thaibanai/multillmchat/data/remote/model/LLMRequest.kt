package com.thaibanai.multillmchat.data.remote.model

import com.google.gson.annotations.SerializedName

// ========== OpenAI ==========

data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val stream: Boolean = true,
    // For non-reasoning models (gpt-4o, etc.)
    val max_tokens: Int? = null,
    // For reasoning models (gpt-5.x)
    @SerializedName("max_completion_tokens")
    val maxCompletionTokens: Int? = null,
    val temperature: Double? = null,
    @SerializedName("reasoning_effort")
    val reasoningEffort: String? = null
)

data class OpenAIMessage(
    val role: String,
    val content: List<OpenAIContentPart>
)

data class OpenAIContentPart(
    val type: String,
    val text: String? = null,
    @SerializedName("image_url")
    val imageUrl: OpenAIImageUrl? = null
)

data class OpenAIImageUrl(
    val url: String,
    val detail: String = "high"
)

data class OpenAIResponse(
    val id: String? = null,
    val choices: List<OpenAIChoice>? = null,
    val error: OpenAIError? = null
)

data class OpenAIChoice(
    val delta: OpenAIDelta? = null,
    val message: OpenAIMessage? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class OpenAIDelta(
    val role: String? = null,
    val content: String? = null
)

data class OpenAIError(
    val message: String? = null,
    val type: String? = null
)

// ========== Anthropic Claude ==========

data class AnthropicRequest(
    val model: String,
    @SerializedName("max_tokens")
    val maxTokens: Int = 4096,
    val messages: List<AnthropicMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.7
)

data class AnthropicMessage(
    val role: String,
    val content: List<AnthropicContentBlock>
)

data class AnthropicContentBlock(
    val type: String,
    val text: String? = null,
    val source: AnthropicImageSource? = null
)

data class AnthropicImageSource(
    val type: String = "base64",
    @SerializedName("media_type")
    val mediaType: String,
    val data: String
)

data class AnthropicResponse(
    val id: String? = null,
    val type: String? = null,
    val role: String? = null,
    val content: List<AnthropicContentBlock>? = null,
    @SerializedName("stop_reason")
    val stopReason: String? = null,
    val error: AnthropicErrorResponse? = null
)

data class AnthropicErrorResponse(
    val type: String? = null,
    val message: String? = null
)

data class AnthropicStreamEvent(
    val type: String? = null,
    val delta: AnthropicDelta? = null,
    val content_block: AnthropicContentBlock? = null,
    val index: Int? = null,
    val message: AnthropicResponse? = null,
    val error: AnthropicErrorResponse? = null
)

data class AnthropicDelta(
    val text: String? = null,
    @SerializedName("stop_reason")
    val stopReason: String? = null
)

// ========== DeepSeek ==========

data class DeepSeekRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val stream: Boolean = true,
    val max_tokens: Int = 4096,
    val temperature: Double = 0.7
)

data class DeepSeekMessage(
    val role: String,
    val content: String
)

data class DeepSeekResponse(
    val id: String? = null,
    val choices: List<DeepSeekChoice>? = null,
    val error: DeepSeekError? = null
)

data class DeepSeekChoice(
    val delta: DeepSeekDelta? = null,
    val message: DeepSeekMessage? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class DeepSeekDelta(
    val role: String? = null,
    val content: String? = null
)

data class DeepSeekError(
    val message: String? = null,
    val type: String? = null
)

// ========== Provider Models ==========

enum class LLMProvider(val id: String, val displayName: String) {
    CLAUDE("claude", "Claude"),
    OPENAI("openai", "OpenAI"),
    DEEPSEEK("deepseek", "DeepSeek")
}
