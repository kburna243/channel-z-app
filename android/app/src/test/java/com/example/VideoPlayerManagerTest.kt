package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MediaItem
import com.example.player.VideoPlayerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VideoPlayerManagerTest {

    @Test
    fun `videoPlayerManager initializes exoPlayer and handles media load and lifecycle`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = VideoPlayerManager(context)

        assertNotNull(manager.getPlayer())

        val testMedia = MediaItem(
            id = "https://test.stream/video.m3u8",
            title = "Test Cult Movie",
            type = "hls"
        )
        manager.loadMedia(testMedia)
        assertEquals(testMedia, manager.currentMedia.value)

        // Test lifecycle
        manager.onStart()

        // Test syncPosition
        manager.syncPosition(targetSeconds = 120.0, paused = false)
        manager.syncPosition(targetSeconds = 120.0, paused = true)
        assertEquals(false, manager.isPlaying.value)

        manager.onStop()
        manager.release()
    }
}
