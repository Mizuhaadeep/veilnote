package com.glassnote.ui.components

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.glassnote.domain.model.AppSettings
import com.glassnote.domain.model.BackgroundSource
import com.glassnote.domain.model.GlassGradient
import com.glassnote.ui.theme.GlassMotion
import com.glassnote.ui.theme.backdropPalette
import com.glassnote.ui.theme.isDarkGlassTheme

private data class BackdropKey(
    val source: BackgroundSource,
    val gradient: GlassGradient,
    val imageUri: String?,
)

/**
 * Full-screen ambient layer. This is the only blurred surface in the app.
 *
 * Android 12+ uses RenderEffect through [Modifier.blur]. On API 26-30 the
 * bitmap is shown as a soft, translucent image beneath a gradient veil; the
 * background never crashes and cards remain completely unblurred.
 */
@Composable
fun AmbientBackground(
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val darkTheme = isDarkGlassTheme()
    val palette = settings.gradient.backdropPalette(darkTheme)
    val backdropKey = remember(
        settings.effectiveBackgroundSource,
        settings.gradient,
        settings.backgroundImageUri,
    ) {
        BackdropKey(
            source = settings.effectiveBackgroundSource,
            gradient = settings.gradient,
            imageUri = settings.backgroundImageUri,
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ambientBreathing")
    val breath by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = backdropKey,
            animationSpec = tween(
                durationMillis = GlassMotion.SLOW_MS,
                easing = GlassMotion.EaseInOut,
            ),
            label = "ambientBackdrop",
        ) { key ->
            BackdropLayer(
                key = key,
                settings = settings,
                darkTheme = darkTheme,
                paletteColors = palette.colors,
                glow = palette.glow,
            )
        }

        // A very slow breathing highlight. It only animates a translucent layer,
        // so there is no image decode or blur allocation per frame.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1f + breath * 0.07f
                    scaleY = 1f + breath * 0.07f
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.76f, size.height * 0.16f)
                val glowRadius = size.maxDimension * 0.62f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.glow.copy(
                                alpha = if (darkTheme) {
                                    0.20f + breath * 0.11f
                                } else {
                                    0.26f + breath * 0.10f
                                },
                            ),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = center,
                )
            }
        }

        // Scrim keeps typography readable over every palette and every photo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to if (darkTheme) {
                            Color.Black.copy(alpha = 0.30f)
                        } else {
                            Color.White.copy(alpha = 0.20f)
                        },
                        0.30f to Color.Transparent,
                        0.72f to Color.Transparent,
                        1.0f to if (darkTheme) {
                            Color.Black.copy(alpha = 0.60f)
                        } else {
                            Color.White.copy(alpha = 0.26f)
                        },
                    ),
                ),
        )
    }
}

@Composable
private fun BackdropLayer(
    key: BackdropKey,
    settings: AppSettings,
    darkTheme: Boolean,
    paletteColors: List<Color>,
    glow: Color,
) {
    val blurRadius = settings.blurRadiusDp
    val supportsRenderBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                if (key.source == BackgroundSource.IMAGE) {
                    scaleX = 1.10f
                    scaleY = 1.10f
                }
            }
            .then(
                if (supportsRenderBlur && blurRadius > 0) {
                    Modifier.blur(
                        radius = blurRadius.dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = paletteColors,
                        start = Offset.Zero,
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    ),
                ),
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glow.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(size.width * 0.20f, size.height * 0.75f),
                    radius = size.maxDimension * 0.55f,
                ),
                center = Offset(size.width * 0.20f, size.height * 0.75f),
                radius = size.maxDimension * 0.55f,
            )
        }

        if (key.source == BackgroundSource.IMAGE && !key.imageUri.isNullOrBlank()) {
            AsyncImage(
                model = key.imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        // On older devices there is no RenderEffect. A translucent
                        // photo + gradient veil is the deliberate fallback.
                        if (!supportsRenderBlur) {
                            Modifier.graphicsLayer(alpha = 0.52f)
                        } else {
                            Modifier
                        },
                    ),
            )
        }

        // Add a little depth even without a photo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (darkTheme) 0.015f else 0.10f),
                            Color.Transparent,
                            Color.Black.copy(alpha = if (darkTheme) 0.08f else 0.02f),
                        ),
                    ),
                ),
        )
    }
}
