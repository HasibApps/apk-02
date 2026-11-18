package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.PdfGalleryTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.net.URLEncoder

class GoogleLensActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
        )
        super.onCreate(savedInstanceState)

        val imagePath = intent?.getStringExtra(EXTRA_IMAGE_PATH)
        val pageNumber = intent?.getIntExtra(EXTRA_PAGE_NUMBER, 1) ?: 1

        if (imagePath.isNullOrEmpty()) {
            Toast.makeText(this, "ইমেজ পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val imgFile = File(imagePath)
        if (!imgFile.exists()) {
            Toast.makeText(this, "ইমেজ ফাইল অনুপস্থিত", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val bitmap = BitmapFactory.decodeFile(imagePath)
        if (bitmap == null) {
            Toast.makeText(this, "ইমেজ ডিকোড করতে সমস্যা", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            PdfGalleryTheme(darkTheme = true) {
                GoogleLensScreen(
                    imagePath = imagePath,
                    pageNumber = pageNumber,
                    bitmap = bitmap,
                    onClose = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_PAGE_NUMBER = "extra_page_number"
    }
}

@Composable
private fun GoogleLensScreen(
    imagePath: String,
    pageNumber: Int,
    bitmap: Bitmap,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isScanning by remember { mutableStateOf(true) }
    var scanStatus by remember { mutableStateOf("স্ক্যান করা হচ্ছে...") }
    var extractedText by remember { mutableStateOf("") }
    var displayContent by remember { mutableStateOf("Google Lens টেক্সট স্ক্যান করছে...") }

    LaunchedEffect(imagePath, bitmap) {
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    isScanning = false
                    val text = visionText.text
                    extractedText = text
                    if (text.isEmpty()) {
                        scanStatus = "কোনো টেক্সট পাওয়া যায়নি"
                        displayContent = "এই পৃষ্ঠায় কোনো টেক্সট শনাক্ত করা যায়নি।"
                    } else {
                        scanStatus = "স্ক্যান সম্পন্ন (${text.length} অক্ষর)"
                        displayContent = text
                    }
                }
                .addOnFailureListener {
                    isScanning = false
                    scanStatus = "স্ক্যানিং সম্পন্ন হয়নি"
                    displayContent = "টেক্সট শনাক্ত করতে সমস্যা হয়েছে।"
                }
        } catch (e: Exception) {
            e.printStackTrace()
            isScanning = false
            scanStatus = "স্ক্যানার চালু সম্ভব হয়নি"
            displayContent = "স্ক্যানার চালাতে সমস্যা হয়েছে।"
        }
    }

    fun copyTextToClipboard() {
        if (extractedText.isEmpty()) {
            Toast.makeText(context, "কপি করার মতো টেক্সট পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Google Lens OCR Text", extractedText))
            Toast.makeText(context, "পেজের সকল টেক্সট ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareExtractedText() {
        if (extractedText.isEmpty()) {
            Toast.makeText(context, "শেয়ার করার মতো টেক্সট নেই", Toast.LENGTH_SHORT).show()
            return
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, extractedText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "টেক্সট শেয়ার করুন"))
    }

    fun translateExtractedText() {
        if (extractedText.isEmpty()) {
            Toast.makeText(context, "অনুবাদ করার টেক্সট নেই", Toast.LENGTH_SHORT).show()
            return
        }
        copyTextToClipboard()
        try {
            val snippet = extractedText.substring(0, extractedText.length.coerceAtMost(500))
            val query = URLEncoder.encode(snippet, "UTF-8")
            val translateIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://translate.google.com/?sl=auto&tl=en&text=$query")
            )
            context.startActivity(translateIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "টেক্সট কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    fun openInOfficialLensSafely() {
        val file = File(imagePath)
        if (!file.exists()) return
        val contentUri = try {
            FileProvider.getUriForFile(context, PdfUtils.fileProviderAuthority(context), file)
        } catch (_: Exception) {
            null
        }
        val pm = context.packageManager

        if (contentUri != null) {
            try {
                val lensDirect = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    component = ComponentName(
                        "com.google.ar.lens",
                        "com.google.vr.apps.ornament.app.lens.LensLauncherActivity"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (lensDirect.resolveActivity(pm) != null) {
                    context.startActivity(lensDirect)
                    return
                }
            } catch (_: Exception) {}

            try {
                val lensView = Intent("com.google.ar.lens.VIEW_ACTION").apply {
                    setDataAndType(contentUri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (lensView.resolveActivity(pm) != null) {
                    context.startActivity(lensView)
                    return
                }
            } catch (_: Exception) {}

            val lensActivities = arrayOf(
                "com.google.android.apps.search.lens.LensShareActivity",
                "com.google.android.apps.search.lens.LensActivity",
                "com.google.android.apps.gsa.staticplugins.opa.lens.OpaLensActivity"
            )
            for (act in lensActivities) {
                try {
                    val gIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        component = ComponentName("com.google.android.googlequicksearchbox", act)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (gIntent.resolveActivity(pm) != null) {
                        context.startActivity(gIntent)
                        return
                    }
                } catch (_: Exception) {}
            }
        }

        copyTextToClipboard()
        Toast.makeText(context, "ইন-অ্যাপ Google Lens এ পেজের সকল টেক্সট কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(Color(0xFF121212))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("lens_close_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Lens", tint = Color.White)
            }
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "Google Lens",
                tint = Color(0xFF64B5F6),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Google Lens • P.$pageNumber",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { copyTextToClipboard() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E50), contentColor = Color(0xFF90CAF9)),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("lens_top_copy_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("সব কপি", fontSize = 12.sp)
            }
            IconButton(
                onClick = { openInOfficialLensSafely() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("lens_open_official_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in Google Lens", tint = Color.White)
            }
        }

        // Image preview stage
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF050505)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF Page",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            if (isScanning) {
                CircularProgressIndicator(
                    color = Color(0xFF64B5F6),
                    modifier = Modifier.size(52.dp)
                )
            }
            Button(
                onClick = { translateExtractedText() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F), contentColor = Color(0xFFE0F7FA)),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .height(38.dp)
                    .testTag("lens_floating_translate_button")
            ) {
                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Translate", fontSize = 12.sp)
            }
        }

        // Bottom Sheet Panel
        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .background(Color(0xFF666666), RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2A2A2A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clickable { copyTextToClipboard() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64B5F6), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Ask about this image / টেক্সট খুঁজুন...",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Lens", tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { copyTextToClipboard() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2), contentColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("lens_chip_copy_all")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("সব টেক্সট কপি করুন", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { translateExtractedText() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333), contentColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("lens_chip_translate")
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Translate (অনুবাদ)", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { shareExtractedText() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333), contentColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("lens_chip_share")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("টেক্সট শেয়ার", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { openInOfficialLensSafely() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333), contentColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("lens_chip_official")
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = Color(0xFF90CAF9), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Google Lens App", fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "পেজের সকল টেক্সট (OCR)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = scanStatus,
                        color = Color(0xFF90CAF9),
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF2A2A2A),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    SelectionContainer {
                        Text(
                            text = displayContent,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
