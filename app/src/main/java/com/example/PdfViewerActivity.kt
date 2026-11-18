package com.example

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.notes.PdfNotesModal
import com.example.ui.theme.PdfGalleryTheme
import com.example.ui.viewer.PageCropDialog
import com.example.ui.viewer.PdfViewerScreen
import com.example.ui.viewer.PdfViewerUiState
import com.example.ui.viewer.PdfViewerViewModel
import java.io.File

class PdfViewerActivity : ComponentActivity() {

    private val viewModel: PdfViewerViewModel by viewModels()

    private var pdfUri: Uri? = null
    private var pdfPath: String? = null
    private var pdfName: String = "Document.pdf"

    private var pendingLensPage: Int = -1
    private var pendingLensImagePath: String? = null
    private var lensOverlayAskedOnce = false

    private var showOverlayPromptDialog by mutableStateOf(false)
    private var activeCropPageData by mutableStateOf<Pair<Int, Bitmap>?>(null)
    private var activeNotesJumpCallback by mutableStateOf<((Int) -> Unit)?>(null)
    private var showNotesModal by mutableStateOf(false)

    private val lensOverlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        launchLensNow(pendingLensPage, pendingLensImagePath)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#1E1E1E")),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#121212"))
        )
        super.onCreate(savedInstanceState)

        parseIntentData(intent)
        viewModel.loadPdf(this, pdfPath, pdfUri, pdfName)

        setContent {
            val isDark by ThemeManager.darkModeFlow.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            PdfGalleryTheme(darkTheme = isDark) {
                PdfViewerScreen(
                    uiState = uiState,
                    pdfDisplayName = pdfName,
                    loadPageBitmap = { idx, width -> viewModel.getOrRenderPage(idx, width) },
                    getCachedPage = { idx -> viewModel.getCachedPage(idx) },
                    onBack = { finish() },
                    onLensClick = { pageIndex, pageBitmap ->
                        val total = (uiState as? PdfViewerUiState.Ready)?.totalPages ?: 0
                        openGoogleLensWithBitmap(pageIndex, pageBitmap, total)
                    },
                    onCropClick = { pageIndex, pageBitmap ->
                        if (pageBitmap == null) {
                            Toast.makeText(this, "পেজ রেন্ডার করা যাচ্ছে না", Toast.LENGTH_SHORT).show()
                        } else {
                            activeCropPageData = pageIndex to pageBitmap
                        }
                    },
                    onNotesClick = { jumpCallback ->
                        activeNotesJumpCallback = jumpCallback
                        showNotesModal = true
                    },
                    onSharePdf = {
                        val file = PdfFile(0L, pdfName, pdfPath ?: "", pdfUri, 0L, System.currentTimeMillis())
                        PdfUtils.sharePdf(this, file)
                    },
                    onCorruptDelete = { finish() }
                )

                val cropPair = activeCropPageData
                val totalPages = (uiState as? PdfViewerUiState.Ready)?.totalPages ?: 1
                if (cropPair != null) {
                    PageCropDialog(
                        initialPageIndex = cropPair.first,
                        initialBitmap = cropPair.second,
                        totalPages = totalPages,
                        loadPageBitmap = { idx ->
                            val w = resources.displayMetrics.widthPixels.coerceAtLeast(1080)
                            viewModel.getCachedPage(idx) ?: viewModel.getOrRenderPage(idx, w)
                        },
                        onDismiss = { activeCropPageData = null }
                    )
                }

                if (showNotesModal) {
                    val identifier = if (!pdfPath.isNullOrEmpty()) pdfPath!! else pdfName
                    PdfNotesModal(
                        pdfId = identifier,
                        pdfName = pdfName,
                        onDismiss = { showNotesModal = false },
                        onJumpToPage = { page -> activeNotesJumpCallback?.invoke(page) }
                    )
                }

                if (showOverlayPromptDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showOverlayPromptDialog = false
                            launchLensNow(pendingLensPage, pendingLensImagePath)
                        },
                        title = { Text("Google Lens পৃষ্ঠা বার") },
                        text = {
                            Text(
                                "Google Lens এর উপরে ◀ ▶ বার দেখাতে 'অন্য অ্যাপের উপরে দেখানো' (Display over other apps) অনুমতি দিন। তাহলে অ্যাপে না ফিরেই Lens থেকে পরের/আগের পৃষ্ঠায় যেতে পারবেন।"
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                showOverlayPromptDialog = false
                                val i = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                                lensOverlayLauncher.launch(i)
                            }) {
                                Text("অনুমতি দিন")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showOverlayPromptDialog = false
                                launchLensNow(pendingLensPage, pendingLensImagePath)
                            }) {
                                Text("বার ছাড়া চালান")
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (LensNavigatorService.isRunning) {
            val stop = Intent(this, LensNavigatorService::class.java).apply {
                action = LensNavigatorService.ACTION_STOP
            }
            try {
                startService(stop)
            } catch (_: Exception) {}
        }
    }

    private fun parseIntentData(intent: Intent?) {
        if (intent != null) {
            if (intent.hasExtra(EXTRA_PDF_NAME)) {
                pdfName = intent.getStringExtra(EXTRA_PDF_NAME) ?: pdfName
            }
            if (intent.hasExtra(EXTRA_PDF_PATH)) {
                pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
            }
            if (intent.hasExtra(EXTRA_PDF_URI)) {
                val uriStr = intent.getStringExtra(EXTRA_PDF_URI)
                if (uriStr != null) pdfUri = Uri.parse(uriStr)
            } else if (intent.data != null) {
                pdfUri = intent.data
                pdfUri?.lastPathSegment?.let { pdfName = it }
            }
        }

        if (pdfUri == null && !pdfPath.isNullOrEmpty()) {
            val f = File(pdfPath!!)
            if (f.exists()) {
                pdfUri = Uri.fromFile(f)
                pdfName = f.name
            }
        }
    }

    private fun openGoogleLensWithBitmap(pageIndex: Int, pageBitmap: Bitmap?, totalPages: Int) {
        if (pageBitmap == null) {
            Toast.makeText(this, "পেজ রেন্ডার করা যাচ্ছে না", Toast.LENGTH_SHORT).show()
            return
        }
        val cachedImg = PdfUtils.saveBitmapToCache(this, pageBitmap, "lens_page_${pageIndex + 1}_")
        if (cachedImg == null) {
            Toast.makeText(this, "Google Lens চালু করতে সমস্যা", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this) &&
            !lensOverlayAskedOnce
        ) {
            lensOverlayAskedOnce = true
            pendingLensPage = pageIndex
            pendingLensImagePath = cachedImg.absolutePath
            showOverlayPromptDialog = true
            return
        }
        launchLensNow(pageIndex, cachedImg.absolutePath, totalPages)
    }

    private fun launchLensNow(pageIndex: Int, imagePath: String?, totalPages: Int = (viewModel.uiState.value as? PdfViewerUiState.Ready)?.totalPages ?: 0) {
        if (imagePath == null || pageIndex < 0) return
        val launched = PdfUtils.launchOfficialGoogleLens(this, imagePath)
        if (!launched) {
            val lensIntent = Intent(this, GoogleLensActivity::class.java).apply {
                putExtra(GoogleLensActivity.EXTRA_IMAGE_PATH, imagePath)
                putExtra(GoogleLensActivity.EXTRA_PAGE_NUMBER, pageIndex + 1)
            }
            startActivity(lensIntent)
            return
        }
        startLensNavigatorBar(pageIndex, totalPages)
    }

    private fun startLensNavigatorBar(pageIndex: Int, totalPages: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return
        try {
            val svc = Intent(this, LensNavigatorService::class.java).apply {
                if (!pdfPath.isNullOrEmpty() && File(pdfPath!!).exists()) {
                    putExtra(LensNavigatorService.EXTRA_PDF_PATH, pdfPath)
                }
                pdfUri?.let { putExtra(LensNavigatorService.EXTRA_PDF_URI, it.toString()) }
                putExtra(LensNavigatorService.EXTRA_PAGE_INDEX, pageIndex)
                putExtra(LensNavigatorService.EXTRA_TOTAL_PAGES, totalPages)
                putExtra(
                    LensNavigatorService.EXTRA_NOTE_ID,
                    if (!pdfPath.isNullOrEmpty()) pdfPath else pdfName
                )
                putExtra(LensNavigatorService.EXTRA_PDF_NAME, pdfName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc)
            } else {
                startService(svc)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val EXTRA_PDF_PATH = "extra_pdf_path"
        const val EXTRA_PDF_URI = "extra_pdf_uri"
        const val EXTRA_PDF_NAME = "extra_pdf_name"
    }
}
