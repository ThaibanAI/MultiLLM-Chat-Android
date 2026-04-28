package com.thaibanai.multillmchat.data.remote.api

import com.thaibanai.multillmchat.data.remote.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun sendMessage(@Body request: OpenAIRequest): Response<OpenAIResponse>

    @Streaming
    @POST("v1/chat/completions")
    suspend fun sendMessageStreaming(@Body request: OpenAIRequest): Response<ResponseBody>
}

interface AnthropicApi {
    @POST("v1/messages")
    suspend fun sendMessage(@Body request: AnthropicRequest): Response<AnthropicResponse>

    @Streaming
    @POST("v1/messages")
    suspend fun sendMessageStreaming(@Body request: AnthropicRequest): Response<ResponseBody>
}

interface DeepSeekApi {
    @POST("chat/completions")
    suspend fun sendMessage(@Body request: DeepSeekRequest): Response<DeepSeekResponse>

    @Streaming
    @POST("chat/completions")
    suspend fun sendMessageStreaming(@Body request: DeepSeekRequest): Response<ResponseBody>
}
