package com.fylio.app.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.core.office.OfficeDocument
import com.fylio.app.data.formatSize
import com.fylio.app.ui.components.FylioIcon
import com.fylio.app.ui.components.Pill
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.LocalFylioSurfaces

// ------------------------------------------------------------------ presentations

/**
 * Slide viewer. Every slide keeps the deck's true aspect ratio (read from
 * presentation.xml), so text never distorts or crops.
 */
@Composable
fun SlidesViewer(
    doc: OfficeDocument,
    fontSize: Float = 1f,
    onPageChange: (Int) -> Unit,
    initialPage: Int = 0
) {
    val surfaces = LocalFylioSurfaces.current
    var index by remember { mutableIntStateOf(initialPage.coerceIn(0, (doc.slides.size - 1).coerceAtLeast(0))) }
    var fullscreen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        if (!fullscreen) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaces.card)
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(doc.slides) { i, slide ->
                    val selected = i == index
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .aspectRatio(slide.aspectRatio.coerceIn(1f, 2f))
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) ElectricBlue else surfaces.cardBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { index = i; onPageChange(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1}",
                            fontSize = 10.sp,
                            color = if (selected) ElectricBlue else surfaces.textSecondary
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (fullscreen) 0.dp else 12.dp)
                .clickable { fullscreen = !fullscreen }
        ) {
            SlideCanvas(
                doc = doc,
                index = index,
                fontSize = fontSize,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(doc.slides.getOrNull(index)?.aspectRatio ?: (16f / 9f))
                    .align(Alignment.Center)
            )
        }

        if (!fullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaces.card)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "‹",
                    fontSize = 24.sp,
                    color = if (index > 0) ElectricBlue else surfaces.textSecondary,
                    modifier = Modifier
                        .clickable(enabled = index > 0) { index--; onPageChange(index) }
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Text(
                    "Slide ${index + 1} of ${doc.slides.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = surfaces.textPrimary
                )
                Text(
                    "›",
                    fontSize = 24.sp,
                    color = if (index < doc.slides.size - 1) ElectricBlue else surfaces.textSecondary,
                    modifier = Modifier
                        .clickable(enabled = index < doc.slides.size - 1) { index++; onPageChange(index) }
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SlideCanvas(
    doc: OfficeDocument,
    index: Int,
    fontSize: Float,
    modifier: Modifier = Modifier
) {
    val slide = doc.slides.getOrNull(index) ?: return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            slide.lines.forEach { line ->
                val size = when {
                    line.isTitle -> 19.sp * fontSize
                    line.level == 0 -> 13.sp * fontSize
                    else -> 12.sp * fontSize
                }
                val weight = if (line.isTitle) FontWeight.Bold else FontWeight.Normal
                Row(modifier = Modifier.padding(vertical = if (line.isTitle) 4.dp else 2.dp)) {
                    if (line.bullet && !line.isTitle) {
                        Text(
                            "•  ",
                            fontSize = size,
                            color = Color(0xFF111827)
                        )
                    }
                    Text(
                        text = line.text,
                        fontSize = size,
                        lineHeight = size * 1.35f,
                        fontWeight = weight,
                        color = if (line.isTitle) Color(0xFF0F172A) else Color(0xFF1F2937)
                    )
                }
            }
            slide.tables.forEach { table ->
                Spacer(Modifier.height(8.dp))
                Column(Modifier.fillMaxWidth()) {
                    table.rows.take(6).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.take(5).forEach { cell ->
                                Text(
                                    cell,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(0.5.dp, Color(0xFFE5E7EB))
                                        .padding(4.dp),
                                    fontSize = 9.sp * fontSize,
                                    color = Color(0xFF111827),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            "${index + 1}",
            modifier = Modifier
                .align(Alignment.BottomEnd),
            fontSize = 8.sp,
            color = Color(0xFF9CA3AF)
        )
    }
}

// ---------------------------------------------------------------------------- word

/** Reflowed DOCX reader with font scaling and a dark/sepia/light reading theme. */
@Composable
fun WordViewer(
    doc: OfficeDocument,
    fontSize: Float,
    darkReader: Boolean,
    initialPage: Int = 0,
    onPageChange: (Int, Int) -> Unit
) {
    // DOCX has no fixed pages, so we paginate the block list for progress purposes.
    val perPage = 28
    val pages = remember(doc.blocks) { doc.blocks.chunked(perPage) }
    var page by remember { mutableIntStateOf(initialPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))) }

    val bg = if (darkReader) Color(0xFF111827) else Color.White
    val fg = if (darkReader) Color(0xFFE5E7EB) else Color(0xFF1F2937)

    Column(
        Modifier
            .fillMaxSize()
            .background(if (darkReader) Color(0xFF0B0F19) else Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (darkReader) Color(0xFF111827) else Color.White)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Page ${page + 1} / ${pages.size.coerceAtLeast(1)}",
                style = MaterialTheme.typography.labelLarge,
                color = if (darkReader) Color(0xFFE5E7EB) else Color(0xFF334155)
            )
            Row {
                Text(
                    "−",
                    fontSize = 18.sp,
                    color = ElectricBlue,
                    modifier = Modifier
                        .clickable { }
                        .padding(horizontal = 10.dp)
                )
                Text(
                    "Page view",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (darkReader) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(pages.size) { p ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    pages[p].forEach { block ->
                        val isHeading = block.isHeading
                        Row {
                            if (block.bullet) {
                                Text(
                                    "•  ",
                                    fontSize = 14.sp * fontSize,
                                    color = fg
                                )
                            }
                            Text(
                                text = block.text,
                                fontSize = (if (isHeading) 17.sp else 14.sp) * fontSize,
                                lineHeight = (if (isHeading) 23.sp else 20.sp) * fontSize,
                                fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal,
                                color = if (isHeading) (if (darkReader) Color.White else Color(0xFF0F172A)) else fg,
                                modifier = Modifier.padding(
                                    top = if (isHeading) 12.dp else 3.dp,
                                    bottom = if (isHeading) 4.dp else 5.dp
                                )
                            )
                        }
                    }
                }
                if (p == page) {
                    androidx.compose.runtime.LaunchedEffect(p) {
                        onPageChange(p, pages.size)
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------- spreadsheet

/** Spreadsheet grid with frozen header row and multi-sheet tabs. */
@Composable
fun SheetViewer(doc: OfficeDocument) {
    val surfaces = LocalFylioSurfaces.current
    var sheetIndex by remember { mutableIntStateOf(0) }
    val sheet = doc.sheets.getOrNull(sheetIndex) ?: return
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        // column headers are part of the frozen block below
        Box(
            Modifier
                .weight(1f)
                .horizontalScroll(hScroll)
                .verticalScroll(vScroll)
                .padding(6.dp)
        ) {
            Column {
                // header row: A, B, C…
                val colCount = sheet.rows.maxOfOrNull { it.size } ?: 0
                Row {
                    Cell("", width = 34.dp, header = true)
                    repeat(colCount) { c ->
                        Cell(columnName(c), width = 96.dp, header = true)
                    }
                }
                sheet.rows.forEachIndexed { r, row ->
                    Row {
                        Cell("${r + 1}", width = 34.dp, header = true)
                        repeat(colCount) { c ->
                            Cell(row.getOrNull(c) ?: "", width = 96.dp)
                        }
                    }
                }
                if (sheet.rows.isEmpty()) {
                    Text(
                        "This sheet is empty",
                        modifier = Modifier.padding(20.dp),
                        color = surfaces.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // sheet tabs
        if (doc.sheets.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaces.card)
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(doc.sheets) { i, s ->
                    val selected = i == sheetIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) ElectricBlue else surfaces.elevated)
                            .clickable { sheetIndex = i }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            s.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else surfaces.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Cell(text: String, width: androidx.compose.ui.unit.Dp, header: Boolean = false) {
    val surfaces = LocalFylioSurfaces.current
    Box(
        modifier = Modifier
            .width(width)
            .heightIn(min = 30.dp)
            .border(0.5.dp, surfaces.cardBorder)
            .background(if (header) surfaces.elevated else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
            color = surfaces.textPrimary,
            maxLines = 2
        )
    }
}

private fun columnName(index: Int): String {
    var n = index
    val sb = StringBuilder()
    while (true) {
        sb.append(('A' + (n % 26)))
        n = n / 26 - 1
        if (n < 0) break
    }
    return sb.reverse().toString()
}

// ------------------------------------------------------------------------ fallbacks

/** Shown when a legacy binary (OLE2) Office file can't be parsed on device. */
@Composable
fun UnsupportedOfficeViewer(name: String, onOpenWith: () -> Unit) {
    val surfaces = LocalFylioSurfaces.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FylioIcon("documents_to_pdf", size = 52.dp)
        Spacer(Modifier.height(14.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            color = surfaces.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "This is the legacy binary format (.doc/.xls/.ppt). Fylio reads the modern " +
                "XML formats (.docx/.xlsx/.pptx) fully offline; for this file, open it in " +
                "another app that ships the legacy parser.",
            style = MaterialTheme.typography.bodySmall,
            color = surfaces.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(ElectricBlue)
                .clickable(onClick = onOpenWith)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text("Open with another app", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}
