package com.solerunner26.zeusreader.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object ZeusMotion {
    const val AURORA_DRIFT_MS = 16_000
    const val AURORA_DRIFT_SECONDARY_MS = 19_000
    const val ORB_PULSE_MS = 3_000
    const val CONSTELLATION_STAGGER_MS = 40
    const val HUE_TRANSITION_MS = 400
    const val COVER_BOB_MS = 6_000

    val standardEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    fun <T> zapSpring(reduceMotion: Boolean) = if (reduceMotion) {
        tween<T>(durationMillis = 0)
    } else {
        spring<T>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    fun <T> settleTween(reduceMotion: Boolean, durationMillis: Int = 240) = tween<T>(
        durationMillis = if (reduceMotion) 0 else durationMillis,
        easing = standardEasing
    )
}
