package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.data.model.ConnectionStatus
import com.example.data.model.MediaItem
import com.example.ui.metadata.MetadataOverlay
import com.example.ui.theme.GrindhouseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Der Infobereich trug ein fest verdrahtetes "LIVE" mit gruenem Punkt — ohne jede Verbindung
 * zum tatsaechlichen Zustand. Faellt der Socket aus, meldete er weiter Betrieb. Auf dem
 * Fernseher ist das die einzige Stelle, an der man nachsieht, warum nichts laeuft.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-television-xhdpi", sdk = [34])
class MetadataStatusTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun show(status: ConnectionStatus) {
        composeTestRule.setContent {
            GrindhouseTheme {
                MetadataOverlay(
                    nowPlaying = MediaItem(id = "x", title = "Werewolf", durationSeconds = 1800.0),
                    upNext = emptyList(),
                    isVisible = true,
                    connectionStatus = status
                )
            }
        }
    }

    @Test
    fun `getrennte verbindung meldet offline statt live`() {
        show(ConnectionStatus.OFFLINE)
        composeTestRule.onNodeWithText("OFFLINE").assertIsDisplayed()
    }

    @Test
    fun `waehrend des wiederverbindens steht nicht live da`() {
        show(ConnectionStatus.RECONNECTING)
        composeTestRule.onNodeWithText("LIVE").assertDoesNotExist()
    }

    @Test
    fun `bei stehender verbindung steht live da`() {
        show(ConnectionStatus.LIVE)
        composeTestRule.onNodeWithText("LIVE").assertIsDisplayed()
    }
}
