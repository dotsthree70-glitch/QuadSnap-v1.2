package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun InlineCameraPreview(
    onCaptured: (Uri) -> Unit,
    onCancel: () -> Unit = {},
    slotIndex: Int = 0,
    onSelectGallery: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E2E)),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
            ) {
                Text("Enable Camera", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        // Start and bind the shared camera manager so all available viewfinders stream simultaneously
        LaunchedEffect(hasCameraPermission) {
            if (hasCameraPermission) {
                SharedCameraManager.startCamera(context, lifecycleOwner)
            }
        }

        val currentFrame by SharedCameraManager.currentFrame.collectAsState()
        val lensFacing by SharedCameraManager.lensFacing.collectAsState()
        val isReady by SharedCameraManager.isReady.collectAsState()

        var isCapturing by remember { mutableStateOf(false) }
        var focusPoint by remember { mutableStateOf<Offset?>(null) }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }

        // Auto-dismiss focus animation after 1.5s
        LaunchedEffect(focusPoint) {
            if (focusPoint != null) {
                delay(1500)
                focusPoint = null
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .pointerInput(containerSize) {
                    detectTapGestures { tapOffset ->
                        focusPoint = tapOffset
                        val normX = if (containerSize.width > 0) tapOffset.x / containerSize.width else 0.5f
                        val normY = if (containerSize.height > 0) tapOffset.y / containerSize.height else 0.5f
                        SharedCameraManager.focus(normX, normY)
                    }
                }
        ) {
            // Live active camera feed
            val frame = currentFrame
            if (frame != null && !frame.isRecycled) {
                Image(
                    bitmap = frame.asImageBitmap(),
                    contentDescription = "Live Camera ${slotIndex + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                scaleX = -1f
                            }
                        },
                    contentScale = ContentScale.Crop
                )
            } else {
                // Sleek loading viewfinder placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF12131A)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFF00E676),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Starting Live Cam ${slotIndex + 1}...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Top status pill indicating Camera is active & streaming live
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .background(Color.Black.copy(alpha = 0.60f), shape = RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CAMERA ${slotIndex + 1} • LIVE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            // Tap-to-Focus Reticle
            focusPoint?.let { pos ->
                val boxSizePx = with(density) { 56.dp.toPx() }
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (pos.x - boxSizePx / 2f).roundToInt(),
                                (pos.y - boxSizePx / 2f).roundToInt()
                            )
                        }
                        .size(56.dp)
                        .border(2.dp, Color(0xFF00E676), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Focussed",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Camera Action Controls at Bottom
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .background(Color.Black.copy(alpha = 0.60f), shape = CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Switch Camera Lens (Back / Front)
                IconButton(
                    onClick = {
                        SharedCameraManager.switchCamera()
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Primary Capture Shutter
                IconButton(
                    onClick = {
                        if (isCapturing) return@IconButton
                        isCapturing = true

                        SharedCameraManager.takePicture(
                            context = context,
                            slotIndex = slotIndex,
                            onSuccess = { uri ->
                                isCapturing = false
                                onCaptured(uri)
                            },
                            onError = { exception ->
                                isCapturing = false
                                Log.e("InlineCameraPreview", "Photo capture failed: ${exception.message}", exception)
                            }
                        )
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White)
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF6C63FF),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture Photo Camera ${slotIndex + 1}",
                            tint = Color(0xFF6C63FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Gallery Picker Button
                if (onSelectGallery != null) {
                    IconButton(
                        onClick = onSelectGallery,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Select from Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(28.dp))
                }
            }
        }
    }
}
