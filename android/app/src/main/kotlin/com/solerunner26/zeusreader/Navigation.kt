package com.solerunner26.zeusreader

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.solerunner26.zeusreader.reader.prefs.AppTheme
import com.solerunner26.zeusreader.ui.home.HomeScreen
import com.solerunner26.zeusreader.ui.library.LibraryScreen
import com.solerunner26.zeusreader.ui.library.LibraryViewModel
import com.solerunner26.zeusreader.ui.online.OnlineAcquisitionScreen
import com.solerunner26.zeusreader.ui.online.OnlineAcquisitionViewModel
import com.solerunner26.zeusreader.ui.passages.CardStudioScreen
import com.solerunner26.zeusreader.ui.passages.PassageDetailScreen
import com.solerunner26.zeusreader.ui.passages.PassagesScreen
import com.solerunner26.zeusreader.ui.passages.PassagesViewModel
import com.solerunner26.zeusreader.ui.reader.ReaderActivity
import com.solerunner26.zeusreader.ui.settings.SettingsScreen
import com.solerunner26.zeusreader.ui.stats.StatsScreen
import com.solerunner26.zeusreader.ui.stats.StatsViewModel
import com.solerunner26.zeusreader.ui.theme.AppUiConfigViewModel
import com.solerunner26.zeusreader.ui.theme.liquidGlass
import com.solerunner26.zeusreader.ui.vocab.PrepareOfflineScreen
import com.solerunner26.zeusreader.ui.vocab.VocabularyScreen
import com.solerunner26.zeusreader.ui.vocab.VocabularyViewModel

@Composable
fun MainNavigation(
    sharedImportUris: List<Uri> = emptyList(),
    onSharedImportsHandled: () -> Unit = {}
) {
    val backStack = rememberNavBackStack(HomeRoute)
    val context = LocalContext.current
    val uiConfigViewModel: AppUiConfigViewModel = hiltViewModel()
    val uiConfig by uiConfigViewModel.config.collectAsStateWithLifecycle()
    val darkTheme = when (uiConfig.appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    val currentTop = backStack.lastOrNull()
    val isRootTab = currentTop is HomeRoute ||
        currentTop is LibraryRoute ||
        currentTop is PassagesRoute ||
        currentTop is VocabularyRoute ||
        currentTop is SettingsRoute

    var lastBackPressMs by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        when {
            !isRootTab && backStack.size > 1 -> {
                backStack.removeLastOrNull()
            }
            currentTop !is HomeRoute -> {
                backStack.clear()
                backStack.add(HomeRoute)
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressMs <= 2_000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressMs = now
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (isRootTab) {
                NavigationBar(
                    containerColor = if (uiConfig.glassUi) Color.Transparent else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .liquidGlass(
                            enabled = uiConfig.glassUi,
                            shape = RoundedCornerShape(28.dp),
                            alpha = 0.78f
                        )
                ) {
                    NavigationBarItem(
                        selected = currentTop is HomeRoute,
                        onClick = { backStack.clear(); backStack.add(HomeRoute) },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentTop is LibraryRoute,
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
                onBack = {
                    if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    } else if (currentTop !is HomeRoute) {
                        backStack.clear()
                        backStack.add(HomeRoute)
                    }
                },
                entryProvider = entryProvider {
                    entry<HomeRoute> {
                        val libVm: LibraryViewModel = hiltViewModel()
                        val passagesVm: PassagesViewModel = hiltViewModel()
                        val statsVm: StatsViewModel = hiltViewModel()
                        androidx.compose.runtime.LaunchedEffect(sharedImportUris) {
                            if (sharedImportUris.isNotEmpty()) {
                                libVm.importUris(sharedImportUris, origin = "share_intent")
                                onSharedImportsHandled()
                            }
                        }
                        HomeScreen(
                            libraryViewModel = libVm,
                            passagesViewModel = passagesVm,
                            statsViewModel = statsVm,
                            glassEnabled = uiConfig.glassUi,
                            onOpenBook = { book ->
                                context.startActivity(
                                    ReaderActivity.createIntent(
                                        context = context,
                                        editionId = book.editionId,
                                        title = book.title,
                                        format = if (book.isImageBook) "IMAGE_BOOK" else book.primaryFormat,
                                        localPath = book.primaryLocalPath
                                    )
                                )
                            },
                            onOpenLibrary = { backStack.clear(); backStack.add(LibraryRoute) },
                            onOpenPassages = { backStack.clear(); backStack.add(PassagesRoute) },
                            onOpenVocab = { backStack.clear(); backStack.add(VocabularyRoute) },
                            onOpenOnline = { backStack.add(OnlineRoute) },
                            onOpenStats = { backStack.add(StatsRoute) },
                            onOpenPrepareOffline = { backStack.add(PrepareOfflineRoute) }
                        )
                    }

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
                            onBack = { backStack.removeLastOrNull() },
                            uiConfig = uiConfig,
                            darkTheme = darkTheme
                        )
                    }

                    entry<StatsRoute> {
                        val viewModel: StatsViewModel = hiltViewModel()
                        StatsScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                            uiConfig = uiConfig,
                            darkTheme = darkTheme
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
