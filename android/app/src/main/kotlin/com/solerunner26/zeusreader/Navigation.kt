package com.solerunner26.zeusreader

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.solerunner26.zeusreader.ui.library.LibraryScreen
import com.solerunner26.zeusreader.ui.library.LibraryViewModel
import com.solerunner26.zeusreader.ui.reader.ReaderActivity
import com.solerunner26.zeusreader.ui.settings.SettingsScreen
import com.solerunner26.zeusreader.ui.theme.AppUiConfigViewModel
import com.solerunner26.zeusreader.ui.vocab.PrepareOfflineScreen
import com.solerunner26.zeusreader.ui.vocab.VocabularyScreen
import com.solerunner26.zeusreader.ui.vocab.VocabularyViewModel

@Composable
fun MainNavigation(
    sharedImportUris: List<Uri> = emptyList(),
    onSharedImportsHandled: () -> Unit = {}
) {
    val backStack = rememberNavBackStack(LibraryRoute)
    val context = LocalContext.current

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<LibraryRoute> {
                val viewModel: LibraryViewModel = hiltViewModel()
                LaunchedEffect(sharedImportUris) {
                    if (sharedImportUris.isNotEmpty()) {
                        viewModel.importUris(sharedImportUris, origin = "share_intent")
                        onSharedImportsHandled()
                    }
                }
                LibraryScreen(
                    viewModel = viewModel,
                    onOpenBook = { book ->
                        val intent = ReaderActivity.createIntent(
                            context = context,
                            editionId = book.editionId,
                            title = book.title,
                            format = if (book.isImageBook) "IMAGE_BOOK" else book.primaryFormat,
                            localPath = book.primaryLocalPath
                        )
                        context.startActivity(intent)
                    },
                    onOpenVocabulary = { backStack.add(VocabularyRoute) },
                    onOpenPrepareOffline = { backStack.add(PrepareOfflineRoute) },
                    onOpenSettings = { backStack.add(SettingsRoute) }
                )
            }

            entry<SettingsRoute> {
                val viewModel: AppUiConfigViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            entry<VocabularyRoute> {
                val viewModel: VocabularyViewModel = hiltViewModel()
                VocabularyScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            entry<PrepareOfflineRoute> {
                val viewModel: VocabularyViewModel = hiltViewModel()
                PrepareOfflineScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
