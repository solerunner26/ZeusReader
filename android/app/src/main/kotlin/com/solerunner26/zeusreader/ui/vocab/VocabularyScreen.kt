package com.solerunner26.zeusreader.ui.vocab

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.data.local.VocabWordEntity
import com.solerunner26.zeusreader.ml.OfflinePackStatus
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.ui.components.AccentBadge
import com.solerunner26.zeusreader.ui.components.ArtifactStage
import com.solerunner26.zeusreader.ui.components.CircleIconButton
import com.solerunner26.zeusreader.ui.components.Confetti
import com.solerunner26.zeusreader.ui.components.CrownArtifact
import com.solerunner26.zeusreader.ui.components.EmptyState
import com.solerunner26.zeusreader.ui.components.Eyebrow
import com.solerunner26.zeusreader.ui.components.FlameArtifact
import com.solerunner26.zeusreader.ui.components.MenuRow
import com.solerunner26.zeusreader.ui.components.PrimaryButton
import com.solerunner26.zeusreader.ui.components.ProgressBar
import com.solerunner26.zeusreader.ui.components.ScreenTitle
import com.solerunner26.zeusreader.ui.components.SecondaryButton
import com.solerunner26.zeusreader.ui.components.Tone
import com.solerunner26.zeusreader.ui.components.TrophyArtifact
import com.solerunner26.zeusreader.ui.components.ZeusCard
import com.solerunner26.zeusreader.ui.components.ZeusLinearProgress
import com.solerunner26.zeusreader.ui.components.ZeusSegmented
import com.solerunner26.zeusreader.ui.components.ZeusTopBar
import com.solerunner26.zeusreader.ui.components.floating
import com.solerunner26.zeusreader.ui.components.rememberNavBarClearance
import com.solerunner26.zeusreader.ui.components.rememberNavClearance
import com.solerunner26.zeusreader.ui.components.staggerIn
import com.solerunner26.zeusreader.ui.theme.ZeusMotion
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.ZeusType
import com.solerunner26.zeusreader.ui.theme.neonBorder
import com.solerunner26.zeusreader.ui.theme.rememberAccentBrush
import com.solerunner26.zeusreader.ui.theme.zeusCard
import com.solerunner26.zeusreader.vocab.ReviewGrade

/* =============================================================================================
 * Offline packs
 * =========================================================================================== */

@Composable
fun PrepareOfflineScreen(viewModel: VocabularyViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val packStatus by viewModel.packStatus.collectAsStateWithLifecycle()
    val downloading by viewModel.downloadingLang.collectAsStateWithLifecycle()
    val message by viewModel.statusMessage.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        ZeusTopBar("Offline packs", subtitle = "Read, look up and translate in airplane mode", onBack = onBack)
        PrepareOfflineContent(
            packStatus = packStatus,
            downloadingLang = downloading,
            statusMessage = message,
            onDownloadPack = viewModel::downloadPack,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = rememberNavBarClearance())
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
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    Column(modifier.padding(horizontal = ZeusSpacing.gutter).testTag("prepare_offline_root"), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.section)) {
        com.solerunner26.zeusreader.ui.components.GradientCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                Eyebrow(if (packStatus.isPreparedPhone) "Ready offline" else "Almost ready", color = c.onAccent)
                Text(if (packStatus.isPreparedPhone) "English · हिन्दी · ગુજરાતી offline" else "English is ready. Add हिन्दी and ગુજરાતી.", style = type.headline, color = c.onAccent)
                Text("Dictionary and seed meanings work in airplane mode. Language packs add full sentence translation.", style = type.caption, color = c.onAccent.copy(alpha = 0.88f))
            }
        }
        PackCard("English dictionary", "WordNet + bilingual seed", packStatus.englishDictionaryReady, false) {}
        PackCard("English → हिन्दी", "ML Kit Hindi pack", packStatus.hindiPackDownloaded, downloadingLang == StudyTargetLanguage.HINDI) { onDownloadPack(StudyTargetLanguage.HINDI) }
        PackCard("English → ગુજરાતી", "ML Kit Gujarati pack", packStatus.gujaratiPackDownloaded, downloadingLang == StudyTargetLanguage.GUJARATI) { onDownloadPack(StudyTargetLanguage.GUJARATI) }
        if (!statusMessage.isNullOrBlank()) Text(statusMessage, style = type.caption, color = c.accentInk)
    }
}

@Composable
private fun PackCard(title: String, subtitle: String, ready: Boolean, downloading: Boolean, onDownload: () -> Unit) {
    ZeusCard(Modifier.fillMaxWidth()) {
        MenuRow(title, subtitle = if (ready) "Ready offline" else if (downloading) "Downloading…" else subtitle, icon = if (ready) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDownload, tone = if (ready) Tone.Ok else Tone.Accent) {
            if (!ready) SecondaryButton("Download", onDownload, enabled = !downloading)
        }
        if (downloading) ZeusLinearProgress()
    }
}

/* =============================================================================================
 * Words
 * =========================================================================================== */

@Composable
fun VocabularyScreen(viewModel: VocabularyViewModel, @Suppress("UNUSED_PARAMETER") onBack: () -> Unit, modifier: Modifier = Modifier) {
    val words by viewModel.words.collectAsStateWithLifecycle()
    VocabularyContent(words = words, onReview = viewModel::reviewWord, onDelete = viewModel::deleteWord, modifier = modifier)
}

private fun VocabWordEntity.meaning(): String = englishMeaning.ifBlank { translatedMeaning }

/** One quiz question: the word, four options (A–D) and the correct index. */
internal data class QuizQuestion(val word: VocabWordEntity, val options: List<String>, val correct: Int)

internal fun buildQuiz(words: List<VocabWordEntity>, due: List<VocabWordEntity>, max: Int = 10, seed: Int = 7): List<QuizQuestion> {
    val withMeaning = words.filter { it.meaning().isNotBlank() }
    if (withMeaning.map { it.meaning() }.distinct().size < 4) return emptyList()
    val pool = (due.filter { it.meaning().isNotBlank() }.ifEmpty { withMeaning }).take(max)
    val rnd = kotlin.random.Random(seed)
    return pool.map { w ->
        val distractors = withMeaning.filter { it.id != w.id && it.meaning() != w.meaning() }.map { it.meaning() }.distinct().shuffled(rnd).take(3)
        val options = (distractors + w.meaning()).shuffled(rnd)
        QuizQuestion(w, options, options.indexOf(w.meaning()))
    }
}

@Composable
internal fun VocabularyContent(
    words: List<VocabWordEntity>,
    onReview: (VocabWordEntity, ReviewGrade) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    nowEpochMs: Long = System.currentTimeMillis()
) {
    val c = ZeusTheme.colors
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val due = remember(words, nowEpochMs) { words.filter { it.nextReviewEpochMs <= nowEpochMs } }
    val navClearance = rememberNavClearance()

    Column(modifier.fillMaxSize().statusBarsPadding().testTag("words_screen")) {
        Row(
            Modifier.fillMaxWidth().padding(start = ZeusSpacing.gutter, end = ZeusSpacing.gutter, top = ZeusSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
        ) {
            ScreenTitle("Words", subtitle = "${due.size} due today · ${words.size} saved", modifier = Modifier.weight(1f))
            FlameArtifact(ZeusSizes.artifactSmall)
        }
        ZeusSegmented(
            options = listOf("Quiz", "Flashcards", "Saved"),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = ZeusSpacing.gutter, vertical = ZeusSpacing.md)
        )
        if (words.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(horizontal = ZeusSpacing.gutter).padding(bottom = navClearance), contentAlignment = Alignment.Center) {
                EmptyState(title = "No words yet", message = "Tap Dictionary on any word while reading and save it — then quiz yourself here in English, हिन्दी and ગુજરાતી.")
            }
            return@Column
        }
        when (tab) {
            0 -> QuizTab(words, due, onReview, Modifier.fillMaxSize().padding(bottom = navClearance))
            1 -> FlashcardTab(words, due, onReview, Modifier.fillMaxSize().padding(bottom = navClearance))
            else -> SavedTab(words, onDelete, navClearance)
        }
    }
}

/* ----- Quiz ----- */

@Composable
private fun QuizTab(words: List<VocabWordEntity>, due: List<VocabWordEntity>, onReview: (VocabWordEntity, ReviewGrade) -> Unit, modifier: Modifier) {
    var round by rememberSaveable { mutableIntStateOf(0) }
    val quiz = remember(words.size, round) { buildQuiz(words, due, seed = round + 7) }
    var index by remember(round) { mutableIntStateOf(0) }
    var picked by remember(round, index) { mutableStateOf<Int?>(null) }
    var checked by remember(round, index) { mutableStateOf(false) }
    val results = remember(round) { mutableStateListOf<Boolean>() }
    val c = ZeusTheme.colors
    val type = ZeusTheme.type

    if (quiz.isEmpty()) {
        Box(modifier.padding(horizontal = ZeusSpacing.gutter), contentAlignment = Alignment.TopCenter) {
            EmptyState(title = "Save 4 words to unlock quizzes", message = "Quizzes mix your saved meanings into A–D choices. Use Flashcards until then.")
        }
        return
    }
    if (index >= quiz.size) {
        QuizResult(score = results.count { it }, total = quiz.size, onAgain = { round++ }, modifier = modifier)
        return
    }
    val q = quiz[index]
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = ZeusSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Word quiz", style = type.label, color = c.ink, modifier = Modifier.weight(1f))
            Text("${index + 1} / ${quiz.size}", style = type.label, color = c.accentInk)
        }
        ProgressBar((index.toFloat()) / quiz.size, height = ZeusSizes.progressThin)
        ZeusCard(Modifier.fillMaxWidth(), padding = PaddingValues(ZeusSpacing.gutter)) {
            Eyebrow(if (q.word.bookTitle.isNotBlank()) "From ${q.word.bookTitle}" else "From your reading", maxLines = 2)
            Text("What does “${q.word.word}” mean?", style = type.headline.copy(fontSize = type.title.fontSize.times(0.8f)), color = c.ink)
            if (q.word.sentenceContext.isNotBlank()) Text("“${q.word.sentenceContext}”", style = type.quote, color = c.muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        q.options.forEachIndexed { i, option ->
            OptionCard(
                letter = ('A' + i).toString(),
                text = option,
                selected = picked == i,
                state = when {
                    !checked -> OptionState.Idle
                    i == q.correct -> OptionState.Correct
                    picked == i -> OptionState.Wrong
                    else -> OptionState.Idle
                },
                enabled = !checked,
                onClick = { picked = i },
                modifier = Modifier.staggerIn(i)
            )
        }
        if (!checked) {
            PrimaryButton("Check answer", {
                checked = true
                val ok = picked == q.correct
                results.add(ok)
                onReview(q.word, if (ok) ReviewGrade.GOOD else ReviewGrade.AGAIN)
            }, enabled = picked != null, modifier = Modifier.fillMaxWidth())
        } else {
            val ok = picked == q.correct
            Text(if (ok) "Correct! Nicely done." else "Not quite — the answer is ${('A' + q.correct)}.", style = type.label, color = if (ok) c.ok else c.rose)
            PrimaryButton(if (index == quiz.lastIndex) "See my score" else "Next question", { index++ }, modifier = Modifier.fillMaxWidth())
        }
    }
}

internal enum class OptionState { Idle, Correct, Wrong }

@Composable
internal fun OptionCard(letter: String, text: String, selected: Boolean, state: OptionState, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    val border = when (state) {
        OptionState.Correct -> c.ok
        OptionState.Wrong -> c.rose
        OptionState.Idle -> if (selected) c.accent else c.line
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ZeusSpacing.giant)
            .neonBorder(selected && state == OptionState.Idle, ZeusShapes.option)
            .zeusCard(ZeusShapes.option)
            .border(if (state != OptionState.Idle || selected) ZeusSpacing.neonBorderWidth else ZeusSpacing.borderWidth, border, ZeusShapes.option)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(ZeusSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
    ) {
        Box(
            Modifier
                .size(ZeusSizes.optionBadge)
                .clip(ZeusShapes.circle)
                .then(
                    when {
                        state == OptionState.Correct -> Modifier.background(c.ok)
                        state == OptionState.Wrong -> Modifier.background(c.rose)
                        selected -> Modifier.background(rememberAccentBrush())
                        else -> Modifier.background(c.tint)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                OptionState.Correct -> Icon(Icons.Rounded.Check, null, tint = c.card, modifier = Modifier.size(ZeusSizes.iconSm))
                OptionState.Wrong -> Icon(Icons.Rounded.Close, null, tint = c.card, modifier = Modifier.size(ZeusSizes.iconSm))
                OptionState.Idle -> Text(letter, style = type.label, color = if (selected) c.onAccent else c.accentInk)
            }
        }
        Text(text, style = type.body, color = c.ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QuizResult(score: Int, total: Int, onAgain: () -> Unit, modifier: Modifier) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    val perfect = score == total
    Box(modifier.testTag("quiz_result")) {
        if (ZeusTheme.celebrations) Confetti(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = ZeusSpacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
        ) {
            ArtifactStage(ZeusSizes.stage) {
                TrophyArtifact(ZeusSizes.trophy)
                CrownArtifact(ZeusSizes.artifactMedium, Modifier.align(Alignment.TopStart).offset(x = ZeusSpacing.xxxl, y = ZeusSpacing.xxxl).floating(rotation = -14f))
            }
            Eyebrow("Word quiz")
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$score", style = type.hero, color = c.ink)
                Text("/$total", style = type.title, color = c.muted, modifier = Modifier.padding(bottom = ZeusSpacing.xs))
            }
            Text(if (perfect) "Perfect score!" else if (score * 2 >= total) "Great work!" else "Good practice!", style = type.headline, color = c.ink)
            Text("Words you missed come back sooner so they stick.", style = type.body, color = c.muted, textAlign = TextAlign.Center)
            PrimaryButton("Quiz me again", onAgain, modifier = Modifier.fillMaxWidth().widthIn(max = ZeusSizes.contentMaxWidth))
        }
    }
}

/* ----- Flashcards ----- */

@Composable
private fun FlashcardTab(words: List<VocabWordEntity>, due: List<VocabWordEntity>, onReview: (VocabWordEntity, ReviewGrade) -> Unit, modifier: Modifier) {
    var practiceAll by rememberSaveable { mutableStateOf(false) }
    val queue = if (due.isNotEmpty()) due else if (practiceAll) words else emptyList()
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = ZeusSpacing.gutter), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
        val card = queue.firstOrNull()
        if (card == null) {
            EmptyState(title = "All caught up", message = "Nothing is due right now. Come back later, or practise every word.", actionLabel = "Practise anyway", onAction = { practiceAll = true })
        } else {
            FlipWordCard(card, "${(words.size - queue.size + 1).coerceAtLeast(1)} of ${words.size}") { grade -> onReview(card, grade) }
        }
    }
}

@Composable
private fun FlipWordCard(word: VocabWordEntity, position: String, onGrade: (ReviewGrade) -> Unit) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    var flipped by remember(word.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, ZeusMotion.settle(ZeusTheme.reduceMotion, ZeusMotion.CARD_FLIP_MS), label = "flip")
    val back = rotation > 90f
    Text("Card $position", style = type.label, color = c.muted)
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ZeusSizes.wordCardMinHeight)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16f * density
            }
            .neonBorder(!back, ZeusShapes.cardLarge)
            .zeusCard(ZeusShapes.cardLarge)
            .clickable(role = Role.Button) { flipped = !flipped }
            .semantics { contentDescription = if (back) "Meaning of ${word.word}" else "${word.word}. Tap to flip." },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().graphicsLayer { rotationY = if (back) 180f else 0f }.padding(ZeusSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
        ) {
            if (!back) {
                Eyebrow(word.bookTitle.ifBlank { "From your reading" }, maxLines = 2)
                Text(word.word, style = type.display, color = c.ink, textAlign = TextAlign.Center)
                if (word.sentenceContext.isNotBlank()) Text("“${word.sentenceContext}”", style = type.quote, color = c.muted, textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                    Icon(Icons.Rounded.TouchApp, null, tint = c.muted, modifier = Modifier.size(ZeusSizes.iconSm))
                    Text("Tap to see the meaning", style = type.caption, color = c.muted)
                }
            } else {
                Eyebrow("Meaning")
                if (word.englishMeaning.isNotBlank()) Text(word.englishMeaning, style = type.bodyStrong, color = c.ink, textAlign = TextAlign.Center)
                if (word.translatedMeaning.isNotBlank()) Text(word.translatedMeaning, style = type.title.copy(fontFamily = ZeusType.uiFor(word.targetLang)), color = c.accentInk, textAlign = TextAlign.Center)
            }
        }
    }
    if (back) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
            SecondaryButton("Again", { onGrade(ReviewGrade.AGAIN) }, danger = true, height = ZeusSpacing.primaryButtonHeight, modifier = Modifier.weight(1f))
            SecondaryButton("Good", { onGrade(ReviewGrade.GOOD) }, height = ZeusSpacing.primaryButtonHeight, modifier = Modifier.weight(1f))
            PrimaryButton("Easy", { onGrade(ReviewGrade.EASY) }, shine = false, modifier = Modifier.weight(1f))
        }
    } else {
        PrimaryButton("Reveal meaning", { flipped = true }, modifier = Modifier.fillMaxWidth())
    }
}

/* ----- Saved ----- */

@Composable
private fun SavedTab(words: List<VocabWordEntity>, onDelete: (String) -> Unit, bottom: androidx.compose.ui.unit.Dp) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    LazyColumn(
        contentPadding = PaddingValues(start = ZeusSpacing.gutter, end = ZeusSpacing.gutter, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
    ) {
        itemsIndexed(words, key = { _, w -> w.id }) { i, w ->
            ZeusCard(Modifier.fillMaxWidth().staggerIn(i)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                            Text(w.word, style = type.headline, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            AccentBadge(w.targetLang.uppercase())
                        }
                        if (w.translatedMeaning.isNotBlank()) Text(w.translatedMeaning, style = type.bodyStrong.copy(fontFamily = ZeusType.uiFor(w.targetLang)), color = c.accentInk, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        if (w.englishMeaning.isNotBlank()) Text(w.englishMeaning, style = type.caption, color = c.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        val src = listOf(w.bookTitle, w.sentenceContext).filter { it.isNotBlank() }.joinToString(" · ")
                        if (src.isNotBlank()) Text(src, style = type.quote, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    CircleIconButton(Icons.Rounded.DeleteOutline, "Delete ${w.word}", { onDelete(w.id) })
                }
            }
        }
    }
}

