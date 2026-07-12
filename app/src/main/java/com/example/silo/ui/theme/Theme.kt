
package com.example.silo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SiloDarkColorScheme = darkColorScheme(
    primary          = Accent,
    secondary        = AccentDim,
    tertiary         = AccentLight,
    background       = BgDeep,
    surface          = BgSurface,
    surfaceVariant   = BgRaised,
    onPrimary        = Color.White,
    onSecondary      = Color.White,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline          = Border,
    error            = RedError,
)

@Composable
fun SiloTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SiloDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}