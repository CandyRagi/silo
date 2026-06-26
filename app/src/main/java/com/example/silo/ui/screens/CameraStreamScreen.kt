package com.example.silo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.silo.ui.theme.SamsungFontFamily
import com.example.silo.ui.theme.SiloColors
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@Composable
fun CameraStreamScreen(
    onBack: () -> Unit,
    onSendFrame: (ByteArray, Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var useFrontCamera by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var lastFrameTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        if (hasCameraPermission) {
            // Camera preview — keyed on useFrontCamera to force recreation on flip
            key(useFrontCamera) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val provider = cameraProviderFuture.get()
                            cameraProvider = provider

                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(320, 240))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                .build()

                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                val now = System.currentTimeMillis()
                                // Limit to ~20 FPS
                                if (now - lastFrameTime >= 50) {
                                    lastFrameTime = now
                                    try {
                                        val yBuffer = imageProxy.planes[0].buffer
                                        val uBuffer = imageProxy.planes[1].buffer
                                        val vBuffer = imageProxy.planes[2].buffer
                                        val ySize = yBuffer.remaining()
                                        val uSize = uBuffer.remaining()
                                        val vSize = vBuffer.remaining()
                                        val nv21 = ByteArray(ySize + uSize + vSize)
                                        yBuffer.get(nv21, 0, ySize)
                                        vBuffer.get(nv21, ySize, vSize)
                                        uBuffer.get(nv21, ySize + vSize, uSize)

                                        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
                                        val out = ByteArrayOutputStream()
                                        // Reduce quality slightly for higher frame rate
                                        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 40, out)
                                        val jpegBytes = out.toByteArray()

                                        if (jpegBytes.size < 60000) {
                                            onSendFrame(jpegBytes, imageProxy.imageInfo.rotationDegrees)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("CameraStream", "Frame error: ${e.message}")
                                    }
                                }
                                imageProxy.close()
                            }

                            val selector = if (useFrontCamera)
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            else
                                CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                            } catch (e: Exception) {
                                Log.e("CameraStream", "Bind failed: ${e.message}")
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // No permission state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Camera permission required",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }

        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                "Camera Stream",
                fontFamily = SamsungFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
        }

        // Bottom right flip button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .navigationBarsPadding()
                .size(56.dp)
                .clip(CircleShape)
                .background(SiloColors.BgSurface.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    useFrontCamera = !useFrontCamera
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Cameraswitch,
                contentDescription = "Flip Camera",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
