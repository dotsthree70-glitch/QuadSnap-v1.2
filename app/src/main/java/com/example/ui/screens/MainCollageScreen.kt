package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Save
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BlendMode
import com.example.data.model.CollageLayout
import com.example.data.model.SlotState
import com.example.ui.components.InlineCameraPreview
import com.example.ui.viewmodel.CollageViewModel
import com.example.utils.ShareSaveHelper
import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults

@Composable
fun MainCollageScreen(viewModel: CollageViewModel) {
    val context = LocalContext.current
    val state by viewModel.collageState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val isAiProcessing by viewModel.isAiProcessing.collectAsState()
    val finalCollage by viewModel.finalCollageBitmap.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var activeSlotForPicker by remember { mutableStateOf<Int?>(null) }

    // System gallery picker configuration
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val slotIndex = activeSlotForPicker
        if (uri != null && slotIndex != null) {
            viewModel.updateSlotWithPhoto(slotIndex, uri)
        }
        activeSlotForPicker = null
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .border(width = 6.dp, color = Color(0xFF56B4FD)), // Sky blue and bold outer border
        containerColor = Color(0xFF56B4FD) // Sky blue top container for seamless status bar blend
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF9E6), // Light Cream
                            Color(0xFFF3E5F5)  // Light Lilac
                        )
                    )
                )
                .border(width = 6.dp, color = Color(0xFF56B4FD)) // Bold sky blue outer border around screen content
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sky Blue Header Background at the top
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = Color(0xFF87CEEB), // Classic Sky Blue
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF56B4FD), // Vibrant Sky Blue
                                        Color(0xFF87CEEB), // True Sky Blue
                                        Color(0xFFA5E1FF)  // Soft Sky Blue
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "QuadSnap",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF311B92))
                                        ),
                                        letterSpacing = (-0.5).sp
                                    )
                                )
                                Text(
                                    text = "by Abdul Majid",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF01579B)
                                    )
                                )
                            }

                            // Undo, Redo, Clear Action Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.undo() },
                                    enabled = canUndo,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF0277BD),
                                        disabledContainerColor = Color.White.copy(alpha = 0.6f),
                                        disabledContentColor = Color(0xFF0277BD).copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .size(46.dp)
                                        .border(1.5.dp, Color(0xFFB3E5FC), CircleShape)
                                        .testTag("undo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Undo Action",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.redo() },
                                    enabled = canRedo,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF0277BD),
                                        disabledContainerColor = Color.White.copy(alpha = 0.6f),
                                        disabledContentColor = Color(0xFF0277BD).copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .size(46.dp)
                                        .border(1.5.dp, Color(0xFFB3E5FC), CircleShape)
                                        .testTag("redo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Redo,
                                        contentDescription = "Redo Action",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.clearAll() },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color(0xFFFFEBEE),
                                        contentColor = Color(0xFFD32F2F)
                                    ),
                                    modifier = Modifier
                                        .size(46.dp)
                                        .border(1.5.dp, Color(0xFFFFCDD2), CircleShape)
                                        .testTag("clear_all_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ClearAll,
                                        contentDescription = "Reset Workspace",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Inner content container with horizontal padding
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Interactive Segmented Collage Grid Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(vertical = 12.dp)
                        .border(
                            width = 3.dp,
                            color = Color(0xFF7C4DFF),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .testTag("collage_grid_container"),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        CollageGrid(
                            layout = state.layout,
                            slots = state.slots,
                            isCircular = state.isCircularStyle,
                            onSlotClick = { index ->
                                activeSlotForPicker = index
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            onClearSlot = { index ->
                                viewModel.clearSlot(index)
                            },
                            onCaptured = { index, uri ->
                                viewModel.updateSlotWithPhoto(index, uri)
                            },
                            onCancelCamera = { index ->
                                viewModel.clearSlot(index)
                            }
                        )
                    }
                }

                // Collage Layout Templates Option Section Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .border(
                            width = 3.dp,
                            color = Color(0xFF7C4DFF),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        // Layout Template Picker & Circular Style Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Choose Collage Layout",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1A237E)
                                )
                            )

                            // Circular vs Classic Style Selector
                            val isCircularActive = state.isCircularStyle
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFEDE7F6))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Classic Rounded Style
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (!isCircularActive) Color(0xFF6200EE) else Color.Transparent)
                                        .clickable { viewModel.setCircularStyle(false) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (!isCircularActive) Color.White else Color(0xFF7E57C2))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Classic",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isCircularActive) Color.White else Color(0xFF5E35B1)
                                        )
                                    }
                                }

                                // Circular Style
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isCircularActive) Color(0xFF6200EE) else Color.Transparent)
                                        .clickable { viewModel.setCircularStyle(true) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(if (isCircularActive) Color.White else Color(0xFF7E57C2))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Circular",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCircularActive) Color.White else Color(0xFF5E35B1)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CollageLayout.values().forEach { layoutOption ->
                                val isSelected = state.layout == layoutOption
                                val bgBrush = if (isSelected) {
                                    Brush.linearGradient(colors = listOf(Color(0xFF6200EE), Color(0xFF8E24AA)))
                                } else {
                                    Brush.linearGradient(colors = listOf(Color(0xFFE3F2FD), Color(0xFFE3F2FD)))
                                }

                                Box(
                                    modifier = Modifier
                                        .width(86.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(bgBrush)
                                        .clickable { viewModel.selectLayout(layoutOption) }
                                        .border(
                                            width = if (isSelected) 3.dp else 1.5.dp,
                                            color = Color(0xFF7C4DFF),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .padding(vertical = 12.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        MiniLayoutPreview(
                                            layout = layoutOption,
                                            isSelected = isSelected,
                                            isCircular = state.isCircularStyle
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = layoutOption.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF455A64),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dynamic Pastel Gradient Palette Selector
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp)
                        .border(
                            width = 3.dp,
                            color = Color(0xFF7C4DFF),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp, top = 14.dp)) {
                        Text(
                            text = "Select Background Gradient",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A237E),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf(
                                "None" to listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC)),
                                "Mint" to listOf(Color(0xFFE0F2F1), Color(0xFFEDE7F6)),
                                "Peach" to listOf(Color(0xFFFFF3E0), Color(0xFFFFFDE7)),
                                "Pink" to listOf(Color(0xFFFCE4EC), Color(0xFFFFF3E0)),
                                "Sky" to listOf(Color(0xFFE1F5FE), Color(0xFFE0F2F1)),
                                "Sunset" to listOf(Color(0xFFF3E5F5), Color(0xFFFCE4EC))
                            ).forEachIndexed { index, (label, colors) ->
                                val isGradientSelected = state.selectedGradientIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Brush.linearGradient(colors))
                                        .border(
                                            width = if (isGradientSelected) 3.5.dp else 1.dp,
                                            color = if (isGradientSelected) Color(0xFF6200EE) else Color(0xFFE0E0E0),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable { viewModel.selectBackgroundGradient(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isGradientSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF6200EE)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF37474F),
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


                // Compile / Render Action Button (Disabled until all camera frames have captured photos or selected images)
                val totalSlotsCount = state.layout.slotCount
                val capturedPhotosCount = (0 until totalSlotsCount).count { i ->
                    state.slots.getOrNull(i) is SlotState.Photo
                }
                val allSlotsFilled = capturedPhotosCount == totalSlotsCount
                val isCompileEnabled = allSlotsFilled && !isAiProcessing

                Button(
                    onClick = {
                        if (isCompileEnabled) {
                            viewModel.generateFinalCollage(context)
                        }
                    },
                    enabled = isCompileEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(bottom = 4.dp)
                        .testTag("render_collage_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    val bgBrush = if (isCompileEnabled) {
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6200EE), Color(0xFFE91E63))
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF455A64).copy(alpha = 0.45f), Color(0xFF37474F).copy(alpha = 0.45f))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = bgBrush,
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAiProcessing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(26.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Compiling Collage...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isCompileEnabled) Icons.Default.AutoAwesome else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isCompileEnabled) Color.White else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCompileEnabled) {
                                        "Compile Collage"
                                    } else {
                                        "Compile Collage ($capturedPhotosCount/$totalSlotsCount filled)"
                                    },
                                    color = if (isCompileEnabled) Color.White else Color.White.copy(alpha = 0.75f),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }
            }


            // Final Render Outcome Overlay Display
            finalCollage?.let { bitmap ->
                var overlayText by remember { mutableStateOf("") }
                var selectedColorIndex by remember { mutableStateOf(0) } // White
                var selectedPositionIndex by remember { mutableStateOf(2) } // Bottom
                var selectedSizeIndex by remember { mutableStateOf(1) } // Medium
                var isBold by remember { mutableStateOf(true) }
                var isItalic by remember { mutableStateOf(false) }
                var showBubbleBackground by remember { mutableStateOf(true) }

                val textColors = listOf(
                    Color.White to "White",
                    Color.Black to "Black",
                    Color(0xFFFFD54F) to "Yellow",
                    Color(0xFFFF1744) to "Red",
                    Color(0xFF00E5FF) to "Sky",
                    Color(0xFFE040FB) to "Pink"
                )

                val positions = listOf(
                    0.15f to "Top",
                    0.50f to "Center",
                    0.85f to "Bottom"
                )

                val textSizes = listOf(
                    16f to "Small",
                    28f to "Medium",
                    40f to "Large"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .clickable { /* Block background taps */ },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.98f)
                            .padding(8.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Compiled Collage",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF6200EE)
                                    )
                                    Text(
                                        text = "Seamless montage • Add captions",
                                        fontSize = 10.sp,
                                        color = Color(0xFF78909C)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.clearFinalCollage() },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color(0xFFF5F5F5)
                                    ),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Preview",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // High-quality finished image with live text overlay preview (Scalable height)
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .heightIn(max = 290.dp)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Finalized collage outcome with no spaces or borders",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )

                                // Live Text Overlay on top of the Image preview
                                if (overlayText.isNotBlank()) {
                                    val alignment = when (selectedPositionIndex) {
                                        0 -> Alignment.TopCenter
                                        1 -> Alignment.Center
                                        else -> Alignment.BottomCenter
                                    }
                                    val fontSizeVal = when (selectedSizeIndex) {
                                        0 -> 12.sp
                                        1 -> 18.sp
                                        else -> 24.sp
                                    }
                                    val fontWeightVal = if (isBold) FontWeight.Bold else FontWeight.Normal
                                    val fontStyleVal = if (isItalic) FontStyle.Italic else FontStyle.Normal
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        contentAlignment = alignment
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (showBubbleBackground) Color.Black.copy(alpha = 0.6f)
                                                    else Color.Transparent
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = overlayText,
                                                color = textColors[selectedColorIndex].first,
                                                fontSize = fontSizeVal,
                                                fontWeight = fontWeightVal,
                                                fontStyle = fontStyleVal,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // --- PROFESSIONAL PHOTO BLENDING CONTROLS ---
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .testTag("dialog_blend_controls_card"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FD)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8EAF6))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color(0xFF6200EE),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "PHOTO BLENDING",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6200EE)
                                            )
                                        }
                                        if (isAiProcessing) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(10.dp),
                                                    strokeWidth = 1.5.dp,
                                                    color = Color(0xFF6200EE)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Blending...", fontSize = 9.sp, color = Color(0xFF6200EE))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Blend styles horizontal selector
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        BlendMode.values().forEach { mode ->
                                            val isSelected = state.blendMode == mode
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        if (!isAiProcessing) {
                                                            viewModel.setBlendMode(mode, context)
                                                        }
                                                    }
                                                    .testTag("dialog_blend_mode_${mode.name.lowercase()}"),
                                                color = if (isSelected) Color(0xFF6200EE) else Color.White,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF6200EE) else Color(0xFFCFD8DC)
                                                ),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = mode.displayName,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else Color(0xFF37474F),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (state.blendMode != BlendMode.CLEAN_GRID) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Blend Softness",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF546E7A)
                                            )
                                            Text(
                                                text = "${state.blendFeatherPx} px",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6200EE)
                                            )
                                        }
                                        Slider(
                                            value = state.blendFeatherPx.toFloat(),
                                            onValueChange = { newFeather ->
                                                viewModel.setBlendFeather(newFeather.toInt())
                                            },
                                            onValueChangeFinished = {
                                                viewModel.setBlendFeather(state.blendFeatherPx, context)
                                            },
                                            valueRange = 16f..96f,
                                            steps = 7,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF6200EE),
                                                activeTrackColor = Color(0xFF6200EE),
                                                inactiveTrackColor = Color(0xFFE0E0E0)
                                            ),
                                            modifier = Modifier.height(18.dp)
                                        )
                                    }
                                }
                            }

                            // --- TEXT EDITOR CAPTION SECTION ---
                            OutlinedTextField(
                                value = overlayText,
                                onValueChange = { overlayText = it },
                                placeholder = { Text("Add a caption...", color = Color.Gray, fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("collage_caption_input"),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                trailingIcon = {
                                    if (overlayText.isNotEmpty()) {
                                        IconButton(onClick = { overlayText = "" }) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Text", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6200EE),
                                    unfocusedBorderColor = Color(0xFFB0BEC5),
                                    focusedContainerColor = Color(0xFFF5F7FA),
                                    unfocusedContainerColor = Color(0xFFF5F7FA)
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Typography Controls Row (Bold, Italic, Shading)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Bold Toggle
                                Button(
                                    onClick = { isBold = !isBold },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isBold) Color(0xFF6200EE) else Color(0xFFECEFF1),
                                        contentColor = if (isBold) Color.White else Color(0xFF455A64)
                                    ),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("B", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                }

                                // Italic Toggle
                                Button(
                                    onClick = { isItalic = !isItalic },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isItalic) Color(0xFF6200EE) else Color(0xFFECEFF1),
                                        contentColor = if (isItalic) Color.White else Color(0xFF455A64)
                                    ),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                // Background bubble Toggle
                                Button(
                                    onClick = { showBubbleBackground = !showBubbleBackground },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (showBubbleBackground) Color(0xFF6200EE) else Color(0xFFECEFF1),
                                        contentColor = if (showBubbleBackground) Color.White else Color(0xFF455A64)
                                    ),
                                    modifier = Modifier.weight(1.5f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Bubble Bg", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Position and Size Row Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Position column selector
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text("POSITION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        positions.forEachIndexed { idx, item ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (selectedPositionIndex == idx) Color(0xFF7C4DFF) else Color(0xFFECEFF1))
                                                    .clickable { selectedPositionIndex = idx },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = item.second,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (selectedPositionIndex == idx) Color.White else Color(0xFF455A64)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Size column selector
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text("SIZE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        textSizes.forEachIndexed { idx, item ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (selectedSizeIndex == idx) Color(0xFF7C4DFF) else Color(0xFFECEFF1))
                                                    .clickable { selectedSizeIndex = idx },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = item.second,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (selectedSizeIndex == idx) Color.White else Color(0xFF455A64)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Color Selector Row
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("TEXT COLOR", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    textColors.forEachIndexed { idx, colorPair ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(colorPair.first)
                                                .border(
                                                    width = if (selectedColorIndex == idx) 2.dp else 1.dp,
                                                    color = if (selectedColorIndex == idx) Color(0xFF6200EE) else Color.Gray.copy(alpha = 0.4f),
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedColorIndex = idx },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selectedColorIndex == idx) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = if (colorPair.first == Color.White) Color.Black else Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Save and Share Action Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Save to Gallery Button
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val finalBakedBitmap = drawTextOnBitmap(
                                                original = bitmap,
                                                text = overlayText,
                                                textColor = textColors[selectedColorIndex].first,
                                                textSize = textSizes[selectedSizeIndex].first,
                                                positionYPercent = positions[selectedPositionIndex].first,
                                                isBold = isBold,
                                                isItalic = isItalic,
                                                showBackgroundBubble = showBubbleBackground
                                            )
                                            ShareSaveHelper.saveBitmapToGallery(context, finalBakedBitmap)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("save_to_gallery_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Green
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "Save to Gallery Icon",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                // Share Button
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val finalBakedBitmap = drawTextOnBitmap(
                                                original = bitmap,
                                                text = overlayText,
                                                textColor = textColors[selectedColorIndex].first,
                                                textSize = textSizes[selectedSizeIndex].first,
                                                positionYPercent = positions[selectedPositionIndex].first,
                                                isBold = isBold,
                                                isItalic = isItalic,
                                                showBackgroundBubble = showBubbleBackground
                                            )
                                            ShareSaveHelper.shareBitmap(context, finalBakedBitmap)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("share_collage_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)) // Light Blue
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Collage Icon",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { viewModel.clearFinalCollage() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                            ) {
                                Text("Back to Collage Studio", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollageGrid(
    layout: CollageLayout,
    slots: List<SlotState>,
    isCircular: Boolean,
    onSlotClick: (Int) -> Unit,
    onClearSlot: (Int) -> Unit,
    onCaptured: (Int, Uri) -> Unit,
    onCancelCamera: (Int) -> Unit
) {
    val circularMode = isCircular
    fun slotAt(i: Int): SlotState = slots.getOrElse(i) { SlotState.CameraActive }

    when (layout) {
        CollageLayout.TWO_SPLIT_VERTICAL -> {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CollageCell(0, slotAt(0), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CollageCell(1, slotAt(1), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
            }
        }
        CollageLayout.TWO_SPLIT_HORIZONTAL -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageCell(0, slotAt(0), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageCell(1, slotAt(1), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
            }
        }
        CollageLayout.THREE_GRID -> {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CollageCell(0, slotAt(0), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        CollageCell(1, slotAt(1), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        CollageCell(2, slotAt(2), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                    }
                }
            }
        }
        CollageLayout.THREE_ROWS -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageCell(0, slotAt(0), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageCell(1, slotAt(1), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageCell(2, slotAt(2), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
            }
        }
        CollageLayout.THREE_COLUMNS -> {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CollageCell(0, slotAt(0), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CollageCell(1, slotAt(1), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CollageCell(2, slotAt(2), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
            }
        }
        CollageLayout.FOUR_GRID -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        CollageCell(0, slotAt(0), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        CollageCell(1, slotAt(1), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                    }
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        CollageCell(2, slotAt(2), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        CollageCell(3, slotAt(3), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                    }
                }
            }
        }
        CollageLayout.FOUR_ROWS -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageCell(0, slotAt(0), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageCell(1, slotAt(1), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageCell(2, slotAt(2), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageCell(3, slotAt(3), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
            }
        }
        CollageLayout.FOUR_COLUMNS -> {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CollageCell(0, slotAt(0), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CollageCell(1, slotAt(1), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CollageCell(2, slotAt(2), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CollageCell(3, slotAt(3), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
            }
        }
        CollageLayout.FOUR_FEATURED -> {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CollageCell(0, slotAt(0), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        CollageCell(1, slotAt(1), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        CollageCell(2, slotAt(2), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        CollageCell(3, slotAt(3), circularMode, onSlotClick, onClearSlot, onCaptured, onCancelCamera)
                    }
                }
            }
        }
    }
}

data class LiveCamTheme(
    val name: String,
    val backgroundGradient: List<Color>,
    val accentColor: Color,
    val titleColor: Color,
    val borderColor: Color,
    val cardBg: Color
)

fun getLiveCamTheme(index: Int): LiveCamTheme {
    return when (index) {
        0 -> LiveCamTheme(
            name = "Camera 1",
            backgroundGradient = listOf(Color(0xFFEDE7F6), Color(0xFFD1C4E9)), // Elegant Lavender & Amethyst
            accentColor = Color(0xFF5E35B1),
            titleColor = Color(0xFF4527A0),
            borderColor = Color(0xFFB39DDB),
            cardBg = Color(0xFFF3E5F5)
        )
        1 -> LiveCamTheme(
            name = "Camera 2",
            backgroundGradient = listOf(Color(0xFFE0F2F1), Color(0xFFB2DFDB)), // Elegant Sage Mint & Emerald
            accentColor = Color(0xFF00796B),
            titleColor = Color(0xFF004D40),
            borderColor = Color(0xFF80CBC4),
            cardBg = Color(0xFFE0F2F1)
        )
        2 -> LiveCamTheme(
            name = "Camera 3",
            backgroundGradient = listOf(Color(0xFFFFF3E0), Color(0xFFFFCCBC)), // Elegant Coral Champagne & Peach
            accentColor = Color(0xFFD84315),
            titleColor = Color(0xFFBF360C),
            borderColor = Color(0xFFFFAB91),
            cardBg = Color(0xFFFBE9E7)
        )
        3 -> LiveCamTheme(
            name = "Camera 4",
            backgroundGradient = listOf(Color(0xFFE1F5FE), Color(0xFF81D4FA)), // Elegant Azure Sky & Cerulean
            accentColor = Color(0xFF0277BD),
            titleColor = Color(0xFF01579B),
            borderColor = Color(0xFF4FC3F7),
            cardBg = Color(0xFFE1F5FE)
        )
        else -> LiveCamTheme(
            name = "Camera ${index + 1}",
            backgroundGradient = listOf(Color(0xFFF5F5F5), Color(0xFFE0E0E0)),
            accentColor = Color(0xFF455A64),
            titleColor = Color(0xFF263238),
            borderColor = Color(0xFFB0BEC5),
            cardBg = Color(0xFFECEFF1)
        )
    }
}

@Composable
fun CollageCell(
    index: Int,
    state: SlotState,
    isCircular: Boolean = false,
    onSlotClick: (Int) -> Unit,
    onClearSlot: (Int) -> Unit,
    onCaptured: (Int, Uri) -> Unit,
    onCancelCamera: (Int) -> Unit
) {
    val theme = remember(index) { getLiveCamTheme(index) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val cellShape = if (isCircular) CircleShape else RoundedCornerShape(18.dp)
        val cellModifier = if (isCircular) {
            val minDim = if (maxWidth < maxHeight) maxWidth else maxHeight
            Modifier
                .size(minDim * 0.94f)
                .clip(CircleShape)
        } else {
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
        }

        Card(
            modifier = cellModifier,
            shape = cellShape,
            colors = CardDefaults.cardColors(containerColor = theme.cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is SlotState.CameraActive, is SlotState.Locked, is SlotState.Empty -> {
                        InlineCameraPreview(
                            slotIndex = index,
                            onCaptured = { uri -> onCaptured(index, uri) },
                            onCancel = { onCancelCamera(index) },
                            onSelectGallery = { onSlotClick(index) }
                        )
                    }
                    is SlotState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(colors = theme.backgroundGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = theme.accentColor)
                        }
                    }
                    is SlotState.Photo -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = state.uri,
                                contentDescription = "Image inside ${theme.name}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Subtle elegant pill badge indicating LiveCam name
                            Box(
                                modifier = Modifier
                                    .align(if (isCircular) Alignment.TopCenter else Alignment.TopStart)
                                    .padding(if (isCircular) 8.dp else 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = theme.name,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Float action overlay buttons
                            Row(
                                modifier = Modifier
                                    .align(if (isCircular) Alignment.BottomCenter else Alignment.TopEnd)
                                    .padding(if (isCircular) 8.dp else 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Retake - resets slot and immediately starts active camera
                                IconButton(
                                    onClick = { onClearSlot(index) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Black.copy(alpha = 0.55f)
                                    ),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retake ${theme.name}",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete
                                IconButton(
                                    onClick = { onClearSlot(index) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Red.copy(alpha = 0.75f)
                                    ),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Photo from ${theme.name}",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniLayoutPreview(
    layout: CollageLayout,
    isSelected: Boolean,
    isCircular: Boolean = false
) {
    val size = 28.dp
    val color = if (isSelected) Color.White.copy(alpha = 0.55f) else Color(0xFF03A9F4).copy(alpha = 0.40f)
    val tileShape = if (isCircular) CircleShape else RoundedCornerShape(2.dp)

    Box(
        modifier = Modifier
            .size(size)
            .background(if (isSelected) Color.Transparent else Color(0xFFE3F2FD), RoundedCornerShape(4.dp))
            .border(1.5.dp, if (isSelected) Color.White else Color(0xFF0288D1).copy(alpha = 0.40f), RoundedCornerShape(4.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        when (layout) {
            CollageLayout.TWO_SPLIT_VERTICAL -> {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                }
            }
            CollageLayout.TWO_SPLIT_HORIZONTAL -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                }
            }
            CollageLayout.THREE_GRID -> {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                    }
                }
            }
            CollageLayout.FOUR_GRID -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                    }
                }
            }
            CollageLayout.THREE_ROWS -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                }
            }
            CollageLayout.THREE_COLUMNS -> {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                }
            }
            CollageLayout.FOUR_ROWS -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                }
            }
            CollageLayout.FOUR_COLUMNS -> {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                }
            }
            CollageLayout.FOUR_FEATURED -> {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(tileShape).background(color))
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(tileShape).background(color))
                    }
                }
            }
        }
    }
}

/**
 * Draws custom caption/overlay text onto a high-quality final collage Bitmap on the fly.
 */
fun drawTextOnBitmap(
    original: android.graphics.Bitmap,
    text: String,
    textColor: androidx.compose.ui.graphics.Color,
    textSize: Float,
    positionYPercent: Float,
    isBold: Boolean,
    isItalic: Boolean,
    showBackgroundBubble: Boolean
): android.graphics.Bitmap {
    if (text.isBlank()) return original

    val bitmap = original.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.toArgb()
        style = android.graphics.Paint.Style.FILL
        // Calculate proportionate text size based on bitmap height
        val proportionateSize = bitmap.height * (textSize / 400f)
        this.textSize = proportionateSize

        var typefaceStyle = android.graphics.Typeface.NORMAL
        if (isBold && isItalic) {
            typefaceStyle = android.graphics.Typeface.BOLD_ITALIC
        } else if (isBold) {
            typefaceStyle = android.graphics.Typeface.BOLD
        } else if (isItalic) {
            typefaceStyle = android.graphics.Typeface.ITALIC
        }
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, typefaceStyle)
        textAlign = android.graphics.Paint.Align.CENTER
    }

    val x = bitmap.width / 2f
    val fontMetrics = paint.fontMetrics
    val textHeight = fontMetrics.descent - fontMetrics.ascent
    val y = (bitmap.height * positionYPercent) - (textHeight / 2f) - fontMetrics.ascent

    if (showBackgroundBubble) {
        val textWidth = paint.measureText(text)
        val paddingX = (bitmap.height * (textSize / 400f)) * 0.4f
        val paddingY = (bitmap.height * (textSize / 400f)) * 0.2f
        val rectPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(153, 0, 0, 0) // Black with 60% alpha
            style = android.graphics.Paint.Style.FILL
        }
        val rect = android.graphics.RectF(
            x - (textWidth / 2f) - paddingX,
            y + fontMetrics.ascent - paddingY,
            x + (textWidth / 2f) + paddingX,
            y + fontMetrics.descent + paddingY
        )
        val rx = (bitmap.height * (textSize / 400f)) * 0.25f
        canvas.drawRoundRect(rect, rx, rx, rectPaint)
    }

    canvas.drawText(text, x, y, paint)
    return bitmap
}

