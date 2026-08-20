package com.example

import com.example.data.movie.MovieInfoRepository
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Die IMDb-Suche liefert zu einem Namen alles, was so heisst — Serie, Film, Kurzfilm, Podcast.
 * Diese Auswahl entscheidet, was davon gemeint ist. Sie greift nur, wenn Wikidata nichts
 * findet, und genau dann gibt es keinen zweiten Anlauf: trifft sie daneben, steht bei der
 * Folge etwas Falsches.
 *
 * Die Treffer unten sind die echten Antworten von IMDb zu den Titeln aus der Playlist.
 */
class ImdbHitSelectionTest {

    private val repo = MovieInfoRepository()

    private fun pick(rawTitle: String, hits: List<Triple<String, Int?, String>>): String? =
        repo.chooseImdbHit(repo.parseTitle(rawTitle), hits)

    /** Der Fall, der den Anlass gab: Wikidata kennt diese Serie nicht. */
    @Test
    fun werewolf_series_beats_the_films_of_the_same_name() {
        val chosen = pick(
            "Werewolf 1x22 Skinwalker",
            listOf(
                Triple("tt15318872", 2022, "TV Movie"),
                Triple("tt0082010", 1981, "Movie"),
                Triple("tt0092480", 1987, "TV Series"),
                Triple("tt0118137", 1995, "Video"),
                Triple("tt7203520", 2018, "Movie")
            )
        )
        assertEquals("tt0092480", chosen)
    }

    /** Drei gleichnamige Serien — das Jahr im Titel muss entscheiden. */
    @Test
    fun year_decides_between_three_series_of_the_same_name() {
        val chosen = pick(
            "The Tomorrow People (1992) | The Rameses Connection Ep. 5",
            listOf(
                Triple("tt2660734", 2013, "TV Series"),
                Triple("tt0069647", 1973, "TV Series"),
                Triple("tt0103568", 1992, "TV Series")
            )
        )
        assertEquals("tt0103568", chosen)
    }

    /**
     * Film von 1982 gegen zwei gleichnamige Serien. Die Entscheidung faellt hier ueber das
     * Jahr — auch ohne die Bevorzugung von Filmen bliebe dieser Fall richtig. Er steht hier
     * als Regressionsschutz fuer das Zusammenspiel, nicht als Beleg fuer die Werksart.
     */
    @Test
    fun film_wins_over_same_named_series_by_year() {
        val chosen = pick(
            "Swamp Thing.[1982].mp4",
            listOf(
                Triple("tt8362852", 2019, "TV Series"),
                Triple("tt0084745", 1982, "Movie"),
                Triple("tt0098919", 1990, "TV Series")
            )
        )
        assertEquals("tt0084745", chosen)
    }

    /**
     * Podcast und Kurzfilm stehen daneben, der Film gewinnt. Gegengeprueft: ohne den
     * Ausschluss bliebe dieser Fall ebenfalls richtig, weil der Film ueber Art und Jahr
     * vorne liegt. Dass der Ausschluss selbst greift, zeigt der letzte Fall.
     */
    @Test
    fun film_wins_when_podcast_and_short_stand_beside_it() {
        val chosen = pick(
            "Strangeland.[1998].mp4",
            listOf(
                Triple("tt33408190", 2021, "Podcast Series"),
                Triple("tt7794378", 2018, "Short"),
                Triple("tt0124102", 1998, "Movie")
            )
        )
        assertEquals("tt0124102", chosen)
    }

    /**
     * Der Beleg fuer den Ausschluss: nur Podcast und Videospiel im Angebot, beide mit
     * passendem Jahr. Ohne das Aussortieren wuerde hier eines von beiden gewaehlt und
     * stuende anschliessend als Filminfo auf dem Bildschirm.
     */
    @Test
    fun nothing_usable_yields_nothing() {
        val chosen = pick(
            "Irgendwas.[2001].mp4",
            listOf(
                Triple("tt1", 2001, "Podcast Series"),
                Triple("tt2", 2001, "Video Game")
            )
        )
        assertEquals(null, chosen)
    }
}
