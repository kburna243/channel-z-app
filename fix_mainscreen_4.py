with open("android/app/src/main/java/com/example/ui/player/GrindhouseMainScreen.kt", "r") as f:
    content = f.read()

content = content.replace("onStartMagicLogin = { u, p -> viewModel.linkWebQueueDevice(u, p) },", "")

with open("android/app/src/main/java/com/example/ui/player/GrindhouseMainScreen.kt", "w") as f:
    f.write(content)
