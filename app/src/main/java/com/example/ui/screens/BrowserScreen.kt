package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.KopiExtension
import com.example.ui.BrowserViewModel
import com.example.ui.InternalScreen
import com.example.ui.components.ChromeMenu
import com.example.ui.components.ChromeOmnibox
import com.example.ui.components.ChromeTabSwitcher
import com.example.ui.components.FindInPageBar
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val extensions by viewModel.extensions.collectAsStateWithLifecycle()
    val savedSites by viewModel.savedSites.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val flags by viewModel.flags.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val currentTabId by viewModel.currentTabId.collectAsStateWithLifecycle()
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val pageTitle by viewModel.pageTitle.collectAsStateWithLifecycle()
    val pageProgress by viewModel.pageProgress.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val isTabSwitcherVisible by viewModel.isTabSwitcherVisible.collectAsStateWithLifecycle()
    val activeInternalScreen by viewModel.activeInternalScreen.collectAsStateWithLifecycle()
    val desktopMode by viewModel.desktopMode.collectAsStateWithLifecycle()
    val findInPageQuery by viewModel.findInPageQuery.collectAsStateWithLifecycle()
    val isFindInPageActive by viewModel.isFindInPageActive.collectAsStateWithLifecycle()
    val findMatchCount by viewModel.findMatchCount.collectAsStateWithLifecycle()

    var isMenuExpanded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val currentTab = remember(tabs, currentTabId) {
        tabs.find { it.id == currentTabId }
    }
    val currentUrl = currentTab?.url ?: "kopi://newtab"
    val isNewTab = currentUrl == "kopi://newtab" && activeInternalScreen == InternalScreen.None

    // Back button handling
    BackHandler(enabled = true) {
        when {
            isTabSwitcherVisible -> viewModel.toggleTabSwitcher()
            activeInternalScreen != InternalScreen.None -> viewModel.closeInternalScreen()
            isFindInPageActive -> viewModel.setFindInPage(false)
            webViewRef?.canGoBack() == true -> webViewRef?.goBack()
            !isNewTab -> viewModel.navigateTo("kopi://newtab")
        }
    }

    // Effect to load URL when current tab or urlInput changes
    LaunchedEffect(currentUrl) {
        if (!currentUrl.startsWith("kopi://") && activeInternalScreen == InternalScreen.None) {
            webViewRef?.let { wv ->
                if (wv.url != currentUrl) {
                    wv.loadUrl(currentUrl)
                }
            }
        }
    }

    // Effect for desktop user-agent mode toggle
    LaunchedEffect(desktopMode) {
        webViewRef?.let { wv ->
            val settings = wv.settings
            if (desktopMode) {
                settings.userAgentString =
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 KopiBrowser/1.0"
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
            } else {
                settings.userAgentString = null // default mobile chromium
                settings.useWideViewPort = false
                settings.loadWithOverviewMode = false
            }
            if (wv.url != null && !wv.url!!.startsWith("kopi://")) {
                wv.reload()
            }
        }
    }

    // Find in page effect
    LaunchedEffect(findInPageQuery) {
        if (isFindInPageActive && findInPageQuery.isNotBlank()) {
            webViewRef?.findAllAsync(findInPageQuery)
        } else {
            webViewRef?.clearMatches()
            viewModel.setFindMatchCount(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chrome Omnibox
            ChromeOmnibox(
                url = urlInput,
                isLoading = isLoading,
                progress = pageProgress,
                tabCount = tabs.size,
                onUrlSubmitted = { viewModel.navigateTo(it) },
                onRefreshClicked = { webViewRef?.reload() },
                onStopClicked = { webViewRef?.stopLoading() },
                onTabSwitcherClicked = { viewModel.toggleTabSwitcher() },
                onMenuClicked = { isMenuExpanded = true }
            )

            // Find in Page Bar
            AnimatedVisibility(visible = isFindInPageActive) {
                FindInPageBar(
                    query = findInPageQuery,
                    matchCount = findMatchCount,
                    onQueryChanged = { viewModel.setFindInPage(true, it) },
                    onNextMatch = { webViewRef?.findNext(true) },
                    onPrevMatch = { webViewRef?.findNext(false) },
                    onClose = {
                        webViewRef?.clearMatches()
                        viewModel.setFindInPage(false)
                    }
                )
            }

            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                // Internal Screens
                when (activeInternalScreen) {
                    InternalScreen.Flags -> {
                        FlagsScreen(
                            flags = flags,
                            onToggleFlag = { id, en -> viewModel.toggleFlag(id, en) },
                            onResetFlags = { viewModel.resetFlags() },
                            onClose = { viewModel.closeInternalScreen() }
                        )
                    }
                    InternalScreen.Extensions -> {
                        ExtensionsScreen(
                            extensions = extensions,
                            onToggleExtension = { viewModel.toggleExtension(it) },
                            onInstallExtension = { viewModel.installExtension(it) },
                            onDeleteExtension = { viewModel.deleteExtension(it) },
                            onOpenKubikStore = {
                                viewModel.closeInternalScreen()
                                viewModel.navigateTo("https://demomelon-git.github.io/kubik-web-store/")
                            },
                            onClose = { viewModel.closeInternalScreen() }
                        )
                    }
                    InternalScreen.Apps -> {
                        AppsScreen(
                            onOpenUrl = {
                                viewModel.closeInternalScreen()
                                viewModel.navigateTo(it)
                            },
                            onClose = { viewModel.closeInternalScreen() }
                        )
                    }
                    InternalScreen.DataKopi -> {
                        DataKopiScreen(
                            profile = profile,
                            savedSites = savedSites,
                            onUpdateProfile = { u, e, k -> viewModel.updateProfile(u, e, k) },
                            onClearSavedSites = { viewModel.clearSavedSites() },
                            onClose = { viewModel.closeInternalScreen() }
                        )
                    }
                    InternalScreen.Sync -> {
                        SyncScreen(
                            devices = devices,
                            isSyncing = isSyncing,
                            syncCode = viewModel.syncRepository.getSyncCode(),
                            lastSyncTime = viewModel.syncRepository.getLastSyncTime(),
                            onPerformSync = { viewModel.performSync() },
                            onPairNewDevice = { code, name -> viewModel.pairNewDevice(code, name) },
                            onRemoveDevice = { viewModel.removeDevice(it) },
                            onClose = { viewModel.closeInternalScreen() }
                        )
                    }
                    InternalScreen.History -> {
                        HistoryBookmarksScreen(
                            initialTab = 0,
                            history = history,
                            bookmarks = bookmarks,
                            onOpenUrl = {
                                viewModel.closeInternalScreen()
                                viewModel.navigateTo(it)
                            },
                            onDeleteBookmark = { viewModel.deleteBookmark(it) },
                            onClearHistory = { viewModel.clearHistory() },
                            onClose = { viewModel.closeInternalScreen() }
                        )
                    }
                    InternalScreen.Bookmarks -> {
                        HistoryBookmarksScreen(
                            initialTab = 1,
                            history = history,
                            bookmarks = bookmarks,
                            onOpenUrl = {
                                viewModel.closeInternalScreen()
                                viewModel.navigateTo(it)
                            },
                            onDeleteBookmark = { viewModel.deleteBookmark(it) },
                            onClearHistory = { viewModel.clearHistory() },
                            onClose = { viewModel.closeInternalScreen() }
                        )
                    }
                    InternalScreen.Settings -> {
                        SettingsScreen(
                            onOpenScreen = { viewModel.openInternalScreen(it) },
                            onClearAllData = {
                                viewModel.clearHistory()
                                viewModel.clearSavedSites()
                                webViewRef?.clearCache(true)
                            },
                            onClose = { viewModel.closeInternalScreen() }
                        )
                    }
                    InternalScreen.None -> {
                        if (isNewTab) {
                            NewTabPage(
                                profile = profile,
                                bookmarks = bookmarks,
                                onSearchSubmitted = { viewModel.navigateTo(it) },
                                onOpenUrl = { viewModel.navigateTo(it) },
                                onOpenScreen = { viewModel.openInternalScreen(it) }
                            )
                        } else {
                            // Android Chromium WebView
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )

                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            databaseEnabled = true
                                            setSupportZoom(true)
                                            builtInZoomControls = true
                                            displayZoomControls = false
                                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                            cacheMode = WebSettings.LOAD_DEFAULT
                                        }

                                        // Strict Data Kopi mode: configure CookieManager
                                        val cookieManager = CookieManager.getInstance()
                                        cookieManager.setAcceptCookie(true)
                                        cookieManager.setAcceptThirdPartyCookies(this, false)

                                        // Cubic Web Store and Data Kopi JavaScript Interface
                                        addJavascriptInterface(
                                            object {
                                                @JavascriptInterface
                                                fun installExtensionFromStore(json: String) {
                                                    try {
                                                        // Parse extension json from Kubik Web Store
                                                        val ext = KopiExtension(
                                                            id = "kubik_" + System.currentTimeMillis(),
                                                            name = "Kubik Store Addon",
                                                            version = "1.0",
                                                            description = "Установлено из Kubik Web Store",
                                                            author = "Kubik Developer",
                                                            isEnabled = true,
                                                            matches = "*",
                                                            scriptContent = json,
                                                            source = "Kubik Web Store"
                                                        )
                                                        coroutineScope.launch {
                                                            viewModel.installExtension(ext)
                                                            Toast.makeText(ctx, "Расширение из Kubik Store установлено!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }

                                                @JavascriptInterface
                                                fun getDataKopiUserId(): String = profile.userId

                                                @JavascriptInterface
                                                fun getDataKopiEmail(): String = profile.email

                                                @JavascriptInterface
                                                fun getDataKopiId(): String = profile.kopiId
                                            },
                                            "KopiBrowserBridge"
                                        )

                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                viewModel.setPageLoadingState(newProgress < 100, newProgress)
                                            }

                                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                                if (title != null && view?.url != null) {
                                                    viewModel.onPageFinished(
                                                        view.url ?: "",
                                                        title,
                                                        view.canGoBack(),
                                                        view.canGoForward()
                                                    )
                                                }
                                            }

                                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                                return super.onConsoleMessage(consoleMessage)
                                            }
                                        }

                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(
                                                view: WebView?,
                                                request: WebResourceRequest?
                                            ): Boolean {
                                                val uri = request?.url ?: return false
                                                val urlString = uri.toString()

                                                if (urlString.startsWith("kopi://")) {
                                                    viewModel.navigateTo(urlString)
                                                    return true
                                                }

                                                if (urlString.startsWith("tel:") || urlString.startsWith("mailto:")) {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                                        ctx.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                    return true
                                                }

                                                return false
                                            }

                                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                viewModel.setPageLoadingState(true, 10)
                                                // Inject Data Kopi on page start
                                                url?.let {
                                                    val dataScript = viewModel.repository.getDataKopiScript()
                                                    view?.evaluateJavascript(dataScript, null)
                                                }
                                            }

                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                val finishedUrl = url ?: ""
                                                viewModel.onPageFinished(
                                                    finishedUrl,
                                                    view?.title,
                                                    view?.canGoBack() ?: false,
                                                    view?.canGoForward() ?: false
                                                )

                                                // Inject Data Kopi + Cubic Web Store Extensions scripts
                                                coroutineScope.launch {
                                                    val dataScript = viewModel.repository.getDataKopiScript()
                                                    val combined = viewModel.extensionEngine.getCombinedScriptsForUrl(finishedUrl, dataScript)
                                                    view?.evaluateJavascript(combined, null)

                                                    // If Kubik Web Store, inject bridge helper
                                                    if (finishedUrl.contains("demomelon-git.github.io/kubik-web-store") ||
                                                        finishedUrl.contains("kubik-web-store") ||
                                                        finishedUrl.contains("demomelon-git.github.io/kubic-web-store") ||
                                                        finishedUrl.contains("kubic-web-store")) {
                                                        val bridgeScript = """
                                                            (function() {
                                                                window.isKopiBrowser = true;
                                                                window.installToKopi = function(script) {
                                                                    window.KopiBrowserBridge.installExtensionFromStore(script);
                                                                };
                                                                console.log('[Kopi Engine] Kubik Web Store Bridge successfully attached.');
                                                            })();
                                                        """.trimIndent()
                                                        view?.evaluateJavascript(bridgeScript, null)
                                                    }
                                                }
                                            }
                                        }

                                        setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                                            if (isDoneCounting) {
                                                viewModel.setFindMatchCount(numberOfMatches)
                                            }
                                        }

                                        if (currentUrl.isNotBlank() && !currentUrl.startsWith("kopi://")) {
                                            loadUrl(currentUrl)
                                        }

                                        webViewRef = this
                                    }
                                },
                                update = { wv ->
                                    webViewRef = wv
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // 3-Dots Chrome Menu Dropdown
        ChromeMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            isBookmarked = isBookmarked,
            desktopMode = desktopMode,
            onGoBack = { webViewRef?.goBack() },
            onGoForward = { webViewRef?.goForward() },
            onToggleBookmark = { viewModel.toggleBookmark() },
            onShare = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, urlInput)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Поделиться ссылкой"))
            },
            onNewTab = { viewModel.createNewTab("kopi://newtab") },
            onNewIncognitoTab = { viewModel.createNewTab("kopi://newtab", isIncognito = true) },
            onOpenScreen = { viewModel.openInternalScreen(it) },
            onToggleDesktopMode = { viewModel.toggleDesktopMode() },
            onFindInPage = { viewModel.setFindInPage(true) }
        )

        // Chrome Tab Switcher Modal Overlay
        AnimatedVisibility(visible = isTabSwitcherVisible) {
            ChromeTabSwitcher(
                tabs = tabs,
                currentTabId = currentTabId,
                onTabSelected = { viewModel.selectTab(it) },
                onCloseTab = { viewModel.closeTab(it) },
                onCloseAllTabs = { viewModel.closeAllTabs() },
                onNewTab = { viewModel.createNewTab("kopi://newtab") },
                onCloseSwitcher = { viewModel.toggleTabSwitcher() }
            )
        }
    }
}
