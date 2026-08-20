package com.example.ui.chat

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "SpellCheck"

/** Ein Fund der Pruefung: wo im Text, was ist gemeint, welche Ersetzungen kommen infrage. */
data class SpellingIssue(
    val offset: Int,
    val length: Int,
    val message: String,
    val replacements: List<String>
)

private val client = OkHttpClient.Builder()
    .connectTimeout(6, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .build()

/**
 * Prueft eine Chat-Nachricht bei LanguageTool.
 *
 * Der oeffentliche Dienst arbeitet ohne Schluessel; die Sprache wird automatisch erkannt, damit
 * deutsche und englische Nachrichten im selben Raum funktionieren. Bewusst nur auf Knopfdruck
 * und nicht bei jedem Tastenanschlag — der Dienst begrenzt die Zugriffe pro Minute, und im Chat
 * will ohnehin niemand beim Tippen korrigiert werden.
 *
 * ponytail: keine Wiederholversuche, kein Zwischenspeicher — schlaegt es fehl, bleibt die
 * Nachricht eben ungeprueft. Sie geht dadurch nicht verloren.
 */
suspend fun checkSpelling(text: String): List<SpellingIssue> = withContext(Dispatchers.IO) {
    val trimmed = text.trim()
    if (trimmed.length < 3) return@withContext emptyList()

    try {
        val body = FormBody.Builder()
            .add("text", trimmed)
            .add("language", "auto")
            .add("preferredVariants", "en-US,de-DE")
            .build()
        val request = Request.Builder()
            .url("https://api.languagetool.org/v2/check")
            .header("User-Agent", "Mikes420Grindhouse/1.4")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "LanguageTool antwortete mit HTTP ${response.code}")
                return@withContext emptyList()
            }
            val matches = JSONObject(response.body?.string().orEmpty()).optJSONArray("matches")
                ?: return@withContext emptyList()

            buildList {
                for (i in 0 until matches.length()) {
                    val match = matches.optJSONObject(i) ?: continue
                    val replacements = match.optJSONArray("replacements")
                    val candidates = buildList {
                        for (j in 0 until (replacements?.length() ?: 0)) {
                            replacements?.optJSONObject(j)?.optString("value")
                                ?.takeIf { it.isNotEmpty() }?.let { add(it) }
                        }
                    }
                    add(
                        SpellingIssue(
                            offset = match.optInt("offset", 0),
                            length = match.optInt("length", 0),
                            message = match.optString("shortMessage").ifEmpty {
                                match.optString("message")
                            },
                            replacements = candidates.take(3)
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Rechtschreibpruefung nicht erreichbar: ${e.message}")
        emptyList()
    }
}
