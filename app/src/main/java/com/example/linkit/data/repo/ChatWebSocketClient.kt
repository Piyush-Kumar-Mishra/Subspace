package com.example.linkit.data.repo

import com.example.linkit.data.TokenStore
import com.example.linkit.data.models.ChatMessageResponse
import com.example.linkit.data.models.WebSocketChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatWebSocketClient @Inject constructor(
    private val tokenStore: TokenStore
) {
    private var webSocket: WebSocket? = null
    private var currentProjectId: Long? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var reconnectJob: Job? = null

    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<ChatMessageResponse>(extraBufferCapacity = 64)
    val incomingMessages = _incomingMessages.asSharedFlow()

    private val _typingUpdates = MutableSharedFlow<Pair<Long, String>>(extraBufferCapacity = 64)
    val typingUpdates = _typingUpdates.asSharedFlow()

    private val _onlineUsersUpdates = MutableSharedFlow<List<Long>>(extraBufferCapacity = 64)
    val onlineUsersUpdates = _onlineUsersUpdates.asSharedFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun connect(projectId: Long) {
        currentProjectId = projectId
        disconnect()

        scope.launch {
            val token = tokenStore.token.first()
            if (token.isNullOrEmpty()) {
                Timber.e("No token available for WebSocket connection")
                return@launch
            }

            val client = OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url("ws://192.168.1.100:8080/ws/chat/$projectId")
                .addHeader("Authorization", "Bearer $token")
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {

                override fun onOpen(ws: WebSocket, response: Response) {
                    Timber.i("WebSocket connected to project $projectId")
                    _connectionStatus.value = true
                    stopReconnectionAttempts()
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    scope.launch {
                        try {
                            val msg = json.decodeFromString<WebSocketChatMessage>(text)
                            handleWebSocketMessage(msg)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to parse WebSocket message")
                        }
                    }
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Timber.i("WebSocket closed: $reason")
                    _connectionStatus.value = false
                    if (code != 1000) attemptReconnection()
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Timber.e(t, "WebSocket connection failed")
                    _connectionStatus.value = false
                    attemptReconnection()
                }
            })
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionStatus.value = false
        stopReconnectionAttempts()
    }

    fun sendMessage(content: String) {
        val ws = webSocket ?: return
        if (!_connectionStatus.value) return
        ws.send("""{"content":"${content.replace("\"", "\\\"")}"}""")
    }

    private fun handleWebSocketMessage(message: WebSocketChatMessage) {
        scope.launch {
            when (message.type) {
                "MESSAGE" -> message.message?.let { _incomingMessages.emit(it) }
                "TYPING" -> {
                    val id = message.typingUserId
                    val name = message.typingUserName
                    if (id != null && name != null) _typingUpdates.emit(id to name)
                }
                "USER_JOINED", "USER_LEFT", "CONNECTED" ->
                    message.onlineUsers?.let { _onlineUsersUpdates.emit(it) }
            }
        }
    }

    private fun attemptReconnection() {
        stopReconnectionAttempts()
        reconnectJob = scope.launch {
            var delaySeconds = 1L
            val maxDelay = 30L

            while (currentProjectId != null && !_connectionStatus.value) {
                Timber.i("Attempting to reconnect in ${delaySeconds}s...")
                delay(delaySeconds * 1000)
                currentProjectId?.let { connect(it) }
                delaySeconds = (delaySeconds * 2).coerceAtMost(maxDelay)
            }
        }
    }

    private fun stopReconnectionAttempts() {
        reconnectJob?.cancel()
        reconnectJob = null
    }
}
