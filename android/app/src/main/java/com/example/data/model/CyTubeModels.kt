package com.example.data.model

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString() + "_" + (1000..9999).random(),
    val username: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSystem: Boolean = false,
    val userRank: Int = 0
)

data class QueueScheduleItem(
    val title: String,
    val durationSeconds: Int = 0,
    val startTimeFormatted: String = "",
    val durationFormatted: String = "",
    val mediaId: String = ""
)

data class MediaItem(
    val id: String,
    val title: String,
    val durationSeconds: Double = 0.0,
    val type: String = "raw",
    val url: String? = null,
    val currentTimeSeconds: Double = 0.0,
    val paused: Boolean = false,
    val directUrl: String = ""
) {
    val isWebStream: Boolean
        get() {
            val t = type.lowercase()
            val i = id.lowercase()
            val u = (url ?: directUrl).lowercase()
            return t == "yt" || t == "tw" || t == "vi" ||
                    (t !in listOf("cm", "fi", "hl", "gd", "raw", "direct", "stream", "mp4", "m3u8") &&
                            (i.contains("youtube.com") || i.contains("youtu.be") || u.contains("youtube.com") || u.contains("youtu.be")))
        }
}

/**
 * Live-Synchronisations-Event von CyTube (mediaUpdate).
 * Entspricht dem Sync-Protokoll aus calzoneman/sync.
 */
data class MediaSyncUpdate(
    val currentTimeSeconds: Double,
    val paused: Boolean
)

/**
 * Was ueber den laufenden Film bekannt ist. Wird nebenlaeufig nachgeladen und ist deshalb
 * durchgehend optional — die Anzeige zeigt einfach, was da ist.
 */
data class MovieInfo(
    val query: String,
    val title: String? = null,
    val year: Int? = null,
    val runtimeMinutes: Int? = null,
    val directors: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val rating: Double? = null,
    val voteCount: Int? = null,
    val plot: String? = null,
    val posterUrl: String? = null,
    val imdbId: String? = null,
    val trivia: List<String> = emptyList(),
    /** Bei Serienfolgen: Staffel und Folge, sofern im Titel angegeben. */
    val season: Int? = null,
    val episode: Int? = null,
    /** Vorschaubilder von YouTube sind 16:9, Filmplakate hochkant — die Anzeige braucht das. */
    val posterIsWide: Boolean = false
) {
    val hasFacts: Boolean
        get() = title != null && (year != null || runtimeMinutes != null ||
                directors.isNotEmpty() || genres.isNotEmpty() || rating != null)
}

/** Ebenen des Einstellungsmenues. Die Chat-Darstellung liegt eine Ebene tiefer. */
enum class SettingsPage {
    MAIN,
    CHAT_APPEARANCE,
    CHAT_ACCOUNT,
    THEME,
    BUG_REPORT
}

/**
 * Ansichten des Chats. Die Reihenfolge ist auch die Reihenfolge beim Durchschalten.
 *
 * SUBTITLE ist die gewohnte Untertitel-Zeile ueber dem Bild, SIDEBAR stellt den Chat als Spalte
 * daneben, HIDDEN blendet ihn ganz aus, und CHAT_ONLY macht das Geraet zum reinen Chat-Fenster
 * mit angehaltenem Video — gedacht fuer das Handy neben dem laufenden Fernseher.
 */
enum class ChatLayout {
    SUBTITLE,
    SIDEBAR,
    HIDDEN,
    CHAT_ONLY
}

/** Ein Nutzer im Raum, so wie CyTube ihn in der userlist meldet. */
data class ChannelUser(
    val name: String,
    val rank: Int = 0,
    val isAfk: Boolean = false,
    val isMuted: Boolean = false,
    val profileImage: String? = null
)

/** Ein Kanal-Emote. Der Kanal liefert rund 1700 davon, geladen wird nur, was vorkommt. */
data class ChannelEmote(
    val name: String,
    val imageUrl: String
)

/** Anmeldestand am CyTube-Konto. Ohne Anmeldung nimmt der Kanal keine Nachrichten an. */
sealed interface LoginState {
    data object LoggedOut : LoginState
    data object InProgress : LoginState
    data class LoggedIn(val username: String) : LoginState
    data class Failed(val error: String) : LoginState
}

enum class ConnectionStatus {
    LIVE,
    RECONNECTING,
    OFFLINE,
    IDLE
}

data class AppSettings(
    val chatEnabled: Boolean = true,
    val chatMaxLines: Int = 3,
    val chatBackgroundOpacity: Float = 0.15f,
    val chatFontSizeSp: Int = 16,
    val languageCode: String = "system",
    val roomName: String = "Channel-Z",
    val customStreamUrl: String = "",
    val safeZoneEnabled: Boolean = true,
    val isMuted: Boolean = false,
    val subtitlesEnabled: Boolean = true,
    val chatAutoHideSeconds: Int = 0,       // 0 = dauerhaft sichtbar, >0 = Sekunden bis Auto-Hide
    val chatTheme: String = "channelz",  // "channelz" | "classic"
    // Farbthema der gesamten Oberflaeche; gueltige Werte stehen in ui/theme/Color.kt
    val appTheme: String = "cinematic",
    val movieInfoEnabled: Boolean = true,
    // IMDb-Daten (Poster, Bewertung, Trivia) kommen ueber einen undokumentierten Endpunkt.
    // Abschaltbar, damit die App auch ohne diese Quelle brauchbar bleibt.
    val imdbEnabled: Boolean = true
)
