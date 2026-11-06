package com.fylio.app.ui.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.core.pdf.PdfEngine
import com.fylio.app.core.pdf.PdfTextSearch
import com.fylio.app.data.db.DocumentEntity
import com.fylio.app.ui.components.Pill
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.LocalFylioSurfaces
import com.fylio.app.util.FileOps
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Continuous-scroll PDF reader.
 *
 * Pages render lazily as they enter the viewport, so a 500-page file opens instantly.
 * Pinch-zoom applies to the whole document (pages stay crisp because each is rendered
 * at 2x device density); double-tap toggles fit-width.
 */
@Composable
fun PdfViewerScreen(
    entity: DocumentEntity,
    file: File,
    onBack: () -> Unit,
    onProgress: (page: Int, pageCount: Int) -> Unit,
    onAnnotate: (pageIndex: Int) -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val surfaces = LocalFylioSurfaces.current
    val context = LocalContext.current
    val pageCount = remember(file.absolutePath) { PdfEngine.pageCount(file) }

    var pageWidthScale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var currentPage by remember { mutableIntStateOf(entity.lastPage.coerceAtLeast(0)) }
    var showThumbs by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PdfTextSearch.Hit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = entity.lastPage.coerceAtLeast(0))

    LaunchedEffect(pageCount) {
        if (pageCount > 0) onProgress(currentPage, pageCount)
    }
    // persist reading position as the user scrolls
    LaunchedEffect(listState) {
        snapshotFlowPage(listState).collect { idx ->
            if (idx != currentPage) {
                currentPage = idx
                onProgress(idx, pageCount)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(if (surfaces.isDark) surfaces.headerWarm else Color(0xFFF1F5F9))
    ) {
        // ---------- toolbar ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaces.card)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = surfaces.textPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    entity.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = surfaces.textPrimary,
                    maxLines = 1
                )
                Text(
                    if (pageCount > 0) "Page ${currentPage + 1} of $pageCount" else "Loading…",
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaces.textSecondary
                )
            }
            IconButton(onClick = { showSearch = true }) {
                Icon(Icons.Outlined.Search, "Search", tint = surfaces.textPrimary)
            }
            IconButton(onClick = { showThumbs = !showThumbs }) {
                Icon(Icons.Outlined.Info, "Pages", tint = surfaces.textPrimary)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, "Share", tint = surfaces.textPrimary)
            }
            IconButton(onClick = { FileOps.printFile(context, file) }) {
                Icon(Icons.Outlined.Print, "Print", tint = surfaces.textPrimary)
            }
            IconButton(onClick = { showDelete = true }) {
                Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }

        // ---------- page list ----------
        Box(Modifier.weight(1f)) {
            if (pageCount == 0) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = ElectricBlue)
                    Spacer(Modifier.height(10.dp))
                    Text("Opening PDF…", color = surfaces.textSecondary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = pageWidthScale
                            scaleY = pageWidthScale
                        },
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pageCount) { index ->
                        PdfPage(
                            file = file,
                            index = index,
                            onDoubleTap = {
                                pageWidthScale = if (pageWidthScale > 1.2f) 1f else 2f
                                if (pageWidthScale == 1f) { panX = 0f; panY = 0f }
                            },
                            onZoom = { zoom, pan ->
                                pageWidthScale = (pageWidthScale * zoom).coerceIn(1f, 4f)
                                if (pageWidthScale > 1f) {
                                    panX += pan.x
                                    panY += pan.y
                                } else {
                                    panX = 0f; panY = 0f
                                }
                            },
                            onAnnotate = { onAnnotate(index) }
                        )
                    }
                }
            }

            // page jump pill
            if (pageCount > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                        .clip(CircleShape)
                        .background(surfaces.card)
                        .clickable { showThumbs = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "${currentPage + 1} / $pageCount",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricBlue
                    )
                }
            }
        }

        // ---------- thumbnail carousel ----------
        if (showThumbs) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaces.card)
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pageCount) { i ->
                    ThumbCell(
                        file = file,
                        index = i,
                        selected = i == currentPage,
                        onClick = {
                            currentPage = i
                            onProgress(i, pageCount)
                            showThumbs = false
                        }
                    )
                }
            }
        }
    }

    // ---------- dialogs ----------
    if (showSearch) {
        AlertDialog(
            onDismissRequest = { showSearch = false },
            title = { Text("Search in document") },
            text = {
                Column {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("Find text") }
                    )
                    Spacer(Modifier.height(8.dp))
                    if (searching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ElectricBlue
                        )
                    } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                        Text("No matches", color = surfaces.textSecondary, fontSize = 12.sp)
                    } else {
                        Column(Modifier.height(180.dp)) {
                            LazyColumn {
                                items(searchResults.take(60)) { hit ->
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showSearch = false
                                            }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            "Page ${hit.page}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricBlue
                                        )
                                        Text(
                                            hit.snippet,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = surfaces.textPrimary,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    searching = true
                    // real search: PDFTextStripper per page, off the main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Thread {
                            val hits = PdfTextSearch.search(file, searchQuery)
                            searching = false
                            searchResults = hits
                        }.start()
                    }
                }) { Text("Search") }
            },
            dismissButton = {
                TextButton(onClick = { showSearch = false }) { Text("Close") }
            }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete document?") },
            text = { Text("${entity.name} will be removed.") },
            confirmButton = {
                TextButton(onClick = { showDelete = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }
}

/** Observes which page index is at the top of the viewport. */
private fun snapshotFlowPage(state: androidx.compose.foundation.lazy.LazyListState) =
    androidx.compose.runtime.snapshotFlow {
        state.firstVisibleItemIndex
    }

@Composable
private fun PdfPage(
    file: File,
    index: Int,
    onDoubleTap: () -> Unit,
    onZoom: (Float, androidx.compose.ui.geometry.Offset) -> Unit,
    onAnnotate: () -> Unit
) {
    val surfaces = LocalFylioSurfaces.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = file.absolutePath, key2 = index) {
        value = withContext(Dispatchers.IO) { PdfEngine.renderPage(file, index, 2f) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ -> onZoom(zoom, pan) }
            }
            .clickable(onClick = onDoubleTap)
    ) {
        val bmp = bitmap
        if (bmp == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f)
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(modifier = Modifier.size(22.dp), color = ElectricBlue) }
        } else {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onAnnotate)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Edit,
                "Annotate",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Annotate", color = Color.White, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ThumbCell(file: File, index: Int, selected: Boolean, onClick: () -> Unit) {
    val surfaces = LocalFylioSurfaces.current
    val bmp by produceState<Bitmap?>(initialValue = null, key1 = index) {
        value = withContext(Dispatchers.IO) { PdfEngine.renderPage(file, index, 0.45f) }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 70.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) ElectricBlue else surfaces.cardBorder,
                    RoundedCornerShape(6.dp)
                )
                .clickable(onClick = onClick)
        ) {
            bmp?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            "${index + 1}",
            fontSize = 9.sp,
            color = if (selected) ElectricBlue else surfaces.textSecondary
        )
    }
}
