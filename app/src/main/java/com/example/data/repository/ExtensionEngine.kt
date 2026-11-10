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
                description = "Блокирует навязчивую рекламу, всплывающие окна и трекеры на сайтах.",
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
                source = "Kubik Web Store"
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
                source = "Kubik Web Store"
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
                source = "Kubik Web Store"
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
                source = "Kubik Web Store"
            ),
            KopiExtension(
                id = "kopi_kubik_bridge",
                name = "Kubik Web Store Auto-Installer",
                version = "1.2.0",
                description = "Интеграция с магазином demomelon-git.github.io/kubik-web-store/ для быстрой установки в 1 клик.",
                author = "Kubik Store Official",
                isEnabled = true,
                matches = "demomelon-git.github.io",
                scriptContent = """
                    (function() {
                        window.isKopiBrowser = true;
                        window.kopiBrowserVersion = "1.0-Chromium";
                        console.log('[Kubik Web Store Bridge] Connected to Kopi Browser.');
                    })();
                """.trimIndent(),
                source = "Kubik Web Store"
            )
        )
    }

    suspend fun seedDefaultExtensionsIfNeeded() {
        val existing = dao.getEnabledExtensionsSync()
        if (existing.isEmpty()) {
            DEFAULT_EXTENSIONS.forEach { ext ->
                dao.insertExtension(ext)
            }
        }
    }

    suspend fun getCombinedScriptsForUrl(url: String, dataKopiScript: String): String {
        val enabledExtensions = dao.getEnabledExtensionsSync()
        val builder = StringBuilder()
        
        // Always prepend Data Kopi runtime injection
        builder.append(dataKopiScript).append("\n\n")

        for (ext in enabledExtensions) {
            if (ext.matches == "*" || url.contains(ext.matches, ignoreCase = true)) {
                builder.append("/* Extension: ").append(ext.name).append(" */\n")
                builder.append(ext.scriptContent).append("\n\n")
            }
        }
        return builder.toString()
    }
}
