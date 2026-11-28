package com.solerunner26.zeusreader

import android.app.Activity
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.ZeusDialog
import com.solerunner26.zeusreader.ui.components.ZeusLinearProgress
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.neonBorder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
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
import com.solerunner26.zeusreader.ui.vocab.PrepareOfflineScreen
import com.solerunner26.zeusreader.ui.vocab.VocabularyScreen
import com.solerunner26.zeusreader.ui.vocab.VocabularyViewModel

@OptIn(ExperimentalLayoutApi::class)
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
                ZeusDialog(
                    onDismissRequest = updateViewModel::dismissForSession,
                    title = "Update available · ${pendingUpdate.tagName}",
                    confirmLabel = if (updateState.isDownloading) "Downloading…" else "Update now",
                    onConfirm = updateViewModel::downloadAndInstall,
                    confirmEnabled = !updateState.isDownloading,
                    dismissLabel = "Later",
                    onDismiss = updateViewModel::dismissForSession
                ) {
                    MetaLabel(
                        text = "Installed v${updateState.currentVersion} → ${pendingUpdate.tagName}",
                        accent = true,
                        maxLines = 2
                    )
                    Text(
                        text = pendingUpdate.releaseNotes.take(450),
                        style = ZeusTheme.type.caption,
                        color = ZeusTheme.colors.textOnSkySecondary
                    )
                    if (updateState.isDownloading) {
                        ZeusLinearProgress(progress = updateState.downloadProgress / 100f)
                        MetaLabel(text = "Downloading… ${updateState.downloadProgress}%")
                    }
                    updateState.statusMessage?.let { msg ->
                        Text(msg, style = ZeusTheme.type.caption, color = ZeusTheme.colors.textOnSkySecondary)
                    }
                }
            }
        }

        val imeVisible = WindowInsets.isImeVisible
        if (isRootTab && !imeVisible) {
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
    val colors = ZeusTheme.colors
    val density = LocalDensity.current
    // The dock has a fixed 68dp height: cap font scaling for its labels (icons carry content descriptions).
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = ZeusSizes.contentMaxWidth)
                .height(ZeusSpacing.dockHeight)
                .shadow(ZeusSizes.shadowGlow, ZeusShapes.dock, ambientColor = colors.neonA, spotColor = colors.neonA)
                .neonBorder(shape = ZeusShapes.dock, intensity = 0.7f)
                .clip(ZeusShapes.dock)
                .background(if (glassEnabled) colors.dockFill else colors.glassFallback)
                .border(ZeusSpacing.glassBorderWidth, colors.glassBorder, ZeusShapes.dock)
                .padding(horizontal = ZeusSpacing.sm, vertical = ZeusSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockTabItem("Home", currentTop is HomeRoute, onSelectHome, Modifier.weight(1f)) { tint ->
                Icon(Icons.Rounded.Home, contentDescription = null, tint = tint, modifier = Modifier.size(ZeusSizes.iconLg))
            }
            DockTabItem("Olympus", currentTop is LibraryRoute, onSelectOlympus, Modifier.weight(1f)) { tint ->
                Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = tint, modifier = Modifier.size(ZeusSizes.iconLg))
            }
            DockTabItem("Sparks", currentTop is PassagesRoute, onSelectSparks, Modifier.weight(1f)) { tint ->
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = tint, modifier = Modifier.size(ZeusSizes.iconLg))
            }
            DockTabItem("Words", currentTop is VocabularyRoute, onSelectWords, Modifier.weight(1f)) { tint ->
                Text(
                    text = "अ",
                    style = ZeusTheme.type.numberMedium,
                    color = tint,
                    maxLines = 1
                )
            }
        }
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
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val tint = if (selected) colors.accentOnSky else colors.textOnSkySecondary
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(ZeusShapes.cardSmall)
            .then(if (selected) Modifier.background(colors.glassFillStrong) else Modifier)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon(tint)
        Text(
            text = label,
            style = type.labelSmall.copy(fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
