package com.fylio.app.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * Toolkit operations built on the platform PdfRenderer.
 *
 * Rewriting pages means re-rasterising them (Android ships no PDF writer), so output
 * files are image-backed PDFs: page-accurate but not text-selectable. Truly lossless
 * editing needs a PDF engine such as PdfBox-Android, planned for Phase 2.
 */
object PdfTools {

    fun pageCount(file: File): Int = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { it.pageCount }
        }
    }.getOrDefault(0)

    /** Renders pages [from]..[to] (inclusive, 0-based) to bitmaps. */
    fun renderPages(file: File, from: Int = 0, to: Int = Int.MAX_VALUE, scale: Float = 2f): List<Bitmap> =
        runCatching {
            val out = mutableListOf<Bitmap>()
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val last = minOf(to, renderer.pageCount - 1)
                    for (i in from..last) {
                        if (i < 0) continue
                        renderer.openPage(i).use { page ->
                            val bmp = Bitmap.createBitmap(
                                (page.width * scale).toInt().coerceAtLeast(1),
                                (page.height * scale).toInt().coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888
                            )
                            bmp.eraseColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            out.add(bmp)
                        }
                    }
                }
            }
            out
        }.getOrDefault(emptyList())

    /** One single-page PDF per page of [file]. */
    fun splitToPages(file: File, outDir: File): List<File> {
        if (!outDir.exists()) outDir.mkdirs()
        val base = file.nameWithoutExtension
        val pages = renderPages(file)
        return pages.mapIndexedNotNull { index, bitmap ->
            runCatching {
                val target = FylioFileNaming.unique(outDir, "${base}_p${index + 1}", "pdf")
                FileOps.bitmapsToPdf(listOf(bitmap), target)
                target
            }.getOrNull()
        }
    }

    /** Keeps the 1-based inclusive page range [from]..[to] in a new PDF. */
    fun extractRange(file: File, outDir: File, from: Int, to: Int): File? {
        if (!outDir.exists()) outDir.mkdirs()
        val bitmaps = renderPages(file, from - 1, to - 1)
        if (bitmaps.isEmpty()) return null
        val base = file.nameWithoutExtension
        val target = FylioFileNaming.unique(outDir, "${base}_pages_${from}-$to", "pdf")
        return runCatching { FileOps.bitmapsToPdf(bitmaps, target) }.getOrNull()
    }

    /** Concatenates every page of [files] into one PDF. */
    fun merge(files: List<File>, outDir: File, name: String = "Merged"): File? {
        if (files.isEmpty()) return null
        if (!outDir.exists()) outDir.mkdirs()
        val bitmaps = files.flatMap { renderPages(it) }
        if (bitmaps.isEmpty()) return null
        val target = FylioFileNaming.unique(outDir, "${name}_${System.currentTimeMillis()}", "pdf")
        return runCatching { FileOps.bitmapsToPdf(bitmaps, target) }.getOrNull()
    }

    /** Every page of [file] written out as a JPEG. */
    fun toImages(file: File, outDir: File): List<File> {
        if (!outDir.exists()) outDir.mkdirs()
        val base = file.nameWithoutExtension
        return renderPages(file).mapIndexedNotNull { index, bitmap ->
            runCatching {
                val target = FylioFileNaming.unique(outDir, "${base}_p${index + 1}", "jpg")
                FileOps.saveJpeg(bitmap, target, 92)
            }.getOrNull()
        }
    }

    /** Wraps [images] into a single PDF. */
    fun fromImages(images: List<File>, outDir: File, name: String = "Images"): File? {
        if (images.isEmpty()) return null
        if (!outDir.exists()) outDir.mkdirs()
        val bitmaps = images.mapNotNull { FileOps.decodeBitmap(it, 2400) }
        if (bitmaps.isEmpty()) return null
        val target = FylioFileNaming.unique(outDir, "${name}_${System.currentTimeMillis()}", "pdf")
        return runCatching { FileOps.bitmapsToPdf(bitmaps, target) }.getOrNull()
    }
}

private object FylioFileNaming {
    fun unique(dir: File, base: String, ext: String): File {
        var candidate = File(dir, "$base.$ext")
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "${base}_$i.$ext")
            i++
        }
        return candidate
    }
}
