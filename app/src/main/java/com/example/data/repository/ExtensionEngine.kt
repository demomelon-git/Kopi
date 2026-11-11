package com.example.data.repository

import com.example.data.local.KopiDao
import com.example.data.model.KopiExtension

class ExtensionEngine(private val dao: KopiDao) {

    companion object {
        const val KUBIK_STORE_URL = "https://demomelon-git.github.io/kubik-web-store/"

        val DEFAULT_EXTENSIONS = listOf(
            KopiExtension(
                id = "kopi_adshield",
                name = "Kopi AdShield Pro",
                version = "2.4.0",
                description = "Блокирует навязчивую рекламу, баннеры и трекеры на сайтах.",
                author = "Kubik Web Store / Kopi Team",
                isEnabled = true,
                matches = "*",
                scriptContent = """
                    (function() {
                        const adSelectors = [
                            'iframe[src*="doubleclick"]', 'iframe[src*="googlesyndication"]',
                            '.ad-container', '.advertisement', '.adsbygoogle', '.banner-ads',
                            'div[id*="ad-"]', 'div[class*="banner_ad"]', '#google_ads'
                        ];
                        function removeAds() {
                            adSelectors.forEach(sel => {
                                document.querySelectorAll(sel).forEach(el => el.remove());
                            });
                        }
                        removeAds();
                        setInterval(removeAds, 1500);
                        console.log('[Kopi AdShield] Active & shielding page.');
                    })();
                """.trimIndent(),
                source = "Kubik Web Store",
                permissions = "activeTab, storage, declarativeNetRequest",
                hasActionButton = true,
                actionTitle = "AdShield Status"
            ),
            KopiExtension(
                id = "kopi_dark_reader",
                name = "Dark Reader for Kopi",
                version = "1.8.2",
                description = "Интеллектуальная темная тема для всех страниц в сети.",
                author = "Alexander S. (Kubik Store)",
                isEnabled = false,
                matches = "*",
                scriptContent = """
                    (function() {
                        if (document.getElementById('kopi-dark-style')) return;
                        const style = document.createElement('style');
                        style.id = 'kopi-dark-style';
                        style.innerHTML = `
                            html {
                                filter: invert(90%) hue-rotate(180deg) !important;
                                background-color: #121212 !important;
                            }
                            img, video, canvas, svg, iframe {
                                filter: invert(100%) hue-rotate(180deg) !important;
                            }
                        `;
                        (document.head || document.documentElement).appendChild(style);
                        console.log('[Dark Reader for Kopi] Injected dark styles.');
                    })();
                """.trimIndent(),
                source = "Kubik Web Store",
                permissions = "activeTab, <all_urls>",
                hasActionButton = true,
                actionTitle = "Dark Theme Toggle"
            ),
            KopiExtension(
                id = "kopi_data_guard",
                name = "Data Kopi Cookie-Replacement Guard",
                version = "1.0.0",
                description = "Заменяет обычные cookie безопасным контейнером Дата Копи (User ID, Email, Kopi ID).",
                author = "Kopi Core Labs",
                isEnabled = true,
                matches = "*",
                scriptContent = """
                    (function() {
                        window.isDataKopiProtected = true;
                        console.log('[Data Kopi Guard] Cookie tracking replaced with Data Kopi container.');
                    })();
                """.trimIndent(),
                source = "Kubik Web Store",
                permissions = "storage, cookies",
                hasActionButton = true,
                actionTitle = "Data Kopi Status"
            ),
            KopiExtension(
                id = "kopi_youtube_enhancer",
                name = "YouTube Clean & Cinema Mode",
                version = "3.1.5",
                description = "Автоматически скрывает баннеры и улучшает плеер YouTube.",
                author = "MediaCraft (Kubik Store)",
                isEnabled = true,
                matches = "youtube.com",
                scriptContent = """
                    (function() {
                        if (!window.location.hostname.includes('youtube.com')) return;
                        function cleanYT() {
                            const ads = document.querySelectorAll('.ytp-ad-overlay-container, .ytp-ad-skip-button-container, ytd-banner-promo-renderer, #masthead-ad');
                            ads.forEach(a => a.remove());
                            const skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern');
                            if (skipBtn) skipBtn.click();
                        }
                        setInterval(cleanYT, 1000);
                        console.log('[YouTube Clean Mode] Active.');
                    })();
                """.trimIndent(),
                source = "Kubik Web Store",
                permissions = "https://www.youtube.com/*, activeTab",
                hasActionButton = true,
                actionTitle = "YouTube Clean"
            )
        )

        val CHROME_API_POLYFILL = """
            (function() {
                if (window._kopiExtensionPolyfillInjected) return;
                window._kopiExtensionPolyfillInjected = true;

                window.chrome = window.chrome || {};
                window.browser = window.browser || window.chrome;

                const _store = {};
                try {
                    const saved = localStorage.getItem('__kopi_ext_storage__');
                    if (saved) Object.assign(_store, JSON.parse(saved));
                } catch(e) {}

                const _save = function() {
                    try { localStorage.setItem('__kopi_ext_storage__', JSON.stringify(_store)); } catch(e) {}
                };

                const makeStorage = function() {
                    return {
                        get: function(keys, callback) {
                            return new Promise(function(resolve) {
                                let res = {};
                                if (typeof keys === 'string') res[keys] = _store[keys];
                                else if (Array.isArray(keys)) keys.forEach(function(k) { res[k] = _store[k]; });
                                else if (typeof keys === 'object' && keys !== null) {
                                    Object.keys(keys).forEach(function(k) {
                                        res[k] = _store[k] !== undefined ? _store[k] : keys[k];
                                    });
                                } else {
                                    res = Object.assign({}, _store);
                                }
                                if (typeof callback === 'function') callback(res);
                                resolve(res);
                            });
                        },
                        set: function(items, callback) {
                            return new Promise(function(resolve) {
                                Object.assign(_store, items);
                                _save();
                                if (typeof callback === 'function') callback();
                                resolve();
                            });
                        },
                        remove: function(keys, callback) {
                            return new Promise(function(resolve) {
                                if (Array.isArray(keys)) keys.forEach(function(k) { delete _store[k]; });
                                else delete _store[keys];
                                _save();
                                if (typeof callback === 'function') callback();
                                resolve();
                            });
                        },
                        clear: function(callback) {
                            return new Promise(function(resolve) {
                                Object.keys(_store).forEach(function(k) { delete _store[k]; });
                                _save();
                                if (typeof callback === 'function') callback();
                                resolve();
                            });
                        }
                    };
                };

                window.chrome.storage = {
                    local: makeStorage(),
                    sync: makeStorage(),
                    session: makeStorage(),
                    managed: makeStorage()
                };

                window.chrome.runtime = window.chrome.runtime || {
                    id: "dommdbdphfnlgamocgilpbigigkpanil",
                    getURL: function(path) { return path; },
                    sendMessage: function(msg, responseCallback) {
                        return new Promise(function(resolve) {
                            if (typeof responseCallback === 'function') responseCallback({ ok: true });
                            resolve({ ok: true });
                        });
                    },
                    onMessage: {
                        addListener: function(fn) {},
                        removeListener: function(fn) {},
                        hasListener: function() { return false; }
                    },
                    onInstalled: { addListener: function(fn) {} }
                };

                window.chrome.declarativeNetRequest = window.chrome.declarativeNetRequest || {
                    updateDynamicRules: function(options, cb) {
                        return new Promise(function(resolve) {
                            if (typeof cb === 'function') cb();
                            resolve();
                        });
                    }
                };

                window.chrome.tabs = window.chrome.tabs || {
                    query: function(q, cb) {
                        return new Promise(function(resolve) {
                            const tabs = [{ id: 1, active: true, url: window.location.href, title: document.title }];
                            if (typeof cb === 'function') cb(tabs);
                            resolve(tabs);
                        });
                    },
                    sendMessage: function(tabId, msg, cb) {
                        return new Promise(function(resolve) {
                            if (typeof cb === 'function') cb({ success: true });
                            resolve({ success: true });
                        });
                    }
                };

                window.chrome.action = window.chrome.action || {
                    setTitle: function() {},
                    setBadgeText: function() {},
                    setBadgeBackgroundColor: function() {}
                };
                window.chrome.browserAction = window.chrome.action;
                window.browser = window.chrome;
            })();
        """.trimIndent()

        val KUBIK_STORE_INJECTION = """
            (function() {
                window.isKopiBrowser = true;
                window.kopiBrowserVersion = "1.0-Chromium";

                // Ensure Chrome tab is selected on Kubik Web Store
                function selectChromeTab() {
                    const tabs = document.querySelectorAll('.tab');
                    tabs.forEach(t => {
                        if (t.dataset.b === 'chrome' && !t.classList.contains('on')) {
                            t.click();
                        }
                    });
                }
                setTimeout(selectChromeTab, 300);

                // Add "Установить в Kopi" action buttons to each addon card
                function injectKopiInstallButtons() {
                    const cards = document.querySelectorAll('.card');
                    cards.forEach(card => {
                        if (card.querySelector('.kopi-install-btn')) return;
                        const dlBtn = card.querySelector('a[download]');
                        if (dlBtn && dlBtn.href) {
                            const extUrl = dlBtn.href;
                            const titleEl = card.querySelector('h2');
                            const addonName = titleEl ? titleEl.textContent.trim() : "Kubik Addon";

                            const btn = document.createElement('button');
                            btn.className = 'btn kopi-install-btn';
                            btn.style.backgroundColor = '#6200EE';
                            btn.style.color = '#FFFFFF';
                            btn.style.marginLeft = '8px';
                            btn.style.fontWeight = 'bold';
                            btn.style.cursor = 'pointer';
                            btn.textContent = '🚀 Установить в Kopi';
                            btn.onclick = function(e) {
                                e.preventDefault();
                                e.stopPropagation();
                                if (window.KopiBrowserBridge && window.KopiBrowserBridge.installExtensionFromUrl) {
                                    window.KopiBrowserBridge.installExtensionFromUrl(extUrl, addonName);
                                } else {
                                    location.href = extUrl;
                                }
                            };
                            dlBtn.parentNode.insertBefore(btn, dlBtn.nextSibling);
                        }
                    });
                }

                setInterval(injectKopiInstallButtons, 600);
            })();
        """.trimIndent()
    }

    suspend fun seedDefaultExtensionsIfNeeded() {
        val existing = dao.getEnabledExtensionsSync()
        if (existing.isEmpty()) {
            DEFAULT_EXTENSIONS.forEach { ext ->
                dao.insertExtension(ext)
            }
        }
    }

    fun isUrlMatching(url: String, patternString: String): Boolean {
        if (patternString.isBlank() || patternString == "*" || patternString.contains("<all_urls>")) {
            return true
        }
        val patterns = patternString.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
        for (pat in patterns) {
            if (pat == "*" || pat == "<all_urls>") return true

            val cleanPat = pat
                .removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("*://")
                .removeSuffix("/*")
                .removeSuffix("*")
                .trim()

            if (cleanPat.isBlank()) return true

            val cleanUrl = url
                .removePrefix("http://")
                .removePrefix("https://")

            if (cleanUrl.contains(cleanPat, ignoreCase = true) || url.contains(cleanPat, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    suspend fun getCombinedScriptsForUrl(url: String, dataKopiScript: String): String {
        val enabledExtensions = dao.getEnabledExtensionsSync()
        val builder = StringBuilder()

        // 1. Data Kopi identity replacement runtime
        builder.append(dataKopiScript).append("\n\n")

        // 2. Chrome Extension API Polyfill
        builder.append(CHROME_API_POLYFILL).append("\n\n")

        // 3. User & Store Extensions
        for (ext in enabledExtensions) {
            if (isUrlMatching(url, ext.matches)) {
                builder.append("/* Extension: ").append(ext.name).append(" */\n")
                builder.append(ext.scriptContent).append("\n\n")
            }
        }

        // 4. Kubik Store specific bridge if visiting the store
        if (url.contains("demomelon-git.github.io/kubik-web-store", ignoreCase = true) ||
            url.contains("kubik-web-store", ignoreCase = true) ||
            url.contains("demomelon-git.github.io/kubic-web-store", ignoreCase = true)
        ) {
            builder.append(KUBIK_STORE_INJECTION).append("\n\n")
        }

        return builder.toString()
    }
}
