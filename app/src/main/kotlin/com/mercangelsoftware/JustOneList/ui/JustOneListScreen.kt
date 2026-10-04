package com.mercangelsoftware.JustOneList.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mercangelsoftware.JustOneList.ListViewModel
import com.mercangelsoftware.JustOneList.R
import com.mercangelsoftware.JustOneList.data.ListItemEntity
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JustOneListScreen(viewModel: ListViewModel) {
    val context = LocalContext.current

    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val view = LocalView.current
    DisposableEffect(view, keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var showClearAllDialog by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Deletes items and offers an Undo snackbar that restores them
    fun deleteWithUndo(items: List<ListItemEntity>) {
        if (items.isEmpty()) return
        viewModel.deleteItems(items)
        val message = context.resources.getQuantityString(
            R.plurals.items_deleted, items.size, items.size
        )
        scope.launch {
            // Replace any pending undo snackbar instead of queueing behind it
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = context.getString(R.string.button_undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreItems(items)
        }
    }

    // Drag-to-reorder: local shadow list so items move instantly during drag
    val localItems = remember { mutableStateListOf<ListItemEntity>() }
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Only reorder within the unchecked section
        if (from.index < localItems.size && to.index < localItems.size) {
            localItems.add(to.index, localItems.removeAt(from.index))
        }
    }

    // Sync local list from DB whenever it changes (skipped while actively dragging)
    LaunchedEffect(uiState.uncheckedItems) {
        if (!reorderableLazyListState.isAnyItemDragging) {
            localItems.clear()
            localItems.addAll(uiState.uncheckedItems)
        }
    }

    // Persist the new order to Room when the drag gesture is released
    LaunchedEffect(reorderableLazyListState) {
        snapshotFlow { reorderableLazyListState.isAnyItemDragging }
            .drop(1)               // skip initial false emission
            .filter { !it }        // only fire when drag ends
            .collect { viewModel.reorderItems(localItems.map { it.id }) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_export)) },
                            onClick = {
                                showMenu = false
                                val allItems = uiState.uncheckedItems + uiState.checkedItems
                                val text = allItems.joinToString("\n") { it.text }
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                clipboard.setPrimaryClip(ClipData.newPlainText("Just One List", text))
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.export_copied)
                                    )
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_import)) },
                            onClick = {
                                showMenu = false
                                showImportDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_keep_screen_on)) },
                            trailingIcon = {
                                if (keepScreenOn) Icon(Icons.Default.Check, contentDescription = null)
                            },
                            onClick = { viewModel.setKeepScreenOn(!keepScreenOn) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_clear_all)) },
                            enabled = uiState.uncheckedItems.isNotEmpty() ||
                                uiState.checkedItems.isNotEmpty(),
                            onClick = {
                                showMenu = false
                                showClearAllDialog = true
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.hint_add_item)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (inputText.isNotBlank()) {
                            viewModel.addItem(inputText)
                            inputText = ""
                        }
                    })
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.addItem(inputText)
                        inputText = ""
                    },
                    enabled = inputText.isNotBlank()
                ) {
                    Text(stringResource(R.string.button_add))
                }
            }

            // While dragging, render the local shadow list so moves appear instantly.
            // Otherwise render straight from the DB-backed state — this avoids a frame
            // where a just-checked item lingers in localItems while it also appears in
            // checkedItems, which would produce a duplicate LazyColumn key and crash.
            val displayedUnchecked =
                if (reorderableLazyListState.isAnyItemDragging) localItems
                else uiState.uncheckedItems

            @Composable
            fun Item(item: ListItemEntity, dragHandleModifier: Modifier?) {
                SwipeToDelete(onDelete = { deleteWithUndo(listOf(item)) }) {
                    ItemRow(
                        item = item,
                        isEditing = editingId == item.id,
                        onToggle = { viewModel.toggleItem(item.id, item.checked) },
                        onStartEdit = { editingId = item.id },
                        onEdit = { newText ->
                            viewModel.editItem(item.id, newText)
                            if (editingId == item.id) editingId = null
                        },
                        onDelete = { deleteWithUndo(listOf(item)) },
                        dragHandleModifier = dragHandleModifier
                    )
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f)
            ) {
                // Unchecked items — draggable
                items(displayedUnchecked, key = { it.id }) { item ->
                    ReorderableItem(reorderableLazyListState, key = item.id) { isDragging ->
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 4.dp else 0.dp,
                            label = "drag elevation"
                        )
                        Surface(shadowElevation = elevation) {
                            Item(item, dragHandleModifier = Modifier.draggableHandle())
                        }
                    }
                }
                // Checked items — static, below the divider
                if (uiState.checkedItems.isNotEmpty()) {
                    item(key = "divider") { SectionDivider() }
                    items(uiState.checkedItems, key = { it.id }) { item ->
                        Item(item, dragHandleModifier = null)
                    }
                }
            }

            TextButton(
                onClick = { deleteWithUndo(uiState.checkedItems) },
                enabled = uiState.checkedItems.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.button_clear_done))
            }
        }
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onImport = { text ->
                viewModel.importItems(text)
                showImportDialog = false
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(stringResource(R.string.clear_confirm_title)) },
            text = { Text(stringResource(R.string.clear_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteWithUndo(uiState.uncheckedItems + uiState.checkedItems)
                        showClearAllDialog = false
                    }
                ) {
                    Text(stringResource(R.string.button_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_import)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.import_hint)) },
                minLines = 4,
                maxLines = 10
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(text) },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(R.string.button_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

/** Swipe in either direction to delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val density = LocalDensity.current
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    // Plain remember (not saveable): if the item is restored via Undo it comes back
    // un-swiped instead of inheriting the dismissed state saved under its key.
    val state = remember {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            density = density,
            confirmValueChange = { value ->
                if (value != SwipeToDismissBoxValue.Settled) currentOnDelete()
                true
            },
            positionalThreshold = positionalThreshold
        )
    }
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val alignment =
                if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd
                else Alignment.CenterStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        Surface { content() }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemRow(
    item: ListItemEntity,
    isEditing: Boolean,
    onToggle: () -> Unit,
    onStartEdit: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    dragHandleModifier: Modifier? = null
) {
    val editLabel = stringResource(R.string.action_edit)
    val deleteLabel = stringResource(R.string.action_delete)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(deleteLabel) { onDelete(); true }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Handle sits outside the clickable area so tapping it doesn't toggle the item
        if (dragHandleModifier != null) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = stringResource(R.string.drag_to_reorder),
                modifier = dragHandleModifier.padding(horizontal = 4.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    enabled = !isEditing,
                    onClick = onToggle,
                    onLongClick = onStartEdit,
                    onLongClickLabel = editLabel
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.checked,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isEditing) {
                var editText by rememberSaveable(item.id) { mutableStateOf(item.text) }
                val focusRequester = remember { FocusRequester() }
                var hasFocused by remember { mutableStateOf(false) }
                BackHandler { onEdit(editText) }
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                hasFocused = true
                            } else if (hasFocused) {
                                // Commit when focus leaves the field (e.g. tapping elsewhere)
                                onEdit(editText)
                            }
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onDone = { onEdit(editText) })
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            } else {
                Text(
                    text = item.text,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.checked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SectionDivider() {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        HorizontalDivider()
        Text(
            text = stringResource(R.string.label_done),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}
