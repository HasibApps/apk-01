package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StudyUtils
import com.example.ui.theme.EmeraldBrand
import com.example.util.GalleryUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil
import kotlin.math.roundToInt

/** PDF zoom সীমা: এর চেয়ে কম (১০০% এর নিচে) zoom-out করা যাবে না। */
const val PDF_MIN_ZOOM = 1.0f
const val PDF_MAX_ZOOM = 4.0f

/**
 * ২ আঙুলের pinch (zoom + pan) ডিটেক্টর। ১ আঙুলের ইভেন্ট ছোঁয় না (স্ক্রল ঠিক থাকে);
 * শুধু ২+ আঙুল থাকলে ইভেন্ট consume করে যাতে নিচের স্ক্রল/ওয়েবভিউ বাধা না দেয়।
 */
suspend fun PointerInputScope.detectPdfPinch(
    onGesture: (centroid: Offset, pan: Offset, zoomChange: Float) -> Unit,
    onEnd: () -> Unit = {}
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var pinching = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressedCount = event.changes.count { it.pressed }
            if (pressedCount >= 2) {
                pinching = true
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                val centroid = event.calculateCentroid(useCurrent = true)
                if (zoom != 1f || pan != Offset.Zero) onGesture(centroid, pan, zoom)
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            } else if (pinching) {
                // pinch এর পর এক আঙুল বাকি থাকলে সবগুলো আঙুল ওঠা পর্যন্ত স্ক্রল/ট্যাপ আটকে রাখি
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
        if (pinching) onEnd()
    }
}

private class PageBmp(val bitmap: Bitmap, val width: Int)

/** বাইট-বাজেটে সীমাবদ্ধ পেজ ক্যাশ (সব পেজ মেমরিতে রেখে OOM হওয়া ঠেকায়) */
private class PageCache(private val maxBytes: Long) {
    val map = mutableStateMapOf<Int, PageBmp>()
    private val order = ArrayDeque<Int>()
    private var bytes = 0L

    fun put(index: Int, value: PageBmp) {
        map[index]?.let { bytes -= it.bitmap.byteCount.toLong() }
        order.remove(index)
        map[index] = value
        order.addLast(index)
        bytes += value.bitmap.byteCount.toLong()
        while (bytes > maxBytes && order.size > 1) {
            val old = order.removeFirst()
            if (old == index) { order.addLast(old); continue }
            map.remove(old)?.let { bytes -= it.bitmap.byteCount.toLong() }
        }
    }

    fun clear() {
        map.clear(); order.clear(); bytes = 0L
    }
}

private fun renderPageBitmap(renderer: PdfRenderer, pageIndex: Int, targetWidthPx: Int): Bitmap? {
    return try {
        synchronized(renderer) {
            val page = renderer.openPage(pageIndex)
            try {
                var w = targetWidthPx.coerceAtLeast(200)
                var h = (w.toFloat() * page.height / page.width).roundToInt()
                // খুব লম্বা হলে ছোট করি (মেমরি সুরক্ষা)
                if (h > 6000) {
                    w = (w * 6000f / h).roundToInt()
                    h = 6000
                }
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(AColor.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bmp
            } finally {
                page.close()
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}

/**
 * অফলাইন PDF ভিউয়ার।
 *  - ২ আঙুলে pinch করে zoom in / zoom out (সর্বনিম্ন ১০০%, সর্বোচ্চ ৪০০%)
 *  - প্রতিটি পৃষ্ঠার হেডারের ডানে ডাউনলোড আইকন → ওই পৃষ্ঠা PNG হিসেবে গ্যালারিতে সেভ
 */
@Composable
fun NativePdfRendererView(
    pdfFile: File,
    zoomScale: Float = 1.0f,
    onZoomChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var pageAspects by remember { mutableStateOf<List<Float>>(emptyList()) } // width / height
    var openError by remember { mutableStateOf<String?>(null) }

    val cache = remember { PageCache(80L * 1024 * 1024) }

    DisposableEffect(pdfFile.absolutePath) {
        try {
            if (pdfFile.exists() && pdfFile.canRead()) {
                val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val aspects = ArrayList<Float>(renderer.pageCount)
                for (i in 0 until renderer.pageCount) {
                    val p = renderer.openPage(i)
                    aspects.add(if (p.height > 0) p.width.toFloat() / p.height else 0.7f)
                    p.close()
                }
                pfd = fd
                pdfRenderer = renderer
                pageAspects = aspects
                pageCount = renderer.pageCount
                openError = null
            } else {
                openError = "পিডিএফ ফাইলটি পাওয়া যায়নি"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            openError = "পিডিএফ ওপেন করতে সমস্যা হয়েছে: ${e.localizedMessage}"
        }

        onDispose {
            try {
                pdfRenderer?.close()
                pfd?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            pdfRenderer = null
            cache.clear()
        }
    }

    if (openError != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = openError ?: "ত্রুটি", color = Color.LightGray, fontSize = 13.sp)
        }
        return
    }

    val renderer = pdfRenderer
    if (pageCount == 0 || renderer == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = EmeraldBrand, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
        }
        return
    }

    val listState = rememberLazyListState()
    var viewportW by remember { mutableIntStateOf(0) }
    var viewportH by remember { mutableIntStateOf(0) }
    var panX by remember { mutableFloatStateOf(0f) } // ০ .. -(W*(zoom-1))

    val zoom = zoomScale.coerceIn(PDF_MIN_ZOOM, PDF_MAX_ZOOM)
    val currentZoom by rememberUpdatedState(zoom)
    val onZoomChangeState by rememberUpdatedState(onZoomChange)

    // ক্রিস্প রেন্ডারের জন্য zoom স্থির হলে রেজোলিউশন লেভেল (১..৪) আপডেট
    var renderLevel by remember { mutableIntStateOf(1) }
    LaunchedEffect(zoom) {
        delay(250)
        renderLevel = ceil(zoom - 0.001f).toInt().coerceIn(1, 4)
    }

    // বাটন দিয়ে zoom বদলালে/কমালে translation ঠিক রাখা
    LaunchedEffect(zoom, viewportW) {
        val minTx = -(viewportW * (zoom - 1f))
        panX = panX.coerceIn(minTx.coerceAtMost(0f), 0f)
    }

    val targetRenderW = (viewportW.coerceAtLeast(360) * renderLevel).coerceAtMost(2400)

    var savingIndex by remember { mutableIntStateOf(-1) }
    fun savePage(index: Int) {
        if (savingIndex >= 0) return
        savingIndex = index
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                val bmp = renderPageBitmap(renderer, index, 2000)
                if (bmp == null) false else {
                    val saved = GalleryUtils.savePngToGallery(
                        context, bmp,
                        "${pdfFile.nameWithoutExtension}_page_${index + 1}",
                        "StudyApp/PDF"
                    )
                    bmp.recycle()
                    saved
                }
            }
            savingIndex = -1
            Toast.makeText(
                context,
                if (ok) "পৃষ্ঠা ${StudyUtils.toBengaliDigits(index + 1)} গ্যালারিতে সেভ হয়েছে 🖼️"
                else "পৃষ্ঠাটি সেভ করা যায়নি",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .clipToBounds()
            .onSizeChanged { viewportW = it.width; viewportH = it.height }
            // ২ আঙুলে pinch zoom
            .pointerInput(Unit) {
                detectPdfPinch(
                    onGesture = { centroid, pan, zoomChange ->
                        val old = currentZoom
                        val new = (old * zoomChange).coerceIn(PDF_MIN_ZOOM, PDF_MAX_ZOOM)
                        val f = new / old
                        val w = viewportW.toFloat()
                        val minTx = -(w * (new - 1f))
                        panX = ((panX - centroid.x) * f + centroid.x + pan.x)
                            .coerceIn(minTx.coerceAtMost(0f), 0f)
                        if (f != 1f) {
                            val idx = listState.firstVisibleItemIndex
                            val off = listState.firstVisibleItemScrollOffset
                            val newOff = ((off + centroid.y) * f - centroid.y - pan.y).roundToInt().coerceAtLeast(0)
                            listState.requestScrollToItem(idx, newOff)
                            onZoomChangeState(new)
                        } else if (pan.y != 0f) {
                            listState.dispatchRawDelta(-pan.y)
                        }
                    }
                )
            }
            // zoom করা অবস্থায় ১ আঙুলে আড়াআড়ি টেনে পাশে সরানো
            .pointerInput(zoom > 1.01f) {
                if (zoom <= 1.01f) return@pointerInput
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val minTx = -(viewportW * (currentZoom - 1f))
                    panX = (panX + dragAmount).coerceIn(minTx.coerceAtMost(0f), 0f)
                }
            }
    ) {
        val contentWidthDp = with(density) { (viewportW * zoom).toDp() }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .width(if (viewportW > 0) contentWidthDp else 0.dp)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = panX
                    transformOrigin = TransformOrigin(0f, 0f)
                },
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(pageCount) { pageIndex ->
                PdfPageItem(
                    pageIndex = pageIndex,
                    totalPages = pageCount,
                    renderer = renderer,
                    aspect = pageAspects.getOrNull(pageIndex) ?: 0.7f,
                    targetWidthPx = targetRenderW,
                    cached = cache.map[pageIndex],
                    isSaving = savingIndex == pageIndex,
                    onBitmapRendered = { bmp -> cache.put(pageIndex, PageBmp(bmp, bmp.width)) },
                    onDownloadPage = { savePage(pageIndex) }
                )
            }
        }
    }
}

@Composable
private fun PdfPageItem(
    pageIndex: Int,
    totalPages: Int,
    renderer: PdfRenderer,
    aspect: Float,
    targetWidthPx: Int,
    cached: PageBmp?,
    isSaving: Boolean,
    onBitmapRendered: (Bitmap) -> Unit,
    onDownloadPage: () -> Unit
) {
    // বর্তমানে ক্যাশে থাকা ছবি যথেষ্ট ক্রিস্প না হলে (বা না থাকলে) রেন্ডার
    LaunchedEffect(pageIndex, targetWidthPx, cached?.width, renderer) {
        if (cached != null && cached.width >= (targetWidthPx * 0.9f).toInt()) return@LaunchedEffect
        val bmp = withContext(Dispatchers.IO) { renderPageBitmap(renderer, pageIndex, targetWidthPx) }
        if (bmp != null) onBitmapRendered(bmp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        // হেডার: [পৃষ্ঠা ৮ / ৩৬] ............ [⬇ PNG ডাউনলোড]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "পৃষ্ঠা ${StudyUtils.toBengaliDigits(pageIndex + 1)} / ${StudyUtils.toBengaliDigits(totalPages)}",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !isSaving) { onDownloadPage() },
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = EmeraldBrand,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "এই পৃষ্ঠা PNG হিসেবে ডাউনলোড",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        val pageModifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect.coerceIn(0.2f, 5f))
            .clip(RoundedCornerShape(6.dp))
        if (cached != null) {
            Image(
                bitmap = cached.bitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = pageModifier,
                contentScale = ContentScale.FillBounds
            )
        } else {
            Box(
                modifier = pageModifier.background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldBrand, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        }
    }
}
