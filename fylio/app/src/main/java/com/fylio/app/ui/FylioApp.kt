package com.fylio.app.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.fylio.app.data.FileKind
import com.fylio.app.data.FylioFile
import com.fylio.app.data.FylioStore
import com.fylio.app.data.FylioTools
import com.fylio.app.data.ToolDef
import com.fylio.app.data.ToolStatus
import com.fylio.app.ui.home.HomeScreen
import com.fylio.app.ui.scanner.ScannerScreen
import com.fylio.app.ui.toolkit.ToolkitScreen
import com.fylio.app.ui.viewer.ViewerScreen
import com.fylio.app.util.FileOps
import com.fylio.app.util.PdfTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface Route {
    data object Home : Route
    data object Scanner : Route
    data class Viewer(val item: FylioFile) : Route
    data class Tools(val toolId: String, val section: String?) : Route
    data object Settings : Route
}

@Composable
fun FylioApp(
    viewModel: FylioViewModel,
    incomingUri: Uri?,
    onIncomingHandled: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    var route by remember { mutableStateOf<Route>(Route.Home) }
    var infoTool by remember { mutableStateOf<ToolDef?>(null) }

    LaunchedEffect(incomingUri) {
        if (incomingUri != null) {
            viewModel.importIncoming(incomingUri)
            onIncomingHandled()
        }
    }

    state.message?.let { msg ->
        LaunchedEffect(msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    fun openTool(toolId: String, section: String? = null) {
        val tool = FylioTools.byId(toolId) ?: return
        when (tool.status) {
            ToolStatus.COMING_SOON -> infoTool = tool
            else -> when (toolId) {
                "scanner", "camera", "camera_to_pdf" -> route = Route.Scanner
                "theme", "about", "privacy", "permissions",
                "clear_cache", "reading_prefs", "file_prefs", "output_prefs" -> {
                    if (toolId == "about") route = Route.Settings
                    else route = Route.Settings
                }
                else -> route = Route.Tools(toolId, section)
            }
        }
    }

    fun runTool(toolId: String, item: FylioFile?) {
        if (item == null && toolId !in setOf("merge_pdf", "images_to_pdf")) {
            Toast.makeText(context, "Pick a document first", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            val exports = FylioStore.exportsDir(context)
            val result = withContext(Dispatchers.IO) {
                when (toolId) {
                    "rotate_pages", "rotate" -> {
                        // Rotate every page of the PDF (or a single image) 90 degrees.
                        val src = item?.file
                        when {
                            src == null -> null
                            item.kind == FileKind.IMAGE -> {
                                val bmp = FileOps.decodeBitmap(src, 2400)
                                if (bmp == null) null else {
                                    val out = FileOps.rotate(bmp, 90)
                                    val target = File(FylioStore.exportsDir(context),
                                        src.nameWithoutExtension + "_rotated.jpg")
                                    FileOps.saveJpeg(out, target)
                                }
                            }
                            else -> {
                                val pages = PdfTools.renderPages(src).map { FileOps.rotate(it, 90) }
                                if (pages.isEmpty()) null
                                else FileOps.bitmapsToPdf(
                                    pages,
                                    File(FylioStore.exportsDir(context),
                                        src.nameWithoutExtension + "_rotated.pdf")
                                )
                            }
                        }
                    }
                    "pdf_to_jpg", "pdf_to_images", "pdf_to_png" -> item?.let { PdfTools.toImages(it.file, exports) }
                    "images_to_pdf", "images_to_pdf_create", "images_to_pdf_conv" ->
                        item?.let { PdfTools.fromImages(listOf(it.file), exports, it.name.substringBeforeLast('.')) }
                    "merge_pdf" -> {
                        val files = state.documents
                            .filter { it.kind == FileKind.PDF || it.kind == FileKind.IMAGE }
                            .map { it.file }
                        PdfTools.merge(files, exports)
                    }
                    "split_pdf" -> item?.let { PdfTools.splitToPages(it.file, exports) }
                    "extract_pages" -> item?.let {
                        PdfTools.extractRange(it.file, exports, 1, minOf(1, PdfTools.pageCount(it.file)).coerceAtLeast(1))
                    }
                    "print_pdf" -> { item?.let { f -> withContext(Dispatchers.Main) { FileOps.openWith(context, f.file) } }; null }
                    // File-manager actions
                    "rename" -> { item?.let { Toast.makeText(context, "Rename from the document viewer", Toast.LENGTH_SHORT).show() }; null }
                    "delete" -> { item?.let { FylioStore.delete(it.file) } }
                    "copy" -> item?.let {
                        val target = FylioStore.uniqueFile(
                            FylioStore.documentsDir(context),
                            it.file.nameWithoutExtension + "_copy",
                            it.file.extension.ifBlank { "dat" }
                        )
                        it.file.copyTo(target, overwrite = false)
                    }
                    "share" -> { item?.let { f -> withContext(Dispatchers.Main) { FileOps.share(context, f.file) } }; null }
                    else -> null
                }
            }

            val summary = when (result) {
                is File -> "Saved ${result.name}"
                is Boolean -> if (result) "Done" else "That didn't work"
                is List<*> -> {
                    val files = result.filterIsInstance<File>()
                    when {
                        files.isEmpty() -> "Nothing to do for that document"
                        files.size == 1 -> "Saved ${files.first().name}"
                        else -> "Saved ${files.size} files to Exports"
                    }
                }
                else -> "Nothing to do for that document"
            }
            viewModel.refresh()
            Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val current = route) {
                Route.Home -> Box(Modifier.statusBarsPadding()) {
                    HomeScreen(
                        state = state,
                        onOpen = { route = Route.Viewer(it) },
                        onTool = { toolId -> openTool(toolId) },
                        onQueryChange = viewModel::setQuery,
                        onSortChange = viewModel::setSort,
                        onSectionMore = { section -> route = Route.Tools("", section) },
                        onSettings = { route = Route.Settings }
                    )
                }

                Route.Scanner -> ScannerScreen(
                    onBack = { route = Route.Home },
                    onSaved = { pdf ->
                        viewModel.saveDocument(pdf, "Saved ${pdf.name}")
                        route = Route.Home
                    }
                )

                is Route.Viewer -> ViewerScreen(
                    item = current.item,
                    onBack = {
                        viewModel.refresh()
                        route = Route.Home
                    },
                    onShare = { FileOps.share(context, current.item.file) },
                    onDelete = { viewModel.delete(current.item) { route = Route.Home } },
                    onRename = { newName ->
                        viewModel.rename(current.item, newName)
                        route = Route.Home
                    }
                )

                is Route.Tools -> ToolkitScreen(
                    toolId = current.toolId,
                    sectionTitle = current.section,
                    documents = state.documents,
                    onBack = { route = Route.Home },
                    onPick = { doc -> runTool(current.toolId.ifBlank { "merge_pdf" }, doc) },
                    onSelectTool = { id -> openTool(id, current.section) },
                    onBlocked = { tool -> infoTool = tool }
                )

                Route.Settings -> SettingsScreen(
                    documentCount = state.documents.size,
                    totalBytes = state.documents.sumOf { it.sizeBytes },
                    onBack = { route = Route.Home },
                    onTool = { id -> openTool(id) }
                )
            }
        }
    }

    infoTool?.let { tool ->
        AlertDialog(
            onDismissRequest = { infoTool = null },
            title = { Text(tool.title) },
            text = {
                Text(
                    tool.description + "\n\nThis tool is wired into the UI and its icon ships " +
                        "with the app, but the processing engine lands in the next phase."
                )
            },
            confirmButton = {
                TextButton(onClick = { infoTool = null }) { Text("Got it") }
            }
        )
    }
}
