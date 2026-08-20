package com.example

import com.example.data.movie.MovieInfoRepository
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Serientitel aus der echten Playlist des Kanals. Sie kommen teils als Dateiname, teils als
 * YouTube-Titel, und die Folgenkennzeichnung ist jedes Mal anders geschrieben. Fällt der
 * Serienname nicht sauber heraus, findet die Datenbank nichts — und die Folge steht ohne
 * Angaben da, während Filme welche haben.
 */
class SeriesTitleParsingTest {

    private val repo = MovieInfoRepository()

    private fun check(raw: String, title: String, season: Int?, episode: Int?) {
        val r = repo.parseTitle(raw)
        assertEquals("Serienname aus \"$raw\"", title, r.title)
        assertEquals("Staffel aus \"$raw\"", season, r.season)
        assertEquals("Folge aus \"$raw\"", episode, r.episode)
    }

    @Test
    fun filename_with_standard_notation() =
        check("Archer S01E10 Dial M for Mother.mp4", "Archer", 1, 10)

    @Test
    fun compact_notation_with_x() =
        check("Werewolf 1x22 Skinwalker", "Werewolf", 1, 22)

    @Test
    fun episode_before_season() =
        check("Swamp Thing    Episode10 Season 1   New Acquaintance", "Swamp Thing", 1, 10)

    @Test
    fun spelled_out_with_trailing_noise() =
        check(
            "Unsolved Mysteries with Robert Stack - Season 1 Episode 20 - Full Episode",
            "Unsolved Mysteries with Robert Stack", 1, 20
        )

    @Test
    fun spelled_out_with_comma() =
        check(
            "Unsolved Mysteries with Robert Stack - Season 1, Episode 22 - Updated Full Episode",
            "Unsolved Mysteries with Robert Stack", 1, 22
        )

    @Test
    fun pipe_separated_with_year_and_remaster_note() {
        val r = repo.parseTitle("The Tomorrow People (1992) | The Rameses Connection Ep. 5 | 4K A.I. Remaster")
        assertEquals("The Tomorrow People", r.title)
        assertEquals(1992, r.year)
        assertEquals(5, r.episode)
    }

    /** Filme dürfen von der Serienerkennung nicht angefasst werden. */
    @Test
    fun films_are_left_alone() {
        val r = repo.parseTitle("Strangeland.[1998].mp4")
        assertEquals("Strangeland", r.title)
        assertEquals(1998, r.year)
        assertEquals(null, r.episode)
    }
}
