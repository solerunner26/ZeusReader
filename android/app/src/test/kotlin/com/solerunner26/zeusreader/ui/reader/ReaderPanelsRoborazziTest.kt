package com.solerunner26.zeusreader.ui.reader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.solerunner26.zeusreader.data.local.HighlightEntity
import com.solerunner26.zeusreader.reader.prefs.ReaderSettings
import com.solerunner26.zeusreader.reader.prefs.ReaderThemeMode
import com.solerunner26.zeusreader.ui.theme.ZeusReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-420dpi")
class ReaderPanelsRoborazziTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureTypographyPanel_light() {
        composeTestRule.setContent {
            ZeusReaderTheme(darkTheme = false) {
                TypographyPanelContent(
                    settings = ReaderSettings(theme = ReaderThemeMode.SEPIA, fontSizePercent = 120),
                    isPdfMode = true,
                    onSettingsChange = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("typography_panel_root")
            .captureRoboImage("reader_typography_panel_light.png")
    }

    @Test
    fun captureTypographyPanel_pureBlackOled() {
        composeTestRule.setContent {
            ZeusReaderTheme(darkTheme = true, pureBlackOled = true) {
                TypographyPanelContent(
                    settings = ReaderSettings(theme = ReaderThemeMode.PURE_BLACK_OLED, warmLightStrength = 0.2f),
                    isPdfMode = false,
                    onSettingsChange = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("typography_panel_root")
            .captureRoboImage("reader_typography_panel_oled.png")
    }

    @Test
    fun captureHighlightsSheet_populated() {
        composeTestRule.setContent {
            ZeusReaderTheme(darkTheme = false) {
                HighlightsSheetContent(
                    bookTitle = "Soil Physics",
                    highlights = listOf(
                        HighlightEntity(
                            id = "h1",
                            editionId = "e1",
                            selectedText = "Capillary action draws water upward through narrow soil pores against gravity.",
                            note = "Relate to root uptake",
                            chapterTitle = "Chapter 4",
                            pageNumber = 42,
                            createdAtEpochMs = 1_000L,
                            updatedAtEpochMs = 1_000L
                        )
                    ),
                    onOpenAddDialog = {},
                    onDeleteHighlight = {},
                    onExportMarkdown = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("highlights_sheet_root")
            .captureRoboImage("reader_highlights_sheet.png")
    }
}
