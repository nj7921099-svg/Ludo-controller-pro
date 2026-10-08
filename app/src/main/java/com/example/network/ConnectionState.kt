package com.example.network

/**
 * Explicit Connection State Machine for Wi-Fi Controller Client.
 *
 * Requirements:
 * IDLE -> NETWORK_UNAVAILABLE -> HOTSPOT_CHECK -> DISCOVERING -> DISCOVERED ->
 * CONNECTING -> SOCKET_CONNECTED -> VERIFYING -> CONNECTED -> DISCONNECTED ->
 * RECONNECTING -> ERROR
 *
 * The UI must show CONNECTED only after:
 * TCP connection + Handshake + Handshake ACK + Ping/Pong
 */
enum class ConnectionState(val displayName: String) {
    IDLE("Idle"),
    NETWORK_UNAVAILABLE("Network Unavailable"),
    HOTSPOT_CHECK("Checking Network / Hotspot"),
    DISCOVERING("Searching for Ludo Host..."),
    DISCOVERED("Ludo Host Found"),
    CONNECTING("Connecting via TCP..."),
    SOCKET_CONNECTED("TCP Socket Connected"),
    VERIFYING("Verifying Handshake & Ping..."),
    CONNECTED("Connected"),
    DISCONNECTED("Not Connected"),
    RECONNECTING("Reconnecting..."),
    ERROR("Connection Error");

    val isConnected: Boolean
        get() = this == CONNECTED

    val isConnecting: Boolean
        get() = this in listOf(
            HOTSPOT_CHECK,
            DISCOVERING,
            DISCOVERED,
            CONNECTING,
            SOCKET_CONNECTED,
            VERIFYING,
            RECONNECTING
        )

    val isDisconnected: Boolean
        get() = !isConnected && !isConnecting
}

/**
 * Top-level indicator state for global top-left badge.
 */
enum class GlobalIndicatorStatus {
    DISCONNECTED, // 🔴 Red
    CONNECTING,   // 🟡 Yellow
    CONNECTED;    // 🟢 Green

    companion object {
        fun fromState(state: ConnectionState): GlobalIndicatorStatus = when {
            state.isConnected -> CONNECTED
            state.isConnecting -> CONNECTING
            else -> DISCONNECTED
        }
    }
}
