package com.fylio.app.ui.toolkit

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.data.FylioFile
import com.fylio.app.data.FylioTools
import com.fylio.app.data.ToolDef
import com.fylio.app.data.ToolStatus
import com.fylio.app.ui.components.FylioFileRow
import com.fylio.app.ui.components.FylioToolIcon
import com.fylio.app.ui.components.FylioToolTile
import com.fylio.app.ui.theme.FylioElectricBlue
import com.fylio.app.ui.theme.FylioGreen
import com.fylio.app.ui.theme.FylioSurfaceDark
import com.fylio.app.ui.theme.FylioSurfaceDarkElevated
import com.fylio.app.ui.theme.FylioTextPrimary
import com.fylio.app.ui.theme.FylioTextSecondary

/**
 * A tool workspace: the header explains what the tool does, the grid shows every tool in
 * the same section (so you can switch without going back), and the list lets you pick the
 * document the tool should act on.
 */
@Composable
fun ToolkitScreen(
    toolId: String,
    sectionTitle: String?,
    documents: List<FylioFile>,
    onBack: () -> Unit,
    onPick: (FylioFile) -> Unit,
    onSelectTool: (String) -> Unit,
    onBlocked: (ToolDef) -> Unit
) {
    val tool = FylioTools.byId(toolId)
    val section = sectionTitle?.let { FylioTools.section(it) }
        ?: FylioTools.sections.firstOrNull { s -> s.tools.any { it.id == toolId } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = FylioTextPrimary)
            }
            Text(
                text = section?.title ?: tool?.title ?: "Tools",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ---------- active tool card ----------
            item {
                if (tool != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(FylioSurfaceDark)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FylioToolIcon(tool.icon, size = 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    tool.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FylioTextPrimary
                                )
                                Text(
                                    statusLabel(tool.status),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (tool.status) {
                                        ToolStatus.READY -> FylioGreen
                                        ToolStatus.PICK_DOCUMENT -> FylioElectricBlue
                                        ToolStatus.COMING_SOON -> FylioTextSecondary
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            tool.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FylioTextSecondary
                        )
                        if (tool.status == ToolStatus.COMING_SOON) {
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(FylioElectricBlue.copy(alpha = 0.15f))
                                    .clickable { onBlocked(tool) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "What's needed to ship this",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FylioElectricBlue
                                )
                            }
                        }
                    }
                }
            }

            // ---------- every tool in this section ----------
            if (section != null) {
                item {
                    Text(
                        "Switch tool",
                        style = MaterialTheme.typography.titleMedium,
                        color = FylioTextPrimary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(((section.tools.size + 3) / 4 * 84).dp),
                        userScrollEnabled = false,
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(section.tools, key = { it.id }) { t ->
                            FylioToolTile(
                                tool = t,
                                tileSize = 54.dp,
                                badge = if (t.status == ToolStatus.COMING_SOON) "soon" else null,
                                onClick = { onSelectTool(t.id) }
                            )
                        }
                    }
                }
            }

            // ---------- document picker ----------
            if (tool != null && tool.status != ToolStatus.COMING_SOON) {
                item {
                    Text(
                        "Pick a document",
                        style = MaterialTheme.typography.titleMedium,
                        color = FylioTextPrimary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                if (documents.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FylioToolIcon("folder", size = 40.dp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Nothing here yet — scan or import a file first",
                                color = FylioTextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(documents, key = { it.path }) { doc ->
                        FylioFileRow(
                            item = doc,
                            subtitle = "${doc.kind.label} · ${doc.sizeLabel}",
                            onClick = { onPick(doc) },
                            trailing = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(FylioElectricBlue.copy(alpha = 0.16f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        "Use",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FylioElectricBlue
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: ToolStatus): String = when (status) {
    ToolStatus.READY -> "Ready"
    ToolStatus.PICK_DOCUMENT -> "Ready — pick a document"
    ToolStatus.COMING_SOON -> "Coming in the next phase"
}
