package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gespeicherte Chat-Zugangsdaten: die Grundlage fuer Auto-Login und Gastmodus.
 * Jeder Test bekommt von Robolectric eine frische Anwendung, die Prefs sind also
 * pro Test leer — wie beim Erststart der App.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatCredentialsTest {

    private fun freshRepo(): SettingsRepository =
        SettingsRepository(ApplicationProvider.getApplicationContext())

    @Test
    fun `ohne gespeicherten Zugang gibt es keine Zugangsdaten`() {
        assertNull(freshRepo().chatCredentials())
    }

    @Test
    fun `Konto mit Passwort wird gespeichert und ueberlebt den Neustart`() {
        val repo = freshRepo()
        repo.saveChatCredentials("mike", "streng-geheim")
        assertEquals("mike" to "streng-geheim", repo.chatCredentials())
        // Eine neue Repository-Instanz liest denselben Speicher — genau der Punkt
        // des Auto-Logins: beim naechsten App-Start sind die Daten noch da.
        assertEquals("mike" to "streng-geheim", freshRepo().chatCredentials())
    }

    @Test
    fun `Gastzugang hat ein leeres Passwort`() {
        val repo = freshRepo()
        repo.saveChatCredentials("gast77", "")
        assertEquals("gast77" to "", repo.chatCredentials())
    }

    @Test
    fun `Abmelden loescht die Zugangsdaten`() {
        val repo = freshRepo()
        repo.saveChatCredentials("mike", "streng-geheim")
        repo.clearChatCredentials()
        assertNull(repo.chatCredentials())
    }
}
