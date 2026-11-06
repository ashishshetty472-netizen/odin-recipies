package com.fylio.app.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.fylio.app.core.image.ImageEngine
import com.fylio.app.data.FylioStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/** One captured page plus its non-destructive edit state. */
data class ScanPage(
    val file: File,
    val rotation: Int = 0,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val mode: ImageEngine.ColorMode = ImageEngine.ColorMode.ORIGINAL,
    val crop: FloatArray? = null   // left, top, right, bottom (0..1)
)

@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onFinish: (pages: List<File>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    val pages = remember { mutableStateListOf<ScanPage>() }
    var autoDetect by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun capture() {
        if (!hasPermission || busy) return
        busy = true
        val target = FylioStore.newScanFile(context)
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(target).build(),
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) =
                    mainHandler.post {
                        if (target.length() > 0) pages.add(ScanPage(target))
                        busy = false
                    }

                override fun onError(exception: ImageCaptureException) = mainHandler.post {
                    target.delete(); busy = false
                }
            }
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            CameraPreview(imageCapture, flashOn, autoDetect)
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(12.dp))
                Text("Camera access is needed to scan", color = Color.White)
                TextButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant permission", color = Color(0xFF60A5FA))
                }
            }
        }

        // top bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            // Auto-detection toggle — OFF by default
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { autoDetect = !autoDetect }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.DocumentScanner,
                    "Auto detection",
                    tint = if (autoDetect) Color(0xFF60A5FA) else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Auto-detection",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .width(30.dp)
                        .height(17.dp)
                        .clip(CircleShape)
                        .background(if (autoDetect) Color(0xFF2563EB) else Color.White.copy(alpha = 0.35f))
                        .padding(2.dp),
                    contentAlignment = if (autoDetect) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            RoundButton(onClick = {
                flashOn = !flashOn
                imageCapture.flashMode =
                    if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            }) {
                Icon(
                    if (flashOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    "Flash",
                    tint = if (flashOn) Color(0xFF60A5FA) else Color.White
                )
            }
        }

        // bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.72f))
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 14.dp)
        ) {
            if (pages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(pages) { i, page ->
                        PageThumb(
                            page = page,
                            index = i,
                            onClick = { editing = i },
                            onDelete = { confirmDelete = i }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (pages.isNotEmpty()) {
                        TextButton(onClick = { onFinish(pages.map { it.file }) }) {
                            Text("Next", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                ShutterButton(busy) { capture() }

                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (pages.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .clickable { editing = pages.lastIndex }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${pages.size}",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text("pages", color = Color(0xFF64748B), fontSize = 8.sp)
                            }
                        }
                    }
                }
            }
        }

        // page editor
        editing?.let { idx ->
            pages.getOrNull(idx)?.let { page ->
                ScanEditorOverlay(
                    page = page,
                    index = idx,
                    total = pages.size,
                    onDismiss = { editing = null },
                    onUpdate = { pages[idx] = it },
                    onDelete = { confirmDelete = idx }
                )
            }
        }

        confirmDelete?.let { idx ->
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                title = { Text("Delete page?") },
                text = { Text("Page ${idx + 1} will be removed from this scan.") },
                confirmButton = {
                    TextButton(onClick = {
                        pages.getOrNull(idx)?.file?.delete()
                        pages.removeAt(idx)
                        confirmDelete = null
                        editing = null
                    }) { Text("Delete", color = Color(0xFFEF4444)) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }
}

@Composable
private fun RoundButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun CameraPreview(imageCapture: ImageCapture, flashOn: Boolean, showGuide: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner, flashOn) {
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture.flashMode =
                if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose { runCatching { future.get().unbindAll() } }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        if (showGuide) DocumentGuide()
    }
}

/** Thin corner-bracket framing guide, only visible when auto-detection is on. */
@Composable
private fun DocumentGuide() {
    Canvas(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp, vertical = 130.dp)
    ) {
        val white = Color.White.copy(alpha = 0.6f)
        val len = 30f
        val w = 2.5f
        drawRect(
            color = white.copy(alpha = 0.25f),
            size = size,
            style = Stroke(width = 1.5f)
        )
        // corner brackets
        drawLine(white, Offset(0f, 0f), Offset(len, 0f), w)
        drawLine(white, Offset(0f, 0f), Offset(0f, len), w)
        drawLine(white, Offset(size.width, 0f), Offset(size.width - len, 0f), w)
        drawLine(white, Offset(size.width, 0f), Offset(size.width, len), w)
        drawLine(white, Offset(0f, size.height), Offset(len, size.height), w)
        drawLine(white, Offset(0f, size.height), Offset(0f, size.height - len), w)
        drawLine(white, Offset(size.width, size.height), Offset(size.width - len, size.height), w)
        drawLine(white, Offset(size.width, size.height), Offset(size.width, size.height - len), w)
    }
}

@Composable
private fun ShutterButton(busy: Boolean, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .size(74.dp)
            .clip(CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
            .clickable(enabled = !busy) {
                scope.launch {
                    scale.animateTo(0.9f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium * 2))
                    delay(110)
                    scale.animateTo(1f, tween(180))
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(56.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(if (busy) Color(0xFFCBD5E1) else Color.White)
        )
    }
}

@Composable
private fun PageThumb(page: ScanPage, index: Int, onClick: () -> Unit, onDelete: () -> Unit) {
    val bitmap = remember(page.file.absolutePath, page.file.lastModified(), page.rotation) {
        ImageEngine.decode(page.file, 220)?.let { ImageEngine.rotate(it, page.rotation) }
    }
    Box {
        Box(
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1F2937))
                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
        ) {
            bitmap?.let {
                androidx.compose.foundation.Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Page ${index + 1}",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp)
            ) { Text("${index + 1}", color = Color.White, fontSize = 8.sp) }
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(17.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Close, "Remove", tint = Color.White, modifier = Modifier.size(10.dp)) }
    }
}
