package com.example.silo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.model.UserProfileState
import com.example.silo.network.SiloUiState
import com.example.silo.ui.components.InfoRow
import com.example.silo.ui.components.SiloCard
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors
import com.example.silo.ui.theme.avatarDrawables
import com.example.silo.ui.theme.pressableScale

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
                            Image(
                                painter            = painterResource(avatarDrawables[profile.avatarIndex]),
                                contentDescription  = null,
                                contentScale        = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, SiloColors.BorderStrong, CircleShape)
                            )

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
    var avatarIndex by remember { mutableStateOf(profile.avatarIndex) }

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
                    profile.update(name.trim(), avatarIndex)
                    onDismiss()
                }
            ) {
                Text("Save", fontFamily = SamsungFontFamily, fontWeight = FontWeight.SemiBold, color = SiloColors.Accent)
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
            Image(
                painter            = painterResource(avatarDrawables[avatarIndex]),
                contentDescription  = null,
                contentScale        = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(2.dp, SiloColors.BorderStrong, CircleShape)
            )

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

            // Avatar Picker
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Profile Picture", fontFamily = SamsungFontFamily, fontSize = 13.sp, color = SiloColors.TextSecondary)

                // 4 columns x 2 rows
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val rows = avatarDrawables.chunked(4)
                    rows.forEachIndexed { rIndex, rowDrawables ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowDrawables.forEachIndexed { cIndex, drawableRes ->
                                val actualIndex = rIndex * 4 + cIndex
                                val isSelected = avatarIndex == actualIndex

                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .pressableScale { avatarIndex = actualIndex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter            = painterResource(drawableRes),
                                        contentDescription  = "Avatar ${actualIndex + 1}",
                                        contentScale        = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) SiloColors.Accent else Color.Transparent,
                                                shape = CircleShape
                                            )
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .align(Alignment.BottomEnd)
                                                .clip(CircleShape)
                                                .background(SiloColors.Accent)
                                                .border(2.dp, SiloColors.BgDeep, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint     = Color.White,
                                                modifier = Modifier.size(12.dp)
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
