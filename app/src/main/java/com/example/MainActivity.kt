package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.model.ThemeMode
import com.example.ui.StudyViewModel
import com.example.ui.components.BackupDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.VideoPlayerView
import com.example.ui.screens.DetailedAnalyticsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SubjectDetailScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: StudyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                StudyTrackerApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun StudyTrackerApp(viewModel: StudyViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    val activeSubjectId by viewModel.activeSubjectId.collectAsState()
    val activePlayingLecture by viewModel.activePlayingLecture.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lectureFilter by viewModel.lectureFilter.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showSplashScreen by remember { mutableStateOf(true) }
    var showDetailedAnalytics by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        showSplashScreen = false
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Toast event collector
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // System Back Press handling
    BackHandler(enabled = showDetailedAnalytics || activePlayingLecture != null || activeSubjectId != null) {
        if (showDetailedAnalytics) {
            showDetailedAnalytics = false
        } else if (activePlayingLecture != null) {
            viewModel.closePlayer()
        } else if (activeSubjectId != null) {
            viewModel.backToHome()
        }
    }

    val activeSubject = subjects.firstOrNull { it.id == activeSubjectId }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when {
                // Screen 0: Detailed Analytics View (Requirement 2)
                showDetailedAnalytics -> {
                    DetailedAnalyticsScreen(
                        subjects = subjects,
                        onNavigateToChapter = { subId, paperIdx, chapterIdx ->
                            viewModel.selectPaper(subId, paperIdx)
                            viewModel.selectChapter(subId, chapterIdx)
                            viewModel.openSubject(subId)
                            showDetailedAnalytics = false
                        },
                        onBack = { showDetailedAnalytics = false },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                // Screen 1: Active In-App Video Player
                activePlayingLecture != null -> {
                    VideoPlayerView(
                        state = activePlayingLecture!!,
                        isAdminUnlocked = isAdminUnlocked,
                        onVerifyAdminPassword = { viewModel.verifyAdminPassword(it) },
                        onClose = { viewModel.closePlayer() },
                        onToggleComplete = {
                            val state = activePlayingLecture!!
                            viewModel.toggleCompleted(state.subjectId, state.lecture.id)
                        },
                        onToggleFavorite = {
                            val state = activePlayingLecture!!
                            viewModel.toggleFavorite(state.subjectId, state.lecture.id)
                        },
                        onUpdateLecture = { title, videoLink, sheetLink ->
                            val state = activePlayingLecture!!
                            viewModel.updateLecture(state.subjectId, state.lecture.id, title, videoLink, sheetLink)
                        },
                        onPlayPreviousLecture = { viewModel.playPreviousLecture() },
                        onPlayNextLecture = { viewModel.playNextLecture() },
                        onUpdateDownloadedVideo = { path ->
                            val state = activePlayingLecture!!
                            viewModel.updateDownloadedVideo(state.subjectId, state.lecture.id, path)
                        },
                        onSaveDownloadedVideoFor = { subjectId, lectureId, path ->
                            viewModel.updateDownloadedVideo(subjectId, lectureId, path)
                        },
                        onDeleteDownloadedVideo = {
                            val state = activePlayingLecture!!
                            viewModel.removeDownloadedVideo(state.subjectId, state.lecture.id)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                // Screen 2: Subject Detail View
                activeSubject != null -> {
                    SubjectDetailScreen(
                        subject = activeSubject,
                        searchQuery = searchQuery,
                        lectureFilter = lectureFilter,
                        isAdminUnlocked = isAdminUnlocked,
                        onBack = { viewModel.backToHome() },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onLectureFilterChange = { viewModel.setLectureFilter(it) },
                        onSelectPaper = { viewModel.selectPaper(activeSubject.id, it) },
                        onSelectChapter = { viewModel.selectChapter(activeSubject.id, it) },
                        onPlayLecture = { subId, subName, chapName, lecture ->
                            viewModel.playLecture(subId, subName, chapName, lecture)
                        },
                        onToggleFavorite = { subId, lecId ->
                            viewModel.toggleFavorite(subId, lecId)
                        },
                        onToggleCompleted = { subId, lecId ->
                            viewModel.toggleCompleted(subId, lecId)
                        },
                        onVerifyAdminPassword = { viewModel.verifyAdminPassword(it) },
                        onBulkMapVideoLinks = { subId, rawText ->
                            viewModel.bulkMapVideoLinks(subId, rawText)
                        },
                        onBulkMapSheetLinks = { subId, rawText ->
                            viewModel.bulkMapSheetLinks(subId, rawText)
                        },
                        onMapLectureTitles = { subId, rawText ->
                            viewModel.mapLectureTitles(subId, rawText)
                        },
                        onUpdateLectureTitle = { subId, lecId, newTitle ->
                            viewModel.updateLectureTitle(subId, lecId, newTitle)
                        },
                        onAddNewChapter = { subId, name ->
                            viewModel.addNewChapter(subId, name)
                        },
                        onRenameChapter = { subId, newName ->
                            viewModel.renameChapter(subId, newName)
                        },
                        onDeleteChapter = { subId ->
                            viewModel.deleteChapter(subId)
                        },
                        onDeleteAllLectures = { subId ->
                            viewModel.deleteAllLecturesInChapter(subId)
                        },
                        onDeleteLecture = { subId, lecId ->
                            viewModel.deleteLecture(subId, lecId)
                        },
                        onSaveNotes = { subId, notes ->
                            viewModel.saveNotes(subId, notes)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

            // Screen 3: Home Dashboard (Banner, Overall Progress, Subjects Grid, Analytics Donut Chart)
            else -> {
                HomeScreen(
                    subjects = subjects,
                    onSubjectClick = { viewModel.openSubject(it) },
                    onOpenSettings = { showSettingsDialog = true },
                    onOpenBackup = { showBackupDialog = true },
                    onOpenDetailedAnalytics = { showDetailedAnalytics = true },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // Settings Dialog (Favorites list + Dark/Light theme toggle)
    if (showSettingsDialog) {
        val favorites = remember(subjects) { viewModel.getAllFavorites() }
        SettingsDialog(
            favorites = favorites,
            currentTheme = themeMode,
            onThemeSelected = { viewModel.setThemeMode(it) },
            onPlayFavorite = { fav ->
                showSettingsDialog = false
                viewModel.playLecture(fav.subjectId, fav.subjectName, fav.chapterName, fav.lecture)
            },
            onRemoveFavorite = { subId, lecId ->
                viewModel.toggleFavorite(subId, lecId)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Backup & Sync Dialog
    if (showBackupDialog) {
        var totalLectures = 0
        var completedLectures = 0
        for (sub in subjects) {
            for (paper in sub.papers) {
                for (chapter in paper.chapters) {
                    totalLectures += chapter.lectures.size
                    completedLectures += chapter.lectures.count { it.completed }
                }
            }
        }

        BackupDialog(
            totalSubjects = subjects.size,
            completedLectures = completedLectures,
            onExportJson = { viewModel.exportDataJson() },
            onImportJson = { viewModel.importDataJson(it) },
            onResetDefaults = { viewModel.resetData() },
            onDismiss = { showBackupDialog = false }
        )
    }

    // Splash / Flash Screen Branding Overlay matching Screenshot_20261026-175942.jpg & file_00000000f1888211adc68367a842b092.png
    androidx.compose.animation.AnimatedVisibility(
        visible = showSplashScreen,
        exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0xFF0F172A)),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(130.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
                androidx.compose.material3.Text(
                    text = "স্টাডি ট্র্যাকার",
                    fontSize = 24.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    color = androidx.compose.ui.graphics.Color.White
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.Text(
                    text = "এইচএসসি কোর্স লেকচার ও স্মার্ট ট্র্যাকার",
                    fontSize = 13.sp,
                    color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
                )
            }
        }
    }
}
}

@Composable
fun HasibBroWatermark(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 24.dp)
    ) {
        val w = size.width
        val h = size.height

        val path = android.graphics.Path().apply {
            moveTo(w * 0.08f, h * 0.85f)
            quadTo(w * 0.5f, h * 0.15f, w * 0.92f, h * 0.85f)
        }

        drawContext.canvas.nativeCanvas.apply {
            val strokePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 34.sp.toPx()
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 3.dp.toPx()
                color = android.graphics.Color.argb(220, 255, 255, 255)
                textAlign = android.graphics.Paint.Align.CENTER
                letterSpacing = 0.18f
            }

            val fillPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 34.sp.toPx()
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                style = android.graphics.Paint.Style.FILL
                color = android.graphics.Color.argb(70, 255, 255, 255)
                textAlign = android.graphics.Paint.Align.CENTER
                letterSpacing = 0.18f
            }

            drawTextOnPath("HASIB_BRO", path, 0f, 0f, fillPaint)
            drawTextOnPath("HASIB_BRO", path, 0f, 0f, strokePaint)
        }
    }
}
