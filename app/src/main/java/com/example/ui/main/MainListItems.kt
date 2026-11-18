package com.example.ui.main

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PdfDownloadHelper
import com.example.PdfFile
import com.example.PdfUtils
import com.example.R
import com.example.UrlPdfItem
import com.example.ui.theme.AccentPurpleDark
import com.example.ui.theme.AccentPurpleLight
import com.example.ui.theme.DeleteRedDark
import com.example.ui.theme.DeleteRedLight
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.ListBackgroundDark
import com.example.ui.theme.ListBackgroundLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfCardItem(
    pdfFile: PdfFile,
    isDarkMode: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isFavorite: Boolean,
    thumbnail: Bitmap?,
    pageCount: Int?,
    onEnsureLoaded: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMenuClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    LaunchedEffect(pdfFile) {
        onEnsureLoaded()
    }

    val listBg = if (isDarkMode) ListBackgroundDark else ListBackgroundLight
    val accentPurple = if (isDarkMode) AccentPurpleDark else AccentPurpleLight
    val deleteRed = if (isDarkMode) DeleteRedDark else DeleteRedLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(28.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("pdf_card_${pdfFile.id}"),
        shape = RoundedCornerShape(28.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 1.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 82.dp, height = 92.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(listBg),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null && !thumbnail.isRecycled) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = pdfFile.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_pdf_file),
                        contentDescription = pdfFile.name,
                        modifier = Modifier.size(44.dp)
                    )
                }

                if (isSelectionMode && isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(92.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = pdfFile.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_favorite_${pdfFile.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                            tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_menu_${pdfFile.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "PDF Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = PdfUtils.toBengaliDigits(pdfFile.formattedSize),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(10.dp))
                        val pagesText = if (pageCount != null && pageCount > 0) {
                            "পৃষ্ঠা ${PdfUtils.toBengaliDigits(pageCount.toString())}"
                        } else {
                            "পৃষ্ঠা …"
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentPurple.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = pagesText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentPurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onDeleteClick,
                        shape = RoundedCornerShape(14.dp),
                        color = deleteRed.copy(alpha = 0.12f),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_delete_${pdfFile.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete PDF",
                                tint = deleteRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadCardItem(
    item: UrlPdfItem,
    isDarkMode: Boolean,
    progressState: PdfDownloadHelper.DownloadProgressState?,
    onReadOrDownloadClick: () -> Unit,
    onCancelDownloadClick: () -> Unit,
    onChangeCoverClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val emerald = if (isDarkMode) EmeraldGreenDark else EmeraldGreenLight
    val deleteRed = if (isDarkMode) DeleteRedDark else DeleteRedLight
    val listBg = if (isDarkMode) ListBackgroundDark else ListBackgroundLight

    var coverBitmap by remember(item.coverImagePath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(item.coverImagePath) {
        val path = item.coverImagePath
        coverBitmap = if (!path.isNullOrEmpty() && File(path).exists()) {
            withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(path)
                } catch (_: Exception) {
                    null
                }
            }
        } else {
            null
        }
    }

    val isDownloading = progressState?.isDownloading == true
    val isPaused = progressState?.isPaused == true
    val hasValidLocalFile = item.isDownloaded &&
        !item.localFilePath.isNullOrEmpty() &&
        File(item.localFilePath!!).exists()

    val statusText = when {
        isDownloading -> "গুগল ড্রাইভ থেকে ডাউনলোড হচ্ছে…"
        hasValidLocalFile -> "Downloaded • অফলাইনে পড়ার জন্য প্রস্তুত"
        isPaused && (progressState?.downloadedBytes ?: 0L) > 0 ->
            "বিরতি (${PdfDownloadHelper.formatBanglaSize(progressState!!.downloadedBytes)} ডাউনলোড হয়েছে)"
        progressState?.errorMessage != null -> "ব্যর্থ: ${progressState.errorMessage}"
        else -> "Google Drive Link • ট্যাপ করে ডাউনলোড করুন"
    }

    val buttonLabel = when {
        hasValidLocalFile -> "Read"
        isDownloading -> "Wait"
        isPaused && (progressState?.downloadedBytes ?: 0L) > 0 -> "Resume"
        progressState?.errorMessage != null -> "Retry"
        else -> "Download"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("download_card_${item.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 1.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(listBg)
                        .clickable(onClick = onChangeCoverClick)
                        .testTag("btn_change_cover_${item.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = coverBitmap
                    if (bmp != null && !bmp.isRecycled) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Change cover image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_pdf_file),
                            contentDescription = "Change cover image",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasValidLocalFile) emerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onReadOrDownloadClick,
                            enabled = !isDownloading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasValidLocalFile) emerald else MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_download_action_${item.id}")
                        ) {
                            Text(buttonLabel, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onCopyLinkClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_copy_link_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Link",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_delete_url_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete Download",
                                tint = deleteRed
                            )
                        }
                    }
                }
            }

            if (isDownloading || (isPaused && (progressState?.downloadedBytes ?: 0L) > 0)) {
                val downloaded = progressState?.downloadedBytes ?: 0L
                val total = progressState?.totalBytes ?: -1L
                val percent = progressState?.percent ?: -1

                Spacer(Modifier.height(10.dp))
                if (percent in 0..100 && total > 0) {
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val curMb = PdfDownloadHelper.formatBanglaSize(downloaded)
                    val totalMb = if (total > 0) PdfDownloadHelper.formatBanglaSize(total) else "অজানা"
                    val pctText = if (percent >= 0 && total > 0) {
                        "${PdfUtils.toBengaliDigits(percent.toString())}%"
                    } else {
                        "…"
                    }
                    Text(
                        text = "$curMb / $totalMb ($pctText)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isDownloading) {
                        OutlinedButton(
                            onClick = onCancelDownloadClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("btn_cancel_dl_${item.id}")
                        ) {
                            Text("বাতিল করুন", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
