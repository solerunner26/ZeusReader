package com.solerunner26.zeusreader.ui.theme

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import com.solerunner26.zeusreader.reader.prefs.AppUiConfig
import kotlin.math.sin

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

/** Paper used for app chrome derived from the app-wide config. */
fun chromePaperFor(config: AppUiConfig, darkTheme: Boolean): ZeusPaper = when (config.readerSurface) {
    ReaderSurface.OLED -> ZeusPaper.OLED
    ReaderSurface.EINK -> ZeusPaper.EINK
    else -> if (darkTheme) ZeusPaper.NIGHT else ZeusPaper.CLOUD
}

/**
 * Root Stormlight theme driven by the full [AppUiConfig]: Theme Pack (sky), Night/Daylight mood,
 * OLED / E-ink chrome, glass, neon borders and motion. Used by [HueAuraBackground].
 */
@Composable
fun StormlightConfigTheme(
    config: AppUiConfig,
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val reduceMotion = isSystemReduceMotionEnabled(context) || isSystemBatterySaverEnabled(context) ||
        !config.masterAnimations
    val paper = chromePaperFor(config, darkTheme)
    ZeusTheme(
        sky = ZeusSky.fromHue(config.themeHue),
        paper = paper,
        darkTheme = darkTheme,
        reduceMotion = reduceMotion,
        hueOverride = config.themeHue,
        daylight = !darkTheme && paper != ZeusPaper.OLED,
        glass = config.glassUi,
        neon = config.aestheticElements,
        neonAnimated = config.animatedBorders,
        shimmer = config.animatedGlare,
        content = content
    )
}

/**
 * Stormlight v4 living sky: two-tone gradient + drifting aurora glows + faint stars
 * (DESIGN.md §3 & §5). Never rendered behind reading text.
 * Also re-provides the theme from [config] so every screen below follows the Theme Pack.
 */
@Composable
fun HueAuraBackground(
    config: AppUiConfig,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val auraEnabled = shouldEnableHueAura(
        config,
        isSystemReduceMotionEnabled(context),
        isSystemBatterySaverEnabled(context)
    )
    StormlightConfigTheme(config = config, darkTheme = darkTheme) {
        StormlightSky(
            animate = auraEnabled,
            intensity = if (config.aestheticElements) config.auraIntensity else 0f,
            modifier = modifier,
            content = content
        )
    }
}

/**
 * The sky itself, reading colours from the current [ZeusTheme]. Animation values are read
 * only in the draw phase, so the drift never recomposes the screen above it.
 */
@Composable
fun StormlightSky(
    animate: Boolean = !ZeusTheme.reduceMotion,
    intensity: Float = 0.4f,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = ZeusTheme.colors
    val sky = ZeusTheme.sky
    val paper = ZeusTheme.paper

    val driftA: State<Float>
    val driftB: State<Float>
    if (animate && !paper.isEink) {
        val transition = rememberInfiniteTransition(label = "stormlight_sky_drift")
        driftA = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = ZeusMotion.AURORA_DRIFT_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aurora_blob_a"
        )
        driftB = transition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = ZeusMotion.AURORA_DRIFT_SECONDARY_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aurora_blob_b"
        )
    } else {
        driftA = remember { mutableFloatStateOf(0.2f) }
        driftB = remember { mutableFloatStateOf(0.8f) }
    }

    val skyBrush = Brush.linearGradient(listOf(colors.skyTop, colors.skyBottom))
    val glowScale = (intensity / 0.4f).coerceIn(0f, 1.35f)
    val glowAlphaA = if (colors.isLight) 0.30f else 0.48f
    val glowAlphaB = if (colors.isLight) 0.24f else 0.38f
    val drawGlows = !paper.isEink && glowScale > 0f
    val stars = !colors.isLight && !paper.isEink && glowScale > 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(skyBrush)
            .drawBehind {
                if (drawGlows) {
                    val a = driftA.value
                    val b = driftB.value
                    val centerA = Offset(
                        x = size.width * (0.05f + 0.16f * a),
                        y = size.height * (0.05f + 0.08f * a)
                    )
                    val centerB = Offset(
                        x = size.width * (0.95f - 0.16f * b),
                        y = size.height * (0.44f + 0.10f * b)
                    )
                    val rA = size.maxDimension * 0.52f
                    val rB = size.maxDimension * 0.48f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(sky.glow1.copy(alpha = (glowAlphaA * glowScale).coerceAtMost(0.7f)), Color.Transparent),
                            center = centerA,
                            radius = rA
                        ),
                        center = centerA,
                        radius = rA
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(sky.glow2.copy(alpha = (glowAlphaB * glowScale).coerceAtMost(0.55f)), Color.Transparent),
                            center = centerB,
                            radius = rB
                        ),
                        center = centerB,
                        radius = rB
                    )
                    if (stars) drawStars(a)
                }
            }
    ) {
        content()
    }
}

private val starField = listOf(
    0.08f to 0.12f, 0.22f to 0.31f, 0.37f to 0.08f, 0.52f to 0.22f, 0.71f to 0.10f,
    0.86f to 0.27f, 0.14f to 0.58f, 0.33f to 0.71f, 0.62f to 0.64f, 0.81f to 0.78f,
    0.45f to 0.88f, 0.93f to 0.52f, 0.05f to 0.86f, 0.68f to 0.40f
)

private fun DrawScope.drawStars(phase: Float) {
    starField.forEachIndexed { i, (fx, fy) ->
        val twinkle = 0.25f + 0.35f * ((sin((phase * 6.283f) + i) + 1f) / 2f)
        drawCircle(
            color = Color.White.copy(alpha = twinkle),
            radius = if (i % 4 == 0) 2.2f * density else 1.2f * density,
            center = Offset(size.width * fx, size.height * fy)
        )
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
            .height(ZeusSizes.vignetteHeight)
            .background(Brush.verticalGradient(colors = listOf(tint, Color.Transparent)))
    )
}
