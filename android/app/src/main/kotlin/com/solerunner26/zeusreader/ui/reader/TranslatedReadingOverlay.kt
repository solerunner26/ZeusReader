package com.solerunner26.zeusreader.ui.reader

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solerunner26.zeusreader.data.local.BookGlossaryEntity
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.reader.prefs.ReaderSettings
import com.solerunner26.zeusreader.translation.TranslatedDisplayMode
import com.solerunner26.zeusreader.translation.TranslatedPassageResult
import com.solerunner26.zeusreader.translation.TranslationEnginePreference
import com.solerunner26.zeusreader.ui.components.CircleIconButton
import com.solerunner26.zeusreader.ui.components.SecondaryButton
import com.solerunner26.zeusreader.ui.components.Eyebrow
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.ZeusLinearProgress
import com.solerunner26.zeusreader.ui.components.MenuRow
import com.solerunner26.zeusreader.ui.components.ZeusTextField
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.ZeusType
import java.util.Locale

private fun TranslatedDisplayMode.lensLabel(lang: StudyTargetLanguage) = when (this) {
    TranslatedDisplayMode.INTERLINEAR -> "Split"
    TranslatedDisplayMode.TRANSLATION_ONLY -> if (lang == StudyTargetLanguage.HINDI) "All हिन्दी" else "All ગુજરાતી"
    TranslatedDisplayMode.SIDE_BY_SIDE -> "Side by side"
}

/**
 * Translate Lens (DESIGN.md §2): reads inside the paper sheet. Split shows EN + translation,
 * All shows only the translation (tap a sentence to peek at the English), Side by side on wide
 * screens. Tapping a sentence puts the lens on it.
 */
@Composable
fun TranslatedReadingOverlay(
    sectionLabel: String,
    passageResult: TranslatedPassageResult?,
    isTranslating: Boolean,
    ocrNotice: String?,
    targetLanguage: StudyTargetLanguage,
    displayMode: TranslatedDisplayMode,
    enginePreference: TranslationEnginePreference,
    glossaryTerms: List<BookGlossaryEntity>,
    settings: ReaderSettings,
    onSelectTargetLanguage: (StudyTargetLanguage) -> Unit,
    onSelectDisplayMode: (TranslatedDisplayMode) -> Unit,
    onSelectEnginePreference: (TranslationEnginePreference) -> Unit,
    onRetranslatePassage: () -> Unit,
    onAddGlossaryTerm: (String) -> Unit,
    onRemoveGlossaryTerm: (String) -> Unit,
    onExitTranslatedMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val ink = settings.theme.textColor
    val paper = settings.theme.backgroundColor
    val lensColor = colors.accent
    var showGlossarySheet by remember { mutableStateOf(false) }
    val revealedEnglish = remember(passageResult?.passageHash) { mutableStateMapOf<Int, Boolean>() }
    var lensIndex by remember(passageResult?.passageHash) { mutableStateOf(0) }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status -> if (status == TextToSpeech.SUCCESS) ttsReady = true }
        instance
    }
    DisposableEffect(tts) {
        onDispose {
            runCatching {
                tts.stop()
                tts.shutdown()
            }
        }
    }

    val readingFamily = settings.fontChoice.toComposeFontFamily()
    val indicStyle = ZeusType.readingFor(targetLanguage.code, type.reading.copy(fontSize = ZeusType.readingSize(settings.fontSizePercent)))
    val baseSp = ZeusType.readingSize(settings.fontSizePercent)
    val isWide = LocalWindowInfo.current.containerSize.width >= 1500

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(paper)
            .testTag("translate_lens")
    ) {
        // Lens header (paper-aware)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ZeusSpacing.lg, end = ZeusSpacing.sm, top = ZeusSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Translate", style = type.label, color = ink, maxLines = 1)
                    val engineSummary = buildString {
                        append(sectionLabel)
                        append(" · ")
                        append(passageResult?.engineIdVersion ?: enginePreference.engineIdVersion)
                        if (passageResult?.fromCache == true) append(" · cached")
                    }
                    Text(engineSummary, style = type.tiny, color = ink.copy(alpha = 0.7f), maxLines = 1)
                }
                PaperIconButton(Icons.Rounded.Refresh, "Retranslate this passage", ink, onRetranslatePassage, Modifier.testTag("retranslate_passage_button"))
                PaperIconButton(Icons.Rounded.Book, "Don't-translate glossary", ink, { showGlossarySheet = true }, Modifier.testTag("open_glossary_button"))
                PaperIconButton(Icons.Rounded.Close, "Return to original view", ink, onExitTranslatedMode, Modifier.testTag("exit_translated_mode_button"))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
            ) {
                TranslatedDisplayMode.entries.forEach { mode ->
                    PaperPill(mode.lensLabel(targetLanguage), displayMode == mode, ink, paper, { onSelectDisplayMode(mode) })
                }
                PaperPill(
                    label = if (targetLanguage == StudyTargetLanguage.HINDI) "हिन्दी → ગુજરાતી" else "ગુજરાતી → हिन्दी",
                    selected = false,
                    ink = ink,
                    paper = paper,
                    onClick = {
                        onSelectTargetLanguage(if (targetLanguage == StudyTargetLanguage.HINDI) StudyTargetLanguage.GUJARATI else StudyTargetLanguage.HINDI)
                    }
                )
                TranslationEnginePreference.entries.forEach { pref ->
                    PaperPill(pref.label, enginePreference == pref, ink, paper, { onSelectEnginePreference(pref) })
                }
            }
        }

        if (isTranslating) ZeusLinearProgress(modifier = Modifier.padding(horizontal = ZeusSpacing.lg, vertical = ZeusSpacing.xs))

        if (!ocrNotice.isNullOrBlank()) {
            Text(
                text = ocrNotice,
                style = type.caption,
                color = ink,
                modifier = Modifier
                    .padding(horizontal = ZeusSpacing.lg, vertical = ZeusSpacing.xs)
                    .clip(ZeusShapes.tile)
                    .background(lensColor.copy(alpha = 0.14f))
                    .padding(ZeusSpacing.sm)
            )
        }

        val pairs = passageResult?.pairs.orEmpty()
        if (pairs.isEmpty() && !isTranslating) {
            Box(modifier = Modifier.fillMaxSize().padding(ZeusSpacing.xxl), contentAlignment = Alignment.Center) {
                Text(
                    "No translatable prose in this section. Diagrams and formulas stay untouched in the original view.",
                    style = type.body,
                    color = ink.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = settings.horizontalMarginDp.dp, vertical = ZeusSpacing.md),
                verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
            ) {
                items(pairs, key = { "${passageResult?.passageHash}_${it.sentenceIndex}" }) { pair ->
                    val onLens = pair.sentenceIndex == lensIndex
                    val revealOriginal = revealedEnglish[pair.sentenceIndex] == true
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ZeusShapes.cardSmall)
                            .then(
                                if (onLens) {
                                    Modifier
                                        .background(lensColor.copy(alpha = 0.10f))
                                        .border(ZeusSpacing.neonBorderWidth, lensColor, ZeusShapes.cardSmall)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(role = Role.Button) {
                                lensIndex = pair.sentenceIndex
                                if (displayMode == TranslatedDisplayMode.TRANSLATION_ONLY) revealedEnglish[pair.sentenceIndex] = !revealOriginal
                            }
                            .padding(ZeusSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)
                    ) {
                        if (displayMode == TranslatedDisplayMode.SIDE_BY_SIDE && isWide) {
                            Row(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.lg)) {
                                Text(pair.sourceEnglish, fontFamily = readingFamily, fontSize = baseSp, lineHeight = baseSp * settings.lineHeightMultiplier, color = ink, modifier = Modifier.weight(1f))
                                Text(pair.translatedText, style = indicStyle, color = ink, modifier = Modifier.weight(1f))
                            }
                        } else {
                            val showEnglish = displayMode != TranslatedDisplayMode.TRANSLATION_ONLY || revealOriginal
                            if (showEnglish) {
                                Text(pair.sourceEnglish, fontFamily = readingFamily, fontSize = baseSp * 0.92f, lineHeight = baseSp * settings.lineHeightMultiplier, color = ink.copy(alpha = 0.78f))
                            }
                            if (!pair.keptUntranslated) {
                                Text(pair.translatedText, style = indicStyle, color = ink)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                                if (pair.keptUntranslated) Text("Kept in English", style = type.tiny, color = ink.copy(alpha = 0.7f))
                                if (displayMode == TranslatedDisplayMode.TRANSLATION_ONLY && !revealOriginal) {
                                    Text("Tap to peek at English", style = type.tiny, color = ink.copy(alpha = 0.7f))
                                }
                            }
                            if (!pair.keptUntranslated) {
                                PaperIconButton(Icons.AutoMirrored.Rounded.VolumeUp, "Listen to translation", ink, {
                                    if (ttsReady) {
                                        tts.language = Locale.forLanguageTag(targetLanguage.code)
                                        tts.speak(pair.translatedText, TextToSpeech.QUEUE_FLUSH, null, "tr_${pair.sentenceIndex}")
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGlossarySheet) {
        var newTerm by remember { mutableStateOf("") }
        ZeusBottomSheet(
            onDismissRequest = { showGlossarySheet = false },
            title = "Don't translate",
            subtitle = "Protected terms stay in English. Changes refresh cached translations."
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                ZeusTextField(value = newTerm, onValueChange = { newTerm = it }, placeholder = "e.g. Euler, xylem", modifier = Modifier.weight(1f))
                SecondaryButton(label = "Add", icon = Icons.Rounded.Add, enabled = newTerm.isNotBlank(), onClick = {
                    onAddGlossaryTerm(newTerm)
                    newTerm = ""
                })
            }
            if (glossaryTerms.isEmpty()) {
                Text("No protected terms for this book yet.", style = type.body, color = colors.muted)
            } else {
                glossaryTerms.forEach { entry ->
                    MenuRow(title = entry.term) {
                        CircleIconButton(Icons.Rounded.Delete, "Remove ${entry.term}", { onRemoveGlossaryTerm(entry.id) })
                    }
                }
            }
            Eyebrow(text = "${glossaryTerms.size} protected terms")
        }
    }
}

@Composable
private fun PaperIconButton(icon: ImageVector, description: String, ink: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(ZeusSpacing.touchMin)
            .clip(ZeusShapes.circle)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(ZeusSizes.iconMd))
    }
}

