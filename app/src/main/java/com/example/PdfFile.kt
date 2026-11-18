package com.example

import android.net.Uri
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

data class PdfFile(
    val id: Long,
    var name: String,
    var path: String,
    var uriString: String,
    var size: Long,
    var dateModified: Long,
    var pageCount: Int = -1
) : Serializable {

    constructor(
        id: Long,
        name: String?,
        path: String?,
        uri: Uri?,
        size: Long,
        dateModified: Long
    ) : this(
        id = id,
        name = name ?: "Document.pdf",
        path = path ?: "",
        uriString = uri?.toString() ?: "",
        size = size,
        dateModified = dateModified
    )

    val uri: Uri?
        get() = if (uriString.isNotEmpty()) Uri.parse(uriString) else null

    fun setUri(uri: Uri?) {
        uriString = uri?.toString() ?: ""
    }

    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
                .coerceAtMost(units.size - 1)
            return String.format(
                Locale.getDefault(),
                "%.2f %s",
                size / 1024.0.pow(digitGroups.toDouble()),
                units[digitGroups]
            )
        }

    val formattedDate: String
        get() = try {
            val timeMs = if (dateModified < 100_000_000_000L) dateModified * 1000L else dateModified
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timeMs))
        } catch (_: Exception) {
            "Recent"
        }

    val formattedFullDate: String
        get() = try {
            val timeMs = if (dateModified < 100_000_000_000L) dateModified * 1000L else dateModified
            SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(timeMs))
        } catch (_: Exception) {
            "Unknown"
        }
}
