package com.kairos.os.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.os.data.db.LocalConversationDao
import com.kairos.os.data.db.LocalMessageDao
import com.kairos.os.domain.models.ChatMessage
import com.kairos.os.domain.models.ChatSearchMatchKind
import com.kairos.os.domain.models.ChatSearchResult
import com.kairos.os.domain.models.Conversation
import com.kairos.os.domain.models.WidgetPayload
import com.kairos.os.domain.usecases.ChatSearchHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val TAG = "ChatViewModel"

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val localConversationDao: LocalConversationDao,
    private val localMessageDao: LocalMessageDao
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessage>> = _currentMessages.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ChatSearchResult>>(emptyList())
    val searchResults: StateFlow<List<ChatSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var realtimeJob: Job? = null
    private var localFlowJob: Job? = null
    private var searchJob: Job? = null

    companion object {
        private const val SEARCH_RESULT_LIMIT = 50
    }

    /**
     * 1. Fetch & merge both cloud (Supabase) and local (Room DB) conversations.
     * 2. Listen to local Room updates and cloud Supabase Realtime changes.
     */
    fun loadConversations() {
        // Listen to local DB changes in real-time
        localFlowJob?.cancel()
        localFlowJob = viewModelScope.launch {
            localConversationDao.getAllConversationsFlow().collectLatest {
                refreshConversations()
            }
        }

        // Initial fetch
        refreshConversations()

        // Realtime subscription for cloud updates
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            try {
                val channel = supabaseClient.channel("conversations-changes")
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "conversations"
                }
                channel.subscribe()
                Log.d(TAG, "loadConversations: realtime subscribed")
                changeFlow.collect { action ->
                    Log.d(TAG, "loadConversations: realtime event: ${action::class.simpleName}")
                    refreshConversations()
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadConversations: realtime subscription failed", e)
            }
        }
    }

    /**
     * Called after a prompt response.
     * Sets current conversationId and refreshes the unified sidebar list.
     */
    fun onPromptResponse(responseConversationId: String?) {
        if (responseConversationId != null && _currentConversationId.value == null) {
            _currentConversationId.value = responseConversationId
        }
        refreshConversations()
    }

    /**
     * Reset state for a brand new conversation (e.g. when user presses Back).
     */
    fun startNewConversation() {
        _currentConversationId.value = null
        _currentMessages.value = emptyList()
    }

    fun selectConversation(conversationId: String?) {
        _currentConversationId.value = conversationId
        if (conversationId != null) {
            loadMessages(conversationId)
        } else {
            _currentMessages.value = emptyList()
        }
    }

    fun searchChats(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            try {
                val localResults = withContext(Dispatchers.IO) {
                    searchLocal(trimmed)
                }
                val cloudResults = searchCloud(trimmed)
                _searchResults.value = ChatSearchHelper.mergeResults(localResults + cloudResults)
            } catch (e: Exception) {
                Log.e(TAG, "searchChats failed", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    private fun searchLocal(query: String): List<ChatSearchResult> {
        val titleHits = localConversationDao.searchConversationsByTitle(query, SEARCH_RESULT_LIMIT)
            .map { row ->
                ChatSearchHelper.toSearchResult(
                    conversationId = row.conversationId,
                    title = row.title,
                    matchedSource = row.matchedText,
                    matchKind = ChatSearchMatchKind.TITLE,
                    messageId = null,
                    sortTimestamp = row.sortTimestamp,
                    query = query
                )
            }

        val messageHits = localMessageDao.searchMessagesByContent(query, SEARCH_RESULT_LIMIT)
            .map { row ->
                ChatSearchHelper.toSearchResult(
                    conversationId = row.conversationId,
                    title = row.title,
                    matchedSource = row.matchedText,
                    matchKind = ChatSearchMatchKind.MESSAGE,
                    messageId = row.messageId,
                    sortTimestamp = row.sortTimestamp,
                    query = query
                )
            }

        return titleHits + messageHits
    }

    private suspend fun searchCloud(query: String): List<ChatSearchResult> {
        val pattern = "%$query%"
        val titleLookup = _conversations.value.associateBy { it.id }

        val titleHits = try {
            supabaseClient.postgrest["conversations"]
                .select {
                    filter {
                        ilike("title", pattern)
                    }
                    order("updated_at", Order.DESCENDING)
                    limit(SEARCH_RESULT_LIMIT.toLong())
                }
                .decodeList<Conversation>()
                .map { conversation ->
                    ChatSearchHelper.toSearchResult(
                        conversationId = conversation.id,
                        title = conversation.title,
                        matchedSource = conversation.title.orEmpty(),
                        matchKind = ChatSearchMatchKind.TITLE,
                        messageId = null,
                        sortTimestamp = conversation.updatedAt,
                        query = query
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Cloud title search failed", e)
            emptyList()
        }

        val messageHits = try {
            val messages = supabaseClient.postgrest["messages"]
                .select {
                    filter {
                        ilike("content", pattern)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(SEARCH_RESULT_LIMIT.toLong())
                }
                .decodeList<ChatMessage>()

            val missingConversationIds = messages
                .map { it.conversationId }
                .distinct()
                .filter { it !in titleLookup }

            val fetchedTitles = if (missingConversationIds.isEmpty()) {
                emptyMap()
            } else {
                fetchConversationTitles(missingConversationIds)
            }

            val resolvedTitles = titleLookup.mapValues { (_, conversation) -> conversation.title } + fetchedTitles

            messages.map { message ->
                ChatSearchHelper.toSearchResult(
                    conversationId = message.conversationId,
                    title = resolvedTitles[message.conversationId],
                    matchedSource = message.content,
                    matchKind = ChatSearchMatchKind.MESSAGE,
                    messageId = message.id,
                    sortTimestamp = message.createdAt,
                    query = query
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloud message search failed", e)
            emptyList()
        }

        return titleHits + messageHits
    }

    private suspend fun fetchConversationTitles(conversationIds: List<String>): Map<String, String?> {
        return try {
            supabaseClient.postgrest["conversations"]
                .select {
                    filter {
                        isIn("id", conversationIds)
                    }
                }
                .decodeList<Conversation>()
                .associate { it.id to it.title }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch conversation titles for search", e)
            emptyMap()
        }
    }

    private fun refreshConversations() {
        viewModelScope.launch {
            try {
                // 1. Fetch Cloud Conversations
                val cloudConversations = try {
                    supabaseClient.postgrest["conversations"]
                        .select {
                            order("updated_at", Order.DESCENDING)
                        }
                        .decodeList<Conversation>()
                        .map { it.copy(isLocal = false) }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch cloud conversations", e)
                    emptyList()
                }

                // 2. Fetch Local Conversations from Room
                val localEntities = withContext(Dispatchers.IO) {
                    localConversationDao.getAllConversations()
                }
                val localConversations = localEntities.map { entity ->
                    Conversation(
                        id = entity.id,
                        userId = entity.userId,
                        title = entity.title,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt,
                        isActive = entity.isActive,
                        isLocal = true
                    )
                }

                // 3. Merge, Deduplicate & Sort Chronologically Descending
                val combined = (cloudConversations + localConversations)
                    .distinctBy { it.id }
                    .sortedByDescending { it.updatedAt }

                Log.d(TAG, "refreshConversations: combined ${combined.size} conversations (${localConversations.size} local, ${cloudConversations.size} cloud)")
                _conversations.value = combined
            } catch (e: Exception) {
                Log.e(TAG, "refreshConversations failed", e)
            }
        }
    }

    private fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            try {
                // Check if conversation exists in local Room DB
                val localEntity = withContext(Dispatchers.IO) {
                    localConversationDao.getConversationById(conversationId)
                }
                if (localEntity != null) {
                    val localMessages = withContext(Dispatchers.IO) {
                        localMessageDao.getMessagesForConversation(conversationId)
                    }
                    val mappedMessages = localMessages.map { entity ->
                        val widgetPayload = entity.widgetPayloadJson?.let { jsonStr ->
                            runCatching {
                                Json { ignoreUnknownKeys = true }.decodeFromString<WidgetPayload>(jsonStr)
                            }.getOrNull()
                        }
                        ChatMessage(
                            id = entity.id,
                            conversationId = entity.conversationId,
                            role = entity.role,
                            content = entity.content,
                            appTarget = entity.appTarget,
                            modelTier = entity.modelTier,
                            widgetPayload = widgetPayload,
                            createdAt = entity.createdAt
                        )
                    }
                    Log.d(TAG, "loadMessages: loaded ${mappedMessages.size} local messages for $conversationId")
                    _currentMessages.value = mappedMessages
                } else {
                    // Fetch from cloud Supabase
                    val result = supabaseClient.postgrest["messages"]
                        .select {
                            filter {
                                eq("conversation_id", conversationId)
                            }
                            order("created_at", Order.ASCENDING)
                        }
                        .decodeList<ChatMessage>()
                    Log.d(TAG, "loadMessages: fetched ${result.size} cloud messages for $conversationId")
                    _currentMessages.value = result
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadMessages: failed", e)
                _currentMessages.value = emptyList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeJob?.cancel()
        localFlowJob?.cancel()
        searchJob?.cancel()
        viewModelScope.launch {
            runCatching { supabaseClient.realtime.removeAllChannels() }
        }
    }
}
