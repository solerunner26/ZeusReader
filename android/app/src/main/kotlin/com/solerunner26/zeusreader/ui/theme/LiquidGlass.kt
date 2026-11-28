package com.solerunner26.zeusreader.ui.theme

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Stormlight v4 translucent glass surface modifier (DESIGN.md §3).
 * Uses rgba(255,255,255,0.08–0.12) fill and rgba(255,255,255,0.16–0.25) 1dp border,
 * or sky base at 85% opacity when glass is disabled.
 */
@Composable
fun Modifier.liquidGlass(
    enabled: Boolean = true,
    shape: Shape = ZeusShapes.card,
    alpha: Float = 0.10f
): Modifier {
    val colors = ZeusTheme.colors
    val effectiveAlpha = alpha.coerceIn(0.08f, 0.14f)
    val bgBrush = if (enabled) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = (effectiveAlpha + 0.03f).coerceAtMost(0.16f)),
                Color.White.copy(alpha = effectiveAlpha)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(colors.glassFallback, colors.glassFallback)
        )
    }
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (enabled) 0.25f else 0.16f),
            colors.sky.glow1.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.12f)
        )
    )
    return this
        .clip(shape)
        .background(bgBrush, shape)
        .border(BorderStroke(ZeusSpacing.glassBorderWidth, borderBrush), shape)
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    glassEnabled: Boolean = true,
    cornerRadius: Dp = 28.dp,
    animateSize: Boolean = true,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val reduceMotion = ZeusTheme.reduceMotion
    val sizeMod = if (animateSize && !reduceMotion) {
        modifier.animateContentSize(
            animationSpec = ZeusMotion.zapSpring(reduceMotion = false)
        )
    } else {
        modifier
    }
    val semantic = ZeusTheme.colors
    if (glassEnabled) {
        ElevatedCard(
            modifier = sizeMod.liquidGlass(enabled = true, shape = shape),
            shape = shape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color.Transparent,
                contentColor = semantic.textOnSky
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides semantic.textOnSky) {
                content()
            }
        }
    } else {
        Surface(
            modifier = sizeMod,
            shape = shape,
            color = semantic.glassFallback,
            contentColor = semantic.textOnSky,
            border = BorderStroke(ZeusSpacing.glassBorderWidth, semantic.glassBorder)
        ) {
            CompositionLocalProvider(LocalContentColor provides semantic.textOnSky) {
                content()
            }
        }
    }
}
