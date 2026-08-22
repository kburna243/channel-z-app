package com.example.data.scraper

import android.util.Log
import com.example.data.model.MediaItem
import com.example.data.model.QueueScheduleItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "DataScraper"

/**
 * UI State object exposing Now Playing, Up Next, and Reddit EPG broadcast fallback metadata.
 */
data class MetadataOverlayState(
    val nowPlaying: MediaItem? = null,
    val upNext: List<MediaItem> = emptyList(),
    val queueItems: List<QueueScheduleItem> = emptyList(),
    val channelName: String = "Channel-Z",
    val userCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastUpdatedTimestamp: Long = 0L,
    val redditScheduleTitle: String? = null,
    val redditScheduleText: String? = null,
    val isRedditFallback: Boolean = false
)

/**
 * DataScraper responsible for fetching live CyTube schedule & upcoming movie lineups
 * directly from https://cytubot.onrender.com/schedule with an automatic smart fallback
 * to r/420Grindhouse Reddit EPG broadcast feeds.
 */
class DataScraper(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()
) {
    private val _scheduleItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val scheduleItems: StateFlow<List<MediaItem>> = _scheduleItems.asStateFlow()

    private val _queueScheduleItems = MutableStateFlow<List<QueueScheduleItem>>(emptyList())
    val queueScheduleItems: StateFlow<List<QueueScheduleItem>> = _queueScheduleItems.asStateFlow()

    private val _redditScheduleTitle = MutableStateFlow<String?>(null)
    val redditScheduleTitle: StateFlow<String?> = _redditScheduleTitle.asStateFlow()

    private val _redditScheduleText = MutableStateFlow<String?>(null)
    val redditScheduleText: StateFlow<String?> = _redditScheduleText.asStateFlow()

    private val _isRedditFallback = MutableStateFlow(false)
    val isRedditFallback: StateFlow<Boolean> = _isRedditFallback.asStateFlow()

    /**
     * Sendeplan-Quellen in Vorzugsreihenfolge.
     *
     * Der Kanal betreibt seinen Schedule-Bot selbst — cytu.be laedt ihn ueber channelOpts.externaljs
     * als Iframe von bot.420grindhouseserver.com, und dort liegt derselbe /schedule-Endpunkt mit
     * identischem JSON-Format. Die frueher fest verdrahtete Render-Instanz bleibt als Zweitquelle
     * stehen, sie war zeitweise vom Anbieter abgeschaltet.
     */
    private val scheduleEndpoints = emptyList<String>()

    private var pollingJob: Job? = null

    init {
        // Channel-Z uses native CyTube WebSocket playlist events instead of external scrapers
    }

    /**
     * Starts periodic schedule fetching (no-op for Channel-Z).
     */
    fun startScraping(pollIntervalMillis: Long = 15000L) {
        // No-op
    }

    fun stopScraping() {
        pollingJob?.cancel()
        pollingJob = null
    }

    suspend fun fetchSchedule() = withContext(ioDispatcher) {
        // No-op
    }

    /**
     * Fetches real-time queue & schedule from https://cytubot.onrender.com/schedule
     */
    private suspend fun fetchScheduleFromCytubot(): Boolean = withContext(ioDispatcher) {
        for (base in scheduleEndpoints) {
            try {
                val url = "$base?t=${System.currentTimeMillis()}"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "ChannelZ-Player/1.0")
                    .header("Cache-Control", "no-cache")
                    .build()

                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    if (body.isNotBlank() && body.trim().startsWith("{")) {
                        val parsedCount = parseCytubotScheduleJson(body)
                        if (parsedCount > 0) {
                            _isRedditFallback.value = false
                            _redditScheduleTitle.value = null
                            _redditScheduleText.value = null
                            return@withContext true
                        }
                    }
                } else {
                    Log.w(TAG, "Schedule endpoint $base returned HTTP ${resp.code}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Schedule endpoint $base unavailable: ${e.message}")
            }
        }
        Log.w(TAG, "No schedule endpoint answered — falling back to Reddit")
        false
    }

    private fun parseCytubotScheduleJson(jsonStr: String): Int {
        try {
            val root = JSONObject(jsonStr)
            val playlist = root.optJSONArray("playlist") ?: return 0
            val remainingSec = root.optDouble("remainingSeconds", 0.0)

            val mediaItems = mutableListOf<MediaItem>()
            val queueItems = mutableListOf<QueueScheduleItem>()

            var accumulatedSeconds = if (remainingSec > 0.0) remainingSec else 0.0
            val nowMs = System.currentTimeMillis()
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            for (i in 0 until playlist.length()) {
                val itemObj = playlist.optJSONObject(i) ?: continue
                val mediaObj = itemObj.optJSONObject("media") ?: itemObj

                val title = mediaObj.optString("title", "Upcoming Video")
                val seconds = mediaObj.optInt("seconds", 0)
                val id = mediaObj.optString("id", "")
                val type = mediaObj.optString("type", "raw")

                val estStartTimeMs = nowMs + (accumulatedSeconds * 1000).toLong()
                val startTimeStr = timeFormat.format(Date(estStartTimeMs))
                val durationStr = formatDuration(seconds)

                mediaItems.add(
                    MediaItem(
                        id = id,
                        title = title,
                        durationSeconds = seconds.toDouble(),
                        type = type
                    )
                )

                queueItems.add(
                    QueueScheduleItem(
                        title = title,
                        durationSeconds = seconds,
                        startTimeFormatted = startTimeStr,
                        durationFormatted = durationStr,
                        mediaId = id
                    )
                )

                accumulatedSeconds += seconds
            }

            _scheduleItems.value = mediaItems
            _queueScheduleItems.value = queueItems
            Log.d(TAG, "Parsed ${queueItems.size} items from Cytubot schedule")
            return queueItems.size
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Cytubot schedule JSON", e)
            return 0
        }
    }

    /**
     * Smart Reddit EPG Fallback: Fetches official schedule lineup from r/420grindhouse.
     */
    suspend fun fetchScheduleFromReddit(subreddit: String = "420grindhouse") = withContext(ioDispatcher) {
        val endpoints = listOf(
            "https://api.pullpush.io/reddit/search/submission/?subreddit=$subreddit&size=15",
            "https://www.reddit.com/r/$subreddit/.rss",
            "https://old.reddit.com/r/$subreddit/new.json?limit=15"
        )

        for (ep in endpoints) {
            try {
                val userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"
                val req = Request.Builder()
                    .url(ep)
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json,application/atom+xml,text/xml")
                    .build()

                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    if (body.isNotBlank()) {
                        val parsed = if (ep.contains("pullpush") || ep.endsWith(".json")) {
                            parseRedditJson(body)
                        } else {
                            parseRedditRss(body)
                        }
                        if (parsed) {
                            Log.d(TAG, "Successfully populated EPG from Reddit ($ep)")
                            return@withContext
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed Reddit EPG fetch from $ep: ${e.message}")
            }
        }
    }

    private fun parseRedditJson(jsonStr: String): Boolean {
        try {
            val root = JSONObject(jsonStr)
            val postsArray = root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("children")
                ?: return false

            val mediaItems = mutableListOf<MediaItem>()
            val queueItems = mutableListOf<QueueScheduleItem>()
            var foundScheduleTitle: String? = null
            var foundScheduleText: String? = null

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            var accumulatedMs = System.currentTimeMillis()

            for (i in 0 until postsArray.length()) {
                val item = postsArray.optJSONObject(i) ?: continue
                val post = item.optJSONObject("data") ?: item

                val rawTitle = post.optString("title", "").trim()
                val selfText = post.optString("selftext", "").trim()

                if (rawTitle.isBlank()) continue

                // Check for dedicated full weekend / daily schedule post
                val isSchedulePost = rawTitle.contains("schedule", ignoreCase = true) ||
                        rawTitle.contains("programm", ignoreCase = true) ||
                        rawTitle.contains("lineup", ignoreCase = true) ||
                        rawTitle.contains("weekend", ignoreCase = true) ||
                        rawTitle.contains("marathon", ignoreCase = true)

                if (isSchedulePost && selfText.isNotBlank() && foundScheduleText == null) {
                    foundScheduleTitle = extractCleanMovieTitle(rawTitle)
                    foundScheduleText = cleanMarkdownText(selfText)
                }

                // Filter out non-film discussion questions (e.g. "CyTube down?", "Anyone know...")
                val isChatQuestion = rawTitle.endsWith("?") ||
                        rawTitle.contains("down?", ignoreCase = true) ||
                        rawTitle.contains("anyone know", ignoreCase = true) ||
                        rawTitle.contains("asking for a friend", ignoreCase = true)

                if (isChatQuestion && !isSchedulePost) continue

                // Clean post title for movie queue item
                val cleanTitle = extractCleanMovieTitle(rawTitle)
                val durationSec = 5400 // default ~90 min per feature film
                val startTimeStr = timeFormat.format(Date(accumulatedMs))

                mediaItems.add(
                    MediaItem(
                        id = "reddit_$i",
                        title = cleanTitle,
                        durationSeconds = durationSec.toDouble()
                    )
                )

                queueItems.add(
                    QueueScheduleItem(
                        title = cleanTitle,
                        durationSeconds = durationSec,
                        startTimeFormatted = startTimeStr,
                        durationFormatted = "90m",
                        mediaId = "reddit_$i"
                    )
                )

                accumulatedMs += (durationSec * 1000L)
            }

            if (queueItems.isNotEmpty() || foundScheduleText != null) {
                _scheduleItems.value = mediaItems
                _queueScheduleItems.value = queueItems
                _redditScheduleTitle.value = foundScheduleTitle
                _redditScheduleText.value = foundScheduleText
                _isRedditFallback.value = true
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Reddit JSON", e)
        }
        return false
    }

    private fun parseRedditRss(rssStr: String): Boolean {
        try {
            val entryRegex = Regex("<entry>([\\s\\S]*?)</entry>")
            val titleRegex = Regex("<title>([\\s\\S]*?)</title>")
            val contentRegex = Regex("<content[^>]*>([\\s\\S]*?)</content>")

            val entries = entryRegex.findAll(rssStr).toList()
            if (entries.isEmpty()) return false

            val mediaItems = mutableListOf<MediaItem>()
            val queueItems = mutableListOf<QueueScheduleItem>()
            var foundScheduleTitle: String? = null
            var foundScheduleText: String? = null

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            var accumulatedMs = System.currentTimeMillis()

            for ((idx, match) in entries.withIndex()) {
                val entryHtml = match.groupValues[1]
                val rawTitle = titleRegex.find(entryHtml)?.groupValues?.get(1) ?: ""
                val cleanTitle = extractCleanMovieTitle(rawTitle)
                val rawContent = contentRegex.find(entryHtml)?.groupValues?.get(1) ?: ""

                if (cleanTitle.isBlank()) continue

                val isSchedulePost = cleanTitle.contains("schedule", ignoreCase = true) ||
                        cleanTitle.contains("programm", ignoreCase = true) ||
                        cleanTitle.contains("lineup", ignoreCase = true) ||
                        cleanTitle.contains("weekend", ignoreCase = true)

                if (isSchedulePost && rawContent.isNotBlank() && foundScheduleText == null) {
                    foundScheduleTitle = cleanTitle
                    foundScheduleText = cleanMarkdownText(rawContent)
                }

                val isChatQuestion = cleanTitle.endsWith("?") ||
                        cleanTitle.contains("down?", ignoreCase = true) ||
                        cleanTitle.contains("anyone know", ignoreCase = true)

                if (isChatQuestion && !isSchedulePost) continue

                val durationSec = 5400
                val startTimeStr = timeFormat.format(Date(accumulatedMs))

                mediaItems.add(
                    MediaItem(
                        id = "reddit_rss_$idx",
                        title = cleanTitle,
                        durationSeconds = durationSec.toDouble()
                    )
                )

                queueItems.add(
                    QueueScheduleItem(
                        title = cleanTitle,
                        durationSeconds = durationSec,
                        startTimeFormatted = startTimeStr,
                        durationFormatted = "90m",
                        mediaId = "reddit_rss_$idx"
                    )
                )

                accumulatedMs += (durationSec * 1000L)
            }

            if (queueItems.isNotEmpty() || foundScheduleText != null) {
                _scheduleItems.value = mediaItems
                _queueScheduleItems.value = queueItems
                _redditScheduleTitle.value = foundScheduleTitle
                _redditScheduleText.value = foundScheduleText
                _isRedditFallback.value = true
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Reddit RSS", e)
        }
        return false
    }

    /**
     * Reddit liefert im RSS-content maskiertes HTML ("&lt;div class=&quot;md&quot;&gt;").
     * Die Reihenfolge ist entscheidend: erst Entities aufloesen, dann Tags entfernen. Vorher lief
     * es andersherum, und der Versuch, maskierte Tags direkt zu treffen, benutzte "&lt;[^&gt;]*&gt;"
     * — das ist eine Zeichenklasse aus den Zeichen & g t ; und keine Negation von "&gt;". Uebrig
     * blieben demaskierte Tags, die die UI als Text im Monospace-Kasten ausgab.
     *
     * Zwei Durchlaeufe, weil Reddit teilweise doppelt maskiert ("&amp;#32;").
     */
    internal fun cleanHtmlEntities(text: String): String {
        var s = text
        repeat(2) {
            s = s
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&#32;", " ")
                .replace("&#x20;", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
        }
        return s
            .replace(Regex("<!--[\\s\\S]*?-->"), "")
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("(?i)submitted by[\\s\\S]*$"), "")
            .replace(Regex("(?i)SC_OFF|SC_ON"), "")
            .trim()
    }

    internal fun cleanMarkdownText(text: String): String {
        val cleaned = cleanHtmlEntities(text)
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")
            .replace(Regex("(?m)^#+\\s*"), "")
            .replace(Regex("(?i)\\[link\\]|\\[comments\\]"), "")
            .replace(Regex("\n{3,}"), "\n\n")

        return cleaned.lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                        !line.startsWith("submitted by", ignoreCase = true) &&
                        !line.startsWith("[link]", ignoreCase = true) &&
                        !line.contains("reddit.com", ignoreCase = true) &&
                        !line.equals("SC_OFF", ignoreCase = true) &&
                        !line.equals("SC_ON", ignoreCase = true)
            }
            .joinToString("\n")
    }

    private fun extractCleanMovieTitle(rawTitle: String): String {
        val cleaned = cleanHtmlEntities(rawTitle)
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("(?m)^#+\\s*"), "")
            .trim()

        // If title contains " - Plot description...", keep "Movie Title (Year)"
        val dashIndex = cleaned.indexOf(" - ")
        return if (dashIndex > 3 && (cleaned.contains("(") || dashIndex < 40)) {
            cleaned.substring(0, dashIndex).trim()
        } else {
            cleaned
        }
    }

    private fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "0:00"
        val mins = seconds / 60
        val secs = seconds % 60
        return if (mins >= 60) {
            val hours = mins / 60
            val remMins = mins % 60
            String.format(Locale.US, "%d:%02d:%02d", hours, remMins, secs)
        } else {
            String.format(Locale.US, "%d:%02d", mins, secs)
        }
    }
}
