package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.Lecture
import com.example.ui.components.LectureCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun lecture_card_screenshot() {
    val sampleLecture = Lecture(
      id = 1,
      title = "Chapter 03 // lecture 01 // Physics",
      link = "https://youtu.be/dQw4w9WgXcQ",
      sheetLink = "https://drive.google.com/sheet1",
      completed = false,
      isFavorite = true
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        LectureCard(
          lecture = sampleLecture,
          onClick = {},
          onToggleFavorite = {},
          onToggleCompleted = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/lecture_card.png")
  }
}
