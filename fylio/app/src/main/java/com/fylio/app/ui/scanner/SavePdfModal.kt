package com.fylio.app.ui.scanner

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val SaveBlue = Color(0xFF2563EB)

/**
 * Export sheet for the scan session.
 *
 * The file name is pre-filled with `Fylio_Scan_yyyyMMdd_HHmmss`, quality and page size are
 * explicit choices, and the user can pick a destination folder through SAF before saving.
 */
@Composable
fun SavePdfModal(
    pageCount: Int,
    onDismiss: () -> Unit,
    onSave: (SaveRequest) -> Unit,
    /** Optional SAF directory picker; when null the app-scoped Documents folder is used. */
    onPickFolder: ((String?) -> Unit)? = null
) {
    val context = LocalContext.current
    var fileName by remember { mutableStateOf(ScanPdfCompiler.normaliseName(ScanPdfCompiler.defaultFileName()).removeSuffix(".pdf")) }
    var quality by remember { mutableStateOf(PdfQuality.MEDIUM) }
    var format by remember { mutableStateOf(PageFormat.A4) }
    var destination by remember { mutableStateOf("Fylio Documents") }
    var destinationTree by remember { mutableStateOf<String?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            destinationTree = uri.toString()
            destination = uri.lastPathSegment?.substringAfterLast(':')?.ifBlank { null }
                ?: "Selected folder"
            onPickFolder?.invoke(uri.toString())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Save PDF", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$pageCount page${if (pageCount == 1) "" else "s"} · generated on this device",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it.replace("\n", "") },
                    label = { Text("File name") },
                    singleLine = true,
                    suffix = { Text(".pdf", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Label("Quality")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PdfQuality.entries.forEach { q ->
                        ChoiceChip(
                            text = when (q) {
                                PdfQuality.LOW -> "Low"
                                PdfQuality.MEDIUM -> "Medium"
                                PdfQuality.HIGH -> "High"
                            },
                            selected = quality == q,
                            modifier = Modifier.weight(1f)
                        ) { quality = q }
                    }
                }
                Text(
                    quality.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(10.dp))
                Label("Page size")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PageFormat.entries.forEach { f ->
                        ChoiceChip(
                            text = f.label,
                            selected = format == f,
                            modifier = Modifier.weight(1f)
                        ) { format = f }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Label("Location")
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { folderPicker.launch(null) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Folder,
                        null,
                        tint = SaveBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            destination,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                        Text(
                            if (destinationTree == null) {
                                "App folder (Private)\nTap to choose another folder"
                            } else "Custom folder · tap to change",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    SaveRequest(
                        fileName = ScanPdfCompiler.normaliseName(fileName),
                        quality = quality,
                        format = format,
                        share = false,
                        destinationTree = destinationTree
                    )
                )
            }) {
                Text("Save & Generate PDF", color = SaveBlue, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onSave(
                    SaveRequest(
                        fileName = ScanPdfCompiler.normaliseName(fileName),
                        quality = quality,
                        format = format,
                        share = true,
                        destinationTree = destinationTree
                    )
                )
            }) {
                Icon(Icons.Outlined.Share, null, tint = SaveBlue, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Share PDF", color = SaveBlue)
            }
        }
    )
}

@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SaveBlue else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 11.sp,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Copies a compiled file into the user-picked SAF folder, when one was chosen.
 * Returns the file that should be shared/opened.
 */
fun publishToDestination(
    context: android.content.Context,
    file: File,
    destinationTree: String?
): File {
    if (destinationTree.isNullOrBlank()) return file
    return runCatching {
        val tree = androidx.documentfile.provider.DocumentFile.fromTreeUri(
            context, android.net.Uri.parse(destinationTree)
        ) ?: return file
        val target = tree.createFile("application/pdf", file.name) ?: return file
        context.contentResolver.openOutputStream(target.uri)?.use { out ->
            file.inputStream().use { input -> input.copyTo(out) }
        }
        file
    }.getOrDefault(file)
}
