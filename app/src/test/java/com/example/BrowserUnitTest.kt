package com.example

import com.example.data.model.DataKopiProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

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
}
