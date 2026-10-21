package com.glassnote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.glassnote.data.repository.NoteRepository
import com.glassnote.domain.model.Note
import com.glassnote.domain.model.Tag
import com.glassnote.domain.model.TagWriteResult
import com.glassnote.ui.theme.GlassMotion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val searchQuery: String = "",
    val selectedTagId: Long? = null,
    val selectedNoteIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() || selectedTagId != null

    val isSelectionMode: Boolean
        get() = selectedNoteIds.isNotEmpty()
}

sealed interface HomeEvent {
    data class NoteDeleted(val noteId: Long, val title: String) : HomeEvent

    data object OperationFailed : HomeEvent
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: NoteRepository,
) : ViewModel() {

    private val searchInput = MutableStateFlow("")
    private val selectedTagId = MutableStateFlow<Long?>(null)
    private val hiddenNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())

    private val eventChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = eventChannel.receiveAsFlow()

    private val deleteJobs = mutableMapOf<Long, Job>()
    private val deleteCleanupJobs = mutableMapOf<Long, Job>()
    private val recentlyDeletedNotes = mutableMapOf<Long, Note>()

    private val visibleNotes: Flow<List<Note>> = combine(
        searchInput
            .debounce(140)
            .map(String::trim)
            .distinctUntilChanged(),
        selectedTagId,
    ) { query, tagId ->
        query to tagId
    }.flatMapLatest { (query, tagId) ->
        repository.observeNotes(searchQuery = query, tagId = tagId)
    }.combine(hiddenNoteIds) { notes, hidden ->
        if (hidden.isEmpty()) notes else notes.filterNot { it.id in hidden }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        visibleNotes,
        repository.observeTags(),
        searchInput,
        selectedTagId,
        selectedNoteIds,
    ) { notes, tags, query, tagId, selection ->
        HomeUiState(
            notes = notes,
            tags = tags,
            searchQuery = query,
            selectedTagId = tagId,
            selectedNoteIds = selection,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onSearchQueryChange(query: String) {
        searchInput.value = query
        selectedNoteIds.value = emptySet()
    }

    fun selectTag(tagId: Long?) {
        selectedTagId.value = tagId
        selectedNoteIds.value = emptySet()
    }

    fun clearFilters() {
        searchInput.value = ""
        selectedTagId.value = null
        selectedNoteIds.value = emptySet()
    }

    fun setEncryption(
        noteId: Long,
        isEncrypted: Boolean,
        passwordHash: String?,
    ) {
        viewModelScope.launch {
            val success = repository.setEncryption(
                noteId = noteId,
                isEncrypted = isEncrypted,
                passwordHash = passwordHash,
            )
            if (!success) {
                eventChannel.send(HomeEvent.OperationFailed)
            }
        }
    }

    fun enterSelection(noteId: Long) {
        selectedNoteIds.value = setOf(noteId)
    }

    fun toggleSelection(noteId: Long) {
        selectedNoteIds.update { current ->
            if (noteId in current) current - noteId else current + noteId
        }
    }

    fun clearSelection() {
        selectedNoteIds.value = emptySet()
    }

    fun deleteSelected() {
        val ids = selectedNoteIds.value
        if (ids.isEmpty()) return
        selectedNoteIds.value = emptySet()
        hiddenNoteIds.update { it + ids }
        viewModelScope.launch {
            val deleted = try {
                repository.deleteNotes(ids)
            } catch (_: Exception) {
                hiddenNoteIds.update { it - ids }
                eventChannel.send(HomeEvent.OperationFailed)
                return@launch
            }
            hiddenNoteIds.update { it - ids }
            if (deleted != ids.size) {
                eventChannel.send(HomeEvent.OperationFailed)
            }
        }
    }

    /**
     * A fresh note has no database row yet. Navigation is now handled directly
     * by the UI so a tap has no channel/coroutine round-trip; the editor creates
     * the note only after the first non-blank save.
     */
    fun togglePinned(note: Note) {
        viewModelScope.launch {
            val updated = repository.setPinned(note.id, !note.pinned)
            if (!updated) {
                eventChannel.send(HomeEvent.OperationFailed)
            }
        }
    }

    fun togglePinnedBottom(note: Note) {
        viewModelScope.launch {
            val updated = repository.setPinnedBottom(note.id, !note.isPinnedBottom)
            if (!updated) {
                eventChannel.send(HomeEvent.OperationFailed)
            }
        }
    }

    /**
     * Hides the note immediately, then schedules the hard delete after a grace
     * period. The UI shows a Snackbar with Undo. If Undo arrives before the
     * database operation, the job is simply cancelled. If it arrives after the
     * hard delete, the returned snapshot is restored.
     */
    fun deleteNote(note: Note) {
        if (deleteJobs.containsKey(note.id)) return
        selectedNoteIds.update { it - note.id }

        deleteCleanupJobs.remove(note.id)?.cancel()
        recentlyDeletedNotes.remove(note.id)
        hiddenNoteIds.update { hidden -> hidden + note.id }
        eventChannel.trySend(HomeEvent.NoteDeleted(noteId = note.id, title = note.title))

        deleteJobs[note.id] = viewModelScope.launch {
            delay(GlassMotion.DELETE_GRACE_MS)
            val snapshot = withContext(NonCancellable) {
                repository.deleteNote(note.id)
            }
            deleteJobs.remove(note.id)
            if (snapshot != null) {
                recentlyDeletedNotes[note.id] = snapshot
                scheduleSnapshotCleanup(note.id)
            }
        }
    }

    fun undoDelete(noteId: Long) {
        val deleteJob = deleteJobs.remove(noteId)
        val existingSnapshot = recentlyDeletedNotes.remove(noteId)
        if (deleteJob == null && existingSnapshot == null) {
            hiddenNoteIds.update { hidden -> hidden - noteId }
            return
        }

        deleteJob?.cancel()
        viewModelScope.launch {
            if (existingSnapshot != null) {
                hiddenNoteIds.update { hidden -> hidden - noteId }
                restoreSnapshot(noteId, existingSnapshot)
                return@launch
            }

            // The grace delay may have elapsed a few milliseconds before the
            // Snackbar action arrived. Wait for that non-cancellable delete to
            // finish and restore its snapshot instead of losing the note.
            deleteJob?.join()
            val lateSnapshot = recentlyDeletedNotes.remove(noteId)
            hiddenNoteIds.update { hidden -> hidden - noteId }
            if (lateSnapshot != null) {
                restoreSnapshot(noteId, lateSnapshot)
            }
        }
    }

    private suspend fun restoreSnapshot(noteId: Long, snapshot: Note) {
        if (!repository.restoreNote(snapshot)) {
            eventChannel.send(HomeEvent.OperationFailed)
        } else {
            hiddenNoteIds.update { hidden -> hidden - noteId }
        }
    }

    suspend fun createTag(name: String): TagWriteResult = repository.createTag(name)

    suspend fun renameTag(tagId: Long, name: String): TagWriteResult =
        repository.renameTag(tagId, name)

    suspend fun deleteTag(tagId: Long): Boolean {
        val deleted = repository.deleteTag(tagId)
        if (deleted && selectedTagId.value == tagId) {
            selectedTagId.value = null
        }
        return deleted
    }

    private fun scheduleSnapshotCleanup(noteId: Long) {
        deleteCleanupJobs.remove(noteId)?.cancel()
        deleteCleanupJobs[noteId] = viewModelScope.launch {
            delay(SNAPSHOT_RETENTION_MS)
            hiddenNoteIds.update { hidden -> hidden - noteId }
            recentlyDeletedNotes.remove(noteId)
            deleteCleanupJobs.remove(noteId)
        }
    }

    override fun onCleared() {
        deleteJobs.values.forEach(Job::cancel)
        deleteCleanupJobs.values.forEach(Job::cancel)
        super.onCleared()
    }

    companion object {
        private const val SNAPSHOT_RETENTION_MS = 60_000L

        fun factory(repository: NoteRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}
