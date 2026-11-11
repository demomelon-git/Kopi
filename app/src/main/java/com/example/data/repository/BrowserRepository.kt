package com.example.data.repository

import com.example.data.local.DataKopiStore
import com.example.data.local.KopiDao
import com.example.data.model.Bookmark
import com.example.data.model.DownloadItem
import com.example.data.model.HistoryItem
import com.example.data.model.KopiExtension
import com.example.data.model.SavedSite
import com.example.data.model.TabItem
import kotlinx.coroutines.flow.Flow
import java.net.URI
import java.util.UUID

class BrowserRepository(
    val dao: KopiDao,
    val dataKopiStore: DataKopiStore,
    val extensionEngine: ExtensionEngine,
    val syncRepository: SyncRepository
) {
    val tabs: Flow<List<TabItem>> = dao.getAllTabs()
    val bookmarks: Flow<List<Bookmark>> = dao.getAllBookmarks()
    val history: Flow<List<HistoryItem>> = dao.getHistory()
    val extensions: Flow<List<KopiExtension>> = dao.getAllExtensions()
    val savedSites: Flow<List<SavedSite>> = dao.getSavedSites()
    val profile = dataKopiStore.profile
    val flags = dataKopiStore.flags

    suspend fun createNewTab(initialUrl: String = "kopi://newtab", isIncognito: Boolean = false): TabItem {
        val tab = TabItem(
            id = UUID.randomUUID().toString(),
            url = initialUrl,
            title = if (initialUrl == "kopi://newtab") "Новая вкладка" else "Загрузка...",
            isIncognito = isIncognito
        )
        dao.insertOrUpdateTab(tab)
        return tab
    }

    suspend fun updateTab(tabId: String, url: String, title: String) {
        val tab = TabItem(
            id = tabId,
            url = url,
            title = if (title.isBlank()) extractDomain(url) else title,
            lastAccessedAt = System.currentTimeMillis()
        )
        dao.insertOrUpdateTab(tab)

        // Add to history if not internal and not incognito
        if (!url.startsWith("kopi://") && url.isNotBlank()) {
            dao.insertHistory(
                HistoryItem(
                    url = url,
                    title = if (title.isBlank()) extractDomain(url) else title,
                    visitedAt = System.currentTimeMillis()
                )
            )

            // Register into Data Kopi saved sites
            val domain = extractDomain(url)
            if (domain.isNotBlank()) {
                dao.insertSavedSite(
                    SavedSite(
                        domain = domain,
                        title = title.ifBlank { domain },
                        dataPayload = """{"kopiId":"${profile.value.kopiId}","syncStatus":"active"}"""
                    )
                )
            }
        }
    }

    suspend fun closeTab(tabId: String) {
        dao.deleteTabById(tabId)
    }

    suspend fun closeAllTabs() {
        dao.clearAllTabs()
    }

    suspend fun toggleBookmark(url: String, title: String): Boolean {
        val exists = dao.isBookmarked(url)
        if (exists) {
            dao.deleteBookmarkByUrl(url)
            return false
        } else {
            dao.insertBookmark(
                Bookmark(
                    url = url,
                    title = title.ifBlank { extractDomain(url) }
                )
            )
            return true
        }
    }

    suspend fun isBookmarked(url: String): Boolean = dao.isBookmarked(url)

    suspend fun deleteBookmark(bookmark: Bookmark) = dao.deleteBookmark(bookmark)

    suspend fun clearHistory() = dao.clearHistory()

    suspend fun toggleExtension(extension: KopiExtension) {
        dao.updateExtension(extension.copy(isEnabled = !extension.isEnabled))
    }

    suspend fun installExtension(extension: KopiExtension) {
        dao.insertExtension(extension)
    }

    val downloads: Flow<List<DownloadItem>> = dao.getAllDownloads()

    suspend fun updateExtension(extension: KopiExtension) {
        dao.updateExtension(extension)
    }

    suspend fun deleteExtension(id: String) {
        dao.deleteExtensionById(id)
    }

    suspend fun clearSavedSites() {
        dao.clearSavedSites()
    }

    suspend fun addDownload(item: DownloadItem): Long {
        return dao.insertDownload(item)
    }

    suspend fun deleteDownload(id: Long) {
        dao.deleteDownloadById(id)
    }

    suspend fun clearDownloads() {
        dao.clearDownloads()
    }

    fun getDataKopiScript(): String {
        val prof = profile.value
        return """
            (function() {
                // Strictly block cookie persistence
                try {
                    Object.defineProperty(document, 'cookie', {
                        get: function() { return ''; },
                        set: function(val) {
                            console.log('[Data Kopi Guard] Cookie persistence blocked. Site data stored in Data Kopi container instead.');
                            return '';
                        },
                        configurable: false
                    });
                } catch(e) {}

                // Replace cookies with Data Kopi container
                window.DataKopi = {
                    userId: "${prof.userId}",
                    email: "${prof.email}",
                    kopiId: "${prof.kopiId}",
                    isCookieReplaced: true,
                    getCredentials: function() {
                        return { userId: "${prof.userId}", email: "${prof.email}", kopiId: "${prof.kopiId}" };
                    }
                };
                console.log('[Kopi Engine] Data Kopi Identity bound for domain:', window.location.hostname);
            })();
        """.trimIndent()
    }

    fun extractDomain(url: String): String {
        return try {
            if (url.startsWith("kopi://") || url.startsWith("kubik://")) return url
            val uri = URI(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
}
