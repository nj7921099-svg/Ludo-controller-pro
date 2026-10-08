package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controller.ProtocolLogEntry
import com.example.ui.theme.GreenActiveTurn
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.PurpleBorderBright
import com.example.ui.theme.PurpleBorderSubtle
import com.example.ui.theme.PurpleGlowLight
import com.example.ui.theme.PurpleGlowPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Requirement 12 & 22:
 * Collapsible Debug & Connection Information Panel matching the concept from the reference:
 * - Connection events & state changes
 * - Captured connection text (inbound from App 1)
 * - Sent connection text (outbound to App 1)
 * Collapsed by default to keep the main controller screen clean and production-ready.
 */
@Composable
fun CollapsibleDebugSection(
    logs: List<ProtocolLogEntry>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "CAPTURED", "SENT"
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "CAPTURED" -> logs.filter { it.direction == ProtocolLogEntry.Direction.INBOUND }
            "SENT" -> logs.filter { it.direction == ProtocolLogEntry.Direction.OUTBOUND }
            else -> logs
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, PurpleBorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Clickable to expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PurpleGlowPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = PurpleGlowLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "DEBUG & CONNECTION LOGS",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${logs.size} Captured / Sent Events",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceCardElevated,
                        border = BorderStroke(0.8.dp, PurpleBorderSubtle)
                    ) {
                        Text(
                            text = if (isExpanded) "COLLAPSE" else "EXPAND",
                            color = PurpleGlowLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Filter Pills & Clear button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("ALL", "CAPTURED", "SENT").forEach { filterTag ->
                                val isSelected = selectedFilter == filterTag
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PurpleGlowPrimary.copy(alpha = 0.25f) else SurfaceCardElevated,
                                    border = BorderStroke(1.dp, if (isSelected) PurpleBorderBright else PurpleBorderSubtle),
                                    modifier = Modifier.clickable { selectedFilter = filterTag }
                                ) {
                                    Text(
                                        text = filterTag,
                                        color = if (isSelected) PurpleGlowLight else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onClearLogs,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF07070B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No packet events recorded yet.",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredLogs, key = { it.id }) { log ->
                                val (dirColor, dirLabel, dirIcon) = when (log.direction) {
                                    ProtocolLogEntry.Direction.INBOUND -> Triple(GreenActiveTurn, "IN", Icons.Default.ArrowDownward)
                                    ProtocolLogEntry.Direction.OUTBOUND -> Triple(PurpleGlowLight, "OUT", Icons.Default.ArrowUpward)
                                    ProtocolLogEntry.Direction.SYSTEM -> Triple(NeonAmber, "SYS", Icons.Default.Info)
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF090A10),
                                    border = BorderStroke(0.6.dp, PurpleBorderSubtle)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = dirIcon,
                                                    contentDescription = null,
                                                    tint = dirColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "[$dirLabel] ${log.type}",
                                                    color = dirColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            Text(
                                                text = timeFormat.format(Date(log.timestamp)),
                                                color = TextDim,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Text(
                                            text = log.summary,
                                            color = TextWhite,
                                            fontSize = 11.sp
                                        )

                                        if (log.fullJson.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = log.fullJson,
                                                color = TextMuted,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
