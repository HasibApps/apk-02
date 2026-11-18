package com.example

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class PdfDownloadHelper private constructor(context: Context) {

    interface DownloadCallback {
        fun onProgress(percent: Int, downloadedBytes: Long, totalBytes: Long)
        fun onSuccess(downloadedFile: File)
        fun onError(errorMessage: String)
    }

    interface DownloadStateListener {
        fun onDownloadProgress(itemId: String, percent: Int, downloadedBytes: Long, totalBytes: Long)
        fun onDownloadSuccess(itemId: String, file: File)
        fun onDownloadError(itemId: String, errorMessage: String)
        fun onDownloadPausedOrCancelled(itemId: String, downloadedBytes: Long, totalBytes: Long)
    }

    data class DownloadProgressState(
        val itemId: String,
        var title: String,
        var percent: Int = 0,
        var downloadedBytes: Long = 0L,
        var totalBytes: Long = -1L,
        var isDownloading: Boolean = false,
        var isPaused: Boolean = false,
        var errorMessage: String? = null
    )

    private val appContext = context.applicationContext
    private val executor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val progressStates = ConcurrentHashMap<String, DownloadProgressState>()
    private val cancelledDownloads = ConcurrentHashMap<String, Boolean>()
    private val stateListeners = CopyOnWriteArrayList<DownloadStateListener>()
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val existing = cookieStore.getOrPut(url.host) { CopyOnWriteArrayList() }
                for (c in cookies) {
                    existing.removeAll { it.name == c.name }
                    existing.add(c)
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val result = mutableListOf<Cookie>()
                for ((host, list) in cookieStore) {
                    if (url.host.endsWith(host) || host.endsWith(url.host)) {
                        result.addAll(list)
                    }
                }
                return result
            }
        })
        .build()

    fun addStateListener(listener: DownloadStateListener) {
        if (!stateListeners.contains(listener)) {
            stateListeners.add(listener)
        }
    }

    fun removeStateListener(listener: DownloadStateListener) {
        stateListeners.remove(listener)
    }

    fun isDownloading(itemId: String): Boolean = progressStates[itemId]?.isDownloading == true

    fun hasActiveDownloads(): Boolean = progressStates.values.any { it.isDownloading }

    fun getProgressState(itemId: String): DownloadProgressState? {
        var state = progressStates[itemId]
        if (state == null) {
            val downloadsDir = UrlPdfManager(appContext).getDownloadsDirectory()
            val safeId = itemId.replace(Regex("[^a-zA-Z0-9_-]"), "")
            val partFile = File(downloadsDir, "dl_$safeId.part")
            if (partFile.exists() && partFile.length() > 0) {
                state = DownloadProgressState(itemId, "Document", downloadedBytes = partFile.length(), isPaused = true)
                progressStates[itemId] = state
            }
        }
        return state
    }

    fun cancelDownload(itemId: String?) {
        if (itemId == null) return
        cancelledDownloads[itemId] = true
        val state = progressStates[itemId]
        if (state != null) {
            state.isDownloading = false
            state.isPaused = true
            val curDownloaded = state.downloadedBytes
            val curTotal = state.totalBytes
            mainHandler.post {
                stateListeners.forEach { it.onDownloadPausedOrCancelled(itemId, curDownloaded, curTotal) }
            }
        }
    }

    fun deletePartFile(itemId: String?) {
        if (itemId == null) return
        cancelDownload(itemId)
        val downloadsDir = UrlPdfManager(appContext).getDownloadsDirectory()
        val safeId = itemId.replace(Regex("[^a-zA-Z0-9_-]"), "")
        val partFile = File(downloadsDir, "dl_$safeId.part")
        if (partFile.exists()) {
            try { partFile.delete() } catch (_: Exception) {}
        }
        progressStates.remove(itemId)
    }

    fun downloadPdf(item: UrlPdfItem?, callback: DownloadCallback?) {
        if (item == null) {
            callback?.onError("Invalid item")
            return
        }
        val itemId = item.id
        cancelledDownloads[itemId] = false

        val state = progressStates.getOrPut(itemId) { DownloadProgressState(itemId, item.title) }
        state.title = item.title
        state.isDownloading = true
        state.isPaused = false
        state.errorMessage = null

        PdfDownloadService.startDownload(appContext, itemId, item.title)

        executor.execute {
            var partFile: File? = null
            try {
                var fileId = UrlPdfManager.extractGoogleDriveFileId(item.originalUrl)
                if (fileId == null) fileId = UrlPdfManager.extractGoogleDriveFileId(item.downloadUrl)

                val downloadUrl = if (!fileId.isNullOrEmpty()) {
                    "https://drive.usercontent.google.com/download?id=$fileId&export=download&authuser=0&confirm=t"
                } else {
                    item.downloadUrl.ifEmpty { UrlPdfManager.extractDirectDownloadUrl(item.originalUrl) }
                }

                val downloadsDir = UrlPdfManager(appContext).getDownloadsDirectory()
                val safeId = itemId.replace(Regex("[^a-zA-Z0-9_-]"), "")
                partFile = File(downloadsDir, "dl_$safeId.part")

                var existingBytes = 0L
                var resumeRequested = false
                if (partFile.exists() && partFile.length() > 0) {
                    existingBytes = partFile.length()
                    resumeRequested = true
                }

                val reqBuilder = Request.Builder()
                    .url(downloadUrl)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                    )
                    .header("Accept", "application/pdf,application/octet-stream,*/*;q=0.8")
                    .header("Accept-Language", "bn,en;q=0.9")

                if (resumeRequested) {
                    reqBuilder.header("Range", "bytes=$existingBytes-")
                }

                var response = httpClient.newCall(reqBuilder.build()).execute()
                var body = response.body ?: throw Exception("সার্ভার থেকে কোনো রেসপন্স পাওয়া যায়নি")

                val statusCode = response.code
                var isResumeSuccess = (statusCode == 206)
                if (!isResumeSuccess && statusCode == 200) {
                    existingBytes = 0L
                }

                val contentType = body.contentType()?.toString()?.lowercase() ?: ""
                val firstChunk = ByteArray(65536)
                var rawIn = body.byteStream()
                var firstChunkSize = 0
                while (firstChunkSize < 65536) {
                    val r = rawIn.read(firstChunk, firstChunkSize, 65536 - firstChunkSize)
                    if (r == -1) break
                    firstChunkSize += r
                }

                var isPdf = isResumeSuccess || (
                    firstChunkSize >= 4 &&
                        firstChunk[0] == 0x25.toByte() &&
                        firstChunk[1] == 0x50.toByte() &&
                        firstChunk[2] == 0x44.toByte() &&
                        firstChunk[3] == 0x46.toByte()
                    )

                if (!isPdf && (contentType.contains("text/html") || startsWithHtml(firstChunk, firstChunkSize))) {
                    val initialHtml = String(firstChunk, 0, firstChunkSize, Charsets.UTF_8)
                    rawIn.close()
                    response.close()

                    val confirmUrl = extractGoogleDriveConfirmUrl(initialHtml, fileId)
                    if (!confirmUrl.isNullOrEmpty()) {
                        val confirmBuilder = Request.Builder()
                            .url(confirmUrl)
                            .header(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                            )
                            .header("Accept", "application/pdf,application/octet-stream,*/*;q=0.8")
                        if (existingBytes > 0) {
                            confirmBuilder.header("Range", "bytes=$existingBytes-")
                        }
                        response = httpClient.newCall(confirmBuilder.build()).execute()
                        body = response.body ?: throw Exception("কনফার্ম করার পর সার্ভার রেসপন্স দেয়নি")
                        if (response.code == 206) {
                            isResumeSuccess = true
                        } else if (response.code == 200) {
                            isResumeSuccess = false
                            existingBytes = 0L
                        }
                        rawIn = body.byteStream()
                        firstChunkSize = 0
                        while (firstChunkSize < 65536) {
                            val r = rawIn.read(firstChunk, firstChunkSize, 65536 - firstChunkSize)
                            if (r == -1) break
                            firstChunkSize += r
                        }
                        isPdf = isResumeSuccess || (
                            firstChunkSize >= 4 &&
                                firstChunk[0] == 0x25.toByte() &&
                                firstChunk[1] == 0x50.toByte() &&
                                firstChunk[2] == 0x44.toByte() &&
                                firstChunk[3] == 0x46.toByte()
                            )
                    } else {
                        throw Exception(explainDriveHtmlError(initialHtml))
                    }
                }

                if (!isPdf && !isResumeSuccess) {
                    rawIn.close()
                    response.close()
                    throw Exception("গুগল ড্রাইভ থেকে সঠিক PDF ফাইল পাওয়া যায়নি। লিংকটির এক্সেস 'Anyone with the link' আছে কিনা নিশ্চিত করুন।")
                }

                val disposition = response.header("Content-Disposition")
                if (disposition != null) {
                    val extractedName = UrlPdfManager.parseFilenameFromContentDisposition(disposition)
                    val clean = UrlPdfManager.cleanExtractedTitle(extractedName)
                    if (clean != null && item.title.startsWith("Drive_Document_")) {
                        item.title = clean
                    }
                }

                var totalBytes = -1L
                val contentRange = response.header("Content-Range")
                if (contentRange != null && contentRange.contains("/")) {
                    try {
                        totalBytes = contentRange.substringAfterLast("/").trim().toLong()
                    } catch (_: Exception) {}
                }
                if (totalBytes <= 0) {
                    val bodyLen = body.contentLength()
                    if (bodyLen > 0) totalBytes = existingBytes + bodyLen
                }

                val bis = BufferedInputStream(rawIn, 65536)
                val bos = BufferedOutputStream(FileOutputStream(partFile, isResumeSuccess), 65536)

                if (firstChunkSize > 0) {
                    bos.write(firstChunk, 0, firstChunkSize)
                }

                val buffer = ByteArray(65536)
                var downloadedBytes = existingBytes + firstChunkSize
                var lastProgressPost = 0L

                state.totalBytes = totalBytes
                state.downloadedBytes = downloadedBytes

                while (true) {
                    val read = bis.read(buffer)
                    if (read == -1) break

                    if (cancelledDownloads[itemId] == true) {
                        bos.flush()
                        bos.close()
                        bis.close()
                        response.close()

                        state.isDownloading = false
                        state.isPaused = true
                        state.downloadedBytes = downloadedBytes
                        val curDownloaded = downloadedBytes
                        val curTotal = totalBytes
                        mainHandler.post {
                            stateListeners.forEach {
                                it.onDownloadPausedOrCancelled(itemId, curDownloaded, curTotal)
                            }
                        }
                        return@execute
                    }

                    bos.write(buffer, 0, read)
                    downloadedBytes += read

                    val now = System.currentTimeMillis()
                    if (now - lastProgressPost > 180 || (totalBytes > 0 && downloadedBytes == totalBytes)) {
                        lastProgressPost = now
                        val curDownloaded = downloadedBytes
                        val curTotal = totalBytes
                        val percent = if (curTotal > 0) ((curDownloaded * 100) / curTotal).toInt() else -1
                        state.percent = percent
                        state.downloadedBytes = curDownloaded
                        state.totalBytes = curTotal

                        mainHandler.post {
                            callback?.onProgress(percent, curDownloaded, curTotal)
                            stateListeners.forEach {
                                it.onDownloadProgress(itemId, percent, curDownloaded, curTotal)
                            }
                        }
                    }
                }

                bos.flush()
                bos.close()
                bis.close()
                response.close()

                if (!partFile.exists() || !UrlPdfManager.isValidPdfFile(partFile)) {
                    if (partFile.exists()) partFile.delete()
                    throw Exception("ডাউনলোড সম্পূর্ণ হলেও ফাইলটি সঠিক PDF নয়")
                }

                val cleanTitle = PdfUtils.cleanPdfName(item.title)
                var safeName = cleanTitle.replace(Regex("[^a-zA-Z0-9._\\u0980-\\u09FF -]"), " ").trim()
                if (safeName.isEmpty()) safeName = "Document"
                if (!safeName.lowercase().endsWith(".pdf")) safeName += ".pdf"

                var finalFile = File(downloadsDir, safeName)
                if (finalFile.exists()) {
                    val baseName = safeName.substring(0, safeName.length - 4).trim()
                    var count = 1
                    while (finalFile.exists()) {
                        finalFile = File(downloadsDir, "$baseName ($count).pdf")
                        count++
                    }
                }
                if (!partFile.renameTo(finalFile)) {
                    finalFile = partFile
                }

                state.isDownloading = false
                state.isPaused = false
                state.percent = 100
                state.downloadedBytes = finalFile.length()
                state.totalBytes = finalFile.length()

                if (item.coverImagePath == null || !File(item.coverImagePath!!).exists()) {
                    val thumbPath = PdfUtils.generateAndSaveFirstPageThumbnail(appContext, finalFile, item.id)
                    if (thumbPath != null) item.coverImagePath = thumbPath
                }

                item.localFilePath = finalFile.absolutePath
                item.isDownloaded = true
                item.fileSize = finalFile.length()
                UrlPdfManager(appContext).saveItem(item)

                val completedFile = finalFile
                mainHandler.post {
                    callback?.onSuccess(completedFile)
                    stateListeners.forEach { it.onDownloadSuccess(itemId, completedFile) }
                }
            } catch (e: Exception) {
                state.isDownloading = false
                state.isPaused = partFile != null && partFile.exists() && partFile.length() > 0
                state.errorMessage = e.message ?: "ডাউনলোড ব্যর্থ হয়েছে"
                val msg = state.errorMessage!!
                mainHandler.post {
                    callback?.onError(msg)
                    stateListeners.forEach { it.onDownloadError(itemId, msg) }
                }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: PdfDownloadHelper? = null

        @Synchronized
        fun getInstance(context: Context): PdfDownloadHelper {
            return instance ?: PdfDownloadHelper(context).also { instance = it }
        }

        fun toBanglaDigits(input: String?): String = PdfUtils.toBengaliDigits(input)

        fun formatBanglaSize(bytes: Long): String {
            if (bytes <= 0) return "০ MB"
            val mb = bytes / (1024.0 * 1024.0)
            val formatted = when {
                mb < 0.1 -> String.format(Locale.US, "%.2f MB", mb)
                mb >= 100 -> String.format(Locale.US, "%.0f MB", mb)
                else -> String.format(Locale.US, "%.1f MB", mb)
            }
            return toBanglaDigits(formatted)
        }

        private fun startsWithHtml(bytes: ByteArray, len: Int): Boolean {
            if (len < 5) return false
            val start = String(bytes, 0, minOf(len, 64)).trim().lowercase()
            return start.startsWith("<!doctype") || start.startsWith("<html") || start.startsWith("<?xml")
        }

        private fun extractGoogleDriveConfirmUrl(html: String?, fileId: String?): String? {
            if (html == null) return null
            var action: String? = null
            val mForm = Pattern.compile(
                "<form[^>]+id=[\"']download-form[\"'][^>]+action=[\"'](.*?)[\"']",
                Pattern.CASE_INSENSITIVE
            ).matcher(html)
            if (mForm.find()) {
                action = mForm.group(1)
            } else {
                val mForm2 = Pattern.compile(
                    "<form[^>]+action=[\"'](https://drive\\.usercontent\\.google\\.com/download.*?)[\"']",
                    Pattern.CASE_INSENSITIVE
                ).matcher(html)
                if (mForm2.find()) action = mForm2.group(1)
            }

            val mConfirm = Pattern.compile(
                "<input[^>]+name=[\"']confirm[\"'][^>]+value=[\"'](.*?)[\"']",
                Pattern.CASE_INSENSITIVE
            ).matcher(html)
            val confirmVal = if (mConfirm.find()) mConfirm.group(1) ?: "t" else "t"

            val mUuid = Pattern.compile(
                "<input[^>]+name=[\"']uuid[\"'][^>]+value=[\"'](.*?)[\"']",
                Pattern.CASE_INSENSITIVE
            ).matcher(html)
            val uuidVal = if (mUuid.find()) mUuid.group(1) else null

            if (action != null) {
                var url = action
                if (!url.startsWith("http")) {
                    url = "https://drive.google.com" + (if (url.startsWith("/")) "" else "/") + url
                }
                if (!url.contains("confirm=")) {
                    url += (if (url.contains("?")) "&" else "?") + "confirm=$confirmVal"
                }
                if (uuidVal != null && !url.contains("uuid=")) url += "&uuid=$uuidVal"
                if (fileId != null && !url.contains("id=")) url += "&id=$fileId"
                if (!url.contains("export=download")) url += "&export=download"
                return url
            }

            val mLink = Pattern.compile(
                "<a[^>]+id=[\"']uc-download-link[\"'][^>]+href=[\"'](.*?)[\"']",
                Pattern.CASE_INSENSITIVE
            ).matcher(html)
            if (mLink.find()) {
                var href = mLink.group(1)?.replace("&amp;", "&") ?: ""
                if (!href.startsWith("http")) {
                    href = "https://drive.google.com" + (if (href.startsWith("/")) "" else "/") + href
                }
                return href
            }

            return if (fileId != null) {
                "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
            } else {
                null
            }
        }

        private fun explainDriveHtmlError(html: String?): String {
            if (html == null) return "ড্রাইভ ফাইলটি ডাউনলোড করা সম্ভব হয়নি"
            val lower = html.lowercase()
            return when {
                lower.contains("you need permission") || lower.contains("access denied") ||
                    lower.contains("request access") || lower.contains("sign-in - google accounts") ->
                    "ফাইলটিতে পাবলিক এক্সেস নেই। ড্রাইভ ফাইলটি 'Anyone with the link' শেয়ার করা আছে কিনা নিশ্চিত করুন।"
                lower.contains("file not found") || lower.contains("does not exist") ||
                    lower.contains("error 404") ->
                    "Google Drive-এ ফাইলটি খুঁজে পাওয়া যায়নি। লিংকটি সঠিক কিনা যাচাই করুন।"
                lower.contains("download quota") || lower.contains("too many users") ||
                    lower.contains("traffic limit") ->
                    "গুগল ড্রাইভ ডাউনলোডের লিমিট শেষ হয়েছে। কিছুক্ষণ পর পুনরায় চেষ্টা করুন।"
                else ->
                    "গুগল ড্রাইভের রেসপন্স যাচাই ব্যর্থ হয়েছে। লিংকটি সঠিক ও পাবলিক কিনা চেক করুন।"
            }
        }
    }
}
