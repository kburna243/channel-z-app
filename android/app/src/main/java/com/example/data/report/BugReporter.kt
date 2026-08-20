package com.example.data.report

import android.os.Build
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Schickt eine Problemmeldung an den Dienst auf der Synology.
 *
 * Zwei Dinge unterscheiden das von der bisherigen Umsetzung auf iOS:
 *
 * 1. Die Antwort wird ausgewertet. Dort meldete die App in jedem Fall "Gesendet — Danke!",
 *    auch wenn der Server gar nicht antwortete. Wer ein Problem meldete, ging davon aus,
 *    dass es ankommt — tatsaechlich kam nie eines an.
 * 2. Geraet, Systemversion und laufender Titel gehen automatisch mit. Ohne diese Angaben
 *    ist "Video ruckelt" nicht nachvollziehbar, und auf einer Fernbedienung tippt sie
 *    niemand freiwillig ab.
 */
class BugReporter {

    /** Ergebnis einer Meldung — bewusst mit Grund, damit die Oberflaeche nichts erfinden muss. */
    sealed interface Result {
        data object Sent : Result
        data class Failed(val reason: String) : Result
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun send(
        description: String,
        severity: String = "medium",
        nowPlaying: String? = null,
        contact: String = ""
    ): Result = withContext(Dispatchers.IO) {
        val details = buildString {
            append(description.trim())
            append("\n\n--\n")
            append("Geraet: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})\n")
            append("Variante: ${BuildConfig.FLAVOR}\n")
            nowPlaying?.takeIf { it.isNotBlank() }?.let { append("Lief gerade: $it\n") }
        }

        var lastReason = "keine Antwort"
        for (endpoint in endpoints) {
            val url = endpoint +
                "?app=" + APP_ID.enc() +
                "&version=" + BuildConfig.VERSION_NAME.enc() +
                "&severity=" + severity.enc() +
                "&description=" + details.enc() +
                "&contact=" + contact.trim().enc()
            try {
                http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        lastReason = "HTTP ${resp.code}"
                        return@use
                    }
                    // Der Dienst antwortet {"ok":true,...}. "ok" heisst: die Meldung liegt in
                    // der Sammeldatei. Mehr braucht es nicht — es geht bewusst keine
                    // Benachrichtigung raus, die Berichte werden gesammelt und spaeter gelesen.
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    if (json?.optBoolean("ok") == true) return@withContext Result.Sent
                    lastReason = json?.optString("error").orEmpty().ifBlank { "unerwartete Antwort" }
                }
            } catch (e: Exception) {
                lastReason = e.javaClass.simpleName
                Log.w("BugReporter", "Meldung an $endpoint fehlgeschlagen", e)
            }
        }
        Result.Failed(lastReason)
    }

    private fun String.enc(): String = URLEncoder.encode(this, "UTF-8").replace("+", "%20")

    companion object {
        private const val APP_ID = "mca-android"

        /**
         * Ziele in Vorzugsreihenfolge. Die Liste existiert, weil derselbe Aufbau beim
         * Update-Feed gebraucht wurde: faellt eine Quelle aus, wird die naechste probiert.
         */
        private val endpoints = listOf(
            "https://servermitte.tailecbf0f.ts.net/mca/bug-report"
        )
    }
}
