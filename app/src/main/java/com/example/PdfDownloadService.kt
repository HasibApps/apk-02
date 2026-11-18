package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File

class PdfDownloadService : Service(), PdfDownloadHelper.DownloadStateListener {

    private var notificationManager: NotificationManager? = null
    private var currentTitle = "PDF Download"
    private var currentItemId: String? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        PdfDownloadHelper.getInstance(this).addStateListener(this)
    }

    override fun onDestroy() {
        PdfDownloadHelper.getInstance(this).removeStateListener(this)
        super.onDestroy()
    }

    override fun onDownloadProgress(itemId: String, percent: Int, downloadedBytes: Long, totalBytes: Long) {
        if (currentItemId == null || currentItemId == itemId) {
            updateProgress(itemId, currentTitle, percent, downloadedBytes, totalBytes)
        }
    }

    override fun onDownloadSuccess(itemId: String, file: File) {
        notifyCompleted(currentTitle, file)
    }

    override fun onDownloadError(itemId: String, errorMessage: String) {
        checkIfAnyDownloadsActive()
    }

    override fun onDownloadPausedOrCancelled(itemId: String, downloadedBytes: Long, totalBytes: Long) {
        checkIfAnyDownloadsActive()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PDF Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background PDF downloading and progress"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
            val title = intent.getStringExtra(EXTRA_ITEM_TITLE)
            when (action) {
                ACTION_START_DOWNLOAD -> {
                    currentItemId = itemId
                    if (!title.isNullOrEmpty()) currentTitle = title
                    startForegroundNotification("ডাউনলোড শুরু হচ্ছে...", 0, -1)
                }
                ACTION_CANCEL_DOWNLOAD -> {
                    if (itemId != null) {
                        PdfDownloadHelper.getInstance(this).cancelDownload(itemId)
                    }
                    checkIfAnyDownloadsActive()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun updateProgress(itemId: String, title: String?, percent: Int, downloadedBytes: Long, totalBytes: Long) {
        if (!title.isNullOrEmpty()) currentTitle = title
        currentItemId = itemId

        val sizeText = PdfDownloadHelper.formatBanglaSize(downloadedBytes)
        val progressMsg = if (totalBytes > 0) {
            val totalSizeText = PdfDownloadHelper.formatBanglaSize(totalBytes)
            val percentText = "${PdfDownloadHelper.toBanglaDigits(percent.toString())}%"
            "$sizeText / $totalSizeText ($percentText)"
        } else {
            "$sizeText ডাউনলোড হচ্ছে..."
        }
        val notification = buildNotification(progressMsg, percent, totalBytes)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun notifyCompleted(title: String?, file: File?) {
        val openIntent = Intent(this, PdfViewerActivity::class.java).apply {
            if (file != null) {
                putExtra("pdf_path", file.absolutePath)
                data = Uri.fromFile(file)
            }
        }
        val pi = PendingIntent.getActivity(
            this,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(title ?: "PDF Download")
            .setContentText("ডাউনলোড সম্পূর্ণ হয়েছে (ট্যাপ করে পড়ুন)")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager?.notify(NOTIFICATION_ID + 1, notification)
        checkIfAnyDownloadsActive()
    }

    private fun checkIfAnyDownloadsActive() {
        if (!PdfDownloadHelper.getInstance(this).hasActiveDownloads()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startForegroundNotification(contentText: String, percent: Int, totalBytes: Long) {
        val notification = buildNotification(contentText, percent, totalBytes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(contentText: String, percent: Int, totalBytes: Long): Notification {
        val appIntent = Intent(this, MainActivity::class.java)
        val openAppPi = PendingIntent.getActivity(
            this,
            100,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, PdfDownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
            currentItemId?.let { putExtra(EXTRA_ITEM_ID, it) }
        }
        val cancelPi = PendingIntent.getService(
            this,
            102,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(currentTitle)
            .setContentText(contentText)
            .setContentIntent(openAppPi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_close, "বাতিল করুন", cancelPi)

        if (totalBytes > 0 && percent >= 0) {
            builder.setProgress(100, percent, false)
        } else {
            builder.setProgress(100, 0, true)
        }
        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_DOWNLOAD = "com.example.ACTION_START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.example.ACTION_CANCEL_DOWNLOAD"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_ITEM_TITLE = "extra_item_title"
        const val CHANNEL_ID = "pdf_download_channel"
        const val NOTIFICATION_ID = 3001

        fun startDownload(context: Context, itemId: String, title: String) {
            val intent = Intent(context, PdfDownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_ITEM_ID, itemId)
                putExtra(EXTRA_ITEM_TITLE, title)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancelDownload(context: Context, itemId: String) {
            val intent = Intent(context, PdfDownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_ITEM_ID, itemId)
            }
            context.startService(intent)
        }
    }
}
