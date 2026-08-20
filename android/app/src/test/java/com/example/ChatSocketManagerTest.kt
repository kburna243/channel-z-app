package com.example

import com.example.data.model.ConnectionStatus
import com.example.data.socket.ChatSocketManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatSocketManagerTest {

    @Test
    fun `chatSocketManager initializes with empty state and handles lifecycle`() {
        val testDispatcher = UnconfinedTestDispatcher()
        val testScope = TestScope(testDispatcher)
        val chatManager = ChatSocketManager(scope = testScope)

        assertNotNull(chatManager.chatMessages)
        assertEquals(emptyList<Any>(), chatManager.chatMessages.value)
        assertEquals(ConnectionStatus.OFFLINE, chatManager.connectionStatus.value)

        chatManager.clearChat()
        assertEquals(0, chatManager.chatMessages.value.size)

        chatManager.disconnect()
        assertEquals(ConnectionStatus.OFFLINE, chatManager.connectionStatus.value)
    }
}
