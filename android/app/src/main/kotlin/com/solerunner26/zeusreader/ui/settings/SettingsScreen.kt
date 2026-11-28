package com.solerunner26.zeusreader.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.ViewDay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.reader.prefs.AppLanguage
import com.solerunner26.zeusreader.reader.prefs.AppTheme
import com.solerunner26.zeusreader.reader.prefs.AppUiConfig
import com.solerunner26.zeusreader.ui.components.MenuRow
import com.solerunner26.zeusreader.ui.components.PrimaryButton
import com.solerunner26.zeusreader.ui.components.SecondaryButton
import com.solerunner26.zeusreader.ui.components.SectionCard
import com.solerunner26.zeusreader.ui.components.SliderRow
import com.solerunner26.zeusreader.ui.components.SwitchRow
import com.solerunner26.zeusreader.ui.components.ZeusChip
import com.solerunner26.zeusreader.ui.components.ZeusDivider
import com.solerunner26.zeusreader.ui.components.ZeusSegmented
import com.solerunner26.zeusreader.ui.components.ZeusTopBar
import com.solerunner26.zeusreader.ui.components.rememberNavBarClearance
import com.solerunner26.zeusreader.ui.theme.AppUiConfigViewModel
import com.solerunner26.zeusreader.ui.theme.HuePreset
import com.solerunner26.zeusreader.ui.theme.ReaderSurface
import com.solerunner26.zeusreader.ui.theme.ZeusPack
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: AppUiConfigViewModel,
    onBack: () -> Unit,
    onOpenStats: () -> Unit = {},
    onOpenOnlineSearch: () -> Unit = {},
    updateState: AppUpdateUiState = AppUpdateUiState(),
    onCheckForUpdate: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val dark = when (config.appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    SettingsScreenContent(
        config = config,
        darkTheme = dark,
        onBack = onBack,
        onOpenStats = onOpenStats,
        onOpenOnlineSearch = onOpenOnlineSearch,
        updateState = updateState,
        onCheckForUpdate = onCheckForUpdate,
        onInstallUpdate = onInstallUpdate,
        onSelectAppTheme = viewModel::setAppTheme,
        onSelectLanguage = viewModel::setAppLanguage,
        onSelectPreset = viewModel::setPreset,
        onToggleHueFromCover = viewModel::setHueFromCover,
        onAuraIntensityChange = viewModel::setAuraIntensity,
        onSelectReaderSurface = viewModel::setReaderSurface,
        onToggleFocusRibbon = viewModel::setFocusRibbon,
        onToggleGlassUi = viewModel::setGlassUi,
        onToggleAestheticElements = viewModel::setAestheticElements,
        onToggleMasterAnimations = viewModel::setMasterAnimations,
        onSelectThemePack = viewModel::setThemePack,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreenContent(
    config: AppUiConfig,
    darkTheme: Boolean,
    onBack: () -> Unit,
    onOpenStats: () -> Unit = {},
    onOpenOnlineSearch: () -> Unit = {},
    updateState: AppUpdateUiState = AppUpdateUiState(),
    onCheckForUpdate: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onSelectAppTheme: (AppTheme) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    @Suppress("UNUSED_PARAMETER") onSelectPreset: (HuePreset) -> Unit,
    onToggleHueFromCover: (Boolean) -> Unit,
    onAuraIntensityChange: (Float) -> Unit,
    onSelectReaderSurface: (ReaderSurface) -> Unit,
    onToggleFocusRibbon: (Boolean) -> Unit,
    @Suppress("UNUSED_PARAMETER") onToggleGlassUi: (Boolean) -> Unit,
    onToggleAestheticElements: (Boolean) -> Unit,
    onToggleMasterAnimations: (Boolean) -> Unit,
    onSelectThemePack: (ZeusPack) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    val currentPack = ZeusPack.fromHue(config.themeHue)

    Column(modifier.fillMaxSize().statusBarsPadding()) {
        ZeusTopBar("Settings", subtitle = "Look, reading and updates", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag("settings_screen_root")
                .padding(horizontal = ZeusSpacing.gutter)
                .padding(bottom = rememberNavBarClearance()),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.section)
        ) {
            SectionCard(title = "Appearance") {
                ZeusSegmented(
                    options = listOf("Day", "Night", "Auto"),
                    icons = listOf(Icons.Rounded.LightMode, Icons.Rounded.DarkMode, Icons.Rounded.Brightness6),
                    selectedIndex = when (config.appTheme) {
                        AppTheme.LIGHT -> 0
                        AppTheme.DARK -> 1
                        AppTheme.SYSTEM -> 2
                    },
                    onSelect = { onSelectAppTheme(listOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.SYSTEM)[it]) }
                )
                Text("Theme pack · ${currentPack.label}", style = type.label, color = c.ink, modifier = Modifier.padding(top = ZeusSpacing.xs))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xxs)) {
                    ZeusPack.entries.forEach { pack ->
                        PackSwatch(pack, selected = pack == currentPack && !config.hueFromCover, onClick = { onSelectThemePack(pack) }, modifier = Modifier.weight(1f))
                    }
                }
                Text(if (darkTheme) "Night: obsidian canvas with a glowing accent." else "Day: warm alabaster canvas with soft shadows.", style = type.caption, color = c.muted)
            }

            SectionCard(title = "Language") {
                ZeusSegmented(
                    options = AppLanguage.entries.map { it.label },
                    selectedIndex = AppLanguage.entries.indexOf(config.appLanguage).coerceAtLeast(0),
                    onSelect = { onSelectLanguage(AppLanguage.entries[it]) }
                )
            }

            SectionCard(title = "Reading") {
                Text("Default page colour", style = type.label, color = c.ink)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                    ReaderSurface.entries.forEach { s -> ZeusChip(s.label, config.readerSurface == s, { onSelectReaderSurface(s) }, outline = config.readerSurface != s) }
                }
                ZeusDivider()
                SwitchRow("Focus ribbon", config.focusRibbon, onToggleFocusRibbon, subtitle = "Keep three lines bright, dim the rest", icon = Icons.Rounded.ViewDay)
            }

            SectionCard(title = "Delight") {
                SwitchRow("Celebrations", config.aestheticElements, onToggleAestheticElements, subtitle = "Confetti, Bolt and glows when you reach a goal", icon = Icons.Rounded.Celebration)
                ZeusDivider()
                SwitchRow("Motion", config.masterAnimations, onToggleMasterAnimations, subtitle = "Springs, shine and floating badges. Off = no animation.", icon = Icons.Rounded.AutoAwesome)
                ZeusDivider()
                SwitchRow("Match book cover", config.hueFromCover, onToggleHueFromCover, subtitle = "Pick the theme pack from the book you're reading", icon = Icons.Rounded.Palette)
                ZeusDivider()
                SliderRow("Background glow", "${(config.auraIntensity * 100).roundToInt()}%", config.auraIntensity, onAuraIntensityChange)
            }

            SectionCard(title = "Tools") {
                MenuRow("Reading stats", subtitle = "Streak, minutes, books and badges", icon = Icons.Rounded.Insights, chevron = true, onClick = onOpenStats)
                ZeusDivider()
                MenuRow("Online books", subtitle = "Search and download to your phone", icon = Icons.Rounded.CloudDownload, chevron = true, onClick = onOpenOnlineSearch)
            }

            SectionCard(title = "App version", selected = updateState.availableUpdate != null) {
                MenuRow("ZeusReader v${updateState.currentVersion}", subtitle = "Build ${updateState.currentVersionCode} · Sunset design", icon = Icons.Rounded.SystemUpdate)
                updateState.statusMessage?.let { Text(it, style = type.caption, color = c.accentInk) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                    SecondaryButton(if (updateState.isChecking) "Checking…" else "Check for updates", onCheckForUpdate, enabled = !updateState.isChecking && !updateState.isDownloading, modifier = Modifier.weight(1f))
                    updateState.availableUpdate?.let { pending ->
                        PrimaryButton(if (updateState.isDownloading) "${updateState.downloadProgress}%" else "Install ${pending.tagName}", onInstallUpdate, enabled = !updateState.isDownloading, height = ZeusSpacing.secondaryButtonHeight, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PackSwatch(pack: ZeusPack, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = ZeusTheme.colors
    Column(
        modifier
            .clip(ZeusShapes.tile)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = "${pack.label} theme pack"
            }
            .padding(ZeusSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)
    ) {
        Box(
            Modifier
                .size(ZeusSizes.swatch)
                .then(if (selected && !c.isEink) Modifier.shadow(ZeusSizes.shadowCard, ZeusShapes.circle, ambientColor = pack.g1, spotColor = pack.g1) else Modifier)
                .clip(ZeusShapes.circle)
                .background(Brush.linearGradient(listOf(pack.g1, pack.g2)))
                .border(if (selected) ZeusSpacing.selectedBorderWidth + ZeusSpacing.hairline else ZeusSpacing.selectedBorderWidth, if (selected) c.ink else c.card, ZeusShapes.circle),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = pack.onAccent, modifier = Modifier.size(ZeusSizes.iconMd))
        }
        Text(pack.label, style = ZeusTheme.type.tiny, color = if (selected) c.ink else c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

