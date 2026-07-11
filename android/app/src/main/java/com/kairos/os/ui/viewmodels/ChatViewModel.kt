package com.kairos.os.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kairos.os.domain.models.ChatMessage
import com.kairos.os.domain.models.Conversation
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

private const val TAG = "ChatViewModel"

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessage>> = _currentMessages.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private var realtimeJob: Job? = null

    /**
     * 1. Fetch all conversations via a standard postgrest query (always works).
     * 2. Subscribe to realtime changes so new conversations appear automatically.
     */
    fun loadConversations() {
        // Initial fetch
        viewModelScope.launch {
            try {
                val result = supabaseClient.postgrest["conversations"]
                    .select {
                        order("updated_at", Order.DESCENDING)
                    }
                    .decodeList<Conversation>()
                Log.d(TAG, "loadConversations: fetched ${result.size} conversations")
                _conversations.value = result
            } catch (e: Exception) {
                Log.e(TAG, "loadConversations: fetch failed", e)
            }
        }

        // Realtime subscription for live updates
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
                    // On any change, re-fetch the full list to keep ordering correct
                    try {
                        val refreshed = supabaseClient.postgrest["conversations"]
                            .select {
                                order("updated_at", Order.DESCENDING)
                            }
                            .decodeList<Conversation>()
                        _conversations.value = refreshed
                    } catch (e: Exception) {
                        Log.e(TAG, "loadConversations: realtime refresh failed", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadConversations: realtime subscription failed", e)
            }
        }
    }

    /**
     * Called after a successful postPrompt response.
     * If this was the first message (new conversation), set the conversationId
     * and refresh the sidebar list.
     */
    fun onPromptResponse(responseConversationId: String?) {
        if (responseConversationId != null && _currentConversationId.value == null) {
            _currentConversationId.value = responseConversationId
        }
        // Always refresh — handles title updates and new conversations
        refreshConversations()
    }

    /**
     * Reset state for a brand new conversation (e.g. when user presses Back).
     * Does NOT clear the sidebar list.
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
                val result = supabaseClient.postgrest["conversations"]
                    .select {
                        order("updated_at", Order.DESCENDING)
                    }
                    .decodeList<Conversation>()
                Log.d(TAG, "refreshConversations: fetched ${result.size} conversations")
                _conversations.value = result
            } catch (e: Exception) {
                Log.e(TAG, "refreshConversations failed", e)
            }
        }
    }

    private fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            try {
                val result = supabaseClient.postgrest["messages"]
                    .select {
                        filter {
                            eq("conversation_id", conversationId)
                        }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<ChatMessage>()
                Log.d(TAG, "loadMessages: fetched ${result.size} messages for $conversationId")
                _currentMessages.value = result
            } catch (e: Exception) {
                Log.e(TAG, "loadMessages: failed", e)
                _currentMessages.value = emptyList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeJob?.cancel()
        viewModelScope.launch {
            runCatching { supabaseClient.realtime.removeAllChannels() }
        }
    }
}
