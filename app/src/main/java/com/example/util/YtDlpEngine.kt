package com.example.util

import android.content.Context
import android.os.SystemClock
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** ডাউনলোডের ফরম্যাট (YTDownloader অ্যাপের মতোই) */
enum class DlFormat(val label: String, val isAudio: Boolean, val args: List<Pair<String, String?>>) {
    V1080(
        "🎬 1080p", false,
        listOf(
            "-f" to "bv*[height<=1080][vcodec^=avc1]+ba[ext=m4a]/bv*[height<=1080]+ba/b[height<=1080]/b",
            "--merge-output-format" to "mp4"
        )
    ),
    V720(
        "🎬 720p", false,
        listOf(
            "-f" to "bv*[height<=720][vcodec^=avc1]+ba[ext=m4a]/bv*[height<=720]+ba/b[height<=720]/b",
            "--merge-output-format" to "mp4"
        )
    ),
    V360(
        "🎬 360p", false,
        listOf(
            "-f" to "bv*[height<=360][vcodec^=avc1]+ba[ext=m4a]/bv*[height<=360]+ba/b[height<=360]/b",
            "--merge-output-format" to "mp4"
        )
    ),
    V480(
        "🎬 480p", false,
        listOf(
            "-f" to "bv*[height<=480][vcodec^=avc1]+ba[ext=m4a]/bv*[height<=480]+ba/b[height<=480]/b",
            "--merge-output-format" to "mp4"
        )
    ),
    MP3("🎵 MP3", true, listOf("-x" to null, "--audio-format" to "mp3")),
    M4A("🎧 M4A", true, listOf("-x" to null, "--audio-format" to "m4a"))
}

data class VideoMeta(
    val title: String,
    val thumbnail: String?,
    val author: String?,
    val durationSec: Int
)

/** ডাউনলোডের লাইভ অবস্থা */
data class DlProgress(
    val percent: Float = 0f,          // 0f..1f
    val speedMBps: Float = 0f,        // MB/s
    val downloadedMB: Float = 0f,     // এখন পর্যন্ত নামানো MB
    val etaSeconds: Long = -1L,
    val processing: Boolean = false   // ভিডিও+অডিও মার্জ হচ্ছে
)

class DownloadCancelledException : Exception("Cancelled")

object YtDlpEngine {
    private const val PROCESS_ID = "study_video_dl"
    private val initMutex = Mutex()

    @Volatile
    var isReady: Boolean = false
        private set

    @Volatile
    private var cancelled = false

    /** yt-dlp + ffmpeg ইঞ্জিন চালু করে (প্রথমবার কয়েক সেকেন্ড লাগে)। */
    suspend fun ensureInit(context: Context) = withContext(Dispatchers.IO) {
        initMutex.withLock {
            if (!isReady) {
                val app = context.applicationContext
                YoutubeDL.getInstance().init(app)
                FFmpeg.getInstance().init(app)
                isReady = true
            }
        }
    }

    suspend fun fetchInfo(context: Context, url: String): VideoMeta = withContext(Dispatchers.IO) {
        ensureInit(context)
        val i = YoutubeDL.getInstance().getInfo(url.trim())
        VideoMeta(i.title ?: "Unknown", i.thumbnail, i.uploader, i.duration)
    }

    suspend fun updateYtDlp(context: Context) = withContext(Dispatchers.IO) {
        ensureInit(context)
        YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext, YoutubeDL.UpdateChannel.STABLE)
        Unit
    }

    fun cancel() {
        cancelled = true
        try {
            YoutubeDL.getInstance().destroyProcessById(PROCESS_ID)
        } catch (_: Throwable) {
        }
    }

    /**
     * ভিডিও ডাউনলোড করে [targetDir] এ "[baseName].<ext>" নামে সেভ করে এবং ফাইলটি রিটার্ন করে।
     * [onProgress] এ লাইভ শতাংশ ও MB/s স্পিড আসে (যেকোনো থ্রেড থেকে কল হতে পারে)।
     */
    suspend fun download(
        context: Context,
        url: String,
        format: DlFormat,
        targetDir: File,
        baseName: String,
        onProgress: (DlProgress) -> Unit
    ): File = withContext(Dispatchers.IO) {
        ensureInit(context)
        cancelled = false

        targetDir.mkdirs()
        val tmp = File(targetDir, ".tmp_${System.currentTimeMillis()}").apply { mkdirs() }

        val lock = Any()
        var phase = 0
        var totalPhases = if (format.isAudio) 1 else 2
        var percent = 0f
        var eta = -1L
        var processing = false
        var parsedSpeed = 0f
        var parsedAt = 0L
        var polledSpeed = 0f
        var downloadedBytes = 0L

        fun emit() {
            val snap = synchronized(lock) {
                val fresh = SystemClock.elapsedRealtime() - parsedAt < 3000L && parsedSpeed > 0f
                DlProgress(
                    percent = percent,
                    speedMBps = if (processing) 0f else if (fresh) parsedSpeed else polledSpeed,
                    downloadedMB = downloadedBytes / (1024f * 1024f),
                    etaSeconds = eta,
                    processing = processing
                )
            }
            onProgress(snap)
        }

        try {
            coroutineScope {
                // ফোল্ডারের সাইজ প্রতি সেকেন্ডে মেপে স্পিড (MB/s) বের করা (ব্যাকআপ হিসাব)
                val poller = launch(Dispatchers.IO) {
                    var last = dirSize(tmp)
                    var lastT = SystemClock.elapsedRealtime()
                    var ema = 0f
                    while (isActive) {
                        delay(1000)
                        val now = SystemClock.elapsedRealtime()
                        val size = dirSize(tmp)
                        val secs = ((now - lastT) / 1000f).coerceAtLeast(0.1f)
                        val inst = (size - last).coerceAtLeast(0L) / (1024f * 1024f) / secs
                        ema = if (ema == 0f) inst else ema * 0.6f + inst * 0.4f
                        last = size
                        lastT = now
                        synchronized(lock) {
                            polledSpeed = ema
                            downloadedBytes = size
                        }
                        emit()
                    }
                }

                try {
                    val req = YoutubeDLRequest(url.trim())
                    req.addOption("-o", tmp.absolutePath + "/video.%(ext)s")
                    format.args.forEach { (k, v) -> if (v == null) req.addOption(k) else req.addOption(k, v) }
                    req.addOption("--no-mtime")
                    req.addOption("--no-playlist")
                    req.addOption("-N", "4") // একসাথে ৪টি ফ্র্যাগমেন্ট নামিয়ে স্পিড বাড়ায়

                    YoutubeDL.getInstance().execute(req, PROCESS_ID) { p, etaSec, line ->
                        val l = line ?: ""
                        synchronized(lock) {
                            Regex("""Downloading \d+ format\(s\):\s*(\S+)""").find(l)?.let {
                                totalPhases = if (it.groupValues[1].contains("+")) 2 else 1
                            }
                            if (l.contains("[download] Destination:")) phase++
                            if (l.contains("[Merger]") || l.contains("[ExtractAudio]") ||
                                l.contains("[VideoConvertor]")
                            ) processing = true
                            parseSpeedMBps(l)?.let {
                                parsedSpeed = it
                                parsedAt = SystemClock.elapsedRealtime()
                            }
                            val ph = maxOf(phase, 1)
                            val v = ((ph - 1) + p.coerceIn(0f, 100f) / 100f) / totalPhases
                            if (v > percent) percent = v.coerceAtMost(0.99f)
                            eta = etaSec
                        }
                        emit()
                    }
                } finally {
                    poller.cancel()
                }
            }

            val out = tmp.listFiles { f ->
                f.isFile && f.length() > 0 &&
                    !f.name.endsWith(".part") && !f.name.endsWith(".ytdl") &&
                    !f.name.contains(".temp.") && !Regex("\\.f\\d+\\.").containsMatchIn(f.name)
            }.orEmpty().maxByOrNull { it.length() }
                ?: throw Exception("ডাউনলোড ফাইল তৈরি হয়নি")

            val dest = File(targetDir, "$baseName.${out.extension}")
            if (dest.exists()) dest.delete()
            if (!out.renameTo(dest)) {
                out.copyTo(dest, true)
                out.delete()
            }
            synchronized(lock) { percent = 1f; processing = false; eta = 0 }
            emit()
            dest
        } catch (e: Throwable) {
            if (cancelled) throw DownloadCancelledException()
            throw e
        } finally {
            tmp.deleteRecursively()
        }
    }

    private fun dirSize(dir: File): Long =
        dir.listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L

    /** yt-dlp লাইন থেকে "at 2.50MiB/s" অংশ পড়ে MB/s এ রূপান্তর */
    private fun parseSpeedMBps(line: String): Float? {
        val m = Regex("""at\s+([\d.]+)\s*([KMG])i?B/s""").find(line) ?: return null
        val n = m.groupValues[1].toFloatOrNull() ?: return null
        return when (m.groupValues[2]) {
            "K" -> n / 1024f
            "G" -> n * 1024f
            else -> n
        }
    }
}
