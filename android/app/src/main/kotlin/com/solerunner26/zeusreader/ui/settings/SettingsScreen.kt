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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Texture
import androidx.compose.material.icons.rounded.ViewDay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.solerunner26.zeusreader.ui.components.GlassPillButton
import com.solerunner26.zeusreader.ui.components.GlassSection
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.PrimaryPillButton
import com.solerunner26.zeusreader.ui.components.ZeusChip
import com.solerunner26.zeusreader.ui.components.ZeusChipRow
import com.solerunner26.zeusreader.ui.components.ZeusDivider
import com.solerunner26.zeusreader.ui.components.ZeusListRow
import com.solerunner26.zeusreader.ui.components.ZeusSegmented
import com.solerunner26.zeusreader.ui.components.ZeusSliderRow
import com.solerunner26.zeusreader.ui.components.ZeusSwitchRow
import com.solerunner26.zeusreader.ui.components.ZeusTopBar
import com.solerunner26.zeusreader.ui.components.rememberNavBarClearance
import com.solerunner26.zeusreader.ui.theme.AppUiConfigViewModel
import com.solerunner26.zeusreader.ui.theme.HuePreset
import com.solerunner26.zeusreader.ui.theme.ReaderSurface
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSky
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.neonBorder
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
        onToggleAnimatedBorders = viewModel::setAnimatedBorders,
        onToggleAnimatedGlare = viewModel::setAnimatedGlare,
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
    onSelectPreset: (HuePreset) -> Unit,
    onToggleHueFromCover: (Boolean) -> Unit,
    onAuraIntensityChange: (Float) -> Unit,
    onSelectReaderSurface: (ReaderSurface) -> Unit,
    onToggleFocusRibbon: (Boolean) -> Unit,
    onToggleGlassUi: (Boolean) -> Unit,
    onToggleAestheticElements: (Boolean) -> Unit,
    onToggleMasterAnimations: (Boolean) -> Unit,
    onSelectThemePack: (ZeusSky) -> Unit = { sky -> HuePreset.entries.firstOrNull { ZeusSky.fromHue(it.hue) == sky }?.let(onSelectPreset) },
    onToggleAnimatedBorders: (Boolean) -> Unit = {},
    onToggleAnimatedGlare: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val currentPack = ZeusSky.fromHue(config.themeHue)

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        ZeusTopBar(title = "Settings", subtitle = "Theme packs, reading, effects & updates", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag("settings_screen_root")
                .padding(bottom = rememberNavBarClearance()),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.section)
        ) {
            // ---- Theme packs ----
            Column(verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
                MetaLabel(text = "Theme packs", accent = true, modifier = Modifier.padding(horizontal = ZeusSpacing.gutter))
                ZeusChipRow {
                    ZeusSky.entries.forEach { sky ->
                        ThemePackCard(
                            sky = sky,
                            selected = sky == currentPack && !config.hueFromCover,
                            onClick = { onSelectThemePack(sky) }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = ZeusSpacing.gutter)
                    .widthIn(max = ZeusSizes.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(ZeusSpacing.section)
            ) {
                GlassSection(title = "Mood") {
                    ZeusSegmented(
                        options = listOf("Daylight", "Night", "Auto"),
                        selectedIndex = when (config.appTheme) {
                            AppTheme.LIGHT -> 0
                            AppTheme.DARK -> 1
                            AppTheme.SYSTEM -> 2
                        },
                        onSelect = { idx -> onSelectAppTheme(listOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.SYSTEM)[idx]) }
                    )
                    Text(
                        text = if (darkTheme) "Night: deep two-tone sky with neon glow." else "Daylight: pale two-tone sky with ink text.",
                        style = type.caption,
                        color = colors.textOnSkySecondary
                    )
                    MetaLabel(text = "App language", accent = true, modifier = Modifier.padding(top = ZeusSpacing.sm))
                    ZeusSegmented(
                        options = AppLanguage.entries.map { it.label },
                        selectedIndex = AppLanguage.entries.indexOf(config.appLanguage).coerceAtLeast(0),
                        onSelect = { onSelectLanguage(AppLanguage.entries[it]) }
                    )
                }

                GlassSection(title = "Sky") {
                    ZeusSwitchRow(
                        title = "Sky from book cover",
                        subtitle = "The app takes the weather of what you are reading",
                        icon = Icons.Rounded.Palette,
                        checked = config.hueFromCover,
                        onCheckedChange = onToggleHueFromCover
                    )
                    ZeusDivider()
                    ZeusSliderRow(
                        title = "Aurora glow",
                        valueLabel = "${(config.auraIntensity * 100).roundToInt()}%",
                        value = config.auraIntensity,
                        onValueChange = onAuraIntensityChange
                    )
                }

                GlassSection(title = "Reading") {
                    Text("Default reading paper", style = type.label, color = colors.textOnSky)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)
                    ) {
                        ReaderSurface.entries.forEach { surface ->
                            ZeusChip(label = surface.label, selected = config.readerSurface == surface, onClick = { onSelectReaderSurface(surface) })
                        }
                    }
                    ZeusDivider()
                    ZeusSwitchRow(
                        title = "Focus ribbon",
                        subtitle = "Keep a three-line reading band bright, dim the rest",
                        icon = Icons.Rounded.ViewDay,
                        checked = config.focusRibbon,
                        onCheckedChange = onToggleFocusRibbon
                    )
                }

                GlassSection(title = "Effects") {
                    ZeusSwitchRow("Glass UI", config.glassUi, onToggleGlassUi, subtitle = "Frosted glass cards; off = solid sky cards", icon = Icons.Rounded.BlurOn)
                    ZeusDivider()
                    ZeusSwitchRow("Neon & aurora", config.aestheticElements, onToggleAestheticElements, subtitle = "Neon borders, glows and drifting aurora", icon = Icons.Rounded.AutoAwesome)
                    ZeusDivider()
                    ZeusSwitchRow("Animated neon borders", config.animatedBorders, onToggleAnimatedBorders, subtitle = "Two-tone light sweeps around hero cards", icon = Icons.Rounded.Bolt, enabled = config.aestheticElements)
                    ZeusDivider()
                    ZeusSwitchRow("Gradient glare", config.animatedGlare, onToggleAnimatedGlare, subtitle = "Slow colour shimmer on buttons and titles", icon = Icons.Rounded.Texture)
                    ZeusDivider()
                    ZeusSwitchRow("Motion", config.masterAnimations, onToggleMasterAnimations, subtitle = "Off = no loops, no flashes, instant transitions", icon = Icons.Rounded.WbSunny)
                }

                GlassSection(title = "Tools & private server") {
                    ZeusListRow(title = "Reading stats", subtitle = "Streak, books, sparks and words", icon = Icons.Rounded.Insights, onClick = onOpenStats) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.textOnSkySecondary)
                    }
                    ZeusDivider()
                    ZeusListRow(title = "Online books", subtitle = "Search, queue and download to your phone", icon = Icons.Rounded.CloudDownload, onClick = onOpenOnlineSearch) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.textOnSkySecondary)
                    }
                }

                GlassSection(title = "App version & updates", neon = updateState.availableUpdate != null) {
                    ZeusListRow(
                        title = "ZeusReader v${updateState.currentVersion}",
                        subtitle = "Build ${updateState.currentVersionCode} · Stormlight v4",
                        icon = Icons.Rounded.SystemUpdate
                    )
                    updateState.statusMessage?.let { msg ->
                        Text(msg, style = type.caption, color = colors.accentOnSky)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                        GlassPillButton(
                            label = if (updateState.isChecking) "Checking…" else "Check updates",
                            onClick = onCheckForUpdate,
                            enabled = !updateState.isChecking && !updateState.isDownloading,
                            modifier = Modifier.weight(1f)
                        )
                        val pending = updateState.availableUpdate
                        if (pending != null) {
                            PrimaryPillButton(
                                label = if (updateState.isDownloading) "${updateState.downloadProgress}%" else "Install ${pending.tagName}",
                                onClick = onInstallUpdate,
                                enabled = !updateState.isDownloading,
                                height = ZeusSpacing.secondaryButtonHeight,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Theme pack card: mini two-tone preview (Night half + Daylight half), name and tagline. */
@Composable
private fun ThemePackCard(sky: ZeusSky, selected: Boolean, onClick: () -> Unit) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Column(
        modifier = Modifier
            .width(ZeusSizes.coverHeroWidth)
            .then(if (selected) Modifier.neonBorder(shape = ZeusShapes.cardCompact) else Modifier)
            .clip(ZeusShapes.cardCompact)
            .border(ZeusSpacing.hairline, colors.glassBorder, ZeusShapes.cardCompact)
            .background(colors.glassFill)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = "${sky.packName} theme pack"
            }
            .padding(ZeusSpacing.smPlus),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ZeusSizes.iconTileLarge + ZeusSpacing.lg)
                .clip(ZeusShapes.tile)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(sky.baseA, sky.baseB, sky.glow1)))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(ZeusSizes.iconLg)
                        .clip(ZeusShapes.circle)
                        .background(Brush.linearGradient(listOf(sky.accent, sky.glow2)))
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(sky.lightA, sky.lightB))),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = sky.accentInk, modifier = Modifier.size(ZeusSizes.iconLg))
                }
            }
        }
        Text(sky.packName, style = type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = colors.textOnSky, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sky.tagline, style = type.caption, color = colors.textOnSkySecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
