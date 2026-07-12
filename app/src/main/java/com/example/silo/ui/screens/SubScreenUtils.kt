package com.example.silo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors

// ── Shared sub-screen header (back arrow + title) ─────────

@Composable
fun SubScreenHeader(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SiloColors.BgDeep)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SiloColors.TextPrimary)
            }
            Text(
                text          = title,
                fontFamily    = SamsungFontFamily,
                fontWeight    = FontWeight.Bold,
                fontSize      = 20.sp,
                color         = SiloColors.TextPrimary,
                letterSpacing = (-0.3).sp
            )
        }
        // Thin divider
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SiloColors.BorderColor)
        )
    }
}

// ── Section label ─────────────────────────────────────────

@Composable
fun SectionLabel(text: String) {
    Text(
        text       = text,
        fontFamily = SamsungFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 12.sp,
        color      = SiloColors.TextMuted,
        letterSpacing = 0.5.sp
    )
}

// ── Empty state for sub-screens ───────────────────────────

@Composable
fun EmptySubScreen(message: String) {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Text(
                message,
                fontFamily = SamsungFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 16.sp,
                color      = SiloColors.TextPrimary,
                textAlign  = TextAlign.Center
            )
        }
    }
}

// ── Generic placeholder screen for future features ────────

@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiloColors.BgDeep)
    ) {
        SubScreenHeader(title = title, onBack = onBack)

        Box(
            modifier         = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier            = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(SiloColors.Accent.copy(0.12f), SiloColors.AccentLight.copy(0.06f))
                            ),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🚧", fontSize = 36.sp)
                }
                Text(
                    text          = title,
                    fontFamily    = SamsungFontFamily,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 20.sp,
                    color         = SiloColors.TextPrimary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text          = "Coming soon",
                    fontFamily    = SamsungFontFamily,
                    fontSize      = 13.sp,
                    color         = SiloColors.TextMuted,
                    textAlign     = TextAlign.Center
                )
            }
        }
    }
}
