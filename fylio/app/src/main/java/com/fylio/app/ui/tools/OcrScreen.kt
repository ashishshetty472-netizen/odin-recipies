package com.fylio.app.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.core.ocr.OcrEngine
import com.fylio.app.core.pdf.PdfEngine
import com.fylio.app.data.DocType
import com.fylio.app.data.FylioStore
import com.fylio.app.data.db.DocumentEntity
import com.fylio.app.ui.components.FileTypeBadge
import com.fylio.app.ui.components.FylioCard
import com.fylio.app.ui.components.FylioIcon
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.FylioDimens
import com.fylio.app.ui.theme.LocalFylioSurfaces
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** On-device OCR. Pick a PDF or image, get selectable text you can copy or export. */
@Composable
fun OcrScreen(
    documents: List<DocumentEntity>,
    onBack: () -> Unit,
    onExported: (File) -> Unit,
    onToast: (String) -> Unit
) {
    val surfaces = LocalFylioSurfaces.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var picked by remember { mutableStateOf<DocumentEntity?>(null) }
    var page by remember { mutableStateOf(0) }
    var pageCount by remember { mutableStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }

    fun runOcr(doc: DocumentEntity, pageIndex: Int) {
        val file = doc.localPath?.let { File(it) } ?: return
        busy = true
        text = ""
        scope.launch {
            val bmp = withContext(Dispatchers.IO) {
                when (doc.typeLabel) {
                    "PDF" -> PdfEngine.renderPage(file, pageIndex, 2.5f)
                    else -> com.fylio.app.core.image.ImageEngine.decode(file, 1800)
                }
            }
            bitmap = bmp
            if (bmp == null) {
                busy = false
                onToast("Could not open the page")
                return@launch
            }
            val result = OcrEngine.recognize(bmp)
            text = result
            busy = false
            if (result.isBlank()) onToast("No text found on this page")
        }
    }

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
            Text(
                "Text recognition",
                style = MaterialTheme.typography.titleLarge,
                color = surfaces.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        val doc = picked
        if (doc == null) {
            LazyColumn(contentPadding = PaddingValues(FylioDimens.screenPadding)) {
                item {
                    FylioCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FylioIcon("ocr", size = 34.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Fylio extracts printed text on device with ML Kit. Pick a PDF " +
                                    "or an image to begin — nothing leaves the phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = surfaces.textSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Choose a page to recognise",
                        style = MaterialTheme.typography.titleMedium,
                        color = surfaces.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(documents.filter { it.typeLabel == "PDF" || it.typeLabel == "Images" }) { d ->
                    val type = DocType.entries.firstOrNull { it.shortLabel == d.typeLabel } ?: DocType.PDF
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(surfaces.card)
                            .clickable {
                                picked = d
                                page = 0
                                pageCount = if (d.typeLabel == "PDF") {
                                    d.localPath?.let { PdfEngine.pageCount(File(it)) } ?: 0
                                } else 1
                                runOcr(d, 0)
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FileTypeBadge(type, size = 38.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            d.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = surfaces.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        FylioIcon("chevron_right", size = 16.dp)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FylioDimens.screenPadding, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        doc.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = surfaces.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (pageCount > 1) {
                        TextButton(onClick = {
                            if (page > 0) { page--; runOcr(doc, page) }
                        }) { Text("‹", color = if (page > 0) ElectricBlue else surfaces.textSecondary) }
                        Text("${page + 1}/$pageCount", fontSize = 11.sp, color = surfaces.textSecondary)
                        TextButton(onClick = {
                            if (page < pageCount - 1) { page++; runOcr(doc, page) }
                        }) {
                            Text("›", color = if (page < pageCount - 1) ElectricBlue else surfaces.textSecondary)
                        }
                    }
                    TextButton(onClick = { picked = null; bitmap = null; text = "" }) {
                        Text("Change", color = ElectricBlue, fontSize = 11.sp)
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(horizontal = FylioDimens.screenPadding)
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaces.card)
                        .border(1.dp, surfaces.cardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = bitmap
                    if (busy) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ElectricBlue, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("Recognising text…", fontSize = 11.sp, color = surfaces.textSecondary)
                        }
                    } else if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Source page",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FylioDimens.screenPadding, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallAction("Copy", "copy") {
                        clipboard.setText(AnnotatedString(text))
                        onToast("Text copied")
                    }
                    SmallAction("Export TXT", "text_to_pdf") {
                        val out = with(context) {
                            FylioStore.uniqueFile(
                                FylioStore.exportsDir(this),
                                doc.nameWithoutExtension + "_ocr",
                                "txt"
                            )
                        }
                        runCatching { out.writeText(text) }
                        onExported(out)
                    }
                    SmallAction("Export PDF", "pdf") {
                        val out = with(context) {
                            FylioStore.uniqueFile(
                                FylioStore.exportsDir(this),
                                doc.nameWithoutExtension + "_ocr",
                                "pdf"
                            )
                        }
                        PdfEngine.textToPdf(text.ifBlank { "(no text)" }, "OCR · ${doc.name}", out)
                        onExported(out)
                    }
                }

                val surfacesLocal = surfaces
                SelectionContainer(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = FylioDimens.screenPadding)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfacesLocal.card)
                ) {
                    LazyColumn(contentPadding = PaddingValues(12.dp)) {
                        item {
                            Text(
                                text.ifBlank { if (busy) "" else "No text recognised yet." },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 17.sp
                                ),
                                color = surfacesLocal.textPrimary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SmallAction(label: String, icon: String, onClick: () -> Unit) {
    val surfaces = LocalFylioSurfaces.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(surfaces.elevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FylioIcon(icon, size = 13.dp)
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = surfaces.textPrimary)
    }
}
