package com.solerunner26.zeusreader.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.reader.prefs.AppLanguage
import com.solerunner26.zeusreader.reader.prefs.AppTheme
import com.solerunner26.zeusreader.reader.prefs.AppUiConfig
import com.solerunner26.zeusreader.ui.theme.AppUiConfigViewModel
import com.solerunner26.zeusreader.ui.theme.HueAuraBackground
import com.solerunner26.zeusreader.ui.theme.HuePreset
import com.solerunner26.zeusreader.ui.theme.ReaderSurface
import com.solerunner26.zeusreader.ui.theme.hslColor
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
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    modifier: Modifier = Modifier
) {
    HueAuraBackground(config = config, darkTheme = darkTheme, modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            topBar = {
                TopAppBar(
                    windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("settings_screen_root"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("App theme", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = config.appTheme == theme,
                            onClick = { onSelectAppTheme(theme) },
                            modifier = Modifier.heightIn(min = 48.dp),
                            label = { Text(theme.label) }
                        )
                    }
                }

                Text("App language", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = config.appLanguage == lang,
                            onClick = { onSelectLanguage(lang) },
                            modifier = Modifier.heightIn(min = 48.dp),
                            label = { Text(lang.label) }
                        )
                    }
                }

                Text("Theme hue", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HuePreset.entries.forEach { preset ->
                        val selected = kotlin.math.abs(config.themeHue - preset.hue) < 1f
                        val swatchColor = hslColor(preset.hue, 0.55f, if (darkTheme) 0.65f else 0.42f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onSelectPreset(preset) }
                                .padding(4.dp)
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = "Hue preset ${preset.label}" }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .then(
                                        if (selected) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(preset.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsSwitchRow(
                            title = "Hue from book cover",
                            checked = config.hueFromCover,
                            onCheckedChange = onToggleHueFromCover
                        )

                        Text(
                            "Hue aura: ${(config.auraIntensity * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Slider(
                            value = config.auraIntensity,
                            onValueChange = onAuraIntensityChange,
                            valueRange = 0f..1f
                        )
                    }
                }

                Text("Default reading surface", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReaderSurface.entries.forEach { surface ->
                        FilterChip(
                            selected = config.readerSurface == surface,
                            onClick = { onSelectReaderSurface(surface) },
                            modifier = Modifier.heightIn(min = 48.dp),
                            label = { Text(surface.label) }
                        )
                    }
                }

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SettingsSwitchRow("Focus ribbon", config.focusRibbon, onToggleFocusRibbon)
                        SettingsSwitchRow("Glass UI", config.glassUi, onToggleGlassUi)
                        SettingsSwitchRow("Aesthetic elements (Aura)", config.aestheticElements, onToggleAestheticElements)
                        SettingsSwitchRow("Master animations", config.masterAnimations, onToggleMasterAnimations)
                    }
                }

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Tools & Private Server", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = onOpenStats,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text("Reading Stats")
                            }
                            androidx.compose.material3.Button(
                                onClick = onOpenOnlineSearch,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text("Online & Sync")
                            }
                        }
                    }
                }

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "App Version & GitHub Updates",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Installed: v${updateState.currentVersion} (build ${updateState.currentVersionCode})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "What's new in 0.4.0: Ink & Hue Liquid Glass UI, Virtual Racks & Cover Grid, PDF OCR/Selection & Search Highlights, Passages & Card Studio, Cloudflare Relay + SLUM, In-App GitHub Updater.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        updateState.statusMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = onCheckForUpdate,
                                enabled = !updateState.isChecking && !updateState.isDownloading,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text(if (updateState.isChecking) "Checking…" else "Check Updates")
                            }
                            if (updateState.availableUpdate != null) {
                                androidx.compose.material3.Button(
                                    onClick = onInstallUpdate,
                                    enabled = !updateState.isDownloading,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(if (updateState.isDownloading) "${updateState.downloadProgress}%" else "Install ${updateState.availableUpdate.tagName}")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
