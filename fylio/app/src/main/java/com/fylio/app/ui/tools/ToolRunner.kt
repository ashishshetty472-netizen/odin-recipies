package com.fylio.app.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.core.image.ImageEngine
import com.fylio.app.core.office.OfficeDocument
import com.fylio.app.core.office.OfficeKind
import com.fylio.app.core.office.OfficeParser
import com.fylio.app.core.pdf.PdfEngine
import com.fylio.app.core.pdf.PdfTextSearch
import com.fylio.app.data.DocType
import com.fylio.app.data.FylioStore
import com.fylio.app.data.db.DocumentEntity
import com.fylio.app.ui.components.FylioIcon
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.LocalFylioSurfaces
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Declarative description of a tool: which formats it accepts, how many files, and
 * what controls it exposes. Keeping this in one table means the Tools grid, the
 * picker filtering and the runner never drift apart.
 */
data class ToolSpec(
    val id: String,
    val title: String,
    val description: String,
    val accepts: Set<DocType>,
    val multi: Boolean = false,
    val action: String,
    val options: Set<Option> = emptySet(),
    val routeTo: Destination = Destination.FILE
) {
    enum class Option { PAGE_RANGE, PASSWORD, WATERMARK_TEXT, QUALITY, ROTATION, FILENAME, PAGE_SIZE, ORIENTATION }
    enum class Destination { FILE, RESULT_LIST, OPEN_FIRST, ANNOTATE }

    companion object {
        private val pdfs = setOf(DocType.PDF)
        private val images = setOf(DocType.IMAGE)
        private val office = setOf(DocType.DOC, DocType.PPT, DocType.XLS)
        private val textish = setOf(DocType.TEXT)

        val all: List<ToolSpec> = listOf(
            // ---------------------------------------------------------- PDF tools
            ToolSpec("merge_pdf", "Merge PDFs", "Combine several PDFs into one document, in the order you pick them.",
                pdfs, multi = true, action = "Merge", routeTo = Destination.OPEN_FIRST),
            ToolSpec("split_pdf", "Split PDF", "Split every page into its own PDF.",
                pdfs, action = "Split", routeTo = Destination.RESULT_LIST),
            ToolSpec("extract_pages", "Extract pages", "Keep a page range and save it as a new PDF.",
                pdfs, action = "Extract", options = setOf(ToolSpec.Option.PAGE_RANGE), routeTo = Destination.OPEN_FIRST),
            ToolSpec("delete_pages", "Delete pages", "Remove a page range, keeping the rest.",
                pdfs, action = "Delete pages", options = setOf(ToolSpec.Option.PAGE_RANGE), routeTo = Destination.OPEN_FIRST),
            ToolSpec("reorder_pages", "Reorder pages", "Reverse the page order (1,2,3 → 3,2,1).",
                pdfs, action = "Reverse order", routeTo = Destination.OPEN_FIRST),
            ToolSpec("rotate_pages", "Rotate pages", "Rotate every page by 90°, 180° or 270°.",
                pdfs, action = "Rotate", options = setOf(ToolSpec.Option.ROTATION), routeTo = Destination.OPEN_FIRST),
            ToolSpec("compress_pdf", "Compress PDF", "Re-encode pages as optimised JPEGs to shrink the file.",
                pdfs, action = "Compress", options = setOf(ToolSpec.Option.QUALITY), routeTo = Destination.OPEN_FIRST),
            ToolSpec("protect_pdf", "Protect PDF", "Add a password that opens the document.",
                pdfs, action = "Protect", options = setOf(ToolSpec.Option.PASSWORD), routeTo = Destination.FILE),
            ToolSpec("unlock_pdf", "Unlock PDF", "Remove the password from a PDF you own.",
                pdfs, action = "Unlock", options = setOf(ToolSpec.Option.PASSWORD), routeTo = Destination.OPEN_FIRST),
            ToolSpec("watermark", "Watermark", "Stamp diagonal text on every page.",
                pdfs, action = "Apply watermark", options = setOf(ToolSpec.Option.WATERMARK_TEXT), routeTo = Destination.OPEN_FIRST),
            ToolSpec("add_page_numbers", "Page numbers", "Stamp \"n / total\" at the bottom of every page.",
                pdfs, action = "Add numbers", routeTo = Destination.OPEN_FIRST),
            ToolSpec("print_pdf", "Print", "Send a document to the system print service.",
                pdfs, action = "Print", routeTo = Destination.FILE),

            // ---------------------------------------------------------- creating
            ToolSpec("blank_pdf", "Blank PDF", "Create an empty A4 PDF ready for annotation.",
                setOf(DocType.OTHER, DocType.PDF, DocType.TEXT, DocType.DOC, DocType.XLS, DocType.PPT, DocType.IMAGE),
                action = "Create blank PDF", options = setOf(ToolSpec.Option.FILENAME), routeTo = Destination.OPEN_FIRST),
            ToolSpec("images_to_pdf", "Images to PDF", "Turn photos or scans into a PDF, one page per image.",
                images, multi = true, action = "Create PDF", options = setOf(ToolSpec.Option.FILENAME, ToolSpec.Option.PAGE_SIZE),
                routeTo = Destination.OPEN_FIRST),
            ToolSpec("images_to_pdf_c", "Create PDF", "Turn photos or scans into a PDF, one page per image.",
                images, multi = true, action = "Create PDF", options = setOf(ToolSpec.Option.FILENAME, ToolSpec.Option.PAGE_SIZE),
                routeTo = Destination.OPEN_FIRST),
            ToolSpec("camera_to_pdf", "Camera to PDF", "Capture pages with the camera and save a PDF.",
                images, multi = true, action = "Create PDF", routeTo = Destination.OPEN_FIRST),
            ToolSpec("scanner", "Scanner", "Capture pages with the camera.", images, action = "Open scanner"),

            // -------------------------------------------------------- converting
            ToolSpec("pdf_to_images", "PDF to images", "Export every page as a JPEG image.",
                pdfs, action = "Export JPGs", options = setOf(ToolSpec.Option.QUALITY), routeTo = Destination.RESULT_LIST),
            ToolSpec("pdf_to_jpg", "PDF to JPG", "Export every page as a JPEG image.",
                pdfs, action = "Export JPGs", options = setOf(ToolSpec.Option.QUALITY), routeTo = Destination.RESULT_LIST),
            ToolSpec("pdf_to_png", "PDF to PNG", "Export every page as a lossless PNG.",
                pdfs, action = "Export PNGs", routeTo = Destination.RESULT_LIST),
            ToolSpec("docx_to_pdf", "DOCX to PDF", "Render a Word document to a paginated PDF.",
                office, action = "Convert to PDF", routeTo = Destination.OPEN_FIRST),
            ToolSpec("pptx_to_pdf", "PPTX to PDF", "Render slides to a landscape PDF, one slide per page.",
                office, action = "Convert to PDF", routeTo = Destination.OPEN_FIRST),
            ToolSpec("xlsx_to_pdf", "XLSX to PDF", "Print the sheet grid to a PDF table.",
                office, action = "Convert to PDF", routeTo = Destination.OPEN_FIRST),
            ToolSpec("txt_to_pdf", "TXT to PDF", "Lay plain text out on A4 pages.",
                textish, action = "Convert to PDF", routeTo = Destination.OPEN_FIRST),
            ToolSpec("html_to_pdf", "HTML to PDF", "Strip HTML to readable text and lay it out on A4.",
                textish, action = "Convert to PDF", routeTo = Destination.OPEN_FIRST),
            ToolSpec("text_to_pdf", "Text to PDF", "Lay plain text out on A4 pages.",
                textish, action = "Convert to PDF", routeTo = Destination.OPEN_FIRST),
            ToolSpec("documents_to_pdf", "Document to PDF", "Convert a document or text file to PDF.",
                office + textish, action = "Convert to PDF", routeTo = Destination.OPEN_FIRST),
            ToolSpec("csv_to_xlsx", "CSV to XLSX", "Wrap CSV data in a real SpreadsheetML workbook.",
                setOf(DocType.TEXT, DocType.XLS), action = "Convert to XLSX", routeTo = Destination.FILE),
            ToolSpec("jpg_to_png", "JPG ⇄ PNG", "Convert an image between JPEG and PNG.",
                images, action = "Convert image", options = setOf(ToolSpec.Option.FILENAME), routeTo = Destination.OPEN_FIRST),
            ToolSpec("webp_to_jpg_png", "WEBP ⇄ JPG", "Convert a WEBP image to JPEG.",
                images, action = "Convert image", routeTo = Destination.OPEN_FIRST),
            ToolSpec("converter", "Converter", "Convert between image and document formats.",
                images + pdfs + office + textish, action = "Convert", routeTo = Destination.OPEN_FIRST),

            // ------------------------------------------------------------ editing
            ToolSpec("annotate", "Annotate", "Draw, highlight, underline and sign on a PDF.",
                pdfs, action = "Annotate", routeTo = Destination.ANNOTATE),
            ToolSpec("sign_pdf", "Sign PDF", "Add a handwritten signature to a PDF.",
                pdfs, action = "Sign", routeTo = Destination.ANNOTATE),
            ToolSpec("ocr", "Text recognition", "Extract printed text with on-device ML Kit.",
                pdfs + images, action = "Recognise text"),
            ToolSpec("form_fill", "Fill form", "Flatten text onto a PDF as filled fields.",
                pdfs, action = "Open annotator", routeTo = Destination.ANNOTATE),
            ToolSpec("repair_pdf", "Repair PDF", "Rebuild a damaged PDF page by page.",
                pdfs, action = "Repair", routeTo = Destination.OPEN_FIRST),

            // ---------------------------------------------------------- utilities
            ToolSpec("crop_pdf", "Crop pages", "Re-render pages to trim white margins.",
                pdfs, action = "Crop", routeTo = Destination.OPEN_FIRST),
            ToolSpec("details", "File info", "See size, pages and where the file lives.",
                DocType.entries.toSet().minus(DocType.OTHER), action = "Show details"),
            ToolSpec("favourite", "Bookmark", "Pin a document to the top of the library.",
                DocType.entries.toSet().minus(DocType.OTHER), action = "Toggle bookmark"),
            ToolSpec("search", "Search", "Find text inside any PDF in your library.",
                pdfs, action = "Search"),
            ToolSpec("print_pdf_alias", "Print", "Send a document to the system print service.",
                pdfs, action = "Print"),
            ToolSpec("permissions", "Permissions", "Review what Fylio is allowed to access.",
                setOf(DocType.OTHER), action = "Open settings"),
            ToolSpec("clear_cache", "Clear cache", "Delete temporary working files.",
                setOf(DocType.OTHER), action = "Clear cache"),
            ToolSpec("privacy", "Privacy", "How Fylio handles your files.",
                setOf(DocType.OTHER), action = "Read"),
            ToolSpec("about", "About", "What's inside this build.",
                setOf(DocType.OTHER), action = "Read"),
            ToolSpec("feedback", "Feedback", "Share this build with someone.",
                setOf(DocType.OTHER), action = "Share"),
            ToolSpec("theme", "Theme", "Light, dark or follow the system.",
                setOf(DocType.OTHER), action = "Open settings"),
            ToolSpec("settings", "Settings", "Open Fylio's settings.",
                setOf(DocType.OTHER), action = "Open settings"),
            ToolSpec("create_pdf", "Create PDF", "Build a PDF from images.",
                images, multi = true, action = "Create PDF", routeTo = Destination.OPEN_FIRST)
        )
    }
}

/**
 * Generic tool screen: shows the spec, lets the user configure options, runs the work
 * off the main thread and reports the produced files.
 */
@Composable
fun ToolRunner(
    spec: ToolSpec,
    documents: List<DocumentEntity>,
    onBack: () -> Unit,
    onPickViaSaf: () -> Unit,
    onFinished: (files: List<File>, note: String) -> Unit,
    onOpenResult: (File) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val surfaces = LocalFylioSurfaces.current

    var selection by remember(spec.id) { mutableStateOf<Set<String>>(emptySet()) }
    var busy by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<File>>(emptyList()) }

    // option state
    var rangeStart by remember { mutableStateOf("1") }
    var rangeEnd by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var quality by remember { mutableIntStateOf(85) }
    var rotation by remember { mutableIntStateOf(90) }
    var fileName by remember { mutableStateOf("") }
    var a4 by remember { mutableStateOf(true) }

    val candidates = documents.filter { DocType.fromName(it.name) in spec.accepts }

    fun run() {
        val chosen = selection.mapNotNull { uri -> candidates.firstOrNull { it.uri == uri } }
        if (chosen.isEmpty() && spec.id !in setOf("blank_pdf")) {
            onToast("Pick a document first")
            return
        }
        busy = true
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { execute(spec, chosen, Options(
                    rangeStart = rangeStart.toIntOrNull() ?: 1,
                    rangeEnd = rangeEnd.toIntOrNull(),
                    password = password,
                    watermark = watermarkText,
                    quality = quality,
                    rotation = rotation,
                    fileName = fileName.ifBlank { null },
                    pageA4 = a4,
                    context = context
                )) }.getOrElse { t ->
                    ToolResult(emptyList(), t.message ?: "Something went wrong")
                }
            }
            busy = false
            if (outcome.files.isEmpty()) {
                onToast(outcome.note.ifBlank { "Nothing was produced" })
            } else {
                when (spec.routeTo) {
                    ToolSpec.Destination.OPEN_FIRST -> {
                        onFinished(outcome.files, outcome.note)
                        onOpenResult(outcome.files.first())
                    }
                    ToolSpec.Destination.RESULT_LIST -> results = outcome.files
                    else -> {
                        onFinished(outcome.files, outcome.note)
                        results = outcome.files
                    }
                }
            }
        }
    }

    // annotate needs a different screen
    if (spec.routeTo == ToolSpec.Destination.ANNOTATE) {
        AnnotatePicker(
            candidates = candidates,
            onBack = onBack,
            onPick = { entity ->
                val path = entity.localPath
                if (path != null) {
                    // handled by the caller through onOpenResult then annotate route
                    onOpenResult(File(path))
                } else onToast("Import the file once so it can be annotated")
            }
        )
        return
    }

    ToolShell(
        title = spec.title,
        iconId = spec.id,
        description = spec.description,
        documents = candidates,
        selected = selection,
        multiSelect = spec.multi,
        busy = busy,
        actionLabel = spec.action,
        onBack = onBack,
        onToggle = { uri ->
            selection = if (spec.multi) {
                if (uri in selection) selection - uri else selection + uri
            } else setOf(uri)
        },
        onRun = ::run,
        extra = {
            Column {
                ToolSpec.Option.PAGE_RANGE.takeIf { it in spec.options }?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OptionField("From", rangeStart) { rangeStart = it }
                        Spacer(Modifier.width(8.dp))
                        OptionField("To", rangeEnd) { rangeEnd = it }
                    }
                }
                ToolSpec.Option.PASSWORD.takeIf { it in spec.options }?.let {
                    Spacer(Modifier.height(6.dp))
                    OptionField("Password", password) { password = it }
                }
                ToolSpec.Option.WATERMARK_TEXT.takeIf { it in spec.options }?.let {
                    Spacer(Modifier.height(6.dp))
                    OptionField("Watermark text", watermarkText) { watermarkText = it }
                }
                ToolSpec.Option.FILENAME.takeIf { it in spec.options }?.let {
                    Spacer(Modifier.height(6.dp))
                    OptionField("File name (optional)", fileName) { fileName = it }
                }
                ToolSpec.Option.QUALITY.takeIf { it in spec.options }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text("Quality: $quality%", fontSize = 11.sp, color = surfaces.textSecondary)
                    Slider(
                        value = quality.toFloat(),
                        onValueChange = { quality = it.toInt() },
                        valueRange = 40f..95f
                    )
                }
                ToolSpec.Option.ROTATION.takeIf { it in spec.options }?.let {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(90, 180, 270).forEach { deg ->
                            val active = rotation == deg
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) ElectricBlue else surfaces.elevated)
                                    .clickable { rotation = deg }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    "$deg°",
                                    fontSize = 11.sp,
                                    color = if (active) Color.White else surfaces.textPrimary
                                )
                            }
                        }
                    }
                }
                ToolSpec.Option.PAGE_SIZE.takeIf { it in spec.options }?.let {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(true to "A4 pages", false to "Image size").forEach { (isA4, label) ->
                            val active = a4 == isA4
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) ElectricBlue else surfaces.elevated)
                                    .clickable { a4 = isA4 }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    color = if (active) Color.White else surfaces.textPrimary
                                )
                            }
                        }
                    }
                }
                if (candidates.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(surfaces.elevated)
                            .clickable(onClick = onPickViaSaf)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FylioIcon("folder", size = 14.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Import a file", fontSize = 11.sp, color = surfaces.textPrimary)
                    }
                }
                if (results.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    ResultList(results) { onOpenResult(it) }
                }
            }
        }
    )
}

@Composable
private fun OptionField(label: String, value: String, onChange: (String) -> Unit) {
    val surfaces = LocalFylioSurfaces.current
    TextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        label = { Text(label, fontSize = 11.sp) },
        textStyle = MaterialTheme.typography.bodySmall,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = surfaces.elevated,
            unfocusedContainerColor = surfaces.elevated,
            focusedIndicatorColor = ElectricBlue,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ResultList(files: List<File>, onOpen: (File) -> Unit) {
    val surfaces = LocalFylioSurfaces.current
    Column {
        Text(
            "${files.size} file(s) created",
            style = MaterialTheme.typography.labelMedium,
            color = surfaces.textSecondary
        )
        Spacer(Modifier.height(6.dp))
        files.take(12).forEach { f ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpen(f) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    f.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaces.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text("Open", fontSize = 10.sp, color = ElectricBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AnnotatePicker(
    candidates: List<DocumentEntity>,
    onBack: () -> Unit,
    onPick: (DocumentEntity) -> Unit
) {
    val surfaces = LocalFylioSurfaces.current
    ToolShell(
        title = "Annotate",
        iconId = "draw",
        description = "Pick a PDF, then draw, highlight, underline, strike or sign. " +
            "Saving writes a flattened copy so any viewer shows your marks.",
        documents = candidates,
        selected = emptySet(),
        multiSelect = false,
        busy = false,
        actionLabel = "Annotate",
        onBack = onBack,
        onToggle = { uri -> candidates.firstOrNull { it.uri == uri }?.let(onPick) },
        onRun = { },
        extra = {
            Text(
                "Tap a document below to open it in the annotator.",
                fontSize = 11.sp,
                color = surfaces.textSecondary
            )
        }
    )
}

// ------------------------------------------------------------------- execution

private data class Options(
    val rangeStart: Int,
    val rangeEnd: Int?,
    val password: String,
    val watermark: String,
    val quality: Int,
    val rotation: Int,
    val fileName: String?,
    val pageA4: Boolean,
    val context: android.content.Context
)

private data class ToolResult(val files: List<File>, val note: String)

/** Single place where a tool id becomes real work. */
private fun execute(
    spec: ToolSpec,
    chosen: List<DocumentEntity>,
    opts: Options
): ToolResult {
    val exports = FylioStore.exportsDir(opts.context)
    val source = chosen.firstOrNull()
    fun file(entity: DocumentEntity): File? =
        entity.localPath?.let { File(it) }?.takeIf { it.exists() }

    fun out(base: String, ext: String): File =
        FylioStore.uniqueFile(exports, opts.fileName ?: base, ext)

    return when (spec.id) {
        "merge_pdf" -> {
            val files = chosen.mapNotNull(::file)
            if (files.size < 2) return ToolResult(emptyList(), "Pick at least two PDFs")
            val target = out("Merged", "pdf")
            PdfEngine.merge(files, target)
                ?.let { ToolResult(listOf(it), "Merged ${files.size} PDFs") }
                ?: ToolResult(emptyList(), "Merge failed")
        }

        "split_pdf" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            val parts = PdfEngine.split(f, exports, f.nameWithoutExtension)
            if (parts.isEmpty()) ToolResult(emptyList(), "Split failed")
            else ToolResult(parts, "Split into ${parts.size} PDFs")
        }

        "extract_pages" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            val count = PdfEngine.pageCount(f)
            val from = opts.rangeStart.coerceIn(1, count)
            val to = (opts.rangeEnd ?: count).coerceIn(from, count)
            val target = out("${f.nameWithoutExtension}_p$from-$to", "pdf")
            PdfEngine.extractPages(f, from, to, target)
                ?.let { ToolResult(listOf(it), "Extracted pages $from–$to") }
                ?: ToolResult(emptyList(), "Extract failed")
        }

        "delete_pages" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            val count = PdfEngine.pageCount(f)
            val from = opts.rangeStart.coerceIn(1, count)
            val to = (opts.rangeEnd ?: from).coerceIn(from, count)
            val target = out("${f.nameWithoutExtension}_trimmed", "pdf")
            PdfEngine.deletePages(f, (from..to).toSet(), target)
                ?.let { ToolResult(listOf(it), "Deleted pages $from–$to") }
                ?: ToolResult(emptyList(), "Delete failed")
        }

        "reorder_pages" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            val count = PdfEngine.pageCount(f)
            val target = out("${f.nameWithoutExtension}_reordered", "pdf")
            PdfEngine.reorder(f, (count downTo 1).toList(), target)
                ?.let { ToolResult(listOf(it), "Pages reversed") }
                ?: ToolResult(emptyList(), "Reorder failed")
        }

        "rotate_pages" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            val target = out("${f.nameWithoutExtension}_rotated", "pdf")
            PdfEngine.rotatePages(f, opts.rotation, target)
                ?.let { ToolResult(listOf(it), "Rotated by ${opts.rotation}°") }
                ?: ToolResult(emptyList(), "Rotate failed")
        }

        "compress_pdf" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            val target = out("${f.nameWithoutExtension}_compressed", "pdf")
            val before = f.length()
            PdfEngine.compress(f, opts.quality, 1.4f, target)?.let { result ->
                val after = result.length()
                val saved = if (before > 0) ((before - after) * 100 / before) else 0
                ToolResult(listOf(result), "Saved ${saved.coerceAtLeast(0)}% (${result.length() / 1024} KB)")
            } ?: ToolResult(emptyList(), "Compress failed")
        }

        "protect_pdf" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            if (opts.password.length < 4) return ToolResult(emptyList(), "Use a password of 4+ characters")
            val target = out("${f.nameWithoutExtension}_protected", "pdf")
            PdfEngine.protect(f, opts.password, target)
                ?.let { ToolResult(listOf(it), "Protected with a password") }
                ?: ToolResult(emptyList(), "Protect failed")
        }

        "unlock_pdf" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            val target = out("${f.nameWithoutExtension}_unlocked", "pdf")
            PdfEngine.unlock(f, opts.password, target)
                ?.let { ToolResult(listOf(it), "Password removed") }
                ?: ToolResult(emptyList(), "Wrong password or not encrypted")
        }

        "watermark" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            if (opts.watermark.isBlank()) return ToolResult(emptyList(), "Enter watermark text")
            val target = out("${f.nameWithoutExtension}_watermark", "pdf")
            PdfEngine.watermark(f, opts.watermark, target)
                ?.let { ToolResult(listOf(it), "Watermark applied") }
                ?: ToolResult(emptyList(), "Watermark failed")
        }

        "add_page_numbers" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            val target = out("${f.nameWithoutExtension}_numbered", "pdf")
            PdfEngine.addPageNumbers(f, target)
                ?.let { ToolResult(listOf(it), "Page numbers added") }
                ?: ToolResult(emptyList(), "Could not add page numbers")
        }

        "print_pdf" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            ToolResult(listOf(f), "Ready to print")
        }

        "blank_pdf" -> {
            val target = out(opts.fileName ?: "Blank", "pdf")
            val blank = Bitmap.createBitmap(1240, 1754, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.WHITE)
            }
            PdfEngine.bitmapsToPdf(listOf(blank), target, pageSize = null)
                ?.let { ToolResult(listOf(it), "Blank PDF created") }
                ?: ToolResult(emptyList(), "Could not create PDF")
        }

        "images_to_pdf", "images_to_pdf_c", "create_pdf", "camera_to_pdf" -> {
            val files = chosen.mapNotNull(::file)
            if (files.isEmpty()) return ToolResult(emptyList(), "Pick at least one image")
            val bitmaps = files.mapNotNull { ImageEngine.decode(it, 2400) }
            if (bitmaps.isEmpty()) return ToolResult(emptyList(), "Could not read the images")
            val target = out(opts.fileName ?: "Images", "pdf")
            PdfEngine.bitmapsToPdf(bitmaps, target)
                ?.let { ToolResult(listOf(it), "PDF with ${bitmaps.size} page(s)") }
                ?: ToolResult(emptyList(), "Could not build PDF")
        }

        "scanner" -> ToolResult(emptyList(), "Open the Scanner tab")

        "pdf_to_images", "pdf_to_jpg", "pdf_to_png" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            val png = spec.id == "pdf_to_png"
            val pages = PdfEngine.renderAll(f, 2f)
            if (pages.isEmpty()) return ToolResult(emptyList(), "Could not render pages")
            val written = pages.mapIndexedNotNull { i, bmp ->
                val ext = if (png) "png" else "jpg"
                val target = FylioStore.uniqueFile(exports, "${f.nameWithoutExtension}_page${i + 1}", ext)
                if (png) ImageEngine.savePng(bmp, target)
                else ImageEngine.saveJpeg(bmp, target, opts.quality)
            }
            if (written.isEmpty()) ToolResult(emptyList(), "Export failed")
            else ToolResult(written, "Exported ${written.size} page(s)")
        }

        "docx_to_pdf", "pptx_to_pdf", "xlsx_to_pdf", "documents_to_pdf" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a document")
            val doc = OfficeParser.parse(f)
                ?: return ToolResult(emptyList(), "This format needs a newer Office file")
            val name = opts.fileName ?: f.nameWithoutExtension
            when (doc.kind) {
                OfficeKind.WORD -> {
                    val text = doc.blocks.joinToString("\n\n") { b ->
                        (if (b.isHeading) b.text.uppercase() else b.text)
                    }
                    val target = out(name, "pdf")
                    PdfEngine.textToPdf(text, target, doc.title)
                        ?.let { ToolResult(listOf(it), "Converted to PDF") }
                        ?: ToolResult(emptyList(), "Conversion failed")
                }
                OfficeKind.PRESENTATION -> {
                    val target = out(name, "pdf")
                    val bitmaps = doc.slides.map { slide ->
                        renderSlideBitmap(slide, doc)
                    }
                    if (bitmaps.isEmpty()) return ToolResult(emptyList(), "No slides found")
                    val written = PdfEngine.bitmapsToPdf(bitmaps, target, jpegQuality = 90)
                    written?.let { ToolResult(listOf(it), "Converted ${doc.slides.size} slides") }
                        ?: ToolResult(emptyList(), "Conversion failed")
                }
                OfficeKind.SPREADSHEET -> {
                    val csv = doc.sheets.joinToString("\n\n") { sheet ->
                        sheet.name + "\n" + sheet.rows.joinToString("\n") { row -> row.joinToString("\t") }
                    }
                    val target = out(name, "pdf")
                    PdfEngine.textToPdf(csv, target, doc.title)
                        ?.let { ToolResult(listOf(it), "Converted ${doc.sheets.size} sheet(s)") }
                        ?: ToolResult(emptyList(), "Conversion failed")
                }
            }
        }

        "txt_to_pdf", "html_to_pdf", "text_to_pdf" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a text file")
            var text = runCatching { f.readText() }.getOrDefault("")
            if (spec.id == "html_to_pdf") {
                text = text
                    .replace(Regex("(?is)<(script|style).*?</\\1>"), " ")
                    .replace(Regex("(?i)<br\\s*/?>"), "\n")
                    .replace(Regex("(?i)</p>"), "\n\n")
                    .replace(Regex("<[^>]+>"), " ")
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace(Regex("[ \\t]+"), " ")
            }
            if (text.isBlank()) return ToolResult(emptyList(), "The file is empty")
            val target = out(opts.fileName ?: f.nameWithoutExtension, "pdf")
            PdfEngine.textToPdf(text, target, f.nameWithoutExtension)
                ?.let { ToolResult(listOf(it), "Converted to PDF") }
                ?: ToolResult(emptyList(), "Conversion failed")
        }

        "csv_to_xlsx" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a CSV file")
            val rows = f.readLines().map { line -> line.split(',', ';', '\t').map { it.trim() } }
            val target = out(f.nameWithoutExtension, "xlsx")
            XlsxWriter.write(rows, target, f.nameWithoutExtension)
                ?.let { ToolResult(listOf(it), "Saved ${rows.size} rows") }
                ?: ToolResult(emptyList(), "Could not write the workbook")
        }

        "jpg_to_png", "webp_to_jpg_png" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick an image")
            val bmp = ImageEngine.decode(f, 3000)
                ?: return ToolResult(emptyList(), "Could not decode the image")
            val target = if (f.extension.equals("png", true)) {
                FylioStore.uniqueFile(exports, f.nameWithoutExtension, "jpg")
            } else {
                FylioStore.uniqueFile(exports, f.nameWithoutExtension, "png")
            }
            val written = if (target.extension == "png") ImageEngine.savePng(bmp, target)
            else ImageEngine.saveJpeg(bmp, target, 94)
            written?.let { ToolResult(listOf(it), "Converted to ${it.extension.uppercase()}") }
                ?: ToolResult(emptyList(), "Conversion failed")
        }

        "converter" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a file")
            when (DocType.fromName(f.name)) {
                DocType.IMAGE -> {
                    val bmp = ImageEngine.decode(f, 2400)
                        ?: return ToolResult(emptyList(), "Could not decode the image")
                    val target = FylioStore.uniqueFile(exports, f.nameWithoutExtension, "pdf")
                    PdfEngine.bitmapsToPdf(listOf(bmp), target)
                        ?.let { ToolResult(listOf(it), "Image converted to PDF") }
                        ?: ToolResult(emptyList(), "Conversion failed")
                }
                DocType.PDF -> {
                    val pages = PdfEngine.renderAll(f, 2f)
                    val written = pages.mapIndexedNotNull { i, bmp ->
                        ImageEngine.saveJpeg(
                            bmp,
                            FylioStore.uniqueFile(exports, "${f.nameWithoutExtension}_p${i + 1}", "jpg"),
                            90
                        )
                    }
                    ToolResult(written, "Exported ${written.size} page(s)")
                }
                DocType.TEXT -> {
                    val text = runCatching { f.readText() }.getOrDefault("")
                    val target = FylioStore.uniqueFile(exports, f.nameWithoutExtension, "pdf")
                    PdfEngine.textToPdf(text, target)
                        ?.let { ToolResult(listOf(it), "Text converted to PDF") }
                        ?: ToolResult(emptyList(), "Conversion failed")
                }
                else -> {
                    val doc = OfficeParser.parse(f)
                        ?: return ToolResult(emptyList(), "Unsupported format")
                    val text = when (doc.kind) {
                        OfficeKind.WORD -> doc.blocks.joinToString("\n\n") { it.text }
                        OfficeKind.SPREADSHEET -> doc.sheets.joinToString("\n\n") { s ->
                            s.rows.joinToString("\n") { it.joinToString("\t") }
                        }
                        OfficeKind.PRESENTATION -> doc.slides.joinToString("\n\n") { s ->
                            s.lines.joinToString("\n") { it.text }
                        }
                    }
                    val target = FylioStore.uniqueFile(exports, f.nameWithoutExtension, "pdf")
                    PdfEngine.textToPdf(text, target, doc.title)
                        ?.let { ToolResult(listOf(it), "Converted to PDF") }
                        ?: ToolResult(emptyList(), "Conversion failed")
                }
            }
        }

        "repair_pdf" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            val target = out("${f.nameWithoutExtension}_repaired", "pdf")
            val bitmaps = PdfEngine.renderAll(f, 2f)
            PdfEngine.bitmapsToPdf(bitmaps, target, jpegQuality = 92)
                ?.let { ToolResult(listOf(it), "Rebuilt ${bitmaps.size} page(s)") }
                ?: ToolResult(emptyList(), "Repair failed")
        }

        "crop_pdf" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF")
            val target = out("${f.nameWithoutExtension}_cropped", "pdf")
            val cropped = PdfEngine.renderAll(f, 2.2f).map {
                ImageEngine.crop(it, 0.03f, 0.03f, 0.97f, 0.97f)
            }
            PdfEngine.bitmapsToPdf(cropped, target, jpegQuality = 92)
                ?.let { ToolResult(listOf(it), "Trimmed margins") }
                ?: ToolResult(emptyList(), "Crop failed")
        }

        "search" -> {
            val f = source?.let(::file) ?: return ToolResult(emptyList(), "Pick a PDF to search")
            val hits = PdfTextSearch.search(f, opts.watermark.ifBlank { "the" })
            ToolResult(emptyList(), "${hits.size} match(es) in ${f.name}")
        }

        else -> ToolResult(emptyList(), "This tool needs more input")
    }
}

/** Renders a slide to a bitmap at 1600px wide so PPTX→PDF keeps its aspect ratio. */
private fun renderSlideBitmap(
    slide: com.fylio.app.core.office.Slide,
    doc: OfficeDocument
): Bitmap {
    val width = 1600
    val height = (width / slide.aspectRatio).toInt().coerceAtLeast(400)
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    canvas.drawColor(android.graphics.Color.WHITE)
    val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(15, 23, 42)
        textSize = 62f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val bodyPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(31, 41, 55)
        textSize = 40f
    }
    var y = 120f
    val left = 110f
    val maxWidth = width - left * 2
    slide.lines.forEach { line ->
        val paint = if (line.isTitle) titlePaint else bodyPaint
        val prefix = if (line.bullet && !line.isTitle) "• " else ""
        var current = prefix + line.text
        // wrap
        while (current.isNotEmpty()) {
            var cut = paint.breakText(current, true, maxWidth, null)
            if (cut <= 0) cut = current.length
            canvas.drawText(current.substring(0, cut), left, y, paint)
            current = current.substring(cut).trimStart()
            y += paint.textSize * 1.45f
            if (y > height - 80f) break
        }
        if (line.isTitle) y += 24f
    }
    return bmp
}
