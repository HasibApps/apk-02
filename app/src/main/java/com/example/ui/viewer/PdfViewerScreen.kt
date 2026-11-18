package com.example.ui.viewer

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PdfUtils
import com.example.ui.theme.PrimaryRedLight
import com.example.ui.theme.ViewerDarkBar
import com.example.ui.theme.ViewerDarkBg
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PdfViewerScreen(
    uiState: PdfViewerUiState,
    pdfDisplayName: String,
    loadPageBitmap: suspend (Int, Int) -> Bitmap?,
    getCachedPage: (Int) -> Bitmap?,
    onBack: () -> Unit,
    onLensClick: (pageIndex: Int, pageBitmap: Bitmap?) -> Unit,
    onCropClick: (pageIndex: Int, pageBitmap: Bitmap?) -> Unit,
    onNotesClick: (onJumpToPage: (Int) -> Unit) -> Unit,
    onSharePdf: () -> Unit,
    onCorruptDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val listState = rememberLazyListState()

    val cleanDocName = remember(pdfDisplayName) {
        val cleaned = PdfUtils.cleanPdfName(pdfDisplayName)
        if (cleaned.lowercase().endsWith(".pdf")) cleaned.dropLast(4) else cleaned
    }

    val totalPages = (uiState as? PdfViewerUiState.Ready)?.totalPages ?: 0
    val currentVisiblePage by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var lastDoubleTapTime by remember { mutableStateOf(0L) }

    var showJumpDialog by remember { mutableStateOf(false) }
    var isFastScrolling by remember { mutableStateOf(false) }
    var tooltipVisible by remember { mutableStateOf(false) }

    val renderTargetWidth = remember {
        (context.resources.displayMetrics.widthPixels * 1.4f).roundToInt().coerceIn(900, 1800)
    }

    fun jumpToPage(pageNumber1Based: Int) {
        if (totalPages <= 0) return
        val targetIdx = (pageNumber1Based - 1).coerceIn(0, totalPages - 1)
        scope.launch {
            listState.scrollToItem(targetIdx)
            Toast.makeText(context, "পৃষ্ঠা ${targetIdx + 1} এ যাওয়া হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isFastScrolling) {
        if (isFastScrolling) {
            tooltipVisible = true
        } else if (tooltipVisible) {
            delay(1000)
            tooltipVisible = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ViewerDarkBg)
            .safeDrawingPadding()
    ) {
        // Slim Custom Title Bar
        Surface(color = ViewerDarkBar, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("viewer_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to list", tint = Color.White)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "App Create By Hasib Bro",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (totalPages > 0) "$cleanDocName • Page ${currentVisiblePage + 1}/$totalPages" else cleanDocName,
                        color = Color(0xFF90CAF9),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        if (totalPages <= 0) {
                            Toast.makeText(context, "PDF লোড হচ্ছে...", Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                                val bmp = getCachedPage(currentVisiblePage) ?: loadPageBitmap(currentVisiblePage, renderTargetWidth)
                                onLensClick(currentVisiblePage, bmp)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("viewer_lens_button")
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Google Lens Scan", tint = Color(0xFF64B5F6), modifier = Modifier.size(21.dp))
                }

                IconButton(
                    onClick = {
                        if (totalPages <= 0) {
                            Toast.makeText(context, "PDF এখনও লোড হয়নি", Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                                val bmp = getCachedPage(currentVisiblePage) ?: loadPageBitmap(currentVisiblePage, renderTargetWidth)
                                onCropClick(currentVisiblePage, bmp)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("viewer_crop_button")
                ) {
                    Icon(Icons.Default.Crop, contentDescription = "Crop Page & Share with AI", tint = Color(0xFF90CAF9), modifier = Modifier.size(21.dp))
                }

                IconButton(
                    onClick = {
                        if (totalPages <= 0) {
                            Toast.makeText(context, "PDF পেজ লোড হচ্ছে...", Toast.LENGTH_SHORT).show()
                        } else {
                            showJumpDialog = true
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("viewer_jump_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search / Jump to Page", tint = Color.White, modifier = Modifier.size(21.dp))
                }

                IconButton(
                    onClick = { onNotesClick { target -> jumpToPage(target) } },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("viewer_notes_button")
                ) {
                    Icon(Icons.Default.Description, contentDescription = "Notes and Chapter Index", tint = Color(0xFFFFE082), modifier = Modifier.size(21.dp))
                }

                IconButton(
                    onClick = onSharePdf,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("viewer_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = Color.White, modifier = Modifier.size(21.dp))
                }
            }
        }

        // Main Content Stage
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val containerWidthPx = with(density) { maxWidth.toPx() }
            val containerHeightPx = with(density) { maxHeight.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(containerWidthPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val now = System.currentTimeMillis()
                            if (now - lastDoubleTapTime < 280L) {
                                if (zoomScale > 1.05f) {
                                    zoomScale = 1f
                                    panX = 0f
                                } else {
                                    zoomScale = 2f
                                }
                                lastDoubleTapTime = 0L
                            } else {
                                lastDoubleTapTime = now
                            }

                            var panAccumulated = 0f
                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount >= 2) {
                                    val zoomChange = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    zoomScale = (zoomScale * zoomChange).coerceIn(1f, 3.5f)
                                    if (zoomScale <= 1.02f) {
                                        panX = 0f
                                    } else {
                                        val maxTravel = (zoomScale - 1f) * containerWidthPx * 0.65f
                                        panX = (panX + pan.x).coerceIn(-maxTravel, maxTravel)
                                    }
                                    event.changes.forEach { it.consume() }
                                } else if (pressedCount == 1 && zoomScale > 1.05f) {
                                    val ch = event.changes.firstOrNull { it.id == down.id } ?: continue
                                    val dx = ch.position.x - ch.previousPosition.x
                                    val dy = ch.position.y - ch.previousPosition.y
                                    panAccumulated += abs(dx)
                                    if (abs(dx) > abs(dy) * 0.8f && panAccumulated > 6f) {
                                        val maxTravel = (zoomScale - 1f) * containerWidthPx * 0.65f
                                        panX = (panX + dx).coerceIn(-maxTravel, maxTravel)
                                        ch.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            if (zoomScale < 1.05f) {
                                zoomScale = 1f
                                panX = 0f
                            }
                        }
                    }
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 6.dp, bottom = 76.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panX
                        }
                ) {
                    items(count = totalPages, key = { it }) { pageIndex ->
                        PdfPageCard(
                            pageIndex = pageIndex,
                            renderTargetWidth = renderTargetWidth,
                            loadPageBitmap = loadPageBitmap,
                            onLensPage = { bmp -> onLensClick(pageIndex, bmp) },
                            onCropPage = { bmp -> onCropClick(pageIndex, bmp) }
                        )
                    }
                }
            }

            if (uiState is PdfViewerUiState.Loading) {
                CircularProgressIndicator(
                    color = PrimaryRedLight,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }

            // Floating Zoom Level Badge
            androidx.compose.animation.AnimatedVisibility(
                visible = zoomScale > 1.01f,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xCC212121),
                    border = BorderStroke(1.dp, Color(0xFF555555)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .height(32.dp)
                        .clickable {
                            zoomScale = 1f
                            panX = 0f
                        }
                        .testTag("viewer_zoom_reset_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(zoomScale * 100).roundToInt()}%",
                            color = Color(0xFFE0E0E0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "• Reset",
                            color = Color(0xFF90CAF9),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Fast Scroll Capsule & Tooltip
            if (totalPages > 1) {
                val topOffsetPx = with(density) { 60.dp.toPx() }
                val capsuleHeightPx = with(density) { 112.dp.toPx() }
                val maxTravelPx = (containerHeightPx - topOffsetPx - capsuleHeightPx - with(density) { 64.dp.toPx() }).coerceAtLeast(1f)
                val autoY = (currentVisiblePage.toFloat() / (totalPages - 1).toFloat()) * maxTravelPx
                var dragY by remember { mutableFloatStateOf(0f) }
                val displayedY = if (isFastScrolling) dragY else autoY

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 60.dp, end = 8.dp)
                        .offset { IntOffset(0, displayedY.roundToInt()) }
                ) {
                    if (tooltipVisible) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xDD1976D2),
                            shadowElevation = 6.dp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "Page ${currentVisiblePage + 1} / $totalPages",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(19.dp),
                        color = Color(0xCC262626),
                        border = BorderStroke(1.dp, Color(0xFF555555)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .width(38.dp)
                            .height(112.dp)
                            .pointerInput(totalPages, maxTravelPx) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        dragY = autoY
                                        isFastScrolling = true
                                    },
                                    onDragEnd = { isFastScrolling = false },
                                    onDragCancel = { isFastScrolling = false },
                                    onVerticalDrag = { _, dragAmount ->
                                        dragY = (dragY + dragAmount).coerceIn(0f, maxTravelPx)
                                        val frac = dragY / maxTravelPx
                                        val targetIdx = (frac * (totalPages - 1)).roundToInt().coerceIn(0, totalPages - 1)
                                        scope.launch { listState.scrollToItem(targetIdx) }
                                    }
                                )
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            IconButton(
                                onClick = { scope.launch { listState.scrollToItem(0) } },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("viewer_scroll_top_button")
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top", tint = Color(0xFF90CAF9))
                            }
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag to scroll",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            IconButton(
                                onClick = { scope.launch { listState.scrollToItem(totalPages - 1) } },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("viewer_scroll_bottom_button")
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to bottom", tint = Color(0xFF90CAF9))
                            }
                        }
                    }
                }
            }

            // Bottom Floating Quick Page Indicator & Jump Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF2D2D2D),
                border = BorderStroke(1.dp, Color(0xFF4A4A4A)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .height(40.dp)
                    .clickable(enabled = totalPages > 0) { showJumpDialog = true }
                    .testTag("viewer_quick_page_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (totalPages > 0) "Page ${currentVisiblePage + 1} / $totalPages" else "Loading...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    VerticalDivider(
                        color = Color(0xFF555555),
                        modifier = Modifier
                            .height(18.dp)
                            .padding(horizontal = 10.dp)
                    )
                    Text(
                        text = "Tap to Jump",
                        color = Color(0xFF90CAF9),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    if (showJumpDialog && totalPages > 0) {
        JumpToPageDialog(
            currentPage1Based = currentVisiblePage + 1,
            totalPages = totalPages,
            onJump = { target ->
                showJumpDialog = false
                jumpToPage(target)
            },
            onDismiss = { showJumpDialog = false }
        )
    }

    if (uiState is PdfViewerUiState.CorruptOrFailed) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("PDF লোড করা সম্ভব হয়নি", fontWeight = FontWeight.Bold) },
            text = {
                Text("ফাইলটি সঠিক PDF নয় অথবা ডাউনলোড করার সময় গুগল ড্রাইভ পেজ সেভ হয়েছিল। ফাইলটি মুছে পুনরায় ডাউনলোড করতে চান?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        uiState.localFile?.let { file ->
                            if (file.exists()) {
                                try {
                                    file.delete()
                                } catch (_: Exception) {}
                            }
                        }
                        onCorruptDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRedLight)
                ) {
                    Text("মুছে ফেলুন ও ফিরে যান")
                }
            },
            dismissButton = {
                TextButton(onClick = onCorruptDelete) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }
}

@Composable
private fun PdfPageCard(
    pageIndex: Int,
    renderTargetWidth: Int,
    loadPageBitmap: suspend (Int, Int) -> Bitmap?,
    onLensPage: (Bitmap?) -> Unit,
    onCropPage: (Bitmap?) -> Unit
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex, renderTargetWidth) {
        bitmap = loadPageBitmap(pageIndex, renderTargetWidth)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF181818))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF333333)
            ) {
                Text(
                    text = "Page ${pageIndex + 1}",
                    color = Color(0xFFE0E0E0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2E3440),
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onLensPage(bitmap) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Google Lens",
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1976D2),
                modifier = Modifier
                    .height(28.dp)
                    .clickable { onLensPage(bitmap) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Google Lens", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Lens", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.width(6.dp))

            TextButton(
                onClick = { onCropPage(bitmap) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Crop, contentDescription = null, tint = Color(0xFF90CAF9), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Crop Page", color = Color(0xFF90CAF9), fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(2.dp))

        val bmp = bitmap
        if (bmp != null) {
            val ratio = (bmp.width.toFloat() / bmp.height.toFloat()).coerceIn(0.4f, 2.5f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .background(Color.White)
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "PDF Page ${pageIndex + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryRedLight, modifier = Modifier.size(36.dp))
            }
        }

        HorizontalDivider(color = Color(0xFF2E2E2E), modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun JumpToPageDialog(
    currentPage1Based: Int,
    totalPages: Int,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initial = currentPage1Based.toString()
    var input by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("পৃষ্ঠায় যান (Jump to Page)", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column {
                Text("পৃষ্ঠা ১ থেকে $totalPages এর মধ্যে যেকোনো নম্বর লিখুন", fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jump_page_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = input.text.trim()
                    if (trimmed.isEmpty()) {
                        Toast.makeText(context, "অনুগ্রহ করে একটি পৃষ্ঠা নম্বর লিখুন", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val target = trimmed.toIntOrNull()
                    if (target == null) {
                        Toast.makeText(context, "সঠিক পৃষ্ঠা সংখ্যা লিখুন", Toast.LENGTH_SHORT).show()
                    } else if (target !in 1..totalPages) {
                        Toast.makeText(context, "১ থেকে $totalPages এর মধ্যে পৃষ্ঠা লিখুন", Toast.LENGTH_SHORT).show()
                    } else {
                        onJump(target)
                    }
                },
                modifier = Modifier.testTag("jump_page_confirm_button")
            ) {
                Text("যান")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
