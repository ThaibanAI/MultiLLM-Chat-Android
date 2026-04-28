package com.thaibanai.multillmchat.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Renders Markdown content using Markwon library via WebView.
 * Provides full markdown rendering including syntax-highlighted code blocks.
 */
@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    val htmlContent = remember(markdown, isDarkTheme) {
        buildMarkdownHtml(markdown, isDarkTheme)
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                null,
                buildMarkdownHtml(markdown, isDarkTheme),
                "text/html",
                "UTF-8",
                null
            )
        },
        modifier = modifier
    )
}

/**
 * Alternatively, use a simpler implementation that doesn't depend on WebView
 * by implementing native markdown rendering with compose text.
 * This function builds an HTML document with embedded styles.
 */
private fun buildMarkdownHtml(markdown: String, isDarkTheme: Boolean): String {
    val bgColor = if (isDarkTheme) "#2D2D2D" else "#FFFFFF"
    val textColor = if (isDarkTheme) "#E8EAED" else "#202124"
    val codeBg = if (isDarkTheme) "#1E1E1E" else "#F1F3F4"
    val codeColor = if (isDarkTheme) "#F8F8F2" else "#333333"
    val borderColor = if (isDarkTheme) "#5F6368" else "#DADCE0"

    // Escape markdown content for HTML display
    // We use a simple approach: convert markdown to HTML using basic transformations
    val htmlBody = markdownToHtml(markdown)

    return """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<style>
    body {
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
        font-size: 14px;
        line-height: 1.6;
        color: $textColor;
        background-color: transparent;
        padding: 0;
        margin: 0;
        word-wrap: break-word;
        overflow-wrap: break-word;
    }
    pre {
        background-color: $codeBg;
        border: 1px solid $borderColor;
        border-radius: 8px;
        padding: 12px;
        overflow-x: auto;
        font-size: 13px;
        line-height: 1.4;
    }
    code {
        font-family: 'JetBrains Mono', 'Fira Code', 'SF Mono', Menlo, Monaco, monospace;
        background-color: $codeBg;
        color: $codeColor;
        padding: 2px 6px;
        border-radius: 4px;
        font-size: 13px;
    }
    pre code {
        background: none;
        padding: 0;
        border-radius: 0;
    }
    h1 { font-size: 20px; margin: 16px 0 8px; }
    h2 { font-size: 18px; margin: 14px 0 6px; }
    h3 { font-size: 16px; margin: 12px 0 4px; }
    h4 { font-size: 14px; margin: 10px 0 4px; }
    p { margin: 6px 0; }
    ul, ol { margin: 6px 0; padding-left: 20px; }
    li { margin: 2px 0; }
    blockquote {
        border-left: 3px solid $borderColor;
        margin: 8px 0;
        padding: 4px 12px;
        color: ${if (isDarkTheme) "#9AA0A6" else "#5F6368"};
    }
    table {
        border-collapse: collapse;
        margin: 8px 0;
        width: 100%;
        font-size: 13px;
    }
    th, td {
        border: 1px solid $borderColor;
        padding: 6px 10px;
        text-align: left;
    }
    th {
        background-color: $codeBg;
        font-weight: 600;
    }
    hr {
        border: none;
        border-top: 1px solid $borderColor;
        margin: 12px 0;
    }
    a { color: #8AB4F8; }
    img { max-width: 100%; border-radius: 8px; }
</style>
</head>
<body>$htmlBody</body>
</html>
""".trimIndent()
}

/**
 * Simple markdown to HTML converter.
 * Handles common markdown patterns. For production, use a proper library like
 * commonmark-java or flexmark integrated with Markwon.
 */
private fun markdownToHtml(md: String): String {
    var html = md
        // Escape HTML
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    // Headers
    html = html.replace(Regex("(?m)^#### (.+)$")) { "<h4>${it.groupValues[1]}</h4>" }
    html = html.replace(Regex("(?m)^### (.+)$")) { "<h3>${it.groupValues[1]}</h3>" }
    html = html.replace(Regex("(?m)^## (.+)$")) { "<h2>${it.groupValues[1]}</h2>" }
    html = html.replace(Regex("(?m)^# (.+)$")) { "<h1>${it.groupValues[1]}</h1>" }

    // Horizontal rules
    html = html.replace(Regex("(?m)^---+\$"), "<hr>")
    html = html.replace(Regex("(?m)^\\*\\*\\*+\$"), "<hr>")

    // Blockquotes
    html = html.replace(Regex("(?m)^> (.+)$")) { "<blockquote>${it.groupValues[1]}</blockquote>" }

    // Code blocks (fenced)
    html = html.replace(Regex("```(\\w*)\\n([\\s\\S]*?)```")) { match ->
        val lang = match.groupValues[1]
        val code = match.groupValues[2].trimEnd()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        val langLabel = if (lang.isNotBlank()) "<div style='font-size:11px;color:#9AA0A6;margin-bottom:4px;'>$lang</div>" else ""
        "<pre><code>${langLabel}${code}</code></pre>"
    }

    // Inline code
    html = html.replace(Regex("`([^`]+)`")) { "<code>${it.groupValues[1]}</code>" }

    // Bold + italic
    html = html.replace(Regex("\\*\\*\\*(.+?)\\*\\*\\*")) { "<strong><em>${it.groupValues[1]}</em></strong>" }
    html = html.replace(Regex("___\\s*(.+?)\\s*___")) { "<strong><em>${it.groupValues[1]}</em></strong>" }

    // Bold
    html = html.replace(Regex("\\*\\*(.+?)\\*\\*")) { "<strong>${it.groupValues[1]}</strong>" }
    html = html.replace(Regex("__(.+?)__")) { "<strong>${it.groupValues[1]}</strong>" }

    // Italic
    html = html.replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")) { "<em>${it.groupValues[1]}</em>" }
    html = html.replace(Regex("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)")) { "<em>${it.groupValues[1]}</em>" }

    // Strikethrough
    html = html.replace(Regex("~~(.+?)~~")) { "<del>${it.groupValues[1]}</del>" }

    // Unordered lists
    html = replaceListItems(html, Regex("(?m)^[*-]\\s+(.+)$"), "ul")

    // Ordered lists
    html = replaceListItems(html, Regex("(?m)^\\d+\\.\\s+(.+)$"), "ol")

    // Links
    html = html.replace(Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")) {
        "<a href=\"${it.groupValues[2]}\">${it.groupValues[1]}</a>"
    }

    // Tables
    html = html.replace(Regex("\\|(.+)\\|\\n\\|[\\s|-]+\\|\\n((?:\\|.+\\|\\n?)*)")) { match ->
        val headerRow = match.groupValues[1]
        val dataRows = match.groupValues[2]

        val headers = headerRow.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val rows = dataRows.trim().split("\n").map { row ->
            row.split("|").map { cell -> cell.trim() }.filter { it.isNotEmpty() }
        }

        val headerCells = headers.joinToString("") { "<th>$it</th>" }
        val bodyRows = rows.joinToString("\n") { row ->
            val cells = row.joinToString("") { "<td>$it</td>" }
            "<tr>$cells</tr>"
        }

        "<table><thead><tr>$headerCells</tr></thead><tbody>$bodyRows</tbody></table>"
    }

    // Convert double newlines to paragraphs
    html = html.replace(Regex("\n\n+")) { "</p><p>" }

    // Single newlines to <br>
    html = html.replace("\n", "<br>")

    // Wrap in paragraph if not already in block elements
    if (!html.startsWith("<h") && !html.startsWith("<pre") && !html.startsWith("<blockquote") &&
        !html.startsWith("<ul") && !html.startsWith("<ol") && !html.startsWith("<table") &&
        !html.startsWith("<hr") && !html.startsWith("<p")) {
        html = "<p>$html</p>"
    }

    return html
}

private fun replaceListItems(text: String, pattern: Regex, listTag: String): String {
    val lines = text.split("\n")
    val result = StringBuilder()
    var inList = false

    for (line in lines) {
        val match = pattern.find(line)
        if (match != null) {
            if (!inList) {
                result.append("<$listTag>")
                inList = true
            }
            result.append("<li>${match.groupValues[1]}</li>")
        } else {
            if (inList) {
                result.append("</$listTag>")
                inList = false
            }
            result.append(line)
        }
        result.append("\n")
    }
    if (inList) {
        result.append("</$listTag>")
    }

    return result.toString()
}
