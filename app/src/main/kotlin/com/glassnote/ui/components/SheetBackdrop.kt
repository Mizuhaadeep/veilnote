package com.glassnote.ui.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.glassnote.ui.theme.GlassMotion

/**
 * Blur-only backdrop for a bottom sheet.
 *
 * There is deliberately no dark scrim: blur alone separates the sheet from the
 * page while preserving the page's colour and brightness.
 *
 * Modifier.blur is a RenderEffect path and is applied on Android 12+ only.
 */
@Composable
fun SheetBackdrop(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurRadius by animateFloatAsState(
        targetValue = if (visible) 20f else 0f,
        animationSpec = tween(
            durationMillis = GlassMotion.SLOW_MS,
            easing = GlassMotion.EaseOut,
        ),
        label = "sheetBackdropBlur",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (supportsBlur && (visible || blurRadius > 0.01f)) {
                        Modifier.blur(
                            radius = blurRadius.dp,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded,
                        )
                    } else {
                        Modifier
                    },
                ),
            content = content,
        )
        // No dark scrim: the sheet is separated by blur only, preserving the
        // underlying colours. The sheet window already blocks interaction.
    }
}

/**
 * Notifies the screen as soon as a sheet starts hiding, including when the user
 * drags it down. That lets the page blur begin fading on the same frame as the
 * sheet exit instead of waiting for `onDismissRequest` after the sheet is gone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetDismissEffect(
    sheetState: SheetState,
    onDismissStarted: () -> Unit,
) {
    LaunchedEffect(sheetState) {
        var hasOpened = false
        var notified = false
        snapshotFlow { sheetState.targetValue }
            .collect { target ->
                if (target != SheetValue.Hidden) hasOpened = true
                if (hasOpened && target == SheetValue.Hidden && !notified) {
                    notified = true
                    onDismissStarted()
                }
            }
    }
}

/**
 * A fast content reveal for a bottom sheet.
 *
 * ModalBottomSheet already owns the window slide. This used to add a second
 * 28dp slide with the slow spring timing, which made every open feel delayed
 * after a rapid open/close. The sheet now only fades its content in quickly;
 * the window motion remains the single source of opening animation.
 */
@Composable
fun AnimatedSheetContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 120,
            easing = GlassMotion.EaseOut,
        ),
        label = "sheetContentAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = contentAlpha },
    ) {
        content()
    }
}
