package com.solerunner26.zeusreader.ui.theme

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.solerunner26.zeusreader.reader.prefs.AppUiConfig

/**
 * Determines whether HueAura and its drift animation are enabled.
 * Strictly disabled under Reduce Motion, Battery Saver, E-ink surface, or Master animations = off.
 */
fun shouldEnableHueAura(
    config: AppUiConfig,
    reduceMotion: Boolean,
    batterySaver: Boolean
): Boolean {
    if (!config.aestheticElements) return false
    if (config.auraIntensity <= 0f) return false
    if (config.readerSurface == ReaderSurface.EINK) return false
    if (!config.masterAnimations) return false
    if (reduceMotion || batterySaver) return false
    return true
}

fun isSystemReduceMotionEnabled(context: Context): Boolean {
    val scale = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }.getOrDefault(1f)
    return scale == 0f
}

fun isSystemBatterySaverEnabled(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isPowerSaveMode == true
}

/**
 * Soft drifting radial Hue Aura layer behind Library, Passages, Home, Vocab, and Settings.
 * Never rendered behind reading text.
 */
@Composable
fun HueAuraBackground(
    config: AppUiConfig,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val reduceMotion = isSystemReduceMotionEnabled(context)
    val batterySaver = isSystemBatterySaverEnabled(context)
    val auraEnabled = shouldEnableHueAura(config, reduceMotion, batterySaver)

    val driftDegrees = if (auraEnabled) {
        val transition = rememberInfiniteTransition(label = "hue_aura_drift")
        val value by transition.animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 14_000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "hue_aura_angle"
        )
        value
    } else {
        0f
    }

    val effectiveHue = normalizeHue(config.themeHue + driftDegrees)
    val scale = (config.auraIntensity / 0.4f).coerceIn(0f, 1.5f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (auraEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val topLeftColor = if (darkTheme) {
                    hslColor(effectiveHue, 0.70f, 0.45f, (0.40f * scale).coerceAtMost(0.75f))
                } else {
                    hslColor(effectiveHue, 0.75f, 0.78f, (0.60f * scale).coerceAtMost(0.85f))
                }
                val rightMiddleColor = if (darkTheme) {
                    hslColor(effectiveHue + 16f, 0.65f, 0.40f, (0.30f * scale).coerceAtMost(0.65f))
                } else {
                    hslColor(effectiveHue + 16f, 0.70f, 0.84f, (0.50f * scale).coerceAtMost(0.75f))
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(topLeftColor, Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.12f),
                        radius = size.maxDimension * 0.58f
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.12f),
                    radius = size.maxDimension * 0.58f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(rightMiddleColor, Color.Transparent),
                        center = Offset(size.width * 0.90f, size.height * 0.52f),
                        radius = size.maxDimension * 0.52f
                    ),
                    center = Offset(size.width * 0.90f, size.height * 0.52f),
                    radius = size.maxDimension * 0.52f
                )
            }
        }
        content()
    }
}

/**
 * Faint top vignette on Paper, Dusk, and Night reading surfaces; none on OLED or E-ink.
 */
@Composable
fun ReaderTopVignette(
    surface: ReaderSurface,
    hue: Float,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val palette = buildReaderSurfacePalette(surface, hue)
    if (!palette.allowVignette || intensity <= 0f) return
    val tint = hslColor(hue, 0.50f, if (palette.isDark) 0.45f else 0.78f, (0.14f * (intensity / 0.4f)).coerceIn(0f, 0.25f))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(tint, Color.Transparent)
                )
            )
    )
}
