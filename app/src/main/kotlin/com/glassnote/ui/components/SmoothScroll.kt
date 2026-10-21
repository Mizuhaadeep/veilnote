package com.glassnote.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.withFrameNanos
import kotlin.math.roundToInt

private const val SmoothScrollDurationNanos = 800_000_000L

/**
 * The same curve the requested tween(800) spec would use:
 * slow start -> fast middle -> slow stop.
 */
private val SmoothScrollEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

/**
 * ScrollState.animateScrollTo does not accept an AnimationSpec on Compose 1.7.
 * Drive the scroll position from frame time instead, so the editor gets the
 * required 800 ms cubic-bezier motion rather than the framework's short default.
 */
suspend fun ScrollState.animateScrollToSmooth(target: Int) {
    val start = value
    val end = target.coerceIn(0, maxValue)
    if (start == end) return

    val startTime = withFrameNanos { it }
    while (true) {
        val now = withFrameNanos { it }
        val fraction = ((now - startTime).toFloat() / SmoothScrollDurationNanos)
            .coerceIn(0f, 1f)
        val eased = SmoothScrollEasing.transform(fraction)
        scrollTo((start + (end - start) * eased).roundToInt())
        if (fraction >= 1f) break
    }
}
