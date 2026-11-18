package com.example.ui.notes

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.FormulaHelper
import com.example.PdfNotesManager
import com.example.PdfUtils
import com.example.TextHistoryManager
import com.example.ThemeManager
import com.example.ui.theme.BengaliSerifFamily
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.abs

private val PAGE_REF_REGEX = Regex("(?:পৃষ্ঠা|[Pp]age|[Pp]\\.)\\s*[:=\\-]??\\s*([0-9০-৯]+)")
private val SCIENTIFIC_SYMBOLS = listOf(
    "H₂O", "CO₂", "→", "⇌", "↑", "↓", "⁺", "⁻", "₂", "₃", "₄",
    "²", "³", "√", "π", "θ", "λ", "Δ", "α", "β", "ω", "±", "×", "÷",
    "∫", "∑", "∞", "≠", "≈", "½", "°", "E=mc²", "F=ma", "v²=u²+2as", "s=ut+½at²"
)

private fun shouldAutoFormatFormula(text: String): Boolean {
    return text.contains("^") || text.contains("->") || text.contains("⇌") ||
        text.contains("_") || text.contains("\\") || text.contains("H2O") ||
        text.contains("CO2") || text.contains("H2SO4") || text.contains("<=>") ||
        text.contains("-->") || text.contains("1/2") || text.contains("+/-")
}

private class PageRefVisualTransformation(private val linkColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = buildAnnotatedString {
            append(text.text)
            for (match in PAGE_REF_REGEX.findAll(text.text)) {
                val numGroup = match.groups[1] ?: continue
                val pageNum = parseBengaliOrAsciiPageNumber(numGroup.value)
                if (pageNum > 0) {
                    addStyle(
                        style = SpanStyle(
                            color = linkColor,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = numGroup.range.first,
                        end = numGroup.range.last + 1
                    )
                }
            }
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

@Composable
fun PdfNotesModal(
    pdfId: String,
    pdfName: String?,
    onDismiss: () -> Unit,
    onJumpToPage: ((Int) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        PdfNotesScreenContent(
            pdfId = pdfId,
            pdfName = pdfName,
            onDismiss = onDismiss,
            onJumpToPage = onJumpToPage
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfNotesScreenContent(
    pdfId: String,
    pdfName: String?,
    onDismiss: () -> Unit,
    onJumpToPage: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notesManager = remember(pdfId) { PdfNotesManager(context) }
    val isDark = ThemeManager.isDarkMode(context)
    val linkColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)
    val pageRefTransformation = remember(linkColor) { PageRefVisualTransformation(linkColor) }

    var showUnlimitedTab by remember { mutableStateOf(true) }
    var isFormulaViewActive by remember { mutableStateOf(false) }
    var currentFontSize by remember { mutableFloatStateOf(notesManager.getNoteFontSize()) }
    var isTwoColumn by remember { mutableStateOf(notesManager.isTwoColumnMode()) }

    val historyManager = remember(pdfId) {
        TextHistoryManager(notesManager.getUnlimitedNoteForPdf(pdfId))
    }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }

    var unlimitedField by remember(pdfId) {
        val initial = notesManager.getUnlimitedNoteForPdf(pdfId)
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
    var col1Field by remember(pdfId) {
        val c1 = notesManager.getNoteCol1ForPdf(pdfId)
        mutableStateOf(TextFieldValue(c1, TextRange(c1.length)))
    }
    var col2Field by remember(pdfId) {
        val c2 = notesManager.getNoteCol2ForPdf(pdfId)
        mutableStateOf(TextFieldValue(c2, TextRange(c2.length)))
    }
    var singleColField by remember(pdfId) {
        val merged = notesManager.getNoteCol1ForPdf(pdfId) + "\n\n" + notesManager.getNoteCol2ForPdf(pdfId)
        mutableStateOf(TextFieldValue(merged, TextRange(merged.length)))
    }
    var focusedChapterField by remember { mutableIntStateOf(1) } // 1 = col1, 2 = col2, 3 = single

    val unlimitedScrollState = rememberScrollState()
    val chapterScrollState = rememberScrollState()

    var showDownloadTxtDialog by remember { mutableStateOf(false) }
    var showStartingPageDialog by remember { mutableStateOf(false) }
    var showResetStartingPageDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showReset10Dialog by remember { mutableStateOf(false) }

    fun updateFontSize(newSize: Float) {
        val clamped = newSize.coerceIn(9.5f, 22.0f)
        currentFontSize = clamped
        notesManager.saveNoteFontSize(clamped)
    }

    fun saveAllNotes() {
        notesManager.saveUnlimitedNoteForPdf(pdfId, unlimitedField.text)
        if (isTwoColumn) {
            notesManager.saveTwoColumnNotes(pdfId, col1Field.text, col2Field.text)
        } else {
            notesManager.saveNoteForPdf(pdfId, singleColField.text)
        }
    }

    fun updateUnlimitedText(newValue: TextFieldValue, recordHistory: Boolean = true) {
        var finalValue = newValue
        if (shouldAutoFormatFormula(newValue.text)) {
            val formatted = FormulaHelper.formatScientificText(newValue.text)
            if (formatted != newValue.text) {
                val diff = formatted.length - newValue.text.length
                val cursor = (newValue.selection.end + diff).coerceIn(0, formatted.length)
                finalValue = TextFieldValue(formatted, TextRange(cursor))
            }
        }
        unlimitedField = finalValue
        notesManager.saveUnlimitedNoteForPdf(pdfId, finalValue.text)
        if (recordHistory) {
            historyManager.recordChange(finalValue.text, finalValue.selection.end)
            canUndo = historyManager.canUndo
            canRedo = historyManager.canRedo
        }
    }

    fun insertFormattedPageTag(pageNum: Int) {
        val bn = PdfNotesManager.toBengaliDigits(pageNum)
        val cur = unlimitedField.text
        val start = unlimitedField.selection.min.coerceIn(0, cur.length)
        val end = unlimitedField.selection.max.coerceIn(0, cur.length)
        val tag = if (start == 0 && cur.isEmpty()) "(পৃষ্ঠা= $bn)\n" else "\n\n(পৃষ্ঠা= $bn)\n"
        val updated = cur.substring(0, start) + tag + cur.substring(end)
        val newCursor = start + tag.length
        updateUnlimitedText(TextFieldValue(updated, TextRange(newCursor)))
        scope.launch {
            unlimitedScrollState.animateScrollTo(unlimitedScrollState.maxValue)
        }
        Toast.makeText(context, "(পৃষ্ঠা= $bn) যুক্ত হয়েছে", Toast.LENGTH_SHORT).show()
    }

    fun handleTapForPageLink(offset: Int, text: String): Boolean {
        for (match in PAGE_REF_REGEX.findAll(text)) {
            if (offset in match.range.first..(match.range.last + 1)) {
                val pageNum = parseBengaliOrAsciiPageNumber(match.groupValues[1])
                if (pageNum > 0) {
                    onJumpToPage?.invoke(pageNum)
                    onDismiss()
                    return true
                }
            }
        }
        return false
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("notes_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Notes")
                    }
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Notes",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (pdfName != null) "নোট: ${PdfUtils.cleanPdfName(pdfName)}" else "নোট ও ইনডেক্স",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { showClearConfirmDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier.testTag("notes_clear_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("মুছুন", color = Color(0xFFE53935), fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            saveAllNotes()
                            Toast.makeText(context, "✓ সকল নোট সংরক্ষণ করা হয়েছে", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("notes_save_button")
                    ) {
                        Text("সেভ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tab Switcher
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val primary = MaterialTheme.colorScheme.primary
                    val onPrimary = MaterialTheme.colorScheme.onPrimary
                    val secText = MaterialTheme.colorScheme.onSurfaceVariant

                    if (showUnlimitedTab) {
                        Button(
                            onClick = { showUnlimitedTab = true },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = onPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("notes_tab_unlimited")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("আনলিমিটেড নোট", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { showUnlimitedTab = false },
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("notes_tab_chapters")
                        ) {
                            Icon(Icons.Default.ViewColumn, contentDescription = null, tint = secText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("অধ্যায় ইনডেক্স (২ কলাম)", color = secText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showUnlimitedTab = true },
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("notes_tab_unlimited")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = secText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("আনলিমিটেড নোট", color = secText, fontSize = 12.sp, maxLines = 1)
                        }
                        Button(
                            onClick = { showUnlimitedTab = false },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = onPrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("notes_tab_chapters")
                        ) {
                            Icon(Icons.Default.ViewColumn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("অধ্যায় ইনডেক্স (২ কলাম)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            if (showUnlimitedTab) {
                // Toolbar Strip 1
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                historyManager.undo()?.let { restored ->
                                    unlimitedField = TextFieldValue(restored.text, TextRange(restored.cursorPosition))
                                    notesManager.saveUnlimitedNoteForPdf(pdfId, restored.text)
                                    canUndo = historyManager.canUndo
                                    canRedo = historyManager.canRedo
                                }
                            },
                            enabled = canUndo,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("notes_undo_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(
                            onClick = {
                                historyManager.redo()?.let { restored ->
                                    unlimitedField = TextFieldValue(restored.text, TextRange(restored.cursorPosition))
                                    notesManager.saveUnlimitedNoteForPdf(pdfId, restored.text)
                                    canUndo = historyManager.canUndo
                                    canRedo = historyManager.canRedo
                                }
                            },
                            enabled = canRedo,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("notes_redo_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                        }

                        VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 4.dp))

                        TextButton(
                            onClick = {
                                scope.launch { unlimitedScrollState.animateScrollTo(0) }
                                unlimitedField = unlimitedField.copy(selection = TextRange(0))
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.testTag("notes_scroll_top_button")
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("সবার উপরে", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = {
                                scope.launch { unlimitedScrollState.animateScrollTo(unlimitedScrollState.maxValue) }
                                unlimitedField = unlimitedField.copy(selection = TextRange(unlimitedField.text.length))
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.testTag("notes_scroll_bottom_button")
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("সবার নিচে", fontSize = 11.sp)
                        }

                        VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 4.dp))

                        Button(
                            onClick = {
                                if (unlimitedField.text.trim().isEmpty()) {
                                    Toast.makeText(context, "কপি করার জন্য কোনো টেক্সট নেই", Toast.LENGTH_SHORT).show()
                                } else {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cb?.setPrimaryClip(ClipData.newPlainText("PDF Unlimited Note", unlimitedField.text))
                                    Toast.makeText(context, "✓ সকল নোট কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("notes_copy_all_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.width(6.dp))

                        Button(
                            onClick = {
                                if (unlimitedField.text.trim().isEmpty()) {
                                    Toast.makeText(context, "ডাউনলোড করার মতো কোনো নোট লেখা হয়নি", Toast.LENGTH_SHORT).show()
                                } else {
                                    showDownloadTxtDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B), contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("notes_download_txt_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ডাউনলোড", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.width(6.dp))

                        Button(
                            onClick = { isFormulaViewActive = !isFormulaViewActive },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = Color(0xFF1565C0)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("notes_toggle_formula_button")
                        ) {
                            Icon(
                                imageVector = if (isFormulaViewActive) Icons.Default.Edit else Icons.Default.Functions,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isFormulaViewActive) "এডিটর ভিউ" else "সমীকরণ ভিউ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.width(6.dp))

                        FontSizePill(
                            fontSize = currentFontSize,
                            onDecrease = { if (currentFontSize > 9.5f) updateFontSize(currentFontSize - 1f) },
                            onIncrease = { if (currentFontSize < 22f) updateFontSize(currentFontSize + 1f) }
                        )
                    }
                }

                // Quick Scientific Symbol Bar
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (sym in SCIENTIFIC_SYMBOLS) {
                            TextButton(
                                onClick = {
                                    if (isFormulaViewActive) isFormulaViewActive = false
                                    val cur = unlimitedField.text
                                    val s = unlimitedField.selection.min.coerceIn(0, cur.length)
                                    val e = unlimitedField.selection.max.coerceIn(0, cur.length)
                                    val updated = cur.substring(0, s) + sym + cur.substring(e)
                                    updateUnlimitedText(TextFieldValue(updated, TextRange(s + sym.length)))
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(sym, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                // Content Area: Editor or KaTeX Formula Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isFormulaViewActive) {
                        val html = remember(unlimitedField.text, isDark) {
                            FormulaHelper.generateHtmlForFormulas(unlimitedField.text, isDark)
                        }
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                }
                            },
                            update = { webView ->
                                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(unlimitedScrollState)
                                .padding(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 320.dp)
                            ) {
                                NoteEditorTextField(
                                    value = unlimitedField,
                                    onValueChange = { updateUnlimitedText(it) },
                                    hint = "এখানে আনলিমিটেড নোট লিখুন বা পেস্ট করুন...\nসকল গণিত, রসায়ন ও পদার্থবিজ্ঞান সূত্রের সমীকরণ স্বয়ংক্রিয়ভাবে সাপোর্ট করে।",
                                    fontSizeSp = currentFontSize,
                                    transformation = pageRefTransformation,
                                    onTapOffset = { offset -> handleTapForPageLink(offset, unlimitedField.text) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                        .testTag("unlimited_notes_editor")
                                )
                            }
                        }
                    }
                }

                // Bottom Bar for Unlimited Note
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1976D2),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("notes_add_page_button")
                                .combinedClickable(
                                    onClick = {
                                        val lastPage = notesManager.getLastUnlimitedPageForPdf(pdfId)
                                        if (lastPage <= 0) {
                                            showStartingPageDialog = true
                                        } else {
                                            val nextPage = lastPage + 1
                                            insertFormattedPageTag(nextPage)
                                            notesManager.saveLastUnlimitedPageForPdf(pdfId, nextPage)
                                        }
                                    },
                                    onLongClick = { showResetStartingPageDialog = true }
                                )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("(+) পৃষ্ঠা", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                val current = unlimitedField.text
                                val formatted = FormulaHelper.formatScientificText(current)
                                if (formatted != current) {
                                    updateUnlimitedText(TextFieldValue(formatted, TextRange(formatted.length)))
                                    Toast.makeText(context, "✓ রসায়ন ও পদার্থ সূত্র সমীকরণ ফরম্যাট হয়েছে", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "সকল সূত্র ইতোমধ্যে সঠিক ফরম্যাটে রয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("notes_smart_format_button")
                        ) {
                            Text("✨ সূত্র ফরম্যাট", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // CHAPTER INDEX PAGE
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FontSizePill(
                            fontSize = currentFontSize,
                            onDecrease = { if (currentFontSize > 9.5f) updateFontSize(currentFontSize - 1f) },
                            onIncrease = { if (currentFontSize < 22f) updateFontSize(currentFontSize + 1f) }
                        )
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                val nextTwoCol = !isTwoColumn
                                isTwoColumn = nextTwoCol
                                notesManager.setTwoColumnMode(nextTwoCol)
                                if (!nextTwoCol) {
                                    val c1 = col1Field.text.trim()
                                    val c2 = col2Field.text.trim()
                                    val merged = c1 + if (c2.isNotEmpty()) "\n\n$c2" else ""
                                    singleColField = TextFieldValue(merged, TextRange(merged.length))
                                    notesManager.saveNoteForPdf(pdfId, merged)
                                } else {
                                    val text = singleColField.text
                                    if (text.contains(PdfNotesManager.COLUMN_SPLIT_DELIMITER)) {
                                        val parts = text.split(Pattern.quote(PdfNotesManager.COLUMN_SPLIT_DELIMITER).toRegex(), 2)
                                        col1Field = TextFieldValue(parts[0], TextRange(parts[0].length))
                                        val p2 = if (parts.size > 1) parts[1] else ""
                                        col2Field = TextFieldValue(p2, TextRange(p2.length))
                                    } else {
                                        var idx = text.indexOf("অধ্যায়:০৬")
                                        if (idx == -1) idx = text.indexOf("অধ্যায়:06")
                                        if (idx != -1) {
                                            val p1 = text.substring(0, idx).trim()
                                            val p2 = text.substring(idx).trim()
                                            col1Field = TextFieldValue(p1, TextRange(p1.length))
                                            col2Field = TextFieldValue(p2, TextRange(p2.length))
                                        } else {
                                            col1Field = TextFieldValue(text, TextRange(text.length))
                                            if (col2Field.text.isEmpty()) {
                                                val def2 = PdfNotesManager.generateChapters(6, 5)
                                                col2Field = TextFieldValue(def2, TextRange(def2.length))
                                            }
                                        }
                                    }
                                    notesManager.saveTwoColumnNotes(pdfId, col1Field.text, col2Field.text)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("notes_toggle_column_button")
                        ) {
                            Icon(
                                imageVector = if (isTwoColumn) Icons.Default.ViewColumn else Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isTwoColumn) "২ কলাম" else "১ কলাম", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = { showReset10Dialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.testTag("notes_reset_10_button")
                        ) {
                            Text("১০ অধ্যায়", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(chapterScrollState)
                        .padding(8.dp)
                ) {
                    if (isTwoColumn) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "কলাম ০১ (অধ্যায় ০১-০৫)",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    NoteEditorTextField(
                                        value = col1Field,
                                        onValueChange = {
                                            col1Field = it
                                            notesManager.saveTwoColumnNotes(pdfId, it.text, col2Field.text)
                                        },
                                        hint = "কলাম ১ নোট লিখুন...",
                                        fontSizeSp = currentFontSize,
                                        transformation = pageRefTransformation,
                                        onTapOffset = { offset -> handleTapForPageLink(offset, col1Field.text) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { if (it.isFocused) focusedChapterField = 1 }
                                            .testTag("chapter_col1_editor")
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 6.dp)
                                    .width(1.5.dp)
                                    .background(Color(0xFFCBD5E1))
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "কলাম ০২ (অধ্যায় ০৬-১০)",
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    NoteEditorTextField(
                                        value = col2Field,
                                        onValueChange = {
                                            col2Field = it
                                            notesManager.saveTwoColumnNotes(pdfId, col1Field.text, it.text)
                                        },
                                        hint = "কলাম ২ নোট লিখুন...",
                                        fontSizeSp = currentFontSize,
                                        transformation = pageRefTransformation,
                                        onTapOffset = { offset -> handleTapForPageLink(offset, col2Field.text) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { if (it.isFocused) focusedChapterField = 2 }
                                            .testTag("chapter_col2_editor")
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 260.dp)
                        ) {
                            NoteEditorTextField(
                                value = singleColField,
                                onValueChange = {
                                    singleColField = it
                                    notesManager.saveNoteForPdf(pdfId, it.text)
                                },
                                hint = "নোট লিখুন...",
                                fontSizeSp = currentFontSize,
                                transformation = pageRefTransformation,
                                onTapOffset = { offset -> handleTapForPageLink(offset, singleColField.text) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                    .onFocusChanged { if (it.isFocused) focusedChapterField = 3 }
                                    .testTag("chapter_single_editor")
                            )
                        }
                    }
                }

                // Bottom Bar for Chapter Index
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val combined = "${col1Field.text}\n${col2Field.text}\n${singleColField.text}"
                                val count = Regex("অধ্যায়\\s*[:：]\\s*([০-৯0-9]+)").findAll(combined).count()
                                val nextCh = (count + 1).coerceAtLeast(2)
                                val tmpl = PdfNotesManager.generateChapterTemplate(nextCh)
                                if (isTwoColumn) {
                                    if (focusedChapterField == 1) {
                                        val cur = col1Field.text
                                        val upd = if (cur.trim().isEmpty()) tmpl.trim() else cur + tmpl
                                        col1Field = TextFieldValue(upd, TextRange(upd.length))
                                    } else {
                                        val cur = col2Field.text
                                        val upd = if (cur.trim().isEmpty()) tmpl.trim() else cur + tmpl
                                        col2Field = TextFieldValue(upd, TextRange(upd.length))
                                    }
                                    notesManager.saveTwoColumnNotes(pdfId, col1Field.text, col2Field.text)
                                } else {
                                    val cur = singleColField.text
                                    val upd = if (cur.trim().isEmpty()) tmpl.trim() else cur + tmpl
                                    singleColField = TextFieldValue(upd, TextRange(upd.length))
                                    notesManager.saveNoteForPdf(pdfId, upd)
                                }
                                scope.launch { chapterScrollState.animateScrollTo(chapterScrollState.maxValue) }
                                Toast.makeText(context, "অধ্যায় $nextCh যোগ করা হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("notes_add_chapter_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("(+) নতুন অধ্যায়", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val insertText = " পৃষ্ঠা= "
                                fun insertInto(tf: TextFieldValue): TextFieldValue {
                                    val s = tf.selection.min.coerceIn(0, tf.text.length)
                                    val e = tf.selection.max.coerceIn(0, tf.text.length)
                                    val upd = tf.text.substring(0, s) + insertText + tf.text.substring(e)
                                    return TextFieldValue(upd, TextRange(s + insertText.length))
                                }
                                if (isTwoColumn) {
                                    if (focusedChapterField == 2) {
                                        col2Field = insertInto(col2Field)
                                    } else {
                                        col1Field = insertInto(col1Field)
                                    }
                                    notesManager.saveTwoColumnNotes(pdfId, col1Field.text, col2Field.text)
                                } else {
                                    singleColField = insertInto(singleColField)
                                    notesManager.saveNoteForPdf(pdfId, singleColField.text)
                                }
                                Toast.makeText(context, "পৃষ্ঠা= যোগ করা হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2), contentColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("notes_add_chapter_page_tag_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("(+) পৃষ্ঠা=", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDownloadTxtDialog) {
        DownloadNoteTxtDialog(
            pdfName = pdfName,
            noteContent = unlimitedField.text,
            onDismiss = { showDownloadTxtDialog = false }
        )
    }

    if (showStartingPageDialog) {
        StartingPageInputDialog(
            title = "(+) পৃষ্ঠা নম্বর নির্ধারণ করুন",
            subtitle = "শুরুর পৃষ্ঠা নম্বরটি লিখুন (যেমন: ১০০ বা ১১)। এরপর থেকে স্বয়ংক্রিয়ভাবে পরবর্তী সংখ্যা যুক্ত হবে:",
            initialValue = "১০০",
            confirmLabel = "যুক্ত করুন",
            onConfirm = { pageNum ->
                showStartingPageDialog = false
                insertFormattedPageTag(pageNum)
                notesManager.saveLastUnlimitedPageForPdf(pdfId, pageNum)
            },
            onDismiss = { showStartingPageDialog = false }
        )
    }

    if (showResetStartingPageDialog) {
        val currentPage = notesManager.getLastUnlimitedPageForPdf(pdfId)
        val msg = if (currentPage > 0) {
            "বর্তমান পৃষ্ঠা ক্রম: ${PdfNotesManager.toBengaliDigits(currentPage)}। নতুন শুরুর পৃষ্ঠা নম্বর লিখুন:"
        } else {
            "শুরুর পৃষ্ঠা নম্বর লিখুন (যেমন: ১০০ বা ১১):"
        }
        StartingPageInputDialog(
            title = "পৃষ্ঠা নম্বর পরিবর্তন / রিসেট",
            subtitle = msg,
            initialValue = if (currentPage > 0) currentPage.toString() else "১০০",
            confirmLabel = "আপডেট করুন",
            onConfirm = { pageNum ->
                showResetStartingPageDialog = false
                notesManager.saveLastUnlimitedPageForPdf(pdfId, pageNum)
                Toast.makeText(
                    context,
                    "পৃষ্ঠা ক্রম ${PdfNotesManager.toBengaliDigits(pageNum)} থেকে শুরু হবে",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showResetStartingPageDialog = false }
        )
    }

    if (showClearConfirmDialog) {
        ClearNotesConfirmDialog(
            isUnlimitedTab = showUnlimitedTab,
            onConfirm = {
                showClearConfirmDialog = false
                if (showUnlimitedTab) {
                    unlimitedField = TextFieldValue("")
                    notesManager.saveUnlimitedNoteForPdf(pdfId, "")
                    notesManager.clearPastedPages(pdfId)
                    Toast.makeText(context, "আনলিমিটেড নোট মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                } else {
                    notesManager.clearNoteForPdf(pdfId)
                    col1Field = TextFieldValue("")
                    col2Field = TextFieldValue("")
                    singleColField = TextFieldValue("")
                    Toast.makeText(context, "অধ্যায় নোট মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showClearConfirmDialog = false }
        )
    }

    if (showReset10Dialog) {
        ResetChaptersConfirmDialog(
            onConfirm = {
                showReset10Dialog = false
                val defCol1 = PdfNotesManager.generateChapters(1, 5)
                val defCol2 = PdfNotesManager.generateChapters(6, 5)
                col1Field = TextFieldValue(defCol1, TextRange(defCol1.length))
                col2Field = TextFieldValue(defCol2, TextRange(defCol2.length))
                singleColField = TextFieldValue("$defCol1\n\n$defCol2")
                notesManager.saveTwoColumnNotes(pdfId, defCol1, defCol2)
                Toast.makeText(context, "১০টি অধ্যায় তৈরি হয়েছে", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showReset10Dialog = false }
        )
    }
}

@Composable
private fun FontSizePill(
    fontSize: Float,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.height(32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(30.dp)
                    .fillMaxHeight()
                    .clickable(onClick = onDecrease)
            ) {
                Text("A-", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            VerticalDivider(modifier = Modifier.height(14.dp))
            Text(
                text = String.format(Locale.getDefault(), "%.0fsp", fontSize),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 5.dp)
            )
            VerticalDivider(modifier = Modifier.height(14.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(30.dp)
                    .fillMaxHeight()
                    .clickable(onClick = onIncrease)
            ) {
                Text("A+", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NoteEditorTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    hint: String,
    fontSizeSp: Float,
    transformation: VisualTransformation,
    onTapOffset: (Int) -> Boolean,
    modifier: Modifier = Modifier
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier) {
        if (value.text.isEmpty()) {
            Text(
                text = hint,
                fontFamily = BengaliSerifFamily,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp + 6f).sp,
                color = hintColor
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontFamily = BengaliSerifFamily,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp + 6f).sp,
                color = textColor
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = transformation,
            onTextLayout = { layoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(layoutResult, value.text) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.pressed }
                                ?: continue
                            val downPos = down.position
                            val downTime = System.currentTimeMillis()
                            while (true) {
                                val next = awaitPointerEvent(PointerEventPass.Initial)
                                val ch = next.changes.firstOrNull { it.id == down.id } ?: break
                                if (!ch.pressed) {
                                    val elapsed = System.currentTimeMillis() - downTime
                                    val moved = abs(ch.position.x - downPos.x) + abs(ch.position.y - downPos.y)
                                    if (elapsed < 320 && moved < 24f) {
                                        layoutResult?.let { lr ->
                                            val off = lr.getOffsetForPosition(ch.position)
                                            onTapOffset(off)
                                        }
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
        )
    }
}
