package com.glassnote.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.glassnote.data.repository.SettingsRepository
import com.glassnote.data.settings.BackgroundImageStore
import com.glassnote.domain.model.AppMode
import com.glassnote.domain.model.AppSettings
import com.glassnote.domain.model.DarkModePreference
import com.glassnote.domain.model.GlassGradient
import com.glassnote.domain.model.StorageMode
import com.glassnote.domain.model.TransitionStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isImportingImage: Boolean = false,
    val imageImportFailed: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backgroundImageStore: BackgroundImageStore,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings(),
    )

    /** False until DataStore has emitted its first value. */
    val settingsLoaded: StateFlow<Boolean> = settingsRepository.settings
        .map { true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    private val isImportingImage = MutableStateFlow(false)
    private val imageImportFailed = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settings,
        isImportingImage,
        imageImportFailed,
    ) { currentSettings, importing, importFailed ->
        SettingsUiState(
            settings = currentSettings,
            isImportingImage = importing,
            imageImportFailed = importFailed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState(),
    )

    fun selectGradient(gradient: GlassGradient) {
        viewModelScope.launch {
            settingsRepository.selectGradient(gradient)
        }
    }

    fun useBackgroundImage(source: Uri) {
        if (isImportingImage.value) return
        viewModelScope.launch {
            isImportingImage.value = true
            imageImportFailed.value = false
            val previousUri = settings.value.backgroundImageUri
            var storedUri: Uri? = null
            try {
                storedUri = backgroundImageStore.import(source)
                settingsRepository.setBackgroundImage(storedUri.toString())
                backgroundImageStore.deleteManagedImage(previousUri)
            } catch (_: Exception) {
                storedUri?.let { backgroundImageStore.deleteManagedImage(it.toString()) }
                imageImportFailed.value = true
            } finally {
                isImportingImage.value = false
            }
        }
    }

    fun clearBackgroundImage() {
        viewModelScope.launch {
            val previousUri = settings.value.backgroundImageUri
            try {
                settingsRepository.clearBackgroundImage()
                backgroundImageStore.deleteManagedImage(previousUri)
            } catch (_: Exception) {
                imageImportFailed.value = true
            }
        }
    }

    /** Re-activates the already saved image after the user picks a gradient. */
    fun activateSavedBackgroundImage() {
        val imageUri = settings.value.backgroundImageUri ?: return
        viewModelScope.launch {
            try {
                settingsRepository.setBackgroundImage(imageUri)
            } catch (_: Exception) {
                imageImportFailed.value = true
            }
        }
    }

    fun setBlurRadiusDp(radiusDp: Int) {
        viewModelScope.launch {
            settingsRepository.setBlurRadiusDp(radiusDp)
        }
    }

    fun setDarkModePreference(preference: DarkModePreference) {
        viewModelScope.launch {
            settingsRepository.setDarkModePreference(preference)
        }
    }

    fun setTransitionStyle(style: TransitionStyle) {
        viewModelScope.launch {
            settingsRepository.setTransitionStyle(style)
        }
    }

    fun setAppMode(mode: AppMode) {
        viewModelScope.launch {
            settingsRepository.setAppMode(mode)
        }
    }

    fun setTransitionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTransitionsEnabled(enabled)
        }
    }

    fun setUrlDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUrlDetectionEnabled(enabled)
        }
    }

    fun setStorageMode(
        mode: StorageMode,
        treeUri: String? = null,
        folderName: String? = null,
    ) {
        viewModelScope.launch {
            settingsRepository.setStorageDecision(
                mode = mode,
                treeUri = treeUri,
                folderName = folderName,
            )
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                settingsRepository.resetToDefaults()
                backgroundImageStore.clearManagedImages()
                imageImportFailed.value = false
            } catch (_: Exception) {
                imageImportFailed.value = true
            }
        }
    }

    fun dismissImageError() {
        imageImportFailed.value = false
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            backgroundImageStore: BackgroundImageStore,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepository = settingsRepository,
                    backgroundImageStore = backgroundImageStore,
                )
            }
        }
    }
}
