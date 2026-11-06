package com.fylio.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.R
import com.fylio.app.data.DocType
import com.fylio.app.data.DocumentItem
import com.fylio.app.ui.theme.DeepElectricBlue
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.ElectricIndigo
import com.fylio.app.ui.theme.FylioDimens
import com.fylio.app.ui.theme.LocalFylioSurfaces

/**
 * The single icon primitive. Every tool icon is a colour vector on a transparent
 * background, framed by a pastel squircle container — per the design system rules
 * (24dp-ish glyph inside a 56dp squircle, unified padding).
 */
@Composable
fun FylioIcon(
    iconId: String,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp
) {
    Image(
        painter = painterResource(id = resolveIcon(iconId)),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

/**
 * Icon ids are stable strings shared with the Tools screen and the file-type table.
 * Each maps to one vector/PNG glyph shipped in res/drawable.
 */
fun resolveIcon(iconId: String): Int = when (iconId) {
    "audio" -> R.drawable.ic_type_audio
    "videos", "video" -> R.drawable.ic_type_video
    "zip" -> R.drawable.ic_type_zip
    "chevron_right" -> R.drawable.ic_tool_chevron_right
    "folder" -> R.drawable.ic_tool_folder
    "search" -> R.drawable.ic_tool_search
    else -> ICON_MAP[iconId] ?: R.drawable.ic_tool_documents_to_pdf
}

private val ICON_MAP: Map<String, Int> = mapOf(
    "about" to R.drawable.ic_tool_about,
    "adjust" to R.drawable.ic_tool_adjust,
    "app_lock" to R.drawable.ic_tool_app_lock,
    "auto_detect" to R.drawable.ic_tool_auto_detect,
    "blank_pdf" to R.drawable.ic_tool_blank_pdf,
    "camera" to R.drawable.ic_tool_camera,
    "camera_to_pdf" to R.drawable.ic_tool_camera_to_pdf,
    "clear_cache" to R.drawable.ic_tool_clear_cache,
    "color" to R.drawable.ic_tool_color,
    "compress_pdf" to R.drawable.ic_tool_compress_pdf,
    "converter" to R.drawable.ic_tool_converter,
    "copy" to R.drawable.ic_tool_copy,
    "create_pdf" to R.drawable.ic_tool_create_pdf,
    "crop" to R.drawable.ic_tool_crop,
    "crop_pdf" to R.drawable.ic_tool_crop_pdf,
    "csv_to_xlsx" to R.drawable.ic_tool_csv_to_xlsx,
    "delete" to R.drawable.ic_tool_delete,
    "delete_pages" to R.drawable.ic_tool_delete_pages,
    "details" to R.drawable.ic_tool_details,
    "documents_to_pdf" to R.drawable.ic_tool_documents_to_pdf,
    "docx_to_pdf" to R.drawable.ic_tool_docx_to_pdf,
    "draw" to R.drawable.ic_tool_draw,
    "duplicates" to R.drawable.ic_tool_duplicates,
    "eraser" to R.drawable.ic_tool_eraser,
    "extract_pages" to R.drawable.ic_tool_extract_pages,
    "favourite" to R.drawable.ic_tool_favourite,
    "feedback" to R.drawable.ic_tool_feedback,
    "file_prefs" to R.drawable.ic_tool_file_prefs,
    "filter" to R.drawable.ic_tool_filter,
    "filters" to R.drawable.ic_tool_filters,
    "flash" to R.drawable.ic_tool_flash,
    "form_fill" to R.drawable.ic_tool_form_fill,
    "gallery" to R.drawable.ic_tool_gallery,
    "grid_list" to R.drawable.ic_tool_grid_list,
    "highlight" to R.drawable.ic_tool_highlight,
    "html_to_pdf" to R.drawable.ic_tool_html_to_pdf,
    "images_to_pdf" to R.drawable.ic_tool_images_to_pdf,
    "images_to_pdf_c" to R.drawable.ic_tool_images_to_pdf_c,
    "jpg_to_png" to R.drawable.ic_tool_jpg_to_png,
    "large_files" to R.drawable.ic_tool_large_files,
    "merge_pdf" to R.drawable.ic_tool_merge_pdf,
    "more" to R.drawable.ic_tool_more,
    "move" to R.drawable.ic_tool_move,
    "multi_page" to R.drawable.ic_tool_multi_page,
    "multiple_images" to R.drawable.ic_tool_multiple_images,
    "notes" to R.drawable.ic_tool_notes,
    "ocr" to R.drawable.ic_tool_ocr,
    "output_prefs" to R.drawable.ic_tool_output_prefs,
    "pdf_to_excel" to R.drawable.ic_tool_pdf_to_excel,
    "pdf_to_images" to R.drawable.ic_tool_pdf_to_images,
    "pdf_to_jpg" to R.drawable.ic_tool_pdf_to_jpg,
    "pdf_to_png" to R.drawable.ic_tool_pdf_to_png,
    "pdf_to_powerpoint" to R.drawable.ic_tool_pdf_to_powerpoint,
    "pdf_to_word" to R.drawable.ic_tool_pdf_to_word,
    "permissions" to R.drawable.ic_tool_permissions,
    "pptx_to_pdf" to R.drawable.ic_tool_pptx_to_pdf,
    "print_pdf" to R.drawable.ic_tool_print_pdf,
    "privacy" to R.drawable.ic_tool_privacy,
    "protect_pdf" to R.drawable.ic_tool_protect_pdf,
    "reading_prefs" to R.drawable.ic_tool_reading_prefs,
    "recent" to R.drawable.ic_tool_recent,
    "redo" to R.drawable.ic_tool_redo,
    "rename" to R.drawable.ic_tool_rename,
    "reorder_pages" to R.drawable.ic_tool_reorder_pages,
    "repair_pdf" to R.drawable.ic_tool_repair_pdf,
    "rotate" to R.drawable.ic_tool_rotate,
    "rotate_pages" to R.drawable.ic_tool_rotate_pages,
    "save_pdf" to R.drawable.ic_tool_save_pdf,
    "scanner" to R.drawable.ic_tool_scanner,
    "shapes" to R.drawable.ic_tool_shapes,
    "share" to R.drawable.ic_tool_share,
    "sign_pdf" to R.drawable.ic_tool_sign_pdf,
    "signature" to R.drawable.ic_tool_signature,
    "sort" to R.drawable.ic_tool_sort,
    "split_pdf" to R.drawable.ic_tool_split_pdf,
    "strikethrough" to R.drawable.ic_tool_strikethrough,
    "text_box" to R.drawable.ic_tool_text_box,
    "text_to_pdf" to R.drawable.ic_tool_text_to_pdf,
    "theme" to R.drawable.ic_tool_theme,
    "txt_to_pdf" to R.drawable.ic_tool_txt_to_pdf,
    "underline" to R.drawable.ic_tool_underline,
    "undo" to R.drawable.ic_tool_undo,
    "unlock_pdf" to R.drawable.ic_tool_unlock_pdf,
    "watermark" to R.drawable.ic_tool_watermark,
    "webp_to_jpg_png" to R.drawable.ic_tool_webp_to_jpg_png,
    "xlsx_to_pdf" to R.drawable.ic_tool_xlsx_to_pdf
)

/** Pastel squircle + glyph: the canonical tool tile from the design sheet. */
@Composable
fun IconTile(
    iconId: String,
    modifier: Modifier = Modifier,
    tileSize: Dp = FylioDimens.iconTile,
    container: Color? = null,
    iconSize: Dp = FylioDimens.iconSize
) {
    val surfaces = LocalFylioSurfaces.current
    val bg = container ?: surfaces.elevated
    Box(
        modifier = modifier
            .size(tileSize)
            .clip(RoundedCornerShape(tileSize * 0.30f))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        FylioIcon(iconId, size = iconSize)
    }
}

/** Icon + label column used in every tool grid. */
@Composable
fun ToolTile(
    iconId: String,
    label: String,
    modifier: Modifier = Modifier,
    tileSize: Dp = FylioDimens.iconTile,
    container: Color? = null,
    labelColor: Color? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconTile(iconId = iconId, tileSize = tileSize, container = container)
        Spacer(Modifier.height(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor ?: LocalFylioSurfaces.current.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 13.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Floating white card used across all screens. */
@Composable
fun FylioCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScopeContent
) {
    val surfaces = LocalFylioSurfaces.current
    val base = modifier.fillMaxWidth()
    Column(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base)
            .clip(RoundedCornerShape(FylioDimens.cardRadius))
            .background(surfaces.card)
            .border(1.dp, surfaces.cardBorder, RoundedCornerShape(FylioDimens.cardRadius))
            .padding(padding)
    ) { content() }
}

typealias ColumnScopeContent = @Composable () -> Unit

/** Section title with the small blue accent bar from the design sheet. */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    val surfaces = LocalFylioSurfaces.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 3.5.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ElectricBlue)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = surfaces.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = ElectricBlue,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/** File-type badge: pastel squircle + the type's glyph. */
@Composable
fun FileTypeBadge(
    type: DocType,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalFylioSurfaces.current
    val container = if (surfaces.isDark) type.accent.copy(alpha = surfaces.pastelScale)
                    else type.container
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        FylioIcon(type.icon, size = size * 0.58f)
    }
}

/** Vibrant gradient hero banner (Scan Document / Create PDF). */
@Composable
fun HeroCard(
    title: String,
    subtitle: String,
    iconId: String,
    modifier: Modifier = Modifier,
    gradient: List<Color> = listOf(ElectricIndigo, DeepElectricBlue),
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            FylioIcon(iconId, size = 26.dp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 2,
            lineHeight = 17.sp
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1
        )
    }
}

/** One segment of the segmented storage bar. */
data class StorageSegment(val label: String, val bytes: Long, val color: Color)

/** Multi-segment rounded storage bar with a legend. */
@Composable
fun StorageBar(
    segments: List<StorageSegment>,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalFylioSurfaces.current
    val used = segments.sumOf { it.bytes }.coerceAtLeast(1L)
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(if (surfaces.isDark) surfaces.elevated else Color(0xFFE9EEF5))
        ) {
            if (segments.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth(0f)
                        .height(10.dp)
                )
            } else {
                segments.forEach { seg ->
                    val weight = (seg.bytes.toFloat() / used).coerceAtLeast(0.0001f)
                    Box(
                        Modifier
                            .fillMaxWidth(weight)
                            .height(10.dp)
                            .background(seg.color)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            segments.forEach { seg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(seg.color)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "${seg.label} ${if (totalBytes > 0) ((seg.bytes * 100) / used) else 0}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = surfaces.textSecondary
                    )
                }
            }
        }
    }
}

/** Pill tag, e.g. "PDF" or "3 pages". */
@Composable
fun Pill(
    text: String,
    tint: Color = ElectricBlue,
    solid: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (solid) tint else tint.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (solid) Color.White else tint
        )
    }
}

/** Document row used by Home recents and the Files list. */
@Composable
fun DocumentRow(
    item: DocumentItem,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val surfaces = LocalFylioSurfaces.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileTypeBadge(item.type)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = surfaces.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = surfaces.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}
