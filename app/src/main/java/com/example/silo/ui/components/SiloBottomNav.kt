package com.example.silo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.silo.ui.theme.SiloColors
import com.example.silo.ui.theme.SamsungFontFamily

@Composable
fun SiloBottomNav(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Files", "Commands")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SiloColors.BgDeep)
            .navigationBarsPadding()
            .height(56.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { idx, label ->
                val selected = selectedTab == idx

                val textColor by animateColorAsState(
                    targetValue   = if (selected) SiloColors.TextPrimary else SiloColors.TextMuted,
                    animationSpec = tween(200),
                    label         = "tabColor$idx"
                )
                val underlineColor by animateColorAsState(
                    targetValue   = if (selected) SiloColors.TextPrimary else Color.Transparent,
                    animationSpec = tween(200),
                    label         = "underline$idx"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { onTabSelected(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    // Column keeps the underline tightly below the text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text          = label,
                            fontSize      = 14.sp,
                            fontFamily    = SamsungFontFamily,
                            fontWeight    = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color         = textColor,
                            letterSpacing = 0.sp
                        )
                        // Underline: same width as the text, 2px tall, 3dp gap below text
                        Spacer(Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .wrapContentWidth()     // match text width
                                .height(2.dp)
                                .background(underlineColor)
                                // Ensure minimum visible width even for short labels
                                .defaultMinSize(minWidth = 20.dp)
                        )
                    }
                }
            }
        }
    }
}
