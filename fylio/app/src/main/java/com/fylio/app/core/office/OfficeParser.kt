package com.fylio.app.core.office

import android.util.Xml
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document as XmlDocument
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Offline OOXML reader for .docx / .pptx / .xlsx.
 *
 * These are ZIP containers of XML parts, so no third-party converter is needed:
 *  - docx  -> word/document.xml   (paragraphs + runs)
 *  - pptx  -> ppt/slides/slideN.xml + ppt/presentation.xml for ordering
 *  - xlsx  -> xl/worksheets/sheetN.xml + xl/sharedStrings.xml + xl/workbook.xml
 *
 * Legacy binary .doc/.xls/.ppt (OLE2) are detected but not parsed — the UI tells the
 * user the truth instead of showing an empty screen.
 */
object OfficeParser {

    private fun open(file: File) = ZipFile(file)

    private fun readXml(zip: ZipFile, entry: String): XmlDocument? = runCatching {
        val stream: InputStream = zip.getInputStream(zip.getEntry(entry) ?: return null)
        stream.use {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                isExpandEntityReferences = false
                // harden against XXE in untrusted documents
                runCatching {
                    setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                }
            }
            factory.newDocumentBuilder().parse(it)
        }
    }.getOrNull()

    fun parse(file: File): OfficeDocument? = runCatching {
        open(file).use { zip ->
            val names = zip.entries().toList().map { it.name }
            when {
                names.any { it == "word/document.xml" } -> parseDocx(zip, file.name)
                names.any { it.startsWith("ppt/slides/slide") } -> parsePptx(zip, file.name)
                names.any { it.startsWith("xl/worksheets/") } -> parseXlsx(zip, file.name)
                else -> null
            }
        }
    }.getOrNull()

    /** True for legacy OLE2 files (first bytes D0 CF 11 E0). */
    fun isLegacyBinary(file: File): Boolean = runCatching {
        file.inputStream().use { s ->
            val b = ByteArray(8); s.read(b)
            b[0] == 0xD0.toByte() && b[1] == 0xCF.toByte() && b[2] == 0x11.toByte() && b[3] == 0xE0.toByte()
        }
    }.getOrDefault(false)

    // -------------------------------------------------------------------- DOCX

    private fun parseDocx(zip: ZipFile, fileName: String): OfficeDocument {
        val doc = readXml(zip, "word/document.xml")
        val blocks = mutableListOf<TextBlock>()
        if (doc != null) {
            val body = doc.getElementsByTagName("w:body")
            if (body.length > 0) {
                val children = body.item(0).childNodes
                for (i in 0 until children.length) {
                    val node = children.item(i)
                    if (node is Element && (node.tagName == "w:p" || node.tagName.endsWith(":p"))) {
                        val text = paragraphText(node)
                        val style = nodeStyle(node)
                        if (text.isNotBlank()) {
                            blocks.add(
                                TextBlock(
                                    text = text.trim(),
                                    isHeading = style.startsWith("Heading") || style == "Title",
                                    bullet = style.contains("List") || hasBullet(node),
                                    level = if (style.startsWith("Heading")) 1 else 0
                                )
                            )
                        }
                    }
                }
            }
        }
        return OfficeDocument(
            kind = OfficeKind.WORD,
            title = fileName.removeSuffix(".docx").removeSuffix(".DOCX"),
            blocks = blocks
        )
    }

    private fun paragraphText(p: Element): String {
        val sb = StringBuilder()
        val runs = p.getElementsByTagName("w:t")
        for (i in 0 until runs.length) {
            val t = runs.item(i) ?: continue
            // skip text belonging to deleted/tracked-change elements
            var parent: Node? = t.parentNode
            var ok = true
            while (parent != null) {
                val nm = parent.nodeName ?: ""
                if (nm.endsWith(":del")) { ok = false; break }
                parent = parent.parentNode
            }
            if (ok) sb.append(t.textContent ?: "")
            // add a space after a line break
            val brs = p.getElementsByTagName("w:br")
            if (i == runs.length - 1 && brs.length > 0) sb.append(" ")
        }
        if (sb.isEmpty()) {
            val tabs = p.getElementsByTagName("w:tab")
            if (tabs.length > 0) sb.append("    ")
        }
        return sb.toString()
    }

    private fun nodeStyle(p: Element): String {
        val pPr = p.getElementsByTagName("w:pStyle")
        if (pPr.length > 0) {
            val valAttr = (pPr.item(0) as Element).getAttribute("w:val")
            return valAttr.ifBlank { "Normal" }
        }
        return "Normal"
    }

    private fun hasBullet(p: Element): Boolean =
        p.getElementsByTagName("w:numPr").length > 0

    // -------------------------------------------------------------------- PPTX

    private fun parsePptx(zip: ZipFile, fileName: String): OfficeDocument {
        val slides = mutableListOf<Slide>()

        // Slide size for the correct aspect ratio
        var aspect = 16f / 9f
        readXml(zip, "ppt/presentation.xml")?.let { pres ->
            val sz = pres.getElementsByTagName("p:sldSz")
            if (sz.length > 0) {
                val e = sz.item(0) as Element
                val cx = e.getAttribute("cx").toFloatOrNull() ?: 0f
                val cy = e.getAttribute("cy").toFloatOrNull() ?: 0f
                if (cx > 0 && cy > 0) aspect = cx / cy
            }
        }

        // Read slide parts in numeric order
        val slideEntries = zip.entries().toList()
            .map { it.name }
            .filter { it.matches(Regex("ppt/slides/slide\\d+\\.xml")) }
            .sortedBy { it.filter(Char::isDigit).toIntOrNull() ?: 0 }

        slideEntries.forEachIndexed { idx, entry ->
            val xml = readXml(zip, entry) ?: return@forEachIndexed
            val lines = mutableListOf<SlideLine>()
            val tables = mutableListOf<TableData>()

            // shapes: each p:sp is a text container
            val shapes = xml.getElementsByTagName("p:sp")
            for (i in 0 until shapes.length) {
                val sp = shapes.item(i) as? Element ?: continue
                val isTitle = sp.getElementsByTagName("p:ph").let { ph ->
                    ph.length > 0 && ((ph.item(0) as Element).getAttribute("type").let {
                        it == "title" || it == "ctrTitle"
                    })
                }
                val paragraphs = sp.getElementsByTagName("a:p")
                for (j in 0 until paragraphs.length) {
                    val p = paragraphs.item(j) as? Element ?: continue
                    val sb = StringBuilder()
                    val runs = p.getElementsByTagName("a:t")
                    for (k in 0 until runs.length) sb.append(runs.item(k).textContent ?: "")
                    val text = sb.toString().trim()
                    if (text.isNotEmpty()) {
                        val lvl = p.getElementsByTagName("a:pPr").let { pr ->
                            if (pr.length > 0) (pr.item(0) as Element).getAttribute("lvl").toIntOrNull() ?: 0
                            else 0
                        }
                        lines.add(
                            SlideLine(
                                text = text,
                                isTitle = isTitle,
                                level = lvl,
                                bullet = lvl > 0 || p.getElementsByTagName("a:buChar").length > 0
                            )
                        )
                    }
                }
            }

            // tables
            val tbls = xml.getElementsByTagName("a:tbl")
            for (t in 0 until tbls.length) {
                val tbl = tbls.item(t) as? Element ?: continue
                val rows = mutableListOf<List<String>>()
                val trs = tbl.getElementsByTagName("a:tr")
                for (r in 0 until trs.length) {
                    val cells = mutableListOf<String>()
                    val tcs = (trs.item(r) as Element).getElementsByTagName("a:tc")
                    for (c in 0 until tcs.length) {
                        val texts = (tcs.item(c) as Element).getElementsByTagName("a:t")
                        val sb = StringBuilder()
                        for (k in 0 until texts.length) sb.append(texts.item(k).textContent ?: "")
                        cells.add(sb.toString())
                    }
                    rows.add(cells)
                }
                if (rows.isNotEmpty()) tables.add(TableData(rows))
            }

            slides.add(Slide(index = idx, lines = lines, aspectRatio = aspect, tables = tables))
        }

        return OfficeDocument(
            kind = OfficeKind.PRESENTATION,
            title = fileName.substringBeforeLast('.'),
            slides = slides
        )
    }

    // -------------------------------------------------------------------- XLSX

    private fun parseXlsx(zip: ZipFile, fileName: String): OfficeDocument {
        // shared strings
        val shared = mutableListOf<String>()
        readXml(zip, "xl/sharedStrings.xml")?.let { doc ->
            val items = doc.getElementsByTagName("si")
            for (i in 0 until items.length) {
                val texts = (items.item(i) as Element).getElementsByTagName("t")
                val sb = StringBuilder()
                for (j in 0 until texts.length) sb.append(texts.item(j).textContent ?: "")
                shared.add(sb.toString())
            }
        }

        // sheet names in workbook order
        val names = mutableListOf<String>()
        readXml(zip, "xl/workbook.xml")?.let { doc ->
            val sheets = doc.getElementsByTagName("sheet")
            for (i in 0 until sheets.length) {
                val e = sheets.item(i) as Element
                names.add(e.getAttribute("name").ifBlank { "Sheet ${i + 1}" })
            }
        }

        val sheetEntries = zip.entries().toList().map { it.name }
            .filter { it.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) }
            .sortedBy { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
            .take(12)   // cap: avoids loading a 50-sheet monster

        val sheets = mutableListOf<Sheet>()
        sheetEntries.forEachIndexed { idx, entry ->
            val xml = readXml(zip, entry) ?: return@forEachIndexed
            val rows = mutableListOf<List<String>>()
            val rowNodes = xml.getElementsByTagName("row")
            var maxCol = 0
            for (r in 0 until minOf(rowNodes.length, 400)) {
                val rowEl = rowNodes.item(r) as Element
                val cells = mutableMapOf<Int, String>()
                val cellNodes = rowEl.getElementsByTagName("c")
                for (c in 0 until cellNodes.length) {
                    val cell = cellNodes.item(c) as Element
                    val ref = cell.getAttribute("r")
                    val col = columnIndex(ref)
                    val type = cell.getAttribute("t")
                    val vNodes = cell.getElementsByTagName("v")
                    val value = when {
                        type == "s" && vNodes.length > 0 ->
                            vNodes.item(0).textContent.toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                        type == "inlineStr" -> {
                            val ts = cell.getElementsByTagName("t")
                            if (ts.length > 0) ts.item(0).textContent ?: "" else ""
                        }
                        vNodes.length > 0 -> vNodes.item(0).textContent ?: ""
                        else -> ""
                    }
                    cells[col] = value
                    if (col > maxCol) maxCol = col
                }
                rows.add((0..maxCol).map { cells[it] ?: "" })
            }
            // trim trailing empty rows
            while (rows.isNotEmpty() && rows.last().all { it.isBlank() }) rows.removeAt(rows.lastIndex)
            sheets.add(
                Sheet(
                    name = names.getOrNull(idx) ?: "Sheet ${idx + 1}",
                    rows = rows
                )
            )
        }

        return OfficeDocument(
            kind = OfficeKind.SPREADSHEET,
            title = fileName.substringBeforeLast('.'),
            sheets = sheets
        )
    }

    private fun columnIndex(ref: String): Int {
        var n = 0
        for (ch in ref) {
            if (ch in 'A'..'Z') n = n * 26 + (ch - 'A' + 1)
            else if (ch in 'a'..'z') n = n * 26 + (ch - 'a' + 1)
            else break
        }
        return (n - 1).coerceAtLeast(0)
    }
}
