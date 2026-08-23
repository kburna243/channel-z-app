package com.example.player

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.AppSettings
import com.example.data.model.ChannelEmote
import com.example.data.model.ChannelUser
import com.example.data.model.ChatLayout
import com.example.data.model.ChatMessage
import com.example.data.model.LoginState
import com.example.data.model.ConnectionStatus
import com.example.data.model.MediaItem
import com.example.data.model.MediaSyncUpdate
import com.example.data.model.MovieInfo
import com.example.data.model.QueueScheduleItem
import com.example.data.model.SettingsPage
import com.example.ui.theme.applyPalette
import com.example.data.repository.SettingsRepository
import com.example.data.movie.MovieInfoRepository
import com.example.data.scraper.DataScraper
import com.example.data.scraper.MetadataOverlayState
import com.example.data.socket.CyTubeSocketClient
import com.example.ui.player.extractYouTubeId
import com.example.data.model.WebQueueOtpState
import com.example.data.webqueue.WebQueueApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "PlayerViewModel"

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepo = SettingsRepository(application)
    val settings: StateFlow<AppSettings> = settingsRepo.settings

    val playerManager = VideoPlayerManager(
        context = application,
        scope = viewModelScope
    )

    private val socketClient = CyTubeSocketClient(viewModelScope)
    val dataScraper = DataScraper(viewModelScope)
    val webQueueClient = WebQueueApiClient(settingsRepo)
    private val movieInfoRepo = MovieInfoRepository()

    private val _movieInfo = MutableStateFlow<MovieInfo?>(null)
    val movieInfo: StateFlow<MovieInfo?> = _movieInfo.asStateFlow()

    private val _isTriviaVisible = MutableStateFlow(false)
    val isTriviaVisible: StateFlow<Boolean> = _isTriviaVisible.asStateFlow()

    private val _isTriviaLoading = MutableStateFlow(false)
    val isTriviaLoading: StateFlow<Boolean> = _isTriviaLoading.asStateFlow()

    private var movieInfoJob: Job? = null
    private var webQueuePollingJob: Job? = null

    val connectionStatus: StateFlow<ConnectionStatus> = socketClient.connectionStatus
    val nowPlaying: StateFlow<MediaItem?> = socketClient.nowPlaying
    val upNext: StateFlow<List<MediaItem>> = socketClient.upNext
    val userCount: StateFlow<Int> = socketClient.userCount
    val chatMessages: StateFlow<List<ChatMessage>> = socketClient.chatMessages
    val users: StateFlow<List<ChannelUser>> = socketClient.users
    val emotes: StateFlow<List<ChannelEmote>> = socketClient.emotes
    val loginState: StateFlow<LoginState> = socketClient.loginState
    val queueScheduleItems: StateFlow<List<QueueScheduleItem>> = dataScraper.queueScheduleItems
    val mediaSyncEvent: SharedFlow<MediaSyncUpdate> = socketClient.mediaSyncEvent

    private val _otpState = MutableStateFlow<WebQueueOtpState>(WebQueueOtpState.Idle)
    val otpState: StateFlow<WebQueueOtpState> = _otpState.asStateFlow()

    private val _isFirstRunDialogOpen = MutableStateFlow(!settingsRepo.isFirstRunCompleted() && settingsRepo.chatCredentials() == null)
    val isFirstRunDialogOpen: StateFlow<Boolean> = _isFirstRunDialogOpen.asStateFlow()

    private val _webQueueScheduleItems = MutableStateFlow<List<QueueScheduleItem>>(emptyList())
    val webQueueScheduleItems: StateFlow<List<QueueScheduleItem>> = _webQueueScheduleItems.asStateFlow()

    private val _webQueueNextSchedule = MutableStateFlow<String?>(null)
    val webQueueNextSchedule: StateFlow<String?> = _webQueueNextSchedule.asStateFlow()

    val metadataOverlayState: StateFlow<MetadataOverlayState> = combine(
        socketClient.nowPlaying,
        socketClient.upNext,
        socketClient.playlist,
        _webQueueScheduleItems,
        _webQueueNextSchedule,
        socketClient.userCount,
        socketClient.motd,
        settings
    ) { args: Array<Any?> ->
        val now = args[0] as? MediaItem
        @Suppress("UNCHECKED_CAST")
        val socketNext = args[1] as? List<MediaItem> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val socketPlaylist = args[2] as? List<MediaItem> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val webQueueItems = args[3] as? List<QueueScheduleItem> ?: emptyList()
        val webNextSched = args[4] as? String
        val users = args[5] as? Int ?: 0
        val motdText = args[6] as? String
        val cfg = args[7] as? AppSettings ?: AppSettings()

        val socketCandidates = if (socketNext.isNotEmpty()) socketNext else socketPlaylist
        val socketQueue = buildQueueScheduleFromSocket(now, socketCandidates)

        val finalNext = if (socketNext.isNotEmpty()) socketNext else webQueueItems.map {
            MediaItem(id = it.mediaId, title = it.title, durationSeconds = it.durationSeconds.toDouble())
        }
        val finalQueueItems = if (webQueueItems.isNotEmpty()) webQueueItems else socketQueue

        val scheduleTitle = if (!webNextSched.isNullOrBlank()) "Upcoming Marathon / Event" else null
        val scheduleText = webNextSched
        val isFallback = finalQueueItems.isEmpty() && !scheduleText.isNullOrBlank()

        MetadataOverlayState(
            nowPlaying = now,
            upNext = finalNext.take(4),
            queueItems = finalQueueItems,
            channelName = cfg.roomName,
            userCount = users,
            isLoading = now == null,
            redditScheduleTitle = scheduleTitle,
            redditScheduleText = scheduleText,
            isRedditFallback = isFallback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MetadataOverlayState()
    )

    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val isBuffering: StateFlow<Boolean> = playerManager.isBuffering
    val playerErrorMessage: StateFlow<String?> = playerManager.playerError
    val isMuted: StateFlow<Boolean> = playerManager.isMuted

    private val _isMetadataVisible = MutableStateFlow(false)
    val isMetadataVisible: StateFlow<Boolean> = _isMetadataVisible.asStateFlow()

    private val _isUpNextVisible = MutableStateFlow(false)
    val isUpNextVisible: StateFlow<Boolean> = _isUpNextVisible.asStateFlow()

    private val _isRemoteHintsVisible = MutableStateFlow(true)
    val isRemoteHintsVisible: StateFlow<Boolean> = _isRemoteHintsVisible.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    // Unterseite der Einstellungen. Liegt im ViewModel, damit die Zurueck-Taste sie kennt und
    // erst die Unterseite schliesst, statt gleich das ganze Menue.
    private val _settingsPage = MutableStateFlow(SettingsPage.MAIN)
    val settingsPage: StateFlow<SettingsPage> = _settingsPage.asStateFlow()

    private val _showExitDialog = MutableStateFlow(false)
    val showExitDialog: StateFlow<Boolean> = _showExitDialog.asStateFlow()

    private var metadataDismissJob: Job? = null
    private var upNextDismissJob: Job? = null
    private var remoteHintsDismissJob: Job? = null

    init {
        connectSocket()

        if (settings.value.customStreamUrl.isNotBlank()) {
            playerManager.loadMedia(null, settings.value.customStreamUrl)
        }

        // Listen for media changes from CyTube to trigger 5-second metadata overlay & load stream into player
        viewModelScope.launch {
            socketClient.mediaChangedEvent.collect { mediaItem ->
                showMetadataOverlay()
                playerManager.loadMedia(mediaItem, settings.value.customStreamUrl)
                loadMovieInfo(mediaItem.title, mediaItem)
            }
        }

        // Listen for media sync updates (time drift correction & play/pause synchronization from CyTube)
        viewModelScope.launch {
            socketClient.mediaSyncEvent.collect { sync ->
                if (nowPlaying.value?.isWebStream != true) {
                    playerManager.syncPosition(sync.currentTimeSeconds, sync.paused)
                }
            }
        }

        // Zugangsdaten erst wegschreiben, wenn der Kanal sie bestaetigt hat — sonst
        // wuerde sich ein vertipptes Passwort festbrennen und bei jedem Start still
        // scheitern. Die stille Wiederaufnahme nach Reconnects geht nicht durch
        // login(), braucht hier also nichts zu speichern (steht laengst fest).
        viewModelScope.launch {
            socketClient.loginState.collect { state ->
                if (state is LoginState.LoggedIn) {
                    pendingCredentials?.let { (name, pw) ->
                        settingsRepo.saveChatCredentials(name, pw)
                        _savedChatUsername.value = name
                    }
                    pendingCredentials = null
                }
            }
        }

        // Magic WebQueue OTP Listener: Automatisches Abfangen des Kryten-Bestätigungscodes
        viewModelScope.launch {
            socketClient.magicOtpCodeEvent.collect { code ->
                val currentOtpState = _otpState.value
                val username = when (currentOtpState) {
                    is WebQueueOtpState.WaitingForCode -> currentOtpState.username
                    is WebQueueOtpState.RequestingOtp -> pendingCredentials?.first ?: _savedChatUsername.value
                    else -> pendingCredentials?.first ?: _savedChatUsername.value
                }.ifBlank { _savedChatUsername.value }

                if (username.isNotBlank() && code.isNotBlank()) {
                    Log.i(TAG, "✨ Magic OTP triggered for '$username' with code '$code'")
                    _otpState.value = WebQueueOtpState.Verifying(username, code)
                    val verifyResult = webQueueClient.verifyOtp(username, code)
                    if (verifyResult.isSuccess) {
                        Log.i(TAG, "🎉 WebQueue Magic Login successful!")
                        _otpState.value = WebQueueOtpState.Success(username)
                        settingsRepo.setFirstRunCompleted(true)
                        startWebQueuePolling()
                    } else {
                        val err = verifyResult.exceptionOrNull()?.message ?: "OTP verification failed"
                        _otpState.value = WebQueueOtpState.Failed(err)
                    }
                }
            }
        }

        // Start WebQueue polling loop
        startWebQueuePolling()

        // Initial auto-hide timer for Remote Hints (6s on startup)
        scheduleRemoteHintsHide(6000L)
    }

    fun getExoPlayer(): ExoPlayer? = playerManager.getPlayer()

    /**
     * Holt die Angaben zum laufenden Film nach. Laeuft nebenher: kommt nichts zurueck,
     * bleibt die Anzeige einfach so, wie sie ohne diese Daten aussieht.
     */
    private fun loadMovieInfo(rawTitle: String, media: MediaItem? = null) {
        movieInfoJob?.cancel()
        _movieInfo.value = null
        _isTriviaVisible.value = false
        if (!settings.value.movieInfoEnabled || rawTitle.isBlank()) return

        movieInfoJob = viewModelScope.launch {
            // 1. Zuerst WebQueue / Channel-Z Katalog abfragen (kennt exacten imdb_tt Code und Beschreibung)
            val catalogResult = webQueueClient.searchCatalog(rawTitle)
            val catalogItem = catalogResult.getOrNull()
            val fromCatalog = if (catalogItem?.imdbTt != null) {
                movieInfoRepo.lookupFromImdbId(catalogItem.imdbTt, catalogItem.title)
            } else null

            // 2. Bei YouTube zuerst dort nachfragen wenn type == yt
            val fromYouTube = if (fromCatalog == null && media?.type?.lowercase() == "yt") {
                movieInfoRepo.lookupYouTube(extractYouTubeId(media.id))
            } else null

            val info = fromCatalog
                ?: movieInfoRepo.lookup(rawTitle, useImdb = settings.value.imdbEnabled)
                ?: fromYouTube

            if (info != null) {
                val finalInfo = if (info.plot.isNullOrBlank() && !catalogItem?.description.isNullOrBlank()) {
                    info.copy(plot = catalogItem.description)
                } else info
                Log.d(TAG, "Filminfo gefunden: ${finalInfo.title} (${finalInfo.year}) imdb=${finalInfo.imdbId}")
                _movieInfo.value = finalInfo
            }
        }
    }

    fun toggleTrivia() {
        if (_isTriviaVisible.value) {
            _isTriviaVisible.value = false
            return
        }
        val info = _movieInfo.value ?: return
        _isTriviaVisible.value = true
        if (info.trivia.isNotEmpty() || info.imdbId == null || !settings.value.imdbEnabled) return

        viewModelScope.launch {
            _isTriviaLoading.value = true
            val items = movieInfoRepo.loadTrivia(info.imdbId)
            _isTriviaLoading.value = false
            _movieInfo.value = _movieInfo.value?.copy(trivia = items)
        }
    }

    fun hideTrivia() {
        _isTriviaVisible.value = false
    }

    fun toggleMovieInfo() {
        settingsRepo.updateSettings { it.copy(movieInfoEnabled = !it.movieInfoEnabled) }
        if (!settings.value.movieInfoEnabled) {
            movieInfoJob?.cancel()
            _movieInfo.value = null
        } else {
            nowPlaying.value?.title?.let { loadMovieInfo(it) }
        }
    }

    fun toggleImdb() {
        settingsRepo.updateSettings { it.copy(imdbEnabled = !it.imdbEnabled) }
        nowPlaying.value?.title?.let { loadMovieInfo(it) }
    }

    private fun connectSocket() {
        // Gespeichertes Chat-Konto (Full-Ausgabe) mitgeben: nach dem Beitritt meldet
        // sich die Sitzung selbst an, Gaeste wie Konten. Ohne Gespeichertes ist es null
        // und der Socket bleibt abgemeldet.
        socketClient.connect(settings.value.roomName, settingsRepo.chatCredentials())
    }

    fun retryConnection() {
        Log.d(TAG, "Retrying CyTube Socket connection to room: ${settings.value.roomName}")
        socketClient.disconnect()
        connectSocket()
    }

    fun showMetadataOverlay() {
        _isMetadataVisible.value = true
        metadataDismissJob?.cancel()
        metadataDismissJob = viewModelScope.launch {
            delay(5000L)
            _isMetadataVisible.value = false
        }
    }

    fun hideMetadataOverlay() {
        metadataDismissJob?.cancel()
        _isMetadataVisible.value = false
    }

    fun toggleUpNext() {
        if (_isUpNextVisible.value) {
            hideUpNext()
        } else {
            showUpNext()
        }
    }

    fun showUpNext() {
        hideMetadataOverlay()
        _isUpNextVisible.value = true
        upNextDismissJob?.cancel()
        upNextDismissJob = viewModelScope.launch {
            delay(15000L)
            _isUpNextVisible.value = false
        }
    }

    fun hideUpNext() {
        upNextDismissJob?.cancel()
        _isUpNextVisible.value = false
    }

    fun onRemoteActivity() {
        _isRemoteHintsVisible.value = true
        scheduleRemoteHintsHide(5000L)
    }

    private fun scheduleRemoteHintsHide(delayMillis: Long) {
        remoteHintsDismissJob?.cancel()
        remoteHintsDismissJob = viewModelScope.launch {
            delay(delayMillis)
            _isRemoteHintsVisible.value = false
        }
    }

    fun toggleMute() {
        playerManager.toggleMute()
    }

    // ------------------------------------------------------------------ Chat (nur Full)

    private val _isUserListVisible = MutableStateFlow(false)
    val isUserListVisible: StateFlow<Boolean> = _isUserListVisible.asStateFlow()

    private val _isComposerOpen = MutableStateFlow(false)
    val isComposerOpen: StateFlow<Boolean> = _isComposerOpen.asStateFlow()

    private val _chatLayout = MutableStateFlow(ChatLayout.SUBTITLE)
    val chatLayout: StateFlow<ChatLayout> = _chatLayout.asStateFlow()

    /** Ansicht vor dem Wechsel in die Vollbild-Chat-Ansicht, fuer den Weg zurueck. */
    private var layoutBeforeChatOnly: ChatLayout = ChatLayout.SUBTITLE

    /** Zugang, der gerade geprueft wird — erst nach Bestaetigung des Kanals persistiert. */
    private var pendingCredentials: Pair<String, String>? = null

    /** Gespeicherter Kontoname fuer die Anzeige; das Passwort bleibt im Repository. */
    private val _savedChatUsername = MutableStateFlow(settingsRepo.chatCredentials()?.first.orEmpty())
    val savedChatUsername: StateFlow<String> = _savedChatUsername.asStateFlow()

    fun login(username: String, password: String) {
        pendingCredentials = username to password
        socketClient.login(username, password)
    }

    fun logout() {
        pendingCredentials = null
        settingsRepo.clearChatCredentials()
        _savedChatUsername.value = ""
        socketClient.logout()
        webQueueClient.logout()
        _webQueueScheduleItems.value = emptyList()
        _webQueueNextSchedule.value = null
        webQueuePollingJob?.cancel()
        _otpState.value = WebQueueOtpState.Idle
    }

    /**
     * Startet den Magic Login:
     * 1. Verbindet / meldet den CyTube-Chat an.
     * 2. Fordert den OTP-Code bei WebQueue (queue.dropsugar.co) an.
     * 3. Kryten schickt den Code per PM -> Socket extrahiert den Code automatisch -> Verify!
     */
    fun startMagicLogin(username: String, password: String = "") {
        val cleanName = username.trim()
        if (cleanName.isBlank()) {
            _otpState.value = WebQueueOtpState.Failed("Please enter your CyTube username")
            return
        }
        _otpState.value = WebQueueOtpState.RequestingOtp
        pendingCredentials = cleanName to password

        viewModelScope.launch {
            // 1. CyTube Socket verbinden / anmelden
            socketClient.connect(settings.value.roomName, cleanName to password)
            if (password.isNotBlank()) {
                socketClient.login(cleanName, password)
            }

            // 2. OTP bei WebQueue anfordern (Kryten schickt PM)
            val requestResult = webQueueClient.requestOtp(cleanName)
            if (requestResult.isFailure) {
                val err = requestResult.exceptionOrNull()?.message ?: "Failed to request code from WebQueue"
                _otpState.value = WebQueueOtpState.Failed(err)
                return@launch
            }

            _otpState.value = WebQueueOtpState.WaitingForCode(cleanName)
        }
    }

    /** Manuelle Eingabe des 6-stelligen Codes, falls gewünscht. */
    fun verifyManualOtp(username: String, code: String) {
        val cleanName = username.trim().ifBlank { _savedChatUsername.value }
        val cleanCode = code.trim()
        if (cleanName.isBlank() || cleanCode.isBlank()) {
            _otpState.value = WebQueueOtpState.Failed("Username and 6-digit code are required")
            return
        }
        _otpState.value = WebQueueOtpState.Verifying(cleanName, cleanCode)
        viewModelScope.launch {
            val result = webQueueClient.verifyOtp(cleanName, cleanCode)
            if (result.isSuccess) {
                _otpState.value = WebQueueOtpState.Success(cleanName)
                settingsRepo.setFirstRunCompleted(true)
                startWebQueuePolling()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Invalid or expired code"
                _otpState.value = WebQueueOtpState.Failed(err)
            }
        }
    }

    fun dismissFirstRunDialog() {
        _isFirstRunDialogOpen.value = false
    }

    fun skipFirstRun() {
        settingsRepo.setFirstRunCompleted(true)
        _isFirstRunDialogOpen.value = false
    }

    fun openAccountLoginDialog() {
        _otpState.value = WebQueueOtpState.Idle
        _isFirstRunDialogOpen.value = true
    }

    fun startWebQueuePolling() {
        webQueuePollingJob?.cancel()
        webQueuePollingJob = viewModelScope.launch {
            Log.d(TAG, "Starting WebQueue polling loop...")
            while (isActive) {
                val queueResult = webQueueClient.fetchQueueState()
                if (queueResult.isSuccess) {
                    val items = queueResult.getOrDefault(emptyList())
                    if (items.isNotEmpty()) {
                        Log.d(TAG, "WebQueue items updated: ${items.size} items in queue")
                        _webQueueScheduleItems.value = items
                    }
                } else {
                    Log.w(TAG, "WebQueue fetchQueueState failed: ${queueResult.exceptionOrNull()?.message}")
                }
                val nextScheduleResult = webQueueClient.fetchNextSchedule()
                if (nextScheduleResult.isSuccess) {
                    val sched = nextScheduleResult.getOrNull()
                    Log.d(TAG, "WebQueue next schedule updated: $sched")
                    _webQueueNextSchedule.value = sched
                }
                delay(15000L)
            }
        }
    }

    fun sendChat(message: String): Boolean = socketClient.sendChat(message)

    fun openComposer() {
        hideMetadataOverlay()
        hideUpNext()
        _isComposerOpen.value = true
    }

    fun closeComposer() {
        _isComposerOpen.value = false
    }

    fun toggleUserList() {
        _isUserListVisible.value = !_isUserListVisible.value
    }

    fun hideUserList() {
        _isUserListVisible.value = false
    }

    /** Reihum durch die Chat-Ansichten, wie ein Druck auf die Layout-Taste. */
    fun cycleChatLayout() {
        val all = ChatLayout.entries
        applyChatLayout(all[(all.indexOf(_chatLayout.value) + 1) % all.size])
    }

    /**
     * Vollbild-Chat fuer Handy und Tablet: hinein wechselt in die Nur-Chat-Ansicht,
     * zurueck in die Ansicht, die vorher aktiv war. Der Knopf sitzt in der
     * Touch-Bedienleiste, weil es dort keine Layout-Taste gibt.
     */
    fun toggleFullChatMode() {
        if (_chatLayout.value == ChatLayout.CHAT_ONLY) {
            applyChatLayout(layoutBeforeChatOnly)
        } else {
            layoutBeforeChatOnly = _chatLayout.value
            applyChatLayout(ChatLayout.CHAT_ONLY)
        }
    }

    private fun applyChatLayout(layout: ChatLayout) {
        _chatLayout.value = layout
        // In der Nur-Chat-Ansicht laeuft kein Video: das Geraet wird zum Chat-Fenster.
        if (layout == ChatLayout.CHAT_ONLY) {
            playerManager.pause()
            playerManager.setMuted(true)
        } else if (playerManager.isMuted.value) {
            playerManager.setMuted(false)
            playerManager.play()
        }
    }

    fun toggleChat() {
        settingsRepo.toggleChat()
    }

    fun updateChatOpacity(opacity: Float) {
        settingsRepo.updateSettings { it.copy(chatBackgroundOpacity = opacity) }
    }

    fun updateChatFontSize(fontSizeSp: Int) {
        settingsRepo.updateSettings { it.copy(chatFontSizeSp = fontSizeSp) }
    }

    fun updateChatMaxLines(lines: Int) {
        settingsRepo.updateSettings { it.copy(chatMaxLines = lines.coerceIn(1, 3)) }
    }

    fun toggleSubtitles() {
        settingsRepo.toggleSubtitles()
    }

    fun updateLanguage(languageCode: String) {
        settingsRepo.updateSettings { it.copy(languageCode = languageCode) }
    }

    fun updateChatAutoHide(seconds: Int) {
        settingsRepo.updateChatAutoHide(seconds)
    }

    fun updateChatTheme(theme: String) {
        settingsRepo.updateChatTheme(theme)
    }

    /**
     * Farbthema umschalten. Die Farben werden hier gesetzt und nicht waehrend des Zeichnens,
     * sonst braeuchte jeder Wechsel einen zusaetzlichen Zeichendurchlauf.
     */
    fun updateAppTheme(id: String) {
        settingsRepo.updateAppTheme(id)
        applyPalette(settingsRepo.settings.value.appTheme)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun openSettings() {
        hideMetadataOverlay()
        hideUpNext()
        _settingsPage.value = SettingsPage.MAIN
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
        _settingsPage.value = SettingsPage.MAIN
    }

    fun openSettingsPage(page: SettingsPage) {
        _settingsPage.value = page
    }

    fun toggleSettings() {
        if (_isSettingsOpen.value) {
            closeSettings()
        } else {
            openSettings()
        }
    }

    fun promptExitDialog() {
        hideMetadataOverlay()
        hideUpNext()
        _showExitDialog.value = true
    }

    fun dismissExitDialog() {
        _showExitDialog.value = false
    }

    fun handleBackPress(): Boolean {
        return when {
            _showExitDialog.value -> {
                dismissExitDialog()
                true
            }
            _isComposerOpen.value -> {
                closeComposer()
                true
            }
            _isUserListVisible.value -> {
                hideUserList()
                true
            }
            _isTriviaVisible.value -> {
                hideTrivia()
                true
            }
            _isSettingsOpen.value && _settingsPage.value != SettingsPage.MAIN -> {
                _settingsPage.value = SettingsPage.MAIN
                true
            }
            _isSettingsOpen.value -> {
                closeSettings()
                true
            }
            _isUpNextVisible.value -> {
                hideUpNext()
                true
            }
            _isMetadataVisible.value -> {
                hideMetadataOverlay()
                true
            }
            else -> {
                promptExitDialog()
                true
            }
        }
    }

    fun playDemoStream() {
        val demoItem = MediaItem(
            id = "dQw4w9WgXcQ",
            title = "Channel-Z Demo Feed",
            durationSeconds = 212.0,
            type = "yt",
            url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            currentTimeSeconds = 0.0,
            paused = false,
            directUrl = ""
        )
        playerManager.loadMedia(demoItem, "")
    }

    private fun buildQueueScheduleFromSocket(
        nowPlaying: MediaItem?,
        upcomingList: List<MediaItem>
    ): List<QueueScheduleItem> {
        if (upcomingList.isEmpty()) return emptyList()

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val nowMs = System.currentTimeMillis()

        val remainingCurrentSec = if (nowPlaying != null && nowPlaying.durationSeconds > 0) {
            val rem = nowPlaying.durationSeconds - nowPlaying.currentTimeSeconds
            if (rem > 0) rem else 0.0
        } else 0.0

        var accumulatedSec = remainingCurrentSec
        val result = mutableListOf<QueueScheduleItem>()

        for (item in upcomingList) {
            val durationSec = item.durationSeconds.toInt()
            val startMs = nowMs + (accumulatedSec * 1000).toLong()
            val startFormatted = timeFormat.format(Date(startMs))
            val durFormatted = formatDurationSec(durationSec)

            result.add(
                QueueScheduleItem(
                    title = item.title,
                    durationSeconds = durationSec,
                    startTimeFormatted = startFormatted,
                    durationFormatted = durFormatted,
                    mediaId = item.id
                )
            )
            accumulatedSec += if (durationSec > 0) durationSec else 180
        }

        return result
    }

    private fun formatDurationSec(seconds: Int): String {
        if (seconds <= 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketClient.disconnect()
        dataScraper.stopScraping()
    }
}
