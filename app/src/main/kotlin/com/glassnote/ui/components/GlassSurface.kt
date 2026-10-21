package com.glassnote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.glassnote.ui.theme.GlassMotion
import com.glassnote.ui.theme.isDarkGlassTheme

internal fun glassBaseBrush(darkTheme: Boolean): Brush = Brush.verticalGradient(
    colors = if (darkTheme) {
        listOf(
            Color.White.copy(alpha = 0.075f),
            Color.White.copy(alpha = 0.025f),
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.38f),
            Color.White.copy(alpha = 0.16f),
        )
    },
)

internal fun glassBorderBrush(darkTheme: Boolean): Brush = Brush.linearGradient(
    colors = if (darkTheme) {
        listOf(
            Color.White.copy(alpha = 0.24f),
            Color.White.copy(alpha = 0.065f),
            Color.White.copy(alpha = 0.025f),
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.82f),
            Color.White.copy(alpha = 0.30f),
            Color.White.copy(alpha = 0.10f),
        )
    },
)

internal fun glassSpecularBrush(darkTheme: Boolean): Brush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = if (darkTheme) 0.07f else 0.16f),
        Color.White.copy(alpha = 0.0f),
    ),
    endY = 220f,
)

/**
 * Frosted glass surface.
 *
 * Deliberately uses translucency, a gradient rim and an explicit rounded clip
 * instead of a blurred bitmap or a Material elevation overlay. No shadow is
 * drawn: shadows on a light ambient background read as dirty opaque rectangles
 * around otherwise transparent glass.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    overlayBrush: Brush? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val darkTheme = isDarkGlassTheme()
    val baseBrush = remember(darkTheme) { glassBaseBrush(darkTheme) }
    val borderBrush = remember(darkTheme) { glassBorderBrush(darkTheme) }
    val specularBrush = remember(darkTheme) { glassSpecularBrush(darkTheme) }

    Box(
        modifier = modifier
            .clip(shape)
            .clipToBounds()
            .background(baseBrush)
            .then(
                if (overlayBrush != null) {
                    Modifier.background(overlayBrush)
                } else {
                    Modifier
                },
            )
            .border(BorderStroke(1.dp, borderBrush), shape),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clipToBounds()
                .background(specularBrush),
        )
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .clipToBounds(),
            content = content,
        )
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    tint: Color? = null,
) {
    val darkTheme = isDarkGlassTheme()
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
            darkTheme -> Color.White.copy(alpha = 0.075f)
            else -> Color.White.copy(alpha = 0.34f)
        },
        animationSpec = tween(GlassMotion.QUICK_MS),
        label = "iconButtonContainer",
    )
    val borderColor = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)
        darkTheme -> Color.White.copy(alpha = 0.14f)
        else -> Color.White.copy(alpha = 0.58f)
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .pressScale(interactionSource)
            .size(42.dp)
            .clip(CircleShape)
            .clipToBounds()
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), CircleShape)
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: contentColor,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
fun GlassPill(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val darkTheme = isDarkGlassTheme()
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            darkTheme -> Color.White.copy(alpha = 0.065f)
            else -> Color.White.copy(alpha = 0.30f)
        },
        animationSpec = tween(GlassMotion.QUICK_MS),
        label = "pillContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.46f)
            darkTheme -> Color.White.copy(alpha = 0.12f)
            else -> Color.White.copy(alpha = 0.52f)
        },
        animationSpec = tween(GlassMotion.QUICK_MS),
        label = "pillBorder",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(GlassMotion.QUICK_MS),
        label = "pillContent",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.94f)
            .defaultMinSize(minHeight = 36.dp)
            .clip(CircleShape)
            .clipToBounds()
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), CircleShape)
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        },
    )
}

@Composable
fun GlassSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val darkTheme = isDarkGlassTheme()
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { data ->
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(18.dp),
            containerColor = if (darkTheme) {
                Color(0xFF20242C).copy(alpha = 0.97f)
            } else {
                Color(0xFFFBF9F5).copy(alpha = 0.97f)
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
            actionColor = MaterialTheme.colorScheme.primary,
            dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
