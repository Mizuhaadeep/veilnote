package com.glassnote.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.glassnote.R
import com.glassnote.domain.model.Tag
import com.glassnote.domain.model.TagWriteResult
import com.glassnote.ui.theme.GlassMotion
import com.glassnote.ui.theme.isDarkGlassTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagerSheet(
    tags: List<Tag>,
    onCreate: suspend (String) -> TagWriteResult,
    onRename: suspend (Long, String) -> TagWriteResult,
    onDelete: suspend (Long) -> Boolean,
    onDismiss: () -> Unit,
    onDismissStarted: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    var renamingTag by remember { mutableStateOf<Tag?>(null) }
    var deletingTag by remember { mutableStateOf<Tag?>(null) }
    var actionError by remember { mutableStateOf<Int?>(null) }
    var dismissing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val dismissSheet: () -> Unit = {
        if (!dismissing) {
            dismissing = true
            onDismissStarted()
            val hideJob = scope.launch { sheetState.hide() }
            hideJob.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    onDismiss()
                }
            }
        }
    }

    SheetDismissEffect(
        sheetState = sheetState,
        onDismissStarted = onDismissStarted,
    )

    ModalBottomSheet(
        onDismissRequest = dismissSheet,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.985f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        AnimatedSheetContent {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                contentPadding = PaddingValues(horizontal = 22.dp),
            ) {
                item { SheetDragHandle() }
                item {
                    SheetHeader(
                        title = stringResource(R.string.tag_sheet_title),
                        subtitle = stringResource(R.string.tag_sheet_subtitle),
                        onClose = dismissSheet,
                    )
                }
                item {
                    TagCreationField(
                        onCreate = onCreate,
                        onCreated = {
                            actionError = null
                        },
                    )
                }
                if (actionError != null) {
                    item {
                        Text(
                            text = stringResource(actionError!!),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                if (tags.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.tag_empty_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 20.dp),
                        )
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = stringResource(R.string.tag_existing),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(
                        items = tags,
                        key = Tag::id,
                        contentType = { "tag-manager-row" },
                    ) { tag ->
                        TagManagerRow(
                            tag = tag,
                            onRename = { renamingTag = tag },
                            onDelete = { deletingTag = tag },
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.13f),
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
        }
    }

    renamingTag?.let { tag ->
        RenameTagDialog(
            tag = tag,
            onDismiss = { renamingTag = null },
            onRename = onRename,
        )
    }

    deletingTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { deletingTag = null },
            title = { Text(stringResource(R.string.tag_delete_title)) },
            text = {
                Text(stringResource(R.string.tag_delete_message, tag.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (onDelete(tag.id)) {
                                deletingTag = null
                                actionError = null
                            } else {
                                actionError = R.string.tag_generic_error
                                deletingTag = null
                            }
                        }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTag = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = sheetDialogColor(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPickerSheet(
    tags: List<Tag>,
    selectedTagIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onCreate: suspend (String) -> TagWriteResult,
    onDismiss: () -> Unit,
    onDismissStarted: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    var dismissing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val dismissSheet: () -> Unit = {
        if (!dismissing) {
            dismissing = true
            onDismissStarted()
            val hideJob = scope.launch { sheetState.hide() }
            hideJob.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    onDismiss()
                }
            }
        }
    }

    SheetDismissEffect(
        sheetState = sheetState,
        onDismissStarted = onDismissStarted,
    )

    ModalBottomSheet(
        onDismissRequest = dismissSheet,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.985f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        AnimatedSheetContent {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                contentPadding = PaddingValues(horizontal = 22.dp),
            ) {
                item { SheetDragHandle() }
                item {
                    SheetHeader(
                        title = stringResource(R.string.tag_picker_title),
                        subtitle = stringResource(R.string.tag_picker_subtitle),
                        onClose = dismissSheet,
                    )
                }
                item {
                    TagCreationField(
                        onCreate = onCreate,
                        onCreated = {},
                    )
                }
                if (tags.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.tag_none),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 20.dp),
                        )
                    }
                } else {
                    item { Spacer(modifier = Modifier.height(18.dp)) }
                    items(
                        items = tags,
                        key = Tag::id,
                        contentType = { "tag-picker-row" },
                    ) { tag ->
                        TagSelectionRow(
                            tag = tag,
                            selected = tag.id in selectedTagIds,
                            onClick = { onToggle(tag.id) },
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
        }
    }
}

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun SheetHeader(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.action_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TagCreationField(
    onCreate: suspend (String) -> TagWriteResult,
    onCreated: (Tag) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var name by rememberSaveable { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    val submit = {
        if (!isSubmitting) {
            val normalized = name.trim()
            if (normalized.isEmpty()) {
                errorRes = R.string.tag_blank_error
            } else {
                scope.launch {
                    isSubmitting = true
                    val result = onCreate(normalized)
                    isSubmitting = false
                    when (result) {
                        is TagWriteResult.Success -> {
                            name = ""
                            errorRes = null
                            keyboard?.hide()
                            onCreated(result.tag)
                        }
                        TagWriteResult.BlankName -> errorRes = R.string.tag_blank_error
                        TagWriteResult.DuplicateName -> errorRes = R.string.tag_duplicate_error
                        TagWriteResult.NotFound -> errorRes = R.string.tag_generic_error
                    }
                }
            }
        }
    }

    OutlinedTextField(
        value = name,
        onValueChange = {
            name = it
            if (errorRes != null) errorRes = null
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clipToBounds(),
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.tag_add_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Label,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            IconButton(onClick = submit, enabled = !isSubmitting) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.tag_add_action),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        isError = errorRes != null,
        supportingText = if (errorRes != null) {
            {
                Text(
                    text = stringResource(errorRes!!),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            null
        },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
    )
}

@Composable
private fun TagManagerRow(
    tag: Tag,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Label,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = tag.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRename) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.cd_edit_tag),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(R.string.cd_delete_tag),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun TagSelectionRow(
    tag: Tag,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(GlassMotion.QUICK_MS),
        label = "tagSelectionBackground",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .selectable(
                selected = selected,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Label,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = tag.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(GlassMotion.QUICK_MS)) + expandVertically(),
            exit = fadeOut(tween(GlassMotion.QUICK_MS)) + shrinkVertically(),
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun RenameTagDialog(
    tag: Tag,
    onDismiss: () -> Unit,
    onRename: suspend (Long, String) -> TagWriteResult,
) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(tag.id) { mutableStateOf(tag.name) }
    var isSaving by remember { mutableStateOf(false) }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    val submit = {
        if (!isSaving) {
            val normalized = name.trim()
            if (normalized.isEmpty()) {
                errorRes = R.string.tag_blank_error
            } else {
                scope.launch {
                    isSaving = true
                    when (onRename(tag.id, normalized)) {
                        is TagWriteResult.Success -> onDismiss()
                        TagWriteResult.BlankName -> errorRes = R.string.tag_blank_error
                        TagWriteResult.DuplicateName -> errorRes = R.string.tag_duplicate_error
                        TagWriteResult.NotFound -> errorRes = R.string.tag_generic_error
                    }
                    isSaving = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.action_rename)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorRes = null
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clipToBounds(),
                isError = errorRes != null,
                supportingText = if (errorRes != null) {
                    {
                        Text(
                            text = stringResource(errorRes!!),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    null
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = !isSaving) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(26.dp),
        containerColor = sheetDialogColor(),
    )
}

@Composable
private fun sheetDialogColor(): Color {    val darkTheme = isDarkGlassTheme()
    return if (darkTheme) Color(0xFF1B1E25) else Color(0xFFFAF7F1)
}
