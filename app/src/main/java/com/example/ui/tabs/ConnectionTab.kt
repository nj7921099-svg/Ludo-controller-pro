package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controller.settings.ControllerSettings
import com.example.network.ConnectionDiagnostics
import com.example.network.ConnectionState
import com.example.network.DiagnosticLogItem
import com.example.network.DiscoveredLudoHost
import com.example.network.GlobalIndicatorStatus
import com.example.network.LudoNetworkProtocol
import com.example.network.NumberTestState
import com.example.ui.components.ControllerSettingsSheet
import com.example.ui.components.GlobalConnectionIndicator
import com.example.ui.theme.GreenActiveBadge
import com.example.ui.theme.GreenActiveTurn
import com.example.ui.theme.GreenActiveTurnLight
import com.example.ui.theme.GreenConnected
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PurpleBorderBright
import com.example.ui.theme.PurpleBorderSubtle
import com.example.ui.theme.PurpleCardBorder
import com.example.ui.theme.PurpleGlowLight
import com.example.ui.theme.PurpleGlowPrimary
import com.example.ui.theme.RedDisconnected
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

/**
 * Dedicated Connect Tab:
 * Implements the Hotspot-First Wi-Fi architecture connecting to Ludo-app (Host/Server).
 *
 * Steps:
 * 1. Turn ON Wi-Fi Hotspot.
 * 2. Allow Ludo phone to connect.
 * 3. Discover Ludo Host automatically via NSD (_ludo._tcp).
 * 4. Connect via TCP.
 * 5. Handshake & Handshake ACK.
 * 6. Ping/Pong verification.
 * 7. 🟢 CONNECTED status.
 *
 * Includes:
 * - Real Connection Diagnostics
 * - Test Connection (Ping/Pong)
 * - Number Communication Test (Buttons 1..6)
 * - In-app Diagnostics Log
 * - Manual IP fallback
 */
@Composable
fun ConnectionTab(
    connectionState: ConnectionState,
    diagnostics: ConnectionDiagnostics,
    discoveredHosts: List<DiscoveredLudoHost>,
    diagnosticLogs: List<DiagnosticLogItem>,
    numberTestState: NumberTestState,
    settings: ControllerSettings,
    onHapticChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onRememberLastModeChange: (Boolean) -> Unit,
    onOpenHotspotSettings: () -> Unit,
    onRefreshNetwork: () -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onConnectHost: (DiscoveredLudoHost) -> Unit,
    onConnectManualIp: (String, Int) -> Unit,
    onTestConnection: () -> Unit,
    onSendNumberTest: (Int) -> Unit,
    onDisconnect: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var manualIp by remember { mutableStateOf("192.168.43.") }
    var manualPort by remember { mutableStateOf("${LudoNetworkProtocol.LUDO_TCP_PORT}") }
    var isManualExpanded by remember { mutableStateOf(false) }
    var isDiagnosticsExpanded by remember { mutableStateOf(true) }
    var isLogExpanded by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val isConnected = connectionState.isConnected
    val isConnecting = connectionState.isConnecting

    // Seamless full-screen container without outer purple border
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TOP BAR: "Connection" + status on left, Settings gear button (⚙) on top-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Connection",
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )

                    GlobalConnectionIndicator(
                        connectionState = connectionState,
                        onClick = { /* Already on Connection tab */ }
                    )
                }

                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("btn_connection_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Controller Settings",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 1. HOTSPOT-FIRST NETWORK STATUS CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCard,
                border = BorderStroke(1.2.dp, PurpleBorderBright)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. LOCAL NETWORK SETUP",
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This Controller phone creates the hotspot network for Ludo phone.",
                        color = TextDim,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Wi-Fi / Hotspot status pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Wi-Fi status
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceCardElevated,
                            border = BorderStroke(1.dp, if (diagnostics.networkAvailable) GreenActiveTurn else PurpleBorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (diagnostics.networkAvailable) GreenConnected else NeonAmber)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Wi-Fi Network", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (diagnostics.networkAvailable) "Connected" else "Checking...",
                                        color = if (diagnostics.networkAvailable) GreenActiveTurnLight else TextDim,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Hotspot status
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceCardElevated,
                            border = BorderStroke(1.dp, if (diagnostics.hotspotActive) GreenActiveTurn else PurpleBorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (diagnostics.hotspotActive) GreenConnected else NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Hotspot State", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (diagnostics.hotspotActive) "Active" else "Available",
                                        color = if (diagnostics.hotspotActive) GreenActiveTurnLight else TextDim,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenHotspotSettings,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleGlowPrimary)
                        ) {
                            Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Hotspot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onRefreshNetwork,
                            modifier = Modifier.weight(0.7f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = PurpleGlowLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check", color = TextWhite, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 2. DISCOVERY & LUDO HOST SCAN CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCard,
                border = BorderStroke(1.2.dp, PurpleBorderBright)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "2. DISCOVER LUDO HOST",
                                color = TextWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "NSD Service: LudoHost (_ludo._tcp)",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }

                        if (diagnostics.discoveryActive) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = NeonAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (diagnostics.discoveryActive) onStopDiscovery() else onStartDiscovery()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (diagnostics.discoveryActive) NeonAmber.copy(alpha = 0.3f) else PurpleGlowPrimary
                            )
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (diagnostics.discoveryActive) "Stop Discovery" else "Search Ludo Host",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Discovered Hosts List
                    Spacer(modifier = Modifier.height(12.dp))

                    if (discoveredHosts.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceCardElevated
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(TextDim)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (diagnostics.discoveryActive) "Searching local Wi-Fi for Ludo Host..." else "No host discovered yet. Tap 'Search Ludo Host'.",
                                    color = TextDim,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        discoveredHosts.forEach { host ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isConnected && diagnostics.ludoHostIp == host.ipAddress) GreenActiveTurn.copy(alpha = 0.12f) else SurfaceCardElevated,
                                border = BorderStroke(1.dp, if (isConnected && diagnostics.ludoHostIp == host.ipAddress) GreenActiveTurn else PurpleBorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = host.serviceName,
                                                color = TextWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = GreenActiveBadge
                                            ) {
                                                Text(
                                                    text = "FOUND",
                                                    color = GreenActiveTurnLight,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "IP: ${host.ipAddress} • Port: ${host.port}",
                                            color = TextDim,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Button(
                                        onClick = { onConnectHost(host) },
                                        enabled = !isConnected,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isConnected) GreenActiveBadge else GreenActiveTurn
                                        ),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(
                                            text = if (isConnected && diagnostics.ludoHostIp == host.ipAddress) "CONNECTED" else "CONNECT",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TextWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. CONNECTION & VERIFICATION STATUS CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCard,
                border = BorderStroke(1.2.dp, if (isConnected) GreenActiveTurn else PurpleBorderBright)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. CONNECTION STATE",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = connectionState.displayName,
                            color = if (isConnected) GreenActiveTurnLight else if (isConnecting) NeonAmber else RedDisconnected,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isConnected) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GreenActiveTurn.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, GreenActiveTurn),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenActiveTurnLight, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("🟢 CONNECTION VERIFIED", color = GreenActiveTurnLight, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Ludo Host: ${diagnostics.ludoHostName ?: "Ludo Phone"}", color = TextWhite, fontSize = 11.sp)
                                Text("Endpoint: ${diagnostics.ludoHostIp}:${diagnostics.ludoHostPort}", color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("Protocol: v1 (TCP Stream)", color = TextDim, fontSize = 11.sp)
                                Text("Ping Latency: ${diagnostics.lastPingLatencyMs ?: 0}ms", color = GreenActiveTurnLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onTestConnection,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PurpleGlowPrimary)
                                    ) {
                                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("TEST CONNECTION", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = onDisconnect,
                                        modifier = Modifier.weight(0.7f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson.copy(alpha = 0.7f))
                                    ) {
                                        Text("DISCONNECT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    }
                                }
                            }
                        }
                    } else if (isConnecting) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeonAmber.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, NeonAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NeonAmber)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Connecting & Verifying...", color = NeonAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Performing TCP handshake & ping...", color = TextDim, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceCardElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔴 NOT CONNECTED\nTurn on Hotspot on this phone, connect Ludo phone, and tap Connect above.",
                                color = TextDim,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // 4. REAL NUMBER COMMUNICATION TEST (Section 12 / Issue 3)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCard,
                border = BorderStroke(1.2.dp, PurpleBorderBright)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "4. LUDO NUMBER TEST",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )

                        if (numberTestState.inProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = NeonAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "End-to-End: NUMBER_SELECTION → TCP Write → Matching ACK → NUMBER_RESULT",
                        color = TextDim,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Real Test Status Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            numberTestState.isPassed -> GreenActiveTurn.copy(alpha = 0.15f)
                            numberTestState.errorReason != null -> NeonCrimson.copy(alpha = 0.15f)
                            numberTestState.inProgress -> NeonAmber.copy(alpha = 0.12f)
                            else -> SurfaceCardElevated
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                numberTestState.isPassed -> GreenActiveTurn
                                numberTestState.errorReason != null -> NeonCrimson
                                numberTestState.inProgress -> NeonAmber
                                else -> PurpleBorderSubtle
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (numberTestState.isPassed) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenActiveTurnLight, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else if (numberTestState.errorReason != null) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = NeonCrimson, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                Text(
                                    text = numberTestState.statusText,
                                    color = when {
                                        numberTestState.isPassed -> GreenActiveTurnLight
                                        numberTestState.errorReason != null -> NeonCrimson
                                        numberTestState.inProgress -> NeonAmber
                                        else -> TextWhite
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (numberTestState.errorReason != null) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Reason: ${numberTestState.errorReason}",
                                    color = NeonCrimson.copy(alpha = 0.9f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons Row 1 (1, 2, 3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (num in 1..3) {
                            Button(
                                onClick = { onSendNumberTest(num) },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                enabled = isConnected && !numberTestState.inProgress,
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated)
                            ) {
                                Text(
                                    text = "$num",
                                    color = if (isConnected) GreenActiveTurnLight else TextDim,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons Row 2 (4, 5, 6)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (num in 4..6) {
                            Button(
                                onClick = { onSendNumberTest(num) },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                enabled = isConnected && !numberTestState.inProgress,
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated)
                            ) {
                                Text(
                                    text = "$num",
                                    color = if (isConnected) GreenActiveTurnLight else TextDim,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // 5. CONNECTION DIAGNOSTICS (Section 9 / Issues 1, 2, 4)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCard,
                border = BorderStroke(1.2.dp, PurpleBorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDiagnosticsExpanded = !isDiagnosticsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "5. CONNECTION DIAGNOSTICS",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = if (isDiagnosticsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = PurpleGlowLight
                        )
                    }

                    AnimatedVisibility(visible = isDiagnosticsExpanded) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            DiagnosticItemRow("Local Network", diagnostics.networkAvailable)
                            DiagnosticItemRow("Hotspot State", diagnostics.hotspotActive)
                            DiagnosticItemRow("Host Discovered", diagnostics.ludoHostIp != null)
                            DiagnosticItemRow("TCP Connected", diagnostics.tcpConnected)
                            DiagnosticItemRow("TCP Write Status", diagnostics.lastTcpWriteSuccess == true)
                            DiagnosticItemRow("Handshake ACK", diagnostics.handshakeAckReceived)
                            DiagnosticItemRow("Ping Verified", diagnostics.pingSuccess)

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Host Endpoint: ${diagnostics.ludoHostIp ?: "None"} : ${diagnostics.ludoHostPort}",
                                color = TextDim,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Authoritative TCP Port: ${LudoNetworkProtocol.LUDO_TCP_PORT}",
                                color = GreenActiveTurnLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Last ACK: ${diagnostics.lastAckStatus ?: "Waiting"}",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Last NUMBER_RESULT: ${diagnostics.lastNumberResultStatus ?: "Waiting"}",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Reconnect Attempts: ${diagnostics.reconnectAttempts}",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                            if (diagnostics.lastErrorStage != null) {
                                Text(
                                    text = "Last Failure Stage: ${diagnostics.lastErrorStage}",
                                    color = NeonCrimson,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Reason: ${diagnostics.lastErrorReason ?: "None"}",
                                    color = NeonCrimson,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // 6. MANUAL IP FALLBACK (Advanced option / Issue 1)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, PurpleBorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isManualExpanded = !isManualExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ADVANCED: MANUAL IP FALLBACK (PORT ${LudoNetworkProtocol.LUDO_TCP_PORT})",
                            color = TextDim,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = if (isManualExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextDim
                        )
                    }

                    AnimatedVisibility(visible = isManualExpanded) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "Use manual connection if mDNS discovery is blocked by your network. Default TCP Port is 8888.",
                                color = TextDim,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = manualIp,
                                    onValueChange = { manualIp = it },
                                    label = { Text("Ludo Host IP", fontSize = 10.sp) },
                                    modifier = Modifier.weight(2f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PurpleBorderBright,
                                        unfocusedBorderColor = PurpleBorderSubtle,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    )
                                )

                                OutlinedTextField(
                                    value = manualPort,
                                    onValueChange = { manualPort = it },
                                    label = { Text("Port (8888)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PurpleBorderBright,
                                        unfocusedBorderColor = PurpleBorderSubtle,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val port = manualPort.toIntOrNull() ?: LudoNetworkProtocol.LUDO_TCP_PORT
                                    onConnectManualIp(manualIp, port)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated)
                            ) {
                                Text("Connect to Manual IP (${manualIp.trim()}:${manualPort.trim()})", fontSize = 12.sp, color = TextWhite)
                            }
                        }
                    }
                }
            }

            // 7. IN-APP DIAGNOSTICS LOG (Section 16)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, PurpleBorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isLogExpanded = !isLogExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DIAGNOSTIC LOGS (${diagnosticLogs.size})",
                            color = TextDim,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = if (isLogExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextDim
                        )
                    }

                    AnimatedVisibility(visible = isLogExpanded) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Clear Log",
                                    color = PurpleGlowLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onClearLogs() }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (diagnosticLogs.isEmpty()) {
                                Text("Log is empty", color = TextDim, fontSize = 11.sp)
                            } else {
                                diagnosticLogs.take(15).forEach { log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log.timestamp,
                                            color = TextDim,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "[${log.stage}]",
                                            color = if (log.isError) NeonCrimson else PurpleGlowLight,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = log.message,
                                            color = if (log.isError) NeonCrimson else TextWhite,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (showSettingsSheet) {
            ControllerSettingsSheet(
                settings = settings,
                onHapticChange = onHapticChange,
                onSoundChange = onSoundChange,
                onAnimationsChange = onAnimationsChange,
                onRememberLastModeChange = onRememberLastModeChange,
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}

@Composable
private fun DiagnosticItemRow(label: String, isSuccess: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextWhite, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isSuccess) "✓ PASS" else "• WAITING",
                color = if (isSuccess) GreenActiveTurnLight else TextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
