package com.example.util

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import com.example.data.repository.JsText
import java.io.File
import java.io.IOException

/** Обычные скачивания файлов из браузера (не расширения). */
object FileDownloader {

    fun fileNameFor(url: String, contentDisposition: String?, mimeType: String?): String {
        val isInline = url.startsWith("blob:", true) || url.startsWith("data:", true)
        return URLUtil.guessFileName(if (isInline) "" else url, contentDisposition, mimeType)
    }

    /** http/https: передаём задачу системному DownloadManager (с cookie, User-Agent и Referer страницы). */
    fun enqueue(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        referer: String?
    ): String {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") throw IOException("Схема «$scheme» не поддерживается")

        val name = fileNameFor(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(uri).apply {
            setTitle(name)
            setDescription(uri.host ?: url)
            if (!mimeType.isNullOrBlank()) setMimeType(mimeType)
            CookieManager.getInstance().getCookie(url)?.let { if (it.isNotBlank()) addRequestHeader("Cookie", it) }
            if (!userAgent.isNullOrBlank()) addRequestHeader("User-Agent", userAgent)
            if (!referer.isNullOrBlank()) addRequestHeader("Referer", referer)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        return name
    }

    /** data:-адрес → (байты, mime) */
    fun decodeDataUrl(url: String): Pair<ByteArray, String>? {
        if (!url.startsWith("data:", ignoreCase = true)) return null
        val comma = url.indexOf(',')
        if (comma < 0) return null
        val meta = url.substring(5, comma)
        val data = url.substring(comma + 1)
        val isBase64 = meta.endsWith(";base64", ignoreCase = true)
        val mime = meta.removeSuffix(";base64").substringBefore(";").ifBlank { "application/octet-stream" }
        val bytes = if (isBase64) Base64.decode(data, Base64.DEFAULT) else Uri.decode(data).toByteArray(Charsets.UTF_8)
        return bytes to mime
    }

    /** JS, который достаёт содержимое blob: на странице и отдаёт его в KopiBrowserBridge.saveBase64 */
    fun blobScript(url: String, fileName: String, mime: String, token: String): String {
        val u = JsText.quote(url)
        val n = JsText.quote(fileName)
        val m = JsText.quote(mime)
        val t = JsText.quote(token)
        return """
            (function(u, n, m, t) {
                try {
                    var x = new XMLHttpRequest();
                    x.open('GET', u, true);
                    x.responseType = 'blob';
                    x.onload = function() {
                        var r = new FileReader();
                        r.onloadend = function() {
                            var s = String(r.result || '');
                            KopiBrowserBridge.saveBase64(s.substring(s.indexOf(',') + 1), n, (x.response && x.response.type) || m, t);
                        };
                        r.readAsDataURL(x.response);
                    };
                    x.onerror = function() { KopiBrowserBridge.downloadFailed('blob'); };
                    x.send();
                } catch (e) { KopiBrowserBridge.downloadFailed(String(e)); }
            })($u, $n, $m, $t);
        """.trimIndent()
    }

    /** Сохраняет байты в общую папку «Загрузки». Возвращает итоговое имя файла. */
    fun saveBytes(context: Context, bytes: ByteArray, fileName: String, mime: String): String {
        var name = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "download" }
        if (!name.contains('.')) {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.let { name += ".$it" }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Не удалось создать файл в «Загрузках»")
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw IOException("Не удалось записать файл")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return name
        }

        @Suppress("DEPRECATION")
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        dir.mkdirs()
        var target = File(dir, name)
        var i = 1
        val base = name.substringBeforeLast('.', name)
        val ext = if (name.contains('.')) "." + name.substringAfterLast('.') else ""
        while (target.exists()) target = File(dir, "$base ($i)$ext").also { i++ }
        target.writeBytes(bytes)
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), null, null)
        return target.name
    }
}
