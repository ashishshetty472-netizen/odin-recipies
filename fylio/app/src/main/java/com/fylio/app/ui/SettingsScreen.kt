package com.fylio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.fylio.app.data.formatSize
import com.fylio.app.ui.components.FylioCustomIcon
import com.fylio.app.ui.theme.FylioBorderDark
import com.fylio.app.ui.theme.FylioElectricBlue
import com.fylio.app.ui.theme.FylioSurfaceDark
import com.fylio.app.ui.theme.FylioTextPrimary
import com.fylio.app.ui.theme.FylioTextSecondary

@Composable
fun SettingsScreen(
    documentCount: Int,
    totalBytes: Long,
    onBack: () -> Unit
) {
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
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InfoCard(
                title = "Storage",
                rows = listOf(
                    "Documents" to "$documentCount",
                    "Space used" to formatSize(totalBytes),
                    "Location" to "files/documents"
                )
            )
            InfoCard(
                title = "About",
                rows = listOf(
                    "App" to "Fylio",
                    "Version" to BuildConfig.VERSION_NAME,
                    "Build" to "Phase 1",
                    "Package" to BuildConfig.APPLICATION_ID
                )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FylioSurfaceDark)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FylioCustomIcon("SCANNER", size = 26.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "  Scanner defaults",
                        style = MaterialTheme.typography.titleMedium,
                        color = FylioTextPrimary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Auto-detect is off by default — flick the guide toggle on the scanner top bar " +
                        "when you want edge hints. Captures export at 2400 px on the long edge.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FylioTextSecondary
                )
            }
            Text(
                "Fylio is built on Jetpack Compose. PDF rendering uses the platform PdfRenderer, " +
                    "so no paid SDK is required.",
                fontSize = 11.sp,
                color = FylioTextSecondary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoCard(title: String, rows: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FylioSurfaceDark)
            .padding(16.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = FylioTextPrimary
        )
        Spacer(Modifier.height(10.dp))
        rows.forEach { (label, value) ->
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
    }
}
