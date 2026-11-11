package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Lecture
import com.example.model.StudyUtils
import com.example.model.Subject
import com.example.ui.LectureFilter
import com.example.ui.components.AdminPasswordDialog
import com.example.ui.components.LectureCard
import com.example.ui.components.MappingType
import com.example.ui.components.SmartMappingDialog
import com.example.ui.theme.AmberPending
import com.example.ui.theme.BlueBrand
import com.example.ui.theme.EmeraldBrand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subject: Subject,
    searchQuery: String,
    lectureFilter: LectureFilter,
    isAdminUnlocked: Boolean,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onLectureFilterChange: (LectureFilter) -> Unit,
    onSelectPaper: (Int) -> Unit,
    onSelectChapter: (Int) -> Unit,
    onPlayLecture: (subjectId: String, subjectName: String, chapterName: String, lecture: Lecture) -> Unit,
    onToggleFavorite: (subjectId: String, lectureId: Int) -> Unit,
    onToggleCompleted: (subjectId: String, lectureId: Int) -> Unit,
    onVerifyAdminPassword: (String) -> Boolean,
    onBulkMapVideoLinks: (subjectId: String, rawText: String) -> Unit,
    onBulkMapSheetLinks: (subjectId: String, rawText: String) -> Unit,
    onMapLectureTitles: (subjectId: String, rawText: String) -> Unit,
    onUpdateLectureTitle: (subjectId: String, lectureId: Int, newTitle: String) -> Unit,
    onAddNewChapter: (subjectId: String, name: String) -> Unit,
    onRenameChapter: (subjectId: String, newName: String) -> Unit,
    onDeleteChapter: (subjectId: String) -> Unit,
    onDeleteAllLectures: (subjectId: String) -> Unit,
    onDeleteLecture: (subjectId: String, lectureId: Int) -> Unit,
    onSaveNotes: (subjectId: String, notes: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePaper = subject.papers.getOrNull(subject.activePaperIndex) ?: subject.papers.firstOrNull()
    val activeChapter = activePaper?.chapters?.getOrNull(subject.activeChapterIndex) ?: activePaper?.chapters?.firstOrNull()

    // Protected Action Dialog States
    var pendingAdminAction by remember { mutableStateOf<String?>(null) }
    var showVideoMappingDialog by remember { mutableStateOf(false) }
    var showSheetMappingDialog by remember { mutableStateOf(false) }
    var showTitleMappingDialog by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var showRenameChapterDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAllLecturesDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var lectureToEditOrDelete by remember { mutableStateOf<Lecture?>(null) }

    // Helper to guard actions with password "hasib99"
    fun executeProtectedAction(actionName: String, onExecute: () -> Unit) {
        if (isAdminUnlocked) {
            onExecute()
        } else {
            pendingAdminAction = actionName
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = subject.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                OutlinedButton(
                    onClick = { showNotesDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldBrand),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.NoteAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নোটস", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Paper Tabs (১ম পত্র, ২য় পত্র, etc.)
        if (subject.papers.size > 1) {
            ScrollableTabRow(
                selectedTabIndex = subject.activePaperIndex.coerceIn(0, subject.papers.lastIndex),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldBrand,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (subject.activePaperIndex in tabPositions.indices) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[subject.activePaperIndex]),
                            color = EmeraldBrand
                        )
                    }
                }
            ) {
                subject.papers.forEachIndexed { index, paper ->
                    Tab(
                        selected = subject.activePaperIndex == index,
                        onClick = { onSelectPaper(index) },
                        text = {
                            Text(
                                text = paper.paperName,
                                fontSize = 13.sp,
                                fontWeight = if (subject.activePaperIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // Chapters Horizontal Bar with Screenshot_20260912-063847.png effect around each chapter
        if (activePaper != null && activePaper.chapters.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                activePaper.chapters.forEachIndexed { index, ch ->
                    val isSelected = subject.activeChapterIndex == index

                    // Outer halo frame matching Screenshot_20260912-063847.png
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = if (isSelected) Color(0xFFE0F2FE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF7DD3FC) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onSelectChapter(index) }
                            .testTag("chapter_tab_$index")
                    ) {
                        Box(
                            modifier = Modifier.padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                                                )
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                val parsedName = remember(ch.name) { StudyUtils.parseChapterName(ch.name) }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    if (parsedName.cycle.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) Color(0xFFFDE047) else Color(0xFFF59E0B),
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = parsedName.cycle,
                                                color = Color(0xFF0F172A),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = parsedName.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("লেকচার খুঁজুন...", fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .testTag("lecture_search_bar")
        )

        // The 5 Admin-Protected Action Buttons (Requirement 4)
        // [স্মার্ট লিঙ্ক ম্যাপিং (অধ্যায়)]
        // [স্মার্ট লিঙ্ক ম্যাপিং Lecture Sheet]
        // (+নতুন অধ্যায়)
        // (নাম পরিবর্তন)
        // (অধ্যায় মুছুন)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button 1: [স্মার্ট লিঙ্ক ম্যাপিং (অধ্যায়)]
            Button(
                onClick = {
                    executeProtectedAction("স্মার্ট লিঙ্ক ম্যাপিং (অধ্যায়)") {
                        showVideoMappingDialog = true
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlueBrand),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_smart_mapping_chapter")
            ) {
                Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("স্মার্ট লিঙ্ক ম্যাপিং (অধ্যায়)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (!isAdminUnlocked) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.White.copy(alpha = 0.8f))
                }
            }

            // Button 2: [স্মার্ট লিঙ্ক ম্যাপিং Lecture Sheet]
            Button(
                onClick = {
                    executeProtectedAction("স্মার্ট লিঙ্ক ম্যাপিং Lecture Sheet") {
                        showSheetMappingDialog = true
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_smart_mapping_sheet")
            ) {
                Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("স্মার্ট লিঙ্ক ম্যাপিং Lecture Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (!isAdminUnlocked) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.White.copy(alpha = 0.8f))
                }
            }

            // Button 3: [টাইটেল ম্যাপিং] (Requirement 3)
            Button(
                onClick = {
                    executeProtectedAction("টাইটেল ম্যাপিং") {
                        showTitleMappingDialog = true
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_smart_mapping_title")
            ) {
                Icon(imageVector = Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("টাইটেল ম্যাপিং", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (!isAdminUnlocked) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.White.copy(alpha = 0.8f))
                }
            }

            // Button 4: (+নতুন অধ্যায়)
            OutlinedButton(
                onClick = {
                    executeProtectedAction("+নতুন অধ্যায়") {
                        showAddChapterDialog = true
                    }
                },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_add_chapter")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldBrand)
                Spacer(modifier = Modifier.width(4.dp))
                Text("+নতুন অধ্যায়", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EmeraldBrand)
                if (!isAdminUnlocked) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(11.dp))
                }
            }

            // Button 5: (নাম পরিবর্তন)
            OutlinedButton(
                onClick = {
                    executeProtectedAction("নাম পরিবর্তন") {
                        showRenameChapterDialog = true
                    }
                },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_rename_chapter")
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("নাম পরিবর্তন", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                if (!isAdminUnlocked) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(11.dp))
                }
            }

            // Button 6: (অধ্যায় মুছুন)
            OutlinedButton(
                onClick = {
                    executeProtectedAction("অধ্যায় মুছুন") {
                        showDeleteConfirmDialog = true
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_delete_chapter")
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("অধ্যায় মুছুন", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                if (!isAdminUnlocked) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(11.dp))
                }
            }
        }

        // Filter Chips (সব, বাকি, সম্পন্ন) + এক ক্লিকে সব মুছুন (Requirement 1)
        val allLectures = activeChapter?.lectures ?: emptyList()
        val totalCount = allLectures.size
        val completedCount = allLectures.count { it.completed }
        val pendingCount = totalCount - completedCount

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = lectureFilter == LectureFilter.ALL,
                onClick = { onLectureFilterChange(LectureFilter.ALL) },
                label = { Text("সব (${StudyUtils.toBengaliDigits(totalCount)})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldBrand.copy(alpha = 0.2f))
            )
            FilterChip(
                selected = lectureFilter == LectureFilter.PENDING,
                onClick = { onLectureFilterChange(LectureFilter.PENDING) },
                label = { Text("বাকি (${StudyUtils.toBengaliDigits(pendingCount)})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AmberPending.copy(alpha = 0.2f))
            )
            FilterChip(
                selected = lectureFilter == LectureFilter.COMPLETED,
                onClick = { onLectureFilterChange(LectureFilter.COMPLETED) },
                label = { Text("সম্পন্ন (${StudyUtils.toBengaliDigits(completedCount)})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldBrand.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.weight(1f))

            // Delete All Button (Requirement 1 - password protected with hasib99)
            OutlinedButton(
                onClick = {
                    pendingAdminAction = "সব লেকচার মুছুন"
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("btn_delete_all_lectures")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("মুছুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Filtered lectures list
        val displayedLectures = allLectures.filter { lec ->
            val matchesFilter = when (lectureFilter) {
                LectureFilter.ALL -> true
                LectureFilter.PENDING -> !lec.completed
                LectureFilter.COMPLETED -> lec.completed
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                lec.title.contains(searchQuery, ignoreCase = true) ||
                        "লেকচার ${lec.id}".contains(searchQuery) ||
                        "${lec.id}".contains(searchQuery)
            }
            matchesFilter && matchesSearch
        }

        if (displayedLectures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (allLectures.isEmpty()) "কোন লেকচার পাওয়া যায়নি।" else "অনুসন্ধানে কোন লেকচার পাওয়া যায়নি।",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedLectures, key = { it.id }) { lecture ->
                    LectureCard(
                        lecture = lecture,
                        onClick = {
                            onPlayLecture(
                                subject.id,
                                subject.name,
                                activeChapter?.name ?: "",
                                lecture
                            )
                        },
                        onToggleFavorite = {
                            onToggleFavorite(subject.id, lecture.id)
                        },
                        onToggleCompleted = {
                            onToggleCompleted(subject.id, lecture.id)
                        },
                        onLongClick = {
                            lectureToEditOrDelete = lecture
                        }
                    )
                }
            }
        }
    }

    // Long Press: Edit Title and Delete Single Lecture Dialog (Requirement 2)
    if (lectureToEditOrDelete != null) {
        val lec = lectureToEditOrDelete!!
        var editedTitle by remember(lec.id) { mutableStateOf(lec.title) }

        Dialog(onDismissRequest = { lectureToEditOrDelete = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "লেকচার পরিচালনা ও টাইটেল পরিবর্তন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "এই লেকচারের টাইটেল পরিবর্তন করুন অথবা প্রয়োজন না থাকলে লেকচারটি সম্পূর্ণ মুছে ফেলুন:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("লেকচার টাইটেল") },
                        placeholder = { Text("যেমন: লেকচার ১") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_lecture_title_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onDeleteLecture(subject.id, lec.id)
                                lectureToEditOrDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_delete_single_lecture")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("মুছুন", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { lectureToEditOrDelete = null }) {
                                Text("বাতিল", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = {
                                    if (editedTitle.isNotBlank()) {
                                        onUpdateLectureTitle(subject.id, lec.id, editedTitle)
                                        lectureToEditOrDelete = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldBrand),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_save_lecture_title")
                            ) {
                                Text("টাইটেল সংরক্ষণ", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete All Lectures Confirmation Dialog (Requirement 1)
    if (showDeleteAllLecturesDialog && activeChapter != null) {
        AlertDialog(
            onDismissRequest = { showDeleteAllLecturesDialog = false },
            title = {
                Text(
                    text = "সব লেকচার মুছে ফেলার সতর্কতা",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিতভাবে \"${activeChapter.name}\" অধ্যায়ের সব (${StudyUtils.toBengaliDigits(activeChapter.lectures.size)}টি) লেকচার এক ক্লিকে মুছে ফেলতে চান?\n\nএই পরিবর্তন অ্যাপে স্থায়ী থাকবে।",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllLectures(subject.id)
                        showDeleteAllLecturesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("btn_confirm_delete_all_lectures")
                ) {
                    Text("হ্যাঁ, সব মুছুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllLecturesDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Admin Password Dialog (Gating buttons with "hasib99")
    if (pendingAdminAction != null) {
        AdminPasswordDialog(
            actionName = pendingAdminAction!!,
            onDismiss = { pendingAdminAction = null },
            onUnlocked = {
                val action = pendingAdminAction
                pendingAdminAction = null
                onVerifyAdminPassword("hasib99")
                when (action) {
                    "স্মার্ট লিঙ্ক ম্যাপিং (অধ্যায়)" -> showVideoMappingDialog = true
                    "স্মার্ট লিঙ্ক ম্যাপিং Lecture Sheet" -> showSheetMappingDialog = true
                    "টাইটেল ম্যাপিং" -> showTitleMappingDialog = true
                    "+নতুন অধ্যায়" -> showAddChapterDialog = true
                    "নাম পরিবর্তন" -> showRenameChapterDialog = true
                    "অধ্যায় মুছুন" -> showDeleteConfirmDialog = true
                    "সব লেকচার মুছুন" -> onDeleteAllLectures(subject.id)
                }
            }
        )
    }

    // Video Mapping Dialog
    if (showVideoMappingDialog && activeChapter != null) {
        val currentLinksText = activeChapter.lectures
            .filter { it.link.isNotBlank() }
            .mapIndexed { idx, it ->
                val num = String.format("%02d", idx + 1)
                "$num#${it.link}"
            }
            .joinToString("\n")

        SmartMappingDialog(
            type = MappingType.VIDEO_LINKS,
            subjectName = subject.name,
            chapterName = activeChapter.name,
            initialText = currentLinksText,
            onDismiss = { showVideoMappingDialog = false },
            onSave = { rawText ->
                onBulkMapVideoLinks(subject.id, rawText)
                showVideoMappingDialog = false
            }
        )
    }

    // Sheet Mapping Dialog
    if (showSheetMappingDialog && activeChapter != null) {
        val hasAnySheet = activeChapter.lectures.any { it.sheetLink.isNotBlank() }
        val currentSheetsText = if (hasAnySheet) {
            activeChapter.lectures
                .mapIndexed { idx, it ->
                    val num = String.format("%02d", idx + 1)
                    val value = if (it.sheetLink.isNotBlank()) it.sheetLink else "N/A"
                    "$num#$value"
                }
                .joinToString("\n")
        } else ""

        SmartMappingDialog(
            type = MappingType.LECTURE_SHEETS,
            subjectName = subject.name,
            chapterName = activeChapter.name,
            initialText = currentSheetsText,
            onDismiss = { showSheetMappingDialog = false },
            onSave = { rawText ->
                onBulkMapSheetLinks(subject.id, rawText)
                showSheetMappingDialog = false
            }
        )
    }

    // Title Mapping Dialog (Requirement 3)
    if (showTitleMappingDialog && activeChapter != null) {
        val currentTitlesText = activeChapter.lectures
            .joinToString("\n") { it.lectureTag.ifBlank { "Lecture:${String.format("%02d", it.id)}" } }

        SmartMappingDialog(
            type = MappingType.TITLES,
            subjectName = subject.name,
            chapterName = activeChapter.name,
            initialText = currentTitlesText,
            onDismiss = { showTitleMappingDialog = false },
            onSave = { rawText ->
                onMapLectureTitles(subject.id, rawText)
                showTitleMappingDialog = false
            }
        )
    }

    // Add Chapter Dialog
    if (showAddChapterDialog) {
        var newChapterName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddChapterDialog = false },
            title = { Text("নতুন অধ্যায় যোগ করুন") },
            text = {
                OutlinedTextField(
                    value = newChapterName,
                    onValueChange = { newChapterName = it },
                    label = { Text("অধ্যায়ের নাম") },
                    placeholder = { Text("যেমন: অধ্যায় ৪: কাজ, শক্তি ও ক্ষমতা") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChapterName.isNotBlank()) {
                            onAddNewChapter(subject.id, newChapterName)
                            showAddChapterDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldBrand)
                ) {
                    Text("যোগ করুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChapterDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Rename Chapter Dialog
    if (showRenameChapterDialog && activeChapter != null) {
        var editChapterName by remember { mutableStateOf(activeChapter.name) }
        AlertDialog(
            onDismissRequest = { showRenameChapterDialog = false },
            title = { Text("অধ্যায়ের নাম পরিবর্তন") },
            text = {
                OutlinedTextField(
                    value = editChapterName,
                    onValueChange = { editChapterName = it },
                    label = { Text("নতুন নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editChapterName.isNotBlank()) {
                            onRenameChapter(subject.id, editChapterName)
                            showRenameChapterDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldBrand)
                ) {
                    Text("আপডেট", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameChapterDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Delete Chapter Confirmation Dialog
    if (showDeleteConfirmDialog && activeChapter != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("অধ্যায় মুছে ফেলার সতর্কতা") },
            text = { Text("আপনি কি নিশ্চিতভাবে \"${activeChapter.name}\" অধ্যায়টি মুছে ফেলতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteChapter(subject.id)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("হ্যাঁ, মুছুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Notes Dialog
    if (showNotesDialog) {
        var notesInput by remember { mutableStateOf(subject.notes) }
        Dialog(onDismissRequest = { showNotesDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${subject.name} - ব্যক্তিগত নোটস 📝",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        placeholder = { Text("এখানে গুরুত্বপূর্ণ সূত্র, টপিক বা নোট লিখে রাখুন...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 15
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showNotesDialog = false }) {
                            Text("বাতিল")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSaveNotes(subject.id, notesInput)
                                showNotesDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldBrand)
                        ) {
                            Text("সংরক্ষণ", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
