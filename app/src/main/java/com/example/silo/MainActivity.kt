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
import androidx.compose.animation.core.*
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
import com.example.silo.ui.screens.CommandsScreen
import com.example.silo.ui.screens.FilesScreen
import com.example.silo.ui.screens.SettingsScreen
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
    var showSettings by remember { mutableStateOf(false) }
    var selectedTab  by remember { mutableIntStateOf(0) }

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
        AnimatedContent(
            targetState = showSettings,
            transitionSpec = {
                if (targetState) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "rootNav"
        ) { inSettings ->
            if (inSettings) {
                SettingsScreen(uiState = uiState, onBack = { showSettings = false })
            } else {
                Column(Modifier.fillMaxSize()) {
                    // Shared top header
                    SiloTopHeader(uiState = uiState, onSettings = { showSettings = true })

                    // Pairing banner (always visible above content)
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

                    // Animated tab content
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                                } else {
                                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                                }
                            },
                            label = "tabContent"
                        ) { tab ->
                            when (tab) {
                                0    -> FilesScreen(uiState = uiState, onPickFiles = { filePicker.launch("*/*") })
                                1    -> CommandsScreen(uiState = uiState)
                                else -> FilesScreen(uiState = uiState, onPickFiles = { filePicker.launch("*/*") })
                            }
                        }
                    }

                    // Bottom nav
                    SiloBottomNav(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                }
            }
        }
    }
}