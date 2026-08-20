package com.example

import com.example.data.movie.MovieInfoRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Titel im Kanal sind rohe Dateinamen ("Strangeland.[1998].mp4"). Aus denen muss ein
 * suchbarer Werktitel werden, sonst findet Wikidata nichts und die ganze Kette liefert nichts.
 * Diese Fälle stammen aus der echten Playlist des Kanals.
 */
class MovieTitleParsingTest {

    private val repo = MovieInfoRepository()

    @Test
    fun strips_extension_and_extracts_year() {
        val r = repo.parseTitle("Strangeland.[1998].mp4")
        assertEquals("Strangeland", r.title)
        assertEquals(1998, r.year)
    }

    @Test
    fun series_episode_reduces_to_series_title() {
        val r = repo.parseTitle("Twin Peaks S01E05 The One-Armed Man.mp4")
        assertEquals("Twin Peaks", r.title)
        assertTrue("Folge nicht als solche erkannt", r.isEpisode)
    }

    @Test
    fun removes_release_tags() {
        val r = repo.parseTitle("Street.Trash.1987.1080p.BluRay.x264-GROUP.mkv")
        assertEquals("Street Trash", r.title)
        assertEquals(1987, r.year)
    }

    @Test
    fun keeps_plain_titles_untouched() {
        val r = repo.parseTitle("Cannibal Holocaust")
        assertEquals("Cannibal Holocaust", r.title)
        assertNull(r.year)
    }

    @Test
    fun handles_year_in_parentheses() {
        val r = repo.parseTitle("The Stepdaughter (2000)")
        assertEquals("The Stepdaughter", r.title)
        assertEquals(2000, r.year)
    }

    /** Bumper und Trailer haben keine Jahreszahl und sollen trotzdem sauber durchlaufen. */
    @Test
    fun survives_titles_without_metadata() {
        val r = repo.parseTitle("Twin Peaks Tuesday 420Grindhouse Bumper")
        assertEquals("Twin Peaks Tuesday 420Grindhouse Bumper", r.title)
        assertNull(r.year)
    }
}
