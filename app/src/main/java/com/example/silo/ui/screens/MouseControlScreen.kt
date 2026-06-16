package com.example.silo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors

@Composable
fun MouseControlScreen(
    onBack: () -> Unit,
    onMouseMove: (Float, Float) -> Unit,
    onMouseClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiloColors.BgDeep)
    ) {
        SubScreenHeader(title = "Mouse Control", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trackpad Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SiloColors.BgSurface)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onMouseMove(dragAmount.x, dragAmount.y)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Trackpad",
                    color = SiloColors.TextMuted,
                    fontSize = 20.sp,
                    fontFamily = SamsungFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }

            // Buttons Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onMouseClick("left") },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SiloColors.AccentPurple)
                ) {
                    Text(
                        "Left Click",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Button(
                    onClick = { onMouseClick("right") },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SiloColors.BgSurface)
                ) {
                    Text(
                        "Right Click",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SiloColors.TextPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
