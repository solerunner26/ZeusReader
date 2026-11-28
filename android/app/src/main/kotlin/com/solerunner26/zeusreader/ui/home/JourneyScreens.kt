package com.solerunner26.zeusreader.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.solerunner26.zeusreader.ui.components.ArtifactStage
import com.solerunner26.zeusreader.ui.components.BoltMascot
import com.solerunner26.zeusreader.ui.components.CappedFontScale
import com.solerunner26.zeusreader.ui.components.CircleIconButton
import com.solerunner26.zeusreader.ui.components.CrownArtifact
import com.solerunner26.zeusreader.ui.components.Eyebrow
import com.solerunner26.zeusreader.ui.components.FlameArtifact
import com.solerunner26.zeusreader.ui.components.GradientCard
import com.solerunner26.zeusreader.ui.components.IconBox
import com.solerunner26.zeusreader.ui.components.MenuRow
import com.solerunner26.zeusreader.ui.components.PagerDots
import com.solerunner26.zeusreader.ui.components.PrimaryButton
import com.solerunner26.zeusreader.ui.components.ProgressBar
import com.solerunner26.zeusreader.ui.components.Starburst
import com.solerunner26.zeusreader.ui.components.Tone
import com.solerunner26.zeusreader.ui.components.TrophyArtifact
import com.solerunner26.zeusreader.ui.components.ZeusCard
import com.solerunner26.zeusreader.ui.components.ZeusDialog
import com.solerunner26.zeusreader.ui.components.ZeusDivider
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.components.ZeusTopBar
import com.solerunner26.zeusreader.ui.components.floating
import com.solerunner26.zeusreader.ui.components.radialGlow
import com.solerunner26.zeusreader.ui.components.rememberNavBarClearance
import com.solerunner26.zeusreader.ui.components.rememberNavClearance
import com.solerunner26.zeusreader.ui.components.staggerIn
import com.solerunner26.zeusreader.ui.stats.ReadingStatsUiState
import com.solerunner26.zeusreader.ui.theme.ZeusMotion
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.ZeusType
import com.solerunner26.zeusreader.ui.theme.rememberAccentBrush

/* =============================================================================================
 * Onboarding (first run)
 * =========================================================================================== */

private data class Slide(val title: String, val body: String)

private val slides = listOf(
    Slide("Meet Bolt, your reading buddy", "EPUB, PDF, comics and scans in one calm library that works offline."),
    Slide("Tap any word. Get it in three languages.", "English, हिन्दी and ગુજરાતી meanings, translation and read-aloud — all on your phone."),
    Slide("Build a streak you're proud of", "Daily goals, word quizzes and badges keep you coming back.")
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    var page by remember { mutableIntStateOf(0) }
    val reduce = ZeusTheme.reduceMotion
    LaunchedEffect(page, reduce) {
        if (!reduce && page < slides.lastIndex) {
            kotlinx.coroutines.delay(ZeusMotion.CAROUSEL_MS.toLong() + 1200L)
            page = (page + 1).coerceAtMost(slides.lastIndex)
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = ZeusSpacing.gutter, vertical = ZeusSpacing.md)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.lg)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            com.solerunner26.zeusreader.ui.components.TextAction("Skip", onFinish)
        }
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (reduce) fadeIn(ZeusMotion.settle(true)) togetherWith fadeOut(ZeusMotion.settle(true))
                else (slideInHorizontally(ZeusMotion.bouncy(false)) { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "onboarding_slide"
        ) { p ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md, Alignment.CenterVertically)
            ) {
                ArtifactStage(ZeusSizes.stage) {
                    when (p) {
                        0 -> {
                            BoltMascot(ZeusSizes.mascotLarge)
                            CrownArtifact(ZeusSizes.artifactMedium, Modifier.align(Alignment.TopStart).offset(x = ZeusSpacing.sm, y = ZeusSpacing.huge).floating(rotation = -12f))
                            FlameArtifact(ZeusSizes.artifactSmall, Modifier.align(Alignment.TopEnd).offset(x = -ZeusSpacing.sm, y = ZeusSpacing.giant).floating())
                        }
                        1 -> ZeusCard(Modifier.widthIn(max = ZeusSizes.stage - ZeusSpacing.huge).rotate(-4f)) {
                            Eyebrow("Dictionary")
                            Text("loam", style = type.title, color = c.ink)
                            Text("दोमट मिट्टी", style = type.headline.copy(fontFamily = ZeusType.NotoSansDevanagari), color = c.ink)
                            Text("ગોરાડુ માટી", style = type.headline.copy(fontFamily = ZeusType.NotoSansGujarati), color = c.ink)
                        }
                        else -> {
                            TrophyArtifact(ZeusSizes.trophy)
                            FlameArtifact(ZeusSizes.artifactMedium, Modifier.align(Alignment.TopStart).offset(x = ZeusSpacing.xl, y = ZeusSpacing.huge).floating())
                        }
                    }
                }
                Text(slides[p].title, style = type.title, color = c.ink, textAlign = TextAlign.Center)
                Text(slides[p].body, style = type.body, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = ZeusSizes.contentMaxWidth))
            }
        }
        PagerDots(slides.size, page, onSelect = { page = it })
        PrimaryButton(
            label = if (page == slides.lastIndex) "Get started" else "Next",
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = { if (page == slides.lastIndex) onFinish() else page++ },
            modifier = Modifier.fillMaxWidth().widthIn(max = ZeusSizes.contentMaxWidth)
        )
    }
}

/* =============================================================================================
 * Streak
 * =========================================================================================== */

private val weekLetters = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun StreakScreen(stats: ReadingStatsUiState, onBack: () -> Unit, onSetGoal: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    var showGoal by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = rememberNavBarClearance())
            .testTag("streak_screen"),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.section)
    ) {
        // Full-bleed gradient header with fading starburst
        Box(
            Modifier
                .fillMaxWidth()
                .clip(ZeusShapes.banner)
                .background(rememberAccentBrush(shine = true))
        ) {
            Starburst(Modifier.matchParentSize())
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = ZeusSpacing.gutter, vertical = ZeusSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                }
                FlameArtifact(ZeusSizes.flameHero, Modifier.floating())
                CappedFontScale(1.3f) {
                    Text("${stats.currentStreakDays}", style = type.hero, color = c.onAccent)
                }
                Text(if (stats.currentStreakDays == 1) "day reading streak" else "day reading streak", style = type.bodyStrong, color = c.onAccent)
            }
        }
        Column(Modifier.padding(horizontal = ZeusSpacing.gutter), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.section)) {
            ZeusCard(Modifier.fillMaxWidth().staggerIn(0)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("This week", style = type.label, color = c.ink, modifier = Modifier.weight(1f))
                    Text("${stats.weekActive.count { it }} of 7", style = type.caption, color = c.muted)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    stats.weekActive.forEachIndexed { i, done ->
                        val today = i == stats.todayIndex
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)) {
                            Box(
                                modifier = Modifier
                                    .size(ZeusSizes.dayDot)
                                    .clip(ZeusShapes.circle)
                                    .then(
                                        when {
                                            done -> Modifier.background(rememberAccentBrush())
                                            today -> Modifier.border(ZeusSpacing.selectedBorderWidth, c.accent, ZeusShapes.circle)
                                            else -> Modifier.border(ZeusSpacing.borderWidth, c.line, ZeusShapes.circle)
                                        }
                                    )
                                    .semantics { stateDescription = if (done) "read" else "not yet" },
                                contentAlignment = Alignment.Center
                            ) {
                                if (done) Icon(Icons.Rounded.Check, null, tint = c.onAccent, modifier = Modifier.size(ZeusSizes.iconSm))
                                else if (today) FlameArtifact(ZeusSizes.iconSm)
                            }
                            Text(weekLetters[i], style = type.tiny, color = c.muted)
                        }
                    }
                }
            }
            ZeusCard(Modifier.fillMaxWidth().staggerIn(1), padding = PaddingValues(ZeusSpacing.none)) {
                Box(Modifier.fillMaxWidth().background(c.tint).heightIn(min = ZeusSizes.mascotMedium + ZeusSpacing.sm)) {
                    BoltMascot(ZeusSizes.mascotMedium, Modifier.align(Alignment.BottomEnd).offset(x = ZeusSpacing.xsPlus, y = ZeusSpacing.md))
                    Column(Modifier.fillMaxWidth(0.64f).padding(ZeusSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                        Text(
                            if (stats.goalReached) "Today's goal is done!" else "Read ${(stats.dailyGoalMinutes - stats.todayMinutes).coerceAtLeast(1)} more minutes",
                            style = type.headline,
                            color = c.ink
                        )
                        Text(
                            if (stats.bestStreakDays > stats.currentStreakDays) "You're ${stats.bestStreakDays - stats.currentStreakDays} days away from your record." else "You're on your best streak ever. Keep it glowing.",
                            style = type.caption,
                            color = c.muted
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).staggerIn(2), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
                ZeusCard(Modifier.weight(1f).fillMaxHeight()) {
                    Text("Best streak", style = type.caption, color = c.muted)
                    Text("${stats.bestStreakDays} days", style = type.numberSmall, color = c.ink)
                }
                ZeusCard(Modifier.weight(1f).fillMaxHeight(), onClick = { showGoal = true }) {
                    Text("Daily goal", style = type.caption, color = c.muted)
                    Text("${stats.dailyGoalMinutes} min", style = type.numberSmall, color = c.ink)
                    Text("Tap to change", style = type.tiny, color = c.accentInk)
                }
            }
        }
    }
    if (showGoal) {
        GoalDialog(current = stats.dailyGoalMinutes, onDismiss = { showGoal = false }, onSave = { onSetGoal(it); showGoal = false })
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun GoalDialog(current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var minutes by remember { mutableIntStateOf(current) }
    ZeusDialog(onDismissRequest = onDismiss, title = "Daily reading goal", confirmLabel = "Save", onConfirm = { onSave(minutes) }) {
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
            listOf(10, 15, 20, 30, 45, 60).forEach { m ->
                com.solerunner26.zeusreader.ui.components.ZeusChip("$m min", minutes == m, { minutes = m })
            }
        }
    }
}

/* =============================================================================================
 * Activity feed
 * =========================================================================================== */

data class ActivityItem(val title: String, val body: String, val tone: Tone, val icon: androidx.compose.ui.graphics.vector.ImageVector, val action: () -> Unit)

@Composable
fun ActivityScreen(items: List<ActivityItem>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val c = ZeusTheme.colors
    Column(modifier.fillMaxSize().statusBarsPadding().testTag("activity_screen")) {
        ZeusTopBar("Activity", subtitle = "What's waiting for you", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = ZeusSpacing.gutter, end = ZeusSpacing.gutter, bottom = rememberNavBarClearance()),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
        ) {
            if (items.isEmpty()) {
                item {
                    com.solerunner26.zeusreader.ui.components.EmptyState(title = "All quiet", message = "Read a little, save highlights and words — your updates will appear here.")
                }
            }
            items.forEachIndexed { i, it ->
                item(key = "a$i") {
                    ZeusCard(Modifier.fillMaxWidth().staggerIn(i), onClick = it.action) {
                        MenuRow(title = it.title, subtitle = it.body, icon = it.icon, tone = it.tone, chevron = true)
                    }
                }
            }
        }
    }
}

/* =============================================================================================
 * Profile hub ("Me" tab)
 * =========================================================================================== */

@Composable
fun ProfileScreen(
    stats: ReadingStatsUiState,
    packLabel: String,
    onEditName: (String) -> Unit,
    onOpenGoal: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenHighlights: () -> Unit,
    onOpenOffline: () -> Unit,
    onOpenOnline: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    var editing by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ZeusSpacing.gutter)
            .padding(top = ZeusSpacing.md, bottom = rememberNavClearance())
            .testTag("profile_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.section)
    ) {
        Box(Modifier.radialGlow(c.glow, 0.9f).padding(ZeusSpacing.md), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(ZeusSizes.avatarXl)
                    .clip(ZeusShapes.circle)
                    .background(rememberAccentBrush())
                    .padding(ZeusSpacing.xs)
                    .clip(ZeusShapes.circle)
                    .background(c.card),
                contentAlignment = Alignment.Center
            ) {
                CappedFontScale(1.2f) { Text(stats.displayName.ifBlank { "Z" }.take(1).uppercase(), style = type.display, color = c.accentInk) }
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(ZeusSpacing.touchMin)
                    .clip(ZeusShapes.circle)
                    .clickable(role = Role.Button) { editing = true }
                    .semantics { contentDescription = "Edit name" },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(ZeusSpacing.xxxl - ZeusSpacing.xxs).clip(ZeusShapes.circle).background(c.ink), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Edit, null, tint = c.canvas, modifier = Modifier.size(ZeusSizes.iconSm))
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stats.displayName.ifBlank { "Reader" }, style = type.headline, color = c.ink)
            Text("${stats.activeReadingDays} reading days · best streak ${stats.bestStreakDays}", style = type.caption, color = c.muted, textAlign = TextAlign.Center)
        }
        ZeusCard(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = ZeusSpacing.md)) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                TrioCell("${stats.totalBooks}", "Books", Modifier.weight(1f))
                Box(Modifier.fillMaxHeight().padding(vertical = ZeusSpacing.xs).size(width = ZeusSpacing.hairline, height = ZeusSpacing.xxl).background(c.line))
                TrioCell(hoursLabel(stats.totalMinutes), "Read", Modifier.weight(1f))
                Box(Modifier.fillMaxHeight().padding(vertical = ZeusSpacing.xs).size(width = ZeusSpacing.hairline, height = ZeusSpacing.xxl).background(c.line))
                TrioCell("${stats.totalVocabWords}", "Words", Modifier.weight(1f))
            }
        }
        ZeusCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = ZeusSpacing.cardPadding, vertical = ZeusSpacing.xs)) {
            MenuRow("Reading goal", icon = Icons.Rounded.TrackChanges, value = "${stats.dailyGoalMinutes} min", chevron = true, onClick = onOpenGoal)
            ZeusDivider()
            MenuRow("Stats & badges", icon = Icons.Rounded.Insights, chevron = true, onClick = onOpenStats)
            ZeusDivider()
            MenuRow("Highlights", icon = Icons.Rounded.AutoAwesome, value = "${stats.totalPassages}", chevron = true, onClick = onOpenHighlights)
            ZeusDivider()
            MenuRow("Theme & settings", icon = Icons.Rounded.Palette, value = packLabel, chevron = true, onClick = onOpenSettings)
            ZeusDivider()
            MenuRow("Offline language packs", icon = Icons.Rounded.CloudDownload, chevron = true, onClick = onOpenOffline)
            ZeusDivider()
            MenuRow("Online books", icon = Icons.Rounded.Public, chevron = true, onClick = onOpenOnline)
            ZeusDivider()
            MenuRow("Help & support", icon = Icons.AutoMirrored.Rounded.HelpOutline, chevron = true, onClick = onOpenHelp)
        }
    }
    if (editing) {
        var name by remember { mutableStateOf(stats.displayName) }
        ZeusDialog(onDismissRequest = { editing = false }, title = "Your name", confirmLabel = "Save", onConfirm = { onEditName(name); editing = false }) {
            ZeusTextField(value = name, onValueChange = { name = it }, placeholder = "What should Bolt call you?", modifier = Modifier.fillMaxWidth())
        }
    }
}

internal fun hoursLabel(minutes: Int): String = if (minutes < 60) "${minutes}m" else "${minutes / 60}h"

@Composable
private fun TrioCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = ZeusTheme.type.numberSmall, color = ZeusTheme.colors.ink, maxLines = 1)
        Text(label, style = ZeusTheme.type.caption, color = ZeusTheme.colors.muted, maxLines = 1)
    }
}

/* =============================================================================================
 * Help & support
 * =========================================================================================== */

private data class Faq(val q: String, val a: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val faqs = listOf(
    Faq("How do I read offline in Hindi or Gujarati?", "Open Me → Offline language packs and download the pack once. Dictionary and translation then work in airplane mode.", Icons.Rounded.CloudDownload),
    Faq("Which file types can I import?", "EPUB, PDF, CBZ comics, XPS, HTML and folders of JPG/PNG pages. Watched folders import new files automatically.", Icons.AutoMirrored.Rounded.MenuBook),
    Faq("How does my streak work?", "Read any day — opening a book, saving a highlight or a word counts. Reach your daily goal to fill today's flame.", Icons.Rounded.AutoAwesome),
    Faq("How do I look up a word?", "In the reader tap Dictionary, or switch a PDF to Text view and tap any word. Save it to Words to quiz yourself later.", Icons.Rounded.Translate),
    Faq("How do I change the colours?", "Me → Theme & settings. Pick one of five theme packs and Day, Night or Auto.", Icons.Rounded.Palette),
    Faq("Is my reading private?", "Yes. Books, highlights and words stay on your phone. Online books and sync only run when you use them.", Icons.Rounded.Settings)
)

@Composable
fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    var query by remember { mutableStateOf("") }
    val open = remember { mutableStateListOf(0) }
    val filtered = faqs.withIndex().filter { query.isBlank() || it.value.q.contains(query, true) || it.value.a.contains(query, true) }
    Column(modifier.fillMaxSize().statusBarsPadding().testTag("help_screen")) {
        ZeusTopBar("Help", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = ZeusSpacing.gutter, end = ZeusSpacing.gutter, bottom = rememberNavBarClearance()),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
        ) {
            item {
                GradientCard(Modifier.fillMaxWidth(), padding = PaddingValues(ZeusSpacing.none)) {
                    Box(Modifier.fillMaxWidth().heightIn(min = ZeusSizes.mascotMedium + ZeusSpacing.lg)) {
                        BoltMascot(ZeusSizes.mascotMedium, Modifier.align(Alignment.BottomEnd).offset(x = ZeusSpacing.xs, y = ZeusSpacing.sm))
                        Column(Modifier.fillMaxWidth(0.64f).padding(ZeusSpacing.gutter), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                            Eyebrow("Ask Bolt", color = c.onAccent)
                            Text("How can we help you read better today?", style = type.headline, color = c.onAccent)
                        }
                    }
                }
            }
            item { ZeusTextField(query, { query = it }, placeholder = "Search help topics", leadingIcon = Icons.Rounded.Search, modifier = Modifier.fillMaxWidth()) }
            filtered.forEach { (i, f) ->
                item(key = "faq$i") {
                    val expanded = i in open
                    val rot by animateFloatAsState(if (expanded) 90f else 0f, ZeusMotion.bouncy(ZeusTheme.reduceMotion), label = "faq_chev")
                    ZeusCard(Modifier.fillMaxWidth().animateContentSize(), padding = PaddingValues(ZeusSpacing.md)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = ZeusSpacing.touchMin)
                                .clip(ZeusShapes.tile)
                                .clickable(role = Role.Button) { if (expanded) open.remove(i) else open.add(i) }
                                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
                        ) {
                            IconBox(f.icon, size = ZeusSizes.iconBoxSmall)
                            Text(f.q, style = type.label, color = c.ink, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.muted, modifier = Modifier.rotate(rot))
                        }
                        if (expanded) Text(f.a, style = type.body, color = c.muted)
                    }
                }
            }
        }
    }
}

