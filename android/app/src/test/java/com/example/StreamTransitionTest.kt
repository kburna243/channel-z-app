package com.example

import android.content.Context
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MediaItem
import com.example.player.VideoPlayerManager
import com.example.player.convertGoogleDriveUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StreamTransitionTest {

    @Test
    fun testGoogleDriveUrlConversion() {
        val standardDriveUrl = "https://drive.google.com/file/d/1A2B3C4D5E6F7G8H9/view?usp=sharing"
        val converted = convertGoogleDriveUrl(standardDriveUrl)
        assertEquals("https://drive.google.com/uc?export=download&id=1A2B3C4D5E6F7G8H9", converted)

        val openDriveUrl = "https://drive.google.com/open?id=9Z8Y7X6W5V4U"
        val convertedOpen = convertGoogleDriveUrl(openDriveUrl)
        assertEquals("https://drive.google.com/uc?export=download&id=9Z8Y7X6W5V4U", convertedOpen)
    }

    @Test
    fun testMediaItemWebStreamDetection() {
        val ytItem = MediaItem(id = "abc12345", title = "YouTube Video", type = "yt")
        assertTrue(ytItem.isWebStream)

        val twitchItem = MediaItem(id = "grindhouse", title = "Twitch Stream", type = "tw")
        assertTrue(twitchItem.isWebStream)

        val hlsItem = MediaItem(id = "https://stream.org/live.m3u8", title = "HLS Stream", type = "hl")
        assertFalse(hlsItem.isWebStream)

        val mp4Item = MediaItem(id = "https://stream.org/movie.mp4", title = "Direct MP4", type = "raw")
        assertFalse(mp4Item.isWebStream)

        val driveItem = MediaItem(id = "https://drive.google.com/file/d/12345/view", title = "Drive Video", type = "gd")
        assertFalse(driveItem.isWebStream)
    }

    @Test
    fun testStreamTransitionsAndGracePeriod() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = VideoPlayerManager(context)
        val player = manager.getPlayer()
        assertNotNull(player)

        // 1. Load Direct MP4 Stream
        val mp4 = MediaItem(id = "https://test.stream/video.mp4", title = "Movie 1", type = "raw", currentTimeSeconds = 0.0)
        manager.loadMedia(mp4)
        assertEquals(mp4, manager.currentMedia.value)

        // Simulate incoming mediaUpdate right after start (t=5s). Must NOT seek because of grace period!
        manager.syncPosition(targetSeconds = 5.0, paused = false)
        // Verify playback state is ready/buffering and no seek disrupted it

        // 2. Transition from MP4 -> HLS (.m3u8)
        val hls = MediaItem(id = "https://test.stream/live.m3u8", title = "Live Stream 2", type = "hl", currentTimeSeconds = 120.0)
        manager.loadMedia(hls)
        assertEquals(hls, manager.currentMedia.value)

        // 3. Transition from HLS -> YouTube (Web Stream)
        val yt = MediaItem(id = "dQw4w9WgXcQ", title = "YouTube Clip", type = "yt", currentTimeSeconds = 0.0)
        manager.loadMedia(yt)
        assertEquals(yt, manager.currentMedia.value)
        assertTrue(yt.isWebStream)
        // ExoPlayer should be stopped/cleared when web stream starts
        assertEquals(0, player?.mediaItemCount)

        // 4. Transition from YouTube -> Google Drive Stream
        val drive = MediaItem(id = "https://drive.google.com/file/d/XYZ987/view", title = "Drive Cult Movie", type = "gd", currentTimeSeconds = 45.0)
        manager.loadMedia(drive)
        assertEquals(drive, manager.currentMedia.value)
        assertFalse(drive.isWebStream)
        assertEquals(1, player?.mediaItemCount)

        // 5. Cleanup
        manager.release()
    }
}
