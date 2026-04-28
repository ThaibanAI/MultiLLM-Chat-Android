package com.thaibanai.multillmchat.util

object Constants {
    const val APP_VERSION = "1.0.0"
    const val GITHUB_URL = "https://github.com/ThaibanAI/MultiLLM-Chat-Android"

    // Suggestion models — updated Apr 2026
    val CLAUDE_MODELS = listOf(
        "claude-opus-4-7",
        "claude-sonnet-4-6",
        "claude-haiku-4-5"
    )

    val OPENAI_MODELS = listOf(
        "gpt-5.5",
        "gpt-5.5-pro",
        "gpt-5.4"
    )

    val DEEPSEEK_MODELS = listOf(
        "deepseek-v4-pro",
        "deepseek-v4-flash"
    )
}
