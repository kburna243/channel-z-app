package com.example

import com.example.ui.metadata.progressFraction
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Der Fuellstand kommt aus fremden Zahlen: CyTube meldet die Position ueber mediaUpdate, und
 * die passt nicht immer zur gemeldeten Laufzeit. Ein Balken, der ueber den Rand hinauslaeuft
 * oder mit einer Division durch null abstuerzt, faellt erst auf dem Fernseher auf.
 */
class ProgressFractionTest {

    @Test
    fun `die haelfte eines films fuellt den halben balken`() {
        assertEquals(0.5f, progressFraction(2700.0, 5400.0), 0.001f)
    }

    @Test
    fun `livestreams ohne laufzeit fuellen nichts`() {
        assertEquals(0f, progressFraction(1234.0, 0.0), 0f)
    }

    @Test
    fun `eine position jenseits der laufzeit bleibt am rand stehen`() {
        assertEquals(1f, progressFraction(6000.0, 5400.0), 0f)
    }

    @Test
    fun `eine negative position faellt auf den anfang zurueck`() {
        assertEquals(0f, progressFraction(-12.0, 5400.0), 0f)
    }
}
