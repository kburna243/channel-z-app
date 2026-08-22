package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppSettings
import com.example.ui.theme.DefaultPaletteId
import com.example.ui.theme.paletteOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("channelz_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            chatEnabled = prefs.getBoolean("chat_enabled", true),
            chatMaxLines = prefs.getInt("chat_max_lines", 3).coerceIn(1, 3),
            chatBackgroundOpacity = prefs.getFloat("chat_opacity", 0.15f),
            chatFontSizeSp = prefs.getInt("chat_font_size", 16),
            languageCode = prefs.getString("language_code", "system") ?: "system",
            roomName = prefs.getString("room_name", "Channel-Z") ?: "Channel-Z",
            customStreamUrl = prefs.getString("custom_stream_url", "") ?: "",
            safeZoneEnabled = prefs.getBoolean("safe_zone", true),
            subtitlesEnabled = prefs.getBoolean("subtitles_enabled", true),
            chatAutoHideSeconds = prefs.getInt("chat_auto_hide_seconds", 0),
            chatTheme = prefs.getString("chat_theme", "channelz") ?: "channelz",
            appTheme = prefs.getString("app_theme", DefaultPaletteId) ?: DefaultPaletteId,
            // Fehlten bisher beim Laden und Speichern: beide Schalter sprangen nach jedem
            // Neustart auf "an" zurueck, egal was eingestellt war.
            movieInfoEnabled = prefs.getBoolean("movie_info_enabled", true),
            imdbEnabled = prefs.getBoolean("imdb_enabled", true)
        )
    }

    fun updateSettings(update: (AppSettings) -> AppSettings) {
        val newSettings = update(_settings.value)
        prefs.edit()
            .putBoolean("chat_enabled", newSettings.chatEnabled)
            .putInt("chat_max_lines", newSettings.chatMaxLines)
            .putFloat("chat_opacity", newSettings.chatBackgroundOpacity)
            .putInt("chat_font_size", newSettings.chatFontSizeSp)
            .putString("language_code", newSettings.languageCode)
            .putString("room_name", newSettings.roomName)
            .putString("custom_stream_url", newSettings.customStreamUrl)
            .putBoolean("safe_zone", newSettings.safeZoneEnabled)
            .putBoolean("subtitles_enabled", newSettings.subtitlesEnabled)
            .putInt("chat_auto_hide_seconds", newSettings.chatAutoHideSeconds)
            .putString("chat_theme", newSettings.chatTheme)
            .putString("app_theme", newSettings.appTheme)
            .putBoolean("movie_info_enabled", newSettings.movieInfoEnabled)
            .putBoolean("imdb_enabled", newSettings.imdbEnabled)
            .apply()
        _settings.value = newSettings
    }

    fun toggleChat() {
        updateSettings { it.copy(chatEnabled = !it.chatEnabled) }
    }

    fun toggleSubtitles() {
        updateSettings { it.copy(subtitlesEnabled = !it.subtitlesEnabled) }
    }

    fun updateChatAutoHide(seconds: Int) {
        updateSettings { it.copy(chatAutoHideSeconds = if (seconds > 0) seconds else 0) }
    }

    /** Setzt das Farbthema. Unbekannte Kennungen fallen auf die Vorgabe zurueck. */
    fun updateAppTheme(id: String) {
        updateSettings { it.copy(appTheme = paletteOf(id).id) }
    }

    fun updateChatTheme(theme: String) {
        val t = if (theme == "classic") "classic" else "channelz"
        updateSettings { it.copy(chatTheme = t) }
    }

    // -------------------------------------------------- Chat-Zugangsdaten (nur Full-Ausgabe)
    // Bewusst NICHT Teil von AppSettings: Das Passwort soll nie in den UI-Zustand
    // gelangen, der durch die halbe App gereicht wird. Gast-Anmeldung heisst:
    // Benutzername gespeichert, Passwort leer.

    /** Gespeichertes Konto (Name, Passwort) oder null, wenn keines hinterlegt ist. */
    fun chatCredentials(): Pair<String, String>? {
        val name = prefs.getString("chat_username", "") ?: ""
        if (name.isEmpty()) return null
        return name to (prefs.getString("chat_password", "") ?: "")
    }

    fun saveChatCredentials(username: String, password: String) {
        prefs.edit()
            .putString("chat_username", username)
            .putString("chat_password", password)
            .apply()
    }

    fun clearChatCredentials() {
        prefs.edit()
            .remove("chat_username")
            .remove("chat_password")
            .apply()
    }
}
