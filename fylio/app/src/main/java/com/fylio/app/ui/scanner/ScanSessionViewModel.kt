package com.fylio.app.ui.scanner

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.fylio.app.core.image.ImageEngine
import com.fylio.app.data.FylioStore
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Document colour modes exposed in the enhance step. */
enum class ScanFilter(val label: String) {
    ORIGINAL("Original"),
    MAGIC("Magic Color"),
    GRAYSCALE("Grayscale"),
    B_AND_W("B&W")
}

/**
 * Four document corners in normalised (0..1) image coordinates.
 *
 * Stored clockwise from the top-left so the warp code always receives a consistent
 * winding order, which is what the homography solver expects.
 */
data class ScanQuad(
    val tlX: Float, val tlY: Float,
    val trX: Float, val trY: Float,
    val brX: Float, val brY: Float,
    val blX: Float, val blY: Float
) {
    val isFull: Boolean
        get() = kotlin.math.abs(tlX) < 0.002f && kotlin.math.abs(tlY) < 0.002f &&
            kotlin.math.abs(trX - 1f) < 0.002f && kotlin.math.abs(brY - 1f) < 0.002f &&
            kotlin.math.abs(blX) < 0.002f

    fun xs(): FloatArray = floatArrayOf(tlX, trX, brX, blX)
    fun ys(): FloatArray = floatArrayOf(tlY, trY, brY, blY)

    fun corners(): List<Pair<Float, Float>> =
        listOf(tlX to tlY, trX to trY, brX to brY, blX to blY)

    fun replaced(index: Int, x: Float, y: Float): ScanQuad {
        val nx = x.coerceIn(0f, 1f)
        val ny = y.coerceIn(0f, 1f)
        return when (index) {
            0 -> copy(tlX = nx, tlY = ny)
            1 -> copy(trX = nx, trY = ny)
            2 -> copy(brX = nx, brY = ny)
            else -> copy(blX = nx, blY = ny)
        }
    }

    /** Shoelace area, used to reject wild detections. */
    fun area(): Float {
        val xs = xs(); val ys = ys()
        var sum = 0f
        for (i in 0..3) {
            val j = (i + 1) % 4
            sum += xs[i] * ys[j] - xs[j] * ys[i]
        }
        return kotlin.math.abs(sum) / 2f
    }

    companion object {
        val Full = ScanQuad(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
        val Default = ScanQuad(0.06f, 0.06f, 0.94f, 0.06f, 0.94f, 0.94f, 0.06f, 0.94f)
    }
}

/**
 * One page of the current scan session. The full-resolution capture lives in a JPEG on
 * disk and every edit (crop quad, rotation, filter, sliders) is a non-destructive
 * parameter, so undo/redo and re-cropping never touch the original pixels.
 */
data class ScannedPage(
    val id: String,
    val sourceFile: File,
    val quad: ScanQuad = ScanQuad.Full,
    val rotation: Int = 0,
    val filter: ScanFilter = ScanFilter.ORIGINAL,
    val brightness: Float = 0f,
    val contrast: Float = 1f
)

data class ScanSession(
    val pages: List<ScannedPage> = emptyList(),
    val activeId: String? = null,
    val detectedQuad: ScanQuad? = null
) {
    val active: ScannedPage? get() = pages.firstOrNull { it.id == activeId } ?: pages.firstOrNull()
    val activeIndex: Int get() = pages.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
}

/**
 * Single source of truth for the whole scan pipeline. It survives navigation between the
 * camera, crop editor and enhance screens, so backing out of an editor never loses pages.
 */
class ScanSessionViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(ScanSession())
    val state: StateFlow<ScanSession> = _state.asStateFlow()

    // ---------------------------------------------------------------- session io

    private fun sessionDir(): File =
        File(FylioStore.scansDir(getApplication()), "session").apply { mkdirs() }

    suspend fun addBitmap(bitmap: Bitmap, detected: ScanQuad? = null): ScannedPage? =
        withContext(Dispatchers.IO) {
            val target = File(sessionDir(), "page_${System.currentTimeMillis()}.jpg")
            val scaled = scaleTo(bitmap, 2400)
            val saved = ImageEngine.saveJpeg(scaled, target, 95) != null
            if (scaled !== bitmap) scaled.recycle()
            if (!saved) return@withContext null
            val page = ScannedPage(
                id = UUID.randomUUID().toString(),
                sourceFile = target,
                quad = detected ?: ScanQuad.Full
            )
            val current = _state.value
            _state.value = current.copy(pages = current.pages + page, activeId = page.id)
            page
        }

    /** Imports images picked from the gallery into the same session. */
    suspend fun addImages(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var added = 0
        uris.forEach { uri ->
            val bitmap = ImageEngine.decode(getApplication(), uri, 2400) ?: return@forEach
            val target = File(sessionDir(), "image_${System.currentTimeMillis()}_$added.jpg")
            if (ImageEngine.saveJpeg(bitmap, target, 95) != null) {
                val page = ScannedPage(id = UUID.randomUUID().toString(), sourceFile = target)
                val current = _state.value
                _state.value = current.copy(pages = current.pages + page, activeId = page.id)
                added++
            }
            bitmap.recycle()
        }
        added
    }

    // ------------------------------------------------------------------ editing

    fun setActive(id: String) {
        _state.value = _state.value.copy(activeId = id)
    }

    fun indexOf(id: String): Int = _state.value.pages.indexOfFirst { it.id == id }

    fun update(id: String, transform: (ScannedPage) -> ScannedPage) {
        val current = _state.value
        _state.value = current.copy(
            pages = current.pages.map { if (it.id == id) transform(it) else it }
        )
    }

    fun remove(id: String) {
        val current = _state.value
        val page = current.pages.firstOrNull { it.id == id } ?: return
        page.sourceFile.delete()
        val remaining = current.pages.filterNot { it.id == id }
        _state.value = current.copy(
            pages = remaining,
            activeId = when {
                remaining.isEmpty() -> null
                current.activeId == id -> remaining[minOf(current.pages.indexOfFirst { it.id == id }, remaining.lastIndex)].id
                else -> current.activeId
            }
        )
    }

    fun move(from: Int, to: Int) {
        val pages = _state.value.pages.toMutableList()
        if (from !in pages.indices || to !in pages.indices || from == to) return
        val item = pages.removeAt(from)
        pages.add(to, item)
        _state.value = _state.value.copy(pages = pages)
    }

    fun setDetectedQuad(quad: ScanQuad?) {
        if (_state.value.detectedQuad == quad) return
        _state.value = _state.value.copy(detectedQuad = quad)
    }

    fun clear() {
        _state.value.pages.forEach { it.sourceFile.delete() }
        _state.value = ScanSession()
    }

    // ------------------------------------------------------------------ render

    /**
     * Applies the non-destructive pipeline: decode → rotate → perspective warp → filter.
     * Always called from a background dispatcher; the UI shows the previous frame while
     * this runs so nothing blocks the main thread.
     */
    suspend fun render(page: ScannedPage, maxDim: Int = 1500): Bitmap? =
        withContext(Dispatchers.IO) {
            val source = ImageEngine.decode(page.sourceFile, maxDim) ?: return@withContext null
            var working = if (page.rotation % 360 == 0) source
            else ImageEngine.rotate(source, page.rotation)
            if (!page.quad.isFull) {
                working = PerspectiveWarp.warp(working, page.quad)
            }
            ScanFilters.apply(working, page.filter, page.brightness, page.contrast)
        }

    /** Full-resolution render used only when compiling the PDF. */
    suspend fun renderForExport(page: ScannedPage, maxDim: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val source = ImageEngine.decode(page.sourceFile, maxDim) ?: return@withContext null
            var working = if (page.rotation % 360 == 0) source
            else ImageEngine.rotate(source, page.rotation)
            if (!page.quad.isFull) working = PerspectiveWarp.warp(working, page.quad)
            ScanFilters.apply(working, page.filter, page.brightness, page.contrast)
        }

    // ------------------------------------------------------------------ helpers

    private fun scaleTo(bitmap: Bitmap, maxDim: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDim) return bitmap
        val scale = maxDim.toFloat() / longest
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    companion object {
        /** Decodes a JPEG capture, normalising EXIF rotation written by CameraX. */
        fun decodeUpright(file: File, maxDim: Int = 2400): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val raw = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
            val rotated = when (
                runCatching {
                    androidx.exifinterface.media.ExifInterface(file)
                        .getAttributeInt(
                            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                        )
                }.getOrDefault(androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL)
            ) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> ImageEngine.rotate(raw, 90)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> ImageEngine.rotate(raw, 180)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> ImageEngine.rotate(raw, 270)
                else -> raw
            }
            return rotated
        }
    }
}
