package com.fylio.app.ui.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.core.image.ImageEngine
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.LocalFylioSurfaces
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full-screen image reader: pinch to zoom, double-tap to toggle fit/2x.
 */
@Composable
fun ImageViewer(file: File, name: String, onBack: () -> Unit) {
    val surfaces = LocalFylioSurfaces.current
    var scale by remember { mutableFloatStateOf(1f) }
    var wrap by remember { mutableStateOf(false) }

    val bitmap by produceState<Bitmap?>(null, file.absolutePath) {
        value = withContext(Dispatchers.IO) { ImageEngine.decode(file, 2800) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ Back", color = Color(0xFF60A5FA)) }
            Text(
                name,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFE5E7EB),
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF1F2937))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .pointerInput(Unit) { detectTapGestures { scale = 1f } }
            ) {
                Text("${(scale * 100).toInt()}%", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 6f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { scale = if (scale > 1.5f) 1f else 2.5f }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmap
            if (bmp == null) {
                CircularProgressIndicator(color = ElectricBlue)
            } else {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
        }

        Text(
            "Pinch to zoom · double-tap to toggle",
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 12.dp, start = 16.dp)
        )
    }
}

/**
 * Plain-text reader with adjustable font size, word wrap toggle and selectable text.
 */
@Composable
fun TextViewer(
    file: File,
    fontSize: Float,
    darkReader: Boolean,
    onBack: () -> Unit
) {
    var wrap by remember { mutableStateOf(true) }
    val text by produceState("", file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            runCatching { file.readText().take(400_000) }.getOrDefault("Could not read the file")
        }
    }

    val bg = if (darkReader) Color(0xFF0B0F19) else Color(0xFFF8FAFC)
    val card = if (darkReader) Color(0xFF111827) else Color.White
    val fg = if (darkReader) Color(0xFFE5E7EB) else Color(0xFF1F2937)

    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(card)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ Back", color = ElectricBlue) }
            Text(
                file.name,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (darkReader) Color(0xFF1F2937) else Color(0xFFF1F5F9))
                    .pointerInput(Unit) { detectTapGestures { wrap = !wrap } }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    if (wrap) "Wrap on" else "Wrap off",
                    color = ElectricBlue,
                    fontSize = 10.sp
                )
            }
        }

        SelectionContainer(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = text.ifBlank { "(empty file)" },
                fontFamily = FontFamily.Monospace,
                fontSize = (12 * fontSize).sp,
                lineHeight = (19 * fontSize).sp,
                softWrap = wrap,
                color = fg,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(card)
                    .padding(14.dp)
            )
        }
    }
}
