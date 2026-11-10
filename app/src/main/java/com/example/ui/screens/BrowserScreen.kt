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
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.KopiExtension
import com.example.ui.BrowserViewModel
import com.example.ui.InternalScreen
import com.example.ui.components.ChromeMenu
import com.example.ui.components.ChromeOmnibox
import com.example.ui.components.ChromeTabSwitcher
import com.example.ui.components.FindInPageBar
import com.example.ui.theme.ChromeBlue
import com.example.ui.theme.KopiCyan
import com.example.ui.theme.KopiGreen
import com.example.ui.theme.KopiPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
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

    val isExtensionsMenuVisible by viewModel.isExtensionsMenuVisible.collectAsStateWithLifecycle()
    val showExtensionPermissions by viewModel.showExtensionPermissions.collectAsStateWithLifecycle()

    var isMenuExpanded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // CRX File Picker launcher
    val crxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                    if (bytes != null) {
                        val fileName = uri.lastPathSegment ?: "extension.crx"
                        val result = viewModel.installCrx(bytes, fileName)
                        if (result.isSuccess) {
                            Toast.makeText(context, "CRX расширение '${result.getOrNull()?.name}' установлено!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Ошибка разбора CRX: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Не удалось открыть CRX: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val currentTab = remember(tabs, currentTabId) {
        tabs.find { it.id == currentTabId }
    }
    val currentUrl = currentTab?.url ?: "kopi://newtab"
    val isNewTab = currentUrl == "kopi://newtab" && activeInternalScreen == InternalScreen.None

    // Back button handling
    BackHandler(enabled = true) {
        when {
            isExtensionsMenuVisible -> viewModel.toggleExtensionsMenu()
            showExtensionPermissions != null -> viewModel.closeExtensionPermissions()
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
                settings.userAgentString = null
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
            // Chrome Omnibox with puzzle extension button
            ChromeOmnibox(
                url = urlInput,
                isLoading = isLoading,
                progress = pageProgress,
                tabCount = tabs.size,
                onUrlSubmitted = { viewModel.navigateTo(it) },
                onRefreshClicked = { webViewRef?.reload() },
                onStopClicked = { webViewRef?.stopLoading() },
                onTabSwitcherClicked = { viewModel.toggleTabSwitcher() },
                onExtensionsClicked = { viewModel.toggleExtensionsMenu() },
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
                            onInstallCrxBytes = { bytes, fileName ->
                                coroutineScope.launch {
                                    val result = viewModel.installCrx(bytes, fileName)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "CRX расширение '${result.getOrNull()?.name}' установлено!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Ошибка разбора CRX: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onDeleteExtension = { viewModel.deleteExtension(it) },
                            onOpenKubikStore = {
                                viewModel.closeInternalScreen()
                                viewModel.navigateTo("https://demomelon-git.github.io/kubik-web-store/")
                            },
                            onTriggerAction = { ext ->
                                webViewRef?.evaluateJavascript(ext.scriptContent, null)
                                Toast.makeText(context, "Действие расширения '${ext.name}' активировано", Toast.LENGTH_SHORT).show()
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

                                        // Download listener for .crx files
                                        setDownloadListener { downloadUrl, _, _, mimetype, _ ->
                                            if (downloadUrl.endsWith(".crx", ignoreCase = true) || mimetype.contains("chrome-extension")) {
                                                Toast.makeText(ctx, "Загрузка CRX пакета...", Toast.LENGTH_SHORT).show()
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    try {
                                                        val connection = java.net.URL(downloadUrl).openConnection()
                                                        val bytes = connection.getInputStream().use { it.readBytes() }
                                                        val fileName = downloadUrl.substringAfterLast("/")
                                                        val res = viewModel.installCrx(bytes, fileName)
                                                        withContext(Dispatchers.Main) {
                                                            if (res.isSuccess) {
                                                                Toast.makeText(ctx, "CRX расширение '${res.getOrNull()?.name}' установлено!", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(ctx, "Ошибка установки CRX: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(ctx, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Kubik Web Store and Data Kopi JavaScript Interface
                                        addJavascriptInterface(
                                            object {
                                                @JavascriptInterface
                                                fun installExtensionFromStore(json: String) {
                                                    try {
                                                        val ext = KopiExtension(
                                                            id = "kubik_" + System.currentTimeMillis(),
                                                            name = "Kubik Store Addon",
                                                            version = "1.0",
                                                            description = "Установлено из Kubik Web Store",
                                                            author = "Kubik Developer",
                                                            isEnabled = true,
                                                            matches = "*",
                                                            scriptContent = json,
                                                            source = "Kubik Web Store",
                                                            permissions = "activeTab, storage",
                                                            hasActionButton = true,
                                                            actionTitle = "Kubik Store Addon"
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

                                                coroutineScope.launch {
                                                    val dataScript = viewModel.repository.getDataKopiScript()
                                                    val combined = viewModel.extensionEngine.getCombinedScriptsForUrl(finishedUrl, dataScript)
                                                    view?.evaluateJavascript(combined, null)

                                                    if (finishedUrl.contains("demomelon-git.github.io/kubik-web-store") ||
                                                        finishedUrl.contains("kubik-web-store") ||
                                                        finishedUrl.contains("demomelon-git.github.io/kubic-web-store") ||
                                                        finishedUrl.contains("kubic-web-store")
                                                    ) {
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

        // Chrome Menu Dropdown
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

        // Extensions Quick Action Bottom Sheet (Puzzle button in toolbar)
        if (isExtensionsMenuVisible) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleExtensionsMenu() },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KopiPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = KopiPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Расширения и разрешения",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Быстрый запуск действий и проверка доступа",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.toggleExtensionsMenu() }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    val enabledExts = extensions.filter { it.isEnabled }
                    if (enabledExts.isEmpty()) {
                        Text(
                            text = "Нет активных расширений. Вы можете установить .CRX файл или добавить из Kubik Web Store.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(enabledExts, key = { it.id }) { ext ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = ext.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(ChromeBlue.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = ext.source,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ChromeBlue
                                                    )
                                                }
                                            }
                                            Text(
                                                text = ext.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Кнопка действия расширения
                                        Button(
                                            onClick = {
                                                webViewRef?.evaluateJavascript(ext.scriptContent, null)
                                                Toast.makeText(context, "Действие '${ext.name}' запущено!", Toast.LENGTH_SHORT).show()
                                                viewModel.toggleExtensionsMenu()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ChromeBlue),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Запуск", fontSize = 11.sp)
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Кнопка разрешения
                                        OutlinedButton(
                                            onClick = { viewModel.openExtensionPermissions(ext) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp), tint = KopiGreen)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.toggleExtensionsMenu()
                                crxPickerLauncher.launch("*/*")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Файл .CRX", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.toggleExtensionsMenu()
                                viewModel.openInternalScreen(InternalScreen.Extensions)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KopiPurple)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Управление", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Permissions Dialog for selected extension
        showExtensionPermissions?.let { ext ->
            ExtensionPermissionsDialog(
                extension = ext,
                onDismiss = { viewModel.closeExtensionPermissions() }
            )
        }
    }
}
