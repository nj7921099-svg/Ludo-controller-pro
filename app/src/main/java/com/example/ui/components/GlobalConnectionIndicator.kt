package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.ConnectionState
import com.example.network.GlobalIndicatorStatus
import com.example.ui.theme.GreenActiveTurn
import com.example.ui.theme.GreenActiveTurnLight
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PurpleBorderSubtle
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextWhite

/**
 * Requirement 8: Global Top-Left Connection Indicator.
 * Every major screen/tab must show this exact indicator in the top-left corner.
 * States:
 * 🔴 Not Connected
 * 🟡 Connecting / Verifying
 * 🟢 Connected
 *
 * Tapping it navigates to the Connect tab.
 */
@Composable
fun GlobalConnectionIndicator(
    connectionState: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = GlobalIndicatorStatus.fromState(connectionState)

    val (dotColor, textLabel, borderColor) = when (status) {
        GlobalIndicatorStatus.CONNECTED -> Triple(
            GreenActiveTurnLight,
            "CONNECTED",
            GreenActiveTurn
        )
        GlobalIndicatorStatus.CONNECTING -> Triple(
            NeonAmber,
            "CONNECTING...",
            NeonAmber.copy(alpha = 0.8f)
        )
        GlobalIndicatorStatus.DISCONNECTED -> Triple(
            NeonCrimson,
            "NOT CONNECTED",
            NeonCrimson.copy(alpha = 0.6f)
        )
    }

    val animatedDotColor by animateColorAsState(targetValue = dotColor, label = "indicatorDotColor")

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCardElevated,
        border = BorderStroke(1.2.dp, borderColor),
        modifier = modifier
            .testTag("global_connection_indicator")
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = dotColor),
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(animatedDotColor)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = textLabel,
                color = TextWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.6.sp
            )
        }
    }
}
