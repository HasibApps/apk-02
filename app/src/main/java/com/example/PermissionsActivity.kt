package com.example

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.PdfGalleryTheme
import com.example.ui.theme.PermissionsCardBg
import com.example.ui.theme.PermissionsNavyBg
import com.example.ui.theme.PermissionsRowBg

class PermissionsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(PermissionsNavyBg.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(PermissionsNavyBg.toArgb())
        )
        super.onCreate(savedInstanceState)

        setContent {
            PdfGalleryTheme(darkTheme = true) {
                PermissionsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTrigger++ }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTrigger++ }

    val prefs = remember { context.getSharedPreferences("perm_prefs", Context.MODE_PRIVATE) }

    val overlayGranted = remember(refreshTrigger) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }
    val accessibilityEnabled = remember(refreshTrigger) { isAccessibilityServiceEnabled(context) }
    val notificationsGranted = remember(refreshTrigger) { isNotificationsGranted(context) }
    val storageNeeded = Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2
    val storageGranted = remember(refreshTrigger) {
        !storageNeeded || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun openAppDetails() {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            )
        } catch (_: Exception) {
            Toast.makeText(context, "সেটিংস খোলা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    fun openOverlaySettings() {
        try {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            )
        } catch (_: Exception) {
            openAppDetails()
        }
    }

    fun openAccessibilitySettings() {
        try {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(
                context,
                "ইনস্টলড সার্ভিস থেকে \"PDF Gallery – Lens Auto Copy\" চালু করুন",
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            openAppDetails()
        }
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !notificationsGranted &&
            (!prefs.getBoolean("asked_notif", false) ||
                (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )))
        ) {
            prefs.edit().putBoolean("asked_notif", true).apply()
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppDetails()
        }
    }

    fun requestStorage() {
        if (!storageNeeded || storageGranted) {
            openAppDetails()
            return
        }
        if ((activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )) || !prefs.getBoolean("asked_storage", false)
        ) {
            prefs.edit().putBoolean("asked_storage", true).apply()
            storageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            openAppDetails()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PermissionsNavyBg)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("permissions_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Settings & Permissions",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "সব ফিচার ঠিকমতো কাজ করার জন্য প্রয়োজনীয় অনুমতি",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = PermissionsCardBg,
            border = BorderStroke(1.dp, Color(0xFF1F2937)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PermissionRowCard(
                    title = "Display Over Other Apps",
                    desc = "ফ্লোটিং বাবল ও নেভিগেশন বার দেখানোর জন্য",
                    statusText = if (overlayGranted) "Active" else "Not granted",
                    statusColor = if (overlayGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                    buttonLabel = if (overlayGranted) "Active" else "Enable",
                    isActiveStyle = overlayGranted,
                    icon = if (overlayGranted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    iconTint = if (overlayGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                    tag = "perm_row_overlay",
                    onClick = { openOverlaySettings() }
                )

                PermissionRowCard(
                    title = "Accessibility Service",
                    desc = "Google Lens-এ অটো ক্লিক (Select text / Copy text) এর জন্য",
                    statusText = if (accessibilityEnabled) "Active" else "Not granted",
                    statusColor = if (accessibilityEnabled) Color(0xFF10B981) else Color(0xFFF59E0B),
                    buttonLabel = if (accessibilityEnabled) "Active" else "Enable",
                    isActiveStyle = accessibilityEnabled,
                    icon = if (accessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    iconTint = if (accessibilityEnabled) Color(0xFF10B981) else Color(0xFFF59E0B),
                    tag = "perm_row_accessibility",
                    onClick = { openAccessibilitySettings() }
                )

                PermissionRowCard(
                    title = "Notifications",
                    desc = "ব্যাকগ্রাউন্ড সার্ভিস ও ডাউনলোড নোটিফিকেশনের জন্য",
                    statusText = if (notificationsGranted) "Active" else "Not granted",
                    statusColor = if (notificationsGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                    buttonLabel = if (notificationsGranted) "Active" else "Enable",
                    isActiveStyle = notificationsGranted,
                    icon = if (notificationsGranted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    iconTint = if (notificationsGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                    tag = "perm_row_notifications",
                    onClick = { requestNotifications() }
                )

                PermissionRowCard(
                    title = "Storage Access",
                    desc = if (storageNeeded) "PDF ফাইল পড়ার জন্য" else "এই ভার্সনে আলাদা পারমিশন লাগে না",
                    statusText = if (storageGranted) "Active" else "Not granted",
                    statusColor = if (storageGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                    buttonLabel = if (storageGranted) "Active" else "Enable",
                    isActiveStyle = storageGranted,
                    icon = if (storageGranted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    iconTint = if (storageGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                    tag = "perm_row_storage",
                    onClick = { requestStorage() }
                )

                PermissionRowCard(
                    title = "All App Permissions",
                    desc = "অ্যাপের সব পারমিশন ও সেটিংস একসাথে দেখুন",
                    statusText = "App info",
                    statusColor = Color(0xFF94A3B8),
                    buttonLabel = "Open",
                    isActiveStyle = true,
                    icon = Icons.Default.Settings,
                    iconTint = Color(0xFF94A3B8),
                    tag = "perm_row_app_info",
                    onClick = { openAppDetails() }
                )
            }
        }
    }
}

@Composable
private fun PermissionRowCard(
    title: String,
    desc: String,
    statusText: String,
    statusColor: Color,
    buttonLabel: String,
    isActiveStyle: Boolean,
    icon: ImageVector,
    iconTint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PermissionsRowBg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = Color(0xFF94A3B8), fontSize = 12.sp)
                Spacer(Modifier.height(3.dp))
                Text(statusText, color = statusColor, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActiveStyle) Color(0xFF28324B) else Color(0xFF2563EB),
                    contentColor = if (isActiveStyle) Color(0xFF9CA3AF) else Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text(buttonLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    return try {
        val enabled = Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        if (enabled != 1) return false
        val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (TextUtils.isEmpty(list)) return false
        val cn = ComponentName(context, LensAccessibilityService::class.java)
        val full = cn.flattenToString()
        val shortName = cn.flattenToShortString()
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(list)
        while (splitter.hasNext()) {
            val s = splitter.next()
            if (s.equals(full, ignoreCase = true) || s.equals(shortName, ignoreCase = true)) {
                return true
            }
        }
        false
    } catch (_: Exception) {
        false
    }
}

private fun isNotificationsGranted(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
