package com.example.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.viewinterop.AndroidView

/** JS -> Kotlin: the iframe pushes its state ~4x per second. */
class YouTubeStateBridge(private val callback: (time: Float, duration: Float, state: Int, loaded: Float) -> Unit) {
    @JavascriptInterface
    fun onState(time: Float, duration: Float, state: Int, loaded: Float) {
        callback(time, duration, state, loaded)
    }
}

/**
 * Online YouTube player with the SAME custom UI as the offline player.
 * The iframe runs with controls=0 (no YouTube chrome); the Compose overlay sits on top, swallows all
 * touches and drives the player through the IFrame API (ytPlay / ytPause / ytSeek / setPlaybackSpeed).
 *
 * NOTE: the YouTube IFrame API only accepts rates up to 2x, so 2.5x / 3x are disabled here
 * (they work in the offline ExoPlayer after download).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeCustomPlayer(
    videoId: String,
    startSeconds: Int,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    playbackSpeed: Float = 1.0f,
    boost2x: Boolean = false,
    onSpeedSelected: (Float) -> Unit = {},
    onToggleBoost: () -> Unit = {},
    isNotesOpen: Boolean = false,
    onToggleNotes: (() -> Unit)? = null,
    onToggleFullscreen: (() -> Unit)? = null,
    onTimeUpdate: (Float) -> Unit = {},
    onWebViewReady: (WebView?) -> Unit = {},
    onCustomView: ((View?, WebChromeClient.CustomViewCallback?) -> Unit)? = null
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }

    val effectiveSpeed = (if (boost2x) 2.0f else playbackSpeed).coerceAtMost(2.0f)

    DisposableEffect(videoId) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
            onWebViewReady(null)
        }
    }

    LaunchedEffect(effectiveSpeed, webView) {
        webView?.evaluateJavascript("if (window.setPlaybackSpeed) window.setPlaybackSpeed($effectiveSpeed);", null)
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
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
                        allowFileAccess = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    addJavascriptInterface(
                        YouTubeStateBridge { time, duration, state, loaded ->
                            // state: -1 unstarted, 0 ended, 1 playing, 2 paused, 3 buffering, 5 cued
                            positionMs = (time * 1000f).toLong().coerceAtLeast(0L)
                            durationMs = if (duration.isNaN() || duration < 0f) 0L else (duration * 1000f).toLong()
                            val frac = if (loaded.isNaN()) 0f else loaded.coerceIn(0f, 1f)
                            bufferedMs = (durationMs * frac).toLong()
                            isPlaying = state == 1
                            isBuffering = state == 3 || state == -1
                            onTimeUpdate(time)
                        },
                        "AndroidBridge"
                    )
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                            onCustomView?.invoke(view, callback)
                        }

                        override fun onHideCustomView() {
                            onCustomView?.invoke(null, null)
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = false
                    }
                    setBackgroundColor(0xFF000000.toInt())
                    loadDataWithBaseURL(
                        "https://www.youtube-nocookie.com",
                        buildYouTubeControlledHtml(videoId, startSeconds),
                        "text/html", "UTF-8", null
                    )
                    webView = this
                    onWebViewReady(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

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
                webView?.evaluateJavascript(if (isPlaying) "ytPause();" else "ytPlay();", null)
            },
            onSeekTo = { target ->
                webView?.evaluateJavascript("ytSeek(${target / 1000.0});", null)
                positionMs = target
            },
            onSpeedSelected = onSpeedSelected,
            onToggleBoost = onToggleBoost,
            onToggleFullscreen = onToggleFullscreen,
            isNotesOpen = isNotesOpen,
            onToggleNotes = onToggleNotes,
            maxSupportedSpeed = 2.0f
        )
    }
}

internal fun buildYouTubeControlledHtml(videoId: String, startTimeSeconds: Int = 0): String {
    val startParam = if (startTimeSeconds > 0) ",\n                            'start': $startTimeSeconds" else ""
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; background: #000; }
                html, body { width: 100%; height: 100%; overflow: hidden; background: #000; }
                .container { position: relative; width: 100%; height: 100%; }
                iframe { width: 100%; height: 100%; border: 0; pointer-events: none; }
            </style>
        </head>
        <body>
            <div class="container"><div id="player"></div></div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                document.getElementsByTagName('script')[0].parentNode.insertBefore(tag, document.getElementsByTagName('script')[0]);

                var player;
                var userExplicitlyPaused = false;
                window.currentPlaybackSpeed = 1.0;

                function pickRate(rate) {
                    try {
                        var rates = player.getAvailablePlaybackRates();
                        var best = rates[0];
                        for (var i = 0; i < rates.length; i++) { if (rates[i] <= rate + 0.001) best = rates[i]; }
                        return best;
                    } catch (e) { return Math.min(rate, 2); }
                }

                window.setPlaybackSpeed = function(rate) {
                    window.currentPlaybackSpeed = rate;
                    try { if (player && player.setPlaybackRate) player.setPlaybackRate(pickRate(rate)); } catch (e) {}
                };
                function ytPlay()  { userExplicitlyPaused = false; try { player.playVideo(); } catch (e) {} }
                function ytPause() { userExplicitlyPaused = true;  try { player.pauseVideo(); } catch (e) {} }
                function ytSeek(sec) { try { player.seekTo(sec, true); } catch (e) {} }

                window.resumePlayback = function() {
                    try {
                        if (player && !userExplicitlyPaused) player.playVideo();
                        window.setPlaybackSpeed(window.currentPlaybackSpeed);
                    } catch (e) {}
                };

                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        height: '100%',
                        width: '100%',
                        videoId: '$videoId',
                        playerVars: {
                            'autoplay': 1,
                            'playsinline': 1,
                            'rel': 0,
                            'modestbranding': 1,
                            'controls': 0,
                            'disablekb': 1,
                            'fs': 0,
                            'iv_load_policy': 3,
                            'cc_load_policy': 0,
                            'enablejsapi': 1$startParam
                        },
                        events: { 'onReady': onPlayerReady, 'onStateChange': onPlayerStateChange }
                    });
                }

                function onPlayerStateChange(event) {
                    if (event.data === YT.PlayerState.PLAYING) {
                        userExplicitlyPaused = false;
                        window.setPlaybackSpeed(window.currentPlaybackSpeed);
                    } else if (event.data === YT.PlayerState.ENDED) {
                        userExplicitlyPaused = true;
                    }
                }

                function onPlayerReady(event) {
                    try { event.target.playVideo(); window.setPlaybackSpeed(window.currentPlaybackSpeed); } catch (e) {}
                    setInterval(function() {
                        try {
                            if (player && player.getCurrentTime && window.AndroidBridge) {
                                window.AndroidBridge.onState(
                                    player.getCurrentTime() || 0,
                                    player.getDuration() || 0,
                                    player.getPlayerState(),
                                    player.getVideoLoadedFraction() || 0
                                );
                            }
                        } catch (e) {}
                    }, 250);
                }

                // keep playing across rotate / fullscreen transitions
                window.addEventListener('resize', function() {
                    if (!userExplicitlyPaused) {
                        setTimeout(window.resumePlayback, 150);
                        setTimeout(window.resumePlayback, 500);
                    }
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}
