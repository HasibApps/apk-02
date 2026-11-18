package com.example.ui.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.PdfUtils
import com.example.ui.components.CropOverlayCanvas
import com.example.ui.components.CropOverlayState
import com.example.ui.theme.PrimaryRedLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix

@Composable
fun PageCropDialog(
    initialPageIndex: Int,
    initialBitmap: Bitmap,
    totalPages: Int,
    loadPageBitmap: suspend (Int) -> Bitmap?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentPage by remember { mutableIntStateOf(initialPageIndex) }
    var currentBitmap by remember { mutableStateOf(initialBitmap) }
    var isInverted by remember { mutableStateOf(false) }
    var isBlackAndWhite by remember { mutableStateOf(false) }

    val cropState = remember { CropOverlayState(initialBitmap.width, initialBitmap.height) }

    LaunchedEffect(currentBitmap) {
        cropState.setSourceImageDimensions(currentBitmap.width, currentBitmap.height)
        cropState.resetToFullBounds()
    }

    val liveColorFilter = remember(isInverted, isBlackAndWhite) {
        if (!isInverted && !isBlackAndWhite) {
            null
        } else {
            val matrix = ComposeColorMatrix()
            if (isInverted) {
                matrix.timesAssign(
                    ComposeColorMatrix(
                        floatArrayOf(
                            -1f, 0f, 0f, 0f, 255f,
                            0f, -1f, 0f, 0f, 255f,
                            0f, 0f, -1f, 0f, 255f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            if (isBlackAndWhite) {
                val sat = ComposeColorMatrix()
                sat.setToSaturation(0f)
                matrix.timesAssign(sat)
            }
            ColorFilter.colorMatrix(matrix)
        }
    }

    fun extractCropped(): Bitmap? {
        val rect = cropState.calculateCropRect(currentBitmap.width, currentBitmap.height)
        if (rect.width() <= 10 || rect.height() <= 10) return null
        return try {
            Bitmap.createBitmap(currentBitmap, rect.left, rect.top, rect.width(), rect.height())
        } catch (_: Exception) {
            null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .safeDrawingPadding(),
            color = Color(0xFF121212)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color(0xFF1F1F1F))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("crop_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Text(
                        text = "Page ${currentPage + 1}/$totalPages",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                isInverted = !isInverted
                                Toast.makeText(
                                    context,
                                    if (isInverted) "ইনভার্ট কালার চালু করা হয়েছে" else "ইনভার্ট কালার বন্ধ করা হয়েছে",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isInverted) Color(0xFF2E7D32) else Color(0xFF333333),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(19.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("crop_inv_button")
                        ) {
                            Text(if (isInverted) "Inv ✓" else "Inv", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                isBlackAndWhite = !isBlackAndWhite
                                Toast.makeText(
                                    context,
                                    if (isBlackAndWhite) "ব্ল্যাক অ্যান্ড হোয়াইট মোড চালু হয়েছে" else "রঙিন মোড চালু হয়েছে",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBlackAndWhite) Color(0xFF2E7D32) else Color(0xFF333333),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(19.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("crop_bw_button")
                        ) {
                            Text(if (isBlackAndWhite) "B&W ✓" else "B&W", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val cropped = extractCropped()
                                if (cropped == null) {
                                    Toast.makeText(context, "ক্রপ ফ্রেম নির্বাচন করুন", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    val saved = withContext(Dispatchers.IO) {
                                        val filtered = applyFilterToBitmap(cropped, isInverted, isBlackAndWhite)
                                        saveBitmapToGallery(context, filtered)
                                    }
                                    Toast.makeText(
                                        context,
                                        if (saved) "✓ ক্রপ করা ছবি সরাসরি গ্যালারিতে সেভ হয়েছে!" else "গ্যালারিতে সেভ করতে সমস্যা হয়েছে",
                                        if (saved) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(19.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("crop_download_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val cropped = extractCropped()
                                if (cropped == null) {
                                    Toast.makeText(context, "ক্রপ ফ্রেম নির্বাচন করুন", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    val saved = withContext(Dispatchers.IO) {
                                        val processed = processDw2Pipeline(cropped)
                                        if (processed != null) {
                                            saveTransparentBitmapLossless(context, processed)
                                        } else {
                                            false
                                        }
                                    }
                                    Toast.makeText(
                                        context,
                                        if (saved) "✓ (dw:2) স্বচ্ছ ব্যাকগ্রাউন্ড ও কালো কালিতে সেভ হয়েছে (Lossless PNG)!" else "গ্যালারিতে সেভ করতে সমস্যা হয়েছে",
                                        if (saved) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B5E20),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(19.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("crop_dw2_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("(dw:2)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val cropped = extractCropped()
                                if (cropped == null) {
                                    Toast.makeText(context, "ক্রপ ফ্রেম নির্বাচন করুন", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    val savedFile = withContext(Dispatchers.IO) {
                                        val filtered = applyFilterToBitmap(cropped, isInverted, isBlackAndWhite)
                                        PdfUtils.saveBitmapToCache(context, filtered, "pdf_crop_")
                                    }
                                    if (savedFile != null) {
                                        PdfUtils.shareSnippetImage(context, savedFile)
                                        Toast.makeText(context, "প্রম্পট কপি করা হয়েছে এবং AI শেয়ার উইন্ডো খোলা হয়েছে!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "ইমেজ সংরক্ষণ করতে ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryRedLight,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(19.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("crop_share_top_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share to AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Page Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFF161616))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val newIdx = currentPage - 1
                            if (newIdx >= 0) {
                                scope.launch {
                                    val bmp = loadPageBitmap(newIdx)
                                    if (bmp != null) {
                                        currentPage = newIdx
                                        currentBitmap = bmp
                                    } else {
                                        Toast.makeText(context, "পেজ লোড করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = currentPage > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2C2C2C),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF202020),
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("crop_prev_page_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("পূর্ববর্তী", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Page ${currentPage + 1}/$totalPages",
                        color = Color(0xFFE0E0E0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Button(
                        onClick = {
                            val newIdx = currentPage + 1
                            if (newIdx < totalPages) {
                                scope.launch {
                                    val bmp = loadPageBitmap(newIdx)
                                    if (bmp != null) {
                                        currentPage = newIdx
                                        currentBitmap = bmp
                                    } else {
                                        Toast.makeText(context, "পেজ লোড করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = currentPage < totalPages - 1,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2C2C2C),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF202020),
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("crop_next_page_button")
                    ) {
                        Text("পরবর্তী", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                // Crop Stage
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Page to crop",
                        contentScale = ContentScale.Fit,
                        colorFilter = liveColorFilter,
                        modifier = Modifier.fillMaxSize()
                    )
                    CropOverlayCanvas(state = cropState, modifier = Modifier.fillMaxSize())
                }

                // Bottom Controls & AI Prompt Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E))
                        .padding(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                        border = BorderStroke(1.dp, Color(0xFF3E3E3E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "AI Prompt (Gemini / ChatGPT Chat Box):",
                                color = Color(0xFF90CAF9),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = PdfUtils.AI_PROMPT,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                if (cb != null) {
                                    cb.setPrimaryClip(ClipData.newPlainText("AI Prompt", PdfUtils.AI_PROMPT))
                                    Toast.makeText(context, "AI Prompt Copied: ${PdfUtils.AI_PROMPT}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFF555555)),
                            shape = RoundedCornerShape(19.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("crop_copy_prompt_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFE0E0E0), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy Prompt", color = Color(0xFFE0E0E0), fontSize = 11.sp)
                        }

                        Spacer(Modifier.width(6.dp))

                        Button(
                            onClick = {
                                val cropped = extractCropped()
                                if (cropped != null) {
                                    scope.launch {
                                        val savedFile = withContext(Dispatchers.IO) {
                                            val filtered = applyFilterToBitmap(cropped, isInverted, isBlackAndWhite)
                                            PdfUtils.saveBitmapToCache(context, filtered, "saved_crop_")
                                        }
                                        if (savedFile != null) {
                                            Toast.makeText(context, "ক্রপ সংরক্ষণ করা হয়েছে: ${savedFile.name}", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "সংরক্ষণ করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "সংরক্ষণ করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3A3A3A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(19.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("crop_save_button")
                        ) {
                            Text("Save", fontSize = 11.sp)
                        }

                        Spacer(Modifier.width(6.dp))

                        Button(
                            onClick = {
                                val cropped = extractCropped()
                                if (cropped == null) {
                                    Toast.makeText(context, "ক্রপ ফ্রেম নির্বাচন করুন", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    val savedFile = withContext(Dispatchers.IO) {
                                        val filtered = applyFilterToBitmap(cropped, isInverted, isBlackAndWhite)
                                        PdfUtils.saveBitmapToCache(context, filtered, "pdf_crop_")
                                    }
                                    if (savedFile != null) {
                                        PdfUtils.shareSnippetImage(context, savedFile)
                                        Toast.makeText(context, "প্রম্পট কপি করা হয়েছে এবং AI শেয়ার উইন্ডো খোলা হয়েছে!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "ইমেজ সংরক্ষণ করতে ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryRedLight,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(19.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("crop_share_bottom_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share to AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun getInvertColorMatrix(): ColorMatrix {
    return ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

private fun applyFilterToBitmap(src: Bitmap, invert: Boolean, bw: Boolean): Bitmap {
    if (!invert && !bw) return src
    return try {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val matrix = ColorMatrix()
        if (invert && bw) {
            matrix.postConcat(getInvertColorMatrix())
            val sat = ColorMatrix().apply { setSaturation(0f) }
            matrix.postConcat(sat)
        } else if (invert) {
            matrix.postConcat(getInvertColorMatrix())
        } else {
            matrix.setSaturation(0f)
        }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        result
    } catch (_: Exception) {
        src
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
    val fileName = "PDF_Crop_${System.currentTimeMillis()}.jpg"
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PDF_Gallery")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                return true
            }
            false
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val subDir = File(picturesDir, "PDF_Gallery").apply { if (!exists()) mkdirs() }
            val imageFile = File(subDir, fileName)
            FileOutputStream(imageFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                fos.flush()
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(imageFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun processDw2Pipeline(source: Bitmap): Bitmap? {
    return try {
        val width = source.width
        val height = source.height
        val invBwBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(invBwBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val matrix = ColorMatrix().apply {
            postConcat(getInvertColorMatrix())
            postConcat(ColorMatrix().apply { setSaturation(0f) })
        }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)

        val pixels = IntArray(width * height)
        invBwBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val bgThreshold = 225
        val fgThreshold = 165
        val range = (bgThreshold - fgThreshold).toFloat()

        for (i in pixels.indices) {
            val luminance = pixels[i] and 0xFF
            pixels[i] = when {
                luminance >= bgThreshold -> 0x00000000
                luminance <= fgThreshold -> 0xFF000000.toInt()
                else -> {
                    val factor = (bgThreshold - luminance) / range
                    val alpha = (255f * factor).roundToInt().coerceIn(0, 255)
                    alpha shl 24
                }
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        invBwBitmap.recycle()
        result
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun saveTransparentBitmapLossless(context: Context, bitmap: Bitmap): Boolean {
    val fileName = "PDF_DW2_${System.currentTimeMillis()}.png"
    var fos: OutputStream? = null
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PDF_Gallery")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                fos = context.contentResolver.openOutputStream(uri)
                fos?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                return true
            }
            false
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val subDir = File(picturesDir, "PDF_Gallery").apply { if (!exists()) mkdirs() }
            val imageFile = File(subDir, fileName)
            FileOutputStream(imageFile).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.flush()
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(imageFile.absolutePath),
                arrayOf("image/png"),
                null
            )
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
