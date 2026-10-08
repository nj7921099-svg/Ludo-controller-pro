package com.example.network

import org.json.JSONObject
import java.util.UUID

/**
 * Common Protocol specification for Ludo Controller (App 2) and Ludo Host (App 1).
 *
 * Rules:
 * - Protocol Version: 1
 * - Format: Newline-delimited JSON
 * - Role: CONTROLLER <-> LUDO_HOST
 */
object LudoNetworkProtocol {
    const val PROTOCOL_VERSION = 1
    const val ROLE_CONTROLLER = "CONTROLLER"
    const val ROLE_LUDO_HOST = "LUDO_HOST"

    const val NSD_SERVICE_TYPE = "_ludo._tcp."
    const val NSD_SERVICE_NAME = "LudoHost"
    const val LUDO_TCP_PORT = 8888
    const val DEFAULT_PORT = 8888

    const val MIN_BOX_COUNT = 2
    const val MAX_BOX_COUNT = 6
    const val MIN_DICE_VALUE = 1
    const val MAX_DICE_VALUE = 6
}

enum class NetworkMessageType {
    HANDSHAKE,
    HANDSHAKE_ACK,
    PING,
    PONG,
    ACK,
    ERROR,
    GET_CONFIG,
    CONFIG,
    STATE_SYNC,
    NUMBER_SELECTION,
    NUMBER_RESULT,
    GAME_EVENT,
    DISCONNECT,
    RECONNECT,
    UNKNOWN;

    companion object {
        fun fromString(type: String?): NetworkMessageType {
            if (type == null) return UNKNOWN
            val clean = type.trim().uppercase()
            return when (clean) {
                "HANDSHAKE" -> HANDSHAKE
                "HANDSHAKE_ACK", "HANDSHAKEACK" -> HANDSHAKE_ACK
                "PING" -> PING
                "PONG" -> PONG
                "ACK", "NUMBER_SELECTION_ACK" -> ACK
                "ERROR", "NACK" -> ERROR
                "GET_CONFIG", "REQUEST_CONFIG" -> GET_CONFIG
                "CONFIG", "GAME_CONFIGURATION" -> CONFIG
                "STATE_SYNC", "SYNC" -> STATE_SYNC
                "NUMBER_SELECTION", "SELECT_NUMBER" -> NUMBER_SELECTION
                "NUMBER_RESULT", "RESULT" -> NUMBER_RESULT
                "GAME_EVENT", "LUDO_EVENT" -> GAME_EVENT
                "DISCONNECT" -> DISCONNECT
                "RECONNECT" -> RECONNECT
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Universal JSON Protocol Packet.
 */
data class NetworkPacket(
    val type: NetworkMessageType,
    val protocolVersion: Int = LudoNetworkProtocol.PROTOCOL_VERSION,
    val role: String = LudoNetworkProtocol.ROLE_CONTROLLER,
    val requestId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String? = null,
    val value: Int? = null,
    val boxId: String? = null,
    val channel: Int? = null,
    val boxCount: Int? = null,
    val message: String? = null,
    val errorCode: String? = null,
    val reason: String? = null,
    val rawJson: String? = null
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("type", type.name)
        json.put("protocolVersion", protocolVersion)
        json.put("role", role)
        json.put("timestamp", timestamp)

        if (requestId != null) json.put("requestId", requestId)
        if (status != null) json.put("status", status)
        if (value != null) json.put("value", value)
        if (boxId != null) json.put("boxId", boxId)
        if (channel != null) json.put("channel", channel)
        if (boxCount != null) json.put("boxCount", boxCount)
        if (message != null) json.put("message", message)
        if (errorCode != null) json.put("errorCode", errorCode)
        if (reason != null) json.put("reason", reason)

        return json.toString()
    }

    fun toTransmissionString(): String = toJson() + "\n"

    companion object {
        fun fromJson(jsonStr: String): NetworkPacket? {
            val clean = jsonStr.trim()
            if (clean.isEmpty()) return null
            return try {
                val json = JSONObject(clean)
                val typeStr = json.optString("type", "")
                val type = NetworkMessageType.fromString(typeStr)
                val version = json.optInt("protocolVersion", 1)
                val role = json.optString("role", "")
                val requestId = if (json.has("requestId")) json.getString("requestId") else null
                val timestamp = json.optLong("timestamp", System.currentTimeMillis())
                val status = if (json.has("status")) json.getString("status") else null
                val value = when {
                    json.has("value") -> json.getInt("value")
                    json.has("result") -> json.getInt("result")
                    json.has("number") -> json.getInt("number")
                    else -> null
                }
                val rawBoxId = when {
                    json.has("boxId") -> json.getString("boxId")
                    json.has("controllerBoxId") -> json.getString("controllerBoxId")
                    else -> null
                }
                val channel = when {
                    json.has("channel") -> json.getInt("channel")
                    rawBoxId != null -> parseChannel(rawBoxId)
                    else -> null
                }
                val boxCount = when {
                    json.has("boxCount") -> json.getInt("boxCount")
                    json.has("count") -> json.getInt("count")
                    json.has("totalBoxes") -> json.getInt("totalBoxes")
                    else -> null
                }
                val message = if (json.has("message")) json.getString("message") else null
                val errorCode = if (json.has("errorCode")) json.getString("errorCode") else null
                val reason = if (json.has("reason")) json.getString("reason") else null

                NetworkPacket(
                    type = type,
                    protocolVersion = version,
                    role = role,
                    requestId = requestId,
                    timestamp = timestamp,
                    status = status,
                    value = value,
                    boxId = rawBoxId,
                    channel = channel,
                    boxCount = boxCount,
                    message = message,
                    errorCode = errorCode,
                    reason = reason,
                    rawJson = clean
                )
            } catch (e: Exception) {
                null
            }
        }

        fun parseChannel(raw: String): Int? {
            val s = raw.trim().uppercase()
            if (s.startsWith("B") || s.startsWith("R")) {
                return s.substring(1).toIntOrNull()
            }
            return s.toIntOrNull()
        }

        fun createHandshake(requestId: String = "hs_${UUID.randomUUID().toString().take(8)}"): NetworkPacket =
            NetworkPacket(
                type = NetworkMessageType.HANDSHAKE,
                protocolVersion = LudoNetworkProtocol.PROTOCOL_VERSION,
                role = LudoNetworkProtocol.ROLE_CONTROLLER,
                requestId = requestId,
                timestamp = System.currentTimeMillis()
            )

        fun createPing(requestId: String = "ping_${UUID.randomUUID().toString().take(8)}"): NetworkPacket =
            NetworkPacket(
                type = NetworkMessageType.PING,
                protocolVersion = LudoNetworkProtocol.PROTOCOL_VERSION,
                role = LudoNetworkProtocol.ROLE_CONTROLLER,
                requestId = requestId,
                timestamp = System.currentTimeMillis()
            )

        fun createPong(requestId: String?): NetworkPacket =
            NetworkPacket(
                type = NetworkMessageType.PONG,
                protocolVersion = LudoNetworkProtocol.PROTOCOL_VERSION,
                role = LudoNetworkProtocol.ROLE_CONTROLLER,
                requestId = requestId,
                timestamp = System.currentTimeMillis()
            )

        fun createNumberSelection(
            boxId: String,
            channel: Int,
            value: Int,
            requestId: String = "cmd_${channel}_${UUID.randomUUID().toString().take(8)}"
        ): NetworkPacket =
            NetworkPacket(
                type = NetworkMessageType.NUMBER_SELECTION,
                protocolVersion = LudoNetworkProtocol.PROTOCOL_VERSION,
                role = LudoNetworkProtocol.ROLE_CONTROLLER,
                boxId = boxId,
                channel = channel,
                value = value,
                requestId = requestId,
                timestamp = System.currentTimeMillis()
            )

        fun createGetConfig(requestId: String = "cfg_${UUID.randomUUID().toString().take(8)}"): NetworkPacket =
            NetworkPacket(
                type = NetworkMessageType.GET_CONFIG,
                protocolVersion = LudoNetworkProtocol.PROTOCOL_VERSION,
                role = LudoNetworkProtocol.ROLE_CONTROLLER,
                requestId = requestId,
                timestamp = System.currentTimeMillis()
            )

        fun createDisconnect(reason: String = "Controller disconnecting"): NetworkPacket =
            NetworkPacket(
                type = NetworkMessageType.DISCONNECT,
                protocolVersion = LudoNetworkProtocol.PROTOCOL_VERSION,
                role = LudoNetworkProtocol.ROLE_CONTROLLER,
                reason = reason,
                timestamp = System.currentTimeMillis()
            )
    }
}
