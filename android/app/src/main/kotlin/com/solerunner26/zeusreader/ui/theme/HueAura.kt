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
 * Determines whether Stormlight aurora drift animation is enabled.
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
 * Stormlight v4 living sky gradient + drifting aurora glow blobs (DESIGN.md §3 & §5).
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
    val sky = ZeusTheme.sky
    val paper = ZeusTheme.paper

    val driftA: Float
    val driftB: Float
    if (auraEnabled) {
        val transition = rememberInfiniteTransition(label = "stormlight_sky_drift")
        val a by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = ZeusMotion.AURORA_DRIFT_MS),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aurora_blob_a"
        )
        val b by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = ZeusMotion.AURORA_DRIFT_SECONDARY_MS),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aurora_blob_b"
        )
        driftA = a
        driftB = b
    } else {
        driftA = 0.2f
        driftB = 0.8f
    }

    val skyBrush = if (paper == ZeusPaper.EINK) {
        Brush.verticalGradient(listOf(ZeusPaper.EINK.bg, ZeusPaper.EINK.bg))
    } else if (paper == ZeusPaper.OLED) {
        Brush.verticalGradient(listOf(Color.Black, sky.baseA.copy(alpha = 0.72f)))
    } else {
        Brush.linearGradient(colors = listOf(sky.baseA, sky.baseB))
    }

    val intensityScale = (config.auraIntensity / 0.4f).coerceIn(0.4f, 1.35f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(skyBrush)
    ) {
        if (config.aestheticElements && paper != ZeusPaper.EINK && config.auraIntensity > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerA = Offset(
                    x = size.width * (0.05f + 0.14f * driftA),
                    y = size.height * (0.06f + 0.08f * driftA)
                )
                val centerB = Offset(
                    x = size.width * (0.92f - 0.14f * driftB),
                    y = size.height * (0.46f + 0.08f * driftB)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            sky.glow1.copy(alpha = (0.45f * intensityScale).coerceAtMost(0.70f)),
                            Color.Transparent
                        ),
                        center = centerA,
                        radius = size.maxDimension * 0.52f
                    ),
                    center = centerA,
                    radius = size.maxDimension * 0.52f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            sky.glow2.copy(alpha = (0.36f * intensityScale).coerceAtMost(0.55f)),
                            Color.Transparent
                        ),
                        center = centerB,
                        radius = size.maxDimension * 0.48f
                    ),
                    center = centerB,
                    radius = size.maxDimension * 0.48f
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
    val sky = ZeusSky.fromHue(hue)
    val tint = sky.glow1.copy(alpha = (0.14f * (intensity / 0.4f)).coerceIn(0f, 0.22f))
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
