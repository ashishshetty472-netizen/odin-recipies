package com.fylio.app.ui.scanner

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers

private val EnhanceBlue = Color(0xFF2563EB)

/**
 * Enhance + multi-page review.
 *
 * Preview rendering always happens off the main thread, and the previous frame stays on
 * screen while a new one is computed, so dragging a slider never stalls the UI.
 */
@Composable
fun EnhanceScanScreen(
    session: ScanSessionViewModel,
    state: ScanSession,
    onBackToCrop: () -> Unit,
    onAddPage: () -> Unit,
    onSave: (SaveRequest) -> Unit
) {
    // Back from enhance -> crop editor of the active page.
    BackHandler(enabled = true) { onBackToCrop() }

    val pages = state.pages
    if (pages.isEmpty()) {
        BackHandler(enabled = true) { onAddPage() }
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No pages in this session", color = Color.White)
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onAddPage) { Text("Add a page", color = Color(0xFF60A5FA)) }
        }
        return
    }

    val active = state.active ?: pages.first()
    var showAdjust by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<ScannedPage?>(null) }

    // Cached preview keyed by every parameter that affects the render.
    val preview by produceState<Bitmap?>(null, active.id, active.quad, active.rotation, active.filter, active.brightness, active.contrast) {
        value = session.render(active, 1400)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ---------------------------------------------------------------- header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackToCrop) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to crop", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Enhance",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Page ${pages.indexOfFirst { it.id == active.id } + 1} of ${pages.size}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
            IconBubble(onClick = { confirmDelete = active }) {
                Icon(Icons.Outlined.Delete, "Delete page", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(6.dp))
            IconBubble(onClick = { showAdjust = true }) {
                Icon(Icons.Outlined.Tune, "Adjust", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        // ------------------------------------------------------------ main page
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = preview
            if (bmp == null) {
                CircularProgressIndicator(color = EnhanceBlue)
            } else {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Page preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                )
            }
        }

        // ------------------------------------------------------- filter selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScanFilter.entries.forEach { filter ->
                val selected = active.filter == filter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) EnhanceBlue else Color(0xFF1F2937))
                        .clickable { session.update(active.id) { it.copy(filter = filter) } }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        filter.label,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // --------------------------------------------------------- page deck
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(pages, key = { _, p -> p.id }) { index, page ->
                val thumb by produceState<Bitmap?>(null, page.id, page.rotation, page.filter, page.quad) {
                    value = session.render(page, 220)
                }
                val isActive = page.id == active.id
                val isDragging = draggingId == page.id
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1F2937))
                            .border(
                                if (isActive) 2.dp else 1.dp,
                                if (isActive) EnhanceBlue else Color.White.copy(alpha = 0.25f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { session.setActive(page.id) }
                            .pointerInput(page.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggingId = page.id },
                                    onDragEnd = { draggingId = null },
                                    onDragCancel = { draggingId = null },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val from = session.indexOf(page.id)
                                        if (dragAmount.x > 24f && from < pages.lastIndex) {
                                            session.move(from, from + 1)
                                        } else if (dragAmount.x < -24f && from > 0) {
                                            session.move(from, from - 1)
                                        }
                                    }
                                )
                            }
                    ) {
                        thumb?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Page ${index + 1}",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        if (isDragging) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(EnhanceBlue.copy(alpha = 0.35f))
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

                        if (pages.size > 1) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                                    .clickable { confirmDelete = page },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    "Remove",
                                    tint = Color.White,
                                    modifier = Modifier.size(9.dp)
                                )
                            }
                        }
                    }
                }
            }
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1F2937))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onAddPage),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Add, "Add page", tint = Color(0xFF60A5FA), modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // --------------------------------------------------------------- save
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111827))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Long-press a page and drag to reorder",
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(EnhanceBlue)
                    .clickable { showSave = true }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Save, "Save", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Save PDF · ${pages.size} Page${if (pages.size == 1) "" else "s"}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------ adjust sheet
    if (showAdjust) {
        AdjustSheet(
            brightness = active.brightness,
            contrast = active.contrast,
            onBrightness = { v -> session.update(active.id) { it.copy(brightness = v) } },
            onContrast = { v -> session.update(active.id) { it.copy(contrast = v) } },
            onReset = { session.update(active.id) { it.copy(brightness = 0f, contrast = 1f) } },
            onDismiss = { showAdjust = false }
        )
    }

    // -------------------------------------------------------------- save sheet
    if (showSave) {
        SavePdfModal(
            pageCount = pages.size,
            onDismiss = { showSave = false },
            onSave = { request ->
                showSave = false
                onSave(request)
            }
        )
    }
}

data class SaveRequest(
    val fileName: String,
    val quality: PdfQuality,
    val format: PageFormat,
    val share: Boolean,
    /** Persisted SAF tree URI when the user chose a custom folder. */
    val destinationTree: String? = null
)

@Composable
private fun IconBubble(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF1F2937))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Floating adjustment sheet with the spec slider ranges. */
@Composable
private fun AdjustSheet(
    brightness: Float,
    contrast: Float,
    onBrightness: (Float) -> Unit,
    onContrast: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fine tune", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                Row {
                    Text(
                        "Brightness",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        String.format("%+.2f", brightness),
                        style = MaterialTheme.typography.labelMedium,
                        color = EnhanceBlue
                    )
                }
                Slider(
                    value = brightness,
                    onValueChange = onBrightness,
                    valueRange = -1f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = EnhanceBlue,
                        activeTrackColor = EnhanceBlue
                    )
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    Text(
                        "Contrast",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        String.format("%.2f", contrast),
                        style = MaterialTheme.typography.labelMedium,
                        color = EnhanceBlue
                    )
                }
                Slider(
                    value = contrast,
                    onValueChange = onContrast,
                    valueRange = 0.5f..2f,
                    colors = SliderDefaults.colors(
                        thumbColor = EnhanceBlue,
                        activeTrackColor = EnhanceBlue
                    )
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = onReset) { Text("Reset") } }
    )
}
