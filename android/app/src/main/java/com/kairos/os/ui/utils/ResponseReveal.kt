package com.kairos.os.ui.utils

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.kairos.os.domain.models.Interaction
import com.kairos.os.domain.models.KairosResponse
import kotlinx.coroutines.delay

private fun splitIntoChunks(text: String): List<String> {
    if (text.isBlank()) return emptyList()

    val sentenceChunks = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
    if (sentenceChunks.size > 1) return sentenceChunks

    val lineChunks = text.split("\n").filter { it.isNotBlank() }
    if (lineChunks.size > 1) return lineChunks

    val words = text.split(Regex("\\s+"))
    if (words.size <= 8) return listOf(text)
    return words.chunked(8).map { it.joinToString(" ") }
}

suspend fun revealAssistantResponse(
    interactions: SnapshotStateList<Interaction>,
    response: KairosResponse,
    chunkDelayMs: Long = 35L
) {
    interactions.removeAll { it is Interaction.Loading }

    val fullText = response.text?.trim().orEmpty()
    val shouldSkipStagger = fullText.isBlank() ||
        response.type == "ERROR" ||
        (fullText.length < 40 && response.widget == null)

    if (shouldSkipStagger) {
        interactions.add(Interaction.AssistantResponse(response))
        return
    }

    val chunks = splitIntoChunks(fullText)
    if (chunks.isEmpty()) {
        interactions.add(Interaction.AssistantResponse(response))
        return
    }

    val modelName = response.meta?.model
    var accumulated = ""

    chunks.forEachIndexed { index, chunk ->
        accumulated = if (accumulated.isEmpty()) chunk else "$accumulated $chunk".trim()
        val isLast = index == chunks.lastIndex

        val streamingIndex = interactions.indexOfLast { it is Interaction.StreamingResponse }
        val streaming = Interaction.StreamingResponse(
            text = accumulated,
            modelName = modelName,
            isComplete = isLast
        )
        if (streamingIndex >= 0) {
            interactions[streamingIndex] = streaming
        } else {
            interactions.add(streaming)
        }

        if (!isLast) delay(chunkDelayMs)
    }

    interactions.removeAll { it is Interaction.StreamingResponse }
    interactions.add(Interaction.AssistantResponse(response))
}

fun cloudStatusFromPrompt(prompt: String): String? {
    val lower = prompt.lowercase()
    return when {
        lower.contains("@gmail") -> "Checking Gmail…"
        lower.contains("@googlecalendar") -> "Checking Calendar…"
        lower.contains("@spotify") -> "Checking Spotify…"
        lower.contains("@github") -> "Checking GitHub…"
        lower.contains("@notion") -> "Checking Notion…"
        lower.contains("@googlesheets") -> "Checking Sheets…"
        lower.contains("@googledrive") -> "Checking Drive…"
        lower.contains("@slack") -> "Checking Slack…"
        else -> null
    }
}
