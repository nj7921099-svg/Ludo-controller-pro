package com.example.network

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Discovered Ludo Host descriptor.
 */
data class DiscoveredLudoHost(
    val serviceName: String,
    val ipAddress: String,
    val port: Int = LudoNetworkProtocol.LUDO_TCP_PORT,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Real Number Test State tracking (Issue 3).
 * NUMBER_SELECTION -> TCP Write -> Matching ACK -> Matching NUMBER_RESULT -> PASS.
 */
data class NumberTestState(
    val inProgress: Boolean = false,
    val selectedNumber: Int? = null,
    val requestId: String? = null,
    val writeSuccess: Boolean = false,
    val ackReceived: Boolean = false,
    val resultReceived: Boolean = false,
    val resultValue: Int? = null,
    val statusText: String = "Ready for test",
    val errorReason: String? = null,
    val isPassed: Boolean = false,
    val latencyMs: Long? = null
)

/**
 * Detailed diagnostics metrics for Connect tab (Issues 1, 2, 3, 4).
 */
data class ConnectionDiagnostics(
    val networkAvailable: Boolean = false,
    val hotspotActive: Boolean = false,
    val discoveryActive: Boolean = false,
    val ludoHostName: String? = null,
    val ludoHostIp: String? = null,
    val ludoHostPort: Int = LudoNetworkProtocol.LUDO_TCP_PORT,
    val tcpConnected: Boolean = false,
    val handshakeSuccess: Boolean = false,
    val handshakeAckReceived: Boolean = false,
    val pingSuccess: Boolean = false,
    val lastPingLatencyMs: Long? = null,
    val lastSentMessage: String? = null,
    val lastReceivedMessage: String? = null,
    val lastTcpWriteSuccess: Boolean? = null,
    val lastAckStatus: String? = null,
    val lastNumberResultStatus: String? = null,
    val lastErrorStage: String? = null,
    val lastErrorReason: String? = null,
    val reconnectAttempts: Int = 0,
    val isVerified: Boolean = false
)

/**
 * Diagnostic log entry for in-app logs.
 */
data class DiagnosticLogItem(
    val timestamp: String,
    val stage: String,
    val message: String,
    val isError: Boolean = false
)

/**
 * Wi-Fi and NSD Connection Manager.
 * Orchestrates:
 * 1. Unified TCP Port (8888) across all paths (Issue 1)
 * 2. Confirmed real TCP socket write/flush via [WifiControllerClient] (Issue 2)
 * 3. Real end-to-end number test with 5s timeout & matching requestId (Issue 3)
 * 4. Automatic, lifecycle-safe network availability monitoring via [ConnectivityManager.NetworkCallback] (Issue 4)
 * 5. Safe reconnection, verification protocol, and diagnostics.
 */
class WifiConnectionManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "WifiConnectionManager"
        private const val HANDSHAKE_TIMEOUT_MS = 6000L
        private const val PING_TIMEOUT_MS = 4000L
        private const val TEST_TIMEOUT_MS = 5000L
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val nsdManager =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    val tcpClient = WifiControllerClient(scope)

    private val _connectionState = MutableStateFlow(ConnectionState.IDLE)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredHosts = MutableStateFlow<List<DiscoveredLudoHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredLudoHost>> = _discoveredHosts.asStateFlow()

    private val _diagnostics = MutableStateFlow(ConnectionDiagnostics())
    val diagnostics: StateFlow<ConnectionDiagnostics> = _diagnostics.asStateFlow()

    private val _diagnosticLogs = MutableStateFlow<List<DiagnosticLogItem>>(emptyList())
    val diagnosticLogs: StateFlow<List<DiagnosticLogItem>> = _diagnosticLogs.asStateFlow()

    private val _numberTestState = MutableStateFlow(NumberTestState())
    val numberTestState: StateFlow<NumberTestState> = _numberTestState.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<NetworkPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<NetworkPacket> = _incomingPackets.asSharedFlow()

    private var multicastLock: WifiManager.MulticastLock? = null
    private var nsdDiscoveryListener: NsdManager.DiscoveryListener? = null
    private var isNsdDiscovering = false

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isNetworkMonitoringRegistered = false

    private var lastDiscoveredHost: DiscoveredLudoHost? = null
    private var pingStartTime: Long = 0L
    private var handshakeRequestId: String? = null
    private var pingRequestId: String? = null

    private var pendingHandshakeAckJob: Job? = null
    private var pendingPingPongJob: Job? = null
    private var reconnectJob: Job? = null
    private var numberTestJob: Job? = null

    init {
        logDiagnostic("INIT", "Wi-Fi Connection Manager initialized (Authoritative TCP Port: ${LudoNetworkProtocol.LUDO_TCP_PORT})")
        setupNetworkMonitoring()
        observeTcpClient()
    }

    /**
     * Requirement (Issue 4):
     * Lifecycle-safe automatic network monitoring.
     * Prevents duplicate registration and safely unregisters on destroy.
     */
    private fun setupNetworkMonitoring() {
        if (isNetworkMonitoringRegistered) return

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val isHotspot = checkIsHotspotActive()
                    val hasNet = isLocalNetworkAvailable() || isHotspot

                    updateDiagnostics {
                        copy(networkAvailable = hasNet, hotspotActive = isHotspot)
                    }
                    logDiagnostic("NETWORK", "Network Available (Hotspot=$isHotspot)")

                    // Auto-recovery: If network was lost and returns, transition to IDLE / allow reconnect
                    if (_connectionState.value == ConnectionState.NETWORK_UNAVAILABLE) {
                        _connectionState.value = ConnectionState.IDLE
                        logDiagnostic("NETWORK", "Network restored -> Ready for connection")
                        // If we had a known host, automatically trigger controlled reconnection
                        lastDiscoveredHost?.let { host ->
                            logDiagnostic("NETWORK", "Auto-reconnecting to known host ${host.ipAddress}:${host.port}...")
                            scope.launch {
                                delay(600)
                                if (_connectionState.value == ConnectionState.IDLE) {
                                    connectToHost(host)
                                }
                            }
                        }
                    }
                }

                override fun onLost(network: Network) {
                    val isHotspot = checkIsHotspotActive()
                    val hasNet = isLocalNetworkAvailable() || isHotspot

                    updateDiagnostics {
                        copy(networkAvailable = hasNet, hotspotActive = isHotspot)
                    }

                    if (!hasNet) {
                        logDiagnostic("NETWORK", "Network lost -> NETWORK_UNAVAILABLE", isError = true)
                        stopDiscovery()
                        if (_connectionState.value.isConnected || _connectionState.value.isConnecting) {
                            tcpClient.disconnect("Network lost")
                        }
                        _connectionState.value = ConnectionState.NETWORK_UNAVAILABLE
                    }
                }
            }

            networkCallback = callback
            connectivityManager?.registerNetworkCallback(request, callback)
            isNetworkMonitoringRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Could not register network callback: ${e.message}")
        }

        // Initial check
        val initialHotspot = checkIsHotspotActive()
        val initialAvailable = isLocalNetworkAvailable() || initialHotspot
        updateDiagnostics {
            copy(networkAvailable = initialAvailable, hotspotActive = initialHotspot)
        }
        if (!initialAvailable) {
            _connectionState.value = ConnectionState.NETWORK_UNAVAILABLE
        }
    }

    private fun observeTcpClient() {
        scope.launch {
            tcpClient.rawIncomingFlow.collect { rawJson ->
                updateDiagnostics { copy(lastReceivedMessage = rawJson) }
                val packet = NetworkPacket.fromJson(rawJson)
                if (packet != null) {
                    handleIncomingPacket(packet)
                } else {
                    logDiagnostic("INBOUND", "Malformed JSON packet: $rawJson", isError = true)
                }
            }
        }

        scope.launch {
            tcpClient.disconnectFlow.collect { reason ->
                handleConnectionLost(reason)
            }
        }
    }

    fun isLocalNetworkAvailable(): Boolean {
        val cm = connectivityManager ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun checkIsHotspotActive(): Boolean {
        return try {
            val method = wifiManager?.javaClass?.getDeclaredMethod("isWifiApEnabled")
            (method?.invoke(wifiManager) as? Boolean) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun openHotspotSettings() {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            logDiagnostic("HOTSPOT", "Opened Wireless & Hotspot Settings")
        } catch (e: Exception) {
            try {
                val tetherIntent = Intent("android.settings.TETHER_SETTINGS").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(tetherIntent)
            } catch (ignored: Exception) {
                logDiagnostic("HOTSPOT", "Could not open Hotspot settings automatically", isError = true)
            }
        }
    }

    fun refreshNetworkStatus() {
        val isHotspot = checkIsHotspotActive()
        val isNet = isLocalNetworkAvailable() || isHotspot
        updateDiagnostics {
            copy(networkAvailable = isNet, hotspotActive = isHotspot)
        }
        if (!isNet) {
            _connectionState.value = ConnectionState.NETWORK_UNAVAILABLE
        } else if (_connectionState.value == ConnectionState.NETWORK_UNAVAILABLE) {
            _connectionState.value = ConnectionState.IDLE
        }
        logDiagnostic("CHECK", "Network: ${if (isNet) "Available" else "Unavailable"}, Hotspot: ${if (isHotspot) "Active" else "Inactive"}")
    }

    /**
     * Starts Android NSD discovery for `_ludo._tcp` / `LudoHost`.
     */
    fun startDiscovery() {
        if (isNsdDiscovering) {
            logDiagnostic("NSD", "Discovery is already running")
            return
        }

        refreshNetworkStatus()
        _connectionState.value = ConnectionState.DISCOVERING
        updateDiagnostics { copy(discoveryActive = true) }
        logDiagnostic("NSD", "Searching for Ludo Host (${LudoNetworkProtocol.NSD_SERVICE_TYPE})...")

        acquireMulticastLock()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                isNsdDiscovering = true
                Log.d(TAG, "NSD discovery started: $regType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                val name = serviceInfo.serviceName ?: ""
                val type = serviceInfo.serviceType ?: ""

                if (type.contains("ludo", ignoreCase = true) || name.contains("Ludo", ignoreCase = true)) {
                    logDiagnostic("NSD", "Found matching service: $name, resolving IP & Port...")
                    resolveNsdService(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val current = _discoveredHosts.value.filter { it.serviceName != serviceInfo.serviceName }
                _discoveredHosts.value = current
            }

            override fun onDiscoveryStopped(serviceType: String) {
                isNsdDiscovering = false
                updateDiagnostics { copy(discoveryActive = false) }
                Log.d(TAG, "NSD discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                isNsdDiscovering = false
                updateDiagnostics {
                    copy(
                        discoveryActive = false,
                        lastErrorStage = "NSD Discovery",
                        lastErrorReason = "Start discovery failed (Code $errorCode)"
                    )
                }
                _connectionState.value = ConnectionState.ERROR
                logDiagnostic("NSD", "Start discovery failed (code $errorCode)", isError = true)
                releaseMulticastLock()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                isNsdDiscovering = false
                updateDiagnostics { copy(discoveryActive = false) }
                releaseMulticastLock()
            }
        }

        nsdDiscoveryListener = listener
        try {
            nsdManager?.discoverServices(
                LudoNetworkProtocol.NSD_SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                listener
            )
        } catch (e: Exception) {
            logDiagnostic("NSD", "Failed to start discovery: ${e.message}", isError = true)
            isNsdDiscovering = false
            updateDiagnostics {
                copy(
                    discoveryActive = false,
                    lastErrorStage = "NSD Discovery",
                    lastErrorReason = e.message ?: "Unknown error"
                )
            }
            _connectionState.value = ConnectionState.ERROR
            releaseMulticastLock()
        }
    }

    private fun resolveNsdService(serviceInfo: NsdServiceInfo) {
        nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                logDiagnostic("NSD", "Resolve failed for ${serviceInfo.serviceName} (Code $errorCode)", isError = true)
            }

            override fun onServiceResolved(resolved: NsdServiceInfo) {
                val host = resolved.host?.hostAddress ?: return
                // Authoritative port from resolved service, or fallback to LUDO_TCP_PORT (8888)
                val port = if (resolved.port > 0) resolved.port else LudoNetworkProtocol.LUDO_TCP_PORT
                val name = resolved.serviceName ?: LudoNetworkProtocol.NSD_SERVICE_NAME

                val discovered = DiscoveredLudoHost(
                    serviceName = name,
                    ipAddress = host,
                    port = port
                )
                lastDiscoveredHost = discovered

                val current = _discoveredHosts.value.toMutableList()
                val idx = current.indexOfFirst { it.ipAddress == host && it.port == port }
                if (idx >= 0) {
                    current[idx] = discovered
                } else {
                    current.add(0, discovered)
                }
                _discoveredHosts.value = current

                updateDiagnostics {
                    copy(
                        ludoHostName = name,
                        ludoHostIp = host,
                        ludoHostPort = port
                    )
                }

                _connectionState.value = ConnectionState.DISCOVERED
                logDiagnostic("NSD", "Ludo Host discovered at $host:$port ($name)")
            }
        })
    }

    fun stopDiscovery() {
        if (!isNsdDiscovering) return
        try {
            nsdDiscoveryListener?.let { listener ->
                nsdManager?.stopServiceDiscovery(listener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping NSD discovery: ${e.message}")
        } finally {
            isNsdDiscovering = false
            nsdDiscoveryListener = null
            updateDiagnostics { copy(discoveryActive = false) }
            releaseMulticastLock()
        }
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("LudoMulticastLock")?.apply {
                    setReferenceCounted(true)
                }
            }
            multicastLock?.acquire()
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire multicast lock: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (ignored: Exception) {}
    }

    /**
     * Connects to discovered host.
     */
    fun connectToHost(host: DiscoveredLudoHost) {
        lastDiscoveredHost = host
        connectToAddress(host.ipAddress, host.port, host.serviceName)
    }

    /**
     * Requirement (Issue 1):
     * Manual IP Fallback uses authoritative LUDO_TCP_PORT (8888).
     */
    fun connectWithManualIp(ipAddress: String, port: Int = LudoNetworkProtocol.LUDO_TCP_PORT) {
        val targetPort = if (port in 1..65535) port else LudoNetworkProtocol.LUDO_TCP_PORT
        val host = DiscoveredLudoHost(
            serviceName = "Manual Host",
            ipAddress = ipAddress.trim(),
            port = targetPort
        )
        lastDiscoveredHost = host
        connectToAddress(host.ipAddress, targetPort, host.serviceName)
    }

    private fun connectToAddress(ip: String, port: Int, hostName: String) {
        scope.launch {
            // Stop discovery before connecting
            stopDiscovery()

            _connectionState.value = ConnectionState.CONNECTING
            updateDiagnostics {
                copy(
                    ludoHostName = hostName,
                    ludoHostIp = ip,
                    ludoHostPort = port,
                    tcpConnected = false,
                    handshakeSuccess = false,
                    handshakeAckReceived = false,
                    pingSuccess = false,
                    isVerified = false,
                    lastErrorStage = null,
                    lastErrorReason = null,
                    lastTcpWriteSuccess = null
                )
            }
            logDiagnostic("TCP", "Connecting to $ip:$port...")

            val connectResult = tcpClient.connect(ip, port)
            if (connectResult.isFailure) {
                val err = connectResult.exceptionOrNull()?.message ?: "Connection timeout / refused"
                logDiagnostic("TCP", "Connection failed: $err", isError = true)
                updateDiagnostics {
                    copy(
                        tcpConnected = false,
                        lastErrorStage = "TCP",
                        lastErrorReason = err
                    )
                }
                _connectionState.value = ConnectionState.ERROR
                return@launch
            }

            _connectionState.value = ConnectionState.SOCKET_CONNECTED
            updateDiagnostics { copy(tcpConnected = true) }
            logDiagnostic("TCP", "TCP socket connected to $ip:$port")

            // Begin verification sequence
            performHandshakeAndVerification()
        }
    }

    /**
     * Verification sequence:
     * 1. Send HANDSHAKE (confirms real TCP write)
     * 2. Await HANDSHAKE_ACK
     * 3. Send PING (confirms real TCP write)
     * 4. Await PONG
     * 5. Set CONNECTED
     */
    private fun performHandshakeAndVerification() {
        scope.launch {
            _connectionState.value = ConnectionState.VERIFYING
            logDiagnostic("VERIFY", "Initiating handshake (Protocol v${LudoNetworkProtocol.PROTOCOL_VERSION})...")

            val hsReqId = "hs_${UUID.randomUUID().toString().take(8)}"
            handshakeRequestId = hsReqId

            val handshakePacket = NetworkPacket.createHandshake(hsReqId)
            val writeResult = sendPacket(handshakePacket)
            if (writeResult !is SendResult.Success) {
                failVerification("Handshake", "Real TCP write failed: ${writeResult.reason}")
                return@launch
            }

            logDiagnostic("HANDSHAKE", "Handshake written to socket ($hsReqId)")

            // Await HANDSHAKE_ACK
            pendingHandshakeAckJob?.cancel()
            val ackReceived = withTimeoutOrNull(HANDSHAKE_TIMEOUT_MS) {
                while (isActive) {
                    if (_diagnostics.value.handshakeAckReceived) return@withTimeoutOrNull true
                    delay(50)
                }
                false
            } ?: false

            if (!ackReceived) {
                failVerification("Handshake", "Timeout waiting for HANDSHAKE_ACK from Ludo Host")
                return@launch
            }

            logDiagnostic("HANDSHAKE", "Handshake ACK verified")

            // Ping/Pong verification
            val pingReqId = "ping_${UUID.randomUUID().toString().take(8)}"
            pingRequestId = pingReqId
            pingStartTime = System.currentTimeMillis()

            val pingPacket = NetworkPacket.createPing(pingReqId)
            val pingWriteResult = sendPacket(pingPacket)
            if (pingWriteResult !is SendResult.Success) {
                failVerification("Ping/Pong", "Failed to write Ping: ${pingWriteResult.reason}")
                return@launch
            }

            logDiagnostic("PING", "Ping written, awaiting Pong...")

            val pongReceived = withTimeoutOrNull(PING_TIMEOUT_MS) {
                while (isActive) {
                    if (_diagnostics.value.pingSuccess) return@withTimeoutOrNull true
                    delay(50)
                }
                false
            } ?: false

            if (!pongReceived) {
                failVerification("Ping/Pong", "Timeout waiting for PONG from Ludo Host")
                return@launch
            }

            // Connection completely verified!
            _connectionState.value = ConnectionState.CONNECTED
            updateDiagnostics { copy(isVerified = true) }
            logDiagnostic("VERIFY", "🟢 CONNECTION VERIFIED! Ping latency: ${_diagnostics.value.lastPingLatencyMs ?: 0}ms")

            // Automatically request config
            sendPacket(NetworkPacket.createGetConfig())
        }
    }

    private fun failVerification(stage: String, reason: String) {
        logDiagnostic(stage, "Verification failed: $reason", isError = true)
        updateDiagnostics {
            copy(
                isVerified = false,
                lastErrorStage = stage,
                lastErrorReason = reason
            )
        }
        _connectionState.value = ConnectionState.ERROR
        tcpClient.disconnect("Verification failed at $stage: $reason")
    }

    private fun handleIncomingPacket(packet: NetworkPacket) {
        scope.launch {
            _incomingPackets.emit(packet)
        }

        when (packet.type) {
            NetworkMessageType.HANDSHAKE_ACK -> {
                logDiagnostic("HANDSHAKE", "Received HANDSHAKE_ACK (status=${packet.status})")
                if (packet.status == "OK" || packet.status == null) {
                    updateDiagnostics {
                        copy(handshakeSuccess = true, handshakeAckReceived = true)
                    }
                } else {
                    failVerification("Handshake", "Handshake rejected: ${packet.message ?: packet.status}")
                }
            }

            NetworkMessageType.PONG -> {
                val latency = if (pingStartTime > 0) System.currentTimeMillis() - pingStartTime else 0L
                logDiagnostic("PONG", "Received PONG (latency=${latency}ms)")
                updateDiagnostics {
                    copy(pingSuccess = true, lastPingLatencyMs = latency)
                }
            }

            NetworkMessageType.PING -> {
                logDiagnostic("PING", "Received PING, responding with PONG")
                scope.launch {
                    sendPacket(NetworkPacket.createPong(packet.requestId))
                }
            }

            NetworkMessageType.ACK -> {
                logDiagnostic("ACK", "Received ACK for requestId=${packet.requestId}")
                updateDiagnostics { copy(lastAckStatus = "Received (${packet.requestId})") }
            }

            NetworkMessageType.NUMBER_RESULT -> {
                logDiagnostic("RESULT", "Received NUMBER_RESULT: ${packet.boxId ?: ""} value=${packet.value}")
                updateDiagnostics { copy(lastNumberResultStatus = "Revealed ${packet.value ?: ""}") }
            }

            NetworkMessageType.ERROR -> {
                logDiagnostic("ERROR", "Remote error: ${packet.message ?: packet.reason}", isError = true)
                updateDiagnostics {
                    copy(
                        lastErrorStage = "Remote Host",
                        lastErrorReason = packet.message ?: packet.reason ?: "Unknown error"
                    )
                }
            }

            NetworkMessageType.DISCONNECT -> {
                logDiagnostic("DISCONNECT", "Remote host disconnected: ${packet.reason}")
                handleConnectionLost("Remote host disconnected: ${packet.reason}")
            }

            else -> {}
        }
    }

    private fun handleConnectionLost(reason: String) {
        logDiagnostic("DISCONNECT", "🔴 NOT CONNECTED: $reason", isError = true)
        _connectionState.value = ConnectionState.DISCONNECTED
        updateDiagnostics {
            copy(
                tcpConnected = false,
                handshakeSuccess = false,
                handshakeAckReceived = false,
                pingSuccess = false,
                isVerified = false,
                lastErrorStage = "Disconnect",
                lastErrorReason = reason,
                lastTcpWriteSuccess = false
            )
        }

        // Close socket cleanly
        tcpClient.disconnect(reason)

        // Attempt controlled reconnect if we have a known host
        lastDiscoveredHost?.let { host ->
            reconnectJob?.cancel()
            reconnectJob = scope.launch {
                delay(1200)
                if (_connectionState.value == ConnectionState.DISCONNECTED) {
                    logDiagnostic("RECONNECT", "Attempting controlled reconnect to ${host.ipAddress}:${host.port}...")
                    _connectionState.value = ConnectionState.RECONNECTING
                    updateDiagnostics {
                        copy(reconnectAttempts = reconnectAttempts + 1)
                    }
                    connectToAddress(host.ipAddress, host.port, host.serviceName)
                }
            }
        }
    }

    /**
     * Test Connection (Ping/Pong).
     */
    fun testConnection() {
        if (!_connectionState.value.isConnected) {
            logDiagnostic("TEST", "Cannot test: not connected", isError = true)
            return
        }

        scope.launch {
            logDiagnostic("TEST", "Sending PING to Ludo Host...")
            updateDiagnostics { copy(pingSuccess = false) }

            val reqId = "test_ping_${UUID.randomUUID().toString().take(6)}"
            pingRequestId = reqId
            pingStartTime = System.currentTimeMillis()

            val packet = NetworkPacket.createPing(reqId)
            val sendRes = sendPacket(packet)
            if (sendRes !is SendResult.Success) {
                logDiagnostic("TEST", "Ping write failed: ${sendRes.reason}", isError = true)
                return@launch
            }

            val received = withTimeoutOrNull(3000L) {
                while (isActive) {
                    if (_diagnostics.value.pingSuccess) return@withTimeoutOrNull true
                    delay(50)
                }
                false
            } ?: false

            if (received) {
                logDiagnostic("TEST", "✓ Pong received! Latency: ${_diagnostics.value.lastPingLatencyMs ?: 0}ms")
            } else {
                logDiagnostic("TEST", "Ping timed out (no Pong received)", isError = true)
            }
        }
    }

    /**
     * Requirement (Issue 3):
     * Real Number Communication Test with matching requestId:
     * NUMBER_SELECTION -> TCP Write Success -> Matching ACK -> Matching NUMBER_RESULT -> PASS.
     * Uses 5-second timeout protection.
     */
    fun sendNumberTest(number: Int) {
        numberTestJob?.cancel()
        numberTestJob = scope.launch {
            if (!_connectionState.value.isConnected) {
                val err = "Cannot test: Socket is not connected to Ludo Host"
                _numberTestState.value = NumberTestState(
                    inProgress = false,
                    selectedNumber = number,
                    statusText = "FAILED: Not Connected",
                    errorReason = err,
                    isPassed = false
                )
                logDiagnostic("TEST_NUM", err, isError = true)
                return@launch
            }

            val testReqId = "req_test_${number}_${System.currentTimeMillis()}"
            val startTime = System.currentTimeMillis()

            _numberTestState.value = NumberTestState(
                inProgress = true,
                selectedNumber = number,
                requestId = testReqId,
                statusText = "Step 1/3: Writing NUMBER_SELECTION to socket...",
                errorReason = null,
                isPassed = false
            )
            logDiagnostic("TEST_NUM", "Starting Number Test for $number (requestId=$testReqId)...")

            val packet = NetworkPacket(
                type = NetworkMessageType.NUMBER_SELECTION,
                protocolVersion = LudoNetworkProtocol.PROTOCOL_VERSION,
                role = LudoNetworkProtocol.ROLE_CONTROLLER,
                boxId = "B1",
                channel = 1,
                value = number,
                requestId = testReqId,
                timestamp = System.currentTimeMillis()
            )

            // Step 1: Real TCP socket write
            val writeResult = sendPacket(packet)
            if (writeResult !is SendResult.Success) {
                val failMsg = "TCP write failed: ${writeResult.reason}"
                _numberTestState.value = _numberTestState.value.copy(
                    inProgress = false,
                    writeSuccess = false,
                    statusText = "FAILED: TCP Write Error",
                    errorReason = failMsg,
                    isPassed = false
                )
                logDiagnostic("TEST_NUM", failMsg, isError = true)
                return@launch
            }

            _numberTestState.value = _numberTestState.value.copy(
                writeSuccess = true,
                statusText = "Step 2/3: TCP written. Awaiting matching ACK..."
            )
            logDiagnostic("TEST_NUM", "TCP written, awaiting matching ACK for $testReqId...")

            // Step 2: Await matching ACK within 5 seconds
            var ackMatched = false
            val ackWaitResult = withTimeoutOrNull(TEST_TIMEOUT_MS) {
                incomingPackets.collect { incoming ->
                    if (incoming.type == NetworkMessageType.ACK) {
                        if (incoming.requestId == testReqId) {
                            ackMatched = true
                            throw CancellationException("ACK_RECEIVED")
                        } else {
                            logDiagnostic(
                                "TEST_NUM",
                                "ACK ignored: requestId mismatch. Expected=$testReqId, Received=${incoming.requestId}"
                            )
                        }
                    }
                }
            }

            if (!ackMatched) {
                val failMsg = "ACK timeout: No matching ACK received within ${TEST_TIMEOUT_MS / 1000}s"
                _numberTestState.value = _numberTestState.value.copy(
                    inProgress = false,
                    statusText = "FAILED: ACK Timeout",
                    errorReason = failMsg,
                    isPassed = false
                )
                logDiagnostic("TEST_NUM", failMsg, isError = true)
                return@launch
            }

            _numberTestState.value = _numberTestState.value.copy(
                ackReceived = true,
                statusText = "Step 3/3: ACK received. Awaiting NUMBER_RESULT..."
            )
            logDiagnostic("TEST_NUM", "Matching ACK received! Awaiting NUMBER_RESULT for $testReqId...")

            // Step 3: Await matching NUMBER_RESULT within 5 seconds (STRICT requestId matching)
            var resultMatchedPacket: NetworkPacket? = null
            val resultWaitResult = withTimeoutOrNull(TEST_TIMEOUT_MS) {
                incomingPackets.collect { incoming ->
                    if (incoming.type == NetworkMessageType.NUMBER_RESULT) {
                        if (incoming.requestId == testReqId) {
                            resultMatchedPacket = incoming
                            throw CancellationException("RESULT_RECEIVED")
                        } else {
                            logDiagnostic(
                                "TEST_NUM",
                                "NUMBER_RESULT ignored: requestId mismatch. Expected=$testReqId, Received=${incoming.requestId}"
                            )
                        }
                    }
                }
            }

            val finalResult = resultMatchedPacket
            if (finalResult == null) {
                val failMsg = "NUMBER_RESULT timeout: ACK arrived but result not received within ${TEST_TIMEOUT_MS / 1000}s"
                _numberTestState.value = _numberTestState.value.copy(
                    inProgress = false,
                    statusText = "FAILED: Result Timeout",
                    errorReason = failMsg,
                    isPassed = false
                )
                logDiagnostic("TEST_NUM", failMsg, isError = true)
                return@launch
            }

            // Step 4: Fully verified PASS
            val elapsed = System.currentTimeMillis() - startTime
            val revealedNumber = finalResult.value ?: number
            _numberTestState.value = _numberTestState.value.copy(
                inProgress = false,
                resultReceived = true,
                resultValue = revealedNumber,
                isPassed = true,
                latencyMs = elapsed,
                statusText = "✓ PASS: Number $revealedNumber confirmed (ACK + RESULT in ${elapsed}ms)"
            )
            logDiagnostic("TEST_NUM", "✓ PASS: Number $revealedNumber successfully communicated end-to-end ($elapsed ms)")
        }
    }

    /**
     * Requirement (Issue 2):
     * Sends packet over TCP socket and awaits real write & flush.
     * Reports actual SendResult.
     */
    suspend fun sendPacket(packet: NetworkPacket): SendResult {
        val json = packet.toTransmissionString()
        updateDiagnostics { copy(lastSentMessage = packet.toJson()) }

        val result = tcpClient.send(json)
        if (result is SendResult.Success) {
            updateDiagnostics { copy(lastTcpWriteSuccess = true) }
        } else if (result is SendResult.Failure) {
            updateDiagnostics {
                copy(
                    lastTcpWriteSuccess = false,
                    lastErrorStage = "TCP Write",
                    lastErrorReason = result.reason
                )
            }
            logDiagnostic("TCP_WRITE", "Write failed: ${result.reason}", isError = true)
            if (_connectionState.value == ConnectionState.CONNECTED) {
                handleConnectionLost("Write failed: ${result.reason}")
            }
        }
        return result
    }

    fun disconnect() {
        logDiagnostic("DISCONNECT", "Disconnect requested by user")
        reconnectJob?.cancel()
        reconnectJob = null
        numberTestJob?.cancel()
        numberTestJob = null
        stopDiscovery()
        scope.launch {
            sendPacket(NetworkPacket.createDisconnect("User requested disconnect"))
            tcpClient.disconnect("User disconnected")
        }
        _connectionState.value = ConnectionState.DISCONNECTED
        updateDiagnostics {
            copy(
                tcpConnected = false,
                isVerified = false,
                handshakeSuccess = false,
                pingSuccess = false,
                lastTcpWriteSuccess = null
            )
        }
    }

    fun clearDiagnosticsLog() {
        _diagnosticLogs.value = emptyList()
    }

    private fun logDiagnostic(stage: String, message: String, isError: Boolean = false) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val item = DiagnosticLogItem(time, stage, message, isError)
        _diagnosticLogs.value = (listOf(item) + _diagnosticLogs.value).take(150)
        Log.d(TAG, "[$stage] $message")
    }

    private inline fun updateDiagnostics(block: ConnectionDiagnostics.() -> ConnectionDiagnostics) {
        _diagnostics.value = _diagnostics.value.block()
    }

    /**
     * Requirement (Issue 4):
     * Complete lifecycle safety: unregister NetworkCallback, stop NSD, close sockets, cancel jobs.
     */
    fun destroy() {
        Log.i(TAG, "Cleaning up WifiConnectionManager...")
        reconnectJob?.cancel()
        numberTestJob?.cancel()
        pendingHandshakeAckJob?.cancel()
        pendingPingPongJob?.cancel()

        // Unregister NetworkCallback safely
        networkCallback?.let { cb ->
            try {
                connectivityManager?.unregisterNetworkCallback(cb)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering NetworkCallback: ${e.message}")
            }
            networkCallback = null
            isNetworkMonitoringRegistered = false
        }

        stopDiscovery()
        tcpClient.disconnect("Destroyed")
    }
}
