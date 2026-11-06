package com.fylio.app.ui.viewer

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.data.FileKind
import com.fylio.app.data.FylioFile
import com.fylio.app.data.FylioTools
import com.fylio.app.ui.components.FylioToolIcon
import com.fylio.app.ui.components.FylioTag
import com.fylio.app.ui.theme.FylioBgDark
import com.fylio.app.ui.theme.FylioElectricBlue
import com.fylio.app.ui.theme.FylioSurfaceDark
import com.fylio.app.ui.theme.FylioTextPrimary
import com.fylio.app.ui.theme.FylioTextSecondary
import com.fylio.app.util.FileOps
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ViewerScreen(
    item: FylioFile,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FylioBgDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = FylioTextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = FylioTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = "${item.kind.label} · ${item.sizeLabel}",
                    color = FylioTextSecondary,
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, "Share", tint = FylioTextPrimary)
            }
            IconButton(onClick = { showRename = true }) {
                Icon(Icons.Outlined.UnfoldMore, "Rename", tint = FylioTextPrimary)
            }
            IconButton(onClick = { showDelete = true }) {
                Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (item.kind) {
                FileKind.PDF -> PdfViewer(item.file)
                FileKind.IMAGE -> ImageViewer(item.file)
                FileKind.TEXT -> TextViewer(item.file)
                else -> DocumentPreview(item)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FylioTag(item.kind.label, item.kind.accent)
            FylioTag("${item.sizeLabel}")
            Spacer(Modifier.weight(1f))
            Text(
                "Tap the share icon to open in another app",
                color = FylioTextSecondary,
                fontSize = 9.sp
            )
        }
    }

    if (showRename) {
        var name by remember { mutableStateOf(item.name) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename document") },
            text = {
                TextField(value = name, onValueChange = { name = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    showRename = false
                    onRename(name)
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete document?") },
            text = { Text("${item.name} will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }
}

/** Renders PDF pages one by one with PdfRenderer (no third-party dependency). */
@Composable
private fun PdfViewer(file: File) {
    var reloadKey by remember { mutableIntStateOf(0) }
    val pageCount = remember(file.absolutePath, reloadKey) {
        runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { it.pageCount }
            }
        }.getOrDefault(0)
    }
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }

    if (pageCount == 0) {
        EmptyState("This PDF has no readable pages", "save_pdf")
        return
    }

    val page = produceState<Bitmap?>(initialValue = null, file.absolutePath, pageIndex, reloadKey) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val safeIndex = pageIndex.coerceIn(0, renderer.pageCount - 1)
                        renderer.openPage(safeIndex).use { p ->
                            val scale = 2f
                            val bmp = Bitmap.createBitmap(
                                (p.width * scale).toInt().coerceAtLeast(1),
                                (p.height * scale).toInt().coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888
                            )
                            // White background — PDF pages are transparent by default.
                            bmp.eraseColor(AndroidColor.WHITE)
                            p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp
                        }
                    }
                }
            }.getOrNull()
        }
    }.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(androidx.compose.ui.graphics.Color(0xFF0B0E14)),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = page
            if (bitmap == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = FylioElectricBlue)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { reloadKey++ }) {
                        Text("Taking long? Retry", color = FylioTextSecondary, fontSize = 11.sp)
                    }
                }
            } else {
                ZoomableImage(bitmap)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(
                onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                enabled = pageIndex > 0
            ) { Text("Prev", color = if (pageIndex > 0) FylioElectricBlue else FylioTextSecondary) }
            Text(
                " ${pageIndex + 1} / $pageCount ",
                color = FylioTextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
            TextButton(
                onClick = { pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1) },
                enabled = pageIndex < pageCount - 1
            ) {
                Text(
                    "Next",
                    color = if (pageIndex < pageCount - 1) FylioElectricBlue else FylioTextSecondary
                )
            }
        }
    }
}

@Composable
private fun ImageViewer(file: File) {
    val bitmap = remember(file.absolutePath, file.lastModified()) {
        FileOps.decodeBitmap(file, 2200)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFF0B0E14)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap == null) {
            EmptyState("Could not decode this image", "gallery")
        } else {
            ZoomableImage(bitmap)
        }
    }
}

@Composable
private fun ZoomableImage(bitmap: Bitmap) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
    )
}

@Composable
private fun TextViewer(file: File) {
    val text = remember(file.absolutePath) {
        runCatching { file.readText().take(200_000) }.getOrDefault("Could not read this file.")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = text,
            color = FylioTextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

/**
 * Office formats can't be rendered natively without an engine, so Fylio shows a
 * structured preview card and hands the file off to another app for full fidelity.
 */
@Composable
private fun DocumentPreview(item: FylioFile) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FylioToolIcon(FylioTools.iconForFile(item), size = 72.dp)
        Spacer(Modifier.height(14.dp))
        Text(
            text = item.name,
            color = FylioTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${item.kind.label} document · ${item.sizeLabel}",
            color = FylioTextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(FylioSurfaceDark)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                "Inline rendering for ${item.kind.label} arrives with the Phase 2 conversion engine. " +
                    "Open it in another app for now.",
                color = FylioTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(260.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = { FileOps.openWith(context, item.file) }) {
            Icon(Icons.Outlined.Lock, null, tint = FylioElectricBlue, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Open with another app", color = FylioElectricBlue)
        }
    }
}

@Composable
private fun EmptyState(message: String, iconId: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FylioToolIcon(iconId, size = 52.dp)
        Spacer(Modifier.height(10.dp))
        Text(message, color = FylioTextSecondary, style = MaterialTheme.typography.bodyLarge)
    }
}

