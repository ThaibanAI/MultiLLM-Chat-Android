package com.thaibanai.multillmchat.data.remote

/**
 * Callback interface for streaming LLM responses.
 */
interface StreamingCallback {
    fun onNewData(text: String)
    fun onComplete(fullText: String)
    fun onError(error: String)
}
