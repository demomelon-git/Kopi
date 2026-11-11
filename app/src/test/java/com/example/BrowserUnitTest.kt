package com.example

import com.example.data.model.DataKopiProfile
import com.example.data.repository.CrxParser
import com.example.data.repository.ExtensionDownloader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BrowserUnitTest {

    @Test
    fun `test domain extraction from http and https urls`() {
        val uri1 = URI("https://demomelon-git.github.io/kubik-web-store/")
        assertEquals("demomelon-git.github.io", uri1.host)

        val uri2 = URI("https://www.google.com/search?q=kopi")
        assertEquals("www.google.com", uri2.host)
    }

    @Test
    fun `test data kopi profile defaults`() {
        val profile = DataKopiProfile(
            userId = "usr_test123",
            email = "demoda228@gmail.com",
            kopiId = "KOPI-TEST-001",
            isCookieReplacementActive = true
        )

        assertEquals("usr_test123", profile.userId)
        assertEquals("demoda228@gmail.com", profile.email)
        assertEquals("KOPI-TEST-001", profile.kopiId)
        assertTrue(profile.isCookieReplacementActive)
    }

    @Test
    fun `test internal kopi schemes recognition`() {
        val flagsUrl = "kopi://flags"
        val extensionsUrl = "kopi://extensions"
        val appsUrl = "kopi://apps"
        val gsUrl = "kopi://gs"
        val dataKopiUrl = "kopi://datakopi"

        assertTrue(flagsUrl.startsWith("kopi://"))
        assertTrue(extensionsUrl.startsWith("kopi://"))
        assertTrue(appsUrl.startsWith("kopi://"))
        assertTrue(gsUrl.startsWith("kopi://"))
        assertTrue(dataKopiUrl.startsWith("kopi://"))
    }

    @Test
    fun `test crx parser extracts manifest, host permissions and content scripts`() {
        // Create an in-memory test CRX zip archive
        val baos = ByteArrayOutputStream()
        // Prefix with CRX header magic Cr24 + fake header bytes
        baos.write(byteArrayOf(0x43, 0x72, 0x32, 0x34, 0x02, 0x00, 0x00, 0x00))

        ZipOutputStream(baos).use { zos ->
            // Add manifest.json with Manifest V3 host_permissions
            val manifestJson = """
                {
                    "manifest_version": 3,
                    "name": "Кубик — главная и поиск",
                    "version": "2.3.0",
                    "description": "Главная страница и метапоиск с помощником Кубиком",
                    "author": "Demomelon",
                    "permissions": ["storage", "declarativeNetRequest"],
                    "host_permissions": [
                        "https://www.google.com/*",
                        "https://api.bing.com/*"
                    ],
                    "action": {
                        "default_title": "Поиск Кубик"
                    },
                    "content_scripts": [
                        {
                            "matches": ["https://www.google.com/search*"],
                            "js": ["content.js"]
                        }
                    ]
                }
            """.trimIndent()
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestJson.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // Add content.js
            val scriptContent = "console.log('Hello from Kubik CRX!');"
            zos.putNextEntry(ZipEntry("content.js"))
            zos.write(scriptContent.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }

        val crxBytes = baos.toByteArray()
        val parsed = CrxParser.parseCrx(crxBytes, "kubik-2.3.0.crx")

        assertEquals("Кубик — главная и поиск", parsed.name)
        assertEquals("2.3.0", parsed.version)
        assertEquals("Главная страница и метапоиск с помощником Кубиком", parsed.description)
        assertEquals("Поиск Кубик", parsed.actionTitle)
        assertTrue(parsed.permissions.contains("storage"))
        assertTrue(parsed.permissions.contains("declarativeNetRequest"))
        assertTrue(parsed.permissions.contains("https://www.google.com/*"))
        assertTrue(parsed.scriptContent.contains("Hello from Kubik CRX!"))
        assertTrue(parsed.hasActionButton)
    }

    @Test
    fun `test only extension packages are treated as extensions`() {
        assertTrue(ExtensionDownloader.isExtensionUrl("https://example.com/files/ext.crx"))
        assertTrue(ExtensionDownloader.isExtensionUrl("https://example.com/files/ext.kubik?v=2"))
        assertTrue(ExtensionDownloader.isExtensionUrl("https://demomelon-git.github.io/kubik-web-store/addons/adocho/dr-1.0.crx"))
        assertTrue(ExtensionDownloader.isExtensionUrl("https://example.com/download", "application/x-chrome-extension"))
        // обычные файлы раньше ошибочно уходили в установку расширений
        assertFalse(ExtensionDownloader.isExtensionUrl("https://example.com/archive.zip"))
        assertFalse(ExtensionDownloader.isExtensionUrl("https://example.com/report.pdf", "application/octet-stream"))
        assertFalse(ExtensionDownloader.isExtensionUrl("https://example.com/addons/adocho/readme.md"))
    }

    @Test
    fun `test crx3 header is stripped and localized name with comments in manifest is resolved`() {
        fun le(n: Int) = byteArrayOf(n.toByte(), (n shr 8).toByte(), (n shr 16).toByte(), (n shr 24).toByte())

        val baos = ByteArrayOutputStream()
        baos.write("Cr24".toByteArray(StandardCharsets.US_ASCII))
        baos.write(le(3))          // версия CRX3
        baos.write(le(5))          // размер заголовка
        baos.write(ByteArray(5))   // заголовок (protobuf)

        ZipOutputStream(baos).use { zos ->
            val manifest = """
                {
                    // Chrome разрешает комментарии в manifest.json
                    "manifest_version": 3,
                    "name": "__MSG_appName__",
                    "default_locale": "en",
                    "version": "1.2.3",
                    "content_scripts": [ { "matches": ["*://*.example.com/*"], "js": ["c.js"] } ]
                }
            """.trimIndent()
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifest.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("_locales/en/messages.json"))
            zos.write("""{ "appName": { "message": "My Extension" } }""".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("c.js"))
            zos.write("console.log('crx3');".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }

        val parsed = CrxParser.parseCrx(baos.toByteArray(), "ext.crx")
        assertEquals("My Extension", parsed.name)
        assertEquals("*://*.example.com/*", parsed.matches)
        assertTrue(parsed.scriptContent.contains("console.log('crx3');"))
    }
}
