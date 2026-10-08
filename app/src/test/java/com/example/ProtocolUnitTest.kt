package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.controller.settings.ControllerSettingsManager
import com.example.network.LudoNetworkProtocol
import com.example.network.NetworkMessageType
import com.example.network.NetworkPacket
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
class ProtocolUnitTest {

    @Test
    fun testHandshakeSerializationAndDeserialization() {
        val handshake = NetworkPacket.createHandshake("hs_123")
        val json = handshake.toJson()
        val parsed = NetworkPacket.fromJson(json)

        assertNotNull(parsed)
        assertEquals(NetworkMessageType.HANDSHAKE, parsed?.type)
        assertEquals(LudoNetworkProtocol.ROLE_CONTROLLER, parsed?.role)
        assertEquals("hs_123", parsed?.requestId)
    }

    @Test
    fun testNumberSelectionMessageCreation() {
        val msg = NetworkPacket.createNumberSelection(
            boxId = "B2",
            channel = 2,
            value = 6,
            requestId = "req_98431"
        )
        val json = msg.toJson()
        val parsed = NetworkPacket.fromJson(json)

        assertNotNull(parsed)
        assertEquals(NetworkMessageType.NUMBER_SELECTION, parsed?.type)
        assertEquals("B2", parsed?.boxId)
        assertEquals(2, parsed?.channel)
        assertEquals(6, parsed?.value)
        assertEquals("req_98431", parsed?.requestId)
    }

    @Test
    fun testPingPongPackets() {
        val ping = NetworkPacket.createPing("ping_1")
        val pong = NetworkPacket.createPong("pong_1")

        assertEquals(NetworkMessageType.PING, NetworkPacket.fromJson(ping.toJson())?.type)
        assertEquals(NetworkMessageType.PONG, NetworkPacket.fromJson(pong.toJson())?.type)
    }

    @Test
    fun testControllerSettingsManagerDefaultsAndUpdates() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = ControllerSettingsManager(context)

        // Verify default settings
        assertTrue(manager.settings.value.hapticEnabled)
        assertFalse(manager.settings.value.soundEffectsEnabled)
        assertTrue(manager.settings.value.animationsEnabled)
        assertTrue(manager.settings.value.rememberLastMode)

        // Update settings
        manager.setHapticEnabled(false)
        assertFalse(manager.settings.value.hapticEnabled)

        manager.setAnimationsEnabled(false)
        assertFalse(manager.settings.value.animationsEnabled)

        manager.setLastMode("PRO")
        assertEquals("PRO", manager.settings.value.lastMode)
    }
}
