package com.fylio.app.ui.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

/**
 * Document filters for the enhance step.
 *
 * Sliders use the spec ranges: brightness -1..1 (additive, in 0..255 units) and contrast
 * 0.5..2 (multiplicative around mid-grey). Magic colour additionally lifts shadows so
 * photographed paper looks white instead of grey.
 */
object ScanFilters {

    fun apply(
        source: Bitmap,
        filter: ScanFilter,
        brightness: Float = 0f,
        contrast: Float = 1f
    ): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        if (filter == ScanFilter.B_AND_W) {
            // Thresholding can't be expressed as a colour matrix, so do it per pixel.
            binarise(source, out, contrast, brightness)
            return out
        }

        val matrix = ColorMatrix()
        val c = contrast.coerceIn(0.5f, 2f)
        val b = (brightness.coerceIn(-1f, 1f) * 255f)
        val translate = (1f - c) * 127.5f + b
        matrix.set(
            floatArrayOf(
                c, 0f, 0f, 0f, translate,
                0f, c, 0f, 0f, translate,
                0f, 0f, c, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        when (filter) {
            ScanFilter.GRAYSCALE -> matrix.postConcat(ColorMatrix().apply { setSaturation(0f) })
            ScanFilter.MAGIC -> {
                // Slightly richer colour, a touch more contrast, then lift the blacks so
                // shadowed paper reads as white.
                matrix.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.08f, 0f, 0f, 0f, 6f,
                            0f, 1.08f, 0f, 0f, 6f,
                            0f, 0f, 1.08f, 0f, 6f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
                matrix.postConcat(ColorMatrix().apply { setSaturation(1.12f) })
            }
            ScanFilter.ORIGINAL -> Unit
            ScanFilter.B_AND_W -> Unit
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    /**
     * Adaptive-ish binarisation: a global luminance thresholded just above mid-grey
     * removes paper texture while keeping stroked text solid. Contrast biases the cut.
     */
    private fun binarise(source: Bitmap, out: Bitmap, contrast: Float, brightness: Float) {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        // threshold moves opposite to contrast: more contrast -> darker cut -> bolder text
        val base = 165f + (1.35f - contrast.coerceIn(0.5f, 2f)) * 70f - brightness * 90f
        val threshold = base.coerceIn(90f, 230f)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val luma = 0.299f * r + 0.587f * g + 0.114f * b
            val v = if (luma > threshold) 255 else 0
            pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /** Small preview-friendly resize that keeps aspect ratio. */
    fun fit(bitmap: Bitmap, maxDim: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDim) return bitmap
        val scale = maxDim.toFloat() / longest
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    fun clamp(v: Float, lo: Float, hi: Float) = min(max(v, lo), hi)
}
