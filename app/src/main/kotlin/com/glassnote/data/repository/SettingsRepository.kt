package com.glassnote.data.repository

import com.glassnote.domain.model.AppMode
import com.glassnote.domain.model.AppSettings
import com.glassnote.domain.model.BackgroundSource
import com.glassnote.domain.model.DarkModePreference
import com.glassnote.domain.model.GlassGradient
import com.glassnote.domain.model.StorageMode
import com.glassnote.domain.model.TransitionStyle
import kotlinx.coroutines.flow.Flow

/**
 * Appearance settings persisted with Preferences DataStore.
 *
 * The flow always emits a complete [AppSettings] object; missing or malformed
 * values fall back to safe defaults instead of surfacing an error to the UI.
 */
interface SettingsRepository {

    val settings: Flow<AppSettings>

    /** Selects a built-in gradient and makes it the active background. */
    suspend fun selectGradient(gradient: GlassGradient)

    /**
     * Stores a user-picked image URI and makes image mode active. The caller is
     * responsible for taking a persistable read permission if the URI needs to
     * survive process death.
     */
    suspend fun setBackgroundImage(imageUri: String)

    /** Removes the image reference and falls back to the current gradient. */
    suspend fun clearBackgroundImage()

    /** Coerces the requested value into the supported 0..80 dp range. */
    suspend fun setBlurRadiusDp(blurRadiusDp: Int)

    suspend fun setDarkModePreference(preference: DarkModePreference)

    suspend fun setTransitionStyle(style: TransitionStyle)

    suspend fun setAppMode(mode: AppMode)

    suspend fun setTransitionsEnabled(enabled: Boolean)

    suspend fun setUrlDetectionEnabled(enabled: Boolean)

    /**
     * Persists the first-run storage decision. SAF requires a tree URI and the
     * resolved public folder name.
     */
    suspend fun setStorageDecision(
        mode: StorageMode,
        treeUri: String? = null,
        folderName: String? = null,
    )

    suspend fun resetToDefaults()
}
