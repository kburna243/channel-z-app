with open("android/app/src/main/java/com/example/player/PlayerViewModel.kt", "r") as f:
    content = f.read()

# remove magic otp code listener entirely
magic_otp_listener = """        // Magic WebQueue OTP Listener: Automatisches Abfangen des Kryten-Bestätigungscodes
        viewModelScope.launch {
            socketClient.magicOtpCodeEvent.collect { code ->
/* magic otp code removed */
            }
        }"""
content = content.replace(magic_otp_listener, "")

with open("android/app/src/main/java/com/example/player/PlayerViewModel.kt", "w") as f:
    f.write(content)
