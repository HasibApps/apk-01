package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FavoriteItem
import com.example.data.StudyRepository
import com.example.model.Chapter
import com.example.model.Lecture
import com.example.model.StudyUtils
import com.example.model.Subject
import com.example.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LectureFilter {
    ALL,
    PENDING,
    COMPLETED
}

data class PlayingLectureState(
    val subjectId: String,
    val subjectName: String,
    val chapterName: String,
    val lecture: Lecture
)

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StudyRepository(application.applicationContext)

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _activeSubjectId = MutableStateFlow<String?>(null)
    val activeSubjectId: StateFlow<String?> = _activeSubjectId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _lectureFilter = MutableStateFlow(LectureFilter.ALL)
    val lectureFilter: StateFlow<LectureFilter> = _lectureFilter.asStateFlow()

    private val _activePlayingLecture = MutableStateFlow<PlayingLectureState?>(null)
    val activePlayingLecture: StateFlow<PlayingLectureState?> = _activePlayingLecture.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _subjects.value = repository.loadSubjects()
        _themeMode.value = repository.getThemeMode()
        autoFetchAllYouTubeTitles()
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastEvent.emit(message)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        repository.setThemeMode(mode)
    }

    fun openSubject(subjectId: String) {
        _activeSubjectId.value = subjectId
        _searchQuery.value = ""
        _lectureFilter.value = LectureFilter.ALL
        autoFetchAllYouTubeTitles()
    }

    fun backToHome() {
        _activeSubjectId.value = null
        _searchQuery.value = ""
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLectureFilter(filter: LectureFilter) {
        _lectureFilter.value = filter
    }

    fun playLecture(subjectId: String, subjectName: String, chapterName: String, lecture: Lecture) {
        _activePlayingLecture.value = PlayingLectureState(
            subjectId = subjectId,
            subjectName = subjectName,
            chapterName = chapterName,
            lecture = lecture
        )
    }

    fun closePlayer() {
        _activePlayingLecture.value = null
    }

    fun playPreviousLecture() {
        val playing = _activePlayingLecture.value ?: return
        val currentList = _subjects.value
        val sub = currentList.firstOrNull { it.id == playing.subjectId } ?: return
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]
        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return
        val chapter = paper.chapters[cIdx]

        val currentIdx = chapter.lectures.indexOfFirst { it.id == playing.lecture.id }
        if (currentIdx > 0) {
            val prevLecture = chapter.lectures[currentIdx - 1]
            playLecture(sub.id, sub.name, chapter.name, prevLecture)
        } else {
            showToast("এটিই এই অধ্যায়ের প্রথম ক্লাস")
        }
    }

    fun playNextLecture() {
        val playing = _activePlayingLecture.value ?: return
        val currentList = _subjects.value
        val sub = currentList.firstOrNull { it.id == playing.subjectId } ?: return
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]
        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return
        val chapter = paper.chapters[cIdx]

        val currentIdx = chapter.lectures.indexOfFirst { it.id == playing.lecture.id }
        if (currentIdx != -1 && currentIdx + 1 < chapter.lectures.size) {
            val nextLecture = chapter.lectures[currentIdx + 1]
            playLecture(sub.id, sub.name, chapter.name, nextLecture)
        } else {
            showToast("এটিই এই অধ্যায়ের শেষ ক্লাস")
        }
    }

    fun updateDownloadedVideo(subjectId: String, lectureId: Int, localPath: String) {
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]
        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return
        val chapter = paper.chapters[cIdx]

        val updatedLectures = chapter.lectures.map { lec ->
            if (lec.id == lectureId) {
                lec.copy(localVideoPath = localPath)
            } else {
                lec
            }
        }

        val updatedChapter = chapter.copy(lectures = updatedLectures)
        val updatedChapters = paper.chapters.toMutableList().apply { set(cIdx, updatedChapter) }
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSub = sub.copy(papers = updatedPapers)

        currentList[sIdx] = updatedSub
        _subjects.value = currentList
        repository.saveSubjects(currentList)

        val playing = _activePlayingLecture.value
        if (playing != null && playing.subjectId == subjectId && playing.lecture.id == lectureId) {
            _activePlayingLecture.value = playing.copy(
                lecture = playing.lecture.copy(localVideoPath = localPath)
            )
        }
        showToast("ভিডিও সফলভাবে সংরক্ষিত হয়েছে! অফলাইনে দেখতে পারবেন")
    }

    fun removeDownloadedVideo(subjectId: String, lectureId: Int) {
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]
        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return
        val chapter = paper.chapters[cIdx]

        val updatedLectures = chapter.lectures.map { lec ->
            if (lec.id == lectureId) {
                lec.copy(localVideoPath = "")
            } else {
                lec
            }
        }

        val updatedChapter = chapter.copy(lectures = updatedLectures)
        val updatedChapters = paper.chapters.toMutableList().apply { set(cIdx, updatedChapter) }
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSub = sub.copy(papers = updatedPapers)

        currentList[sIdx] = updatedSub
        _subjects.value = currentList
        repository.saveSubjects(currentList)

        val playing = _activePlayingLecture.value
        if (playing != null && playing.subjectId == subjectId && playing.lecture.id == lectureId) {
            _activePlayingLecture.value = playing.copy(
                lecture = playing.lecture.copy(localVideoPath = "")
            )
        }
        showToast("ডাউনলোড করা ফাইলটি মুছে ফেলা হয়েছে")
    }

    fun verifyAdminPassword(password: String): Boolean {
        return if (password.trim() == "hasib99") {
            _isAdminUnlocked.value = true
            showToast("পাসওয়ার্ড সঠিক! ফিচারটি উন্মুক্ত করা হলো")
            true
        } else {
            false
        }
    }

    fun toggleFavorite(subjectId: String, lectureId: Int) {
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return

        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]

        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return
        val chapter = paper.chapters[cIdx]

        var newFavState = false
        val updatedLectures = chapter.lectures.map { lec ->
            if (lec.id == lectureId) {
                newFavState = !lec.isFavorite
                lec.copy(isFavorite = newFavState)
            } else {
                lec
            }
        }

        val updatedChapter = chapter.copy(lectures = updatedLectures)
        val updatedChapters = paper.chapters.toMutableList().apply { set(cIdx, updatedChapter) }
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSubject = sub.copy(papers = updatedPapers)

        currentList[sIdx] = updatedSubject
        _subjects.value = currentList
        repository.saveSubjects(currentList)

        // Also update player state if currently playing
        val playing = _activePlayingLecture.value
        if (playing != null && playing.subjectId == subjectId && playing.lecture.id == lectureId) {
            _activePlayingLecture.value = playing.copy(lecture = playing.lecture.copy(isFavorite = newFavState))
        }

        showToast(if (newFavState) "ফেভারিটে যোগ করা হলো ❤️" else "ফেভারিট থেকে সরানো হলো")
    }

    fun toggleCompleted(subjectId: String, lectureId: Int) {
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return

        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]

        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return
        val chapter = paper.chapters[cIdx]

        var newCompletedState = false
        val updatedLectures = chapter.lectures.map { lec ->
            if (lec.id == lectureId) {
                newCompletedState = !lec.completed
                lec.copy(completed = newCompletedState)
            } else {
                lec
            }
        }

        val updatedChapter = chapter.copy(lectures = updatedLectures)
        val updatedChapters = paper.chapters.toMutableList().apply { set(cIdx, updatedChapter) }
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSubject = sub.copy(papers = updatedPapers)

        currentList[sIdx] = updatedSubject
        _subjects.value = currentList
        repository.saveSubjects(currentList)

        // Also update active player state if open
        val playing = _activePlayingLecture.value
        if (playing != null && playing.subjectId == subjectId && playing.lecture.id == lectureId) {
            _activePlayingLecture.value = playing.copy(lecture = playing.lecture.copy(completed = newCompletedState))
        }

        showToast(
            if (newCompletedState) "লেকচার ${StudyUtils.toBengaliDigits(lectureId)} সম্পন্ন হয়েছে! ✓"
            else "লেকচার ${StudyUtils.toBengaliDigits(lectureId)} বাকি চিহ্নিত করা হলো"
        )
    }

    fun selectPaper(subjectId: String, paperIndex: Int) {
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        currentList[sIdx] = sub.copy(activePaperIndex = paperIndex, activeChapterIndex = 0)
        _subjects.value = currentList
        repository.saveSubjects(currentList)
    }

    fun selectChapter(subjectId: String, chapterIndex: Int) {
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        currentList[sIdx] = sub.copy(activeChapterIndex = chapterIndex)
        _subjects.value = currentList
        repository.saveSubjects(currentList)
    }

    fun addNewChapter(subjectId: String, name: String) {
        if (name.isBlank()) return
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]

        val newChapter = Chapter(
            name = name.trim(),
            lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
        )
        val updatedChapters = paper.chapters + newChapter
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSub = sub.copy(papers = updatedPapers, activeChapterIndex = updatedChapters.lastIndex)

        currentList[sIdx] = updatedSub
        _subjects.value = currentList
        repository.saveSubjects(currentList)
        showToast("নতুন অধ্যায় সফলভাবে যোগ করা হয়েছে!")
    }

    fun renameChapter(subjectId: String, newName: String) {
        if (newName.isBlank()) return
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]
        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return

        val chapter = paper.chapters[cIdx]
        val updatedChapter = chapter.copy(name = newName.trim())
        val updatedChapters = paper.chapters.toMutableList().apply { set(cIdx, updatedChapter) }
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSub = sub.copy(papers = updatedPapers)

        currentList[sIdx] = updatedSub
        _subjects.value = currentList
        repository.saveSubjects(currentList)
        showToast("অধ্যায়ের নাম পরিবর্তন করা হয়েছে")
    }

    fun deleteChapter(subjectId: String) {
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]
        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return

        val updatedChapters = paper.chapters.toMutableList().apply { removeAt(cIdx) }
        val newIndex = (cIdx - 1).coerceAtLeast(0)
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSub = sub.copy(papers = updatedPapers, activeChapterIndex = newIndex)

        currentList[sIdx] = updatedSub
        _subjects.value = currentList
        repository.saveSubjects(currentList)
        showToast("অধ্যায়টি মুছে ফেলা হয়েছে")
    }

    fun bulkMapVideoLinks(subjectId: String, rawText: String) {
        val rawLinks = StudyUtils.extractUrls(rawText)
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]
        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return

        val chapter = paper.chapters[cIdx]
        val existingLectures = chapter.lectures
        val targetCount = rawLinks.size
        val newLectures = mutableListOf<Lecture>()

        for (i in 1..targetCount) {
            val existing = existingLectures.firstOrNull { it.id == i }
            val link = rawLinks[i - 1]
            newLectures.add(
                Lecture(
                    id = i,
                    title = existing?.title ?: "লেকচার ${StudyUtils.toBengaliDigits(i)}",
                    link = link,
                    sheetLink = existing?.sheetLink ?: "",
                    localVideoPath = existing?.localVideoPath ?: "",
                    completed = existing?.completed ?: false,
                    isFavorite = existing?.isFavorite ?: false,
                    lectureTag = existing?.lectureTag ?: ""
                )
            )
        }

        val updatedChapter = chapter.copy(lectures = newLectures)
        val updatedChapters = paper.chapters.toMutableList().apply { set(cIdx, updatedChapter) }
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSub = sub.copy(papers = updatedPapers)

        currentList[sIdx] = updatedSub
        _subjects.value = currentList
        repository.saveSubjects(currentList)
        showToast("ভিডিও লিঙ্কসমূহ সফলভাবে ম্যাপ করা হয়েছে (${StudyUtils.toBengaliDigits(rawLinks.size)}টি লিঙ্ক)")

        // Auto-fetch real YouTube titles in background for this chapter (Requirement 2)
        viewModelScope.launch(Dispatchers.IO) {
            fetchAndApplyYouTubeTitlesForChapter(subjectId, cIdx, pIdx)
        }
    }

    fun bulkMapSheetLinks(subjectId: String, rawText: String) {
        val sheetEntries = StudyUtils.parseSheetEntries(rawText)
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]
        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return

        val chapter = paper.chapters[cIdx]
        val existingLectures = chapter.lectures
        val count = maxOf(existingLectures.size, sheetEntries.size)
        val newLectures = mutableListOf<Lecture>()

        for (i in 1..count) {
            val existing = existingLectures.firstOrNull { it.id == i }
            // If i - 1 < sheetEntries.size => set mapped sheet entry. If user edited/removed items, clear the rest!
            val sheetEntry = if (i - 1 < sheetEntries.size) sheetEntries[i - 1] else ""
            newLectures.add(
                Lecture(
                    id = i,
                    title = existing?.title ?: "লেকচার ${StudyUtils.toBengaliDigits(i)}",
                    link = existing?.link ?: "",
                    sheetLink = sheetEntry,
                    localVideoPath = existing?.localVideoPath ?: "",
                    completed = existing?.completed ?: false,
                    isFavorite = existing?.isFavorite ?: false,
                    lectureTag = existing?.lectureTag ?: ""
                )
            )
        }

        val updatedChapter = chapter.copy(lectures = newLectures)
        val updatedChapters = paper.chapters.toMutableList().apply { set(cIdx, updatedChapter) }
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSub = sub.copy(papers = updatedPapers)

        currentList[sIdx] = updatedSub
        _subjects.value = currentList
        repository.saveSubjects(currentList)
        showToast("Lecture Sheet ম্যাপিং সম্পন্ন হয়েছে (${StudyUtils.toBengaliDigits(sheetEntries.size)}টি আইটেম)")
    }

    fun mapLectureTitles(subjectId: String, rawText: String) {
        val tags = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (tags.isEmpty()) {
            showToast("কোনো টাইটেল বা ট্যাগ পাওয়া যায়নি")
            return
        }
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]
        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return

        val chapter = paper.chapters[cIdx]
        val existingLectures = chapter.lectures
        val maxCount = maxOf(existingLectures.size, tags.size)
        val newLectures = mutableListOf<Lecture>()

        for (i in 0 until maxCount) {
            val existing = existingLectures.getOrNull(i)
            val tag = if (i < tags.size) tags[i] else (existing?.lectureTag ?: "")
            val id = existing?.id ?: (i + 1)
            newLectures.add(
                Lecture(
                    id = id,
                    title = existing?.title ?: "লেকচার ${StudyUtils.toBengaliDigits(i + 1)}",
                    link = existing?.link ?: "",
                    sheetLink = existing?.sheetLink ?: "",
                    localVideoPath = existing?.localVideoPath ?: "",
                    completed = existing?.completed ?: false,
                    isFavorite = existing?.isFavorite ?: false,
                    lectureTag = tag
                )
            )
        }

        val updatedChapter = chapter.copy(lectures = newLectures)
        val updatedChapters = paper.chapters.toMutableList().apply { set(cIdx, updatedChapter) }
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSub = sub.copy(papers = updatedPapers)

        currentList[sIdx] = updatedSub
        _subjects.value = currentList
        repository.saveSubjects(currentList)
        showToast("টাইটেল ম্যাপিং সম্পন্ন হয়েছে (${StudyUtils.toBengaliDigits(tags.size)}টি ট্যাগ)")
    }

    private suspend fun fetchAndApplyYouTubeTitlesForChapter(subjectId: String, cIdx: Int, pIdx: Int) {
        val currentList = _subjects.value
        val sub = currentList.firstOrNull { it.id == subjectId } ?: return
        val paper = sub.papers.getOrNull(pIdx) ?: return
        val chapter = paper.chapters.getOrNull(cIdx) ?: return

        var hasUpdates = false
        val updatedLectures = chapter.lectures.map { lec ->
            if (lec.link.isNotBlank()) {
                val realTitle = StudyUtils.fetchYouTubeTitleSync(lec.link)
                if (!realTitle.isNullOrBlank() && realTitle != lec.title) {
                    hasUpdates = true
                    lec.copy(title = realTitle)
                } else {
                    lec
                }
            } else {
                lec
            }
        }

        if (hasUpdates) {
            withContext(Dispatchers.Main) {
                val list = _subjects.value.toMutableList()
                val sIndex = list.indexOfFirst { it.id == subjectId }
                if (sIndex != -1) {
                    val s = list[sIndex]
                    val p = s.papers.getOrNull(pIdx)
                    if (p != null) {
                        val ch = p.chapters.getOrNull(cIdx)
                        if (ch != null) {
                            val updatedCh = ch.copy(lectures = updatedLectures)
                            val updatedChs = p.chapters.toMutableList().apply { set(cIdx, updatedCh) }
                            val updatedP = p.copy(chapters = updatedChs)
                            val updatedPs = s.papers.toMutableList().apply { set(pIdx, updatedP) }
                            val updatedS = s.copy(papers = updatedPs)
                            list[sIndex] = updatedS
                            _subjects.value = list
                            repository.saveSubjects(list)
                        }
                    }
                }
            }
        }
    }

    fun autoFetchAllYouTubeTitles() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = _subjects.value
            var anyChanged = false
            val updatedSubjects = currentList.map { sub ->
                var subChanged = false
                val updatedPapers = sub.papers.map { paper ->
                    var paperChanged = false
                    val updatedChapters = paper.chapters.map { chapter ->
                        var chapterChanged = false
                        val updatedLectures = chapter.lectures.map { lec ->
                            if (lec.link.isNotBlank() && (lec.title.startsWith("লেকচার") || lec.title.startsWith("Lecture") || lec.title.isBlank())) {
                                val realTitle = StudyUtils.fetchYouTubeTitleSync(lec.link)
                                if (!realTitle.isNullOrBlank() && realTitle != lec.title) {
                                    chapterChanged = true
                                    lec.copy(title = realTitle)
                                } else {
                                    lec
                                }
                            } else {
                                lec
                            }
                        }
                        if (chapterChanged) {
                            paperChanged = true
                            chapter.copy(lectures = updatedLectures)
                        } else {
                            chapter
                        }
                    }
                    if (paperChanged) {
                        subChanged = true
                        paper.copy(chapters = updatedChapters)
                    } else {
                        paper
                    }
                }
                if (subChanged) {
                    anyChanged = true
                    sub.copy(papers = updatedPapers)
                } else {
                    sub
                }
            }

            if (anyChanged) {
                withContext(Dispatchers.Main) {
                    _subjects.value = updatedSubjects
                    repository.saveSubjects(updatedSubjects)
                }
            }
        }
    }

    fun updateLectureTitle(subjectId: String, lectureId: Int, newTitle: String) {
        if (newTitle.isBlank()) return
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]
        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return

        val chapter = paper.chapters[cIdx]
        val updatedLectures = chapter.lectures.map { lec ->
            if (lec.id == lectureId) {
                lec.copy(title = newTitle.trim())
            } else {
                lec
            }
        }

        val updatedChapter = chapter.copy(lectures = updatedLectures)
        val updatedChapters = paper.chapters.toMutableList().apply { set(cIdx, updatedChapter) }
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSub = sub.copy(papers = updatedPapers)

        currentList[sIdx] = updatedSub
        _subjects.value = currentList
        repository.saveSubjects(currentList)

        val playing = _activePlayingLecture.value
        if (playing != null && playing.subjectId == subjectId && playing.lecture.id == lectureId) {
            _activePlayingLecture.value = playing.copy(
                lecture = playing.lecture.copy(title = newTitle.trim())
            )
        }
        showToast("লেকচার টাইটেল সফলভাবে পরিবর্তিত হয়েছে")
    }

    fun deleteAllLecturesInChapter(subjectId: String) {
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]
        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return

        val chapter = paper.chapters[cIdx]
        val updatedChapter = chapter.copy(lectures = emptyList())
        val updatedChapters = paper.chapters.toMutableList().apply { set(cIdx, updatedChapter) }
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSub = sub.copy(papers = updatedPapers)

        currentList[sIdx] = updatedSub
        _subjects.value = currentList
        repository.saveSubjects(currentList)

        val playing = _activePlayingLecture.value
        if (playing != null && playing.subjectId == subjectId) {
            _activePlayingLecture.value = null
        }
        showToast("অধ্যায়ের সব লেকচার মুছে ফেলা হয়েছে")
    }

    fun updateLecture(subjectId: String, lectureId: Int, title: String, videoLink: String, sheetLink: String) {
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]
        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return

        val chapter = paper.chapters[cIdx]
        val updatedLectures = chapter.lectures.map { lec ->
            if (lec.id == lectureId) {
                lec.copy(
                    title = title.ifBlank { lec.title },
                    link = videoLink.trim(),
                    sheetLink = sheetLink.trim()
                )
            } else {
                lec
            }
        }

        val updatedChapter = chapter.copy(lectures = updatedLectures)
        val updatedChapters = paper.chapters.toMutableList().apply { set(cIdx, updatedChapter) }
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSub = sub.copy(papers = updatedPapers)

        currentList[sIdx] = updatedSub
        _subjects.value = currentList
        repository.saveSubjects(currentList)

        val playing = _activePlayingLecture.value
        if (playing != null && playing.subjectId == subjectId && playing.lecture.id == lectureId) {
            _activePlayingLecture.value = playing.copy(
                lecture = playing.lecture.copy(
                    title = title.ifBlank { playing.lecture.title },
                    link = videoLink.trim(),
                    sheetLink = sheetLink.trim()
                )
            )
        }
        showToast("লেকচার তথ্য আপডেট করা হয়েছে")

        if (videoLink.isNotBlank() && (title.isBlank() || title.startsWith("লেকচার"))) {
            viewModelScope.launch(Dispatchers.IO) {
                val realTitle = StudyUtils.fetchYouTubeTitleSync(videoLink)
                if (!realTitle.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        updateLectureTitle(subjectId, lectureId, realTitle)
                    }
                }
            }
        }
    }

    fun deleteLecture(subjectId: String, lectureId: Int) {
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        val sub = currentList[sIdx]
        val pIdx = sub.activePaperIndex
        if (pIdx !in sub.papers.indices) return
        val paper = sub.papers[pIdx]
        val cIdx = sub.activeChapterIndex
        if (cIdx !in paper.chapters.indices) return

        val chapter = paper.chapters[cIdx]
        val updatedLectures = chapter.lectures.filter { it.id != lectureId }

        val updatedChapter = chapter.copy(lectures = updatedLectures)
        val updatedChapters = paper.chapters.toMutableList().apply { set(cIdx, updatedChapter) }
        val updatedPaper = paper.copy(chapters = updatedChapters)
        val updatedPapers = sub.papers.toMutableList().apply { set(pIdx, updatedPaper) }
        val updatedSub = sub.copy(papers = updatedPapers)

        currentList[sIdx] = updatedSub
        _subjects.value = currentList
        repository.saveSubjects(currentList)

        val playing = _activePlayingLecture.value
        if (playing != null && playing.subjectId == subjectId && playing.lecture.id == lectureId) {
            _activePlayingLecture.value = null
        }
        showToast("লেকচার ${StudyUtils.toBengaliDigits(lectureId)} মুছে ফেলা হয়েছে")
    }

    fun saveNotes(subjectId: String, notes: String) {
        val currentList = _subjects.value.toMutableList()
        val sIdx = currentList.indexOfFirst { it.id == subjectId }
        if (sIdx == -1) return
        currentList[sIdx] = currentList[sIdx].copy(notes = notes)
        _subjects.value = currentList
        repository.saveSubjects(currentList)
        showToast("নোট সংরক্ষিত হয়েছে")
    }

    fun getAllFavorites(): List<FavoriteItem> {
        val list = mutableListOf<FavoriteItem>()
        for (sub in _subjects.value) {
            for (paper in sub.papers) {
                for (chapter in paper.chapters) {
                    for (lecture in chapter.lectures) {
                        if (lecture.isFavorite) {
                            list.add(
                                FavoriteItem(
                                    subjectId = sub.id,
                                    subjectName = sub.name,
                                    paperName = paper.paperName,
                                    chapterName = chapter.name,
                                    lecture = lecture
                                )
                            )
                        }
                    }
                }
            }
        }
        return list
    }

    fun exportDataJson(): String {
        return repository.getExportJson(_subjects.value)
    }

    fun importDataJson(json: String): Boolean {
        val imported = repository.importFromJson(json)
        return if (imported != null) {
            _subjects.value = imported
            showToast("ডাটা সফলভাবে ইম্পোর্ট করা হয়েছে")
            true
        } else {
            showToast("JSON ফাইল পার্স করতে ব্যর্থ হয়েছে!")
            false
        }
    }

    fun resetData() {
        val defaults = repository.resetToDefaults()
        _subjects.value = defaults
        _activeSubjectId.value = null
        showToast("ডিফল্ট ডেটা রিস্টোর হয়েছে")
    }
}
