package com.example.silo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.model.UserProfileState
import com.example.silo.model.avatarPalette
import com.example.silo.network.SiloUiState
import com.example.silo.ui.components.InfoRow
import com.example.silo.ui.components.SiloCard
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors

@Composable
fun SettingsScreen(uiState: SiloUiState, profile: UserProfileState, onBack: () -> Unit) {
    var showEditProfile by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    val displayName = profile.displayName.ifEmpty { uiState.deviceName.ifEmpty { "This Device" } }

                    SiloCard {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        if (profile.displayName.isNotEmpty()) SolidColor(profile.avatarColor) else Brush.linearGradient(
                                            listOf(SiloColors.AccentPurple.copy(0.25f), SiloColors.AccentViolet.copy(0.1f))
                                        ),
                                        CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        if (profile.displayName.isNotEmpty()) profile.avatarColor else SiloColors.AccentPurple.copy(0.4f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (profile.displayName.isNotEmpty()) {
                                    Text(
                                        text = profile.displayName.take(1).uppercase(),
                                        color = Color.White,
                                        fontFamily = SamsungFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.PhoneAndroid,
                                        contentDescription = null,
                                        tint     = SiloColors.AccentPurple,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    displayName,
                                    fontFamily = SamsungFontFamily,
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

                            // Edit button
                            IconButton(onClick = { showEditProfile = true }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit Profile", tint = SiloColors.TextMuted)
                            }
                        }
                    }
                }

                // Audio section
                item {
                    Text("Audio", fontFamily = SamsungFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SiloColors.TextSecondary)
                }
                item {
                    SiloCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Allow PC audio streaming", fontSize = 14.sp, color = SiloColors.TextPrimary)
                            Switch(
                                checked = profile.allowAudioStreaming,
                                onCheckedChange = { profile.setAudioStreaming(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = SiloColors.AccentPurple)
                            )
                        }
                    }
                }

                // Network section
                item {
                    Text("Network", fontFamily = SamsungFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SiloColors.TextSecondary)
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
                    Text("About", fontFamily = SamsungFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SiloColors.TextSecondary)
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

        // ── Edit Profile Overlay ──────────────────────────────
        AnimatedVisibility(
            visible = showEditProfile,
            enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
        ) {
            EditProfileOverlay(
                profile = profile,
                onDismiss = { showEditProfile = false }
            )
        }
    }
}

@Composable
fun EditProfileOverlay(profile: UserProfileState, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(profile.displayName) }
    var colorIndex by remember { mutableStateOf(profile.avatarColorIndex) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiloColors.BgDeep)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Cancel", tint = SiloColors.TextPrimary)
            }
            Text(
                "Edit Profile",
                fontFamily = SamsungFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp,
                color      = SiloColors.TextPrimary,
                modifier   = Modifier.weight(1f),
                letterSpacing = (-0.3).sp
            )
            TextButton(
                onClick = {
                    profile.update(name.trim(), colorIndex)
                    onDismiss()
                }
            ) {
                Text("Save", fontFamily = SamsungFontFamily, fontWeight = FontWeight.SemiBold, color = SiloColors.AccentPurple)
            }
        }

        Divider(color = SiloColors.BorderColor)

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Preview avatar
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(avatarPalette[colorIndex], CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val initial = name.trim().take(1).uppercase()
                if (initial.isNotEmpty()) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontFamily = SamsungFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp
                    )
                } else {
                    Icon(
                        Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        tint     = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Name Input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Display Name", fontFamily = SamsungFontFamily, fontSize = 13.sp, color = SiloColors.TextSecondary)
                
                Surface(
                    color = SiloColors.BgSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SiloColors.BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = TextStyle(
                            color = SiloColors.TextPrimary,
                            fontSize = 16.sp,
                            fontFamily = SamsungFontFamily
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        decorationBox = { innerTextField ->
                            if (name.isEmpty()) {
                                Text("Enter your name", color = SiloColors.TextMuted, fontSize = 16.sp, fontFamily = SamsungFontFamily)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // Color Picker
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Profile Color", fontFamily = SamsungFontFamily, fontSize = 13.sp, color = SiloColors.TextSecondary)
                
                // 4 columns x 2 rows
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val rows = avatarPalette.chunked(4)
                    rows.forEachIndexed { rIndex, rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowColors.forEachIndexed { cIndex, color ->
                                val actualIndex = rIndex * 4 + cIndex
                                val isSelected = colorIndex == actualIndex
                                
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) SiloColors.TextPrimary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { colorIndex = actualIndex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White)
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
