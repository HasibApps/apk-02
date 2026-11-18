package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.notes.parseBengaliOrAsciiPageNumber
import com.example.ui.theme.PdfGalleryTheme

class AgentStartPageActivity : ComponentActivity() {

    private var agentDispatched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val currentPdfPage = intent.getIntExtra(EXTRA_CURRENT_PDF_PAGE, 0)
        val totalPdfPages = intent.getIntExtra(EXTRA_TOTAL_PDF_PAGES, 0)

        setContent {
            val isDark by ThemeManager.darkModeFlow.collectAsStateWithLifecycle()
            PdfGalleryTheme(darkTheme = isDark) {
                AgentStartPagePrompt(
                    currentPdfPage = currentPdfPage,
                    totalPdfPages = totalPdfPages,
                    onConfirm = { startPage ->
                        if (!agentDispatched) {
                            agentDispatched = true
                            LensNavigatorService.setBarVisible(true)
                            Handler(Looper.getMainLooper()).postDelayed({
                                LensNavigatorService.startAgentWithStartingPage(startPage)
                            }, 220L)
                        }
                        finish()
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    },
                    onDismiss = {
                        finish()
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        LensNavigatorService.setBarVisible(true)
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_CURRENT_PDF_PAGE = "agent_current_pdf_page"
        private const val EXTRA_TOTAL_PDF_PAGES = "agent_total_pdf_pages"

        @JvmStatic
        fun launch(context: Context, currentPdfPage: Int, totalPdfPages: Int) {
            val i = Intent(context, AgentStartPageActivity::class.java).apply {
                putExtra(EXTRA_CURRENT_PDF_PAGE, currentPdfPage)
                putExtra(EXTRA_TOTAL_PDF_PAGES, totalPdfPages)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(i)
        }
    }
}

@Composable
private fun AgentStartPagePrompt(
    currentPdfPage: Int,
    totalPdfPages: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BackHandler(onBack = onDismiss)

    val parsedPreview = remember(inputText) {
        parseBengaliOrAsciiPageNumber(inputText.trim())
    }

    fun trySubmit() {
        val parsed = parseBengaliOrAsciiPageNumber(inputText.trim())
        if (parsed <= 0) {
            errorText = "অনুগ্রহ করে শুরুর পৃষ্ঠা নম্বর লিখুন (যেমন: ১ বা ০১)"
            return
        }
        onConfirm(parsed)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .safeDrawingPadding()
            .imePadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF81C995).copy(alpha = 0.16f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_agent),
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "অটো এজেন্ট – শুরুর পৃষ্ঠা",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val pdfInfo = if (totalPdfPages > 0) {
                            "বর্তমান PDF: পৃষ্ঠা ${PdfUtils.toBengaliDigits((currentPdfPage + 1).toString())} / ${PdfUtils.toBengaliDigits(totalPdfPages.toString())}"
                        } else {
                            "বর্তমান PDF: পৃষ্ঠা ${PdfUtils.toBengaliDigits((currentPdfPage + 1).toString())}"
                        }
                        Text(
                            text = pdfInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "প্রতিটি কপি করা টেক্সট আনলিমিটেড নোটে যাওয়ার আগে [ পৃষ্ঠা : নং ] ফরমেটে যুক্ত হবে। কোথায় থেকে শুরু হবে তা নিচের বক্সে লিখুন:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        errorText = null
                    },
                    label = { Text("শুরুর পৃষ্ঠা নং (যেমন: ১ বা ০১)") },
                    placeholder = { Text("০১") },
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { msg ->
                        { Text(msg, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { trySubmit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("agent_start_page_input")
                )

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "আনলিমিটেড নোট ফরম্যাট প্রিভিউ:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                        val firstTag = if (parsedPreview > 0) {
                            "[ পৃষ্ঠা : ${PdfNotesManager.toBengaliDigits(parsedPreview)} ]"
                        } else {
                            "[ পৃষ্ঠা : ___ ]"
                        }
                        val secondTag = if (parsedPreview > 0) {
                            "[ পৃষ্ঠা : ${PdfNotesManager.toBengaliDigits(parsedPreview + 1)} ]"
                        } else {
                            "[ পৃষ্ঠা : ___ ]"
                        }
                        Text(
                            text = "$firstTag\n১ম কপি করা টেক্সট…\n\n$secondTag\n২য় কপি করা টেক্সট…",
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("agent_start_page_cancel")
                    ) {
                        Text("বাতিল")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { trySubmit() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("agent_start_page_confirm")
                    ) {
                        Text("Agent চালু করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
