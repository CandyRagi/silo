package com.example.silo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.silo.ui.theme.pressableScale
import com.example.silo.ui.theme.staggeredEntrance

// Sub-destinations within the Files tab — used by SiloApp for full-screen navigation
sealed class FilesDestination {
    object Grid          : FilesDestination()
    object FileTransfer  : FilesDestination()
    object ImageTransfer : FilesDestination()
    object History       : FilesDestination()
    object Placeholder4  : FilesDestination()
    object Placeholder6  : FilesDestination()
}

private data class GridItem(
    val label:       String,
    val icon:        ImageVector,
    val accent:      Color,
    val enabled:     Boolean,
    val onClick:     () -> Unit
)

// Just the 2×3 grid — navigation is handled by SiloApp
@Composable
fun FilesScreen(
    isConnected: Boolean,
    onNavigate: (FilesDestination) -> Unit,
    onPickFiles: (String) -> Unit,
    onCameraCapture: () -> Unit
) {
    val items = listOf(
        GridItem("File Transfer",  Icons.Outlined.FolderOpen,    SiloColors.Accent,      isConnected, { onPickFiles("*/*") }),
        GridItem("Image Transfer", Icons.Outlined.Image,         SiloColors.AccentLight, isConnected, { onPickFiles("image/*") }),
        GridItem("Quick Camera",   Icons.Outlined.CameraAlt,     SiloColors.Green,       isConnected, { onCameraCapture() }),
        GridItem("History",        Icons.Outlined.History,       SiloColors.Amber,       true,        { onNavigate(FilesDestination.History) }),
        GridItem("More",           Icons.Outlined.GridView,      SiloColors.TextMuted,   true,        { onNavigate(FilesDestination.Placeholder6) }),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiloColors.BgDeep)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.chunked(2).forEachIndexed { rowIdx, rowItems ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEachIndexed { colIdx, item ->
                    GridBox(
                        item     = item,
                        index    = rowIdx * 2 + colIdx,
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
private fun GridBox(item: GridItem, index: Int, modifier: Modifier, onClick: () -> Unit) {
    val alpha = if (item.enabled) 1f else 0.4f

    Box(
        modifier = modifier
            .staggeredEntrance(index)
            .clip(RoundedCornerShape(18.dp))
            .background(SiloColors.BgSurface)
            .pressableScale(enabled = item.enabled, onClick = onClick),
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
                        RoundedCornerShape(14.dp)
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
