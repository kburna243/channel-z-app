package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.AppSettings
import com.example.data.model.MediaItem
import com.example.data.model.MovieInfo
import com.example.data.model.SettingsPage
import com.example.ui.metadata.MetadataOverlay
import com.example.ui.settings.SettingsOverlay
import com.example.ui.theme.GrindhouseTheme
import com.example.ui.theme.Palettes
import com.example.ui.theme.applyPalette
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Kein Test, sondern ein Bildlieferant: rendert das Menue in jedem Farbthema, damit sich die
 * Paletten ansehen lassen, ohne die App auf einem Geraet zu starten. Bei Farben ist das Bild
 * der einzige Beleg, der zaehlt.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp-television-xhdpi", sdk = [34])
class ThemePreviewShots {

    @get:Rule val composeTestRule = createComposeRule()

    private fun shoot(themeId: String, page: SettingsPage, name: String) {
        applyPalette(themeId)
        composeTestRule.setContent {
            GrindhouseTheme {
                SettingsOverlay(
                    settings = AppSettings(appTheme = themeId),
                    isOpen = true,
                    settingsPage = page,
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
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$name.png")
    }

    @Test fun cinematic() = shoot("cinematic", SettingsPage.MAIN, "theme-1-cinematic")
    @Test fun cyberpunk() = shoot("cyberpunk", SettingsPage.MAIN, "theme-2-cyberpunk")
    @Test fun editorial() = shoot("editorial", SettingsPage.MAIN, "theme-3-editorial")
    @Test fun grindhouse() = shoot("grindhouse", SettingsPage.MAIN, "theme-4-grindhouse")
    /** Der Infobereich mit allem, was gleichzeitig auftreten kann: Plakat, Fakten, Balken, Naechstes. */
    private fun shootInfo(themeId: String, name: String) {
        applyPalette(themeId)
        composeTestRule.setContent {
            GrindhouseTheme {
                MetadataOverlay(
                    nowPlaying = MediaItem(
                        id = "abc",
                        title = "Unsolved Mysteries with Robert Stack - Season 1 Episode 23 - Full Episode",
                        durationSeconds = 2883.0,
                        currentTimeSeconds = 1140.0,
                        type = "yt"
                    ),
                    upNext = listOf(
                        MediaItem(id = "def", title = "The Tomorrow People (1992) | The Living Stones Ep. 3", durationSeconds = 1455.0)
                    ),
                    isVisible = true,
                    movieInfo = MovieInfo(
                        query = "Unsolved Mysteries S01E23",
                        title = "Unsolved Mysteries",
                        year = 1987,
                        runtimeMinutes = 60,
                        genres = listOf("crime television series"),
                        rating = 8.3,
                        season = 1,
                        episode = 23
                    )
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$name.png")
    }

    @Test fun infoCinematic() = shootInfo("cinematic", "info-1-cinematic")
    @Test fun infoCyberpunk() = shootInfo("cyberpunk", "info-2-cyberpunk")
    @Test fun infoEditorial() = shootInfo("editorial", "info-3-editorial")
    @Test fun infoGrindhouse() = shootInfo("grindhouse", "info-4-grindhouse")

    @Test fun chooser() = shoot("cinematic", SettingsPage.THEME, "theme-0-auswahl")
}
