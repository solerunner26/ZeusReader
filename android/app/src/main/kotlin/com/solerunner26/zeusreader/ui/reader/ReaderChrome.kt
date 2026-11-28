package com.solerunner26.zeusreader.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.solerunner26.zeusreader.ui.components.GlassIconButton
import com.solerunner26.zeusreader.ui.components.MetaLabel
import com.solerunner26.zeusreader.ui.theme.StormlightSky
import com.solerunner26.zeusreader.ui.theme.ZeusMotion
import com.solerunner26.zeusreader.ui.theme.ZeusPalette
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.liquidGlass
import com.solerunner26.zeusreader.ui.theme.rememberPrimaryGradient
import kotlin.math.roundToInt

/** One tool in the Orb's Constellation menu. */
data class OrbTool(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val active: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Fly target: [count] stops (chapters or pages), [current] index, labels, and the jump action.
 * Drag the Orb to fly; release to land, slide to the left edge to cancel.
 */
data class FlyModel(
    val count: Int,
    val current: Int,
    val unitLabel: String,
    val labelFor: (Int) -> String,
    val onFly: (Int) -> Unit
)

/**
 * Stormlight reader frame (DESIGN.md §1, §6 "Reader"): loud sky outside, calm paper inside.
 * - Top: glass back button · title + meta · [actions]
 * - Middle: 30dp rounded paper sheet inset 8dp inside the sky (never overlapped by chrome)
 * - Bottom: ‹ progress track › and The Orb (tap = Constellation, drag = Fly)
 */
@Composable
fun StormlightReaderFrame(
    title: String,
    meta: String,
    paperColor: Color,
    onBack: () -> Unit,
    progressFraction: Float?,
    progressLabel: String,
    tools: List<OrbTool>,
    modifier: Modifier = Modifier,
    fly: FlyModel? = null,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    canGoPrevious: Boolean = true,
    canGoNext: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    var constellationOpen by remember { mutableStateOf(false) }
    var flyIndex by remember { mutableIntStateOf(-1) }
    var flyCancel by remember { mutableStateOf(false) }

    BackHandler(enabled = constellationOpen) { constellationOpen = false }

    StormlightSky(animate = !ZeusTheme.reduceMotion, intensity = 0.35f, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ZeusSpacing.topBarHeight)
                    .padding(horizontal = ZeusSpacing.md, vertical = ZeusSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back to library", onBack)
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = type.label.copy(fontFamily = type.Unbounded), color = colors.textOnSky, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (meta.isNotBlank()) {
                        MetaLabel(text = meta)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xs), verticalAlignment = Alignment.CenterVertically, content = actions)
            }

            // Paper sheet
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = ZeusSpacing.pageInset)
                    .shadow(ZeusSizes.shadowSoft, ZeusShapes.paperSheet)
                    .clip(ZeusShapes.paperSheet)
                    .background(paperColor)
                    .testTag("reader_paper_sheet"),
                content = content
            )

            // Bottom: Fly scrubber + Orb
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ZeusSpacing.md, end = ZeusSpacing.md, top = ZeusSpacing.sm, bottom = ZeusSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
            ) {
                if (onPrevious != null) {
                    GlassIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous page", onPrevious, enabled = canGoPrevious)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                    MetaLabel(text = progressLabel)
                    if (progressFraction != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ZeusSizes.progressTrack)
                                .clip(ZeusShapes.pill)
                                .background(colors.trackInactive)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                    .height(ZeusSizes.progressTrack)
                                    .clip(ZeusShapes.pill)
                                    .background(rememberPrimaryGradient(animate = false))
                            )
                        }
                    }
                }
                if (onNext != null) {
                    GlassIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next page", onNext, enabled = canGoNext)
                }
                TheOrb(
                    open = constellationOpen,
                    onTap = { constellationOpen = !constellationOpen },
                    fly = fly,
                    onFlyChange = { idx, cancel -> flyIndex = idx; flyCancel = cancel },
                    onFlyEnd = { idx, cancel ->
                        if (!cancel && fly != null && idx in 0 until fly.count && idx != fly.current) fly.onFly(idx)
                        flyIndex = -1
                    }
                )
            }
        }

        // Constellation (tool bloom) overlay
        AnimatedVisibility(
            visible = constellationOpen,
            enter = fadeIn(ZeusMotion.settleTween(ZeusTheme.reduceMotion, 160)),
            exit = fadeOut(ZeusMotion.settleTween(ZeusTheme.reduceMotion, 120))
        ) {
            Constellation(
                tools = tools,
                onDismiss = { constellationOpen = false }
            )
        }

        // Fly overlay
        if (fly != null && flyIndex >= 0) {
            FlyOverlay(fly = fly, index = flyIndex, cancel = flyCancel)
        }
    }
}

/** The Orb: 68dp glowing gradient control with a 3s pulse. */
@Composable
private fun TheOrb(
    open: Boolean,
    onTap: () -> Unit,
    fly: FlyModel?,
    onFlyChange: (Int, Boolean) -> Unit,
    onFlyEnd: (Int, Boolean) -> Unit
) {
    val colors = ZeusTheme.colors
    val sky = ZeusTheme.sky
    val pulse = if (!ZeusTheme.reduceMotion) {
        val t = rememberInfiniteTransition(label = "orb_pulse")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(ZeusMotion.ORB_PULSE_MS), RepeatMode.Reverse), label = "orb_pulse_v")
    } else {
        null
    }
    val eink = ZeusTheme.paper.isEink
    val view = androidx.compose.ui.platform.LocalView.current

    Box(
        modifier = Modifier
            .size(ZeusSpacing.orbSize)
            .drawBehind {
                if (!eink) {
                    val p = pulse?.value ?: 0.5f
                    drawCircle(
                        brush = Brush.radialGradient(listOf(sky.glow2.copy(alpha = 0.35f + 0.25f * p), Color.Transparent)),
                        radius = size.minDimension * (0.62f + 0.12f * p)
                    )
                }
            }
            .shadow(ZeusSizes.shadowGlow, ZeusShapes.orb, ambientColor = sky.glow2, spotColor = sky.glow2)
            .clip(ZeusShapes.orb)
            .background(rememberPrimaryGradient())
            .semantics {
                contentDescription = if (open) "Close tools" else "Reader tools. Drag up or down to fly through the book."
                onClick { onTap(); true }
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
            .pointerInput(fly) {
                val model = fly ?: return@pointerInput
                if (model.count <= 1) return@pointerInput
                var dragY = 0f
                var dragX = 0f
                fun height() = view.height.toFloat().coerceAtLeast(1f)
                fun width() = view.width.toFloat().coerceAtLeast(1f)
                fun target() = ((dragY / height()) * (model.count - 1)).roundToInt().coerceIn(0, model.count - 1)
                fun cancelled() = dragX < width() * 0.25f
                detectDragGestures(
                    onDragStart = {
                        dragY = model.current.toFloat() / (model.count - 1).coerceAtLeast(1) * height()
                        dragX = width()
                        onFlyChange(model.current, false)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragY = (dragY + amount.y).coerceIn(0f, height())
                        dragX = (dragX + amount.x).coerceIn(0f, width())
                        onFlyChange(target(), cancelled())
                    },
                    onDragEnd = { onFlyEnd(target(), cancelled()) },
                    onDragCancel = { onFlyEnd(-1, true) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (open) Icons.Rounded.Close else Icons.Rounded.Bolt,
            contentDescription = null,
            tint = colors.textOnAccent,
            modifier = Modifier.size(ZeusSizes.orbIcon)
        )
    }
}

/**
 * Constellation: tools rise from the Orb as a zig-zag chain of glass pills joined by faint
 * star lines; each pops in with a 40ms stagger (spring). Tap outside to close.
 */
@Composable
private fun Constellation(tools: List<OrbTool>, onDismiss: () -> Unit) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val sky = ZeusTheme.sky
    val reduceMotion = ZeusTheme.reduceMotion
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .navigationBarsPadding()
            .testTag("orb_constellation")
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = ZeusSpacing.md, bottom = ZeusSpacing.orbSize + ZeusSpacing.lg)
                .widthIn(max = ZeusSizes.contentMaxWidth),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.sm),
            horizontalAlignment = Alignment.End
        ) {
            tools.forEachIndexed { index, tool ->
                val anim = remember { Animatable(if (reduceMotion) 1f else 0f) }
                LaunchedEffect(Unit) {
                    if (!reduceMotion) {
                        kotlinx.coroutines.delay((tools.size - index) * ZeusMotion.CONSTELLATION_STAGGER_MS.toLong())
                        anim.animateTo(1f, ZeusMotion.zapSpring(false))
                    }
                }
                val zig = if (index % 2 == 0) ZeusSpacing.none else ZeusSpacing.xxl
                Row(
                    modifier = Modifier
                        .padding(end = zig)
                        .graphicsLayer {
                            val v = anim.value
                            alpha = v.coerceIn(0f, 1f)
                            scaleX = 0.6f + 0.4f * v
                            scaleY = 0.6f + 0.4f * v
                            translationY = (1f - v) * 60f
                        }
                        .heightIn(min = ZeusSpacing.touchMin)
                        .clip(ZeusShapes.pill)
                        .background(if (tool.active) colors.chipSelectedBg else colors.sheetBackground)
                        .liquidGlass(shape = ZeusShapes.pill)
                        .clickable(enabled = tool.enabled, role = Role.Button) {
                            onDismiss()
                            tool.onClick()
                        }
                        .padding(start = ZeusSpacing.lg, end = ZeusSpacing.xs, top = ZeusSpacing.xs, bottom = ZeusSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
                ) {
                    Text(
                        text = tool.label,
                        style = type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = when {
                            !tool.enabled -> colors.textOnSkyMuted
                            tool.active -> colors.chipSelectedFg
                            else -> colors.textOnSky
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .size(ZeusSpacing.touchMin - ZeusSpacing.xs)
                            .clip(ZeusShapes.circle)
                            .background(
                                if (index % 2 == 0) Brush.linearGradient(listOf(sky.accent, sky.glow2))
                                else Brush.linearGradient(listOf(sky.glow1, sky.glow2))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tool.icon, contentDescription = null, tint = colors.textOnAccent, modifier = Modifier.size(ZeusSizes.iconMd))
                    }
                }
            }
        }
    }
}

/** Fly: giant stop number, label and a planet rail of nearby stops. */
@Composable
private fun FlyOverlay(fly: FlyModel, index: Int, cancel: Boolean) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val sky = ZeusTheme.sky
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.skyTop.copy(alpha = 0.96f), colors.skyBottom.copy(alpha = 0.96f))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(ZeusSpacing.gutter)
            .testTag("fly_overlay")
    ) {
        Column(modifier = Modifier.align(Alignment.TopStart), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
            MetaLabel(text = "Flying through", accent = true)
            Text("${index + 1}", style = type.displayGiant, color = colors.textOnSky, maxLines = 1)
            MetaLabel(text = "of ${fly.count} ${fly.unitLabel} · ${((index + 1) * 100 / fly.count.coerceAtLeast(1))}%")
            Spacer(Modifier.height(ZeusSpacing.md))
            Text(fly.labelFor(index), style = type.headline, color = colors.textOnSky, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = ZeusSizes.coverHeroWidth * 1.6f))
        }
        // Planet rail
        Column(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md),
            horizontalAlignment = Alignment.End
        ) {
            val start = (index - 3).coerceAtLeast(0)
            val end = (start + 7).coerceAtMost(fly.count)
            for (i in start until end) {
                val active = i == index
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
                    Text(
                        fly.labelFor(i),
                        style = if (active) type.title.copy(fontFamily = type.Unbounded) else type.caption,
                        color = if (active) colors.textOnSky else colors.textOnSkyMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(max = ZeusSizes.coverHeroWidth)
                    )
                    Box(
                        modifier = Modifier
                            .size(if (active) ZeusSizes.flyPlanetActive else ZeusSizes.flyPlanet)
                            .clip(ZeusShapes.circle)
                            .background(if (active) Brush.radialGradient(listOf(sky.accent, sky.glow2)) else Brush.linearGradient(listOf(colors.glassFillStrong, colors.glassFill))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i + 1}", style = if (active) type.numberMedium else type.metaSmall, color = if (active) colors.textOnAccent else colors.textOnSky, maxLines = 1)
                    }
                }
            }
        }
        Row(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            Text(if (cancel) "Release to cancel" else "Let go to land", style = type.label, color = if (cancel) colors.errorGlow else colors.textOnSky, modifier = Modifier.weight(1f))
            Text("Slide left to cancel", style = type.caption, color = colors.textOnSkySecondary)
        }
    }
}

/** Paper-aware small pill used inside the reading sheet (search hits, lens chips…). */
@Composable
fun PaperPill(
    label: String,
    selected: Boolean,
    ink: Color,
    paper: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = ZeusSpacing.chipHeight)
            .clip(ZeusShapes.pill)
            .background(if (selected) ink else ink.copy(alpha = 0.06f))
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
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(ZeusPalette.warmLight.copy(alpha = strength))
        )
    }
}

