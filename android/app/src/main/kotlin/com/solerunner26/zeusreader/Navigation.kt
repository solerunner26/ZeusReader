package com.solerunner26.zeusreader

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.solerunner26.zeusreader.ui.library.LibraryScreen
import com.solerunner26.zeusreader.ui.library.LibraryViewModel
import com.solerunner26.zeusreader.ui.reader.ImageBookReaderScreen
import com.solerunner26.zeusreader.ui.reader.ImageBookReaderViewModel
import com.solerunner26.zeusreader.ui.reader.PdfReaderScreen
import com.solerunner26.zeusreader.ui.reader.PdfReaderViewModel

@Composable
fun MainNavigation(
    sharedImportUris: List<Uri> = emptyList(),
    onSharedImportsHandled: () -> Unit = {}
) {
    val backStack = rememberNavBackStack(LibraryRoute)

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
                        if (book.isImageBook) {
                            backStack.add(
                                ImageBookReaderRoute(
                                    editionId = book.editionId,
                                    title = book.title
                                )
                            )
                        } else if (!book.primaryLocalPath.isNullOrBlank()) {
                            backStack.add(
                                PdfReaderRoute(
                                    editionId = book.editionId,
                                    title = book.title,
                                    localPath = book.primaryLocalPath
                                )
                            )
                        }
                    }
                )
            }

            entry<PdfReaderRoute> { route ->
                val viewModel: PdfReaderViewModel = hiltViewModel()
                PdfReaderScreen(
                    editionId = route.editionId,
                    title = route.title,
                    localPath = route.localPath,
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() }
                )
            }

            entry<ImageBookReaderRoute> { route ->
                val viewModel: ImageBookReaderViewModel = hiltViewModel()
                ImageBookReaderScreen(
                    editionId = route.editionId,
                    title = route.title,
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
