package com.glassnote.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * iOS-like tactile press feedback.
 *
 * The layout is never remeasured: only the graphics layer is scaled. The
 * interaction source is supplied by the caller so the same source also drives
 * the ripple-free [androidx.compose.foundation.clickable] gesture.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.90f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 600f,
        ),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * A faster, shallower response for larger surfaces such as note cards.
 */
@Composable
fun Modifier.cardPressScale(
    interactionSource: InteractionSource,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "cardPressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
