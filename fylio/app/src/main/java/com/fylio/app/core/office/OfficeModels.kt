package com.fylio.app.core.office

/**
 * Format-agnostic model the viewers render. Office formats are parsed into these so the
 * Compose layer never touches XML.
 */

data class OfficeDocument(
    val kind: OfficeKind,
    val title: String,
    val slides: List<Slide> = emptyList(),
    val blocks: List<TextBlock> = emptyList(),
    val sheets: List<Sheet> = emptyList()
)

enum class OfficeKind { WORD, PRESENTATION, SPREADSHEET }

/** A slide/page in a presentation, or a page of a Word document. */
data class Slide(
    val index: Int,
    val lines: List<SlideLine> = emptyList(),
    val aspectRatio: Float = 16f / 9f,
    val background: Int? = null,
    val tables: List<TableData> = emptyList()
)

data class SlideLine(
    val text: String,
    val isTitle: Boolean = false,
    val level: Int = 0,
    val bullet: Boolean = false
)

data class TextBlock(
    val text: String,
    val isHeading: Boolean = false,
    val bullet: Boolean = false,
    val level: Int = 0
)

data class Sheet(
    val name: String,
    val rows: List<List<String>>,
    val columnWidths: List<Float> = emptyList()
)

data class TableData(
    val rows: List<List<String>>
)
