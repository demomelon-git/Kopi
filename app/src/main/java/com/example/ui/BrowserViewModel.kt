package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DataKopiStore
import com.example.data.local.KopiDatabase
import com.example.data.model.Bookmark
import com.example.data.model.DownloadItem
import com.example.data.model.HistoryItem
import com.example.data.model.KopiExtension
import com.example.data.model.KopiFlag
import com.example.data.model.SavedSite
import com.example.data.model.SyncDevice
import com.example.data.model.TabItem
import com.example.data.repository.BrowserRepository
import com.example.data.repository.CrxParser
import com.example.data.repository.ExtensionEngine
import com.example.data.repository.KdParser
import com.example.data.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class InternalScreen {
    object None : InternalScreen()
    object Flags : InternalScreen()
    object Extensions : InternalScreen()
    object Apps : InternalScreen()
    object DataKopi : InternalScreen()
    object Sync : InternalScreen()
    object History : InternalScreen()
    object Bookmarks : InternalScreen()
    object Downloads : InternalScreen()
    object Settings : InternalScreen()
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = KopiDatabase.getDatabase(application)
    private val dao = db.kopiDao()
    val dataKopiStore = DataKopiStore(application)
    val extensionEngine = ExtensionEngine(dao)
    val syncRepository = SyncRepository(dao, dataKopiStore)
    val repository = BrowserRepository(dao, dataKopiStore, extensionEngine, syncRepository)

    val tabs: StateFlow<List<TabItem>> = repository.tabs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<Bookmark>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryItem>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val extensions: StateFlow<List<KopiExtension>> = repository.extensions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadItem>> = repository.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedSites: StateFlow<List<SavedSite>> = repository.savedSites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile = repository.profile
    val flags = repository.flags
    val devices: StateFlow<List<SyncDevice>> = syncRepository.devices
    val isSyncing = syncRepository.isSyncing

    private val _currentTabId = MutableStateFlow<String?>(null)
    val currentTabId: StateFlow<String?> = _currentTabId.asStateFlow()

    private val _urlInput = MutableStateFlow("kopi://newtab")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _pageTitle = MutableStateFlow("Новая вкладка")
    val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    private val _pageProgress = MutableStateFlow(0)
    val pageProgress: StateFlow<Int> = _pageProgress.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _isTabSwitcherVisible = MutableStateFlow(false)
    val isTabSwitcherVisible: StateFlow<Boolean> = _isTabSwitcherVisible.asStateFlow()

    private val _activeInternalScreen = MutableStateFlow<InternalScreen>(InternalScreen.None)
    val activeInternalScreen: StateFlow<InternalScreen> = _activeInternalScreen.asStateFlow()

    private val _desktopMode = MutableStateFlow(false)
    val desktopMode: StateFlow<Boolean> = _desktopMode.asStateFlow()

    private val _findInPageQuery = MutableStateFlow("")
    val findInPageQuery: StateFlow<String> = _findInPageQuery.asStateFlow()

    private val _isFindInPageActive = MutableStateFlow(false)
    val isFindInPageActive: StateFlow<Boolean> = _isFindInPageActive.asStateFlow()

    private val _findMatchCount = MutableStateFlow(0)
    val findMatchCount: StateFlow<Int> = _findMatchCount.asStateFlow()

    private val _activeExtensionAction = MutableStateFlow<KopiExtension?>(null)
    val activeExtensionAction: StateFlow<KopiExtension?> = _activeExtensionAction.asStateFlow()

    private val _showExtensionPermissions = MutableStateFlow<KopiExtension?>(null)
    val showExtensionPermissions: StateFlow<KopiExtension?> = _showExtensionPermissions.asStateFlow()

    private val _isExtensionsMenuVisible = MutableStateFlow(false)
    val isExtensionsMenuVisible: StateFlow<Boolean> = _isExtensionsMenuVisible.asStateFlow()

    private val _pendingInstallExtension = MutableStateFlow<KopiExtension?>(null)
    val pendingInstallExtension: StateFlow<KopiExtension?> = _pendingInstallExtension.asStateFlow()

    private val _customNewTabUrl = MutableStateFlow<String?>("kubik://new_tab")
    val customNewTabUrl: StateFlow<String?> = _customNewTabUrl.asStateFlow()

    val customSchemes = mutableMapOf<String, String>()

    init {
        // Pre-register default KD schemes
        customSchemes["kubik://new_tab"] = """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Кубик — Главная страница</title>
                <style>
                    body { font-family: -apple-system, Roboto, sans-serif; background: #0F172A; color: #F8FAFC; margin: 0; padding: 32px 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 80vh; text-align: center; }
                    .logo { font-size: 44px; font-weight: 900; letter-spacing: -1px; margin-bottom: 8px; }
                    .logo span:nth-child(1) { color: #EA4335; }
                    .logo span:nth-child(2) { color: #FBBC05; }
                    .logo span:nth-child(3) { color: #4285F4; }
                    .logo span:nth-child(4) { color: #34A853; }
                    .logo span:nth-child(5) { color: #EA4335; }
                    .sub { font-size: 14px; color: #94A3B8; margin-bottom: 24px; }
                    .search-box { display: flex; width: 100%; max-width: 540px; background: #1E293B; border-radius: 28px; padding: 6px 16px; border: 1px solid #334155; box-shadow: 0 4px 20px rgba(0,0,0,0.3); }
                    input { flex: 1; background: transparent; border: none; outline: none; color: #fff; font-size: 16px; padding: 10px 4px; }
                    button { background: #3B82F6; color: #fff; border: none; border-radius: 20px; padding: 8px 18px; font-weight: bold; cursor: pointer; }
                    .chips { display: flex; gap: 8px; margin-top: 24px; flex-wrap: wrap; justify-content: center; }
                    .chip { background: #1E293B; border: 1px solid #334155; padding: 8px 16px; border-radius: 20px; color: #38BDF8; text-decoration: none; font-size: 13px; font-weight: 600; }
                    .badge { margin-top: 32px; font-size: 11px; background: rgba(56,189,248,0.15); color: #38BDF8; padding: 6px 14px; border-radius: 12px; }
                </style>
            </head>
            <body>
                <div class="logo"><span>К</span><span>у</span><span>б</span><span>и</span><span>к</span></div>
                <div class="sub">Главная страница и поиск KD расширения (kubik://new_tab)</div>
                <form class="search-box" action="https://www.google.com/search" method="get">
                    <input type="text" name="q" placeholder="Поиск в Кубике или адрес..." autofocus autocomplete="off">
                    <button type="submit">Найти</button>
                </form>
                <div class="chips">
                    <a class="chip" href="https://demomelon-git.github.io/kubik-web-store/">Kubik Store</a>
                    <a class="chip" href="https://127.0.0.1/">127.0.0.1</a>
                    <a class="chip" href="kopi://downloads">Загрузки</a>
                    <a class="chip" href="kopi://datakopi">Дата Копи</a>
                </div>
                <div class="badge">KD Extension Runtime • Active on kubik://new_tab</div>
            </body>
            </html>
        """.trimIndent()

        viewModelScope.launch {
            extensionEngine.seedDefaultExtensionsIfNeeded()
            // Observe tabs to select initial tab
            repository.tabs.collect { tabList ->
                if (tabList.isEmpty()) {
                    val initialTab = repository.createNewTab("kopi://newtab")
                    _currentTabId.value = initialTab.id
                    _urlInput.value = initialTab.url
                    _pageTitle.value = initialTab.title
                } else if (_currentTabId.value == null || tabList.none { it.id == _currentTabId.value }) {
                    val firstTab = tabList.first()
                    _currentTabId.value = firstTab.id
                    _urlInput.value = firstTab.url
                    _pageTitle.value = firstTab.title
                    checkBookmarked(firstTab.url)
                }
            }
        }
    }

    fun onUrlInputChanged(newInput: String) {
        _urlInput.value = newInput
    }

    fun navigateTo(rawInput: String) {
        val trimmed = rawInput.trim()
        val targetUrl = formatUrl(trimmed)
        _urlInput.value = targetUrl

        // Check if internal kopi:// scheme
        if (targetUrl.startsWith("kopi://")) {
            handleInternalUrl(targetUrl)
            return
        }

        _activeInternalScreen.value = InternalScreen.None
        _isTabSwitcherVisible.value = false
        val tabId = _currentTabId.value ?: return

        viewModelScope.launch {
            repository.updateTab(tabId, targetUrl, repository.extractDomain(targetUrl))
            checkBookmarked(targetUrl)
        }
    }

    private fun handleInternalUrl(url: String) {
        val normalized = url.lowercase().trim()
        when {
            normalized == "kopi://flags" -> _activeInternalScreen.value = InternalScreen.Flags
            normalized == "kopi://extensions" -> _activeInternalScreen.value = InternalScreen.Extensions
            normalized == "kopi://apps" -> _activeInternalScreen.value = InternalScreen.Apps
            normalized == "kopi://datakopi" -> _activeInternalScreen.value = InternalScreen.DataKopi
            normalized == "kopi://sync" -> _activeInternalScreen.value = InternalScreen.Sync
            normalized == "kopi://history" -> _activeInternalScreen.value = InternalScreen.History
            normalized == "kopi://bookmarks" -> _activeInternalScreen.value = InternalScreen.Bookmarks
            normalized == "kopi://downloads" -> _activeInternalScreen.value = InternalScreen.Downloads
            normalized == "kopi://settings" -> _activeInternalScreen.value = InternalScreen.Settings
            normalized == "kopi://gs" -> {
                // Direct Google Search / Kopi high-speed search
                _activeInternalScreen.value = InternalScreen.None
                navigateTo("https://www.google.com")
            }
            normalized == "kopi://newtab" -> {
                _activeInternalScreen.value = InternalScreen.None
                _urlInput.value = "kopi://newtab"
                _pageTitle.value = "Новая вкладка"
                val tabId = _currentTabId.value
                if (tabId != null) {
                    viewModelScope.launch { repository.updateTab(tabId, "kopi://newtab", "Новая вкладка") }
                }
            }
            else -> {
                _activeInternalScreen.value = InternalScreen.None
            }
        }
    }

    fun openInternalScreen(screen: InternalScreen) {
        _activeInternalScreen.value = screen
        _isTabSwitcherVisible.value = false
        when (screen) {
            InternalScreen.Flags -> _urlInput.value = "kopi://flags"
            InternalScreen.Extensions -> _urlInput.value = "kopi://extensions"
            InternalScreen.Apps -> _urlInput.value = "kopi://apps"
            InternalScreen.DataKopi -> _urlInput.value = "kopi://datakopi"
            InternalScreen.Sync -> _urlInput.value = "kopi://sync"
            InternalScreen.History -> _urlInput.value = "kopi://history"
            InternalScreen.Bookmarks -> _urlInput.value = "kopi://bookmarks"
            InternalScreen.Downloads -> _urlInput.value = "kopi://downloads"
            InternalScreen.Settings -> _urlInput.value = "kopi://settings"
            InternalScreen.None -> _urlInput.value = currentTabUrl()
        }
    }

    fun closeInternalScreen() {
        _activeInternalScreen.value = InternalScreen.None
        _urlInput.value = currentTabUrl()
    }

    private fun currentTabUrl(): String {
        val tab = tabs.value.find { it.id == _currentTabId.value }
        return tab?.url ?: "kopi://newtab"
    }

    fun createNewTab(url: String = "kopi://newtab", isIncognito: Boolean = false) {
        val resolvedUrl = if (url == "kopi://newtab" && _customNewTabUrl.value != null) {
            _customNewTabUrl.value!!
        } else {
            url
        }
        viewModelScope.launch {
            val tab = repository.createNewTab(resolvedUrl, isIncognito)
            _currentTabId.value = tab.id
            _urlInput.value = tab.url
            _pageTitle.value = tab.title
            _isTabSwitcherVisible.value = false
            _activeInternalScreen.value = InternalScreen.None
            checkBookmarked(resolvedUrl)
        }
    }

    fun selectTab(tabId: String) {
        val tab = tabs.value.find { it.id == tabId } ?: return
        _currentTabId.value = tab.id
        _urlInput.value = tab.url
        _pageTitle.value = tab.title
        _isTabSwitcherVisible.value = false
        _activeInternalScreen.value = InternalScreen.None
        checkBookmarked(tab.url)
    }

    fun closeTab(tabId: String) {
        viewModelScope.launch {
            repository.closeTab(tabId)
            val remaining = tabs.value.filter { it.id != tabId }
            if (remaining.isNotEmpty()) {
                val next = remaining.first()
                _currentTabId.value = next.id
                _urlInput.value = next.url
                _pageTitle.value = next.title
            } else {
                createNewTab("kopi://newtab")
            }
        }
    }

    fun closeAllTabs() {
        viewModelScope.launch {
            repository.closeAllTabs()
            createNewTab("kopi://newtab")
        }
    }

    fun toggleTabSwitcher() {
        _isTabSwitcherVisible.value = !_isTabSwitcherVisible.value
    }

    fun setPageLoadingState(isLoading: Boolean, progress: Int) {
        _isLoading.value = isLoading
        _pageProgress.value = progress
    }

    fun onPageFinished(url: String, title: String?, canBack: Boolean, canForward: Boolean) {
        _isLoading.value = false
        _pageProgress.value = 100
        _canGoBack.value = canBack
        _canGoForward.value = canForward

        if (!url.startsWith("kopi://") && _activeInternalScreen.value == InternalScreen.None) {
            _urlInput.value = url
            val displayTitle = title ?: repository.extractDomain(url)
            _pageTitle.value = displayTitle
            val tabId = _currentTabId.value
            if (tabId != null) {
                viewModelScope.launch {
                    repository.updateTab(tabId, url, displayTitle)
                    checkBookmarked(url)
                }
            }
        }
    }

    fun toggleBookmark() {
        val url = _urlInput.value
        val title = _pageTitle.value
        if (url.startsWith("kopi://") || url.isBlank()) return
        viewModelScope.launch {
            val bookmarked = repository.toggleBookmark(url, title)
            _isBookmarked.value = bookmarked
        }
    }

    private fun checkBookmarked(url: String) {
        if (url.startsWith("kopi://")) {
            _isBookmarked.value = false
            return
        }
        viewModelScope.launch {
            _isBookmarked.value = repository.isBookmarked(url)
        }
    }

    fun toggleDesktopMode() {
        _desktopMode.value = !_desktopMode.value
    }

    fun toggleFlag(flagId: String, isEnabled: Boolean) {
        dataKopiStore.toggleFlag(flagId, isEnabled)
    }

    fun resetFlags() {
        dataKopiStore.resetFlags()
    }

    fun toggleExtension(ext: KopiExtension) {
        viewModelScope.launch {
            repository.toggleExtension(ext)
        }
    }

    fun installExtension(ext: KopiExtension) {
        viewModelScope.launch {
            repository.installExtension(ext)
        }
    }

    fun deleteExtension(id: String) {
        viewModelScope.launch {
            repository.deleteExtension(id)
        }
    }

    fun showInstallPrompt(extension: KopiExtension) {
        _pendingInstallExtension.value = extension
    }

    fun confirmInstallPendingExtension(): KopiExtension? {
        val ext = _pendingInstallExtension.value ?: return null
        viewModelScope.launch {
            repository.installExtension(ext)
        }
        _pendingInstallExtension.value = null
        return ext
    }

    fun dismissInstallPendingExtension() {
        _pendingInstallExtension.value = null
    }

    fun setNewTabUrl(url: String) {
        _customNewTabUrl.value = url
    }

    fun registerCustomScheme(schemeUrl: String, html: String) {
        customSchemes[schemeUrl] = html
    }

    fun addDownloadRecord(fileName: String, url: String, filePath: String, size: Long, mimeType: String) {
        viewModelScope.launch {
            repository.addDownload(
                com.example.data.model.DownloadItem(
                    fileName = fileName,
                    url = url,
                    filePath = filePath,
                    fileSize = size,
                    mimeType = mimeType,
                    status = "COMPLETED",
                    downloadedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            repository.deleteDownload(id)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.clearDownloads()
        }
    }

    fun prepareKdForInstall(bytes: ByteArray, fileName: String): Result<KopiExtension> {
        return try {
            val parsed = KdParser.parseKd(bytes, fileName)
            if (parsed.newTabOverrideUrl.isNotBlank()) {
                setNewTabUrl(parsed.newTabOverrideUrl)
            }
            if (parsed.customSchemeUrl.isNotBlank() && parsed.customHtml.isNotBlank()) {
                registerCustomScheme(parsed.customSchemeUrl, parsed.customHtml)
            }
            _pendingInstallExtension.value = parsed
            Result.success(parsed)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun prepareCrxForInstall(bytes: ByteArray, fileName: String): Result<KopiExtension> {
        return prepareKdForInstall(bytes, fileName)
    }

    suspend fun installCrx(bytes: ByteArray, fileName: String): Result<KopiExtension> {
        return try {
            val parsed = KdParser.parseKd(bytes, fileName)
            if (parsed.newTabOverrideUrl.isNotBlank()) {
                setNewTabUrl(parsed.newTabOverrideUrl)
            }
            if (parsed.customSchemeUrl.isNotBlank() && parsed.customHtml.isNotBlank()) {
                registerCustomScheme(parsed.customSchemeUrl, parsed.customHtml)
            }
            repository.installExtension(parsed)
            Result.success(parsed)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun updateExtensionPermissions(ext: KopiExtension, newPermissions: String) {
        viewModelScope.launch {
            val updated = ext.copy(permissions = newPermissions)
            repository.updateExtension(updated)
            if (_showExtensionPermissions.value?.id == ext.id) {
                _showExtensionPermissions.value = updated
            }
        }
    }

    fun updateExtension(ext: KopiExtension) {
        viewModelScope.launch {
            repository.updateExtension(ext)
        }
    }

    fun toggleExtensionsMenu() {
        _isExtensionsMenuVisible.value = !_isExtensionsMenuVisible.value
    }

    fun openExtensionAction(ext: KopiExtension) {
        _activeExtensionAction.value = ext
        _isExtensionsMenuVisible.value = false
    }

    fun closeExtensionAction() {
        _activeExtensionAction.value = null
    }

    fun openExtensionPermissions(ext: KopiExtension) {
        _showExtensionPermissions.value = ext
    }

    fun closeExtensionPermissions() {
        _showExtensionPermissions.value = null
    }

    fun updateProfile(userId: String, email: String, kopiId: String) {
        dataKopiStore.updateProfile(userId, email, kopiId)
    }

    fun clearSavedSites() {
        viewModelScope.launch {
            repository.clearSavedSites()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
            if (bookmark.url == _urlInput.value) {
                _isBookmarked.value = false
            }
        }
    }

    fun performSync() {
        viewModelScope.launch {
            syncRepository.performSync()
        }
    }

    fun pairNewDevice(code: String, name: String): Boolean {
        return syncRepository.pairNewDevice(code, name)
    }

    fun removeDevice(id: String) {
        syncRepository.removeDevice(id)
    }

    fun setFindInPage(active: Boolean, query: String = "") {
        _isFindInPageActive.value = active
        _findInPageQuery.value = query
    }

    fun setFindMatchCount(count: Int) {
        _findMatchCount.value = count
    }

    private fun formatUrl(input: String): String {
        if (input.startsWith("kopi://", ignoreCase = true)) {
            return input
        }
        if (input.startsWith("http://", ignoreCase = true) || input.startsWith("https://", ignoreCase = true)) {
            return input
        }
        // If it looks like a domain (contains dot without spaces)
        if (input.contains(".") && !input.contains(" ")) {
            return "https://$input"
        }
        // Otherwise treat as Google Search
        return "https://www.google.com/search?q=" + java.net.URLEncoder.encode(input, "UTF-8")
    }
}
