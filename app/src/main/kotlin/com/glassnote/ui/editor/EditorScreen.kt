package com.glassnote.ui.editor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.glassnote.R
import com.glassnote.domain.model.NoteBlock
import com.glassnote.ui.components.GlassSurface
import com.glassnote.util.FileUtils
import com.glassnote.util.LocationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private data class FocusTarget(
    val blockId: String,
    val offset: Int,
)

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val textValues = remember { mutableStateMapOf<String, TextFieldValue>() }
    var activeBlockId by remember { mutableStateOf<String?>(null) }
    var pendingFocus by remember { mutableStateOf<FocusTarget?>(null) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }
    var attaching by remember { mutableStateOf(false) }
    var titleValue by remember { mutableStateOf(TextFieldValue("")) }
    var isEditMode by rememberSaveable { mutableStateOf(false) }

    val wordCount by remember(uiState.blocks) {
        derivedStateOf {
            uiState.blocks
                .filterIsInstance<NoteBlock.TextBlock>()
                .sumOf { block -> block.content.count { !it.isWhitespace() } }
        }
    }

    LaunchedEffect(uiState.title) {
        if (titleValue.text != uiState.title) {
            titleValue = TextFieldValue(
                text = uiState.title,
                selection = TextRange(uiState.title.length),
            )
        }
    }

    LaunchedEffect(uiState.blocks) {
        uiState.blocks.forEach { block ->
            if (block is NoteBlock.TextBlock) {
                val existing = textValues[block.id]
                if (existing == null) {
                    textValues[block.id] = TextFieldValue(
                        text = block.content,
                        selection = TextRange(block.content.length),
                    )
                } else if (existing.text != block.content) {
                    textValues[block.id] = existing.copy(
                        text = block.content,
                        selection = TextRange(block.content.length),
                    )
                }
            }
        }
        if (activeBlockId == null) {
            activeBlockId = uiState.blocks
                .filterIsInstance<NoteBlock.TextBlock>()
                .firstOrNull()
                ?.id
        }
    }

    LaunchedEffect(pendingFocus, isEditMode) {
        val target = pendingFocus ?: return@LaunchedEffect
        if (!isEditMode) return@LaunchedEffect
        delay(150)
        val block = uiState.blocks.firstOrNull { it.id == target.blockId }
            as? NoteBlock.TextBlock ?: return@LaunchedEffect
        val safeOffset = target.offset.coerceIn(0, block.content.length)
        textValues[target.blockId] = TextFieldValue(
            text = block.content,
            selection = TextRange(safeOffset),
        )
        focusRequesters[target.blockId]?.requestFocus()
        // Never call animateScrollToItem here: that was the source of the
        // read -> edit jump. The list keeps the user's current scroll offset.
        pendingFocus = null
    }

    val insertBlock: (NoteBlock) -> Unit = { block ->
        val anchorId = activeBlockId
            ?: uiState.blocks.filterIsInstance<NoteBlock.TextBlock>().lastOrNull()?.id
        val newTextId = viewModel.insertBlockAfter(anchorId, block)
        activeBlockId = newTextId
        pendingFocus = FocusTarget(newTextId, 0)
    }

    val showError: (Int) -> Unit = { messageRes ->
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            attaching = true
            val copied = FileUtils.copyUriToInternalStorage(context, uri)
            attaching = false
            if (copied == null) {
                showError(R.string.editor_attachment_failed)
            } else {
                insertBlock(NoteBlock.ImageBlock(localPath = copied))
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            attaching = true
            val copied = FileUtils.copyFileToInternalStorage(context, uri)
            attaching = false
            if (copied == null) {
                showError(R.string.editor_attachment_failed)
            } else {
                insertBlock(
                    NoteBlock.FileBlock(
                        id = UUID.randomUUID().toString(),
                        localPath = copied.absolutePath,
                        fileName = copied.fileName,
                        fileSize = copied.fileSize,
                    ),
                )
            }
        }
    }

    val addLocation: () -> Unit = {
        scope.launch {
            attaching = true
            val location = LocationUtils.getCurrentLocation(context)
            attaching = false
            if (location == null) {
                showError(R.string.editor_location_failed)
            } else {
                insertBlock(
                    NoteBlock.LocationBlock(
                        id = UUID.randomUUID().toString(),
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = location.address,
                    ),
                )
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            addLocation()
        } else {
            showError(R.string.editor_location_permission_denied)
        }
    }

    val finishAndBack: () -> Unit = {
        scope.launch {
            viewModel.flushPendingSave()
            onBack()
        }
    }

    val finishEdit: () -> Unit = {
        isEditMode = false
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        scope.launch { viewModel.flushPendingSave() }
    }

    val enterEditMode: () -> Unit = {
        val anchorId = activeBlockId
            ?: uiState.blocks.filterIsInstance<NoteBlock.TextBlock>().firstOrNull()?.id
        isEditMode = true
        if (anchorId != null) {
            activeBlockId = anchorId
            val previousValue = textValues[anchorId]
            val offset = previousValue?.selection?.start
                ?: previousValue?.text?.length
                ?: 0
            pendingFocus = FocusTarget(anchorId, offset)
        }
    }

    val handleBack: () -> Unit = {
        if (isEditMode) finishEdit() else finishAndBack()
    }

    BackHandler(onBack = handleBack)

    DisposableEffect(Unit) {
        onDispose { viewModel.flushInBackground() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        EditorTopBar(
            isEditMode = isEditMode,
            saveStatus = uiState.saveStatus,
            pinned = uiState.pinned,
            onTogglePin = viewModel::togglePinned,
            onToggleEdit = {
                if (isEditMode) finishEdit() else enterEditMode()
            },
            onBack = handleBack,
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Column
        }

        if (uiState.loadFailed) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.editor_load_error),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 10.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "title") {
                if (isEditMode) {
                    BasicTextField(
                        value = titleValue,
                        onValueChange = { updated ->
                            titleValue = updated
                            viewModel.onTitleChange(updated.text)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Box {
                                if (titleValue.text.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.editor_placeholder_title),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                }
                                inner()
                            }
                        },
                    )
                } else {
                    Text(
                        text = titleValue.text.ifBlank {
                            stringResource(R.string.editor_placeholder_title)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = if (titleValue.text.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }

            items(
                items = uiState.blocks,
                key = { block -> block.id },
            ) { block ->
                val blockFocusRequester = remember(block.id) { FocusRequester() }
                DisposableEffect(block.id) {
                    focusRequesters[block.id] = blockFocusRequester
                    onDispose { focusRequesters.remove(block.id) }
                }

                when (block) {
                    is NoteBlock.TextBlock -> {
                        if (isEditMode) {
                            TextBlockField(
                                block = block,
                                textValue = textValues[block.id] ?: TextFieldValue(
                                    text = block.content,
                                    selection = TextRange(block.content.length),
                                ),
                                focusRequester = blockFocusRequester,
                                onTextValueChange = { updated ->
                                    textValues[block.id] = updated
                                    activeBlockId = block.id
                                    viewModel.onBlockTextChange(block.id, updated.text)
                                },
                                onFocused = { activeBlockId = block.id },
                                onBackspaceAtStart = {
                                    val outcome = viewModel.mergeWithPrevious(block.id)
                                    if (outcome == null) {
                                        false
                                    } else {
                                        activeBlockId = outcome.focusBlockId
                                        pendingFocus = FocusTarget(
                                            blockId = outcome.focusBlockId,
                                            offset = outcome.cursorOffset,
                                        )
                                        true
                                    }
                                },
                            )
                        } else {
                            ReadOnlyTextBlock(
                                block = block,
                                onClick = { offset ->
                                    // Record the exact clicked block and character
                                    // offset before switching modes.
                                    activeBlockId = block.id
                                    pendingFocus = FocusTarget(block.id, offset)
                                    isEditMode = true
                                },
                            )
                        }
                    }

                    is NoteBlock.ImageBlock -> {
                        AsyncImage(
                            model = File(block.localPath),
                            contentDescription = stringResource(R.string.editor_image_content_desc),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 380.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    previewImagePath = block.localPath
                                },
                        )
                    }

                    is NoteBlock.FileBlock -> {
                        AttachmentCard(
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = block.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = block.fileSize,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }

                    is NoteBlock.LocationBlock -> {
                        AttachmentCard(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                            },
                            modifier = Modifier.clickable {
                                openLocation(
                                    context = context,
                                    latitude = block.latitude,
                                    longitude = block.longitude,
                                    address = block.address,
                                )
                            },
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = block.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = String.format(
                                        java.util.Locale.US,
                                        "%.5f, %.5f",
                                        block.latitude,
                                        block.longitude,
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isEditMode) {
            AttachmentBar(
                attaching = attaching,
                onPickImage = { imagePicker.launch("image/*") },
                onPickFile = { filePicker.launch("*/*") },
                onPickLocation = {
                    if (LocationUtils.hasLocationPermission(context)) {
                        addLocation()
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
            )
        }

        WordCountFooter(wordCount = wordCount)
    }

    previewImagePath?.let { path ->
        Dialog(
            onDismissRequest = { previewImagePath = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { previewImagePath = null },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = File(path),
                    contentDescription = stringResource(R.string.editor_image_content_desc),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun WordCountFooter(wordCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.editor_total_words, wordCount),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun ReadOnlyTextBlock(
    block: NoteBlock.TextBlock,
    onClick: (Int) -> Unit,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val placeholder = stringResource(R.string.editor_block_placeholder)
    val displayText = block.content.ifEmpty { placeholder }

    Text(
        text = displayText,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(block.id, block.content) {
                detectTapGestures { position ->
                    val result = layout ?: return@detectTapGestures
                    val offset = result.getOffsetForPosition(position)
                        .coerceIn(0, block.content.length)
                    onClick(offset)
                }
            },
        onTextLayout = { layout = it },
        style = TextStyle(
            color = if (block.content.isEmpty()) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
            },
            fontSize = 17.sp,
            lineHeight = 28.sp,
        ),
    )
}

@Composable
private fun TextBlockField(
    block: NoteBlock.TextBlock,
    textValue: TextFieldValue,
    focusRequester: FocusRequester,
    onTextValueChange: (TextFieldValue) -> Unit,
    onFocused: () -> Unit,
    onBackspaceAtStart: () -> Boolean,
) {
    BasicTextField(
        value = textValue,
        onValueChange = onTextValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.Backspace &&
                    textValue.selection.collapsed &&
                    textValue.selection.start == 0
                ) {
                    onBackspaceAtStart()
                } else {
                    false
                }
            },
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
            fontSize = 17.sp,
            lineHeight = 28.sp,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (block.content.isEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_block_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f),
                        fontSize = 17.sp,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun AttachmentCard(
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            content()
        }
    }
}

@Composable
private fun AttachmentBar(
    attaching: Boolean,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onPickLocation: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AttachmentAction(
                icon = Icons.Outlined.Image,
                label = stringResource(R.string.editor_attach_image),
                enabled = !attaching,
                onClick = onPickImage,
            )
            AttachmentAction(
                icon = Icons.Outlined.AttachFile,
                label = stringResource(R.string.editor_attach_file),
                enabled = !attaching,
                onClick = onPickFile,
            )
            AttachmentAction(
                icon = Icons.Outlined.LocationOn,
                label = stringResource(R.string.editor_attach_location),
                enabled = !attaching,
                onClick = onPickLocation,
            )
            if (attaching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

@Composable
private fun AttachmentAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EditorTopBar(
    isEditMode: Boolean,
    saveStatus: EditorSaveStatus,
    pinned: Boolean,
    onTogglePin: () -> Unit,
    onToggleEdit: () -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.editor_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            val status = stringResource(
                when (saveStatus) {
                    EditorSaveStatus.SAVED -> R.string.editor_save_saved
                    EditorSaveStatus.PENDING -> R.string.editor_save_pending
                    EditorSaveStatus.SAVING -> R.string.editor_save_saving
                    EditorSaveStatus.FAILED -> R.string.editor_save_failed
                },
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        IconButton(onClick = onTogglePin) {
            Icon(
                imageVector = Icons.Outlined.PushPin,
                contentDescription = stringResource(
                    if (pinned) R.string.note_unpin else R.string.note_pin,
                ),
                tint = if (pinned) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        TextButton(onClick = onToggleEdit) {
            Text(
                stringResource(
                    if (isEditMode) R.string.editor_action_done
                    else R.string.editor_action_edit,
                ),
            )
        }
    }
}

private fun openLocation(
    context: android.content.Context,
    latitude: Double,
    longitude: Double,
    address: String,
) {
    val encoded = Uri.encode(address)
    val uri = "geo:$latitude,$longitude?q=$latitude,$longitude($encoded)".toUri()
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        Toast.makeText(
            context,
            context.getString(R.string.editor_location_open_failed),
            Toast.LENGTH_SHORT,
        ).show()
    }
}
