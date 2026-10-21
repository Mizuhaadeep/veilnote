package com.glassnote.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.glassnote.ui.theme.GlassMotion

/**
 * A single fine line that travels across the editor header after a successful
 * save. [saveRevision] is incremented by the ViewModel once the database write
 * has actually completed.
 */
@Composable
fun SaveSweepLine(
    saveRevision: Long,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val accent = MaterialTheme.colorScheme.primary

    LaunchedEffect(saveRevision) {
        if (saveRevision <= 0L) return@LaunchedEffect
        progress.snapTo(0f)
        alpha.snapTo(1f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = GlassMotion.SAVE_SWEEP_MS,
                easing = LinearEasing,
            ),
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 140),
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp),
    ) {
        val endX = size.width * progress.value
        if (endX <= 0f || alpha.value <= 0f) return@Canvas

        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    accent.copy(alpha = 0f),
                    accent.copy(alpha = 0.9f * alpha.value),
                    accent.copy(alpha = 0f),
                ),
                startX = 0f,
                endX = endX,
            ),
            start = Offset(0f, size.height / 2f),
            end = Offset(endX, size.height / 2f),
            strokeWidth = size.height,
        )
    }
}
