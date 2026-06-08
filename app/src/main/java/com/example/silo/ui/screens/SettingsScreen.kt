package com.example.silo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.network.SiloUiState
import com.example.silo.ui.components.InfoRow
import com.example.silo.ui.components.SiloCard
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors

@Composable
fun SettingsScreen(uiState: SiloUiState, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {

        // ── Header (plain background, no card) ─────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SiloColors.BgDeep)
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SiloColors.TextPrimary)
                }
                Text(
                    "Settings",
                    fontFamily = SamsungFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = SiloColors.TextPrimary,
                    letterSpacing = (-0.3).sp
                )
            }
        }

        // ── Content ────────────────────────────────────────
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Device identity card
            item {
                SiloCard {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            SiloColors.AccentPurple.copy(0.25f),
                                            SiloColors.AccentViolet.copy(0.1f)
                                        )
                                    ),
                                    CircleShape
                                )
                                .border(1.dp, SiloColors.AccentPurple.copy(0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PhoneAndroid,
                                contentDescription = null,
                                tint     = SiloColors.AccentPurple,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                uiState.deviceName.ifEmpty { "This Device" },
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp,
                                color      = SiloColors.TextPrimary
                            )
                            Text(
                                uiState.localIP.ifEmpty { "No network" },
                                fontSize   = 12.sp,
                                color      = SiloColors.TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Network section
            item {
                Text("Network", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SiloColors.TextSecondary)
            }
            item {
                SiloCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoRow("Discovery port", "41234")
                        Divider(color = SiloColors.BorderColor)
                        InfoRow("Transfer port",  "41236")
                        Divider(color = SiloColors.BorderColor)
                        InfoRow("Protocol",        "Silo UDP v1.0")
                        Divider(color = SiloColors.BorderColor)
                        InfoRow("Chunk size",      "60 KB")
                    }
                }
            }

            // About section
            item {
                Text("About", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SiloColors.TextSecondary)
            }
            item {
                SiloCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoRow("App",     "Silo")
                        Divider(color = SiloColors.BorderColor)
                        InfoRow("Version", "1.0.0")
                        Divider(color = SiloColors.BorderColor)
                        InfoRow("Build",   "Debug")
                    }
                }
            }
        }
    }
}
