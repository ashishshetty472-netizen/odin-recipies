package com.fylio.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    data class Toolkit(val category: String) : Route
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

    // "Open with Fylio" hand-off
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

    fun runTool(category: String, item: FylioFile?) {
        scope.launch {
            val exports = FylioStore.exportsDir(context)
            val result: Any? = withContext(Dispatchers.IO) {
                when (category) {
                    "CONVERT" -> when {
                        item == null -> null
                        item.kind == FileKind.PDF -> PdfTools.toImages(item.file, exports)
                        item.kind == FileKind.IMAGE ->
                            PdfTools.fromImages(listOf(item.file), exports, item.name.substringBeforeLast('.'))
                        else -> emptyList<File>()
                    }
                    "MERGE" -> {
                        val pdfs = state.documents
                            .filter { it.kind == FileKind.PDF || it.kind == FileKind.IMAGE }
                            .map { it.file }
                        PdfTools.merge(pdfs, exports)
                    }
                    "SPLIT" -> item?.let { PdfTools.splitToPages(it.file, exports) }
                    "EXTRACT" -> item?.let {
                        val count = PdfTools.pageCount(it.file)
                        PdfTools.extractRange(it.file, exports, 1, minOf(1, count).coerceAtLeast(1))
                    }
                    "ROTATE" -> null
                    else -> null
                }
            }

            val summary = when (result) {
                is File -> "Saved ${result.name}"
                is List<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val files = result.filterIsInstance<File>()
                    if (files.isEmpty()) "That tool needs a PDF or image document"
                    else if (files.size == 1) "Saved ${files.first().name}"
                    else "Saved ${files.size} files to Exports"
                }
                else -> "Nothing to do — pick a document first"
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
                        onScan = { route = Route.Scanner },
                        onTool = { category ->
                            if (category == "SCANNER") route = Route.Scanner
                            else route = Route.Toolkit(category)
                        },
                        onQueryChange = viewModel::setQuery,
                        onSortChange = viewModel::setSort,
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
                    onDelete = {
                        viewModel.delete(current.item) { route = Route.Home }
                    },
                    onRename = { newName ->
                        viewModel.rename(current.item, newName)
                        route = Route.Home
                    }
                )

                is Route.Toolkit -> ToolkitScreen(
                    category = current.category,
                    documents = state.documents,
                    onBack = { route = Route.Home },
                    onPick = { doc -> runTool(current.category, doc) },
                    onAction = { _, _ ->
                        Toast.makeText(
                            context,
                            "This tool lands in Phase 2 — the ready tools are Convert, Merge, Split and Extract.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )

                Route.Settings -> SettingsScreen(
                    documentCount = state.documents.size,
                    totalBytes = state.documents.sumOf { it.sizeBytes },
                    onBack = { route = Route.Home }
                )
            }
        }
    }
}
