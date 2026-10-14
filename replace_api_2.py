import re

with open("android/app/src/main/java/com/example/data/webqueue/WebQueueApiClient.kt", "r") as f:
    content = f.read()

# Replace the stray verifyOtp which my regex missed with linkDevice
search_verify_otp = """    /**
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
    }"""

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

content = content.replace(search_verify_otp, replace_link_device)

with open("android/app/src/main/java/com/example/data/webqueue/WebQueueApiClient.kt", "w") as f:
    f.write(content)
