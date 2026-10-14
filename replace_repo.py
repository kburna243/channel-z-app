import sys
with open(sys.argv[1], 'r') as f:
    content = f.read()
import re

search = """    fun saveWebQueueCookies(cookiesJson: String) {
        prefs.edit().putString("webqueue_cookies", cookiesJson).apply()
    }

    fun clearWebQueueCookies() {
        prefs.edit().remove("webqueue_cookies").apply()
    }"""

replace = """    fun saveWebQueueCookies(cookiesJson: String) {
        prefs.edit().putString("webqueue_cookies", cookiesJson).apply()
    }

    fun clearWebQueueCookies() {
        prefs.edit().remove("webqueue_cookies").apply()
    }

    fun webQueueApiKey(): String? {
        return prefs.getString("webqueue_api_key", null)
    }

    fun saveWebQueueApiKey(apiKey: String) {
        prefs.edit().putString("webqueue_api_key", apiKey).apply()
    }

    fun clearWebQueueApiKey() {
        prefs.edit().remove("webqueue_api_key").apply()
    }"""

if search in content:
    with open(sys.argv[1], 'w') as f:
        f.write(content.replace(search, replace))
    print("Replaced!")
else:
    print("Search string not found")
