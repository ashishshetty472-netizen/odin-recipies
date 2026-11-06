package com.fylio.app.ui.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Perspective correction without OpenCV.
 *
 * A 3×3 homography is solved from the four source corners to a rectangle of the page's
 * estimated size, then the bitmap is resampled with bilinear interpolation into the
 * destination. That reproduces the "straighten a slanted page" effect of a scanner while
 * keeping the APK free of native dependencies.
 */
object PerspectiveWarp {

    /**
     * Warps the quadrilateral [quad] (normalised coordinates) out of [source] and returns
     * an upright rectangular page.
     */
    fun warp(source: Bitmap, quad: ScanQuad): Bitmap {
        val w = source.width.toFloat()
        val h = source.height.toFloat()

        val sx = quad.xs().map { it * w }.toFloatArray()
        val sy = quad.ys().map { it * h }.toFloatArray()

        val topWidth = hypot(sx[1] - sx[0], sy[1] - sy[0])
        val bottomWidth = hypot(sx[2] - sx[3], sy[2] - sy[3])
        val leftHeight = hypot(sx[3] - sx[0], sy[3] - sy[0])
        val rightHeight = hypot(sx[2] - sx[1], sy[2] - sy[1])

        var outW = max(topWidth, bottomWidth).roundToInt()
        var outH = max(leftHeight, rightHeight).roundToInt()
        if (outW < 32 || outH < 32) return source

        // Cap the working resolution: a phone capture is plenty at 2600px and this keeps
        // the resample loop well under a second.
        val longest = max(outW, outH)
        if (longest > 2600) {
            val s = 2600f / longest
            outW = (outW * s).roundToInt().coerceAtLeast(32)
            outH = (outH * s).roundToInt().coerceAtLeast(32)
        }

        val dstX = floatArrayOf(0f, outW.toFloat(), outW.toFloat(), 0f)
        val dstY = floatArrayOf(0f, 0f, outH.toFloat(), outH.toFloat())

        val inverse = runCatching {
            // Solve rect -> quad, then invert: that direction avoids holes in the output.
            val m = solveHomography(dstX, dstY, sx, sy)
            invert3x3(m)
        }.getOrNull() ?: return cropBoundingBox(source, quad)

        val srcPixels = IntArray(source.width * source.height)
        source.getPixels(srcPixels, 0, source.width, 0, 0, source.width, source.height)

        val out = IntArray(outW * outH)
        val maxX = source.width - 1
        val maxY = source.height - 1

        var index = 0
        for (y in 0 until outH) {
            val fy = y.toFloat()
            for (x in 0 until outW) {
                val fx = x.toFloat()
                val denom = inverse[6] * fx + inverse[7] * fy + inverse[8]
                if (denom == 0f) {
                    out[index++] = Color.WHITE
                    continue
                }
                val u = (inverse[0] * fx + inverse[1] * fy + inverse[2]) / denom
                val v = (inverse[3] * fx + inverse[4] * fy + inverse[5]) / denom
                if (u < 0f || v < 0f || u > maxX.toFloat() || v > maxY.toFloat()) {
                    out[index++] = Color.WHITE
                    continue
                }
                // bilinear sample
                val x0 = u.toInt()
                val y0 = v.toInt()
                val x1 = min(x0 + 1, maxX)
                val y1 = min(y0 + 1, maxY)
                val dx = u - x0
                val dy = v - y0

                val p00 = srcPixels[y0 * source.width + x0]
                val p10 = srcPixels[y0 * source.width + x1]
                val p01 = srcPixels[y1 * source.width + x0]
                val p11 = srcPixels[y1 * source.width + x1]

                val r = blend(p00, p10, p01, p11, dx, dy, 16)
                val g = blend(p00, p10, p01, p11, dx, dy, 8)
                val b = blend(p00, p10, p01, p11, dx, dy, 0)
                out[index++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val warped = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        warped.setPixels(out, 0, outW, 0, 0, outW, outH)
        return warped
    }

    private fun blend(p00: Int, p10: Int, p01: Int, p11: Int, dx: Float, dy: Float, shift: Int): Int {
        val c00 = (p00 shr shift) and 0xFF
        val c10 = (p10 shr shift) and 0xFF
        val c01 = (p01 shr shift) and 0xFF
        val c11 = (p11 shr shift) and 0xFF
        val top = c00 + (c10 - c00) * dx
        val bottom = c01 + (c11 - c01) * dx
        return (top + (bottom - top) * dy).roundToInt().coerceIn(0, 255)
    }

    /** Fallback: axis-aligned crop when the homography is degenerate. */
    private fun cropBoundingBox(source: Bitmap, quad: ScanQuad): Bitmap {
        val xs = quad.xs().map { it * source.width }
        val ys = quad.ys().map { it * source.height }
        val left = xs.min().roundToInt().coerceIn(0, source.width - 1)
        val top = ys.min().roundToInt().coerceIn(0, source.height - 1)
        val right = xs.max().roundToInt().coerceIn(left + 1, source.width)
        val bottom = ys.max().roundToInt().coerceIn(top + 1, source.height)
        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }

    /**
     * Classic 8×8 linear system for a plane-to-plane homography, solved by Gaussian
     * elimination with partial pivoting. Returns the 9 coefficients row-major.
     */
    private fun solveHomography(
        srcX: FloatArray,
        srcY: FloatArray,
        dstX: FloatArray,
        dstY: FloatArray
    ): FloatArray {
        val a = Array(8) { DoubleArray(9) }
        for (i in 0..3) {
            val x = srcX[i].toDouble()
            val y = srcY[i].toDouble()
            val u = dstX[i].toDouble()
            val v = dstY[i].toDouble()

            a[i * 2][0] = x; a[i * 2][1] = y; a[i * 2][2] = 1.0
            a[i * 2][3] = 0.0; a[i * 2][4] = 0.0; a[i * 2][5] = 0.0
            a[i * 2][6] = -x * u; a[i * 2][7] = -y * u; a[i * 2][8] = u

            a[i * 2 + 1][0] = 0.0; a[i * 2 + 1][1] = 0.0; a[i * 2 + 1][2] = 0.0
            a[i * 2 + 1][3] = x; a[i * 2 + 1][4] = y; a[i * 2 + 1][5] = 1.0
            a[i * 2 + 1][6] = -x * v; a[i * 2 + 1][7] = -y * v; a[i * 2 + 1][8] = v
        }

        for (col in 0 until 8) {
            var pivot = col
            for (row in col + 1 until 8) {
                if (kotlin.math.abs(a[row][col]) > kotlin.math.abs(a[pivot][col])) pivot = row
            }
            if (kotlin.math.abs(a[pivot][col]) < 1e-9) continue
            val tmp = a[col]; a[col] = a[pivot]; a[pivot] = tmp

            val div = a[col][col]
            for (k in col until 9) a[col][k] /= div

            for (row in 0 until 8) {
                if (row == col) continue
                val factor = a[row][col]
                if (factor == 0.0) continue
                for (k in col until 9) a[row][k] -= factor * a[col][k]
            }
        }

        return FloatArray(9) { i ->
            if (i < 8) a[i][8].toFloat() else 1f
        }
    }

    private fun invert3x3(m: FloatArray): FloatArray {
        val a = m[0].toDouble(); val b = m[1].toDouble(); val c = m[2].toDouble()
        val d = m[3].toDouble(); val e = m[4].toDouble(); val f = m[5].toDouble()
        val g = m[6].toDouble(); val h = m[7].toDouble(); val i = m[8].toDouble()

        val det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
        require(kotlin.math.abs(det) > 1e-12) { "singular" }

        val inv = 1.0 / det
        return floatArrayOf(
            ((e * i - f * h) * inv).toFloat(),
            ((c * h - b * i) * inv).toFloat(),
            ((b * f - c * e) * inv).toFloat(),
            ((f * g - d * i) * inv).toFloat(),
            ((a * i - c * g) * inv).toFloat(),
            ((c * d - a * f) * inv).toFloat(),
            ((d * h - e * g) * inv).toFloat(),
            ((b * g - a * h) * inv).toFloat(),
            ((a * e - b * d) * inv).toFloat()
        )
    }

    /** Renders the quad outline + corner handles on top of a preview bitmap. */
    fun drawQuadOverlay(
        canvas: Canvas,
        quad: ScanQuad,
        width: Float,
        height: Float,
        stroke: Paint,
        handleFill: Paint,
        handleRadius: Float
    ) {
        val xs = quad.xs().map { it * width }
        val ys = quad.ys().map { it * height }
        val path = android.graphics.Path().apply {
            moveTo(xs[0], ys[0])
            lineTo(xs[1], ys[1])
            lineTo(xs[2], ys[2])
            lineTo(xs[3], ys[3])
            close()
        }
        canvas.drawPath(path, stroke)
        for (k in 0..3) {
            canvas.drawCircle(xs[k], ys[k], handleRadius, handleFill)
        }
    }
}
