package com.solerunner26.zeusreader.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object ZeusMotion {
    const val SHINE_MS = 6_000
    const val FLOAT_MS = 4_000
    const val STARBURST_MS = 40_000
    const val CONFETTI_MS = 3_600
    const val PULSE_MS = 4_000
    const val FILL_MS = 1_200
    const val STAGGER_MS = 60
    const val CAROUSEL_MS = 3_800
    const val CARD_FLIP_MS = 520
    const val PRESS_SCALE = 0.96f

    /** cubic-bezier(.34, 1.56, .64, 1) — the Sunset "spring" overshoot. */
    val springEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    val standardEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    fun <T> press() = spring<T>(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)

    fun <T> bouncy(reduceMotion: Boolean) = if (reduceMotion) {
        tween<T>(durationMillis = 0)
    } else {
        spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    }

    fun <T> fill(reduceMotion: Boolean, durationMillis: Int = FILL_MS) = tween<T>(
        durationMillis = if (reduceMotion) 0 else durationMillis,
        easing = springEasing
    )

    fun <T> settle(reduceMotion: Boolean, durationMillis: Int = 280) = tween<T>(
        durationMillis = if (reduceMotion) 0 else durationMillis,
        easing = standardEasing
    )
}
