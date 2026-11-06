package com.fylio.app.ui.toolkit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.fylio.app.ui.components.FylioCustomIcon
import com.fylio.app.ui.components.FylioFileRow
import com.fylio.app.ui.theme.FylioBorderDark
import com.fylio.app.ui.theme.FylioElectricBlue
import com.fylio.app.ui.theme.FylioSurfaceDark
import com.fylio.app.ui.theme.FylioTextPrimary
import com.fylio.app.ui.theme.FylioTextSecondary

private data class ToolInfo(
    val category: String,
    val title: String,
    val description: String,
    val status: String
)

private val toolInfo = mapOf(
    "CONVERT" to ToolInfo(
        "CONVERT", "Convert",
        "Turn PDF pages into images, or images into a fresh PDF.", "Ready"
    ),
    "MERGE" to ToolInfo(
        "MERGE", "Merge",
        "Combine several documents into one PDF, in the order you pick.", "Ready"
    ),
    "SPLIT" to ToolInfo(
        "SPLIT", "Split",
        "Burst every page of a PDF into its own single-page PDF.", "Ready"
    ),
    "EXTRACT" to ToolInfo(
        "EXTRACT", "Extract pages",
        "Pull a page range out of a PDF and keep the original untouched.", "Ready"
    ),
    "OCR" to ToolInfo(
        "OCR", "OCR",
        "Read text from scans and images offline.", "Phase 2 — engine not bundled"
    ),
    "COMPRESS" to ToolInfo(
        "COMPRESS", "Compress",
        "Re-encode pages at a lower quality target to shrink files.", "Phase 2"
    ),
    "ROTATE" to ToolInfo(
        "ROTATE", "Rotate",
        "Fix the orientation of every page in a document.", "Ready for images"
    ),
    "SIGN" to ToolInfo(
        "SIGN", "Sign",
        "Draw a signature and stamp it on a page.", "Phase 2"
    ),
    "LOCK" to ToolInfo(
        "LOCK", "Lock",
        "Password-protect a PDF.", "Phase 2 — no PDF engine yet"
    ),
    "ANNOTATE" to ToolInfo(
        "ANNOTATE", "Annotate",
        "Highlight, draw and add notes on a page.", "Phase 2"
    )
)

@Composable
fun ToolkitScreen(
    category: String,
    documents: List<FylioFile>,
    onBack: () -> Unit,
    onPick: (FylioFile) -> Unit,
    onAction: (String, FylioFile?) -> Unit
) {
    val info = toolInfo[category] ?: ToolInfo(
        category, category.lowercase().replaceFirstChar { it.uppercase() },
        "Choose a document to work with.", "Ready"
    )

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
                text = info.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(FylioSurfaceDark)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FylioCustomIcon(info.category, size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                info.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = FylioTextPrimary
                            )
                            Text(
                                info.status,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (info.status == "Ready") {
                                    com.fylio.app.ui.theme.FylioGreen
                                } else FylioElectricBlue
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        info.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FylioTextSecondary
                    )
                    if (!info.status.startsWith("Ready")) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(FylioElectricBlue.copy(alpha = 0.15f))
                                .clickable { onAction(category, null) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "See what's needed",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FylioElectricBlue
                            )
                        }
                    }
                }
            }

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
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FylioCustomIcon("FILE", size = 40.dp)
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
