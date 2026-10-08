package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.example.ui.theme.GreenActiveBadge
import com.example.ui.theme.GreenActiveTurn
import com.example.ui.theme.GreenActiveTurnLight
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PurpleBorderBright
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Classic Ludo Board Colors matching reference image
private val LudoGreen = Color(0xFF00A651)
private val LudoYellow = Color(0xFFFFD600)
private val LudoRed = Color(0xFFE52521)
private val LudoBlue = Color(0xFF00A0E9)
private val TrackWhite = Color(0xFFFFFFFF)
private val GridBorderColor = Color(0xFF888888)

/**
 * EXACT 4-CORNER CLASSIC LUDO LAYOUT:
 * - GREEN  -> TOP-LEFT     (Alignment.TopStart,    Channel 2, Box B2)
 * - YELLOW -> TOP-RIGHT    (Alignment.TopEnd,      Channel 3, Box B3)
 * - RED    -> BOTTOM-LEFT  (Alignment.BottomStart, Channel 1, Box B1)
 * - BLUE   -> BOTTOM-RIGHT (Alignment.BottomEnd,   Channel 4, Box B4)
 * Center cross and triangles remain completely open and free in the center.
 */
data class SimplePlayerYard(
    val boxId: String,
    val channel: Int,
    val name: String,
    val color: Color,
    val alignment: Alignment
)

val ClassicSimpleYards = listOf(
    SimplePlayerYard(boxId = "B2", channel = 2, name = "GREEN", color = LudoGreen, alignment = Alignment.TopStart),
    SimplePlayerYard(boxId = "B3", channel = 3, name = "YELLOW", color = LudoYellow, alignment = Alignment.TopEnd),
    SimplePlayerYard(boxId = "B1", channel = 1, name = "RED", color = LudoRed, alignment = Alignment.BottomStart),
    SimplePlayerYard(boxId = "B4", channel = 4, name = "BLUE", color = LudoBlue, alignment = Alignment.BottomEnd)
)

/**
 * Simple Mode Board View:
 * Renders the 15x15 classic Ludo board with the 4 player yards strictly placed
 * in the four corner quadrants:
 * - Top-Left: Green
 * - Top-Right: Yellow
 * - Bottom-Left: Red
 * - Bottom-Right: Blue
 * - Center: Open for Ludo path / center cross
 */
@Composable
fun LudoSimpleBoardView(
    boxes: Map<String, BoxChannelState>,
    boxCount: Int,
    selectedBoxForInput: String?,
    onYardClick: (String) -> Unit,
    onNumberSelected: ((String, Int) -> Unit)? = null,
    onDismissSelection: (() -> Unit)? = null,
    hapticEnabled: Boolean = true,
    animationsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val boardSize = maxWidth
        val cellSize = boardSize / 15f
        // Each corner yard is exactly 6 out of 15 cells (40% of board size)
        val yardSize = cellSize * 6f

        // 1. Pixel-perfect 15x15 Ludo Board Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLudoBoard(size = size)
        }

        // 2. Interactive Overlay Layers for the 4 Player Yards in the EXACT 4 CORNERS
        ClassicSimpleYards.forEach { yard ->
            val isActive = boxes.containsKey(yard.boxId)
            val boxState = boxes[yard.boxId] ?: BoxChannelState.createDefault(yard.channel)
            val isSelected = (yard.boxId == selectedBoxForInput)

            Box(
                modifier = Modifier
                    .size(yardSize)
                    .align(yard.alignment)
                    .padding(3.dp) // Clean margin keeping clear of central paths
            ) {
                PlayerYardCard(
                    yard = yard,
                    boxState = boxState,
                    isActive = isActive,
                    isSelected = isSelected,
                    pulseAlpha = pulseAlpha,
                    hapticEnabled = hapticEnabled,
                    animationsEnabled = animationsEnabled,
                    onNumberSelected = { num ->
                        onNumberSelected?.invoke(yard.boxId, num)
                    },
                    onDismiss = {
                        onDismissSelection?.invoke()
                    },
                    onClick = {
                        onYardClick(yard.boxId)
                    }
                )
            }
        }

        // 3. Central LUDO Brand Emblem at the center intersection
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(cellSize * 2.8f),
            shape = CircleShape,
            color = Color(0xFF090D16).copy(alpha = 0.94f),
            border = BorderStroke(1.2.dp, Color(0xFF2E3852)),
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LUDO",
                    color = TextWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

/**
 * Interactive touch target card for one player yard:
 * Feels like a physical Ludo remote button:
 * - Clear player color & name
 * - Player number & box ID
 * - Subtle glass/card gradient treatment
 * - Soft player-color glow and rounded corners
 * - Scale animation & haptic feedback on tap
 * - Selected and inactive states
 */
@Composable
private fun PlayerYardCard(
    yard: SimplePlayerYard,
    boxState: BoxChannelState,
    isActive: Boolean,
    isSelected: Boolean,
    pulseAlpha: Float,
    hapticEnabled: Boolean,
    animationsEnabled: Boolean,
    onNumberSelected: (Int) -> Unit = {},
    onDismiss: () -> Unit = {},
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    var isSending by remember { mutableStateOf(false) }
    var sentNumber by remember { mutableStateOf<Int?>(null) }

    val borderColor = when {
        !isActive -> Color(0xFF1E2433).copy(alpha = 0.5f)
        isSelected -> if (animationsEnabled) Color.White.copy(alpha = pulseAlpha) else Color.White
        else -> yard.color.copy(alpha = 0.85f)
    }

    val borderWidth = if (isSelected) 2.5.dp else if (isActive) 1.6.dp else 1.dp

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("simple_yard_${yard.boxId}")
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                onClick = {
                    if (isActive) {
                        ControllerHaptics.playPlayerTap(context, hapticEnabled)
                        if (animationsEnabled) {
                            coroutineScope.launch {
                                scale.animateTo(1.04f, animationSpec = tween(70, easing = FastOutSlowInEasing))
                                scale.animateTo(1.00f, animationSpec = tween(120, easing = FastOutSlowInEasing))
                            }
                        }
                        onClick()
                    } else {
                        ControllerHaptics.playInvalidTap(context, hapticEnabled)
                    }
                }
            ),
        shape = RoundedCornerShape(14.dp),
        color = when {
            !isActive -> Color(0xFF090D14).copy(alpha = 0.85f)
            isSelected -> Color(0xFF0D121F)
            else -> yard.color.copy(alpha = 0.18f)
        },
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = if (isSelected) 8.dp else if (isActive) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isActive) {
                        Brush.verticalGradient(
                            listOf(
                                yard.color.copy(alpha = if (isSelected) 0.38f else 0.22f),
                                Color(0xFF0C101A).copy(alpha = 0.94f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF101420).copy(alpha = 0.70f),
                                Color(0xFF080B12).copy(alpha = 0.90f)
                            )
                        )
                    }
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            // When user taps this box, open the polished in-place dice number selection right inside this card!
            if (isActive && isSelected) {
                AnimatedVisibility(
                    visible = true,
                    enter = if (animationsEnabled) {
                        scaleIn(initialScale = 0.85f, animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(180))
                    } else fadeIn(tween(0)),
                    exit = if (animationsEnabled) {
                        scaleOut(targetScale = 0.85f, animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeOut(tween(150))
                    } else fadeOut(tween(0))
                ) {
                    if (sentNumber != null) {
                        // Short confirmation animation right in this player box: "✓ 6 SENT"
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Sent",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "✓ $sentNumber SENT",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = yard.name,
                                color = yard.color,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        // In-place Dice Numbers (1..6) with top bar and 2 rows of 3 dice buttons
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp, vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Mini Header: Player label and Close button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(yard.color)
                                    )
                                    Text(
                                        text = "${yard.name} (${yard.boxId})",
                                        color = TextWhite,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .clickable {
                                            if (!isSending) onDismiss()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = TextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // 2 rows of 3 numbers (1, 2, 3 and 4, 5, 6)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                // Row 1: 1, 2, 3
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    for (num in 1..3) {
                                        InYardDiceNumberButton(
                                            number = num,
                                            accentColor = yard.color,
                                            enabled = !isSending,
                                            animationsEnabled = animationsEnabled,
                                            onClick = {
                                                if (isSending) return@InYardDiceNumberButton
                                                isSending = true
                                                ControllerHaptics.playNumberSelect(context, hapticEnabled)
                                                onNumberSelected(num)
                                                ControllerHaptics.playSendSuccess(context, hapticEnabled)
                                                sentNumber = num
                                                coroutineScope.launch {
                                                    delay(700)
                                                    isSending = false
                                                    sentNumber = null
                                                    onDismiss()
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Row 2: 4, 5, 6
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    for (num in 4..6) {
                                        InYardDiceNumberButton(
                                            number = num,
                                            accentColor = yard.color,
                                            enabled = !isSending,
                                            animationsEnabled = animationsEnabled,
                                            onClick = {
                                                if (isSending) return@InYardDiceNumberButton
                                                isSending = true
                                                ControllerHaptics.playNumberSelect(context, hapticEnabled)
                                                onNumberSelected(num)
                                                ControllerHaptics.playSendSuccess(context, hapticEnabled)
                                                sentNumber = num
                                                coroutineScope.launch {
                                                    delay(700)
                                                    isSending = false
                                                    sentNumber = null
                                                    onDismiss()
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
            } else {
                // Default Remote Button View for the player yard
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 1. Clear Player Color Title
                    Text(
                        text = yard.name,
                        color = if (!isActive) TextMuted else if (yard.color == LudoYellow) Color(0xFFFFD600) else yard.color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    // 2. Player Name / Number
                    Text(
                        text = "PLAYER ${yard.channel}",
                        color = if (isActive) TextWhite else TextDim,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "(${yard.boxId})",
                        color = if (isActive) yard.color.copy(alpha = 0.85f) else TextDim,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    if (!isActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Inactive",
                                tint = TextDim,
                                modifier = Modifier.size(9.dp)
                            )
                            Text(
                                text = "INACTIVE",
                                color = TextDim,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        val (statusLabel, statusBg, statusTextColor) = when (boxState.status) {
                            BoxCommandStatus.SELECTING -> Triple("SELECT", GreenActiveBadge, GreenActiveTurnLight)
                            BoxCommandStatus.SENT -> Triple("SENT: ${boxState.pendingCommandNumber ?: ""}", PurpleBorderBright, TextWhite)
                            BoxCommandStatus.ACKNOWLEDGED -> Triple("ACK (${boxState.pendingCommandNumber ?: ""})", PurpleBorderBright, GreenActiveTurnLight)
                            BoxCommandStatus.REVEALED -> Triple("REV: ${boxState.revealedValue ?: ""}", GreenActiveBadge, TextWhite)
                            BoxCommandStatus.SENDING -> Triple("...", NeonAmber, TextWhite)
                            BoxCommandStatus.READY -> Triple("TAP TO ROLL", Color.Black.copy(alpha = 0.5f), GreenActiveTurnLight)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusBg,
                            border = BorderStroke(0.8.dp, yard.color.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = statusLabel,
                                color = statusTextColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact in-yard dice button matching physical remote dice buttons.
 * Renders large number and authentic dice dots.
 */
@Composable
private fun InYardDiceNumberButton(
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
            .testTag("inbox_dice_$number")
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                onClick = {
                    if (animationsEnabled) {
                        coroutineScope.launch {
                            scale.animateTo(1.10f, animationSpec = tween(50, easing = FastOutSlowInEasing))
                            scale.animateTo(1.00f, animationSpec = tween(80, easing = FastOutSlowInEasing))
                        }
                    }
                    onClick()
                }
            ),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF141926),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (enabled) 0.85f else 0.3f)),
        shadowElevation = if (enabled) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number.toString(),
                color = if (enabled) TextWhite else TextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            DiceDots(
                number = number,
                dotColor = if (enabled) accentColor else TextDim.copy(alpha = 0.4f),
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

/**
 * Draws the 15x15 classic Ludo board on Canvas.
 */
private fun DrawScope.drawLudoBoard(size: Size) {
    val totalWidth = size.width
    val cellSize = totalWidth / 15f
    val strokeWidth = 1.2f

    // Background
    drawRect(color = TrackWhite, size = size)

    // 1. The 4 Big Corner Yards (6x6 cells each)
    val yardSizePx = cellSize * 6f

    // Top-Left (Green)
    drawRect(color = LudoGreen, topLeft = Offset(0f, 0f), size = Size(yardSizePx, yardSizePx))
    drawRect(
        color = TrackWhite,
        topLeft = Offset(cellSize, cellSize),
        size = Size(cellSize * 4f, cellSize * 4f)
    )
    drawYardTokenCircles(LudoGreen, cellSize, 0f, 0f)

    // Top-Right (Yellow)
    drawRect(color = LudoYellow, topLeft = Offset(cellSize * 9f, 0f), size = Size(yardSizePx, yardSizePx))
    drawRect(
        color = TrackWhite,
        topLeft = Offset(cellSize * 10f, cellSize),
        size = Size(cellSize * 4f, cellSize * 4f)
    )
    drawYardTokenCircles(LudoYellow, cellSize, cellSize * 9f, 0f)

    // Bottom-Left (Red)
    drawRect(color = LudoRed, topLeft = Offset(0f, cellSize * 9f), size = Size(yardSizePx, yardSizePx))
    drawRect(
        color = TrackWhite,
        topLeft = Offset(cellSize, cellSize * 10f),
        size = Size(cellSize * 4f, cellSize * 4f)
    )
    drawYardTokenCircles(LudoRed, cellSize, 0f, cellSize * 9f)

    // Bottom-Right (Blue)
    drawRect(color = LudoBlue, topLeft = Offset(cellSize * 9f, cellSize * 9f), size = Size(yardSizePx, yardSizePx))
    drawRect(
        color = TrackWhite,
        topLeft = Offset(cellSize * 10f, cellSize * 10f),
        size = Size(cellSize * 4f, cellSize * 4f)
    )
    drawYardTokenCircles(LudoBlue, cellSize, cellSize * 9f, cellSize * 9f)

    // 2. Center 3x3 Home Triangles
    val centerLeft = cellSize * 6f
    val centerTop = cellSize * 6f
    val centerRight = cellSize * 9f
    val centerBottom = cellSize * 9f
    val centerMidX = cellSize * 7.5f
    val centerMidY = cellSize * 7.5f

    // Left Triangle (Green)
    val greenTri = Path().apply {
        moveTo(centerLeft, centerTop)
        lineTo(centerLeft, centerBottom)
        lineTo(centerMidX, centerMidY)
        close()
    }
    drawPath(greenTri, color = LudoGreen)

    // Top Triangle (Yellow)
    val yellowTri = Path().apply {
        moveTo(centerLeft, centerTop)
        lineTo(centerRight, centerTop)
        lineTo(centerMidX, centerMidY)
        close()
    }
    drawPath(yellowTri, color = LudoYellow)

    // Right Triangle (Blue)
    val blueTri = Path().apply {
        moveTo(centerRight, centerTop)
        lineTo(centerRight, centerBottom)
        lineTo(centerMidX, centerMidY)
        close()
    }
    drawPath(blueTri, color = LudoBlue)

    // Bottom Triangle (Red)
    val redTri = Path().apply {
        moveTo(centerLeft, centerBottom)
        lineTo(centerRight, centerBottom)
        lineTo(centerMidX, centerMidY)
        close()
    }
    drawPath(redTri, color = LudoRed)

    // 3. Colored Home Run Columns / Rows
    // Top arm: Yellow home column (col 7, rows 1..5)
    for (r in 1..5) {
        drawRect(color = LudoYellow, topLeft = Offset(cellSize * 7f, cellSize * r), size = Size(cellSize, cellSize))
    }
    // Top arm starting arrow cell (col 6, row 1)
    drawRect(color = LudoYellow, topLeft = Offset(cellSize * 8f, cellSize * 1f), size = Size(cellSize, cellSize))

    // Left arm: Green home row (cols 1..5, row 7)
    for (c in 1..5) {
        drawRect(color = LudoGreen, topLeft = Offset(cellSize * c, cellSize * 7f), size = Size(cellSize, cellSize))
    }
    // Left arm start cell (col 1, row 6)
    drawRect(color = LudoGreen, topLeft = Offset(cellSize * 1f, cellSize * 6f), size = Size(cellSize, cellSize))

    // Bottom arm: Red home column (col 7, rows 9..13)
    for (r in 9..13) {
        drawRect(color = LudoRed, topLeft = Offset(cellSize * 7f, cellSize * r), size = Size(cellSize, cellSize))
    }
    // Bottom arm start cell (col 6, row 13)
    drawRect(color = LudoRed, topLeft = Offset(cellSize * 6f, cellSize * 13f), size = Size(cellSize, cellSize))

    // Right arm: Blue home row (cols 9..13, row 7)
    for (c in 9..13) {
        drawRect(color = LudoBlue, topLeft = Offset(cellSize * c, cellSize * 7f), size = Size(cellSize, cellSize))
    }
    // Right arm start cell (col 13, row 8)
    drawRect(color = LudoBlue, topLeft = Offset(cellSize * 13f, cellSize * 8f), size = Size(cellSize, cellSize))

    // 4. Grid lines on tracks
    // Top arm (cols 6..8, rows 0..5)
    for (r in 0..5) {
        for (c in 6..8) {
            drawRect(
                color = GridBorderColor,
                topLeft = Offset(c * cellSize, r * cellSize),
                size = Size(cellSize, cellSize),
                style = Stroke(strokeWidth)
            )
        }
    }

    // Left arm (cols 0..5, rows 6..8)
    for (r in 6..8) {
        for (c in 0..5) {
            drawRect(
                color = GridBorderColor,
                topLeft = Offset(c * cellSize, r * cellSize),
                size = Size(cellSize, cellSize),
                style = Stroke(strokeWidth)
            )
        }
    }

    // Bottom arm (cols 6..8, rows 9..14)
    for (r in 9..14) {
        for (c in 6..8) {
            drawRect(
                color = GridBorderColor,
                topLeft = Offset(c * cellSize, r * cellSize),
                size = Size(cellSize, cellSize),
                style = Stroke(strokeWidth)
            )
        }
    }

    // Right arm (cols 9..14, rows 6..8)
    for (r in 6..8) {
        for (c in 9..14) {
            drawRect(
                color = GridBorderColor,
                topLeft = Offset(c * cellSize, r * cellSize),
                size = Size(cellSize, cellSize),
                style = Stroke(strokeWidth)
            )
        }
    }

    // Safe Stars
    drawSafeStar(cellSize * 8.5f, cellSize * 2.5f, cellSize * 0.35f, Color(0xFF444444)) // Top arm star
    drawSafeStar(cellSize * 2.5f, cellSize * 8.5f, cellSize * 0.35f, Color(0xFF444444)) // Left arm star
    drawSafeStar(cellSize * 6.5f, cellSize * 12.5f, cellSize * 0.35f, Color(0xFF444444)) // Bottom arm star
    drawSafeStar(cellSize * 12.5f, cellSize * 6.5f, cellSize * 0.35f, Color(0xFF444444)) // Right arm star

    // Outer border around the whole board
    drawRect(
        color = GridBorderColor,
        topLeft = Offset(0f, 0f),
        size = size,
        style = Stroke(strokeWidth * 2f)
    )
}

/**
 * Draws the 4 circular token spots in each home yard.
 */
private fun DrawScope.drawYardTokenCircles(color: Color, cellSize: Float, originX: Float, originY: Float) {
    val radius = cellSize * 0.55f
    val positions = listOf(
        Offset(originX + cellSize * 2.2f, originY + cellSize * 2.2f),
        Offset(originX + cellSize * 3.8f, originY + cellSize * 2.2f),
        Offset(originX + cellSize * 2.2f, originY + cellSize * 3.8f),
        Offset(originX + cellSize * 3.8f, originY + cellSize * 3.8f)
    )

    positions.forEach { center ->
        drawCircle(color = color, radius = radius, center = center)
        drawCircle(color = Color.White, radius = radius * 0.55f, center = center)
    }
}

/**
 * Draws a 5-pointed star on safe squares.
 */
private fun DrawScope.drawSafeStar(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path()
    val innerRadius = radius * 0.45f
    val points = 5
    var angle = -Math.PI / 2

    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val x = cx + (r * Math.cos(angle)).toFloat()
        val y = cy + (r * Math.sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        angle += Math.PI / points
    }
    path.close()
    drawPath(path, color = color, style = Stroke(1.2f))
}
