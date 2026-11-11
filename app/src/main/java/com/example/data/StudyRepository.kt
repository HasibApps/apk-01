package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Chapter
import com.example.model.Lecture
import com.example.model.Paper
import com.example.model.StudyUtils
import com.example.model.Subject
import com.example.model.ThemeMode
import org.json.JSONArray
import org.json.JSONObject

data class FavoriteItem(
    val subjectId: String,
    val subjectName: String,
    val paperName: String,
    val chapterName: String,
    val lecture: Lecture
)

class StudyRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hsc_study_tracker_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SUBJECTS_JSON = "subjects_data_json"
        private const val KEY_THEME_MODE = "theme_mode_pref"
    }

    fun loadSubjects(): List<Subject> {
        val jsonString = prefs.getString(KEY_SUBJECTS_JSON, null)
        if (jsonString.isNullOrBlank()) {
            val defaults = StudyUtils.getDefaultSubjects()
            saveSubjects(defaults)
            return defaults
        }
        val loaded = try {
            parseSubjectsJson(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            val defaults = StudyUtils.getDefaultSubjects()
            saveSubjects(defaults)
            return defaults
        }

        // Ensure Higher Math 2nd paper and vibrant colors are up to date
        val defaultHM = StudyUtils.getDefaultSubjects().find { it.id == "sub_7" }
        val defaultHM2ndPaper = defaultHM?.papers?.getOrNull(1)
        var needsSave = false

        val updated = loaded.map { sub ->
            val defaultSub = StudyUtils.getDefaultSubjects().find { it.id == sub.id }
            var subCopy = sub
            if (defaultSub != null && (sub.colorStart != defaultSub.colorStart || sub.colorEnd != defaultSub.colorEnd)) {
                subCopy = subCopy.copy(colorStart = defaultSub.colorStart, colorEnd = defaultSub.colorEnd)
                needsSave = true
            }
            if (sub.id == "sub_7" && defaultHM2ndPaper != null) {
                val papersCopy = subCopy.papers.mapIndexed { pIdx, paper ->
                    if (pIdx == 1 || paper.paperName.contains("দ্বিতীয়") || paper.paperName.contains("2nd")) {
                        val hasLinearProgramming = paper.chapters.any { it.name.contains("যোগাশ্রয়ী") || it.name.contains("রৈখিক") }
                        if (!hasLinearProgramming) {
                            needsSave = true
                            defaultHM2ndPaper
                        } else {
                            paper
                        }
                    } else {
                        paper
                    }
                }
                subCopy = subCopy.copy(papers = papersCopy)
            }

            // Automatically upgrade chapter names to include Cy: cycle prefixes if missing
            if (defaultSub != null) {
                val papersWithCycles = subCopy.papers.mapIndexed { pIdx, paper ->
                    val defaultPaper = defaultSub.papers.getOrNull(pIdx)
                    if (defaultPaper != null && defaultPaper.chapters.size == paper.chapters.size) {
                        val newChapters = paper.chapters.mapIndexed { cIdx, ch ->
                            val defCh = defaultPaper.chapters[cIdx]
                            if (defCh.name.startsWith("Cy:", ignoreCase = true) && !ch.name.startsWith("Cy:", ignoreCase = true)) {
                                needsSave = true
                                ch.copy(name = defCh.name)
                            } else {
                                ch
                            }
                        }
                        paper.copy(chapters = newChapters)
                    } else {
                        paper
                    }
                }
                subCopy = subCopy.copy(papers = papersWithCycles)
            }
            // User requirement: default to 2 lectures per chapter (prune unused empty default template lectures 3..10)
            val prunedPapers = subCopy.papers.map { paper ->
                val prunedChapters = paper.chapters.map { ch ->
                    if (ch.lectures.size > 2) {
                        val extraAreBlank = ch.lectures.drop(2).all { lec ->
                            lec.link.isBlank() && lec.sheetLink.isBlank() &&
                                    lec.localVideoPath.isBlank() && !lec.completed && !lec.isFavorite &&
                                    (lec.title == "লেকচার ${lec.id}" || lec.title == "লেকচার ${StudyUtils.toBengaliDigits(lec.id)}" || lec.title.startsWith("লেকচার"))
                        }
                        if (extraAreBlank) {
                            needsSave = true
                            ch.copy(lectures = ch.lectures.take(2))
                        } else {
                            ch
                        }
                    } else {
                        ch
                    }
                }
                paper.copy(chapters = prunedChapters)
            }
            subCopy.copy(papers = prunedPapers)
        }

        if (needsSave) {
            saveSubjects(updated)
        }
        return updated
    }

    fun saveSubjects(subjects: List<Subject>) {
        try {
            val jsonArray = subjectsToJson(subjects)
            prefs.edit().putString(KEY_SUBJECTS_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getExportJson(subjects: List<Subject>): String {
        return subjectsToJson(subjects).toString(2)
    }

    fun importFromJson(jsonString: String): List<Subject>? {
        return try {
            val subjects = parseSubjectsJson(jsonString)
            if (subjects.isNotEmpty()) {
                saveSubjects(subjects)
                subjects
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun resetToDefaults(): List<Subject> {
        val defaults = StudyUtils.getDefaultSubjects()
        saveSubjects(defaults)
        return defaults
    }

    fun getThemeMode(): ThemeMode {
        val modeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return try {
            ThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    private fun subjectsToJson(subjects: List<Subject>): JSONArray {
        val jsonArray = JSONArray()
        for (sub in subjects) {
            val subObj = JSONObject().apply {
                put("id", sub.id)
                put("name", sub.name)
                put("icon", sub.icon)
                put("isTextIcon", sub.isTextIcon)
                put("colorStart", sub.colorStart)
                put("colorEnd", sub.colorEnd)
                put("activePaperIndex", sub.activePaperIndex)
                put("activeChapterIndex", sub.activeChapterIndex)
                put("notes", sub.notes)

                val papersArr = JSONArray()
                for (p in sub.papers) {
                    val pObj = JSONObject().apply {
                        put("paperName", p.paperName)
                        val chArr = JSONArray()
                        for (ch in p.chapters) {
                            val chObj = JSONObject().apply {
                                put("name", ch.name)
                                val lecArr = JSONArray()
                                for (lec in ch.lectures) {
                                    val lecObj = JSONObject().apply {
                                        put("id", lec.id)
                                        put("title", lec.title)
                                        put("link", lec.link)
                                        put("sheetLink", lec.sheetLink)
                                        put("localVideoPath", lec.localVideoPath)
                                        put("completed", lec.completed)
                                        put("isFavorite", lec.isFavorite)
                                        put("lectureTag", lec.lectureTag)
                                    }
                                    lecArr.put(lecObj)
                                }
                                put("lectures", lecArr)
                            }
                            chArr.put(chObj)
                        }
                        put("chapters", chArr)
                    }
                    papersArr.put(pObj)
                }
                put("papers", papersArr)
            }
            jsonArray.put(subObj)
        }
        return jsonArray
    }

    private fun parseSubjectsJson(jsonString: String): List<Subject> {
        val list = mutableListOf<Subject>()
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val subObj = jsonArray.getJSONObject(i)
            val id = subObj.optString("id", "sub_$i")
            val name = subObj.optString("name", "বিষয় $i")
            val icon = subObj.optString("icon", "fa-book")
            val isTextIcon = subObj.optBoolean("isTextIcon", false)
            val colorStart = subObj.optLong("colorStart", 0xFF059669)
            val colorEnd = subObj.optLong("colorEnd", 0xFF14B8A6)
            val activePaperIndex = subObj.optInt("activePaperIndex", 0)
            val activeChapterIndex = subObj.optInt("activeChapterIndex", 0)
            val notes = subObj.optString("notes", "")

            val papers = mutableListOf<Paper>()
            val papersArr = subObj.optJSONArray("papers")
            if (papersArr != null) {
                for (pIdx in 0 until papersArr.length()) {
                    val pObj = papersArr.getJSONObject(pIdx)
                    val paperName = pObj.optString("paperName", "পত্র ${pIdx + 1}")
                    val chapters = mutableListOf<Chapter>()
                    val chArr = pObj.optJSONArray("chapters")
                    if (chArr != null) {
                        for (cIdx in 0 until chArr.length()) {
                            val chObj = chArr.getJSONObject(cIdx)
                            val chName = chObj.optString("name", "অধ্যায় ${cIdx + 1}")
                            val lectures = mutableListOf<Lecture>()
                            val lecArr = chObj.optJSONArray("lectures")
                            if (lecArr != null) {
                                for (lIdx in 0 until lecArr.length()) {
                                    val lecObj = lecArr.getJSONObject(lIdx)
                                    lectures.add(
                                        Lecture(
                                            id = lecObj.optInt("id", lIdx + 1),
                                            title = lecObj.optString("title", "লেকচার ${lIdx + 1}"),
                                            link = lecObj.optString("link", ""),
                                            sheetLink = lecObj.optString("sheetLink", ""),
                                            localVideoPath = lecObj.optString("localVideoPath", ""),
                                            completed = lecObj.optBoolean("completed", false),
                                            isFavorite = lecObj.optBoolean("isFavorite", false),
                                            lectureTag = lecObj.optString("lectureTag", "")
                                        )
                                    )
                                }
                            }
                            chapters.add(Chapter(name = chName, lectures = lectures))
                        }
                    }
                    papers.add(Paper(paperName = paperName, chapters = chapters))
                }
            }

            list.add(
                Subject(
                    id = id,
                    name = name,
                    icon = icon,
                    isTextIcon = isTextIcon,
                    colorStart = colorStart,
                    colorEnd = colorEnd,
                    papers = papers,
                    activePaperIndex = activePaperIndex,
                    activeChapterIndex = activeChapterIndex,
                    notes = notes
                )
            )
        }
        return list
    }
}
