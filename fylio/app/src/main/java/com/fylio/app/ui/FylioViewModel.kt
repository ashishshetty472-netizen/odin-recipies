package com.fylio.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fylio.app.data.FileKind
import com.fylio.app.data.FylioFile
import com.fylio.app.data.FylioStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class LibraryState(
    val documents: List<FylioFile> = emptyList(),
    val query: String = "",
    val sortBy: SortOrder = SortOrder.RECENT,
    val loading: Boolean = true,
    val message: String? = null
) {
    val visible: List<FylioFile>
        get() {
            val filtered = if (query.isBlank()) documents else documents.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.kind.label.contains(query, ignoreCase = true)
            }
            return when (sortBy) {
                SortOrder.RECENT -> filtered.sortedByDescending { it.modified }
                SortOrder.NAME -> filtered.sortedBy { it.name.lowercase() }
                SortOrder.SIZE -> filtered.sortedByDescending { it.sizeBytes }
                SortOrder.TYPE -> filtered.sortedBy { it.kind.ordinal }
            }
        }
}

enum class SortOrder(val label: String) {
    RECENT("Recent"), NAME("Name"), SIZE("Size"), TYPE("Type")
}

class FylioViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val docs = withContext(Dispatchers.IO) { FylioStore.listDocuments(getApplication()) }
            _state.value = _state.value.copy(documents = docs, loading = false)
        }
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    fun setSort(order: SortOrder) {
        _state.value = _state.value.copy(sortBy = order)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun delete(item: FylioFile, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { FylioStore.delete(item.file) }
            _state.value = _state.value.copy(
                message = if (ok) "Deleted ${item.name}" else "Could not delete ${item.name}"
            )
            refresh()
            onDone()
        }
    }

    fun rename(item: FylioFile, newName: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { FylioStore.rename(item.file, newName) }
            _state.value = _state.value.copy(
                message = if (result != null) "Renamed to ${result.name}" else "Rename failed"
            )
            refresh()
        }
    }

    /** Persists a new document produced by the scanner or a toolkit action. */
    fun saveDocument(file: File, note: String? = null) {
        refresh()
        if (note != null) _state.value = _state.value.copy(message = note)
    }

    fun importIncoming(uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val imported = withContext(Dispatchers.IO) {
                runCatching {
                    val name = queryDisplayName(ctx, uri) ?: "Imported_${System.currentTimeMillis()}"
                    val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@runCatching null
                    FylioStore.import(ctx, name, bytes)
                }.getOrNull()
            }
            _state.value = _state.value.copy(
                message = if (imported != null) "Imported ${imported.name}" else "Import failed"
            )
            refresh()
        }
    }

    private fun queryDisplayName(ctx: android.content.Context, uri: Uri): String? =
        runCatching {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        }.getOrNull()

    fun documentsByKind(kind: FileKind): List<FylioFile> =
        _state.value.documents.filter { it.kind == kind }
}
