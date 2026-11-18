package com.example.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.PdfFile
import com.example.PdfUtils
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CreateFolderDialog(
    title: String = "নতুন ফোল্ডার তৈরি করুন",
    subtitle: String? = null,
    hint: String = "ফোল্ডারের নাম লিখুন",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(hint) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_folder_name")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                modifier = Modifier.testTag("btn_confirm_folder")
            ) {
                Text("তৈরি করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun ManageFolderDialog(
    folderId: String,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf("menu") }
    var newName by remember { mutableStateOf(folderId) }

    when (mode) {
        "rename" -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("ফোল্ডারের নাম পরিবর্তন") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onRename(newName); onDismiss() }) { Text("সংরক্ষণ") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
            )
        }
        "delete" -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("ফোল্ডার মুছে ফেলুন") },
                text = { Text("আপনি কি নিশ্চিত যে '$folderId' ফোল্ডারটি মুছে ফেলতে চান?") },
                confirmButton = {
                    TextButton(
                        onClick = { onDelete(); onDismiss() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("মুছে ফেলুন") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
            )
        }
        else -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("ফোল্ডার: $folderId") },
                text = {
                    Column {
                        Text(
                            text = "নাম পরিবর্তন করুন (Rename)",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { mode = "rename" }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "মুছে ফেলুন (Delete)",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { mode = "delete" }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("বন্ধ করুন") } }
            )
        }
    }
}

@Composable
fun MoveToFolderDialog(
    folders: List<String>,
    onSelectFolder: (String) -> Unit,
    onCreateNewFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ফোল্ডারে রাখুন") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                folders.forEach { folder ->
                    Text(
                        text = folder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFolder(folder); onDismiss() }
                            .padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = "+ নতুন ফোল্ডার তৈরি করুন",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss(); onCreateNewFolder() }
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
fun RenamePdfDialog(
    pdfFile: PdfFile,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(pdfFile.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_rename_pdf")
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name); onDismiss() }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(); onDismiss() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun PdfPropertiesDialog(
    pdfFile: PdfFile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pageText by remember { mutableStateOf("Loading…") }
    LaunchedEffect(pdfFile) {
        val pages = withContext(Dispatchers.IO) {
            pdfFile.uri?.let { PdfUtils.getPageCount(context, it) } ?: 1
        }
        pageText = "$pages ${if (pages == 1) "Page" else "Pages"}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Document Properties") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PropertyLine("Name", pdfFile.name)
                PropertyLine("Path", pdfFile.path.ifEmpty { pdfFile.uriString })
                PropertyLine("Size", "${pdfFile.formattedSize} (${pdfFile.size} bytes)")
                PropertyLine("Modified", pdfFile.formattedFullDate)
                PropertyLine("Pages", pageText)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.testTag("btn_close_properties")) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PropertyLine(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DeleteFolderConfirmDialog(
    folderName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ফোল্ডার মুছে ফেলা") },
        text = { Text("আপনি কি নিশ্চিত যে '$folderName' ফোল্ডারটি মুছে ফেলতে চান?") },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(); onDismiss() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("মুছে ফেলুন") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}
