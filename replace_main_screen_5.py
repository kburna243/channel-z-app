with open("android/app/src/main/java/com/example/ui/player/GrindhouseMainScreen.kt", "r") as f:
    content = f.read()

search = """                com.example.ui.components.FirstRunLoginDialog(
                    isOpen = isFirstRunDialogOpen,
                    otpState = otpState,
                    initialUsername = savedChatUsername,
                    onStartMagicLogin = { u, p -> viewModel.startMagicLogin(u, p) },
                    onVerifyManualOtp = { u, c -> viewModel.verifyManualOtp(u, c) },
                    onDismiss = { viewModel.dismissFirstRunDialog() },
                    onSkip = { viewModel.skipFirstRun() }
                )"""

replace = """                com.example.ui.components.FirstRunLoginDialog(
                    isOpen = isFirstRunDialogOpen,
                    linkState = linkState,
                    onVerifyDeviceCode = { c -> viewModel.linkWebQueueDevice(c) },
                    onDismiss = { viewModel.dismissFirstRunDialog() },
                    onSkip = { viewModel.skipFirstRun() }
                )"""

# I need to restore the state val definition because I replaced it before but wait... it says otpState = otpState in the actual file!
# Let me replace "otpState = otpState," etc directly

content = content.replace("otpState = otpState", "linkState = linkState")
content = content.replace("initialUsername = savedChatUsername,", "")
content = content.replace("onStartMagicLogin = { u, p -> viewModel.startMagicLogin(u, p) },", "onVerifyDeviceCode = { c -> viewModel.linkWebQueueDevice(c) },")
content = content.replace("onVerifyManualOtp = { u, c -> viewModel.verifyManualOtp(u, c) },", "")
content = content.replace("val otpState by viewModel.otpState.collectAsStateWithLifecycle()", "val linkState by viewModel.linkState.collectAsStateWithLifecycle()") # just in case

with open("android/app/src/main/java/com/example/ui/player/GrindhouseMainScreen.kt", "w") as f:
    f.write(content)
