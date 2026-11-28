package com.solerunner26.zeusreader.ui.theme

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.solerunner26.zeusreader.reader.prefs.AppUiConfig

/**
 * Whether decorative motion (glows, shine, floating artifacts) may run.
 * Off under Reduce Motion, Battery Saver, E-ink, Motion off, or decorations off.
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
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    return scale == 0f
}

fun isSystemBatterySaverEnabled(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isPowerSaveMode == true
}

/** Theme from the full app config: pack (hue), Day/Night, OLED/E-ink, motion and celebrations. */
@Composable
fun ZeusConfigTheme(config: AppUiConfig, darkTheme: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reduceMotion = isSystemReduceMotionEnabled(context) || isSystemBatterySaverEnabled(context) || !config.masterAnimations
    ZeusTheme(
        pack = ZeusPack.fromHue(config.themeHue),
        mode = ZeusMode.fromReaderSurface(config.readerSurface, darkTheme),
        reduceMotion = reduceMotion,
        celebrations = config.aestheticElements,
        hueOverride = config.themeHue,
        content = content
    )
}

/**
 * App background: warm canvas with a soft radial accent fade at the top (Sunset §Colour "fading effects").
 * Also re-provides the theme from [config] so every screen follows the selected pack and mode.
 */
@Composable
fun HueAuraBackground(
    config: AppUiConfig,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    ZeusConfigTheme(config = config, darkTheme = darkTheme) {
        ZeusBackground(glow = config.aestheticElements && config.auraIntensity > 0f, modifier = modifier, content = content)
    }
}

@Composable
fun ZeusBackground(
    modifier: Modifier = Modifier,
    glow: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val c = ZeusTheme.colors
    val showGlow = glow && !c.isEink
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .drawBehind {
                if (showGlow) {
                    val center = Offset(size.width * 0.85f, 0f)
                    val r = size.width * 0.95f
                    drawCircle(
                        brush = Brush.radialGradient(listOf(c.glow.copy(alpha = if (c.isDark) 0.22f else 0.16f), Color.Transparent), center = center, radius = r),
                        radius = r,
                        center = center
                    )
                }
            },
        content = content
    )
}

/** Faint top vignette on Paper, Dusk and Night reading surfaces; none on OLED or E-ink. */
@Composable
fun ReaderTopVignette(surface: ReaderSurface, hue: Float, intensity: Float, modifier: Modifier = Modifier) {
    val palette = buildReaderSurfacePalette(surface, hue)
    if (!palette.allowVignette || intensity <= 0f) return
    val tint = ZeusPack.fromHue(hue).g1.copy(alpha = (0.12f * (intensity / 0.4f)).coerceIn(0f, 0.2f))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ZeusSizes.vignetteHeight)
            .background(Brush.verticalGradient(listOf(tint, Color.Transparent)))
    )
}
