package com.example.silo.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.silo.model.UserProfileState
import com.example.silo.network.SiloUiState
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors

@Composable
fun SiloTopHeader(uiState: SiloUiState, profile: UserProfileState, onSettings: () -> Unit) {
    val isConnected = uiState.connectedSession != null
    val displayName = profile.displayName.ifEmpty { uiState.deviceName.ifEmpty { "My Device" } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SiloColors.BgDeep)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: device name + status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text          = displayName,
                    fontFamily    = SamsungFontFamily,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 22.sp,
                    color         = SiloColors.TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(5.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                        initialValue  = 1f,
                        targetValue   = 0.35f,
                        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label         = "pulseVal"
                    )
                    Box(
                        Modifier
                            .size(6.dp)
                            .scale(if (!isConnected) pulse else 1f)
                            .background(if (isConnected) SiloColors.Green else SiloColors.TextMuted, CircleShape)
                    )
                    AnimatedContent(
                        targetState  = isConnected,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                        label        = "statusText"
                    ) { connected ->
                        Text(
                            text       = if (connected) "Connected" else "Not Connected",
                            fontFamily = SamsungFontFamily,
                            fontSize   = 12.sp,
                            color      = if (connected) SiloColors.Green else SiloColors.TextMuted,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.sp
                        )
                    }
                }
            }

            // Right: profile avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (profile.displayName.isNotEmpty()) profile.avatarColor else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (profile.displayName.isNotEmpty()) profile.avatarColor else SiloColors.TextMuted.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .clickable { onSettings() },
                contentAlignment = Alignment.Center
            ) {
                if (profile.displayName.isNotEmpty()) {
                    Text(
                        text = profile.displayName.take(1).uppercase(),
                        color = Color.White,
                        fontFamily = SamsungFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = "Settings",
                        tint     = SiloColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
