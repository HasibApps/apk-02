package com.example.ui.main

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.UrlPdfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImportUrlDialog(
    prefilledUrl: String?,
    chosenCoverUri: Uri?,
    existingItemCount: Int,
    onPickCoverImage: () -> Unit,
    onConfirmImport: (url: String, title: String, coverUri: Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var urlText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var isFetchingTitle by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var helperMessage by remember {
        mutableStateOf("গুগল ড্রাইভ লিংক দিলে ফাইলের নাম স্বয়ংক্রিয়ভাবে যোগ হবে")
    }
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(prefilledUrl) {
        val initial = prefilledUrl?.trim().orEmpty()
        if (initial.isNotEmpty()) {
            urlText = initial
        } else {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = clipboard?.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val copied = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
                    if (copied.isNotEmpty()) {
                        urlText = copied
                        Toast.makeText(
                            context,
                            "ক্লিপবোর্ডের লিংক স্বয়ংক্রিয়ভাবে পেস্ট হয়েছে",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(chosenCoverUri) {
        coverBitmap = if (chosenCoverUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(chosenCoverUri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        } else {
            null
        }
    }

    LaunchedEffect(urlText) {
        val trimmed = urlText.trim()
        if (trimmed.length > 10 &&
            (trimmed.contains("drive.google.com") || trimmed.contains("/d/") || trimmed.contains("id="))
        ) {
            delay(400)
            isFetchingTitle = true
            helperMessage = "গুগল ড্রাইভ থেকে ফাইলের নাম আনা হচ্ছে..."
            val fetched = withContext(Dispatchers.IO) {
                UrlPdfManager.fetchDriveTitleSync(trimmed)
            }
            isFetchingTitle = false
            if (!fetched.isNullOrBlank()) {
                val cur = titleText.trim()
                if (cur.isEmpty() || cur.startsWith("Drive_Document_")) {
                    titleText = fetched
                }
                helperMessage = "✓ গুগল ড্রাইভ থেকে স্বয়ংক্রিয়ভাবে নাম সেট হয়েছে"
            } else {
                helperMessage = "গুগল ড্রাইভ লিংক দিলে ফাইলের নাম স্বয়ংক্রিয়ভাবে যোগ হবে"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(8.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Import PDF from URL",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Google Drive Link (URL)") },
                    placeholder = { Text("https://drive.google.com/file/d/...") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_drive_url")
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("PDF Title (Auto or Custom)") },
                    supportingText = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isFetchingTitle) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(helperMessage, fontSize = 11.sp)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_pdf_title")
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(56.dp, 72.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        val bmp = coverBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Selected Cover",
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_pdf_file),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = onPickCoverImage,
                            modifier = Modifier.testTag("btn_select_cover")
                        ) {
                            Text("Select Cover Image")
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (chosenCoverUri != null) "Custom cover image chosen!" else "Optional thumbnail cover",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_cancel_import_url")
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = !isSubmitting,
                        onClick = {
                            val rawUrl = urlText.trim()
                            if (rawUrl.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "অনুগ্রহ করে গুগল ড্রাইভের লিংক দিন",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            val customTitle = titleText.trim()
                            if (customTitle.isEmpty()) {
                                isSubmitting = true
                                scope.launch {
                                    val fetched = withContext(Dispatchers.IO) {
                                        UrlPdfManager.fetchDriveTitleSync(rawUrl)
                                    }
                                    isSubmitting = false
                                    var finalTitle = fetched?.trim().orEmpty()
                                    if (finalTitle.isEmpty()) {
                                        finalTitle = "Drive_Document_${existingItemCount + 1}.pdf"
                                    }
                                    if (!finalTitle.lowercase().endsWith(".pdf")) {
                                        finalTitle += ".pdf"
                                    }
                                    onConfirmImport(rawUrl, finalTitle, chosenCoverUri)
                                }
                            } else {
                                val normalized = if (customTitle.lowercase().endsWith(".pdf")) {
                                    customTitle
                                } else {
                                    "$customTitle.pdf"
                                }
                                onConfirmImport(rawUrl, normalized, chosenCoverUri)
                            }
                        },
                        modifier = Modifier.testTag("btn_confirm_import_url")
                    ) {
                        Text(if (isSubmitting) "যাচাই করা হচ্ছে..." else "Import PDF")
                    }
                }
            }
        }
    }
}
