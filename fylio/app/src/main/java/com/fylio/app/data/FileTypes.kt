package com.fylio.app.data

import androidx.compose.ui.graphics.Color
import com.fylio.app.ui.theme.AudioContainer
import com.fylio.app.ui.theme.AudioCyan
import com.fylio.app.ui.theme.DocBlue
import com.fylio.app.ui.theme.DocContainer
import com.fylio.app.ui.theme.ImageContainer
import com.fylio.app.ui.theme.ImagePurple
import com.fylio.app.ui.theme.PdfContainer
import com.fylio.app.ui.theme.PdfRed
import com.fylio.app.ui.theme.PptContainer
import com.fylio.app.ui.theme.PptOrange
import com.fylio.app.ui.theme.UtilityContainer
import com.fylio.app.ui.theme.UtilityIndigo
import com.fylio.app.ui.theme.VideoContainer
import com.fylio.app.ui.theme.VideoPink
import com.fylio.app.ui.theme.XlsContainer
import com.fylio.app.ui.theme.XlsGreen
import com.fylio.app.ui.theme.ZipContainer
import com.fylio.app.ui.theme.ZipSlate

/**
 * Every format Fylio can recognise. [accent] is the brand colour, [container] the
 * pastel backing used behind the icon on light surfaces.
 */
enum class DocType(
    val label: String,
    val shortLabel: String,
    val accent: Color,
    val container: Color,
    val extensions: List<String>,
    /** Which shipped icon carries this type. */
    val icon: String
) {
    PDF("PDF", "PDF", PdfRed, PdfContainer, listOf("pdf"), "save_pdf"),
    DOC("Word", "DOC", DocBlue, DocContainer, listOf("doc", "docx", "rtf", "odt"), "pdf_to_word"),
    XLS("Spreadsheet", "XLS", XlsGreen, XlsContainer, listOf("xls", "xlsx", "csv", "ods"), "pdf_to_excel"),
    PPT("Presentation", "PPT", PptOrange, PptContainer, listOf("ppt", "pptx", "odp"), "pdf_to_powerpoint"),
    IMAGE("Images", "IMG", ImagePurple, ImageContainer, listOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic"), "gallery"),
    TEXT("Text", "TXT", ZipSlate, ZipContainer, listOf("txt", "md", "log", "json", "xml", "html", "htm"), "documents_to_pdf"),
    AUDIO("Audio", "AUD", AudioCyan, AudioContainer, listOf("mp3", "wav", "m4a", "ogg", "flac", "aac"), "audio"),
    VIDEO("Videos", "VID", VideoPink, VideoContainer, listOf("mp4", "mkv", "mov", "avi", "webm", "3gp"), "videos"),
    ZIP("Archives", "ZIP", ZipSlate, ZipContainer, listOf("zip", "rar", "7z", "tar", "gz"), "zip"),
    OTHER("Other", "FILE", UtilityIndigo, UtilityContainer, emptyList(), "details");

    companion object {
        fun fromName(name: String): DocType {
            val ext = name.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { it.extensions.contains(ext) } ?: OTHER
        }
    }
}

/**
 * A single document Fylio knows about. [treeUri] is set for files picked through the
 * Storage Access Framework, [localPath] for files Fylio created in its own folder.
 */
data class DocumentItem(
    val uri: String,
    val name: String,
    val sizeBytes: Long,
    val modified: Long,
    val localPath: String? = null,
    val type: DocType = DocType.fromName(name)
) {
    val sizeLabel: String get() = formatSize(sizeBytes)
    val isLocal: Boolean get() = localPath != null
}

fun formatSize(bytes: Long): String = when {
    bytes <= 0L -> "0 B"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.0f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
    else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
}
