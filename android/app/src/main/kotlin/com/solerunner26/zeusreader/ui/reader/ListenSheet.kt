package com.solerunner26.zeusreader.ui.reader

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.ui.components.GlassIconButton
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.ZeusChip
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.ZeusType
import com.solerunner26.zeusreader.ui.theme.rememberPrimaryGradient
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

/** Voices Listen can read in. */
enum class ListenVoice(val label: String, val tag: String) {
    EN("English", "en"),
    HI("हिन्दी", "hi"),
    GU("ગુજરાતી", "gu")
}

/** Splits [text] into readable sentences for Listen. */
internal fun listenSentences(text: String): List<String> =
    text.split(Regex("(?<=[.!?।])\\s+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }

/**
 * Listen (DESIGN.md §2): karaoke read-aloud of [text] with word highlighting driven by TTS word
 * boundaries, waveform, ±1 sentence skip, speed, voice and sleep timer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListenSheet(
    title: String,
    sectionLabel: String,
    text: String,
    onDismiss: () -> Unit,
    initialVoice: ListenVoice = ListenVoice.EN
) {
    val context = LocalContext.current
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val scope = rememberCoroutineScope()
    val sentences = remember(text) { listenSentences(text) }

    var ready by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var sentenceIndex by remember { mutableIntStateOf(0) }
    var wordStart by remember { mutableIntStateOf(-1) }
    var wordEnd by remember { mutableIntStateOf(-1) }
    var speed by remember { mutableFloatStateOf(1f) }
    var voice by remember { mutableStateOf(initialVoice) }

    val tts = remember(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status -> if (status == TextToSpeech.SUCCESS) ready = true }
        instance
    }
    val sleepTimer = remember(tts) {
        TtsSleepTimerController(scope = scope, onExpired = {
            runCatching { tts.stop() }
            playing = false
        })
    }
    val sleepState by sleepTimer.state.collectAsStateWithLifecycle()

    fun speakFrom(index: Int) {
        if (!ready || sentences.isEmpty()) return
        val i = index.coerceIn(0, sentences.lastIndex)
        sentenceIndex = i
        wordStart = -1
        wordEnd = -1
        tts.language = Locale.forLanguageTag(voice.tag)
        tts.setSpeechRate(speed)
        tts.speak(sentences[i], TextToSpeech.QUEUE_FLUSH, null, "listen_$i")
        playing = true
    }

    DisposableEffect(tts) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                wordStart = start
                wordEnd = end
            }
            override fun onDone(utteranceId: String?) {
                val next = (utteranceId?.removePrefix("listen_")?.toIntOrNull() ?: sentenceIndex) + 1
                if (next <= sentences.lastIndex && playing) {
                    sentenceIndex = next
                    wordStart = -1
                    tts.speak(sentences[next], TextToSpeech.QUEUE_FLUSH, null, "listen_$next")
                } else {
                    playing = false
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                playing = false
            }
        })
        onDispose {
            sleepTimer.cancel()
            runCatching {
                tts.stop()
                tts.shutdown()
            }
        }
    }

    ZeusBottomSheet(
        onDismissRequest = onDismiss,
        title = "Listen",
        subtitle = "$title · $sectionLabel"
    ) {
        if (sentences.isEmpty()) {
            Text("Nothing to read aloud on this page yet.", style = type.body, color = colors.textOnSkySecondary)
            return@ZeusBottomSheet
        }
        MetaLabel(text = "Sentence ${sentenceIndex + 1} of ${sentences.size}", accent = true)
        val current = sentences[sentenceIndex]
        val karaoke = remember(current, wordStart, wordEnd, colors) {
            buildAnnotatedString {
                if (wordStart in 0 until wordEnd && wordEnd <= current.length) {
                    withStyle(SpanStyle(color = colors.textOnSky)) { append(current.substring(0, wordStart)) }
                    withStyle(SpanStyle(color = colors.textOnAccent, background = colors.sky.accent)) {
                        append(current.substring(wordStart, wordEnd))
                    }
                    withStyle(SpanStyle(color = colors.textOnSkyMuted)) { append(current.substring(wordEnd)) }
                } else {
                    withStyle(SpanStyle(color = colors.textOnSky)) { append(current) }
                }
            }
        }
        val readingStyle = ZeusType.readingFor(voice.tag, type.readingHeadline.copy(fontSize = type.headline.fontSize))
        Text(karaoke, style = readingStyle)

        Waveform(playing = playing)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(Icons.Rounded.Replay10, "Previous sentence", { speakFrom(sentenceIndex - 1) }, size = ZeusSizes.skipButton)
            Box(
                modifier = Modifier
                    .size(ZeusSizes.listenOrb)
                    .shadow(ZeusSizes.shadowGlow, ZeusShapes.circle, ambientColor = ZeusTheme.sky.glow2, spotColor = ZeusTheme.sky.glow2)
                    .clip(ZeusShapes.circle)
                    .background(rememberPrimaryGradient())
                    .clickable(role = Role.Button, enabled = ready) {
                        if (playing) {
                            tts.stop()
                            playing = false
                        } else {
                            speakFrom(sentenceIndex)
                        }
                    }
                    .semantics { contentDescription = if (playing) "Pause" else "Play" },
                contentAlignment = Alignment.Center
            ) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = colors.textOnAccent, modifier = Modifier.size(ZeusSizes.iconXl))
            }
            GlassIconButton(Icons.Rounded.Forward10, "Next sentence", { speakFrom(sentenceIndex + 1) }, size = ZeusSizes.skipButton)
        }
        if (!ready) MetaLabel(text = "Preparing voice…")

        MetaLabel(text = "Speed", accent = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
            listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                ZeusChip("${s}×", abs(speed - s) < 0.01f, {
                    speed = s
                    if (playing) speakFrom(sentenceIndex)
                })
            }
        }
        MetaLabel(text = "Voice", accent = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
            ListenVoice.entries.forEach { v ->
                ZeusChip(v.label, voice == v, {
                    voice = v
                    if (playing) speakFrom(sentenceIndex)
                })
            }
        }
        MetaLabel(text = "Sleep timer", accent = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
            TtsSleepTimerOption.entries.forEach { opt ->
                val label = if (opt == TtsSleepTimerOption.OFF) "Off" else "${opt.durationMinutes}m"
                ZeusChip(
                    label = if (sleepState.selectedOption == opt && sleepState.isActive) "${(sleepState.remainingSeconds + 59) / 60}m left" else label,
                    selected = sleepState.selectedOption == opt,
                    onClick = { sleepTimer.select(opt) },
                    icon = if (opt == TtsSleepTimerOption.OFF) null else Icons.Rounded.Bedtime
                )
            }
        }
    }
}

/** Animated waveform bars (static under Reduce Motion or when paused). */
@Composable
private fun Waveform(playing: Boolean) {
    val sky = ZeusTheme.sky
    val colors = ZeusTheme.colors
    val animate = playing && !ZeusTheme.reduceMotion
    val phase = if (animate) {
        val t = rememberInfiniteTransition(label = "wave")
        t.animateFloat(0f, 6.283f, infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "wave_phase")
    } else {
        null
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(ZeusSizes.waveformHeight)
    ) {
        val bars = 34
        val gap = size.width / bars
        val barW = ZeusSizes.waveformBar.toPx()
        val p = phase?.value ?: 0f
        val brush = Brush.verticalGradient(listOf(sky.accent, sky.glow2))
        for (i in 0 until bars) {
            val h = size.height * (0.25f + 0.7f * ((sin(i * 0.7f + p) + 1f) / 2f) * (0.5f + 0.5f * sin(i * 0.23f).let { it * it }))
            val x = i * gap + (gap - barW) / 2f
            if (i < bars / 2 || playing) {
                drawRoundRect(brush = brush, topLeft = Offset(x, (size.height - h) / 2f), size = Size(barW, h), cornerRadius = CornerRadius(barW / 2f))
            } else {
                drawRoundRect(color = colors.trackInactive, topLeft = Offset(x, (size.height - h) / 2f), size = Size(barW, h), cornerRadius = CornerRadius(barW / 2f))
            }
        }
    }
}
