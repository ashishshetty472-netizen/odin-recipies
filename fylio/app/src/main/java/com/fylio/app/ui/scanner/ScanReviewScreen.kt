package com.fylio.app.ui.scanner

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.core.image.ImageEngine
import com.fylio.app.core.pdf.PdfEngine
import com.fylio.app.data.FylioStore
import com.fylio.app.ui.components.FylioCard
import com.fylio.app.ui.components.FylioIcon
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.FylioDimens
import com.fylio.app.ui.theme.LocalFylioSurfaces
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Review + export step after scanning: reorder, apply the colour mode to every page,
 * then write a real multi-page PDF with the chosen page size and quality.
 */
@Composable
fun ScanReviewScreen(
    pages: List<String>,
    onBack: () -> Unit,
    onSaved: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val surfaces = LocalFylioSurfaces.current

    val files = remember(pages) { mutableStateListOf(*pages.map { File(it) }.toTypedArray()) }
    var index by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf(ImageEngine.ColorMode.MAGIC) }
    var applyToAll by remember { mutableStateOf(true) }
    var fileName by remember { mutableStateOf("Scan_${System.currentTimeMillis() / 1000}") }
    var a4 by remember { mutableStateOf(true) }
    var quality by remember { mutableIntStateOf(85) }
    var busy by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Int?>(null) }

    val file = files.getOrNull(index)
    val bitmap = remember(file?.absolutePath, mode) {
        file?.let { f ->
            ImageEngine.decode(f, 1600)?.let { ImageEngine.enhance(it, mode = mode) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaces.headerWarm)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ Back", color = ElectricBlue) }
            Column(Modifier.weight(1f)) {
                Text(
                    "Review scan",
                    style = MaterialTheme.typography.titleMedium,
                    color = surfaces.textPrimary
                )
                Text(
                    "${files.size} page(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaces.textSecondary
                )
            }
            if (file != null) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(surfaces.elevated)
                        .clickable { pendingDelete = index }
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        "Delete page",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = FylioDimens.screenPadding, vertical = 8.dp)
        ) {
            val bmp = bitmap
            if (bmp == null) {
                CircularProgressIndicator(color = ElectricBlue, modifier = Modifier.align(Alignment.Center))
            } else {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Page ${index + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, surfaces.cardBorder, RoundedCornerShape(12.dp))
                        .align(Alignment.Center)
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = FylioDimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(files) { i, f ->
                val thumb: Bitmap? = remember(f.absolutePath) { ImageEngine.decode(f, 200) }
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaces.card)
                        .border(
                            if (i == index) 2.dp else 1.dp,
                            if (i == index) ElectricBlue else surfaces.cardBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { index = i }
                ) {
                    thumb?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = FylioDimens.screenPadding, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(
                    ImageEngine.ColorMode.ORIGINAL to "Original",
                    ImageEngine.ColorMode.MAGIC to "Magic",
                    ImageEngine.ColorMode.GRAYSCALE to "Gray",
                    ImageEngine.ColorMode.BLACK_WHITE to "B&W"
                ).forEach { (m, label) ->
                    val active = mode == m
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (active) ElectricBlue else surfaces.card)
                            .clickable { mode = m }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            label,
                            fontSize = 10.sp,
                            color = if (active) Color.White else surfaces.textSecondary
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("All pages", fontSize = 9.sp, color = surfaces.textSecondary)
                Spacer(Modifier.width(4.dp))
                Switch(
                    checked = applyToAll,
                    onCheckedChange = { applyToAll = it },
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            FylioCard(padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        singleLine = true,
                        label = { Text("File name", fontSize = 10.sp) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf(true to "A4", false to "Fit image").forEach { (isA4, label) ->
                        val active = a4 == isA4
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (active) ElectricBlue else surfaces.elevated)
                                .clickable { a4 = isA4 }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 10.sp,
                                color = if (active) Color.White else surfaces.textPrimary
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Quality $quality%", fontSize = 10.sp, color = surfaces.textSecondary)
                }
                androidx.compose.material3.Slider(
                    value = quality.toFloat(),
                    onValueChange = { quality = it.toInt() },
                    valueRange = 50f..95f
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (busy || files.isEmpty()) surfaces.elevated else ElectricBlue)
                    .clickable(enabled = !busy && files.isNotEmpty()) {
                        busy = true
                        scope.launch {
                            val target = withContext(Dispatchers.IO) {
                                FylioStore.uniqueFile(
                                    FylioStore.scansDir(context),
                                    fileName.ifBlank { "Scan" },
                                    "pdf"
                                )
                            }
                            val result = withContext(Dispatchers.IO) {
                                val bitmaps = files.mapNotNull { f ->
                                    ImageEngine.decode(f, 2400)?.let { ImageEngine.enhance(it, mode = mode) }
                                }
                                if (bitmaps.isEmpty()) return@withContext null
                                val pageSize = if (a4) com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4 else null
                                PdfEngine.bitmapsToPdf(bitmaps, target, quality, pageSize)
                            }
                            busy = false
                            if (result != null) {
                                files.forEach { it.delete() }
                                onSaved(result)
                            }
                        }
                    }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = ElectricBlue,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Building PDF…", color = surfaces.textPrimary, fontWeight = FontWeight.SemiBold)
                } else {
                    FylioIcon("save_pdf", size = 18.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Save PDF · ${files.size} Pages",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    pendingDelete?.let { i ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete page ${i + 1}?") },
            confirmButton = {
                TextButton(onClick = {
                    files.getOrNull(i)?.delete()
                    files.removeAt(i)
                    index = index.coerceAtMost((files.size - 1).coerceAtLeast(0))
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}
