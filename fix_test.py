import re

with open("android/app/src/test/java/com/example/WebQueueAuthTest.kt", "r") as f:
    content = f.read()

content = content.replace("WebQueueOtpState", "WebQueueLinkState")
content = content.replace("otpState", "linkState")
content = content.replace("startMagicLogin", "linkWebQueueDevice")
content = content.replace("verifyManualOtp", "linkWebQueueDevice")
content = content.replace("RequestingOtp", "RequestingLink")

with open("android/app/src/test/java/com/example/WebQueueAuthTest.kt", "w") as f:
    f.write(content)
