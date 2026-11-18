package com.example.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.FolderManager
import com.example.PdfFile
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuBottomSheet(
    isDarkMode: Boolean,
    activeFolder: String,
    totalPdfCount: Int,
    favoritesCount: Int,
    folders: List<String>,
    folderCounts: Map<String, Int>,
    onToggleDarkMode: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onSelectFolder: (String) -> Unit,
    onRequestCreateFolder: () -> Unit,
    onRequestDeleteFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "মেনু ও সেটিংস",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "ডার্ক মোড (Dark Mode)",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = onToggleDarkMode,
                    modifier = Modifier.testTag("switch_dark_mode")
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    },
                    modifier = Modifier.testTag("btn_menu_settings")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings & Permissions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ফোল্ডার (Folders)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onRequestCreateFolder,
                    modifier = Modifier.testTag("btn_menu_create_folder")
                ) {
                    Text("+ নতুন ফোল্ডার")
                }
            }

            Spacer(Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                MenuFolderRow(
                    label = "সব ফাইল (All PDFs)",
                    count = totalPdfCount,
                    isActive = activeFolder.equals(FolderManager.FOLDER_ALL, ignoreCase = true),
                    canDelete = false,
                    onClick = {
                        onDismiss()
                        onSelectFolder(FolderManager.FOLDER_ALL)
                    },
                    onDelete = {}
                )
                MenuFolderRow(
                    label = "★ ফেভারিট (Favorites)",
                    count = favoritesCount,
                    isActive = activeFolder.equals(FolderManager.FOLDER_FAVORITES, ignoreCase = true),
                    canDelete = false,
                    onClick = {
                        onDismiss()
                        onSelectFolder(FolderManager.FOLDER_FAVORITES)
                    },
                    onDelete = {}
                )
                folders.forEach { fName ->
                    MenuFolderRow(
                        label = fName,
                        count = folderCounts[fName] ?: 0,
                        isActive = activeFolder.equals(fName, ignoreCase = true),
                        canDelete = true,
                        onClick = {
                            onDismiss()
                            onSelectFolder(fName)
                        },
                        onDelete = { onRequestDeleteFolder(fName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuFolderRow(
    label: String,
    count: Int,
    isActive: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isActive) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        if (canDelete) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Folder",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfOptionsBottomSheet(
    pdfFile: PdfFile,
    onMoveToFolder: () -> Unit,
    onOpenWith: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onCompress: () -> Unit,
    onShare: () -> Unit,
    onCopyPath: () -> Unit,
    onCreateShortcut: () -> Unit,
    onProperties: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_pdf_file),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pdfFile.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${pdfFile.formattedSize} • ${pdfFile.formattedDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(4.dp))

            SheetOptionRow(Icons.AutoMirrored.Filled.DriveFileMove, "ফোল্ডারে রাখুন (Move to Folder)", "action_move_folder") {
                onDismiss(); onMoveToFolder()
            }
            SheetOptionRow(Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.open_with), "action_open_with") {
                onDismiss(); onOpenWith()
            }
            SheetOptionRow(Icons.Filled.ContentCut, stringResource(R.string.cut), "action_cut") {
                onDismiss(); onCut()
            }
            SheetOptionRow(Icons.Filled.ContentCopy, stringResource(R.string.copy), "action_copy") {
                onDismiss(); onCopy()
            }
            SheetOptionRow(
                icon = Icons.Filled.DeleteOutline,
                label = stringResource(R.string.delete),
                tag = "action_delete",
                tint = MaterialTheme.colorScheme.error
            ) {
                onDismiss(); onDelete()
            }
            SheetOptionRow(Icons.Filled.DriveFileRenameOutline, stringResource(R.string.rename), "action_rename") {
                onDismiss(); onRename()
            }
            SheetOptionRow(Icons.Filled.Archive, stringResource(R.string.compress), "action_compress") {
                onDismiss(); onCompress()
            }
            SheetOptionRow(Icons.Filled.Share, stringResource(R.string.share), "action_share") {
                onDismiss(); onShare()
            }
            SheetOptionRow(Icons.Filled.Link, stringResource(R.string.copy_path), "action_copy_path") {
                onDismiss(); onCopyPath()
            }
            SheetOptionRow(Icons.Filled.AddToHomeScreen, stringResource(R.string.create_shortcut), "action_shortcut") {
                onDismiss(); onCreateShortcut()
            }
            SheetOptionRow(Icons.Filled.Info, stringResource(R.string.properties), "action_properties") {
                onDismiss(); onProperties()
            }
        }
    }
}

@Composable
private fun SheetOptionRow(
    icon: ImageVector,
    label: String,
    tag: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}
