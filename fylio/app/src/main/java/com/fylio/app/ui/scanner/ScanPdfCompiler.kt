package com.fylio.app.ui.scanner

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/** Export quality tiers from the save sheet. */
enum class PdfQuality(val label: String, val maxDim: Int, val jpegQuality: Int) {
    LOW("Low (Compressed)", 1400, 62),
    MEDIUM("Medium (Standard)", 2000, 82),
    HIGH("High (Uncompressed)", 2800, 95)
}

enum class PageFormat(val label: String) {
    A4("A4"),
    FIT_IMAGE("Fit image"),
    LETTER("Letter")
}

/**
 * Compiles rendered page bitmaps into a real PDF with `android.graphics.pdf.PdfDocument`.
 * Pages are written at the requested pixel density so text stays sharp when zoomed.
 */
object ScanPdfCompiler {

    private const val A4_WIDTH = 595f      // points (72 dpi)
    private const val A4_HEIGHT = 842f
    private const val LETTER_WIDTH = 612f
    private const val LETTER_HEIGHT = 792f

    /** e.g. Fylio_Scan_20251106_134501.pdf */
    fun defaultFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "Fylio_Scan_$stamp.pdf"
    }

    /** Ensures the name ends in .pdf and has no path separators. */
    fun normaliseName(raw: String): String {
        val cleaned = raw.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_").ifBlank { defaultFileName() }
        return if (cleaned.lowercase().endsWith(".pdf")) cleaned else "$cleaned.pdf"
    }

    /**
     * Writes [bitmaps] to [target]. Each page is drawn onto the requested paper size while
     * preserving the image aspect ratio, so nothing is stretched or cropped.
     */
    fun compile(
        bitmaps: List<Bitmap>,
        target: File,
        format: PageFormat = PageFormat.A4,
        onPage: (Int) -> Unit = {}
    ): File? {
        if (bitmaps.isEmpty()) return null
        val document = PdfDocument()
        return runCatching {
            bitmaps.forEachIndexed { index, bitmap ->
                val (pageW, pageH) = when (format) {
                    PageFormat.A4 -> A4_WIDTH to A4_HEIGHT
                    PageFormat.LETTER -> LETTER_WIDTH to LETTER_HEIGHT
                    PageFormat.FIT_IMAGE -> {
                        // Paper follows the capture: points at 72dpi from the pixel size
                        val scale = 72f / 150f
                        (bitmap.width * scale).coerceAtLeast(72f) to
                            (bitmap.height * scale).coerceAtLeast(72f)
                    }
                }
                val pageInfo = PdfDocument.PageInfo.Builder(
                    pageW.toInt().coerceAtLeast(72),
                    pageH.toInt().coerceAtLeast(72),
                    index + 1
                ).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                val scale = minOf(pageW / bitmap.width, pageH / bitmap.height)
                val drawW = bitmap.width * scale
                val drawH = bitmap.height * scale
                val left = (pageW - drawW) / 2f
                val top = (pageH - drawH) / 2f
                canvas.drawBitmap(
                    bitmap,
                    null,
                    android.graphics.RectF(left, top, left + drawW, top + drawH),
                    android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
                )
                document.finishPage(page)
                onPage(index + 1)
            }
            target.parentFile?.mkdirs()
            java.io.FileOutputStream(target).use { document.writeTo(it) }
            target
        }.also {
            document.close()
        }.getOrNull()
    }

    /** Decodes + downsamples a captured page for export at the chosen quality tier. */
    fun exportScale(sourceWidth: Int, sourceHeight: Int, quality: PdfQuality): Float {
        val longest = max(sourceWidth, sourceHeight)
        return if (longest <= quality.maxDim) 1f else quality.maxDim.toFloat() / longest
    }
}
