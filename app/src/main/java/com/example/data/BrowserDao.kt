package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {
    // --- History Queries ---
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntry)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistory(id: Int)

    @Query("DELETE FROM history WHERE id IN (:ids)")
    suspend fun deleteHistoryEntries(ids: List<Int>)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()

    // --- Bookmark Queries ---
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(entry: BookmarkEntry)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url LIMIT 1)")
    suspend fun isBookmarked(url: String): Boolean

    // --- Tab Queries ---
    @Query("SELECT * FROM tabs ORDER BY timestamp ASC")
    fun getAllTabs(): Flow<List<TabEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntry)

    @Update
    suspend fun updateTab(tab: TabEntry)

    @Delete
    suspend fun deleteTab(tab: TabEntry)

    @Query("DELETE FROM tabs")
    suspend fun clearAllTabs()

    // --- Download Queries ---
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntry): Long

    @Update
    suspend fun updateDownload(download: DownloadEntry)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Int): DownloadEntry?

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: Int)

    // --- Note Queries ---
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<NoteEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntry)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Int)

    // --- Memory Vault Queries ---
    @Query("SELECT * FROM memory_vault ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryVaultEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryVaultEntry)

    @Query("DELETE FROM memory_vault WHERE id = :id")
    suspend fun deleteMemory(id: Int)

    // --- Knowledge Graph Queries ---
    @Query("SELECT * FROM knowledge_graph ORDER BY timestamp DESC")
    fun getAllKnowledgeGraph(): Flow<List<KnowledgeGraphEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(knowledge: KnowledgeGraphEntry)

    @Query("DELETE FROM knowledge_graph WHERE id = :id")
    suspend fun deleteKnowledge(id: Int)
}
