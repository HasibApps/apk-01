package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StudyUtils
import com.example.ui.theme.EmeraldBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun NativePdfRendererView(
    pdfFile: File,
    zoomScale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var openError by remember { mutableStateOf<String?>(null) }

    // Rendered page bitmaps cache
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }

    // Initialize PdfRenderer safely
    DisposableEffect(pdfFile.absolutePath) {
        try {
            if (pdfFile.exists() && pdfFile.canRead()) {
                val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                pfd = fd
                pdfRenderer = renderer
                pageCount = renderer.pageCount
                openError = null
            } else {
                openError = "পিডিএফ ফাইলটি পাওয়া যায়নি"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            openError = "পিডিএফ ওপেন করতে সমস্যা হয়েছে: ${e.localizedMessage}"
        }

        onDispose {
            try {
                pdfRenderer?.close()
                pfd?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            pageBitmaps.values.forEach { it.recycle() }
            pageBitmaps.clear()
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
            Text(
                text = openError ?: "ত্রুটি",
                color = Color.LightGray,
                fontSize = 13.sp
            )
        }
        return
    }

    if (pageCount == 0 || pdfRenderer == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = EmeraldBrand,
                strokeWidth = 2.dp,
                modifier = Modifier.size(32.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = zoomScale
                scaleY = zoomScale
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
            .background(Color(0xFF0F172A)),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(pageCount) { pageIndex ->
            PdfPageItem(
                pageIndex = pageIndex,
                totalPages = pageCount,
                renderer = pdfRenderer,
                zoomScale = zoomScale,
                cachedBitmap = pageBitmaps[pageIndex],
                onBitmapRendered = { bmp ->
                    pageBitmaps[pageIndex] = bmp
                }
            )
        }
    }
}

@Composable
private fun PdfPageItem(
    pageIndex: Int,
    totalPages: Int,
    renderer: PdfRenderer?,
    zoomScale: Float,
    cachedBitmap: Bitmap?,
    onBitmapRendered: (Bitmap) -> Unit
) {
    var isRendering by remember { mutableStateOf(false) }

    LaunchedEffect(pageIndex, zoomScale, renderer) {
        if (renderer == null || cachedBitmap != null) return@LaunchedEffect

        isRendering = true
        withContext(Dispatchers.IO) {
            var renderedBitmap: Bitmap? = null
            try {
                // Synchronize access to PdfRenderer since it's not thread-safe
                synchronized(renderer) {
                    val page = renderer.openPage(pageIndex)
                    // Scale factor for crisp rendering on high-density screens
                    val densityMultiplier = (2f * zoomScale).coerceIn(1.2f, 3.5f)
                    val width = (page.width * densityMultiplier).toInt()
                    val height = (page.height * densityMultiplier).toInt()

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderedBitmap = bitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                renderedBitmap?.let { onBitmapRendered(it) }
                isRendering = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Page Number Indicator
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = "পৃষ্ঠা ${StudyUtils.toBengaliDigits(pageIndex + 1)} / ${StudyUtils.toBengaliDigits(totalPages)}",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        if (cachedBitmap != null) {
            Image(
                bitmap = cachedBitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.FillWidth
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = EmeraldBrand,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
