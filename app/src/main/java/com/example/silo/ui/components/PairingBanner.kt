package com.example.silo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.network.PairRequest
import com.example.silo.ui.theme.SiloColors

@Composable
fun PairingBanner(
    req:      PairRequest,
    onAccept: () -> Unit,
    onDeny:   () -> Unit
) {
    Surface(
        color  = Color(0xFF1a1630),
        border = BorderStroke(1.dp, SiloColors.AccentPurple.copy(alpha = 0.5f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    Text("Connection Request", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = SiloColors.TextPrimary)
                    Text("\"${req.desktopName}\" wants to connect to your phone.", fontSize = 13.sp, color = SiloColors.TextSecondary)
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
                    onClick  = onAccept,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = SiloColors.AccentPurple)
                ) { Text("Accept", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
