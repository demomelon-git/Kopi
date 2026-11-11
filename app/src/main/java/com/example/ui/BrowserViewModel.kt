package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DataKopiStore
import com.example.data.local.KopiDatabase
import com.example.data.model.Bookmark
import com.example.data.model.HistoryItem
import com.example.data.model.KopiExtension
import com.example.data.model.KopiFlag
import com.example.data.model.SavedSite
import com.example.data.model.SyncDevice
import com.example.data.model.TabItem
import com.example.data.repository.BrowserRepository
import com.example.data.repository.CrxParser
import com.example.data.repository.ExtensionEngine
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

    init {
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
        viewModelScope.launch {
            val tab = repository.createNewTab(url, isIncognito)
            _currentTabId.value = tab.id
            _urlInput.value = tab.url
            _pageTitle.value = tab.title
            _isTabSwitcherVisible.value = false
            _activeInternalScreen.value = InternalScreen.None
            checkBookmarked(url)
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

    fun prepareCrxForInstall(bytes: ByteArray, fileName: String): Result<KopiExtension> {
        return try {
            val parsed = CrxParser.parseCrx(bytes, fileName)
            _pendingInstallExtension.value = parsed
            Result.success(parsed)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun installCrx(bytes: ByteArray, fileName: String): Result<KopiExtension> {
        return try {
            val parsed = CrxParser.parseCrx(bytes, fileName)
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
