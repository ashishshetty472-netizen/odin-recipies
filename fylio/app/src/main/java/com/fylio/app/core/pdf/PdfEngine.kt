package com.fylio.app.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import java.io.File
import java.io.FileOutputStream

/**
 * All PDF work happens here.
 *
 * - Reading/rendering uses the platform [PdfRenderer] (fast, no dependencies).
 * - Editing uses PDFBox-Android so pages keep their real vector content: merge, split,
 *   extract, delete, reorder, rotate and protect all operate on actual PDF objects
 *   rather than re-rasterised images.
 */
object PdfEngine {

    fun init(context: Context) {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    // ---------------------------------------------------------------- rendering

    fun pageCount(file: File): Int = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { it.pageCount }
        }
    }.getOrDefault(0)

    fun pageSize(file: File, pageIndex: Int): Pair<Int, Int>? = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { r ->
                r.openPage(pageIndex.coerceIn(0, r.pageCount - 1)).use { it.width to it.height }
            }
        }
    }.getOrNull()

    /** Renders one page at [scale] (2f ≈ 144 dpi). */
    fun renderPage(file: File, pageIndex: Int, scale: Float = 2f): Bitmap? = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) return null
                renderer.openPage(pageIndex.coerceIn(0, renderer.pageCount - 1)).use { page ->
                    val bmp = Bitmap.createBitmap(
                        (page.width * scale).toInt().coerceAtLeast(1),
                        (page.height * scale).toInt().coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }
        }
    }.getOrNull()

    fun renderAll(file: File, scale: Float = 2f): List<Bitmap> {
        val count = pageCount(file)
        return (0 until count).mapNotNull { renderPage(file, it, scale) }
    }

    // ------------------------------------------------------------------ editing

    fun merge(inputs: List<File>, target: File): File? = runCatching {
        PDDocument().use { out ->
            inputs.forEach { input ->
                PDDocument.load(input).use { doc -> 
                    for (i in 0 until doc.numberOfPages) out.importPage(doc.getPage(i))
                }
            }
            target.parentFile?.mkdirs()
            out.save(target)
        }
        target
    }.getOrNull()

    /** Splits every page into its own PDF. */
    fun split(input: File, outDir: File, baseName: String = input.nameWithoutExtension): List<File> =
        runCatching {
            outDir.mkdirs()
            val created = mutableListOf<File>()
            PDDocument.load(input).use { src ->
                for (i in 0 until src.numberOfPages) {
                    PDDocument().use { one ->
                        one.importPage(src.getPage(i))
                        val f = unique(outDir, "${baseName}_p${i + 1}", "pdf")
                        one.save(f)
                        created.add(f)
                    }
                }
            }
            created
        }.getOrDefault(emptyList())

    /** Keeps pages [from]..[to] (1-based inclusive). */
    fun extractPages(input: File, from: Int, to: Int, target: File): File? = runCatching {
        PDDocument.load(input).use { src ->
            PDDocument().use { out ->
                val lo = (from - 1).coerceIn(0, src.numberOfPages - 1)
                val hi = (to - 1).coerceIn(lo, src.numberOfPages - 1)
                for (i in lo..hi) out.importPage(src.getPage(i))
                target.parentFile?.mkdirs()
                out.save(target)
            }
        }
        target
    }.getOrNull()

    /** Removes the 1-based [pages] list, keeping everything else. */
    fun deletePages(input: File, pages: Set<Int>, target: File): File? = runCatching {
        PDDocument.load(input).use { doc ->
            pages.sortedDescending().forEach { p ->
                val idx = p - 1
                if (idx in 0 until doc.numberOfPages) doc.removePage(idx)
            }
            target.parentFile?.mkdirs()
            doc.save(target)
        }
        target
    }.getOrNull()

    /** Reorders pages: [order] is the new sequence of 1-based original page numbers. */
    fun reorder(input: File, order: List<Int>, target: File): File? = runCatching {
        PDDocument.load(input).use { src ->
            PDDocument().use { out ->
                order.forEach { p ->
                    val idx = p - 1
                    if (idx in 0 until src.numberOfPages) out.importPage(src.getPage(idx))
                }
                target.parentFile?.mkdirs()
                out.save(target)
            }
        }
        target
    }.getOrNull()

    fun rotatePages(input: File, degrees: Int, target: File): File? = runCatching {
        PDDocument.load(input).use { doc ->
            for (i in 0 until doc.numberOfPages) {
                val page = doc.getPage(i)
                page.rotation = ((page.rotation + degrees) % 360 + 360) % 360
            }
            target.parentFile?.mkdirs()
            doc.save(target)
        }
        target
    }.getOrNull()

    fun protect(input: File, userPassword: String, target: File): File? = runCatching {
        PDDocument.load(input).use { doc ->
            val ap = AccessPermission()
            val policy = StandardProtectionPolicy(null, userPassword, ap).apply {
                encryptionKeyLength = 128
                permissions = ap
            }
            doc.protect(policy)
            target.parentFile?.mkdirs()
            doc.save(target)
        }
        target
    }.getOrNull()

    /** Rewrites the document at a lower JPEG quality to shrink it. */
    fun compress(input: File, quality: Int, scale: Float, target: File): File? = runCatching {
        val bitmaps = renderAll(input, scale)
        bitmapsToPdf(bitmaps, target, jpegQuality = quality)
        target
    }.getOrNull()

    // ------------------------------------------------------------- construction

    /** Builds a PDF whose pages are [bitmaps] (used by the scanner and image import). */
    fun bitmapsToPdf(
        bitmaps: List<Bitmap>,
        target: File,
        jpegQuality: Int = 88,
        pageSize: PDRectangle? = null
    ): File? = runCatching {
        PDDocument().use { doc ->
            bitmaps.forEach { bmp ->
                val rect = pageSize ?: PDRectangle(bmp.width.toFloat(), bmp.height.toFloat())
                val page = PDPage(rect)
                doc.addPage(page)
                val image = JPEGFactory.createFromImage(doc, bmp, jpegQuality / 100f)
                PDPageContentStream(doc, page).use { cs ->
                    // fit the bitmap inside the page box, preserving aspect
                    val sx = rect.width / bmp.width
                    val sy = rect.height / bmp.height
                    val s = minOf(sx, sy)
                    val w = bmp.width * s
                    val h = bmp.height * s
                    cs.drawImage(image, (rect.width - w) / 2f, (rect.height - h) / 2f, w, h)
                }
                bmp.recycle()
            }
            target.parentFile?.mkdirs()
            doc.save(target)
        }
        target
    }.getOrNull()

    /** Writes [text] as paginated A4 pages. */
    fun textToPdf(text: String, target: File, title: String? = null): File? = runCatching {
        PDDocument().use { doc ->
            val font = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA
            val bold = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
            val margin = 56f
            val fontSize = 11f
            val leading = 15.5f
            val usable = PDRectangle.A4.width - margin * 2

            fun wrap(line: String): List<String> {
                if (line.isEmpty()) return listOf("")
                val words = line.split(' ')
                val out = mutableListOf<String>()
                var cur = StringBuilder()
                for (w in words) {
                    val candidate = if (cur.isEmpty()) w else "$cur $w"
                    val width = runCatching { font.getStringWidth(candidate) / 1000f * fontSize }.getOrDefault(0f)
                    if (width > usable && cur.isNotEmpty()) {
                        out.add(cur.toString()); cur = StringBuilder(w)
                    } else cur = StringBuilder(candidate)
                }
                if (cur.isNotEmpty()) out.add(cur.toString())
                return out
            }

            val lines = text.split('\n').flatMap { wrap(it.replace("\t", "    ")) }
            var page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            var cs = PDPageContentStream(doc, page)
            var y = PDRectangle.A4.height - margin

            cs.beginText()
            cs.setFont(if (title != null) bold else font, if (title != null) 15f else fontSize)
            cs.newLineAtOffset(margin, y)
            if (title != null) {
                cs.showText(title.take(90))
                cs.endText()
                y -= 26f
                cs.beginText()
                cs.setFont(font, fontSize)
                cs.newLineAtOffset(margin, y)
            }
            for (line in lines) {
                if (y < margin + leading) {
                    cs.endText(); cs.close()
                    page = PDPage(PDRectangle.A4)
                    doc.addPage(page)
                    cs = PDPageContentStream(doc, page)
                    cs.beginText()
                    cs.setFont(font, fontSize)
                    y = PDRectangle.A4.height - margin
                    cs.newLineAtOffset(margin, y)
                }
                cs.showText(line.map { if (it.code in 32..126 || it.code in 160..255) it else '?' }.joinToString(""))
                cs.newLineAtOffset(0f, -leading)
                y -= leading
            }
            cs.endText()
            cs.close()
            target.parentFile?.mkdirs()
            doc.save(target)
        }
        target
    }.getOrNull()

    /** Stamps text on a page (used by the watermark tool). */
    fun watermark(input: File, text: String, target: File, alpha: Float = 0.18f): File? = runCatching {
        PDDocument.load(input).use { doc ->
            val font = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
            for (i in 0 until doc.numberOfPages) {
                val page = doc.getPage(i)
                val size = 60f
                val width = font.getStringWidth(text) / 1000f * size
                PDPageContentStream(
                    doc, page,
                    PDPageContentStream.AppendMode.APPEND, true, true
                ).use { cs ->
                    val gs = PDExtendedGraphicsState()
                    gs.nonStrokingAlphaConstant = alpha
                    cs.setGraphicsStateParameters(gs)
                    cs.beginText()
                    cs.setFont(font, size)
                    cs.setNonStrokingColor(0.55f, 0.55f, 0.55f)
                    cs.setTextMatrix(
                        com.tom_roush.pdfbox.util.Matrix.getRotateInstance(
                            Math.toRadians(45.0), 80.0, 200.0
                        )
                    )
                    cs.newLineAtOffset(80f, 200f)
                    cs.showText(text.take(40))
                    cs.endText()
                }
            }
            target.parentFile?.mkdirs()
            doc.save(target)
        }
        target
    }.getOrNull()

    /** Removes password protection when the user knows the password. */
    fun unlock(input: File, password: String, target: File): File? = runCatching {
        PDDocument.load(input, password).use { doc ->
            doc.isAllSecurityToBeRemoved = true
            target.parentFile?.mkdirs()
            doc.save(target)
        }
        target
    }.getOrNull()

    /** True when the PDF can only be opened with a password. */
    fun isEncrypted(file: File): Boolean = runCatching {
        PDDocument.load(file).use { false }
    }.getOrElse { true }

    /** Stamps "n / total" at the bottom centre of every page. */
    fun addPageNumbers(input: File, target: File): File? = runCatching {
        PDDocument.load(input).use { doc ->
            val font = com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA
            val total = doc.numberOfPages
            for (i in 0 until total) {
                val page = doc.getPage(i)
                val label = "${i + 1} / $total"
                val size = 9f
                val width = font.getStringWidth(label) / 1000f * size
                PDPageContentStream(
                    doc, page,
                    PDPageContentStream.AppendMode.APPEND, true, true
                ).use { cs ->
                    cs.beginText()
                    cs.setFont(font, size)
                    cs.setNonStrokingColor(0.35f, 0.35f, 0.35f)
                    cs.newLineAtOffset((page.mediaBox.width - width) / 2f, 22f)
                    cs.showText(label)
                    cs.endText()
                }
            }
            target.parentFile?.mkdirs()
            doc.save(target)
        }
        target
    }.getOrNull()

    // ------------------------------------------------------------------ helpers

    fun unique(dir: File, base: String, ext: String): File {
        dir.mkdirs()
        var f = File(dir, "$base.$ext")
        var i = 1
        while (f.exists()) { f = File(dir, "${base}_$i.$ext"); i++ }
        return f
    }

    fun writeTemp(context: Context, name: String): File {
        val dir = File(context.cacheDir, "pdfwork").apply { mkdirs() }
        return File(dir, name)
    }
}
