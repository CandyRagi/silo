package com.example.silo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.silo.network.SiloService
import com.example.silo.ui.components.PairingBanner
import com.example.silo.ui.components.SiloBottomNav
import com.example.silo.ui.components.SiloTopHeader
import com.example.silo.ui.screens.*
import com.example.silo.ui.theme.SiloColors
import com.example.silo.ui.theme.SiloTheme

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

@Composable
fun SiloApp(siloService: SiloService) {
    val context     = LocalContext.current
    val uiState     by siloService.uiState.collectAsStateWithLifecycle()

    // Root-level navigation state
    var showSettings     by remember { mutableStateOf(false) }
    var selectedTab      by remember { mutableIntStateOf(0) }
    var filesDestination by remember { mutableStateOf<FilesDestination>(FilesDestination.Grid) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> uris.forEach { uri -> siloService.sendFile(uri) } }

    LaunchedEffect(Unit) {
        siloService.start()
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
        if (storagePerms.isNotEmpty()) permLauncher.launch(storagePerms.toTypedArray())
    }

    Surface(modifier = Modifier.fillMaxSize(), color = SiloColors.BgDeep) {

        // Determine whether a full-screen overlay is active
        val inSubScreen = showSettings || filesDestination != FilesDestination.Grid

        AnimatedContent(
            targetState  = inSubScreen,
            transitionSpec = {
                if (targetState) {
                    slideInHorizontally(tween(280)) { it } + fadeIn(tween(220)) togetherWith
                    slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(220))
                } else {
                    slideInHorizontally(tween(280)) { -it } + fadeIn(tween(220)) togetherWith
                    slideOutHorizontally(tween(280)) { it } + fadeOut(tween(220))
                }
            },
            label = "rootNav"
        ) { overlayActive ->

            if (overlayActive) {
                // ── Full-screen overlay (no header / bottom nav) ────────
                when {
                    showSettings -> SettingsScreen(
                        uiState = uiState,
                        onBack  = { showSettings = false }
                    )
                    filesDestination == FilesDestination.FileTransfer -> FileTransferScreen(
                        uiState    = uiState,
                        onPickFiles = { filePicker.launch("*/*") },
                        onBack     = { filesDestination = FilesDestination.Grid }
                    )
                    filesDestination == FilesDestination.ImageTransfer -> ImageTransferScreen(
                        uiState    = uiState,
                        onPickFiles = { filePicker.launch("image/*") },
                        onBack     = { filesDestination = FilesDestination.Grid }
                    )
                    filesDestination == FilesDestination.History -> HistoryScreen(
                        uiState = uiState,
                        onBack  = { filesDestination = FilesDestination.Grid }
                    )
                    filesDestination == FilesDestination.Placeholder4 -> PlaceholderScreen(
                        title  = "Clipboard",
                        onBack = { filesDestination = FilesDestination.Grid }
                    )
                    filesDestination == FilesDestination.Placeholder5 -> PlaceholderScreen(
                        title  = "Audio",
                        onBack = { filesDestination = FilesDestination.Grid }
                    )
                    filesDestination == FilesDestination.Placeholder6 -> PlaceholderScreen(
                        title  = "More",
                        onBack = { filesDestination = FilesDestination.Grid }
                    )
                }

            } else {
                // ── Normal shell (header + tabs + bottom nav) ──────────
                Column(Modifier.fillMaxSize()) {

                    SiloTopHeader(
                        uiState    = uiState,
                        onSettings = { showSettings = true }
                    )

                    AnimatedVisibility(
                        visible = uiState.pendingPairRequest != null,
                        enter   = expandVertically() + fadeIn(),
                        exit    = shrinkVertically() + fadeOut()
                    ) {
                        uiState.pendingPairRequest?.let { req ->
                            PairingBanner(
                                req      = req,
                                onVerify = { pin -> siloService.verifyAndAcceptPairing(req, pin) },
                                onDeny   = { siloService.denyPairing(req) }
                            )
                        }
                    }

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        AnimatedContent(
                            targetState  = selectedTab,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally(tween(250)) { it } + fadeIn(tween(200)) togetherWith
                                    slideOutHorizontally(tween(250)) { -it } + fadeOut(tween(200))
                                } else {
                                    slideInHorizontally(tween(250)) { -it } + fadeIn(tween(200)) togetherWith
                                    slideOutHorizontally(tween(250)) { it } + fadeOut(tween(200))
                                }
                            },
                            label = "tabContent"
                        ) { tab ->
                            when (tab) {
                                0    -> FilesScreen(onNavigate = { filesDestination = it })
                                1    -> CommandsScreen(uiState = uiState)
                                else -> FilesScreen(onNavigate = { filesDestination = it })
                            }
                        }
                    }

                    SiloBottomNav(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                }
            }
        }
    }
}