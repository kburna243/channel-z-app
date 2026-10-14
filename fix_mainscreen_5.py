import re
with open("android/app/src/main/java/com/example/ui/player/GrindhouseMainScreen.kt", "r") as f:
    content = f.read()

content = content.replace("onLoginChat = { name, pw -> viewModel.linkWebQueueDevice(name, pw) },", "onLoginChat = { name, pw -> viewModel.login(name, pw) },")
with open("android/app/src/main/java/com/example/ui/player/GrindhouseMainScreen.kt", "w") as f:
    f.write(content)
