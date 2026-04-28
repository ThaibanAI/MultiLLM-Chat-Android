package com.thaibanai.multillmchat.data.remote

import com.google.gson.Gson
import com.thaibanai.multillmchat.data.remote.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

sealed class StreamEvent {
    data class Delta(val text: String) : StreamEvent()
    data class Complete(val fullText: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}

@Singleton
class StreamingService @Inject constructor(
    private val gson: Gson,
    @Named("openai") private val openAiClient: OkHttpClient,
    @Named("anthropic") private val anthropicClient: OkHttpClient,
    @Named("deepseek") private val deepSeekClient: OkHttpClient
) {

    companion object {
        private const val ANTHROPIC_URL = "https://api.anthropic.com/v1/messages"
        private const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"
        private const val DEEPSEEK_URL = "https://api.deepseek.com/chat/completions"
        private const val ANTHROPIC_VERSION = "2023-06-01"

        private fun parseErrorBody(response: Response): String {
            val bodyStr = response.body?.string()
            if (!bodyStr.isNullOrBlank()) {
                return try {
                    val map = Gson().fromJson(bodyStr, Map::class.java)
                    val err = map["error"]
                    when (err) {
                        is Map<*, *> -> {
                            val msg = err["message"] ?: err["type"] ?: err
                            "HTTP ${response.code}: $msg"
                        }
                        is String -> "HTTP ${response.code}: $err"
                        else -> "HTTP ${response.code}: $bodyStr"
                    }
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
            }
            return "HTTP ${response.code}: ${response.message}"
        }
    }

    fun streamOpenAI(request: OpenAIRequest, apiKey: String): Flow<StreamEvent> = callbackFlow {
        val json = gson.toJson(request)
        val body = json.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(OPENAI_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        val fullText = StringBuilder()

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data == "[DONE]") {
                    trySend(StreamEvent.Complete(fullText.toString()))
                    return
                }
                try {
                    val response = gson.fromJson(data, OpenAIResponse::class.java)
                    if (response.error != null) {
                        trySend(StreamEvent.Error("HTTP 400: ${response.error.message}"))
                        channel.close()
                        return
                    }
                    val delta = response.choices?.firstOrNull()?.delta?.content
                    if (!delta.isNullOrBlank()) {
                        fullText.append(delta)
                        trySend(StreamEvent.Delta(delta))
                    }
                } catch (e: Exception) {
                    // Skip malformed SSE lines
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                val errorMsg = when {
                    t != null -> "Network error: ${t.message}"
                    response != null -> parseErrorBody(response)
                    else -> "Unknown error"
                }
                trySend(StreamEvent.Error(errorMsg))
                channel.close()
            }

            override fun onClosed(eventSource: EventSource) {
                if (fullText.isNotEmpty()) {
                    trySend(StreamEvent.Complete(fullText.toString()))
                }
                channel.close()
            }

            override fun onOpen(eventSource: EventSource, response: Response) {
                // Connection established
            }
        }

        EventSources.createFactory(openAiClient).newEventSource(httpRequest, listener)

        awaitClose {
            // Cleanup handled by OkHttp
        }
    }

    fun streamAnthropic(request: AnthropicRequest, apiKey: String): Flow<StreamEvent> = callbackFlow {
        val json = gson.toJson(request)
        val body = json.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(ANTHROPIC_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", ANTHROPIC_VERSION)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val fullText = StringBuilder()

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                try {
                    val event = gson.fromJson(data, AnthropicStreamEvent::class.java)
                    when (event.type) {
                        "content_block_delta" -> {
                            val text = event.delta?.text
                            if (!text.isNullOrBlank()) {
                                fullText.append(text)
                                trySend(StreamEvent.Delta(text))
                            }
                        }
                        "message_delta" -> {
                            // Might contain stop_reason, no text delta
                        }
                        "message_stop" -> {
                            trySend(StreamEvent.Complete(fullText.toString()))
                        }
                        "error" -> {
                            val errorMsg = event.message?.content?.firstOrNull()?.text
                                ?: event.error?.message ?: "API error"
                            trySend(StreamEvent.Error(errorMsg))
                        }
                        "ping" -> {
                            // Keepalive - ignore
                        }
                    }
                } catch (e: Exception) {
                    // Skip malformed events
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                val errorMsg = when {
                    t != null -> "Network error: ${t.message}"
                    response != null -> {
                        val bodyStr = response.body?.string()
                        if (!bodyStr.isNullOrBlank()) {
                            try {
                                val errResponse = gson.fromJson(bodyStr, AnthropicResponse::class.java)
                                errResponse.error?.message ?: "HTTP ${response.code}"
                            } catch (e: Exception) {
                                "HTTP ${response.code}: $bodyStr"
                            }
                        } else {
                            "HTTP ${response.code}: ${response.message}"
                        }
                    }
                    else -> "Unknown error"
                }
                trySend(StreamEvent.Error(errorMsg))
                channel.close()
            }

            override fun onClosed(eventSource: EventSource) {
                if (fullText.isNotEmpty()) {
                    trySend(StreamEvent.Complete(fullText.toString()))
                }
                channel.close()
            }
        }

        EventSources.createFactory(anthropicClient).newEventSource(httpRequest, listener)

        awaitClose { }
    }

    fun streamDeepSeek(request: DeepSeekRequest, apiKey: String): Flow<StreamEvent> = callbackFlow {
        val json = gson.toJson(request)
        val body = json.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(DEEPSEEK_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        val fullText = StringBuilder()

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data == "[DONE]") {
                    trySend(StreamEvent.Complete(fullText.toString()))
                    return
                }
                try {
                    val response = gson.fromJson(data, DeepSeekResponse::class.java)
                    if (response.error != null) {
                        trySend(StreamEvent.Error("API error: ${response.error.message}"))
                        channel.close()
                        return
                    }
                    val delta = response.choices?.firstOrNull()?.delta?.content
                    if (!delta.isNullOrBlank()) {
                        fullText.append(delta)
                        trySend(StreamEvent.Delta(delta))
                    }
                } catch (e: Exception) {
                    // Skip malformed
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                val errorMsg = when {
                    t != null -> "Network error: ${t.message}"
                    response != null -> parseErrorBody(response)
                    else -> "Unknown error"
                }
                trySend(StreamEvent.Error(errorMsg))
                channel.close()
            }

            override fun onClosed(eventSource: EventSource) {
                if (fullText.isNotEmpty()) {
                    trySend(StreamEvent.Complete(fullText.toString()))
                }
                channel.close()
            }
        }

        EventSources.createFactory(deepSeekClient).newEventSource(httpRequest, listener)

        awaitClose { }
    }
}
