package com.example

import org.json.JSONObject
import java.io.Serializable

data class UrlPdfItem(
    var id: String = System.currentTimeMillis().toString(),
    var title: String = "Document.pdf",
    var originalUrl: String = "",
    var downloadUrl: String = "",
    var coverImagePath: String? = null,
    var localFilePath: String? = null,
    var dateAdded: Long = System.currentTimeMillis(),
    var fileSize: Long = 0L,
    var isDownloaded: Boolean = false
) : Serializable {

    val effectiveDownloadUrl: String
        get() = downloadUrl.ifEmpty { originalUrl }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        try {
            obj.put("id", id)
            obj.put("title", title)
            obj.put("originalUrl", originalUrl)
            obj.put("downloadUrl", downloadUrl)
            obj.put("coverImagePath", coverImagePath ?: "")
            obj.put("localFilePath", localFilePath ?: "")
            obj.put("dateAdded", dateAdded)
            obj.put("fileSize", fileSize)
            obj.put("isDownloaded", isDownloaded)
        } catch (_: Exception) {
        }
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject?): UrlPdfItem? {
            if (obj == null) return null
            return try {
                val cover = obj.optString("coverImagePath", "")
                val local = obj.optString("localFilePath", "")
                UrlPdfItem(
                    id = obj.optString("id", System.currentTimeMillis().toString()),
                    title = obj.optString("title", "Untitled.pdf"),
                    originalUrl = obj.optString("originalUrl", ""),
                    downloadUrl = obj.optString("downloadUrl", ""),
                    coverImagePath = cover.ifEmpty { null },
                    localFilePath = local.ifEmpty { null },
                    dateAdded = obj.optLong("dateAdded", System.currentTimeMillis()),
                    fileSize = obj.optLong("fileSize", 0L),
                    isDownloaded = obj.optBoolean("isDownloaded", false)
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
