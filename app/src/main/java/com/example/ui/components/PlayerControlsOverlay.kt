package com.example.ui.components

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.PixelCopy
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StudyUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// =============================================================================
//  Shared helpers
// =============================================================================

internal val PlayerAccent = Color(0xFF00B0FF)
private val BarColor = Color(0xFF1B1B1D).copy(alpha = 0.66f)

/** Speed list shown in the settings menu: Normal 1.25x 1.5x 1.75x 2x 2.5x 3x */
internal val PlayerSpeedOptions = listOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

internal fun speedLabel(s: Float): String = when {
    s == 1.0f -> "Normal"
    s % 1f == 0f -> "${s.toInt()}x"
    else -> "${s}x"
}

/** HH:MM:SS style when the video is >= 1h, otherwise MM:SS. Never throws on negatives. */
internal fun formatPlayerTime(ms: Long, longForm: Boolean): String {
    val total = ms.coerceAtLeast(0L) / 1000L
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (longForm) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}

internal fun Context.findPlayerActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private fun bnPercent(fraction: Float): String =
    StudyUtils.toBengaliDigits((fraction.coerceIn(0f, 1f) * 100f).roundToInt()) + "%"

private enum class GestureKind { VOLUME, BRIGHTNESS }
private data class GestureIndicator(val kind: GestureKind, val value: Float)

// =============================================================================
//  Screenshot (camera icon): PixelCopy of the player area -> Gallery
// =============================================================================

private fun captureWindowRegion(activity: Activity, rect: android.graphics.Rect, onDone: (Bitmap?) -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || rect.width() <= 0 || rect.height() <= 0) {
        onDone(null); return
    }
    try {
        val bmp = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        PixelCopy.request(
            activity.window, rect, bmp,
            { result -> onDone(if (result == PixelCopy.SUCCESS) bmp else null) },
            Handler(Looper.getMainLooper())
        )
    } catch (e: Exception) {
        e.printStackTrace()
        onDone(null)
    }
}

private fun saveBitmapToGallery(context: Context, bmp: Bitmap): Boolean {
    val name = "StudyApp_${System.currentTimeMillis()}.jpg"
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/StudyApp")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } else {
            // API 24-28: no storage permission in this app -> app-specific pictures folder
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return false
            dir.mkdirs()
            FileOutputStream(File(dir, name)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// =============================================================================
//  The overlay itself. Backend-agnostic (ExoPlayer / YouTube iFrame):
//  host feeds state in, receives commands out.
// =============================================================================

/**
 * Custom player UI (reference: Screenshot_20261125-111842 / -115732).
 *
 *  - Bottom glass pill:  [play/pause] [volume] ──seek── [camera] [note] [settings]
 *  - Fullscreen pill:    [play/pause] [-10] [+10] [volume] 0:01:17 ──seek── -2:59:17 [camera] [note] [settings] [exit]
 *  - Fullscreen gestures: left half vertical drag = brightness, right half = volume
 *  - Single tap = show/hide controls ONLY (never seeks)
 *  - Double tap = toggle 2x speed (never seeks)
 *  - Seek bar: tap-to-seek, drag-to-scrub with live timestamp pill
 */
@Composable
fun PlayerControlsOverlay(
    isFullscreen: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    playbackSpeed: Float,
    boost2x: Boolean,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onToggleBoost: () -> Unit,
    onToggleFullscreen: (() -> Unit)?,
    isNotesOpen: Boolean,
    onToggleNotes: (() -> Unit)?,
    modifier: Modifier = Modifier,
    maxSupportedSpeed: Float = 3.0f,
    /** true  = seek once on finger release (best for long videos)
     *  false = throttled seek while dragging */
    seekOnDragRelease: Boolean = true,
    /** Explicit -10/+10 buttons in the fullscreen bar (buttons only, never screen taps) */
    showSeekButtons: Boolean = true
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findPlayerActivity() }
    val audio = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val scope = rememberCoroutineScope()

    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var seekTarget by remember { mutableStateOf<Long?>(null) }
    var seekStamp by remember { mutableIntStateOf(0) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var indicator by remember { mutableStateOf<GestureIndicator?>(null) }
    var indicatorTick by remember { mutableIntStateOf(0) }
    var gestureDragging by remember { mutableStateOf(false) }
    var volumeLevel by remember { mutableFloatStateOf(0f) }
    var lastNonZeroVolume by remember { mutableFloatStateOf(0.5f) }
    var windowBounds by remember { mutableStateOf(android.graphics.Rect()) }
    var hideForCapture by remember { mutableStateOf(false) }
    var lastThrottledSeekAt by remember { mutableLongStateOf(0L) }

    val currentToggleBoost by rememberUpdatedState(onToggleBoost)

    // ---------------------------------------------------------------- system volume / brightness
    fun readVolume(): Float {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
    }

    fun applyVolume(v: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val clamped = v.coerceIn(0f, 1f)
        try {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (clamped * max).roundToInt(), 0)
        } catch (_: SecurityException) { /* DND policy – ignore */ }
        volumeLevel = clamped
        if (clamped > 0f) lastNonZeroVolume = clamped
    }

    fun readBrightness(): Float {
        val w = activity?.window ?: return 0.5f
        val b = w.attributes.screenBrightness
        if (b >= 0f) return b
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } catch (e: Exception) {
            0.5f
        }
    }

    fun applyBrightness(v: Float) {
        val w = activity?.window ?: return
        val lp = w.attributes
        lp.screenBrightness = v.coerceIn(0.01f, 1f)
        w.attributes = lp
    }

    fun showIndicator(kind: GestureKind, value: Float) {
        indicator = GestureIndicator(kind, value.coerceIn(0f, 1f))
        indicatorTick++
    }

    DisposableEffect(Unit) {
        volumeLevel = readVolume()
        if (volumeLevel > 0f) lastNonZeroVolume = volumeLevel
        onDispose {
            // Give brightness control back to the system when the player goes away
            activity?.window?.let { w ->
                val lp = w.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                w.attributes = lp
            }
        }
    }

    // ---------------------------------------------------------------- effects
    LaunchedEffect(controlsVisible, isPlaying, interactionTick, scrubbing, showSpeedMenu) {
        if (controlsVisible && isPlaying && !scrubbing && !showSpeedMenu) {
            delay(3500)
            controlsVisible = false
        }
    }
    LaunchedEffect(isPlaying) { if (!isPlaying) controlsVisible = true }
    LaunchedEffect(controlsVisible) { if (controlsVisible) volumeLevel = readVolume() }

    LaunchedEffect(indicatorTick) {
        delay(900)
        if (!gestureDragging) indicator = null
    }

    // Optimistic position after a seek so the progress tick can't make the thumb jump back
    LaunchedEffect(seekStamp) {
        if (seekTarget != null) {
            delay(1200)
            seekTarget = null
        }
    }
    LaunchedEffect(positionMs) {
        val t = seekTarget
        if (t != null && abs(positionMs - t) < 1500L) seekTarget = null
    }

    // ---------------------------------------------------------------- derived values
    val safeDuration = durationMs.coerceAtLeast(0L)
    val canSeek = safeDuration > 0L
    val longForm = safeDuration >= 3_600_000L || positionMs >= 3_600_000L
    val basePos = seekTarget ?: positionMs
    val shownPos =
        if (scrubbing) (scrubFraction * safeDuration).toLong() else basePos.coerceIn(0L, if (canSeek) safeDuration else Long.MAX_VALUE)
    val fraction = if (canSeek) (shownPos.toFloat() / safeDuration).coerceIn(0f, 1f) else 0f
    val bufferedFraction = if (canSeek) (bufferedMs.toFloat() / safeDuration).coerceIn(0f, 1f) else 0f

    fun commitSeekMs(target: Long) {
        if (!canSeek) return
        val t = target.coerceIn(0L, safeDuration)
        seekTarget = t
        seekStamp++
        onSeekTo(t)
    }

    val doScreenshot: () -> Unit = {
        val act = activity
        if (act != null) {
            scope.launch {
                hideForCapture = true
                delay(220) // let the overlay fade out so it is not in the picture
                captureWindowRegion(act, android.graphics.Rect(windowBounds)) { bmp ->
                    hideForCapture = false
                    val ok = bmp != null && saveBitmapToGallery(context, bmp)
                    Toast.makeText(
                        context,
                        if (ok) "স্ক্রিনশট গ্যালারিতে সেভ হয়েছে 📸" else "স্ক্রিনশট নেওয়া যায়নি",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val showControls = controlsVisible && !hideForCapture

    // ===========================================================================  UI
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val b = coords.boundsInWindow()
                windowBounds = android.graphics.Rect(
                    b.left.roundToInt(), b.top.roundToInt(), b.right.roundToInt(), b.bottom.roundToInt()
                )
            }
    ) {
        // ---------------------------------------------------------- gesture layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                // single tap = toggle controls ; double tap = 2x on/off. NO seeking here.
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                            showSpeedMenu = false
                            interactionTick++
                        },
                        onDoubleTap = {
                            currentToggleBoost()
                            interactionTick++
                        }
                    )
                }
                // fullscreen only: left half = brightness, right half = volume
                .pointerInput(isFullscreen) {
                    if (!isFullscreen) return@pointerInput
                    var leftSide = true
                    var startValue = 0f
                    var accumulated = 0f
                    detectVerticalDragGestures(
                        onDragStart = { o: Offset ->
                            gestureDragging = true
                            leftSide = o.x < size.width / 2f
                            accumulated = 0f
                            startValue = if (leftSide) readBrightness() else readVolume()
                            showIndicator(if (leftSide) GestureKind.BRIGHTNESS else GestureKind.VOLUME, startValue)
                        },
                        onDragEnd = { gestureDragging = false; indicatorTick++ },
                        onDragCancel = { gestureDragging = false; indicatorTick++ },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            // drag up = increase. ~75% of the height sweeps the full range.
                            accumulated += (-dragAmount / size.height.coerceAtLeast(1)) * 1.35f
                            val v = (startValue + accumulated).coerceIn(0f, 1f)
                            if (leftSide) applyBrightness(v) else applyVolume(v)
                            showIndicator(if (leftSide) GestureKind.BRIGHTNESS else GestureKind.VOLUME, v)
                        }
                    )
                }
        )

        // ---------------------------------------------------------- buffering spinner
        if (isBuffering && !hideForCapture) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.align(Alignment.Center).size(38.dp)
            )
        }

        // ---------------------------------------------------------- 2x badge
        if (boost2x && !hideForCapture) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("2x ⚡", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ---------------------------------------------------------- volume / brightness pill (top centre)
        val ind = indicator
        if (ind != null && !hideForCapture) {
            GesturePill(
                indicator = ind,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp)
            )
        }

        // ---------------------------------------------------------- controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Inline mode: fullscreen button in a small pill at top-right (as in the reference image)
                if (!isFullscreen && onToggleFullscreen != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(BarColor, RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 2.dp)
                    ) {
                        PlayerIconButton(Icons.Default.Fullscreen, "Fullscreen") {
                            interactionTick++
                            onToggleFullscreen()
                        }
                    }
                }

                // ---- Video-র মাঝখানে: [⏪10] [▶/⏸]  (বামে শুধু ১০ সেকেন্ড পিছনের বাটন) ----
                val centerBtnSize = if (isFullscreen) 64.dp else 52.dp
                val sideBtnSize = if (isFullscreen) 48.dp else 40.dp
                // ১০ সেকেন্ড পিছনে (play/pause বাটনের ঠিক বামে)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = -(centerBtnSize / 2 + sideBtnSize / 2 + 18.dp))
                        .size(sideBtnSize)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                        .clickable { interactionTick++; commitSeekMs(shownPos - 10_000L) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Back 10s",
                        tint = Color.White,
                        modifier = Modifier.size(if (isFullscreen) 30.dp else 24.dp)
                    )
                }
                // মাঝখানের play / pause (বাফারিং স্পিনার দেখালে লুকায়)
                if (!isBuffering) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(centerBtnSize)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                            .clickable { interactionTick++; onPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(if (isFullscreen) 38.dp else 30.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (isFullscreen) 20.dp else 8.dp,
                            vertical = if (isFullscreen) 14.dp else 8.dp
                        )
                ) {
                    if (!isFullscreen) {
                        Text(
                            text = "${formatPlayerTime(shownPos, longForm)} / ${formatPlayerTime(safeDuration, longForm)}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 14.dp, bottom = 2.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isFullscreen) 56.dp else 50.dp)
                            .background(BarColor, RoundedCornerShape(28.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // play / pause
                        PlayerIconButton(
                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            desc = if (isPlaying) "Pause" else "Play"
                        ) { interactionTick++; onPlayPause() }

                        // -10 / +10 : explicit buttons only (fullscreen)
                        if (isFullscreen && showSeekButtons) {
                            PlayerIconButton(Icons.Default.Replay10, "Back 10s") {
                                interactionTick++; commitSeekMs(shownPos - 10_000L)
                            }
                            PlayerIconButton(Icons.Default.Forward10, "Forward 10s") {
                                interactionTick++; commitSeekMs(shownPos + 10_000L)
                            }
                        }

                        // volume / mute
                        PlayerIconButton(
                            icon = if (volumeLevel <= 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            desc = "Volume"
                        ) {
                            interactionTick++
                            if (volumeLevel > 0f) applyVolume(0f) else applyVolume(lastNonZeroVolume.coerceAtLeast(0.3f))
                            showIndicator(GestureKind.VOLUME, volumeLevel)
                        }

                        if (isFullscreen) {
                            Text(
                                text = formatPlayerTime(shownPos, longForm),
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.widthIn(min = 48.dp).padding(start = 4.dp)
                            )
                        }

                        PlayerSeekBar(
                            modifier = Modifier.weight(1f).padding(horizontal = if (isFullscreen) 12.dp else 8.dp),
                            fraction = fraction,
                            bufferedFraction = bufferedFraction,
                            enabled = canSeek,
                            scrubbing = scrubbing,
                            previewLabel = formatPlayerTime(shownPos, longForm),
                            onScrubStart = { f ->
                                scrubbing = true
                                scrubFraction = f
                                interactionTick++
                            },
                            onScrubUpdate = { f ->
                                scrubFraction = f
                                interactionTick++
                                if (!seekOnDragRelease) {
                                    val now = SystemClock.elapsedRealtime()
                                    if (now - lastThrottledSeekAt > 200L) {
                                        lastThrottledSeekAt = now
                                        onSeekTo((f * safeDuration).toLong())
                                    }
                                }
                            },
                            onScrubEnd = { f ->
                                commitSeekMs((f.coerceIn(0f, 1f) * safeDuration).toLong())
                                scrubbing = false
                                interactionTick++
                            },
                            onScrubCancel = { scrubbing = false }
                        )

                        if (isFullscreen) {
                            Text(
                                text = "-" + formatPlayerTime((safeDuration - shownPos).coerceAtLeast(0L), longForm),
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.widthIn(min = 56.dp).padding(end = 4.dp)
                            )
                        }

                        // camera (screenshot)
                        PlayerIconButton(Icons.Default.PhotoCamera, "Screenshot") {
                            interactionTick++; doScreenshot()
                        }

                        // note (lecture sheet pdf)
                        if (onToggleNotes != null) {
                            PlayerIconButton(
                                icon = Icons.Default.StickyNote2,
                                desc = "Lecture sheet",
                                tint = if (isNotesOpen) PlayerAccent else Color.White
                            ) { interactionTick++; onToggleNotes() }
                        }

                        // settings -> speed menu
                        Box {
                            PlayerIconButton(Icons.Default.Settings, "Settings") {
                                interactionTick++; showSpeedMenu = true
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("প্লেব্যাক স্পিড", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                    onClick = {},
                                    enabled = false
                                )
                                PlayerSpeedOptions.forEach { s ->
                                    val supported = s <= maxSupportedSpeed + 0.001f
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = speedLabel(s) + if (!supported) "  (শুধু অফলাইন ভিডিওতে)" else "",
                                                fontSize = 14.sp
                                            )
                                        },
                                        leadingIcon = {
                                            if (playbackSpeed == s) {
                                                Icon(Icons.Default.Check, null, tint = PlayerAccent, modifier = Modifier.size(18.dp))
                                            } else {
                                                Spacer(Modifier.size(18.dp))
                                            }
                                        },
                                        enabled = supported,
                                        onClick = {
                                            onSpeedSelected(s)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // exit fullscreen (inside the bar when fullscreen)
                        if (isFullscreen && onToggleFullscreen != null) {
                            PlayerIconButton(Icons.Default.FullscreenExit, "Exit fullscreen") {
                                interactionTick++; onToggleFullscreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
//  Small building blocks
// =============================================================================

@Composable
private fun PlayerIconButton(
    icon: ImageVector,
    desc: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) {
        Icon(imageVector = icon, contentDescription = desc, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/** White rounded pill: [icon] label ........ ৯৫%   (reference: Screenshot_20261125-115732) */
@Composable
private fun GesturePill(indicator: GestureIndicator, modifier: Modifier = Modifier) {
    val isVolume = indicator.kind == GestureKind.VOLUME
    val label = if (isVolume) "শব্দ" else "উজ্জ্বলতা"
    val icon: ImageVector = when {
        !isVolume -> Icons.Default.BrightnessMedium
        indicator.value <= 0f -> Icons.AutoMirrored.Filled.VolumeOff
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }
    Box(
        modifier = modifier
            .width(230.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFB8B8B8))
    ) {
        // filled portion
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(indicator.value.coerceIn(0.001f, 1f))
                .background(Color.White)
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color.Black, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(label, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Text(bnPercent(indicator.value), color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Seek bar with a 48dp touch area and a slim visual track.
 *
 * One gesture handler covers both interactions:
 *   - press + release          => tap-to-seek   (ratio = x / trackWidth, clamped 0..1)
 *   - press + horizontal move  => drag-to-scrub (live timestamp pill)
 * Forced LTR so the timeline never mirrors in RTL locales (video time is not RTL).
 */
@Composable
private fun PlayerSeekBar(
    fraction: Float,
    bufferedFraction: Float,
    enabled: Boolean,
    scrubbing: Boolean,
    previewLabel: String,
    onScrubStart: (Float) -> Unit,
    onScrubUpdate: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    onScrubCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val density = LocalDensity.current
        var widthPx by remember { mutableIntStateOf(1) }
        var pillWidthPx by remember { mutableIntStateOf(0) }
        val shown = fraction.coerceIn(0f, 1f)
        val trackHeight = if (scrubbing) 6.dp else 4.dp
        val thumbSize = if (scrubbing) 18.dp else 12.dp
        val thumbPx = with(density) { thumbSize.toPx() }
        val pillLift = with(density) { 46.dp.roundToPx() }
        val edgeSlack = with(density) { 24.dp.roundToPx() }

        Box(
            modifier = modifier
                .height(48.dp)
                .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fun ratio(x: Float) = (x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        var last = ratio(down.position.x)
                        onScrubStart(last)
                        down.consume()
                        var cancelled = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) { cancelled = true; break }
                            if (change.changedToUp()) { change.consume(); break }
                            last = ratio(change.position.x)
                            onScrubUpdate(last)
                            change.consume()
                        }
                        if (cancelled) onScrubCancel() else onScrubEnd(last)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // unplayed
            Box(
                Modifier.fillMaxWidth().height(trackHeight).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.30f))
            )
            // buffered
            Box(
                Modifier.fillMaxWidth(bufferedFraction.coerceIn(0f, 1f)).height(trackHeight).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.48f))
            )
            // played
            Box(
                Modifier.fillMaxWidth(shown).height(trackHeight).clip(CircleShape)
                    .background(PlayerAccent)
            )
            // thumb
            Box(
                Modifier
                    .offset { IntOffset((shown * widthPx - thumbPx / 2f).roundToInt(), 0) }
                    .size(thumbSize)
                    .background(PlayerAccent, CircleShape)
            )
            // live timestamp preview
            if (scrubbing) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            val x = (shown * widthPx - pillWidthPx / 2f).roundToInt()
                                .coerceIn(-edgeSlack, (widthPx - pillWidthPx + edgeSlack).coerceAtLeast(-edgeSlack))
                            IntOffset(x, -pillLift)
                        }
                        .onSizeChanged { pillWidthPx = it.width }
                        .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(previewLabel, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
