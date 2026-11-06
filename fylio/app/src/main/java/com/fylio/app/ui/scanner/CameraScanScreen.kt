package com.fylio.app.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/** Electric blue used by every scanner overlay. */
private val ScanBlue = Color(0xFF2563EB)

/**
 * Full-screen camera for capturing pages.
 *
 * The viewfinder is completely clean unless auto-detection is switched on, in which case
 * analysed frames drive a translucent polygon that snaps to the detected document.
 */
@Composable
fun CameraScanScreen(
    session: ScanSessionViewModel,
    state: ScanSession,
    onBack: () -> Unit,
    onReviewPage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    var autoDetect by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    // Camera back button -> Home (not app exit).
    BackHandler(enabled = true) { onBack() }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
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
    ) { granted ->
        hasPermission = granted
        if (!granted) toast = "Camera permission is needed to scan"
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            importing = true
            scope.launch {
                val added = session.addImages(uris)
                importing = false
                toast = if (added > 0) "Imported $added page(s)" else "Could not import those images"
            }
        }
    }

    fun capture() {
        if (!hasPermission || capturing) return
        capturing = true
        val target = FylioStoreFile.newCapture(context)
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(target).build(),
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    mainHandler.post {
                        if (target.length() == 0L) {
                            capturing = false
                            toast = "Capture failed"
                            return@post
                        }
                        scope.launch {
                            val bitmap = withContext(Dispatchers.IO) {
                                ScanSessionViewModel.decodeUpright(target, 2600)
                            }
                            if (bitmap != null) {
                                val page = session.addBitmap(
                                    bitmap,
                                    if (autoDetect) state.detectedQuad else null
                                )
                                bitmap.recycle()
                                if (page == null) toast = "Could not save that page"
                            } else {
                                toast = "Could not read that capture"
                            }
                            target.delete()
                            capturing = false
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    mainHandler.post {
                        target.delete()
                        capturing = false
                        toast = "Capture failed: ${exception.message}"
                    }
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
            CameraPreview(
                imageCapture = imageCapture,
                flashOn = flashOn,
                autoDetect = autoDetect,
                onQuad = { session.setDetectedQuad(it) }
            )
        } else {
            PermissionPrompt { permissionLauncher.launch(Manifest.permission.CAMERA) }
        }

        // Detected-document polygon: only drawn while auto-detection is ON.
        if (autoDetect) {
            val quad = state.detectedQuad
            Canvas(Modifier.fillMaxSize()) {
                if (quad != null) {
                    val path = Path().apply {
                        moveTo(quad.tlX * size.width, quad.tlY * size.height)
                        lineTo(quad.trX * size.width, quad.trY * size.height)
                        lineTo(quad.brX * size.width, quad.brY * size.height)
                        lineTo(quad.blX * size.width, quad.blY * size.height)
                        close()
                    }
                    // #2563EB at 20% fill
                    drawPath(path, ScanBlue.copy(alpha = 0.20f))
                    drawPath(path, ScanBlue, style = Stroke(width = 2.5f))
                    quad.corners().forEach { (x, y) ->
                        drawCircle(Color.White, radius = 6f, center = Offset(x * size.width, y * size.height))
                        drawCircle(ScanBlue, radius = 6f, center = Offset(x * size.width, y * size.height), style = Stroke(2f))
                    }
                } else {
                    // subtle corner brackets hint while nothing is detected
                    val w = 26f
                    val m = 40f
                    val c = Color.White.copy(alpha = 0.45f)
                    val sw = 2f
                    drawLine(c, Offset(m, m), Offset(m + w, m), sw)
                    drawLine(c, Offset(m, m), Offset(m, m + w), sw)
                    drawLine(c, Offset(size.width - m, m), Offset(size.width - m - w, m), sw)
                    drawLine(c, Offset(size.width - m, m), Offset(size.width - m, m + w), sw)
                    drawLine(c, Offset(m, size.height - m), Offset(m + w, size.height - m), sw)
                    drawLine(c, Offset(m, size.height - m), Offset(m, size.height - m - w), sw)
                    drawLine(c, Offset(size.width - m, size.height - m), Offset(size.width - m - w, size.height - m), sw)
                    drawLine(c, Offset(size.width - m, size.height - m), Offset(size.width - m, size.height - m - w), sw)
                }
            }
        }

        // ---------------------------------------------------------------- top bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Scan", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (state.pages.isEmpty()) "No pages yet"
                    else "${state.pages.size} page(s) captured",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp
                )
            }
            AutoDetectPill(autoDetect) {
                autoDetect = !autoDetect
                if (!autoDetect) session.setDetectedQuad(null)
            }
            Spacer(Modifier.width(8.dp))
            GlassButton(onClick = {
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

        toast?.let { message ->
            LaunchedEffect(message) {
                delay(2400)
                toast = null
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 84.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(message, color = Color.White, fontSize = 11.sp)
            }
        }

        // ------------------------------------------------------------- bottom dock
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.70f))
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 16.dp)
        ) {
            if (state.pages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.pages) { index, page ->
                        PageChip(
                            page = page,
                            index = index,
                            onClick = { onReviewPage(page.id) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // gallery import
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !importing) { galleryLauncher.launch("image/*") }
                            .padding(8.dp)
                    ) {
                        if (importing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Outlined.PhotoLibrary,
                                "Import",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Gallery", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
                    }
                }

                ShutterButton(busy = capturing || !hasPermission) { capture() }

                // page count pill / next
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (state.pages.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Check,
                                null,
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Review", color = Color.White.copy(alpha = 0.35f), fontSize = 9.sp)
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(ScanBlue)
                                .clickable {
                                    state.pages.firstOrNull()?.let { onReviewPage(it.id) }
                                }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Text(
                                "${state.pages.size} page${if (state.pages.size == 1) "" else "s"}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Review", color = Color.White.copy(alpha = 0.85f), fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }
}

/** Thin wrapper so the capture file naming lives with the session code. */
private object FylioStoreFile {
    fun newCapture(context: android.content.Context) =
        com.fylio.app.data.FylioStore.newScanFile(context, "Capture")
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(46.dp))
        Spacer(Modifier.height(14.dp))
        Text(
            "Camera access is needed to scan pages",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Fylio only uses the camera while the scanner is open.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onRequest) {
            Text("Grant permission", color = Color(0xFF60A5FA), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GlassButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun AutoDetectPill(enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(28.dp)
                .height(16.dp)
                .clip(CircleShape)
                .background(if (enabled) ScanBlue else Color.White.copy(alpha = 0.3f))
                .padding(2.dp),
            contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            "Auto",
            color = if (enabled) Color(0xFF93C5FD) else Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ShutterButton(busy: Boolean, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.75f), CircleShape)
            .clickable(enabled = !busy) {
                scope.launch {
                    scale.animateTo(0.88f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium * 3))
                    delay(90)
                    scale.animateTo(1f, tween(170))
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(54.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(if (busy) Color(0xFFCBD5E1) else Color.White)
        )
    }
}

@Composable
private fun PageChip(page: ScannedPage, index: Int, onClick: () -> Unit) {
    val thumb = remember(page.id, page.sourceFile.lastModified(), page.rotation) {
        ScanSessionViewModel.decodeUpright(page.sourceFile, 200)?.let {
            if (page.rotation % 360 == 0) it else com.fylio.app.core.image.ImageEngine.rotate(it, page.rotation)
        }
    }
    Box(
        Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1F2937))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        thumb?.let {
            Image(
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
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(horizontal = 4.dp)
        ) {
            Text("${index + 1}", color = Color.White, fontSize = 8.sp)
        }
    }
}

/**
 * CameraX bindings.
 *
 * The analysis pipeline is only attached while auto-detection is enabled, so the clean
 * viewfinder costs nothing when the feature is off.
 */
@Composable
private fun CameraPreview(
    imageCapture: ImageCapture,
    flashOn: Boolean,
    autoDetect: Boolean,
    onQuad: (ScanQuad) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    // One executor + analyzer for the lifetime of the screen: recreating them in the
    // DisposableEffect below would close a detector that CameraX still holds.
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember { DocumentAnalyzer { quad -> onQuad(quad) } }
    val currentOnQuad = rememberUpdatedState(onQuad)
    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            analysisExecutor.shutdown()
        }
    }

    DisposableEffect(lifecycleOwner, flashOn, autoDetect) {
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = runCatching { future.get() }.getOrNull() ?: return@Runnable
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture.flashMode =
                if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            val analysis = if (autoDetect) {
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(android.util.Size(1280, 720))
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, analyzer) }
            } else null

            runCatching {
                provider.unbindAll()
                if (analysis != null) {
                    provider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageCapture, analysis
                    )
                } else {
                    provider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageCapture
                    )
                }
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { future.get().unbindAll() }
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}
