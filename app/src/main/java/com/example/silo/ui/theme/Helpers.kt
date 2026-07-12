package com.example.silo.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Subtle tactile press feedback (scale down ~4% while held) for tappable
 * surfaces — grid tiles, cards, buttons. Keeps motion consistent app-wide.
 * Reads press state off [interactionSource], which must be the same instance
 * passed to the surface's `clickable(...)` so the two stay in sync.
 */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.96f): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) pressedScale else 1f,
        animationSpec = tween(120),
        label         = "pressScale"
    )
    return this.scale(scale)
}

/**
 * Convenience: wires up a shared [MutableInteractionSource] so a tappable
 * surface gets ripple-free click handling plus [pressScale] in one call.
 */
@Composable
fun Modifier.pressableScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .pressScale(interactionSource, pressedScale)
        .clickable(
            interactionSource = interactionSource,
            indication        = null,
            enabled           = enabled,
            onClick           = onClick
        )
}

/**
 * Fade + rise entrance used for grid tiles so a screen's items appear in a
 * quick staggered wave rather than all at once. [index] drives the delay.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int, staggerMs: Int = 45): Modifier {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(12f) }
    LaunchedEffect(Unit) {
        delay(index.toLong() * staggerMs)
        launch { alpha.animateTo(1f, tween(260)) }
        launch { offsetY.animateTo(0f, tween(320)) }
    }
    return this
        .alpha(alpha.value)
        .offset(y = offsetY.value.dp)
}

fun fileEmoji(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "jpg","jpeg","png","gif","webp","heic" -> "🖼"
    "mp4","mov","avi","mkv","webm"         -> "🎬"
    "mp3","wav","flac","aac","ogg"         -> "🎵"
    "pdf"                                  -> "📄"
    "doc","docx"                           -> "📝"
    "xls","xlsx"                           -> "📊"
    "zip","rar","tar","gz","7z"            -> "📦"
    "apk"                                  -> "📱"
    "txt","md"                             -> "📃"
    else                                   -> "📁"
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024L             -> "$bytes B"
    bytes < 1024L * 1024      -> "${"%.1f".format(bytes / 1024f)} KB"
    bytes < 1024L * 1024 * 1024 -> "${"%.1f".format(bytes / 1024f / 1024)} MB"
    else                      -> "${"%.2f".format(bytes / 1024f / 1024 / 1024)} GB"
}
