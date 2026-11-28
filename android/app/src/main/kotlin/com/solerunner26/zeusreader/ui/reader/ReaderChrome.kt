package com.solerunner26.zeusreader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.solerunner26.zeusreader.ui.components.CappedFontScale
import com.solerunner26.zeusreader.ui.components.CircleIconButton
import com.solerunner26.zeusreader.ui.components.MenuRow
import com.solerunner26.zeusreader.ui.components.PrimaryButton
import com.solerunner26.zeusreader.ui.components.ProgressBar
import com.solerunner26.zeusreader.ui.components.ZeusBottomSheet
import com.solerunner26.zeusreader.ui.components.ZeusSlider
import com.solerunner26.zeusreader.ui.theme.ZeusBackground
import com.solerunner26.zeusreader.ui.theme.ZeusPalette
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.pressScale
import com.solerunner26.zeusreader.ui.theme.zeusCard
import kotlin.math.roundToInt

/** One reader tool (bottom toolbar or the More sheet). */
data class ReaderTool(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val active: Boolean = false,
    val onClick: () -> Unit
)

/** "Jump to" target: [count] stops (chapters or pages), the [current] one, labels and the jump. */
data class JumpModel(
    val count: Int,
    val current: Int,
    val unitLabel: String,
    val labelFor: (Int) -> String,
    val onJump: (Int) -> Unit
)

/**
 * Reader scaffold (design/sunset): calm canvas, top progress with fraction, a rounded paper card
 * (decoration never touches the words), page row and a bottom toolbar. Tap the progress to jump.
 */
@Composable
fun ReaderFrame(
    title: String,
    meta: String,
    paperColor: Color,
    onBack: () -> Unit,
    progressFraction: Float?,
    progressLabel: String,
    tools: List<ReaderTool>,
    modifier: Modifier = Modifier,
    jump: JumpModel? = null,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    canGoPrevious: Boolean = true,
    canGoNext: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val c = ZeusTheme.colors
    val type = ZeusTheme.type
    var showMore by remember { mutableStateOf(false) }
    var showJump by remember { mutableStateOf(false) }
    val visible = if (tools.size > 5) tools.take(4) else tools
    val overflow = if (tools.size > 5) tools.drop(4) else emptyList()

    ZeusBackground(glow = false, modifier = modifier) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Top: back · chapter + fraction + thin progress · actions
            Row(
                Modifier.fillMaxWidth().padding(horizontal = ZeusSpacing.md, vertical = ZeusSpacing.xsPlus),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
            ) {
                CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back to library", onBack)
                Column(
                    Modifier
                        .weight(1f)
                        .clip(ZeusShapes.tile)
                        .then(if (jump != null) Modifier.clickable(role = Role.Button) { showJump = true } else Modifier)
                        .semantics { contentDescription = "$title. $meta. $progressLabel" }
                        .padding(vertical = ZeusSpacing.xxs),
                    verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                        Text(meta.ifBlank { title }, style = type.label, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(progressLabel, style = type.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = c.accentInk, maxLines = 1)
                    }
                    if (progressFraction != null) ProgressBar(progressFraction, height = ZeusSizes.progressThin)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xs), content = actions)
            }

            // Paper
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = ZeusSpacing.sm, vertical = ZeusSpacing.xs)
                    .zeusCard(ZeusShapes.paper)
                    .background(paperColor)
                    .testTag("reader_paper_sheet"),
                content = content
            )

            // Bottom toolbar with page turns at both ends
            Row(
                Modifier.fillMaxWidth().padding(horizontal = ZeusSpacing.sm, vertical = ZeusSpacing.xsPlus),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)
            ) {
                if (onPrevious != null) CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous page", onPrevious, enabled = canGoPrevious)
                CappedFontScale(1.3f) {
                    Row(
                        Modifier
                            .weight(1f)
                            .heightIn(min = ZeusSpacing.navHeight - ZeusSpacing.sm)
                            .zeusCard(ZeusShapes.nav, elevation = ZeusSizes.shadowLift)
                            .padding(horizontal = ZeusSpacing.xxs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        visible.forEach { tool -> ToolButton(tool, Modifier.weight(1f)) }
                        if (overflow.isNotEmpty()) ToolButton(ReaderTool("More", Icons.Rounded.MoreHoriz, active = showMore) { showMore = true }, Modifier.weight(1f))
                    }
                }
                if (onNext != null) CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next page", onNext, enabled = canGoNext)
            }
        }
    }

    if (showMore) {
        ZeusBottomSheet(onDismissRequest = { showMore = false }, title = "More tools") {
            overflow.forEach { tool ->
                MenuRow(
                    title = tool.label,
                    icon = tool.icon,
                    onClick = if (tool.enabled) ({ showMore = false; tool.onClick() }) else null,
                    subtitle = if (tool.active) "On" else null,
                    chevron = tool.enabled
                )
            }
        }
    }
    if (showJump && jump != null) {
        var target by remember { mutableFloatStateOf(jump.current.toFloat()) }
        val idx = target.roundToInt().coerceIn(0, (jump.count - 1).coerceAtLeast(0))
        ZeusBottomSheet(onDismissRequest = { showJump = false }, title = "Jump to", subtitle = "${jump.count} ${jump.unitLabel}") {
            Text("${idx + 1}", style = type.hero, color = c.ink)
            Text(jump.labelFor(idx), style = type.headline, color = c.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
            ZeusSlider(target, { target = it }, valueRange = 0f..(jump.count - 1).coerceAtLeast(1).toFloat())
            PrimaryButton("Go to ${jump.unitLabel.removeSuffix("s")} ${idx + 1}", { jump.onJump(idx); showJump = false }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ToolButton(tool: ReaderTool, modifier: Modifier = Modifier) {
    val c = ZeusTheme.colors
    val source = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxHeight()
            .heightIn(min = ZeusSpacing.touchMin)
            .pressScale(source)
            .clip(ZeusShapes.tile)
            .clickable(source, indication = androidx.compose.material3.ripple(), enabled = tool.enabled, role = Role.Button, onClick = tool.onClick)
            .padding(vertical = ZeusSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xxs, Alignment.CenterVertically)
    ) {
        val tint = when {
            !tool.enabled -> c.muted.copy(alpha = 0.5f)
            tool.active -> c.accentInk
            else -> c.muted
        }
        Box(
            Modifier
                .size(width = ZeusSizes.navPillWidth - ZeusSpacing.sm, height = ZeusSizes.navPillHeight - ZeusSpacing.xs)
                .clip(ZeusShapes.pill)
                .then(if (tool.active) Modifier.background(c.tint) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(tool.icon, null, tint = tint, modifier = Modifier.size(ZeusSizes.iconMd))
        }
        Text(tool.label, style = ZeusTheme.type.tiny, color = if (tool.active) c.ink else c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Paper-aware small pill used inside the reading page (search hits, lens chips…). */
@Composable
fun PaperPill(label: String, selected: Boolean, ink: Color, paper: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = ZeusSpacing.chipHeight)
            .clip(ZeusShapes.pill)
            .background(if (selected) ink else ink.copy(alpha = 0.07f))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = ZeusSpacing.mdPlus, vertical = ZeusSpacing.xsPlus),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = ZeusTheme.type.label, color = if (selected) paper else ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Warm-light filter overlay used by every reader. */
@Composable
fun BoxScope.WarmLightOverlay(strength: Float) {
    if (strength > 0.01f) {
        Box(Modifier.matchParentSize().background(ZeusPalette.warmLight.copy(alpha = strength)))
    }
}

