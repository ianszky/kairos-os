package com.kairos.os.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.os.data.db.LocalConversationDao
import com.kairos.os.data.db.LocalMessageDao
import com.kairos.os.domain.models.ChatMessage
import com.kairos.os.domain.models.Conversation
import com.kairos.os.domain.models.WidgetPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

    private var realtimeJob: Job? = null
    private var localFlowJob: Job? = null

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
                val localEntities = localConversationDao.getAllConversations()
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
                val localEntity = localConversationDao.getConversationById(conversationId)
                if (localEntity != null) {
                    val localMessages = localMessageDao.getMessagesForConversation(conversationId)
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
        viewModelScope.launch {
            runCatching { supabaseClient.realtime.removeAllChannels() }
        }
    }
}
