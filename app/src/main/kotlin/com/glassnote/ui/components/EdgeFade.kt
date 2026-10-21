package com.glassnote.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * Fades the top edge of a scrolling surface to transparent using an offscreen
 * DstIn mask. A coloured scrim would add a second hard band on top of the glass
 * background; this mask reveals the ambient layer instead.
 */
fun Modifier.topEdgeFade(height: Dp): Modifier = this
    .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = 0f,
                endY = height.toPx(),
            ),
            blendMode = BlendMode.DstIn,
        )
    }
