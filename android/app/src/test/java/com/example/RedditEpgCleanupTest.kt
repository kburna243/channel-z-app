package com.example

import com.example.data.scraper.DataScraper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression: der Reddit-EPG-Fallback hat maskiertes HTML in den Sendeplan-Kasten geschrieben.
 * Der Text erscheint dort in Monospace, entsprechend sah es nach Quelltext aus.
 *
 * Ursache war die Reihenfolge in cleanHtmlEntities — Tags entfernen, bevor die Entities aufgeloest
 * waren — plus ein Regex, dessen Zeichenklasse "[^&gt;]" die Zeichen & g t ; ausschloss statt der
 * Folge "&gt;". Was der kaputte Ausdruck stehen liess, wurde anschliessend zu echten spitzen
 * Klammern demaskiert und landete als Text in der UI.
 */
class RedditEpgCleanupTest {

    private val scraper = DataScraper(CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
        .also { it.stopScraping() }

    /** Auszug, wie Reddit den Beitragstext im RSS-content ausliefert. */
    private val redditRssContent =
        "&lt;!-- SC_OFF --&gt;&lt;div class=\"md\"&gt;&lt;p&gt;22:00 Raw Force&lt;/p&gt;" +
        "&lt;p&gt;23:23 New York Ripper&lt;/p&gt;&lt;/div&gt;&lt;!-- SC_ON --&gt; " +
        "&lt;a href=\"https://reddit.com/r/420grindhouse\"&gt;[link]&lt;/a&gt;"

    private val markupMarkers =
        listOf("&lt;", "&gt;", "&amp;", "&quot;", "<div", "</p>", "<p>", "<a ", "SC_OFF", "SC_ON")

    @Test
    fun escaped_markup_is_removed_not_just_unescaped() {
        val out = scraper.cleanHtmlEntities(redditRssContent)
        for (marker in markupMarkers) {
            assertFalse("Markup uebrig geblieben: $marker\n---\n$out", out.contains(marker))
        }
        assertTrue("Inhalt verloren:\n$out", out.contains("Raw Force") && out.contains("New York Ripper"))
    }

    @Test
    fun schedule_text_stays_readable_after_markdown_cleanup() {
        val out = scraper.cleanMarkdownText(redditRssContent)
        for (marker in markupMarkers) {
            assertFalse("Markup uebrig geblieben: $marker\n---\n$out", out.contains(marker))
        }
        assertTrue("Inhalt verloren:\n$out", out.contains("Raw Force"))
        assertFalse("Reddit-Link nicht entfernt:\n$out", out.contains("reddit.com"))
    }

    /** Reddit maskiert stellenweise doppelt — "&amp;#32;" muss zum Leerzeichen werden. */
    @Test
    fun double_escaped_entities_are_resolved() {
        val out = scraper.cleanHtmlEntities("Raw&amp;#32;Force &amp;amp; New York Ripper")
        assertFalse("Entity uebrig:\n$out", out.contains("&#32;") || out.contains("&amp;"))
        assertTrue("Erwartet 'Raw Force':\n$out", out.contains("Raw Force"))
    }
}
