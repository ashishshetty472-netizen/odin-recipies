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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.BuildConfig
import com.fylio.app.data.formatSize
import com.fylio.app.ui.LibraryUiState
import com.fylio.app.ui.components.FylioCard
import com.fylio.app.ui.components.FylioIcon
import com.fylio.app.ui.components.SectionTitle
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.FylioDimens
import com.fylio.app.ui.theme.LocalFylioSurfaces

/** Reader preferences, persisted by the caller. */
data class ReaderSettings(
    val darkReader: Boolean = false,
    val fontSize: Float = 1f,
    val ocrLanguageLatin: Boolean = true
)

@Composable
fun SettingsScreen(
    state: LibraryUiState,
    reader: ReaderSettings,
    onReaderChange: (ReaderSettings) -> Unit,
    onClearCache: () -> Unit,
    onAbout: () -> Unit,
    onManagePermissions: () -> Unit
) {
    val surfaces = LocalFylioSurfaces.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(surfaces.headerWarm)
                    .statusBarsPadding()
                    .padding(horizontal = FylioDimens.screenPadding, vertical = 16.dp)
            ) {
                Text("Settings", style = MaterialTheme.typography.headlineSmall, color = surfaces.textPrimary)
                Text(
                    "${BuildConfig.VERSION_NAME} · all processing on device",
                    style = MaterialTheme.typography.labelMedium,
                    color = surfaces.textSecondary
                )
            }
        }

        // ---------- reading ----------
        item {
            FylioCard(
                modifier = Modifier.padding(horizontal = FylioDimens.screenPadding, vertical = 8.dp)
            ) {
                SectionTitle("Reading")
                Spacer(Modifier.height(6.dp))
                SettingRow(
                    icon = "theme",
                    title = "Dark reading mode",
                    subtitle = "Applies to the DOCX/TXT reader"
                ) {
                    Switch(
                        checked = reader.darkReader,
                        onCheckedChange = { onReaderChange(reader.copy(darkReader = it)) },
                        colors = SwitchDefaults.colors(checkedTrackColor = ElectricBlue)
                    )
                }
                FontSizeRow(reader.fontSize) { onReaderChange(reader.copy(fontSize = it)) }
            }
        }

        // ---------- storage ----------
        item {
            FylioCard(
                modifier = Modifier.padding(horizontal = FylioDimens.screenPadding, vertical = 8.dp)
            ) {
                SectionTitle("Storage")
                Spacer(Modifier.height(8.dp))
                StatRow("Documents", "${state.documents.size}")
                StatRow("Indexed size", formatSize(state.totalBytes))
                StatRow("Local files", "${state.local.size}")
                Spacer(Modifier.height(10.dp))
                ActionPill("Clear cache", "clear_cache", onClearCache)
            }
        }

        // ---------- permissions ----------
        item {
            FylioCard(
                modifier = Modifier.padding(horizontal = FylioDimens.screenPadding, vertical = 8.dp)
            ) {
                SectionTitle("Permissions & privacy")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Fylio reads files you pick with the system picker and keeps only a " +
                        "persisted read permission for those exact files. Nothing is uploaded. " +
                        "Camera access is used only while the scanner is open.",
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaces.textSecondary
                )
                Spacer(Modifier.height(10.dp))
                ActionPill("Manage permissions", "permissions", onManagePermissions)
            }
        }

        // ---------- about ----------
        item {
            FylioCard(
                modifier = Modifier.padding(horizontal = FylioDimens.screenPadding, vertical = 8.dp)
            ) {
                SectionTitle("About")
                Spacer(Modifier.height(8.dp))
                StatRow("App", "Fylio")
                StatRow("Version", BuildConfig.VERSION_NAME)
                StatRow("Build", BuildConfig.BUILD_LABEL)
                StatRow("Package", BuildConfig.APPLICATION_ID)
                Spacer(Modifier.height(10.dp))
                ActionPill("What's inside", "details", onAbout)
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: String,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    val surfaces = LocalFylioSurfaces.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaces.elevated),
            contentAlignment = Alignment.Center
        ) { FylioIcon(icon, size = 20.dp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = surfaces.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = surfaces.textSecondary)
        }
        trailing()
    }
}

@Composable
private fun FontSizeRow(value: Float, onChange: (Float) -> Unit) {
    val surfaces = LocalFylioSurfaces.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaces.elevated),
            contentAlignment = Alignment.Center
        ) { FylioIcon("text_to_pdf", size = 20.dp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Reader font size", style = MaterialTheme.typography.titleSmall, color = surfaces.textPrimary)
            Text(
                when {
                    value < 0.95f -> "Small"
                    value > 1.15f -> "Large"
                    else -> "Default"
                },
                style = MaterialTheme.typography.labelSmall,
                color = surfaces.textSecondary
            )
        }
        listOf(0.85f to "A", 1f to "A", 1.25f to "A").forEachIndexed { i, (v, label) ->
            val active = kotlin.math.abs(value - v) < 0.05f
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (active) ElectricBlue else Color.Transparent)
                    .border(1.dp, if (active) Color.Transparent else surfaces.cardBorder, CircleShape)
                    .clickable { onChange(v) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = (11 + i * 3).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) Color.White else surfaces.textSecondary
                )
            }
        }
    }
}

@Composable
internal fun StatRow(label: String, value: String) {
    val surfaces = LocalFylioSurfaces.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = surfaces.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = surfaces.textPrimary)
    }
}

@Composable
internal fun ActionPill(text: String, icon: String, onClick: () -> Unit) {
    val surfaces = LocalFylioSurfaces.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaces.elevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FylioIcon(icon, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = surfaces.textPrimary)
    }
}
