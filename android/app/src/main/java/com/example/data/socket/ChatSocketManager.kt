package com.example.data.socket

import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.ConnectionStatus
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "ChatSocketManager"

/**
 * ChatSocketManager connects to the CyTube Socket.io server,
 * listens for 'chatMsg' events, parses chat payloads, and exposes
 * StateFlows for UI consumption.
 */
class ChatSocketManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    private val maxHistorySize: Int = 100
) {
    private var socket: Socket? = null
    private var currentRoomName: String = "420Grindhouse"
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null
    private var isIntentionallyClosed = false

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.OFFLINE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _latestMessage = MutableStateFlow<ChatMessage?>(null)
    val latestMessage: StateFlow<ChatMessage?> = _latestMessage.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Connects to the CyTube room Socket.IO server.
     */
    fun connect(roomName: String = "420Grindhouse") {
        currentRoomName = roomName
        isIntentionallyClosed = false
        reconnectJob?.cancel()

        scope.launch {
            _connectionStatus.value = ConnectionStatus.RECONNECTING
            try {
                val serverUrl = fetchSocketServerUrl(roomName)
                initSocket(serverUrl, roomName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve socket config for room $roomName, falling back to default", e)
                initSocket("https://cytu.be", roomName)
            }
        }
    }

    private suspend fun fetchSocketServerUrl(roomName: String): String = withContext(Dispatchers.IO) {
        val configUrl = "https://cytu.be/socketconfig/$roomName.json"
        try {
            val request = Request.Builder().url(configUrl).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val servers = json.optJSONArray("servers")
                    if (servers != null && servers.length() > 0) {
                        val firstServer = servers.getJSONObject(0)
                        val url = firstServer.optString("url")
                        if (url.isNotBlank()) return@withContext url
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch socket server config: ${e.message}")
        }
        "https://cytu.be"
    }

    private fun initSocket(serverUrl: String, roomName: String) {
        try {
            socket?.disconnect()
            socket?.off()

            val opts = IO.Options().apply {
                forceNew = true
                reconnection = false
                timeout = 15000
                transports = arrayOf("websocket", "polling")
            }

            val newSocket = IO.socket(serverUrl, opts)
            socket = newSocket

            newSocket.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Chat socket connected to $serverUrl for room $roomName")
                reconnectAttempt = 0
                _connectionStatus.value = ConnectionStatus.LIVE
                val joinObj = JSONObject().apply {
                    put("name", roomName)
                }
                newSocket.emit("joinChannel", joinObj)
            }

            newSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val err = args.firstOrNull()
                Log.e(TAG, "Chat socket connect error: $err")
                handleDisconnect()
            }

            newSocket.on(Socket.EVENT_DISCONNECT) {
                Log.w(TAG, "Chat socket disconnected")
                handleDisconnect()
            }

            // Listen for 'chatMsg' events
            newSocket.on("chatMsg") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                handleIncomingChat(data)
            }

            newSocket.connect()

        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing chat socket", e)
            handleDisconnect()
        }
    }

    /**
     * Parses the incoming CyTube 'chatMsg' payload and updates StateFlows.
     */
    private fun handleIncomingChat(data: JSONObject) {
        val username = data.optString("username", "Anon")
        val msg = data.optString("msg", "")
        val time = data.optLong("time", System.currentTimeMillis())
        val meta = data.optJSONObject("meta")
        val isSystem = meta?.optBoolean("addClass") == true ||
                username.equals("[server]", ignoreCase = true) ||
                username.equals("system", ignoreCase = true)
        val rank = data.optInt("rank", 0)

        val chatMessage = ChatMessage(
            username = username,
            text = msg,
            timestamp = time,
            isSystem = isSystem,
            userRank = rank
        )

        scope.launch {
            _latestMessage.value = chatMessage
            val updated = (_chatMessages.value + chatMessage).takeLast(maxHistorySize)
            _chatMessages.value = updated
        }
    }

    private fun handleDisconnect() {
        if (isIntentionallyClosed) return

        _connectionStatus.value = ConnectionStatus.RECONNECTING
        reconnectAttempt++

        val delayMillis = (Math.pow(2.0, reconnectAttempt.coerceAtMost(5).toDouble()) * 1000).toLong()
        Log.d(TAG, "Scheduling chat socket reconnect #$reconnectAttempt in ${delayMillis}ms")

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delayMillis)
            if (!isIntentionallyClosed) {
                if (reconnectAttempt > 6) {
                    _connectionStatus.value = ConnectionStatus.OFFLINE
                } else {
                    connect(currentRoomName)
                }
            }
        }
    }

    /**
     * Manually retries connection to the chat server.
     */
    fun retry() {
        reconnectAttempt = 0
        connect(currentRoomName)
    }

    /**
     * Clears local chat history buffer.
     */
    fun clearChat() {
        _chatMessages.value = emptyList()
        _latestMessage.value = null
    }

    /**
     * Disconnects the socket and cleans up resources.
     */
    fun disconnect() {
        isIntentionallyClosed = true
        reconnectJob?.cancel()
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionStatus.value = ConnectionStatus.OFFLINE
    }
}
