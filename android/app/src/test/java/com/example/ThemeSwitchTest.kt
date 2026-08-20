package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.SettingsRepository
import com.example.ui.theme.AccentIceBlue
import com.example.ui.theme.MidnightCanvas
import com.example.ui.theme.Palettes
import com.example.ui.theme.applyPalette
import com.example.ui.theme.paletteOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Das Farbthema lebt in globalem Zustand und wird aus den Einstellungen wiederhergestellt.
 * Beide Haelften koennen unabhaengig brechen: die Farben werden nicht gesetzt, oder die Wahl
 * ueberlebt den Neustart nicht — in beiden Faellen steht der Nutzer wieder beim Standardthema.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeSwitchTest {

    private fun repo() = SettingsRepository(ApplicationProvider.getApplicationContext())

    @Test
    fun applying_a_palette_changes_the_colours() {
        applyPalette("cinematic")
        val cinematicBackground = MidnightCanvas
        val cinematicAccent = AccentIceBlue

        applyPalette("editorial")
        assertNotEquals("Hintergrund haengt am Thema", cinematicBackground, MidnightCanvas)
        assertNotEquals("Akzent haengt am Thema", cinematicAccent, AccentIceBlue)
        assertEquals(paletteOf("editorial").background, MidnightCanvas)
        assertEquals(paletteOf("editorial").accent, AccentIceBlue)
    }

    /** Jede Palette muss vollstaendig durchschlagen, nicht nur die, die ich gerade ansehe. */
    @Test
    fun every_palette_applies_completely() {
        Palettes.forEach { palette ->
            applyPalette(palette.id)
            assertEquals(palette.id, palette.background, MidnightCanvas)
            assertEquals(palette.id, palette.accent, AccentIceBlue)
        }
    }

    /** Eine unbekannte Kennung — etwa aus einer aelteren Version — darf nicht ins Leere laufen. */
    @Test
    fun unknown_id_falls_back_to_the_default() {
        assertEquals(Palettes.first(), paletteOf("gibt-es-nicht"))
    }

    @Test
    fun theme_choice_survives_a_restart() {
        repo().updateAppTheme("cyberpunk")
        assertEquals("cyberpunk", repo().settings.value.appTheme)
    }

    /**
     * Diese beiden Schalter wurden zwar im Dialog umgelegt, aber nie gespeichert: sie fehlten
     * in loadSettings() und in updateSettings().
     */
    @Test
    fun movie_and_imdb_switches_survive_a_restart() {
        repo().updateSettings { it.copy(movieInfoEnabled = false, imdbEnabled = false) }
        val reloaded = repo().settings.value
        assertTrue("beide Schalter bleiben aus", !reloaded.movieInfoEnabled && !reloaded.imdbEnabled)
    }
}
