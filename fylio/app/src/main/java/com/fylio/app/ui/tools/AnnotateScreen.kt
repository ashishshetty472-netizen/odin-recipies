package com.fylio.app.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.core.pdf.PdfEngine
import com.fylio.app.data.FylioStore
import com.fylio.app.ui.components.FylioIcon
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.LocalFylioSurfaces
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One drawn annotation, in normalised page coordinates so it scales with the page. */
sealed interface Annotation {
    data class Ink(val points: List<Offset>, val color: Color, val width: Float) : Annotation
    data class Highlight(val start: Offset, val end: Offset, val color: Color) : Annotation
    data class Underline(val start: Offset, val end: Offset, val color: Color) : Annotation
    data class Strike(val start: Offset, val end: Offset, val color: Color) : Annotation
    data class Text(val origin: Offset, val text: String, val color: Color, val size: Float) : Annotation
    data class Signature(val points: List<Offset>, val width: Float) : Annotation
}

/**
 * PDF annotation canvas. Annotations are drawn live over the page and baked into the
 * exported PDF, so the saved copy shows them in any viewer.
 */
@Composable
fun AnnotateScreen(
    pdfFile: File,
    pageIndex: Int,
    initialPage: Int,
    onBack: () -> Unit,
    onSaved: (File) -> Unit
) {
    val surfaces = LocalFylioSurfaces.current
    val context = LocalContext.current

    var page by remember { mutableStateOf(initialPage.coerceAtLeast(0)) }
    val annotations = remember { mutableStateListOf<Annotation>() }
    var tool by remember { mutableStateOf(AnnotateTool.INK) }
    var inkColor by remember { mutableStateOf(Color(0xFFEF4444)) }
    var showTextDialog by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf("") }
    var pendingTextAt by remember { mutableStateOf<Offset?>(null) }
    var saving by remember { mutableStateOf(false) }

    val pageCount = remember { PdfEngine.pageCount(pdfFile) }
    val pageBitmap by produceState<Bitmap?>(null, page) {
        value = withContext(Dispatchers.IO) { PdfEngine.renderPage(pdfFile, page, 2f) }
    }

    // ink being drawn right now
    val livePoints = remember { mutableStateListOf<Offset>() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ Back", color = Color(0xFF60A5FA)) }
            Text(
                "Annotate · page ${page + 1}/${pageCount.coerceAtLeast(1)}",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                saving = true
            }) { Text("Save", color = Color(0xFF60A5FA), fontWeight = FontWeight.SemiBold) }
        }

        // page + canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = pageBitmap
            if (bmp == null) {
                CircularProgressIndicator(color = ElectricBlue)
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Page ${page + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxSize()
                    )
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(tool, page) {
                                when (tool) {
                                    AnnotateTool.TEXT -> detectTapGestures { offset ->
                                        pendingTextAt = Offset(
                                            offset.x / size.width,
                                            offset.y / size.height
                                        )
                                        showTextDialog = true
                                    }
                                    else -> detectDragGestures(
                                        onDragStart = { livePoints.clear(); livePoints.add(it) },
                                        onDrag = { change, _ -> livePoints.add(change.position) },
                                        onDragEnd = {
                                            if (livePoints.size > 1) {
                                                val norm = livePoints.map {
                                                    Offset(it.x / size.width, it.y / size.height)
                                                }
                                                annotations.add(
                                                    when (tool) {
                                                        AnnotateTool.INK -> Annotation.Ink(norm, inkColor, 3f)
                                                        AnnotateTool.SIGN -> Annotation.Signature(norm, 3f)
                                                        AnnotateTool.HIGHLIGHT -> Annotation.Highlight(
                                                            norm.first(), norm.last(),
                                                            Color(0xFFFFE066).copy(alpha = 0.55f)
                                                        )
                                                        AnnotateTool.UNDERLINE -> Annotation.Underline(
                                                            norm.first(), norm.last(), inkColor
                                                        )
                                                        AnnotateTool.STRIKE -> Annotation.Strike(
                                                            norm.first(), norm.last(), inkColor
                                                        )
                                                        else -> Annotation.Ink(norm, inkColor, 3f)
                                                    }
                                                )
                                            }
                                            livePoints.clear()
                                        }
                                    )
                                }
                            }
                    ) {
                        drawAnnotations(annotations)
                        if (livePoints.size > 1) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                val first = Offset(
                                    livePoints.first().x / size.width * size.width,
                                    livePoints.first().y / size.height * size.height
                                )
                                moveTo(livePoints.first().x, livePoints.first().y)
                                livePoints.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(
                                path,
                                color = if (tool == AnnotateTool.SIGN) Color(0xFF1D4ED8) else inkColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 3f,
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                    }
                    if (saving) {
                        SavingOverlay(pageCount) { quality ->
                            saving = false
                            // bake annotations into a new PDF
                            Thread {
                                val out = bakeAnnotations(
                                    context = context,
                                    source = pdfFile,
                                    pageIndex = page,
                                    annotations = annotations.toList(),
                                    quality = quality
                                )
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    if (out != null) onSaved(out)
                                }
                            }.start()
                        }
                    }
                }
            }
        }

        // page nav
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹",
                fontSize = 22.sp,
                color = if (page > 0) Color(0xFF60A5FA) else Color(0xFF475569),
                modifier = Modifier
                    .clip(CircleShape)
                    .then(
                        if (page > 0) Modifier.pointerInput(Unit) {
                            detectTapGestures { page-- }
                        } else Modifier
                    )
                    .padding(horizontal = 18.dp)
            )
            Text("${page + 1} / $pageCount", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Text(
                "›",
                fontSize = 22.sp,
                color = if (page < pageCount - 1) Color(0xFF60A5FA) else Color(0xFF475569),
                modifier = Modifier
                    .clip(CircleShape)
                    .then(
                        if (page < pageCount - 1) Modifier.pointerInput(Unit) {
                            detectTapGestures { page++ }
                        } else Modifier
                    )
                    .padding(horizontal = 18.dp)
            )
        }

        // toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111827))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AnnotateTool.entries.forEach { t ->
                val selected = tool == t
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) ElectricBlue.copy(alpha = 0.18f) else Color.Transparent)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .then(
                                if (selected) Modifier.border(1.5.dp, ElectricBlue, CircleShape)
                                else Modifier
                            )
                            .pointerInput(Unit) { detectTapGestures { tool = t } },
                        contentAlignment = Alignment.Center
                    ) {
                        FylioIcon(t.icon, size = 14.dp)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        t.label,
                        fontSize = 8.sp,
                        color = if (selected) ElectricBlue else Color(0xFF94A3B8)
                    )
                }
            }
        }

        // colour swatches
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111827))
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            listOf(
                Color(0xFFEF4444), Color(0xFF2563EB), Color(0xFF10B981),
                Color(0xFFF59E0B), Color(0xFF111827), Color(0xFF8B5CF6)
            ).forEach { c ->
                Box(
                    Modifier
                        .padding(horizontal = 5.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(c)
                        .then(
                            if (inkColor == c) Modifier.border(2.dp, Color.White, CircleShape)
                            else Modifier
                        )
                        .pointerInput(Unit) { detectTapGestures { inkColor = c } }
                )
            }
            Spacer(Modifier.width(10.dp))
            TextButton(onClick = { annotations.clear() }) {
                Text("Clear", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }
    }

    if (showTextDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Add text") },
            text = {
                androidx.compose.material3.TextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    singleLine = false,
                    placeholder = { Text("Type annotation") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingTextAt?.let {
                        annotations.add(Annotation.Text(it, textValue, inkColor, 12f))
                    }
                    textValue = ""
                    showTextDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) { Text("Cancel") }
            }
        )
    }
}

enum class AnnotateTool(val label: String, val icon: String) {
    INK("Draw", "draw"),
    HIGHLIGHT("Highlight", "highlight"),
    UNDERLINE("Underline", "underline"),
    STRIKE("Strike", "strikethrough"),
    TEXT("Text", "text_box"),
    SIGN("Sign", "signature")
}

/** Draws the annotation list in normalised coordinates scaled to the canvas. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAnnotations(list: List<Annotation>) {
    list.forEach { a ->
        when (a) {
            is Annotation.Ink -> drawInk(a.points, a.color, a.width)
            is Annotation.Signature -> drawInk(a.points, Color(0xFF1D4ED8), a.width + 0.6f)
            is Annotation.Highlight -> {
                val x0 = a.start.x * size.width
                val y0 = a.start.y * size.height
                val x1 = a.end.x * size.width
                val y1 = a.end.y * size.height
                drawRect(
                    color = a.color,
                    topLeft = Offset(minOf(x0, x1), minOf(y0, y1) - 6f),
                    size = androidx.compose.ui.geometry.Size(
                        kotlin.math.abs(x1 - x0),
                        kotlin.math.abs(y1 - y0) + 12f
                    )
                )
            }
            is Annotation.Underline -> drawLine(
                a.color,
                Offset(a.start.x * size.width, a.start.y * size.height),
                Offset(a.end.x * size.width, a.end.y * size.height),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
            is Annotation.Strike -> drawLine(
                a.color,
                Offset(a.start.x * size.width, a.start.y * size.height),
                Offset(a.end.x * size.width, a.end.y * size.height),
                strokeWidth = 2f
            )
            is Annotation.Text -> drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = a.color.toArgb()
                    textSize = a.size * 3f
                    isAntiAlias = true
                }
                drawText(a.text, a.origin.x * size.width, a.origin.y * size.height, paint)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawInk(
    points: List<Offset>,
    color: Color,
    width: Float
) {
    if (points.size < 2) return
    val path = androidx.compose.ui.graphics.Path()
    path.moveTo(points.first().x * size.width, points.first().y * size.height)
    points.drop(1).forEach { path.lineTo(it.x * size.width, it.y * size.height) }
    drawPath(
        path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = width, cap = StrokeCap.Round)
    )
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)

@Composable
private fun SavingOverlay(pageCount: Int, onConfirm: (Int) -> Unit) {
    var quality by remember { mutableStateOf(85) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { },
        title = { Text("Save annotated copy") },
        text = {
            Column {
                Text("Annotations are baked into a new PDF with $pageCount pages.")
                Spacer(Modifier.height(10.dp))
                Text("Quality: $quality%", style = MaterialTheme.typography.labelMedium)
                androidx.compose.material3.Slider(
                    value = quality.toFloat(),
                    onValueChange = { quality = it.toInt() },
                    valueRange = 50f..95f
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(quality) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { }) { Text("Cancel") } }
    )
}

/**
 * Renders every page, draws the annotations of [pageIndex] onto that page's bitmap, then
 * rebuilds the PDF. Other pages keep their original content as rendered images, which is
 * the standard approach for flattened annotations without a layout engine.
 */
private fun bakeAnnotations(
    context: android.content.Context,
    source: File,
    pageIndex: Int,
    annotations: List<Annotation>,
    quality: Int
): File? {
    val count = PdfEngine.pageCount(source)
    if (count == 0) return null
    val bitmaps = mutableListOf<Bitmap>()
    for (i in 0 until count) {
        val bmp = PdfEngine.renderPage(source, i, 2f)?.let { it.copy(Bitmap.Config.ARGB_8888, true) }
            ?: continue
        if (i == pageIndex && annotations.isNotEmpty()) {
            val canvas = android.graphics.Canvas(bmp)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            annotations.forEach { a ->
                when (a) {
                    is Annotation.Ink -> {
                        paint.style = android.graphics.Paint.Style.STROKE
                        paint.strokeWidth = a.width * bmp.width / 400f
                        paint.strokeCap = android.graphics.Paint.Cap.ROUND
                        paint.color = a.color.toArgb()
                        val p = android.graphics.Path()
                        a.points.forEachIndexed { idx, pt ->
                            val x = pt.x * bmp.width
                            val y = pt.y * bmp.height
                            if (idx == 0) p.moveTo(x, y) else p.lineTo(x, y)
                        }
                        canvas.drawPath(p, paint)
                    }
                    is Annotation.Signature -> {
                        paint.style = android.graphics.Paint.Style.STROKE
                        paint.strokeWidth = a.width * bmp.width / 400f
                        paint.color = android.graphics.Color.rgb(29, 78, 216)
                        val p = android.graphics.Path()
                        a.points.forEachIndexed { idx, pt ->
                            val x = pt.x * bmp.width
                            val y = pt.y * bmp.height
                            if (idx == 0) p.moveTo(x, y) else p.lineTo(x, y)
                        }
                        canvas.drawPath(p, paint)
                    }
                    is Annotation.Highlight -> {
                        paint.style = android.graphics.Paint.Style.FILL
                        paint.color = 0x8CFFE066.toInt()
                        val x0 = a.start.x * bmp.width
                        val y0 = a.start.y * bmp.height
                        val x1 = a.end.x * bmp.width
                        val y1 = a.end.y * bmp.height
                        canvas.drawRect(
                            minOf(x0, x1), minOf(y0, y1) - bmp.height / 200f,
                            maxOf(x0, x1), maxOf(y0, y1) + bmp.height / 200f, paint
                        )
                    }
                    is Annotation.Underline -> {
                        paint.style = android.graphics.Paint.Style.STROKE
                        paint.strokeWidth = bmp.height / 400f
                        paint.color = a.color.toArgb()
                        canvas.drawLine(
                            a.start.x * bmp.width, a.start.y * bmp.height,
                            a.end.x * bmp.width, a.end.y * bmp.height, paint
                        )
                    }
                    is Annotation.Strike -> {
                        paint.style = android.graphics.Paint.Style.STROKE
                        paint.strokeWidth = bmp.height / 400f
                        paint.color = a.color.toArgb()
                        canvas.drawLine(
                            a.start.x * bmp.width, a.start.y * bmp.height,
                            a.end.x * bmp.width, a.end.y * bmp.height, paint
                        )
                    }
                    is Annotation.Text -> {
                        paint.style = android.graphics.Paint.Style.FILL
                        paint.color = a.color.toArgb()
                        paint.textSize = a.size * bmp.width / 50f
                        canvas.drawText(
                            a.text,
                            a.origin.x * bmp.width,
                            a.origin.y * bmp.height,
                            paint
                        )
                    }
                }
            }
        }
        bitmaps.add(bmp)
    }
    val target = FylioStore.uniqueFile(
        FylioStore.exportsDir(context),
        source.nameWithoutExtension + "_annotated",
        "pdf"
    )
    return PdfEngine.bitmapsToPdf(bitmaps, target, jpegQuality = quality)
}
