package com.example.silo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.network.SiloUiState
import com.example.silo.ui.components.SiloCard
import com.example.silo.ui.components.StatusBadge
import com.example.silo.ui.components.TransferCard
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors
import com.example.silo.ui.theme.fileEmoji
import com.example.silo.ui.theme.formatBytes

@Composable
fun FileTransferScreen(uiState: SiloUiState, onPickFiles: () -> Unit, onBack: () -> Unit) {
    val allTransfers = (uiState.activeTransfers + uiState.completedTransfers)
        .filter { it.fileName.let { n -> !n.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp|heic|mp4|mov|avi|mkv|webm)$", RegexOption.IGNORE_CASE)) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiloColors.BgDeep)
    ) {
        // Header
        SubScreenHeader(title = "File Transfer", onBack = onBack)

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Send button
            item {
                Surface(
                    onClick  = onPickFiles,
                    enabled  = uiState.connectedSession != null,
                    color    = Color.Transparent,
                    shape    = RoundedCornerShape(18.dp),
                    border   = BorderStroke(
                        1.5.dp,
                        if (uiState.connectedSession != null)
                            Brush.linearGradient(listOf(SiloColors.Accent, SiloColors.AccentLight))
                        else
                            Brush.linearGradient(listOf(SiloColors.TextMuted.copy(0.3f), SiloColors.TextMuted.copy(0.3f)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            Modifier
                                .size(50.dp)
                                .background(
                                    Brush.linearGradient(listOf(SiloColors.Accent.copy(0.25f), SiloColors.AccentLight.copy(0.1f))),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Upload, null, tint = if (uiState.connectedSession != null) SiloColors.Accent else SiloColors.TextMuted, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Send Files", fontFamily = SamsungFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = if (uiState.connectedSession != null) SiloColors.TextPrimary else SiloColors.TextMuted)
                            Text(
                                if (uiState.connectedSession != null) "Tap to choose files from your device" else "Connect to a desktop first",
                                fontFamily = SamsungFontFamily, fontSize = 12.sp, color = SiloColors.TextMuted
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = SiloColors.TextMuted)
                    }
                }
            }

            // Queued
            if (uiState.pendingSendFiles.isNotEmpty()) {
                item { SectionLabel("Queued (${uiState.pendingSendFiles.size})") }
                items(uiState.pendingSendFiles) { f ->
                    SiloCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(fileEmoji(f.name), fontSize = 24.sp)
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontFamily = SamsungFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = SiloColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatBytes(f.size), fontFamily = SamsungFontFamily, fontSize = 11.sp, color = SiloColors.TextMuted)
                            }
                            StatusBadge("Queued", SiloColors.Accent)
                        }
                    }
                }
            }

            // Transfers
            if (allTransfers.isNotEmpty()) {
                item { SectionLabel("Transfers") }
                items(allTransfers) { TransferCard(it) }
            }
        }
    }
}
