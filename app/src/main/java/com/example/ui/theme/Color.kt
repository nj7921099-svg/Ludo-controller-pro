package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Pure pitch black background matching reference image
val PitchBlack = Color(0xFF030305)
val VoidBackground = Color(0xFF050508)
val SurfaceCard = Color(0xFF0B0B12)
val SurfaceCardElevated = Color(0xFF131320)
val SurfaceCardPressed = Color(0xFF1B1B2C)

// Sleek Cyber Dark Accents (Refined subtle borders & modern cyan/indigo glow instead of harsh purple lines)
val CyberBorderSubtle = Color(0xFF1E2230)
val CyberBorderBright = Color(0xFF2E3852)
val CyberCardBorder = Color(0xFF262C3E)
val CyberButtonBg = Color(0xFF10131C)
val AccentGlowPrimary = Color(0xFF00E5FF)
val AccentGlowSecondary = Color(0xFF3B82F6)
val AccentGlowLight = Color(0xFF67E8F9)

// Backwards-compatible aliases for existing references
val PurpleOuterGlow = Color(0xFF38BDF8)
val PurpleGlowPrimary = Color(0xFF00E5FF)
val PurpleGlowSecondary = Color(0xFF2563EB)
val PurpleGlowLight = Color(0xFF67E8F9)
val PurpleBorderSubtle = Color(0xFF1C2232)
val PurpleBorderBright = Color(0xFF28334A)
val PurpleCardBorder = Color(0xFF222B3D)
val PurpleButtonBg = Color(0xFF101420)

// Green Active Turn Indicators
val GreenActiveTurn = Color(0xFF22C55E)
val GreenActiveTurnLight = Color(0xFF4ADE80)
val GreenActiveTurnGlow = Color(0xFF16A34A)
val GreenActiveBadge = Color(0xFF15803D)

// Connection Status Accents
val RedDisconnected = Color(0xFFEF4444)
val RedDisconnectedGlow = Color(0xFFDC2626)
val GreenConnected = Color(0xFF22C55E)
val NeonAmber = Color(0xFFFFB703)
val NeonCyan = Color(0xFF00E5FF)
val NeonCrimson = Color(0xFFFF2A6D)

// Text Colors
val TextWhite = Color(0xFFFFFFFF)
val TextLight = Color(0xFFF1F5F9)
val TextMuted = Color(0xFF94A3B8)
val TextDim = Color(0xFF64748B)

// Compatibility aliases
val NeonEmerald = GreenActiveTurn
val SurfaceBorder = PurpleBorderSubtle
val TextPrimary = TextWhite
val TextSecondary = TextMuted

// Channel Colors for B1..B6 (Ludo color mapped)
val ChannelColors = listOf(
    Color(0xFFFF3366), // B1 - Red
    Color(0xFF22C55E), // B2 - Green
    Color(0xFFFFD600), // B3 - Yellow
    Color(0xFF00B0FF), // B4 - Blue
    Color(0xFFA855F7), // B5 - Purple
    Color(0xFFFF6D00)  // B6 - Orange
)

fun getChannelColor(channel: Int): Color {
    val index = (channel - 1).coerceIn(0, ChannelColors.size - 1)
    return ChannelColors[index]
}
