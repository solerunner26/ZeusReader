package com.solerunner26.zeusreader.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/** Elevated card: soft diffuse shadow + card fill + hairline (Sunset "depth, not clutter"). */
@Composable
fun Modifier.zeusCard(shape: Shape = ZeusShapes.card, elevation: Dp = ZeusSizes.shadowCard): Modifier {
    val c = ZeusTheme.colors
    return this
        .then(if (c.isEink) Modifier else Modifier.shadow(elevation, shape, ambientColor = c.shadow, spotColor = c.shadow))
        .clip(shape)
        .background(c.card, shape)
        .border(ZeusSpacing.borderWidth, c.line, shape)
}

/** Accent ring + soft glow marking the selected / active item. */
@Composable
fun Modifier.neonBorder(selected: Boolean = true, shape: Shape = ZeusShapes.card, width: Dp = ZeusSpacing.neonBorderWidth): Modifier {
    if (!selected) return this
    val c = ZeusTheme.colors
    return this.drawWithContent {
        drawContent()
        val outline = shape.createOutline(size, layoutDirection, this)
        val px = width.toPx()
        if (!c.isEink) drawOutline(outline, color = c.glow, style = Stroke(px * 5f), alpha = 0.45f)
        drawOutline(outline, color = c.accent, style = Stroke(px))
    }
}

/** Phase 0→1 looping (static when motion is reduced). */
@Composable
fun rememberLoopPhase(durationMillis: Int, reverse: Boolean = false, animate: Boolean = !ZeusTheme.reduceMotion): State<Float> {
    if (!animate) return remember { mutableFloatStateOf(0.25f) }
    val t = rememberInfiniteTransition(label = "zeus_loop")
    return t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing), if (reverse) RepeatMode.Reverse else RepeatMode.Restart),
        label = "zeus_loop_value"
    )
}

/** The accent gradient (g1 → g2, 135°). With [shine], it slowly slides like the mockup's buttons. */
@Composable
fun rememberAccentBrush(shine: Boolean = false): Brush {
    val c = ZeusTheme.colors
    if (!c.gradientEnabled) return Brush.linearGradient(listOf(c.accent, c.accent))
    if (!shine || ZeusTheme.reduceMotion) return Brush.linearGradient(listOf(c.accent, c.accent2))
    val p by rememberLoopPhase(ZeusMotion.SHINE_MS)
    return Brush.linearGradient(
        colors = listOf(c.accent, c.accent2, c.accent),
        start = Offset(-900f + 1800f * p, 0f),
        end = Offset(0f + 1800f * p, 400f),
        tileMode = TileMode.Mirror
    )
}

/** Spring press: scales to 0.96 while pressed. Pass the same [interactionSource] to `clickable`. */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val reduce = ZeusTheme.reduceMotion
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduce) ZeusMotion.PRESS_SCALE else 1f,
        animationSpec = ZeusMotion.press(),
        label = "press_scale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
