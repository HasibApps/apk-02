package com.example

import android.content.Context
import android.content.SharedPreferences

class FolderManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getAllFolders(): List<String> {
        val list = mutableListOf<String>()
        val hidden = prefs.getStringSet(KEY_HIDDEN_FOLDERS, emptySet()) ?: emptySet()
        if (!hidden.contains(FOLDER_SCREENSHOTS)) {
            list.add(FOLDER_SCREENSHOTS)
        }
        if (!hidden.contains(FOLDER_DOWNLOAD)) {
            list.add(FOLDER_DOWNLOAD)
        }
        val custom = prefs.getStringSet(KEY_FOLDERS, emptySet()) ?: emptySet()
        for (f in custom) {
            if (!list.contains(f)) {
                list.add(f)
            }
        }
        return list
    }

    fun createFolder(folderName: String?) {
        val name = folderName?.trim().orEmpty()
        if (name.isEmpty()) return

        val hidden = prefs.getStringSet(KEY_HIDDEN_FOLDERS, emptySet()) ?: emptySet()
        if (hidden.contains(name)) {
            val updatedHidden = hidden.toMutableSet().apply { remove(name) }
            prefs.edit().putStringSet(KEY_HIDDEN_FOLDERS, updatedHidden).apply()
        }

        val existing = prefs.getStringSet(KEY_FOLDERS, emptySet()) ?: emptySet()
        val updated = existing.toMutableSet().apply { add(name) }
        prefs.edit().putStringSet(KEY_FOLDERS, updated).apply()
    }

    fun deleteFolder(folderName: String?) {
        val name = folderName?.trim().orEmpty()
        if (name.isEmpty()) return

        val existing = prefs.getStringSet(KEY_FOLDERS, emptySet()) ?: emptySet()
        if (existing.contains(name)) {
            val updated = existing.toMutableSet().apply { remove(name) }
            prefs.edit().putStringSet(KEY_FOLDERS, updated).apply()
        }

        if (FOLDER_SCREENSHOTS.equals(name, ignoreCase = true) ||
            FOLDER_DOWNLOAD.equals(name, ignoreCase = true)
        ) {
            val hidden = prefs.getStringSet(KEY_HIDDEN_FOLDERS, emptySet()) ?: emptySet()
            val updatedHidden = hidden.toMutableSet().apply { add(name) }
            prefs.edit().putStringSet(KEY_HIDDEN_FOLDERS, updatedHidden).apply()
        }
    }

    fun renameFolder(oldName: String?, newName: String?) {
        val cleanNew = newName?.trim().orEmpty()
        if (oldName == null || cleanNew.isEmpty()) return
        createFolder(cleanNew)

        val all = prefs.all
        val edit = prefs.edit()
        for ((key, value) in all) {
            if (key.startsWith(KEY_MAPPING_PREFIX) && oldName == value) {
                edit.putString(key, cleanNew)
            }
        }
        edit.apply()

        deleteFolder(oldName)
    }

    fun assignPdfToFolder(pdfIdentifier: String?, folderName: String) {
        if (pdfIdentifier == null) return
        prefs.edit().putString(KEY_MAPPING_PREFIX + pdfIdentifier, folderName).apply()
    }

    fun getFolderForPdf(pdf: PdfFile?): String? {
        if (pdf == null) return null
        val id = pdf.path.ifEmpty { pdf.name }
        val assigned = prefs.getString(KEY_MAPPING_PREFIX + id, null)
        if (assigned != null) return assigned

        val hidden = prefs.getStringSet(KEY_HIDDEN_FOLDERS, emptySet()) ?: emptySet()
        val lowerName = pdf.name.lowercase()
        val lowerPath = pdf.path.lowercase()
        if ((lowerName.contains("screenshot") || lowerPath.contains("screenshot")) &&
            !hidden.contains(FOLDER_SCREENSHOTS)
        ) {
            return FOLDER_SCREENSHOTS
        }
        if ((lowerPath.contains("download") || lowerName.contains("download")) &&
            !hidden.contains(FOLDER_DOWNLOAD)
        ) {
            return FOLDER_DOWNLOAD
        }
        return null
    }

    fun filterByFolder(allFiles: List<PdfFile>, folder: String?): List<PdfFile> {
        if (folder == null || FOLDER_ALL.equals(folder, ignoreCase = true)) {
            return allFiles.toList()
        }
        if (FOLDER_FAVORITES.equals(folder, ignoreCase = true)) {
            val favMgr = FavoritesManager(appContext)
            return allFiles.filter { favMgr.isFavorite(it) }
        }
        return allFiles.filter { folder.equals(getFolderForPdf(it), ignoreCase = true) }
    }

    companion object {
        private const val PREF_NAME = "pdf_folder_prefs"
        private const val KEY_FOLDERS = "custom_folders"
        private const val KEY_HIDDEN_FOLDERS = "hidden_default_folders"
        private const val KEY_MAPPING_PREFIX = "file_folder_"

        const val FOLDER_ALL = "All"
        const val FOLDER_FAVORITES = "Favorites"
        const val FOLDER_SCREENSHOTS = "Screenshots"
        const val FOLDER_DOWNLOAD = "Download"
    }
}
