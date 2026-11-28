package com.solerunner26.zeusreader.ui.vocab

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.data.local.VocabWordEntity
import com.solerunner26.zeusreader.ml.OfflinePackStatus
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.ui.components.EmptyState
import com.solerunner26.zeusreader.ui.components.GlassIconButton
import com.solerunner26.zeusreader.ui.components.GlassPillButton
import com.solerunner26.zeusreader.ui.components.GlassSection
import com.solerunner26.zeusreader.ui.components.GradientHeading
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.PrimaryPillButton
import com.solerunner26.zeusreader.ui.components.StickerChip
import com.solerunner26.zeusreader.ui.components.ZeusLinearProgress
import com.solerunner26.zeusreader.ui.components.ZeusListRow
import com.solerunner26.zeusreader.ui.components.ZeusSegmented
import com.solerunner26.zeusreader.ui.components.ZeusTopBar
import com.solerunner26.zeusreader.ui.components.rememberDockClearance
import com.solerunner26.zeusreader.ui.components.rememberNavBarClearance
import com.solerunner26.zeusreader.ui.theme.GlassSurface
import com.solerunner26.zeusreader.ui.theme.ZeusMotion
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.vocab.ReviewGrade

@Composable
fun PrepareOfflineScreen(
    viewModel: VocabularyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val packStatus by viewModel.packStatus.collectAsStateWithLifecycle()
    val downloading by viewModel.downloadingLang.collectAsStateWithLifecycle()
    val message by viewModel.statusMessage.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        ZeusTopBar(title = "Offline packs", subtitle = "Read, look up and translate in airplane mode", onBack = onBack)
        PrepareOfflineContent(
            packStatus = packStatus,
            downloadingLang = downloading,
            statusMessage = message,
            onDownloadPack = viewModel::downloadPack,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = rememberNavBarClearance())
        )
    }
}

@Composable
internal fun PrepareOfflineContent(
    packStatus: OfflinePackStatus,
    downloadingLang: StudyTargetLanguage?,
    statusMessage: String?,
    onDownloadPack: (StudyTargetLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Column(
        modifier = modifier
            .padding(horizontal = ZeusSpacing.gutter)
            .widthIn(max = ZeusSizes.contentMaxWidth)
            .testTag("prepare_offline_root"),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.lg)
    ) {
        GlassSection(neon = true) {
            MetaLabel(text = if (packStatus.isPreparedPhone) "Prepared phone" else "Fresh install", accent = true)
            Text(
                text = if (packStatus.isPreparedPhone) "Fully offline: English · हिन्दी · ગુજરાતી" else "English ready — language packs missing",
                style = type.headline,
                color = colors.textOnSky
            )
            Text(
                text = "The bundled WordNet dictionary and seed glosses work in airplane mode. Download the ML Kit packs below for full sentence translation offline.",
                style = type.body,
                color = colors.textOnSkySecondary
            )
        }
        PackRow("English WordNet + bilingual seed", packStatus.englishDictionaryReady, false) {}
        PackRow("English → Hindi (हिन्दी)", packStatus.hindiPackDownloaded, downloadingLang == StudyTargetLanguage.HINDI) {
            onDownloadPack(StudyTargetLanguage.HINDI)
        }
        PackRow("English → Gujarati (ગુજરાતી)", packStatus.gujaratiPackDownloaded, downloadingLang == StudyTargetLanguage.GUJARATI) {
            onDownloadPack(StudyTargetLanguage.GUJARATI)
        }
        if (!statusMessage.isNullOrBlank()) {
            Text(statusMessage, style = type.caption, color = colors.accentOnSky)
        }
    }
}

@Composable
private fun PackRow(title: String, ready: Boolean, isDownloading: Boolean, onDownload: () -> Unit) {
    GlassSection {
        ZeusListRow(
            title = title,
            subtitle = if (ready) "Ready offline" else if (isDownloading) "Downloading…" else "Not downloaded",
            icon = if (ready) Icons.Rounded.CheckCircle else Icons.Rounded.Translate,
            iconFilled = ready
        ) {
            if (!ready) {
                GlassPillButton(label = "Download", icon = Icons.Rounded.CloudDownload, onClick = onDownload, enabled = !isDownloading)
            }
        }
        if (isDownloading) ZeusLinearProgress()
    }
}

@Composable
fun VocabularyScreen(
    viewModel: VocabularyViewModel,
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val words by viewModel.words.collectAsStateWithLifecycle()
    VocabularyContent(
        words = words,
        onReview = viewModel::reviewWord,
        onDelete = viewModel::deleteWord,
        modifier = modifier
    )
}

@Composable
internal fun VocabularyContent(
    words: List<VocabWordEntity>,
    onReview: (VocabWordEntity, ReviewGrade) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    nowEpochMs: Long = System.currentTimeMillis()
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    var tab by remember { mutableIntStateOf(0) }
    var practiceAll by remember { mutableStateOf(false) }
    val due = remember(words, nowEpochMs) { words.filter { it.nextReviewEpochMs <= nowEpochMs } }
    val dockClearance = rememberDockClearance()

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ZeusSpacing.gutter, end = ZeusSpacing.gutter, top = ZeusSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                GradientHeading(text = "Words")
                Text("${words.size} collected while reading", style = type.caption, color = colors.textOnSkySecondary, maxLines = 1)
            }
            StickerChip(text = "${due.size} due", accentFill = true, rotation = 4f)
        }
        ZeusSegmented(
            options = listOf("Review", "Saved (${words.size})"),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = ZeusSpacing.gutter, vertical = ZeusSpacing.md)
        )

        if (words.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ZeusSpacing.gutter)
                    .padding(bottom = dockClearance),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Rounded.Translate,
                    title = "No words yet",
                    message = "Tap Zap on any word while reading to save its meaning in English, हिन्दी and ગુજરાતી."
                )
            }
        } else if (tab == 0) {
            val queue = if (practiceAll || due.isNotEmpty()) (if (due.isNotEmpty()) due else words) else emptyList()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ZeusSpacing.gutter)
                    .padding(bottom = dockClearance),
                verticalArrangement = Arrangement.spacedBy(ZeusSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val card = queue.firstOrNull()
                if (card == null) {
                    EmptyState(
                        icon = Icons.Rounded.CheckCircle,
                        title = "All caught up",
                        message = "No words are due right now. Come back later, or practise them all.",
                        actionLabel = "Practise anyway",
                        onAction = { practiceAll = true }
                    )
                } else {
                    FlipWordCard(
                        word = card,
                        position = "${words.size - queue.size + 1} / ${words.size}",
                        onGrade = { grade -> onReview(card, grade) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = ZeusSpacing.gutter, end = ZeusSpacing.gutter, bottom = dockClearance),
                verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
            ) {
                items(words, key = { it.id }) { item ->
                    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = ZeusShapes.cardCompact) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(ZeusSpacing.mdPlus),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                                    Text(item.word, style = type.title.copy(fontFamily = type.Unbounded), color = colors.textOnSky, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                    MetaLabel(text = item.targetLang, accent = true)
                                }
                                if (item.translatedMeaning.isNotBlank()) {
                                    Text(item.translatedMeaning, style = type.body.copy(fontFamily = indicFontFor(item.targetLang)), color = colors.accentOnSky, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                                if (item.englishMeaning.isNotBlank()) {
                                    Text(item.englishMeaning, style = type.caption, color = colors.textOnSky, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                                if (item.sentenceContext.isNotBlank() || item.bookTitle.isNotBlank()) {
                                    Text(
                                        text = listOf(item.bookTitle, item.sentenceContext).filter { it.isNotBlank() }.joinToString(" · "),
                                        style = type.quoteSmall,
                                        color = colors.textOnSkySecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            GlassIconButton(icon = Icons.Rounded.DeleteOutline, contentDescription = "Delete ${item.word}", onClick = { onDelete(item.id) })
                        }
                    }
                }
            }
        }
    }
}

private fun indicFontFor(lang: String) = when (lang.lowercase()) {
    "hi" -> com.solerunner26.zeusreader.ui.theme.ZeusType.NotoSansDevanagari
    "gu" -> com.solerunner26.zeusreader.ui.theme.ZeusType.NotoSansGujarati
    else -> com.solerunner26.zeusreader.ui.theme.ZeusType.BricolageGrotesque
}

/** Flip card: tap to turn (rotationY), then grade with Again / Got it / Easy pills. */
@Composable
private fun FlipWordCard(word: VocabWordEntity, position: String, onGrade: (ReviewGrade) -> Unit) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    var flipped by remember(word.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = ZeusMotion.settleTween(ZeusTheme.reduceMotion, ZeusMotion.CARD_FLIP_MS),
        label = "word_flip"
    )
    val showBack = rotation > 90f

    MetaLabel(text = "Card $position", modifier = Modifier.fillMaxWidth())
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ZeusSizes.wordCardMinHeight)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16f * density
            },
        neon = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ZeusSizes.wordCardMinHeight)
                .clickable(role = Role.Button) { flipped = !flipped }
                .semantics { contentDescription = if (showBack) "Meaning of ${word.word}. Tap to see the word." else "${word.word}. Tap to reveal the meaning." }
                .graphicsLayer { rotationY = if (showBack) 180f else 0f }
                .padding(ZeusSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!showBack) {
                MetaLabel(text = word.bookTitle.ifBlank { "From your reading" }, accent = true)
                Text(word.word, style = type.screenTitle, color = colors.textOnSky, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (word.sentenceContext.isNotBlank()) {
                    Text("“${word.sentenceContext}”", style = type.quote, color = colors.textOnSkySecondary, textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                    Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = colors.textOnSkyMuted)
                    Text("Tap to flip", style = type.caption, color = colors.textOnSkyMuted)
                }
            } else {
                MetaLabel(text = "Meaning", accent = true)
                Text(word.translatedMeaning.ifBlank { word.word }, style = type.headline.copy(fontFamily = indicFontFor(word.targetLang)), color = colors.accentOnSky, textAlign = TextAlign.Center)
                if (word.englishMeaning.isNotBlank()) {
                    Text(word.englishMeaning, style = type.body, color = colors.textOnSky, textAlign = TextAlign.Center)
                }
            }
        }
    }
    if (showBack) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
            GlassPillButton(label = "Again", onClick = { onGrade(ReviewGrade.AGAIN) }, danger = true, modifier = Modifier.weight(1f))
            GlassPillButton(label = "Got it", onClick = { onGrade(ReviewGrade.GOOD) }, modifier = Modifier.weight(1f))
            PrimaryPillButton(label = "Easy", onClick = { onGrade(ReviewGrade.EASY) }, height = ZeusSpacing.secondaryButtonHeight, shimmer = false, modifier = Modifier.weight(1f))
        }
    } else {
        PrimaryPillButton(label = "Reveal meaning", onClick = { flipped = true }, modifier = Modifier.fillMaxWidth())
    }
}

