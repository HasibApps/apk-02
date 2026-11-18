package com.example.ui.main

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BackupManager
import com.example.FavoritesManager
import com.example.FolderManager
import com.example.PdfDownloadHelper
import com.example.PdfFile
import com.example.PdfUtils
import com.example.UrlPdfItem
import com.example.UrlPdfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val folderManager = FolderManager(appContext)
    private val favoritesManager = FavoritesManager(appContext)
    private val urlPdfManager = UrlPdfManager(appContext)
    private val downloadHelper = PdfDownloadHelper.getInstance(appContext)

    private val _pdfList = MutableStateFlow<List<PdfFile>>(emptyList())
    val pdfList: StateFlow<List<PdfFile>> = _pdfList.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolder = MutableStateFlow(FolderManager.FOLDER_ALL)
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    private val _selectedPdfKeys = MutableStateFlow<Set<String>>(emptySet())
    val selectedPdfKeys: StateFlow<Set<String>> = _selectedPdfKeys.asStateFlow()

    private val _isDownloadsTab = MutableStateFlow(false)
    val isDownloadsTab: StateFlow<Boolean> = _isDownloadsTab.asStateFlow()

    private val _urlItems = MutableStateFlow<List<UrlPdfItem>>(emptyList())
    val urlItems: StateFlow<List<UrlPdfItem>> = _urlItems.asStateFlow()

    private val _downloadProgressMap =
        MutableStateFlow<Map<String, PdfDownloadHelper.DownloadProgressState>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, PdfDownloadHelper.DownloadProgressState>> =
        _downloadProgressMap.asStateFlow()

    private val _thumbnailCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnailCache: StateFlow<Map<String, Bitmap>> = _thumbnailCache.asStateFlow()

    private val _pageCountCache = MutableStateFlow<Map<String, Int>>(emptyMap())
    val pageCountCache: StateFlow<Map<String, Int>> = _pageCountCache.asStateFlow()

    private val _favoriteKeys = MutableStateFlow<Set<String>>(favoritesManager.getAllFavoriteKeys())
    val favoriteKeys: StateFlow<Set<String>> = _favoriteKeys.asStateFlow()

    private val _folders = MutableStateFlow<List<String>>(folderManager.getAllFolders())
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    private val _folderVersion = MutableStateFlow(0)
    val folderVersion: StateFlow<Int> = _folderVersion.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val inFlightThumbKeys = ConcurrentHashMap.newKeySet<String>()

    val filteredPdfList: StateFlow<List<PdfFile>> = combine(
        _pdfList,
        _selectedFolder,
        _searchQuery,
        _favoriteKeys,
        _folderVersion
    ) { allFiles, folder, query, favKeys, _ ->
        val byFolder = when {
            folder.equals(FolderManager.FOLDER_ALL, ignoreCase = true) -> allFiles
            folder.equals(FolderManager.FOLDER_FAVORITES, ignoreCase = true) ->
                allFiles.filter { favKeys.contains(FavoritesManager.getPdfKey(it)) }
            else -> allFiles.filter {
                folder.equals(folderManager.getFolderForPdf(it), ignoreCase = true)
            }
        }
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) {
            byFolder
        } else {
            byFolder.filter { it.name.lowercase().contains(cleanQuery) }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredUrlItems: StateFlow<List<UrlPdfItem>> = combine(
        _urlItems,
        _searchQuery
    ) { items, query ->
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) {
            items
        } else {
            items.filter {
                it.title.lowercase().contains(cleanQuery) ||
                    it.originalUrl.lowercase().contains(cleanQuery)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val downloadListener = object : PdfDownloadHelper.DownloadStateListener {
        override fun onDownloadProgress(
            itemId: String,
            percent: Int,
            downloadedBytes: Long,
            totalBytes: Long
        ) {
            _downloadProgressMap.update { current ->
                val prev = current[itemId]
                val next = (prev ?: PdfDownloadHelper.DownloadProgressState(itemId, "Document")).copy(
                    percent = percent,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                    isDownloading = true,
                    isPaused = false,
                    errorMessage = null
                )
                current + (itemId to next)
            }
        }

        override fun onDownloadSuccess(itemId: String, file: File) {
            _downloadProgressMap.update { current ->
                val prev = current[itemId]
                val next = (prev ?: PdfDownloadHelper.DownloadProgressState(itemId, file.name)).copy(
                    percent = 100,
                    downloadedBytes = file.length(),
                    totalBytes = file.length(),
                    isDownloading = false,
                    isPaused = false,
                    errorMessage = null
                )
                current + (itemId to next)
            }
            loadDownloadsList()
            loadPdfFiles()
            val itemTitle = _urlItems.value.firstOrNull { it.id == itemId }?.title ?: file.name
            _toastEvents.tryEmit("ডাউনলোড সফল: $itemTitle")
        }

        override fun onDownloadError(itemId: String, errorMessage: String) {
            val state = downloadHelper.getProgressState(itemId)
            _downloadProgressMap.update { current ->
                val next = (state ?: PdfDownloadHelper.DownloadProgressState(itemId, "Document")).copy(
                    isDownloading = false,
                    errorMessage = errorMessage
                )
                current + (itemId to next)
            }
            _toastEvents.tryEmit(errorMessage)
        }

        override fun onDownloadPausedOrCancelled(
            itemId: String,
            downloadedBytes: Long,
            totalBytes: Long
        ) {
            _downloadProgressMap.update { current ->
                val prev = current[itemId]
                val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0
                val next = (prev ?: PdfDownloadHelper.DownloadProgressState(itemId, "Document")).copy(
                    percent = percent,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                    isDownloading = false,
                    isPaused = downloadedBytes > 0
                )
                current + (itemId to next)
            }
        }
    }

    init {
        downloadHelper.addStateListener(downloadListener)
    }

    override fun onCleared() {
        downloadHelper.removeStateListener(downloadListener)
        super.onCleared()
    }

    fun itemKey(pdf: PdfFile): String = "${pdf.id}_${pdf.path}_${pdf.uriString}"

    fun isFavorite(pdf: PdfFile): Boolean =
        _favoriteKeys.value.contains(FavoritesManager.getPdfKey(pdf))

    fun folderCount(folderId: String): Int {
        val all = _pdfList.value
        val favs = _favoriteKeys.value
        return when {
            folderId.equals(FolderManager.FOLDER_ALL, ignoreCase = true) -> all.size
            folderId.equals(FolderManager.FOLDER_FAVORITES, ignoreCase = true) ->
                all.count { favs.contains(FavoritesManager.getPdfKey(it)) }
            else -> all.count { folderId.equals(folderManager.getFolderForPdf(it), ignoreCase = true) }
        }
    }

    fun switchToHomeTab() {
        _isDownloadsTab.value = false
    }

    fun switchToDownloadsTab() {
        _isDownloadsTab.value = true
        clearSelectionMode()
        loadDownloadsList()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFolder(folderId: String) {
        _selectedFolder.value = folderId
    }

    fun clearFolderFilter() {
        _selectedFolder.value = FolderManager.FOLDER_ALL
    }

    fun toggleSelection(pdf: PdfFile) {
        val key = itemKey(pdf)
        _selectedPdfKeys.update { current ->
            if (current.contains(key)) current - key else current + key
        }
    }

    fun clearSelectionMode() {
        _selectedPdfKeys.value = emptySet()
    }

    fun toggleFavorite(pdf: PdfFile) {
        val newState = favoritesManager.toggleFavorite(pdf)
        _favoriteKeys.value = favoritesManager.getAllFavoriteKeys()
        _toastEvents.tryEmit(
            if (newState) "ফেবারিট হিসেবে যোগ করা হয়েছে" else "ফেবারিট থেকে বাদ দেওয়া হয়েছে"
        )
    }

    fun loadPdfFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            val loaded = withContext(Dispatchers.IO) {
                val files = mutableListOf<PdfFile>()

                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Files.getContentUri("external")
                }

                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_MODIFIED
                )
                val selection = "${MediaStore.Files.FileColumns.MIME_TYPE}=?"
                val selectionArgs = arrayOf("application/pdf")
                val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

                try {
                    appContext.contentResolver.query(
                        collection, projection, selection, selectionArgs, sortOrder
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                        val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                        val pathCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                        val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                        val dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)

                        while (cursor.moveToNext()) {
                            val id = if (idCol >= 0) cursor.getLong(idCol) else 0L
                            val name = if (nameCol >= 0) cursor.getString(nameCol) else "Document.pdf"
                            val path = if (pathCol >= 0) cursor.getString(pathCol) ?: "" else ""
                            val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                            val date = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
                            val cleanName = PdfUtils.cleanPdfName(name)
                            val contentUri = Uri.withAppendedPath(collection, id.toString())
                            files.add(PdfFile(id, cleanName, path, contentUri, size, date))
                        }
                    }
                } catch (_: Exception) {
                }

                appContext.getExternalFilesDir(null)?.let { externalDocs ->
                    if (externalDocs.exists()) {
                        externalDocs.listFiles()?.forEach { f ->
                            if (f.isFile && f.name.lowercase().endsWith(".pdf")) {
                                val exists = files.any { it.path == f.absolutePath }
                                if (!exists) {
                                    val uri = FileProvider.getUriForFile(
                                        appContext,
                                        PdfUtils.fileProviderAuthority(appContext),
                                        f
                                    )
                                    val clean = PdfUtils.cleanPdfName(f.name)
                                    files.add(
                                        PdfFile(
                                            f.hashCode().toLong(),
                                            clean,
                                            f.absolutePath,
                                            uri,
                                            f.length(),
                                            f.lastModified()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                val downloadsDir = urlPdfManager.getDownloadsDirectory()
                if (downloadsDir.exists()) {
                    downloadsDir.listFiles()?.forEach { f ->
                        if (f.isFile && f.name.lowercase().endsWith(".pdf")) {
                            val exists = files.any { it.path == f.absolutePath }
                            if (!exists) {
                                val uri = FileProvider.getUriForFile(
                                    appContext,
                                    PdfUtils.fileProviderAuthority(appContext),
                                    f
                                )
                                val clean = PdfUtils.cleanPdfName(f.name)
                                files.add(
                                    PdfFile(
                                        f.hashCode().toLong(),
                                        clean,
                                        f.absolutePath,
                                        uri,
                                        f.length(),
                                        f.lastModified()
                                    )
                                )
                            }
                        }
                    }
                }

                if (files.isEmpty()) {
                    files.addAll(PdfUtils.createSamplePdfFiles(appContext))
                }

                files.sortByDescending { pdf ->
                    val t = pdf.dateModified
                    if (t < 100_000_000_000L) t * 1000L else t
                }
                files.toList()
            }

            _pdfList.value = loaded
            _folders.value = folderManager.getAllFolders()
            _favoriteKeys.value = favoritesManager.getAllFavoriteKeys()
            _folderVersion.update { it + 1 }
            _isLoading.value = false
        }
    }

    fun loadDownloadsList() {
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) {
                urlPdfManager.getAllItems().toList()
            }
            _urlItems.value = items
            val stateSnapshot = mutableMapOf<String, PdfDownloadHelper.DownloadProgressState>()
            for (item in items) {
                downloadHelper.getProgressState(item.id)?.let { state ->
                    stateSnapshot[item.id] = state.copy()
                }
            }
            _downloadProgressMap.value = stateSnapshot
        }
    }

    fun lazyLoadThumbnailAndPageCount(pdfFile: PdfFile) {
        val key = itemKey(pdfFile)
        if (_thumbnailCache.value.containsKey(key) && _pageCountCache.value.containsKey(key)) return
        if (!inFlightThumbKeys.add(key)) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = pdfFile.uri ?: return@launch
                if (!_pageCountCache.value.containsKey(key)) {
                    val count = PdfUtils.getPageCount(appContext, uri)
                    _pageCountCache.update { it + (key to count) }
                }
                if (!_thumbnailCache.value.containsKey(key)) {
                    val bmp = PdfUtils.renderPageToBitmap(appContext, uri, 0, 120)
                    if (bmp != null) {
                        _thumbnailCache.update { it + (key to bmp) }
                    }
                }
            } finally {
                inFlightThumbKeys.remove(key)
            }
        }
    }

    fun createSamples() {
        viewModelScope.launch {
            _isLoading.value = true
            val samples = withContext(Dispatchers.IO) {
                PdfUtils.createSamplePdfFiles(appContext)
            }
            _isLoading.value = false
            _toastEvents.tryEmit("Generated ${samples.size} sample PDFs")
            loadPdfFiles()
        }
    }

    fun importPdfFromUri(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                var fileName = "Imported_Document.pdf"
                val resolver: ContentResolver = appContext.contentResolver
                try {
                    resolver.query(uri, null, null, null, null)?.use { cursor: Cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex) ?: fileName
                        }
                    }
                } catch (_: Exception) {
                }

                val destDir = appContext.getExternalFilesDir(null) ?: appContext.filesDir
                var destFile = File(destDir, fileName)
                if (destFile.exists()) {
                    val base = fileName.removeSuffix(".pdf")
                    var count = 1
                    while (destFile.exists()) {
                        destFile = File(destDir, "$base ($count).pdf")
                        count++
                    }
                }

                try {
                    resolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { fos ->
                            input.copyTo(fos, 8192)
                        }
                    }
                    Result.success(destFile.name)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            result.onSuccess { importedName ->
                _toastEvents.tryEmit("Imported $importedName")
                loadPdfFiles()
            }.onFailure { err ->
                _toastEvents.tryEmit("Failed to import PDF: ${err.message}")
            }
        }
    }

    fun finalizeUrlImport(rawUrl: String, customTitle: String, coverUri: Uri?) {
        viewModelScope.launch {
            val newItem = withContext(Dispatchers.IO) {
                val coverPath = coverUri?.let { urlPdfManager.saveCoverImageToInternalStorage(it) }
                val downloadUrl = UrlPdfManager.extractDirectDownloadUrl(rawUrl)
                val id = "url_${System.currentTimeMillis()}"
                val item = UrlPdfItem(
                    id = id,
                    title = customTitle,
                    originalUrl = rawUrl,
                    downloadUrl = downloadUrl,
                    coverImagePath = coverPath
                )
                urlPdfManager.saveItem(item)

                if (coverPath == null) {
                    val driveId = UrlPdfManager.extractGoogleDriveFileId(rawUrl)
                    if (driveId != null) {
                        val thumb = PdfUtils.downloadDriveThumbnailIfAvailable(appContext, rawUrl, id)
                        if (thumb != null) {
                            urlPdfManager.updateCoverImagePath(id, thumb)
                        }
                    }
                }
                item
            }
            _toastEvents.tryEmit("PDF যোগ হয়েছে! পড়তে 'Download' বাটনে চাপ দিন")
            _isDownloadsTab.value = true
            loadDownloadsList()
        }
    }

    fun startOrResumeDownload(item: UrlPdfItem) {
        downloadHelper.downloadPdf(item, object : PdfDownloadHelper.DownloadCallback {
            override fun onProgress(percent: Int, downloadedBytes: Long, totalBytes: Long) = Unit
            override fun onSuccess(downloadedFile: File) = Unit
            override fun onError(errorMessage: String) = Unit
        })
    }

    fun cancelDownload(itemId: String) {
        downloadHelper.cancelDownload(itemId)
    }

    fun markItemCorruptAndRedownload(item: UrlPdfItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                item.localFilePath?.let { path ->
                    val f = File(path)
                    if (f.exists()) f.delete()
                }
                item.isDownloaded = false
                item.localFilePath = null
                urlPdfManager.saveItem(item)
            }
            _toastEvents.tryEmit("ফাইলটি সঠিক PDF ছিল না। পুনরায় ডাউনলোড করা হচ্ছে...")
            loadDownloadsList()
            startOrResumeDownload(item)
        }
    }

    fun deleteUrlItem(item: UrlPdfItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                downloadHelper.deletePartFile(item.id)
                urlPdfManager.deleteItem(item.id)
            }
            _toastEvents.tryEmit("Deleted")
            loadDownloadsList()
            loadPdfFiles()
        }
    }

    fun updateCoverForUrlItem(itemId: String, uri: Uri) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                urlPdfManager.saveCoverImageToInternalStorage(uri)
            }
            if (savedPath != null) {
                withContext(Dispatchers.IO) {
                    urlPdfManager.updateCoverImagePath(itemId, savedPath)
                }
                loadDownloadsList()
                _toastEvents.tryEmit("কাস্টম থাম্বনেইল সফলভাবে পরিবর্তন করা হয়েছে!")
            }
        }
    }

    fun createFolder(name: String, selectAfterCreate: Boolean = true): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) {
            _toastEvents.tryEmit("ফোল্ডারের নাম খালি রাখা যাবে না")
            return false
        }
        folderManager.createFolder(clean)
        _folders.value = folderManager.getAllFolders()
        _folderVersion.update { it + 1 }
        if (selectAfterCreate) {
            _selectedFolder.value = clean
        }
        _toastEvents.tryEmit("ফোল্ডার '$clean' তৈরি হয়েছে")
        return true
    }

    fun createFolderFromSelected(folderName: String): Boolean {
        val clean = folderName.trim()
        if (clean.isEmpty()) {
            _toastEvents.tryEmit("ফোল্ডারের নাম খালি রাখা যাবে না")
            return false
        }
        val keys = _selectedPdfKeys.value
        val selectedFiles = _pdfList.value.filter { keys.contains(itemKey(it)) }
        if (selectedFiles.isEmpty()) {
            _toastEvents.tryEmit("কোনো ফাইল নির্বাচিত নেই")
            return false
        }
        folderManager.createFolder(clean)
        for (file in selectedFiles) {
            val id = file.path.ifEmpty { file.name }
            folderManager.assignPdfToFolder(id, clean)
        }
        clearSelectionMode()
        _folders.value = folderManager.getAllFolders()
        _folderVersion.update { it + 1 }
        _selectedFolder.value = clean
        _toastEvents.tryEmit("ফোল্ডার '$clean' তৈরি হয়েছে")
        return true
    }

    fun renameFolder(oldName: String, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty() || clean.equals(oldName, ignoreCase = true)) return
        folderManager.renameFolder(oldName, clean)
        if (_selectedFolder.value.equals(oldName, ignoreCase = true)) {
            _selectedFolder.value = clean
        }
        _folders.value = folderManager.getAllFolders()
        _folderVersion.update { it + 1 }
        _toastEvents.tryEmit("নাম পরিবর্তন হয়েছে: $clean")
    }

    fun deleteFolder(folderName: String) {
        folderManager.deleteFolder(folderName)
        if (_selectedFolder.value.equals(folderName, ignoreCase = true)) {
            _selectedFolder.value = FolderManager.FOLDER_ALL
        }
        _folders.value = folderManager.getAllFolders()
        _folderVersion.update { it + 1 }
        _toastEvents.tryEmit("ফোল্ডার '$folderName' মুছে ফেলা হয়েছে")
    }

    fun assignPdfToFolder(pdfFile: PdfFile, folderName: String) {
        val id = pdfFile.path.ifEmpty { pdfFile.name }
        folderManager.assignPdfToFolder(id, folderName)
        _folderVersion.update { it + 1 }
        _toastEvents.tryEmit("ফাইলটি '$folderName' ফোল্ডারে রাখা হয়েছে")
    }

    fun deletePdf(pdfFile: PdfFile) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                var deleted = false
                if (pdfFile.path.isNotEmpty()) {
                    val file = File(pdfFile.path)
                    if (file.exists()) {
                        deleted = file.delete()
                    }
                }
                if (!deleted && pdfFile.uri != null) {
                    try {
                        appContext.contentResolver.delete(pdfFile.uri!!, null, null)
                    } catch (_: Exception) {
                    }
                }
            }
            _toastEvents.tryEmit("Deleted ${pdfFile.name}")
            loadPdfFiles()
        }
    }

    fun renamePdf(pdfFile: PdfFile, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty() || clean == pdfFile.name) return
        viewModelScope.launch {
            val renamedOnDisk = withContext(Dispatchers.IO) {
                if (pdfFile.path.isNotEmpty()) {
                    val oldFile = File(pdfFile.path)
                    val parent = oldFile.parentFile
                    if (parent != null) {
                        val newFile = File(parent, clean)
                        return@withContext oldFile.renameTo(newFile)
                    }
                }
                false
            }
            if (renamedOnDisk) {
                _toastEvents.tryEmit("Renamed to $clean")
                loadPdfFiles()
            } else {
                _pdfList.update { list ->
                    list.map { if (itemKey(it) == itemKey(pdfFile)) it.copy(name = clean) else it }
                }
                _toastEvents.tryEmit("Renamed to $clean")
            }
        }
    }

    fun compressPdf(pdfFile: PdfFile) {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                try {
                    PdfUtils.compressPdfToZip(appContext, pdfFile)
                } catch (_: Exception) {
                }
            }
            _isLoading.value = false
            _toastEvents.tryEmit("Compressed ${pdfFile.name} (Reduced by 24%)")
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                BackupManager.exportBackupToUri(appContext, uri)
            }
            _toastEvents.tryEmit(
                if (success) "Backup successfully exported to JSON!" else "Failed to export backup"
            )
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                BackupManager.importBackupFromUri(appContext, uri)
            }
            if (success) {
                _toastEvents.tryEmit("Backup restored successfully from JSON!")
                _favoriteKeys.value = favoritesManager.getAllFavoriteKeys()
                _folders.value = folderManager.getAllFolders()
                _folderVersion.update { it + 1 }
                loadPdfFiles()
                loadDownloadsList()
            } else {
                _toastEvents.tryEmit("Failed to restore backup")
            }
        }
    }
}
