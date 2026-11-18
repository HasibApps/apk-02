package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.notes.PdfNotesModal
import com.example.ui.theme.PdfGalleryTheme

class LensNoteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val noteId = intent.getStringExtra(EXTRA_NOTE_ID)
        val pdfName = intent.getStringExtra(EXTRA_PDF_NAME)

        if (savedInstanceState != null || noteId == null) {
            finish()
            return
        }

        setContent {
            val isDark by ThemeManager.darkModeFlow.collectAsStateWithLifecycle()
            PdfGalleryTheme(darkTheme = isDark) {
                PdfNotesModal(
                    pdfId = noteId,
                    pdfName = pdfName,
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        LensNavigatorService.setBarVisible(true)
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_NOTE_ID = "lens_note_id"
        private const val EXTRA_PDF_NAME = "lens_note_pdf_name"

        @JvmStatic
        fun launch(context: Context, noteId: String, pdfName: String?) {
            val i = Intent(context, LensNoteActivity::class.java).apply {
                putExtra(EXTRA_NOTE_ID, noteId)
                putExtra(EXTRA_PDF_NAME, pdfName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(i)
        }
    }
}
