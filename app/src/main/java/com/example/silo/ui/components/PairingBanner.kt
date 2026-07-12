package com.example.silo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import com.example.silo.network.PairRequest
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors

/**
 * Full-screen, centered pairing-request dialog. Sits above every other layer
 * (header, tabs, bottom nav, sub-screens) so a request can never be missed
 * regardless of where the user currently is in the app.
 */
@Composable
fun PairingRequestOverlay(
    pendingRequest: PairRequest?,
    onAccept: (PairRequest) -> Unit,
    onDeny:   (PairRequest) -> Unit
) {
    AnimatedVisibility(
        visible = pendingRequest != null,
        enter   = fadeIn(tween(150)),
        exit    = fadeOut(tween(150))
    ) {
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.64f)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = pendingRequest != null,
                enter   = scaleIn(
                    initialScale  = 0.85f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f)
                ) + fadeIn(tween(180)),
                exit    = scaleOut(targetScale = 0.9f, animationSpec = tween(140)) + fadeOut(tween(140))
            ) {
                pendingRequest?.let { req ->
                    Surface(
                        modifier        = Modifier
                            .padding(horizontal = 32.dp)
                            .widthIn(max = 340.dp),
                        color           = SiloColors.BgSurface,
                        shape           = RoundedCornerShape(20.dp),
                        border          = BorderStroke(1.dp, SiloColors.BorderStrong),
                        shadowElevation = 24.dp
                    ) {
                        Column(
                            modifier             = Modifier.padding(24.dp),
                            horizontalAlignment  = Alignment.CenterHorizontally,
                            verticalArrangement  = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(SiloColors.Accent.copy(0.22f), SiloColors.AccentLight.copy(0.08f))
                                        ),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint     = SiloColors.Accent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(
                                "Connection Request",
                                fontFamily = SamsungFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp,
                                color      = SiloColors.TextPrimary,
                                textAlign  = TextAlign.Center
                            )

                            Text(
                                buildString {
                                    append(req.desktopName)
                                    append(" wants to connect to your phone.")
                                },
                                fontFamily = SamsungFontFamily,
                                fontSize   = 13.sp,
                                color      = SiloColors.TextSecondary,
                                textAlign  = TextAlign.Center,
                                lineHeight = 19.sp
                            )

                            Spacer(Modifier.height(18.dp))

                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick  = { onDeny(req) },
                                    modifier = Modifier.weight(1f),
                                    shape    = RoundedCornerShape(12.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = SiloColors.Red),
                                    border   = BorderStroke(1.dp, SiloColors.Red.copy(0.4f))
                                ) { Text("Reject", fontFamily = SamsungFontFamily, fontWeight = FontWeight.SemiBold) }

                                Button(
                                    onClick  = { onAccept(req) },
                                    modifier = Modifier.weight(1f),
                                    shape    = RoundedCornerShape(12.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = SiloColors.Accent)
                                ) { Text("Accept", fontFamily = SamsungFontFamily, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }
        }
    }
}
