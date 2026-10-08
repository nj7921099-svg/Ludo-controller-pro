package com.example.ui.tabs

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controller.BoxChannelState
import com.example.controller.BoxCommandStatus
import com.example.controller.settings.ControllerHaptics
import com.example.controller.settings.ControllerSettings
import com.example.network.ConnectionState
import com.example.ui.components.ClassicSimpleYards
import com.example.ui.components.DiceDots
import com.example.ui.components.GlobalConnectionIndicator
import com.example.ui.components.LudoSimpleBoardView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.theme.GreenActiveBadge
import com.example.ui.theme.GreenActiveTurn
import com.example.ui.theme.GreenActiveTurnLight
import com.example.ui.theme.GreenConnected
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PurpleBorderBright
import com.example.ui.theme.PurpleBorderSubtle
import com.example.ui.theme.PurpleButtonBg
import com.example.ui.theme.PurpleCardBorder
import com.example.ui.theme.PurpleGlowLight
import com.example.ui.theme.PurpleGlowPrimary
import com.example.ui.theme.PurpleOuterGlow
import com.example.ui.theme.RedDisconnected
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

/**
 * Controller Tab matching reference image:
 * - NO turn order / NO clockwise system in App 2.
 * - Any box can be tapped at any time.
 * - Tapping a box opens the number selection panel specifically for that box.
 * - Number selection sends exactly once with a unique commandId.
 * - Multiple independent pending commands are allowed simultaneously across boxes.
 */
@Composable
fun ControllerMainTab(
    connectionState: ConnectionState,
    boxCount: Int,
    boxes: Map<String, BoxChannelState>,
    selectedBoxForInput: String?,
    lastSentInfo: Pair<String, Int>?,
    lastReceivedInfo: String?,
    lastResultStatus: String,
    isSimulationMode: Boolean,
    settings: ControllerSettings,
    onBoxTap: (String) -> Unit,
    onNumberSelected: (String, Int) -> Unit,
    onModeChanged: (String) -> Unit = {},
    onNavigateToConnect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentMode by remember {
        mutableStateOf(
            if (settings.rememberLastMode && settings.lastMode == "PRO") ControllerMode.PRO else ControllerMode.SIMPLE
        )
    }
    var isCommunicationExpanded by remember { mutableStateOf(true) }

    // Connection status (single source of truth)
    val isConnected = connectionState.isConnected || (isSimulationMode && true)

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOP HEADER WITH GLOBAL TOP-LEFT INDICATOR
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, Color(0xFF1E2638))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlobalConnectionIndicator(
                        connectionState = connectionState,
                        onClick = onNavigateToConnect
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CONTROLLER",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = if (isConnected) "Ludo Host Active" else "Tap indicator to connect",
                            color = if (isConnected) GreenActiveTurnLight else TextDim,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 2. MODE SELECTION BAR (Right above DETECTED BOXES / LUDO BOARD)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mode_selector_bar"),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, Color(0xFF1E2638))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Simple Mode Pill (Default)
                    val isSimple = (currentMode == ControllerMode.SIMPLE)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("mode_simple_btn")
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                ControllerHaptics.playPlayerTap(context, settings.hapticEnabled)
                                currentMode = ControllerMode.SIMPLE
                                if (settings.rememberLastMode) {
                                    onModeChanged(ControllerMode.SIMPLE.name)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSimple) Color(0xFF0F2B48) else Color.Transparent,
                        border = if (isSimple) BorderStroke(1.dp, Color(0xFF00E5FF)) else null,
                        shadowElevation = if (isSimple) 4.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🎲 SIMPLE MODE",
                                color = if (isSimple) Color(0xFF00E5FF) else TextDim,
                                fontSize = 12.sp,
                                fontWeight = if (isSimple) FontWeight.Black else FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Pro Mode Pill
                    val isPro = (currentMode == ControllerMode.PRO)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("mode_pro_btn")
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                ControllerHaptics.playPlayerTap(context, settings.hapticEnabled)
                                currentMode = ControllerMode.PRO
                                if (settings.rememberLastMode) {
                                    onModeChanged(ControllerMode.PRO.name)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPro) Color(0xFF0F2B48) else Color.Transparent,
                        border = if (isPro) BorderStroke(1.dp, Color(0xFF00E5FF)) else null,
                        shadowElevation = if (isPro) 4.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "⚙️ PRO MODE",
                                color = if (isPro) Color(0xFF00E5FF) else TextDim,
                                fontSize = 12.sp,
                                fontWeight = if (isPro) FontWeight.Black else FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // 3. MAIN CONTROLLER CONTENT (SIMPLE vs PRO)
            if (currentMode == ControllerMode.SIMPLE) {
                // ==============================
                // SIMPLE MODE: CLASSIC LUDO REMOTE
                // ==============================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 15x15 Ludo Board View with 4 physical remote button player yards
                    // Tapping any active player box opens the number selection window smoothly IN-PLACE on that exact player box!
                    LudoSimpleBoardView(
                        boxes = boxes,
                        boxCount = boxCount,
                        selectedBoxForInput = selectedBoxForInput,
                        onYardClick = { boxId ->
                            onBoxTap(boxId)
                        },
                        onNumberSelected = { boxId, num ->
                            onNumberSelected(boxId, num)
                        },
                        onDismissSelection = {
                            onBoxTap("")
                        },
                        hapticEnabled = settings.hapticEnabled,
                        animationsEnabled = settings.animationsEnabled,
                        modifier = Modifier
                            .fillMaxWidth(0.96f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF1E283C), RoundedCornerShape(16.dp))
                    )

                    val selectedYard = ClassicSimpleYards.firstOrNull { it.boxId == selectedBoxForInput }
                    val isSelectedActive = selectedYard != null && boxes.containsKey(selectedYard.boxId)

                    // Polished Dice-Number Selector Panel (1..6)
                    AnimatedVisibility(
                        visible = isSelectedActive,
                        enter = if (settings.animationsEnabled) {
                            expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn()
                        } else {
                            fadeIn(animationSpec = tween(0))
                        },
                        exit = if (settings.animationsEnabled) {
                            shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut()
                        } else {
                            fadeOut(animationSpec = tween(0))
                        }
                    ) {
                        if (selectedYard != null) {
                            SimpleDiceSelectorPanel(
                                yard = selectedYard,
                                hapticEnabled = settings.hapticEnabled,
                                animationsEnabled = settings.animationsEnabled,
                                onNumberSelected = { num ->
                                    onNumberSelected(selectedYard.boxId, num)
                                },
                                onDismiss = {
                                    onBoxTap("") // close selection
                                }
                            )
                        }
                    }

                    // Helper card when no player is selected
                    if (!isSelectedActive) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceCard,
                            border = BorderStroke(1.dp, Color(0xFF1E283C))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tap GREEN, YELLOW, RED, or BLUE above to roll",
                                    color = TextDim,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            } else {
                // ==============================
                // PRO MODE: EXISTING B1..B6 SYSTEM
                // ==============================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // DETECTED BOXES SECTION (ANY BOX CAN BE TAPPED)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DETECTED BOXES",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 1.2.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Row of B-boxes (strictly B1..B4, B5 completely removed)
                        val activeBoxes = (1..boxCount.coerceIn(2, 4)).map { ch ->
                            val id = "B$ch"
                            boxes[id] ?: BoxChannelState.createDefault(ch)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            activeBoxes.forEach { box ->
                                val isSelected = (box.controllerId == selectedBoxForInput)
                                PlayerBoxCard(
                                    box = box,
                                    isSelected = isSelected,
                                    onClick = {
                                        triggerHaptic(context)
                                        onBoxTap(box.controllerId)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Target box identification and assigned color
                        val targetBox = selectedBoxForInput ?: (boxes.keys.firstOrNull() ?: "B1")
                        val targetBoxColor = getBoxColor(targetBox)
                        val targetBoxName = getBoxColorName(targetBox)
                        val targetIndex = activeBoxes.indexOfFirst { it.controllerId == targetBox }.coerceAtLeast(0)

                        // Target box status line
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedBoxForInput != null) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(targetBoxColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ASSIGNING TO: ",
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "$targetBox ($targetBoxName)",
                                    color = targetBoxColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text(
                                    text = "Select any box above",
                                    color = TextDim,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Animated Connecting Arrow linking selected box to the number selection window
                        AnimatedConnectingArrow(
                            selectedIndex = targetIndex,
                            totalBoxes = activeBoxes.size,
                            boxColor = targetBoxColor
                        )

                        // SELECT NUMBER PANEL (with assigned box color border, shadow, and header)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = targetBoxColor),
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceCard,
                            border = BorderStroke(1.8.dp, targetBoxColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(targetBoxColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SELECT NUMBER FOR $targetBox ($targetBoxName)",
                                        color = TextWhite,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Buttons: Row 1 (1, 2, 3) - Square, height equals length
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    for (num in 1..3) {
                                        NumberPillButton(
                                            number = num,
                                            accentColor = targetBoxColor,
                                            onClick = {
                                                triggerHaptic(context)
                                                onNumberSelected(targetBox, num)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Buttons: Row 2 (4, 5, 6) - Square, height equals length
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    for (num in 4..6) {
                                        NumberPillButton(
                                            number = num,
                                            accentColor = targetBoxColor,
                                            onClick = {
                                                triggerHaptic(context)
                                                onNumberSelected(targetBox, num)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. LAST RESULT / COMMAND BAR
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, Color(0xFF1E2638))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LAST COMMAND - $lastResultStatus",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    val resultDisplay = if (lastSentInfo != null) {
                        "${lastSentInfo.first} → ${lastSentInfo.second}"
                    } else {
                        "None"
                    }

                    Text(
                        text = resultDisplay,
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 5. COMMUNICATION PANEL (Collapsible with dropdown arrow)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, Color(0xFF1E2638))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCommunicationExpanded = !isCommunicationExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMMUNICATION",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        )

                        Icon(
                            imageVector = if (isCommunicationExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Communication Details",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isCommunicationExpanded) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // SENT
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "SENT COMMANDS",
                                        color = TextDim,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (lastSentInfo != null) "${lastSentInfo.first} → ${lastSentInfo.second}" else "None",
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // RECEIVED
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "APP 1 STATUS",
                                        color = TextDim,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = lastReceivedInfo ?: "Ready",
                                        color = if (lastReceivedInfo?.contains("pending", ignoreCase = true) == true) TextMuted else GreenActiveTurnLight,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Helper to get the canonical Ludo color for each controller box:
 * B1 -> RED, B2 -> GREEN, B3 -> YELLOW, B4 -> BLUE
 */
private fun getBoxColor(boxId: String): Color {
    return when (boxId) {
        "B1" -> Color(0xFFE52521) // Classic Ludo Red
        "B2" -> Color(0xFF00A651) // Classic Ludo Green
        "B3" -> Color(0xFFFFD600) // Classic Ludo Yellow
        "B4" -> Color(0xFF00A0E9) // Classic Ludo Blue
        else -> PurpleGlowPrimary
    }
}

private fun getBoxColorName(boxId: String): String {
    return when (boxId) {
        "B1" -> "RED"
        "B2" -> "GREEN"
        "B3" -> "YELLOW"
        "B4" -> "BLUE"
        else -> ""
    }
}

/**
 * Animated connecting arrow bridging the selected box card and the number selection window.
 * Draws an active glowing connector and downward flowing animated pulse / arrowheads
 * that give a clear, direct connection between the selected box and the number selection window.
 */
@Composable
private fun AnimatedConnectingArrow(
    selectedIndex: Int,
    totalBoxes: Int,
    boxColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "connecting_arrow")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_arrow"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        val count = totalBoxes.coerceAtLeast(1)
        val validIndex = selectedIndex.coerceIn(0, count - 1)
        val spacing = 8.dp.toPx()
        val totalSpacing = spacing * (count - 1)
        val boxWidth = (size.width - totalSpacing) / count
        val startX = validIndex * (boxWidth + spacing) + boxWidth / 2f

        // 1. Subtle glowing guide line from selected box to panel
        drawLine(
            color = boxColor.copy(alpha = 0.35f * glowPulse),
            start = Offset(startX, 0f),
            end = Offset(startX, size.height),
            strokeWidth = 2.dp.toPx()
        )

        // 2. Animated pulse line segment traveling downward
        val beamLength = 12.dp.toPx()
        val beamY = pulseProgress * (size.height + beamLength) - beamLength
        val clampedStartY = beamY.coerceIn(0f, size.height)
        val clampedEndY = (beamY + beamLength).coerceIn(0f, size.height)
        if (clampedEndY > clampedStartY) {
            drawLine(
                color = Color.White.copy(alpha = 0.95f),
                start = Offset(startX, clampedStartY),
                end = Offset(startX, clampedEndY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 3. Moving chevron arrow head traveling downward
        val chevronY = (pulseProgress * (size.height - 10.dp.toPx())).coerceAtLeast(2.dp.toPx())
        val chevronWidth = 7.dp.toPx()
        val chevronHeight = 5.dp.toPx()
        val chevronPath = Path().apply {
            moveTo(startX - chevronWidth, chevronY)
            lineTo(startX, chevronY + chevronHeight)
            lineTo(startX + chevronWidth, chevronY)
        }
        drawPath(
            path = chevronPath,
            color = Color.White.copy(alpha = 0.9f),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 4. Anchored arrow head at the bottom pointing into the number selection window
        val arrowHeadWidth = 14.dp.toPx()
        val arrowHeadHeight = 8.dp.toPx()
        val arrowPath = Path().apply {
            moveTo(startX - arrowHeadWidth / 2f, size.height - arrowHeadHeight)
            lineTo(startX + arrowHeadWidth / 2f, size.height - arrowHeadHeight)
            lineTo(startX, size.height)
            close()
        }
        drawPath(
            path = arrowPath,
            color = boxColor
        )
        // Outer glow on arrow head
        drawPath(
            path = arrowPath,
            color = Color.White.copy(alpha = glowPulse * 0.7f),
            style = Stroke(width = 1.2.dp.toPx())
        )
    }
}

/**
 * Individual B-box card in the DETECTED BOXES row.
 * Color-coded with its respective classic Ludo color:
 * B1 (Red), B2 (Green), B3 (Yellow), B4 (Blue).
 * Shows status badge and connection notch if selected.
 */
@Composable
private fun PlayerBoxCard(
    box: BoxChannelState,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boxColor = getBoxColor(box.controllerId)
    val colorName = getBoxColorName(box.controllerId)
    val (statusLabel, statusBg, statusColor) = when (box.status) {
        BoxCommandStatus.SELECTING -> Triple("EDIT", GreenActiveBadge, GreenActiveTurnLight)
        BoxCommandStatus.SENT -> Triple("→ ${box.pendingCommandNumber ?: ""}", boxColor, TextWhite)
        BoxCommandStatus.ACKNOWLEDGED -> Triple("ACK", PurpleBorderBright, GreenActiveTurnLight)
        BoxCommandStatus.REVEALED -> Triple("REV: ${box.revealedValue ?: ""}", GreenActiveBadge, TextWhite)
        BoxCommandStatus.SENDING -> Triple("...", NeonAmber, TextWhite)
        BoxCommandStatus.READY -> Triple("READY", Color(0xFF1B1B2A), TextDim)
    }

    Surface(
        modifier = modifier
            .testTag("box_${box.controllerId}")
            .height(78.dp)
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = boxColor,
                ambientColor = boxColor
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) boxColor.copy(alpha = 0.25f) else Color(0xFF0C0A14),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.2.dp,
            color = if (isSelected) boxColor else boxColor.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top colored indicator bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .background(boxColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = box.controllerId,
                    color = if (isSelected) TextWhite else boxColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = colorName,
                    color = boxColor.copy(alpha = 0.85f),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) boxColor.copy(alpha = 0.85f) else statusBg
                ) {
                    Text(
                        text = statusLabel,
                        color = if (isSelected) TextWhite else statusColor,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Bottom connection notch for selected box
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(boxColor)
                )
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }
        }
    }
}

/**
 * Square dice-style number button [ 1 ], [ 2 ], etc.
 * Height is equal to width (aspectRatio 1:1) as requested.
 * Large bold text font and themed accent glow.
 */
@Composable
private fun NumberPillButton(
    number: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PurpleGlowPrimary
) {
    Surface(
        modifier = modifier
            .testTag("num_btn_$number")
            .aspectRatio(1f) // Square: height is equal to length/width as requested!
            .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = accentColor)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor),
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = PurpleButtonBg,
        border = BorderStroke(1.5.dp, accentColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Polished Dice-Number Selector Panel for Simple Mode.
 * Visual physical remote feel:
 * - Clear player identity and color glow
 * - Numbers 1..6 formatted as authentic dice faces with dot patterns
 * - Accidental double tap protection: disables controls while send is in-flight
 * - Haptic feedback for selection and send confirmation
 * - Short confirmation animation: "✓ 6 SENT TO GREEN", then auto-returns to board
 */
@Composable
private fun SimpleDiceSelectorPanel(
    yard: com.example.ui.components.SimplePlayerYard,
    hapticEnabled: Boolean,
    animationsEnabled: Boolean,
    onNumberSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSending by remember { mutableStateOf(false) }
    var sentNumber by remember { mutableStateOf<Int?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = yard.color.copy(alpha = 0.5f))
            .testTag("simple_dice_selector_panel"),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceCard,
        border = BorderStroke(1.2.dp, yard.color.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (sentNumber != null) {
                // Short confirmation animation: ✓ 6 SENT
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "✓ $sentNumber SENT TO ${yard.name}",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Holding for player's turn on Ludo App",
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
            } else {
                // Header with player color badge and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(yard.color)
                        )
                        Text(
                            text = "${yard.name} — PLAYER ${yard.channel} (${yard.boxId})",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            if (!isSending) onDismiss()
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_close_dice_selector")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Numbers: Row 1 (1, 2, 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (num in 1..3) {
                        SimpleRemoteDiceButton(
                            number = num,
                            accentColor = yard.color,
                            enabled = !isSending,
                            animationsEnabled = animationsEnabled,
                            onClick = {
                                if (isSending) return@SimpleRemoteDiceButton
                                isSending = true
                                ControllerHaptics.playNumberSelect(context, hapticEnabled)
                                onNumberSelected(num)
                                ControllerHaptics.playSendSuccess(context, hapticEnabled)
                                sentNumber = num
                                coroutineScope.launch {
                                    delay(700)
                                    isSending = false
                                    sentNumber = null
                                    onDismiss() // return to Simple Mode board!
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Numbers: Row 2 (4, 5, 6)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (num in 4..6) {
                        SimpleRemoteDiceButton(
                            number = num,
                            accentColor = yard.color,
                            enabled = !isSending,
                            animationsEnabled = animationsEnabled,
                            onClick = {
                                if (isSending) return@SimpleRemoteDiceButton
                                isSending = true
                                ControllerHaptics.playNumberSelect(context, hapticEnabled)
                                onNumberSelected(num)
                                ControllerHaptics.playSendSuccess(context, hapticEnabled)
                                sentNumber = num
                                coroutineScope.launch {
                                    delay(700)
                                    isSending = false
                                    sentNumber = null
                                    onDismiss() // return to Simple Mode board!
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Physical Ludo Remote Dice button.
 * Resembles a physical dice face with dot pattern representation.
 */
@Composable
private fun SimpleRemoteDiceButton(
    number: Int,
    accentColor: Color,
    enabled: Boolean,
    animationsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = modifier
            .testTag("simple_dice_$number")
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                enabled = enabled,
                onClick = {
                    if (animationsEnabled) {
                        coroutineScope.launch {
                            scale.animateTo(1.08f, animationSpec = tween(60, easing = FastOutSlowInEasing))
                            scale.animateTo(1.00f, animationSpec = tween(100, easing = FastOutSlowInEasing))
                        }
                    }
                    onClick()
                }
            ),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF141926),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = if (enabled) 0.85f else 0.3f)),
        shadowElevation = if (enabled) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number.toString(),
                color = if (enabled) TextWhite else TextMuted,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            DiceDots(
                number = number,
                dotColor = if (enabled) accentColor else TextDim.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun triggerHaptic(context: Context, durationMs: Long = 35) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            v?.vibrate(durationMs)
        }
    } catch (_: Exception) {}
}
