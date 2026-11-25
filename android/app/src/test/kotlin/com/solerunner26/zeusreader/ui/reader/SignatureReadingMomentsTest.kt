package com.solerunner26.zeusreader.ui.reader

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solerunner26.zeusreader.ui.theme.ZeusReaderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SignatureReadingMomentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `ForeEdgeProgress exposes TalkBack percentage and responds to tap`() {
        var seekCalled = false
        composeRule.setContent {
            ZeusReaderTheme {
                ForeEdgeProgress(
                    progressFraction = 0.37f,
                    onSeekFraction = { seekCalled = true }
                )
            }
        }
        composeRule.onNodeWithTag("fore_edge_progress")
            .assertIsDisplayed()
            .assertContentDescriptionEquals("37 percent read")
            .performClick()
        assertTrue(seekCalled)
    }

    @Test
    fun `RibbonBookmark and ChapterBreathCard render and trigger callbacks`() {
        var toggled = false
        var nextChapterClicked = false
        composeRule.setContent {
            ZeusReaderTheme {
                androidx.compose.foundation.layout.Column {
                    RibbonBookmark(
                        isBookmarked = false,
                        chromeVisible = true,
                        onToggleBookmark = { toggled = true }
                    )
                    ChapterBreathCard(
                        chapterTitle = "Chapter 1: Foundations",
                        minutesRead = 14,
                        passagesSaved = 2,
                        wordsLearned = 5,
                        onNextChapter = { nextChapterClicked = true },
                        onDismiss = {}
                    )
                }
            }
        }
        composeRule.onNodeWithTag("ribbon_bookmark").performClick()
        assertTrue(toggled)

        composeRule.onNodeWithText("Chapter 1: Foundations").assertIsDisplayed()
        composeRule.onNodeWithText("14 min").assertIsDisplayed()
        composeRule.onNodeWithText("Next chapter").performClick()
        assertTrue(nextChapterClicked)
    }
}
