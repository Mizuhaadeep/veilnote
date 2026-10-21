package com.glassnote.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.glassnote.domain.model.AppMode

/**
 * MINIMAL disables URL annotations and decorative transitions globally without
 * threading a parameter through every card/editor composable.
 */
val LocalAppMode = staticCompositionLocalOf { AppMode.FULL }

/**
 * Effective URL-link switch: FULL mode AND the per-feature toggle.
 * MINIMAL mode should provide false here.
 */
val LocalUrlDetectionEnabled = staticCompositionLocalOf { true }
