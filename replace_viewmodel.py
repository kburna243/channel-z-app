import re

with open("android/app/src/main/java/com/example/player/PlayerViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.data.model.WebQueueOtpState", "import com.example.data.model.WebQueueLinkState")
content = content.replace("WebQueueOtpState", "WebQueueLinkState")
content = content.replace("_otpState", "_linkState")
content = content.replace("otpState", "linkState")
content = content.replace("RequestingOtp", "RequestingLink")

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

# Replace startMagicLogin and verifyManualOtp with linkWebQueueDevice
auth_functions_search = """    /**
     * Startet den Magic Login:
     * 1. Verbindet / meldet den CyTube-Chat an.
     * 2. Fordert den OTP-Code bei WebQueue (queue.dropsugar.co) an.
     * 3. Kryten schickt den Code per PM -> Socket extrahiert den Code automatisch -> Verify!
     */
    fun startMagicLogin(username: String, password: String = "") {
        val cleanName = username.trim()
        if (cleanName.isBlank()) {
            _linkState.value = WebQueueLinkState.Failed("Please enter your CyTube username")
            return
        }
        _linkState.value = WebQueueLinkState.RequestingLink
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
                _linkState.value = WebQueueLinkState.Failed(err)
                return@launch
            }

            _linkState.value = WebQueueLinkState.WaitingForCode(cleanName)
        }
    }

    /** Manuelle Eingabe des 6-stelligen Codes, falls gewünscht. */
    fun verifyManualOtp(username: String, code: String) {
        val cleanName = username.trim().ifBlank { _savedChatUsername.value }
        val cleanCode = code.trim()
        if (cleanName.isBlank() || cleanCode.isBlank()) {
            _linkState.value = WebQueueLinkState.Failed("Username and 6-digit code are required")
            return
        }
        _linkState.value = WebQueueLinkState.Verifying(cleanName, cleanCode)
        viewModelScope.launch {
            val result = webQueueClient.verifyOtp(cleanName, cleanCode)
            if (result.isSuccess) {
                _linkState.value = WebQueueLinkState.Success(cleanName)
                settingsRepo.setFirstRunCompleted(true)
                startWebQueuePolling()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Invalid or expired code"
                _linkState.value = WebQueueLinkState.Failed(err)
            }
        }
    }"""

replace_auth_functions = """    fun linkWebQueueDevice(code: String) {
        val cleanCode = code.trim()
        if (cleanCode.isBlank()) {
            _linkState.value = WebQueueLinkState.Failed("Code is required")
            return
        }
        _linkState.value = WebQueueLinkState.Verifying(cleanCode)
        viewModelScope.launch {
            val result = webQueueClient.linkDevice(cleanCode)
            if (result.isSuccess) {
                _linkState.value = WebQueueLinkState.Success(result.getOrDefault(""))
                settingsRepo.setFirstRunCompleted(true)
                startWebQueuePolling()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Invalid or expired code"
                _linkState.value = WebQueueLinkState.Failed(err)
            }
        }
    }"""
content = content.replace(auth_functions_search, replace_auth_functions)

with open("android/app/src/main/java/com/example/player/PlayerViewModel.kt", "w") as f:
    f.write(content)
