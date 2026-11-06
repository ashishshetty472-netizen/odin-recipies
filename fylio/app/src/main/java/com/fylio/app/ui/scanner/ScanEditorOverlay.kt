package com.fylio.app.ui.scanner

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.core.image.ImageEngine
import com.fylio.app.ui.components.FylioIcon

/**
 * Full-screen scan page editor: crop, rotate, colour mode, brightness/contrast.
 * Edits stay non-destructive — the source JPEG is only rewritten on export.
 */
@Composable
fun ScanEditorOverlay(
    page: ScanPage,
    index: Int,
    total: Int,
    onDismiss: () -> Unit,
    onUpdate: (ScanPage) -> Unit,
    onDelete: () -> Unit
) {
    var mode by remember { mutableStateOf(page.mode) }
    var brightness by remember { mutableFloatStateOf(page.brightness) }
    var contrast by remember { mutableFloatStateOf(page.contrast) }
    var rotation by remember { mutableStateOf(page.rotation) }
    var cropMode by remember { mutableStateOf(false) }
    // crop rect in normalised coordinates
    var cropL by remember { mutableFloatStateOf(0.06f) }
    var cropT by remember { mutableFloatStateOf(0.06f) }
    var cropR by remember { mutableFloatStateOf(0.94f) }
    var cropB by remember { mutableFloatStateOf(0.94f) }

    val preview = remember(page.file.absolutePath, rotation, brightness, contrast, mode) {
        ImageEngine.decode(page.file, 900)?.let { src ->
            val rotated = ImageEngine.rotate(src, rotation)
            ImageEngine.enhance(rotated, brightness, contrast, mode)
        }
    }

    fun commit(close: Boolean) {
        val crop = if (cropMode) floatArrayOf(cropL, cropT, cropR, cropB) else page.crop
        onUpdate(
            page.copy(
                rotation = rotation,
                brightness = brightness,
                contrast = contrast,
                mode = mode,
                crop = crop
            )
        )
        if (close) onDismiss()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            // header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { commit(true) }) {
                    Icon(Icons.Outlined.Close, "Close", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Edit Scan",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Page ${index + 1} of $total",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, "Delete page", tint = Color(0xFFEF4444))
                }
                IconButton(onClick = { rotation = (rotation + 90) % 360 }) {
                    Icon(Icons.Outlined.RotateRight, "Rotate", tint = Color(0xFF60A5FA))
                }
                IconButton(onClick = { cropMode = !cropMode }) {
                    Icon(
                        Icons.Outlined.Crop,
                        "Crop",
                        tint = if (cropMode) Color(0xFF60A5FA) else Color.White
                    )
                }
            }

            // preview
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val bmp = preview
                if (bmp != null) {
                    Box {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                        )
                        if (cropMode) {
                            // crop selection overlay
                            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                val l = cropL * size.width
                                val t = cropT * size.height
                                val r = cropR * size.width
                                val b = cropB * size.height
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    size = androidx.compose.ui.geometry.Size(size.width, t)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, b),
                                    size = androidx.compose.ui.geometry.Size(size.width, size.height - b)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, t),
                                    size = androidx.compose.ui.geometry.Size(l, b - t)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    topLeft = androidx.compose.ui.geometry.Offset(r, t),
                                    size = androidx.compose.ui.geometry.Size(size.width - r, b - t)
                                )
                                drawRect(
                                    color = Color.White,
                                    topLeft = androidx.compose.ui.geometry.Offset(l, t),
                                    size = androidx.compose.ui.geometry.Size(r - l, b - t),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(2f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // colour modes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImageEngine.ColorMode.entries.forEach { m ->
                    val selected = mode == m
                    val label = when (m) {
                        ImageEngine.ColorMode.ORIGINAL -> "Original"
                        ImageEngine.ColorMode.MAGIC -> "Magic"
                        ImageEngine.ColorMode.GRAYSCALE -> "Grayscale"
                        ImageEngine.ColorMode.BLACK_WHITE -> "B&W"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) Color(0xFF2563EB) else Color(0xFF1F2937)
                            )
                            .clickable { mode = m }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }
            }

            // sliders
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF111827))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                SliderRow("Brightness", brightness, 0.6f..1.5f) { brightness = it }
                SliderRow("Contrast", contrast, 0.7f..1.8f) { contrast = it }
                if (cropMode) {
                    SliderRow("Crop left", cropL, 0f..0.45f) { cropL = it.coerceAtMost(cropR - 0.1f) }
                    SliderRow("Crop right", cropR, 0.55f..1f) { cropR = it.coerceAtLeast(cropL + 0.1f) }
                }
            }

            // actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { commit(false) }) {
                    Text("Apply", color = Color(0xFF94A3B8))
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2563EB))
                        .clickable { commit(true) }
                        .padding(horizontal = 22.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Check,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Done",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 10.sp, modifier = Modifier.width(70.dp))
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF60A5FA),
                activeTrackColor = Color(0xFF60A5FA),
                inactiveTrackColor = Color(0xFF374151)
            )
        )
        Text(
            String.format("%.2f", value),
            color = Color(0xFF94A3B8),
            fontSize = 10.sp,
            modifier = Modifier.width(36.dp)
        )
    }
}
