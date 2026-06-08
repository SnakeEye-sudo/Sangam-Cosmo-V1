package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val dao: BrowserDao) {

    // --- History Operations ---
    val allHistory: Flow<List<HistoryEntry>> = dao.getAllHistory()

    suspend fun insertHistory(title: String, url: String) {
        // Avoid adding about:blank or empty urls to history
        if (url.isNotBlank() && !url.startsWith("about:") && !url.startsWith("javascript:")) {
            dao.insertHistory(HistoryEntry(title = title, url = url))
        }
    }

    suspend fun deleteHistory(id: Int) {
        dao.deleteHistory(id)
    }

    suspend fun deleteHistoryEntries(ids: List<Int>) {
        if (ids.isNotEmpty()) {
            dao.deleteHistoryEntries(ids)
        }
    }

    suspend fun clearAllHistory() {
        dao.clearAllHistory()
    }

    // --- Bookmarks Operations ---
    val allBookmarks: Flow<List<BookmarkEntry>> = dao.getAllBookmarks()

    suspend fun insertBookmark(title: String, url: String) {
        if (url.isNotBlank()) {
            dao.insertBookmark(BookmarkEntry(title = title, url = url))
        }
    }

    suspend fun deleteBookmarkByUrl(url: String) {
        dao.deleteBookmarkByUrl(url)
    }

    suspend fun deleteBookmark(id: Int) {
        dao.deleteBookmark(id)
    }

    suspend fun isBookmarked(url: String): Boolean {
        return dao.isBookmarked(url)
    }

    // --- Tabs Operations ---
    val allTabs: Flow<List<TabEntry>> = dao.getAllTabs()

    suspend fun insertTab(tab: TabEntry) {
        dao.insertTab(tab)
    }

    suspend fun updateTab(tab: TabEntry) {
        dao.updateTab(tab)
    }

    suspend fun deleteTab(tab: TabEntry) {
        dao.deleteTab(tab)
    }

    suspend fun clearAllTabs() {
        dao.clearAllTabs()
    }

    // --- Downloads Operations ---
    val allDownloads: Flow<List<DownloadEntry>> = dao.getAllDownloads()

    suspend fun insertDownload(download: DownloadEntry): Long {
        return dao.insertDownload(download)
    }

    suspend fun updateDownload(download: DownloadEntry) {
        dao.updateDownload(download)
    }

    suspend fun getDownloadById(id: Int): DownloadEntry? {
        return dao.getDownloadById(id)
    }

    suspend fun deleteDownload(id: Int) {
        dao.deleteDownload(id)
    }

    // --- Notes Operations ---
    val allNotes: Flow<List<NoteEntry>> = dao.getAllNotes()

    suspend fun insertNote(title: String, content: String) {
        dao.insertNote(NoteEntry(title = title, content = content))
    }

    suspend fun deleteNote(id: Int) {
        dao.deleteNote(id)
    }

    // --- Memory Vault Operations ---
    val allMemories: Flow<List<MemoryVaultEntry>> = dao.getAllMemories()

    suspend fun insertMemory(query: String, keyFact: String) {
        dao.insertMemory(MemoryVaultEntry(query = query, keyFact = keyFact))
    }

    suspend fun deleteMemory(id: Int) {
        dao.deleteMemory(id)
    }

    // --- Knowledge Graph Operations ---
    val allKnowledge: Flow<List<KnowledgeGraphEntry>> = dao.getAllKnowledgeGraph()

    suspend fun insertKnowledge(subject: String, relationship: String, objectName: String, summary: String) {
        dao.insertKnowledge(KnowledgeGraphEntry(subject = subject, relationship = relationship, objectName = objectName, summary = summary))
    }

    suspend fun deleteKnowledge(id: Int) {
        dao.deleteKnowledge(id)
    }
}
