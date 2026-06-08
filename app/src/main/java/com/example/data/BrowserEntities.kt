package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tabs")
data class TabEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val isActive: Boolean = false,
    val isIncognito: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val fileName: String,
    val mimeType: String,
    val filePath: String,
    val status: String, // "PENDING", "DOWNLOADING", "COMPLETED", "FAILED"
    val progress: Float = 0f, // 0 to 100
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_vault")
data class MemoryVaultEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val keyFact: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "knowledge_graph")
data class KnowledgeGraphEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val relationship: String,
    val objectName: String,
    val summary: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

