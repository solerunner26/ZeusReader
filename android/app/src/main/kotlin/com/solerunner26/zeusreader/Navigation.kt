package com.solerunner26.zeusreader

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.solerunner26.zeusreader.ui.library.LibraryScreen
import com.solerunner26.zeusreader.ui.library.LibraryViewModel
import com.solerunner26.zeusreader.ui.passages.CardStudioScreen
import com.solerunner26.zeusreader.ui.passages.PassageDetailScreen
import com.solerunner26.zeusreader.ui.passages.PassagesScreen
import com.solerunner26.zeusreader.ui.passages.PassagesViewModel
import com.solerunner26.zeusreader.ui.reader.ReaderActivity
import com.solerunner26.zeusreader.ui.online.OnlineAcquisitionScreen
import com.solerunner26.zeusreader.ui.online.OnlineAcquisitionViewModel
import com.solerunner26.zeusreader.ui.settings.SettingsScreen
import com.solerunner26.zeusreader.ui.stats.StatsScreen
import com.solerunner26.zeusreader.ui.stats.StatsViewModel
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
    val currentTop = backStack.lastOrNull()
    val showBottomBar = currentTop is LibraryRoute ||
        currentTop is PassagesRoute ||
        currentTop is VocabularyRoute ||
        currentTop is SettingsRoute

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTop is LibraryRoute,
                        onClick = { backStack.clear(); backStack.add(LibraryRoute) },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { backStack.clear(); backStack.add(LibraryRoute) },
                        icon = { Icon(Icons.Rounded.AutoStories, contentDescription = "Library") },
                        label = { Text("Library") }
                    )
                    NavigationBarItem(
                        selected = currentTop is PassagesRoute,
                        onClick = { backStack.clear(); backStack.add(PassagesRoute) },
                        icon = { Icon(Icons.Rounded.FormatQuote, contentDescription = "Passages") },
                        label = { Text("Passages") }
                    )
                    NavigationBarItem(
                        selected = currentTop is VocabularyRoute,
                        onClick = { backStack.clear(); backStack.add(VocabularyRoute) },
                        icon = { Icon(Icons.Rounded.School, contentDescription = "Vocab") },
                        label = { Text("Vocab") }
                    )
                    NavigationBarItem(
                        selected = currentTop is SettingsRoute,
                        onClick = { backStack.clear(); backStack.add(SettingsRoute) },
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<LibraryRoute> {
                        val viewModel: LibraryViewModel = hiltViewModel()
                        androidx.compose.runtime.LaunchedEffect(sharedImportUris) {
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

                    entry<PassagesRoute> {
                        val viewModel: PassagesViewModel = hiltViewModel()
                        PassagesScreen(
                            viewModel = viewModel,
                            onOpenPassage = { id -> backStack.add(PassageDetailRoute(id)) },
                            onCreateCard = { id -> backStack.add(CardStudioRoute(id)) }
                        )
                    }

                    entry<PassageDetailRoute> { route ->
                        val viewModel: PassagesViewModel = hiltViewModel()
                        PassageDetailScreen(
                            passageId = route.passageId,
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenInBook = { editionId, _ ->
                                val book = viewModel.uiState.value.books.firstOrNull { it.editionId == editionId }
                                if (book != null) {
                                    context.startActivity(
                                        ReaderActivity.createIntent(
                                            context = context,
                                            editionId = book.editionId,
                                            title = book.title,
                                            format = if (book.isImageBook) "IMAGE_BOOK" else book.primaryFormat,
                                            localPath = book.primaryLocalPath
                                        )
                                    )
                                }
                            },
                            onCreateCard = { id -> backStack.add(CardStudioRoute(id)) }
                        )
                    }

                    entry<CardStudioRoute> { route ->
                        val viewModel: PassagesViewModel = hiltViewModel()
                        CardStudioScreen(
                            passageId = route.passageId,
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }

                    entry<SettingsRoute> {
                        val viewModel: AppUiConfigViewModel = hiltViewModel()
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenStats = { backStack.add(StatsRoute) },
                            onOpenOnlineSearch = { backStack.add(OnlineRoute) }
                        )
                    }

                    entry<OnlineRoute> {
                        val viewModel: OnlineAcquisitionViewModel = hiltViewModel()
                        OnlineAcquisitionScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }

                    entry<StatsRoute> {
                        val viewModel: StatsViewModel = hiltViewModel()
                        StatsScreen(
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
    }
}
