package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.BooleanSupplier

class LensAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Polled on demand
    }

    override fun onInterrupt() {}

    fun copyAllLensText(timeoutMs: Long, cancelled: BooleanSupplier?): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastSelectTap = 0L
        var selectTaps = 0

        while (System.currentTimeMillis() < deadline) {
            if (cancelled?.asBoolean == true) return false

            val roots = lensRoots()
            try {
                if (roots.isNotEmpty()) {
                    val copy = find(roots, COPY_TEXT_LABELS, COPY_EXCLUDE)
                    if (copy != null && click(copy)) {
                        sleepQuietly(60)
                        return true
                    }
                    val now = System.currentTimeMillis()
                    if (selectTaps < 12 && now - lastSelectTap > 1000) {
                        val sel = find(roots, SELECT_TEXT_LABELS, null)
                        if (sel != null && click(sel)) {
                            selectTaps++
                            lastSelectTap = now
                            sleepQuietly(100)
                            continue
                        }
                    }
                }
            } finally {
                recycleAll(roots)
            }
            sleepQuietly(70)
        }
        return false
    }

    fun readClipboardDirect(): String {
        return try {
            val cb = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
            if (cb == null || !cb.hasPrimaryClip()) return ""
            val clip = cb.primaryClip ?: return ""
            if (clip.itemCount == 0) return ""
            clip.getItemAt(0).coerceToText(this)?.toString() ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun lensRoots(): List<AccessibilityNodeInfo> {
        val out = mutableListOf<AccessibilityNodeInfo>()
        try {
            val windows = windows
            if (windows != null) {
                for (w in windows) {
                    val r = w.root ?: continue
                    if (isLensPackage(r.packageName)) out.add(r) else @Suppress("DEPRECATION") r.recycle()
                }
            }
        } catch (_: Exception) {}

        if (out.isEmpty()) {
            try {
                val r = rootInActiveWindow
                if (r != null) {
                    if (isLensPackage(r.packageName)) out.add(r) else @Suppress("DEPRECATION") r.recycle()
                }
            } catch (_: Exception) {}
        }
        return out
    }

    private fun find(
        roots: List<AccessibilityNodeInfo>,
        labels: Array<String>,
        exclude: Array<String>?
    ): AccessibilityNodeInfo? {
        val best = arrayOf<AccessibilityNodeInfo?>(null)
        val bestIdx = intArrayOf(Int.MAX_VALUE)
        for (root in roots) {
            walk(root, labels, exclude, best, bestIdx, 0)
            if (bestIdx[0] == 0) break
        }
        return best[0]
    }

    private fun walk(
        node: AccessibilityNodeInfo?,
        labels: Array<String>,
        exclude: Array<String>?,
        best: Array<AccessibilityNodeInfo?>,
        bestIdx: IntArray,
        depth: Int
    ) {
        if (node == null || depth > 40 || bestIdx[0] == 0) return
        if (node.isVisibleToUser) {
            for (i in labels.indices) {
                if (i >= bestIdx[0]) break
                if (matches(node, labels[i], exclude)) {
                    best[0] = node
                    bestIdx[0] = i
                    break
                }
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) walk(child, labels, exclude, best, bestIdx, depth + 1)
        }
    }

    private fun click(node: AccessibilityNodeInfo): Boolean {
        var n: AccessibilityNodeInfo? = node
        for (i in 0 until 6) {
            if (n == null) break
            if (n.isClickable && n.isEnabled) {
                if (n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            }
            n = n.parent
        }
        val r = Rect()
        node.getBoundsInScreen(r)
        if (r.isEmpty) return false
        return tap(r.centerX(), r.centerY())
    }

    private fun tap(x: Int, y: Int): Boolean {
        return try {
            val p = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
            val g = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(p, 0, 60))
                .build()
            val done = CountDownLatch(1)
            val ok = booleanArrayOf(false)
            val dispatched = dispatchGesture(g, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    ok[0] = true
                    done.countDown()
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    done.countDown()
                }
            }, null)
            if (!dispatched) return false
            done.await(2, TimeUnit.SECONDS)
            ok[0]
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        @Volatile
        private var instance: LensAccessibilityService? = null

        fun get(): LensAccessibilityService? = instance

        private val LENS_PACKAGES = arrayOf(
            "com.google.android.googlequicksearchbox",
            "com.google.ar.lens"
        )

        private val SELECT_TEXT_LABELS = arrayOf(
            "select text", "টেক্সট নির্বাচন", "টেক্সট সিলেক্ট", "লেখা নির্বাচন", "লেখা সিলেক্ট",
            "text auswählen", "seleccionar texto", "sélectionner le texte"
        )

        private val COPY_TEXT_LABELS = arrayOf(
            "copy text", "copy all", "টেক্সট কপি", "লেখা কপি", "সব কপি",
            "texto kopieren", "copiar texto", "copier le texte", "copy", "কপি"
        )

        private val COPY_EXCLUDE = arrayOf("image", "photo", "link", "url", "ছবি", "লিঙ্ক", "path")

        private fun isLensPackage(pkg: CharSequence?): Boolean {
            if (pkg == null) return false
            return LENS_PACKAGES.any { it.contentEquals(pkg) }
        }

        private fun matches(n: AccessibilityNodeInfo, label: String, exclude: Array<String>?): Boolean {
            val t = lower(n.text)
            val d = lower(n.contentDescription)
            val hit = (t.isNotEmpty() && t.contains(label)) || (d.isNotEmpty() && d.contains(label))
            if (!hit) return false
            if (t.length > 40 && d.length > 40) return false
            if (exclude != null) {
                for (x in exclude) {
                    if (t.contains(x) || d.contains(x)) return false
                }
            }
            return true
        }

        private fun lower(cs: CharSequence?): String =
            cs?.toString()?.trim()?.lowercase(Locale.ROOT) ?: ""

        private fun recycleAll(list: List<AccessibilityNodeInfo>) {
            for (n in list) {
                try {
                    @Suppress("DEPRECATION")
                    n.recycle()
                } catch (_: Exception) {}
            }
        }

        private fun sleepQuietly(ms: Long) {
            try {
                Thread.sleep(ms)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
}
