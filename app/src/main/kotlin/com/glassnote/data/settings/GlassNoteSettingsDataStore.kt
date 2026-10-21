package com.glassnote.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore

private const val SETTINGS_DATASTORE_NAME = "glass_note_settings"

/**
 * One process-wide Preferences DataStore for appearance settings.
 *
 * Keeping the delegate at top level guarantees that no two DataStore instances
 * can point at the same file, which would otherwise throw at runtime.
 */
val Context.glassNoteSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_DATASTORE_NAME,
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
