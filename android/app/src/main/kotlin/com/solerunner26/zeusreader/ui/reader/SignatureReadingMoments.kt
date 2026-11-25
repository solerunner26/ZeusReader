package com.solerunner26.zeusreader.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solerunner26.zeusreader.ui.theme.LocalZeusPalette
import kotlin.math.roundToInt

/**
 * 1. Fore-edge progress: vertical stack of ~40 hairlines at the right margin (12dp visual width,
 * 48dp touch target). Read = 1dp light, remaining = 2dp ink-soft, current = 3dp hue.
 */
@Composable
fun ForeEdgeProgress(
    progressFraction: Float,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalZeusPalette.current
    val clamped = progressFraction.coerceIn(0f, 1f)
    val percent = (clamped * 100f).roundToInt()

    Box(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .testTag("fore_edge_progress")
            .semantics { contentDescription = "$percent percent read" }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                    onSeekFraction(fraction)
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val fraction = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                    onSeekFraction(fraction)
                }
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        Canvas(
            modifier = Modifier
                .width(12.dp)
                .fillMaxHeight()
                .padding(vertical = 32.dp)
        ) {
            val lineCount = 40
            val activeIndex = (clamped * (lineCount - 1)).roundToInt()
            val stepY = size.height / (lineCount - 1).coerceAtLeast(1)

            for (i in 0 until lineCount) {
                val y = i * stepY
                when {
                    i == activeIndex -> drawLine(
                        color = palette.hue,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 3.dp.toPx()
                    )
                    i < activeIndex -> drawLine(
                        color = palette.ink.copy(alpha = 0.28f),
                        start = Offset(size.width * 0.35f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    else -> drawLine(
                        color = palette.inkSoft.copy(alpha = 0.50f),
                        start = Offset(size.width * 0.2f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }
}

/**
 * 2. Focus ribbon: keeps a 3-line band at 100% opacity with hairline hue rules;
 * dims text outside the band to ~35% contrast.
 */
@Composable
fun FocusRibbonOverlay(
    enabled: Boolean,
    bandFraction: Float,
    surfaceBackground: Color,
    modifier: Modifier = Modifier
) {
    if (!enabled) return
    val palette = LocalZeusPalette.current
    val clamped = bandFraction.coerceIn(0.1f, 0.9f)
    val dimColor = surfaceBackground.copy(alpha = 0.65f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("focus_ribbon_overlay")
    ) {
        val topWeight = (clamped - 0.08f).coerceAtLeast(0.02f)
        val bottomWeight = (1f - clamped - 0.08f).coerceAtLeast(0.02f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(topWeight)
                .background(dimColor)
        )
        HorizontalDivider(thickness = 1.dp, color = palette.hue)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
        )
        HorizontalDivider(thickness = 1.dp, color = palette.hue)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(bottomWeight)
                .background(dimColor)
        )
    }
}

/**
 * 3. Margin marks: 4dp hue bar in the left margin beside saved highlights/passages.
 */
@Composable
fun MarginMarkBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalZeusPalette.current
    Box(
        modifier = modifier
            .width(48.dp)
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag("margin_mark_bar")
            .semantics { contentDescription = "Open saved passage or highlight" },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .width(4.dp)
                .height(36.dp)
                .background(palette.hue, shape = MaterialTheme.shapes.small)
        )
    }
}

/**
 * 4. Ribbon bookmark: hue ribbon hanging at top-right (40dp tall, 64dp when chrome visible).
 * Pull down or tap to toggle bookmark.
 */
@Composable
fun RibbonBookmark(
    isBookmarked: Boolean,
    chromeVisible: Boolean,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalZeusPalette.current
    val ribbonHeight = if (chromeVisible) 64.dp else 40.dp
    val ribbonShape = GenericShape { size, _ ->
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, size.height)
        lineTo(size.width / 2f, size.height * 0.78f)
        lineTo(0f, size.height)
        close()
    }

    Box(
        modifier = modifier
            .width(48.dp)
            .heightIn(min = 48.dp)
            .testTag("ribbon_bookmark")
            .semantics {
                contentDescription = if (isBookmarked) "Remove ribbon bookmark" else "Add ribbon bookmark"
            }
            .pointerInput(isBookmarked) {
                detectTapGestures { onToggleBookmark() }
            }
            .pointerInput(isBookmarked) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onVerticalDrag = { _, dragAmount -> totalDrag += dragAmount },
                    onDragEnd = {
                        if (totalDrag > 24f) {
                            onToggleBookmark()
                        }
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(ribbonHeight)
                .clip(ribbonShape)
                .background(if (isBookmarked) palette.hue else palette.hueContainer)
        )
    }
}

/**
 * 5. Chapter breath: end-of-chapter card showing time read, passages saved, words learned,
 * and next chapter action.
 */
@Composable
fun ChapterBreathCard(
    chapterTitle: String,
    minutesRead: Int,
    passagesSaved: Int,
    wordsLearned: Int,
    onNextChapter: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("chapter_breath_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Chapter complete",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = chapterTitle,
                style = MaterialTheme.typography.headlineMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BreathStat("Time read", "${minutesRead.coerceAtLeast(1)} min")
                BreathStat("Passages saved", "$passagesSaved")
                BreathStat("Words learned", "$wordsLearned")
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Stay here")
                }
                Button(
                    onClick = onNextChapter,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Next chapter")
                }
            }
        }
    }
}

@Composable
private fun BreathStat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
