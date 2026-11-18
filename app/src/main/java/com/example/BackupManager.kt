package com.example

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader

object BackupManager {

    fun generateBackupJson(context: Context): JSONObject {
        val root = JSONObject()
        try {
            root.put("app", "PDF Gallery")
            root.put("version", 1)
            root.put("timestamp", System.currentTimeMillis())

            val favMgr = FavoritesManager(context)
            val favArray = JSONArray()
            for (key in favMgr.getAllFavoriteKeys()) {
                favArray.put(key)
            }
            root.put("favorites", favArray)

            val urlMgr = UrlPdfManager(context)
            val notesMgr = PdfNotesManager(context)
            val items = urlMgr.getAllItems()
            val urlArray = JSONArray()
            for (item in items) {
                val itemJson = item.toJson()
                var col1: String? = null
                var col2: String? = null

                val localPath = item.localFilePath
                when {
                    localPath != null && notesMgr.hasCustomNotes(localPath) -> {
                        col1 = notesMgr.getNoteCol1ForPdf(localPath)
                        col2 = notesMgr.getNoteCol2ForPdf(localPath)
                    }
                    notesMgr.hasCustomNotes(item.title) -> {
                        col1 = notesMgr.getNoteCol1ForPdf(item.title)
                        col2 = notesMgr.getNoteCol2ForPdf(item.title)
                    }
                    notesMgr.hasCustomNotes(item.id) -> {
                        col1 = notesMgr.getNoteCol1ForPdf(item.id)
                        col2 = notesMgr.getNoteCol2ForPdf(item.id)
                    }
                    notesMgr.hasCustomNotes(item.originalUrl) -> {
                        col1 = notesMgr.getNoteCol1ForPdf(item.originalUrl)
                        col2 = notesMgr.getNoteCol2ForPdf(item.originalUrl)
                    }
                    localPath != null -> {
                        col1 = notesMgr.getNoteCol1ForPdf(localPath)
                        col2 = notesMgr.getNoteCol2ForPdf(localPath)
                    }
                }

                if (col1 != null || col2 != null) {
                    itemJson.put("note_col1", col1 ?: "")
                    itemJson.put("note_col2", col2 ?: "")
                    itemJson.put("notes", "${col1 ?: ""}\n\n${col2 ?: ""}")
                }
                urlArray.put(itemJson)
            }
            root.put("url_pdf_items", urlArray)

            val folderMgr = FolderManager(context)
            val foldersArray = JSONArray()
            for (f in folderMgr.getAllFolders()) {
                foldersArray.put(f)
            }
            root.put("folders", foldersArray)
            root.put("all_pdf_notes", notesMgr.exportAllNotesAsJson())
        } catch (_: Exception) {}
        return root
    }

    fun exportBackupToUri(context: Context, uri: Uri): Boolean {
        return try {
            val jsonStr = generateBackupJson(context).toString(2)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonStr.toByteArray(Charsets.UTF_8))
                out.flush()
                return true
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    fun importBackupFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                }
            } ?: return false

            val root = JSONObject(sb.toString())

            if (root.has("favorites")) {
                val favs = root.getJSONArray("favorites")
                val favMgr = FavoritesManager(context)
                val set = favMgr.getAllFavoriteKeys().toMutableSet()
                for (i in 0 until favs.length()) {
                    set.add(favs.getString(i))
                }
                favMgr.restoreFavoriteKeys(set)
            }

            val notesMgr = PdfNotesManager(context)

            if (root.has("url_pdf_items")) {
                val urls = root.getJSONArray("url_pdf_items")
                val urlMgr = UrlPdfManager(context)
                for (i in 0 until urls.length()) {
                    val itemObj = urls.getJSONObject(i)
                    val item = UrlPdfItem.fromJson(itemObj)
                    if (item != null) {
                        urlMgr.saveItem(item)
                        val col1 = if (itemObj.has("note_col1")) itemObj.optString("note_col1") else null
                        val col2 = if (itemObj.has("note_col2")) itemObj.optString("note_col2") else null
                        val fullNotes = if (itemObj.has("notes")) itemObj.optString("notes") else null
                        if (col1 != null || col2 != null || fullNotes != null) {
                            val c1 = col1 ?: ""
                            val c2 = col2 ?: ""
                            if (!item.localFilePath.isNullOrEmpty()) {
                                notesMgr.saveTwoColumnNotes(item.localFilePath, c1, c2)
                            }
                            if (item.title.isNotEmpty()) {
                                notesMgr.saveTwoColumnNotes(item.title, c1, c2)
                            }
                            if (item.id.isNotEmpty()) {
                                notesMgr.saveTwoColumnNotes(item.id, c1, c2)
                            }
                            if (item.originalUrl.isNotEmpty()) {
                                notesMgr.saveTwoColumnNotes(item.originalUrl, c1, c2)
                            }
                        }
                    }
                }
            }

            if (root.has("folders")) {
                val folders = root.getJSONArray("folders")
                val folderMgr = FolderManager(context)
                for (i in 0 until folders.length()) {
                    folderMgr.createFolder(folders.getString(i))
                }
            }

            if (root.has("all_pdf_notes")) {
                notesMgr.restoreNotesFromJson(root.getJSONArray("all_pdf_notes"))
            }

            true
        } catch (_: Exception) {
            false
        }
    }
}
