package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class UrlPdfManager(context: Context) {

    interface DriveTitleCallback {
        fun onTitleFetched(title: String)
        fun onError(error: String)
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getAllItems(): MutableList<UrlPdfItem> {
        val list = mutableListOf<UrlPdfItem>()
        val raw = prefs.getString(KEY_ITEMS, null)
        var needsResave = false
        if (!raw.isNullOrBlank()) {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val item = UrlPdfItem.fromJson(array.getJSONObject(i)) ?: continue
                    val cleanTitle = PdfUtils.cleanPdfName(item.title)
                    if (cleanTitle != item.title) {
                        item.title = cleanTitle
                        needsResave = true
                    }

                    val localPath = item.localFilePath
                    if (localPath != null) {
                        var f = File(localPath)
                        if (f.name.lowercase().startsWith("dl_url_") || f.name.lowercase().startsWith("dl_")) {
                            val cleanFileName = PdfUtils.cleanPdfName(f.name)
                            val renamedFile = File(f.parentFile, cleanFileName)
                            if (!renamedFile.exists() && f.exists() && f.renameTo(renamedFile)) {
                                f = renamedFile
                                item.localFilePath = renamedFile.absolutePath
                                needsResave = true
                            }
                        }

                        if (f.exists() && isValidPdfFile(f)) {
                            item.isDownloaded = true
                            if (item.coverImagePath == null || !File(item.coverImagePath!!).exists()) {
                                val autoThumb = PdfUtils.generateAndSaveFirstPageThumbnail(appContext, f, item.id)
                                if (autoThumb != null) {
                                    item.coverImagePath = autoThumb
                                    needsResave = true
                                }
                            }
                        } else {
                            item.isDownloaded = false
                            if (f.exists() && !isValidPdfFile(f)) {
                                f.delete()
                                item.localFilePath = null
                                needsResave = true
                            }
                        }
                    } else {
                        item.isDownloaded = false
                        if (item.coverImagePath == null || !File(item.coverImagePath!!).exists()) {
                            titleExecutor.execute {
                                val thumb = PdfUtils.downloadDriveThumbnailIfAvailable(
                                    appContext,
                                    item.originalUrl,
                                    item.id
                                )
                                if (thumb != null) {
                                    updateCoverImagePath(item.id, thumb)
                                }
                            }
                        }
                    }
                    list.add(item)
                }
            } catch (_: Exception) {}
        }
        if (needsResave) {
            saveAll(list)
        }
        return list
    }

    @Synchronized
    fun updateCoverImagePath(itemId: String?, newCoverPath: String?) {
        if (itemId == null) return
        val items = getAllItems()
        var found = false
        for (it in items) {
            if (itemId == it.id) {
                it.coverImagePath = newCoverPath
                found = true
                break
            }
        }
        if (found) {
            saveAll(items)
        }
    }

    @Synchronized
    fun saveItem(item: UrlPdfItem?) {
        if (item == null) return
        val items = getAllItems()
        val idx = items.indexOfFirst { it.id == item.id }
        if (idx >= 0) {
            items[idx] = item
        } else {
            items.add(0, item)
        }
        saveAll(items)
    }

    @Synchronized
    fun deleteItem(id: String?) {
        if (id == null) return
        val items = getAllItems()
        val toRemove = items.firstOrNull { it.id == id } ?: return
        toRemove.localFilePath?.let { try { File(it).delete() } catch (_: Exception) {} }
        toRemove.coverImagePath?.let { try { File(it).delete() } catch (_: Exception) {} }
        items.remove(toRemove)
        saveAll(items)
    }

    @Synchronized
    fun saveAll(items: List<UrlPdfItem>?) {
        val array = JSONArray()
        items?.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    @Synchronized
    fun getItemById(id: String): UrlPdfItem? = getAllItems().firstOrNull { it.id == id }

    fun saveCoverImageToInternalStorage(uri: Uri?): String? {
        if (uri == null) return null
        return try {
            val coversDir = File(appContext.filesDir, "covers").apply { if (!exists()) mkdirs() }
            val dest = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
            val bitmap = appContext.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: return null
            FileOutputStream(dest).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.flush()
            }
            bitmap.recycle()
            dest.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun getDownloadsDirectory(): File =
        File(appContext.filesDir, "downloaded_pdfs").apply { if (!exists()) mkdirs() }

    companion object {
        private const val PREF_NAME = "pdf_url_items_prefs"
        private const val KEY_ITEMS = "url_pdf_items_json"

        private val titleExecutor = Executors.newFixedThreadPool(2)
        private val mainHandler = Handler(Looper.getMainLooper())
        private val titleHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        fun isValidPdfFile(file: File?): Boolean {
            if (file == null || !file.exists() || file.length() < 100) return false
            return try {
                FileInputStream(file).use { input ->
                    val header = ByteArray(4)
                    val read = input.read(header)
                    read == 4 &&
                        header[0] == 0x25.toByte() &&
                        header[1] == 0x50.toByte() &&
                        header[2] == 0x44.toByte() &&
                        header[3] == 0x46.toByte()
                }
            } catch (_: Exception) {
                false
            }
        }

        fun extractGoogleDriveFileId(url: String?): String? {
            if (url == null) return null
            val clean = url.trim()
            val patterns = arrayOf(
                "/file/d/([a-zA-Z0-9_-]+)",
                "/d/([a-zA-Z0-9_-]+)",
                "[?&]id=([a-zA-Z0-9_-]+)"
            )
            for (p in patterns) {
                val m = Pattern.compile(p).matcher(clean)
                if (m.find()) return m.group(1)
            }
            return null
        }

        fun extractDirectDownloadUrl(url: String?): String {
            if (url == null) return ""
            val clean = url.trim()
            val fileId = extractGoogleDriveFileId(clean)
            if (!fileId.isNullOrEmpty()) {
                return "https://drive.usercontent.google.com/download?id=$fileId&export=download&authuser=0&confirm=t"
            }
            return clean
        }

        fun fetchDriveTitleAsync(rawUrl: String?, callback: DriveTitleCallback?) {
            if (rawUrl.isNullOrBlank()) {
                callback?.onError("Empty URL")
                return
            }
            val url = rawUrl.trim()
            titleExecutor.execute {
                val title = fetchDriveTitleInternal(url)
                mainHandler.post {
                    if (!title.isNullOrEmpty()) {
                        callback?.onTitleFetched(title)
                    } else {
                        callback?.onError("Could not extract title")
                    }
                }
            }
        }

        fun fetchDriveTitleSync(rawUrl: String?): String? {
            if (rawUrl.isNullOrBlank()) return null
            return fetchDriveTitleInternal(rawUrl.trim())
        }

        private fun fetchDriveTitleInternal(url: String): String? {
            val fileId = extractGoogleDriveFileId(url)
            if (!fileId.isNullOrEmpty()) {
                try {
                    val viewUrl = "https://drive.google.com/file/d/$fileId/view"
                    val request = Request.Builder()
                        .url(viewUrl)
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                        )
                        .header("Accept-Language", "bn,en;q=0.9")
                        .build()
                    titleHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful && response.body != null) {
                            val html = response.body!!.string()
                            val patterns = arrayOf(
                                "<meta\\s+property=[\"']og:title[\"']\\s+content=[\"'](.*?)[\"']",
                                "<meta\\s+name=[\"']title[\"']\\s+content=[\"'](.*?)[\"']",
                                "<title>(.*?)</title>",
                                "\"title\":\"([^\"]+?\\.pdf)\""
                            )
                            for (pat in patterns) {
                                val m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(html)
                                if (m.find()) {
                                    val clean = cleanExtractedTitle(m.group(1))
                                    if (clean != null) return clean
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}

                try {
                    val dlUrl =
                        "https://drive.usercontent.google.com/download?id=$fileId&export=download&authuser=0&confirm=t"
                    val req = Request.Builder()
                        .url(dlUrl)
                        .header("Range", "bytes=0-1024")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build()
                    titleHttpClient.newCall(req).execute().use { resp ->
                        val disposition = resp.header("Content-Disposition")
                        if (disposition != null) {
                            val extracted = parseFilenameFromContentDisposition(disposition)
                            val clean = cleanExtractedTitle(extracted)
                            if (clean != null) return clean
                        }
                    }
                } catch (_: Exception) {}
            }

            try {
                val lastPath = Uri.parse(url).lastPathSegment
                if (lastPath != null && lastPath.lowercase().endsWith(".pdf")) {
                    return cleanExtractedTitle(lastPath)
                }
            } catch (_: Exception) {}
            return null
        }

        fun parseFilenameFromContentDisposition(disposition: String?): String? {
            if (disposition == null) return null
            val mUtf = Pattern.compile("filename\\*=UTF-8''([^;\\r\\n]+)", Pattern.CASE_INSENSITIVE)
                .matcher(disposition)
            if (mUtf.find()) {
                try {
                    return URLDecoder.decode(mUtf.group(1), "UTF-8")
                } catch (_: Exception) {}
            }
            val mQuoted = Pattern.compile("filename=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
                .matcher(disposition)
            if (mQuoted.find()) return mQuoted.group(1)

            val mPlain = Pattern.compile("filename=([^;\\s]+)", Pattern.CASE_INSENSITIVE)
                .matcher(disposition)
            if (mPlain.find()) return mPlain.group(1)
            return null
        }

        fun cleanExtractedTitle(rawTitle: String?): String? {
            var clean = rawTitle?.trim() ?: return null
            if (clean.isEmpty()) return null
            try {
                if (clean.contains("%")) {
                    clean = URLDecoder.decode(clean, "UTF-8")
                }
            } catch (_: Exception) {}

            clean = clean.replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")

            if (clean.endsWith(" - Google Drive")) {
                clean = clean.removeSuffix(" - Google Drive").trim()
            }
            if (clean.endsWith(" - Google Docs")) {
                clean = clean.removeSuffix(" - Google Docs").trim()
            }
            val lower = clean.lowercase()
            if (lower == "google drive" || lower == "google drive - virus scan warning" ||
                lower.contains("meet google drive") || lower.contains("access denied") ||
                lower.contains("sign-in - google accounts")
            ) {
                return null
            }
            if (!clean.lowercase().endsWith(".pdf")) {
                clean += ".pdf"
            }
            return clean
        }
    }
}
