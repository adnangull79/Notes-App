package com.example.notepad.Screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notepad.Checklist.*
import com.example.notepad.CreatNotes1.ColorPickerDialog
import com.example.notepad.CreatNotes1.FormatState
import com.example.notepad.CreatNotes1.FormattingToolbar
import com.example.notepad.CreatNotes1.FormattingFAB
import com.example.notepad.CreatNotes1.NoteOptionsBottomSheet
import com.example.notepad.FolderEntity
import com.example.notepad.NoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListNoteScreen(
    onNavigateBack: () -> Unit,
    noteViewModel: NoteViewModel = viewModel(),
    noteId: Int? = null
) {
    val scope = rememberCoroutineScope()
    val folders by noteViewModel.folders.collectAsState(initial = emptyList())
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // Checklist State Manager
    val checklistManager = remember(scope) {
        ChecklistStateManager(
            scope = scope,
            config = ChecklistConfig(
                showDragHandles = true,
                showAddButton = true,
                enableDragReorder = true,
                autoFocusNewItems = true
            )
        )
    }

    var title by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(noteId != null) }
    var isFavorite by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<FolderEntity?>(null) }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }

    // Formatting state
    var formatState by remember { mutableStateOf(FormatState()) }

    // Bottom bar visibility
    var showBottomBar by remember { mutableStateOf(true) }

    // Background color
    val defaultNoteBgColor = MaterialTheme.colorScheme.background
    var noteBackgroundColor by remember { mutableStateOf(defaultNoteBgColor) }

    val textRenderColor = MaterialTheme.colorScheme.onSurface
    val bottomBarIconColor = MaterialTheme.colorScheme.onSurface

    val dragDropState = remember { DragDropState() }
    val itemHeightPx = with(LocalDensity.current) { 80.dp.toPx() }

    // Auto-save functionality with proper order tracking
    LaunchedEffect(title, checklistManager.items.toList()) {
        if (!isLoading && noteId != null && title.isNotBlank()) {
            delay(1000)
            val content = checklistManager.itemsToString()
            val folderId = selectedFolder?.id ?: folders.firstOrNull()?.id ?: 1
            noteViewModel.updateNote(
                id = noteId,
                folderId = folderId,
                title = title,
                content = content
            )
        }
    }

    // Load existing note
    LaunchedEffect(noteId) {
        if (noteId != null) {
            isLoading = true
            scope.launch {
                val existing = noteViewModel.getNoteById(noteId)
                existing?.let { n ->
                    title = n.title
                    isFavorite = n.isFavorite
                    selectedFolder = folders.find { it.id == n.folderId }
                    checklistManager.parseAndLoadItems(n.content ?: "")
                }
                isLoading = false
            }
        }
    }

    // Focus title for new notes
    LaunchedEffect(Unit) {
        if (noteId == null) {
            delay(150)
            titleFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Scaffold(
        containerColor = noteBackgroundColor,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = if (noteId != null) "Edit Checklist" else "New Checklist",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (noteId == null && title.isNotBlank()) {
                            val content = checklistManager.itemsToString()
                            val folderId = selectedFolder?.id ?: folders.firstOrNull()?.id ?: 1
                            noteViewModel.createNote(
                                folderId = folderId,
                                title = title,
                                content = content,
                                noteType = com.example.notepad.NoteType.LIST  // ✅ ADD THIS LINE
                            )
                        }
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {

                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Use the shared FormattingToolbar component (NO parameters for checklist-specific functions)
            FormattingToolbar(
                showBottomBar = showBottomBar,
                onShowBottomBarChange = { showBottomBar = it },
                contentText = androidx.compose.ui.text.input.TextFieldValue(""), // Dummy for checklist
                onContentChange = {}, // Not used in checklist
                formatState = formatState,
                onFormatStateChange = { formatState = it },
                onApplyFormatting = { _, _, _ -> }, // Not used in checklist
                onPushHistory = {}, // Handled by ChecklistStateManager
                onUndo = { checklistManager.undo() },
                onRedo = { checklistManager.redo() },
                canUndo = checklistManager.canUndo(),
                canRedo = checklistManager.canRedo(),
                onShowColorPicker = { showColorPicker = true },
                textColor = textRenderColor,
                bottomBarIconColor = bottomBarIconColor,
                onAddDrawing = {} // Not used in checklist
            )
        },
        content = { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Title") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(titleFocusRequester),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(
                                items = checklistManager.items,
                                key = { _, item -> item.id }
                            ) { index, item ->
                                ChecklistItemRow(
                                    item = item,
                                    index = index,
                                    dragDropState = dragDropState,
                                    itemHeight = itemHeightPx,
                                    itemCount = checklistManager.items.size,
                                    focusRequester = checklistManager.getFocusRequester(item.id),
                                    formatState = formatState,
                                    textRenderColor = textRenderColor,
                                    isFocused = checklistManager.focusedItemId == item.id,
                                    onFocusChanged = { focused ->
                                        checklistManager.setFocusedItem(if (focused) item.id else null)
                                    },
                                    onCheckedChange = { checked ->
                                        checklistManager.toggleItemChecked(item.id)
                                    },
                                    onTextChange = { newText ->
                                        if (newText.contains("\n")) {
                                            val cleanText = newText.replace("\n", "").trim()
                                            checklistManager.updateItemText(item.id, cleanText, formatState)

                                            // Add new item after this one
                                            checklistManager.addItem(
                                                text = "",
                                                formatState = formatState,
                                                insertAfterIndex = index,
                                                autoFocus = true
                                            )

                                            scope.launch {
                                                delay(50)
                                                listState.animateScrollToItem(
                                                    index = (index + 1).coerceAtMost(checklistManager.items.size - 1)
                                                )
                                            }
                                        } else {
                                            checklistManager.updateItemText(item.id, newText, formatState)
                                        }
                                    },
                                    onDelete = {
                                        checklistManager.deleteItem(item.id)
                                    },
                                    onReorder = { fromIndex, toIndex ->
                                        checklistManager.reorderItems(fromIndex, toIndex)
                                    },
                                    onFormatChange = { updatedItem ->
                                        checklistManager.updateItemFormatting(item.id, updatedItem)
                                    }
                                )
                            }

                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Spacer(modifier = Modifier.width(32.dp))
                                    Spacer(modifier = Modifier.width(4.dp))

                                    TextButton(
                                        onClick = {
                                            checklistManager.addItem(
                                                text = "",
                                                formatState = formatState,
                                                insertAfterIndex = null,
                                                autoFocus = true
                                            )

                                            scope.launch {
                                                delay(100)
                                                listState.animateScrollToItem(checklistManager.items.size - 1)
                                            }
                                        },
                                        modifier = Modifier.wrapContentWidth(Alignment.Start)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add item",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("List item")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Floating Action Button (when toolbar is hidden)
                FormattingFAB(
                    showBottomBar = showBottomBar,
                    onShowBottomBar = { showBottomBar = true }
                )
            }
        }
    )

    if (showFolderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFolderSheet = false }
        ) {
            Text(
                "Folder selection coming soon...",
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (showAddSheet) {
        val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = addSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Add to Checklist",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))

                ListItem(
                    headlineContent = { Text("Image") },
                    supportingContent = { Text("Add photo from gallery (Coming soon)") },
                    leadingContent = { Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showAddSheet = false }
                )
                ListItem(
                    headlineContent = { Text("Voice Recording") },
                    supportingContent = { Text("Record audio note (Coming soon)") },
                    leadingContent = { Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showAddSheet = false }
                )
                ListItem(
                    headlineContent = { Text("Camera") },
                    supportingContent = { Text("Take photo with camera (Coming soon)") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showAddSheet = false }
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
// More options bottom sheet
    NoteOptionsBottomSheet(
        showSheet = showMoreMenu,
        isFavorite = isFavorite,
        onDismiss = { showMoreMenu = false },
        onShare = { /* TODO: Implement share */ },
        onPin = { /* TODO: Implement pin */ },
        onLabels = { showFolderSheet = true },
        onToggleFavorite = { isFavorite = !isFavorite },
        onDelete = {
            if (noteId != null) {
                noteViewModel.moveToTrash(noteId)
                onNavigateBack()
            }
        }
    )
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = noteBackgroundColor,
            onColorSelected = {
                noteBackgroundColor = it
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
            defaultColor = defaultNoteBgColor
        )
    }
}