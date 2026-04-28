package com.thaibanai.multillmchat.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that injects the API key header for the provider's requests.
 * The key is resolved lazily at request time so changes take effect immediately.
 */
class ApiKeyInterceptor(
    private val headerName: String,
    private val authScheme: String = "Bearer",
    private val keyProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val apiKey = keyProvider()

        val request = if (apiKey.isNullOrBlank()) {
            // If no API key, mark the request so the service can detect it
            originalRequest.newBuilder()
                .header("X-API-Key-Missing", "true")
                .build()
        } else {
            originalRequest.newBuilder()
                .header(headerName, "$authScheme $apiKey")
                .build()
        }

        return chain.proceed(request)
    }
}
