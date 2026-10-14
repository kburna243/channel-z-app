import re
with open("android/app/src/main/java/com/example/ui/player/GrindhouseMainScreen.kt", "r") as f:
    content = f.read()

search = r"com\.example\.ui\.components\.FirstRunLoginDialog\([\s\S]*?onSkip = \{ viewModel\.skipFirstRun\(\) \}\n\s*\)"
replace = """com.example.ui.components.FirstRunLoginDialog(
                    isOpen = isFirstRunDialogOpen,
                    linkState = linkState,
                    onVerifyDeviceCode = { c -> viewModel.linkWebQueueDevice(c) },
                    onDismiss = { viewModel.dismissFirstRunDialog() },
                    onSkip = { viewModel.skipFirstRun() }
                )"""

content = re.sub(search, replace, content)

with open("android/app/src/main/java/com/example/ui/player/GrindhouseMainScreen.kt", "w") as f:
    f.write(content)
