import re

with open("android/app/src/main/java/com/example/ui/components/FirstRunLoginDialog.kt", "r") as f:
    content = f.read()

# Replace the whole FirstRunLoginDialog composable
search_function = r"@Composable\nfun FirstRunLoginDialog\(.*?\n}\n"
# Actually regex matching multiline might be tricky if not careful, let's just do a string replacement of the whole file because it's a small component file.
