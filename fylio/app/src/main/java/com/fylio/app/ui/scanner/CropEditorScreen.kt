package com.fylio.app.ui.scanner

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.core.image.ImageEngine
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CropBlue = Color(0xFF2563EB)

/**
 * Interactive perspective crop.
 *
 * Four draggable corner handles sit on the document corners; grid lines connect them and
 * "Apply Crop" warps the quad into a straightened rectangle. Rotation and auto-fit are
 * one tap away. No linear sliders are used for cropping.
 */
@Composable
fun CropEditorScreen(
    session: ScanSessionViewModel,
    state: ScanSession,
    pageId: String,
    onBack: () -> Unit,
    onApplied: () -> Unit
) {
    val page = state.pages.firstOrNull { it.id == pageId }
    if (page == null) {
        // Session changed underneath us (page deleted) — fall back, never crash.
        onBack()
        return
    }

    // Crop editor back -> camera, keeping every captured page.
    BackHandler(enabled = true) { onBack() }

    var quad by remember(page.id) {
        mutableStateOf(if (page.quad.isFull) ScanQuad.Default else page.quad)
    }
    var rotation by remember(page.id) { mutableStateOf(page.rotation) }
    var applying by remember { mutableStateOf(false) }
    var activeCorner by remember { mutableStateOf(-1) }

    val bitmap by produceState<Bitmap?>(null, page.id, rotation) {
        value = withContext(Dispatchers.IO) {
            val decoded = ScanSessionViewModel.decodeUpright(page.sourceFile, 1800)
            if (decoded == null) null
            else if (rotation % 360 == 0) decoded else ImageEngine.rotate(decoded, rotation)
        }
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
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Adjust crop",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Page ${state.pages.indexOfFirst { it.id == pageId } + 1} of ${state.pages.size} · drag the corners",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
            TextButton(onClick = {
                // Reset restores the detected/auto bounds
                quad = state.detectedQuad ?: ScanQuad.Default
            }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Restore, "Reset", tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                    Text("Reset", color = Color(0xFF60A5FA), fontSize = 9.sp)
                }
            }
            TextButton(onClick = { rotation = (rotation + 90) % 360 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.RotateRight, "Rotate", tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                    Text("Rotate", color = Color(0xFF60A5FA), fontSize = 9.sp)
                }
            }
            TextButton(onClick = { quad = state.detectedQuad ?: ScanQuad.Full }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Check, "Auto-fit", tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                    Text("Auto-fit", color = Color(0xFF60A5FA), fontSize = 9.sp)
                }
            }
        }

        // ------------------------------------------------------- image + handles
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmap
            if (bmp == null) {
                CircularProgressIndicator(color = CropBlue)
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat(), matchHeightConstraintsFirst = true)
                ) {
                    val density = LocalDensity.current
                    val widthPx = with(density) { maxWidth.toPx() }
                    val heightPx = with(density) { maxHeight.toPx() }
                    val imageAspect = bmp.width.toFloat() / bmp.height.toFloat()
                    val boxAspect = widthPx / heightPx
                    val drawW: Float
                    val drawH: Float
                    val offsetX: Float
                    val offsetY: Float
                    if (imageAspect > boxAspect) {
                        drawW = widthPx
                        drawH = widthPx / imageAspect
                        offsetX = 0f
                        offsetY = (heightPx - drawH) / 2f
                    } else {
                        drawH = heightPx
                        drawW = heightPx * imageAspect
                        offsetX = (widthPx - drawW) / 2f
                        offsetY = 0f
                    }

                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Page to crop",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // dark scrim outside the selection
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(quad, drawW, drawH, offsetX, offsetY) {
                                detectDragGestures(
                                    onDragStart = { start ->
                                        // grab the nearest corner within a generous radius
                                        val pts = quad.corners()
                                        var best = -1
                                        var bestDist = 90f
                                        pts.forEachIndexed { index, (nx, ny) ->
                                            val px = offsetX + nx * drawW
                                            val py = offsetY + ny * drawH
                                            val d = abs(start.x - px) + abs(start.y - py)
                                            if (d < bestDist) {
                                                bestDist = d
                                                best = index
                                            }
                                        }
                                        activeCorner = best
                                    },
                                    onDragEnd = { activeCorner = -1 },
                                    onDragCancel = { activeCorner = -1 },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val corner = activeCorner
                                        if (corner >= 0 && drawW > 0f && drawH > 0f) {
                                            val pts = quad.corners()
                                            val (curX, curY) = pts[corner]
                                            val newNormX = curX + dragAmount.x / drawW
                                            val newNormY = curY + dragAmount.y / drawH
                                            quad = quad.replaced(corner, newNormX, newNormY)
                                        }
                                    }
                                )
                            }
                    ) {
                        // scrim
                        val xs = quad.xs().map { offsetX + it * drawW }
                        val ys = quad.ys().map { offsetY + it * drawH }
                        val selection = Path().apply {
                            moveTo(xs[0], ys[0])
                            lineTo(xs[1], ys[1])
                            lineTo(xs[2], ys[2])
                            lineTo(xs[3], ys[3])
                            close()
                        }
                        val outer = Path().apply {
                            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                        }
                        val scrim = Path().apply {
                            addPath(outer)
                            addPath(selection)
                            fillType = PathFillType.EvenOdd
                        }
                        drawPath(scrim, Color.Black.copy(alpha = 0.45f))

                        // grid: thirds inside the selection
                        val gridColor = Color.White.copy(alpha = 0.35f)
                        for (t in 1..2) {
                            val f = t / 3f
                            val topX = xs[0] + (xs[1] - xs[0]) * f
                            val topY = ys[0] + (ys[1] - ys[0]) * f
                            val bottomX = xs[3] + (xs[2] - xs[3]) * f
                            val bottomY = ys[3] + (ys[2] - ys[3]) * f
                            drawLine(gridColor, Offset(topX, topY), Offset(bottomX, bottomY), 1.5f)

                            val leftX = xs[0] + (xs[3] - xs[0]) * f
                            val leftY = ys[0] + (ys[3] - ys[0]) * f
                            val rightX = xs[1] + (xs[2] - xs[1]) * f
                            val rightY = ys[1] + (ys[2] - ys[1]) * f
                            drawLine(gridColor, Offset(leftX, leftY), Offset(rightX, rightY), 1.5f)
                        }

                        drawPath(selection, CropBlue, style = Stroke(width = 2.5f))

                        // corner handles
                        xs.indices.forEach { i ->
                            val c = Offset(xs[i], ys[i])
                            drawCircle(Color.White, radius = 13f, center = c)
                            drawCircle(
                                if (activeCorner == i) Color(0xFF1D4ED8) else CropBlue,
                                radius = 13f,
                                center = c
                            )
                            drawCircle(Color.White, radius = 5f, center = c)
                        }

                        // live size readout
                        val area = quad.area()
                        if (area < 1f) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "${(area * 100).toInt()}%",
                                size.width / 2f,
                                24f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 26f
                                    isAntiAlias = true
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                            )
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------- footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111827))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1F2937))
                        .clickable {
                            quad = ScanQuad.Default
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("Reset bounds", color = Color.White, fontSize = 11.sp)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1F2937))
                        .clickable { rotation = (rotation + 90) % 360 }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("Rotate 90°", color = Color.White, fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CropBlue)
                        .clickable(enabled = !applying) {
                            applying = true
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (applying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Apply Crop",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Apply Crop: commit quad + rotation, then hand control to the enhance screen.
    if (applying) {
        androidx.compose.runtime.LaunchedEffect(page.id, quad, rotation) {
            session.update(page.id) { it.copy(quad = quad, rotation = rotation) }
            applying = false
            onApplied()
        }
    }
}
