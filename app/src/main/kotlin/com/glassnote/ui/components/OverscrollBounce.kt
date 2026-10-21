package com.glassnote.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Small, self-contained rubber-band state for edge overscroll.
 *
 * Drag offsets use a cheap mutable float (no coroutine per pointer event).
 * The release is driven by [Animatable] with the requested iOS-like spring.
 */
@Stable
class OverscrollBounceState internal constructor(
    private val scope: CoroutineScope,
    private val maxOffsetPx: Float,
) {
    var offsetPx by mutableFloatStateOf(0f)
        private set

    private val settleAnim = Animatable(0f)
    private var settleJob: Job? = null

    fun onDragAvailable(availableY: Float) {
        settleJob?.cancel()
        offsetPx = (offsetPx + availableY * 0.5f).coerceIn(-maxOffsetPx, maxOffsetPx)
    }

    fun settle() {
        if (offsetPx == 0f) return
        settleJob?.cancel()
        val start = offsetPx
        settleJob = scope.launch {
            settleAnim.snapTo(start)
            settleAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) {
                offsetPx = value
            }
        }
    }
}

@Composable
fun rememberOverscrollBounceState(
    isScrollInProgress: Boolean,
): OverscrollBounceState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val maxOffset = with(density) { 132.dp.toPx() }
    val state = remember(scope, maxOffset) {
        OverscrollBounceState(scope = scope, maxOffsetPx = maxOffset)
    }

    LaunchedEffect(isScrollInProgress) {
        if (!isScrollInProgress) {
            state.settle()
        }
    }

    return state
}

@Composable
fun Modifier.iosOverscrollBounce(
    state: OverscrollBounceState,
): Modifier {
    val connection = remember(state) {
        object : NestedScrollConnection {
            @Suppress("DEPRECATION")
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val fromUser = source == NestedScrollSource.Drag ||
                    source == NestedScrollSource.UserInput
                if (fromUser && available.y != 0f) {
                    state.onDragAvailable(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                state.settle()
                return Velocity.Zero
            }
        }
    }

    return this
        .nestedScroll(connection)
        .graphicsLayer {
            translationY = state.offsetPx
        }
}
