package com.example.silo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.network.SiloUiState
import com.example.silo.ui.components.SiloCard
import com.example.silo.ui.components.StatusBadge
import com.example.silo.ui.components.TransferCard
import com.example.silo.ui.theme.SiloColors
import com.example.silo.ui.theme.fileEmoji
import com.example.silo.ui.theme.formatBytes

@Composable
fun FilesScreen(uiState: SiloUiState, onPickFiles: () -> Unit) {
    val allTransfers = uiState.activeTransfers + uiState.completedTransfers

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.connectedSession == null) {
            // ── Not connected empty state ───────────────────────
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Animated radar rings
                        Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                            val infiniteTransition = rememberInfiniteTransition(label = "radar")
                            (0..2).forEach { ring ->
                                val scale by infiniteTransition.animateFloat(
                                    initialValue  = 0.3f,
                                    targetValue   = 1.3f,
                                    animationSpec = infiniteRepeatable(
                                        tween(2200, delayMillis = ring * 700),
                                        RepeatMode.Restart
                                    ),
                                    label = "ring$ring"
                                )
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue  = 0.6f,
                                    targetValue   = 0f,
                                    animationSpec = infiniteRepeatable(
                                        tween(2200, delayMillis = ring * 700),
                                        RepeatMode.Restart
                                    ),
                                    label = "alpha$ring"
                                )
                                Box(
                                    Modifier
                                        .size(90.dp)
                                        .scale(scale)
                                        .alpha(alpha)
                                        .border(1.5.dp, SiloColors.AccentPurple.copy(alpha = alpha), CircleShape)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                SiloColors.AccentPurple.copy(0.2f),
                                                SiloColors.AccentViolet.copy(0.1f)
                                            )
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Wifi,
                                    contentDescription = null,
                                    tint     = SiloColors.AccentPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Text(
                            "Not Connected",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 17.sp,
                            color      = SiloColors.TextPrimary
                        )
                        Text(
                            "Open Silo on your PC and scan\nfor devices to get started",
                            fontSize   = 13.sp,
                            color      = SiloColors.TextSecondary,
                            textAlign  = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        } else {
            // ── Send files button ──────────────────────────────
            item {
                Surface(
                    onClick  = onPickFiles,
                    color    = Color.Transparent,
                    shape    = RoundedCornerShape(20.dp),
                    border   = BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(listOf(SiloColors.AccentPurple, SiloColors.AccentViolet))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            SiloColors.AccentPurple.copy(0.25f),
                                            SiloColors.AccentViolet.copy(0.1f)
                                        )
                                    ),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null, tint = SiloColors.AccentPurple, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Send Files", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = SiloColors.TextPrimary)
                            Text("Tap to choose files from your device", fontSize = 12.sp, color = SiloColors.TextSecondary)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SiloColors.TextMuted)
                    }
                }
            }

            // ── Queued files ───────────────────────────────────
            if (uiState.pendingSendFiles.isNotEmpty()) {
                item {
                    Text(
                        "Queued (${uiState.pendingSendFiles.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp,
                        color      = SiloColors.TextSecondary
                    )
                }
                items(uiState.pendingSendFiles) { fileInfo ->
                    SiloCard {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(fileEmoji(fileInfo.name), fontSize = 24.sp)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    fileInfo.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize   = 13.sp,
                                    color      = SiloColors.TextPrimary,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis
                                )
                                Text(formatBytes(fileInfo.size), fontSize = 11.sp, color = SiloColors.TextSecondary)
                            }
                            StatusBadge("Queued", SiloColors.AccentPurple)
                        }
                    }
                }
            }

            // ── Transfer history ───────────────────────────────
            if (allTransfers.isNotEmpty()) {
                item {
                    Text(
                        "Transfers",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp,
                        color      = SiloColors.TextSecondary
                    )
                }
                items(allTransfers) { transfer ->
                    TransferCard(transfer)
                }
            }
        }
    }
}
