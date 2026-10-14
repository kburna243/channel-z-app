import re

with open("android/app/src/main/java/com/example/ui/components/FirstRunLoginDialog.kt", "r") as f:
    content = f.read()

content = content.replace("var showManualCodeInput by remember { mutableStateOf(false) }", "")
content = content.replace("val usernameFocusRequester = remember { FocusRequester() }", "")

with open("android/app/src/main/java/com/example/ui/components/FirstRunLoginDialog.kt", "w") as f:
    f.write(content)
