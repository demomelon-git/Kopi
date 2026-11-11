package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Bookmark
import com.example.data.model.DownloadItem
import com.example.data.model.HistoryItem
import com.example.data.model.KopiExtension
import com.example.data.model.SavedSite
import com.example.data.model.TabItem
import kotlinx.coroutines.flow.Flow

@Dao
interface KopiDao {
    // Tabs
    @Query("SELECT * FROM tabs ORDER BY lastAccessedAt DESC")
    fun getAllTabs(): Flow<List<TabItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTab(tab: TabItem)

    @Delete
    suspend fun deleteTab(tab: TabItem)

    @Query("DELETE FROM tabs WHERE id = :tabId")
    suspend fun deleteTabById(tabId: String)

    @Query("DELETE FROM tabs")
    suspend fun clearAllTabs()

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    suspend fun isBookmarked(url: String): Boolean

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    // History
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 200")
    fun getHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    // Extensions
    @Query("SELECT * FROM extensions")
    fun getAllExtensions(): Flow<List<KopiExtension>>

    @Query("SELECT * FROM extensions WHERE isEnabled = 1")
    suspend fun getEnabledExtensionsSync(): List<KopiExtension>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtension(extension: KopiExtension)

    @Update
    suspend fun updateExtension(extension: KopiExtension)

    @Delete
    suspend fun deleteExtension(extension: KopiExtension)

    @Query("DELETE FROM extensions WHERE id = :id")
    suspend fun deleteExtensionById(id: String)

    // Saved Sites (Data Kopi)
    @Query("SELECT * FROM saved_sites ORDER BY savedAt DESC")
    fun getSavedSites(): Flow<List<SavedSite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedSite(site: SavedSite)

    @Delete
    suspend fun deleteSavedSite(site: SavedSite)

    @Query("DELETE FROM saved_sites")
    suspend fun clearSavedSites()

    // Downloads
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(item: DownloadItem): Long

    @Delete
    suspend fun deleteDownload(item: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    @Query("DELETE FROM downloads")
    suspend fun clearDownloads()
}
