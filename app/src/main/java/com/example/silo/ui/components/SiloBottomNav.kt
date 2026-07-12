package com.example.silo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(48.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / tabs.size
            val indicatorOffset by animateDpAsState(
                targetValue   = tabWidth * selectedTab,
                animationSpec = tween(220),
                label         = "navIndicator"
            )

            // Sliding active-tab pill — mirrors desktop's .nav-item.active background
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SiloColors.BgRaised)
            )

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
                        Text(
                            text          = label,
                            fontSize      = 14.sp,
                            fontFamily    = SamsungFontFamily,
                            fontWeight    = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color         = textColor,
                            letterSpacing = 0.sp
                        )
                    }
                }
            }
        }
    }
}
