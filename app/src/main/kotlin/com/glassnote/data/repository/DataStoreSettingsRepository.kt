package com.glassnote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.glassnote.domain.model.AppMode
import com.glassnote.domain.model.AppSettings
import com.glassnote.domain.model.BackgroundSource
import com.glassnote.domain.model.DarkModePreference
import com.glassnote.domain.model.GlassGradient
import com.glassnote.domain.model.StorageMode
import com.glassnote.domain.model.TransitionStyle
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { failure ->
            if (failure is IOException) {
                emit(emptyPreferences())
            } else {
                throw failure
            }
        }
        .map { preferences -> preferences.toAppSettings() }

    override suspend fun selectGradient(gradient: GlassGradient) {
        dataStore.edit { preferences ->
            preferences[KEY_GRADIENT] = gradient.storageKey
            preferences[KEY_BACKGROUND_SOURCE] = BackgroundSource.GRADIENT.name
        }
    }

    override suspend fun setBackgroundImage(imageUri: String) {
        val normalizedUri = imageUri.trim()
        if (normalizedUri.isEmpty()) {
            clearBackgroundImage()
            return
        }

        dataStore.edit { preferences ->
            preferences[KEY_BACKGROUND_IMAGE_URI] = normalizedUri
            preferences[KEY_BACKGROUND_SOURCE] = BackgroundSource.IMAGE.name
        }
    }

    override suspend fun clearBackgroundImage() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_BACKGROUND_IMAGE_URI)
            preferences[KEY_BACKGROUND_SOURCE] = BackgroundSource.GRADIENT.name
        }
    }

    override suspend fun setBlurRadiusDp(blurRadiusDp: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_BLUR_RADIUS_DP] = blurRadiusDp.coerceIn(
                AppSettings.MIN_BLUR_RADIUS_DP,
                AppSettings.MAX_BLUR_RADIUS_DP,
            )
        }
    }

    override suspend fun setDarkModePreference(preference: DarkModePreference) {
        dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = preference.name
        }
    }

    override suspend fun setTransitionStyle(style: TransitionStyle) {
        dataStore.edit { preferences ->
            preferences[KEY_TRANSITION_STYLE] = style.name
        }
    }

    override suspend fun setAppMode(mode: AppMode) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_MODE] = mode.name
        }
    }

    override suspend fun setTransitionsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_TRANSITIONS_ENABLED] = enabled
        }
    }

    override suspend fun setUrlDetectionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_URL_DETECTION_ENABLED] = enabled
        }
    }

    override suspend fun setStorageDecision(
        mode: StorageMode,
        treeUri: String?,
        folderName: String?,
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_STORAGE_MODE] = mode.name
            if (mode == StorageMode.SAF_MODE) {
                treeUri?.takeIf(String::isNotBlank)?.let {
                    preferences[KEY_SAF_TREE_URI] = it
                }
                folderName?.takeIf(String::isNotBlank)?.let {
                    preferences[KEY_SAF_FOLDER_NAME] = it
                }
            } else {
                preferences.remove(KEY_SAF_TREE_URI)
                preferences.remove(KEY_SAF_FOLDER_NAME)
            }
        }
    }

    override suspend fun resetToDefaults() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val imageUri = this[KEY_BACKGROUND_IMAGE_URI]?.takeIf(String::isNotBlank)
        val storedSource = enumOrDefault(
            rawValue = this[KEY_BACKGROUND_SOURCE],
            fallback = BackgroundSource.GRADIENT,
        )
        val source = if (storedSource == BackgroundSource.IMAGE && imageUri == null) {
            BackgroundSource.GRADIENT
        } else {
            storedSource
        }

        val blurRadius = (this[KEY_BLUR_RADIUS_DP] ?: AppSettings.DEFAULT_BLUR_RADIUS_DP)
            .coerceIn(
                AppSettings.MIN_BLUR_RADIUS_DP,
                AppSettings.MAX_BLUR_RADIUS_DP,
            )

        return AppSettings(
            backgroundSource = source,
            gradient = GlassGradient.fromStorageKey(this[KEY_GRADIENT]),
            backgroundImageUri = imageUri,
            blurRadiusDp = blurRadius,
            darkModePreference = enumOrDefault(
                rawValue = this[KEY_DARK_MODE],
                fallback = DarkModePreference.SYSTEM,
            ),
            transitionStyle = enumOrDefault(
                rawValue = this[KEY_TRANSITION_STYLE],
                fallback = TransitionStyle.BOTTOM_UP,
            ),
            appMode = enumOrDefault(
                rawValue = this[KEY_APP_MODE],
                fallback = AppMode.FULL,
            ),
            transitionsEnabled = this[KEY_TRANSITIONS_ENABLED] ?: true,
            urlDetectionEnabled = this[KEY_URL_DETECTION_ENABLED] ?: true,
            storageMode = this[KEY_STORAGE_MODE]?.let { raw ->
                enumValues<StorageMode>().firstOrNull { it.name == raw }
            },
            safTreeUri = this[KEY_SAF_TREE_URI]?.takeIf(String::isNotBlank),
            safFolderName = this[KEY_SAF_FOLDER_NAME]?.takeIf(String::isNotBlank),
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(
        rawValue: String?,
        fallback: T,
    ): T = enumValues<T>().firstOrNull { it.name == rawValue } ?: fallback

    private companion object {
        val KEY_BACKGROUND_SOURCE = stringPreferencesKey("background_source")
        val KEY_GRADIENT = stringPreferencesKey("background_gradient")
        val KEY_BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        val KEY_BLUR_RADIUS_DP = intPreferencesKey("blur_radius_dp")
        val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        val KEY_TRANSITION_STYLE = stringPreferencesKey("transition_style")
        val KEY_APP_MODE = stringPreferencesKey("app_mode")
        val KEY_TRANSITIONS_ENABLED = booleanPreferencesKey("transitions_enabled")
        val KEY_URL_DETECTION_ENABLED = booleanPreferencesKey("url_detection_enabled")
        val KEY_STORAGE_MODE = stringPreferencesKey("storage_mode")
        val KEY_SAF_TREE_URI = stringPreferencesKey("saf_tree_uri")
        val KEY_SAF_FOLDER_NAME = stringPreferencesKey("saf_folder_name")
    }
}
