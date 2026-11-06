package com.fylio.app.ui.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

/**
 * Live document-edge analysis for the camera preview.
 *
 * ML Kit's object detector only returns axis-aligned boxes, which is not enough for a
 * perspective crop, so this analyzer finds the actual page edges itself: it downsamples
 * each frame to greyscale, computes gradient magnitude (a cheap Sobel), and tracks the
 * strongest run of edges along the top, bottom, left and right of the frame. The result is
 * a quad whose corners follow a tilted sheet of paper — and it costs one pass over a
 * ~160×120 buffer, so it runs comfortably at 30 fps on the analysis thread.
 *
 * When no confident edges exist (low light, cluttered desk) it reports the full frame and
 * the crop editor falls back to its default inset.
 */
class DocumentAnalyzer(
    private val onQuad: (ScanQuad) -> Unit
) : ImageAnalysis.Analyzer {

    private val busy = AtomicBoolean(false)

    /** Reused work buffers — allocation in the analyzer would cause GC jank. */
    private var grey: IntArray = IntArray(0)
    private var width = 0
    private var height = 0

    override fun analyze(imageProxy: ImageProxy) {
        if (!busy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        try {
            val plane = imageProxy.planes.firstOrNull()
            if (plane == null) {
                onQuad(ScanQuad.Full)
                return
            }
            val quad = detect(
                buffer = plane.buffer,
                rowStride = plane.rowStride,
                pixelStride = plane.pixelStride,
                srcWidth = imageProxy.width,
                srcHeight = imageProxy.height,
                rotation = imageProxy.imageInfo.rotationDegrees
            )
            onQuad(quad)
        } catch (t: Throwable) {
            onQuad(ScanQuad.Full)
        } finally {
            busy.set(false)
            imageProxy.close()
        }
    }

    /**
     * Builds a small greyscale image from the YUV luma plane, then locates the document
     * edges. Coordinates are returned normalised so the UI can map them onto any size.
     */
    private fun detect(
        buffer: java.nio.ByteBuffer,
        rowStride: Int,
        pixelStride: Int,
        srcWidth: Int,
        srcHeight: Int,
        rotation: Int
    ): ScanQuad {
        if (srcWidth <= 0 || srcHeight <= 0) return ScanQuad.Full

        // target ~160px on the long edge
        val step = max(1, max(srcWidth, srcHeight) / 160)
        val w = srcWidth / step
        val h = srcHeight / step
        if (w < 24 || h < 24) return ScanQuad.Full

        if (grey.size < w * h) grey = IntArray(w * h)
        width = w
        height = h

        for (y in 0 until h) {
            val row = y * step * rowStride
            for (x in 0 until w) {
                val index = row + x * step * pixelStride
                grey[y * w + x] = if (index < buffer.limit()) (buffer.get(index).toInt() and 0xFF) else 128
            }
        }

        // Gradient magnitude along rows and columns
        val gradX = IntArray(w * h)
        val gradY = IntArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                gradX[i] = kotlin.math.abs(grey[i + 1] - grey[i - 1])
                gradY[i] = kotlin.math.abs(grey[i + w] - grey[i - w])
            }
        }

        // For each side, find the row/column with the strongest cumulative edge energy,
        // searching only in the outer third of the frame.
        val top = strongestLine(gradY, from = height / 12, to = height / 3, horizontal = true)
        val bottom = strongestLine(gradY, from = height * 2 / 3, to = height - height / 12, horizontal = true)
        val left = strongestLine(gradX, from = width / 12, to = width / 3, horizontal = false)
        val right = strongestLine(gradX, from = width * 2 / 3, to = width - width / 12, horizontal = false)

        if (top == null || bottom == null || left == null || right == null) return ScanQuad.Full
        if (bottom - top < height / 4 || right - left < width / 4) return ScanQuad.Full

        // Map the small-image coordinates into the display orientation
        var tlX = left.toFloat() / w
        var tlY = top.toFloat() / h
        var trX = right.toFloat() / w
        var trY = top.toFloat() / h
        var brX = right.toFloat() / w
        var brY = bottom.toFloat() / h
        var blX = left.toFloat() / w
        var blY = bottom.toFloat() / h

        when (rotation) {
            90 -> {
                val t = floatArrayOf(tlX, tlY); tlX = 1f - t[1]; tlY = t[0]
                val t2 = floatArrayOf(trX, trY); trX = 1f - t2[1]; trY = t2[0]
                val t3 = floatArrayOf(brX, brY); brX = 1f - t3[1]; brY = t3[0]
                val t4 = floatArrayOf(blX, blY); blX = 1f - t4[1]; blY = t4[0]
            }
            180 -> {
                tlX = 1f - tlX; tlY = 1f - tlY
                trX = 1f - trX; trY = 1f - trY
                brX = 1f - brX; brY = 1f - brY
                blX = 1f - blX; blY = 1f - blY
            }
            270 -> {
                val t = floatArrayOf(tlX, tlY); tlX = t[1]; tlY = 1f - t[0]
                val t2 = floatArrayOf(trX, trY); trX = t2[1]; trY = 1f - t2[0]
                val t3 = floatArrayOf(brX, brY); brX = t3[1]; brY = 1f - t3[0]
                val t4 = floatArrayOf(blX, blY); blX = t4[1]; blY = 1f - t4[0]
            }
        }

        val quad = ScanQuad(tlX, tlY, trX, trY, brX, brY, blX, blY)
        // Sanity: reject degenerate quads rather than snapping to nonsense.
        return if (quad.area() < 0.12f) ScanQuad.Full else quad
    }

    /**
     * Sums gradient energy along each candidate row (or column) and returns the strongest
     * index, ignoring weak lines so an empty frame doesn't produce an edge.
     */
    private fun strongestLine(
        gradient: IntArray,
        from: Int,
        to: Int,
        horizontal: Boolean
    ): Int? {
        if (from >= to) return null
        var bestIndex: Int? = null
        var bestScore = 0f
        val span = if (horizontal) width else height

        for (position in from until to) {
            var sum = 0f
            if (horizontal) {
                val row = position
                if (row !in 0 until height) continue
                for (x in 0 until width) sum += gradient[row * width + x]
            } else {
                val col = position
                if (col !in 0 until width) continue
                for (y in 0 until height) sum += gradient[y * width + col]
            }
            val score = sum / span.toFloat()
            if (score > bestScore) {
                bestScore = score
                bestIndex = position
            }
        }

        // Reject flat frames: require a meaningful average gradient.
        if (bestScore < 6f) return null
        return bestIndex
    }

    fun close() = Unit
}
