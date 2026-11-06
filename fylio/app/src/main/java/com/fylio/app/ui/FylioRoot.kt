package com.fylio.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fylio.app.data.DocType
import com.fylio.app.data.DocumentItem
import com.fylio.app.data.db.DocumentEntity
import com.fylio.app.ui.components.FylioIcon
import com.fylio.app.ui.components.IconTile
import com.fylio.app.ui.screens.FilesScreen
import com.fylio.app.ui.screens.HomeScreen
import com.fylio.app.ui.screens.SettingsScreen
import com.fylio.app.ui.screens.ToolsScreen
import com.fylio.app.ui.theme.ElectricBlue
import com.fylio.app.ui.theme.LocalFylioSurfaces

/** Bottom navigation destinations, in the order from the design sheet. */
enum class Tab(val label: String, val icon: String) {
    HOME("Home", "folder"),
    FILES("Files", "folder"),
    TOOLS("Tools", "adjust"),
    SETTINGS("Settings", "theme")
}

/** Full-screen destinations pushed above the tabs. */
sealed interface Overlay {
    data class Viewer(val uri: String) : Overlay
    data class ToolScreen(val toolId: String) : Overlay
    data object Scanner : Overlay
    data class ScanEditor(val pages: List<String>) : Overlay
    data object Ocr : Overlay
    data object About : Overlay
}

@Composable
fun FylioRoot(
    viewModel: FylioViewModel,
    state: LibraryUiState,
    selection: Set<String>,
    tab: Tab,
    onTabChange: (Tab) -> Unit,
    overlay: Overlay?,
    onOverlay: (Overlay?) -> Unit,
    onOpen: (DocumentEntity) -> Unit,
    onImport: () -> Unit,
    content: @Composable () -> Unit
) {
    val surfaces = LocalFylioSurfaces.current

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                content()
            }
            FylioBottomBar(
                selected = tab,
                onSelect = onTabChange,
                hidden = overlay != null
            )
        }
    }
}

/**
 * Minimal tab bar: white card, subtle electric-blue pill behind the active icon.
 */
@Composable
fun FylioBottomBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    hidden: Boolean = false
) {
    val surfaces = LocalFylioSurfaces.current
    AnimatedVisibility(
        visible = !hidden,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaces.card)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tab.entries.forEach { t ->
                val active = t == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(t) }
                        .padding(vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 26.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) ElectricBlue.copy(alpha = 0.12f) else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        FylioIcon(
                            iconId = when (t) {
                                Tab.HOME -> "folder"
                                Tab.FILES -> "documents_to_pdf"
                                Tab.TOOLS -> "adjust"
                                Tab.SETTINGS -> "theme"
                            },
                            size = 20.dp
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = t.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) ElectricBlue else surfaces.textSecondary
                    )
                }
            }
        }
    }
}
