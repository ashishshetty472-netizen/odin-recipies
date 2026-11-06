package com.fylio.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.R
import com.fylio.app.data.FylioFile
import com.fylio.app.data.IconCategory
import com.fylio.app.ui.theme.FylioBorderDark
import com.fylio.app.ui.theme.FylioElectricBlue
import com.fylio.app.ui.theme.FylioSurfaceDark
import com.fylio.app.ui.theme.FylioTextSecondary

/**
 * Maps a category key to one of the 22 custom vector drawables in res/drawable.
 * Keep this in sync with [IconCategory].
 */
@Composable
fun FylioCustomIcon(
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color? = null
) {
    val drawableId = when (category) {
        IconCategory.PDF -> R.drawable.ic_pdf_red
        IconCategory.WORD -> R.drawable.ic_word_blue
        IconCategory.PPT -> R.drawable.ic_ppt_orange
        IconCategory.EXCEL -> R.drawable.ic_excel_green
        IconCategory.IMAGE -> R.drawable.ic_image_purple
        IconCategory.SCANNER -> R.drawable.ic_scanner_cyan
        IconCategory.CONVERT -> R.drawable.ic_convert_gradient
        IconCategory.OCR -> R.drawable.ic_ocr_purple
        IconCategory.FILE -> R.drawable.ic_file_neutral
        IconCategory.SORT -> R.drawable.ic_sort_green
        IconCategory.MERGE -> R.drawable.ic_merge_red
        IconCategory.SPLIT -> R.drawable.ic_split_red
        IconCategory.EXTRACT -> R.drawable.ic_extract_red
        IconCategory.DELETE -> R.drawable.ic_delete_red
        IconCategory.ROTATE -> R.drawable.ic_rotate_blue
        IconCategory.COMPRESS -> R.drawable.ic_compress_red
        IconCategory.LOCK -> R.drawable.ic_lock_amber
        IconCategory.UNLOCK -> R.drawable.ic_unlock_green
        IconCategory.SETTINGS -> R.drawable.ic_settings_grey
        IconCategory.ANNOTATE -> R.drawable.ic_annotation_purple
        IconCategory.CROP -> R.drawable.ic_crop_green
        IconCategory.SIGN -> R.drawable.ic_sign_blue
        IconCategory.PRINT -> R.drawable.ic_pdf_red
        else -> R.drawable.ic_pdf_red
    }
    Image(
        painter = painterResource(id = drawableId),
        contentDescription = category,
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.tint(it) }
    )
}

/** Icon tile used everywhere a document type is shown. */
@Composable
fun FylioFileBadge(
    category: String,
    accent: Color,
    size: Dp = 44.dp,
    cornerRadius: Dp = 13.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        FylioCustomIcon(category = category, size = size * 0.62f)
    }
}

/** Pill-shaped tag, e.g. "2 pages" or "PDF". */
@Composable
fun FylioTag(
    text: String,
    accent: Color = FylioElectricBlue,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.15f))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
    }
}

/** Section header with an optional trailing action. */
@Composable
fun FylioSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = FylioElectricBlue,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAction() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/** Row in the document library. */
@Composable
fun FylioFileRow(
    item: FylioFile,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FylioSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, FylioBorderDark)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FylioFileBadge(category = item.kind.category, accent = item.kind.accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FylioTextSecondary,
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
}
