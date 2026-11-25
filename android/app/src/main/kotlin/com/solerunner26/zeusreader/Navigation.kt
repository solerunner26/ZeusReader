package com.solerunner26.zeusreader

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.solerunner26.zeusreader.ui.library.LibraryScreen
import com.solerunner26.zeusreader.ui.library.LibraryViewModel

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(LibraryRoute)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<LibraryRoute> {
                val viewModel: LibraryViewModel = hiltViewModel()
                LibraryScreen(viewModel = viewModel)
            }
        }
    )
}
