package com.example.data.webqueue

import android.util.Log
import com.example.data.model.MediaItem
import com.example.data.model.QueueScheduleItem
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "WebQueueApiClient"
private const val BASE_URL = "https://queue.dropsugar.co"

class WebQueueApiClient(
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            val existing = cookieStore.getOrPut(host) { mutableListOf() }
            for (cookie in cookies) {
                existing.removeAll { it.name == cookie.name }
                existing.add(cookie)
            }
            // Persist cookies to settings
            val serialized = serializeCookies(existing)
            settingsRepository.saveWebQueueCookies(serialized)
            Log.d(TAG, "Saved ${cookies.size} cookies for $host (Total stored: ${existing.size})")
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val host = url.host
            val inMemory = cookieStore[host]
            if (!inMemory.isNullOrEmpty()) return inMemory

            // Try restoring from preferences
            val saved = settingsRepository.webQueueCookies()
            if (!saved.isNullOrBlank()) {
                val restored = deserializeCookies(url, saved)
                cookieStore[host] = restored.toMutableList()
                return restored
            }
            return emptyList()
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    init {
        // Load initial cookies from settings if present
        val saved = settingsRepository.webQueueCookies()
        if (!saved.isNullOrBlank()) {
            val httpUrl = BASE_URL.toHttpUrl()
            cookieStore[httpUrl.host] = deserializeCookies(httpUrl, saved).toMutableList()
        }
    }

    fun hasValidSession(): Boolean {
        val httpUrl = BASE_URL.toHttpUrl()
        val cookies = cookieStore[httpUrl.host] ?: return false
        val now = System.currentTimeMillis()
        return cookies.any { it.expiresAt > now || it.persistent }
    }

    /**
     * Step 1: Request 6-digit OTP code to be sent via CyTube PM by Kryten.
     */
    suspend fun requestOtp(username: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val json = JSONObject().apply {
                put("username", username.trim())
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/auth/otp/request")
                .post(body)
                .header("User-Agent", "channelz-app")
                .build()

            val response = httpClient.newCall(request).execute()
            val respBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                Log.d(TAG, "OTP successfully requested for '$username'")
                Result.success(Unit)
            } else {
                val detail = parseErrorDetail(respBody, "Failed to request OTP (HTTP ${response.code})")
                Log.w(TAG, "OTP request failed: $detail")
                Result.failure(Exception(detail))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during OTP request", e)
            Result.failure(e)
        }
    }

    /**
     * Step 2: Verify the 6-digit OTP code and store authenticated session cookies.
     */
    suspend fun verifyOtp(username: String, code: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val json = JSONObject().apply {
                put("username", username.trim())
                put("code", code.trim())
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/auth/otp/verify")
                .post(body)
                .header("User-Agent", "channelz-app")
                .build()

            val response = httpClient.newCall(request).execute()
            val respBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                Log.d(TAG, "OTP successfully verified for '$username'!")
                settingsRepository.setFirstRunCompleted(true)
                Result.success(Unit)
            } else {
                val detail = parseErrorDetail(respBody, "Invalid or expired code (HTTP ${response.code})")
                Log.w(TAG, "OTP verification failed: $detail")
                Result.failure(Exception(detail))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during OTP verification", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches the current live queue state from WebQueue.
     */
    suspend fun fetchQueueState(): Result<List<QueueScheduleItem>> = withContext(ioDispatcher) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/queue/state")
                .get()
                .header("User-Agent", "channelz-app")
                .build()

            val response = httpClient.newCall(request).execute()
            val respBody = response.body?.string().orEmpty()

            if (response.isSuccessful && respBody.isNotBlank()) {
                val items = parseQueueStateJson(respBody)
                Result.success(items)
            } else {
                Result.failure(Exception("Failed to fetch queue state: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching queue state", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches announcement info about the next scheduled block/playlist.
     */
    suspend fun fetchNextSchedule(): Result<String?> = withContext(ioDispatcher) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/queue/next-schedule")
                .get()
                .header("User-Agent", "channelz-app")
                .build()

            val response = httpClient.newCall(request).execute()
            val respBody = response.body?.string().orEmpty()

            if (response.isSuccessful && respBody.isNotBlank()) {
                val root = JSONObject(respBody)
                val title = root.optString("title").ifBlank { root.optString("name") }
                val desc = root.optString("description").ifBlank { root.optString("time_str") }
                val combined = when {
                    title.isNotBlank() && desc.isNotBlank() -> "$title ($desc)"
                    title.isNotBlank() -> title
                    desc.isNotBlank() -> desc
                    else -> null
                }
                Result.success(combined)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching next schedule: ${e.message}")
            Result.success(null)
        }
    }

    /**
     * Searches the Channel-Z catalog for movie/show details including IMDB tt number.
     */
    suspend fun searchCatalog(query: String): Result<com.example.data.model.CatalogItem?> = withContext(ioDispatcher) {
        try {
            val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            val request = Request.Builder()
                .url("$BASE_URL/catalog/search?q=$encoded")
                .get()
                .header("User-Agent", "channelz-app")
                .build()

            val response = httpClient.newCall(request).execute()
            val respBody = response.body?.string().orEmpty()

            if (response.isSuccessful && respBody.isNotBlank()) {
                val root = JSONObject(respBody)
                val items = root.optJSONArray("items") ?: root.optJSONArray("results")
                if (items != null && items.length() > 0) {
                    val first = items.getJSONObject(0)
                    val item = com.example.data.model.CatalogItem(
                        friendlyToken = first.optString("friendly_token", first.optString("token")),
                        title = first.optString("title"),
                        description = first.optString("description").takeIf { it.isNotBlank() },
                        durationSec = first.optInt("duration_sec", first.optInt("duration", 0)),
                        imdbTt = first.optString("imdb_tt").takeIf { it.isNotBlank() },
                        contentType = first.optString("content_type").takeIf { it.isNotBlank() },
                        lookupYear = first.optInt("lookup_year").takeIf { it > 0 },
                        category = first.optString("category").takeIf { it.isNotBlank() },
                        posterUrl = first.optString("thumbnail_url", first.optString("poster_url")).takeIf { it.isNotBlank() }
                    )
                    return@withContext Result.success(item)
                }
            }
            Result.success(null)
        } catch (e: Exception) {
            Log.w(TAG, "Error searching catalog for '$query': ${e.message}")
            Result.failure(e)
        }
    }

    fun logout() {
        cookieStore.clear()
        settingsRepository.clearWebQueueCookies()
        Log.d(TAG, "WebQueue session cleared")
    }

    private fun parseQueueStateJson(jsonStr: String): List<QueueScheduleItem> {
        val result = mutableListOf<QueueScheduleItem>()
        try {
            val root = JSONObject(jsonStr)
            val itemsArray = root.optJSONArray("items")
                ?: root.optJSONArray("queue")
                ?: root.optJSONArray("playlist")
                ?: return emptyList()

            var accumulatedSeconds = root.optDouble("remaining_seconds", 0.0)
            if (accumulatedSeconds <= 0.0) {
                accumulatedSeconds = root.optDouble("remainingSeconds", root.optDouble("time_remaining", 0.0))
            }
            val nowMs = System.currentTimeMillis()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.optJSONObject(i) ?: continue
                val mediaObj = itemObj.optJSONObject("media") ?: itemObj

                val title = mediaObj.optString("title", itemObj.optString("title", "Upcoming Item"))
                val durationSec = when {
                    mediaObj.has("duration_sec") -> mediaObj.optInt("duration_sec")
                    mediaObj.has("duration_seconds") -> mediaObj.optInt("duration_seconds")
                    mediaObj.has("duration") -> mediaObj.optInt("duration")
                    mediaObj.has("seconds") -> mediaObj.optInt("seconds")
                    itemObj.has("duration_sec") -> itemObj.optInt("duration_sec")
                    itemObj.has("duration_seconds") -> itemObj.optInt("duration_seconds")
                    itemObj.has("duration") -> itemObj.optInt("duration")
                    itemObj.has("seconds") -> itemObj.optInt("seconds")
                    else -> 0
                }
                val mediaId = mediaObj.optString("id", itemObj.optString("uid", itemObj.optString("id", "")))

                val estStartTimeMs = nowMs + (accumulatedSeconds * 1000).toLong()
                val startTimeStr = timeFormat.format(Date(estStartTimeMs))
                val durationStr = formatDuration(durationSec)

                result.add(
                    QueueScheduleItem(
                        title = title,
                        durationSeconds = durationSec,
                        startTimeFormatted = startTimeStr,
                        durationFormatted = durationStr,
                        mediaId = mediaId
                    )
                )

                accumulatedSeconds += if (durationSec > 0) durationSec else 300
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing queue state JSON", e)
        }
        return result
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

    private fun parseErrorDetail(body: String, fallback: String): String {
        return try {
            val json = JSONObject(body)
            json.optString("detail").ifBlank { fallback }
        } catch (e: Exception) {
            fallback
        }
    }

    private fun serializeCookies(cookies: List<Cookie>): String {
        val array = JSONArray()
        for (c in cookies) {
            val obj = JSONObject().apply {
                put("name", c.name)
                put("value", c.value)
                put("domain", c.domain)
                put("path", c.path)
                put("expiresAt", c.expiresAt)
                put("secure", c.secure)
                put("httpOnly", c.httpOnly)
                put("persistent", c.persistent)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeCookies(httpUrl: HttpUrl, json: String): List<Cookie> {
        val result = mutableListOf<Cookie>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val builder = Cookie.Builder()
                    .name(obj.getString("name"))
                    .value(obj.getString("value"))
                    .domain(obj.getString("domain"))
                    .path(obj.getString("path"))
                    .expiresAt(obj.optLong("expiresAt", System.currentTimeMillis() + 86400000L))

                if (obj.optBoolean("secure", false)) builder.secure()
                if (obj.optBoolean("httpOnly", false)) builder.httpOnly()

                result.add(builder.build())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing cookies", e)
        }
        return result
    }
}
