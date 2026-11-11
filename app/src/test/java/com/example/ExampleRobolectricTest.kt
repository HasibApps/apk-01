package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.StudyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Study Tracker", appName)
  }

  @Test
  fun `test bengali digits conversion`() {
    assertEquals("১০", StudyUtils.toBengaliDigits(10))
    assertEquals("১", StudyUtils.toBengaliDigits(1))
    assertEquals("০", StudyUtils.toBengaliDigits(0))
    assertEquals("৯৯", StudyUtils.toBengaliDigits(99))
  }

  @Test
  fun `test youtube video id extraction`() {
    val url1 = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    val id1 = StudyUtils.extractYouTubeVideoId(url1)
    assertEquals("dQw4w9WgXcQ", id1)

    val url2 = "https://youtu.be/dQw4w9WgXcQ"
    val id2 = StudyUtils.extractYouTubeVideoId(url2)
    assertEquals("dQw4w9WgXcQ", id2)

    val url3 = "https://youtu.be/bcuiTJuTjxw?"
    val id3 = StudyUtils.extractYouTubeVideoId(url3)
    assertEquals("bcuiTJuTjxw", id3)

    val url4 = "https://youtu.be/bcuiTJuTjxw?si=12345"
    val id4 = StudyUtils.extractYouTubeVideoId(url4)
    assertEquals("bcuiTJuTjxw", id4)
  }

  @Test
  fun `test default subjects list`() {
    val defaults = StudyUtils.getDefaultSubjects()
    assertTrue(defaults.isNotEmpty())
    assertEquals("বাংলা", defaults[0].name)
  }

  @Test
  fun `test drive preview url formatting`() {
    val driveUrl = "https://drive.google.com/file/d/1OaN8eP5hllcRx7ZqEICDCk_3Qm1GKv19/view?usp=sharing"
    val previewUrl = StudyUtils.formatDrivePreviewUrl(driveUrl)
    assertEquals("https://drive.google.com/file/d/1OaN8eP5hllcRx7ZqEICDCk_3Qm1GKv19/preview", previewUrl)
  }

  @Test
  fun `test local video validity check`() {
    val nonExistent = com.example.util.VideoDownloader.isLocalVideoValid("/path/to/nonexistent.mp4")
    org.junit.Assert.assertFalse(nonExistent)

    val emptyPath = com.example.util.VideoDownloader.isLocalVideoValid("")
    org.junit.Assert.assertFalse(emptyPath)

    // Verify HTML error pages disguised as MP4 are rejected
    val tempHtmlFile = java.io.File.createTempFile("fake_video", ".mp4")
    tempHtmlFile.writeText("<!DOCTYPE html><html><head><title>404 Not Found</title></head><body>Error</body></html>" + "A".repeat(30000))
    val isHtmlValid = com.example.util.VideoDownloader.isLocalVideoValid(tempHtmlFile.absolutePath)
    org.junit.Assert.assertFalse(isHtmlValid)
    tempHtmlFile.delete()
  }

  @Test
  fun `test pdf downloader direct drive url conversion`() {
    val driveUrl = "https://drive.google.com/file/d/1Bxyz987AbCdEfGhIjkLmNoPqRsTuVwXy/view?usp=sharing"
    val directDownloadUrl = com.example.util.PdfDownloader.resolveDirectDownloadUrl(driveUrl)
    assertEquals(
      "https://drive.usercontent.google.com/download?id=1Bxyz987AbCdEfGhIjkLmNoPqRsTuVwXy&export=download&confirm=t",
      directDownloadUrl
    )
  }

  @Test
  fun `test lecture deletion persistence in study repository`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.StudyRepository(context)
    val subjects = repository.loadSubjects()
    val sub = subjects.first()
    val initialLecturesCount = sub.papers.first().chapters.first().lectures.size
    assertTrue("Initial lectures should be at least 1", initialLecturesCount > 0)

    val lectureToDeleteId = sub.papers.first().chapters.first().lectures.first().id
    val updatedChapters = sub.papers.first().chapters.mapIndexed { idx, ch ->
      if (idx == 0) ch.copy(lectures = ch.lectures.filter { it.id != lectureToDeleteId }) else ch
    }
    val updatedPapers = sub.papers.mapIndexed { idx, p ->
      if (idx == 0) p.copy(chapters = updatedChapters) else p
    }
    val updatedSubjects = subjects.mapIndexed { idx, s ->
      if (idx == 0) s.copy(papers = updatedPapers) else s
    }
    repository.saveSubjects(updatedSubjects)

    val reloaded = repository.loadSubjects()
    val reloadedCount = reloaded.first().papers.first().chapters.first().lectures.size
    assertEquals(initialLecturesCount - 1, reloadedCount)
    assertTrue(reloaded.first().papers.first().chapters.first().lectures.none { it.id == lectureToDeleteId })
  }

  @Test
  fun `test default lectures per chapter is exactly 2`() {
    val defaults = StudyUtils.getDefaultSubjects()
    for (subject in defaults) {
      for (paper in subject.papers) {
        for (chapter in paper.chapters) {
          assertEquals("Chapter ${chapter.name} in ${subject.name} must have 2 default lectures", 2, chapter.lectures.size)
          assertEquals("লেকচার ১", chapter.lectures[0].title)
          assertEquals("লেকচার ২", chapter.lectures[1].title)
        }
      }
    }
  }

  @Test
  fun `test backup json export and import`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.StudyRepository(context)
    val subjects = repository.loadSubjects()
    val json = repository.getExportJson(subjects)
    assertNotNull(json)
    assertTrue(json.contains("বাংলা"))

    val imported = repository.importFromJson(json)
    assertNotNull(imported)
    assertEquals(subjects.size, imported!!.size)
  }

  @Test
  fun `test concatenated urls extraction without spacing`() {
    val concatenated = "https://youtu.be/472jq96HsbQhttps://youtu.be/PUVg0Agxg4Uhttps://youtu.be/labZeLm87LUhttps://youtu.be/QNyC0kYNOQk"
    val extracted = StudyUtils.extractUrls(concatenated)
    assertEquals(4, extracted.size)
    assertEquals("https://youtu.be/472jq96HsbQ", extracted[0])
    assertEquals("https://youtu.be/PUVg0Agxg4U", extracted[1])
    assertEquals("https://youtu.be/labZeLm87LU", extracted[2])
    assertEquals("https://youtu.be/QNyC0kYNOQk", extracted[3])
  }

  @Test
  fun `test delete all lectures in chapter via viewmodel`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.StudyViewModel(app)
    val sub = viewModel.subjects.value.first()

    // Ensure there are lectures initially
    val initialCount = viewModel.subjects.value.first().papers.first().chapters.first().lectures.size
    assertTrue(initialCount > 0)

    // Delete all lectures
    viewModel.deleteAllLecturesInChapter(sub.id)
    val afterDelete = viewModel.subjects.value.first().papers.first().chapters.first().lectures
    assertEquals(0, afterDelete.size)
  }

  @Test
  fun `test update lecture title via viewmodel`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.StudyViewModel(app)
    val sub = viewModel.subjects.value.first()
    val lecId = sub.papers.first().chapters.first().lectures.first().id

    viewModel.updateLectureTitle(sub.id, lecId, "লেকচার ১ (মাস্টার ক্লাস)")
    val updated = viewModel.subjects.value.first().papers.first().chapters.first().lectures.first()
    assertEquals("লেকচার ১ (মাস্টার ক্লাস)", updated.title)
  }

  @Test
  fun `test map lecture titles maps lectureTag`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.StudyViewModel(app)
    val sub = viewModel.subjects.value.first()

    val tagsRaw = """
      Lecture:01
      Lecture:02
      Lecture:02.2
      Lecture:03
      Lecture:04
      Lecture:04.2
    """.trimIndent()

    viewModel.mapLectureTitles(sub.id, tagsRaw)
    val lectures = viewModel.subjects.value.first().papers.first().chapters.first().lectures
    assertEquals(6, lectures.size)
    assertEquals("Lecture:01", lectures[0].lectureTag)
    assertEquals("Lecture:02", lectures[1].lectureTag)
    assertEquals("Lecture:02.2", lectures[2].lectureTag)
    assertEquals("Lecture:03", lectures[3].lectureTag)
    assertEquals("Lecture:04", lectures[4].lectureTag)
    assertEquals("Lecture:04.2", lectures[5].lectureTag)
  }

  @Test
  fun `test parse sheet entries with NA and urls`() {
    val raw = """
      https://drive.google.com/file/d/test1/view?usp=sharing
      N/A
      https://drive.google.com/file/d/test2/view?usp=sharing
      N/A
    """.trimIndent()

    val entries = StudyUtils.parseSheetEntries(raw)
    assertEquals(4, entries.size)
    assertEquals("https://drive.google.com/file/d/test1/view?usp=sharing", entries[0])
    assertEquals("N/A", entries[1])
    assertEquals("https://drive.google.com/file/d/test2/view?usp=sharing", entries[2])
    assertEquals("N/A", entries[3])

    val formatted = StudyUtils.formatSheetMappingText("https://drive.google.com/test1N/Ahttps://drive.google.com/test2")
    val parsedConcatenated = StudyUtils.parseSheetEntries(formatted)
    assertEquals(3, parsedConcatenated.size)
    assertEquals("N/A", parsedConcatenated[1])
  }

  @Test
  fun `test bulk map sheet links with NA saves correctly`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.StudyViewModel(app)
    val sub = viewModel.subjects.value.first()

    val sheetData = "https://drive.google.com/file/d/test1\nN/A\nhttps://drive.google.com/file/d/test3"
    viewModel.bulkMapSheetLinks(sub.id, sheetData)

    val lectures = viewModel.subjects.value.first().papers.first().chapters.first().lectures
    assertEquals(3, lectures.size)
    assertEquals("https://drive.google.com/file/d/test1", lectures[0].sheetLink)
    assertEquals("N/A", lectures[1].sheetLink)
    assertEquals("https://drive.google.com/file/d/test3", lectures[2].sheetLink)
  }

  @Test
  fun `test admin password is hasib99`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.StudyViewModel(app)

    org.junit.Assert.assertFalse(viewModel.verifyAdminPassword("wrongpass"))
    org.junit.Assert.assertTrue(viewModel.verifyAdminPassword("hasib99"))
  }

  @Test
  fun `test thirteen papers analytics configuration and dynamic count`() {
    val defaults = StudyUtils.getDefaultSubjects()
    val config = com.example.ui.screens.THIRTEEN_PAPERS_CONFIG
    assertEquals(13, config.size)

    for (item in config) {
      val sub = defaults.find { it.id == item.subjectId }
      assertNotNull("Subject ${item.subjectId} for ${item.displayName} should exist", sub)
      val paper = sub!!.papers.getOrNull(item.paperIndex)
      assertNotNull("Paper ${item.paperIndex} for ${item.displayName} should exist", paper)
      assertTrue("Chapters in ${item.displayName} should not be empty", paper!!.chapters.isNotEmpty())
    }
  }
}
