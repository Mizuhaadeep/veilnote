package com.glassnote.domain.model

/** Background options available on the settings screen. */
enum class BackgroundSource {
    GRADIENT,
    IMAGE,
}

/** Built-in gradient presets. Persisted by [storageKey], never by ordinal. */
enum class GlassGradient(val storageKey: String) {
    AURORA("aurora"),
    SUNSET("sunset"),
    MIDNIGHT("midnight"),
    SAGE("sage"),
    BLUSH("blush"),
    OCEAN("ocean"),
    ;

    companion object {
        val DEFAULT: GlassGradient = AURORA

        fun fromStorageKey(key: String?): GlassGradient =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}

/** Follow the system theme, or force one mode for the app. */
enum class DarkModePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Animated page entrance style. Persisted by enum name. */
enum class TransitionStyle {
    BOTTOM_UP,
    SCALE,
}

/** Where notes are mirrored for uninstall/reinstall survival. */
enum class StorageMode {
    SAF_MODE,
    PRIVATE_MODE,
}

/** Global visual complexity mode. */
enum class AppMode {
    MINIMAL,
    FULL,
}

/** Persisted appearance settings. */
data class AppSettings(
    val backgroundSource: BackgroundSource = BackgroundSource.GRADIENT,
    val gradient: GlassGradient = GlassGradient.DEFAULT,
    val backgroundImageUri: String? = null,
    val blurRadiusDp: Int = DEFAULT_BLUR_RADIUS_DP,
    val darkModePreference: DarkModePreference = DarkModePreference.SYSTEM,
    val transitionStyle: TransitionStyle = TransitionStyle.BOTTOM_UP,
    val appMode: AppMode = AppMode.FULL,
    val transitionsEnabled: Boolean = true,
    val urlDetectionEnabled: Boolean = true,
    /** null means the first-run storage decision has not been made yet. */
    val storageMode: StorageMode? = null,
    val safTreeUri: String? = null,
    val safFolderName: String? = null,
) {
    /**
     * The source that can actually be rendered. If an image was selected but
     * its URI is missing (for example after clearing app data), the gradient
     * remains a safe fallback.
     */
    val effectiveBackgroundSource: BackgroundSource
        get() = if (
            backgroundSource == BackgroundSource.IMAGE &&
            backgroundImageUri.isNullOrBlank()
        ) {
            BackgroundSource.GRADIENT
        } else {
            backgroundSource
        }

    companion object {
        const val MIN_BLUR_RADIUS_DP = 0
        const val MAX_BLUR_RADIUS_DP = 80
        const val DEFAULT_BLUR_RADIUS_DP = 48
    }
}
