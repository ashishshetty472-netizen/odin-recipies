package com.fylio.app.data

import android.content.Context
import java.io.File

/**
 * Fylio's own document area (`files/documents`), shared with the FileProvider so scans
 * and exports can be handed to other apps.
 */
object FylioStore {

    fun documentsDir(context: Context): File =
        File(context.filesDir, "documents").apply { mkdirs() }

    fun scansDir(context: Context): File =
        File(documentsDir(context), "Scans").apply { mkdirs() }

    fun exportsDir(context: Context): File =
        File(documentsDir(context), "Exports").apply { mkdirs() }

    fun newScanFile(context: Context, prefix: String = "Scan"): File {
        val stamp = System.currentTimeMillis()
        return File(scansDir(context), "${prefix}_$stamp.jpg")
    }

    fun uniqueFile(dir: File, baseName: String, extension: String): File {
        dir.mkdirs()
        var candidate = File(dir, "$baseName.$extension")
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "${baseName}_$index.$extension")
            index++
        }
        return candidate
    }

    /** Ensures there is at least one document so a fresh install isn't an empty screen. */
    fun seedIfEmpty(context: Context) {
        val dir = documentsDir(context)
        val hasContent = listOf(dir, scansDir(context), exportsDir(context))
            .any { d -> d.listFiles()?.any { it.isFile } == true }
        if (hasContent) return
        File(dir, "Welcome to Fylio.txt").writeText(
            """
            FYLIO — document viewer, scanner, converter.

            What works in this build:
              - Reads PDF (continuous scroll, zoom, page jump, in-document search)
              - Reads DOCX, PPTX and XLSX by parsing the OOXML directly on device
              - Scans with the camera, edits pages, exports a real PDF
              - Merges, splits, extracts, deletes, reorders, rotates and protects PDFs
              - Converts PDF to images, images to PDF, TXT/HTML to PDF
              - OCR (on-device ML Kit) on any image or scan
              - Annotates with ink, highlight, underline, strikethrough and text boxes

            Everything is offline. Documents stay on this device.
            """.trimIndent()
        )
    }
}
