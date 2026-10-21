package com.glassnote

import android.content.Context
import com.glassnote.data.local.GlassNoteDatabase
import com.glassnote.data.repository.DataStoreSettingsRepository
import com.glassnote.data.repository.NoteRepository
import com.glassnote.data.repository.OfflineNoteRepository
import com.glassnote.data.repository.SettingsRepository
import com.glassnote.data.settings.BackgroundImageStore
import com.glassnote.data.settings.glassNoteSettingsDataStore
import com.glassnote.data.storage.SafNoteStore
import com.glassnote.data.storage.SafSyncManager

/**
 * Manual dependency container. Keeping construction here avoids a DI framework
 * while still making the repositories easy to replace in tests.
 */
class AppContainer(
    context: Context,
    clock: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext

    private val database: GlassNoteDatabase by lazy {
        GlassNoteDatabase.getInstance(appContext)
    }

    val noteRepository: NoteRepository by lazy {
        OfflineNoteRepository(
            database = database,
            noteDao = database.noteDao(),
            tagDao = database.tagDao(),
            clock = clock,
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(appContext.glassNoteSettingsDataStore)
    }

    val backgroundImageStore: BackgroundImageStore by lazy {
        BackgroundImageStore(appContext)
    }

    val safNoteStore: SafNoteStore by lazy {
        SafNoteStore(appContext)
    }

    val safSyncManager: SafSyncManager by lazy {
        SafSyncManager(
            context = appContext,
            noteRepository = noteRepository,
            settingsRepository = settingsRepository,
            noteStore = safNoteStore,
        )
    }
}
