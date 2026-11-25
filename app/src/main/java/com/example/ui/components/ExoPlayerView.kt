package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.delay
import java.io.File

/**
 * Offline (downloaded file) player. Video surface = Media3 PlayerView with its own controller disabled;
 * ALL controls come from [PlayerControlsOverlay] so offline and YouTube playback look/behave the same.
 *
 * Speed/boost state is owned by the caller (VideoPlayerView) so it survives fullscreen & player switches.
 */
@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerView(
    filePath: String,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    playbackSpeed: Float = 1.0f,
    boost2x: Boolean = false,
    onSpeedSelected: (Float) -> Unit = {},
    onToggleBoost: () -> Unit = {},
    isNotesOpen: Boolean = false,
    onToggleNotes: (() -> Unit)? = null,
    onToggleFullscreen: (() -> Unit)? = null,
    onFallbackToOnline: (() -> Unit)? = null,
    onReDownload: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember(filePath) {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (filePath.startsWith("content://") || filePath.startsWith("http")) {
                Uri.parse(filePath)
            } else {
                Uri.fromFile(File(filePath))
            }
            setMediaItem(MediaItem.fromUri(uri))
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
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) playbackError = null
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // State ticker (the overlay decides whether to show this or the scrub position, so no fighting)
    LaunchedEffect(exoPlayer) {
        while (true) {
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.let { if (it < 0L) 0L else it } // C.TIME_UNSET is negative
            bufferedMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            delay(250)
        }
    }

    // Menu speed or double-tap 2x
    val effectiveSpeed = if (boost2x) 2.0f else playbackSpeed
    LaunchedEffect(effectiveSpeed, exoPlayer) {
        exoPlayer.playbackParameters = PlaybackParameters(effectiveSpeed)
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
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER) // overlay draws its own spinner
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (playbackError == null) {
            PlayerControlsOverlay(
                isFullscreen = isFullscreen,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedMs = bufferedMs,
                playbackSpeed = playbackSpeed,
                boost2x = boost2x,
                onPlayPause = {
                    if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0L)
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                onSeekTo = { target ->
                    exoPlayer.seekTo(target)
                    positionMs = target
                },
                onSpeedSelected = onSpeedSelected,
                onToggleBoost = onToggleBoost,
                onToggleFullscreen = onToggleFullscreen,
                isNotesOpen = isNotesOpen,
                onToggleNotes = onToggleNotes,
                maxSupportedSpeed = 3.0f
            )
        }

        // Error fallback if the offline file is unplayable
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
    }
}
