package com.thaibanai.multillmchat.util

object Constants {
    const val APP_VERSION = "1.0.0"
    const val GITHUB_URL = "https://github.com/ThaibanAI/MultiLLM-Chat-Android"

    // Suggestion models
    val CLAUDE_MODELS = listOf(
        "claude-sonnet-4-20250514",
        "claude-3-5-sonnet-20241022",
        "claude-3-5-haiku-20241022",
        "claude-3-opus-20240229"
    )

    val OPENAI_MODELS = listOf(
        "gpt-4o",
        "gpt-4o-mini",
        "gpt-4-turbo",
        "gpt-3.5-turbo"
    )

    val DEEPSEEK_MODELS = listOf(
        "deepseek-chat",
        "deepseek-reasoner"
    )
}
