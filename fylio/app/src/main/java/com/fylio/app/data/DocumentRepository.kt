package com.fylio.app.data

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.fylio.app.data.db.DocumentEntity
import com.fylio.app.data.db.FylioDatabase
import com.fylio.app.core.pdf.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Single entry point for documents.
 *
 * Files can live in two places:
 *  - Fylio's own folder (`files/documents`), used for scans and exports.
 *  - Anywhere the user picked via SAF, kept alive with a persisted permission.
 *
 * Anything unreachable (file moved, deleted, permission revoked) is flagged in Room
 * instead of crashing the UI.
 */
class DocumentRepository(private val context: Context) {

    private val dao = FylioDatabase.get(context).documents()

    val recent: Flow<List<DocumentEntity>> = dao.observeRecent()
    val continueReading: Flow<List<DocumentEntity>> = dao.observeContinueReading(8)

    fun search(query: String): Flow<List<DocumentEntity>> =
        if (query.isBlank()) recent else dao.search(query)

    // ------------------------------------------------------------- local folder

    fun documentsDir(): File = File(context.filesDir, "documents").apply { mkdirs() }
    fun scansDir(): File = File(documentsDir(), "Scans").apply { mkdirs() }
    fun exportsDir(): File = File(documentsDir(), "Exports").apply { mkdirs() }

    /** Every file Fylio owns, as a DocumentItem. */
    suspend fun localDocuments(): List<DocumentItem> = withContext(Dispatchers.IO) {
        listOf(documentsDir(), scansDir(), exportsDir())
            .flatMap { dir -> dir.listFiles()?.filter { it.isFile } ?: emptyList() }
            .distinctBy { it.absolutePath }
            .sortedByDescending { it.lastModified() }
            .map { f ->
                DocumentItem(
                    uri = Uri.fromFile(f).toString(),
                    name = f.name,
                    sizeBytes = f.length(),
                    modified = f.lastModified(),
                    localPath = f.absolutePath
                )
            }
    }

    /** Registers a local file (scan/export) in the history index. */
    suspend fun registerLocal(file: File, mime: String? = null) = withContext(Dispatchers.IO) {
        val item = DocumentItem(
            uri = Uri.fromFile(file).toString(),
            name = file.name,
            sizeBytes = file.length(),
            modified = file.lastModified(),
            localPath = file.absolutePath
        )
        dao.upsert(
            DocumentEntity(
                uri = item.uri,
                name = item.name,
                mime = mime,
                sizeBytes = item.sizeBytes,
                modified = item.modified,
                localPath = item.localPath,
                typeLabel = item.type.shortLabel,
                persistable = false,
                lastOpened = 0L,
                lastPage = 0,
                pageCount = if (item.type == DocType.PDF) PdfEngine.pageCount(file) else 0
            )
        )
        item
    }

    // -------------------------------------------------------------------- SAF

    /** Called for every URI returned by the document picker; persists read access. */
    suspend fun importUri(uri: Uri, takePersistable: Boolean = true): DocumentItem? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (takePersistable) {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                }
                val meta = queryMeta(uri) ?: return@runCatching null
                val item = DocumentItem(
                    uri = uri.toString(),
                    name = meta.first,
                    sizeBytes = meta.second,
                    modified = System.currentTimeMillis(),
                    localPath = null
                )
                dao.upsert(
                    DocumentEntity(
                        uri = item.uri,
                        name = item.name,
                        mime = context.contentResolver.getType(uri),
                        sizeBytes = item.sizeBytes,
                        modified = item.modified,
                        localPath = null,
                        typeLabel = item.type.shortLabel,
                        persistable = takePersistable,
                        lastOpened = 0L,
                        lastPage = 0,
                        pageCount = 0
                    )
                )
                item
            }.getOrNull()
        }

    /** Copies a content URI into Fylio's own folder (used before editing). */
    suspend fun materialize(item: DocumentEntity): File? = withContext(Dispatchers.IO) {
        item.localPath?.let { path ->
            val f = File(path)
            return@withContext if (f.exists()) f else { dao.setMissing(item.uri, true); null }
        }
        runCatching {
            val uri = Uri.parse(item.uri)
            val target = File(documentsDir(), item.name).let { f ->
                if (f.exists()) PdfEngine.unique(documentsDir(), item.name.substringBeforeLast('.'), item.name.substringAfterLast('.', "bin"))
                else f
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { out -> input.copyTo(out) }
            } ?: return@runCatching null
            target
        }.getOrElse {
            dao.setMissing(item.uri, true)
            null
        }
    }

    /** True when the document still resolves (persisted permission still valid). */
    suspend fun isReachable(entity: DocumentEntity): Boolean = withContext(Dispatchers.IO) {
        entity.localPath?.let { return@withContext File(it).exists() }
        runCatching {
            val uri = Uri.parse(entity.uri)
            // DocumentFile.fromSingleUri does not hit the disk, so probe the stream
            context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
    }

    private fun queryMeta(uri: Uri): Pair<String, Long>? {
        var name: String? = null
        var size = 0L
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { c: Cursor ->
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (nameIdx >= 0) name = c.getString(nameIdx)
                    if (sizeIdx >= 0) size = c.getLong(sizeIdx)
                }
            }
        }
        if (name == null) {
            name = DocumentFile.fromSingleUri(context, uri)?.name
                ?: uri.lastPathSegment ?: "Document"
        }
        return name?.let { it to size }
    }

    // ------------------------------------------------------------ reading state

    suspend fun markOpened(uri: String, pageCount: Int) =
        dao.markOpened(uri, System.currentTimeMillis(), pageCount)

    suspend fun saveProgress(uri: String, page: Int, pageCount: Int) =
        dao.saveProgress(uri, page, pageCount)

    suspend fun byUri(uri: String) = dao.byUri(uri)

    suspend fun setFavourite(uri: String, fav: Boolean) = dao.setFavourite(uri, fav)

    suspend fun rename(entity: DocumentEntity, newName: String): String? = withContext(Dispatchers.IO) {
        if (entity.localPath != null) {
            val src = File(entity.localPath)
            val target = File(src.parentFile, newName)
            if (src.renameTo(target)) {
                val newUri = Uri.fromFile(target).toString()
                dao.upsert(
                    entity.copy(
                        uri = newUri, name = newName, localPath = target.absolutePath,
                        modified = System.currentTimeMillis()
                    )
                )
                dao.delete(entity.uri)
                return@withContext newUri
            }
            null
        } else {
            runCatching {
                val doc = DocumentFile.fromSingleUri(context, Uri.parse(entity.uri))
                if (doc != null && doc.renameTo(newName)) {
                    dao.rename(entity.uri, newName)
                    entity.uri
                } else null
            }.getOrNull()
        }
    }

    suspend fun delete(entity: DocumentEntity): Boolean = withContext(Dispatchers.IO) {
        val ok = if (entity.localPath != null) File(entity.localPath).delete()
        else runCatching {
            DocumentFile.fromSingleUri(context, Uri.parse(entity.uri))?.delete() ?: false
        }.getOrDefault(false)
        if (ok) dao.delete(entity.uri) else dao.setMissing(entity.uri, true)
        ok
    }

    suspend fun allIndexed(): List<DocumentEntity> = withContext(Dispatchers.IO) { dao.all() }
}
