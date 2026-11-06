package com.fylio.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.data.DocType
import com.fylio.app.data.DocumentItem
import com.fylio.app.data.db.DocumentEntity
import com.fylio.app.data.formatSize
import com.fylio.app.ui.LibraryUiState
import com.fylio.app.ui.components.DocumentRow
import com.fylio.app.ui.components.FileTypeBadge
import com.fylio.app.ui.components.FylioCard
import com.fylio.app.ui.components.FylioIcon
import com.fylio.app.ui.components.HeroCard
import com.fylio.app.ui.components.IconTile
import com.fylio.app.ui.components.SectionTitle
import com.fylio.app.ui.components.StorageBar
import com.fylio.app.ui.components.StorageSegment
import com.fylio.app.ui.components.ToolTile
import com.fylio.app.ui.theme.DeepElectricBlue
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.ElectricIndigo
import com.fylio.app.ui.theme.FylioDimens
import com.fylio.app.ui.theme.LocalFylioSurfaces
import com.fylio.app.ui.theme.PdfRed
import com.fylio.app.ui.theme.XlsGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    state: LibraryUiState,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onTool: (String) -> Unit,
    onOpenEntity: (DocumentEntity) -> Unit,
    onSeeAllFiles: () -> Unit,
    onSettings: () -> Unit
) {
    val surfaces = LocalFylioSurfaces.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ---------- warm header zone ----------
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaces.headerWarm)
                    .statusBarsPadding()
                    .padding(
                        start = FylioDimens.screenPadding,
                        end = FylioDimens.screenPadding,
                        top = 12.dp,
                        bottom = 20.dp
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(
                            id = com.fylio.app.R.drawable.ic_fylio_logo
                        ),
                        contentDescription = "Fylio",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Fylio",
                            style = MaterialTheme.typography.headlineSmall,
                            color = surfaces.textPrimary
                        )
                        Text(
                            "Scan · View · Manage",
                            style = MaterialTheme.typography.labelSmall,
                            color = surfaces.textSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(surfaces.card)
                            .clickable(onClick = onSearch)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FylioIcon("search", size = 15.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Search",
                                style = MaterialTheme.typography.labelMedium,
                                color = surfaces.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // ---------- storage overview ----------
        item {
            FylioCard(
                modifier = Modifier.padding(
                    horizontal = FylioDimens.screenPadding,
                    vertical = 14.dp
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Storage Used",
                        style = MaterialTheme.typography.titleMedium,
                        color = surfaces.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${formatSize(state.totalBytes)} · ${state.documents.size} files",
                        style = MaterialTheme.typography.labelMedium,
                        color = surfaces.textSecondary
                    )
                }
                Spacer(Modifier.height(12.dp))
                StorageBar(
                    segments = state.storageSegments.map {
                        StorageSegment(it.first.shortLabel, it.second, it.first.accent)
                    },
                    totalBytes = state.totalBytes
                )
            }
        }

        // ---------- primary actions ----------
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FylioDimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroCard(
                    title = "Scan Document",
                    subtitle = "Camera → PDF",
                    iconId = "scanner",
                    modifier = Modifier.weight(1f),
                    gradient = listOf(ElectricIndigo, DeepElectricBlue),
                    onClick = onScan
                )
                HeroCard(
                    title = "Create PDF",
                    subtitle = "Images & files",
                    iconId = "create_pdf",
                    modifier = Modifier.weight(1f),
                    gradient = listOf(Color(0xFF60A5FA), ElectricBlue),
                    onClick = { onTool("create_pdf") }
                )
            }
        }

        // ---------- continue reading ----------
        if (state.continueReading.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "Continue Reading",
                    modifier = Modifier.padding(
                        start = FylioDimens.screenPadding,
                        end = FylioDimens.screenPadding,
                        top = 22.dp,
                        bottom = 10.dp
                    )
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = FylioDimens.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.continueReading, key = { it.uri }) { doc ->
                        ContinueReadingCard(doc) { onOpenEntity(doc) }
                    }
                }
            }
        }

        // ---------- quick access ----------
        item {
            SectionTitle(
                title = "Quick Access",
                modifier = Modifier.padding(
                    start = FylioDimens.screenPadding,
                    end = FylioDimens.screenPadding,
                    top = 22.dp,
                    bottom = 8.dp
                )
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FylioDimens.screenPadding),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    Triple("merge_pdf", "PDF Tools", "merge_pdf"),
                    Triple("converter", "Convert", "converter"),
                    Triple("ocr", "OCR", "ocr"),
                    Triple("folder", "Files", "folder")
                ).forEach { (icon, label, toolId) ->
                    ToolTile(
                        iconId = icon,
                        label = label,
                        tileSize = 60.dp,
                        onClick = {
                            if (label == "Files") onSeeAllFiles() else onTool(toolId)
                        }
                    )
                }
            }
        }

        // ---------- recent files ----------
        item {
            SectionTitle(
                title = "Recent Files",
                action = "See all",
                onAction = onSeeAllFiles,
                modifier = Modifier.padding(
                    start = FylioDimens.screenPadding,
                    end = FylioDimens.screenPadding,
                    top = 22.dp,
                    bottom = 6.dp
                )
            )
        }
        if (state.documents.isEmpty()) {
            item {
                FylioCard(
                    modifier = Modifier.padding(horizontal = FylioDimens.screenPadding)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile("folder", tileSize = 44.dp, iconSize = 22.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "No documents yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = surfaces.textPrimary
                            )
                            Text(
                                "Scan a page or import a file to get started",
                                style = MaterialTheme.typography.bodySmall,
                                color = surfaces.textSecondary
                            )
                        }
                    }
                }
            }
        } else {
            items(state.documents.take(6), key = { it.uri }) { doc ->
                Box(Modifier.padding(horizontal = FylioDimens.screenPadding)) {
                    DocumentRow(
                        item = doc.toItem(),
                        subtitle = "${doc.typeLabel} · ${formatSize(doc.sizeBytes)} · ${relativeDate(doc.modified)}",
                        onClick = { onOpenEntity(doc) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(doc: DocumentEntity, onClick: () -> Unit) {
    val surfaces = LocalFylioSurfaces.current
    val type = DocType.entries.firstOrNull { it.shortLabel == doc.typeLabel } ?: DocType.OTHER
    Column(
        modifier = Modifier
            .width(230.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(surfaces.card)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FileTypeBadge(type, size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    doc.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = surfaces.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (doc.pageCount > 0) "Page ${(doc.lastPage + 1).coerceAtMost(doc.pageCount)} of ${doc.pageCount}"
                    else "Last opened ${relativeDate(doc.lastOpened)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = surfaces.textSecondary,
                    maxLines = 1
                )
            }
        }
        // No progress bar here by design — the design sheet shows a progress bar in one
        // variant and the final revision removed it, so only the page label is shown.
    }
}

internal fun DocumentEntity.toItem() = DocumentItem(
    uri = uri,
    name = name,
    sizeBytes = sizeBytes,
    modified = modified,
    localPath = localPath
)

internal fun relativeDate(millis: Long): String {
    if (millis <= 0L) return "never"
    val diff = System.currentTimeMillis() - millis
    val day = 24 * 60 * 60 * 1000L
    return when {
        diff < 60_000 -> "just now"
        diff < 60 * 60 * 1000 -> "${diff / 60_000}m ago"
        diff < day -> "${diff / (60 * 60 * 1000)}h ago"
        diff < 7 * day -> "${diff / day}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
    }
}
