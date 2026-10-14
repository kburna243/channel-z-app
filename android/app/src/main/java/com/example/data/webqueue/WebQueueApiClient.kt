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
import okhttp3.Interceptor
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

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        if (chain.request().url.encodedPath.startsWith("/api/public/v1")) {
            settingsRepository.webQueueApiKey()?.let { apiKey ->
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
        }
        chain.proceed(requestBuilder.build())
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(authInterceptor)
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
        return !settingsRepository.webQueueApiKey().isNullOrBlank()
    }



    suspend fun linkDevice(code: String): Result<String> = withContext(ioDispatcher) {
        try {
            val json = JSONObject().apply {
                put("code", code.trim().uppercase())
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/public/v1/link")
                .post(body)
                .header("User-Agent", "channelz-app")
                .build()

            val response = httpClient.newCall(request).execute()
            val respBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val root = JSONObject(respBody)
                val apiKey = root.optString("api_key")
                val username = root.optString("username")
                if (apiKey.isNotBlank()) {
                    settingsRepository.saveWebQueueApiKey(apiKey)
                    Log.d(TAG, "Device linked successfully for '$username'!")
                    settingsRepository.setFirstRunCompleted(true)
                    Result.success(username)
                } else {
                    Result.failure(Exception("Missing api_key in response"))
                }
            } else {
                val detail = parseErrorDetail(respBody, "Invalid or expired code (HTTP ${response.code})")
                Log.w(TAG, "Link verification failed: $detail")
                Result.failure(Exception(detail))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during link verification", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches the current live queue state from WebQueue.
     */
    suspend fun fetchQueueState(): Result<List<QueueScheduleItem>> = withContext(ioDispatcher) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/public/v1/queue")
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
                .url("$BASE_URL/api/public/v1/events")
                .get()
                .header("User-Agent", "channelz-app")
                .build()

            val response = httpClient.newCall(request).execute()
            val respBody = response.body?.string().orEmpty()

            if (response.isSuccessful && respBody.isNotBlank()) {
                val root = JSONObject(respBody)
                val eventsArray = root.optJSONArray("events")
                if (eventsArray != null && eventsArray.length() > 0) {
                    val firstEvent = eventsArray.getJSONObject(0)
                    val label = firstEvent.optString("label")
                    Result.success(label)
                } else {
                    Result.success(null)
                }
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
        settingsRepository.clearWebQueueApiKey()
        Log.d(TAG, "WebQueue session cleared")
    }

    private fun parseQueueStateJson(jsonStr: String): List<QueueScheduleItem> {
        val result = mutableListOf<QueueScheduleItem>()
        try {
            val root = JSONObject(jsonStr)
            val itemsArray = root.optJSONArray("items") ?: return emptyList()

            val nowMs = System.currentTimeMillis()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.optJSONObject(i) ?: continue

                val title = itemObj.optString("title", "Upcoming Item")
                val durationSec = itemObj.optInt("duration_sec", 0)
                val friendlyToken = itemObj.optString("friendly_token", "")
                val estStartInSec = itemObj.optInt("estimated_start_in_sec", -1)
                val isNowPlaying = itemObj.optBoolean("is_now_playing", false)

                val estStartTimeMs = if (isNowPlaying) {
                    nowMs
                } else if (estStartInSec >= 0) {
                    nowMs + (estStartInSec * 1000).toLong()
                } else {
                    nowMs
                }

                val startTimeStr = timeFormat.format(Date(estStartTimeMs))
                val durationStr = formatDuration(durationSec)

                result.add(
                    QueueScheduleItem(
                        title = title,
                        durationSeconds = durationSec,
                        startTimeFormatted = startTimeStr,
                        durationFormatted = durationStr,
                        mediaId = friendlyToken
                    )
                )
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
