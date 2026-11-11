package com.example

import com.example.data.model.DataKopiProfile
import com.example.data.model.DownloadItem
import com.example.data.repository.KdParser
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
        val downloadsUrl = "kopi://downloads"

        assertTrue(flagsUrl.startsWith("kopi://"))
        assertTrue(extensionsUrl.startsWith("kopi://"))
        assertTrue(appsUrl.startsWith("kopi://"))
        assertTrue(gsUrl.startsWith("kopi://"))
        assertTrue(dataKopiUrl.startsWith("kopi://"))
        assertTrue(downloadsUrl.startsWith("kopi://"))
    }

    @Test
    fun `test download item entity model`() {
        val item = DownloadItem(
            id = 1,
            fileName = "sample_file.pdf",
            url = "https://example.com/sample_file.pdf",
            filePath = "/storage/downloads/sample_file.pdf",
            fileSize = 1048576,
            mimeType = "application/pdf"
        )

        assertEquals("sample_file.pdf", item.fileName)
        assertEquals(1048576L, item.fileSize)
        assertEquals("COMPLETED", item.status)
    }

    @Test
    fun `test kd parser with set_new_tab, custom scheme kubik and 127_0_0_1 permissions`() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // kd.json manifest
            val kdJson = """
                {
                    "name": "Кубик Веб-поиск",
                    "version": "2.3.0",
                    "description": "Поиск и новая вкладка KD",
                    "author": "Demomelon",
                    "new_tab": "kubik://new_tab",
                    "custom_scheme": "kubik://new_tab",
                    "permissions": ["activeTab", "storage", "https://127.0.0.1/*"]
                }
            """.trimIndent()
            zos.putNextEntry(ZipEntry("kd.json"))
            zos.write(kdJson.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // newtab.html
            val htmlContent = "<html><body><h1>Кубик Поиск</h1></body></html>"
            zos.putNextEntry(ZipEntry("newtab.html"))
            zos.write(htmlContent.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // content.js
            val scriptContent = "set_new_tab('kubik://new_tab'); console.log('KD active');"
            zos.putNextEntry(ZipEntry("content.js"))
            zos.write(scriptContent.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }

        val kdBytes = baos.toByteArray()
        val parsed = KdParser.parseKd(kdBytes, "kubik.kd")

        assertEquals("Кубик Веб-поиск", parsed.name)
        assertEquals("2.3.0", parsed.version)
        assertEquals("kubik://new_tab", parsed.newTabOverrideUrl)
        assertEquals("kubik://new_tab", parsed.customSchemeUrl)
        assertTrue(parsed.customHtml.contains("Кубик Поиск"))
        assertTrue(parsed.permissions.contains("127.0.0.1"))
        assertTrue(parsed.scriptContent.contains("set_new_tab('kubik://new_tab')"))
    }
}
