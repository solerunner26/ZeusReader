package com.solerunner26.zeusreader.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.R
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.ui.components.BookCover
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.PrimaryPillButton
import com.solerunner26.zeusreader.ui.components.ProgressRing
import com.solerunner26.zeusreader.ui.components.QuickActionTile
import com.solerunner26.zeusreader.ui.components.StickerChip
import com.solerunner26.zeusreader.ui.components.ZeusSectionHeader
import com.solerunner26.zeusreader.ui.components.rememberDockClearance
import com.solerunner26.zeusreader.ui.library.LibraryUiState
import com.solerunner26.zeusreader.ui.library.LibraryViewModel
import com.solerunner26.zeusreader.ui.passages.PassagesViewModel
import com.solerunner26.zeusreader.ui.stats.StatsViewModel
import com.solerunner26.zeusreader.ui.theme.GlassSurface
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.rememberPrimaryGradient
import java.time.LocalTime

private const val BANNER_ASPECT = 1024f / 215f

/** Reading progress (0..1) when it is known: EPUB stores ‰ of the book in lastPageIndex. */
internal fun LibraryBookSummary.knownProgress(): Float? {
    val idx = lastPageIndex ?: return null
    return if (primaryFormat.equals("EPUB", ignoreCase = true)) (idx / 1000f).coerceIn(0f, 1f) else null
}

internal fun LibraryBookSummary.progressLabel(): String {
    val idx = lastPageIndex ?: return "Not started"
    return knownProgress()?.let { "${(it * 100).toInt()}% read" } ?: "p.${idx + 1}"
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
    glassEnabled: Boolean,
    onOpenBook: (LibraryBookSummary) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPassages: () -> Unit,
    onOpenVocab: () -> Unit,
    onOpenOnline: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenPrepareOffline: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val libState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val passagesState by passagesViewModel.uiState.collectAsStateWithLifecycle()
    val statsState by statsViewModel.uiState.collectAsStateWithLifecycle()

    val books = (libState as? LibraryUiState.Populated)?.books.orEmpty()
    HomeContent(
        books = books,
        featuredSparkText = passagesState.passages.firstOrNull()?.passage?.text,
        streakDays = statsState.currentStreakDays,
        totalBooks = statsState.totalBooks,
        booksInProgress = statsState.booksInProgress,
        dueVocabWords = statsState.dueVocabWords,
        glassEnabled = glassEnabled,
        onOpenBook = onOpenBook,
        onOpenLibrary = onOpenLibrary,
        onOpenPassages = onOpenPassages,
        onOpenVocab = onOpenVocab,
        onOpenOnline = onOpenOnline,
        onOpenStats = onOpenStats,
        onOpenPrepareOffline = onOpenPrepareOffline,
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}

@Composable
internal fun HomeContent(
    books: List<LibraryBookSummary>,
    featuredSparkText: String?,
    streakDays: Int,
    totalBooks: Int,
    booksInProgress: Int,
    dueVocabWords: Int,
    glassEnabled: Boolean,
    onOpenBook: (LibraryBookSummary) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPassages: () -> Unit,
    onOpenVocab: () -> Unit,
    onOpenOnline: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenPrepareOffline: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val continueBook = remember(books) {
        books.filter { (it.lastPageIndex ?: 0) > 0 }.maxByOrNull { it.updatedAtEpochMs } ?: books.firstOrNull()
    }
    val shelf = remember(books, continueBook) {
        books.filter { it.editionId != continueBook?.editionId }.sortedByDescending { it.updatedAtEpochMs }.take(12)
    }
    val greeting = remember { greetingFor(LocalTime.now().hour) }

    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val dockClearance = rememberDockClearance()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("home_screen"),
        contentPadding = PaddingValues(top = ZeusSpacing.md, bottom = dockClearance),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.section),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: settings avatar · greeting + wordmark · streak sticker
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ZeusSpacing.gutter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(ZeusSizes.avatar)
                        .shadow(ZeusSizes.shadowSoft, ZeusShapes.circle, ambientColor = ZeusTheme.sky.glow2, spotColor = ZeusTheme.sky.glow2)
                        .clip(ZeusShapes.circle)
                        .background(rememberPrimaryGradient(animate = false))
                        .clickable(role = Role.Button, onClick = onOpenSettings)
                        .semantics { contentDescription = "Profile and settings" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = colors.textOnAccent, modifier = Modifier.size(ZeusSizes.iconMd))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xxs)) {
                    Text(greeting, style = type.caption, color = colors.textOnSkySecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Image(
                        painter = painterResource(id = R.drawable.zeus_home_banner),
                        contentDescription = "ZeusReader",
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        modifier = Modifier
                            .height(ZeusSizes.bannerHeight)
                            .aspectRatio(BANNER_ASPECT, matchHeightConstraintsFirst = true)
                    )
                }
                StickerChip(
                    text = "${streakDays}-day streak",
                    icon = Icons.Rounded.Bolt,
                    rotation = 3f,
                    onClick = onOpenStats
                )
            }
        }

        // Hero stage: floating 3D cover + charge ring
        item(key = "hero") {
            HeroStage(
                book = continueBook,
                totalBooks = totalBooks,
                booksInProgress = booksInProgress,
                onOpenBook = onOpenBook,
                onOpenLibrary = onOpenLibrary,
                onOpenStats = onOpenStats
            )
        }

        // Resume card
        item(key = "resume") {
            GlassSurface(
                modifier = Modifier
                    .padding(horizontal = ZeusSpacing.gutter)
                    .widthIn(max = ZeusSizes.contentMaxWidth)
                    .fillMaxWidth(),
                neon = true,
                glassEnabled = glassEnabled
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ZeusSpacing.cardPadding, vertical = ZeusSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
                ) {
                    if (continueBook != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                            MetaLabel(
                                text = continueBook.primaryFormat.ifBlank { "Book" } + " · " + continueBook.category.ifBlank { "General" },
                                accent = true,
                                modifier = Modifier.weight(1f)
                            )
                            MetaLabel(text = continueBook.progressLabel())
                        }
                        Text(
                            text = continueBook.title,
                            style = type.headline,
                            color = colors.textOnSky,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (continueBook.authors.isNotBlank()) {
                            Text(
                                text = continueBook.authors,
                                style = type.quoteSmall,
                                color = colors.textOnSkySecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        PrimaryPillButton(
                            label = "Jump back in",
                            icon = Icons.AutoMirrored.Rounded.ArrowForward,
                            onClick = { onOpenBook(continueBook) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        MetaLabel(text = "Your reading sky", accent = true)
                        Text("Start your sanctuary", style = type.headline, color = colors.textOnSky)
                        Text(
                            text = "Import EPUB, PDF, CBZ or XPS books into Olympus, or find one online.",
                            style = type.body,
                            color = colors.textOnSkySecondary
                        )
                        PrimaryPillButton(
                            label = "Open Olympus",
                            icon = Icons.Rounded.FileOpen,
                            leadingIcon = true,
                            onClick = onOpenLibrary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Quick actions
        item(key = "quick") {
            Row(
                modifier = Modifier
                    .padding(horizontal = ZeusSpacing.gutter)
                    .widthIn(max = ZeusSizes.contentMaxWidth)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
            ) {
                QuickActionTile(Icons.Rounded.AutoStories, "Olympus", onOpenLibrary, Modifier.weight(1f), accent = true)
                QuickActionTile(Icons.Rounded.CloudDownload, "Online books", onOpenOnline, Modifier.weight(1f))
                QuickActionTile(Icons.Rounded.Translate, "Offline packs", onOpenPrepareOffline, Modifier.weight(1f))
                QuickActionTile(Icons.Rounded.Insights, "Stats", onOpenStats, Modifier.weight(1f))
            }
        }

        // Bento: resurfaced spark + words waiting
        item(key = "bento") {
            Row(
                modifier = Modifier
                    .padding(horizontal = ZeusSpacing.gutter)
                    .widthIn(max = ZeusSizes.contentMaxWidth)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
            ) {
                GlassSurface(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight()
                        .heightIn(min = ZeusSizes.bentoMinHeight),
                    shape = ZeusShapes.cardCompact,
                    glassEnabled = glassEnabled
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onOpenPassages)
                            .padding(ZeusSpacing.mdPlus),
                        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)
                    ) {
                        MetaLabel(text = if (featuredSparkText != null) "A spark resurfaced" else "Sparks", accent = true)
                        Text(
                            text = featuredSparkText?.let { "“$it”" }
                                ?: "Save lines while reading — they come back here like stars.",
                            style = type.quoteSmall,
                            color = colors.textOnSky,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                WordsSticker(
                    count = dueVocabWords,
                    onClick = onOpenVocab,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = ZeusSizes.bentoMinHeight)
                )
            }
        }

        // Shelf of other books
        if (shelf.isNotEmpty()) {
            item(key = "shelf_header") {
                ZeusSectionHeader(
                    title = "On your shelf",
                    actionLabel = "See all",
                    onAction = onOpenLibrary,
                    modifier = Modifier
                        .padding(horizontal = ZeusSpacing.gutter)
                        .widthIn(max = ZeusSizes.contentMaxWidth)
                )
            }
            item(key = "shelf") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = ZeusSpacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
                ) {
                    items(shelf, key = { it.editionId }) { book ->
                        ShelfBook(book = book, onClick = { onOpenBook(book) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStage(
    book: LibraryBookSummary?,
    totalBooks: Int,
    booksInProgress: Int,
    onOpenBook: (LibraryBookSummary) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenStats: () -> Unit
) {
    val sky = ZeusTheme.sky
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val reduceMotion = ZeusTheme.reduceMotion
    val bob = if (!reduceMotion) {
        val transition = rememberInfiniteTransition(label = "home_cover_bob")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(ZeusTheme.motion.COVER_BOB_MS), RepeatMode.Reverse),
            label = "cover_bob"
        )
    } else {
        null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ZeusSizes.heroStageHeight),
        contentAlignment = Alignment.Center
    ) {
        // Accent glow halo behind the book
        if (!ZeusTheme.paper.isEink) {
            Box(
                modifier = Modifier
                    .size(ZeusSizes.glowBlob)
                    .background(Brush.radialGradient(listOf(sky.accent.copy(alpha = if (colors.isLight) 0.30f else 0.34f), Color.Transparent)))
            )
        }
        Box(
            modifier = Modifier.size(width = ZeusSizes.coverHeroWidth + ZeusSizes.chargeRing - ZeusSpacing.sm, height = ZeusSizes.coverHeroHeight),
        ) {
            val coverMod = Modifier
                .align(Alignment.CenterStart)
                .size(width = ZeusSizes.coverHeroWidth, height = ZeusSizes.coverHeroHeight)
                .graphicsLayer {
                    val p = bob?.value ?: 0.5f
                    translationY = -8f * density * p
                    rotationY = -16f + 3f * p
                    rotationX = 5f - 2f * p
                    cameraDistance = 14f * density
                }
                .shadow(ZeusSizes.shadowCover, ZeusShapes.coverLarge, ambientColor = ZeusTheme.colors.shadow, spotColor = ZeusTheme.colors.shadow)
            if (book != null) {
                BookCover(
                    title = book.title,
                    authors = book.authors,
                    format = if (book.isImageBook) "IMG" else book.primaryFormat,
                    coverPath = book.coverPath,
                    shape = ZeusShapes.coverLarge,
                    modifier = coverMod
                        .clickable(role = Role.Button) { onOpenBook(book) }
                        .semantics { contentDescription = "Open ${book.title}" }
                )
            } else {
                BookCover(
                    title = "Your Reading Sky",
                    authors = "ZeusReader",
                    format = "Start",
                    shape = ZeusShapes.coverLarge,
                    modifier = coverMod.clickable(role = Role.Button, onClick = onOpenLibrary)
                )
            }

            // Charge ring
            val bookProgress = book?.knownProgress()
            val ringProgress = bookProgress
                ?: if (totalBooks > 0) booksInProgress.toFloat() / totalBooks.toFloat() else 0f
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(ZeusSizes.chargeRing)
                    .shadow(ZeusSizes.shadowSoft, ZeusShapes.circle)
                    .clip(ZeusShapes.circle)
                    .background(colors.sheetBackground)
                    .clickable(role = Role.Button, onClick = onOpenStats)
                    .semantics {
                        contentDescription = if (bookProgress != null) {
                            "${(bookProgress * 100).toInt()} percent of this book read"
                        } else {
                            "$booksInProgress of $totalBooks books in progress"
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                ProgressRing(progress = ringProgress, size = ZeusSizes.chargeRing, stroke = ZeusSizes.chargeRingStroke) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (bookProgress != null) "${(bookProgress * 100).toInt()}%" else "$booksInProgress",
                            style = type.numberMedium,
                            color = colors.textOnSky,
                            maxLines = 1
                        )
                        Text(
                            text = if (bookProgress != null) "read" else "of $totalBooks",
                            style = type.metaSmall,
                            color = colors.textOnSkySecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordsSticker(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Box(modifier = modifier.rotate(-2f)) {
        if (!ZeusTheme.paper.isEink) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(start = ZeusSizes.stickerOffset, top = ZeusSizes.stickerOffset)
                    .background(colors.neonB, ZeusShapes.cardCompact)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = ZeusSizes.stickerOffset, bottom = ZeusSizes.stickerOffset)
                .clip(ZeusShapes.cardCompact)
                .background(colors.stickerLightBg)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(ZeusSpacing.mdPlus),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$count", style = type.displayNumber, color = colors.stickerInk, maxLines = 1)
            Text(
                text = if (count == 1) "word waiting for you" else "words waiting for you",
                style = type.sticker,
                color = colors.stickerInk,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ShelfBook(book: LibraryBookSummary, onClick: () -> Unit) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Column(
        modifier = Modifier
            .width(ZeusSizes.coverFlowSideWidth)
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
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        )
        Text(book.title, style = type.labelSmall, color = colors.textOnSky, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(book.progressLabel(), style = type.metaSmall, color = colors.textOnSkySecondary, maxLines = 1)
    }
}

/** Kept for source compatibility — delegates to the shared [BookCover]. */
@Composable
fun BookCoverThumbnail(
    title: String,
    format: String,
    coverPath: String?,
    modifier: Modifier = Modifier
) {
    BookCover(title = title, format = format, coverPath = coverPath, modifier = modifier)
}

