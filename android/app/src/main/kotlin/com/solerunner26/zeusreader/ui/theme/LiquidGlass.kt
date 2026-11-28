package com.solerunner26.zeusreader.ui.theme

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Stormlight v4 translucent glass surface (DESIGN.md §3):
 * white 8–16% fill + 1dp luminous border, or sky base at 85% when glass is off.
 * In Daylight mood the glass is frosted white with an ink hairline.
 */
@Composable
fun Modifier.liquidGlass(
    enabled: Boolean = ZeusTheme.glass,
    shape: Shape = ZeusShapes.card,
    alpha: Float = 0.10f
): Modifier {
    val colors = ZeusTheme.colors
    val fill = if (!enabled) {
        Brush.verticalGradient(listOf(colors.glassFallback, colors.glassFallback))
    } else if (colors.isLight) {
        Brush.verticalGradient(listOf(colors.glassFillStrong, colors.glassFill))
    } else {
        val a = alpha.coerceIn(0.06f, 0.16f)
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = (a + 0.04f).coerceAtMost(0.20f)), Color.White.copy(alpha = a))
        )
    }
    val border = Brush.linearGradient(
        listOf(colors.glassHighlight, colors.neonA.copy(alpha = if (colors.isLight) 0.30f else 0.26f), colors.glassBorder)
    )
    return this
        .clip(shape)
        .background(fill, shape)
        .border(ZeusSpacing.glassBorderWidth, border, shape)
}

/** Animated phase 0→1 for neon sweeps / gradient shimmer; static when motion is reduced. */
@Composable
fun rememberStormlightPhase(durationMillis: Int = ZeusMotion.NEON_SWEEP_MS, animate: Boolean = !ZeusTheme.reduceMotion): State<Float> {
    if (!animate) return remember { mutableFloatStateOf(0.12f) }
    val transition = rememberInfiniteTransition(label = "stormlight_phase")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "stormlight_phase_value"
    )
}

/**
 * Neon border: a two-tone (glow1 → glow2) luminous stroke with a soft outer glow.
 * When [animate] the gradient rotates around the shape (drawn in the draw phase only).
 */
@Composable
fun Modifier.neonBorder(
    shape: Shape = ZeusShapes.card,
    width: Dp = ZeusSpacing.neonBorderWidth,
    animate: Boolean = true,
    intensity: Float = 1f
): Modifier {
    if (!ZeusTheme.neon) return this
    val colors = ZeusTheme.colors
    val phase = rememberStormlightPhase(animate = animate && ZeusTheme.neonAnimated)
    val a = colors.neonA
    val b = colors.neonB
    val glowAlpha = (if (colors.isLight) 0.18f else 0.30f) * intensity
    return this.drawWithContent {
        drawContent()
        val angle = phase.value * 6.2832f
        val r = size.maxDimension
        val c = Offset(size.width / 2f, size.height / 2f)
        val start = Offset(c.x + cos(angle) * r, c.y + sin(angle) * r)
        val end = Offset(c.x - cos(angle) * r, c.y - sin(angle) * r)
        val brush = Brush.linearGradient(listOf(a, b, a), start = start, end = end, tileMode = TileMode.Mirror)
        val outline: Outline = shape.createOutline(size, layoutDirection, this)
        val strokePx = width.toPx()
        drawOutline(outline, brush = brush, alpha = glowAlpha, style = Stroke(width = strokePx * 5f))
        drawOutline(outline, brush = brush, alpha = (0.9f * intensity).coerceAtMost(1f), style = Stroke(width = strokePx))
    }
}

/** Two-tone gradient brush (accent → glow2) for primary pills, optionally shimmering. */
@Composable
fun rememberPrimaryGradient(animate: Boolean = false): Brush {
    val sky = ZeusTheme.sky
    val colors = ZeusTheme.colors
    if (ZeusTheme.paper.isEink) return Brush.linearGradient(listOf(colors.textOnSky, colors.textOnSky))
    val phase = rememberStormlightPhase(durationMillis = ZeusMotion.SHIMMER_MS, animate = animate && ZeusTheme.shimmer)
    val p = phase.value
    return Brush.linearGradient(
        colors = listOf(sky.accent, sky.glow2, sky.accent),
        start = Offset(-600f + 1200f * p, 0f),
        end = Offset(600f + 1200f * p, 300f),
        tileMode = TileMode.Mirror
    )
}

/** Animated two-tone text brush (glow1 ↔ glow2 / accent) for hero headings. */
@Composable
fun rememberHeadingBrush(animate: Boolean = true): Brush {
    val colors = ZeusTheme.colors
    val sky = ZeusTheme.sky
    if (ZeusTheme.paper.isEink) return Brush.linearGradient(listOf(colors.textOnSky, colors.textOnSky))
    val phase = rememberStormlightPhase(durationMillis = ZeusMotion.SHIMMER_MS, animate = animate && ZeusTheme.shimmer)
    val p = phase.value
    val second = if (colors.isLight) sky.accentInk else sky.accent
    return Brush.linearGradient(
        colors = listOf(colors.textOnSky, second, colors.textOnSky),
        start = Offset(-800f + 1600f * p, 0f),
        end = Offset(0f + 1600f * p, 200f),
        tileMode = TileMode.Mirror
    )
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    glassEnabled: Boolean = ZeusTheme.glass,
    cornerRadius: Dp = 28.dp,
    animateSize: Boolean = true,
    neon: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    GlassSurface(
        modifier = modifier,
        shape = shape,
        glassEnabled = glassEnabled,
        neon = neon,
        animateSize = animateSize
    ) { content() }
}

/** Base glass container used by every card in the app. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = ZeusShapes.card,
    glassEnabled: Boolean = ZeusTheme.glass,
    neon: Boolean = false,
    neonAnimated: Boolean = true,
    animateSize: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = ZeusTheme.colors
    val reduceMotion = ZeusTheme.reduceMotion
    var m = modifier
    if (neon) m = m.neonBorder(shape = shape, animate = neonAnimated)
    m = m.liquidGlass(enabled = glassEnabled, shape = shape)
    if (animateSize && !reduceMotion) m = m.animateContentSize(ZeusMotion.zapSpring(reduceMotion = false))
    CompositionLocalProvider(LocalContentColor provides colors.textOnSky) {
        Box(modifier = m, content = content)
    }
}
