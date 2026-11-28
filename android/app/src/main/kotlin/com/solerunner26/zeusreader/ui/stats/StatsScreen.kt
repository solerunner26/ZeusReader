package com.solerunner26.zeusreader.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.reader.prefs.AppUiConfig
import com.solerunner26.zeusreader.ui.components.CrownArtifact
import com.solerunner26.zeusreader.ui.components.FlameArtifact
import com.solerunner26.zeusreader.ui.components.MenuRow
import com.solerunner26.zeusreader.ui.components.ProgressBar
import com.solerunner26.zeusreader.ui.components.ProgressRing
import com.solerunner26.zeusreader.ui.components.SectionCard
import com.solerunner26.zeusreader.ui.components.Tone
import com.solerunner26.zeusreader.ui.components.TrophyArtifact
import com.solerunner26.zeusreader.ui.components.ZeusCard
import com.solerunner26.zeusreader.ui.components.ZeusDivider
import com.solerunner26.zeusreader.ui.components.ZeusTopBar
import com.solerunner26.zeusreader.ui.components.radialGlow
import com.solerunner26.zeusreader.ui.components.rememberNavBarClearance
import com.solerunner26.zeusreader.ui.components.staggerIn
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") uiConfig: AppUiConfig = AppUiConfig(),
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    StatsContent(state = state, onBack = onBack, modifier = modifier)
}

private data class Badge(val title: String, val current: Int, val target: Int, val art: @Composable () -> Unit)

@Composable
internal fun StatsContent(state: ReadingStatsUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    val today = LocalDate.now()
    val monthGoal = state.dailyGoalMinutes * today.lengthOfMonth()
    val monthProgress = if (monthGoal > 0) state.monthMinutes.toFloat() / monthGoal else 0f
    val badges = listOf(
        Badge("Bookworm · 10 books", state.totalBooks, 10) { CrownArtifact(ZeusSizes.artifactSmall) },
        Badge("On fire · 30-day streak", state.currentStreakDays, 30) { FlameArtifact(ZeusSizes.artifactSmall) },
        Badge("Word collector · 50 words", state.totalVocabWords, 50) { CrownArtifact(ZeusSizes.artifactSmall) },
        Badge("Highlighter · 25 highlights", state.totalPassages, 25) { FlameArtifact(ZeusSizes.artifactSmall) },
        Badge("Marathon · 20 hours read", state.totalMinutes / 60, 20) { TrophyArtifact(ZeusSizes.artifactSmall, float = false) }
    )
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        ZeusTopBar("Your ${today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}", subtitle = "Reading stats & badges", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ZeusSpacing.gutter)
                .padding(bottom = rememberNavBarClearance())
                .testTag("stats_screen"),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.section)
        ) {
            ZeusCard(Modifier.fillMaxWidth().staggerIn(0)) {
                Box(Modifier.fillMaxWidth().radialGlow(c.glow, 0.5f).padding(vertical = ZeusSpacing.sm), contentAlignment = Alignment.Center) {
                    ProgressRing(monthProgress) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(monthProgress * 100).toInt().coerceAtMost(999)}%", style = type.number, color = c.ink)
                            Text("of ${monthGoal / 60} h goal", style = type.caption, color = c.muted)
                        }
                    }
                }
            }
            ZeusCard(Modifier.fillMaxWidth().staggerIn(1)) {
                MenuRow("Time read this month", icon = Icons.Rounded.Schedule, value = minutesLabel(state.monthMinutes))
                ZeusDivider()
                MenuRow("Books in progress", icon = Icons.AutoMirrored.Rounded.MenuBook, tone = Tone.Info, value = "${state.booksInProgress} of ${state.totalBooks}")
                ZeusDivider()
                MenuRow("Highlights saved", icon = Icons.Rounded.AutoAwesome, tone = Tone.Rose, value = "${state.totalPassages}")
                ZeusDivider()
                MenuRow("Words collected", icon = Icons.Rounded.Translate, tone = Tone.Ok, value = "${state.totalVocabWords} · ${state.dueVocabWords} due")
            }
            SectionCard(title = "Badges", modifier = Modifier.staggerIn(2)) {
                badges.forEach { b ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
                        b.art()
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(b.title, style = type.label, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("${b.current.coerceAtMost(b.target)}/${b.target}", style = type.caption, color = c.muted)
                            }
                            ProgressBar(b.current.toFloat() / b.target, height = ZeusSizes.progressThin)
                        }
                    }
                }
            }
        }
    }
}

internal fun minutesLabel(minutes: Int): String = if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} m"
