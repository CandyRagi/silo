package com.example.silo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silo.network.SiloUiState
import com.example.silo.network.TransferStatus
import com.example.silo.ui.components.TransferCard
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.silo.database.SiloDatabase
import com.example.silo.network.TransferDirection

@Composable
fun HistoryScreen(uiState: SiloUiState, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = SiloDatabase.getDatabase(context)
    val historyEntities by db.historyDao().getAllHistory().collectAsState(initial = emptyList())
    
    val completed = historyEntities.map {
        com.example.silo.network.TransferInfo(
            id = it.id.toString(),
            sessionId = "",
            fileId = "",
            fileName = it.fileName,
            totalBytes = it.totalBytes,
            bytesTransferred = it.totalBytes,
            progress = 100,
            direction = if (it.direction == "send") TransferDirection.SEND else TransferDirection.RECEIVE,
            status = TransferStatus.COMPLETE
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SiloColors.BgDeep)
    ) {
        SubScreenHeader(title = "History", onBack = onBack)

        if (completed.isEmpty()) {
            EmptySubScreen(
                message = "No transfers yet"
            )
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { SectionLabel("Completed (${completed.size})") }
                items(completed) { TransferCard(it) }
            }
        }
    }
}
