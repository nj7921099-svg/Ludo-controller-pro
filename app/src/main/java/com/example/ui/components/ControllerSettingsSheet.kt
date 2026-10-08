package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controller.settings.ControllerSettings
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

/**
 * Controller Settings modal sheet accessible from the top-right of Connection tab.
 * Displays toggles for Haptic Feedback, Sound Effects, Animations, and Remember Last Mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerSettingsSheet(
    settings: ControllerSettings,
    onHapticChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onRememberLastModeChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PitchBlack,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .testTag("controller_settings_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title + Close Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E283C), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "SETTINGS",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Remote & UX Preferences",
                            color = TextDim,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Settings",
                        tint = TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Setting 1: Haptic Feedback
            SettingItemCard(
                icon = Icons.Default.Vibration,
                iconTint = Color(0xFF00E5FF),
                title = "Haptic Feedback",
                description = "Vibrate on player taps, dice rolls & sends",
                checked = settings.hapticEnabled,
                onCheckedChange = onHapticChange,
                testTag = "switch_haptic"
            )

            // Setting 2: Sound Effects
            SettingItemCard(
                icon = Icons.Default.VolumeUp,
                iconTint = Color(0xFFA855F7),
                title = "Sound Effects",
                description = "Audio cues during interaction and roll",
                checked = settings.soundEffectsEnabled,
                onCheckedChange = onSoundChange,
                testTag = "switch_sound"
            )

            // Setting 3: Animations
            SettingItemCard(
                icon = Icons.Default.AutoAwesome,
                iconTint = Color(0xFFFFD600),
                title = "Animations",
                description = "Smooth scale pulses and visual glow",
                checked = settings.animationsEnabled,
                onCheckedChange = onAnimationsChange,
                testTag = "switch_animations"
            )

            // Setting 4: Remember Last Mode
            SettingItemCard(
                icon = Icons.Default.History,
                iconTint = Color(0xFF00A651),
                title = "Remember Last Mode",
                description = "Restore Simple/Pro mode on app launch",
                checked = settings.rememberLastMode,
                onCheckedChange = onRememberLastModeChange,
                testTag = "switch_remember_mode"
            )

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SettingItemCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        color = TextDim,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF00E5FF),
                    uncheckedTrackColor = Color(0xFF161B28),
                    uncheckedThumbColor = TextMuted,
                    uncheckedBorderColor = Color(0xFF283248)
                )
            )
        }
    }
}
