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
import androidx.compose.foundation.lazy.LazyRow
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
import com.fylio.app.data.FylioTools
import com.fylio.app.data.ToolDef
import com.fylio.app.data.ToolStatus
import com.fylio.app.ui.LibraryState
import com.fylio.app.ui.SortOrder
import com.fylio.app.ui.components.FylioBrandHeader
import com.fylio.app.ui.components.FylioFileRow
import com.fylio.app.ui.components.FylioSectionHeader
import com.fylio.app.ui.components.FylioToolIcon
import com.fylio.app.ui.components.FylioToolTile
import com.fylio.app.ui.theme.FylioBorderDark
import com.fylio.app.ui.theme.FylioElectricBlue
import com.fylio.app.ui.theme.FylioSurfaceDark
import com.fylio.app.ui.theme.FylioSurfaceDarkElevated
import com.fylio.app.ui.theme.FylioTextPrimary
import com.fylio.app.ui.theme.FylioTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    state: LibraryState,
    onOpen: (FylioFile) -> Unit,
    onTool: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onSectionMore: (String) -> Unit,
    onSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ---------- brand header ----------
        item {
            FylioBrandHeader(
                subtitle = "${state.documents.size} documents · on-device",
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(FylioSurfaceDark)
                            .clickable { onSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        FylioToolIcon("theme_prefs", size = 22.dp)
                    }
                }
            )
        }

        // ---------- main tools ----------
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FylioTools.mainTools.forEach { tool ->
                    MainToolCard(
                        tool = tool,
                        modifier = Modifier.weight(1f),
                        onClick = { onTool(tool.id) }
                    )
                }
            }
        }

        // ---------- search ----------
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

        // ---------- tool sections, each a horizontal rail ----------
        val rails = listOf(
            "PDF Tools",
            "Document Scanner",
            "Create PDF",
            "File Conversion",
            "Editing & Annotation",
            "File Manager",
            "Utilities & Settings"
        )
        items(rails) { title ->
            val section = FylioTools.section(title) ?: return@items
            Column {
                FylioSectionHeader(
                    title = title,
                    modifier = Modifier.padding(top = 6.dp),
                    action = "All",
                    onAction = { onSectionMore(title) }
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(section.tools, key = { it.id }) { tool ->
                        FylioToolTile(
                            tool = tool,
                            tileSize = 58.dp,
                            badge = if (tool.status == ToolStatus.COMING_SOON) "soon" else null,
                            onClick = { onTool(tool.id) }
                        )
                    }
                }
            }
        }

        // ---------- documents ----------
        item {
            FylioSectionHeader(
                title = "Documents",
                modifier = Modifier.padding(top = 8.dp)
            )
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
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FylioToolIcon("folder", size = 44.dp)
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

/** One of the four big cards in the Main Tools row. */
@Composable
private fun MainToolCard(
    tool: ToolDef,
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
        FylioToolIcon(tool.icon, size = 46.dp)
        Spacer(Modifier.height(7.dp))
        Text(
            text = tool.title,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = FylioTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun relativeTime(millis: Long): String {
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

