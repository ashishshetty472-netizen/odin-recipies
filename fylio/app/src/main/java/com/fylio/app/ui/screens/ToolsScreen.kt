package com.fylio.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fylio.app.ui.components.FylioCard
import com.fylio.app.ui.components.SectionTitle
import com.fylio.app.ui.components.ToolTile
import com.fylio.app.ui.theme.FylioDimens
import com.fylio.app.ui.theme.LocalFylioSurfaces

private data class ToolGroup(
    val title: String,
    val tools: List<Pair<String, String>>  // iconId to label
)

/**
 * Tools tab. Every tile maps to a real implementation id handled in
 * [com.fylio.app.ui.FylioApp]; nothing here is decorative.
 */
@Composable
fun ToolsScreen(onTool: (String) -> Unit) {
    val surfaces = LocalFylioSurfaces.current

    val groups = listOf(
        ToolGroup(
            "PDF Tools", listOf(
                "merge_pdf" to "Merge",
                "split_pdf" to "Split",
                "extract_pages" to "Extract",
                "delete_pages" to "Delete\npages",
                "reorder_pages" to "Reorder",
                "rotate_pages" to "Rotate",
                "compress_pdf" to "Compress",
                "protect_pdf" to "Protect",
                "unlock_pdf" to "Unlock",
                "watermark" to "Watermark",
                "print_pdf" to "Print"
            )
        ),
        ToolGroup(
            "Create", listOf(
                "scanner" to "Scan",
                "images_to_pdf_c" to "Images\nto PDF",
                "camera_to_pdf" to "Camera\nto PDF",
                "blank_pdf" to "Blank\nPDF"
            )
        ),
        ToolGroup(
            "Convert", listOf(
                "pdf_to_images" to "PDF\nto JPG",
                "pdf_to_png" to "PDF\nto PNG",
                "docx_to_pdf" to "DOCX\nto PDF",
                "pptx_to_pdf" to "PPTX\nto PDF",
                "xlsx_to_pdf" to "XLSX\nto PDF",
                "txt_to_pdf" to "TXT\nto PDF",
                "html_to_pdf" to "HTML\nto PDF",
                "jpg_to_png" to "JPG\n⇄ PNG",
                "webp_to_jpg_png" to "WEBP\n⇄ JPG",
                "csv_to_xlsx" to "CSV\nto XLSX"
            )
        ),
        ToolGroup(
            "Edit & Annotate", listOf(
                "highlight" to "Highlight",
                "underline" to "Underline",
                "strikethrough" to "Strike",
                "draw" to "Draw",
                "text_box" to "Text box",
                "signature" to "Sign"
            )
        ),
        ToolGroup(
            "Utilities", listOf(
                "ocr" to "OCR",
                "search" to "Search",
                "details" to "File info",
                "favourite" to "Bookmark",
                "theme" to "Theme",
                "settings" to "Settings"
            )
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(surfaces.headerWarm)
                    .statusBarsPadding()
                    .padding(horizontal = FylioDimens.screenPadding, vertical = 16.dp)
            ) {
                Text(
                    "Tools",
                    style = MaterialTheme.typography.headlineSmall,
                    color = surfaces.textPrimary
                )
                Text(
                    "Everything runs offline on this device",
                    style = MaterialTheme.typography.labelMedium,
                    color = surfaces.textSecondary
                )
            }
        }

        groups.forEach { group ->
            item {
                FylioCard(
                    modifier = Modifier.padding(
                        horizontal = FylioDimens.screenPadding,
                        vertical = 8.dp
                    ),
                    padding = 14.dp
                ) {
                    SectionTitle(group.title)
                    Spacer(Modifier.height(8.dp))
                    // rows of 4
                    group.tools.chunked(4).forEach { rowTools ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            rowTools.forEach { (icon, label) ->
                                ToolTile(
                                    iconId = icon,
                                    label = label,
                                    tileSize = 58.dp,
                                    onClick = { onTool(icon) }
                                )
                            }
                            repeat(4 - rowTools.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
