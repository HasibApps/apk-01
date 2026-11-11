package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.filled.Pause
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerView(
    filePath: String,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    externalPlaybackSpeed: Float = 1.0f,
    onToggleFullscreen: (() -> Unit)? = null,
    onFallbackToOnline: (() -> Unit)? = null,
    onReDownload: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var currentSpeed by remember { mutableFloatStateOf(externalPlaybackSpeed) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableStateOf(0) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var bufferedMs by remember { mutableStateOf(0L) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var seekFeedback by remember { mutableStateOf<String?>(null) }
    var seekFeedbackTick by remember { mutableStateOf(0) }
    var seekFeedbackRight by remember { mutableStateOf(true) }

    val exoPlayer = remember(filePath) {
        ExoPlayer.Builder(context).build().apply {
            val file = File(filePath)
            val uri = if (filePath.startsWith("content://") || filePath.startsWith("http")) {
                Uri.parse(filePath)
            } else {
                Uri.fromFile(file)
            }
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = "ভিডিও ফাইলটি চালানো যাচ্ছে না (সম্ভবত ডাউনলোড অসম্পূর্ণ বা ফরম্যাট মিলছে না)।"
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    playbackError = null
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Progress ticker
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (!isScrubbing) {
                positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            }
            durationMs = exoPlayer.duration.let { if (it < 0) 0L else it }
            bufferedMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            delay(250)
        }
    }

    // Auto-hide controls
    LaunchedEffect(controlsVisible, isPlaying, interactionTick, isScrubbing) {
        if (controlsVisible && isPlaying && !isScrubbing) {
            delay(3500)
            controlsVisible = false
        }
    }

    // Clear double-tap seek feedback
    LaunchedEffect(seekFeedbackTick) {
        if (seekFeedback != null) {
            delay(700)
            seekFeedback = null
        }
    }

    LaunchedEffect(currentSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(currentSpeed)
    }

    LaunchedEffect(externalPlaybackSpeed) {
        if (externalPlaybackSpeed > 0f) {
            currentSpeed = externalPlaybackSpeed
            exoPlayer.playbackParameters = PlaybackParameters(externalPlaybackSpeed)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = exoPlayer
                    // Custom controls are drawn in Compose below (only Play/Pause in the center)
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture layer: single tap = show/hide controls,
        // double tap on right half = +10s, double tap on left half = -10s (YouTube style)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(exoPlayer) {
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                            interactionTick++
                        },
                        onDoubleTap = { offset ->
                            val forward = offset.x > size.width / 2f
                            val dur = exoPlayer.duration.let { if (it < 0) Long.MAX_VALUE else it }
                            val target = if (forward) {
                                (exoPlayer.currentPosition + 10_000L).coerceAtMost(dur)
                            } else {
                                (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                            }
                            exoPlayer.seekTo(target)
                            positionMs = target
                            seekFeedbackRight = forward
                            seekFeedback = if (forward) "+10 সেকেন্ড" else "-10 সেকেন্ড"
                            seekFeedbackTick++
                        }
                    )
                }
        )

        // Double-tap feedback
        if (seekFeedback != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (seekFeedbackRight) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 36.dp)
                ) {
                    Text(
                        text = seekFeedback ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Controls: ONLY the Play/Pause button in the center, seek bar at the very bottom
        if (controlsVisible && playbackError == null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f))) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .background(Color.White, CircleShape)
                        .pointerInput(exoPlayer) {
                            detectTapGestures(onTap = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                interactionTick++
                            })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                ) {
                    val shownPos = if (isScrubbing) (scrubFraction * durationMs).toLong() else positionMs
                    Text(
                        text = "${formatExoTime(shownPos)}  •  ${formatExoTime(durationMs)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
                    )
                    ExoSeekBar(
                        fraction = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
                        bufferedFraction = if (durationMs > 0) bufferedMs.toFloat() / durationMs else 0f,
                        onScrub = { f ->
                            isScrubbing = true
                            scrubFraction = f
                            interactionTick++
                        },
                        onScrubFinished = { f ->
                            if (durationMs > 0) {
                                val target = (f * durationMs).toLong()
                                exoPlayer.seekTo(target)
                                positionMs = target
                            }
                            isScrubbing = false
                            interactionTick++
                        },
                        scrubbing = isScrubbing,
                        scrubFraction = scrubFraction
                    )
                }
            }
        }

        // Error Fallback Screen if offline video is unplayable
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playbackError ?: "ভিডিও লোড ব্যর্থ হয়েছে",
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onFallbackToOnline != null) {
                            Button(
                                onClick = onFallbackToOnline,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("অনলাইন YouTube প্লেয়ার", fontSize = 12.sp)
                            }
                        }
                        if (onReDownload != null) {
                            OutlinedButton(
                                onClick = onReDownload,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("আবার ডাউনলোড", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Clean, Non-overlapping Top/Bottom Bar Indicator
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10B981).copy(alpha = 0.9f)
            ) {
                Text(
                    text = "ExoPlayer অফলাইন",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            // Speed Selector Button
            Box {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.padding(2.dp)
                ) {
                    IconButton(
                        onClick = { showSpeedMenu = true },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Text(
                            text = "${currentSpeed}x",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = { showSpeedMenu = false }
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                        DropdownMenuItem(
                            text = { Text("${speed}x") },
                            onClick = {
                                currentSpeed = speed
                                showSpeedMenu = false
                            }
                        )
                    }
                }
            }

            // Fullscreen toggle
            if (onToggleFullscreen != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.padding(2.dp)
                ) {
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}


private fun formatExoTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val sec = total % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, sec) else String.format("%02d:%02d", m, sec)
}

/** Thin YouTube-style seek bar glued to the very bottom edge of the player. */
@Composable
private fun ExoSeekBar(
    fraction: Float,
    bufferedFraction: Float,
    scrubbing: Boolean,
    scrubFraction: Float,
    onScrub: (Float) -> Unit,
    onScrubFinished: (Float) -> Unit
) {
    var widthPx by remember { mutableStateOf(1) }
    val density = LocalDensity.current
    val shown = (if (scrubbing) scrubFraction else fraction).coerceIn(0f, 1f)
    val thumbSize = if (scrubbing) 16.dp else 12.dp
    val thumbPx = with(density) { thumbSize.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { o ->
                    onScrubFinished((o.x / widthPx).coerceIn(0f, 1f))
                })
            }
            .pointerInput(Unit) {
                var last = 0f
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        last = (o.x / widthPx).coerceIn(0f, 1f)
                        onScrub(last)
                    },
                    onDragEnd = { onScrubFinished(last) },
                    onDragCancel = { onScrubFinished(last) },
                    onHorizontalDrag = { change, _ ->
                        last = (change.position.x / widthPx).coerceIn(0f, 1f)
                        onScrub(last)
                    }
                )
            },
        contentAlignment = Alignment.BottomStart
    ) {
        // Track (sits at the very bottom)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.3f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferedFraction.coerceIn(0f, 1f))
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(shown)
                .height(3.dp)
                .background(Color(0xFFEF4444))
        )
        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset((shown * widthPx - thumbPx / 2f).toInt().coerceIn(0, (widthPx - thumbPx).toInt().coerceAtLeast(0)), (thumbPx / 2f - with(density) { 1.5.dp.toPx() }).toInt()) }
                .size(thumbSize)
                .background(Color(0xFFEF4444), CircleShape)
        )
    }
}
