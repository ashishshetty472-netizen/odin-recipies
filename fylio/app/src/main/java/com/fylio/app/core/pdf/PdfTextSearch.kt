package com.fylio.app.core.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

/**
 * In-document text search. PDFBox extracts each page's text, so results come with a
 * real page number and a snippet around the match.
 */
object PdfTextSearch {

    data class Hit(val page: Int, val snippet: String, val offset: Int)

    fun search(file: File, query: String, maxHits: Int = 80): List<Hit> {
        if (query.isBlank()) return emptyList()
        val needle = query.lowercase()
        return runCatching {
            val hits = mutableListOf<Hit>()
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                for (p in 1..doc.numberOfPages) {
                    if (hits.size >= maxHits) break
                    stripper.startPage = p
                    stripper.endPage = p
                    val text = runCatching { stripper.getText(doc) }.getOrDefault("")
                    if (text.isBlank()) continue
                    val hay = text.lowercase()
                    var idx = hay.indexOf(needle)
                    while (idx >= 0 && hits.size < maxHits) {
                        val start = (idx - 60).coerceAtLeast(0)
                        val end = (idx + query.length + 90).coerceAtMost(text.length)
                        val snippet = text.substring(start, end)
                            .replace('\n', ' ')
                            .replace(Regex("\\s+"), " ")
                            .trim()
                        hits.add(Hit(page = p, snippet = snippet, offset = idx))
                        idx = hay.indexOf(needle, idx + needle.length)
                    }
                }
            }
            hits
        }.getOrDefault(emptyList())
    }

    /** Full plain text of the document (used by OCR export and text-to-PDF). */
    fun extractAllText(file: File): String = runCatching {
        PDDocument.load(file).use { doc ->
            val stripper = PDFTextStripper()
            stripper.getText(doc)
        }
    }.getOrDefault("")
}
