package com.fylio.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fylio.app.core.pdf.PdfEngine
import com.fylio.app.data.DocumentItem
import com.fylio.app.data.DocumentRepository
import com.fylio.app.data.DocType
import com.fylio.app.data.FylioStore
import com.fylio.app.data.db.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class LibraryUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val local: List<DocumentItem> = emptyList(),
    val continueReading: List<DocumentEntity> = emptyList(),
    val query: String = "",
    val typeFilter: DocType? = null,
    val sort: SortOrder = SortOrder.RECENT,
    val grid: Boolean = false,
    val loading: Boolean = true,
    val toast: String? = null
) {
    val storageSegments: List<Pair<DocType, Long>>
        get() = documents.groupBy { it.typeLabel }
            .mapNotNull { (label, docs) ->
                val type = DocType.entries.firstOrNull { it.shortLabel == label } ?: return@mapNotNull null
                type to docs.sumOf { it.sizeBytes }
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

    val totalBytes: Long get() = documents.sumOf { it.sizeBytes }

    val visible: List<DocumentEntity>
        get() {
            var list = documents
            if (typeFilter != null) list = list.filter { it.typeLabel == typeFilter.shortLabel }
            if (query.isNotBlank()) {
                list = list.filter { it.name.contains(query, ignoreCase = true) }
            }
            return when (sort) {
                SortOrder.RECENT -> list.sortedByDescending { it.lastOpened.takeIf { t -> t > 0 } ?: it.modified }
                SortOrder.NAME -> list.sortedBy { it.name.lowercase() }
                SortOrder.SIZE -> list.sortedByDescending { it.sizeBytes }
                SortOrder.TYPE -> list.sortedBy { it.typeLabel }
            }
        }
}

enum class SortOrder(val label: String) {
    RECENT("Recent"), NAME("Name"), SIZE("Size"), TYPE("Type")
}

class FylioViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DocumentRepository(app)
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    /** Documents the toolbar is acting on (multi-select in Files). */
    private val _selection = MutableStateFlow<Set<String>>(emptySet())
    val selection: StateFlow<Set<String>> = _selection.asStateFlow()

    init {
        PdfEngine.init(app)
        FylioStore.seedIfEmpty(app)
        viewModelScope.launch {
            repo.recent.collect { docs ->
                _state.value = _state.value.copy(documents = docs, loading = false)
            }
        }
        viewModelScope.launch {
            repo.continueReading.collect { docs ->
                _state.value = _state.value.copy(continueReading = docs)
            }
        }
        refreshLocal()
    }

    fun refreshLocal() {
        viewModelScope.launch {
            val local = repo.localDocuments()
            _state.value = _state.value.copy(local = local)
        }
    }

    fun setQuery(q: String) { _state.value = _state.value.copy(query = q) }
    fun setFilter(t: DocType?) { _state.value = _state.value.copy(typeFilter = t) }
    fun setSort(s: SortOrder) { _state.value = _state.value.copy(sort = s) }
    fun toggleGrid() { _state.value = _state.value.copy(grid = !_state.value.grid) }
    fun clearToast() { _state.value = _state.value.copy(toast = null) }

    private fun toast(msg: String) { _state.value = _state.value.copy(toast = msg) }

    // ------------------------------------------------------------- selection

    fun toggleSelection(uri: String) {
        val cur = _selection.value
        _selection.value = if (uri in cur) cur - uri else cur + uri
    }

    fun clearSelection() { _selection.value = emptySet() }

    fun selectAll() { _selection.value = _state.value.visible.map { it.uri }.toSet() }

    // -------------------------------------------------------------- importing

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            var count = 0
            uris.forEach { uri ->
                val item = repo.importUri(uri)
                if (item != null) count++
            }
            _state.value = _state.value.copy(
                toast = if (count == 0) "Import failed" else "Added $count document(s)"
            )
        }
    }

    fun registerLocalFile(file: File, note: String? = null) {
        viewModelScope.launch {
            repo.registerLocal(file)
            refreshLocal()
            if (note != null) toast(note)
            _state.value = _state.value.copy(toast = note ?: "Saved ${file.name}")
        }
    }

    // --------------------------------------------------------------- reading

    fun openDocument(uri: String, pageCount: Int) {
        viewModelScope.launch { repo.markOpened(uri, pageCount) }
    }

    fun saveProgress(uri: String, page: Int, pageCount: Int) {
        viewModelScope.launch { repo.saveProgress(uri, page, pageCount) }
    }

    suspend fun byUri(uri: String): DocumentEntity? = repo.byUri(uri)

    suspend fun materialize(entity: DocumentEntity): File? = repo.materialize(entity)

    suspend fun isReachable(entity: DocumentEntity): Boolean = repo.isReachable(entity)

    fun rename(entity: DocumentEntity, newName: String) {
        viewModelScope.launch {
            val ok = repo.rename(entity, newName)
            toast(if (ok != null) "Renamed" else "Rename failed")
        }
    }

    fun delete(entity: DocumentEntity) {
        viewModelScope.launch {
            val ok = repo.delete(entity)
            refreshLocal()
            toast(if (ok) "Deleted ${entity.name}" else "Could not delete")
        }
    }

    fun deleteSelected() {
        val uris = _selection.value
        viewModelScope.launch {
            var n = 0
            uris.forEach { uri ->
                repo.byUri(uri)?.let { if (repo.delete(it)) n++ }
            }
            clearSelection()
            refreshLocal()
            toast("Deleted $n document(s)")
        }
    }

    fun favourite(entity: DocumentEntity, fav: Boolean) {
        viewModelScope.launch { repo.setFavourite(entity.uri, fav) }
    }

    // ---------------------------------------------------------------- storage

    fun syncIndex() {
        viewModelScope.launch {
            val withContextIO = withContext(Dispatchers.IO) { repo.allIndexed() }
            var fixed = 0
            withContextIO.forEach { entity ->
                val reachable = repo.isReachable(entity)
                if (!reachable) fixed++
            }
            if (fixed > 0) toast("$fixed document(s) no longer reachable")
        }
    }
}
