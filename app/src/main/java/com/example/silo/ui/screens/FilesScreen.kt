package com.example.silo.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors

// Sub-destinations within the Files tab — used by SiloApp for full-screen navigation
sealed class FilesDestination {
    object Grid          : FilesDestination()
    object FileTransfer  : FilesDestination()
    object ImageTransfer : FilesDestination()
    object History       : FilesDestination()
    object Placeholder4  : FilesDestination()
    object Placeholder5  : FilesDestination()
    object Placeholder6  : FilesDestination()
}

private data class GridItem(
    val label:       String,
    val icon:        ImageVector,
    val accent:      Color,
    val destination: FilesDestination
)

// Just the 2×3 grid — navigation is handled by SiloApp
@Composable
fun FilesScreen(onNavigate: (FilesDestination) -> Unit) {
    val items = listOf(
        GridItem("File Transfer",  Icons.Outlined.FolderOpen,   SiloColors.AccentPurple,  FilesDestination.FileTransfer),
        GridItem("Image Transfer", Icons.Outlined.Image,         SiloColors.AccentViolet,  FilesDestination.ImageTransfer),
        GridItem("History",        Icons.Outlined.History,       Color(0xFF3B82F6),        FilesDestination.History),
        GridItem("Clipboard",      Icons.Outlined.ContentPaste, Color(0xFF10B981),        FilesDestination.Placeholder4),
        GridItem("Audio",          Icons.Outlined.MusicNote,    Color(0xFFF59E0B),        FilesDestination.Placeholder5),
        GridItem("More",           Icons.Outlined.GridView,     SiloColors.TextMuted,     FilesDestination.Placeholder6),
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
                    GridBox(
                        item     = item,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick  = { onNavigate(item.destination) }
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GridBox(item: GridItem, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SiloColors.BgSurface)
            .border(1.dp, SiloColors.BorderColor, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
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
                            listOf(item.accent.copy(alpha = 0.18f), item.accent.copy(alpha = 0.06f))
                        ),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = item.icon,
                    contentDescription = item.label,
                    tint               = item.accent,
                    modifier           = Modifier.size(26.dp)
                )
            }
            Text(
                text          = item.label,
                fontFamily    = SamsungFontFamily,
                fontWeight    = FontWeight.Medium,
                fontSize      = 13.sp,
                color         = SiloColors.TextPrimary,
                letterSpacing = 0.sp
            )
        }
    }
}
