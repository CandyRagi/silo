package com.example.silo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.network.SiloUiState
import com.example.silo.ui.components.TransferCard
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors

@Composable
fun ImageTransferScreen(uiState: SiloUiState, onPickFiles: () -> Unit, onBack: () -> Unit) {
    val imageTransfers = (uiState.activeTransfers + uiState.completedTransfers)
        .filter { it.fileName.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp|heic)$", RegexOption.IGNORE_CASE)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiloColors.BgDeep)
    ) {
        SubScreenHeader(title = "Image Transfer", onBack = onBack)

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Send images button
            item {
                Surface(
                    onClick  = onPickFiles,
                    enabled  = uiState.connectedSession != null,
                    color    = Color.Transparent,
                    shape    = RoundedCornerShape(20.dp),
                    border   = BorderStroke(
                        1.5.dp,
                        if (uiState.connectedSession != null)
                            Brush.linearGradient(listOf(SiloColors.AccentViolet, Color(0xFF818CF8)))
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
                                    Brush.linearGradient(listOf(SiloColors.AccentViolet.copy(0.25f), Color(0xFF818CF8).copy(0.1f))),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Image, null, tint = if (uiState.connectedSession != null) SiloColors.AccentViolet else SiloColors.TextMuted, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Send Images", fontFamily = SamsungFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = if (uiState.connectedSession != null) SiloColors.TextPrimary else SiloColors.TextMuted)
                            Text(
                                if (uiState.connectedSession != null) "Tap to choose images from your gallery" else "Connect to a desktop first",
                                fontFamily = SamsungFontFamily, fontSize = 12.sp, color = SiloColors.TextMuted
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = SiloColors.TextMuted)
                    }
                }
            }

            if (imageTransfers.isNotEmpty()) {
                item { SectionLabel("Image Transfers") }
                items(imageTransfers) { TransferCard(it) }
            }
        }
    }
}
