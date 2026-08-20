package com.example

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import com.example.data.model.AppSettings
import com.example.ui.settings.SettingsOverlay
import com.example.ui.theme.GrindhouseTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Der Einstellungsdialog trug seine Texte teils als Kotlin-Literale, gesteuert ueber
 * `languageCode == "de"`. Weil der Auslieferungs-Default "system" ist, stand dort dauerhaft
 * Englisch — unabhaengig von der gewaehlten Sprache. Dieser Test rendert den Dialog unter der
 * jeweiligen Locale und prueft, dass die Oberflaeche wirklich in dieser Sprache erscheint.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-xhdpi", sdk = [34])
class SettingsLocalizationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderSettings(languageCode: String) {
        // Entscheidend ist die Resource-Locale, nicht das AppSettings-Feld: genau daran hing der
        // Bug, denn die Literale im Code haben die Locale schlicht ignoriert.
        RuntimeEnvironment.setQualifiers("+$languageCode")
        composeTestRule.setContent {
            GrindhouseTheme {
                SettingsOverlay(
                    settings = AppSettings(languageCode = languageCode),
                    isOpen = true,
                    onClose = {},
                    onToggleChat = {},
                    onUpdateOpacity = {},
                    onUpdateFontSize = {},
                    onUpdateLanguage = {},
                    onPlayDemoStream = {},
                    onRetryConnection = {}
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    /** Alle sichtbaren Texte des Dialogs einsammeln. */
    private fun visibleText(): String {
        val texts = mutableListOf<String>()
        composeTestRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
            .fetchSemanticsNodes()
            .forEach { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.forEach { texts.add(it.text) }
            }
        return texts.joinToString(" | ")
    }

    @Test
    fun english_locale_shows_no_german_text() {
        renderSettings("en")
        val text = visibleText()
        assertTrue("Dialog rendert keine Texte", text.isNotBlank())

        val germanMarkers = listOf(
            "Zeile", "Untertitel", "Dauerhaft", "Deckkraft", "Schrift", "Einstellungen",
            "Farbthema", "Schliessen", "Schließen", "AKTIV", "AUS", "Prüfen", "Installieren"
        )
        val hits = germanMarkers.filter { text.contains(it) }
        assertTrue("Deutsche Texte in englischer Oberflaeche: $hits\nGerendert: $text", hits.isEmpty())
    }

    @Test
    fun german_locale_shows_no_english_leftovers() {
        renderSettings("de")
        val text = visibleText()
        assertTrue("Dialog rendert keine Texte", text.isNotBlank())

        val englishMarkers = listOf(
            "Chat Lines", "Subtitles", "Always visible", "Display Duration",
            "Color Theme", "line(s)", "Close", "Settings"
        )
        val hits = englishMarkers.filter { text.contains(it) }
        assertTrue("Englische Reste in deutscher Oberflaeche: $hits\nGerendert: $text", hits.isEmpty())
    }
}
