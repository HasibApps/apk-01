package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.Lecture
import com.example.ui.theme.EmeraldBrand
import com.example.util.DlFormat
import com.example.util.DlProgress
import com.example.util.DownloadTracker
import com.example.util.DownloadCancelledException
import com.example.util.VideoDownloader
import com.example.util.VideoMeta
import com.example.util.YtDlpEngine
import kotlinx.coroutines.launch
import java.io.File

private val BgGrad = Brush.verticalGradient(
    listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
)
private val PinkGrad = Brush.horizontalGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)))
private val PurpleGrad = Brush.horizontalGradient(listOf(Color(0xFF7F00FF), Color(0xFFE100FF)))
private val SoftText = Color(0xFFB0B0D0)

@Composable
private fun GradBtn(
    text: String,
    brush: Brush,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun Glass(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

private fun formatEta(sec: Long): String {
    if (sec < 0) return "--:--"
    return "%d:%02d".format(sec / 60, sec % 60)
}

@Composable
fun DownloadDialog(
    lecture: Lecture,
    subjectName: String,
    chapterName: String,
    onDismiss: () -> Unit,
    onPlayOffline: (String) -> Unit,
    onDownloadCompleted: (filePath: String) -> Unit,
    onDeleteVideo: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val isDownloaded = remember(lecture.localVideoPath) {
        VideoDownloader.isLocalVideoValid(lecture.localVideoPath)
    }
    // ইতিমধ্যে ডাউনলোড থাকলে আগে "সংরক্ষিত" কার্ড দেখাবে; "পুনরায় ডাউনলোড" চাপলে ডাউনলোডার খুলবে
    var showDownloader by remember { mutableStateOf(!isDownloaded) }

    var url by remember { mutableStateOf(lecture.link.trim()) }
    var format by remember { mutableStateOf(DlFormat.V720) }
    var engineReady by remember { mutableStateOf(YtDlpEngine.isReady) }
    var status by remember {
        mutableStateOf(if (YtDlpEngine.isReady) "Ready ✅" else "Starting engine…")
    }
    var localBusy by remember { mutableStateOf(false) } // Fetch / Update চলার সময়
    var meta by remember { mutableStateOf<VideoMeta?>(null) }

    // ডাউনলোড অ্যাপ-লেভেলে চলে, তাই ডায়ালগ Hide করলেও থামে না
    val tracker by DownloadTracker.state.collectAsState()
    val downloading = tracker.active && tracker.lectureId == lecture.id
    val otherDownloading = tracker.active && tracker.lectureId != lecture.id
    val progress = tracker.progress
    val busy = localBusy || tracker.active
    val shownStatus =
        if (tracker.lectureId == lecture.id && tracker.status.isNotBlank()) tracker.status else status

    // ডাউনলোডার খুললে yt-dlp ইঞ্জিন চালু করা
    LaunchedEffect(showDownloader) {
        if (showDownloader && !YtDlpEngine.isReady) {
            try {
                YtDlpEngine.ensureInit(context)
                engineReady = true
                status = "Ready ✅"
            } catch (e: Throwable) {
                status = "Init failed: ${e.message?.take(150)}"
            }
        } else if (YtDlpEngine.isReady) {
            engineReady = true
        }
    }

    fun fetchInfo() {
        scope.launch {
            localBusy = true
            status = "Fetching info…"
            try {
                meta = YtDlpEngine.fetchInfo(context, url)
                status = "Info loaded"
            } catch (e: Throwable) {
                status = "Error: ${e.message?.take(150)}"
            }
            localBusy = false
        }
    }

    fun startDownload() {
        val link = url.trim()
        if (link.isBlank()) {
            status = "আগে ভিডিওর লিঙ্ক দিন"
            return
        }
        val oldPath = lecture.localVideoPath
        DownloadTracker.start(
            context = context,
            lectureId = lecture.id,
            url = link,
            format = format
        ) { path ->
            if (oldPath.isNotBlank() && oldPath != path) {
                VideoDownloader.deleteVideo(oldPath)
            }
            // অফলাইনে সেভ → অ্যাপের ভেতরেই লোড/প্লে (ডায়ালগ Hide থাকলেও কাজ করবে)
            onDownloadCompleted(path)
            onPlayOffline(path)
            onDismiss()
        }
    }

    Dialog(
        // ডাউনলোড চললে Back/বাইরে ট্যাপ = Hide (ডাউনলোড থামে না)
        onDismissRequest = { if (!localBusy) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !localBusy,
            dismissOnClickOutside = !localBusy,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(BgGrad)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ---------- Header ----------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFFFF416C)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("YT Downloader", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Powered by yt-dlp", fontSize = 12.sp, color = SoftText)
                        Text(
                            "$subjectName • $chapterName",
                            fontSize = 11.sp, color = SoftText,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (downloading) {
                        // Hide: পপআপ লুকায়, ডাউনলোড চলতে থাকে
                        TextButton(onClick = onDismiss) {
                            Text("Hide", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!busy) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White)
                        }
                    }
                }

                if (!showDownloader && isDownloaded) {
                    // ---------- ইতিমধ্যে সংরক্ষিত ----------
                    val file = File(lecture.localVideoPath)
                    val sizeMb = file.length() / (1024f * 1024f)
                    Glass {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = EmeraldBrand, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "ভিডিও অফলাইনে সংরক্ষিত আছে",
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                        }
                        Text("সাইজ: ${"%.1f".format(sizeMb)} MB • ${file.name}", color = SoftText, fontSize = 12.sp)
                        GradBtn("▶  অফলাইনে চালান", PinkGrad, Modifier.fillMaxWidth()) {
                            onPlayOffline(lecture.localVideoPath)
                            onDismiss()
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GradBtn("🗑 মুছুন", PurpleGrad, Modifier.weight(1f)) {
                                VideoDownloader.deleteVideo(lecture.localVideoPath)
                                onDeleteVideo()
                            }
                            GradBtn("↻ আবার ডাউনলোড", PurpleGrad, Modifier.weight(1f)) {
                                showDownloader = true
                            }
                        }
                    }
                } else {
                    // ---------- লিঙ্ক + Paste + Fetch ----------
                    Glass {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            singleLine = true,
                            enabled = !busy,
                            placeholder = { Text("Paste video link…") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE100FF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.White.copy(alpha = 0.6f),
                                focusedPlaceholderColor = SoftText,
                                unfocusedPlaceholderColor = SoftText,
                                cursorColor = Color.White
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GradBtn("📋 Paste", PurpleGrad, Modifier.weight(1f), !busy) {
                                clipboard.getText()?.text?.let { url = it.trim() }
                            }
                            GradBtn(
                                "🔍 Fetch", PinkGrad, Modifier.weight(1f),
                                engineReady && !busy && url.isNotBlank()
                            ) { fetchInfo() }
                        }
                    }

                    // ---------- ভিডিও তথ্য (Fetch করলে) ----------
                    meta?.let { m ->
                        Glass {
                            AsyncImage(
                                model = m.thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )
                            Text(
                                m.title, color = Color.White, fontWeight = FontWeight.Bold,
                                fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${m.author ?: ""} • ${m.durationSec / 60}:${"%02d".format(m.durationSec % 60)}",
                                color = SoftText, fontSize = 12.sp
                            )
                        }
                    }

                    // ---------- Format + Download ----------
                    Glass {
                        Text("Format", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DlFormat.values().forEach { f ->
                                FilterChip(
                                    selected = format == f,
                                    onClick = { if (!busy) format = f },
                                    label = { Text(f.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        labelColor = Color.White,
                                        selectedContainerColor = Color(0xFFE100FF).copy(alpha = 0.35f),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        GradBtn(
                            "⬇  Download", PinkGrad, Modifier.fillMaxWidth(),
                            engineReady && !busy && !otherDownloading && url.isNotBlank()
                        ) { startDownload() }
                        if (otherDownloading) {
                            Text("অন্য একটি ভিডিও ডাউনলোড চলছে…", color = SoftText, fontSize = 12.sp)
                        }

                        // ---------- লাইভ প্রগ্রেস + MB/s স্পিড ----------
                        if (downloading) {
                            LinearProgressIndicator(
                                progress = { progress.percent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = Color(0xFFFF416C),
                                trackColor = Color.White.copy(alpha = 0.15f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${(progress.percent * 100).toInt()}%",
                                    color = Color.White, fontWeight = FontWeight.Bold
                                )
                                if (progress.processing) {
                                    Text("⚙ ভিডিও প্রসেস হচ্ছে…", color = SoftText, fontSize = 13.sp)
                                } else {
                                    Text(
                                        "⚡ ${"%.2f".format(progress.speedMBps)} MB/s",
                                        color = Color(0xFF7CFFB2), fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${"%.1f".format(progress.downloadedMB)} MB ডাউনলোড হয়েছে",
                                    color = SoftText, fontSize = 12.sp
                                )
                                if (!progress.processing) {
                                    Text("বাকি: ${formatEta(progress.etaSeconds)}", color = SoftText, fontSize = 12.sp)
                                }
                            }
                            GradBtn("✖ Cancel", PurpleGrad, Modifier.fillMaxWidth()) {
                                DownloadTracker.cancel()
                            }
                        }
                    }

                    // ---------- স্ট্যাটাস + আপডেট ----------
                    Glass {
                        Text(shownStatus, color = Color.White, fontSize = 16.sp)
                        GradBtn(
                            "🔄 Update yt-dlp (latest)", PurpleGrad, Modifier.fillMaxWidth(),
                            engineReady && !busy
                        ) {
                            scope.launch {
                                localBusy = true
                                status = "Updating yt-dlp…"
                                status = try {
                                    YtDlpEngine.updateYtDlp(context)
                                    "yt-dlp updated ✅"
                                } catch (e: Throwable) {
                                    "Update failed: ${e.message?.take(100)}"
                                }
                                localBusy = false
                            }
                        }
                    }
                }
            }
        }
    }
}
