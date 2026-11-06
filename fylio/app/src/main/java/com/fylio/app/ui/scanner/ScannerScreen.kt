package com.fylio.app.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.fylio.app.data.FylioStore
import com.fylio.app.ui.theme.FylioBgDark
import com.fylio.app.ui.theme.FylioElectricBlue
import com.fylio.app.ui.theme.FylioSurfaceDark
import com.fylio.app.ui.theme.FylioTextSecondary
import com.fylio.app.util.FileOps
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A captured page plus its non-destructive edit state. */
data class ScannedPage(
    val file: File,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val sharpness: Float = 1f,
    val rotation: Int = 0
)

@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onSaved: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    val pages = remember { mutableStateListOf<ScannedPage>() }
    var autoDetectEnabled by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }
    var editingPage by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun applyCameraState() {
        imageCapture.flashMode =
            if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    fun capture() {
        if (!hasCameraPermission || busy) return
        busy = true
        val target = FylioStore.newScanFile(context)
        val options = ImageCapture.OutputFileOptions.Builder(target).build()
        imageCapture.takePicture(
            options,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Compose state must be touched on the main thread.
                    mainHandler.post {
                        if (target.length() > 0) pages.add(ScannedPage(target))
                        busy = false
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    mainHandler.post {
                        target.delete()
                        busy = false
                    }
                }
            }
        )
    }

    fun exportAsPdf() {
        if (pages.isEmpty()) return
        scope.launch {
            busy = true
            val out = withContext(Dispatchers.IO) {
                runCatching {
                    val bitmaps = pages.mapNotNull { page ->
                        val bmp = FileOps.decodeBitmap(page.file, 2400) ?: return@mapNotNull null
                        val adjusted = FileOps.adjust(
                            FileOps.rotate(bmp, page.rotation),
                            page.brightness,
                            page.contrast,
                            page.sharpness
                        )
                        adjusted
                    }
                    if (bitmaps.isEmpty()) return@runCatching null
                    val dir = FylioStore.exportsDir(context)
                    val target = FylioStore.uniqueFile(
                        dir,
                        "Scan_${System.currentTimeMillis()}",
                        "pdf"
                    )
                    FileOps.bitmapsToPdf(bitmaps, target)
                }.getOrNull()
            }
            busy = false
            if (out != null) onSaved(out)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            CameraPreview(
                imageCapture = imageCapture,
                flashOn = flashOn,
                showGuide = autoDetectEnabled
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Camera permission is needed to scan documents",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant permission", color = FylioElectricBlue)
                }
            }
        }

        // ---- Top bar -------------------------------------------------------
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            RoundIconButton(
                onClick = {
                    flashOn = !flashOn
                    applyCameraState()
                }
            ) {
                Icon(
                    if (flashOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    "Flash",
                    tint = if (flashOn) FylioElectricBlue else Color.White
                )
            }
            Spacer(Modifier.width(8.dp))
            // Auto-detect toggle — OFF by default (Phase 1)
            RoundIconButton(
                onClick = { autoDetectEnabled = !autoDetectEnabled },
                background = if (autoDetectEnabled) FylioElectricBlue.copy(alpha = 0.22f)
                else Color.Black.copy(alpha = 0.45f)
            ) {
                Icon(
                    Icons.Outlined.DocumentScanner,
                    "Auto detect",
                    tint = if (autoDetectEnabled) FylioElectricBlue else Color.White
                )
            }
        }

        // ---- Bottom controls ----------------------------------------------
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xB3121824))
                .navigationBarsPadding()
                .padding(top = 12.dp, bottom = 16.dp)
        ) {
            if (pages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(pages) { index, page ->
                        ThumbnailChip(
                            page = page,
                            index = index,
                            onClick = { editingPage = index },
                            onDelete = { showDeleteConfirm = index }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${pages.size} ${if (pages.size == 1) "page" else "pages"}",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        if (pages.isNotEmpty()) {
                            Text(
                                "Tap a page to adjust",
                                color = FylioTextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                ShutterButton(busy = busy, onClick = { capture() })

                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (pages.isNotEmpty()) {
                        TextButton(onClick = { exportAsPdf() }, enabled = !busy) {
                            Text(
                                if (busy) "Saving…" else "Save PDF",
                                color = FylioElectricBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ---- Page editor ---------------------------------------------------
        editingPage?.let { index ->
            val page = pages.getOrNull(index) ?: return@let
            ScannedPageEditorView(
                page = page,
                index = index,
                total = pages.size,
                onDismiss = { editingPage = null },
                onUpdate = { updated -> pages[index] = updated },
                onRequestDelete = { showDeleteConfirm = index }
            )
        }

        // ---- Delete confirmation ------------------------------------------
        showDeleteConfirm?.let { index ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("Delete page?") },
                text = { Text("Page ${index + 1} will be removed from this scan.") },
                confirmButton = {
                    TextButton(onClick = {
                        pages.getOrNull(index)?.file?.delete()
                        pages.removeAt(index)
                        showDeleteConfirm = null
                        editingPage = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            pages.forEach { /* files stay on disk; volume cleanup happens in settings later */ }
        }
    }
}

@Composable
private fun RoundIconButton(
    onClick: () -> Unit,
    background: Color = Color.Black.copy(alpha = 0.45f),
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun CameraPreview(
    imageCapture: ImageCapture,
    flashOn: Boolean,
    showGuide: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner, flashOn) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture.flashMode =
                if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose { runCatching { cameraProviderFuture.get().unbindAll() } }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        if (showGuide) DocumentGuide()
    }
}

/**
 * Subtle document guide (Phase 1): thin stroke at 35% alpha plus small corner
 * brackets instead of the old thick blue frame.
 */
@Composable
private fun DocumentGuide() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp, vertical = 120.dp)
    ) {
        val stroke = 2f
        val white = Color.White.copy(alpha = 0.55f)
        val blue = FylioElectricBlue.copy(alpha = 0.75f)
        val inset = 1f

        drawRoundRect(
            color = white.copy(alpha = 0.35f),
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f),
            style = Stroke(width = stroke)
        )

        val seg = 34f
        val outer = 6f
        val inner = 3.5f
        // Top-left
        drawLine(white, Offset(0f, 0f), Offset(seg, 0f), outer)
        drawLine(white, Offset(0f, 0f), Offset(0f, seg), outer)
        drawLine(blue, Offset(0f, 0f), Offset(seg * 0.62f, 0f), inner)
        drawLine(blue, Offset(0f, 0f), Offset(0f, seg * 0.62f), inner)
        // Top-right
        drawLine(white, Offset(size.width, 0f), Offset(size.width - seg, 0f), outer)
        drawLine(white, Offset(size.width, 0f), Offset(size.width, seg), outer)
        drawLine(blue, Offset(size.width, 0f), Offset(size.width - seg * 0.62f, 0f), inner)
        drawLine(blue, Offset(size.width, 0f), Offset(size.width, seg * 0.62f), inner)
        // Bottom-left
        drawLine(white, Offset(0f, size.height), Offset(seg, size.height), outer)
        drawLine(white, Offset(0f, size.height), Offset(0f, size.height - seg), outer)
        drawLine(blue, Offset(0f, size.height), Offset(seg * 0.62f, size.height), inner)
        drawLine(blue, Offset(0f, size.height), Offset(0f, size.height - seg * 0.62f), inner)
        // Bottom-right
        drawLine(white, Offset(size.width, size.height), Offset(size.width - seg, size.height), outer)
        drawLine(white, Offset(size.width, size.height), Offset(size.width, size.height - seg), outer)
        drawLine(blue, Offset(size.width, size.height), Offset(size.width - seg * 0.62f, size.height), inner)
        drawLine(blue, Offset(size.width, size.height), Offset(size.width, size.height - seg * 0.62f), inner)
    }
}

/** 84dp outer / 58dp inner shutter with a spring press animation. */
@Composable
private fun ShutterButton(busy: Boolean, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(FylioElectricBlue.copy(alpha = 0.16f))
            .clickable(enabled = !busy) {
                scope.launch {
                    scale.animateTo(
                        0.88f,
                        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium * 2f)
                    )
                    delay(120)
                    scale.animateTo(1f, tween(durationMillis = 200))
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(if (busy) Color(0xFFB9C4D2) else Color.White)
                .border(1.dp, Color(0xFFE2E8F0), CircleShape)
        )
    }
}

@Composable
private fun ThumbnailChip(
    page: ScannedPage,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(page.file.absolutePath, page.file.lastModified(), page.rotation) {
        FileOps.decodeBitmap(page.file, 240)?.let { FileOps.rotate(it, page.rotation) }
    }
    Box {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(FylioSurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Page ${index + 1}",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text("${index + 1}", color = Color.White, fontSize = 9.sp)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Close, "Remove", tint = Color.White, modifier = Modifier.size(11.dp))
        }
    }
}

/** Page editor: brightness / contrast / sharpness sliders + rotate + delete. */
@Composable
private fun ScannedPageEditorView(
    page: ScannedPage,
    index: Int,
    total: Int,
    onDismiss: () -> Unit,
    onUpdate: (ScannedPage) -> Unit,
    onRequestDelete: () -> Unit
) {
    val preview = remember(
        page.file.absolutePath,
        page.rotation,
        page.brightness,
        page.contrast,
        page.sharpness
    ) {
        FileOps.decodeBitmap(page.file, 700)?.let { bmp ->
            FileOps.adjust(
                FileOps.rotate(bmp, page.rotation),
                page.brightness,
                page.contrast,
                page.sharpness
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2121824))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, "Close", tint = Color.White)
                }
                Text(
                    "Page ${index + 1} of $total",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRequestDelete) {
                    Icon(Icons.Outlined.Delete, "Delete page", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = {
                    onUpdate(page.copy(rotation = (page.rotation + 90) % 360))
                }) {
                    Icon(Icons.Outlined.Edit, "Rotate 90°", tint = FylioElectricBlue)
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = "Scan preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(preview.width.toFloat() / preview.height.toFloat())
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FylioSurfaceDark)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                AdjustSlider(
                    label = "Brightness",
                    value = page.brightness,
                    range = 0.6f..1.4f,
                    onChange = { onUpdate(page.copy(brightness = it)) }
                )
                AdjustSlider(
                    label = "Contrast",
                    value = page.contrast,
                    range = 0.8f..1.8f,
                    onChange = { onUpdate(page.copy(contrast = it)) }
                )
                AdjustSlider(
                    label = "Sharpness",
                    value = page.sharpness,
                    range = 0.5f..1.5f,
                    onChange = { onUpdate(page.copy(sharpness = it)) }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onUpdate(page.copy(brightness = 1f, contrast = 1f, sharpness = 1f))
                    }) { Text("Reset", color = FylioTextSecondary) }
                    TextButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Check, null, tint = FylioElectricBlue)
                        Spacer(Modifier.width(4.dp))
                        Text("Done", color = FylioElectricBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(
                String.format("%.2f", value),
                color = FylioTextSecondary,
                fontSize = 11.sp
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = FylioElectricBlue,
                activeTrackColor = FylioElectricBlue,
                inactiveTrackColor = FylioBgDark
            )
        )
    }
}
