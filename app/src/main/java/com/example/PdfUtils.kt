package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PdfUtils {

    const val AI_PROMPT = "এই স্লাইডগুলোর সমস্ত হাতের + প্লেইন টেক্সট A to Z copy করে দাও"

    fun fileProviderAuthority(context: Context): String = "${context.packageName}.fileprovider"

    fun cleanPdfName(rawName: String?): String {
        if (rawName == null) return "Document.pdf"
        var cleaned = rawName.trim()
        cleaned = cleaned.replaceFirst(Regex("^(?i)dl_url_\\d+_"), "")
        cleaned = cleaned.replaceFirst(Regex("^(?i)dl_[a-zA-Z0-9]+_"), "")
        return cleaned
    }

    fun generateAndSaveFirstPageThumbnail(context: Context, pdfFile: File?, itemId: String?): String? {
        if (pdfFile == null || !pdfFile.exists()) return null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount <= 0) return null

            page = renderer.openPage(0)
            val originalW = page.width
            val originalH = page.height
            val targetWidth = 360
            val targetHeight = (originalH.toFloat() * targetWidth / originalW).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
            val safeId = (itemId ?: "pdf_${pdfFile.name.hashCode()}").replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val dest = File(coversDir, "thumb_$safeId.jpg")
            FileOutputStream(dest).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                fos.flush()
            }
            bitmap.recycle()
            dest.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    fun downloadDriveThumbnailIfAvailable(context: Context, originalUrl: String?, itemId: String?): String? {
        if (originalUrl.isNullOrEmpty()) return null
        val fileId = UrlPdfManager.extractGoogleDriveFileId(originalUrl)
        if (fileId.isNullOrEmpty()) return null

        val thumbUrl = "https://drive.google.com/thumbnail?id=$fileId&sz=w400"
        return try {
            val url = URL(thumbUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.instanceFollowRedirects = true
            conn.connect()
            if (conn.responseCode == 200) {
                val bitmap = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                conn.disconnect()
                if (bitmap != null) {
                    val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
                    val safeId = (itemId ?: "drive_$fileId").replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    val dest = File(coversDir, "thumb_$safeId.jpg")
                    FileOutputStream(dest).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                        fos.flush()
                    }
                    bitmap.recycle()
                    return dest.absolutePath
                }
            } else {
                conn.disconnect()
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun launchOfficialGoogleLens(context: Context, imagePath: String?): Boolean {
        if (imagePath.isNullOrEmpty()) return false
        val file = File(imagePath)
        if (!file.exists()) return false

        val contentUri = try {
            FileProvider.getUriForFile(context, fileProviderAuthority(context), file)
        } catch (_: Exception) {
            return false
        }

        val pm = context.packageManager

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
                return true
            }
        } catch (_: Exception) {}

        try {
            val lensView = Intent("com.google.ar.lens.VIEW_ACTION").apply {
                setDataAndType(contentUri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (lensView.resolveActivity(pm) != null) {
                context.startActivity(lensView)
                return true
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
                    return true
                }
            } catch (_: Exception) {}
        }
        return false
    }

    fun renderPageToBitmap(context: Context, uri: Uri, pageIndex: Int, targetWidth: Int): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = if (uri.scheme == "file" && uri.path != null) {
                ParcelFileDescriptor.open(File(uri.path!!), ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }
            if (pfd == null) return null

            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null

            page = renderer.openPage(pageIndex)
            var width = page.width
            var height = page.height
            if (targetWidth > 0 && width > 0) {
                val scale = targetWidth.toFloat() / width
                width = targetWidth
                height = (height * scale).toInt().coerceAtLeast(1)
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    fun getPageCount(context: Context, uri: Uri): Int {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = if (uri.scheme == "file" && uri.path != null) {
                ParcelFileDescriptor.open(File(uri.path!!), ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                return renderer.pageCount
            }
            1
        } catch (e: Exception) {
            e.printStackTrace()
            1
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    @Throws(IOException::class)
    fun compressPdfToZip(context: Context, pdfFile: PdfFile): File {
        val pdfSource = File(pdfFile.path)
        val outputZip = File(
            context.getExternalFilesDir(null),
            pdfFile.name.replace(".pdf", "") + "_compressed.zip"
        )
        FileInputStream(pdfSource).use { fis ->
            ZipOutputStream(FileOutputStream(outputZip)).use { zos ->
                zos.putNextEntry(ZipEntry(pdfFile.name))
                val buffer = ByteArray(8192)
                var length: Int
                while (fis.read(buffer).also { length = it } > 0) {
                    zos.write(buffer, 0, length)
                }
                zos.closeEntry()
            }
        }
        return outputZip
    }

    fun sharePdf(context: Context, pdfFile: PdfFile) {
        var contentUri = pdfFile.uri
        if (pdfFile.path.isNotEmpty()) {
            val file = File(pdfFile.path)
            if (file.exists()) {
                try {
                    contentUri = FileProvider.getUriForFile(context, fileProviderAuthority(context), file)
                } catch (_: Exception) {}
            }
        }
        if (contentUri == null) return

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, pdfFile.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF Document via"))
    }

    fun shareSnippetImage(context: Context, imageFile: File) {
        shareSnippetImageWithPrompt(context, imageFile, AI_PROMPT)
    }

    fun shareSnippetImageWithPrompt(context: Context, imageFile: File, prompt: String?) {
        if (!imageFile.exists()) return
        val contentUri = FileProvider.getUriForFile(context, fileProviderAuthority(context), imageFile)

        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && !prompt.isNullOrEmpty()) {
                clipboard.setPrimaryClip(ClipData.newPlainText("AI Prompt", prompt))
            }
        } catch (_: Exception) {}

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            if (!prompt.isNullOrEmpty()) {
                putExtra(Intent.EXTRA_TEXT, prompt)
                putExtra(Intent.EXTRA_SUBJECT, prompt)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Snippet to AI (Gemini) / Chat"))
    }

    fun toBengaliDigits(input: String?): String {
        if (input == null) return ""
        val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder(input.length)
        for (c in input) {
            sb.append(if (c in '0'..'9') bn[c - '0'] else c)
        }
        return sb.toString()
    }

    fun saveBitmapJpegToCache(context: Context, bitmap: Bitmap, prefix: String?): File? {
        val cacheDir = File(context.cacheDir, "snippets").apply { if (!exists()) mkdirs() }
        val file = File(cacheDir, "${prefix ?: "page_"}${System.currentTimeMillis()}.jpg")
        return try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap, prefix: String?): File? {
        val cacheDir = File(context.cacheDir, "snippets").apply { if (!exists()) mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(cacheDir, "${prefix ?: "snippet_"}$timestamp.png")
        return try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createSamplePdfFiles(context: Context): List<PdfFile> {
        val createdList = mutableListOf<PdfFile>()
        val docsDir = context.getExternalFilesDir(null) ?: context.filesDir

        createSamplePdf(
            docsDir,
            "Machine_Learning_Lecture_Notes.pdf",
            "Machine Learning: Core Principles & Architecture",
            arrayOf(
                "1. Deep Neural Networks & Backpropagation Foundations",
                "2. Attention Mechanisms and Transformer Architectures",
                "3. Model Optimization: Quantization, Pruning, and Latency Tuning",
                "4. Reinforcement Learning via Human Feedback (RLHF)",
                "5. Practical Implementations in Mobile Device Environments"
            ),
            3
        )
        createSamplePdf(
            docsDir,
            "Product_Design_Sprint_Guide.pdf",
            "Product Design Sprint: Framework & Guidelines",
            arrayOf(
                "Day 1: Map the Problem & Target Key Challenges",
                "Day 2: Sketch Competing Solutions & Ideate",
                "Day 3: Decide on Top Concepts & Storyboard",
                "Day 4: Prototype Realistic User Experiences",
                "Day 5: Test Directly with Target Customers"
            ),
            2
        )
        createSamplePdf(
            docsDir,
            "Quarterly_Financial_Summary_2026.pdf",
            "Enterprise Growth & Financial Performance Q3 2026",
            arrayOf(
                "Key Financial Highlights: Total Revenue +34% YoY",
                "Operating Efficiency: Cloud Infrastructure Margins Reaching 62%",
                "R&D Allocation: Focus on On-Device Machine Intelligence & Security",
                "Capital Expenditures & Balance Sheet Summary",
                "Forecast for Fiscal Year 2027 and Strategic Initiatives"
            ),
            4
        )

        docsDir.listFiles()?.forEach { file ->
            if (file.name.lowercase().endsWith(".pdf")) {
                val uri = FileProvider.getUriForFile(context, fileProviderAuthority(context), file)
                createdList.add(
                    PdfFile(
                        file.hashCode().toLong(),
                        file.name,
                        file.absolutePath,
                        uri,
                        file.length(),
                        file.lastModified()
                    )
                )
            }
        }
        return createdList
    }

    private fun createSamplePdf(
        dir: File,
        fileName: String,
        title: String,
        points: Array<String>,
        numPages: Int
    ) {
        val file = File(dir, fileName)
        if (file.exists() && file.length() > 0) return

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val paintTitle = Paint().apply {
            color = Color.rgb(211, 47, 47)
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val paintHeader = Paint().apply {
            color = Color.rgb(31, 41, 55)
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val paintBody = Paint().apply {
            color = Color.rgb(75, 85, 99)
            textSize = 12f
            isAntiAlias = true
        }
        val paintDivider = Paint().apply {
            color = Color.rgb(229, 231, 235)
            strokeWidth = 2f
        }
        val paintBox = Paint().apply {
            color = Color.rgb(248, 249, 250)
            style = Paint.Style.FILL
        }
        val paintBoxBorder = Paint().apply {
            color = Color.rgb(220, 225, 230)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val redBand = Paint().apply {
            color = Color.rgb(211, 47, 47)
        }

        for (p in 1..numPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, p).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 8f, redBand)
            canvas.drawText(title, 40f, 50f, paintTitle)
            canvas.drawLine(40f, 65f, (pageWidth - 40).toFloat(), 65f, paintDivider)

            canvas.drawRoundRect(40f, 85f, (pageWidth - 40).toFloat(), 150f, 8f, 8f, paintBox)
            canvas.drawRoundRect(40f, 85f, (pageWidth - 40).toFloat(), 150f, 8f, 8f, paintBoxBorder)
            canvas.drawText("Chapter $p: Detailed Analytical Review", 55f, 115f, paintHeader)
            canvas.drawText("Document Reference ID: DOC-2026-$p • Confidential", 55f, 135f, paintBody)

            var y = 185f
            for (pt in points) {
                canvas.drawCircle(50f, y - 4f, 3f, paintTitle)
                canvas.drawText(pt, 65f, y, paintHeader)
                y += 24f
                canvas.drawText("Comprehensive analysis including technical parameters, metric evaluations,", 65f, y, paintBody)
                y += 18f
                canvas.drawText("standard compliance checklists, and cross-functional performance reviews.", 65f, y, paintBody)
                y += 36f
            }

            canvas.drawLine(40f, (pageHeight - 50).toFloat(), (pageWidth - 40).toFloat(), (pageHeight - 50).toFloat(), paintDivider)
            canvas.drawText("PDF Assistant & Reader • Native Android System", 40f, (pageHeight - 30).toFloat(), paintBody)
            canvas.drawText("Page $p of $numPages", (pageWidth - 110).toFloat(), (pageHeight - 30).toFloat(), paintBody)
            document.finishPage(page)
        }

        try {
            FileOutputStream(file).use { document.writeTo(it) }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            document.close()
        }
    }
}
