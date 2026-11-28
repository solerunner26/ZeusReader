package com.solerunner26.zeusreader.ui.stats

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.reader.prefs.AppUiConfig
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.ProgressRing
import com.solerunner26.zeusreader.ui.components.StatTile
import com.solerunner26.zeusreader.ui.components.StickerChip
import com.solerunner26.zeusreader.ui.components.ZeusTopBar
import com.solerunner26.zeusreader.ui.components.rememberNavBarClearance
import com.solerunner26.zeusreader.ui.theme.GlassSurface
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") uiConfig: AppUiConfig = AppUiConfig(),
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsContent(state = state, onBack = onBack, modifier = modifier)
}

@Composable
internal fun StatsContent(state: ReadingStatsUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        ZeusTopBar(title = "Reading stats", subtitle = "Your charge, streak and harvest", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ZeusSpacing.gutter)
                .padding(bottom = rememberNavBarClearance())
                .widthIn(max = ZeusSizes.contentMaxWidth)
                .testTag("stats_screen"),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.lg)
        ) {
            GlassSurface(modifier = Modifier.fillMaxWidth(), neon = true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ZeusSpacing.cardPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.lg)
                ) {
                    val progress = if (state.totalBooks > 0) state.booksInProgress.toFloat() / state.totalBooks else 0f
                    ProgressRing(progress = progress, size = ZeusSizes.chargeRing, stroke = ZeusSizes.chargeRingStroke) {
                        Text("${state.currentStreakDays}", style = type.displayNumber, color = colors.textOnSky, maxLines = 1)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                        MetaLabel(text = "Current streak", accent = true)
                        Text(
                            text = if (state.currentStreakDays == 1) "1 day of lightning" else "${state.currentStreakDays} days of lightning",
                            style = type.headline,
                            color = colors.textOnSky,
                            maxLines = 2
                        )
                        Text("${state.activeReadingDays} active reading days recorded", style = type.caption, color = colors.textOnSkySecondary)
                    }
                }
            }
            StickerChip(text = "Keep the charge alive today", icon = Icons.Rounded.Bolt, rotation = -2f, cream = true)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
                StatTile(value = "${state.totalBooks}", label = "Books in Olympus", caption = "${state.booksInProgress} in progress", icon = Icons.Rounded.AutoStories, modifier = Modifier.weight(1f))
                StatTile(value = "${state.totalPassages}", label = "Sparks saved", caption = "Offline quotes", icon = Icons.Rounded.AutoAwesome, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
                StatTile(value = "${state.totalVocabWords}", label = "Words collected", caption = "${state.dueVocabWords} due for review", icon = Icons.Rounded.Translate, modifier = Modifier.weight(1f))
                StatTile(value = "${state.activeReadingDays}", label = "Reading days", caption = "All time", icon = Icons.Rounded.Bolt, modifier = Modifier.weight(1f))
            }
        }
    }
}
