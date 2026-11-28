package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * ভিডিও ডাউনলোডের Foreground Service।
 * অ্যাপ থেকে বের হলে বা অন্য পেজে গেলেও ডাউনলোড বন্ধ হয় না, এবং নোটিফিকেশনে
 * "15% / 480 MB" আকারে প্রগ্রেস দেখায় (একাধিক ডাউনলোডে প্রতিটির আলাদা নোটিফিকেশন)।
 */
class VideoDownloadService : Service() {

    companion object {
        private const val CHANNEL_ID = "video_downloads"
        private const val FOREGROUND_ID = 7001
        private const val ACTION_CANCEL = "com.example.action.CANCEL_DOWNLOAD"
        private const val ACTION_CANCEL_ALL = "com.example.action.CANCEL_ALL_DOWNLOADS"
        private const val EXTRA_KEY = "key"

        /** ইতিমধ্যে জানানো ফলাফল ("key#run") — সার্ভিস আবার চালু হলেও একই নোটিফিকেশন ২বার আসবে না */
        private val announced = HashSet<String>()

        fun ensureRunning(context: Context) {
            val intent = Intent(context.applicationContext, VideoDownloadService::class.java)
            try {
                ContextCompat.startForegroundService(context.applicationContext, intent)
            } catch (e: Throwable) {
                // ফোরগ্রাউন্ড সার্ভিস চালু না হলেও ডাউনলোড অ্যাপ প্রসেসে চলবে
                e.printStackTrace()
            }
        }

        private fun notifId(key: String) = 8000 + abs(key.hashCode() % 90000)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collectJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val perItemShown = HashSet<String>()
    private var lastSignature = ""
    private var lastRenderAt = 0L

    private val nm by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> intent.getStringExtra(EXTRA_KEY)?.let { DownloadTracker.cancel(it) }
            ACTION_CANCEL_ALL -> DownloadTracker.cancelAll()
        }

        val st = DownloadTracker.state.value
        if (intent?.action != null && st.activeCount == 0 && collectJob == null) {
            // কোনো চলমান ডাউনলোড নেই (যেমন পুরোনো নোটিফিকেশনের Cancel চাপা হয়েছে)
            stopSelf()
            return START_NOT_STICKY
        }

        val first = buildForegroundNotification(st)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_ID, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(FOREGROUND_ID, first)
        }
        acquireWakeLock()

        if (collectJob == null) {
            collectJob = scope.launch {
                DownloadTracker.state.collect { render(it) }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        collectJob?.cancel()
        collectJob = null
        scope.cancel()
        releaseWakeLock()
        perItemShown.forEach { nm.cancel(notifId(it)) }
        perItemShown.clear()
        super.onDestroy()
    }

    // ------------------------------------------------------------------ rendering

    private fun render(st: DownloadTracker.State) {
        val active = st.activeItems

        // শেষ হওয়া আইটেমের ফলাফল নোটিফিকেশন (একবারই)
        st.items.values.filter { !it.active }.forEach { item ->
            val id = "${item.key}#${item.run}"
            if (announced.add(id)) {
                nm.cancel(notifId(item.key))
                perItemShown.remove(item.key)
                if (!item.cancelled) nm.notify(notifId(item.key), buildResultNotification(item))
            }
        }

        if (active.isEmpty()) {
            collectJob?.cancel()
            collectJob = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return
        }

        // নোটিফিকেশন আপডেট সীমিত করি (সেকেন্ডে ~১ বার), তবে সেট বদলালে সাথে সাথে
        val signature = active.joinToString(",") { it.key + (if (it.progress.processing) "p" else "") }
        val now = System.currentTimeMillis()
        if (signature == lastSignature && now - lastRenderAt < 1000L) return
        lastSignature = signature
        lastRenderAt = now

        nm.notify(FOREGROUND_ID, buildForegroundNotification(st))

        if (active.size == 1) {
            // একটিমাত্র ডাউনলোড: ফোরগ্রাউন্ড নোটিফিকেশনেই সব দেখায়
            perItemShown.forEach { nm.cancel(notifId(it)) }
            perItemShown.clear()
        } else {
            val activeKeys = active.map { it.key }.toSet()
            perItemShown.filter { it !in activeKeys }.forEach { nm.cancel(notifId(it)); perItemShown.remove(it) }
            active.forEach { item ->
                perItemShown.add(item.key)
                nm.notify(notifId(item.key), buildItemNotification(item, ongoing = true))
            }
        }
    }

    private fun buildForegroundNotification(st: DownloadTracker.State): Notification {
        val active = st.activeItems
        return when {
            active.size == 1 -> buildItemNotification(active[0], ongoing = true)
            active.isEmpty() -> baseBuilder().setContentTitle("ডাউনলোড প্রস্তুত হচ্ছে…").setOngoing(true).build()
            else -> {
                val avg = (active.map { it.progress.percent }.average() * 100).toInt().coerceIn(0, 100)
                baseBuilder()
                    .setContentTitle("${active.size} টি ভিডিও ডাউনলোড হচ্ছে")
                    .setContentText("গড় অগ্রগতি $avg%")
                    .setProgress(100, avg, false)
                    .setOngoing(true)
                    .addAction(
                        0, "সব বাতিল",
                        servicePendingIntent(ACTION_CANCEL_ALL, null, 1)
                    )
                    .build()
            }
        }
    }

    private fun buildItemNotification(item: DownloadTracker.Item, ongoing: Boolean): Notification {
        val p = item.progress
        val pct = (p.percent * 100).toInt().coerceIn(0, 100)
        val text = when {
            p.processing -> "প্রসেস হচ্ছে…"
            p.totalMB > 0f -> "$pct% / ${formatMB(p.totalMB)}  •  ⚡ ${"%.1f".format(p.speedMBps)} MB/s"
            else -> "$pct%  •  ${formatMB(p.downloadedMB)} ডাউনলোড হয়েছে"
        }
        return baseBuilder()
            .setContentTitle(item.title.ifBlank { "ভিডিও ডাউনলোড" })
            .setContentText(text)
            .setSubText(item.subjectName)
            .setProgress(100, pct, p.processing || (pct == 0 && p.downloadedMB <= 0f))
            .setOngoing(ongoing)
            .addAction(0, "বাতিল", servicePendingIntent(ACTION_CANCEL, item.key, notifId(item.key)))
            .build()
    }

    private fun buildResultNotification(item: DownloadTracker.Item): Notification {
        val ok = item.success
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (ok) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setContentTitle(if (ok) "ডাউনলোড সম্পন্ন ✅" else "ডাউনলোড ব্যর্থ ❌")
            .setContentText(if (ok) item.title else "${item.title}: ${item.status.removePrefix("Failed: ")}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                if (ok) item.title else "${item.title}\n${item.status.removePrefix("Failed: ")}"
            ))
            .setContentIntent(openAppPendingIntent())
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun baseBuilder(): NotificationCompat.Builder =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(openAppPendingIntent())
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

    private fun openAppPendingIntent(): PendingIntent {
        val i = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun servicePendingIntent(action: String, key: String?, requestCode: Int): PendingIntent {
        val i = Intent(this, VideoDownloadService::class.java).setAction(action)
        if (key != null) i.putExtra(EXTRA_KEY, key)
        return PendingIntent.getService(this, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun formatMB(mb: Float): String =
        if (mb >= 1024f) "%.2f GB".format(mb / 1024f) else "%.0f MB".format(mb)

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "ভিডিও ডাউনলোড", NotificationManager.IMPORTANCE_LOW).apply {
                description = "ব্যাকগ্রাউন্ডে ভিডিও ডাউনলোডের অগ্রগতি"
                setShowBadge(false)
            }
            nm.createNotificationChannel(ch)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StudyApp:VideoDownload").apply {
                acquire(6 * 60 * 60 * 1000L)
            }
        } catch (_: Throwable) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Throwable) {
        }
        wakeLock = null
    }
}
