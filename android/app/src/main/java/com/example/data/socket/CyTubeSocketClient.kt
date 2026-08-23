package com.example.data.socket

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.model.ChannelEmote
import com.example.data.model.ChannelUser
import com.example.data.model.ChatMessage
import com.example.data.model.LoginState
import com.example.data.model.ConnectionStatus
import com.example.data.model.MediaItem
import com.example.data.model.MediaSyncUpdate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "CyTubeSocketClient"

/** CyTube erlaubt requestPlaylist einmal pro Minute (REQ_PLAYLIST_LIMIT_REACHED). */
private const val PLAYLIST_REQUEST_MIN_INTERVAL_MS = 60_000L

/**
 * High-performance, battle-tested CyTube WebSocket & Socket.IO Engine.IO v3 Client.
 * Connects directly to CyTube's WebSocket server cluster (e.g. bigapple.cytu.be),
 * handles joinChannel, requestPlaylist, live chat, and Google Drive / Direct stream metadata.
 */
class CyTubeSocketClient(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private var webSocket: WebSocket? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pingRunnable: Runnable? = null
    private var reconnectRunnable: Runnable? = null

    private var currentRoomName: String = "Channel-Z"
    private var isIntentionallyClosed = false
    private var reconnectAttempt = 0
    private var lastPlaylistRequestMs = 0L

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _nowPlaying = MutableStateFlow<MediaItem?>(null)
    val nowPlaying: StateFlow<MediaItem?> = _nowPlaying.asStateFlow()

    private val _upNext = MutableStateFlow<List<MediaItem>>(emptyList())
    val upNext: StateFlow<List<MediaItem>> = _upNext.asStateFlow()

    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
    val playlist: StateFlow<List<MediaItem>> = _playlist.asStateFlow()

    private val _userCount = MutableStateFlow(0)
    val userCount: StateFlow<Int> = _userCount.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _users = MutableStateFlow<List<ChannelUser>>(emptyList())
    val users: StateFlow<List<ChannelUser>> = _users.asStateFlow()

    private val _emotes = MutableStateFlow<List<ChannelEmote>>(emptyList())
    val emotes: StateFlow<List<ChannelEmote>> = _emotes.asStateFlow()

    private val _motd = MutableStateFlow<String?>(null)
    val motd: StateFlow<String?> = _motd.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.LoggedOut)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    /** Zugangsdaten, damit nach einem Reconnect nicht von Hand neu angemeldet werden muss. */
    private var credentials: Pair<String, String>? = null

    private val _mediaChangedEvent = MutableSharedFlow<MediaItem>(replay = 1, extraBufferCapacity = 16)
    val mediaChangedEvent: SharedFlow<MediaItem> = _mediaChangedEvent.asSharedFlow()

    private val _mediaSyncEvent = MutableSharedFlow<MediaSyncUpdate>(replay = 1, extraBufferCapacity = 16)
    val mediaSyncEvent: SharedFlow<MediaSyncUpdate> = _mediaSyncEvent.asSharedFlow()

    private val _privateMessageEvent = MutableSharedFlow<com.example.data.model.PrivateMessage>(replay = 0, extraBufferCapacity = 16)
    val privateMessageEvent: SharedFlow<com.example.data.model.PrivateMessage> = _privateMessageEvent.asSharedFlow()

    private val _magicOtpCodeEvent = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 8)
    val magicOtpCodeEvent: SharedFlow<String> = _magicOtpCodeEvent.asSharedFlow()

    private val cachedPlaylist = mutableListOf<MediaItem>()

    fun connect(roomName: String = "Channel-Z", savedCredentials: Pair<String, String>? = null) {
        cancelReconnect()
        isIntentionallyClosed = false
        disconnectInternal()
        currentRoomName = roomName
        // Gespeichertes Konto uebergeben: Der Handshake-Weg meldet die Sitzung dann
        // automatisch an — auch Gaeste (Passwort leer), ohne dass jemand tippen muss.
        if (savedCredentials != null) {
            credentials = savedCredentials
        }

        _connectionStatus.value = ConnectionStatus.RECONNECTING
        Log.d(TAG, "Resolving CyTube socket server for room '$roomName'...")

        scope.launch(ioDispatcher) {
            val serverUrl = resolveSocketServerUrl(roomName)
            withContext(Dispatchers.Main) {
                connectWebSocket(serverUrl, roomName)
            }
        }
    }

    private fun resolveSocketServerUrl(room: String): String {
        val userAgent = "Mozilla/5.0 (Linux; Android 11; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        val endpoints = listOf(
            "https://cytu.be/socketconfig/$room.json"
        )

        for (endpoint in endpoints) {
            try {
                val req = Request.Builder().url(endpoint).header("User-Agent", userAgent).build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        val servers = json.optJSONArray("servers")
                        if (servers != null && servers.length() > 0) {
                            val sObj = servers.getJSONObject(0)
                            val targetUrl = sObj.optString("url", "")
                            if (targetUrl.isNotBlank()) {
                                Log.d(TAG, "Resolved CyTube socket server from $endpoint: $targetUrl")
                                return targetUrl
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed resolving socket config from $endpoint: ${e.message}")
            }
        }
        return "https://cytu.be"
    }

    private fun connectWebSocket(targetServerUrl: String, room: String) {
        var wsUrl = targetServerUrl
        if (wsUrl.startsWith("http://")) {
            wsUrl = wsUrl.replace("http://", "ws://")
        } else if (wsUrl.startsWith("https://")) {
            wsUrl = wsUrl.replace("https://", "wss://")
        }

        if (!wsUrl.contains("socket.io")) {
            val delimiter = if (wsUrl.endsWith("/")) "" else "/"
            wsUrl = "$wsUrl${delimiter}socket.io/?EIO=3&transport=websocket"
        }

        Log.d(TAG, "Connecting OkHttp WebSocket to: $wsUrl")
        val userAgent = "Mozilla/5.0 (Linux; Android 11; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        val request = Request.Builder()
            .url(wsUrl)
            .header("User-Agent", userAgent)
            .header("Origin", "https://cytu.be")
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connection opened")
                reconnectAttempt = 0
                _connectionStatus.value = ConnectionStatus.LIVE
                startPingLoop()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleSocketIoPacket(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                _connectionStatus.value = ConnectionStatus.RECONNECTING
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                stopPingLoop()
                if (!isIntentionallyClosed) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket connection failure: ${t.message}", t)
                stopPingLoop()
                if (!isIntentionallyClosed) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun handleSocketIoPacket(text: String) {
        if (text.isEmpty()) return

        // Engine.IO packet types:
        // 0: Handshake
        // 2: Server ping -> reply with 3 (Pong)
        // 3: Pong
        // 42: Socket.IO message event
        when {
            text.startsWith("0") || text == "40" -> {
                // Handshake or Socket.IO connect completed: join channel & request playlist queue
                joinChannel()
                // Nach einem Verbindungsabbruch ist die Sitzung weg — mit gemerkten Zugangsdaten
                // still wieder anmelden, sonst steht man ploetzlich stumm im Raum.
                credentials?.let { (name, pw) ->
                    handler.postDelayed({ login(name, pw) }, 600)
                }
            }
            text.startsWith("2") -> {
                webSocket?.send("3")
            }
            text.startsWith("42") -> {
                try {
                    val jsonStr = text.substring(2)
                    val eventArray = JSONArray(jsonStr)
                    val eventName = eventArray.optString(0)

                    when (eventName) {
                        "chatMsg" -> {
                            val data = eventArray.optJSONObject(1)
                            if (data != null) handleIncomingChat(data)
                        }
                        "pm" -> {
                            val data = eventArray.optJSONObject(1)
                            if (data != null) handleIncomingPm(data)
                        }
                        "changeMedia", "setCurrent" -> {
                            val data = eventArray.optJSONObject(1)
                            if (data != null) handleMediaChange(data)
                        }
                        "mediaUpdate" -> {
                            val data = eventArray.optJSONObject(1)
                            if (data != null) handleMediaUpdate(data)
                        }
                        "playlist", "setPlaylist" -> {
                            val data = eventArray.opt(1)
                            val array = when (data) {
                                is JSONArray -> data
                                is JSONObject -> data.optJSONArray("playlist") ?: data.optJSONArray("items")
                                else -> null
                            }
                            if (array != null) {
                                Log.d(TAG, "Received playlist with ${array.length()} items")
                                handlePlaylist(array)
                            }
                        }
                        "queue" -> {
                            val data = eventArray.optJSONObject(1)
                            if (data != null) handleQueueItem(data)
                        }
                        "delete" -> {
                            val data = eventArray.optJSONObject(1)
                            val uid = data?.optString("uid", "") ?: ""
                            if (uid.isNotEmpty()) {
                                cachedPlaylist.removeAll { it.id == uid }
                                updateUpNextList()
                            }
                        }
                        "userlist", "setUserlist" -> {
                            val users = eventArray.optJSONArray(1)
                            if (users != null) {
                                _userCount.value = users.length()
                                handleUserList(users)
                            }
                        }
                        "emoteList" -> {
                            val list = eventArray.optJSONArray(1)
                            if (list != null) handleEmoteList(list)
                        }
                        "setMotd" -> {
                            val motdHtml = eventArray.optString(1, "")
                            if (motdHtml.isNotBlank()) {
                                val cleanText = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    android.text.Html.fromHtml(motdHtml, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                                } else {
                                    @Suppress("DEPRECATION")
                                    android.text.Html.fromHtml(motdHtml).toString().trim()
                                }
                                _motd.value = cleanText.ifBlank { null }
                            }
                        }
                        "login" -> {
                            val data = eventArray.optJSONObject(1)
                            handleLoginResult(data)
                        }
                        "usercount" -> {
                            val count = eventArray.optInt(1, 0)
                            _userCount.value = count
                        }
                        "addUser" -> {
                            _userCount.value += 1
                            eventArray.optJSONObject(1)?.let { obj ->
                                parseUser(obj)?.let { user ->
                                    _users.value = (_users.value + user).distinctBy { it.name }
                                }
                            }
                        }
                        "userLeave" -> {
                            _userCount.value = maxOf(0, _userCount.value - 1)
                            val name = eventArray.optJSONObject(1)?.optString("name").orEmpty()
                            if (name.isNotEmpty()) {
                                _users.value = _users.value.filterNot { it.name == name }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing socket message: $text", e)
                }
            }
        }
    }

    /**
     * Meldet sich am CyTube-Konto an. Ohne Anmeldung verwirft der Kanal Chat-Nachrichten
     * kommentarlos — die Berechtigung 'chat' steht dort auf Rang 1, Gaeste haben -1.
     */
    fun login(username: String, password: String) {
        credentials = username to password
        _loginState.value = LoginState.InProgress
        val payload = JSONArray().apply {
            put("login")
            put(JSONObject().apply {
                put("name", username)
                put("pw", password)
            })
        }
        webSocket?.send("42$payload")
    }

    fun logout() {
        credentials = null
        _loginState.value = LoginState.LoggedOut
    }

    /** Schickt eine Nachricht in den Raum. Ohne Anmeldung wuerde sie im Nichts landen. */
    fun sendChat(message: String): Boolean {
        val text = message.trim()
        if (text.isEmpty() || _loginState.value !is LoginState.LoggedIn) return false
        val payload = JSONArray().apply {
            put("chatMsg")
            put(JSONObject().apply {
                put("msg", text)
                put("meta", JSONObject())
            })
        }
        webSocket?.send("42$payload")
        return true
    }

    private fun joinChannel() {
        val socket = webSocket ?: return
        try {
            val joinPayload = JSONArray().apply {
                put("joinChannel")
                put(JSONObject().apply { put("name", currentRoomName) })
            }
            val frame = "42$joinPayload"
            Log.d(TAG, "Sending joinChannel: $frame")
            socket.send(frame)

            // Request room playlist queue
            handler.postDelayed({
                val plPayload = JSONArray().apply {
                    put("requestPlaylist")
                }
                socket.send("42$plPayload")
            }, 300)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending joinChannel", e)
        }
    }

    private fun handleLoginResult(data: JSONObject?) {
        val success = data?.optBoolean("success", false) ?: false
        _loginState.value = if (success) {
            val name = data?.optString("name").orEmpty().ifEmpty { credentials?.first.orEmpty() }
            Log.d(TAG, "Angemeldet als $name")
            LoginState.LoggedIn(name)
        } else {
            val error = data?.optString("error").orEmpty()
                .ifEmpty { "Anmeldung fehlgeschlagen" }
            Log.w(TAG, "Anmeldung abgelehnt: $error")
            credentials = null
            LoginState.Failed(error)
        }
    }

    private fun handleUserList(array: JSONArray) {
        val list = buildList {
            for (i in 0 until array.length()) {
                array.optJSONObject(i)?.let { obj -> parseUser(obj)?.let { add(it) } }
            }
        }
        _users.value = list.sortedWith(compareByDescending<ChannelUser> { it.rank }.thenBy { it.name.lowercase() })
    }

    private fun parseUser(obj: JSONObject): ChannelUser? {
        val name = obj.optString("name").orEmpty()
        if (name.isEmpty()) return null
        val meta = obj.optJSONObject("meta")
        return ChannelUser(
            name = name,
            rank = obj.optInt("rank", 0),
            isAfk = meta?.optBoolean("afk", false) ?: false,
            isMuted = meta?.optBoolean("muted", false) ?: false,
            profileImage = obj.optJSONObject("profile")?.optString("image").orEmpty().ifEmpty { null }
        )
    }

    private fun handleEmoteList(array: JSONArray) {
        val list = buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name").orEmpty()
                val image = obj.optString("image").orEmpty()
                if (name.isNotEmpty() && image.isNotEmpty()) {
                    add(ChannelEmote(name = name, imageUrl = image))
                }
            }
        }
        Log.d(TAG, "Emote-Liste erhalten: ${list.size} Eintraege")
        _emotes.value = list
    }

    private fun handleIncomingChat(data: JSONObject) {
        val username = data.optString("username", "Guest")
        val rawMsg = data.optString("msg", "")
        val time = data.optLong("time", System.currentTimeMillis())
        val meta = data.optJSONObject("meta")
        val userRank = data.optInt("rank", meta?.optInt("rank", 0) ?: 0)
        val isSystem = username.equals("[System]", ignoreCase = true) || username.equals("System", ignoreCase = true)

        val cleanMsg = rawMsg.replace(Regex("<[^>]*>"), "").trim()
        if (cleanMsg.isNotEmpty()) {
            val newMsg = ChatMessage(
                id = "${time}_${username.hashCode()}_${(1000..9999).random()}",
                username = username,
                text = cleanMsg,
                timestamp = time,
                isSystem = isSystem,
                userRank = userRank
            )
            val current = _chatMessages.value.toMutableList()
            current.add(newMsg)
            if (current.size > 100) current.removeAt(0)
            _chatMessages.value = current
        }
    }

    private fun handleIncomingPm(data: JSONObject) {
        val from = data.optString("username", data.optString("from", "System"))
        val rawMsg = data.optString("msg", data.optString("text", ""))
        val time = data.optLong("time", System.currentTimeMillis())
        val to = data.optString("to", "")
        val cleanMsg = rawMsg.replace(Regex("<[^>]*>"), "").trim()

        if (cleanMsg.isNotEmpty()) {
            val pm = com.example.data.model.PrivateMessage(
                from = from,
                text = cleanMsg,
                timestamp = time,
                to = to
            )
            _privateMessageEvent.tryEmit(pm)
            Log.d(TAG, "PM received from $from: $cleanMsg")

            // Magic OTP Auto-Extraction: Detect 6-digit code from Kryten / WebQueue bot
            val otpRegex = Regex("""\b(\d{6})\b""")
            val match = otpRegex.find(cleanMsg)
            if (match != null) {
                val code = match.groupValues[1]
                Log.i(TAG, "✨ Magic OTP Code extracted from PM ($from): $code")
                _magicOtpCodeEvent.tryEmit(code)
            }
        }
    }

    private fun handleMediaChange(data: JSONObject) {
        val id = data.optString("id", "")
        val title = data.optString("title", "Channel-Z Live")
        val duration = data.optDouble("seconds", data.optDouble("duration", 0.0))
        val type = data.optString("type", "raw")
        val currentTime = data.optDouble("currentTime", data.optDouble("time", 0.0))
        val paused = data.optBoolean("paused", false)
        val directUrl = parseDirectUrlFromData(data)

        val item = MediaItem(
            id = id,
            title = title,
            durationSeconds = duration,
            type = type,
            url = directUrl.ifBlank { if (id.startsWith("http")) id else null },
            currentTimeSeconds = currentTime,
            paused = paused,
            directUrl = directUrl
        )

        val previous = _nowPlaying.value
        val isRealChange = previous == null || previous.id != id || previous.title != title

        Log.d(TAG, "Media changed: '$title' (type: $type, id: $id, direct: $directUrl)")
        _nowPlaying.value = item
        if (isRealChange) {
            _mediaChangedEvent.tryEmit(item)
        }
        _mediaSyncEvent.tryEmit(MediaSyncUpdate(currentTime, paused))
        updateUpNextList()

        // Playlist nachfordern, wenn die Queue leer ist. CyTube limitiert requestPlaylist auf
        // einen Aufruf pro Minute und antwortet sonst mit REQ_PLAYLIST_LIMIT_REACHED — vorher lief
        // das bei jedem Medienwechsel und damit regelmaessig ins Limit.
        if (cachedPlaylist.isEmpty()) {
            val now = System.currentTimeMillis()
            if (now - lastPlaylistRequestMs >= PLAYLIST_REQUEST_MIN_INTERVAL_MS) {
                lastPlaylistRequestMs = now
                val plPayload = JSONArray().apply { put("requestPlaylist") }
                webSocket?.send("42$plPayload")
            }
        }
    }

    private fun handleMediaUpdate(data: JSONObject) {
        val paused = data.optBoolean("paused", false)
        val currentTime = data.optDouble("currentTime", data.optDouble("time", 0.0))
        _nowPlaying.value?.let { current ->
            _nowPlaying.value = current.copy(
                currentTimeSeconds = currentTime,
                paused = paused
            )
        }
        _mediaSyncEvent.tryEmit(MediaSyncUpdate(currentTime, paused))
    }

    private fun handlePlaylist(array: JSONArray) {
        val items = mutableListOf<MediaItem>()
        for (i in 0 until array.length()) {
            val entry = array.optJSONObject(i)
            val mediaObj = entry?.optJSONObject("media") ?: entry
            if (mediaObj != null) {
                val mediaId = mediaObj.optString("id", "")
                val uid = entry?.optString("uid", "") ?: ""
                val id = mediaId.ifBlank { uid.ifBlank { i.toString() } }
                val fallbackTitle = entry?.optString("title", "Upcoming Video") ?: "Upcoming Video"
                val title = mediaObj.optString("title", fallbackTitle)
                val type = mediaObj.optString("type", "raw")
                val direct = parseDirectUrlFromData(mediaObj)
                items.add(
                    MediaItem(
                        id = id,
                        title = title,
                        durationSeconds = mediaObj.optDouble("seconds", mediaObj.optDouble("duration", 0.0)),
                        type = type,
                        url = direct.ifBlank { if (id.startsWith("http")) id else null },
                        directUrl = direct
                    )
                )
            }
        }
        cachedPlaylist.clear()
        cachedPlaylist.addAll(items)
        updateUpNextList()
    }

    private fun handleQueueItem(data: JSONObject) {
        val item = data.optJSONObject("item")
        val mediaObj = item?.optJSONObject("media") ?: item
        if (mediaObj != null) {
            val id = mediaObj.optString("id", "")
            val type = mediaObj.optString("type", "raw")
            val direct = parseDirectUrlFromData(mediaObj)
            val newMedia = MediaItem(
                id = id,
                title = mediaObj.optString("title", "Queued Media"),
                durationSeconds = mediaObj.optDouble("seconds", mediaObj.optDouble("duration", 0.0)),
                type = type,
                url = direct.ifBlank { if (id.startsWith("http")) id else null },
                directUrl = direct
            )
            cachedPlaylist.add(newMedia)
            updateUpNextList()
        }
    }

    private fun updateUpNextList() {
        _playlist.value = cachedPlaylist.toList()
        val current = _nowPlaying.value
        if (cachedPlaylist.isNotEmpty()) {
            if (current != null) {
                val currentIndex = cachedPlaylist.indexOfFirst {
                    (it.id.isNotBlank() && it.id == current.id) ||
                    (it.title.isNotBlank() && it.title == current.title)
                }
                if (currentIndex != -1 && currentIndex + 1 < cachedPlaylist.size) {
                    _upNext.value = cachedPlaylist.subList(currentIndex + 1, cachedPlaylist.size).take(4)
                    return
                }
                val remaining = cachedPlaylist.filter {
                    (current.id.isBlank() || it.id != current.id) &&
                    (current.title.isBlank() || it.title != current.title)
                }
                if (remaining.isNotEmpty()) {
                    _upNext.value = remaining.take(4)
                    return
                }
            }
            _upNext.value = cachedPlaylist.drop(1).take(4)
        } else {
            _upNext.value = emptyList()
        }
    }

    private fun parseDirectUrlFromData(data: JSONObject): String {
        val meta = data.optJSONObject("meta")
        if (meta != null) {
            val direct = meta.optJSONObject("direct")
            if (direct != null) {
                val keys = direct.keys()
                val qualities = mutableListOf<String>()
                while (keys.hasNext()) {
                    qualities.add(keys.next())
                }
                qualities.sortByDescending { k ->
                    k.replace(Regex("\\D"), "").toIntOrNull() ?: 0
                }
                for (q in qualities) {
                    val arr = direct.optJSONArray(q)
                    if (arr != null && arr.length() > 0) {
                        val item = arr.optJSONObject(0)
                        val link = item?.optString("link", "") ?: ""
                        if (link.isNotEmpty()) return link
                    }
                }
            }
            val directStr = meta.optString("direct", "")
            if (directStr.isNotEmpty() && directStr.startsWith("http")) return directStr
        }
        val id = data.optString("id", "")
        if (id.startsWith("http")) return id
        return ""
    }

    private fun startPingLoop() {
        pingRunnable = object : Runnable {
            override fun run() {
                // Engine.io client ping: "2"
                webSocket?.send("2")
                handler.postDelayed(this, 25000)
            }
        }
        handler.post(pingRunnable!!)
    }

    private fun stopPingLoop() {
        pingRunnable?.let { handler.removeCallbacks(it) }
        pingRunnable = null
    }

    private fun scheduleReconnect() {
        if (isIntentionallyClosed) return

        reconnectAttempt++
        _connectionStatus.value = ConnectionStatus.RECONNECTING
        val backoffMs = minOf(30000L, 2000L * (1 shl minOf(reconnectAttempt, 4)))
        Log.d(TAG, "Scheduling auto-reconnect in ${backoffMs}ms (Attempt $reconnectAttempt)")

        reconnectRunnable = Runnable {
            if (!isIntentionallyClosed && _connectionStatus.value != ConnectionStatus.LIVE) {
                connect(currentRoomName)
            }
        }
        handler.postDelayed(reconnectRunnable!!, backoffMs)
    }

    private fun cancelReconnect() {
        reconnectRunnable?.let { handler.removeCallbacks(it) }
        reconnectRunnable = null
    }

    private fun disconnectInternal() {
        stopPingLoop()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    fun disconnect() {
        isIntentionallyClosed = true
        cancelReconnect()
        disconnectInternal()
        _connectionStatus.value = ConnectionStatus.OFFLINE
    }
}
