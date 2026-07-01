package com.example.silo.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.network.SiloUiState
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors

sealed class CommandsDestination {
    object Grid            : CommandsDestination()
    object MouseControl    : CommandsDestination()
    object KeyboardControl : CommandsDestination()
    object CameraStream    : CommandsDestination()
}

private data class CommandItem(
    val label:         String,
    val icon:          ImageVector,
    val accent:        Color,
    val enabled:       Boolean,
    val isHighlighted: Boolean = false,
    val onClick:       () -> Unit
)

@Composable
fun CommandsScreen(
    uiState: SiloUiState,
    onNavigate: (CommandsDestination) -> Unit,
    onStartShare: (Int, Intent) -> Unit,
    onStopShare: () -> Unit
) {
    val context = LocalContext.current
    val isConnected = uiState.connectedSession != null
    val isAllowed = isConnected && uiState.desktopAllowsControl

    val projectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            onStartShare(result.resultCode, result.data!!)
        }
    }

    val isSharing = uiState.isScreenSharing

    val items = listOf(
        CommandItem("Mouse Control",   Icons.Outlined.Mouse,             SiloColors.AccentPurple,  isAllowed, false, { onNavigate(CommandsDestination.MouseControl) }),
        CommandItem("Keyboard Control",Icons.Outlined.Keyboard,          SiloColors.AccentViolet,  isAllowed, false, { onNavigate(CommandsDestination.KeyboardControl) }),
        CommandItem("Camera Stream",   Icons.Outlined.Videocam,          Color(0xFF3B82F6),        isAllowed, false, { onNavigate(CommandsDestination.CameraStream) }),
        CommandItem(
            label = if (isSharing) "Sharing..." else "Screen Share",
            icon = Icons.Outlined.Cast,
            accent = SiloColors.AccentPurple,
            enabled = isAllowed,
            isHighlighted = isSharing,
            onClick = {
                if (isSharing) {
                    onStopShare()
                } else {
                    launcher.launch(projectionManager.createScreenCaptureIntent())
                }
            }
        ),
        CommandItem("Custom Command",  Icons.Outlined.Code,              Color(0xFF10B981),        isAllowed, false, {}),
        CommandItem("More",            Icons.Outlined.GridView,          SiloColors.TextMuted,     true,      false, {}),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiloColors.BgDeep)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    CommandGridBox(
                        item     = item,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick  = item.onClick
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CommandGridBox(item: CommandItem, modifier: Modifier, onClick: () -> Unit) {
    val alpha = if (item.enabled) 1f else 0.4f
    val backgroundBrush = if (item.isHighlighted) {
        Brush.verticalGradient(
            colors = listOf(
                item.accent.copy(alpha = 0.25f),
                item.accent.copy(alpha = 0.12f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                SiloColors.BgSurface,
                SiloColors.BgSurface
            )
        )
    }

    val borderModifier = if (item.isHighlighted) {
        Modifier.border(
            width = 1.5.dp,
            color = item.accent,
            shape = RoundedCornerShape(20.dp)
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundBrush)
            .then(borderModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                enabled           = item.enabled,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(item.accent.copy(alpha = 0.18f * alpha), item.accent.copy(alpha = 0.06f * alpha))
                        ),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = item.icon,
                    contentDescription = item.label,
                    tint               = item.accent.copy(alpha = alpha),
                    modifier           = Modifier.size(26.dp)
                )
            }
            Text(
                text          = item.label,
                fontFamily    = SamsungFontFamily,
                fontWeight    = FontWeight.Medium,
                fontSize      = 13.sp,
                color         = SiloColors.TextPrimary.copy(alpha = alpha),
                letterSpacing = 0.sp
            )
        }
    }
}
