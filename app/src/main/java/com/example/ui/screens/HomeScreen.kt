package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StudyUtils
import com.example.model.Subject
import com.example.ui.theme.AmberPending
import com.example.ui.theme.EmeraldBrand
import com.example.ui.theme.TealAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    subjects: List<Subject>,
    onSubjectClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenDetailedAnalytics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (grandTotalLectures, grandCompletedLectures) = remember(subjects) {
        var total = 0
        var completed = 0
        for (sub in subjects) {
            for (paper in sub.papers) {
                for (chapter in paper.chapters) {
                    total += chapter.lectures.size
                    completed += chapter.lectures.count { it.completed }
                }
            }
        }
        total to completed
    }

    val overallPercent = if (grandTotalLectures > 0) {
        ((grandCompletedLectures.toFloat() / grandTotalLectures) * 100).toInt()
    } else {
        0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header with Settings on top-left and Backup on top-right
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )

                    Column {
                        Text(
                            text = "স্টাডি ট্র্যাকার",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "স্মার্ট লিঙ্ক ও ইউটিউব প্লেয়ার",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("top_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = onOpenBackup,
                    modifier = Modifier.testTag("top_backup_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Backup and Sync",
                        tint = EmeraldBrand
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {

            // 3. Subjects Grid Section Header
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "বিষয়সমূহ (পছন্দের বিষয়ে ক্লিক করুন)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            // 4. Subject Cards (Grid of 2 columns)
            items(subjects, key = { it.id }) { subject ->
                SubjectGridCard(
                    subject = subject,
                    onClick = { onSubjectClick(subject.id) }
                )
            }

            // 5. Analytics Section
            item(span = { GridItemSpan(2) }) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = EmeraldBrand,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "সামগ্রিক স্টাডি অ্যানালিটিক্স",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // User Requirement 2: ( view details ) button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = EmeraldBrand.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, EmeraldBrand.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clickable { onOpenDetailedAnalytics() }
                                    .testTag("btn_view_details")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "view details",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldBrand
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = "View Details",
                                        tint = EmeraldBrand,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // Donut Chart Canvas
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(110.dp)
                            ) {
                                Canvas(modifier = Modifier.size(100.dp)) {
                                    val strokeWidth = 14.dp.toPx()
                                    val total = grandTotalLectures.coerceAtLeast(1).toFloat()
                                    val completedAngle = (grandCompletedLectures / total) * 360f

                                    // Background / Remaining arc
                                    drawArc(
                                        color = Color(0xFFF59E0B),
                                        startAngle = -90f + completedAngle,
                                        sweepAngle = 360f - completedAngle,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )

                                    // Completed arc
                                    drawArc(
                                        color = Color(0xFF10B981),
                                        startAngle = -90f,
                                        sweepAngle = completedAngle,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$overallPercent%",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "সম্পন্ন",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Stats Chips
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.width(150.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = EmeraldBrand.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "সম্পন্ন ক্লাস", fontSize = 11.sp, color = EmeraldBrand, fontWeight = FontWeight.Medium)
                                        Text(text = StudyUtils.toBengaliDigits(grandCompletedLectures), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldBrand)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AmberPending.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "বাকি ক্লাস", fontSize = 11.sp, color = AmberPending, fontWeight = FontWeight.Medium)
                                        Text(text = StudyUtils.toBengaliDigits(grandTotalLectures - grandCompletedLectures), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmberPending)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Footer matching Screenshot_20260911-120913.jpg
            item(span = { GridItemSpan(2) }) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Copyright ©2026 এইটা একটা Educational App.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "The App Create By Hasib Bro",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "contact: ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "@Yyuu43222",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0088CC),
                            modifier = Modifier
                                .clickable {
                                    try {
                                        val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=Yyuu43222"))
                                        context.startActivity(telegramIntent)
                                    } catch (e: Exception) {
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Yyuu43222"))
                                        context.startActivity(webIntent)
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectGridCard(
    subject: Subject,
    onClick: () -> Unit
) {
    val gradientColors = when (subject.id) {
        "sub_1" -> listOf(Color(0xFFE11D48), Color(0xFFBE123C)) // বাংলা (Rose / Crimson)
        "sub_2" -> listOf(Color(0xFF0284C7), Color(0xFF0369A1)) // ইংরেজি (Azure Blue)
        "sub_3" -> listOf(Color(0xFFEA580C), Color(0xFFD97706)) // আইসিটি (Warm Amber Orange - Screenshot_20260912-063710.png)
        "sub_4" -> listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)) // পদার্থবিজ্ঞান (Deep Violet)
        "sub_5" -> listOf(Color(0xFF059669), Color(0xFF047857)) // রসায়ন (Emerald)
        "sub_6" -> listOf(Color(0xFF16A34A), Color(0xFF15803D)) // জীববিজ্ঞান (Forest Green)
        "sub_7" -> listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)) // উচ্চতর গণিত (Royal Blue)
        else -> listOf(Color(subject.colorStart), Color(subject.colorEnd))
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag("subject_card_${subject.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
        ) {
            // Decorative background circles matching Screenshot_20260912-063710.png
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Top-right subtle circular watermark
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = size.width * 0.46f,
                    center = Offset(size.width * 0.96f, size.height * 0.12f)
                )
                // Bottom-left subtle circular watermark
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.width * 0.36f,
                    center = Offset(size.width * 0.08f, size.height * 0.96f)
                )
            }

            // Card content: Title on upper left, circle action button on bottom right
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = subject.name,
                        fontSize = if (subject.name.length > 8) 16.sp else 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = if (subject.papers.size > 1) "১ম ও ২য় পত্র" else "মূল কোর্স"
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.20f),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.65f)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "প্রবেশ করুন",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getSubjectIconVector(iconName: String): ImageVector {
    return when (iconName) {
        "brain" -> Icons.Default.Psychology
        "atom" -> Icons.Default.EmojiObjects
        "flask" -> Icons.Default.Science
        "dna" -> Icons.Default.LocalFlorist
        "calculator" -> Icons.Default.Calculate
        else -> Icons.Default.AutoStories
    }
}
