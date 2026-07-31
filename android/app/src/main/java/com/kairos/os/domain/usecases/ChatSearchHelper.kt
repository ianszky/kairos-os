package com.kairos.os.domain.usecases

import com.kairos.os.domain.models.ChatSearchMatchKind
import com.kairos.os.domain.models.ChatSearchResult

object ChatSearchHelper {
    private const val SNIPPET_WINDOW = 120

    fun extractMatchSnippet(text: String, query: String): String {
        if (text.isBlank()) return text

        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return text.take(SNIPPET_WINDOW)

        val lowerQuery = trimmedQuery.lowercase()
        val matchingLine = text.lineSequence()
            .firstOrNull { it.lowercase().contains(lowerQuery) }
            ?.trim()

        if (matchingLine != null) return matchingLine

        val lowerText = text.lowercase()
        val matchIndex = lowerText.indexOf(lowerQuery)
        if (matchIndex < 0) return text.take(SNIPPET_WINDOW)

        val start = maxOf(0, matchIndex - 40)
        val end = minOf(text.length, matchIndex + trimmedQuery.length + 40)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        return prefix + text.substring(start, end) + suffix
    }

    fun mergeResults(results: List<ChatSearchResult>, maxResults: Int = 100): List<ChatSearchResult> {
        return results
            .distinctBy { "${it.conversationId}:${it.matchKind}:${it.messageId ?: "title"}" }
            .sortedByDescending { it.sortTimestamp }
            .take(maxResults)
    }

    fun displayTitle(rawTitle: String?): String {
        return rawTitle?.takeIf { it.isNotBlank() } ?: "New Conversation"
    }

    fun toSearchResult(
        conversationId: String,
        title: String?,
        matchedSource: String,
        matchKind: ChatSearchMatchKind,
        messageId: String?,
        sortTimestamp: String,
        query: String
    ): ChatSearchResult {
        val resolvedTitle = displayTitle(title)
        val snippetSource = when (matchKind) {
            ChatSearchMatchKind.TITLE -> resolvedTitle
            ChatSearchMatchKind.MESSAGE -> matchedSource
        }
        return ChatSearchResult(
            conversationId = conversationId,
            title = resolvedTitle,
            matchedText = extractMatchSnippet(snippetSource, query),
            matchKind = matchKind,
            messageId = messageId,
            sortTimestamp = sortTimestamp
        )
    }
}
