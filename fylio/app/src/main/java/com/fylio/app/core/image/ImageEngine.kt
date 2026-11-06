package com.fylio.app.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Bitmap work for the scanner, converters and viewer. Everything is local.
 */
object ImageEngine {

    /** Decoded bitmap, downsampled so the long edge stays <= [maxDim]. */
    fun decode(context: Context, uri: Uri, maxDim: Int = 2400): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        var largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / sample > maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }.getOrNull()

    fun decode(file: File, maxDim: Int = 2400): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        var largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / sample > maxDim) sample *= 2
        BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()

    fun rotate(src: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return src
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    /** Scanner colour modes. */
    enum class ColorMode { ORIGINAL, MAGIC, GRAYSCALE, BLACK_WHITE }

    /**
     * Applies the scan preview settings in one pass:
     * brightness/contrast via a colour matrix, then optional grayscale / B&W.
     */
    fun enhance(
        src: Bitmap,
        brightness: Float = 1f,
        contrast: Float = 1f,
        mode: ColorMode = ColorMode.ORIGINAL
    ): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix()
        // contrast around mid-grey, then brightness
        val c = contrast
        val t = (1f - c) * 127.5f
        val bright = (brightness - 1f) * 255f
        cm.set(
            floatArrayOf(
                c, 0f, 0f, 0f, t + bright,
                0f, c, 0f, 0f, t + bright,
                0f, 0f, c, 0f, t + bright,
                0f, 0f, 0f, 1f, 0f
            )
        )
        when (mode) {
            ColorMode.GRAYSCALE -> cm.setSaturation(0f)
            ColorMode.BLACK_WHITE -> {
                cm.setSaturation(0f)
                // push contrast hard for a document look
                val bw = ColorMatrix(
                    floatArrayOf(
                        2.4f, 0f, 0f, 0f, -180f,
                        0f, 2.4f, 0f, 0f, -180f,
                        0f, 0f, 2.4f, 0f, -180f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(bw)
            }
            ColorMode.MAGIC -> {
                // gentle warm/document boost: slight saturation + contrast
                cm.postConcat(ColorMatrix().apply { setSaturation(1.15f) })
            }
            ColorMode.ORIGINAL -> Unit
        }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    fun saveJpeg(bitmap: Bitmap, target: File, quality: Int = 92): File? = runCatching {
        target.parentFile?.mkdirs()
        FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        target
    }.getOrNull()

    fun savePng(bitmap: Bitmap, target: File): File? = runCatching {
        target.parentFile?.mkdirs()
        FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        target
    }.getOrNull()

    fun saveWebp(bitmap: Bitmap, target: File, quality: Int = 90): File? = runCatching {
        target.parentFile?.mkdirs()
        FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.WEBP, quality, it) }
        target
    }.getOrNull()

    /** Crops a normalised rect (0..1) out of the bitmap. */
    fun crop(src: Bitmap, left: Float, top: Float, right: Float, bottom: Float): Bitmap {
        val x = (left * src.width).toInt().coerceIn(0, src.width - 2)
        val y = (top * src.height).toInt().coerceIn(0, src.height - 2)
        val w = ((right - left) * src.width).toInt().coerceIn(2, src.width - x)
        val h = ((bottom - top) * src.height).toInt().coerceIn(2, src.height - y)
        return Bitmap.createBitmap(src, x, y, w, h)
    }

    fun flattenOnWhite(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.WHITE)
        c.drawBitmap(src, 0f, 0f, null)
        return out
    }
}
