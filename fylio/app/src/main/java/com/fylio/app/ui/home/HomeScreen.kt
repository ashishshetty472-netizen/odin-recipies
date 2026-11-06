package com.fylio.app.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.data.FylioFile
import com.fylio.app.ui.LibraryState
import com.fylio.app.ui.SortOrder
import com.fylio.app.ui.components.FylioCustomIcon
import com.fylio.app.ui.components.FylioFileRow
import com.fylio.app.ui.components.FylioSectionHeader
import com.fylio.app.ui.theme.FylioBorderDark
import com.fylio.app.ui.theme.FylioElectricBlue
import com.fylio.app.ui.theme.FylioSurfaceDark
import com.fylio.app.ui.theme.FylioTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ToolAction(
    val category: String,
    val title: String,
    val subtitle: String
)

private val tools = listOf(
    ToolAction("SCANNER", "Scan", "Camera → PDF"),
    ToolAction("CONVERT", "Convert", "PDF ⇄ image"),
    ToolAction("MERGE", "Merge", "Join pages"),
    ToolAction("SPLIT", "Split", "Burst a PDF"),
    ToolAction("EXTRACT", "Extract", "Pull pages out"),
    ToolAction("OCR", "OCR", "Read the text"),
    ToolAction("COMPRESS", "Compress", "Shrink files"),
    ToolAction("ROTATE", "Rotate", "Fix orientation")
)

@Composable
fun HomeScreen(
    state: LibraryState,
    onOpen: (FylioFile) -> Unit,
    onScan: () -> Unit,
    onTool: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Fylio",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${state.documents.size} documents · scans, PDFs & images",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FylioTextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FylioSurfaceDark)
                        .clickable { onSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    FylioCustomIcon("SETTINGS", size = 22.dp)
                }
            }
        }

        item { Spacer(Modifier.height(2.dp)) }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search documents", color = FylioTextSecondary) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = FylioTextSecondary)
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Clear",
                            tint = FylioTextSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onQueryChange("") }
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FylioElectricBlue,
                    unfocusedBorderColor = FylioBorderDark,
                    focusedContainerColor = FylioSurfaceDark,
                    unfocusedContainerColor = FylioSurfaceDark
                )
            )
        }

        item {
            Spacer(Modifier.height(4.dp))
            ScanBanner(onScan = onScan)
        }

        item {
            Spacer(Modifier.height(4.dp))
            FylioSectionHeader(title = "Toolkit")
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tools.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { tool ->
                            ToolTile(
                                tool = tool,
                                modifier = Modifier.weight(1f),
                                onClick = { onTool(tool.category) }
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FylioSectionHeader(
                    title = "Documents",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SortOrder.entries.forEach { order ->
                    val selected = state.sortBy == order
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) FylioElectricBlue.copy(alpha = 0.18f)
                                else FylioSurfaceDark
                            )
                            .clickable { onSortChange(order) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = order.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) FylioElectricBlue else FylioTextSecondary
                        )
                    }
                }
            }
        }

        if (state.visible.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FylioCustomIcon("FILE", size = 44.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (state.query.isBlank()) "No documents yet"
                            else "Nothing matches \"${state.query}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = FylioTextSecondary
                        )
                    }
                }
            }
        }

        items(state.visible, key = { it.path }) { doc ->
            FylioFileRow(
                item = doc,
                subtitle = "${doc.kind.label} · ${doc.sizeLabel} · ${relativeTime(doc.modified)}",
                onClick = { onOpen(doc) }
            )
        }
    }
}

@Composable
private fun ToolTile(
    tool: ToolAction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FylioSurfaceDark)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FylioCustomIcon(tool.category, size = 26.dp)
        Spacer(Modifier.height(7.dp))
        Text(
            text = tool.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = tool.subtitle,
            fontSize = 9.sp,
            color = FylioTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ScanBanner(onScan: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(FylioElectricBlue.copy(alpha = 0.13f))
            .clickable(onClick = onScan)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FylioCustomIcon("SCANNER", size = 34.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Scan a document",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Multi-page capture · auto-detect · export to PDF",
                style = MaterialTheme.typography.bodyMedium,
                color = FylioTextSecondary
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(FylioElectricBlue)
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
        }
    }
}

private fun relativeTime(millis: Long): String {
    if (millis <= 0L) return "unknown"
    val diff = System.currentTimeMillis() - millis
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        diff < minute -> "just now"
        diff < hour -> "${diff / minute}m ago"
        diff < day -> "${diff / hour}h ago"
        diff < 7 * day -> "${diff / day}d ago"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))
    }
}
