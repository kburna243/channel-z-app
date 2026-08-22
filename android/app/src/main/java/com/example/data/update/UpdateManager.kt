package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val notes: String,
    val minAndroid: Int
) {
    fun isNewerThan(currentCode: Int): Boolean = versionCode > currentCode
}

class UpdateManager(private val context: Context) {

    /**
     * Quellen fuer den Update-Hinweis, in Vorzugsreihenfolge.
     *
     * GitHub steht vorn, weil es von jedem Netz aus erreichbar ist. Die servermitte-Adresse
     * hinter Tailscale bleibt als Zweitquelle: Sie funktioniert im Tailnet, von aussen bricht
     * der Funnel den TLS-Handshake ab (EOFException), was auf dem Fire TV als "Server nicht
     * erreichbar" ankam. cytube.420grindhouse.org ist ersatzlos entfallen — die Domain existiert
     * im DNS nicht und hat nur einen Timeout gekostet.
     */
    private val versionEndpoints = listOf(
        "https://raw.githubusercontent.com/kburna243/channel-z-app/main/version.json"
    )

    /**
     * Grund des letzten fehlgeschlagenen Abrufs, kurz und fuer die Anzeige gedacht.
     * Ohne das meldete die App jeden Fehler als "Server nicht erreichbar" — auch dann, wenn
     * in Wahrheit der TLS-Handshake abbrach oder der Server mit einem HTTP-Fehler antwortete.
     */
    @Volatile
    var lastFailureReason: String? = null
        private set

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val currentVersionName: String by lazy {
        try {
            BuildConfig.VERSION_NAME.ifBlank {
                val pi = context.packageManager.getPackageInfo(context.packageName, 0)
                pi.versionName ?: "1.1.3"
            }
        } catch (e: Exception) { "1.1.3" }
    }

    val currentVersionCode: Int by lazy {
        try {
            if (BuildConfig.VERSION_CODE > 0) BuildConfig.VERSION_CODE
            else {
                val pi = context.packageManager.getPackageInfo(context.packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode.toInt()
                else @Suppress("DEPRECATION") pi.versionCode
            }
        } catch (e: Exception) { 14 }
    }

    /**
     * Liefert den Intent zur Freigabe "Unbekannte Apps installieren", falls sie fehlt — sonst null.
     *
     * Ohne diese Freigabe passiert beim Tippen auf "Installieren" scheinbar nichts: Der Download
     * laeuft, der ACTION_VIEW-Intent startet auch, und das System blockt still ab. Der Nutzer hat
     * dann 29 MB geladen und steht vor derselben Version. Deshalb wird vor dem Download gefragt.
     */
    fun installPermissionIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        if (context.packageManager.canRequestPackageInstalls()) return null
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    suspend fun fetchUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val failures = mutableListOf<String>()
        for (url in versionEndpoints) {
            val host = runCatching { java.net.URL(url).host }.getOrDefault(url)
            try {
                Log.d("UpdateManager", "Checking update from $url ...")
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "ChannelZ-Client/1.0")
                    .build()
                val resp = http.newCall(request).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: continue
                    Log.d("UpdateManager", "Got update response: $body")
                    val j = JSONObject(body)
                    // Jede Ausgabe laedt ihre eigene APK: die Full-Ausgabe die
                    // Handy-Fassung mit Chat, die Light-Ausgabe die TV-Fassung.
                    val apkUrl = if (BuildConfig.HAS_CHAT_INPUT) {
                        j.optString("url", j.optString("lightUrl", ""))
                    } else {
                        j.optString("lightUrl", j.optString("url", ""))
                    }
                    lastFailureReason = null
                    return@withContext UpdateInfo(
                        versionName = j.optString("version", "?"),
                        versionCode = j.optInt("versionCode", 0),
                        apkUrl = apkUrl,
                        notes = j.optString("notes", ""),
                        minAndroid = j.optInt("minAndroid", 0)
                    )
                } else {
                    Log.w("UpdateManager", "Endpoint $url returned code ${resp.code}")
                    failures += "$host: HTTP ${resp.code}"
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Failed checking updates from $url", e)
                failures += "$host: ${e.javaClass.simpleName}"
            }
        }
        lastFailureReason = failures.joinToString(" · ").ifBlank { null }
        null
    }

    suspend fun downloadAndInstall(info: UpdateInfo): Intent? = withContext(Dispatchers.IO) {
        try {
            Log.d("UpdateManager", "Downloading update APK from ${info.apkUrl} ...")
            val apkFile = File(context.cacheDir, "channelz-update.apk")
            if (apkFile.exists()) apkFile.delete()

            val request = Request.Builder()
                .url(info.apkUrl)
                .header("User-Agent", "ChannelZ-Client/1.0")
                .build()

            val resp = http.newCall(request).execute()
            if (!resp.isSuccessful) {
                Log.e("UpdateManager", "Download failed with HTTP ${resp.code}")
                return@withContext null
            }

            resp.body?.byteStream()?.use { input ->
                apkFile.outputStream().use { input.copyTo(it) }
            } ?: return@withContext null

            Log.d("UpdateManager", "Download complete: ${apkFile.length()} bytes. Launching FileProvider intent...")

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Download or fileprovider failed", e)
            null
        }
    }
}
