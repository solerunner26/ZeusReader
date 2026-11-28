package com.solerunner26.zeusreader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.solerunner26.zeusreader.reader.prefs.PdfFitMode
import com.solerunner26.zeusreader.reader.prefs.ReaderFontChoice
import com.solerunner26.zeusreader.reader.prefs.ReaderSettings
import com.solerunner26.zeusreader.reader.prefs.ReaderThemeMode
import com.solerunner26.zeusreader.ui.components.GlassIconButton
import com.solerunner26.zeusreader.ui.components.GlassSection
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.ZeusChip
import com.solerunner26.zeusreader.ui.components.ZeusDivider
import com.solerunner26.zeusreader.ui.components.ZeusSegmented
import com.solerunner26.zeusreader.ui.components.ZeusSliderRow
import com.solerunner26.zeusreader.ui.components.ZeusSwitchRow
import com.solerunner26.zeusreader.ui.theme.ReaderSurface
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.ZeusType
import kotlin.math.roundToInt

/** Vibe check (DESIGN.md §2): paper, size, font, spacing, warm light, one-handed & PDF layout. */
@Composable
fun TypographyBottomSheet(
    settings: ReaderSettings,
    isPdfMode: Boolean,
    focusRibbon: Boolean = false,
    onToggleFocusRibbon: (Boolean) -> Unit = {},
    onSettingsChange: ((ReaderSettings) -> ReaderSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ZeusBottomSheet(onDismissRequest = onDismiss, title = "Vibe check", subtitle = "Paper, type and comfort") {
        TypographyPanelContent(
            settings = settings,
            isPdfMode = isPdfMode,
            focusRibbon = focusRibbon,
            onToggleFocusRibbon = onToggleFocusRibbon,
            onSettingsChange = onSettingsChange
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TypographyPanelContent(
    settings: ReaderSettings,
    isPdfMode: Boolean,
    focusRibbon: Boolean = false,
    onToggleFocusRibbon: (Boolean) -> Unit = {},
    onSettingsChange: ((ReaderSettings) -> ReaderSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("typography_panel_root"),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
    ) {
        // Live preview on the chosen paper
        val previewSp = ZeusType.readingSize(settings.fontSizePercent)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ZeusShapes.cardCompact)
                .background(settings.theme.backgroundColor)
                .border(ZeusSpacing.hairline, colors.glassBorder, ZeusShapes.cardCompact)
                .testTag("typography_live_preview")
        ) {
            Text(
                text = "Capillary action draws water upward through narrow soil pores — ${settings.fontSizePercent}%.",
                fontFamily = settings.fontChoice.toComposeFontFamily(),
                fontSize = previewSp,
                lineHeight = previewSp * settings.lineHeightMultiplier,
                color = settings.theme.textColor,
                modifier = Modifier.padding(ZeusSpacing.lg)
            )
            WarmLightOverlay(settings.warmLightStrength)
        }

        MetaLabel(text = "Paper", accent = true)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ReaderThemeMode.entries.forEach { mode ->
                val selected = settings.theme == mode
                val label = ReaderSurface.fromLegacyThemeMode(mode).label
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                    Box(
                        modifier = Modifier
                            .size(ZeusSizes.swatch)
                            .clip(ZeusShapes.circle)
                            .background(mode.backgroundColor)
                            .border(if (selected) ZeusSpacing.selectedBorderWidth else ZeusSpacing.hairline, if (selected) colors.accentOnSky else colors.glassBorder, ZeusShapes.circle)
                            .clickable(role = Role.RadioButton) { onSettingsChange { it.copy(theme = mode) } }
                            .semantics {
                                this.selected = selected
                                contentDescription = "$label paper"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = mode.textColor, modifier = Modifier.size(ZeusSizes.iconMd))
                        } else {
                            Text("Aa", style = type.label, color = mode.textColor)
                        }
                    }
                    Text(label, style = type.labelSmall, color = colors.textOnSkySecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        MetaLabel(text = "Font", accent = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
            ReaderFontChoice.entries.forEach { font ->
                ZeusChip(
                    label = font.label,
                    selected = settings.fontChoice == font,
                    onClick = { onSettingsChange { it.copy(fontChoice = font) } },
                    labelStyle = type.label.copy(fontFamily = font.toComposeFontFamily())
                )
            }
        }

        GlassSection {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                GlassIconButton(Icons.Rounded.TextDecrease, "Smaller text", {
                    onSettingsChange { it.copy(fontSizePercent = (it.fontSizePercent - 10).coerceAtLeast(80)) }
                })
                ZeusSliderRow(
                    title = "Size",
                    valueLabel = "${settings.fontSizePercent}%",
                    value = settings.fontSizePercent.toFloat(),
                    onValueChange = { v -> onSettingsChange { it.copy(fontSizePercent = (v / 10f).roundToInt() * 10) } },
                    valueRange = 80f..220f,
                    modifier = Modifier.weight(1f)
                )
                GlassIconButton(Icons.Rounded.TextIncrease, "Bigger text", {
                    onSettingsChange { it.copy(fontSizePercent = (it.fontSizePercent + 10).coerceAtMost(220)) }
                })
            }
            ZeusSliderRow(
                title = "Line height",
                valueLabel = "${String.format("%.2f", settings.lineHeightMultiplier)}×",
                value = settings.lineHeightMultiplier,
                onValueChange = { v -> onSettingsChange { it.copy(lineHeightMultiplier = v) } },
                valueRange = 1.1f..2.0f
            )
            ZeusSliderRow(
                title = "Warm light",
                valueLabel = "${(settings.warmLightStrength * 100).roundToInt()}%",
                value = settings.warmLightStrength,
                onValueChange = { v -> onSettingsChange { it.copy(warmLightStrength = v) } },
                valueRange = 0f..0.45f
            )
        }

        GlassSection(title = "One-handed") {
            ZeusSwitchRow("Focus ribbon", focusRibbon, onToggleFocusRibbon, subtitle = "Three bright lines, the rest dimmed")
            ZeusDivider()
            ZeusSwitchRow("Lock rotation", settings.rotationLocked, { c -> onSettingsChange { it.copy(rotationLocked = c) } })
            ZeusDivider()
            ZeusSwitchRow("Volume keys turn pages", settings.volumeKeyPageTurn, { c -> onSettingsChange { it.copy(volumeKeyPageTurn = c) } })
            ZeusDivider()
            ZeusSwitchRow("Left-edge brightness swipe", settings.edgeBrightnessSwipe, { c -> onSettingsChange { it.copy(edgeBrightnessSwipe = c) } })
        }

        if (isPdfMode) {
            GlassSection(title = "PDF layout") {
                ZeusSegmented(
                    options = PdfFitMode.entries.map { it.label },
                    selectedIndex = PdfFitMode.entries.indexOf(settings.pdfFitMode),
                    onSelect = { idx -> onSettingsChange { it.copy(pdfFitMode = PdfFitMode.entries[idx]) } }
                )
                ZeusSwitchRow("Auto-crop white margins", settings.pdfAutoCropMargins, { c -> onSettingsChange { it.copy(pdfAutoCropMargins = c) } })
                ZeusDivider()
                ZeusSwitchRow("Keep zoom across pages", settings.pdfRetainZoomAcrossPages, { c -> onSettingsChange { it.copy(pdfRetainZoomAcrossPages = c) } })
                ZeusDivider()
                ZeusSwitchRow("Text reflow", settings.pdfReflowMode, { c -> onSettingsChange { it.copy(pdfReflowMode = c) } }, subtitle = "Re-flow the page into readable text")
            }
        }
    }
}

