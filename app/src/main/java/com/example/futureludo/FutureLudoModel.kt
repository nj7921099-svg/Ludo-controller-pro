package com.example.futureludo

import com.example.network.LudoNetworkProtocol
import com.example.network.NetworkMessageType
import com.example.network.NetworkPacket

/**
 * Architectural bridge preparing Controller-BT for seamless evolution into a Ludo companion/controller.
 * When Ludo-app operates, the controller channels (B1..B6 / R1..R6) cleanly map to Player Tokens / Channels and Game Events.
 */
enum class LudoPlayerColor(val hex: Long, val displayName: String, val channelIndex: Int) {
    RED(0xFFFF3366, "Red Player", 1),
    GREEN(0xFF00E676, "Green Player", 2),
    YELLOW(0xFFFFD600, "Yellow Player", 3),
    BLUE(0xFF2979FF, "Blue Player", 4),
    PURPLE(0xFFAA00FF, "Purple Player", 5),
    ORANGE(0xFFFF6D00, "Orange Player", 6);

    companion object {
        fun fromChannel(channel: Int): LudoPlayerColor {
            return entries.firstOrNull { it.channelIndex == channel } ?: RED
        }
    }
}

data class FutureLudoTokenState(
    val tokenId: String,
    val playerColor: LudoPlayerColor,
    val positionOnBoard: Int = 0,
    val isHome: Boolean = true,
    val isFinished: Boolean = false
)

data class FutureLudoGameState(
    val activeColorTurn: LudoPlayerColor = LudoPlayerColor.RED,
    val lastDiceValue: Int = 6,
    val isGameActive: Boolean = false,
    val gamePhase: String = "READY_TO_START"
) {
    fun toNetworkPacket(): NetworkPacket {
        return NetworkPacket(
            type = NetworkMessageType.GAME_EVENT,
            protocolVersion = LudoNetworkProtocol.PROTOCOL_VERSION,
            role = LudoNetworkProtocol.ROLE_CONTROLLER,
            message = "GAME_SYNC",
            reason = "{\"turn\":\"${activeColorTurn.name}\",\"dice\":$lastDiceValue,\"phase\":\"$gamePhase\"}"
        )
    }
}
