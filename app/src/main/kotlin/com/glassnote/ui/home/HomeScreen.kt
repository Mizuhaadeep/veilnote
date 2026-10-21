package com.glassnote.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glassnote.R
import com.glassnote.domain.model.Note
import com.glassnote.domain.model.Tag
import com.glassnote.ui.components.GlassIconButton
import com.glassnote.ui.components.GlassPill
import com.glassnote.ui.components.GlassSnackbarHost
import com.glassnote.ui.components.GlassSurface
import com.glassnote.ui.components.PasswordDialog
import com.glassnote.ui.components.PasswordDialogMode
import com.glassnote.ui.components.ScrollToEdgeControls
import com.glassnote.ui.components.SheetBackdrop
import com.glassnote.ui.components.TagManagerSheet
import com.glassnote.security.PasswordHasher
import com.glassnote.ui.components.cardPressScale
import com.glassnote.ui.components.iosOverscrollBounce
import com.glassnote.ui.components.pressScale
import com.glassnote.ui.components.rememberOverscrollBounceState
import com.glassnote.ui.components.rememberUrlLinkColor
import com.glassnote.ui.components.UrlText
import com.glassnote.ui.transition.TransitionOrigin
import com.glassnote.ui.components.rememberScrollControlsVisible
import com.glassnote.ui.components.topEdgeFade
import com.glassnote.ui.theme.GlassMotion
import com.glassnote.ui.theme.LocalUrlDetectionEnabled
import com.glassnote.ui.theme.isDarkGlassTheme
import com.glassnote.ui.theme.labelRes
import com.glassnote.ui.theme.palette
import com.glassnote.ui.util.rememberFullDateTime
import com.glassnote.ui.util.rememberNoteListTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class PasswordRequest(
    val note: Note,
    val mode: PasswordDialogMode,
    val origin: TransitionOrigin? = null,
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenNote: (Long, TransitionOrigin?) -> Unit,
    onCreateNote: (TransitionOrigin?) -> Unit,
    onOpenSettings: (TransitionOrigin?) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectionMode = uiState.isSelectionMode
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    val context = LocalContext.current
    var showTagManager by rememberSaveable { mutableStateOf(false) }
    var tagBackdropVisible by rememberSaveable { mutableStateOf(false) }
    var tagSheetLocked by remember { mutableStateOf(false) }
    var passwordRequest by remember { mutableStateOf<PasswordRequest?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val openTagManager: () -> Unit = {
        if (!showTagManager && !tagSheetLocked) {
            tagSheetLocked = true
            tagBackdropVisible = true
            showTagManager = true
        }
    }

    BackHandler(enabled = selectionMode) {
        viewModel.clearSelection()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.NoteDeleted -> {
                    val title = event.title.ifBlank { context.getString(R.string.note_untitled) }
                    val message = context.getString(R.string.note_deleted_snackbar, title)
                    launch {
                        val result = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = context.getString(R.string.action_undo),
                            withDismissAction = true,
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete(event.noteId)
                        }
                    }
                }
                HomeEvent.OperationFailed -> {
                    launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.operation_failed),
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SheetBackdrop(
            visible = tagBackdropVisible,
            modifier = Modifier.fillMaxSize(),
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    GlassSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp),
                    )
                },
                floatingActionButton = {
                    if (!selectionMode) {
                        NewNoteFab(
                            onClick = onCreateNote,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(end = 4.dp, bottom = 4.dp),
                        )
                    }
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .statusBarsPadding(),
                ) {
                    HomeHeader(
                        noteCount = uiState.notes.size,
                        isLoading = uiState.isLoading,
                        selectionCount = uiState.selectedNoteIds.size,
                        onClearSelection = viewModel::clearSelection,
                        onDeleteSelected = viewModel::deleteSelected,
                        onOpenSettings = onOpenSettings,
                    )
                    SearchField(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    TagFilters(
                        tags = uiState.tags,
                        selectedTagId = uiState.selectedTagId,
                        onSelectTag = viewModel::selectTag,
                        onManageTags = openTagManager,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .topEdgeFade(24.dp),
                    ) {
                        HomeContent(
                            uiState = uiState,
                            listState = listState,
                            nowMillis = nowMillis,
                            onOpenNote = onOpenNote,
                            onCreateNote = onCreateNote,
                            onDeleteNote = viewModel::deleteNote,
                            onTogglePin = viewModel::togglePinned,
                            onTogglePinBottom = viewModel::togglePinnedBottom,
                            onEnterSelection = viewModel::enterSelection,
                            onToggleSelection = viewModel::toggleSelection,
                            onClearFilters = viewModel::clearFilters,
                            onEncryptNote = { note ->
                                passwordError = null
                                passwordRequest = PasswordRequest(
                                    note = note,
                                    mode = PasswordDialogMode.ENCRYPT,
                                )
                            },
                            onRemoveEncryption = { note ->
                                passwordError = null
                                passwordRequest = PasswordRequest(
                                    note = note,
                                    mode = PasswordDialogMode.REMOVE_ENCRYPTION,
                                )
                            },
                            onUnlockNote = { note, origin ->
                                passwordError = null
                                passwordRequest = PasswordRequest(
                                    note = note,
                                    mode = PasswordDialogMode.UNLOCK,
                                    origin = origin,
                                )
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        if (showTagManager) {
            TagManagerSheet(
                tags = uiState.tags,
                onCreate = viewModel::createTag,
                onRename = viewModel::renameTag,
                onDelete = viewModel::deleteTag,
                onDismissStarted = { tagBackdropVisible = false },
                onDismiss = {
                    tagBackdropVisible = false
                    tagSheetLocked = false
                    showTagManager = false
                },
            )
        }

        passwordRequest?.let { request ->
            PasswordDialog(
                mode = request.mode,
                errorMessage = passwordError,
                onDismiss = {
                    passwordRequest = null
                    passwordError = null
                },
                onConfirm = { password ->
                    when (request.mode) {
                        PasswordDialogMode.ENCRYPT -> {
                            viewModel.setEncryption(
                                noteId = request.note.id,
                                isEncrypted = true,
                                passwordHash = PasswordHasher.sha256(password),
                            )
                            passwordRequest = null
                            passwordError = null
                        }

                        PasswordDialogMode.UNLOCK -> {
                            if (PasswordHasher.verify(password, request.note.passwordHash)) {
                                val origin = request.origin
                                passwordRequest = null
                                passwordError = null
                                onOpenNote(request.note.id, origin)
                            } else {
                                passwordError = context.getString(R.string.password_error_wrong)
                            }
                        }

                        PasswordDialogMode.REMOVE_ENCRYPTION -> {
                            if (PasswordHasher.verify(password, request.note.passwordHash)) {
                                viewModel.setEncryption(
                                    noteId = request.note.id,
                                    isEncrypted = false,
                                    passwordHash = null,
                                )
                                passwordRequest = null
                                passwordError = null
                            } else {
                                passwordError = context.getString(R.string.password_error_wrong)
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeHeader(
    noteCount: Int,
    isLoading: Boolean,
    selectionCount: Int,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onOpenSettings: (TransitionOrigin?) -> Unit,
) {
    Column {
        var settingsBounds by remember { mutableStateOf<Rect?>(null) }
        AnimatedVisibility(
            visible = selectionCount > 0,
            enter = fadeIn(tween(180)) + expandVertically(
                animationSpec = tween(180),
                expandFrom = Alignment.Top,
            ),
            exit = fadeOut(tween(140)) + shrinkVertically(
                animationSpec = tween(140),
                shrinkTowards = Alignment.Top,
            ),
        ) {
            SelectionHeader(
                count = selectionCount,
                onClearSelection = onClearSelection,
                onDeleteSelected = onDeleteSelected,
            )
        }

        AnimatedVisibility(
            visible = selectionCount == 0,
            enter = fadeIn(tween(180)) + expandVertically(
                animationSpec = tween(180),
                expandFrom = Alignment.Top,
            ),
            exit = fadeOut(tween(140)) + shrinkVertically(
                animationSpec = tween(140),
                shrinkTowards = Alignment.Top,
            ),
        ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.brand_name),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.8.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    isLoading -> stringResource(R.string.brand_tagline)
                    noteCount == 0 -> stringResource(R.string.home_note_count_empty)
                    else -> pluralStringResource(
                        R.plurals.home_note_count,
                        noteCount,
                        noteCount,
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier.onGloballyPositioned { coordinates ->
                settingsBounds = coordinates.boundsInWindow()
            },
        ) {
            GlassIconButton(
                onClick = {
                    onOpenSettings(settingsBounds?.let { TransitionOrigin(it, 21.dp) })
                },
                icon = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.action_settings),
            )
        }
    }
        }
    }
}

@Composable
private fun SelectionHeader(
    count: Int,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(
            onClick = onClearSelection,
            icon = Icons.Outlined.Close,
            contentDescription = stringResource(R.string.cd_exit_selection),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = pluralStringResource(R.plurals.selection_count, count, count),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        GlassIconButton(
            onClick = onDeleteSelected,
            icon = Icons.Outlined.DeleteOutline,
            contentDescription = stringResource(R.string.action_delete_selected),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .animateContentSize(animationSpec = tween(GlassMotion.QUICK_MS)),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds()
                    .background(Color.Transparent)
                    .padding(horizontal = 10.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                        }
                        innerTextField()
                    }
                },
            )
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(tween(GlassMotion.QUICK_MS)),
                exit = fadeOut(tween(GlassMotion.QUICK_MS)),
            ) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clipToBounds()
                        .background(Color.Transparent),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TagFilters(
    tags: List<Tag>,
    selectedTagId: Long?,
    onSelectTag: (Long?) -> Unit,
    onManageTags: () -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .padding(top = 13.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "all") {
            GlassPill(
                selected = selectedTagId == null,
                onClick = { onSelectTag(null) },
            ) {
                Text(
                    text = stringResource(R.string.tag_all),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        items(items = tags, key = Tag::id) { tag ->
            GlassPill(
                selected = selectedTagId == tag.id,
                onClick = { onSelectTag(tag.id) },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Label,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp),
                )
            }
        }
        item(key = "manage") {
            GlassPill(
                selected = false,
                onClick = onManageTags,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.tag_manage),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private enum class HomeContentMode {
    LOADING,
    EMPTY,
    NO_RESULTS,
    NOTES,
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    listState: LazyListState,
    nowMillis: Long,
    onOpenNote: (Long, TransitionOrigin?) -> Unit,
    onCreateNote: (TransitionOrigin?) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onTogglePinBottom: (Note) -> Unit,
    onEnterSelection: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onClearFilters: () -> Unit,
    onEncryptNote: (Note) -> Unit,
    onRemoveEncryption: (Note) -> Unit,
    onUnlockNote: (Note, TransitionOrigin?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mode = when {
        uiState.isLoading -> HomeContentMode.LOADING
        uiState.notes.isEmpty() && uiState.hasActiveFilters -> HomeContentMode.NO_RESULTS
        uiState.notes.isEmpty() -> HomeContentMode.EMPTY
        else -> HomeContentMode.NOTES
    }

    AnimatedContent(
        targetState = mode,
        modifier = modifier,
        transitionSpec = {
            (
                fadeIn(
                    animationSpec = tween(
                        durationMillis = GlassMotion.STANDARD_MS,
                        easing = GlassMotion.EaseOut,
                    ),
                ) + slideInVertically(
                    initialOffsetY = { it / 28 },
                    animationSpec = tween(
                        durationMillis = GlassMotion.STANDARD_MS,
                        easing = GlassMotion.EaseOut,
                    ),
                )
                ) togetherWith fadeOut(
                animationSpec = tween(GlassMotion.QUICK_MS),
            )
        },
        label = "homeContentMode",
    ) { targetMode ->
        when (targetMode) {
            HomeContentMode.LOADING -> HomeLoadingState(modifier = Modifier.fillMaxSize())
            HomeContentMode.EMPTY -> HomeEmptyState(
                title = stringResource(R.string.empty_title),
                body = stringResource(R.string.empty_body),
                actionLabel = stringResource(R.string.empty_action),
                onAction = onCreateNote,
                modifier = Modifier.fillMaxSize(),
            )
            HomeContentMode.NO_RESULTS -> HomeEmptyState(
                title = stringResource(R.string.search_empty_title),
                body = stringResource(R.string.search_empty_body),
                actionLabel = stringResource(R.string.search_clear_filters),
                onAction = { _ -> onClearFilters() },
                modifier = Modifier.fillMaxSize(),
            )
            HomeContentMode.NOTES -> NotesList(
                notes = uiState.notes,
                listState = listState,
                nowMillis = nowMillis,
                selectedNoteIds = uiState.selectedNoteIds,
                onOpenNote = onOpenNote,
                onDeleteNote = onDeleteNote,
                onTogglePin = onTogglePin,
                onTogglePinBottom = onTogglePinBottom,
                onEnterSelection = onEnterSelection,
                onToggleSelection = onToggleSelection,
                onEncryptNote = onEncryptNote,
                onRemoveEncryption = onRemoveEncryption,
                onUnlockNote = onUnlockNote,
            )
        }
    }
}

@Composable
private fun NotesList(
    notes: List<Note>,
    listState: LazyListState,
    nowMillis: Long,
    selectedNoteIds: Set<Long>,
    onOpenNote: (Long, TransitionOrigin?) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onTogglePinBottom: (Note) -> Unit,
    onEnterSelection: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onEncryptNote: (Note) -> Unit,
    onRemoveEncryption: (Note) -> Unit,
    onUnlockNote: (Note, TransitionOrigin?) -> Unit,
) {
    // Entrance animation belongs to a note's first appearance. Lazy items are
    // disposed while scrolling, so remember this outside the item scope.
    val seenNoteIds = remember { mutableSetOf<Long>() }
    val bounceState = rememberOverscrollBounceState(listState.isScrollInProgress)
    val scope = rememberCoroutineScope()
    val scrollControlsVisible = rememberScrollControlsVisible(listState.isScrollInProgress)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .iosOverscrollBounce(bounceState),
            state = listState,
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 14.dp,
                bottom = 126.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = notes,
                key = Note::id,
            ) { note ->
                val animateEntrance = remember(note.id) { seenNoteIds.add(note.id) }
                AnimatedNoteItem(
                    note = note,
                    nowMillis = nowMillis,
                    animateEntrance = animateEntrance,
                    selectionMode = selectedNoteIds.isNotEmpty(),
                    selected = note.id in selectedNoteIds,
                    onClick = { bounds ->
                        val origin = TransitionOrigin(bounds, 24.dp)
                        if (note.isEncrypted) {
                            onUnlockNote(note, origin)
                        } else {
                            onOpenNote(note.id, origin)
                        }
                    },
                    onDelete = { onDeleteNote(note) },
                    onTogglePin = { onTogglePin(note) },
                    onTogglePinBottom = { onTogglePinBottom(note) },
                    onEncrypt = { onEncryptNote(note) },
                    onRemoveEncryption = { onRemoveEncryption(note) },
                    onEnterSelection = { onEnterSelection(note.id) },
                    onToggleSelection = { onToggleSelection(note.id) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(GlassMotion.QUICK_MS, easing = GlassMotion.EaseOut),
                        placementSpec = tween(GlassMotion.STANDARD_MS, easing = GlassMotion.EaseInOut),
                        fadeOutSpec = tween(GlassMotion.QUICK_MS),
                    ),
                )
            }
        }

        ScrollToEdgeControls(
            visible = scrollControlsVisible,
            canScrollBackward = listState.canScrollBackward,
            canScrollForward = listState.canScrollForward,
            onScrollToTop = {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            onScrollToBottom = {
                scope.launch {
                    listState.animateScrollToItem(notes.lastIndex)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 150.dp),
        )
    }
}

@Composable
private fun AnimatedNoteItem(
    note: Note,
    nowMillis: Long,
    animateEntrance: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: (Rect) -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onTogglePinBottom: () -> Unit,
    onEncrypt: () -> Unit,
    onRemoveEncryption: () -> Unit,
    onEnterSelection: () -> Unit,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var entered by remember(note.id) { mutableStateOf(!animateEntrance) }
    var removing by remember(note.id) { mutableStateOf(false) }
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(note.id, animateEntrance) {
        if (animateEntrance) {
            entered = true
        }
    }

    val enterAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(GlassMotion.STANDARD_MS, easing = GlassMotion.EaseOut),
        label = "noteEnterAlpha",
    )
    val enterOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (entered) 0.dp else 14.dp,
        animationSpec = tween(GlassMotion.STANDARD_MS, easing = GlassMotion.EaseOut),
        label = "noteEnterOffset",
    )
    val removeAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (removing) 0f else 1f,
        animationSpec = tween(GlassMotion.QUICK_MS + 40, easing = GlassMotion.EaseInOut),
        label = "noteRemoveAlpha",
    )
    val removeOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (removing) 46.dp else 0.dp,
        animationSpec = tween(GlassMotion.QUICK_MS + 40, easing = GlassMotion.EaseInOut),
        label = "noteRemoveOffset",
    )

    val requestDelete = {
        if (!removing) {
            removing = true
            scope.launch {
                delay(GlassMotion.QUICK_MS.toLong() + 40L)
                onDelete()
            }
        }
    }

    GlassNoteCard(
        note = note,
        nowMillis = nowMillis,
        enabled = !removing,
        selectionMode = selectionMode,
        selected = selected,
        onClick = {
            if (selectionMode) onToggleSelection() else onClick(cardBounds)
        },
        onLongClick = {
            if (selectionMode) onToggleSelection() else onEnterSelection()
        },
        onDelete = requestDelete,
        onTogglePin = onTogglePin,
        onTogglePinBottom = onTogglePinBottom,
        onEncrypt = onEncrypt,
        onRemoveEncryption = onRemoveEncryption,
        onToggleSelection = onToggleSelection,
        modifier = modifier
            .onGloballyPositioned { cardBounds = it.boundsInWindow() }
            .graphicsLayer {
                alpha = enterAlpha * removeAlpha
                translationY = enterOffset.toPx()
                translationX = removeOffset.toPx()
            },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlassNoteCard(
    note: Note,
    nowMillis: Long,
    enabled: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onTogglePinBottom: () -> Unit,
    onEncrypt: () -> Unit,
    onRemoveEncryption: () -> Unit,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val darkTheme = isDarkGlassTheme()
    val context = LocalContext.current
    val urlLinksEnabled = LocalUrlDetectionEnabled.current
    val urlLinkColor = rememberUrlLinkColor()
    val palette = note.ambienceColor.palette(darkTheme)
    val shape = RoundedCornerShape(24.dp)
    val overlayBrush = Brush.linearGradient(
        colors = listOf(
            palette.primary.copy(alpha = if (darkTheme) 0.16f else 0.15f),
            palette.secondary.copy(alpha = if (darkTheme) 0.035f else 0.055f),
        ),
    )
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val moreInteractionSource = remember { MutableInteractionSource() }
    val selectionOverlay by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(220, easing = GlassMotion.EaseInOut),
        label = "cardSelectionTint",
    )
    val wordCount by remember(note.title, note.content, note.isEncrypted) {
        derivedStateOf {
            if (note.isEncrypted) {
                0
            } else {
                note.title.count { !it.isWhitespace() } +
                    note.content.count { !it.isWhitespace() }
            }
        }
    }
    val hasTags = note.tags.isNotEmpty()
    val tagLabel = if (hasTags) {
        buildString {
            append(note.tags.first().name)
            if (note.tags.size > 1) append(" +${note.tags.size - 1}")
        }
    } else {
        ""
    }
    val timeLabel = rememberNoteListTime(
        timestamp = note.updatedAt,
        nowMillis = nowMillis,
    )
    val absoluteTimeLabel = rememberFullDateTime(note.updatedAt)
    val untitledLabel = stringResource(R.string.note_untitled)
    val encryptedTitle = stringResource(R.string.note_encrypted_title)
    val encryptedContent = stringResource(R.string.note_encrypted_hidden)
    val encryptedWordCount = stringResource(R.string.note_encrypted_word_count)
    val encryptedTime = stringResource(R.string.note_encrypted_time)
    val encryptedAbsoluteTime = stringResource(R.string.note_encrypted_absolute_time)
    val titleText = remember(
        note.title,
        note.isEncrypted,
        untitledLabel,
        urlLinkColor,
        context,
        urlLinksEnabled,
    ) {
        if (note.isEncrypted) {
            null
        } else if (urlLinksEnabled) {
            UrlText.buildUrlAnnotatedString(
                text = note.title.ifBlank { untitledLabel },
                linkColor = urlLinkColor,
                onUrlClick = { url -> UrlText.openInBrowser(context, url) },
            )
        } else {
            null
        }
    }
    val contentText = remember(
        note.content,
        note.isEncrypted,
        urlLinkColor,
        context,
        urlLinksEnabled,
    ) {
        if (note.isEncrypted) {
            null
        } else if (urlLinksEnabled && note.content.isNotBlank()) {
            UrlText.buildUrlAnnotatedString(
                text = note.content.trim(),
                linkColor = urlLinkColor,
                onUrlClick = { url -> UrlText.openInBrowser(context, url) },
            )
        } else {
            null
        }
    }

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .cardPressScale(interactionSource),
        shape = shape,
        overlayBrush = overlayBrush,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .combinedClickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(selectionOverlay),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 148.dp, max = 280.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (note.pinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = stringResource(R.string.note_pin),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(30.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectionMode,
                        enter = fadeIn(tween(160)) + scaleIn(
                            initialScale = 0.70f,
                            animationSpec = tween(180, easing = GlassMotion.EaseOut),
                        ),
                        exit = fadeOut(tween(120)) + scaleOut(
                            targetScale = 0.70f,
                            animationSpec = tween(120, easing = GlassMotion.EaseInOut),
                        ),
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { onToggleSelection() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier.size(30.dp),
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = !selectionMode,
                        enter = fadeIn(tween(160)) + scaleIn(
                            initialScale = 0.70f,
                            animationSpec = tween(180, easing = GlassMotion.EaseOut),
                        ),
                        exit = fadeOut(tween(120)) + scaleOut(
                            targetScale = 0.70f,
                            animationSpec = tween(120, easing = GlassMotion.EaseInOut),
                        ),
                    ) {
                    Box {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .pressScale(moreInteractionSource, pressedScale = 0.90f)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = moreInteractionSource,
                                indication = null,
                                enabled = enabled,
                                role = Role.Button,
                                onClick = { menuExpanded = true },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreHoriz,
                            contentDescription = stringResource(R.string.cd_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(
                                        if (note.pinned) R.string.note_unpin else R.string.note_pin,
                                    ),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.PushPin,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onTogglePin()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(
                                        if (note.isPinnedBottom) {
                                            R.string.note_unpin_bottom
                                        } else {
                                            R.string.note_pin_bottom
                                        },
                                    ),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDownward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onTogglePinBottom()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(
                                        if (note.isEncrypted) {
                                            R.string.note_remove_encryption
                                        } else {
                                            R.string.note_encrypt
                                        },
                                    ),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (note.isEncrypted) {
                                        Icons.Outlined.LockOpen
                                    } else {
                                        Icons.Outlined.Lock
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                if (note.isEncrypted) {
                                    onRemoveEncryption()
                                } else {
                                    onEncrypt()
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.action_delete),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
                }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (note.isEncrypted) {
                    AnnotatedString(encryptedTitle)
                } else {
                    titleText ?: AnnotatedString(note.title.ifBlank { untitledLabel })
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (note.isEncrypted) {
                    MaterialTheme.colorScheme.onSurface
                } else if (note.title.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.isEncrypted || note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = if (note.isEncrypted) {
                        AnnotatedString(encryptedContent)
                    } else {
                        contentText ?: AnnotatedString(note.content.trim())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (note.isEncrypted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = stringResource(R.string.note_empty_content_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            Spacer(modifier = Modifier.height(13.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AmbienceBadge(
                        color = palette.dot,
                        label = stringResource(note.ambienceColor.labelRes()),
                    )
                    if (hasTags) {
                        MiniTag(
                            text = tagLabel,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    Text(
                        text = if (note.isEncrypted) {
                            encryptedWordCount
                        } else {
                            pluralStringResource(
                                R.plurals.word_count,
                                wordCount,
                                wordCount,
                            )
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.1.sp,
                            fontSize = 10.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = if (note.isEncrypted) encryptedTime else timeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.15.sp,
                            fontSize = 10.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (note.isEncrypted) encryptedAbsoluteTime else absoluteTimeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.05.sp,
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbienceBadge(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun MiniTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Label,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            modifier = Modifier.size(10.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier.widthIn(max = 300.dp),
            contentPadding = PaddingValues(28.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.loading_notes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomeEmptyState(
    title: String,
    body: String,
    actionLabel: String,
    onAction: (TransitionOrigin?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionInteraction = remember { MutableInteractionSource() }
    var actionBounds by remember { mutableStateOf<Rect?>(null) }
    Box(
        modifier = modifier.padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = {
                    onAction(actionBounds?.let { TransitionOrigin(it, 16.dp) })
                },
                modifier = Modifier
                    .pressScale(actionInteraction, pressedScale = 0.94f)
                    .onGloballyPositioned { coordinates ->
                        actionBounds = coordinates.boundsInWindow()
                    },
                interactionSource = actionInteraction,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun NewNoteFab(
    onClick: (TransitionOrigin?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(21.dp)
    val interactionSource = remember { MutableInteractionSource() }
    var sourceBounds by remember { mutableStateOf<Rect?>(null) }
    Row(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                sourceBounds = coordinates.boundsInWindow()
            }
            .pressScale(interactionSource, pressedScale = 0.90f)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    ),
                ),
            )
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    onClick(sourceBounds?.let { TransitionOrigin(it, 21.dp) })
                },
            )
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = stringResource(R.string.action_new_note),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
