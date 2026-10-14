import re
with open("android/app/src/main/java/com/example/data/webqueue/WebQueueApiClient.kt", "r") as f:
    content = f.read()

# remove duplicate linkDevice
search_duplicate = """    suspend fun linkDevice(code: String): Result<String> = withContext(ioDispatcher) {
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

# split by duplicate and take first part + duplicate string + last part
parts = content.split(search_duplicate)
if len(parts) == 3:
    content = parts[0] + search_duplicate + parts[2]

with open("android/app/src/main/java/com/example/data/webqueue/WebQueueApiClient.kt", "w") as f:
    f.write(content)
