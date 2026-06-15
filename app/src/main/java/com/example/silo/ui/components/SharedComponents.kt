package com.example.silo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.network.TransferDirection
import com.example.silo.network.TransferInfo
import com.example.silo.network.TransferStatus
import com.example.silo.ui.theme.SiloColors
import com.example.silo.ui.theme.formatBytes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile

// ── SiloCard ──────────────────────────────────────────────

@Composable
fun SiloCard(
    borderColor: androidx.compose.ui.graphics.Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color           = SiloColors.BgSurface,
        shape           = RoundedCornerShape(16.dp),
        border          = BorderStroke(1.dp, borderColor ?: SiloColors.BorderColor),
        tonalElevation  = 0.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

// ── StatusBadge ───────────────────────────────────────────

@Composable
fun StatusBadge(label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text       = label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            color      = color,
            letterSpacing = 0.5.sp
        )
    }
}

// ── InfoRow ───────────────────────────────────────────────

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = SiloColors.TextSecondary)
        Text(
            value,
            fontSize   = 12.sp,
            color      = SiloColors.TextPrimary,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

// ── TransferCard ──────────────────────────────────────────

@Composable
fun TransferCard(transfer: TransferInfo) {
    SiloCard(borderColor = when (transfer.status) {
        TransferStatus.COMPLETE -> SiloColors.Green.copy(alpha = 0.3f)
        TransferStatus.ERROR    -> SiloColors.Red.copy(alpha = 0.3f)
        else                    -> null
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment     = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.InsertDriveFile, contentDescription = null, tint = SiloColors.TextSecondary, modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        transfer.fileName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp,
                        color      = SiloColors.TextPrimary,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        "${formatBytes(transfer.totalBytes)} · ${if (transfer.direction == TransferDirection.SEND) "Sending" else "Receiving"}",
                        fontSize = 11.sp,
                        color    = SiloColors.TextSecondary
                    )
                }
                val badgeColor = when (transfer.status) {
                    TransferStatus.COMPLETE -> SiloColors.Green
                    TransferStatus.ERROR    -> SiloColors.Red
                    else                    -> SiloColors.AccentPurple
                }
                StatusBadge(
                    label = when (transfer.status) {
                        TransferStatus.COMPLETE    -> "Done"
                        TransferStatus.ERROR       -> "Error"
                        TransferStatus.IN_PROGRESS -> if (transfer.direction == TransferDirection.SEND) "Sending" else "Receiving"
                        else                       -> "Pending"
                    },
                    color = badgeColor
                )
            }

            if (transfer.status == TransferStatus.IN_PROGRESS) {
                val animProgress by animateFloatAsState(
                    targetValue   = transfer.progress / 100f,
                    animationSpec = tween(300),
                    label         = "progress"
                )
                LinearProgressIndicator(
                    progress   = { animProgress },
                    modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color      = SiloColors.AccentPurple,
                    trackColor = SiloColors.BgRaised
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${transfer.progress}%", fontSize = 11.sp, color = SiloColors.TextSecondary)
                    Text(
                        "${formatBytes(transfer.bytesTransferred)} / ${formatBytes(transfer.totalBytes)}",
                        fontSize = 11.sp,
                        color    = SiloColors.TextSecondary
                    )
                }
            }
        }
    }
}
