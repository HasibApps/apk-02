package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

class LensNavigatorService : Service() {

    private val agentExecutor = Executors.newSingleThreadExecutor()
    private val executor = Executors.newSingleThreadExecutor()
    private val prefetchExecutor = Executors.newSingleThreadExecutor()
    private var prefetchFuture: Future<File?>? = null
    private var prefetchPage = -1
    private var lastAgentText = ""
    private val mainHandler = Handler(Looper.getMainLooper())

    private var windowManager: WindowManager? = null
    private var barView: View? = null
    private var tvPageInfo: TextView? = null
    private var btnPrev: ImageButton? = null
    private var btnNext: ImageButton? = null
    private var btnNote: ImageButton? = null
    private var btnCopy: ImageButton? = null
    private var btnAgent: ImageButton? = null
    private var btnStopAgent: ImageButton? = null
    private var layoutAgentPanel: View? = null
    private var tvAgentTitle: TextView? = null
    private var tvAgentPage: TextView? = null
    private var tvAgentCompleted: TextView? = null
    private var tvAgentStatus: TextView? = null
    private var pbAgent: ProgressBar? = null

    private lateinit var notesManager: PdfNotesManager
    private var noteId: String = "current_pdf"
    private var pdfName: String? = null
    private var busyLabel = "লোড হচ্ছে…"

    @Volatile
    private var agentRunning = false

    @Volatile
    private var stopRequested = false

    private var agentCompleted = 0
    private var agentStartDisplayPage = 1
    private var currentAgentDisplayPage = 1
    private var wakeLock: PowerManager.WakeLock? = null

    private var pdfPath: String? = null
    private var pdfUri: Uri? = null
    private var currentPage = 0
    private var totalPages = 0
    private var busy = false
    private var lastImage: File? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        instance = this
        notesManager = PdfNotesManager(applicationContext)
        createNotificationChannel()
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && ACTION_STOP == intent.action) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent != null) {
            pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
            val uriStr = intent.getStringExtra(EXTRA_PDF_URI)
            pdfUri = uriStr?.let { Uri.parse(it) }
            currentPage = intent.getIntExtra(EXTRA_PAGE_INDEX, 0)
            totalPages = intent.getIntExtra(EXTRA_TOTAL_PAGES, 0)
            intent.getStringExtra(EXTRA_NOTE_ID)?.let { noteId = it }
            intent.getStringExtra(EXTRA_PDF_NAME)?.let { pdfName = it }
        }
        if (pdfUri == null && pdfPath != null) {
            pdfUri = Uri.fromFile(File(pdfPath!!))
        }
        if (noteId == "current_pdf") {
            noteId = pdfPath ?: pdfName ?: "current_pdf"
        }
        if (totalPages <= 0 && pdfUri != null) {
            totalPages = PdfUtils.getPageCount(this, pdfUri!!)
        }
        showBar()
        updateUi()
        return START_NOT_STICKY
    }

    private fun showBar() {
        if (barView != null) return
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val themed = ContextThemeWrapper(this, R.style.Theme_MyApplication)
            val root = LayoutInflater.from(themed).inflate(R.layout.layout_lens_nav_bar, null)
            barView = root

            tvPageInfo = root.findViewById(R.id.tvLensNavPage)
            btnPrev = root.findViewById(R.id.btnLensNavPrev)
            btnNext = root.findViewById(R.id.btnLensNavNext)
            val btnClose = root.findViewById<View>(R.id.btnLensNavClose)
            btnNote = root.findViewById(R.id.btnLensNavNote)
            btnCopy = root.findViewById(R.id.btnLensNavCopy)
            btnAgent = root.findViewById(R.id.btnLensNavAgent)
            btnStopAgent = root.findViewById(R.id.btnLensNavStopAgent)
            layoutAgentPanel = root.findViewById(R.id.layoutAgentPanel)
            tvAgentTitle = root.findViewById(R.id.tvAgentTitle)
            tvAgentPage = root.findViewById(R.id.tvAgentPage)
            tvAgentCompleted = root.findViewById(R.id.tvAgentCompleted)
            tvAgentStatus = root.findViewById(R.id.tvAgentStatus)
            pbAgent = root.findViewById(R.id.pbAgent)

            if (resources.configuration.screenWidthDp < 380) {
                root.findViewById<View>(R.id.ivLensNavLogo)?.visibility = View.GONE
            }

            btnPrev?.setOnClickListener { goToPage(currentPage - 1) }
            btnNext?.setOnClickListener { goToPage(currentPage + 1) }
            btnClose.setOnClickListener { stopSelf() }
            btnNote?.setOnClickListener { onNoteClicked() }
            btnCopy?.setOnClickListener { onCopyClicked() }
            btnAgent?.setOnClickListener { promptAndStartAgent() }
            btnStopAgent?.setOnClickListener { requestStopAgent() }

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = getStatusBarHeight()
            }
            windowManager?.addView(root, lp)
        } catch (e: Exception) {
            e.printStackTrace()
            barView = null
            stopSelf()
        }
    }

    private fun getStatusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    private fun updateUi() {
        val pageTv = tvPageInfo ?: return
        pageTv.text = when {
            busy -> busyLabel
            totalPages > 0 -> PdfUtils.toBengaliDigits("পৃষ্ঠা ${currentPage + 1} / $totalPages")
            else -> PdfUtils.toBengaliDigits("পৃষ্ঠা ${currentPage + 1}")
        }
        val idle = !busy && !agentRunning
        setEnabledAlpha(btnPrev, idle && currentPage > 0)
        setEnabledAlpha(btnNext, idle && (totalPages <= 0 || currentPage < totalPages - 1))
        setEnabledAlpha(btnNote, idle)
        setEnabledAlpha(btnCopy, idle)
        setEnabledAlpha(btnAgent, !busy)
    }

    private fun setEnabledAlpha(v: View?, enabled: Boolean) {
        v ?: return
        v.isEnabled = enabled
        v.alpha = if (enabled) 1f else 0.35f
    }

    private fun goToPage(target: Int) {
        val uri = pdfUri ?: return
        if (busy || agentRunning) return
        if (target < 0 || (totalPages > 0 && target >= totalPages)) return
        busy = true
        updateUi()

        executor.execute {
            val bmp = PdfUtils.renderPageToBitmap(applicationContext, uri, target, RENDER_WIDTH)
            val file = bmp?.let {
                PdfUtils.saveBitmapJpegToCache(applicationContext, it, "lens_page_${target + 1}_")
            }
            bmp?.recycle()

            mainHandler.post {
                busy = false
                if (file == null) {
                    Toast.makeText(this, "পেজ রেন্ডার করা যায়নি", Toast.LENGTH_SHORT).show()
                    updateUi()
                    return@post
                }
                val launched = PdfUtils.launchOfficialGoogleLens(applicationContext, file.absolutePath)
                if (launched) {
                    currentPage = target
                    if (lastImage != null && lastImage != file) {
                        lastImage?.delete()
                    }
                    lastImage = file
                } else {
                    Toast.makeText(this, "Google Lens চালু করা যায়নি", Toast.LENGTH_SHORT).show()
                }
                updateUi()
            }
        }
    }

    private fun onNoteClicked() {
        if (agentRunning) {
            Toast.makeText(this, "Agent চলছে — নোট খুলতে আগে Stop করুন", Toast.LENGTH_SHORT).show()
            return
        }
        if (busy) return
        openNoteScreen()
    }

    private fun openNoteScreen() {
        setBarVisible(false)
        try {
            LensNoteActivity.launch(applicationContext, noteId, pdfName)
        } catch (e: Exception) {
            e.printStackTrace()
            setBarVisible(true)
            Toast.makeText(this, "নোট খোলা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onCopyClicked() {
        if (busy || agentRunning || pdfUri == null) return
        if (!ensureLensAccessibility()) return
        val page = currentPage
        busy = true
        busyLabel = "Lens থেকে লেখা কপি হচ্ছে…"
        updateUi()

        executor.execute {
            var message: String
            var openNote = false
            try {
                if (notesManager.isPagePasted(noteId, page)) {
                    message = "পৃষ্ঠা ${page + 1} আগেই নোটে যুক্ত আছে"
                    openNote = true
                } else {
                    val text = grabTextFromLens()
                    if (text.isEmpty()) {
                        message = "Lens থেকে লেখা কপি করা যায়নি — Lens পেজে \"Select text\" বাটন আছে কিনা দেখুন"
                    } else if (appendWithRetry(page, text)) {
                        message = "✓ পৃষ্ঠা ${page + 1} কপি হয়ে নোটে যুক্ত হয়েছে"
                        openNote = true
                    } else {
                        message = "নোটে যুক্ত করা যায়নি (লেখা ক্লিপবোর্ডে কপি আছে)"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                message = "Lens থেকে টেক্সট কপি করা যায়নি"
            }

            mainHandler.post {
                busy = false
                busyLabel = "লোড হচ্ছে…"
                updateUi()
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                if (openNote) openNoteScreen()
            }
        }
    }

    private fun ensureLensAccessibility(): Boolean {
        if (LensAccessibilityService.get() != null) return true
        Toast.makeText(
            this,
            "Accessibility তে \"PDF Gallery – Lens Auto Copy\" চালু করুন, তারপর আবার কপি চাপুন",
            Toast.LENGTH_LONG
        ).show()
        try {
            val i = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(i)
        } catch (_: Exception) {}
        return false
    }

    private fun grabTextFromLens(): String {
        val acc = LensAccessibilityService.get() ?: return ""

        val primed = CountDownLatch(1)
        mainHandler.post {
            try {
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cb?.setPrimaryClip(ClipData.newPlainText("pdfgallery", CLIP_SENTINEL))
            } catch (_: Exception) {}
            primed.countDown()
        }
        awaitQuietly(primed, 2)

        val copied = acc.copyAllLensText(LENS_COPY_TIMEOUT_MS) { stopRequested }
        if (!copied) return ""

        val direct = acc.readClipboardDirect().trim()
        if (direct.isNotEmpty() && !direct.contains("__PDFGALLERY_LENS_PENDING__")) return direct

        val got = CountDownLatch(1)
        val ref = AtomicReference("")
        ClipboardGrabActivity.pending = ClipboardGrabActivity.Callback { t ->
            ref.set(t)
            got.countDown()
        }
        mainHandler.post {
            try {
                ClipboardGrabActivity.launch(applicationContext)
            } catch (_: Exception) {
                got.countDown()
            }
        }
        awaitQuietly(got, 6)

        val text = ref.get().trim()
        return if (text.contains("__PDFGALLERY_LENS_PENDING__")) "" else text
    }

    private fun awaitQuietly(latch: CountDownLatch, seconds: Long) {
        try {
            latch.await(seconds, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            stopRequested = true
        }
    }

    private fun appendWithRetry(page: Int, text: String): Boolean {
        for (i in 0 until MAX_RETRIES) {
            if (notesManager.appendPageToUnlimitedNote(noteId, page, text)) return true
            if (notesManager.isPagePasted(noteId, page)) return true
            sleepQuietly(300L * (i + 1))
        }
        return false
    }

    private fun promptAndStartAgent() {
        if (pdfUri == null || agentRunning || busy) return
        setBarVisible(false)
        try {
            AgentStartPageActivity.launch(applicationContext, currentPage, totalPages)
        } catch (e: Exception) {
            e.printStackTrace()
            setBarVisible(true)
            Toast.makeText(this, "বক্স খোলা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAgent(startingDisplayPage: Int) {
        val uri = pdfUri ?: return
        if (agentRunning || busy) return
        if (totalPages <= 0) totalPages = PdfUtils.getPageCount(this, uri)
        agentStartDisplayPage = startingDisplayPage.coerceAtLeast(1)
        currentAgentDisplayPage = agentStartDisplayPage
        agentRunning = true
        stopRequested = false
        agentCompleted = 0
        acquireWakeLock()
        showAgentRunningUi()
        agentExecutor.execute { runAgent() }
    }

    private fun requestStopAgent() {
        if (!agentRunning || stopRequested) return
        stopRequested = true
        setAgentStatus("Stopping...")
        btnStopAgent?.isEnabled = false
        btnStopAgent?.alpha = 0.5f
    }

    private fun runAgent() {
        val total = totalPages
        var endTitle = "🤖 Agent Stopped"
        var endStatus = "Stopped"
        var finishedAll = false
        try {
            if (notesManager.wasAgentInterrupted(noteId)) {
                mainHandler.post {
                    Toast.makeText(
                        this,
                        "আগের অসমাপ্ত কাজ থেকে চালিয়ে যাওয়া হচ্ছে",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            notesManager.saveAgentProgress(noteId, notesManager.getAgentLastCompleted(noteId), true)

            if (LensAccessibilityService.get() == null) {
                throw AgentFailure("Accessibility সার্ভিস চালু নেই")
            }
            sleepQuietly(350)
            setAgentStatus("Reading PDF Page")

            var page = nextUnprocessed(currentPage, total)
            if (page < total && page != currentPage) {
                if (!loadPage(page, true)) throw AgentFailure("পৃষ্ঠা ${page + 1} লোড করা যায়নি")
            }

            while (page < total && !stopRequested) {
                setAgentPage(page)
                setAgentStatus("Selecting & Copying Text in Lens")
                startPrefetch(nextUnprocessed(page + 1, total))
                var text = grabTextFromLens()
                if (text.isNotEmpty() && text == lastAgentText && !stopRequested) {
                    sleepQuietly(1200)
                    text = grabTextFromLens()
                    if (text == lastAgentText) text = ""
                }
                if (text.isNotEmpty()) lastAgentText = text
                if (stopRequested) break

                if (text.isNotEmpty()) {
                    val notePageNumber = currentAgentDisplayPage
                    val formattedText = PdfNotesManager.formatAgentPageText(notePageNumber, text)
                    setAgentStatus("Opening Unlimited Note")
                    notesManager.getUnlimitedNoteForPdf(noteId)
                    setAgentStatus("Pasting Text [ পৃষ্ঠা : ${PdfNotesManager.toBengaliDigits(notePageNumber)} ]")
                    if (!appendWithRetry(page, formattedText)) {
                        throw AgentFailure("পৃষ্ঠা ${page + 1} নোটে পেস্ট করা যায়নি")
                    }
                    currentAgentDisplayPage++
                }
                agentCompleted++
                notesManager.saveAgentProgress(noteId, page, true)
                setAgentCompleted()
                if (stopRequested) break

                val next = nextUnprocessed(page + 1, total)
                if (next >= total) {
                    page = total
                    break
                }
                setAgentStatus("Loading Next Page")
                if (!loadPage(next, true)) throw AgentFailure("পৃষ্ঠা ${next + 1} লোড করা যায়নি")
                page = next
            }

            finishedAll = !stopRequested && page >= total
            if (finishedAll) {
                endTitle = "🤖 Agent Finished"
                endStatus = "All Pages Successfully Processed!"
                setAgentStatus("Completed")
                notesManager.saveAgentProgress(noteId, total - 1, false)
            } else {
                notesManager.saveAgentProgress(noteId, notesManager.getAgentLastCompleted(noteId), false)
            }
        } catch (f: AgentFailure) {
            endTitle = "🤖 Agent Paused"
            endStatus = "${f.message} — আবার Agent চালু করুন"
            notesManager.saveAgentProgress(noteId, notesManager.getAgentLastCompleted(noteId), false)
        } catch (t: Throwable) {
            t.printStackTrace()
            endTitle = "🤖 Agent Paused"
            endStatus = "ত্রুটি হয়েছে — আবার Agent চালু করুন"
            notesManager.saveAgentProgress(noteId, notesManager.getAgentLastCompleted(noteId), false)
        }

        val finalTitle = endTitle
        val finalStatus = endStatus
        val done = finishedAll
        mainHandler.post { showAgentEndedUi(finalTitle, finalStatus, done) }
    }

    private class AgentFailure(msg: String) : RuntimeException(msg)

    private fun nextUnprocessed(from: Int, total: Int): Int {
        val pasted = notesManager.getPastedPages(noteId)
        if (pasted.isNotEmpty() && notesManager.getUnlimitedNoteForPdf(noteId).trim().isEmpty()) {
            notesManager.clearPastedPages(noteId)
            pasted.clear()
        }
        var p = max(0, from)
        while (p < total && pasted.contains(p)) p++
        return p
    }

    private fun startPrefetch(page: Int) {
        val uri = pdfUri ?: return
        if (page < 0 || (totalPages > 0 && page >= totalPages)) return
        prefetchPage = page
        prefetchFuture = prefetchExecutor.submit<File?> { renderLensImage(uri, page) }
    }

    private fun renderLensImage(uri: Uri, page: Int): File? {
        val bmp = PdfUtils.renderPageToBitmap(applicationContext, uri, page, RENDER_WIDTH) ?: return null
        return try {
            PdfUtils.saveBitmapJpegToCache(applicationContext, bmp, "lens_page_${page + 1}_")
        } finally {
            bmp.recycle()
        }
    }

    private fun loadPage(page: Int, showInLens: Boolean): Boolean {
        val uri = pdfUri ?: return false
        if (showInLens) {
            var file: File? = null
            if (prefetchFuture != null && prefetchPage == page) {
                try {
                    file = prefetchFuture?.get(8, TimeUnit.SECONDS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    stopRequested = true
                } catch (_: ExecutionException) {
                } catch (_: TimeoutException) {}
                prefetchFuture = null
                prefetchPage = -1
            }
            var i = 0
            while (i < MAX_RETRIES && file == null && !stopRequested) {
                file = renderLensImage(uri, page)
                if (file == null) sleepQuietly(300L * (i + 1))
                i++
            }
            if (file == null) return false
            if (PdfUtils.launchOfficialGoogleLens(applicationContext, file.absolutePath)) {
                if (lastImage != null && lastImage != file) {
                    lastImage?.delete()
                }
                lastImage = file
                sleepQuietly(LENS_SETTLE_MS)
            }
        }
        currentPage = page
        setAgentPage(page)
        return true
    }

    private fun sleepQuietly(ms: Long) {
        try {
            Thread.sleep(ms)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            stopRequested = true
        }
    }

    private fun showAgentRunningUi() {
        btnAgent?.visibility = View.GONE
        btnStopAgent?.visibility = View.VISIBLE
        btnStopAgent?.isEnabled = true
        btnStopAgent?.alpha = 1f
        layoutAgentPanel?.visibility = View.VISIBLE
        tvAgentTitle?.text = "🤖 Agent Running..."
        tvAgentStatus?.text = "Status: Starting..."
        setAgentPage(currentPage)
        setAgentCompleted()
        updateUi()
    }

    private fun showAgentEndedUi(title: String, status: String, done: Boolean) {
        agentRunning = false
        stopRequested = false
        releaseWakeLock()
        btnStopAgent?.visibility = View.GONE
        btnAgent?.visibility = View.VISIBLE
        tvAgentTitle?.text = title
        tvAgentStatus?.text = "Status: $status"
        setAgentCompleted()
        if (done) {
            pbAgent?.progress = 100
            Toast.makeText(this, "All Pages Successfully Processed!", Toast.LENGTH_LONG).show()
        }
        updateUi()
        val panel = layoutAgentPanel
        mainHandler.postDelayed({
            if (!agentRunning && panel != null) panel.visibility = View.GONE
        }, if (done) 8000L else 5000L)
    }

    private fun setAgentStatus(status: String) {
        mainHandler.post {
            val suffix = if (status.endsWith("...") || status == "Completed") "" else "..."
            tvAgentStatus?.text = "Status: $status$suffix"
        }
    }

    private fun setAgentPage(page: Int) {
        val total = totalPages
        val noteTag = "[ পৃষ্ঠা : ${PdfNotesManager.toBengaliDigits(currentAgentDisplayPage)} ]"
        mainHandler.post {
            tvAgentPage?.text = "Current Page: ${page + 1} / $total • নোট: $noteTag"
            if (total > 0) pbAgent?.progress = (page * 100L / total).toInt()
            updateUi()
        }
    }

    private fun setAgentCompleted() {
        val done = agentCompleted
        mainHandler.post {
            tvAgentCompleted?.text = "Completed Pages: $done"
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pdfgallery:lensagent")?.apply {
                acquire(6 * 60 * 60 * 1000L)
            }
        } catch (_: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lens Page Navigator",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Google Lens এর উপরে পৃষ্ঠা পরিবর্তনের বার"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun startAsForeground() {
        val stopIntent = Intent(this, LensNavigatorService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PDF Gallery")
            .setContentText("Google Lens পৃষ্ঠা বার চালু আছে")
            .setSmallIcon(R.drawable.ic_google_lens)
            .addAction(R.drawable.ic_close, "বন্ধ করুন", stopPi)
            .setOngoing(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, n)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        isRunning = false
        instance = null
        stopRequested = true
        releaseWakeLock()
        if (windowManager != null && barView != null) {
            try {
                windowManager?.removeView(barView)
            } catch (_: Exception) {}
        }
        barView = null
        executor.shutdownNow()
        prefetchExecutor.shutdownNow()
        agentExecutor.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.example.LENS_NAV_STOP"
        const val EXTRA_PDF_PATH = "lens_nav_pdf_path"
        const val EXTRA_PDF_URI = "lens_nav_pdf_uri"
        const val EXTRA_PAGE_INDEX = "lens_nav_page_index"
        const val EXTRA_TOTAL_PAGES = "lens_nav_total_pages"
        const val EXTRA_NOTE_ID = "lens_nav_note_id"
        const val EXTRA_PDF_NAME = "lens_nav_pdf_name"

        private const val CHANNEL_ID = "pdf_lens_navigator_channel"
        private const val NOTIFICATION_ID = 2002
        private const val RENDER_WIDTH = 1600
        private const val CLIP_SENTINEL = "\u200B__PDFGALLERY_LENS_PENDING__\u200B"
        private const val LENS_COPY_TIMEOUT_MS = 20000L
        private const val MAX_RETRIES = 3
        private const val LENS_SETTLE_MS = 450L

        @Volatile
        @JvmField
        var isRunning = false

        @Volatile
        private var instance: LensNavigatorService? = null

        fun setBarVisible(visible: Boolean) {
            val svc = instance ?: return
            svc.mainHandler.post {
                svc.barView?.visibility = if (visible) View.VISIBLE else View.GONE
            }
        }

        fun startAgentWithStartingPage(startingPage: Int) {
            val svc = instance ?: return
            svc.mainHandler.post {
                svc.startAgent(startingPage.coerceAtLeast(1))
            }
        }
    }
}
