import re

with open("android/app/src/main/java/com/example/player/PlayerViewModel.kt", "r") as f:
    content = f.read()

# Remove PM listener processing OTP code
pm_listener_search = """                is SocketEvent.PrivateMessageReceived -> {
                    val pm = event.pm
                    if (pm.from.equals("Kryten", ignoreCase = true)) {
                        val currentOtpState = _linkState.value
                        val expectedUsername = when (currentOtpState) {
                            is WebQueueLinkState.WaitingForCode -> currentOtpState.username
                            is WebQueueLinkState.RequestingLink -> pendingCredentials?.first ?: _savedChatUsername.value
                            else -> null
                        }
                        if (expectedUsername != null) {
                            val codeMatch = Regex("\\b([0-9]{6})\\b").find(pm.text)
                            if (codeMatch != null) {
                                val code = codeMatch.groupValues[1]
                                Log.d("PlayerViewModel", "Magic OTP Code received from Kryten: $code")
                                _linkState.value = WebQueueLinkState.Verifying(expectedUsername, code)
                                viewModelScope.launch {
                                    val result = webQueueClient.verifyOtp(expectedUsername, code)
                                    if (result.isSuccess) {
                                        _linkState.value = WebQueueLinkState.Success(expectedUsername)
                                        settingsRepo.setFirstRunCompleted(true)
                                        startWebQueuePolling()
                                    } else {
                                        val err = result.exceptionOrNull()?.message ?: "Invalid or expired code"
                                        _linkState.value = WebQueueLinkState.Failed(err)
                                    }
                                }
                            }
                        }
                    }
                }"""
content = content.replace(pm_listener_search, "                is SocketEvent.PrivateMessageReceived -> { /* PMs ignored for now */ }")
with open("android/app/src/main/java/com/example/player/PlayerViewModel.kt", "w") as f:
    f.write(content)
