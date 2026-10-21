package com.glassnote.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glassnote.R
import com.glassnote.ui.theme.GlassMotion
import kotlinx.coroutines.delay

/**
 * Shared visibility state for the scroll-to-top/bottom controls.
 *
 * It deliberately watches the generic `isScrollInProgress` boolean so the same
 * component works with LazyListState (home) and ScrollState (editor).
 */
@Composable
fun rememberScrollControlsVisible(isScrollInProgress: Boolean): Boolean {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(isScrollInProgress) {
        if (isScrollInProgress) {
            visible = true
        } else {
            delay(2_000)
            visible = false
        }
    }

    return visible
}

/**
 * Stateless pair of frosted scroll-to-edge buttons.
 *
 * The caller owns positioning and the actual animateScrollToItem/animateScrollTo
 * call, because the two screen types expose different scroll APIs.
 */
@Composable
fun ScrollToEdgeControls(
    visible: Boolean,
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
    onScrollToTop: () -> Unit,
    onScrollToBottom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(180)) + scaleIn(
            initialScale = 0.86f,
            animationSpec = tween(180, easing = GlassMotion.EaseOut),
        ),
        exit = fadeOut(tween(180)) + scaleOut(
            targetScale = 0.86f,
            animationSpec = tween(180, easing = GlassMotion.EaseInOut),
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassIconButton(
                onClick = onScrollToTop,
                icon = Icons.Outlined.KeyboardArrowUp,
                contentDescription = stringResource(R.string.cd_scroll_to_top),
                enabled = canScrollBackward,
            )
            GlassIconButton(
                onClick = onScrollToBottom,
                icon = Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_scroll_to_bottom),
                enabled = canScrollForward,
            )
        }
    }
}
