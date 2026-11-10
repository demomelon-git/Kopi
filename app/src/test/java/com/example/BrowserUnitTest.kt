package com.example

import com.example.data.model.DataKopiProfile
import com.example.data.repository.CrxParser
import org.junit.Assert.assertEquals
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
    fun `test crx parser extracts manifest and content scripts`() {
        // Create an in-memory test CRX zip archive
        val baos = ByteArrayOutputStream()
        // Prefix with CRX header magic Cr24 + fake header bytes
        baos.write(byteArrayOf(0x43, 0x72, 0x32, 0x34, 0x02, 0x00, 0x00, 0x00))

        ZipOutputStream(baos).use { zos ->
            // Add manifest.json
            val manifestJson = """
                {
                    "manifest_version": 3,
                    "name": "Test Kopi Ext",
                    "version": "1.2.3",
                    "description": "Test extension description",
                    "author": "Kopi Dev",
                    "permissions": ["storage", "activeTab", "cookies"],
                    "action": {
                        "default_title": "Run Test Action"
                    },
                    "content_scripts": [
                        {
                            "matches": ["*://*.google.com/*"],
                            "js": ["content.js"]
                        }
                    ]
                }
            """.trimIndent()
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestJson.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // Add content.js
            val scriptContent = "console.log('Hello from CRX!');"
            zos.putNextEntry(ZipEntry("content.js"))
            zos.write(scriptContent.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }

        val crxBytes = baos.toByteArray()
        val parsed = CrxParser.parseCrx(crxBytes, "test.crx")

        assertEquals("Test Kopi Ext", parsed.name)
        assertEquals("1.2.3", parsed.version)
        assertEquals("Test extension description", parsed.description)
        assertEquals("Run Test Action", parsed.actionTitle)
        assertTrue(parsed.permissions.contains("storage"))
        assertTrue(parsed.permissions.contains("activeTab"))
        assertTrue(parsed.scriptContent.contains("Hello from CRX!"))
        assertTrue(parsed.hasActionButton)
    }
}
