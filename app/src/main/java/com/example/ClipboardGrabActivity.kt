package com.example

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

/**
 * Invisible, split-second activity used to read clipboard when focus is needed
 * after Google Lens copies page text.
 */
class ClipboardGrabActivity : Activity() {

    fun interface Callback {
        fun onText(text: String)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var delivered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        handler.postDelayed({ deliver("") }, 2500)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !delivered) {
            deliver(readClipboard())
        }
    }

    private fun readClipboard(): String {
        return try {
            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (cb == null || !cb.hasPrimaryClip()) return ""
            val clip = cb.primaryClip ?: return ""
            if (clip.itemCount == 0) return ""
            clip.getItemAt(0).coerceToText(this)?.toString() ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun deliver(text: String) {
        if (delivered) return
        delivered = true
        handler.removeCallbacksAndMessages(null)
        val cb = pending
        pending = null
        cb?.onText(text)
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (!delivered) {
            delivered = true
            val cb = pending
            pending = null
            cb?.onText("")
        }
        super.onDestroy()
    }

    companion object {
        @Volatile
        @JvmField
        var pending: Callback? = null

        fun launch(ctx: Context) {
            val i = Intent(ctx, ClipboardGrabActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
            ctx.startActivity(i)
        }
    }
}
