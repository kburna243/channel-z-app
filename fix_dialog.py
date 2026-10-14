import re

with open("android/app/src/main/java/com/example/ui/components/FirstRunLoginDialog.kt", "r") as f:
    content = f.read()

# Fix signature
search_sig = """@Composable
fun FirstRunLoginDialog(
    isOpen: Boolean,
    linkState: WebQueueLinkState,
    ,
    onStartMagicLogin: (username: String, password: String) -> Unit,
    onVerifyManualOtp: (username: String, code: String) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {"""

replace_sig = """@Composable
fun FirstRunLoginDialog(
    isOpen: Boolean,
    linkState: WebQueueLinkState,
    onVerifyDeviceCode: (code: String) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {"""

content = content.replace(search_sig, replace_sig)

# Remove initialUsername reference
content = content.replace("var username by remember { mutableStateOf(initialUsername) }", "")
content = content.replace("var password by remember { mutableStateOf(\"\") }", "")

search_launch_effect = """    LaunchedEffect(isOpen) {
        if (isOpen && initialUsername.isBlank()) {
            delay(200)
            try { usernameFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }"""
content = content.replace(search_launch_effect, "")

content = content.replace("if (linkState !is WebQueueLinkState.RequestingOtp && linkState !is WebQueueLinkState.Verifying)", "if (linkState !is WebQueueLinkState.RequestingLink && linkState !is WebQueueLinkState.Verifying)")

with open("android/app/src/main/java/com/example/ui/components/FirstRunLoginDialog.kt", "w") as f:
    f.write(content)
