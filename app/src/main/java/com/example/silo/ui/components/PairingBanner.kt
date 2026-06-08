package com.example.silo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.example.silo.network.PairRequest
import com.example.silo.ui.theme.SiloColors

@Composable
fun PairingBanner(
    req:      PairRequest,
    onVerify: (String) -> Boolean,
    onDeny:   () -> Unit
) {
    var digits   by remember { mutableStateOf(List(6) { "" }) }
    var errorMsg by remember { mutableStateOf("") }
    val focusRequesters   = remember { List(6) { FocusRequester() } }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequesters[0].requestFocus() }

    Surface(
        color  = Color(0xFF1a1630),
        border = BorderStroke(1.dp, SiloColors.AccentPurple.copy(alpha = 0.5f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint     = SiloColors.AccentPurple,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text("Pairing Request", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SiloColors.TextPrimary)
                    Text("\"${req.desktopName}\" wants to connect", fontSize = 12.sp, color = SiloColors.TextSecondary)
                }
            }

            // PIN entry card
            Surface(
                color  = Color(0xFF0e0e1a),
                shape  = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SiloColors.BorderColor)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Enter the code shown on your PC", fontSize = 11.sp, color = SiloColors.TextSecondary)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        digits.forEachIndexed { idx, value ->
                            if (idx == 3) {
                                Text("—", color = SiloColors.TextMuted, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 2.dp))
                            }
                            Surface(
                                color  = if (errorMsg.isNotEmpty()) Color(0xFF2a0e0e) else Color(0xFF1a1a28),
                                shape  = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp,
                                    when {
                                        errorMsg.isNotEmpty() -> SiloColors.Red.copy(0.6f)
                                        value.isNotEmpty()    -> SiloColors.AccentPurple.copy(0.7f)
                                        else                  -> SiloColors.AccentPurple.copy(0.3f)
                                    }
                                )
                            ) {
                                BasicTextField(
                                    value       = value,
                                    onValueChange = { new ->
                                        val clean = new.filter { it.isDigit() }.take(1)
                                        digits    = digits.toMutableList().also { it[idx] = clean }
                                        errorMsg  = ""
                                        if (clean.isNotEmpty() && idx < 5) focusRequesters[idx + 1].requestFocus()
                                    },
                                    modifier    = Modifier
                                        .width(44.dp)
                                        .height(54.dp)
                                        .focusRequester(focusRequesters[idx]),
                                    textStyle   = TextStyle(
                                        fontSize   = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = SiloColors.TextPrimary,
                                        textAlign  = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction    = if (idx == 5) ImeAction.Done else ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { if (idx < 5) focusRequesters[idx + 1].requestFocus() },
                                        onDone = { keyboardController?.hide() }
                                    ),
                                    singleLine  = true,
                                    decorationBox = { inner ->
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() }
                                    }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = errorMsg.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        Text(errorMsg, fontSize = 11.sp, color = SiloColors.Red, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick  = onDeny,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = SiloColors.Red),
                    border   = BorderStroke(1.dp, SiloColors.Red.copy(0.4f))
                ) { Text("Reject", fontWeight = FontWeight.SemiBold) }

                Button(
                    onClick  = {
                        val entered = digits.joinToString("")
                        if (entered.length < 6) { errorMsg = "Enter all 6 digits"; return@Button }
                        val ok = onVerify(entered)
                        if (!ok) {
                            errorMsg = "Wrong code — check your PC"
                            digits   = List(6) { "" }
                            focusRequesters[0].requestFocus()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = SiloColors.AccentPurple)
                ) { Text("Confirm", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
