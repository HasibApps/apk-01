package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Chapter
import com.example.model.StudyUtils
import com.example.model.Subject
import com.example.ui.theme.EmeraldBrand

data class AnalyticsSubjectDefinition(
    val id: Int,
    val displayName: String,
    val subjectId: String,
    val paperIndex: Int,
    val accentColor: Color,
    val gradientColors: List<Color>
)

val analyticsSubjectList = listOf(
    AnalyticsSubjectDefinition(
        id = 1,
        displayName = "১। গনিত ১ম পত্র",
        subjectId = "sub_7",
        paperIndex = 0,
        accentColor = Color(0xFF2563EB),
        gradientColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
    ),
    AnalyticsSubjectDefinition(
        id = 2,
        displayName = "২। গনিত ২য় পত্র",
        subjectId = "sub_7",
        paperIndex = 1,
        accentColor = Color(0xFF3B82F6),
        gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF1E40AF))
    ),
    AnalyticsSubjectDefinition(
        id = 3,
        displayName = "৩। পদার্থ ১ম পত্র",
        subjectId = "sub_4",
        paperIndex = 0,
        accentColor = Color(0xFF7C3AED),
        gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))
    ),
    AnalyticsSubjectDefinition(
        id = 4,
        displayName = "৪। পদার্থ ২য় পত্র",
        subjectId = "sub_4",
        paperIndex = 1,
        accentColor = Color(0xFF8B5CF6),
        gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF5B21B6))
    ),
    AnalyticsSubjectDefinition(
        id = 5,
        displayName = "৫। রসায়ন ১ম পত্র",
        subjectId = "sub_5",
        paperIndex = 0,
        accentColor = Color(0xFF059669),
        gradientColors = listOf(Color(0xFF059669), Color(0xFF047857))
    ),
    AnalyticsSubjectDefinition(
        id = 6,
        displayName = "৬। রসায়ন ২য় পত্র",
        subjectId = "sub_5",
        paperIndex = 1,
        accentColor = Color(0xFF10B981),
        gradientColors = listOf(Color(0xFF10B981), Color(0xFF065F46))
    ),
    AnalyticsSubjectDefinition(
        id = 7,
        displayName = "৭। জীববিজ্ঞান ১ম পত্র",
        subjectId = "sub_6",
        paperIndex = 0,
        accentColor = Color(0xFF16A34A),
        gradientColors = listOf(Color(0xFF16A34A), Color(0xFF15803D))
    ),
    AnalyticsSubjectDefinition(
        id = 8,
        displayName = "৮। জীববিজ্ঞান ২য় পত্র",
        subjectId = "sub_6",
        paperIndex = 1,
        accentColor = Color(0xFF22C55E),
        gradientColors = listOf(Color(0xFF22C55E), Color(0xFF166534))
    ),
    AnalyticsSubjectDefinition(
        id = 9,
        displayName = "৯। ইংরেজি ১ম পত্র",
        subjectId = "sub_2",
        paperIndex = 0,
        accentColor = Color(0xFF0284C7),
        gradientColors = listOf(Color(0xFF0284C7), Color(0xFF0369A1))
    ),
    AnalyticsSubjectDefinition(
        id = 10,
        displayName = "১০। ইংরেজি ২য় পত্র",
        subjectId = "sub_2",
        paperIndex = 1,
        accentColor = Color(0xFF0EA5E9),
        gradientColors = listOf(Color(0xFF0EA5E9), Color(0xFF075985))
    ),
    AnalyticsSubjectDefinition(
        id = 11,
        displayName = "১১। বাংলা ১ম পত্র",
        subjectId = "sub_1",
        paperIndex = 0,
        accentColor = Color(0xFFE11D48),
        gradientColors = listOf(Color(0xFFE11D48), Color(0xFFBE123C))
    ),
    AnalyticsSubjectDefinition(
        id = 12,
        displayName = "১২। বাংলা ২য় পত্র",
        subjectId = "sub_1",
        paperIndex = 1,
        accentColor = Color(0xFFF43F5E),
        gradientColors = listOf(Color(0xFFF43F5E), Color(0xFF9F1239))
    ),
    AnalyticsSubjectDefinition(
        id = 13,
        displayName = "১৩। আইসিটি",
        subjectId = "sub_3",
        paperIndex = 0,
        accentColor = Color(0xFFD97706),
        gradientColors = listOf(Color(0xFFD97706), Color(0xFFB45309))
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDetailsScreen(
    subjects: List<Subject>,
    onBack: () -> Unit,
    onNavigateToChapter: (subjectId: String, paperIndex: Int, chapterIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Step 1: List of 13 Subjects; Step 2: Selected subject's chapters
    var selectedDefinition by remember { mutableStateOf<AnalyticsSubjectDefinition?>(null) }

    BackHandler(enabled = selectedDefinition != null) {
        selectedDefinition = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedDefinition == null) "স্টাডি অ্যানালিটিক্স বিবরণী" else selectedDefinition!!.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (selectedDefinition == null) "মূল ৭টি বইয়ের ১৩টি বিষয়ের সামগ্রিক অগ্রগতি" else "সকল অধ্যায়ের বাস্তবভিত্তিক অগ্রগতি ও ক্লাস সংখ্যা",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedDefinition != null) {
                                selectedDefinition = null
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("analytics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedDefinition == null) {
                // STEP 1: Top to Bottom List of 13 Subjects
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("analytics_subjects_list")
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = EmeraldBrand.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, EmeraldBrand.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = EmeraldBrand,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "১৩টি বিষয়ভিত্তিক স্টাডি প্রগ্রেস",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = EmeraldBrand
                                    )
                                    Text(
                                        text = "যে কোনো বিষয়ে ক্লিক করে সকল অধ্যায়ের বিস্তারিত ক্লাস ও অগ্রগতি দেখুন",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    items(analyticsSubjectList, key = { it.id }) { def ->
                        val sub = subjects.find { it.id == def.subjectId }
                        val paper = sub?.papers?.getOrNull(def.paperIndex)
                        val totalClasses = paper?.chapters?.sumOf { it.lectures.size } ?: 0
                        val completedClasses = paper?.chapters?.sumOf { ch -> ch.lectures.count { it.completed } } ?: 0
                        val percent = if (totalClasses > 0) (completedClasses * 100) / totalClasses else 0

                        AnalyticsSubjectCard(
                            definition = def,
                            totalClasses = totalClasses,
                            completedClasses = completedClasses,
                            percent = percent,
                            onClick = { selectedDefinition = def }
                        )
                    }
                }
            } else {
                // STEP 2: Selected Subject Chapter Details List
                val def = selectedDefinition!!
                val sub = subjects.find { it.id == def.subjectId }
                val paper = sub?.papers?.getOrNull(def.paperIndex)
                val chapters = paper?.chapters ?: emptyList()
                val totalClasses = chapters.sumOf { it.lectures.size }
                val completedClasses = chapters.sumOf { ch -> ch.lectures.count { it.completed } }
                val overallPercent = if (totalClasses > 0) (completedClasses * 100) / totalClasses else 0

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("analytics_chapters_list")
                ) {
                    item {
                        // Summary Banner for Selected Subject
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            border = BorderStroke(1.dp, def.accentColor.copy(alpha = 0.4f)),
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
                                    Text(
                                        text = def.displayName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = def.accentColor
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = def.accentColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "$overallPercent%",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = def.accentColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "মোট ক্লাস: ${StudyUtils.toBengaliDigits(totalClasses)}টি",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "সম্পন্ন: ${StudyUtils.toBengaliDigits(completedClasses)}টি",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldBrand
                                    )
                                }

                                val animatedOverallProgress by animateFloatAsState(
                                    targetValue = overallPercent / 100f,
                                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                                    label = "overallProgressAnim"
                                )

                                LinearProgressIndicator(
                                    progress = { animatedOverallProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = def.accentColor,
                                    trackColor = def.accentColor.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "অধ্যায়ভিত্তিক তালিকা (${StudyUtils.toBengaliDigits(chapters.size)}টি অধ্যায়)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    itemsIndexed(chapters) { cIdx, ch ->
                        val chTotal = ch.lectures.size
                        val chCompleted = ch.lectures.count { it.completed }
                        val chPercent = if (chTotal > 0) (chCompleted * 100) / chTotal else 0

                        AnalyticsChapterCard(
                            chapter = ch,
                            chapterIndex = cIdx,
                            totalClasses = chTotal,
                            completedClasses = chCompleted,
                            percent = chPercent,
                            accentColor = def.accentColor,
                            onClick = {
                                if (sub != null) {
                                    onNavigateToChapter(sub.id, def.paperIndex, cIdx)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsSubjectCard(
    definition: AnalyticsSubjectDefinition,
    totalClasses: Int,
    completedClasses: Int,
    percent: Int,
    onClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "progressAnim"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("analytics_subject_card_${definition.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(definition.gradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = StudyUtils.toBengaliDigits(definition.id),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = definition.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ক্লাস: ${StudyUtils.toBengaliDigits(totalClasses)}টি  •  সম্পন্ন: ${StudyUtils.toBengaliDigits(completedClasses)}টি",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = definition.accentColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$percent%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = definition.accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Animated Linear Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = definition.accentColor,
                trackColor = definition.accentColor.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
fun AnalyticsChapterCard(
    chapter: Chapter,
    chapterIndex: Int,
    totalClasses: Int,
    completedClasses: Int,
    percent: Int,
    accentColor: Color,
    onClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "chapterProgressAnim"
    )

    val parsed = remember(chapter.name) { StudyUtils.parseChapterName(chapter.name) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("analytics_chapter_card_$chapterIndex")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Distinct cycle badge if exists
                    if (parsed.cycle.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = parsed.cycle,
                                color = Color(0xFF0F172A),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = parsed.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Format required: [১ অধ্যায়: ... ২৩% ক্লাস:৮টি ]
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$percent%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "ক্লাস:${StudyUtils.toBengaliDigits(totalClasses)}টি",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Go to Chapter",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (percent == 100) EmeraldBrand else accentColor,
                trackColor = accentColor.copy(alpha = 0.12f)
            )
        }
    }
}
