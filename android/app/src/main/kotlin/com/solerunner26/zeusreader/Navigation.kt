package com.solerunner26.zeusreader

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.reader.prefs.AppTheme
import com.solerunner26.zeusreader.ui.components.CappedFontScale
import com.solerunner26.zeusreader.ui.components.Eyebrow
import com.solerunner26.zeusreader.ui.components.Tone
import com.solerunner26.zeusreader.ui.components.ZeusDialog
import com.solerunner26.zeusreader.ui.components.ZeusLinearProgress
import com.solerunner26.zeusreader.ui.home.ActivityItem
import com.solerunner26.zeusreader.ui.home.ActivityScreen
import com.solerunner26.zeusreader.ui.home.GoalDialog
import com.solerunner26.zeusreader.ui.home.HelpScreen
import com.solerunner26.zeusreader.ui.home.HomeScreen
import com.solerunner26.zeusreader.ui.home.OnboardingScreen
import com.solerunner26.zeusreader.ui.home.ProfileScreen
import com.solerunner26.zeusreader.ui.home.StreakScreen
import com.solerunner26.zeusreader.ui.library.LibraryScreen
import com.solerunner26.zeusreader.ui.library.LibraryUiState
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
import com.solerunner26.zeusreader.ui.stats.ReaderProfile
import com.solerunner26.zeusreader.ui.stats.StatsScreen
import com.solerunner26.zeusreader.ui.stats.StatsViewModel
import com.solerunner26.zeusreader.ui.theme.AppUiConfigViewModel
import com.solerunner26.zeusreader.ui.theme.ZeusPack
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.pressScale
import com.solerunner26.zeusreader.ui.theme.rememberAccentBrush
import com.solerunner26.zeusreader.ui.theme.zeusCard
import com.solerunner26.zeusreader.ui.vocab.PrepareOfflineScreen
import com.solerunner26.zeusreader.ui.vocab.VocabularyScreen
import com.solerunner26.zeusreader.ui.vocab.VocabularyViewModel

private fun openBook(context: Context, book: LibraryBookSummary) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainNavigation(
    sharedImportUris: List<Uri> = emptyList(),
    onSharedImportsHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile = remember { ReaderProfile(context) }
    val backStack = rememberNavBackStack(if (profile.onboardingDone) HomeRoute else OnboardingRoute)
    val uiConfigViewModel: AppUiConfigViewModel = hiltViewModel()
    val uiConfig by uiConfigViewModel.config.collectAsStateWithLifecycle()
    val updateViewModel: AppUpdateViewModel = hiltViewModel()
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val statsVm: StatsViewModel = hiltViewModel()
    val stats by statsVm.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { statsVm.refresh() }

    @Suppress("UNUSED_VARIABLE")
    val darkTheme = when (uiConfig.appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val currentTop = backStack.lastOrNull()
    val isRootTab = currentTop is HomeRoute || currentTop is LibraryRoute || currentTop is VocabularyRoute || currentTop is ProfileRoute
    var lastBackPressMs by remember { mutableLongStateOf(0L) }
    var showGoalDialog by remember { mutableStateOf(false) }

    fun goTab(route: NavKey) {
        backStack.clear()
        backStack.add(route)
    }

    BackHandler(enabled = true) {
        when {
            currentTop is OnboardingRoute -> (context as? Activity)?.finish()
            !isRootTab && backStack.size > 1 -> backStack.removeLastOrNull()
            !isRootTab -> goTab(HomeRoute)
            currentTop !is HomeRoute -> goTab(HomeRoute)
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

    val popOrHome: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() else goTab(HomeRoute) }

    Box(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { popOrHome() },
            entryProvider = entryProvider {
                entry<OnboardingRoute> {
                    OnboardingScreen(onFinish = {
                        profile.onboardingDone = true
                        goTab(HomeRoute)
                    })
                }

                entry<HomeRoute> {
                    val libVm: LibraryViewModel = hiltViewModel()
                    val passagesVm: PassagesViewModel = hiltViewModel()
                    LaunchedEffect(sharedImportUris) {
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
                        onOpenBook = { openBook(context, it) },
                        onOpenLibrary = { goTab(LibraryRoute) },
                        onOpenPassages = { backStack.add(PassagesRoute) },
                        onOpenVocab = { goTab(VocabularyRoute) },
                        onOpenOnline = { backStack.add(OnlineRoute) },
                        onOpenStats = { backStack.add(StatsRoute) },
                        onOpenPrepareOffline = { backStack.add(PrepareOfflineRoute) },
                        onOpenSettings = { backStack.add(SettingsRoute) },
                        onOpenProfile = { goTab(ProfileRoute) },
                        onOpenStreak = { backStack.add(StreakRoute) },
                        onOpenActivity = { backStack.add(ActivityRoute) }
                    )
                }

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
                        onOpenBook = { openBook(context, it) },
                        onOpenVocabulary = { goTab(VocabularyRoute) },
                        onOpenPrepareOffline = { backStack.add(PrepareOfflineRoute) },
                        onOpenSettings = { backStack.add(SettingsRoute) },
                        onOpenOnline = { backStack.add(OnlineRoute) }
                    )
                }

                entry<PassagesRoute> {
                    val viewModel: PassagesViewModel = hiltViewModel()
                    PassagesScreen(
                        viewModel = viewModel,
                        onOpenPassage = { id -> backStack.add(PassageDetailRoute(id)) },
                        onCreateCard = { id -> backStack.add(CardStudioRoute(id)) },
                        onBack = popOrHome
                    )
                }

                entry<PassageDetailRoute> { route ->
                    val viewModel: PassagesViewModel = hiltViewModel()
                    PassageDetailScreen(
                        passageId = route.passageId,
                        viewModel = viewModel,
                        onBack = popOrHome,
                        onOpenInBook = { editionId, _ ->
                            viewModel.uiState.value.books.firstOrNull { it.editionId == editionId }?.let { openBook(context, it) }
                        },
                        onCreateCard = { id -> backStack.add(CardStudioRoute(id)) }
                    )
                }

                entry<CardStudioRoute> { route ->
                    val viewModel: PassagesViewModel = hiltViewModel()
                    CardStudioScreen(passageId = route.passageId, viewModel = viewModel, onBack = popOrHome)
                }

                entry<SettingsRoute> {
                    SettingsScreen(
                        viewModel = uiConfigViewModel,
                        onBack = popOrHome,
                        onOpenStats = { backStack.add(StatsRoute) },
                        onOpenOnlineSearch = { backStack.add(OnlineRoute) },
                        updateState = updateState,
                        onCheckForUpdate = { updateViewModel.checkNow(silentIfUpToDate = false) },
                        onInstallUpdate = updateViewModel::downloadAndInstall
                    )
                }

                entry<OnlineRoute> {
                    val viewModel: OnlineAcquisitionViewModel = hiltViewModel()
                    OnlineAcquisitionScreen(viewModel = viewModel, onBack = popOrHome, uiConfig = uiConfig)
                }

                entry<StatsRoute> {
                    StatsScreen(viewModel = statsVm, onBack = popOrHome, uiConfig = uiConfig)
                }

                entry<StreakRoute> {
                    StreakScreen(stats = stats, onBack = popOrHome, onSetGoal = statsVm::setDailyGoal)
                }

                entry<ActivityRoute> {
                    val items = buildList {
                        if (!stats.goalReached) {
                            add(ActivityItem("Keep your ${stats.currentStreakDays}-day streak", "Read ${(stats.dailyGoalMinutes - stats.todayMinutes).coerceAtLeast(1)} more minutes today.", Tone.Accent, Icons.Rounded.LocalFireDepartment) { backStack.add(StreakRoute) })
                        } else {
                            add(ActivityItem("Today's goal reached", "${stats.todayMinutes} minutes read. Nice work!", Tone.Ok, Icons.Rounded.TrackChanges) { backStack.add(StreakRoute) })
                        }
                        if (stats.dueVocabWords > 0) add(ActivityItem("${stats.dueVocabWords} words are ready to review", "A quick quiz keeps them fresh.", Tone.Info, Icons.Rounded.Translate) { goTab(VocabularyRoute) })
                        if (stats.totalPassages > 0) add(ActivityItem("${stats.totalPassages} highlights saved", "Revisit them or turn one into a card.", Tone.Rose, Icons.Rounded.AutoAwesome) { backStack.add(PassagesRoute) })
                        if (stats.booksInProgress > 0) add(ActivityItem("${stats.booksInProgress} books in progress", "Pick up where you left off.", Tone.Accent, Icons.AutoMirrored.Rounded.MenuBook) { goTab(LibraryRoute) })
                        add(ActivityItem("Find something new", "Search open catalogues and download to your phone.", Tone.Ok, Icons.Rounded.Download) { backStack.add(OnlineRoute) })
                    }
                    ActivityScreen(items = items, onBack = popOrHome)
                }

                entry<ProfileRoute> {
                    ProfileScreen(
                        stats = stats,
                        packLabel = ZeusPack.fromHue(uiConfig.themeHue).label,
                        onEditName = statsVm::setDisplayName,
                        onOpenGoal = { showGoalDialog = true },
                        onOpenSettings = { backStack.add(SettingsRoute) },
                        onOpenStats = { backStack.add(StatsRoute) },
                        onOpenHighlights = { backStack.add(PassagesRoute) },
                        onOpenOffline = { backStack.add(PrepareOfflineRoute) },
                        onOpenOnline = { backStack.add(OnlineRoute) },
                        onOpenHelp = { backStack.add(HelpRoute) }
                    )
                }

                entry<HelpRoute> { HelpScreen(onBack = popOrHome) }

                entry<VocabularyRoute> {
                    val viewModel: VocabularyViewModel = hiltViewModel()
                    VocabularyScreen(viewModel = viewModel, onBack = popOrHome)
                }

                entry<PrepareOfflineRoute> {
                    val viewModel: VocabularyViewModel = hiltViewModel()
                    PrepareOfflineScreen(viewModel = viewModel, onBack = popOrHome)
                }
            }
        )

        val pendingUpdate = updateState.availableUpdate
        if (pendingUpdate != null && !updateState.dismissedForSession && currentTop !is OnboardingRoute) {
            ZeusDialog(
                onDismissRequest = updateViewModel::dismissForSession,
                title = "Update available · ${pendingUpdate.tagName}",
                confirmLabel = if (updateState.isDownloading) "Downloading…" else "Update now",
                onConfirm = updateViewModel::downloadAndInstall,
                confirmEnabled = !updateState.isDownloading,
                dismissLabel = "Later",
                onDismiss = updateViewModel::dismissForSession
            ) {
                Eyebrow("Installed v${updateState.currentVersion} → ${pendingUpdate.tagName}", maxLines = 2)
                Text(pendingUpdate.releaseNotes.take(450), style = ZeusTheme.type.caption, color = ZeusTheme.colors.muted)
                if (updateState.isDownloading) {
                    ZeusLinearProgress(progress = updateState.downloadProgress / 100f)
                    Text("Downloading… ${updateState.downloadProgress}%", style = ZeusTheme.type.caption, color = ZeusTheme.colors.muted)
                }
                updateState.statusMessage?.let { Text(it, style = ZeusTheme.type.caption, color = ZeusTheme.colors.muted) }
            }
        }

        if (showGoalDialog) {
            GoalDialog(current = stats.dailyGoalMinutes, onDismiss = { showGoalDialog = false }, onSave = { statsVm.setDailyGoal(it); showGoalDialog = false })
        }

        if (isRootTab && !WindowInsets.isImeVisible) {
            BottomBar(
                currentTop = currentTop,
                onSelect = { goTab(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = ZeusSpacing.navInset, vertical = ZeusSpacing.navInset)
            )
        }
    }
}

private data class Tab(val route: NavKey, val label: String, val icon: ImageVector, val iconSelected: ImageVector)

private val tabs = listOf(
    Tab(HomeRoute, "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    Tab(LibraryRoute, "Library", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Rounded.MenuBook),
    Tab(VocabularyRoute, "Words", Icons.Rounded.Translate, Icons.Rounded.Translate),
    Tab(ProfileRoute, "Me", Icons.Outlined.Person, Icons.Rounded.Person)
)

/** Fixed 4-tab bottom bar: filled icon on a gradient pill for the active tab. */
@Composable
private fun BottomBar(currentTop: Any?, onSelect: (NavKey) -> Unit, modifier: Modifier = Modifier) {
    val c = ZeusTheme.colors
    CappedFontScale(1.3f) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = ZeusSizes.contentMaxWidth)
                .height(ZeusSpacing.navHeight)
                .zeusCard(ZeusShapes.nav, elevation = ZeusSizes.shadowLift)
                .padding(horizontal = ZeusSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = currentTop?.let { it::class == tab.route::class } == true
                val source = remember { MutableInteractionSource() }
                val pillWidth by animateDpAsState(if (selected) ZeusSizes.navPillWidth else ZeusSizes.navPillHeight, com.solerunner26.zeusreader.ui.theme.ZeusMotion.bouncy(ZeusTheme.reduceMotion), label = "nav_pill")
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pressScale(source)
                        .clip(ZeusShapes.tile)
                        .selectable(selected = selected, interactionSource = source, indication = null, role = Role.Tab) { onSelect(tab.route) }
                        .semantics { contentDescription = tab.label },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs, Alignment.CenterVertically)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = pillWidth, height = ZeusSizes.navPillHeight)
                            .then(if (selected && !c.isEink) Modifier.shadow(ZeusSizes.shadowCard, ZeusShapes.pill, ambientColor = c.glow, spotColor = c.glow) else Modifier)
                            .clip(ZeusShapes.pill)
                            .then(if (selected) Modifier.background(rememberAccentBrush()) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (selected) tab.iconSelected else tab.icon, null, tint = if (selected) c.onAccent else c.muted, modifier = Modifier.size(ZeusSizes.iconMd))
                    }
                    Text(tab.label, style = ZeusTheme.type.tiny, color = if (selected) c.ink else c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

