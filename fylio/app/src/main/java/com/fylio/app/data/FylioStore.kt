package com.fylio.app.data

import android.content.Context
import java.io.File

/**
 * Single access point for Fylio's own document folder
 * (`context.filesDir/documents`) — shared with the FileProvider declared in the manifest
 * so scans and exports can be opened by other apps.
 */
object FylioStore {

    fun documentsDir(context: Context): File =
        File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }

    fun scansDir(context: Context): File =
        File(documentsDir(context), "Scans").apply { if (!exists()) mkdirs() }

    fun exportsDir(context: Context): File =
        File(documentsDir(context), "Exports").apply { if (!exists()) mkdirs() }

    fun newScanFile(context: Context, prefix: String = "Scan"): File {
        val stamp = System.currentTimeMillis()
        return File(scansDir(context), "${prefix}_$stamp.jpg")
    }

    fun uniqueFile(dir: File, baseName: String, extension: String): File {
        var candidate = File(dir, "$baseName.$extension")
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "${baseName}_$index.$extension")
            index++
        }
        return candidate
    }

    /** Every document Fylio owns, newest first. Seed content is created on first launch. */
    fun listDocuments(context: Context): List<FylioFile> {
        seedIfEmpty(context)
        val roots = listOf(documentsDir(context), scansDir(context), exportsDir(context))
        return roots
            .flatMap { dir -> dir.listFiles()?.filter { it.isFile } ?: emptyList() }
            .distinctBy { it.absolutePath }
            .sortedByDescending { it.lastModified() }
            .map { FylioFile(it, FileKind.fromName(it.name)) }
    }

    fun delete(file: File): Boolean = runCatching { file.delete() }.getOrDefault(false)

    fun rename(file: File, newName: String): File? = runCatching {
        val clean = newName.trim().ifBlank { file.nameWithoutExtension }
        val target = File(file.parentFile, clean)
        if (file.renameTo(target)) target else null
    }.getOrNull()

    fun import(context: Context, name: String, bytes: ByteArray): File? = runCatching {
        val dir = documentsDir(context)
        val ext = name.substringAfterLast('.', "bin")
        val base = name.substringBeforeLast('.', name)
        val target = uniqueFile(dir, base, ext)
        target.writeBytes(bytes)
        target
    }.getOrNull()

    /** Small sample document so a fresh install has something to open. */
    private fun seedIfEmpty(context: Context) {
        val dir = documentsDir(context)
        val hasContent = listOf(dir, scansDir(context), exportsDir(context))
            .any { d -> d.listFiles()?.any { it.isFile } == true }
        if (hasContent) return

        val welcome = File(dir, "Welcome to Fylio.txt")
        welcome.writeText(
            """
            FYLIO — universal document viewer, scanner and PDF toolkit.

            What works in this build (Phase 1):
              • Document library with custom file-type icons
              • PDF reader with page navigation
              • Image viewer with zoom and rotate
              • Text / Word / Excel / Slides document preview
              • Document scanner: auto-detect guide, redesigned shutter, page thumbnails
              • Page adjustments: brightness, contrast, sharpness
              • Rename, delete, and share any document

            Scans are saved to files/documents/Scans and exported files to files/documents/Exports.
            Open PDFs and images from other apps and Fylio will pick them up via "Open with".
            """.trimIndent()
        )
    }
}
