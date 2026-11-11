package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabs")
data class TabItem(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val isIncognito: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "extensions")
data class KopiExtension(
    @PrimaryKey val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val isEnabled: Boolean = true,
    val matches: String = "*",
    val scriptContent: String,
    val source: String = "Формат KD",
    val permissions: String = "storage, activeTab",
    val hasActionButton: Boolean = true,
    val popupHtml: String = "",
    val actionTitle: String = "",
    val newTabOverrideUrl: String = "",
    val customSchemeUrl: String = "",
    val customHtml: String = ""
)

@Entity(tableName = "saved_sites")
data class SavedSite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val title: String,
    val dataPayload: String = "{}",
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val url: String,
    val filePath: String,
    val fileSize: Long = 0,
    val mimeType: String = "",
    val status: String = "COMPLETED", // DOWNLOADING, COMPLETED, FAILED
    val downloadedAt: Long = System.currentTimeMillis()
)

data class KopiFlag(
    val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val category: String = "Experimental"
)

data class DataKopiProfile(
    val userId: String = "usr_94a73e8",
    val email: String = "demoda228@gmail.com",
    val kopiId: String = "KOPI-228-CHROME",
    val isCookieReplacementActive: Boolean = true
)

data class SyncDevice(
    val id: String,
    val name: String,
    val type: String,
    val lastSyncTime: Long,
    val isCurrentDevice: Boolean = false
)
