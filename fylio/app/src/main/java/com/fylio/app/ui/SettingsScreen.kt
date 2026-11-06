package com.fylio.app.ui

import androidx.compose.foundation.background
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
import com.fylio.app.BuildConfig
import com.fylio.app.data.BuildInfo
import com.fylio.app.data.FylioTools
import com.fylio.app.data.ToolStatus
import com.fylio.app.data.formatSize
import com.fylio.app.ui.components.FylioToolIcon
import com.fylio.app.ui.components.FylioToolTile
import com.fylio.app.ui.theme.FylioElectricBlue
import com.fylio.app.ui.theme.FylioSurfaceDark
import com.fylio.app.ui.theme.FylioSurfaceDarkElevated
import com.fylio.app.ui.theme.FylioTextPrimary
import com.fylio.app.ui.theme.FylioTextSecondary

@Composable
fun SettingsScreen(
    documentCount: Int,
    totalBytes: Long,
    onBack: () -> Unit,
    onTool: (String) -> Unit
) {
    val utils = FylioTools.section("Utilities & Settings")?.tools.orEmpty()
    val ready = FylioTools.all.count { it.status != ToolStatus.COMING_SOON }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = FylioTextPrimary)
                }
                Text("Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(FylioSurfaceDark)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(FylioSurfaceDarkElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        FylioToolIcon("about", size = 32.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Fylio", style = MaterialTheme.typography.titleLarge, color = FylioTextPrimary)
                        Text(
                            "${BuildConfig.VERSION_NAME} · ${BuildInfo.BUILD_LABEL}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FylioTextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                InfoRow("Documents", "$documentCount")
                InfoRow("Space used", formatSize(totalBytes))
                InfoRow("Tools ready", "$ready of ${FylioTools.all.size}")
                InfoRow("Icons shipped", "${BuildInfo.ICON_COUNT}")
                InfoRow("Package", BuildConfig.APPLICATION_ID)
            }
        }

        item {
            Text(
                "Utilities",
                style = MaterialTheme.typography.titleMedium,
                color = FylioTextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(((utils.size + 4) / 5 * 84).dp),
                userScrollEnabled = false
            ) {
                items(utils, key = { it.id }) { t ->
                    FylioToolTile(
                        tool = t,
                        tileSize = 54.dp,
                        badge = if (t.status == ToolStatus.COMING_SOON) "soon" else null,
                        onClick = { onTool(t.id) }
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FylioSurfaceDark)
                    .padding(16.dp)
            ) {
                Text(
                    "Everything stays on device",
                    style = MaterialTheme.typography.titleMedium,
                    color = FylioTextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Fylio reads and writes only its own documents folder " +
                        "(files/documents). Nothing is uploaded anywhere. Scans are stored as " +
                        "JPEGs and exports are written to files/documents/Exports.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FylioTextSecondary
                )
            }
        }

        item {
            Text(
                "Scanner: auto-detect is off by default — turn it on from the scanner top bar. " +
                    "Captures export at 2400 px on the long edge. PDF rendering uses the platform " +
                    "PdfRenderer, so no third-party SDK is required.",
                fontSize = 11.sp,
                color = FylioTextSecondary,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = FylioTextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = FylioTextPrimary
        )
    }
}
