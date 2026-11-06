package com.fylio.app.ui.tools

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.data.DocType
import com.fylio.app.data.db.DocumentEntity
import com.fylio.app.data.formatSize
import com.fylio.app.ui.components.FileTypeBadge
import com.fylio.app.ui.components.FylioCard
import com.fylio.app.ui.components.FylioIcon
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.FylioDimens
import com.fylio.app.ui.theme.LocalFylioSurfaces

/** Standard shell for a tool screen: header, explanation, document picker, run button. */
@Composable
fun ToolShell(
    title: String,
    iconId: String,
    description: String,
    documents: List<DocumentEntity>,
    selected: Set<String>,
    multiSelect: Boolean,
    busy: Boolean,
    actionLabel: String,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onRun: () -> Unit,
    extra: @Composable (() -> Unit)? = null
) {
    val surfaces = LocalFylioSurfaces.current
    // filter the list to formats the tool can actually handle
    val candidates = documents

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaces.headerWarm)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ Back", color = ElectricBlue) }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = surfaces.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(FylioDimens.screenPadding)
        ) {
            item {
                FylioCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FylioIcon(iconId, size = 34.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            description,
                            style = MaterialTheme.typography.bodySmall,
                            color = surfaces.textSecondary
                        )
                    }
                    if (extra != null) {
                        Spacer(Modifier.height(12.dp))
                        extra()
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    if (multiSelect) "Select documents (in order)" else "Pick a document",
                    style = MaterialTheme.typography.titleMedium,
                    color = surfaces.textPrimary
                )
            }

            if (candidates.isEmpty()) {
                item {
                    Text(
                        "No documents yet — import something first",
                        style = MaterialTheme.typography.bodySmall,
                        color = surfaces.textSecondary,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            items(candidates, key = { it.uri }) { doc ->
                val isSelected = doc.uri in selected
                val type = DocType.entries.firstOrNull { it.shortLabel == doc.typeLabel } ?: DocType.OTHER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) ElectricBlue.copy(alpha = 0.10f) else surfaces.card
                        )
                        .clickable(enabled = !busy) { onToggle(doc.uri) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FileTypeBadge(type, size = 38.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            doc.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = surfaces.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${doc.typeLabel} · ${formatSize(doc.sizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = surfaces.textSecondary
                        )
                    }
                    if (multiSelect && isSelected) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .background(surfaces.card)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (busy) surfaces.elevated else ElectricBlue)
                    .clickable(enabled = !busy && selected.isNotEmpty(), onClick = onRun)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = ElectricBlue,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Working…", color = surfaces.textPrimary, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(
                        actionLabel,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
