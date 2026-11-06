package com.fylio.app.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.data.FylioStore
import com.fylio.app.util.FileOps
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Navigator for the scan pipeline. Back always steps one screen towards the camera. */
private sealed interface ScanRoute {
    data object Camera : ScanRoute
    data class Crop(val pageId: String) : ScanRoute
    data object Enhance : ScanRoute
}

/**
 * Hosts the whole scan pipeline (camera → crop → enhance → PDF) with a single
 * [ScanSessionViewModel], so pages survive every back navigation.
 */
@Composable
fun ScanFlowScreen(
    viewModel: ScanSessionViewModel? = null,
    onExit: () -> Unit,
    onSaved: (File, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session: ScanSessionViewModel = viewModel
        ?: androidx.lifecycle.viewmodel.compose.viewModel<ScanSessionViewModel>()
    val state by session.state.collectAsState()

    var route by remember { mutableStateOf<ScanRoute>(ScanRoute.Camera) }
    var compiling by remember { mutableStateOf(false) }
    var compiledPages by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    // True when the camera was opened from the enhance screen via "Add page", so back
    // returns to the session instead of dropping the user out of the pipeline.
    var addingToSession by remember { mutableStateOf(false) }

    // ---------------------------------------------------------- navigation
    fun leaveFlow() {
        session.clear()
        onExit()
    }

    /** Camera -> crop of the freshly captured page, or straight back home when empty. */
    fun openCrop(pageId: String) {
        session.setActive(pageId)
        addingToSession = false
        route = ScanRoute.Crop(pageId)
    }

    when (val current = route) {
        is ScanRoute.Camera -> CameraScanScreen(
            session = session,
            state = state,
            onBack = {
                if (addingToSession && state.pages.isNotEmpty()) {
                    addingToSession = false
                    route = ScanRoute.Enhance
                } else {
                    leaveFlow()
                }
            },
            onReviewPage = { pageId -> openCrop(pageId) }
        )

        is ScanRoute.Crop -> CropEditorScreen(
            session = session,
            state = state,
            pageId = current.pageId,
            onBack = { route = ScanRoute.Camera },
            onApplied = { route = ScanRoute.Enhance }
        )

        ScanRoute.Enhance -> EnhanceScanScreen(
            session = session,
            state = state,
            onBackToCrop = {
                val active = state.active
                if (active != null) route = ScanRoute.Crop(active.id) else route = ScanRoute.Camera
            },
            onAddPage = {
                addingToSession = true
                route = ScanRoute.Camera
            },
            onSave = { request ->
                compiling = true
                totalPages = state.pages.size
                compiledPages = 0
                scope.launch {
                    val pages = state.pages.toList()
                    val rendered = withContext(Dispatchers.IO) {
                        pages.mapNotNull { page ->
                            session.renderForExport(page, request.quality.maxDim)
                        }
                    }
                    val target = withContext(Dispatchers.IO) {
                        FylioStore.uniqueFile(
                            FylioStore.exportsDir(context),
                            request.fileName.removeSuffix(".pdf"),
                            "pdf"
                        )
                    }
                    val compiled = withContext(Dispatchers.IO) {
                        ScanPdfCompiler.compile(
                            bitmaps = rendered,
                            target = target,
                            format = request.format,
                            onPage = { done -> compiledPages = done }
                        )
                    }
                    rendered.forEach { it.recycle() }
                    compiling = false
                    if (compiled != null) {
                        publishToDestination(context, compiled, request.destinationTree)
                        session.clear()
                        if (request.share) {
                            FileOps.share(context, compiled)
                            onSaved(compiled, "Saved ${compiled.name}")
                            onExit()
                        } else {
                            onSaved(compiled, "Saved ${compiled.name}")
                        }
                    }
                }
            }
        )
    }

    if (compiling) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF111827))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color(0xFF2563EB), modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    "Generating PDF",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (totalPages > 0) "Page ${compiledPages.coerceAtMost(totalPages)} of $totalPages"
                    else "Preparing pages",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = {
                        if (totalPages == 0) 0f
                        else compiledPages.toFloat() / totalPages.toFloat()
                    },
                    color = Color(0xFF2563EB),
                    trackColor = Color(0xFF1F2937),
                    modifier = Modifier.size(width = 180.dp, height = 4.dp)
                )
            }
        }
    }
}
