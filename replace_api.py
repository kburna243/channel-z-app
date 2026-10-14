import sys
import re

with open("android/app/src/main/java/com/example/data/webqueue/WebQueueApiClient.kt", 'r') as f:
    content = f.read()

# Replace Imports
content = content.replace("import okhttp3.HttpUrl.Companion.toHttpUrl", "import okhttp3.HttpUrl.Companion.toHttpUrl\nimport okhttp3.Interceptor")

# Replace httpClient to add Interceptor
search_client = """    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)"""

replace_client = """    private val authInterceptor = Interceptor { chain ->
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
        .connectTimeout(10, TimeUnit.SECONDS)"""

content = content.replace(search_client, replace_client)

# Replace Session check
search_has_session = """    fun hasValidSession(): Boolean {
        val httpUrl = BASE_URL.toHttpUrl()
        val cookies = cookieStore[httpUrl.host] ?: return false
        val now = System.currentTimeMillis()
        return cookies.any { it.expiresAt > now || it.persistent }
    }"""

replace_has_session = """    fun hasValidSession(): Boolean {
        return !settingsRepository.webQueueApiKey().isNullOrBlank()
    }"""

content = content.replace(search_has_session, replace_has_session)

# Replace requestOtp and verifyOtp with linkDevice
import re
# Regex to match requestOtp and verifyOtp
search_otp_methods = r"    /\*\*\n     \* Step 1: Request 6-digit OTP code.*?\n        }\n    }"
content = re.sub(search_otp_methods, "", content, flags=re.DOTALL)

replace_link_device = """    suspend fun linkDevice(code: String): Result<String> = withContext(ioDispatcher) {
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
    }"""
# insert before fetchQueueState
content = content.replace("    /**\n     * Fetches the current live queue state from WebQueue.", replace_link_device + "\n\n    /**\n     * Fetches the current live queue state from WebQueue.")

# Update URLs
content = content.replace("$BASE_URL/queue/state", "$BASE_URL/api/public/v1/queue")
content = content.replace("$BASE_URL/queue/next-schedule", "$BASE_URL/api/public/v1/events")

# Update next-schedule parsing
search_schedule_parse = """                val root = JSONObject(respBody)
                val title = root.optString("title").ifBlank { root.optString("name") }
                val desc = root.optString("description").ifBlank { root.optString("time_str") }
                val combined = when {
                    title.isNotBlank() && desc.isNotBlank() -> "$title ($desc)"
                    title.isNotBlank() -> title
                    desc.isNotBlank() -> desc
                    else -> null
                }
                Result.success(combined)"""

replace_schedule_parse = """                val root = JSONObject(respBody)
                val eventsArray = root.optJSONArray("events")
                if (eventsArray != null && eventsArray.length() > 0) {
                    val firstEvent = eventsArray.getJSONObject(0)
                    val label = firstEvent.optString("label")
                    Result.success(label)
                } else {
                    Result.success(null)
                }"""
content = content.replace(search_schedule_parse, replace_schedule_parse)

# Update logout
content = content.replace("settingsRepository.clearWebQueueCookies()", "settingsRepository.clearWebQueueCookies()\n        settingsRepository.clearWebQueueApiKey()")


# Replace parseQueueStateJson
search_parse_queue = """    private fun parseQueueStateJson(jsonStr: String): List<QueueScheduleItem> {
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
    }"""

replace_parse_queue = """    private fun parseQueueStateJson(jsonStr: String): List<QueueScheduleItem> {
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
    }"""
content = content.replace(search_parse_queue, replace_parse_queue)

with open("android/app/src/main/java/com/example/data/webqueue/WebQueueApiClient.kt", "w") as f:
    f.write(content)
print("Done python script")
