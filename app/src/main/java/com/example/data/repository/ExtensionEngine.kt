package com.example.data.repository

import com.example.data.local.KopiDao
import com.example.data.model.KopiExtension

class ExtensionEngine(private val dao: KopiDao) {

    companion object {
        const val KUBIK_STORE_URL = "https://demomelon-git.github.io/kubik-web-store/"

        val DEFAULT_EXTENSIONS = listOf(
            KopiExtension(
                id = "kd_kubik_search",
                name = "Кубик Веб-поиск (.KD)",
                version = "2.3.0",
                description = "Главная страница и метапоиск с помощником Кубиком в формате KD (kubik://new_tab и 127.0.0.1).",
                author = "Demomelon",
                isEnabled = true,
                matches = "*",
                scriptContent = """
                    (function() {
                        if (typeof set_new_tab === 'function') {
                            set_new_tab('kubik://new_tab');
                        }
                        console.log('[Kopi KD] Кубик Веб-поиск активен с поддержкой kubik://new_tab');
                    })();
                """.trimIndent(),
                source = "Формат KD",
                permissions = "activeTab, storage, https://127.0.0.1/*, https://www.google.com/*",
                hasActionButton = true,
                actionTitle = "Кубик Поиск",
                newTabOverrideUrl = "kubik://new_tab",
                customSchemeUrl = "kubik://new_tab",
                customHtml = """
                    <!DOCTYPE html>
                    <html lang="ru">
                    <head>
                        <meta charset="utf-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <title>Кубик — Главная страница</title>
                        <style>
                            body { font-family: -apple-system, Roboto, sans-serif; background: #0F172A; color: #F8FAFC; margin: 0; padding: 32px 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 80vh; text-align: center; }
                            .logo { font-size: 42px; font-weight: 900; letter-spacing: -1px; margin-bottom: 8px; }
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
                        <div class="sub">Метапоиск и главная страница KD расширения (kubik://new_tab)</div>
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
            ),
            KopiExtension(
                id = "kopi_adshield",
                name = "Kopi AdShield Pro (.KD)",
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
                source = "Формат KD",
                permissions = "activeTab, storage, declarativeNetRequest",
                hasActionButton = true,
                actionTitle = "AdShield Status"
            ),
            KopiExtension(
                id = "kopi_dark_reader",
                name = "Dark Reader for Kopi (.KD)",
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
                source = "Формат KD",
                permissions = "activeTab, <all_urls>",
                hasActionButton = true,
                actionTitle = "Dark Theme Toggle"
            )
        )

        val KD_CUSTOM_COMMANDS_INJECTION = """
            (function() {
                window.kopi = window.kopi || {};

                // Custom KD command: set_new_tab(url)
                // Allows KD extensions to configure the browser's new tab page (e.g. 'kubik://new_tab', 'https://127.0.0.1/', etc.)
                window.kopi.set_new_tab = function(url) {
                    console.log('[Kopi KD Engine] set_new_tab called with url:', url);
                    if (window.KopiBrowserBridge && window.KopiBrowserBridge.setNewTab) {
                        window.KopiBrowserBridge.setNewTab(url);
                    }
                };
                window.set_new_tab = window.kopi.set_new_tab;

                // Custom KD command: register_site(schemeUrl, htmlContent)
                // Allows KD extensions to create custom websites like kubik://new_tab
                window.kopi.register_site = function(schemeUrl, htmlContent) {
                    console.log('[Kopi KD Engine] register_site called for:', schemeUrl);
                    if (window.KopiBrowserBridge && window.KopiBrowserBridge.registerCustomScheme) {
                        window.KopiBrowserBridge.registerCustomScheme(schemeUrl, htmlContent || '');
                    }
                };
                window.register_site = window.kopi.register_site;

                // Custom KD command: get_data_kopi()
                window.kopi.get_data_kopi = function() {
                    if (window.KopiBrowserBridge) {
                        return {
                            userId: window.KopiBrowserBridge.getDataKopiUserId(),
                            email: window.KopiBrowserBridge.getDataKopiEmail(),
                            kopiId: window.KopiBrowserBridge.getDataKopiId()
                        };
                    }
                    return {};
                };

                // Custom KD command: request_permission(domain)
                window.kopi.request_permission = function(domain) {
                    if (window.KopiBrowserBridge && window.KopiBrowserBridge.requestPermission) {
                        window.KopiBrowserBridge.requestPermission(domain);
                    }
                };
            })();
        """.trimIndent()

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
                            btn.textContent = '🚀 Установить .KD в Kopi';
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

        // 1. Data Kopi identity replacement runtime (strictly blocks cookies)
        builder.append(dataKopiScript).append("\n\n")

        // 2. Custom KD Commands runtime (set_new_tab, register_site, etc.)
        builder.append(KD_CUSTOM_COMMANDS_INJECTION).append("\n\n")

        // 3. Chrome Extension API Polyfill
        builder.append(CHROME_API_POLYFILL).append("\n\n")

        // 4. KD Extensions
        for (ext in enabledExtensions) {
            if (isUrlMatching(url, ext.matches)) {
                builder.append("/* KD Extension: ").append(ext.name).append(" */\n")
                builder.append(ext.scriptContent).append("\n\n")
            }
        }

        // 5. Kubik Store specific bridge if visiting the store
        if (url.contains("demomelon-git.github.io/kubik-web-store", ignoreCase = true) ||
            url.contains("kubik-web-store", ignoreCase = true) ||
            url.contains("demomelon-git.github.io/kubic-web-store", ignoreCase = true)
        ) {
            builder.append(KUBIK_STORE_INJECTION).append("\n\n")
        }

        return builder.toString()
    }
}
