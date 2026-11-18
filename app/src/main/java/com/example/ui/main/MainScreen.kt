package com.example.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.FolderManager
import com.example.PdfDownloadHelper
import com.example.PdfFile
import com.example.R
import com.example.UrlPdfItem
import com.example.ui.theme.ListBackgroundDark
import com.example.ui.theme.ListBackgroundLight
import android.graphics.Bitmap

@Composable
fun MainScreen(
    isDarkMode: Boolean,
    isDownloadsTab: Boolean,
    isLoading: Boolean,
    pdfList: List<PdfFile>,
    totalPdfCount: Int,
    urlItems: List<UrlPdfItem>,
    searchQuery: String,
    selectedFolder: String,
    folders: List<String>,
    folderCounts: Map<String, Int>,
    favoritesCount: Int,
    selectedKeys: Set<String>,
    favoriteKeys: Set<String>,
    thumbnailCache: Map<String, Bitmap>,
    pageCountCache: Map<String, Int>,
    downloadProgressMap: Map<String, PdfDownloadHelper.DownloadProgressState>,
    itemKey: (PdfFile) -> String,
    favKey: (PdfFile) -> String,
    onOpenMenu: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectFolder: (String) -> Unit,
    onClearFolderFilter: () -> Unit,
    onLongClickFolder: (String) -> Unit,
    onCreateFolderClick: () -> Unit,
    onCreateFolderFromSelectedClick: () -> Unit,
    onClearSelectionMode: () -> Unit,
    onEnsurePdfLoaded: (PdfFile) -> Unit,
    onPdfClick: (PdfFile) -> Unit,
    onPdfLongClick: (PdfFile) -> Unit,
    onToggleFavorite: (PdfFile) -> Unit,
    onPdfMenuClick: (PdfFile) -> Unit,
    onPdfDeleteClick: (PdfFile) -> Unit,
    onDownloadActionClick: (UrlPdfItem) -> Unit,
    onCancelDownloadClick: (String) -> Unit,
    onChangeCoverClick: (UrlPdfItem) -> Unit,
    onCopyUrlLinkClick: (UrlPdfItem) -> Unit,
    onDeleteUrlItemClick: (UrlPdfItem) -> Unit,
    onSwitchToHome: () -> Unit,
    onSwitchToDownloads: () -> Unit,
    onExportClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onImportPdfClick: () -> Unit,
    onImportUrlClick: () -> Unit,
    onCreateSamplesClick: () -> Unit
) {
    val isSelectionMode = selectedKeys.isNotEmpty()
    val listBg = if (isDarkMode) ListBackgroundDark else ListBackgroundLight

    BackHandler(enabled = isSelectionMode || searchQuery.isNotEmpty() || isDownloadsTab) {
        when {
            isSelectionMode -> onClearSelectionMode()
            searchQuery.isNotEmpty() -> onSearchQueryChange("")
            isDownloadsTab -> onSwitchToHome()
        }
    }

    var swipeAccumulated by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(listBg)
    ) {
        Surface(
            color = if (isSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                if (isSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClearSelectionMode,
                            modifier = Modifier.testTag("btn_cancel_multiselect")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cancel Selection",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = "${selectedKeys.size}টি ফাইল নির্বাচিত",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        ElevatedButton(
                            onClick = onCreateFolderFromSelectedClick,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_create_folder_selected")
                        ) {
                            Text("+ নতুন Folder", fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        IconButton(
                            onClick = onOpenMenu,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .testTag("btn_main_menu")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Main Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo_new),
                                contentDescription = null,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        val subtitleCount = if (isDownloadsTab) {
                            "${urlItems.size} items"
                        } else {
                            "${pdfList.size} PDFs"
                        }
                        Text(
                            text = subtitleCount,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag("btn_clear_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("input_search_pdfs")
                )

                if (!isDownloadsTab) {
                    FolderChipsBar(
                        selectedFolder = selectedFolder,
                        totalPdfCount = totalPdfCount,
                        favoritesCount = favoritesCount,
                        folders = folders,
                        folderCounts = folderCounts,
                        onSelectFolder = onSelectFolder,
                        onLongClickFolder = onLongClickFolder,
                        onCreateFolderClick = onCreateFolderClick
                    )
                    if (!selectedFolder.equals(FolderManager.FOLDER_ALL, ignoreCase = true)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InputChip(
                                selected = true,
                                onClick = onClearFolderFilter,
                                label = { Text("Folder: $selectedFolder (${pdfList.size})") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear filter",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("chip_active_folder")
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(isSelectionMode) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeAccumulated = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            swipeAccumulated += dragAmount
                            if (swipeAccumulated > 140f && !isSelectionMode) {
                                swipeAccumulated = 0f
                                onOpenMenu()
                            }
                        }
                    )
                }
        ) {
            if (!isDownloadsTab) {
                if (pdfList.isEmpty() && !isLoading) {
                    EmptyGalleryState(
                        isDownloadsTab = false,
                        onCreateSamplesClick = onCreateSamplesClick,
                        onImportPdfClick = onImportPdfClick,
                        onImportUrlClick = onImportUrlClick
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("list_home_pdfs")
                    ) {
                        items(
                            items = pdfList,
                            key = { itemKey(it) }
                        ) { pdf ->
                            val key = itemKey(pdf)
                            PdfCardItem(
                                pdfFile = pdf,
                                isDarkMode = isDarkMode,
                                isSelected = selectedKeys.contains(key),
                                isSelectionMode = isSelectionMode,
                                isFavorite = favoriteKeys.contains(favKey(pdf)),
                                thumbnail = thumbnailCache[key],
                                pageCount = pageCountCache[key],
                                onEnsureLoaded = { onEnsurePdfLoaded(pdf) },
                                onClick = { onPdfClick(pdf) },
                                onLongClick = { onPdfLongClick(pdf) },
                                onToggleFavorite = { onToggleFavorite(pdf) },
                                onMenuClick = { onPdfMenuClick(pdf) },
                                onDeleteClick = { onPdfDeleteClick(pdf) }
                            )
                        }
                    }
                }
            } else {
                if (urlItems.isEmpty() && !isLoading) {
                    EmptyGalleryState(
                        isDownloadsTab = true,
                        onCreateSamplesClick = onCreateSamplesClick,
                        onImportPdfClick = onImportPdfClick,
                        onImportUrlClick = onImportUrlClick
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 8.dp, bottom = 92.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("list_download_pdfs")
                    ) {
                        items(
                            items = urlItems,
                            key = { it.id }
                        ) { item ->
                            DownloadCardItem(
                                item = item,
                                isDarkMode = isDarkMode,
                                progressState = downloadProgressMap[item.id],
                                onReadOrDownloadClick = { onDownloadActionClick(item) },
                                onCancelDownloadClick = { onCancelDownloadClick(item.id) },
                                onChangeCoverClick = { onChangeCoverClick(item) },
                                onCopyLinkClick = { onCopyUrlLinkClick(item) },
                                onDeleteClick = { onDeleteUrlItemClick(item) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallFloatingActionButton(
                        onClick = onImportUrlClick,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_import_url_fab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Import URL", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    ExtendedFloatingActionButton(
                        onClick = onImportPdfClick,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.import_pdf), fontWeight = FontWeight.Bold) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_import_pdf")
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        BottomNavigationRow(
            isDownloadsTab = isDownloadsTab,
            onHomeClick = onSwitchToHome,
            onDownloadClick = onSwitchToDownloads,
            onExportClick = onExportClick,
            onRestoreClick = onRestoreClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderChipsBar(
    selectedFolder: String,
    totalPdfCount: Int,
    favoritesCount: Int,
    folders: List<String>,
    folderCounts: Map<String, Int>,
    onSelectFolder: (String) -> Unit,
    onLongClickFolder: (String) -> Unit,
    onCreateFolderClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedFolder.equals(FolderManager.FOLDER_ALL, ignoreCase = true),
            onClick = { onSelectFolder(FolderManager.FOLDER_ALL) },
            label = { Text("সব ($totalPdfCount)") },
            modifier = Modifier.testTag("chip_folder_all")
        )
        FilterChip(
            selected = selectedFolder.equals(FolderManager.FOLDER_FAVORITES, ignoreCase = true),
            onClick = { onSelectFolder(FolderManager.FOLDER_FAVORITES) },
            label = { Text("★ ফেভারিট ($favoritesCount)") },
            modifier = Modifier.testTag("chip_folder_favorites")
        )
        folders.forEach { folder ->
            val count = folderCounts[folder] ?: 0
            val active = selectedFolder.equals(folder, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (active) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(
                        onClick = { onSelectFolder(folder) },
                        onLongClick = { onLongClickFolder(folder) }
                    )
                    .testTag("chip_folder_$folder")
            ) {
                Text(
                    text = "$folder ($count)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        OutlinedButton(
            onClick = onCreateFolderClick,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier
                .height(36.dp)
                .testTag("btn_quick_create_folder")
        ) {
            Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("+ Folder", fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyGalleryState(
    isDownloadsTab: Boolean,
    onCreateSamplesClick: () -> Unit,
    onImportPdfClick: () -> Unit,
    onImportUrlClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_pdf_file),
            contentDescription = null,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isDownloadsTab) "No Downloaded PDFs" else stringResource(R.string.no_pdfs_found),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isDownloadsTab) {
                "Click 'Import URL' to add a Google Drive PDF link and download it for offline reading."
            } else {
                stringResource(R.string.no_pdfs_subtitle)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))

        if (isDownloadsTab) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onImportUrlClick,
                    modifier = Modifier.testTag("btn_empty_import_url")
                ) { Text("Import URL") }
                Button(
                    onClick = onImportPdfClick,
                    modifier = Modifier.testTag("btn_empty_import_pdf")
                ) { Text(stringResource(R.string.import_pdf)) }
            }
            Spacer(Modifier.height(10.dp))
        }

        OutlinedButton(
            onClick = onCreateSamplesClick,
            modifier = Modifier.testTag("btn_empty_create_samples")
        ) {
            Text("+ Sample PDFs")
        }
    }
}

@Composable
private fun BottomNavigationRow(
    isDownloadsTab: Boolean,
    onHomeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onExportClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Filled.Home,
                    label = "হোম",
                    selected = !isDownloadsTab,
                    tag = "tab_home",
                    onClick = onHomeClick,
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    icon = Icons.Filled.CloudDownload,
                    label = "ডাউনলোড",
                    selected = isDownloadsTab,
                    tag = "tab_download",
                    onClick = onDownloadClick,
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    icon = Icons.Filled.UploadFile,
                    label = "এক্সপোর্ট",
                    selected = false,
                    tag = "tab_export",
                    onClick = onExportClick,
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    icon = Icons.Filled.FolderOpen,
                    label = "রিস্টোর",
                    selected = false,
                    tag = "tab_import",
                    onClick = onRestoreClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick)
            .testTag(tag),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint
        )
    }
}
