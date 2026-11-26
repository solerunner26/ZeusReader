package com.solerunner26.zeusreader.ui.theme

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS-inspired Liquid Glass surface modifier with frosted translucent tint,
 * specular highlight border, and soft ambient elevation.
 */
@Composable
fun Modifier.liquidGlass(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    alpha: Float = 0.72f
): Modifier {
    if (!enabled) return this
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.55f),
            primary.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.14f)
        )
    )
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            surface.copy(alpha = (alpha + 0.10f).coerceAtMost(0.92f)),
            surface.copy(alpha = alpha)
        )
    )
    return this
        .shadow(elevation = 8.dp, shape = shape, clip = false)
        .clip(shape)
        .background(bgBrush, shape)
        .border(BorderStroke(1.dp, borderBrush), shape)
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    glassEnabled: Boolean = true,
    cornerRadius: Dp = 24.dp,
    animateSize: Boolean = true,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val sizeMod = if (animateSize) {
        modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    } else {
        modifier
    }
    if (glassEnabled) {
        ElevatedCard(
            modifier = sizeMod.liquidGlass(enabled = true, shape = shape),
            shape = shape,
            colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
        ) {
            content()
        }
    } else {
        ElevatedCard(
            modifier = sizeMod,
            shape = shape,
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            content()
        }
    }
}
