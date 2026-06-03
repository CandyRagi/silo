package com.example.silo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.silo.network.*
import com.example.silo.ui.theme.SiloTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var siloService: SiloService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        siloService = SiloService(applicationContext)

        setContent {
            SiloTheme {
                SiloApp(siloService = siloService)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        siloService.stop()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiloApp(siloService: SiloService) {
    val context = LocalContext.current
    val uiState by siloService.uiState.collectAsStateWithLifecycle()

    // Storage permission launcher — only for file access, NOT needed for networking
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* storage grants handled; service already started */ }

    // File picker
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri -> siloService.sendFile(uri) }
    }

    LaunchedEffect(Unit) {
        // INTERNET / WIFI_STATE / NETWORK_STATE are normal permissions — granted at install,
        // never need a runtime request. Start the service unconditionally right away.
        siloService.start()

        // Request storage permissions separately (only needed when user picks files)
        val storagePerms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (storagePerms.isNotEmpty()) {
            permLauncher.launch(storagePerms.toTypedArray())
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SiloColors.BgDeep
    ) {
        Column(Modifier.fillMaxSize()) {
            // Top App Bar
            SiloTopBar(uiState = uiState)

            // Pairing request banner
            AnimatedVisibility(visible = uiState.pendingPairRequest != null) {
                uiState.pendingPairRequest?.let { req ->
                    PairingBanner(
                        req = req,
                        onAccept = { siloService.acceptPairing(req) },
                        onDeny   = { siloService.denyPairing(req) }
                    )
                }
            }

            // Main tabs
            var selectedTab by remember { mutableIntStateOf(0) }
            val tabs = listOf("Connection", "Send Files", "Transfers")

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = SiloColors.BgBase,
                contentColor     = SiloColors.AccentPurple,
                indicator = { positions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(positions[selectedTab])
                            .height(2.dp)
                            .background(
                                brush = Brush.horizontalGradient(listOf(SiloColors.AccentPurple, SiloColors.AccentViolet)),
                                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                    )
                }
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick  = { selectedTab = idx },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == idx) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (selectedTab == idx) SiloColors.TextPrimary else SiloColors.TextSecondary
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> ConnectionTab(uiState = uiState)
                1 -> SendFilesTab(uiState = uiState, onPickFiles = { filePicker.launch("*/*") })
                2 -> TransfersTab(uiState = uiState)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
// TOP BAR
// ══════════════════════════════════════════════════════════

@Composable
fun SiloTopBar(uiState: SiloUiState) {
    Surface(
        color  = SiloColors.BgBase,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated logo icon
            val infiniteTransition = rememberInfiniteTransition(label = "logo")
            val hue by infiniteTransition.animateFloat(
                initialValue = 240f,
                targetValue  = 300f,
                animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
                label = "hue"
            )

            Box(
                Modifier
                    .size(36.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(SiloColors.AccentPurple, SiloColors.AccentViolet)),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Layers, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text("Silo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SiloColors.TextPrimary, letterSpacing = (-0.5).sp)
                Text(
                    text = if (uiState.isListening) "Listening for connections" else "Starting…",
                    fontSize = 11.sp,
                    color = SiloColors.TextSecondary
                )
            }

            Spacer(Modifier.weight(1f))

            // Status indicator
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val dotColor = when {
                    uiState.connectedSession != null -> SiloColors.Green
                    uiState.isListening -> SiloColors.AccentPurple
                    else -> SiloColors.TextMuted
                }
                val dotScale by rememberInfiniteTransition(label = "dot").animateFloat(
                    initialValue = 1f, targetValue = 0.6f,
                    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                    label = "scale"
                )
                Box(
                    Modifier
                        .size(8.dp)
                        .scale(if (uiState.connectedSession == null && uiState.isListening) dotScale else 1f)
                        .background(dotColor, CircleShape)
                )
                Text(
                    text = when {
                        uiState.connectedSession != null -> "Connected"
                        uiState.isListening -> "Scanning"
                        else -> "Offline"
                    },
                    fontSize = 12.sp,
                    color = SiloColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
// PAIRING BANNER
// ══════════════════════════════════════════════════════════

@Composable
fun PairingBanner(req: PairRequest, onAccept: () -> Unit, onDeny: () -> Unit) {
    Surface(
        color = Color(0xFF1a1630),
        border = BorderStroke(1.dp, SiloColors.AccentPurple.copy(alpha = 0.5f))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = SiloColors.AccentPurple, modifier = Modifier.size(20.dp))
                Column {
                    Text("Pairing Request", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SiloColors.TextPrimary)
                    Text("\"${req.desktopName}\" wants to connect", fontSize = 12.sp, color = SiloColors.TextSecondary)
                }
            }

            // PIN display
            Surface(
                color = Color(0xFF0e0e1a),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SiloColors.BorderColor)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Enter this PIN on the desktop", fontSize = 11.sp, color = SiloColors.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        req.pin.forEachIndexed { idx, digit ->
                            if (idx == 3) Text("—", color = SiloColors.TextMuted, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 2.dp))
                            Surface(
                                color  = Color(0xFF1a1a28),
                                shape  = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SiloColors.AccentPurple.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = digit.toString(),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    fontSize = 24.sp, fontWeight = FontWeight.Bold,
                                    color = SiloColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDeny,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SiloColors.Red),
                    border = BorderStroke(1.dp, SiloColors.Red.copy(alpha = 0.4f))
                ) {
                    Text("Reject", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SiloColors.AccentPurple)
                ) {
                    Text("Accept", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
// CONNECTION TAB
// ══════════════════════════════════════════════════════════

@Composable
fun ConnectionTab(uiState: SiloUiState) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Device info card
        item {
            SiloCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(
                                Brush.linearGradient(listOf(SiloColors.AccentPurple.copy(alpha = 0.3f), SiloColors.AccentViolet.copy(alpha = 0.15f))),
                                RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, SiloColors.AccentPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = SiloColors.AccentPurple, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(uiState.deviceName.ifEmpty { "This Device" }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = SiloColors.TextPrimary)
                        Text(uiState.localIP.ifEmpty { "No network" }, fontSize = 12.sp, color = SiloColors.TextMuted, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }

        // Connected session
        if (uiState.connectedSession != null) {
            item {
                SiloCard(borderColor = SiloColors.Green.copy(alpha = 0.4f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(10.dp).background(SiloColors.Green, CircleShape))
                        Column(Modifier.weight(1f)) {
                            Text("Connected to Desktop", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SiloColors.TextPrimary)
                            Text(uiState.connectedSession!!, fontSize = 11.sp, color = SiloColors.TextSecondary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SiloColors.Green)
                    }
                }
            }
        } else {
            item {
                SiloCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        // Animated radar
                        Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                            val infiniteTransition = rememberInfiniteTransition(label = "radar")
                            (0..2).forEach { ring ->
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 0.3f, targetValue = 1.2f,
                                    animationSpec = infiniteRepeatable(tween(2000, delayMillis = ring * 600), RepeatMode.Restart),
                                    label = "ring$ring"
                                )
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.7f, targetValue = 0f,
                                    animationSpec = infiniteRepeatable(tween(2000, delayMillis = ring * 600), RepeatMode.Restart),
                                    label = "alpha$ring"
                                )
                                Box(
                                    Modifier
                                        .size(70.dp)
                                        .scale(scale)
                                        .alpha(alpha)
                                        .border(1.dp, SiloColors.AccentPurple.copy(alpha = alpha), CircleShape)
                                )
                            }
                            Icon(Icons.Outlined.Wifi, contentDescription = null, tint = SiloColors.AccentPurple, modifier = Modifier.size(32.dp))
                        }
                        Text("Waiting for desktop…", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SiloColors.TextPrimary)
                        Text("Open Silo on your PC and click Scan for Devices", fontSize = 12.sp, color = SiloColors.TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // Protocol info
        item {
            SiloCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Network Info", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SiloColors.TextSecondary)
                    InfoRow("Discovery port", "41234")
                    InfoRow("Transfer port", "41236")
                    InfoRow("Protocol", "Silo UDP v1.0")
                    InfoRow("Chunk size", "60 KB")
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
// SEND FILES TAB
// ══════════════════════════════════════════════════════════

@Composable
fun SendFilesTab(uiState: SiloUiState, onPickFiles: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        if (uiState.connectedSession == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Outlined.LinkOff, contentDescription = null, tint = SiloColors.TextMuted, modifier = Modifier.size(48.dp))
                    Text("Not Connected", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = SiloColors.TextPrimary)
                    Text("Connect to a desktop first to send files", fontSize = 13.sp, color = SiloColors.TextSecondary, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // File picker button
                    Surface(
                        onClick = onPickFiles,
                        color  = Color.Transparent,
                        shape  = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, Brush.linearGradient(listOf(SiloColors.AccentPurple, SiloColors.AccentViolet))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(60.dp)
                                    .background(
                                        Brush.linearGradient(listOf(SiloColors.AccentPurple.copy(alpha = 0.2f), SiloColors.AccentViolet.copy(alpha = 0.1f))),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Upload, contentDescription = null, tint = SiloColors.AccentPurple, modifier = Modifier.size(28.dp))
                            }
                            Text("Select Files to Send", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = SiloColors.TextPrimary)
                            Text("Tap to choose files from your device", fontSize = 12.sp, color = SiloColors.TextSecondary)
                        }
                    }
                }

                if (uiState.pendingSendFiles.isNotEmpty()) {
                    item {
                        Text(
                            "Queue (${uiState.pendingSendFiles.size})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = SiloColors.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(uiState.pendingSendFiles) { fileInfo ->
                        SiloCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(fileEmoji(fileInfo.name), fontSize = 24.sp)
                                Column(Modifier.weight(1f)) {
                                    Text(fileInfo.name, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = SiloColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(formatBytes(fileInfo.size), fontSize = 11.sp, color = SiloColors.TextSecondary)
                                }
                                Box(
                                    Modifier
                                        .background(SiloColors.AccentPurple.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Queued", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SiloColors.AccentPurple, letterSpacing = 0.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
// TRANSFERS TAB
// ══════════════════════════════════════════════════════════

@Composable
fun TransfersTab(uiState: SiloUiState) {
    val allTransfers = uiState.activeTransfers + uiState.completedTransfers

    if (allTransfers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = SiloColors.TextMuted, modifier = Modifier.size(48.dp))
                Text("No transfers yet", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = SiloColors.TextPrimary)
                Text("Files sent or received will appear here", fontSize = 13.sp, color = SiloColors.TextSecondary, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allTransfers) { transfer ->
                TransferCard(transfer)
            }
        }
    }
}

@Composable
fun TransferCard(transfer: TransferInfo) {
    SiloCard(borderColor = when (transfer.status) {
        TransferStatus.COMPLETE -> SiloColors.Green.copy(alpha = 0.3f)
        TransferStatus.ERROR    -> SiloColors.Red.copy(alpha = 0.3f)
        else                    -> null
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(fileEmoji(transfer.fileName), fontSize = 24.sp)
                Column(Modifier.weight(1f)) {
                    Text(transfer.fileName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SiloColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${formatBytes(transfer.totalBytes)} · ${if (transfer.direction == TransferDirection.SEND) "Sending" else "Receiving"}", fontSize = 11.sp, color = SiloColors.TextSecondary)
                }
                val badgeColor = when (transfer.status) {
                    TransferStatus.COMPLETE -> SiloColors.Green
                    TransferStatus.ERROR    -> SiloColors.Red
                    else                    -> SiloColors.AccentPurple
                }
                val badgeLabel = when (transfer.status) {
                    TransferStatus.COMPLETE  -> "Done"
                    TransferStatus.ERROR     -> "Error"
                    TransferStatus.IN_PROGRESS -> if (transfer.direction == TransferDirection.SEND) "Sending" else "Receiving"
                    else                     -> "Pending"
                }
                Box(
                    Modifier.background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(badgeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor, letterSpacing = 0.5.sp)
                }
            }

            if (transfer.status == TransferStatus.IN_PROGRESS) {
                val animProgress by animateFloatAsState(
                    targetValue = transfer.progress / 100f,
                    animationSpec = tween(300),
                    label = "progress"
                )
                LinearProgressIndicator(
                    progress = { animProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color     = SiloColors.AccentPurple,
                    trackColor = SiloColors.BgRaised,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${transfer.progress}%", fontSize = 11.sp, color = SiloColors.TextSecondary)
                    Text("${formatBytes(transfer.bytesTransferred)} / ${formatBytes(transfer.totalBytes)}", fontSize = 11.sp, color = SiloColors.TextSecondary)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ══════════════════════════════════════════════════════════

@Composable
fun SiloCard(
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color  = SiloColors.BgSurface,
        shape  = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor ?: SiloColors.BorderColor),
        tonalElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = SiloColors.TextSecondary)
        Text(value, fontSize = 12.sp, color = SiloColors.TextPrimary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

// ══════════════════════════════════════════════════════════
// HELPERS
// ══════════════════════════════════════════════════════════

fun fileEmoji(name: String): String {
    return when (name.substringAfterLast('.', "").lowercase()) {
        "jpg","jpeg","png","gif","webp","heic" -> "🖼"
        "mp4","mov","avi","mkv","webm"         -> "🎬"
        "mp3","wav","flac","aac","ogg"         -> "🎵"
        "pdf"                                  -> "📄"
        "doc","docx"                           -> "📝"
        "xls","xlsx"                           -> "📊"
        "zip","rar","tar","gz","7z"            -> "📦"
        "apk"                                  -> "📱"
        "txt","md"                             -> "📃"
        else                                   -> "📁"
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024L        -> "$bytes B"
        bytes < 1024L * 1024 -> "${"%.1f".format(bytes / 1024f)} KB"
        bytes < 1024L * 1024 * 1024 -> "${"%.1f".format(bytes / 1024f / 1024)} MB"
        else                 -> "${"%.2f".format(bytes / 1024f / 1024 / 1024)} GB"
    }
}

// ══════════════════════════════════════════════════════════
// COLOR PALETTE
// ══════════════════════════════════════════════════════════

object SiloColors {
    val BgDeep      = Color(0xFF08080F)
    val BgBase      = Color(0xFF0E0E1A)
    val BgSurface   = Color(0xFF14141F)
    val BgRaised    = Color(0xFF1A1A28)
    val AccentPurple = Color(0xFF6366F1)
    val AccentViolet = Color(0xFFA78BFA)
    val Green       = Color(0xFF22C55E)
    val Red         = Color(0xFFEF4444)
    val TextPrimary  = Color(0xFFF1F1F8)
    val TextSecondary = Color(0xFF9090B0)
    val TextMuted    = Color(0xFF55556A)
    val BorderColor  = Color(0x12FFFFFF)
}