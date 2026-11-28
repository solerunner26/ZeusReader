package com.solerunner26.zeusreader.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.ui.components.Avatar
import com.solerunner26.zeusreader.ui.components.BoltMascot
import com.solerunner26.zeusreader.ui.components.BookCover
import com.solerunner26.zeusreader.ui.components.CircleIconButton
import com.solerunner26.zeusreader.ui.components.CrownArtifact
import com.solerunner26.zeusreader.ui.components.Eyebrow
import com.solerunner26.zeusreader.ui.components.FlameArtifact
import com.solerunner26.zeusreader.ui.components.GradientCard
import com.solerunner26.zeusreader.ui.components.IconBox
import com.solerunner26.zeusreader.ui.components.MetricCard
import com.solerunner26.zeusreader.ui.components.PillButton
import com.solerunner26.zeusreader.ui.components.ProgressBar
import com.solerunner26.zeusreader.ui.components.SectionHeader
import com.solerunner26.zeusreader.ui.components.Tone
import com.solerunner26.zeusreader.ui.components.ZeusCard
import com.solerunner26.zeusreader.ui.components.rememberNavClearance
import com.solerunner26.zeusreader.ui.components.staggerIn
import com.solerunner26.zeusreader.ui.library.LibraryUiState
import com.solerunner26.zeusreader.ui.library.LibraryViewModel
import com.solerunner26.zeusreader.ui.passages.PassagesViewModel
import com.solerunner26.zeusreader.ui.stats.ReadingStatsUiState
import com.solerunner26.zeusreader.ui.stats.StatsViewModel
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import java.time.LocalTime

/** Reading progress (0..1) when known: EPUB stores ‰ of the book in lastPageIndex. */
internal fun LibraryBookSummary.knownProgress(): Float? {
    val idx = lastPageIndex ?: return null
    return if (primaryFormat.equals("EPUB", ignoreCase = true)) (idx / 1000f).coerceIn(0f, 1f) else null
}

internal fun LibraryBookSummary.progressLabel(): String {
    val idx = lastPageIndex ?: return "New"
    return knownProgress()?.let { "${(it * 100).toInt()}% read" } ?: "Page ${idx + 1}"
}

private fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Late-night reading"
}

@Composable
fun HomeScreen(
    libraryViewModel: LibraryViewModel,
    passagesViewModel: PassagesViewModel,
    statsViewModel: StatsViewModel,
    @Suppress("UNUSED_PARAMETER") glassEnabled: Boolean,
    onOpenBook: (LibraryBookSummary) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPassages: () -> Unit,
    onOpenVocab: () -> Unit,
    onOpenOnline: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenPrepareOffline: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenProfile: () -> Unit = onOpenSettings,
    onOpenStreak: () -> Unit = onOpenStats,
    onOpenActivity: () -> Unit = onOpenStats,
    modifier: Modifier = Modifier
) {
    val libState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val passagesState by passagesViewModel.uiState.collectAsStateWithLifecycle()
    val stats by statsViewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { statsViewModel.refresh() }

    HomeContent(
        books = (libState as? LibraryUiState.Populated)?.books.orEmpty(),
        featuredSparkText = passagesState.passages.firstOrNull()?.passage?.text,
        stats = stats,
        onOpenBook = onOpenBook,
        onOpenLibrary = onOpenLibrary,
        onOpenPassages = onOpenPassages,
        onOpenVocab = onOpenVocab,
        onOpenOnline = onOpenOnline,
        onOpenStreak = onOpenStreak,
        onOpenPrepareOffline = onOpenPrepareOffline,
        onOpenProfile = onOpenProfile,
        onOpenActivity = onOpenActivity,
        modifier = modifier
    )
}

@Composable
internal fun HomeContent(
    books: List<LibraryBookSummary>,
    featuredSparkText: String?,
    stats: ReadingStatsUiState,
    onOpenBook: (LibraryBookSummary) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPassages: () -> Unit,
    onOpenVocab: () -> Unit,
    onOpenOnline: () -> Unit,
    onOpenStreak: () -> Unit,
    onOpenPrepareOffline: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenActivity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    val continueBook = remember(books) {
        books.filter { (it.lastPageIndex ?: 0) > 0 }.maxByOrNull { it.updatedAtEpochMs } ?: books.firstOrNull()
    }
    val shelf = remember(books, continueBook) {
        books.filter { it.editionId != continueBook?.editionId }.sortedByDescending { it.updatedAtEpochMs }.take(12)
    }
    val greeting = remember { greetingFor(LocalTime.now().hour) }
    val name = stats.displayName

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("home_screen"),
        contentPadding = PaddingValues(start = ZeusSpacing.gutter, end = ZeusSpacing.gutter, top = ZeusSpacing.sm, bottom = rememberNavClearance()),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.section)
    ) {
        item(key = "greeting") {
            Row(Modifier.fillMaxWidth().staggerIn(0), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
                Avatar(initial = name.ifBlank { "Z" }, onClick = onOpenProfile, contentDescription = "Your profile")
                Column(Modifier.weight(1f)) {
                    Text(if (name.isBlank()) greeting else "$greeting, $name", style = type.caption, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = when {
                            continueBook == null -> "Let's find your first book"
                            stats.goalReached -> "Goal done. Keep going?"
                            else -> "Ready to read?"
                        },
                        style = type.headline,
                        color = c.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                CircleIconButton(Icons.Rounded.NotificationsNone, "Activity", onOpenActivity, badge = stats.dueVocabWords > 0 || !stats.goalReached)
            }
        }

        item(key = "hero") {
            HeroCard(book = continueBook, onOpenBook = onOpenBook, onOpenLibrary = onOpenLibrary, modifier = Modifier.staggerIn(1))
        }

        item(key = "goal") {
            ZeusCard(modifier = Modifier.fillMaxWidth().staggerIn(2), onClick = onOpenStreak) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                    Text("Today's goal", style = type.label, color = c.ink, modifier = Modifier.weight(1f))
                    if (stats.goalReached) CrownArtifact(ZeusSizes.iconLg)
                    Text("${stats.todayMinutes} / ${stats.dailyGoalMinutes} min", style = type.label, color = c.accentInk, maxLines = 1)
                }
                ProgressBar(stats.goalProgress)
                Text(
                    text = if (stats.goalReached) "Goal reached! Your streak is safe today." else "${(stats.dailyGoalMinutes - stats.todayMinutes).coerceAtLeast(1)} more minutes keeps your streak alive",
                    style = type.caption,
                    color = c.muted
                )
            }
        }

        item(key = "metrics") {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).staggerIn(3), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
                MetricCard(
                    value = "${stats.currentStreakDays}",
                    label = if (stats.currentStreakDays == 1) "day streak" else "day streak",
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = onOpenStreak,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    art = { IconBoxArt { FlameArtifact(ZeusSizes.iconLg) } }
                )
                MetricCard(
                    value = "${stats.dueVocabWords}",
                    label = "words to review",
                    icon = Icons.Rounded.Translate,
                    tone = Tone.Info,
                    onClick = onOpenVocab,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        item(key = "quick") {
            Row(Modifier.fillMaxWidth().staggerIn(4), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                QuickAction(Icons.Rounded.Add, "Import", onOpenLibrary, Modifier.weight(1f), filled = true)
                QuickAction(Icons.Rounded.AutoAwesome, "Highlights", onOpenPassages, Modifier.weight(1f))
                QuickAction(Icons.Rounded.Public, "Find books", onOpenOnline, Modifier.weight(1f))
                QuickAction(Icons.Rounded.CloudDownload, "Offline", onOpenPrepareOffline, Modifier.weight(1f))
            }
        }

        if (shelf.isNotEmpty()) {
            item(key = "shelf_head") { SectionHeader("On your shelf", actionLabel = "See all", onAction = onOpenLibrary) }
            item(key = "shelf") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
                    items(shelf, key = { it.editionId }) { book -> ShelfBook(book) { onOpenBook(book) } }
                }
            }
        }

        if (featuredSparkText != null) {
            item(key = "spark") {
                ZeusCard(Modifier.fillMaxWidth(), onClick = onOpenPassages) {
                    Eyebrow("A highlight resurfaced")
                    Text("“$featuredSparkText”", style = type.quote, color = c.ink, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun IconBoxArt(content: @Composable () -> Unit) {
    val c = ZeusTheme.colors
    Box(
        Modifier
            .width(ZeusSizes.iconBox)
            .aspectRatio(1f)
            .clip(ZeusShapes.iconBox)
            .background(c.tint),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun HeroCard(book: LibraryBookSummary?, onOpenBook: (LibraryBookSummary) -> Unit, onOpenLibrary: () -> Unit, modifier: Modifier = Modifier) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    GradientCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ZeusSizes.heroMinHeight)
            .semantics { contentDescription = book?.let { "Continue reading ${it.title}" } ?: "Open your library" },
        onClick = { if (book != null) onOpenBook(book) else onOpenLibrary() },
        padding = PaddingValues(ZeusSpacing.none)
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = ZeusSizes.heroMinHeight)) {
            BoltMascot(
                ZeusSizes.heroMascot,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = ZeusSpacing.xsPlus, y = ZeusSpacing.smPlus)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .padding(ZeusSpacing.gutter),
                verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)
            ) {
                Eyebrow(if (book != null) "Continue reading" else "Your library", color = c.onAccent)
                Text(book?.title ?: "Bring your first book", style = type.headline.copy(fontSize = type.title.fontSize.times(0.8f)), color = c.onAccent, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(
                    text = book?.let { listOf(it.authors.takeIf { a -> a.isNotBlank() }, it.progressLabel()).filterNotNull().joinToString(" · ") } ?: "EPUB, PDF, comics or scans",
                    style = type.caption,
                    color = c.onAccent.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                book?.knownProgress()?.let { ProgressBar(it, height = ZeusSizes.progressThin, onAccent = true) }
                PillButton(
                    label = if (book != null) "Resume" else "Import",
                    icon = if (book != null) Icons.Rounded.PlayArrow else Icons.Rounded.Add,
                    onClick = { if (book != null) onOpenBook(book) else onOpenLibrary() },
                    modifier = Modifier.padding(top = ZeusSpacing.xs)
                )
            }
        }
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = false) {
    val c = ZeusTheme.colors
    Column(
        modifier = modifier
            .clip(ZeusShapes.tile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = ZeusSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)
    ) {
        IconBox(icon, size = ZeusSizes.quickTile, filled = filled)
        com.solerunner26.zeusreader.ui.components.CappedFontScale(1.4f) {
            Text(label, style = ZeusTheme.type.caption, color = c.muted, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ShelfBook(book: LibraryBookSummary, onClick: () -> Unit) {
    val c = ZeusTheme.colors
    Column(
        modifier = Modifier
            .width(ZeusSizes.coverShelfWidth)
            .clip(ZeusShapes.tile)
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)
    ) {
        BookCover(
            title = book.title,
            authors = book.authors,
            format = if (book.isImageBook) "IMG" else book.primaryFormat,
            coverPath = book.coverPath,
            compact = true,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
        )
        Text(book.title, style = ZeusTheme.type.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(book.progressLabel(), style = ZeusTheme.type.tiny, color = c.muted, maxLines = 1)
    }
}

/** Kept for source compatibility — delegates to the shared [BookCover]. */
@Composable
fun BookCoverThumbnail(title: String, format: String, coverPath: String?, modifier: Modifier = Modifier) {
    BookCover(title = title, format = format, coverPath = coverPath, modifier = modifier)
}
