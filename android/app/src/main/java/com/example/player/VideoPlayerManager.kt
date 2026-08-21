package com.example.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.data.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "VideoPlayerManager"
const val DEMO_FALLBACK_STREAM_URL = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

fun convertGoogleDriveUrl(url: String): String {
    if (url.contains("drive.google.com/file/d/")) {
        val startIndex = url.indexOf("/file/d/") + 8
        val endIndex = url.indexOf("/", startIndex).let { if (it == -1) url.length else it }
        val fileId = url.substring(startIndex, endIndex)
        return "https://drive.google.com/uc?export=download&id=$fileId"
    }
    if (url.contains("drive.google.com/open?id=")) {
        val fileId = url.substringAfter("open?id=").substringBefore("&")
        return "https://drive.google.com/uc?export=download&id=$fileId"
    }
    return url
}

/**
 * Robust Media3 ExoPlayer & Web Media Manager for CyTube live streams, HLS (.m3u8), DASH (.mpd), Google Drive, and YouTube feeds.
 */
@OptIn(UnstableApi::class)
/**
 * Vorlauf in Sekunden, mit dem der Player gegenueber der vom Kanal gemeldeten Zeit startet.
 *
 * CyTube nennt die Position, an der der Kanal *jetzt* steht. Bis der Player geladen, gepuffert
 * und angelaufen ist, vergehen ein bis drei Sekunden — der Kanal ist dann laengst weiter, und
 * die App haengt fuer den Rest des Videos hinterher. Aufgefallen ist das als rund zwei Sekunden
 * Versatz gegenueber der CyTube-Seite im Browser.
 *
 * Der Wert bleibt bewusst kleiner als die Drift-Schwelle von 3 s in syncPlayback: sonst wuerde
 * die laufende Korrektur den Vorlauf sofort wieder einkassieren.
 *
 * Dieselbe Loesung wie in spudzareneat/grindhouse-tv (web/src/player/leadtime.js), dort mit
 * demselben Vorgabewert von 2 s.
 */
const val PLAYBACK_LEAD_SECONDS = 2.0

/**
 * Der Vorlauf gilt nur beim Einstieg mitten in ein laufendes Video.
 *
 * Faengt das Video gerade erst an — beim Videowechsel meldet CyTube eine Position nahe null —
 * gibt es nichts aufzuholen: der Player startet ja gemeinsam mit dem Kanal. Der Vorlauf wuerde
 * dann nur die ersten zwei Sekunden ueberspringen und bei ExoPlayer einen Suchlauf direkt nach
 * dem Laden ausloesen, der den Decoder leert und als Aussetzer zu sehen ist.
 */
fun leadFor(currentTimeSeconds: Double): Double =
    if (currentTimeSeconds > 5.0) PLAYBACK_LEAD_SECONDS else 0.0

class VideoPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) : DefaultLifecycleObserver {

    private var exoPlayer: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    private val _currentMedia = MutableStateFlow<MediaItem?>(null)
    val currentMedia: StateFlow<MediaItem?> = _currentMedia.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var currentStreamUrl: String? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var shouldPlayWhenReady = true
    private var lastPlaybackPosition: Long = 0L
    private var lastSeekTimestampMs: Long = 0L
    private var mediaLoadedTimestampMs: Long = 0L

    init {
        initializePlayer()
    }

    /**
     * Initializes the ExoPlayer instance with optimal streaming audio attributes, cross-protocol redirects, and listeners.
     */
    fun initializePlayer() {
        if (exoPlayer != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 11; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        exoPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                playWhenReady = shouldPlayWhenReady
                repeatMode = Player.REPEAT_MODE_ALL

                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        if (_currentMedia.value?.isWebStream != true) {
                            _isPlaying.value = playing
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        _playbackState.value = state
                        if (_currentMedia.value?.isWebStream != true) {
                            when (state) {
                                Player.STATE_BUFFERING -> {
                                    _isBuffering.value = true
                                    _playerError.value = null
                                }
                                Player.STATE_READY -> {
                                    _isBuffering.value = false
                                    _playerError.value = null
                                    reconnectAttempts = 0
                                }
                                Player.STATE_ENDED -> {
                                    _isBuffering.value = false
                                    _isPlaying.value = false
                                }
                                Player.STATE_IDLE -> {
                                    _isBuffering.value = false
                                    _isPlaying.value = false
                                }
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        if (_currentMedia.value?.isWebStream != true) {
                            Log.e(TAG, "ExoPlayer playback error [code: ${error.errorCode}]: ${error.message}", error)
                            _playerError.value = error.localizedMessage ?: "Playback error"
                            _isBuffering.value = false
                            _isPlaying.value = false
                            handlePlaybackError(error)
                        }
                    }
                })
            }

        currentStreamUrl?.let { url ->
            loadStreamInternal(url, resumePosition = lastPlaybackPosition)
        }
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    /**
     * Sets and prepares media for playback based on CyTube MediaItem metadata or custom stream URLs.
     */
    fun loadMedia(media: MediaItem?, customStreamUrl: String? = null) {
        _currentMedia.value = media
        reconnectAttempts = 0
        reconnectJob?.cancel()
        val now = System.currentTimeMillis()
        mediaLoadedTimestampMs = now
        lastSeekTimestampMs = now

        if (media == null && customStreamUrl.isNullOrBlank()) {
            currentStreamUrl = null
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            _isPlaying.value = false
            _isBuffering.value = false
            _playerError.value = null
            return
        }

        if (!customStreamUrl.isNullOrBlank()) {
            currentStreamUrl = customStreamUrl
            loadStreamInternal(customStreamUrl)
            return
        }

        if (media != null && media.isWebStream) {
            // Web stream (YouTube / Twitch) is handled in WebView surface
            Log.d(TAG, "Web stream detected: ${media.title} (${media.id}). Handled via WebView surface.")
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            _isPlaying.value = !media.paused
            _isBuffering.value = false
            _playerError.value = null
            return
        }

        val rawUrl = when {
            media?.directUrl?.isNotBlank() == true -> media.directUrl
            media?.url?.isNotBlank() == true -> media.url
            media?.id?.startsWith("http://") == true || media?.id?.startsWith("https://") == true -> media.id
            else -> null
        }

        if (rawUrl != null) {
            val streamUrl = convertGoogleDriveUrl(rawUrl)
            currentStreamUrl = streamUrl
            val mediaPos = media?.currentTimeSeconds ?: 0.0
            val initialSeek = (mediaPos + leadFor(mediaPos)) * 1000
            loadStreamInternal(streamUrl, resumePosition = initialSeek.toLong())
        } else {
            Log.d(TAG, "No direct streamable URL for media: ${media?.title} (type: ${media?.type})")
            currentStreamUrl = null
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            _isPlaying.value = false
            _isBuffering.value = false
            _playerError.value = null
        }
    }

    /**
     * Builds and sets the ExoPlayer MediaItem with MIME-type inference (HLS, DASH, MP4, WebM, MKV).
     */
    private fun loadStreamInternal(url: String, resumePosition: Long = 0L) {
        val player = exoPlayer ?: return

        try {
            val uri = Uri.parse(url)
            val builder = ExoMediaItem.Builder().setUri(uri)

            val lowerUrl = url.lowercase()
            when {
                lowerUrl.contains(".m3u8") || lowerUrl.contains("/hls/") || lowerUrl.contains("live.m3u8") || lowerUrl.contains("manifest") -> {
                    builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                lowerUrl.contains(".mpd") || lowerUrl.contains("/dash/") -> {
                    builder.setMimeType(MimeTypes.APPLICATION_MPD)
                }
                lowerUrl.endsWith(".mp4") -> {
                    builder.setMimeType(MimeTypes.VIDEO_MP4)
                }
                lowerUrl.endsWith(".webm") -> {
                    builder.setMimeType(MimeTypes.VIDEO_WEBM)
                }
                lowerUrl.endsWith(".mkv") -> {
                    builder.setMimeType(MimeTypes.VIDEO_MATROSKA)
                }
            }

            val exoMediaItem = builder.build()
            player.clearMediaItems()
            player.setMediaItem(exoMediaItem)
            if (resumePosition > 0L) {
                player.seekTo(resumePosition)
            }
            player.prepare()
            player.playWhenReady = true
            player.play()
            _isPlaying.value = true
            _playerError.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load stream url: $url", e)
            _playerError.value = "Failed to load stream: ${e.localizedMessage}"
            _isPlaying.value = false
            _isBuffering.value = false
        }
    }

    /**
     * Sync time drift & playback state from CyTube updates (3.0s threshold like CyTube sync).
     */
    fun syncPosition(targetSeconds: Double, paused: Boolean? = null) {
        val player = exoPlayer ?: return
        if (_currentMedia.value?.isWebStream == true) return

        // 1. Play / Pause state sync
        if (paused != null) {
            if (paused && player.playWhenReady) {
                pause()
            } else if (!paused && !player.playWhenReady && shouldPlayWhenReady) {
                play()
            }
        }

        // 2. Continuous Sync & Adaptive Drift Reconciliation
        val now = System.currentTimeMillis()
        // Grace period: Während des ersten Ladens (25s nach Videowechsel) oder beim Puffern KEINE Korrekturen
        if (now - mediaLoadedTimestampMs < 25000L) return
        if (player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_IDLE) return

        // Lead-Time Kompensation (analog spudzareneat/grindhouse-tv leadtime.js):
        // +2s Vorlauf kompensiert Netzwerk- und Decoderlatenz gegenüber dem CyTube-Server.
        val effectiveTargetSeconds = targetSeconds + PLAYBACK_LEAD_SECONDS
        val targetMs = (effectiveTargetSeconds * 1000).toLong()
        if (targetMs >= 0) {
            val currentMs = player.currentPosition
            val diffMs = targetMs - currentMs // positiv = wir hängen hinterher, negativ = wir sind voraus
            val absDiffMs = Math.abs(diffMs)

            if (absDiffMs > 120000L && (now - lastSeekTimestampMs > 45000L)) {
                // Nur bei extremen Desyncs (> 2 Minuten, z.B. manueller Playlist-Sprung): Harter Seek
                lastSeekTimestampMs = now
                Log.d(TAG, "Syncing major desync (>120s) via seekTo: local=${currentMs}ms, target=${targetMs}ms (diff=${diffMs}ms)")
                player.setPlaybackSpeed(1.0f)
                player.seekTo(targetMs)
            } else if (absDiffMs >= 15000L && absDiffMs <= 120000L) {
                // Große Abweichung (15s bis 120s): Stärkere Geschwindigkeitsanpassung (1.12x / 0.88x)
                // Holt 1.2s Drift alle 10s auf — komplett ohne Decoder-Flush oder Bild-Einfrieren!
                val speed = if (diffMs > 0) 1.12f else 0.88f
                if (Math.abs(player.playbackParameters.speed - speed) > 0.01f) {
                    Log.d(TAG, "Fast-nudging playback speed to ${speed}x (drift: ${diffMs}ms)")
                    player.setPlaybackSpeed(speed)
                }
            } else if (absDiffMs >= 4000L && absDiffMs < 15000L) {
                // Mittlere Abweichung (4s bis 15s): Zügige Geschwindigkeitsanpassung (1.06x / 0.94x)
                val speed = if (diffMs > 0) 1.06f else 0.94f
                if (Math.abs(player.playbackParameters.speed - speed) > 0.01f) {
                    Log.d(TAG, "Medium-nudging playback speed to ${speed}x (drift: ${diffMs}ms)")
                    player.setPlaybackSpeed(speed)
                }
            } else if (absDiffMs >= 1200L && absDiffMs < 4000L) {
                // Sanfte Abweichung (1.2s bis 4s): Sanfte Geschwindigkeitsanpassung (1.02x / 0.98x)
                val speed = if (diffMs > 0) 1.02f else 0.98f
                if (Math.abs(player.playbackParameters.speed - speed) > 0.01f) {
                    Log.d(TAG, "Soft-nudging playback speed to ${speed}x (drift: ${diffMs}ms)")
                    player.setPlaybackSpeed(speed)
                }
            } else if (absDiffMs < 1200L) {
                // Perfekt im Sync (< 1.2s): Normalgeschwindigkeit 1.0x
                if (player.playbackParameters.speed != 1.0f) {
                    player.setPlaybackSpeed(1.0f)
                }
            }
        }
    }

    fun playDemoStream() {
        currentStreamUrl = DEMO_FALLBACK_STREAM_URL
        reconnectAttempts = 0
        loadStreamInternal(DEMO_FALLBACK_STREAM_URL)
    }

    fun play() {
        shouldPlayWhenReady = true
        _isPlaying.value = true
        exoPlayer?.play()
    }

    fun pause() {
        shouldPlayWhenReady = false
        _isPlaying.value = false
        exoPlayer?.pause()
    }

    fun togglePlayPause() {
        if (_currentMedia.value?.isWebStream == true) {
            _isPlaying.value = !_isPlaying.value
        } else {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    pause()
                } else {
                    play()
                }
            }
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        exoPlayer?.volume = if (_isMuted.value) 0f else 1f
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        exoPlayer?.volume = if (muted) 0f else 1f
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun retry() {
        reconnectAttempts = 0
        currentStreamUrl?.let { loadStreamInternal(it) } ?: playDemoStream()
    }

    private fun handlePlaybackError(error: PlaybackException) {
        val cause = error.cause
        val isUnrecognizedFormat = cause is androidx.media3.exoplayer.source.UnrecognizedInputFormatException ||
                error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED

        if (isUnrecognizedFormat) {
            Log.w(TAG, "Stream format is unsupported by ExoPlayer: $currentStreamUrl")
            _playerError.value = "Format not directly streamable (${_currentMedia.value?.title ?: "Stream"})"
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            _isPlaying.value = false
            _isBuffering.value = false
            return
        }

        reconnectAttempts++
        if (reconnectAttempts > 3) {
            Log.w(TAG, "Max playback reconnect attempts reached (3)")
            return
        }

        val delayMillis = (Math.pow(2.0, reconnectAttempts.toDouble()) * 1000).toLong()
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delayMillis)
            exoPlayer?.let { player ->
                player.prepare()
                player.play()
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        onStart()
    }

    fun onStart() {
        if (exoPlayer == null) {
            initializePlayer()
        } else {
            if (shouldPlayWhenReady && _currentMedia.value?.isWebStream != true) {
                exoPlayer?.play()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        onStop()
    }

    fun onStop() {
        exoPlayer?.let { player ->
            lastPlaybackPosition = player.currentPosition
            player.pause()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
    }

    fun release() {
        reconnectJob?.cancel()
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            player.release()
        }
        exoPlayer = null
        _isPlaying.value = false
        _isBuffering.value = false
    }
}
