package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.data.model.MediaItem
import com.example.data.model.QueueScheduleItem
import com.example.ui.metadata.MetadataOverlay
import com.example.ui.theme.GrindhouseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Der Infobereich sammelte bis zu vier kommende Titel ein und zeigte davon einen. Drei
 * passen ohne Gedraenge ins Panel und beantworten die Frage, die man am Fernseher wirklich
 * hat: lohnt sich Dranbleiben.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-television-xhdpi", sdk = [36])
class UpNextRowsTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val nowPlaying = MediaItem(id = "x", title = "Werewolf", durationSeconds = 1800.0)

    @Test
    fun `der zeitplan zeigt drei kommende titel`() {
        composeTestRule.setContent {
            GrindhouseTheme {
                MetadataOverlay(
                    nowPlaying = nowPlaying,
                    upNext = emptyList(),
                    queueItems = listOf(
                        QueueScheduleItem("Blood Feast", 4800, "20:15", "1:20:00", "a"),
                        QueueScheduleItem("Basket Case", 5100, "21:35", "1:25:00", "b"),
                        QueueScheduleItem("Street Trash", 5400, "23:00", "1:30:00", "c"),
                        QueueScheduleItem("Nicht mehr sichtbar", 3600, "00:30", "1:00:00", "d")
                    ),
                    isVisible = true
                )
            }
        }
        composeTestRule.onNodeWithText("Blood Feast").assertIsDisplayed()
        composeTestRule.onNodeWithText("Basket Case").assertIsDisplayed()
        composeTestRule.onNodeWithText("Street Trash").assertIsDisplayed()
        // Vier waeren zu viel fuer das Panel — dafuer gibt es die eigene Warteschlangen-Ansicht.
        composeTestRule.onNodeWithText("Nicht mehr sichtbar").assertDoesNotExist()
    }

    @Test
    fun `ohne zeitplan traegt die socket-warteschlange die liste`() {
        composeTestRule.setContent {
            GrindhouseTheme {
                MetadataOverlay(
                    nowPlaying = nowPlaying,
                    upNext = listOf(
                        MediaItem(id = "1", title = "Maniac Cop", durationSeconds = 5100.0),
                        MediaItem(id = "2", title = "The Stuff", durationSeconds = 5280.0)
                    ),
                    isVisible = true
                )
            }
        }
        composeTestRule.onNodeWithText("Maniac Cop").assertIsDisplayed()
        composeTestRule.onNodeWithText("The Stuff").assertIsDisplayed()
    }
}
