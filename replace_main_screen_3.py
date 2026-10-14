with open("android/app/src/main/java/com/example/ui/player/GrindhouseMainScreen.kt", "r") as f:
    content = f.read()

# Replace callbacks for first run login dialog
# Search via regex because indentation could vary
import re
search_callbacks = r"FirstRunLoginDialog\([\s\S]*?otpState = otpState,[\s\S]*?initialUsername = savedUsername[\s\S]*?\)"

replace_callbacks = """FirstRunLoginDialog(
                    isOpen = isFirstRunDialogOpen,
                    onDismiss = { viewModel.dismissFirstRunDialog() },
                    onSkip = { viewModel.skipFirstRun() },
                    onVerifyDeviceCode = { c -> viewModel.linkWebQueueDevice(c) },
                    linkState = linkState
                )"""

content = re.sub(search_callbacks, replace_callbacks, content)

with open("android/app/src/main/java/com/example/ui/player/GrindhouseMainScreen.kt", "w") as f:
    f.write(content)
