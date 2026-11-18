package com.example

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private lateinit var layoutParams: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
        startForegroundServiceNotification()

        if (Settings.canDrawOverlays(this)) {
            initFloatingBubble()
        } else {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PDF Assistant Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating assistant button for PDF screen snippets"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingBubbleService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.floating_overlay_title))
            .setContentText(getString(R.string.floating_overlay_desc))
            .setSmallIcon(R.drawable.ic_crop)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_close, "Disable (বন্ধ করুন)", stopPendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initFloatingBubble() {
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val themedContext = ContextThemeWrapper(this, R.style.Theme_MyApplication)
            floatingView = LayoutInflater.from(themedContext).inflate(R.layout.layout_floating_button, null)

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 24
                y = 350
            }

            val view = floatingView ?: return
            val cardButton = view.findViewById<View>(R.id.floatingCard)
            val ivClose = view.findViewById<ImageView>(R.id.ivFloatingClose)

            cardButton.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var touchStartTime = 0L

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = layoutParams.x
                            initialY = layoutParams.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            touchStartTime = System.currentTimeMillis()
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                            layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                            try {
                                windowManager?.updateViewLayout(floatingView, layoutParams)
                            } catch (_: Exception) {}
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            val duration = System.currentTimeMillis() - touchStartTime
                            val diffX = abs(event.rawX - initialTouchX)
                            val diffY = abs(event.rawY - initialTouchY)
                            if (duration < 350 && diffX < 15 && diffY < 15) {
                                triggerSnippetCapture()
                            }
                            return true
                        }
                    }
                    return false
                }
            })

            ivClose.setOnClickListener {
                Toast.makeText(this, "ভাসমান বাটন বন্ধ করা হয়েছে (Floating button disabled)", Toast.LENGTH_SHORT).show()
                stopSelf()
            }

            cardButton.setOnLongClickListener {
                Toast.makeText(this, "ভাসমান বাটন বন্ধ করা হয়েছে (Floating button disabled)", Toast.LENGTH_SHORT).show()
                stopSelf()
                true
            }

            windowManager?.addView(floatingView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerSnippetCapture() {
        Toast.makeText(this, "Opening screen snippet frame...", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, ScreenSnippetActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && ACTION_STOP == intent.action) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        if (windowManager != null && floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (_: Exception) {}
        }
        floatingView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.example.ACTION_STOP"
        const val ACTION_START_PROJECTION = "com.example.ACTION_START_PROJECTION"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private const val CHANNEL_ID = "pdf_floating_assistant_channel"
        private const val NOTIFICATION_ID = 2001

        @Volatile
        @JvmField
        var isServiceRunning = false
    }
}
