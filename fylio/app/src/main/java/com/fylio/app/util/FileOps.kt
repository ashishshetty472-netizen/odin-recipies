package com.fylio.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileOps {

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun share(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType(file.name)
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    }

    fun openWith(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(context, file), mimeType(file.name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
    }

    fun mimeType(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "txt", "log", "md" -> "text/plain"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        else -> "application/octet-stream"
    }

    /** Decoded bitmap, downsampled to stay inside [maxDimension]. */
    fun decodeBitmap(file: File, maxDimension: Int = 1400): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        var largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / sample > maxDimension) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    /**
     * Applies the scanner's adjustment sliders to [source] and returns a new bitmap.
     * Contrast/brightness use a linear curve, sharpness uses an unsharp-mask style
     * neighbour blend so the sliders stay cheap enough for on-device preview.
     */
    fun adjust(
        source: Bitmap,
        brightness: Float = 1f,
        contrast: Float = 1f,
        sharpness: Float = 1f
    ): Bitmap {
        val w = source.width
        val h = source.height
        if (w < 2 || h < 2) return source

        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)

        // Brightness + contrast LUT
        val lut = IntArray(256)
        for (i in 0 until 256) {
            val v = ((i - 128) * contrast + 128) * brightness
            lut[i] = v.toInt().coerceIn(0, 255)
        }

        val sharp = (sharpness - 1f).coerceIn(-0.45f, 0.45f)

        for (y in 0 until h) {
            val yBase = y * w
            for (x in 0 until w) {
                val idx = yBase + x
                val c = pixels[idx]
                var r = lut[(c shr 16) and 0xFF]
                var g = lut[(c shr 8) and 0xFF]
                var b = lut[c and 0xFF]

                if (sharp != 0f) {
                    val left = pixels[yBase + if (x > 0) x - 1 else x]
                    val right = pixels[yBase + if (x < w - 1) x + 1 else x]
                    val up = pixels[(if (y > 0) y - 1 else y) * w + x]
                    val down = pixels[(if (y < h - 1) y + 1 else y) * w + x]
                    val blurR = (((left shr 16) and 0xFF) + ((right shr 16) and 0xFF) +
                        ((up shr 16) and 0xFF) + ((down shr 16) and 0xFF)) / 4
                    val blurG = (((left shr 8) and 0xFF) + ((right shr 8) and 0xFF) +
                        ((up shr 8) and 0xFF) + ((down shr 8) and 0xFF)) / 4
                    val blurB = ((left and 0xFF) + (right and 0xFF) +
                        (up and 0xFF) + (down and 0xFF)) / 4
                    r = (r + (r - blurR) * sharp * 2f).toInt().coerceIn(0, 255)
                    g = (g + (g - blurG) * sharp * 2f).toInt().coerceIn(0, 255)
                    b = (b + (b - blurB) * sharp * 2f).toInt().coerceIn(0, 255)
                }
                out[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    fun saveJpeg(bitmap: Bitmap, target: File, quality: Int = 92): File {
        FileOutputStream(target).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return target
    }

    /** Wraps one or more bitmaps into a single PDF page sequence. */
    fun bitmapsToPdf(bitmaps: List<Bitmap>, target: File): File {
        val document = android.graphics.pdf.PdfDocument()
        bitmaps.forEachIndexed { index, bitmap ->
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                bitmap.width, bitmap.height, index + 1
            ).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawColor(android.graphics.Color.WHITE)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            document.finishPage(page)
        }
        FileOutputStream(target).use { document.writeTo(it) }
        document.close()
        return target
    }

    /** Rotates a bitmap by [degrees] (must be a multiple of 90). */
    fun rotate(source: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return source
        val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
