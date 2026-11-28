package com.solerunner26.zeusreader

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
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
import com.solerunner26.zeusreader.ui.settings.AppUpdateViewModel
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
    val updateViewModel: AppUpdateViewModel = hiltViewModel()
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val darkTheme = when (uiConfig.appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    val currentTop = backStack.lastOrNull()
    val isRootTab = currentTop is HomeRoute ||
        currentTop is LibraryRoute ||
        currentTop is PassagesRoute ||
        currentTop is VocabularyRoute

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

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                            onOpenPrepareOffline = { backStack.add(PrepareOfflineRoute) },
                            onOpenSettings = { backStack.add(SettingsRoute) }
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
                            onOpenOnlineSearch = { backStack.add(OnlineRoute) },
                            updateState = updateState,
                            onCheckForUpdate = { updateViewModel.checkNow(silentIfUpToDate = false) },
                            onInstallUpdate = updateViewModel::downloadAndInstall
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

            val pendingUpdate = updateState.availableUpdate
            if (pendingUpdate != null && !updateState.dismissedForSession) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = updateViewModel::dismissForSession,
                    title = { Text("Update Available: ${pendingUpdate.tagName}") },
                    text = {
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Installed: v${updateState.currentVersion} → New: ${pendingUpdate.tagName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = pendingUpdate.releaseNotes.take(450),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (updateState.isDownloading) {
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { updateState.downloadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Downloading… ${updateState.downloadProgress}%",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            updateState.statusMessage?.let { msg ->
                                Text(msg, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = updateViewModel::downloadAndInstall,
                            enabled = !updateState.isDownloading
                        ) {
                            Text(if (updateState.isDownloading) "Downloading…" else "Update Now")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = updateViewModel::dismissForSession) {
                            Text("Later")
                        }
                    }
                )
            }
        }

        if (isRootTab) {
            StormlightBottomDock(
                currentTop = currentTop,
                glassEnabled = uiConfig.glassUi,
                onSelectHome = { backStack.clear(); backStack.add(HomeRoute) },
                onSelectOlympus = { backStack.clear(); backStack.add(LibraryRoute) },
                onSelectSparks = { backStack.clear(); backStack.add(PassagesRoute) },
                onSelectWords = { backStack.clear(); backStack.add(VocabularyRoute) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = ZeusSpacing.dockInset, vertical = ZeusSpacing.dockInset)
            )
        }
    }
}

@Composable
private fun StormlightBottomDock(
    currentTop: Any?,
    glassEnabled: Boolean,
    onSelectHome: () -> Unit,
    onSelectOlympus: () -> Unit,
    onSelectSparks: () -> Unit,
    onSelectWords: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sky = ZeusTheme.sky
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val bgColor = if (glassEnabled) colors.dockFill else colors.glassFallback

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ZeusSpacing.dockHeight)
            .clip(ZeusShapes.dock)
            .background(bgColor)
            .border(BorderStroke(ZeusSpacing.glassBorderWidth, colors.glassBorder), ZeusShapes.dock)
            .padding(horizontal = ZeusSpacing.sm),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockTabItem(
            label = "Home",
            selected = currentTop is HomeRoute,
            onClick = onSelectHome,
            modifier = Modifier.weight(1f),
            icon = { tint ->
                Icon(Icons.Rounded.Home, contentDescription = "Home", tint = tint, modifier = Modifier.size(22.dp))
            }
        )
        DockTabItem(
            label = "Olympus",
            selected = currentTop is LibraryRoute,
            onClick = onSelectOlympus,
            modifier = Modifier.weight(1f),
            icon = { tint ->
                Icon(Icons.Rounded.AutoStories, contentDescription = "Olympus", tint = tint, modifier = Modifier.size(22.dp))
            }
        )
        DockTabItem(
            label = "Sparks",
            selected = currentTop is PassagesRoute,
            onClick = onSelectSparks,
            modifier = Modifier.weight(1f),
            icon = { tint ->
                Icon(Icons.Rounded.AutoAwesome, contentDescription = "Sparks", tint = tint, modifier = Modifier.size(22.dp))
            }
        )
        DockTabItem(
            label = "Words",
            selected = currentTop is VocabularyRoute,
            onClick = onSelectWords,
            modifier = Modifier.weight(1f),
            icon = { tint ->
                Text(
                    text = "अ",
                    style = type.title.copy(fontFamily = type.Unbounded, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp),
                    color = tint
                )
            }
        )
    }
}

@Composable
private fun DockTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (Color) -> Unit
) {
    val sky = ZeusTheme.sky
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val tint = if (selected) sky.accent else colors.textOnSky.copy(alpha = 0.72f)

    Column(
        modifier = modifier
            .heightIn(min = ZeusSpacing.touchMin)
            .clip(ZeusShapes.pill)
            .clickable(onClick = onClick)
            .padding(vertical = ZeusSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon(tint)
        Text(
            text = label,
            style = type.meta.copy(
                fontFamily = type.BricolageGrotesque,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
            ),
            color = tint,
            maxLines = 1
        )
    }
}
