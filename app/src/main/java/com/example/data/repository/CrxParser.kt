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

object CrxParser {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val mapAdapter = moshi.adapter<Map<String, Any>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    fun parseCrx(bytes: ByteArray, defaultName: String = "CRX Extension"): KopiExtension {
        var zipOffset = 0
        for (i in 0 until (bytes.size - 4)) {
            if (bytes[i] == 0x50.toByte() && bytes[i + 1] == 0x4B.toByte() &&
                bytes[i + 2] == 0x03.toByte() && bytes[i + 3] == 0x04.toByte()
            ) {
                zipOffset = i
                break
            }
        }

        val zipBytes = if (zipOffset > 0) bytes.copyOfRange(zipOffset, bytes.size) else bytes
        val fileEntries = mutableMapOf<String, ByteArray>()

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

        // Locate manifest.json
        val manifestBytes = fileEntries["manifest.json"]
            ?: fileEntries.entries.find { it.key.endsWith("manifest.json", ignoreCase = true) }?.value
            ?: throw IllegalArgumentException("Файл manifest.json не найден внутри CRX пакета")

        val manifestStr = String(manifestBytes, StandardCharsets.UTF_8)
        val jsonMap = mapAdapter.fromJson(manifestStr) ?: emptyMap()

        val name = jsonMap["name"] as? String ?: defaultName.removeSuffix(".crx")
        val version = jsonMap["version"] as? String ?: "1.0.0"
        val description = jsonMap["description"] as? String ?: "Установлено из CRX пакета"
        val author = jsonMap["author"] as? String ?: "CRX Developer"

        // Parse permissions
        val permissionsList = mutableListOf<String>()
        val permsList = jsonMap["permissions"] as? List<*>
        permsList?.filterIsInstance<String>()?.let { permissionsList.addAll(it) }

        val optionalPermsList = jsonMap["optional_permissions"] as? List<*>
        optionalPermsList?.filterIsInstance<String>()?.let { permissionsList.addAll(it) }

        val permissionsString = permissionsList.joinToString(", ").ifBlank { "activeTab, storage" }

        // Action / Browser Action
        val actionObj = (jsonMap["action"] as? Map<*, *>) ?: (jsonMap["browser_action"] as? Map<*, *>)
        val actionTitle = (actionObj?.get("default_title") as? String) ?: name
        val defaultPopup = (actionObj?.get("default_popup") as? String) ?: ""
        var popupHtml = ""
        if (defaultPopup.isNotBlank() && fileEntries.containsKey(defaultPopup)) {
            popupHtml = String(fileEntries[defaultPopup] ?: byteArrayOf(), StandardCharsets.UTF_8)
        }

        // Matches and content scripts
        val matchesSet = mutableSetOf<String>()
        val scriptBuilder = StringBuilder()

        val contentScripts = jsonMap["content_scripts"] as? List<*>
        if (contentScripts != null) {
            for (csItem in contentScripts) {
                val cs = csItem as? Map<*, *> ?: continue
                val matchesList = cs["matches"] as? List<*>
                matchesList?.filterIsInstance<String>()?.let { matchesSet.addAll(it) }

                val jsList = cs["js"] as? List<*>
                if (jsList != null) {
                    for (jsPathItem in jsList) {
                        val jsPath = (jsPathItem as? String)?.removePrefix("/") ?: continue
                        val fileData = fileEntries[jsPath]
                            ?: fileEntries.entries.find { it.key.endsWith(jsPath, ignoreCase = true) }?.value
                        if (fileData != null) {
                            scriptBuilder.append("/* Script: $jsPath */\n")
                            scriptBuilder.append(String(fileData, StandardCharsets.UTF_8))
                            scriptBuilder.append("\n\n")
                        }
                    }
                }
            }
        }

        // If no content script, check background scripts or service worker
        if (scriptBuilder.isBlank()) {
            val background = jsonMap["background"] as? Map<*, *>
            val serviceWorker = (background?.get("service_worker") as? String) ?: ""
            if (serviceWorker.isNotBlank()) {
                val fileData = fileEntries[serviceWorker.removePrefix("/")]
                if (fileData != null) {
                    scriptBuilder.append("/* Background Service Worker: $serviceWorker */\n")
                    scriptBuilder.append(String(fileData, StandardCharsets.UTF_8))
                }
            } else {
                val bgScripts = background?.get("scripts") as? List<*>
                if (bgScripts != null) {
                    for (sItem in bgScripts) {
                        val path = (sItem as? String)?.removePrefix("/") ?: continue
                        val fileData = fileEntries[path]
                        if (fileData != null) {
                            scriptBuilder.append(String(fileData, StandardCharsets.UTF_8)).append("\n")
                        }
                    }
                }
            }
        }

        if (scriptBuilder.isBlank()) {
            scriptBuilder.append("""
                console.log('[Kopi CRX Engine] $name (v$version) активирован для страницы');
            """.trimIndent())
        }

        val matchesRule = if (matchesSet.isEmpty() || matchesSet.any { it.contains("<all_urls>") || it == "*" }) {
            "*"
        } else {
            matchesSet.joinToString(", ")
        }

        val extId = "crx_" + UUID.randomUUID().toString().take(8)

        return KopiExtension(
            id = extId,
            name = name,
            version = version,
            description = description,
            author = author,
            isEnabled = true,
            matches = matchesRule,
            scriptContent = scriptBuilder.toString(),
            source = "CRX Файл",
            permissions = permissionsString,
            hasActionButton = true,
            popupHtml = popupHtml,
            actionTitle = actionTitle
        )
    }
}
