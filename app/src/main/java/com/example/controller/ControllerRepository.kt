package com.example.controller

import android.util.Log
import com.example.futureludo.FutureLudoGameState
import com.example.network.ConnectionState
import com.example.network.LudoNetworkProtocol
import com.example.network.NetworkMessageType
import com.example.network.NetworkPacket
import com.example.network.SendResult
import com.example.network.WifiConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * Controller Repository (App 2) using Wi-Fi / TCP Architecture.
 *
 * Rules:
 * 1. NO controller-side turn order, NO clockwise turn system.
 * 2. The user can tap ANY detected box at ANY time (B1..B6).
 * 3. Tapping a box opens the number selection panel specifically for that box.
 * 4. Selecting a number sends ONE unique NUMBER_SELECTION command to Ludo Host (App 1).
 * 5. NO automatic loops, NO repeating signals.
 * 6. Multiple independent pending commands are allowed simultaneously across boxes.
 * 7. When App 1 sends ACK or NUMBER_RESULT, App 2 updates that specific box.
 * 8. Connection-Aware: If not connected, does NOT silently send commands.
 */
class ControllerRepository(
    val wifiManager: WifiConnectionManager,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "LudoControllerRepo"
    }

    // Dynamic box count (2..4). Defaults to 4 until Ludo Host sends config
    private val _boxCount = MutableStateFlow(4)
    val boxCount: StateFlow<Int> = _boxCount.asStateFlow()

    // Map of active controller boxes: "B1".."B6"
    private val _boxes = MutableStateFlow<Map<String, BoxChannelState>>(emptyMap())
    val boxes: StateFlow<Map<String, BoxChannelState>> = _boxes.asStateFlow()

    // Which box currently has its number selection panel open (e.g. "B1" or null)
    private val _selectedBoxForInput = MutableStateFlow<String?>("B1")
    val selectedBoxForInput: StateFlow<String?> = _selectedBoxForInput.asStateFlow()

    // Most recent command info for UI (e.g. "B4 → 6")
    private val _lastSentInfo = MutableStateFlow<Pair<String, Int>?>(null)
    val lastSentInfo: StateFlow<Pair<String, Int>?> = _lastSentInfo.asStateFlow()

    private val _lastReceivedInfo = MutableStateFlow<String?>("Ready")
    val lastReceivedInfo: StateFlow<String?> = _lastReceivedInfo.asStateFlow()

    private val _lastResultStatus = MutableStateFlow("Ready")
    val lastResultStatus: StateFlow<String> = _lastResultStatus.asStateFlow()

    // Status alert message banner
    private val _systemAlert = MutableStateFlow<String?>(null)
    val systemAlert: StateFlow<String?> = _systemAlert.asStateFlow()

    // Simulation mode toggle for test scenarios
    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    // Protocol packet logs for collapsible debug area
    private val _logs = MutableStateFlow<List<ProtocolLogEntry>>(emptyList())
    val logs: StateFlow<List<ProtocolLogEntry>> = _logs.asStateFlow()

    // Map of dispatched command IDs to prevent accidental duplicate sends
    private val dispatchedCommands = ConcurrentHashMap<String, Boolean>()
    // Set of completed command IDs to prevent duplicate response processing
    private val completedCommands = ConcurrentHashMap.newKeySet<String>()
    private val sendingLock = AtomicBoolean(false)

    // Future Ludo companion state
    private val _futureLudoState = MutableStateFlow(FutureLudoGameState())
    val futureLudoState: StateFlow<FutureLudoGameState> = _futureLudoState.asStateFlow()

    init {
        // Initialize default boxes (B1..B4)
        rebuildBoxes(_boxCount.value)

        // Observe network connection states
        scope.launch {
            wifiManager.connectionState.collect { state ->
                handleConnectionStateChanged(state)
            }
        }

        // Observe incoming network packets from Ludo Host
        scope.launch {
            wifiManager.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }
    }

    private fun handleConnectionStateChanged(state: ConnectionState) {
        addLog(
            ProtocolLogEntry.Direction.SYSTEM,
            "STATE",
            "Wi-Fi State: ${state.name}",
            state.displayName
        )

        when (state) {
            ConnectionState.CONNECTED -> {
                _systemAlert.value = "🟢 Connected to Ludo Host"
                requestSyncConfig()
            }
            ConnectionState.DISCONNECTED -> {
                _systemAlert.value = "🔴 Connection Lost"
            }
            ConnectionState.ERROR -> {
                val err = wifiManager.diagnostics.value.lastErrorReason ?: "Connection error"
                _systemAlert.value = "⚠️ Error: $err"
            }
            else -> {}
        }
    }

    private fun handleIncomingPacket(packet: NetworkPacket) {
        addLog(
            ProtocolLogEntry.Direction.INBOUND,
            packet.type.name,
            "Received: ${packet.boxId ?: ""} value=${packet.value ?: ""}",
            packet.toJson(),
            packet.requestId
        )

        when (packet.type) {
            NetworkMessageType.CONFIG, NetworkMessageType.STATE_SYNC -> {
                packet.boxCount?.let { count ->
                    applyBoxCountFromApp1(count, "Ludo Host Config")
                }
            }

            NetworkMessageType.ACK -> {
                handleAcknowledgement(packet)
            }

            NetworkMessageType.NUMBER_RESULT -> {
                handleNumberResult(packet)
            }

            NetworkMessageType.ERROR -> {
                _systemAlert.value = "Ludo Host Error: ${packet.message ?: "Unknown error"}"
            }

            else -> {}
        }
    }

    /**
     * Requirement 1: Dynamic Boxes (2..6).
     */
    fun applyBoxCountFromApp1(count: Int, source: String = "External") {
        if (count in LudoNetworkProtocol.MIN_BOX_COUNT..LudoNetworkProtocol.MAX_BOX_COUNT) {
            if (_boxCount.value != count) {
                Log.i(TAG, "Box count updated to $count from $source")
                _boxCount.value = count
                rebuildBoxes(count)
                _systemAlert.value = "Detected $count Boxes from Ludo Host"
            }
        } else {
            val err = "Rejected invalid boxCount: $count (Allowed: 2..6)"
            Log.e(TAG, err)
            _systemAlert.value = err
        }
    }

    /**
     * Requirement 2 & 3: ANY box can be tapped at ANY time!
     * When user taps B4, opens the number selection panel specifically for B4.
     */
    fun onBoxTap(controllerId: String) {
        val box = _boxes.value[controllerId] ?: return

        val currentMap = _boxes.value.toMutableMap()
        val isCurrentlyOpen = box.isNumberPanelOpen

        // Toggle or set selected box
        for ((key, b) in currentMap) {
            if (key == controllerId) {
                currentMap[key] = b.copy(
                    isNumberPanelOpen = !isCurrentlyOpen,
                    status = if (!isCurrentlyOpen) BoxCommandStatus.SELECTING else b.status
                )
            } else {
                currentMap[key] = b.copy(isNumberPanelOpen = false)
            }
        }
        _boxes.value = currentMap
        _selectedBoxForInput.value = if (!isCurrentlyOpen) controllerId else null
    }

    /**
     * Requirement 4, 13 & 14: NUMBER SELECTION & CONNECTION-AWARE CONTROLS.
     * When user selects a number (e.g. B4 -> 6):
     * - If not connected: shows "🔴 Not Connected. Connect to Ludo Host first."
     * - If connected: sends NUMBER_SELECTION over Wi-Fi TCP to Ludo Host.
     */
    fun sendNumberForBox(controllerId: String, number: Int): Boolean {
        if (number !in 1..6) return false

        val isConn = wifiManager.connectionState.value.isConnected || _isSimulationMode.value
        if (!isConn) {
            _systemAlert.value = "🔴 Not Connected. Connect to Ludo Host first."
            return false
        }

        val box = _boxes.value[controllerId] ?: return false

        // Prevent rapid double-tap re-entry
        if (!sendingLock.compareAndSet(false, true)) {
            return false
        }

        try {
            val commandId = "cmd_${controllerId}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

            // 1. Update box state immediately
            val currentMap = _boxes.value.toMutableMap()
            currentMap[controllerId] = box.copy(
                pendingCommandNumber = number,
                status = BoxCommandStatus.SENT,
                lastCommandId = commandId,
                isNumberPanelOpen = false,
                errorMessage = null
            )
            _boxes.value = currentMap

            _selectedBoxForInput.value = controllerId
            dispatchedCommands[commandId] = true

            // Update UI communication panel stats
            _lastSentInfo.value = Pair(controllerId, number)
            _lastReceivedInfo.value = "Result pending"
            _lastResultStatus.value = "Waiting for ACK..."

            // 2. Prepare Wi-Fi Protocol Message
            val packet = NetworkPacket.createNumberSelection(
                boxId = controllerId,
                channel = box.channel,
                value = number,
                requestId = commandId
            )

            addLog(
                ProtocolLogEntry.Direction.OUTBOUND,
                packet.type.name,
                "Sent to Ludo Host: $controllerId = $number",
                packet.toJson(),
                commandId
            )

            if (_isSimulationMode.value) {
                simulateApp1Response(controllerId, box.channel, number, commandId)
                return true
            }

            // 3. Send over Wi-Fi TCP (Confirmed Real Write)
            scope.launch {
                val writeResult = wifiManager.sendPacket(packet)
                if (writeResult !is SendResult.Success) {
                    _systemAlert.value = "Command write failed: ${writeResult.reason}"
                    val curr = _boxes.value.toMutableMap()
                    curr[controllerId]?.let { b ->
                        curr[controllerId] = b.copy(status = BoxCommandStatus.READY, errorMessage = writeResult.reason)
                        _boxes.value = curr
                    }
                } else {
                    _systemAlert.value = "Sent $controllerId → $number (TCP written)"
                }
            }
            return true
        } finally {
            sendingLock.set(false)
        }
    }

    /**
     * Ludo Host Acknowledgement handling.
     * Matches strictly by requestId.
     */
    private fun handleAcknowledgement(packet: NetworkPacket) {
        val reqId = packet.requestId
        if (reqId.isNullOrBlank()) {
            Log.w(TAG, "ACK ignored: missing requestId")
            return
        }
        val currentMap = _boxes.value.toMutableMap()
        var matched = false
        for ((key, b) in currentMap) {
            if (b.lastCommandId == reqId) {
                currentMap[key] = b.copy(status = BoxCommandStatus.ACKNOWLEDGED)
                _lastResultStatus.value = "Acknowledged"
                matched = true
            }
        }
        if (matched) {
            _boxes.value = currentMap
            addLog(
                ProtocolLogEntry.Direction.INBOUND,
                packet.type.name,
                "Acknowledged command $reqId",
                packet.toJson(),
                reqId
            )
        } else {
            Log.d(TAG, "ACK ignored: no outstanding command matches requestId '$reqId'")
        }
    }

    /**
     * Requirement: Strict NUMBER_RESULT Correlation.
     * 1. Authoritative key is requestId.
     * 2. NO fallback to boxId or channel if requestId is missing or unmatched.
     * 3. Must match an outstanding command's lastCommandId.
     * 4. If packet.boxId is present, verify it matches the target box (or corresponding channel);
     *    if it contradicts (e.g. sent for B4 but packet says B2), treat as protocol mismatch and reject.
     * 5. Duplicate responses cannot complete a command twice.
     * 6. Update ONLY the target box for that command.
     */
    private fun handleNumberResult(packet: NetworkPacket) {
        val incomingRequestId = packet.requestId
        if (incomingRequestId.isNullOrBlank()) {
            Log.w(TAG, "NUMBER_RESULT ignored: missing requestId in packet: ${packet.toJson()}")
            addLog(
                ProtocolLogEntry.Direction.INBOUND,
                packet.type.name,
                "Ignored NUMBER_RESULT: missing requestId",
                packet.toJson()
            )
            return
        }

        // Prevent duplicate completion if already completed
        if (completedCommands.contains(incomingRequestId)) {
            Log.w(TAG, "NUMBER_RESULT ignored: duplicate response for already-completed command $incomingRequestId")
            addLog(
                ProtocolLogEntry.Direction.INBOUND,
                packet.type.name,
                "Ignored duplicate NUMBER_RESULT for $incomingRequestId",
                packet.toJson(),
                incomingRequestId
            )
            return
        }

        val currentMap = _boxes.value.toMutableMap()

        // STRICT MATCH: find the box with this exact lastCommandId
        val matchingEntry = currentMap.entries.firstOrNull { it.value.lastCommandId == incomingRequestId }
        if (matchingEntry == null) {
            Log.w(TAG, "NUMBER_RESULT ignored: no outstanding command matches requestId '$incomingRequestId'")
            addLog(
                ProtocolLogEntry.Direction.INBOUND,
                packet.type.name,
                "Ignored unmatched/stale NUMBER_RESULT ($incomingRequestId)",
                packet.toJson(),
                incomingRequestId
            )
            return
        }

        val targetControllerId = matchingEntry.key
        val targetBox = matchingEntry.value

        // Box ID protocol check: if packet.boxId is provided, it must correspond to this target box
        val rawBoxId = packet.boxId?.trim()?.uppercase()
        if (!rawBoxId.isNullOrEmpty()) {
            val expectedBoxId = targetControllerId.uppercase() // e.g. "B4"
            val expectedRId = "R${targetBox.channel}" // e.g. "R4"
            val expectedChannelStr = "${targetBox.channel}" // e.g. "4"
            if (rawBoxId != expectedBoxId && rawBoxId != expectedRId && rawBoxId != expectedChannelStr) {
                val err = "NUMBER_RESULT protocol mismatch: command $incomingRequestId was sent for $targetControllerId, but received boxId=$rawBoxId"
                Log.e(TAG, err)
                addLog(
                    ProtocolLogEntry.Direction.INBOUND,
                    packet.type.name,
                    "Rejected protocol mismatch: $err",
                    packet.toJson(),
                    incomingRequestId
                )
                return
            }
        }

        val value = packet.value ?: return
        if (value !in 1..6) return

        // Mark command as completed to prevent duplicate processing
        completedCommands.add(incomingRequestId)

        val newHistory = (listOf(value) + targetBox.rollHistory).take(5)

        currentMap[targetControllerId] = targetBox.copy(
            revealedValue = value,
            status = BoxCommandStatus.REVEALED,
            rollHistory = newHistory,
            lastUpdated = System.currentTimeMillis()
        )
        _boxes.value = currentMap

        _lastReceivedInfo.value = "$targetControllerId Revealed $value"
        _lastResultStatus.value = "Confirmed ($value)"
        _systemAlert.value = "Revealed: $targetControllerId = $value"

        addLog(
            ProtocolLogEntry.Direction.INBOUND,
            packet.type.name,
            "Accepted NUMBER_RESULT for $targetControllerId: $value ($incomingRequestId)",
            packet.toJson(),
            incomingRequestId
        )
    }

    fun closeNumberPanel() {
        val currentMap = _boxes.value.toMutableMap()
        for ((key, b) in currentMap) {
            if (b.isNumberPanelOpen) {
                currentMap[key] = b.copy(isNumberPanelOpen = false)
            }
        }
        _boxes.value = currentMap
        _selectedBoxForInput.value = null
    }

    fun requestSyncConfig() {
        val packet = NetworkPacket.createGetConfig()
        scope.launch {
            wifiManager.sendPacket(packet)
        }
        addLog(
            ProtocolLogEntry.Direction.OUTBOUND,
            packet.type.name,
            "Requested Config from Ludo Host",
            packet.toJson(),
            packet.requestId
        )
    }

    fun toggleSimulationMode(enabled: Boolean) {
        _isSimulationMode.value = enabled
        _systemAlert.value = if (enabled) "Simulation Mode Enabled" else "Real Wi-Fi Mode Enabled"
    }

    private fun simulateApp1Response(controllerId: String, channel: Int, number: Int, commandId: String) {
        scope.launch {
            delay(350)
            handleAcknowledgement(
                NetworkPacket(
                    type = NetworkMessageType.ACK,
                    requestId = commandId,
                    status = "OK"
                )
            )

            delay(1200)
            handleNumberResult(
                NetworkPacket(
                    type = NetworkMessageType.NUMBER_RESULT,
                    boxId = controllerId,
                    channel = channel,
                    value = number,
                    requestId = commandId
                )
            )
        }
    }

    fun simulateApp1FallbackRoll(boxId: String? = null) {
        val targetId = boxId ?: _selectedBoxForInput.value ?: "B1"
        val box = _boxes.value[targetId] ?: return
        val channel = box.channel
        val randomVal = Random.nextInt(1, 7)
        val commandId = "sim_${targetId}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        // Set outstanding command on target box so strict matching accepts the simulated result
        val currentMap = _boxes.value.toMutableMap()
        currentMap[targetId] = box.copy(
            lastCommandId = commandId,
            status = BoxCommandStatus.SENT
        )
        _boxes.value = currentMap

        val packet = NetworkPacket(
            type = NetworkMessageType.NUMBER_RESULT,
            boxId = targetId,
            channel = channel,
            value = randomVal,
            requestId = commandId
        )
        handleNumberResult(packet)
    }

    private fun rebuildBoxes(count: Int) {
        val existing = _boxes.value
        val newMap = LinkedHashMap<String, BoxChannelState>()
        for (i in 1..count.coerceIn(2, 4)) {
            val id = "B$i"
            newMap[id] = existing[id] ?: BoxChannelState.createDefault(i)
        }
        _boxes.value = newMap
        if (_selectedBoxForInput.value != null && !newMap.containsKey(_selectedBoxForInput.value)) {
            _selectedBoxForInput.value = "B1"
        }
    }

    fun dismissAlert() {
        _systemAlert.value = null
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun addLog(
        direction: ProtocolLogEntry.Direction,
        type: String,
        summary: String,
        json: String,
        commandId: String? = null
    ) {
        val entry = ProtocolLogEntry(
            direction = direction,
            type = type,
            summary = summary,
            fullJson = json,
            commandId = commandId
        )
        _logs.value = (listOf(entry) + _logs.value).take(100)
    }
}
