package com.solerunner26.zeusreader.ui.library

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.ui.theme.ZeusReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-420dpi")
class LibraryScreenRoborazziTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleBooks = listOf(
        LibraryBookSummary(
            workId = "w1",
            editionId = "e1",
            title = "Capillary Transport in Porous Media",
            authors = "J. Bear",
            language = "en",
            publicationYear = 2018,
            primaryFormat = "PDF",
            totalByteSize = 4_820_000L,
            assetCount = 1,
            imagePageCount = 0,
            primaryLocalPath = "/tmp/sample.pdf",
            lastPageIndex = 12,
            updatedAtEpochMs = 1_700_000_000_000L
        ),
        LibraryBookSummary(
            workId = "w2",
            editionId = "e2",
            title = "Botany Lab Plates",
            authors = "",
            language = "",
            publicationYear = null,
            primaryFormat = "IMAGE_BOOK",
            totalByteSize = 2_450_000L,
            assetCount = 5,
            imagePageCount = 5,
            primaryLocalPath = "/tmp/page1.jpg",
            lastPageIndex = 0,
            updatedAtEpochMs = 1_699_999_000_000L
        )
    )

    @Test
    fun captureEmptyLibrary_light() {
        composeTestRule.setContent {
            ZeusReaderTheme(darkTheme = false) {
                LibraryScreenContent(
                    uiState = LibraryUiState.Empty,
                    isImporting = false,
                    snackbarHostState = SnackbarHostState(),
                    onImportClick = {},
                    onOpenWatchedFolders = {},
                    onSearchQueryChange = {},
                    onFilterSelected = {},
                    onOpenBook = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("library_screen_root")
            .captureRoboImage("library_empty_light.png")
    }

    @Test
    fun capturePopulatedLibrary_light() {
        composeTestRule.setContent {
            ZeusReaderTheme(darkTheme = false) {
                LibraryScreenContent(
                    uiState = LibraryUiState.Populated(books = sampleBooks),
                    isImporting = false,
                    snackbarHostState = SnackbarHostState(),
                    onImportClick = {},
                    onOpenWatchedFolders = {},
                    onSearchQueryChange = {},
                    onFilterSelected = {},
                    onOpenBook = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("library_screen_root")
            .captureRoboImage("library_populated_light.png")
    }

    @Test
    fun capturePopulatedLibrary_pureBlackOledDark() {
        composeTestRule.setContent {
            ZeusReaderTheme(darkTheme = true, pureBlackOled = true) {
                LibraryScreenContent(
                    uiState = LibraryUiState.Populated(books = sampleBooks),
                    isImporting = false,
                    snackbarHostState = SnackbarHostState(),
                    onImportClick = {},
                    onOpenWatchedFolders = {},
                    onSearchQueryChange = {},
                    onFilterSelected = {},
                    onOpenBook = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("library_screen_root")
            .captureRoboImage("library_populated_oled_dark.png")
    }

    @Test
    @Config(sdk = [35], qualifiers = "w412dp-h915dp-420dpi", fontScale = 2.0f)
    fun capturePopulatedLibrary_fontScale200() {
        composeTestRule.setContent {
            ZeusReaderTheme(darkTheme = false) {
                LibraryScreenContent(
                    uiState = LibraryUiState.Populated(books = sampleBooks),
                    isImporting = false,
                    snackbarHostState = SnackbarHostState(),
                    onImportClick = {},
                    onOpenWatchedFolders = {},
                    onSearchQueryChange = {},
                    onFilterSelected = {},
                    onOpenBook = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("library_screen_root")
            .captureRoboImage("library_populated_fontscale_200.png")
    }
}
