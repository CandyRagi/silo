package com.example.silo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.IBinder
import java.io.File
import com.example.silo.network.SiloService
import com.example.silo.model.UserProfileState
import com.example.silo.ui.components.PairingBanner
import com.example.silo.ui.components.SiloBottomNav
import com.example.silo.ui.components.SiloTopHeader
import com.example.silo.ui.screens.*
import com.example.silo.ui.theme.SiloColors
import com.example.silo.ui.theme.SiloTheme

class MainActivity : ComponentActivity() {
    private var siloService by mutableStateOf<SiloService?>(null)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SiloService.LocalBinder
            siloService = binder.getService()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            siloService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val intent = Intent(this, SiloService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            SiloTheme {
                val service = siloService
                if (service != null) {
                    SiloApp(siloService = service)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(SiloColors.BgDeep))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}

@Composable
fun SiloApp(siloService: SiloService) {
    val context     = LocalContext.current
    val uiState     by siloService.uiState.collectAsStateWithLifecycle()
    val profile     = remember { UserProfileState(context) }

    // Root-level navigation state
    var showSettings      by remember { mutableStateOf(false) }
    var selectedTab       by remember { mutableIntStateOf(0) }
    var filesDestination  by remember { mutableStateOf<FilesDestination>(FilesDestination.Grid) }
    var commandsDestination by remember { mutableStateOf<CommandsDestination>(CommandsDestination.Grid) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> uris.forEach { uri -> siloService.sendFile(uri) } }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                siloService.sendFile(Uri.fromFile(file))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

    DisposableEffect(Unit) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        
        fun attemptClipboardSync(reason: String) {
            if (siloService.uiState.value.connectedSession != null) {
                try {
                    val clip = clipboardManager?.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val text = clip.getItemAt(0).coerceToText(context)?.toString()
                        if (!text.isNullOrEmpty()) {
                            android.widget.Toast.makeText(context, "Syncing Clipboard ($reason)...", android.widget.Toast.LENGTH_SHORT).show()
                            siloService.sendClipboard(text)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val listener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
            attemptClipboardSync("changed")
        }
        clipboardManager?.addPrimaryClipChangedListener(listener)

        onDispose {
            clipboardManager?.removePrimaryClipChangedListener(listener)
        }
    }

    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        val focusListener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                if (siloService.uiState.value.connectedSession != null) {
                    try {
                        val clip = clipboardManager?.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val text = clip.getItemAt(0).coerceToText(context)?.toString()
                            if (!text.isNullOrEmpty()) {
                                siloService.sendClipboard(text)
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        onDispose {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = SiloColors.BgDeep) {

        // Determine whether a full-screen overlay is active
        val inSubScreen = showSettings || filesDestination != FilesDestination.Grid || commandsDestination != CommandsDestination.Grid

        BackHandler(enabled = inSubScreen) {
            when {
                showSettings -> showSettings = false
                filesDestination != FilesDestination.Grid -> filesDestination = FilesDestination.Grid
                commandsDestination != CommandsDestination.Grid -> commandsDestination = CommandsDestination.Grid
            }
        }

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
                        profile = profile,
                        onBack  = { showSettings = false }
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
                    commandsDestination == CommandsDestination.MouseControl -> MouseControlScreen(
                        onBack       = { commandsDestination = CommandsDestination.Grid },
                        onMouseMove  = { dx, dy -> siloService.sendMouseMove(dx, dy) },
                        onMouseClick = { btn -> siloService.sendMouseClick(btn) }
                    )
                    commandsDestination == CommandsDestination.KeyboardControl -> KeyboardControlScreen(
                        onBack       = { commandsDestination = CommandsDestination.Grid },
                        onSendKey    = { key -> siloService.sendKeyboard(key) },
                        onSendMove   = { dx, dy -> siloService.sendMouseMove(dx, dy) },
                        onSendClick  = { btn -> siloService.sendMouseClick(btn) }
                    )
                    commandsDestination == CommandsDestination.CameraStream -> CameraStreamScreen(
                        onBack      = { commandsDestination = CommandsDestination.Grid },
                        onSendFrame = { bytes, rot -> siloService.sendCameraFrame(bytes, rot) }
                    )
                    commandsDestination == CommandsDestination.ScreenShare -> ScreenShareScreen(
                        onBack       = { commandsDestination = CommandsDestination.Grid },
                        onStartShare = { code, data -> siloService.startScreenCapture(code, data) },
                        onStopShare  = { siloService.stopScreenCapture() }
                    )
                }

            } else {
                // ── Normal shell (header + tabs + bottom nav) ──────────
                Column(Modifier.fillMaxSize()) {

                    SiloTopHeader(
                        uiState    = uiState,
                        profile    = profile,
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
                                onAccept = { siloService.acceptPairing(req) },
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
                                0    -> FilesScreen(
                                    isConnected = uiState.connectedSession != null,
                                    onNavigate  = { filesDestination = it },
                                    onPickFiles = { mime -> filePicker.launch(mime) },
                                    onCameraCapture = { cameraLauncher.launch(null) }
                                )
                                1    -> CommandsScreen(
                                    uiState    = uiState,
                                    onNavigate = { commandsDestination = it }
                                )
                                else -> FilesScreen(
                                    isConnected = uiState.connectedSession != null,
                                    onNavigate  = { filesDestination = it },
                                    onPickFiles = { mime -> filePicker.launch(mime) },
                                    onCameraCapture = { cameraLauncher.launch(null) }
                                )
                            }
                        }
                    }

                    SiloBottomNav(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                }
            }
        }
    }
}