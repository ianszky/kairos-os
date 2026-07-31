package com.kairos.os.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kairos.os.domain.models.ChatSearchMatchKind
import com.kairos.os.domain.models.ChatSearchResult
import com.kairos.os.ui.googleSansFont
import com.kairos.os.ui.viewmodels.ChatViewModel
import kotlinx.coroutines.delay

@Composable
fun SearchChatScreen(
    chatViewModel: ChatViewModel,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by chatViewModel.searchResults.collectAsState()
    val isSearching by chatViewModel.isSearching.collectAsState()

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            chatViewModel.clearSearch()
            return@LaunchedEffect
        }
        delay(300)
        chatViewModel.searchChats(searchQuery)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 80.dp)
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search chats...", fontFamily = googleSansFont) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ),
                singleLine = true
            )

            when {
                searchQuery.isBlank() -> {
                    SearchEmptyState(
                        modifier = Modifier.weight(1f),
                        title = "Search Chat Histories",
                        subtitle = "Search conversation titles or message keywords."
                    )
                }
                isSearching && searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                searchResults.isEmpty() -> {
                    SearchEmptyState(
                        modifier = Modifier.weight(1f),
                        title = "No Matches Found",
                        subtitle = "Try a different search query."
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(searchResults, key = { "${it.conversationId}:${it.matchKind}:${it.messageId ?: "title"}" }) { result ->
                            ChatSearchResultCard(
                                result = result,
                                query = searchQuery,
                                onClick = { onOpenConversation(result.conversationId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyState(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String
) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = googleSansFont),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChatSearchResultCard(
    result: ChatSearchResult,
    query: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = result.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = googleSansFont,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildHighlightedText(
                    text = result.matchedText,
                    query = query,
                    highlightColor = MaterialTheme.colorScheme.primary
                ),
                fontSize = 14.sp,
                fontFamily = googleSansFont,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (result.matchKind == ChatSearchMatchKind.MESSAGE) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildHighlightedText(
    text: String,
    query: String,
    highlightColor: androidx.compose.ui.graphics.Color
): AnnotatedString {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isBlank()) return AnnotatedString(text)

    val lowerText = text.lowercase()
    val lowerQuery = trimmedQuery.lowercase()

    return buildAnnotatedString {
        var start = 0
        while (start < text.length) {
            val matchIndex = lowerText.indexOf(lowerQuery, start)
            if (matchIndex < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, matchIndex))
            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                append(text.substring(matchIndex, matchIndex + trimmedQuery.length))
            }
            start = matchIndex + trimmedQuery.length
        }
    }
}
