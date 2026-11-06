package com.fylio.app.ui.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlin.math.max
import kotlin.math.min
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Document-edge analysis for the live camera preview.
 *
 * ML Kit's on-device object detector gives us a document-shaped bounding box for free
 * (the bundled "multiple objects / single image" model finds rectangular paper well),
 * and that box is expanded to a quad that can be handed straight to the crop editor.
 * Frames are dropped while one is in flight, so the preview stays at full frame rate.
 */
class DocumentAnalyzer(
    private val onQuad: (ScanQuad) -> Unit
) : ImageAnalysis.Analyzer {

    private val busy = AtomicBoolean(false)
    private val detector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .build()
    )

    override fun analyze(imageProxy: ImageProxy) {
        if (!busy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            busy.set(false)
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val input = InputImage.fromMediaImage(mediaImage, rotation)
        val rawWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
        val rawHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

        detector.process(input)
            .addOnSuccessListener { objects ->
                onQuad(pickDocument(objects, rawWidth.toFloat(), rawHeight.toFloat()) ?: ScanQuad.Full)
            }
            .addOnFailureListener { onQuad(ScanQuad.Full) }
            .addOnCompleteListener {
                busy.set(false)
                imageProxy.close()
            }
    }

    /**
     * Chooses the most document-like object: large, roughly rectangular, centred.
     * Coordinates arrive in image-pixel space and are normalised here so the caller can
     * map them onto any view size with the same matrix maths.
     */
    private fun pickDocument(
        objects: List<DetectedObject>,
        width: Float,
        height: Float
    ): ScanQuad? {
        if (objects.isEmpty() || width <= 0f || height <= 0f) return null

        var best: DetectedObject? = null
        var bestScore = 0f
        objects.forEach { obj ->
            val box = obj.boundingBox
            if (box.width() < width * 0.25f || box.height() < height * 0.25f) return@forEach
            val area = (box.width() * box.height()) / (width * height)
            val aspect = box.width().toFloat() / max(1, box.height())
            // A4 portrait is ~0.71, landscape ~1.41 — accept a generous band around both
            val aspectScore = when {
                aspect in 0.55f..0.85f -> 1f
                aspect in 1.2f..1.75f -> 1f
                aspect in 0.4f..2.2f -> 0.6f
                else -> 0.2f
            }
            val sizeScore = min(area * 2.2f, 1f)
            val score = sizeScore * aspectScore
            if (score > bestScore) {
                bestScore = score
                best = obj
            }
        }

        val box = best?.boundingBox ?: return null
        // A touch of inward padding: detectors habitually clip the page edge.
        val padX = box.width() * 0.015f
        val padY = box.height() * 0.015f
        val left = ((box.left + padX) / width).coerceIn(0f, 1f)
        val top = ((box.top + padY) / height).coerceIn(0f, 1f)
        val right = ((box.right - padX) / width).coerceIn(0f, 1f)
        val bottom = ((box.bottom - padY) / height).coerceIn(0f, 1f)
        if (right - left < 0.15f || bottom - top < 0.15f) return null

        return ScanQuad(left, top, right, top, right, bottom, left, bottom)
    }

    fun close() {
        runCatching { detector.close() }
    }
}
