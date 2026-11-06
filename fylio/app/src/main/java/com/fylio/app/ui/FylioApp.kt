package com.fylio.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.meta.BuildInfo
import com.fylio.app.core.office.OfficeKind
import com.fylio.app.core.office.OfficeParser
import com.fylio.app.core.pdf.PdfEngine
import com.fylio.app.data.DocType
import com.fylio.app.data.db.DocumentEntity
import com.fylio.app.ui.components.FylioIcon
import com.fylio.app.ui.screens.FilesScreen
import com.fylio.app.ui.screens.HomeScreen
import com.fylio.app.ui.screens.ReaderSettings
import com.fylio.app.ui.screens.SettingsScreen
import com.fylio.app.ui.screens.ToolsScreen
import com.fylio.app.ui.scanner.ScanFlowScreen
import com.fylio.app.ui.tools.AnnotateScreen
import com.fylio.app.ui.tools.OcrScreen
import com.fylio.app.ui.tools.ToolRunner
import com.fylio.app.ui.tools.ToolSpec
import com.fylio.app.ui.viewer.ImageViewer
import com.fylio.app.ui.viewer.PdfViewerScreen
import com.fylio.app.ui.viewer.SheetViewer
import com.fylio.app.ui.viewer.SlidesViewer
import com.fylio.app.ui.viewer.TextViewer
import com.fylio.app.ui.viewer.UnsupportedOfficeViewer
import com.fylio.app.ui.viewer.WordViewer
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.FylioTheme
import com.fylio.app.ui.theme.LocalFylioSurfaces
import com.fylio.app.util.FileOps
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Full-screen destinations pushed above the tab bar. */
private sealed interface Route {
    data object Tabs : Route
    data class Reader(val uri: String) : Route
    data class Tool(val toolId: String) : Route
    data object Scanner : Route
    data class Annotate(val uri: String, val page: Int) : Route
    data object Ocr : Route
    data object About : Route
}

/**
 * Application shell: four tabs, the document readers and every tool screen.
 * All state lives in [FylioViewModel]; this file only routes intents to it.
 */
@Composable
fun FylioApp(
    viewModel: FylioViewModel,
    incomingUri: Uri?,
    onIncomingHandled: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val selection by viewModel.selection.collectAsState()

    var tab by remember { mutableStateOf(Tab.HOME) }
    var route by remember { mutableStateOf<Route>(Route.Tabs) }
    var readerSettings by remember { mutableStateOf(ReaderSettings()) }
    var busy by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    fun toast(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    // ---------------------------------------------------------------- pickers
    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> viewModel.importUris(uris) }

    /** Opens the reader for a document URI, materialising SAF files when needed. */
    fun openDocument(entity: DocumentEntity) {
        scope.launch {
            val reachable = viewModel.isReachable(entity)
            if (!reachable) {
                toast("${entity.name} is no longer available")
                return@launch
            }
            busy = "Opening ${entity.name}"
            val file = withContext(Dispatchers.IO) { viewModel.materialize(entity) }
            busy = null
            if (file == null) {
                toast("Could not open ${entity.name}")
                return@launch
            }
            viewModel.openDocument(entity.uri, if (entity.typeLabel == "PDF") PdfEngine.pageCount(file) else entity.pageCount)
            route = Route.Reader(entity.uri)
        }
    }

    // "Open with Fylio"
    LaunchedEffect(incomingUri) {
        val uri = incomingUri ?: return@LaunchedEffect
        onIncomingHandled()
        viewModel.importUris(listOf(uri))
        toast("Imported into Fylio")
    }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbar.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    val surfaces = LocalFylioSurfaces.current

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        (slideInHorizontally { it / 6 } + fadeIn(tween(180)))
                            .togetherWith(slideOutHorizontally { -it / 8 } + fadeOut(tween(140)))
                    },
                    label = "route"
                ) { current ->
                    when (current) {
                        Route.Tabs -> AnimatedContent(targetState = tab, label = "tab") { t ->
                            when (t) {
                                Tab.HOME -> HomeScreen(
                                    state = state,
                                    onSearch = { tab = Tab.FILES },
                                    onScan = { route = Route.Scanner },
                                    onTool = { id ->
                                        when (id) {
                                            "files", "recent" -> tab = Tab.FILES
                                            "settings", "theme" -> tab = Tab.SETTINGS
                                            "tools", "converter" -> tab = Tab.TOOLS
                                            else -> route = Route.Tool(id)
                                        }
                                    },
                                    onOpenEntity = { openDocument(it) },
                                    onSeeAllFiles = { tab = Tab.FILES },
                                    onSettings = { tab = Tab.SETTINGS }
                                )

                                Tab.FILES -> FilesScreen(
                                    state = state,
                                    selection = selection,
                                    onQuery = viewModel::setQuery,
                                    onFilter = viewModel::setFilter,
                                    onSort = viewModel::setSort,
                                    onToggleGrid = viewModel::toggleGrid,
                                    onImport = { importPicker.launch(arrayOf("*/*")) },
                                    onOpen = { openDocument(it) },
                                    onLongPress = viewModel::toggleSelection,
                                    onToggleSelect = viewModel::toggleSelection,
                                    onSelectAll = viewModel::selectAll,
                                    onClearSelection = viewModel::clearSelection,
                                    onDeleteSelected = { viewModel.deleteSelected() }
                                )

                                Tab.TOOLS -> ToolsScreen(onTool = { route = Route.Tool(it) })

                                Tab.SETTINGS -> SettingsScreen(
                                    state = state,
                                    reader = readerSettings,
                                    onReaderChange = { readerSettings = it },
                                    onClearCache = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                context.cacheDir.deleteRecursively()
                                                FylioStore.scansDir(context).listFiles()
                                                    ?.filter { it.length() == 0L }
                                                    ?.forEach { it.delete() }
                                            }
                                            toast("Cache cleared")
                                        }
                                    },
                                    onAbout = { route = Route.About },
                                    onManagePermissions = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                    Uri.fromParts("package", context.packageName, null)
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        is Route.Reader -> ReaderHost(
                            uri = current.uri,
                            viewModel = viewModel,
                            readerSettings = readerSettings,
                            onBack = { route = Route.Tabs },
                            onAnnotate = { file, page ->
                                route = Route.Annotate(Uri.fromFile(file).toString(), page)
                            },
                            onToast = ::toast
                        )

                        is Route.Tool -> {
                            val spec = ToolSpec.all.firstOrNull { it.id == current.toolId }
                            if (current.toolId in SCAN_FLOW_TOOLS) {
                                ScanFlowScreen(
                                    onExit = { route = Route.Tabs },
                                    onSaved = { file, note ->
                                        viewModel.registerLocalFile(file, note)
                                        route = Route.Reader(Uri.fromFile(file).toString())
                                    }
                                )
                            } else if (spec == null) {
                                MissingTool(onBack = { route = Route.Tabs })
                            } else {
                                ToolRunner(
                                    spec = spec,
                                    documents = state.documents,
                                    onBack = { route = Route.Tabs },
                                    onPickViaSaf = { importPicker.launch(arrayOf("*/*")) },
                                    onFinished = { files, note ->
                                        viewModel.registerLocalFile(files.first(), note)
                                        toast(note)
                                        route = Route.Tabs
                                    },
                                    onOpenResult = { file ->
                                        viewModel.registerLocalFile(file)
                                        route = Route.Reader(Uri.fromFile(file).toString())
                                    },
                                    onToast = ::toast
                                )
                            }
                        }

                        Route.Scanner -> ScanFlowScreen(
                            onExit = { route = Route.Tabs },
                            onSaved = { file, note ->
                                viewModel.registerLocalFile(file, note)
                                route = Route.Reader(Uri.fromFile(file).toString())
                            }
                        )

                        is Route.Annotate -> AnnotateScreen(
                            pdfFile = File(Uri.parse(current.uri).path ?: ""),
                            pageIndex = current.page,
                            initialPage = current.page,
                            onBack = { route = Route.Tabs },
                            onSaved = { file ->
                                viewModel.registerLocalFile(file, "Saved annotated copy")
                                route = Route.Reader(Uri.fromFile(file).toString())
                            }
                        )

                        Route.Ocr -> OcrScreen(
                            documents = state.documents,
                            onBack = { route = Route.Tabs },
                            onExported = { file ->
                                viewModel.registerLocalFile(file)
                                toast("Saved ${file.name}")
                            },
                            onToast = ::toast
                        )

                        Route.About -> AboutScreen(onBack = { route = Route.Tabs })
                    }
                }
            }

            if (route is Route.Tabs) {
                FylioBottomBar(selected = tab, onSelect = { tab = it })
            }
        }

        // snackbar host
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp, start = 16.dp, end = 16.dp)
        )

        busy?.let { label ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(surfaces.card)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = ElectricBlue, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(label, style = MaterialTheme.typography.bodySmall, color = surfaces.textSecondary)
                }
            }
        }
    }
}

// -------------------------------------------------------------------- readers

/** Materialises the document once, then dispatches to the right renderer. */
@Composable
private fun ReaderHost(
    uri: String,
    viewModel: FylioViewModel,
    readerSettings: ReaderSettings,
    onBack: () -> Unit,
    onAnnotate: (File, Int) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val surfaces = LocalFylioSurfaces.current
    var entity by remember(uri) { mutableStateOf<DocumentEntity?>(null) }
    var file by remember(uri) { mutableStateOf<File?>(null) }
    var error by remember(uri) { mutableStateOf<String?>(null) }

    LaunchedEffect(uri) {
        val e = viewModel.byUri(uri)
        if (e == null) {
            error = "Document not found"
            return@LaunchedEffect
        }
        entity = e
        val f = viewModel.materialize(e)
        if (f == null || !f.exists()) error = "The file could not be opened"
        else file = f
    }

    val e = entity
    val f = file
    if (error != null) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FylioIcon("details", size = 44.dp)
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = surfaces.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack) { Text("Back", color = ElectricBlue) }
        }
        return
    }
    if (e == null || f == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ElectricBlue)
        }
        return
    }

    val type = DocType.fromName(e.name)
    when (type) {
        DocType.PDF -> PdfViewerScreen(
            entity = e,
            file = f,
            onBack = onBack,
            onProgress = { page, count -> viewModel.saveProgress(e.uri, page, count) },
            onAnnotate = { page -> onAnnotate(f, page) },
            onShare = { FileOps.share(context, f) },
            onDelete = {
                viewModel.delete(e)
                onBack()
            }
        )

        DocType.IMAGE -> ImageViewer(file = f, name = e.name, onBack = onBack)

        DocType.TEXT -> TextViewer(
            file = f,
            fontSize = readerSettings.fontSize,
            darkReader = readerSettings.darkReader,
            onBack = onBack
        )

        DocType.DOC, DocType.PPT, DocType.XLS -> {
            if (OfficeParser.isLegacyBinary(f)) {
                UnsupportedOfficeViewer(name = e.name, onOpenWith = { FileOps.openWith(context, f) })
            } else {
                val doc = remember(f.absolutePath) { OfficeParser.parse(f) }
                if (doc == null) {
                    UnsupportedOfficeViewer(name = e.name, onOpenWith = { FileOps.openWith(context, f) })
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(surfaces.card)
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onBack) { Text("‹ Back", color = ElectricBlue) }
                            Text(
                                e.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = surfaces.textPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            TextButton(onClick = { FileOps.share(context, f) }) {
                                Text("Share", color = ElectricBlue, fontSize = 12.sp)
                            }
                        }
                        when (doc.kind) {
                            OfficeKind.PRESENTATION -> SlidesViewer(
                                doc = doc,
                                fontSize = readerSettings.fontSize,
                                initialPage = e.lastPage,
                                onPageChange = { p -> viewModel.saveProgress(e.uri, p, doc.slides.size) }
                            )
                            OfficeKind.WORD -> WordViewer(
                                doc = doc,
                                fontSize = readerSettings.fontSize,
                                darkReader = readerSettings.darkReader,
                                initialPage = e.lastPage,
                                onPageChange = { p, total -> viewModel.saveProgress(e.uri, p, total) }
                            )
                            OfficeKind.SPREADSHEET -> SheetViewer(doc = doc)
                        }
                    }
                }
            }
        }

        else -> UnsupportedOfficeViewer(
            name = e.name,
            onOpenWith = { FileOps.openWith(context, f) }
        )
    }
}

// ------------------------------------------------------------------ misc bits

@Composable
private fun MissingTool(onBack: () -> Unit) {
    val surfaces = LocalFylioSurfaces.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Tool unavailable", style = MaterialTheme.typography.titleMedium, color = surfaces.textPrimary)
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onBack) { Text("Back", color = ElectricBlue) }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val surfaces = LocalFylioSurfaces.current
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
            Text("What's inside", style = MaterialTheme.typography.titleLarge, color = surfaces.textPrimary)
        }
        Column(Modifier.padding(20.dp)) {
            listOf(
                "PDF reader" to "Continuous scroll, pinch zoom, page jump, in-document text search, print, properties.",
                "Presentations" to "PPTX parsed offline — real slide text, true aspect ratio, thumbnail strip, fullscreen.",
                "Documents" to "DOCX reflowed with font scaling and a dark reading mode.",
                "Spreadsheets" to "XLSX grid with sheet tabs, frozen headers and synced scrolling.",
                "Scanner" to "CameraX capture, flash, optional edge guide, crop, rotate, brightness, contrast, B&W, multi-page PDF export.",
                "PDF tools" to "Merge, split, extract, delete, reorder, rotate, compress, protect, unlock, watermark, page numbers.",
                "Converters" to "PDF→JPG/PNG, images→PDF, DOCX/PPTX/XLSX→PDF, CSV→XLSX, TXT/HTML→PDF, JPG↔PNG/WEBP.",
                "OCR" to "ML Kit on-device text recognition with copy and TXT/PDF export.",
                "Annotation" to "Ink, highlight, underline, strikethrough, text boxes and signatures baked into a copy.",
                "History" to "Room keeps reading position and page counts; missing files are flagged, never crash."
            ).forEach { (title, body) ->
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = surfaces.textPrimary,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Text(body, style = MaterialTheme.typography.bodySmall, color = surfaces.textSecondary)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Build ${BuildInfo.version} · offline only · no analytics",
                style = MaterialTheme.typography.labelSmall,
                color = surfaces.textSecondary
            )
        }
    }
}

/** Tool ids that open the camera-based scan pipeline instead of the generic runner. */
private val SCAN_FLOW_TOOLS = setOf("scanner", "camera_to_pdf", "create_pdf")

/** Time-aware greeting for the home header. */
private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 5 -> "Working late"
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Good night"
    }
}
