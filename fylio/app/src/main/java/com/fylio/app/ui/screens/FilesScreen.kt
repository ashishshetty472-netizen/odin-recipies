package com.fylio.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.data.DocType
import com.fylio.app.data.db.DocumentEntity
import com.fylio.app.data.formatSize
import com.fylio.app.ui.LibraryUiState
import com.fylio.app.ui.SortOrder
import com.fylio.app.ui.components.DocumentRow
import com.fylio.app.ui.components.FileTypeBadge
import com.fylio.app.ui.components.FylioIcon
import com.fylio.app.ui.components.Pill
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.FylioDimens
import com.fylio.app.ui.theme.LocalFylioSurfaces
import com.fylio.app.util.FileOps
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FilesScreen(
    state: LibraryUiState,
    selection: Set<String>,
    onQuery: (String) -> Unit,
    onFilter: (DocType?) -> Unit,
    onSort: (SortOrder) -> Unit,
    onToggleGrid: () -> Unit,
    onImport: () -> Unit,
    onOpen: (DocumentEntity) -> Unit,
    onLongPress: (String) -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    val surfaces = LocalFylioSurfaces.current
    val context = LocalContext.current
    val selecting = selection.isNotEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ---------- header ----------
        Column(
            Modifier
                .fillMaxWidth()
                .background(surfaces.headerWarm)
                .statusBarsPadding()
                .padding(horizontal = FylioDimens.screenPadding, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selecting) {
                    Text(
                        "${selection.size} selected",
                        style = MaterialTheme.typography.headlineSmall,
                        color = surfaces.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconAction(Icons.Outlined.Close, "Clear") { onClearSelection() }
                    IconAction(Icons.Outlined.Share, "Share") {
                        selection.firstOrNull()?.let { uri ->
                            state.documents.firstOrNull { it.uri == uri }?.let { doc ->
                                val f = doc.localPath?.let { java.io.File(it) }
                                if (f != null && f.exists()) FileOps.share(context, f)
                            }
                        }
                    }
                    IconAction(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error) {
                        onDeleteSelected()
                    }
                } else {
                    Text(
                        "Documents",
                        style = MaterialTheme.typography.headlineSmall,
                        color = surfaces.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Select all",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onSelectAll)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricBlue)
                            .clickable(onClick = onImport)
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text("Import", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            // search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(surfaces.card)
                    .border(1.dp, surfaces.cardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FylioIcon("search", size = 16.dp)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (state.query.isEmpty()) {
                        Text(
                            "Search documents",
                            style = MaterialTheme.typography.bodyMedium,
                            color = surfaces.textSecondary
                        )
                    }
                    BasicTextField(
                        value = state.query,
                        onValueChange = onQuery,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = surfaces.textPrimary
                        ),
                        cursorBrush = SolidColor(ElectricBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.query.isNotEmpty()) {
                    Icon(
                        Icons.Outlined.Close,
                        "Clear",
                        tint = surfaces.textSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onQuery("") }
                    )
                }
            }
        }

        // ---------- filters ----------
        LazyRow(
            contentPadding = PaddingValues(horizontal = FylioDimens.screenPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    label = "All",
                    selected = state.typeFilter == null,
                    onClick = { onFilter(null) }
                )
            }
            items(DocType.entries.filter { it != DocType.OTHER }) { type ->
                FilterChip(
                    label = type.shortLabel,
                    selected = state.typeFilter == type,
                    onClick = { onFilter(if (state.typeFilter == type) null else type) }
                )
            }
        }

        // ---------- sort / layout ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FylioDimens.screenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortOrder.entries.forEach { order ->
                val active = state.sort == order
                Text(
                    text = order.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) ElectricBlue else surfaces.textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSort(order) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleGrid)
                    .padding(6.dp)
            ) {
                FylioIcon(if (state.grid) "reorder_pages" else "grid_list", size = 18.dp)
            }
        }

        if (state.visible.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                FylioIcon("folder", size = 48.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (state.query.isBlank()) "No documents here yet"
                    else "Nothing matches \"${state.query}\"",
                    style = MaterialTheme.typography.titleMedium,
                    color = surfaces.textSecondary
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricBlue)
                        .clickable(onClick = onImport)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("Import a document", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        } else if (state.grid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(FylioDimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.visible, key = { it.uri }) { doc ->
                    GridCard(
                        doc = doc,
                        selected = doc.uri in selection,
                        selecting = selecting,
                        onClick = {
                            if (selecting) onToggleSelect(doc.uri) else onOpen(doc)
                        },
                        onLongClick = { onLongPress(doc.uri) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = FylioDimens.screenPadding,
                    end = FylioDimens.screenPadding,
                    bottom = 20.dp
                )
            ) {
                items(state.visible, key = { it.uri }) { doc ->
                    val selected = doc.uri in selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) ElectricBlue.copy(alpha = 0.08f) else Color.Transparent)
                            .clickable {
                                if (selecting) onToggleSelect(doc.uri) else onOpen(doc)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selecting) {
                            Box(
                                Modifier
                                    .padding(start = 8.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) ElectricBlue else Color.Transparent)
                                    .border(1.5.dp, if (selected) ElectricBlue else surfaces.cardBorder, CircleShape)
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            DocumentRow(
                                item = doc.toItem(),
                                subtitle = "${doc.typeLabel} · ${formatSize(doc.sizeBytes)} · ${relativeDate(doc.modified)}",
                                onClick = {
                                    if (selecting) onToggleSelect(doc.uri) else onOpen(doc)
                                },
                                trailing = {
                                    Pill(doc.typeLabel, tint = docTypeColor(doc))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val surfaces = LocalFylioSurfaces.current
    Icon(
        icon,
        contentDescription = label,
        tint = if (tint == Color.Unspecified) surfaces.textPrimary else tint,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp)
            .size(20.dp)
    )
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val surfaces = LocalFylioSurfaces.current
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) ElectricBlue else surfaces.card)
            .border(
                1.dp,
                if (selected) Color.Transparent else surfaces.cardBorder,
                CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else surfaces.textSecondary
        )
    }
}

@Composable
private fun GridCard(
    doc: DocumentEntity,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val surfaces = LocalFylioSurfaces.current
    val type = DocType.entries.firstOrNull { it.shortLabel == doc.typeLabel } ?: DocType.OTHER
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaces.card)
            .border(
                1.5.dp,
                if (selected) ElectricBlue else surfaces.cardBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FileTypeBadge(type, size = 52.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            doc.name,
            style = MaterialTheme.typography.labelMedium,
            color = surfaces.textPrimary,
            maxLines = 2,
            lineHeight = 13.sp,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            formatSize(doc.sizeBytes),
            style = MaterialTheme.typography.labelSmall,
            color = surfaces.textSecondary
        )
    }
}

@Composable
internal fun docTypeColor(doc: DocumentEntity): Color {
    val type = DocType.entries.firstOrNull { it.shortLabel == doc.typeLabel } ?: DocType.OTHER
    return type.accent
}
