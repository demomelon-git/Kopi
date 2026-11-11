package com.example.data.repository

import com.example.data.model.KopiExtension
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object KdParser {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val mapAdapter = moshi.adapter<Map<String, Any>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    fun parseKd(bytes: ByteArray, defaultName: String = "extension.kd"): KopiExtension {
        val fileEntries = mutableMapOf<String, ByteArray>()
        var isZip = false

        // Check for ZIP magic signature 0x50, 0x4B, 0x03, 0x04
        var zipOffset = -1
        for (i in 0 until (bytes.size - 4)) {
            if (bytes[i] == 0x50.toByte() && bytes[i + 1] == 0x4B.toByte() &&
                bytes[i + 2] == 0x03.toByte() && bytes[i + 3] == 0x04.toByte()
            ) {
                zipOffset = i
                break
            }
        }

        if (zipOffset >= 0) {
            isZip = true
            val zipBytes = if (zipOffset > 0) bytes.copyOfRange(zipOffset, bytes.size) else bytes
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val path = entry.name.removePrefix("/")
                        fileEntries[path] = zis.readBytes()
                    }
                    entry = zis.nextEntry
                }
            }
        }

        var jsonMap: Map<String, Any> = emptyMap()
        var customHtml = ""
        val scriptBuilder = StringBuilder()

        if (isZip && fileEntries.isNotEmpty()) {
            // Find config: kd.json, manifest.json, package.json
            val configBytes = fileEntries["kd.json"]
                ?: fileEntries.entries.find { it.key.endsWith("kd.json", ignoreCase = true) }?.value
                ?: fileEntries["manifest.json"]
                ?: fileEntries.entries.find { it.key.endsWith("manifest.json", ignoreCase = true) }?.value
                ?: fileEntries["package.json"]

            if (configBytes != null) {
                try {
                    val configStr = String(configBytes, StandardCharsets.UTF_8)
                    jsonMap = mapAdapter.fromJson(configStr) ?: emptyMap()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Extract HTML files (newtab.html, index.html, custom.html)
            val htmlEntry = fileEntries["newtab.html"]
                ?: fileEntries["index.html"]
                ?: fileEntries.entries.find { it.key.endsWith(".html", ignoreCase = true) }?.value
            if (htmlEntry != null) {
                customHtml = String(htmlEntry, StandardCharsets.UTF_8)
            }

            // Extract JS files
            for ((path, data) in fileEntries) {
                if (path.endsWith(".js", ignoreCase = true)) {
                    scriptBuilder.append("/* KD Script: $path */\n")
                    scriptBuilder.append(String(data, StandardCharsets.UTF_8))
                    scriptBuilder.append("\n\n")
                }
            }
        } else {
            // Text or JSON .kd package
            try {
                val text = String(bytes, StandardCharsets.UTF_8).trim()
                if (text.startsWith("{")) {
                    jsonMap = mapAdapter.fromJson(text) ?: emptyMap()
                    customHtml = jsonMap["html"] as? String ?: ""
                    val jsContent = jsonMap["js"] as? String ?: ""
                    if (jsContent.isNotBlank()) {
                        scriptBuilder.append(jsContent)
                    }
                } else {
                    // Plain JS or HTML in .kd
                    if (text.contains("<html") || text.contains("<body") || text.contains("<!DOCTYPE")) {
                        customHtml = text
                        scriptBuilder.append("// KD HTML with embedded scripts loaded")
                    } else {
                        scriptBuilder.append(text)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val name = jsonMap["name"] as? String
            ?: jsonMap["title"] as? String
            ?: defaultName.removeSuffix(".kd").removeSuffix(".crx").removeSuffix(".zip")
        val version = jsonMap["version"] as? String ?: "1.0.0"
        val description = jsonMap["description"] as? String
            ?: jsonMap["short"] as? String
            ?: "Расширение формата KD (Kopi Developer)"
        val author = jsonMap["author"] as? String ?: "KD Developer"

        // New tab override command from metadata or js
        var newTabUrl = jsonMap["new_tab"] as? String
            ?: jsonMap["newtab"] as? String
            ?: ""

        // If manifest has chrome_url_overrides.newtab
        val overrides = jsonMap["chrome_url_overrides"] as? Map<*, *>
        if (overrides?.get("newtab") != null) {
            val file = overrides["newtab"].toString()
            newTabUrl = "kubik://new_tab"
        }

        // Custom scheme (e.g. kubik://new_tab)
        var customScheme = jsonMap["custom_scheme"] as? String
            ?: jsonMap["scheme"] as? String
            ?: ""

        if (customScheme.isBlank() && (name.contains("кубик", ignoreCase = true) || name.contains("kubik", ignoreCase = true))) {
            customScheme = "kubik://new_tab"
            if (newTabUrl.isBlank()) {
                newTabUrl = "kubik://new_tab"
            }
        }

        // Parse permissions
        val permissionsList = mutableListOf<String>()
        val permsList = jsonMap["permissions"] as? List<*>
        permsList?.filterIsInstance<String>()?.let { permissionsList.addAll(it) }

        val hostPermsList = jsonMap["host_permissions"] as? List<*>
        hostPermsList?.filterIsInstance<String>()?.let { permissionsList.addAll(it) }

        // If newTabUrl contains 127.0.0.1 or domain, ensure permission exists
        if (newTabUrl.contains("127.0.0.1")) {
            permissionsList.add("https://127.0.0.1/*")
            permissionsList.add("http://127.0.0.1/*")
        }

        val permissionsString = permissionsList.distinct().filter { it.isNotBlank() }
            .joinToString(", ").ifBlank { "activeTab, storage" }

        if (scriptBuilder.isBlank()) {
            scriptBuilder.append("""
                console.log('[Kopi KD Engine] Расширение $name ($version) активно');
            """.trimIndent())
        }

        // Prepend custom set_new_tab command if specified in manifest
        if (newTabUrl.isNotBlank()) {
            val setCmd = "\nif (typeof set_new_tab === 'function') { set_new_tab('$newTabUrl'); }\n"
            scriptBuilder.insert(0, setCmd)
        }

        val extId = "kd_" + UUID.randomUUID().toString().take(8)

        return KopiExtension(
            id = extId,
            name = name,
            version = version,
            description = description,
            author = author,
            isEnabled = true,
            matches = "*",
            scriptContent = scriptBuilder.toString(),
            source = "Формат KD",
            permissions = permissionsString,
            hasActionButton = true,
            popupHtml = customHtml,
            actionTitle = name,
            newTabOverrideUrl = newTabUrl,
            customSchemeUrl = customScheme,
            customHtml = customHtml
        )
    }
}
