package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PurpleBorderSubtle
import com.example.ui.theme.PurpleGlowLight
import com.example.ui.theme.PurpleGlowPrimary
import com.example.ui.theme.PurpleOuterGlow
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextWhite

enum class AppTab(val title: String) {
    CONTROLLER("Controller"),
    CONNECTION("Connection")
}

/**
 * Bottom Navigation Bar:
 * Approximately 30% taller container height than standard (~76dp min height + safe area insets),
 * maintaining vertical centering, futuristic dark aesthetic, and purple glow indicators.
 * Tab order: Controller (left), Connection (right).
 */
@Composable
fun BottomNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF07080C),
        border = BorderStroke(width = 0.8.dp, color = Color(0xFF1E2433)),
        shadowElevation = 16.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(min = 76.dp)
                .padding(horizontal = 24.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Controller (Left)
                NavItem(
                    tab = AppTab.CONTROLLER,
                    isSelected = selectedTab == AppTab.CONTROLLER,
                    onClick = { onTabSelected(AppTab.CONTROLLER) }
                )

                // Tab 2: Connection (Right of Controller)
                NavItem(
                    tab = AppTab.CONNECTION,
                    isSelected = selectedTab == AppTab.CONNECTION,
                    onClick = { onTabSelected(AppTab.CONNECTION) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    tab: AppTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) TextWhite else TextDim,
        label = "navTextColor"
    )

    Column(
        modifier = Modifier
            .testTag("tab_${tab.name.lowercase()}")
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = PurpleGlowLight),
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing purple pill indicator for active tab
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PurpleGlowPrimary)
                    .shadow(8.dp, RoundedCornerShape(2.dp), spotColor = PurpleGlowLight)
            )
            Spacer(modifier = Modifier.height(6.dp))
        } else {
            Spacer(modifier = Modifier.height(9.dp))
        }

        Text(
            text = tab.title,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
