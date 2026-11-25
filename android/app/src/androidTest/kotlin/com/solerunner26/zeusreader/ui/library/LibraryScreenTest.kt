package com.solerunner26.zeusreader.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.solerunner26.zeusreader.ui.theme.ZeusReaderTheme
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyState_displaysTitleAndImportCta() {
        composeTestRule.setContent {
            ZeusReaderTheme {
                LibraryScreenContent(
                    uiState = LibraryUiState.Empty,
                    snackbarHostState = SnackbarHostState(),
                    onImportClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Your library is empty").assertIsDisplayed()
        composeTestRule.onNodeWithText("Import from device").assertIsDisplayed()
    }
}
