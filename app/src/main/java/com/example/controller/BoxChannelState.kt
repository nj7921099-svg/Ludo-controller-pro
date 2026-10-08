package com.example.controller

/**
 * Communication status of an individual controller box command.
 * Note: This is purely a communication state, NOT a game turn.
 */
enum class BoxCommandStatus {
    READY,          // Box is idle, ready for user to tap
    SELECTING,      // User has tapped this box; number panel is open
    SENDING,        // Number chosen; sending over Wi-Fi
    SENT,           // Successfully transmitted to App 1 (pending on App 1)
    ACKNOWLEDGED,   // App 1 acknowledged receipt of the command
    REVEALED        // App 1 has revealed the number on its screen
}

/**
 * State of one controller box (B1..B6).
 * Directly maps to App 1's R1..R6.
 * Any box can be tapped at any time, independently.
 */
data class BoxChannelState(
    val controllerId: String,               // "B1", "B2", ...
    val app1Id: String,                     // "R1", "R2", ...
    val channel: Int,                       // 1..6
    val status: BoxCommandStatus = BoxCommandStatus.READY,
    val isNumberPanelOpen: Boolean = false, // True if number picker is open for this box
    val pendingCommandNumber: Int? = null,  // Number selected and sent
    val lastCommandId: String? = null,      // Unique command ID for duplicate prevention
    val revealedValue: Int? = null,         // Value revealed on App 1
    val lastUpdated: Long = System.currentTimeMillis(),
    val rollHistory: List<Int> = emptyList(),
    val errorMessage: String? = null
) {
    val boxId: String get() = controllerId
    val value: Int? get() = revealedValue ?: pendingCommandNumber
    val hasValue: Boolean get() = value != null

    companion object {
        fun createDefault(channel: Int): BoxChannelState {
            val c = channel.coerceIn(1, 4)
            return BoxChannelState(
                controllerId = "B$c",
                app1Id = "R$c",
                channel = c,
                status = BoxCommandStatus.READY
            )
        }
    }
}

/**
 * Log entry for protocol and event tracking in debug / inspector area.
 */
data class ProtocolLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val direction: Direction,
    val type: String,
    val summary: String,
    val fullJson: String,
    val commandId: String? = null
) {
    enum class Direction {
        INBOUND,
        OUTBOUND,
        SYSTEM
    }
}
