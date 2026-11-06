package com.fylio.app.data

import androidx.compose.ui.graphics.Color
import com.fylio.app.ui.theme.FylioCyan
import com.fylio.app.ui.theme.FylioGreen
import com.fylio.app.ui.theme.FylioGrey
import com.fylio.app.ui.theme.FylioOrange
import com.fylio.app.ui.theme.FylioPurple
import com.fylio.app.ui.theme.FylioRed
import java.io.File

/** A document Fylio knows how to present. */
data class FylioFile(
    val file: File,
    val kind: FileKind
) {
    val name: String get() = file.name
    val sizeBytes: Long get() = if (file.exists()) file.length() else 0L
    val sizeLabel: String get() = formatSize(sizeBytes)
    val modified: Long get() = file.lastModified()
    val path: String get() = file.absolutePath
}

enum class FileKind(
    val category: String,
    val label: String,
    val accent: Color,
    val extensions: List<String>
) {
    PDF("PDF", "PDF", FylioRed, listOf("pdf")),
    WORD("WORD", "Word", Color(0xFF2B6CB0), listOf("doc", "docx", "rtf", "odt")),
    EXCEL("EXCEL", "Excel", FylioGreen, listOf("xls", "xlsx", "csv", "ods")),
    PPT("PPT", "Slides", FylioOrange, listOf("ppt", "pptx", "odp")),
    IMAGE("IMAGE", "Image", FylioPurple, listOf("png", "jpg", "jpeg", "webp", "bmp", "gif", "heic")),
    TEXT("FILE", "Text", FylioGrey, listOf("txt", "md", "log", "json", "xml", "html")),
    OTHER("FILE", "File", FylioGrey, emptyList());

    companion object {
        fun fromName(name: String): FileKind {
            val ext = name.substringAfterLast('.', "").lowercase()
            return FileKind.entries.firstOrNull { it.extensions.contains(ext) } ?: OTHER
        }
    }
}

fun formatSize(bytes: Long): String = when {
    bytes <= 0L -> "0 B"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.0f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
    else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
}
