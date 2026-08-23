package com.example

import com.example.data.model.PrivateMessage
import com.example.data.model.WebQueueOtpState
import com.example.data.repository.SettingsRepository
import com.example.data.webqueue.WebQueueApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WebQueueAuthTest {

    @Test
    fun `extracts 6-digit OTP code from Kryten PM message`() {
        val testMessages = listOf(
            "Your Channel-Z WebQueue login code is 849201. Do not share it." to "849201",
            "Verification code: 123456" to "123456",
            "Code 009812 expires in 5 minutes" to "009812",
            "[Kryten] OTP: 654321" to "654321"
        )

        val otpRegex = Regex("""\b(\d{6})\b""")

        for ((msg, expectedCode) in testMessages) {
            val match = otpRegex.find(msg)
            assertNotNull("Should find 6-digit code in: '$msg'", match)
            assertEquals("Extracted code should match expected", expectedCode, match?.groupValues?.get(1))
        }
    }

    @Test
    fun `persists and restores first run completed flag in SettingsRepository`() {
        val context = RuntimeEnvironment.getApplication()
        val repo = SettingsRepository(context)

        assertFalse("First run should initially be false", repo.isFirstRunCompleted())

        repo.setFirstRunCompleted(true)
        assertTrue("First run should now be true", repo.isFirstRunCompleted())

        repo.setFirstRunCompleted(false)
        assertFalse("First run should be false after reset", repo.isFirstRunCompleted())
    }

    @Test
    fun `persists and clears WebQueue session cookies in SettingsRepository`() {
        val context = RuntimeEnvironment.getApplication()
        val repo = SettingsRepository(context)

        val sampleCookies = """[{"name":"session_id","value":"xyz123","domain":"queue.dropsugar.co","path":"/","expiresAt":9999999999999,"secure":true,"httpOnly":true,"persistent":true}]"""

        repo.saveWebQueueCookies(sampleCookies)
        assertEquals(sampleCookies, repo.webQueueCookies())

        repo.clearWebQueueCookies()
        assertEquals(null, repo.webQueueCookies())
    }
}
