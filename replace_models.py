import sys
with open("android/app/src/main/java/com/example/data/model/WebQueueModels.kt", "r") as f:
    content = f.read()

search = """sealed interface WebQueueOtpState {
    data object Idle : WebQueueOtpState
    data object RequestingOtp : WebQueueOtpState
    data class WaitingForCode(val username: String) : WebQueueOtpState
    data class Verifying(val username: String, val code: String) : WebQueueOtpState
    data class Success(val username: String) : WebQueueOtpState
    data class Failed(val error: String) : WebQueueOtpState
}"""

replace = """sealed interface WebQueueLinkState {
    data object Idle : WebQueueLinkState
    data object RequestingLink : WebQueueLinkState
    data class Verifying(val code: String) : WebQueueLinkState
    data class Success(val username: String) : WebQueueLinkState
    data class Failed(val error: String) : WebQueueLinkState
}"""

content = content.replace("WebQueueOtpState", "WebQueueLinkState")
content = content.replace("RequestingOtp", "RequestingLink")
content = content.replace("WaitingForCode(val username: String)", "WaitingForCode(val username: String)") # actually we removed waiting for code
content = content.replace(search.replace("WebQueueOtpState", "WebQueueLinkState").replace("RequestingOtp", "RequestingLink"), replace)

with open("android/app/src/main/java/com/example/data/model/WebQueueModels.kt", "w") as f:
    f.write(content)
