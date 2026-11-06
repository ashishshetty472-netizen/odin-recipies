package com.fylio.app.core.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * On-device OCR via ML Kit (bundled Latin model — no network, no API key).
 */
object OcrEngine {

    data class OcrResult(
        val text: String,
        val blocks: List<String>
    )

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognize(bitmap: Bitmap): OcrResult = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { text ->
                val blocks = text.textBlocks.mapNotNull { b -> b.text.takeIf { it.isNotBlank() } }
                cont.resume(OcrResult(text.text, blocks))
            }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    fun close() {
        runCatching { recognizer.close() }
    }
}
