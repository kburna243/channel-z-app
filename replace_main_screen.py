import re
with open("android/app/src/main/java/com/example/ui/player/GrindhouseMainScreen.kt", "r") as f:
    content = f.read()

# Replace callbacks for first run login dialog
search_callbacks = """                FirstRunLoginDialog(
                    isOpen = isFirstRunDialogOpen,
                    onDismiss = { viewModel.dismissFirstRunDialog() },
                    onSkip = { viewModel.skipFirstRun() },
                    onStartMagicLogin = { u, p -> viewModel.startMagicLogin(u, p) },
                    onVerifyManualOtp = { u, c -> viewModel.verifyManualOtp(u, c) },
                    otpState = otpState,
                    initialUsername = savedUsername
                )"""

replace_callbacks = """                FirstRunLoginDialog(
                    isOpen = isFirstRunDialogOpen,
                    onDismiss = { viewModel.dismissFirstRunDialog() },
                    onSkip = { viewModel.skipFirstRun() },
                    onVerifyDeviceCode = { c -> viewModel.linkWebQueueDevice(c) },
                    linkState = linkState
                )"""

content = content.replace(search_callbacks, replace_callbacks)

# Also need to replace otpState -> linkState
content = content.replace("val otpState by viewModel.otpState.collectAsState()", "val linkState by viewModel.linkState.collectAsState()")

with open("android/app/src/main/java/com/example/ui/player/GrindhouseMainScreen.kt", "w") as f:
    f.write(content)
