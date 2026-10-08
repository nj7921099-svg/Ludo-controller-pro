package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppTab
import com.example.ui.components.BottomNavBar
import com.example.ui.tabs.ConnectionTab
import com.example.ui.tabs.ControllerMainTab
import com.example.ui.theme.AccentGlowPrimary
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main screen coordinating the two tabs:
 * 1. Controller Tab (Left): Classic Simple Mode board & Pro Mode B1..B4 controls
 * 2. Connection Tab (Right): Hotspot-first Wi-Fi discovery, TCP handshake & verification, tests
 *
 * Full-screen layout with clean, edge-to-edge dark theme without harsh purple borders,
 * smooth animations, auto-dismissing timed alerts (3 seconds), and horizontal swipe transitions.
 */
@Composable
fun ControllerScreen(
    viewModel: ControllerViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    // Tabs ordered: 0 -> CONTROLLER (Left), 1 -> CONNECTION (Right)
    val tabs = remember { listOf(AppTab.CONTROLLER, AppTab.CONNECTION) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    val connectionState by viewModel.connectionState.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val discoveredHosts by viewModel.discoveredHosts.collectAsState()
    val diagnosticLogs by viewModel.diagnosticLogs.collectAsState()
    val numberTestState by viewModel.numberTestState.collectAsState()

    val boxCount by viewModel.boxCount.collectAsState()
    val boxes by viewModel.boxes.collectAsState()
    val selectedBoxForInput by viewModel.selectedBoxForInput.collectAsState()
    val lastSentInfo by viewModel.lastSentInfo.collectAsState()
    val lastReceivedInfo by viewModel.lastReceivedInfo.collectAsState()
    val lastResultStatus by viewModel.lastResultStatus.collectAsState()
    val systemAlert by viewModel.systemAlert.collectAsState()
    val isSimulationMode by viewModel.isSimulationMode.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val currentTab = tabs.getOrElse(pagerState.currentPage) { AppTab.CONTROLLER }

    // Auto-dismiss alert pop-up with a strict 3-second time limit as requested
    LaunchedEffect(systemAlert) {
        if (systemAlert != null) {
            delay(3000)
            viewModel.dismissAlert()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = PitchBlack,
        bottomBar = {
            BottomNavBar(
                selectedTab = currentTab,
                onTabSelected = { tab ->
                    val pageIndex = tabs.indexOf(tab).coerceAtLeast(0)
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            page = pageIndex,
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Global Alert / Toast Banner with sleek design and smooth slide/fade animations
            AnimatedVisibility(
                visible = systemAlert != null,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(250))
            ) {
                systemAlert?.let { alertText ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceCardElevated,
                        border = BorderStroke(1.dp, Color(0xFF263248)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = AccentGlowPrimary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentGlowPrimary)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = alertText,
                                    color = TextWhite,
                                    fontSize = 12.5.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            IconButton(
                                onClick = { viewModel.dismissAlert() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Swipeable HorizontalPager between Controller and Connection tabs with smooth animation
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page: Int ->
                when (tabs[page]) {
                    AppTab.CONTROLLER -> {
                        ControllerMainTab(
                            connectionState = connectionState,
                            boxCount = boxCount,
                            boxes = boxes,
                            selectedBoxForInput = selectedBoxForInput,
                            lastSentInfo = lastSentInfo,
                            lastReceivedInfo = lastReceivedInfo,
                            lastResultStatus = lastResultStatus,
                            isSimulationMode = isSimulationMode,
                            settings = settings,
                            onBoxTap = { controllerId ->
                                viewModel.onBoxTap(controllerId)
                            },
                            onNumberSelected = { controllerId, number ->
                                viewModel.sendNumberForBox(controllerId, number)
                            },
                            onModeChanged = { mode ->
                                viewModel.setLastMode(mode)
                            },
                            onNavigateToConnect = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        page = 1,
                                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                        )
                    }

                    AppTab.CONNECTION -> {
                        ConnectionTab(
                            connectionState = connectionState,
                            diagnostics = diagnostics,
                            discoveredHosts = discoveredHosts,
                            diagnosticLogs = diagnosticLogs,
                            numberTestState = numberTestState,
                            settings = settings,
                            onHapticChange = { viewModel.setHapticEnabled(it) },
                            onSoundChange = { viewModel.setSoundEffectsEnabled(it) },
                            onAnimationsChange = { viewModel.setAnimationsEnabled(it) },
                            onRememberLastModeChange = { viewModel.setRememberLastMode(it) },
                            onOpenHotspotSettings = { viewModel.openHotspotSettings() },
                            onRefreshNetwork = { viewModel.refreshNetworkStatus() },
                            onStartDiscovery = { viewModel.startDiscovery() },
                            onStopDiscovery = { viewModel.stopDiscovery() },
                            onConnectHost = { host -> viewModel.connectToHost(host) },
                            onConnectManualIp = { ip, port -> viewModel.connectWithManualIp(ip, port) },
                            onTestConnection = { viewModel.testConnection() },
                            onSendNumberTest = { num -> viewModel.sendNumberTest(num) },
                            onDisconnect = { viewModel.disconnect() },
                            onClearLogs = { viewModel.clearDiagnosticsLog() }
                        )
                    }
                }
            }
        }
    }
}
