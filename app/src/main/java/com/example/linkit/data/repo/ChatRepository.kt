package com.example.linkit.data.repo

import com.example.linkit.data.api.ChatApi
import com.example.linkit.data.local.dao.ChatMessageDao
import com.example.linkit.data.local.entity.ChatMessageEntity
import com.example.linkit.data.local.entity.toChatMessageEntity
import com.example.linkit.data.local.entity.toChatMessageResponse
import com.example.linkit.data.models.ChatHistoryResponse
import com.example.linkit.data.models.ChatMessageRequest
import com.example.linkit.data.models.ChatMessageResponse
import com.example.linkit.util.NetworkResult
import com.example.linkit.util.safeApiCallDirect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface ChatRepository {
    // HTTP
    fun getChatHistory(projectId: Long, beforeTimestamp: String? = null, limit: Int = 20): Flow<NetworkResult<ChatHistoryResponse>>
    fun sendMessage(projectId: Long, content: String): Flow<NetworkResult<ChatMessageResponse>>

    // WebSocket
    fun connectToChat(projectId: Long)
    fun disconnectFromChat(projectId: Long)
    fun sendWebSocketMessage(content: String)

    // WebSocket streams
    val incomingMessages: Flow<ChatMessageResponse>
    val connectionStatus: Flow<Boolean>
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val webSocketClient: ChatWebSocketClient,
    private val chatMessageDao: ChatMessageDao
) : ChatRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _incomingMessages = MutableSharedFlow<ChatMessageResponse>(extraBufferCapacity = 64)
    private val _connectionStatus = MutableStateFlow(false)

    override val incomingMessages: Flow<ChatMessageResponse> = _incomingMessages.asSharedFlow()
    override val connectionStatus: Flow<Boolean> = _connectionStatus.asStateFlow()

    init {
        startWebSocketObservers()
    }

    private fun startWebSocketObservers() {
        scope.launch { webSocketClient.incomingMessages.collect { _incomingMessages.emit(it) } }
        scope.launch { webSocketClient.connectionStatus.collect { _connectionStatus.value = it } }
    }

    override fun getChatHistory(
        projectId: Long,
        beforeTimestamp: String?,
        limit: Int
    ): Flow<NetworkResult<ChatHistoryResponse>> = flow {
        val local = chatMessageDao.getMessages(projectId, limit)
        if (local.isNotEmpty()) {
            emit(
                NetworkResult.Success(
                    ChatHistoryResponse(
                        messages = local.map { it.toChatMessageResponse() },
                        hasMore = true,
                        nextPageToken = null
                    )
                )
            )
        }

        emit(NetworkResult.Loading())

        val result = safeApiCallDirect {
            chatApi.getChatHistory(projectId, beforeTimestamp, limit)
        }

        when (result) {
            is NetworkResult.Success -> {
                val history = result.data
                chatMessageDao.insertMessages(history.messages.map { it.toChatMessageEntity() })
                emit(NetworkResult.Success(history))
            }

            is NetworkResult.Error -> emit(result)
            is NetworkResult.Loading -> emit(result)
        }
    }

    override fun sendMessage(projectId: Long, content: String): Flow<NetworkResult<ChatMessageResponse>> = flow {
        val tempId = System.currentTimeMillis()
        val tempMessage = ChatMessageEntity(
            id = tempId,
            projectId = projectId,
            senderId = null,
            messageType = "USER_MESSAGE",
            content = content,
            systemEventType = null,
            systemEventData = null,
            createdAt = Instant.now().toString(),
            isOwnMessage = true,
            showDateHeader = false,
            dateHeader = null,
            isSynced = false
        )
        chatMessageDao.insertMessage(tempMessage)

        emit(NetworkResult.Loading())

        val result = safeApiCallDirect {
            chatApi.sendMessage(projectId, ChatMessageRequest(content))
        }

        when (result) {
            is NetworkResult.Success -> {
                val msg = result.data
                chatMessageDao.deleteMessage(tempId)
                chatMessageDao.insertMessage(msg.toChatMessageEntity().copy(isSynced = true))
                emit(NetworkResult.Success(msg))
            }

            is NetworkResult.Error -> emit(result)
            is NetworkResult.Loading -> emit(result)
        }
    }

    override fun connectToChat(projectId: Long) {
        webSocketClient.connect(projectId)
    }

    override fun disconnectFromChat(projectId: Long) {
        webSocketClient.disconnect()
    }

    override fun sendWebSocketMessage(content: String) {
        webSocketClient.sendMessage(content)
    }

}