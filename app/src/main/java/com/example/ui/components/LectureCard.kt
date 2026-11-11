package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Lecture
import com.example.model.StudyUtils
import com.example.ui.theme.EmeraldBrand
import com.example.ui.theme.RoseFavorite

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LectureCard(
    lecture: Lecture,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCompleted: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val videoId = remember(lecture.link) { StudyUtils.extractYouTubeVideoId(lecture.link) }
    val thumbnailUrl = remember(videoId) { StudyUtils.getYouTubeThumbnailUrl(videoId) }
    var menuOpen by remember { mutableStateOf(false) }

    // YouTube-style list item: big rounded thumbnail on the left, title on the right, ⋮ menu at the end.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp)
            .testTag("lecture_card_${lecture.id}"),
        verticalAlignment = Alignment.Top
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(150.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF131D31)),
            contentAlignment = Alignment.Center
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.LightGray.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Completed: red progress line at the bottom of the thumbnail (like a watched YouTube video)
            if (lecture.completed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFFEF4444))
                )
            }

            // Offline badge on thumbnail corner
            if (lecture.localVideoPath.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = EmeraldBrand,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Offline", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title + small info line
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lecture.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val rawTag = lecture.lectureTag.ifBlank { "Lecture:${String.format("%02d", lecture.id)}" }
                Text(
                    text = rawTag.removePrefix("▶").trim(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val sheetTrimmed = lecture.sheetLink.trim()
                if (sheetTrimmed.equals("N/A", ignoreCase = true)) {
                    Text(
                        text = "N/A",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                            .testTag("lecture_na_badge_${lecture.id}")
                    )
                } else if (sheetTrimmed.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                            .testTag("lecture_sheet_badge_${lecture.id}")
                    ) {
                        Icon(Icons.Default.Description, null, tint = Color(0xFF0284C7), modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Sheet", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                }

                if (lecture.isFavorite) {
                    Icon(Icons.Filled.Favorite, null, tint = RoseFavorite, modifier = Modifier.size(13.dp))
                }
            }
        }

        // ⋮ menu (favorite / completed / edit)
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("lecture_menu_btn_${lecture.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (lecture.isFavorite) "ফেভারিট থেকে সরান" else "ফেভারিট করুন") },
                    leadingIcon = {
                        Icon(
                            if (lecture.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            null,
                            tint = if (lecture.isFavorite) RoseFavorite else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { menuOpen = false; onToggleFavorite() },
                    modifier = Modifier.testTag("favorite_btn_${lecture.id}")
                )
                DropdownMenuItem(
                    text = { Text(if (lecture.completed) "সম্পন্ন চিহ্ন তুলুন" else "সম্পন্ন করুন") },
                    leadingIcon = {
                        Icon(
                            if (lecture.completed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                            null,
                            tint = if (lecture.completed) EmeraldBrand else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { menuOpen = false; onToggleCompleted() },
                    modifier = Modifier.testTag("complete_btn_${lecture.id}")
                )
                if (onLongClick != null) {
                    DropdownMenuItem(
                        text = { Text("এডিট / মুছুন") },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { menuOpen = false; onLongClick() }
                    )
                }
            }
        }
    }
}
