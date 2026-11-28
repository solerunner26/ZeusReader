package com.solerunner26.zeusreader.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.solerunner26.zeusreader.ui.theme.ZeusMotion
import com.solerunner26.zeusreader.ui.theme.ZeusPalette
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.rememberLoopPhase
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Sunset decorative artifacts (design/sunset/DESIGN.md): Bolt the owl, crown, flame, trophy,
 * confetti, starburst halo and radial glow. All are decorative (hidden from accessibility),
 * drawn on Canvas so they follow the active theme pack, and still when motion is reduced.
 */

/** Gentle up-down float used by mascot and floating badges. */
@Composable
fun Modifier.floating(amplitude: Float = 7f, rotation: Float = 0f): Modifier {
    val reduce = ZeusTheme.reduceMotion
    val p by rememberLoopPhase(ZeusMotion.FLOAT_MS, reverse = true, animate = !reduce)
    return this.graphicsLayer {
        translationY = if (reduce) 0f else -amplitude * density * p
        rotationZ = rotation
    }
}

/** Bolt — glossy owl reading a book, coloured with the theme pack. */
@Composable
fun BoltMascot(size: Dp, modifier: Modifier = Modifier, float: Boolean = true) {
    val c = ZeusTheme.colors
    val g1 = c.accent
    val g2 = c.accent2
    val eink = c.isEink
    Canvas(
        modifier = modifier
            .size(size)
            .then(if (float) Modifier.floating() else Modifier)
            .clearAndSetSemantics { }
    ) {
        val s = this.size.minDimension / 200f
        scale(s, s, pivot = Offset.Zero) {
            val body = if (eink) Brush.linearGradient(listOf(Color.White, Color.White)) else Brush.radialGradient(
                0f to Color.White, 0.25f to g2, 1f to g1,
                center = Offset(76f, 60f), radius = 150f
            )
            // ground shadow
            drawOval(Color.Black.copy(alpha = 0.16f), topLeft = Offset(48f, 181f), size = Size(104f, 14f))
            // ears
            drawPath(Path().apply { moveTo(58f, 44f); lineTo(70f, 18f); lineTo(84f, 40f); close() }, body)
            drawPath(Path().apply { moveTo(142f, 44f); lineTo(130f, 18f); lineTo(116f, 40f); close() }, body)
            // body
            drawOval(body, topLeft = Offset(38f, 38f), size = Size(124f, 140f))
            if (eink) drawOval(Color.Black, topLeft = Offset(38f, 38f), size = Size(124f, 140f), style = Stroke(3f))
            // belly
            drawOval(
                Brush.radialGradient(listOf(Color.White, ZeusPalette.mascotBelly), center = Offset(96f, 112f), radius = 70f),
                topLeft = Offset(60f, 84f), size = Size(80f, 88f)
            )
            // eyes
            val eye = Brush.radialGradient(listOf(Color.White, ZeusPalette.mascotEye), center = Offset(100f, 70f), radius = 60f)
            drawCircle(eye, radius = 22f, center = Offset(78f, 82f))
            drawCircle(eye, radius = 22f, center = Offset(122f, 82f))
            drawCircle(ZeusPalette.charcoal, radius = 10f, center = Offset(81f, 84f))
            drawCircle(ZeusPalette.charcoal, radius = 10f, center = Offset(119f, 84f))
            drawCircle(Color.White, radius = 3.5f, center = Offset(84f, 80f))
            drawCircle(Color.White, radius = 3.5f, center = Offset(122f, 80f))
            // beak
            drawPath(Path().apply { moveTo(92f, 100f); lineTo(100f, 112f); lineTo(108f, 100f); close() }, ZeusPalette.charcoal)
            // lightning crest
            drawPath(
                Path().apply { moveTo(100f, 30f); lineTo(92f, 46f); lineTo(100f, 46f); lineTo(96f, 58f); lineTo(110f, 40f); lineTo(102f, 40f); lineTo(106f, 30f); close() },
                if (eink) ZeusPalette.charcoal else Color.White.copy(alpha = 0.95f)
            )
            // book
            rotate(-8f, pivot = Offset(100f, 150f)) {
                drawRoundRect(
                    Brush.linearGradient(listOf(ZeusPalette.bookDark, ZeusPalette.charcoal)),
                    topLeft = Offset(62f, 136f), size = Size(76f, 40f), cornerRadius = CornerRadius(6f)
                )
                drawLine(Color.White.copy(alpha = 0.35f), Offset(100f, 138f), Offset(100f, 174f), strokeWidth = 2f)
                drawRoundRect(Color.White.copy(alpha = 0.5f), Offset(68f, 143f), Size(26f, 3f), CornerRadius(1.5f))
                drawRoundRect(Color.White.copy(alpha = 0.5f), Offset(106f, 143f), Size(26f, 3f), CornerRadius(1.5f))
            }
            // wings holding the book
            drawOval(body, topLeft = Offset(53f, 141f), size = Size(26f, 18f))
            drawOval(body, topLeft = Offset(121f, 141f), size = Size(26f, 18f))
            // gloss
            rotate(-25f, pivot = Offset(74f, 60f)) {
                drawOval(Color.White.copy(alpha = 0.35f), topLeft = Offset(60f, 54f), size = Size(28f, 12f))
            }
        }
    }
}

/** Gold crown badge. */
@Composable
fun CrownArtifact(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size).clearAndSetSemantics { }) {
        val s = this.size.minDimension / 64f
        scale(s, s, pivot = Offset.Zero) {
            val gold = Brush.verticalGradient(listOf(ZeusPalette.goldLight, ZeusPalette.gold, ZeusPalette.goldDeep), startY = 10f, endY = 56f)
            val crown = Path().apply {
                moveTo(8f, 22f); lineTo(20f, 34f); lineTo(32f, 14f); lineTo(44f, 34f); lineTo(56f, 22f); lineTo(52f, 50f); lineTo(12f, 50f); close()
            }
            drawPath(crown, gold)
            drawPath(crown, ZeusPalette.goldEdge, style = Stroke(2f, join = StrokeJoin.Round))
            drawRoundRect(gold, Offset(12f, 48f), Size(40f, 8f), CornerRadius(3f))
            drawRoundRect(ZeusPalette.goldEdge, Offset(12f, 48f), Size(40f, 8f), CornerRadius(3f), style = Stroke(2f))
            drawCircle(ZeusPalette.ruby, 4f, Offset(32f, 38f))
            listOf(Offset(8f, 22f), Offset(32f, 14f), Offset(56f, 22f)).forEach { drawCircle(ZeusPalette.goldLight, 3.5f, it) }
            drawLine(Color.White.copy(alpha = 0.6f), Offset(16f, 30f), Offset(20f, 34f), 2.5f, StrokeCap.Round)
        }
    }
}

/** Streak flame. */
@Composable
fun FlameArtifact(size: Dp, modifier: Modifier = Modifier) {
    val eink = ZeusTheme.colors.isEink
    Canvas(modifier.size(size).clearAndSetSemantics { }) {
        val s = this.size.minDimension / 64f
        scale(s, s, pivot = Offset.Zero) {
            val outer = Path().apply {
                moveTo(32f, 4f); cubicTo(32f, 4f, 50f, 18f, 50f, 36f)
                cubicTo(50f, 46f, 42f, 54f, 32f, 54f); cubicTo(22f, 54f, 14f, 46f, 14f, 36f)
                cubicTo(14f, 27f, 19f, 22f, 19f, 22f); cubicTo(19f, 22f, 22f, 30f, 28f, 30f)
                cubicTo(28f, 18f, 32f, 10f, 32f, 4f); close()
            }
            drawPath(
                outer,
                if (eink) Brush.linearGradient(listOf(Color.Black, Color.Black)) else Brush.radialGradient(
                    0f to ZeusPalette.flameCore, 0.35f to ZeusPalette.gold, 1f to ZeusPalette.flameEdge,
                    center = Offset(32f, 46f), radius = 44f
                )
            )
            val inner = Path().apply {
                moveTo(32f, 30f); cubicTo(32f, 30f, 40f, 36f, 40f, 44f); cubicTo(40f, 48f, 36f, 52f, 32f, 52f)
                cubicTo(28f, 52f, 24f, 48f, 24f, 44f); cubicTo(24f, 39f, 27f, 37f, 27f, 37f)
                cubicTo(27f, 37f, 28f, 40f, 31f, 40f); cubicTo(31f, 35f, 32f, 33f, 32f, 30f); close()
            }
            drawPath(inner, if (eink) Color.White else ZeusPalette.flameCore.copy(alpha = 0.9f))
        }
    }
}

/** Metallic trophy. */
@Composable
fun TrophyArtifact(size: Dp, modifier: Modifier = Modifier, float: Boolean = true) {
    Canvas(modifier.size(size).then(if (float) Modifier.floating() else Modifier).clearAndSetSemantics { }) {
        val s = this.size.minDimension / 120f
        scale(s, s, pivot = Offset.Zero) {
            val gold = Brush.linearGradient(listOf(ZeusPalette.goldLight, ZeusPalette.gold, ZeusPalette.goldDeep), start = Offset(30f, 18f), end = Offset(90f, 84f))
            drawPath(Path().apply {
                moveTo(30f, 18f); lineTo(90f, 18f); lineTo(90f, 40f); cubicTo(90f, 56f, 77f, 70f, 60f, 70f)
                cubicTo(43f, 70f, 30f, 56f, 30f, 40f); close()
            }, gold)
            drawPath(Path().apply { moveTo(30f, 26f); lineTo(18f, 26f); cubicTo(16f, 38f, 24f, 46f, 32f, 46f) }, gold, style = Stroke(8f, cap = StrokeCap.Round))
            drawPath(Path().apply { moveTo(90f, 26f); lineTo(102f, 26f); cubicTo(104f, 38f, 96f, 46f, 88f, 46f) }, gold, style = Stroke(8f, cap = StrokeCap.Round))
            drawRect(gold, Offset(52f, 68f), Size(16f, 16f))
            drawRoundRect(Brush.verticalGradient(listOf(ZeusPalette.bookDark, ZeusPalette.charcoal), startY = 84f, endY = 106f), Offset(34f, 84f), Size(52f, 22f), CornerRadius(6f))
            drawRoundRect(ZeusPalette.gold.copy(alpha = 0.9f), Offset(46f, 90f), Size(28f, 6f), CornerRadius(3f))
            drawLine(Color.White.copy(alpha = 0.55f), Offset(42f, 26f), Offset(50f, 50f), 5f, StrokeCap.Round)
            // star
            val star = Path()
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) 11f else 4.5f
                val a = -PI / 2 + i * PI / 5
                val x = 60f + (r * cos(a)).toFloat()
                val y = 36f + (r * sin(a)).toFloat()
                if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
            }
            star.close()
            drawPath(star, Color.White.copy(alpha = 0.92f))
        }
    }
}

/** Soft radial glow behind hero objects. */
fun Modifier.radialGlow(color: Color, radiusFraction: Float = 0.6f): Modifier = this.drawBehind {
    val r = size.maxDimension * radiusFraction
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = r), radius = r, center = center)
}

/** Slowly spinning starburst halo (rays fading out from the centre). */
@Composable
fun Starburst(modifier: Modifier = Modifier, color: Color = Color.White.copy(alpha = 0.22f), rays: Int = 20) {
    if (ZeusTheme.colors.isEink) return
    val p by rememberLoopPhase(ZeusMotion.STARBURST_MS)
    Canvas(modifier.clearAndSetSemantics { }) {
        val r = size.minDimension / 2f
        rotate(p * 360f) {
            val step = 360f / rays
            for (i in 0 until rays) {
                val a0 = Math.toRadians((i * step).toDouble())
                val a1 = Math.toRadians((i * step + step * 0.4f).toDouble())
                val path = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + (r * cos(a0)).toFloat(), center.y + (r * sin(a0)).toFloat())
                    lineTo(center.x + (r * cos(a1)).toFloat(), center.y + (r * sin(a1)).toFloat())
                    close()
                }
                drawPath(path, Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = r))
            }
        }
    }
}

/** Falling confetti for celebration screens (static scatter under Reduce Motion). */
@Composable
fun Confetti(modifier: Modifier = Modifier, pieces: Int = 28) {
    val c = ZeusTheme.colors
    if (c.isEink) return
    val reduce = !ZeusTheme.celebrations
    val p by rememberLoopPhase(ZeusMotion.CONFETTI_MS, animate = !reduce)
    val colors = remember(c.accent, c.accent2) {
        listOf(c.accent, c.accent2, ZeusPalette.ruby, ZeusPalette.confettiBlue, ZeusPalette.confettiGreen, Color.White)
    }
    Canvas(modifier.clearAndSetSemantics { }) {
        for (k in 0 until pieces) {
            val x = size.width * (((k * 37) % 100) / 100f)
            val offset = (k * 0.137f) % 1f
            val t = (p + offset) % 1f
            val y = -20f + t * (size.height * 0.75f) + (k % 5) * 12f
            val w = (6 + (k % 3) * 3) * density
            val h = if (k % 4 == 0) w else w * 1.4f
            drawConfettiPiece(Offset(x, y), Size(w, h), colors[k % colors.size], t * 300f + k * 23f, k % 4 == 0)
        }
    }
}

private fun DrawScope.drawConfettiPiece(at: Offset, size: Size, color: Color, angle: Float, round: Boolean) {
    translate(at.x, at.y) {
        rotate(angle, pivot = Offset.Zero) {
            if (round) drawCircle(color, size.width / 2f, Offset.Zero)
            else drawRoundRect(color, Offset(-size.width / 2f, -size.height / 2f), size, CornerRadius(2f * density))
        }
    }
}

/** Centred stage: glow + optional starburst behind [content]. */
@Composable
fun ArtifactStage(size: Dp, modifier: Modifier = Modifier, starburst: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val c = ZeusTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .radialGlow(c.glow),
        contentAlignment = Alignment.Center
    ) {
        if (starburst) Starburst(Modifier.fillMaxSize(), color = c.accent2.copy(alpha = if (c.isDark) 0.22f else 0.28f))
        content()
    }
}
