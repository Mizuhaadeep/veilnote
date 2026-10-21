package com.glassnote.ui.transition

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlin.math.min

private val PageShape = RoundedCornerShape(28.dp)

/**
 * Bounds + the exact source corner radius for the mask reveal.
 *
 * For the settings gear this is half of its square size (a circle); for the
 * FAB/empty action it is the capsule radius.
 */
data class TransitionOrigin(
    val bounds: Rect,
    val cornerRadius: Dp,
)

/**
 * BOTTOM_UP only. The SCALE style does not use the NavHost at all: settings /
 * new-note pages are rendered as a mask overlay above Home by [MaskRevealPage].
 */
@Composable
fun ContainerTransformPage(
    animatedVisibilityScope: AnimatedVisibilityScope,
    closing: Boolean,
    modifier: Modifier = Modifier,
    minimal: Boolean = false,
    content: @Composable () -> Unit,
) {
    BottomUpPage(
        animatedVisibilityScope = animatedVisibilityScope,
        closing = closing,
        minimal = minimal,
        modifier = modifier,
        content = content,
    )
}

@Composable
private fun BottomUpPage(
    animatedVisibilityScope: AnimatedVisibilityScope,
    closing: Boolean,
    minimal: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Keep the first composition hidden so AnimatedVisibility owns a real
        // enter transition instead of snapping into place.
        visible = true
    }
    LaunchedEffect(closing) {
        if (closing) visible = false
    }
    LaunchedEffect(animatedVisibilityScope) {
        snapshotFlow { animatedVisibilityScope.transition.targetState }
            .collect { target ->
                if (target == EnterExitState.PostExit) visible = false
            }
    }

    val brush = pageBrush()

    val enterTransition = if (minimal) {
        fadeIn(animationSpec = tween(140))
    } else {
        slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = 400f,
            ),
        ) + fadeIn(
            animationSpec = spring(
                dampingRatio = 0.90f,
                stiffness = 400f,
            ),
        )
    }
    val exitTransition = if (minimal) {
        fadeOut(animationSpec = tween(100))
    } else {
        slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = spring(
                dampingRatio = 0.90f,
                stiffness = 400f,
            ),
        ) + fadeOut(animationSpec = tween(160))
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.fillMaxSize(),
            enter = enterTransition,
            exit = exitTransition,
        ) {
            PageSurface(
                brush = brush,
                modifier = Modifier.fillMaxSize(),
                shape = PageShape,
                elevation = if (closing || minimal) 0.dp else 28.dp,
                content = content,
            )
        }
    }
}

/**
 * Double-layer mask reveal for SCALE.
 *
 * The page is laid out at its normal full-screen size. There is no scaleX /
 * scaleY, no AnimatedVisibility, and no NavHost transition on this path. The
 * only animation source is [progress]; a dynamic Shape clips the normal page
 * from the exact button rect + radius to the full screen rect + 0 radius.
 */
@Composable
fun MaskRevealPage(
    origin: TransitionOrigin,
    closing: Boolean,
    onExitFinished: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val currentOnExitFinished by rememberUpdatedState(onExitFinished)
    var exitNotified by remember { mutableStateOf(false) }

    LaunchedEffect(closing) {
        if (closing) {
            // The old enter coroutine is cancelled by this key change. Capture
            // the interrupted value and animate back; never reset to 0.
            progress.snapTo(progress.value)
            progress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.88f,
                    stiffness = 420f,
                ),
            )
            if (!exitNotified) {
                exitNotified = true
                currentOnExitFinished()
            }
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 420f,
                ),
            )
        }
    }

    val density = LocalDensity.current
    val brush = pageBrush()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val rootWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val rootHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val p = progress.value.coerceIn(0f, 1f)
        val source = origin.bounds

        val maskRect = Rect(
            left = source.left + (0f - source.left) * p,
            top = source.top + (0f - source.top) * p,
            right = source.right + (rootWidthPx - source.right) * p,
            bottom = source.bottom + (rootHeightPx - source.bottom) * p,
        )
        val startRadiusPx = with(density) { origin.cornerRadius.toPx() }
        val maskRadiusPx = (startRadiusPx * (1f - p)).coerceAtLeast(0f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    shape = MaskRevealShape(maskRect, maskRadiusPx)
                    clip = true
                }
                .background(brush),
        ) {
            content()
        }
    }
}

/**
 * A Shape is required rather than RoundedCornerShape: the mask has to be
 * positioned at the button's XY coordinates, while RoundedCornerShape only
 * rounds the layer's own full-screen corners. The outline is a RoundRect whose
 * rect and radius are both interpolated by the single Animatable.
 */
private data class MaskRevealShape(
    val rect: Rect,
    val radiusPx: Float,
) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): Outline {
        val left = rect.left.coerceIn(0f, size.width)
        val top = rect.top.coerceIn(0f, size.height)
        val right = rect.right.coerceIn(left, size.width)
        val bottom = rect.bottom.coerceIn(top, size.height)
        val maxRadius = min((right - left) / 2f, (bottom - top) / 2f)
        val radius = radiusPx.coerceIn(0f, maxRadius)

        return Outline.Rounded(
            RoundRect(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                radiusX = radius,
                radiusY = radius,
            ),
        )
    }
}

@Composable
private fun PageSurface(
    brush: Brush,
    modifier: Modifier,
    shape: Shape,
    elevation: Dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
            )
            .clip(shape)
            .clipToBounds()
            .background(brush),
    ) {
        content()
    }
}

@Composable
private fun pageBrush(): Brush = Brush.verticalGradient(
    listOf(
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.surface,
    ),
)

/**
 * Home behind BOTTOM_UP pages. Kept scrim-free. During a page exit the blur is
 * dropped to zero to avoid GPU contention with the closing animation.
 */
@Composable
fun HomeTransitionBackdrop(
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    exiting: Boolean = false,
    minimal: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dim by animatedVisibilityScope.transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = 0.85f,
                stiffness = 400f,
            )
        },
        label = "homeTransitionDim",
    ) { state ->
        if (state == EnterExitState.PostExit) 1f else 0f
    }

    val veil by androidx.compose.animation.animateColorAsState(
        targetValue = if (!minimal && !exiting && dim > 0.01f) {
            MaterialTheme.colorScheme.background.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(260),
        label = "homeTransitionVeil",
    )

    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurFactor = if (exiting || minimal) 0f else dim
    val blurRadius = (12f * blurFactor).dp
    val blockerInteraction = remember { MutableInteractionSource() }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (supportsBlur && blurFactor > 0.01f) {
                        Modifier.blur(
                            radius = blurRadius,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded,
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            content()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(veil)
                .then(
                    if (dim > 0.01f && !exiting) {
                        Modifier.clickable(
                            interactionSource = blockerInteraction,
                            indication = null,
                            onClick = {},
                        )
                    } else {
                        Modifier
                    },
                ),
        )
    }
}
