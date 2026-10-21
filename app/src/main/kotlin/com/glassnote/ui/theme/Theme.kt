package com.glassnote.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/** Shared motion values. All timed UI transitions stay in the 220-320 ms range. */
object GlassMotion {
    const val QUICK_MS = 220
    const val STANDARD_MS = 280
    const val SLOW_MS = 320
    const val SAVE_SWEEP_MS = 300
    const val AUTOSAVE_DEBOUNCE_MS = 1_000L
    const val DELETE_GRACE_MS = 4_000L
    const val SNACKBAR_VISIBLE_MS = 10_000L

    /** For a short, calm entrance. */
    val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    /** For movement that starts and ends gently. */
    val EaseInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** The save line must travel evenly, without a fancy bounce. */
    val Linear = LinearEasing
}

/**
 * A single no-op indication for the whole app.
 *
 * Ripple is deliberately removed globally: GlassNote uses spring press-scale
 * feedback on its tappable surfaces instead. Components that accept an explicit
 * `indication` (the custom clickables) also pass `null` for the same reason.
 */
private object NoRippleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        // An empty node draws nothing, so no ripple, highlight or glow is added.
        return object : Modifier.Node() {}
    }

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 0x51A7
}

internal val GlassShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

/**
 * Not 1f on purpose: the new theme's content stays visible from the first frame,
 * so the transition can never flash a blank solid-color screen.
 */
private const val ThemeTransitionCoverAlpha = 0.72f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassNoteTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    /*
     * The rendered theme is intentionally one recomposition behind the requested
     * theme. On a switch we first apply an old-background tint, then swap the
     * actual ColorScheme. The first frame of the new theme is already tinted, so
     * there is no one-frame white/black flash before the fade starts, and the
     * tint is translucent so content never disappears behind a solid color.
     */
    var displayedDarkTheme by remember { mutableStateOf(darkTheme) }
    val displayedScheme = if (displayedDarkTheme) GlassDarkColors else GlassLightColors

    var lastBackground by remember { mutableStateOf(displayedScheme.background) }
    var transitionColor by remember { mutableStateOf(displayedScheme.background) }
    val transitionAlpha = remember { Animatable(0f) }

    LaunchedEffect(darkTheme) {
        if (displayedDarkTheme != darkTheme) {
            val nextScheme = if (darkTheme) GlassDarkColors else GlassLightColors

            // 1. Prepare the tint while the old theme is still displayed.
            transitionColor = lastBackground
            transitionAlpha.snapTo(ThemeTransitionCoverAlpha)

            // 2. Swap the actual theme only after the cover is opaque.
            displayedDarkTheme = darkTheme
            lastBackground = nextScheme.background

            // 3. Reveal the new theme. Only a layer alpha changes per frame.
            transitionAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = GlassMotion.SLOW_MS,
                    easing = GlassMotion.EaseInOut,
                ),
            )
        }
    }

    MaterialTheme(
        colorScheme = displayedScheme,
        typography = GlassTypography,
        shapes = GlassShapes,
    ) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val activity = view.context.findActivity() ?: return@SideEffect
                WindowCompat.getInsetsController(activity.window, view).apply {
                    isAppearanceLightStatusBars = !displayedDarkTheme
                    isAppearanceLightNavigationBars = !displayedDarkTheme
                }
            }
        }

        CompositionLocalProvider(
            LocalIndication provides NoRippleIndication,
            LocalOverscrollConfiguration provides OverscrollConfiguration(
                glowColor = Color.Transparent,
                drawPadding = PaddingValues(0.dp),
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()

                /*
                 * The theme itself switches instantly. Only this single opaque
                 * rectangle is faded on the layer/draw phase, so the global tree
                 * is not recomposed every frame. Compose therefore keeps the
                 * display's full refresh rate (90/120 Hz on capable panels), with
                 * a 60 fps floor on 60 Hz devices.
                 */
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = transitionAlpha.value
                        }
                        .background(transitionColor),
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
