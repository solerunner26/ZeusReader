package com.solerunner26.zeusreader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solerunner26.zeusreader.reader.prefs.PdfFitMode
import com.solerunner26.zeusreader.reader.prefs.ReaderFontChoice
import com.solerunner26.zeusreader.reader.prefs.ReaderSettings
import com.solerunner26.zeusreader.reader.prefs.ReaderThemeMode
import com.solerunner26.zeusreader.ui.theme.ReaderSurface
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypographyBottomSheet(
    settings: ReaderSettings,
    isPdfMode: Boolean,
    focusRibbon: Boolean = false,
    onToggleFocusRibbon: (Boolean) -> Unit = {},
    onSettingsChange: ((ReaderSettings) -> ReaderSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        TypographyPanelContent(
            settings = settings,
            isPdfMode = isPdfMode,
            focusRibbon = focusRibbon,
            onToggleFocusRibbon = onToggleFocusRibbon,
            onSettingsChange = onSettingsChange
        )
    }
}

@Composable
internal fun TypographyPanelContent(
    settings: ReaderSettings,
    isPdfMode: Boolean,
    focusRibbon: Boolean = false,
    onToggleFocusRibbon: (Boolean) -> Unit = {},
    onSettingsChange: ((ReaderSettings) -> ReaderSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("typography_panel_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Reading Appearance & Ergonomics", style = MaterialTheme.typography.titleLarge)

        // Live preview so font size, line height, font family, surface, and warm filter are visible immediately
        val previewSp = (16f * settings.fontSizePercent / 100f).sp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(settings.theme.backgroundColor)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                .padding(14.dp)
                .testTag("typography_live_preview")
        ) {
            Text(
                text = "Live Preview (${settings.fontSizePercent}%): The quick brown fox reads calmly across every page.",
                fontFamily = settings.fontChoice.toComposeFontFamily(),
                fontSize = previewSp,
                lineHeight = previewSp * settings.lineHeightMultiplier,
                color = settings.theme.textColor
            )
            if (settings.warmLightStrength > 0.01f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFFFFA726).copy(alpha = settings.warmLightStrength))
                )
            }
        }

        Text("Reading Surface", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReaderThemeMode.entries.forEach { mode ->
                val surfaceLabel = ReaderSurface.fromLegacyThemeMode(mode).label
                FilterChip(
                    selected = settings.theme == mode,
                    onClick = { onSettingsChange { it.copy(theme = mode) } },
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = { Text(surfaceLabel) }
                )
            }
        }

        Text("Font Family", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReaderFontChoice.entries.forEach { font ->
                FilterChip(
                    selected = settings.fontChoice == font,
                    onClick = { onSettingsChange { it.copy(fontChoice = font) } },
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = { Text(font.label) }
                )
            }
        }

        Text(
            text = "Font Size: ${settings.fontSizePercent}%",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = settings.fontSizePercent.toFloat(),
            onValueChange = { value ->
                onSettingsChange { it.copy(fontSizePercent = (value / 10f).roundToInt() * 10) }
            },
            valueRange = 80f..220f
        )

        Text(
            text = "Line Height: ${String.format("%.2f", settings.lineHeightMultiplier)}x",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = settings.lineHeightMultiplier,
            onValueChange = { value ->
                onSettingsChange { it.copy(lineHeightMultiplier = value) }
            },
            valueRange = 1.1f..2.0f
        )

        Text(
            text = "Warm Light Filter: ${(settings.warmLightStrength * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = settings.warmLightStrength,
            onValueChange = { value ->
                onSettingsChange { it.copy(warmLightStrength = value) }
            },
            valueRange = 0f..0.45f
        )

        HorizontalDivider()
        Text("One-Handed Controls", style = MaterialTheme.typography.titleMedium)

        SettingsToggleRow(
            title = "Focus ribbon (3-line reading band)",
            checked = focusRibbon,
            onCheckedChange = onToggleFocusRibbon
        )
        SettingsToggleRow(
            title = "Lock screen rotation",
            checked = settings.rotationLocked,
            onCheckedChange = { checked -> onSettingsChange { it.copy(rotationLocked = checked) } }
        )
        SettingsToggleRow(
            title = "Volume keys turn pages",
            checked = settings.volumeKeyPageTurn,
            onCheckedChange = { checked -> onSettingsChange { it.copy(volumeKeyPageTurn = checked) } }
        )
        SettingsToggleRow(
            title = "Left-edge brightness swipe",
            checked = settings.edgeBrightnessSwipe,
            onCheckedChange = { checked -> onSettingsChange { it.copy(edgeBrightnessSwipe = checked) } }
        )

        if (isPdfMode) {
            HorizontalDivider()
            Text("PDF Layout Extras", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PdfFitMode.entries.forEach { fit ->
                    FilterChip(
                        selected = settings.pdfFitMode == fit,
                        onClick = { onSettingsChange { it.copy(pdfFitMode = fit) } },
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text(fit.label) }
                    )
                }
            }
            SettingsToggleRow(
                title = "Auto-crop white margins",
                checked = settings.pdfAutoCropMargins,
                onCheckedChange = { checked -> onSettingsChange { it.copy(pdfAutoCropMargins = checked) } }
            )
            SettingsToggleRow(
                title = "Retain zoom across pages",
                checked = settings.pdfRetainZoomAcrossPages,
                onCheckedChange = { checked -> onSettingsChange { it.copy(pdfRetainZoomAcrossPages = checked) } }
            )
            SettingsToggleRow(
                title = "Structured text reflow",
                checked = settings.pdfReflowMode,
                onCheckedChange = { checked -> onSettingsChange { it.copy(pdfReflowMode = checked) } }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
