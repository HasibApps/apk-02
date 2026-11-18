package com.example.ui.notes

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.PdfNotesManager
import com.example.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

fun parseBengaliOrAsciiPageNumber(str: String?): Int {
    if (str.isNullOrEmpty()) return -1
    val sb = StringBuilder()
    for (c in str) {
        when (c) {
            in '0'..'9' -> sb.append(c)
            '০' -> sb.append('0')
            '১' -> sb.append('1')
            '২' -> sb.append('2')
            '৩' -> sb.append('3')
            '৪' -> sb.append('4')
            '৫' -> sb.append('5')
            '৬' -> sb.append('6')
            '৭' -> sb.append('7')
            '৮' -> sb.append('8')
            '৯' -> sb.append('9')
        }
    }
    return sb.toString().toIntOrNull() ?: -1
}

@Composable
fun StartingPageInputDialog(
    title: String,
    subtitle: String,
    initialValue: String,
    confirmLabel: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember(initialValue) {
        mutableStateOf(TextFieldValue(initialValue, TextRange(0, initialValue.length)))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column {
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("১০০") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("starting_page_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = parseBengaliOrAsciiPageNumber(input.text.trim())
                    onConfirm(if (parsed > 0) parsed else 100)
                },
                modifier = Modifier.testTag("starting_page_confirm_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun DownloadNoteTxtDialog(
    pdfName: String?,
    noteContent: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val defaultBase = remember(pdfName) {
        var name = if (pdfName != null) PdfUtils.cleanPdfName(pdfName) else "PDF_Note"
        name = name.replace(Regex("[^a-zA-Z0-9_\\-\\u0980-\\u09FF]"), "_")
        if (name.lowercase().endsWith(".pdf")) {
            name = name.dropLast(4)
        }
        "${name}_Note.txt"
    }

    var fileNameInput by remember(defaultBase) {
        mutableStateOf(TextFieldValue(defaultBase, TextRange(defaultBase.length)))
    }
    val charCount = noteContent.length
    val wordCount = remember(noteContent) {
        if (noteContent.trim().isEmpty()) 0 else noteContent.trim().split(Regex("\\s+")).size
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "নোট ডাউনলোড (.txt)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "মোট বর্ণ: $charCount | মোট শব্দ: $wordCount",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = fileNameInput,
                    onValueChange = { fileNameInput = it },
                    label = { Text("ফাইলের নাম") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("download_txt_filename_input")
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("বাতিল")
                    }
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, fileNameInput.text)
                                putExtra(Intent.EXTRA_TEXT, noteContent)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Note via"))
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_txt_share_button")
                    ) {
                        Text("শেয়ার")
                    }
                    Button(
                        onClick = {
                            var finalName = fileNameInput.text.trim()
                            if (finalName.isEmpty()) {
                                finalName = "Note_${System.currentTimeMillis()}.txt"
                            }
                            if (!finalName.lowercase().endsWith(".txt")) {
                                finalName += ".txt"
                            }
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    saveNoteAsTxtFile(context, finalName, noteContent)
                                }
                                if (ok) {
                                    Toast.makeText(
                                        context,
                                        "✓ ডাউনলোড সফল: $finalName\n(Downloads ফোল্ডারে সংরক্ষিত)",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "ফাইল ডাউনলোড করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("download_txt_confirm_button")
                    ) {
                        Text("ডাউনলোড", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun saveNoteAsTxtFile(context: Context, fileName: String, content: String): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(StandardCharsets.UTF_8))
                    out.flush()
                    return true
                }
            }
            false
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val targetFile = File(downloadsDir, fileName)
            FileOutputStream(targetFile).use { fos ->
                fos.write(content.toByteArray(StandardCharsets.UTF_8))
                fos.flush()
            }
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@Composable
fun ClearNotesConfirmDialog(
    isUnlimitedTab: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নোট মুছে ফেলা", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                if (isUnlimitedTab) {
                    "আপনি কি এই PDF-এর আনলিমিটেড নোট মুছে ফেলতে চান?"
                } else {
                    "আপনি কি অধ্যায় ইনডেক্সের নোট মুছে ফেলতে চান?"
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                modifier = Modifier.testTag("clear_notes_confirm_button")
            ) {
                Text("মুছে ফেলুন", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun ResetChaptersConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ডিফল্ট ১০ অধ্যায় রিসেট", fontWeight = FontWeight.Bold) },
        text = { Text("আপনি কি ১০টি অধ্যায়ের ডিফল্ট ফরম্যাট ফিরিয়ে আনতে চান?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("reset_chapters_confirm_button")
            ) {
                Text("হ্যাঁ, রিসেট করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
