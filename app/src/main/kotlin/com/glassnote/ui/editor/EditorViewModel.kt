package com.glassnote.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.glassnote.data.repository.NoteRepository
import com.glassnote.domain.model.AmbienceColor
import com.glassnote.domain.model.NoteBlock
import com.glassnote.domain.model.Tag
import com.glassnote.domain.model.TagWriteResult
import com.glassnote.domain.model.isBlankDocument
import com.glassnote.ui.theme.GlassMotion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

enum class EditorSaveStatus {
    SAVED,
    PENDING,
    SAVING,
    FAILED,
}

data class EditorUiState(
    val noteId: Long,
    val title: String = "",
    val blocks: List<NoteBlock> = emptyList(),
    val pinned: Boolean = false,
    val ambienceColor: AmbienceColor = AmbienceColor.DEFAULT,
    val selectedTagIds: Set<Long> = emptySet(),
    val tags: List<Tag> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val saveStatus: EditorSaveStatus = EditorSaveStatus.SAVED,
    val saveRevision: Long = 0L,
)

data class MergeOutcome(
    val focusBlockId: String,
    val cursorOffset: Int,
)

@OptIn(FlowPreview::class)
class EditorViewModel(
    noteId: Long,
    private val repository: NoteRepository,
) : ViewModel() {

    private data class Draft(
        val title: String,
        val blocks: List<NoteBlock>,
        val ambienceColor: AmbienceColor,
        val tagIds: Set<Long>,
    )

    private val _uiState = MutableStateFlow(
        EditorUiState(
            noteId = noteId,
            blocks = listOf(NoteBlock.TextBlock(content = "")),
        ),
    )
    val uiState: StateFlow<EditorUiState> = _uiState

    private var lastSavedDraft: Draft? = null
    private var currentNoteId: Long = noteId.takeIf { it > 0L } ?: 0L
    private var pinSaveJob: Job? = null
    private val saveMutex = Mutex()
    private val pinMutex = Mutex()

    private val saveRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        loadNote(noteId)

        viewModelScope.launch {
            repository.observeTags().collect { tags ->
                _uiState.update { current ->
                    val validIds = tags.mapTo(HashSet()) { it.id }
                    current.copy(
                        tags = tags,
                        selectedTagIds = if (current.isLoading) {
                            current.selectedTagIds
                        } else {
                            current.selectedTagIds.intersect(validIds)
                        },
                    )
                }
            }
        }

        viewModelScope.launch {
            saveRequests
                .debounce(GlassMotion.AUTOSAVE_DEBOUNCE_MS)
                .collect { persistCurrentDraft() }
        }
    }

    private fun loadNote(noteId: Long) {
        viewModelScope.launch {
            if (noteId <= 0L) {
                val now = System.currentTimeMillis()
                val initialBlocks = listOf(NoteBlock.TextBlock(content = ""))
                lastSavedDraft = Draft(
                    title = "",
                    blocks = initialBlocks,
                    ambienceColor = AmbienceColor.DEFAULT,
                    tagIds = emptySet(),
                )
                _uiState.update {
                    it.copy(
                        blocks = initialBlocks,
                        isLoading = false,
                        loadFailed = false,
                        createdAt = now,
                        updatedAt = now,
                        saveStatus = EditorSaveStatus.SAVED,
                    )
                }
                return@launch
            }

            val note = repository.getNote(noteId)
            if (note == null) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                return@launch
            }

            val blocks = note.blocks.ifEmpty {
                listOf(NoteBlock.TextBlock(content = ""))
            }
            val draft = Draft(
                title = note.title,
                blocks = note.blocks,
                ambienceColor = note.ambienceColor,
                tagIds = note.tags.mapTo(LinkedHashSet()) { it.id },
            )
            currentNoteId = note.id
            lastSavedDraft = draft
            _uiState.update {
                it.copy(
                    title = note.title,
                    blocks = blocks,
                    pinned = note.pinned,
                    ambienceColor = note.ambienceColor,
                    selectedTagIds = draft.tagIds,
                    isLoading = false,
                    loadFailed = false,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    saveStatus = EditorSaveStatus.SAVED,
                )
            }
        }
    }

    fun onTitleChange(title: String) {
        if (!canEdit()) return
        _uiState.update { it.copy(title = title, saveStatus = EditorSaveStatus.PENDING) }
        requestDebouncedSave()
    }

    fun onBlockTextChange(blockId: String, content: String) {
        if (!canEdit()) return
        _uiState.update { state ->
            state.copy(
                blocks = state.blocks.map { block ->
                    if (block.id == blockId && block is NoteBlock.TextBlock) {
                        block.copy(content = content)
                    } else {
                        block
                    }
                },
                saveStatus = EditorSaveStatus.PENDING,
            )
        }
        requestDebouncedSave()
    }

    /**
     * Inserts [block] after [activeBlockId] and always adds a fresh empty text
     * block after it. Returns the new text block id so the UI can focus it.
     */
    fun insertBlockAfter(activeBlockId: String?, block: NoteBlock): String {
        val newTextBlock = NoteBlock.TextBlock(
            id = UUID.randomUUID().toString(),
            content = "",
        )
        _uiState.update { state ->
            val activeIndex = state.blocks.indexOfFirst { it.id == activeBlockId }
            val insertAt = if (activeIndex >= 0) activeIndex + 1 else state.blocks.size
            val updated = state.blocks.toMutableList().apply {
                add(insertAt, block)
                add(insertAt + 1, newTextBlock)
            }
            state.copy(
                blocks = updated,
                saveStatus = EditorSaveStatus.PENDING,
            )
        }
        requestDebouncedSave()
        return newTextBlock.id
    }

    fun deleteBlock(blockId: String) {
        if (!canEdit()) return
        _uiState.update { state ->
            val remaining = state.blocks.filterNot { it.id == blockId }.ifEmpty {
                listOf(NoteBlock.TextBlock(content = ""))
            }
            state.copy(
                blocks = remaining,
                saveStatus = EditorSaveStatus.PENDING,
            )
        }
        requestDebouncedSave()
    }

    /**
     * Backspace at offset 0:
     * - previous media block: delete it, keep focus on current text;
     * - previous text block: append current text to it and focus the join point.
     */
    fun mergeWithPrevious(currentBlockId: String): MergeOutcome? {
        if (!canEdit()) return null
        val state = _uiState.value
        val index = state.blocks.indexOfFirst { it.id == currentBlockId }
        if (index <= 0) return null
        val current = state.blocks[index] as? NoteBlock.TextBlock ?: return null
        val previous = state.blocks[index - 1]

        return if (previous is NoteBlock.TextBlock) {
            val mergedContent = previous.content + current.content
            val boundary = previous.content.length
            val updated = state.blocks.toMutableList().apply {
                this[index - 1] = previous.copy(content = mergedContent)
                removeAt(index)
            }
            _uiState.update {
                it.copy(
                    blocks = updated,
                    saveStatus = EditorSaveStatus.PENDING,
                )
            }
            requestDebouncedSave()
            MergeOutcome(focusBlockId = previous.id, cursorOffset = boundary)
        } else {
            val updated = state.blocks.toMutableList().apply { removeAt(index - 1) }
            _uiState.update {
                it.copy(
                    blocks = updated,
                    saveStatus = EditorSaveStatus.PENDING,
                )
            }
            requestDebouncedSave()
            MergeOutcome(focusBlockId = current.id, cursorOffset = 0)
        }
    }

    fun selectAmbience(color: AmbienceColor) {
        if (!canEdit()) return
        _uiState.update { it.copy(ambienceColor = color, saveStatus = EditorSaveStatus.PENDING) }
        requestDebouncedSave()
    }

    fun toggleTag(tagId: Long) {
        if (!canEdit()) return
        _uiState.update { current ->
            val updatedIds = if (tagId in current.selectedTagIds) {
                current.selectedTagIds - tagId
            } else {
                current.selectedTagIds + tagId
            }
            current.copy(selectedTagIds = updatedIds, saveStatus = EditorSaveStatus.PENDING)
        }
        requestDebouncedSave()
    }

    suspend fun createTag(name: String): TagWriteResult {
        val result = repository.createTag(name)
        if (result is TagWriteResult.Success) {
            _uiState.update { current ->
                val tags = (current.tags + result.tag)
                    .distinctBy(Tag::id)
                    .sortedBy { tag -> tag.name.lowercase() }
                current.copy(
                    tags = tags,
                    selectedTagIds = current.selectedTagIds + result.tag.id,
                    saveStatus = EditorSaveStatus.PENDING,
                )
            }
            requestDebouncedSave()
        }
        return result
    }

    fun togglePinned() {
        if (!canEdit()) return
        val target = !_uiState.value.pinned
        _uiState.update { it.copy(pinned = target) }
        val noteId = currentNoteId
        if (noteId <= 0L) return

        pinSaveJob?.cancel()
        pinSaveJob = viewModelScope.launch {
            val saved = pinMutex.withLock {
                repository.setPinned(noteId, target)
            }
            if (!saved && _uiState.value.pinned == target) {
                _uiState.update { it.copy(pinned = !target) }
            }
        }
    }

    fun retrySave() {
        if (!canEdit()) return
        viewModelScope.launch { persistCurrentDraft() }
    }

    suspend fun flushPendingSave() {
        if (!canEdit()) return
        pinSaveJob?.cancel()
        pinSaveJob = null
        withContext(NonCancellable) {
            persistCurrentDraft()
            val noteId = currentNoteId
            if (noteId > 0L) {
                pinMutex.withLock {
                    repository.setPinned(noteId, _uiState.value.pinned)
                }
            }
        }
    }

    fun flushInBackground() {
        if (!canEdit()) return
        viewModelScope.launch {
            persistCurrentDraft()
            val noteId = currentNoteId
            if (noteId > 0L) {
                pinMutex.withLock {
                    repository.setPinned(noteId, _uiState.value.pinned)
                }
            }
        }
    }

    private fun requestDebouncedSave() {
        if (!canEdit()) return
        saveRequests.tryEmit(Unit)
    }

    private suspend fun persistCurrentDraft() {
        if (!canEdit()) return

        saveMutex.withLock {
            val current = _uiState.value
            if (!current.isEditable()) return

            val draft = current.toDraft()
            if (draft.title.isBlank() && draft.blocks.isBlankDocument()) {
                deleteBlankDraft()
                return
            }

            if (currentNoteId > 0L && draft == lastSavedDraft) {
                _uiState.update { it.copy(saveStatus = EditorSaveStatus.SAVED) }
                return
            }

            _uiState.update { it.copy(saveStatus = EditorSaveStatus.SAVING) }
            var createdId = 0L
            val saved = try {
                withContext(Dispatchers.IO) {
                    if (currentNoteId <= 0L) {
                        val newId = repository.createNote(
                            title = draft.title,
                            blocks = draft.blocks,
                            ambienceColor = draft.ambienceColor,
                            tagIds = draft.tagIds,
                        )
                        if (newId <= 0L) {
                            false
                        } else {
                            if (_uiState.value.pinned) {
                                repository.setPinned(newId, true)
                            }
                            createdId = newId
                            true
                        }
                    } else {
                        val savedBlocks = repository.saveDraft(
                            noteId = currentNoteId,
                            title = draft.title,
                            blocks = draft.blocks,
                            ambienceColor = draft.ambienceColor,
                        )
                        if (!savedBlocks) {
                            false
                        } else if (draft.tagIds != lastSavedDraft?.tagIds) {
                            repository.setNoteTags(currentNoteId, draft.tagIds)
                        } else {
                            true
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                false
            }

            if (saved) {
                if (createdId > 0L) currentNoteId = createdId
                lastSavedDraft = draft
                val now = System.currentTimeMillis()
                val stillCurrent = _uiState.value.toDraft() == draft
                _uiState.update { latest ->
                    latest.copy(
                        noteId = if (createdId > 0L) createdId else latest.noteId,
                        saveStatus = if (stillCurrent) {
                            EditorSaveStatus.SAVED
                        } else {
                            EditorSaveStatus.PENDING
                        },
                        saveRevision = if (stillCurrent) {
                            latest.saveRevision + 1L
                        } else {
                            latest.saveRevision
                        },
                        createdAt = if (createdId > 0L) now else latest.createdAt,
                        updatedAt = if (stillCurrent) now else latest.updatedAt,
                    )
                }
            } else {
                _uiState.update { it.copy(saveStatus = EditorSaveStatus.FAILED) }
            }
        }
    }

    private suspend fun deleteBlankDraft() {
        val noteId = currentNoteId
        if (noteId <= 0L) {
            _uiState.update { it.copy(saveStatus = EditorSaveStatus.SAVED) }
            return
        }

        pinSaveJob?.cancel()
        pinSaveJob = null
        val deleted = try {
            withContext(Dispatchers.IO) { repository.deleteNote(noteId) }
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        }

        if (deleted) {
            currentNoteId = 0L
            lastSavedDraft = null
            _uiState.update {
                it.copy(noteId = 0L, saveStatus = EditorSaveStatus.SAVED)
            }
        } else {
            _uiState.update { it.copy(saveStatus = EditorSaveStatus.FAILED) }
        }
    }

    private fun EditorUiState.toDraft(): Draft = Draft(
        title = title,
        blocks = blocks,
        ambienceColor = ambienceColor,
        tagIds = selectedTagIds,
    )

    private fun canEdit(): Boolean = _uiState.value.isEditable()

    private fun EditorUiState.isEditable(): Boolean = !isLoading && !loadFailed

    companion object {
        fun factory(
            noteId: Long,
            repository: NoteRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { EditorViewModel(noteId = noteId, repository = repository) }
        }
    }
}
