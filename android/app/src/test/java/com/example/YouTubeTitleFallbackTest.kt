package com.example

import com.example.data.movie.MovieInfoRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Titel aus dem Kanal, die weder Film noch Serie sind — Bumper, Werbespots und Sketche.
 * Für die greift die Filmdatenbank nicht, sie dürfen die Titelaufbereitung aber auch nicht
 * durcheinanderbringen: was hier herausfällt, geht anschließend an YouTube.
 */
class YouTubeTitleFallbackTest {

    private val repo = MovieInfoRepository()

    @Test
    fun bumper_titles_survive_unchanged() {
        val r = repo.parseTitle("Twin Peaks Tuesday 420Grindhouse Bumper")
        assertEquals("Twin Peaks Tuesday 420Grindhouse Bumper", r.title)
    }

    @Test
    fun commercial_with_year_keeps_its_name() {
        val r = repo.parseTitle("Pizza Hut Meat Lover's Pizza 1989 Commercial")
        assertEquals(1989, r.year)
        assertTrue("Titel unbrauchbar: ${r.title}", r.title.contains("Pizza Hut"))
    }

    @Test
    fun sketch_title_with_dash_is_not_truncated_at_the_dash() {
        // " - " trennt bei Szene-Releases den Gruppennamen ab. Hier gehoert es zum Titel,
        // deshalb darf nur ein alleinstehender Zusatz am Ende wegfallen.
        val r = repo.parseTitle("Census Taker - Saturday Night Live")
        assertTrue("Titel abgeschnitten: ${r.title}", r.title.contains("Census Taker"))
    }
}
