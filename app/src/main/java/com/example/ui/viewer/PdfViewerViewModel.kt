package com.example.ui.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.UrlPdfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

sealed interface PdfViewerUiState {
    data object Loading : PdfViewerUiState
    data class Ready(val totalPages: Int, val pdfName: String) : PdfViewerUiState
    data class CorruptOrFailed(val localFile: File?) : PdfViewerUiState
}

class PdfViewerViewModel : ViewModel() {

    private val rendererExecutor = Executors.newSingleThreadExecutor()
    private val rendererDispatcher = rendererExecutor.asCoroutineDispatcher()

    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    private val pageCache: LruCache<Int, Bitmap> = object : LruCache<Int, Bitmap>(
        ((Runtime.getRuntime().maxMemory() / 1024).toInt() / 6).coerceAtLeast(16 * 1024)
    ) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount / 1024
    }

    private val _uiState = MutableStateFlow<PdfViewerUiState>(PdfViewerUiState.Loading)
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    fun loadPdf(context: Context, pdfPath: String?, pdfUri: Uri?, pdfName: String) {
        viewModelScope.launch {
            _uiState.value = PdfViewerUiState.Loading
            val appCtx = context.applicationContext
            val result = withContext(rendererDispatcher) {
                closeInternal()
                pageCache.evictAll()

                val localFile = when {
                    !pdfPath.isNullOrEmpty() -> File(pdfPath)
                    pdfUri != null && "file".equals(pdfUri.scheme, ignoreCase = true) ->
                        pdfUri.path?.let { File(it) }
                    else -> null
                }

                if (localFile != null && localFile.exists() && !UrlPdfManager.isValidPdfFile(localFile)) {
                    return@withContext PdfViewerUiState.CorruptOrFailed(localFile)
                }

                try {
                    parcelFileDescriptor = when {
                        localFile != null && localFile.exists() ->
                            ParcelFileDescriptor.open(localFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        pdfUri != null && "file".equals(pdfUri.scheme, ignoreCase = true) && pdfUri.path != null ->
                            ParcelFileDescriptor.open(File(pdfUri.path!!), ParcelFileDescriptor.MODE_READ_ONLY)
                        pdfUri != null ->
                            appCtx.contentResolver.openFileDescriptor(pdfUri, "r")
                        !pdfPath.isNullOrEmpty() ->
                            ParcelFileDescriptor.open(File(pdfPath), ParcelFileDescriptor.MODE_READ_ONLY)
                        else -> null
                    }

                    val pfd = parcelFileDescriptor
                        ?: return@withContext PdfViewerUiState.CorruptOrFailed(localFile)
                    val renderer = PdfRenderer(pfd)
                    pdfRenderer = renderer
                    PdfViewerUiState.Ready(renderer.pageCount, pdfName)
                } catch (e: Exception) {
                    e.printStackTrace()
                    closeInternal()
                    PdfViewerUiState.CorruptOrFailed(localFile)
                }
            }
            _uiState.value = result
        }
    }

    fun getCachedPage(pageIndex: Int): Bitmap? = pageCache.get(pageIndex)

    suspend fun getOrRenderPage(pageIndex: Int, targetWidth: Int): Bitmap? {
        pageCache.get(pageIndex)?.let { return it }
        return withContext(rendererDispatcher) {
            pageCache.get(pageIndex)?.let { return@withContext it }
            val renderer = pdfRenderer ?: return@withContext null
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

            var page: PdfRenderer.Page? = null
            try {
                page = renderer.openPage(pageIndex)
                val safeWidth = targetWidth.coerceAtLeast(720)
                val ratio = page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                val bmpH = (safeWidth * ratio).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(safeWidth, bmpH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pageCache.put(pageIndex, bitmap)
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                try {
                    page?.close()
                } catch (_: Exception) {}
            }
        }
    }

    private fun closeInternal() {
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        pdfRenderer = null
        try {
            parcelFileDescriptor?.close()
        } catch (_: Exception) {}
        parcelFileDescriptor = null
    }

    override fun onCleared() {
        rendererExecutor.execute {
            closeInternal()
            pageCache.evictAll()
        }
        rendererExecutor.shutdown()
        super.onCleared()
    }
}
