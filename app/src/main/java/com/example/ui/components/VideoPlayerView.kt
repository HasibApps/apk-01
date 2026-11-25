package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.model.Lecture
import com.example.model.StudyUtils
import com.example.ui.PlayingLectureState
import com.example.ui.theme.EmeraldBrand
import com.example.ui.theme.RoseFavorite
import com.example.util.PdfDownloader
import com.example.util.VideoDownloader
import kotlinx.coroutines.launch

enum class ActivePlayerType {
    YOUTUBE,
    OFFLINE_EXOPLAYER
}

class YouTubeBridge(private val onTimeUpdate: (Float) -> Unit) {
    @JavascriptInterface
    fun sendCurrentTime(seconds: Float) {
        onTimeUpdate(seconds)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerView(
    state: PlayingLectureState,
    isAdminUnlocked: Boolean,
    onVerifyAdminPassword: (String) -> Boolean,
    onClose: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onUpdateLecture: (title: String, videoLink: String, sheetLink: String) -> Unit,
    onPlayPreviousLecture: () -> Unit,
    onPlayNextLecture: () -> Unit,
    onUpdateDownloadedVideo: (filePath: String) -> Unit,
    onSaveDownloadedVideoFor: ((subjectId: String, lectureId: Int, filePath: String) -> Unit)? = null,
    onDeleteDownloadedVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lecture = state.lecture
    val videoId = remember(lecture.link) { StudyUtils.extractYouTubeVideoId(lecture.link) }

    // Offline video availability
    val isOfflineAvailable = remember(lecture.localVideoPath) {
        VideoDownloader.isLocalVideoValid(lecture.localVideoPath)
    }

    // Default to offline player if downloaded, else YouTube
    var playerType by remember(lecture.id, isOfflineAvailable) {
        mutableStateOf(if (isOfflineAvailable) ActivePlayerType.OFFLINE_EXOPLAYER else ActivePlayerType.YOUTUBE)
    }

    // Dialog states
    var showDownloadDialog by remember { mutableStateOf(false) }
    val dlState by com.example.util.DownloadTracker.state.collectAsState()
    val dlItem = com.example.util.DownloadTracker.itemFor(dlState, state.subjectId, state.chapterName, lecture.id)
    var showPasswordDialogForEdit by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showSheetDialog by remember { mutableStateOf(false) }

    // In-app PDF State (Screenshot_20261025-120102.jpg)
    var isPdfSplitViewOpen by remember { mutableStateOf(false) }

    // Persistent playback timestamp tracking so fullscreen transition NEVER resets timeline
    var currentPlaybackSeconds by rememberSaveable(lecture.id) { mutableFloatStateOf(0f) }

    // Fullscreen state
    var isAppFullscreen by rememberSaveable { mutableStateOf(false) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Playback speed (settings menu) + double-tap 2x boost. Owned here so they survive
    // fullscreen toggles and switching between the offline / YouTube players.
    var playbackSpeed by rememberSaveable { mutableFloatStateOf(1.0f) }
    var boost2x by rememberSaveable { mutableStateOf(false) }
    val toggleFullscreen: () -> Unit = {
        isAppFullscreen = !isAppFullscreen
        webViewRef?.evaluateJavascript("if (window.resumePlayback) window.resumePlayback();", null)
    }
    // Note icon: lecture sheet PDF. Portrait = below the video, fullscreen = side-by-side with the video.
    val toggleNotes: () -> Unit = {
        if (lecture.sheetLink.isBlank()) showSheetDialog = true
        else isPdfSplitViewOpen = !isPdfSplitViewOpen
    }

    val activity = context as? Activity

    // Fullscreen mode handler: manages orientation and system bars
    LaunchedEffect(isAppFullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        if (isAppFullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
        }

        // Seamless playback: actively tell YouTube player to continue playing across fullscreen transitions without pause
        kotlinx.coroutines.delay(100)
        webViewRef?.evaluateJavascript("if (window.resumePlayback) window.resumePlayback(); else if (player && player.playVideo) player.playVideo();", null)
        kotlinx.coroutines.delay(350)
        webViewRef?.evaluateJavascript("if (window.resumePlayback) window.resumePlayback(); else if (player && player.playVideo) player.playVideo();", null)
        kotlinx.coroutines.delay(700)
        webViewRef?.evaluateJavascript("if (window.resumePlayback) window.resumePlayback(); else if (player && player.playVideo) player.playVideo();", null)
    }

    LaunchedEffect(customFullscreenView) {
        if (customFullscreenView != null) {
            kotlinx.coroutines.delay(120)
            webViewRef?.evaluateJavascript("if (window.resumePlayback) window.resumePlayback(); else if (player && player.playVideo) player.playVideo();", null)
            kotlinx.coroutines.delay(350)
            webViewRef?.evaluateJavascript("if (window.resumePlayback) window.resumePlayback(); else if (player && player.playVideo) player.playVideo();", null)
        }
    }

    // Ensure orientation resets on dispose
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Back button leaves fullscreen first
    BackHandler(enabled = isAppFullscreen || customFullscreenView != null) {
        if (customFullscreenView != null) {
            customViewCallback?.onCustomViewHidden()
            customFullscreenView = null
            customViewCallback = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else if (isAppFullscreen) {
            isAppFullscreen = false
        }
    }

    // Custom view overlay for web iframe fullscreen
    if (customFullscreenView != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .zIndex(999f)
        ) {
            AndroidView(
                factory = {
                    (customFullscreenView!!.parent as? ViewGroup)?.removeView(customFullscreenView)
                    customFullscreenView!!
                },
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = {
                    customViewCallback?.onCustomViewHidden()
                    customFullscreenView = null
                    customViewCallback = null
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Exit Fullscreen",
                    tint = Color.White
                )
            }
        }
    }

    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(
                    if (!isAppFullscreen) Modifier.verticalScroll(scrollState).padding(14.dp)
                    else Modifier
                ),
            horizontalAlignment = Alignment.Start
        ) {
            // Header content (hidden when in full screen)
            if (!isAppFullscreen) {
                // Red / Accent Subheader
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.subjectName.uppercase()} • ${state.chapterName.uppercase()}",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Player Mode Toggle if offline video is available
                    if (isOfflineAvailable) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, EmeraldBrand.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (playerType == ActivePlayerType.OFFLINE_EXOPLAYER) EmeraldBrand else Color.Transparent,
                                    modifier = Modifier
                                        .clickable { playerType = ActivePlayerType.OFFLINE_EXOPLAYER }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "ExoPlayer",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (playerType == ActivePlayerType.OFFLINE_EXOPLAYER) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (playerType == ActivePlayerType.YOUTUBE) Color(0xFFEF4444) else Color.Transparent,
                                    modifier = Modifier
                                        .clickable { playerType = ActivePlayerType.YOUTUBE }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "YouTube",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (playerType == ActivePlayerType.YOUTUBE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Main Title "CLASS"
                Text(
                    text = "CLASS",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // =========================================================================
            // VIDEO VIEWPORT:
            //  - normal  : 16:9 card
            //  - fullscreen: video fills the screen; if the Note (PDF) icon is on, the lecture sheet opens
            //    in a narrow panel on the right while the video keeps playing (same composables, no reload)
            // =========================================================================
            Row(
                modifier = if (isAppFullscreen) Modifier.fillMaxSize().background(Color.Black)
                else Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = (if (isAppFullscreen) {
                        Modifier
                            .weight(if (isPdfSplitViewOpen) 0.62f else 1f)
                            .fillMaxHeight()
                    } else {
                        Modifier
                            .weight(1f)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    }).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // Case 1: offline downloaded video (ExoPlayer + custom controls)
                    if (playerType == ActivePlayerType.OFFLINE_EXOPLAYER && isOfflineAvailable) {
                        ExoPlayerView(
                            filePath = lecture.localVideoPath ?: "",
                            modifier = Modifier.fillMaxSize(),
                            isFullscreen = isAppFullscreen,
                            playbackSpeed = playbackSpeed,
                            boost2x = boost2x,
                            onSpeedSelected = { playbackSpeed = it; boost2x = false },
                            onToggleBoost = { boost2x = !boost2x },
                            isNotesOpen = isPdfSplitViewOpen,
                            onToggleNotes = toggleNotes,
                            onToggleFullscreen = toggleFullscreen,
                            onFallbackToOnline = { playerType = ActivePlayerType.YOUTUBE },
                            onReDownload = { showDownloadDialog = true }
                        )
                    }
                    // Case 2: YouTube (iframe, controls=0 + the same custom controls)
                    else if (!videoId.isNullOrBlank()) {
                        YouTubeCustomPlayer(
                            videoId = videoId,
                            startSeconds = currentPlaybackSeconds.toInt(),
                            modifier = Modifier.fillMaxSize(),
                            isFullscreen = isAppFullscreen,
                            playbackSpeed = playbackSpeed,
                            boost2x = boost2x,
                            onSpeedSelected = { playbackSpeed = it; boost2x = false },
                            onToggleBoost = { boost2x = !boost2x },
                            isNotesOpen = isPdfSplitViewOpen,
                            onToggleNotes = toggleNotes,
                            onToggleFullscreen = toggleFullscreen,
                            onTimeUpdate = { currentPlaybackSeconds = it },
                            onWebViewReady = { webViewRef = it },
                            onCustomView = { view, cb ->
                                customFullscreenView = view
                                customViewCallback = cb
                            }
                        )
                } else if (lecture.link.isNotBlank()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                }
                                loadUrl(lecture.link)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "কোন ভিডিও লিঙ্ক যুক্ত করা হয়নি",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (!isAdminUnlocked) {
                                    showPasswordDialogForEdit = true
                                } else {
                                    showEditDialog = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldBrand)
                        ) {
                            Text("ভিডিও লিঙ্ক যোগ করুন", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
                }

                // Fullscreen + Note icon  =>  video (left) | lecture sheet PDF (right)
                if (isAppFullscreen && isPdfSplitViewOpen) {
                    InAppPdfSlidesViewer(
                        subjectId = state.subjectId,
                        lectureId = lecture.id,
                        sheetUrl = lecture.sheetLink,
                        onOpenInBrowser = { openUrlInBrowser(context, lecture.sheetLink) },
                        onClosePdf = { isPdfSplitViewOpen = false },
                        modifier = Modifier
                            .weight(0.38f)
                            .fillMaxHeight()
                            .padding(6.dp)
                    )
                }
            }

            // Normal Screen Content (Actions, In-App PDF, Class Details)
            if (!isAppFullscreen) {
                Spacer(modifier = Modifier.height(14.dp))

                // PRIMARY ACTION BUTTONS ROW:
                // [ 📄 Open Pdf / Hide Pdf ]
                // [ 📥 Download ]
                // [ ← Back to classes ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button 1: Open Pdf
                    OutlinedButton(
                        onClick = {
                            if (lecture.sheetLink.isBlank()) {
                                showSheetDialog = true
                            } else {
                                isPdfSplitViewOpen = !isPdfSplitViewOpen
                            }
                        },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isPdfSplitViewOpen) Color(0xFF0284C7).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            contentColor = if (isPdfSplitViewOpen) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isPdfSplitViewOpen) Color(0xFF0284C7) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("open_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPdfSplitViewOpen) "Hide Pdf" else "Open Pdf",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Button 2: Download Button
                    OutlinedButton(
                        onClick = { showDownloadDialog = true },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isOfflineAvailable) EmeraldBrand.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            contentColor = if (isOfflineAvailable) EmeraldBrand else MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isOfflineAvailable) EmeraldBrand else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("download_button")
                    ) {
                        Icon(
                            imageVector = if (isOfflineAvailable) Icons.Default.CheckCircle else Icons.Default.Download,
                            contentDescription = null,
                            tint = if (isOfflineAvailable) EmeraldBrand else Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (dlItem?.active == true)
                                "${((dlItem?.progress?.percent ?: 0f) * 100).toInt()}%"
                            else if (isOfflineAvailable) "Downloaded" else "Download",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Button 3: Back to classes Button
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(44.dp)
                            .testTag("back_to_classes_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Back to classes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Hide করা অবস্থায় চলমান ডাউনলোডের % ও স্পিড
                if (!showDownloadDialog && dlItem?.active == true) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDownloadDialog = true }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if ((dlItem?.progress?.processing == true)) "⚙ প্রসেস হচ্ছে…"
                                    else "⬇ ডাউনলোড হচ্ছে ${((dlItem?.progress?.percent ?: 0f) * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "⚡ ${"%.2f".format((dlItem?.progress?.speedMBps ?: 0f))} MB/s",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { (dlItem?.progress?.percent ?: 0f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF10B981).copy(alpha = 0.2f)
                            )
                        }
                    }
                }

                // =========================================================================
                // IN-APP PDF / SHEET SLIDES VIEWER (Matching Screenshot_20261025-120102.jpg)
                // With download icon and offline viewing support!
                // =========================================================================
                if (isPdfSplitViewOpen) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Navigation bar between classes (Previous Class / Next Class)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onPlayPreviousLecture,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("পূর্ববর্তী ক্লাস", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }

                            Text(
                                text = "লেকচার নোট ও স্লাইড",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )

                            OutlinedButton(
                                onClick = onPlayNextLecture,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("পরবর্তী ক্লাস", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Embedded In-App PDF Slides Viewer Container with Download & Offline support
                    InAppPdfSlidesViewer(
                        subjectId = state.subjectId,
                        lectureId = lecture.id,
                        sheetUrl = lecture.sheetLink,
                        onOpenInBrowser = { openUrlInBrowser(context, lecture.sheetLink) },
                        onClosePdf = { isPdfSplitViewOpen = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Open Sheet in Browser Option
                if (lecture.sheetLink.isNotBlank() && !isPdfSplitViewOpen) {
                    OutlinedButton(
                        onClick = { openUrlInBrowser(context, lecture.sheetLink) },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("open_sheet_browser_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ব্রাউজারে শিট দেখুন (আগের মতো)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Lecture Details & Quick Control Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lecture.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Lecture ${StudyUtils.toBengaliDigits(lecture.id)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isOfflineAvailable) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = EmeraldBrand.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "✓ অফলাইন ভিডিও সংরক্ষিত",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldBrand,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onToggleFavorite,
                                    modifier = Modifier.testTag("player_favorite_button")
                                ) {
                                    Icon(
                                        imageVector = if (lecture.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (lecture.isFavorite) RoseFavorite else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = onToggleComplete,
                                    modifier = Modifier.testTag("player_complete_button")
                                ) {
                                    Icon(
                                        imageVector = if (lecture.completed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                        contentDescription = "Complete",
                                        tint = if (lecture.completed) EmeraldBrand else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        showPasswordDialogForEdit = true
                                    },
                                    modifier = Modifier.testTag("player_edit_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Lecture",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    // Download Dialog
    if (showDownloadDialog) {
        DownloadDialog(
            lecture = lecture,
            subjectId = state.subjectId,
            subjectName = state.subjectName,
            chapterName = state.chapterName,
            onDismiss = { showDownloadDialog = false },
            onPlayOffline = {
                playerType = ActivePlayerType.OFFLINE_EXOPLAYER
            },
            onDeleteVideo = {
                onDeleteDownloadedVideo()
                playerType = ActivePlayerType.YOUTUBE
            }
        )
    }

    // Admin Password Dialog for Edit Details
    if (showPasswordDialogForEdit) {
        AdminPasswordDialog(
            actionName = "লেকচার ও শিট তথ্য এডিট",
            onDismiss = { showPasswordDialogForEdit = false },
            onUnlocked = {
                onVerifyAdminPassword("hasib99")
                showPasswordDialogForEdit = false
                showEditDialog = true
            }
        )
    }

    // Edit Lecture Dialog
    if (showEditDialog) {
        var editTitle by remember { mutableStateOf(lecture.title) }
        var editVideoLink by remember { mutableStateOf(lecture.link) }
        var editSheetLink by remember { mutableStateOf(lecture.sheetLink) }

        Dialog(onDismissRequest = { showEditDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "লেকচার তথ্য এডিট করুন",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("লেকচার টাইটেল") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editVideoLink,
                        onValueChange = { editVideoLink = it },
                        label = { Text("ইউটিউব ভিডিও লিঙ্ক") },
                        placeholder = { Text("https://youtu.be/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editSheetLink,
                        onValueChange = { editSheetLink = it },
                        label = { Text("Lecture Sheet লিঙ্ক (PDF/Drive)") },
                        placeholder = { Text("https://drive.google.com/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showEditDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("বাতিল")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onUpdateLecture(editTitle, editVideoLink, editSheetLink)
                                showEditDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldBrand)
                        ) {
                            Text("সংরক্ষণ", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Set Sheet Link Dialog
    if (showSheetDialog) {
        var inputSheetLink by remember { mutableStateOf(lecture.sheetLink) }

        Dialog(onDismissRequest = { showSheetDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Lecture Sheet লিঙ্ক যুক্ত করুন",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "এই লেকচারের ক্লাস নোট বা পিডিএফ লিঙ্ক দিন:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputSheetLink,
                        onValueChange = { inputSheetLink = it },
                        label = { Text("Lecture Sheet লিঙ্ক (PDF/Drive)") },
                        placeholder = { Text("https://drive.google.com/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showSheetDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("বাতিল")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onUpdateLecture(lecture.title, lecture.link, inputSheetLink)
                                showSheetDialog = false
                                if (inputSheetLink.isNotBlank()) {
                                    isPdfSplitViewOpen = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("সেভ করুন", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * In-App PDF & Slides Viewer (Screenshot_20261025-120102.jpg)
 * With zoom - +, download icon for offline viewing without net, and smooth drag scrolling!
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun InAppPdfSlidesViewer(
    subjectId: String,
    lectureId: Int,
    sheetUrl: String,
    onOpenInBrowser: () -> Unit,
    onClosePdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val previewUrl = remember(sheetUrl) { StudyUtils.formatDrivePreviewUrl(sheetUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var zoomScale by remember { mutableFloatStateOf(PDF_MIN_ZOOM) }
    // অনলাইন (WebView) ভিউয়ারের pinch-zoom এর pan অফসেট
    var webPanX by remember { mutableFloatStateOf(0f) }
    var webPanY by remember { mutableFloatStateOf(0f) }
    var webBoxW by remember { mutableFloatStateOf(0f) }
    var webBoxH by remember { mutableFloatStateOf(0f) }

    // Offline PDF State
    var isDownloadingPdf by remember { mutableStateOf(false) }
    var downloadProgressText by remember { mutableStateOf("") }
    var showOfflineInfoDialog by remember { mutableStateOf(false) }

    var isPdfOffline by remember(subjectId, lectureId) {
        mutableStateOf(PdfDownloader.isPdfDownloaded(context, subjectId, lectureId))
    }
    val offlinePdfFile = remember(subjectId, lectureId, isPdfOffline) {
        PdfDownloader.getPdfFileForLecture(context, subjectId, lectureId)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .background(Color(0xFF0B1120))
            .padding(10.dp)
    ) {
        // PDF Controls Top Bar (Screenshot_20261025-120102.jpg: [PDF SLIDES] [-] [+] [Download] [Open in Browser] [X])
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPdfOffline) EmeraldBrand.copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.2f),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = if (isPdfOffline) "PDF OFFLINE ✓" else "PDF SLIDES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPdfOffline) EmeraldBrand else Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Zoom Out (-)
                IconButton(
                    onClick = {
                        // ১০০% এর নিচে zoom-out করা যাবে না
                        zoomScale = (zoomScale - 0.25f).coerceAtLeast(PDF_MIN_ZOOM)
                        if (zoomScale <= PDF_MIN_ZOOM) { webPanX = 0f; webPanY = 0f }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Zoom In (+)
                IconButton(
                    onClick = {
                        zoomScale = (zoomScale + 0.25f).coerceAtMost(PDF_MAX_ZOOM)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // PDF Download Icon (User Request 1: Next to - + zoom buttons, download for offline viewing without net)
                if (isDownloadingPdf) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else if (isPdfOffline) {
                    IconButton(
                        onClick = {
                            showOfflineInfoDialog = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Offline PDF Downloaded",
                            tint = EmeraldBrand,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (sheetUrl.isNotBlank()) {
                                isDownloadingPdf = true
                                coroutineScope.launch {
                                    val downloaded = PdfDownloader.downloadPdf(
                                        context = context,
                                        sheetUrl = sheetUrl,
                                        subjectId = subjectId,
                                        lectureId = lectureId,
                                        onProgress = { _, status ->
                                            downloadProgressText = status
                                        }
                                    )
                                    isDownloadingPdf = false
                                    if (downloaded != null) {
                                        isPdfOffline = true
                                        Toast.makeText(context, "পিডিএফ সফলভাবে ডাউনলোড হয়েছে! অফলাইনে রেডি", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "কোন শিট বা পিডিএফ লিঙ্ক সংযুক্ত নেই", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download PDF for offline",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Open in browser button
                IconButton(
                    onClick = onOpenInBrowser,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Browser",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Close PDF
                IconButton(
                    onClick = onClosePdf,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close PDF",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Live Download Progress Bar if downloading
        if (isDownloadingPdf && downloadProgressText.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = downloadProgressText,
                    fontSize = 10.sp,
                    color = Color(0xFF38BDF8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // PDF Viewport: Uses Native Offline Renderer when downloaded, or Web Embed preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isPdfOffline && offlinePdfFile.exists()) {
                // NATIVE OFFLINE PDF RENDERER (Zero internet needed)
                NativePdfRendererView(
                    pdfFile = offlinePdfFile,
                    zoomScale = zoomScale,
                    onZoomChange = { zoomScale = it },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Online WebView embed with active zoom scaling
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .onSizeChanged { webBoxW = it.width.toFloat(); webBoxH = it.height.toFloat() }
                        // ২ আঙুলে pinch zoom (অনলাইন) — সর্বনিম্ন ১০০%
                        .pointerInput(Unit) {
                            detectPdfPinch(
                                onGesture = { centroid, pan, zoomChange ->
                                    val old = zoomScale
                                    val new = (old * zoomChange).coerceIn(PDF_MIN_ZOOM, PDF_MAX_ZOOM)
                                    val f = new / old
                                    val minX = -(webBoxW * (new - 1f))
                                    val minY = -(webBoxH * (new - 1f))
                                    webPanX = ((webPanX - centroid.x) * f + centroid.x + pan.x)
                                        .coerceIn(minX.coerceAtMost(0f), 0f)
                                    webPanY = ((webPanY - centroid.y) * f + centroid.y + pan.y)
                                        .coerceIn(minY.coerceAtMost(0f), 0f)
                                    zoomScale = new
                                }
                            )
                        }
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = webPanX
                            translationY = webPanY
                            transformOrigin = TransformOrigin(0f, 0f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(android.graphics.Color.BLACK)
                                isNestedScrollingEnabled = true
                                overScrollMode = View.OVER_SCROLL_ALWAYS
                                isVerticalScrollBarEnabled = true
                                isHorizontalScrollBarEnabled = true

                                setOnTouchListener { view, event ->
                                    when (event.action) {
                                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                            view.parent?.requestDisallowInterceptTouchEvent(true)
                                        }
                                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                            view.parent?.requestDisallowInterceptTouchEvent(false)
                                        }
                                    }
                                    false
                                }

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = false
                                    displayZoomControls = false
                                    setSupportZoom(false)
                                    allowFileAccess = true
                                    databaseEnabled = true
                                    userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                    }
                                }
                                webChromeClient = WebChromeClient()
                                loadUrl(previewUrl)
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFF38BDF8),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }

    // Offline PDF Info / Re-download Dialog
    if (showOfflineInfoDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineInfoDialog = false },
            title = {
                Text(
                    text = "অফলাইন পিডিএফ সংরক্ষিত ✓",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = "এই লেকচারের পিডিএফ আপনার ডিভাইসে অফলাইনে সংরক্ষিত আছে। ইন্টারনেট (নেট) ছাড়াই এটি দেখা যাচ্ছে। আপনি চাইলে এটি আবার ডাউনলোড করতে বা মুছে ফেলতে পারেন।",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOfflineInfoDialog = false
                        isDownloadingPdf = true
                        coroutineScope.launch {
                            val downloaded = PdfDownloader.downloadPdf(
                                context = context,
                                sheetUrl = sheetUrl,
                                subjectId = subjectId,
                                lectureId = lectureId,
                                onProgress = { _, status ->
                                    downloadProgressText = status
                                }
                            )
                            isDownloadingPdf = false
                            if (downloaded != null) {
                                isPdfOffline = true
                                Toast.makeText(context, "পিডিএফ পুনরায় ডাউনলোড হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldBrand)
                ) {
                    Text("আবার ডাউনলোড", color = Color.White)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            PdfDownloader.deleteDownloadedPdf(context, subjectId, lectureId)
                            isPdfOffline = false
                            showOfflineInfoDialog = false
                            Toast.makeText(context, "অফলাইন পিডিএফ মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("মুছে ফেলুন", color = Color(0xFFEF4444))
                    }
                    TextButton(onClick = { showOfflineInfoDialog = false }) {
                        Text("ঠিক আছে")
                    }
                }
            }
        )
    }
}

private fun openUrlInBrowser(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
