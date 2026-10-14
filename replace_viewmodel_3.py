import re
with open("android/app/src/main/java/com/example/player/PlayerViewModel.kt", "r") as f:
    content = f.read()

# removing magic otp code
magic_otp_code = """                val currentOtpState = _linkState.value
                val username = when (currentOtpState) {
                    is WebQueueLinkState.WaitingForCode -> currentOtpState.username
                    is WebQueueLinkState.RequestingLink -> pendingCredentials?.first ?: _savedChatUsername.value
                    else -> pendingCredentials?.first ?: _savedChatUsername.value
                }.ifBlank { _savedChatUsername.value }

                if (username.isNotBlank() && code.isNotBlank()) {
                    Log.i(TAG, "✨ Magic OTP triggered for '$username' with code '$code'")
                    _linkState.value = WebQueueLinkState.Verifying(username, code)
                    val verifyResult = webQueueClient.verifyOtp(username, code)
                    if (verifyResult.isSuccess) {
                        Log.i(TAG, "🎉 WebQueue Magic Login successful!")
                        _linkState.value = WebQueueLinkState.Success(username)
                        settingsRepo.setFirstRunCompleted(true)
                        startWebQueuePolling()
                    } else {
                        val err = verifyResult.exceptionOrNull()?.message ?: "OTP verification failed"
                        _linkState.value = WebQueueLinkState.Failed(err)
                    }
                }"""

# Need to find where this is located in PlayerViewModel to remove it correctly
content = content.replace(magic_otp_code, "/* magic otp code removed */")

with open("android/app/src/main/java/com/example/player/PlayerViewModel.kt", "w") as f:
    f.write(content)
