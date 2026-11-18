package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.main.CreateFolderDialog
import com.example.ui.main.DeleteConfirmDialog
import com.example.ui.main.DeleteFolderConfirmDialog
import com.example.ui.main.ImportUrlDialog
import com.example.ui.main.MainMenuBottomSheet
import com.example.ui.main.MainScreen
import com.example.ui.main.MainViewModel
import com.example.ui.main.ManageFolderDialog
import com.example.ui.main.MoveToFolderDialog
import com.example.ui.main.PdfOptionsBottomSheet
import com.example.ui.main.PdfPropertiesDialog
import com.example.ui.main.RenamePdfDialog
import com.example.ui.theme.PdfGalleryTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var showMainMenuSheet by mutableStateOf(false)
    private var showImportUrlDialog by mutableStateOf(false)
    private var prefilledShareUrl by mutableStateOf<String?>(null)
    private var selectedImportCoverUri by mutableStateOf<Uri?>(null)
    private var targetItemIdForCoverChange: String? = null

    private var showCreateFolderDialog by mutableStateOf(false)
    private var showCreateFolderForSelectedDialog by mutableStateOf(false)
    private var pendingMovePdfAfterFolderCreate by mutableStateOf<PdfFile?>(null)
    private var managingFolderId by mutableStateOf<String?>(null)
    private var deletingFolderId by mutableStateOf<String?>(null)

    private var activeOptionsPdf by mutableStateOf<PdfFile?>(null)
    private var moveFolderPdf by mutableStateOf<PdfFile?>(null)
    private var renamePdfTarget by mutableStateOf<PdfFile?>(null)
    private var deletePdfTarget by mutableStateOf<PdfFile?>(null)
    private var propertiesPdfTarget by mutableStateOf<PdfFile?>(null)
    private var deleteUrlItemTarget by mutableStateOf<UrlPdfItem?>(null)

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.loadPdfFiles()
    }

    private val pickPdfLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            viewModel.importPdfFromUri(uri)
        }
    }

    private val pickImportCoverLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImportCoverUri = uri
        }
    }

    private val changeExistingCoverLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val itemId = targetItemIdForCoverChange
        targetItemIdForCoverChange = null
        if (uri != null && itemId != null) {
            viewModel.updateCoverForUrlItem(itemId, uri)
        }
    }

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri)
        }
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        stopFloatingBubbleIfRunning()
        checkStoragePermissionAndLoad()
        handleIncomingShareIntent(intent)

        setContent {
            val isDark by ThemeManager.darkModeFlow.collectAsStateWithLifecycle()
            val isDownloadsTab by viewModel.isDownloadsTab.collectAsStateWithLifecycle()
            val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
            val pdfList by viewModel.filteredPdfList.collectAsStateWithLifecycle()
            val allPdfs by viewModel.pdfList.collectAsStateWithLifecycle()
            val urlItems by viewModel.filteredUrlItems.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
            val folders by viewModel.folders.collectAsStateWithLifecycle()
            val selectedKeys by viewModel.selectedPdfKeys.collectAsStateWithLifecycle()
            val favoriteKeys by viewModel.favoriteKeys.collectAsStateWithLifecycle()
            val thumbnailCache by viewModel.thumbnailCache.collectAsStateWithLifecycle()
            val pageCountCache by viewModel.pageCountCache.collectAsStateWithLifecycle()
            val downloadProgressMap by viewModel.downloadProgressMap.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.toastEvents.collect { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }

            val folderCounts = folders.associateWith { viewModel.folderCount(it) }
            val favoritesCount = viewModel.folderCount(FolderManager.FOLDER_FAVORITES)

            PdfGalleryTheme(darkTheme = isDark) {
                MainScreen(
                    isDarkMode = isDark,
                    isDownloadsTab = isDownloadsTab,
                    isLoading = isLoading,
                    pdfList = pdfList,
                    totalPdfCount = allPdfs.size,
                    urlItems = urlItems,
                    searchQuery = searchQuery,
                    selectedFolder = selectedFolder,
                    folders = folders,
                    folderCounts = folderCounts,
                    favoritesCount = favoritesCount,
                    selectedKeys = selectedKeys,
                    favoriteKeys = favoriteKeys,
                    thumbnailCache = thumbnailCache,
                    pageCountCache = pageCountCache,
                    downloadProgressMap = downloadProgressMap,
                    itemKey = { viewModel.itemKey(it) },
                    favKey = { FavoritesManager.getPdfKey(it) },
                    onOpenMenu = { showMainMenuSheet = true },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onSelectFolder = { viewModel.selectFolder(it) },
                    onClearFolderFilter = { viewModel.clearFolderFilter() },
                    onLongClickFolder = { managingFolderId = it },
                    onCreateFolderClick = {
                        pendingMovePdfAfterFolderCreate = null
                        showCreateFolderDialog = true
                    },
                    onCreateFolderFromSelectedClick = { showCreateFolderForSelectedDialog = true },
                    onClearSelectionMode = { viewModel.clearSelectionMode() },
                    onEnsurePdfLoaded = { viewModel.lazyLoadThumbnailAndPageCount(it) },
                    onPdfClick = { pdf ->
                        if (selectedKeys.isNotEmpty()) {
                            viewModel.toggleSelection(pdf)
                        } else {
                            openPdfViewer(pdf)
                        }
                    },
                    onPdfLongClick = { pdf -> viewModel.toggleSelection(pdf) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onPdfMenuClick = { activeOptionsPdf = it },
                    onPdfDeleteClick = { deletePdfTarget = it },
                    onDownloadActionClick = { handleDownloadOrRead(it) },
                    onCancelDownloadClick = { viewModel.cancelDownload(it) },
                    onChangeCoverClick = { item ->
                        targetItemIdForCoverChange = item.id
                        changeExistingCoverLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onCopyUrlLinkClick = { item -> copyText("PDF Link", item.originalUrl, "Link copied") },
                    onDeleteUrlItemClick = { deleteUrlItemTarget = it },
                    onSwitchToHome = { viewModel.switchToHomeTab() },
                    onSwitchToDownloads = { viewModel.switchToDownloadsTab() },
                    onExportClick = {
                        exportBackupLauncher.launch("pdf_gallery_backup_${System.currentTimeMillis()}.json")
                    },
                    onRestoreClick = {
                        importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    onImportPdfClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                    onImportUrlClick = {
                        prefilledShareUrl = null
                        selectedImportCoverUri = null
                        showImportUrlDialog = true
                    },
                    onCreateSamplesClick = { viewModel.createSamples() }
                )

                if (showMainMenuSheet) {
                    MainMenuBottomSheet(
                        isDarkMode = isDark,
                        activeFolder = selectedFolder,
                        totalPdfCount = allPdfs.size,
                        favoritesCount = favoritesCount,
                        folders = folders,
                        folderCounts = folderCounts,
                        onToggleDarkMode = { checked ->
                            ThemeManager.setDarkMode(this@MainActivity, checked)
                        },
                        onOpenSettings = {
                            startActivity(Intent(this@MainActivity, PermissionsActivity::class.java))
                        },
                        onSelectFolder = { folder ->
                            viewModel.switchToHomeTab()
                            viewModel.selectFolder(folder)
                        },
                        onRequestCreateFolder = {
                            pendingMovePdfAfterFolderCreate = null
                            showCreateFolderDialog = true
                        },
                        onRequestDeleteFolder = { deletingFolderId = it },
                        onDismiss = { showMainMenuSheet = false }
                    )
                }

                activeOptionsPdf?.let { pdf ->
                    PdfOptionsBottomSheet(
                        pdfFile = pdf,
                        onMoveToFolder = { moveFolderPdf = pdf },
                        onOpenWith = { openWithExternalApp(pdf) },
                        onCut = { Toast.makeText(this, "Cut: \"${pdf.name}\"", Toast.LENGTH_SHORT).show() },
                        onCopy = { Toast.makeText(this, "Copied: \"${pdf.name}\"", Toast.LENGTH_SHORT).show() },
                        onDelete = { deletePdfTarget = pdf },
                        onRename = { renamePdfTarget = pdf },
                        onCompress = { viewModel.compressPdf(pdf) },
                        onShare = { PdfUtils.sharePdf(this, pdf) },
                        onCopyPath = {
                            val path = pdf.path.ifEmpty { pdf.uriString.ifEmpty { pdf.name } }
                            copyText("PDF Path", path, "Path copied to clipboard")
                        },
                        onCreateShortcut = { createDesktopShortcut(pdf) },
                        onProperties = { propertiesPdfTarget = pdf },
                        onDismiss = { activeOptionsPdf = null }
                    )
                }

                if (showImportUrlDialog) {
                    ImportUrlDialog(
                        prefilledUrl = prefilledShareUrl,
                        chosenCoverUri = selectedImportCoverUri,
                        existingItemCount = urlItems.size,
                        onPickCoverImage = {
                            pickImportCoverLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onConfirmImport = { rawUrl, title, coverUri ->
                            showImportUrlDialog = false
                            prefilledShareUrl = null
                            selectedImportCoverUri = null
                            viewModel.finalizeUrlImport(rawUrl, title, coverUri)
                        },
                        onDismiss = {
                            showImportUrlDialog = false
                            prefilledShareUrl = null
                            selectedImportCoverUri = null
                        }
                    )
                }

                if (showCreateFolderDialog) {
                    CreateFolderDialog(
                        onConfirm = { name ->
                            if (viewModel.createFolder(name, selectAfterCreate = true)) {
                                pendingMovePdfAfterFolderCreate?.let { targetPdf ->
                                    viewModel.assignPdfToFolder(targetPdf, name.trim())
                                    pendingMovePdfAfterFolderCreate = null
                                }
                                showCreateFolderDialog = false
                            }
                        },
                        onDismiss = {
                            pendingMovePdfAfterFolderCreate = null
                            showCreateFolderDialog = false
                        }
                    )
                }

                if (showCreateFolderForSelectedDialog) {
                    CreateFolderDialog(
                        title = "নতুন Folder তৈরি করুন",
                        subtitle = "নির্বাচিত ${selectedKeys.size}টি ফাইলের জন্য ফোল্ডারের নাম দিন:",
                        hint = "ফোল্ডারের নাম লিখুন (যেমন: জীববিজ্ঞান নোট)",
                        onConfirm = { name ->
                            if (viewModel.createFolderFromSelected(name)) {
                                showCreateFolderForSelectedDialog = false
                            }
                        },
                        onDismiss = { showCreateFolderForSelectedDialog = false }
                    )
                }

                managingFolderId?.let { folder ->
                    ManageFolderDialog(
                        folderId = folder,
                        onRename = { newName -> viewModel.renameFolder(folder, newName) },
                        onDelete = { viewModel.deleteFolder(folder) },
                        onDismiss = { managingFolderId = null }
                    )
                }

                deletingFolderId?.let { folder ->
                    DeleteFolderConfirmDialog(
                        folderName = folder,
                        onConfirm = { viewModel.deleteFolder(folder) },
                        onDismiss = { deletingFolderId = null }
                    )
                }

                moveFolderPdf?.let { pdf ->
                    MoveToFolderDialog(
                        folders = folders,
                        onSelectFolder = { chosen -> viewModel.assignPdfToFolder(pdf, chosen) },
                        onCreateNewFolder = {
                            pendingMovePdfAfterFolderCreate = pdf
                            showCreateFolderDialog = true
                        },
                        onDismiss = { moveFolderPdf = null }
                    )
                }

                renamePdfTarget?.let { pdf ->
                    RenamePdfDialog(
                        pdfFile = pdf,
                        onConfirm = { newName -> viewModel.renamePdf(pdf, newName) },
                        onDismiss = { renamePdfTarget = null }
                    )
                }

                deletePdfTarget?.let { pdf ->
                    DeleteConfirmDialog(
                        title = getString(R.string.delete),
                        message = "Are you sure you want to delete \"${pdf.name}\"?",
                        onConfirm = { viewModel.deletePdf(pdf) },
                        onDismiss = { deletePdfTarget = null }
                    )
                }

                deleteUrlItemTarget?.let { item ->
                    DeleteConfirmDialog(
                        title = "Delete PDF",
                        message = "Are you sure you want to delete '${item.title}'?",
                        onConfirm = { viewModel.deleteUrlItem(item) },
                        onDismiss = { deleteUrlItemTarget = null }
                    )
                }

                propertiesPdfTarget?.let { pdf ->
                    PdfPropertiesDialog(
                        pdfFile = pdf,
                        onDismiss = { propertiesPdfTarget = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null &&
            ("text/plain" == type || type.startsWith("text/"))
        ) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
            if (!sharedText.isNullOrEmpty()) {
                Toast.makeText(this, "Shared URL received: $sharedText", Toast.LENGTH_LONG).show()
                viewModel.switchToDownloadsTab()
                prefilledShareUrl = sharedText
                selectedImportCoverUri = null
                showImportUrlDialog = true
            }
        }
    }

    private fun checkStoragePermissionAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            viewModel.loadPdfFiles()
        } else {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.loadPdfFiles()
            } else {
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun stopFloatingBubbleIfRunning() {
        if (FloatingBubbleService.isServiceRunning) {
            try {
                val serviceIntent = Intent(this, FloatingBubbleService::class.java).apply {
                    action = FloatingBubbleService.ACTION_STOP
                }
                stopService(serviceIntent)
                FloatingBubbleService.isServiceRunning = false
            } catch (_: Exception) {
            }
        }
    }

    private fun openPdfViewer(pdfFile: PdfFile) {
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra(PdfViewerActivity.EXTRA_PDF_PATH, pdfFile.path)
            putExtra(PdfViewerActivity.EXTRA_PDF_NAME, pdfFile.name)
            pdfFile.uri?.let { putExtra(PdfViewerActivity.EXTRA_PDF_URI, it.toString()) }
        }
        startActivity(intent)
    }

    private fun handleDownloadOrRead(item: UrlPdfItem) {
        val localPath = item.localFilePath
        if (item.isDownloaded && !localPath.isNullOrEmpty()) {
            val file = File(localPath)
            if (file.exists() && UrlPdfManager.isValidPdfFile(file)) {
                val uri = FileProvider.getUriForFile(this, PdfUtils.fileProviderAuthority(this), file)
                val intent = Intent(this, PdfViewerActivity::class.java).apply {
                    putExtra(PdfViewerActivity.EXTRA_PDF_PATH, file.absolutePath)
                    putExtra(PdfViewerActivity.EXTRA_PDF_NAME, item.title)
                    putExtra(PdfViewerActivity.EXTRA_PDF_URI, uri.toString())
                }
                startActivity(intent)
                return
            } else {
                viewModel.markItemCorruptAndRedownload(item)
                return
            }
        }
        viewModel.startOrResumeDownload(item)
    }

    private fun openWithExternalApp(pdfFile: PdfFile) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfFile.uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.open_with)))
        } catch (_: Exception) {
            Toast.makeText(this, "No external application found for PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyText(label: String, text: String, toastMessage: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createDesktopShortcut(pdfFile: PdfFile) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            val shortcutIntent = Intent(this, PdfViewerActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(PdfViewerActivity.EXTRA_PDF_PATH, pdfFile.path)
                putExtra(PdfViewerActivity.EXTRA_PDF_NAME, pdfFile.name)
                pdfFile.uri?.let { putExtra(PdfViewerActivity.EXTRA_PDF_URI, it.toString()) }
            }
            val pinShortcutInfo = ShortcutInfoCompat.Builder(this, "pdf_${pdfFile.id}")
                .setShortLabel(pdfFile.name)
                .setIcon(IconCompat.createWithResource(this, R.drawable.ic_pdf_file))
                .setIntent(shortcutIntent)
                .build()
            ShortcutManagerCompat.requestPinShortcut(this, pinShortcutInfo, null)
            Toast.makeText(this, "Pinned \"${pdfFile.name}\" to Home Screen!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Shortcut requested for Home Screen", Toast.LENGTH_SHORT).show()
        }
    }
}
