package com.example.silo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors

@Composable
fun KeyboardControlScreen(
    onBack: () -> Unit,
    onSendKey: (String) -> Unit,
    onSendMove: (Float, Float) -> Unit,
    onSendClick: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // We use a dummy text to capture backspaces and typing.
    // Ensure the cursor is at the end (index 1) so backspace deletes the space.
    var textValue by remember { mutableStateOf(TextFieldValue(" ", selection = TextRange(1))) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiloColors.BgDeep)
    ) {
        SubScreenHeader(title = "Keyboard Control", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                        detectTapGestures(onTap = { onSendClick("left") })
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onSendMove(dragAmount.x, dragAmount.y)
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

            Text(
                "Type on your keyboard to control the PC",
                color = SiloColors.TextSecondary
            )

            // Invisible text field to capture input
            BasicTextField(
                value = textValue,
                onValueChange = { newValue ->
                    val oldText = textValue.text
                    val newText = newValue.text

                    if (newText.length < oldText.length) {
                        // Backspace was pressed
                        onSendKey("BACKSPACE")
                    } else if (newText.length > oldText.length) {
                        // New character(s) typed
                        val addedStr = newText.substring(oldText.length)
                        for (char in addedStr) {
                            if (char == '\n') {
                                onSendKey("ENTER")
                            } else {
                                onSendKey(char.toString())
                            }
                        }
                    }

                    // Reset to " " with cursor at the end to keep capturing backspace
                    textValue = TextFieldValue(" ", selection = TextRange(1))
                },
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    keyboardType = KeyboardType.Ascii
                ),
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SiloColors.AccentViolet)
            ) {
                Text("Show Keyboard")
            }
        }
    }
}
