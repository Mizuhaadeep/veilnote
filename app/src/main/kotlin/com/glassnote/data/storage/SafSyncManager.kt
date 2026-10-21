package com.glassnote.data.storage

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.glassnote.R
import com.glassnote.data.repository.NoteRepository
import com.glassnote.data.repository.SettingsRepository
import com.glassnote.domain.model.StorageMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

/**
 * Keeps the Room cache and the public `.veil` mirror in sync.
 *
 * The first read imports files that survived an uninstall/reinstall, then every
 * Room change is exported after a short debounce. Any permission/IO failure is
 * caught here, the user is warned, and the persisted mode is forced back to
 * PRIVATE_MODE so the app keeps working from Room only.
 */
class SafSyncManager(
    private val context: Context,
    private val noteRepository: NoteRepository,
    private val settingsRepository: SettingsRepository,
    private val noteStore: SafNoteStore,
) {

    suspend fun run(treeUri: Uri) {
        try {
            val imported = noteStore.readAll(treeUri)
            if (imported.isNotEmpty()) {
                noteRepository.importNotes(imported)
            }

            noteRepository.observeNotes().collectLatest { notes ->
                delay(EXPORT_DEBOUNCE_MS)
                noteStore.export(treeUri, notes)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            settingsRepository.setStorageDecision(StorageMode.PRIVATE_MODE)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    R.string.storage_fallback_toast,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    private companion object {
        const val EXPORT_DEBOUNCE_MS = 350L
    }
}
