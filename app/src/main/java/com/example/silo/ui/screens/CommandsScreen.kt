package com.example.silo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.network.SiloUiState
import com.example.silo.ui.components.SiloCard
import com.example.silo.ui.theme.SiloColors

@Composable
fun CommandsScreen(uiState: SiloUiState) {
    val isConnected = uiState.connectedSession != null

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isConnected) {
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
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(SiloColors.BgSurface, CircleShape)
                                .border(1.dp, SiloColors.BorderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Terminal,
                                contentDescription = null,
                                tint     = SiloColors.TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            "Not Connected",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 17.sp,
                            color      = SiloColors.TextPrimary
                        )
                        Text(
                            "Connect to a desktop to use commands",
                            fontSize  = 13.sp,
                            color     = SiloColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // ── Quick commands ─────────────────────────────────
            item {
                Text(
                    "Quick Commands",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = SiloColors.TextSecondary
                )
            }

            val commands = listOf(
                Triple("Lock Screen",      Icons.Outlined.Lock,               SiloColors.AccentPurple),
                Triple("Mute / Unmute",    Icons.Outlined.VolumeOff,          SiloColors.AccentViolet),
                Triple("Sleep Display",    Icons.Outlined.DarkMode,           Color(0xFF3B82F6)),
                Triple("Shutdown",         Icons.Outlined.PowerSettingsNew,   SiloColors.Red),
            )

            items(commands) { (label, icon, color) ->
                Surface(
                    onClick = { /* TODO: wire up commands */ },
                    color   = SiloColors.BgSurface,
                    shape   = RoundedCornerShape(16.dp),
                    border  = androidx.compose.foundation.BorderStroke(1.dp, SiloColors.BorderColor)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        }
                        Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = SiloColors.TextPrimary, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SiloColors.TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            // ── Coming soon ────────────────────────────────────
            item {
                Text(
                    "Coming Soon",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = SiloColors.TextSecondary
                )
            }
            item {
                SiloCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            "Remote clipboard sync",
                            "Notification mirror",
                            "Hotkey triggers"
                        ).forEach { feature ->
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .background(SiloColors.AccentPurple.copy(alpha = 0.5f), CircleShape)
                                )
                                Text(feature, fontSize = 13.sp, color = SiloColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
