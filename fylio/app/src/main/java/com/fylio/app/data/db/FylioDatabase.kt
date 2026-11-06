package com.fylio.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Everything Fylio remembers about a document between launches: where it lives,
 * how far you read, and whether it was opened with a persisted SAF permission.
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val uri: String,
    val name: String,
    val mime: String?,
    val sizeBytes: Long,
    val modified: Long,
    val localPath: String?,
    val typeLabel: String,
    /** Persisted SAF tree/document permission granted by the user. */
    val persistable: Boolean,
    val lastOpened: Long,
    val lastPage: Int,
    val pageCount: Int,
    val favourite: Boolean = false,
    /** Set when the file can no longer be reached (moved/deleted/permission revoked). */
    val missing: Boolean = false
) {
    val progress: Float
        get() = if (pageCount <= 0) 0f else (lastPage.toFloat() / pageCount).coerceIn(0f, 1f)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE missing = 0 ORDER BY lastOpened DESC")
    fun observeRecent(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE missing = 0 AND lastOpened > 0 ORDER BY lastOpened DESC LIMIT :limit")
    fun observeContinueReading(limit: Int): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE uri = :uri")
    suspend fun byUri(uri: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE missing = 0 ORDER BY modified DESC")
    suspend fun all(): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DocumentEntity)

    @Query("UPDATE documents SET lastOpened = :time, pageCount = :pageCount WHERE uri = :uri")
    suspend fun markOpened(uri: String, time: Long, pageCount: Int)

    @Query("UPDATE documents SET lastPage = :page, pageCount = :pageCount WHERE uri = :uri")
    suspend fun saveProgress(uri: String, page: Int, pageCount: Int)

    @Query("UPDATE documents SET favourite = :fav WHERE uri = :uri")
    suspend fun setFavourite(uri: String, fav: Boolean)

    @Query("UPDATE documents SET missing = :missing WHERE uri = :uri")
    suspend fun setMissing(uri: String, missing: Boolean)

    @Query("UPDATE documents SET name = :name WHERE uri = :uri")
    suspend fun rename(uri: String, name: String)

    @Query("DELETE FROM documents WHERE uri = :uri")
    suspend fun delete(uri: String)

    @Query("SELECT * FROM documents WHERE name LIKE '%' || :q || '%' AND missing = 0 ORDER BY lastOpened DESC")
    fun search(q: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE uri = :uri AND missing = 0 AND pageCount > 0")
    suspend fun readingState(uri: String): DocumentEntity?
}

@Database(entities = [DocumentEntity::class], version = 1, exportSchema = false)
abstract class FylioDatabase : RoomDatabase() {
    abstract fun documents(): DocumentDao

    companion object {
        @Volatile private var instance: FylioDatabase? = null

        fun get(context: Context): FylioDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                FylioDatabase::class.java,
                "fylio.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
