package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Chapter
import com.example.model.Paper
import com.example.model.StudyUtils
import com.example.model.Subject
import com.example.ui.theme.EmeraldBrand

data class PaperMeta(
    val serialBengali: String,
    val displayName: String,
    val subjectId: String,
    val paperIndex: Int,
    val colorStart: Color,
    val colorEnd: Color
)

val THIRTEEN_PAPERS_CONFIG = listOf(
    PaperMeta("১।", "গনিত ১ম পত্র", "sub_7", 0, Color(0xFF2563EB), Color(0xFF1D4ED8)),
    PaperMeta("২।", "গনিত ২য় পত্র", "sub_7", 1, Color(0xFF1D4ED8), Color(0xFF1E40AF)),
    PaperMeta("৩।", "পদার্থ ১ম পত্র", "sub_4", 0, Color(0xFF7C3AED), Color(0xFF6D28D9)),
    PaperMeta("৪।", "পদার্থ ২য় পত্র", "sub_4", 1, Color(0xFF6D28D9), Color(0xFF5B21B6)),
    PaperMeta("৫।", "রসায়ন ১ম পত্র", "sub_5", 0, Color(0xFF059669), Color(0xFF047857)),
    PaperMeta("৬।", "রসায়ন ২য় পত্র", "sub_5", 1, Color(0xFF047857), Color(0xFF065F46)),
    PaperMeta("৭।", "জীববিজ্ঞান ১ম পত্র", "sub_6", 0, Color(0xFF16A34A), Color(0xFF15803D)),
    PaperMeta("৮।", "জীববিজ্ঞান ২য় পত্র", "sub_6", 1, Color(0xFF15803D), Color(0xFF166534)),
    PaperMeta("৯।", "ইংরেজি ১ম পত্র", "sub_2", 0, Color(0xFF0284C7), Color(0xFF0369A1)),
    PaperMeta("১০।", "ইংরেজি ২য় পত্র", "sub_2", 1, Color(0xFF0369A1), Color(0xFF075985)),
    PaperMeta("১১।", "বাংলা ১ম পত্র", "sub_1", 0, Color(0xFFE11D48), Color(0xFFBE123C)),
    PaperMeta("১২।", "বাংলা ২য় পত্র", "sub_1", 1, Color(0xFFBE123C), Color(0xFF9F1239)),
    PaperMeta("১৩।", "আইসিটি", "sub_3", 0, Color(0xFFD97706), Color(0xFFB45309))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedAnalyticsScreen(
    subjects: List<Subject>,
    onNavigateToChapter: (subjectId: String, paperIndex: Int, chapterIndex: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPaperMeta by remember { mutableStateOf<PaperMeta?>(null) }

    // System Back Press handling: If in step 2 (chapter list), go back to step 1 (13 subjects list); else exit detailed analytics
    BackHandler {
        if (selectedPaperMeta != null) {
            selectedPaperMeta = null
        } else {
            onBack()
        }
    }

    AnimatedContent(
        targetState = selectedPaperMeta,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "AnalyticsPageTransition",
        modifier = modifier.fillMaxSize()
    ) { currentPaperMeta ->
        if (currentPaperMeta == null) {
            // STEP 01: 13 Subjects / Papers Overview
            ThirteenPapersListView(
                subjects = subjects,
                onSelectPaper = { selectedPaperMeta = it },
                onBack = onBack
            )
        } else {
            // STEP 02: Chapters Breakdown for the selected Subject/Paper
            val targetSubject = subjects.find { it.id == currentPaperMeta.subjectId }
            val targetPaper = targetSubject?.papers?.getOrNull(currentPaperMeta.paperIndex)

            PaperChaptersDetailView(
                meta = currentPaperMeta,
                paper = targetPaper,
                onChapterClick = { chapterIndex ->
                    onNavigateToChapter(currentPaperMeta.subjectId, currentPaperMeta.paperIndex, chapterIndex)
                },
                onBackToPapersList = { selectedPaperMeta = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThirteenPapersListView(
    subjects: List<Subject>,
    onSelectPaper: (PaperMeta) -> Unit,
    onBack: () -> Unit
) {
    // Grand totals calculated dynamically in real-time
    val (grandTotal, grandCompleted) = remember(subjects) {
        var total = 0
        var completed = 0
        for (sub in subjects) {
            for (paper in sub.papers) {
                for (ch in paper.chapters) {
                    total += ch.lectures.size
                    completed += ch.lectures.count { it.completed }
                }
            }
        }
        total to completed
    }

    val grandPercent = if (grandTotal > 0) ((grandCompleted.toFloat() / grandTotal) * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "স্টাডি অ্যানালিটিক্স (বিস্তারিত)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "মূল ৭টি বইয়ের ১৩টি বিষয়ের অগ্রগতি",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Stats Banner
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldBrand.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = EmeraldBrand,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "সার্বিক সম্পন্ন হার",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "মোট ১৩টি বিষয় • রিয়েল-টাইম আপডেট",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = EmeraldBrand.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, EmeraldBrand.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${StudyUtils.toBengaliDigits(grandPercent)}% সম্পন্ন",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldBrand,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (grandPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldBrand,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "মোট ক্লাস: ${StudyUtils.toBengaliDigits(grandTotal)}টি",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "সম্পন্ন: ${StudyUtils.toBengaliDigits(grandCompleted)}টি",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldBrand
                            )
                        }
                    }
                }
            }

            // Section Label
            item {
                Text(
                    text = "বিষয়ভিত্তিক তালিকা (অধ্যায় দেখতে ক্লিক করুন)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            // 13 Subjects / Papers
            itemsIndexed(THIRTEEN_PAPERS_CONFIG) { _, meta ->
                val targetSubject = subjects.find { it.id == meta.subjectId }
                val targetPaper = targetSubject?.papers?.getOrNull(meta.paperIndex)

                // Real-time dynamic count & percentage
                val totalClasses = targetPaper?.chapters?.sumOf { it.lectures.size } ?: 0
                val completedClasses = targetPaper?.chapters?.sumOf { it.lectures.count { lec -> lec.completed } } ?: 0
                val percent = if (totalClasses > 0) ((completedClasses.toFloat() / totalClasses) * 100).toInt() else 0

                PaperRowCard(
                    meta = meta,
                    totalClasses = totalClasses,
                    completedClasses = completedClasses,
                    percent = percent,
                    onClick = { onSelectPaper(meta) }
                )
            }
        }
    }
}

@Composable
private fun PaperRowCard(
    meta: PaperMeta,
    totalClasses: Int,
    completedClasses: Int,
    percent: Int,
    onClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "progress_${meta.displayName}"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("paper_card_${meta.subjectId}_${meta.paperIndex}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Serial number badge with subject gradient
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(meta.colorStart, meta.colorEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = meta.serialBengali.replace("।", ""),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Column {
                        // Requested Format: e.g. "১। গনিত ১ম পত্র"
                        Text(
                            text = "${meta.serialBengali} ${meta.displayName}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "সম্পন্ন: ${StudyUtils.toBengaliDigits(completedClasses)} / ${StudyUtils.toBengaliDigits(totalClasses)}টি",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Requested Format: [ ৪৫%  ক্লাস:৬৩টি ]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Percentage Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = meta.colorStart.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, meta.colorStart.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "${StudyUtils.toBengaliDigits(percent)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = meta.colorStart,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    // Class Count Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "ক্লাস:${StudyUtils.toBengaliDigits(totalClasses)}টি",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "View chapters",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Animated Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = meta.colorStart,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaperChaptersDetailView(
    meta: PaperMeta,
    paper: Paper?,
    onChapterClick: (chapterIndex: Int) -> Unit,
    onBackToPapersList: () -> Unit
) {
    val chapters = paper?.chapters ?: emptyList()
    val totalPaperClasses = chapters.sumOf { it.lectures.size }
    val completedPaperClasses = chapters.sumOf { it.lectures.count { lec -> lec.completed } }
    val overallPaperPercent = if (totalPaperClasses > 0) {
        ((completedPaperClasses.toFloat() / totalPaperClasses) * 100).toInt()
    } else {
        0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "${meta.serialBengali} ${meta.displayName}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "সকল অধ্যায়ের তালিকা (${StudyUtils.toBengaliDigits(chapters.size)}টি অধ্যায়)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackToPapersList) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Papers",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Summary Header Card for this Paper
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, meta.colorStart.copy(alpha = 0.35f)),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Brush.linearGradient(listOf(meta.colorStart, meta.colorEnd))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    // Required Format: [ ১। গনিত ১ম পত্র  ৪৫%  ক্লাস:৬৩টি ]
                                    Text(
                                        text = "${meta.serialBengali} ${meta.displayName}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "সম্পন্ন: ${StudyUtils.toBengaliDigits(completedPaperClasses)} / ${StudyUtils.toBengaliDigits(totalPaperClasses)}টি",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = meta.colorStart.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, meta.colorStart.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${StudyUtils.toBengaliDigits(overallPaperPercent)}% সম্পন্ন",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = meta.colorStart,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (overallPaperPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = meta.colorStart,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }

            item {
                Text(
                    text = "অধ্যায়ভিত্তিক বিস্তারিত অগ্রগতি:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            if (chapters.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "কোনো অধ্যায় পাওয়া যায়নি",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(chapters) { index, chapter ->
                    ChapterRowCard(
                        index = index,
                        chapter = chapter,
                        accentColor = meta.colorStart,
                        onClick = { onChapterClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterRowCard(
    index: Int,
    chapter: Chapter,
    accentColor: Color,
    onClick: () -> Unit
) {
    val totalLectures = chapter.lectures.size
    val completedLectures = chapter.lectures.count { it.completed }
    val percent = if (totalLectures > 0) ((completedLectures.toFloat() / totalLectures) * 100).toInt() else 0

    val animatedProgress by animateFloatAsState(
        targetValue = (percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "chapter_progress_$index"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("chapter_card_$index")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Chapter Serial badge
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = StudyUtils.toBengaliDigits(index + 1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }

                    // Chapter Title
                    Text(
                        text = chapter.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Format: [ ২৩%  ক্লাস:৮টি ]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (percent == 100) EmeraldBrand.copy(alpha = 0.15f) else accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, (if (percent == 100) EmeraldBrand else accentColor).copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "${StudyUtils.toBengaliDigits(percent)}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (percent == 100) EmeraldBrand else accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "ক্লাস:${StudyUtils.toBengaliDigits(totalLectures)}টি",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (percent == 100) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = EmeraldBrand,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Open Chapter",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Chapter Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (percent == 100) EmeraldBrand else accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
