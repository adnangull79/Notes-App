package com.example.notepad.CreatNotes1

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notepad.CreatNotes.FormattingManager
import com.example.notepad.Drawing.*
import com.example.notepad.FolderEntity
import com.example.notepad.NoteType
import com.example.notepad.NoteViewModel
import com.example.notepad.UI_theme.CategoryKey
import com.example.notepad.UI_theme.categoryContainerColor
import com.example.notepad.UI_theme.categoryIconColor
import kotlinx.coroutines.launch

// --- Section Data Classes ---
sealed class NoteSection {
    abstract val id: Long

    data class CanvasSection(
        override val id: Long,
        val canvasState: CanvasState = CanvasState()
    ) : NoteSection()

    class TextSection(
        override val id: Long
    ) : NoteSection() {
        var content by mutableStateOf(TextFieldValue(""))
        var formatState by mutableStateOf(FormatState())
        var formatMap by mutableStateOf<FormatMap>(emptyList())
        var focusRequester = FocusRequester()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteScreen(
    onNavigateBack: () -> Unit,
    noteViewModel: NoteViewModel = viewModel(),
    noteId: Int? = null,
    noteType: NoteType = NoteType.TEXT
) {
    val titleFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val defaultNoteBgColor = MaterialTheme.colorScheme.background
    var noteBackgroundColor by remember { mutableStateOf(defaultNoteBgColor) }
    val textRenderColor = MaterialTheme.colorScheme.onSurface
    val bottomBarIconColor = MaterialTheme.colorScheme.onSurface

    var title by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<FolderEntity?>(null) }

    var showFolderSheet by remember { mutableStateOf(false) }
    var showCreateCategorySheet by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showBottomBar by remember { mutableStateOf(true) }

    var currentNoteType by remember { mutableStateOf(noteType) }
    var isContentFocused by remember { mutableStateOf(false) }
    val isEditMode = noteId != null
    var isLoading by remember { mutableStateOf(noteId != null) }
    var isFavorite by remember { mutableStateOf(false) }

    val isDarkTheme = isSystemInDarkTheme()

    val sections = remember {
        mutableStateListOf<NoteSection>(
            NoteSection.TextSection(id = System.currentTimeMillis())
        )
    }

    var activeCanvasId by remember { mutableStateOf<Long?>(null) }
    var currentTool by remember { mutableStateOf(DrawingTool.PEN) }
    var drawColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(8f) }

    var showToolOptions by remember { mutableStateOf(false) }
    var showDrawingColorPicker by remember { mutableStateOf(false) }
    var showCanvasOptions by remember { mutableStateOf(false) }
    var previousToolbarState by remember { mutableStateOf<String?>(null) }

    var recomposeTrigger by remember { mutableStateOf(0) }

    val isDrawingMode = activeCanvasId != null

    val activeCanvas = sections.firstOrNull {
        it is NoteSection.CanvasSection && it.id == activeCanvasId
    } as? NoteSection.CanvasSection

    var activeTextSectionId by remember {
        mutableStateOf<Long?>((sections.firstOrNull() as? NoteSection.TextSection)?.id)
    }

    val activeTextSection = sections.firstOrNull {
        it is NoteSection.TextSection && it.id == activeTextSectionId
    } as? NoteSection.TextSection

    val formattingManager = remember { FormattingManager(textRenderColor) }

    var checklistItems by remember { mutableStateOf(listOf<ChecklistItem>()) }

    val folders by noteViewModel.folders.collectAsState()
    val scope = rememberCoroutineScope()
    var pendingNewFolderName by remember { mutableStateOf<String?>(null) }

    val drawingManager = remember(activeCanvas) {
        activeCanvas?.let { DrawingManager(it.canvasState) }
    }

    fun getAllContentAsString(): String {
        val sb = StringBuilder()
        sections.forEach { section ->
            when (section) {
                is NoteSection.TextSection -> {
                    if (section.content.text.isNotBlank()) {
                        sb.append(section.content.text)
                        sb.append("\n")
                    }
                }
                is NoteSection.CanvasSection -> {
                    sb.append("[DRAWING]\n")
                }
            }
        }
        return sb.toString().trim()
    }

    val saveAndExit = {
        if (title.isNotBlank()) {
            val contentToSave = getAllContentAsString()
            val folderToUse = selectedFolder ?: folders.firstOrNull()

            if (folderToUse != null) {
                if (isEditMode && noteId != null) {
                    noteViewModel.updateNote(
                        id = noteId,
                        folderId = folderToUse.id,
                        title = title,
                        content = contentToSave
                    )
                } else {
                    noteViewModel.createNote(
                        folderId = folderToUse.id,
                        title = title,
                        content = contentToSave
                    )
                }
            }
        }
        onNavigateBack()
    }

    BackHandler {
        if (isDrawingMode) {
            activeCanvas?.canvasState?.hasDrawn = true
            activeCanvasId = null
            showToolOptions = false
            showDrawingColorPicker = false
            showCanvasOptions = false
        } else {
            saveAndExit()
        }
    }

    LaunchedEffect(noteId) {
        if (noteId != null) {
            scope.launch {
                val existingNote = noteViewModel.getNoteById(noteId)
                if (existingNote != null) {
                    title = existingNote.title
                    isFavorite = existingNote.isFavorite
                    val firstTextSection = sections.firstOrNull() as? NoteSection.TextSection
                    if (firstTextSection != null) {
                        val initialContent = TextFieldValue(existingNote.content ?: "")
                        firstTextSection.content = initialContent
                        formattingManager.initializeHistory(initialContent)
                    }
                }
                isLoading = false
            }
        } else {
            val firstTextSection = sections.firstOrNull() as? NoteSection.TextSection
            if (firstTextSection != null) {
                formattingManager.initializeHistory(firstTextSection.content)
            }
        }
    }

    LaunchedEffect(Unit) { noteViewModel.loadFolders() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        titleFocusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(folders, noteId) {
        if (noteId != null) {
            scope.launch {
                val existingNote = noteViewModel.getNoteById(noteId)
                existingNote?.let { n ->
                    selectedFolder = folders.find { it.id == n.folderId }
                }
            }
        }
    }

    LaunchedEffect(folders) {
        if (!isEditMode && selectedFolder == null && folders.isNotEmpty()) {
            selectedFolder = folders.first()
        }
        pendingNewFolderName?.let { name ->
            val match = folders.lastOrNull { it.name == name } ?: folders.find { it.name == name }
            if (match != null) {
                selectedFolder = match
                pendingNewFolderName = null
            }
        }
    }

    fun addDrawingCanvas() {
        val newCanvasId = System.currentTimeMillis()
        val newCanvas = NoteSection.CanvasSection(id = newCanvasId)
        sections.add(newCanvas)
        activeCanvasId = newCanvasId
        keyboardController?.hide()
    }

    fun finishDrawing() {
        activeCanvas?.canvasState?.hasDrawn = true
        val canvasIndex = sections.indexOfFirst { it.id == activeCanvasId }
        activeCanvasId = null
        showToolOptions = false
        showDrawingColorPicker = false
        showCanvasOptions = false

        if (canvasIndex != -1) {
            val nextIndex = canvasIndex + 1

            // Always create new text section below canvas
            val newTextSection = NoteSection.TextSection(id = System.currentTimeMillis())
            sections.add(nextIndex, newTextSection)
            activeTextSectionId = newTextSection.id

            // ✅ Auto-focus the new text field after a short delay
            scope.launch {
                kotlinx.coroutines.delay(100)
                try {
                    newTextSection.focusRequester.requestFocus()
                    keyboardController?.show()
                } catch (e: Exception) {
                    // Focus request might fail if composable not ready
                }
            }
        }
    }

    Scaffold(
        containerColor = noteBackgroundColor,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = if (isEditMode) "Edit Note" else "Create Note",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isDrawingMode) {
                            finishDrawing()
                        } else {
                            saveAndExit()
                        }
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    if (isDrawingMode) {
                        IconButton(onClick = { finishDrawing() }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Done Drawing",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = { Icon(Icons.Default.Share, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Pin") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = { Icon(Icons.Default.PushPin, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Folder") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = { Icon(Icons.Default.Folder, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Add a Label") },
                            onClick = {
                                showMoreMenu = false
                                showFolderSheet = true
                            },
                            leadingIcon = { Icon(Icons.Default.Label, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites") },
                            onClick = {
                                showMoreMenu = false
                                isFavorite = !isFavorite
                            },
                            leadingIcon = { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Print") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = { Icon(Icons.Default.Print, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (isDrawingMode && activeCanvas != null) {
                DrawingBottomBar(
                    currentTool = currentTool,
                    drawColor = drawColor,
                    strokeWidth = strokeWidth,
                    canvasBgColor = activeCanvas.canvasState.bgColor,
                    showGridLines = activeCanvas.canvasState.showGrid,
                    showToolOptions = showToolOptions,
                    showColorPicker = showDrawingColorPicker,
                    showCanvasOptions = showCanvasOptions,
                    canUndo = activeCanvas.canvasState.paths.isNotEmpty() || activeCanvas.canvasState.currentPath.isNotEmpty(),
                    canRedo = activeCanvas.canvasState.redoStack.isNotEmpty(),
                    previousToolbarState = previousToolbarState,
                    onToolChange = { tool ->
                        currentTool = tool
                        showToolOptions = true
                        showDrawingColorPicker = false
                        showCanvasOptions = false
                        previousToolbarState = null
                    },
                    onColorChange = { drawColor = it },
                    onStrokeWidthChange = { strokeWidth = it },
                    onCanvasBgChange = {
                        activeCanvas.canvasState.bgColor = it
                        recomposeTrigger++
                    },
                    onGridToggle = {
                        activeCanvas.canvasState.showGrid = it
                        recomposeTrigger++
                    },
                    onShowColorPicker = {
                        previousToolbarState = if (showToolOptions) "TOOL_OPTIONS" else null
                        showDrawingColorPicker = true
                        showToolOptions = false
                        showCanvasOptions = false
                    },
                    onShowCanvasOptions = {
                        previousToolbarState = if (showToolOptions) "TOOL_OPTIONS" else null
                        showCanvasOptions = true
                        showToolOptions = false
                        showDrawingColorPicker = false
                    },
                    onUndo = {
                        if (drawingManager?.undo() == true) {
                            recomposeTrigger++
                        }
                    },
                    onRedo = {
                        if (drawingManager?.redo() == true) {
                            recomposeTrigger++
                        }
                    },
                    onCloseSubPanel = {
                        showToolOptions = false
                        showDrawingColorPicker = false
                        showCanvasOptions = false
                        previousToolbarState = null
                    },
                    onBackToToolOptions = {
                        showToolOptions = true
                        showDrawingColorPicker = false
                        showCanvasOptions = false
                        previousToolbarState = null
                    }
                )
            } else {
                FormattingToolbar(
                    showBottomBar = showBottomBar,
                    onShowBottomBarChange = { showBottomBar = it },
                    contentText = activeTextSection?.content ?: TextFieldValue(""),
                    onContentChange = { newValue ->
                        activeTextSection?.let { section ->
                            val newSpans = formattingManager.handleTextChange(section.content, newValue)
                            formattingManager.updateFormatMap(newSpans)
                            section.content = newValue
                        }
                    },
                    formatState = activeTextSection?.formatState ?: FormatState(),
                    onFormatStateChange = {
                        activeTextSection?.formatState = it
                        formattingManager.updateFormatState(it)
                    },
                    onApplyFormatting = { start, end, style ->
                        formattingManager.applyFormatting(
                            activeTextSection?.content ?: TextFieldValue(""),
                            start, end, style
                        )
                    },
                    onPushHistory = {
                        activeTextSection?.let {
                            formattingManager.pushHistory(it.content, formattingManager.formatMap, formattingManager.formatState)
                        }
                    },
                    onUndo = {
                        formattingManager.undo()?.let { (value, _, _) ->
                            activeTextSection?.content = value
                        }
                    },
                    onRedo = {
                        formattingManager.redo()?.let { (value, _, _) ->
                            activeTextSection?.content = value
                        }
                    },
                    canUndo = formattingManager.canUndo(),
                    canRedo = formattingManager.canRedo(),
                    onShowColorPicker = { showColorPicker = true },
                    textColor = textRenderColor,
                    bottomBarIconColor = bottomBarIconColor,
                    onAddDrawing = { addDrawingCanvas() }
                )
            }
        },

        ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text("Title", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    },
                    enabled = !isDrawingMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-12).dp)
                        .focusRequester(titleFocusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = textRenderColor,
                        unfocusedTextColor = textRenderColor,
                        disabledTextColor = textRenderColor.copy(alpha = 0.6f)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                sections.forEach { section ->
                    when (section) {
                        is NoteSection.TextSection -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .focusRequester(section.focusRequester)
                            ) {
                                FormattedTextEditor(
                                    content = section.content,
                                    onContentChange = { newValue ->
                                        if (!isDrawingMode) {
                                            val newSpans = formattingManager.handleTextChange(section.content, newValue)
                                            formattingManager.updateFormatMap(newSpans)
                                            formattingManager.pushHistory(newValue, newSpans, formattingManager.formatState)
                                            section.content = newValue
                                        }
                                    },
                                    formatState = section.formatState,
                                    onFormatStateChange = {
                                        if (!isDrawingMode) {
                                            section.formatState = it
                                            formattingManager.updateFormatState(it)
                                        }
                                    },
                                    formatMap = section.formatMap,
                                    onFormatMapChange = { newMap ->
                                        if (!isDrawingMode) {
                                            section.formatMap = newMap
                                            formattingManager.updateFormatMap(newMap)
                                            formattingManager.pushHistory(section.content, newMap, formattingManager.formatState)
                                        }
                                    },
                                    textColor = textRenderColor,
                                    onFocusChanged = { focused ->
                                        if (focused && !isDrawingMode) {
                                            activeTextSectionId = section.id
                                            isContentFocused = true
                                        }
                                    },
                                    onCheckboxToggle = { toggledValue ->
                                        if (!isDrawingMode) {
                                            section.content = toggledValue
                                            formattingManager.pushHistory(toggledValue, formattingManager.formatMap, formattingManager.formatState)
                                        }
                                    }
                                )
                            }
                        }

                        is NoteSection.CanvasSection -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(500.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                DrawingCanvas(
                                    canvasState = section.canvasState,
                                    drawColor = drawColor,
                                    strokeWidth = strokeWidth,
                                    currentTool = currentTool,
                                    isDrawingMode = section.id == activeCanvasId,
                                    isCanvasLocked = section.canvasState.hasDrawn && section.id != activeCanvasId,
                                    recomposeTrigger = recomposeTrigger,
                                    onRecompose = { recomposeTrigger++ },
                                    onEnterDrawingMode = {
                                        if (!section.canvasState.hasDrawn || section.id == activeCanvasId) {
                                            activeCanvasId = section.id
                                            keyboardController?.hide()
                                        }
                                    }
                                )

                                if (section.canvasState.hasDrawn && section.id != activeCanvasId) {
                                    IconButton(
                                        onClick = {
                                            activeCanvasId = section.id
                                            keyboardController?.hide()
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .size(32.dp)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Canvas",
                                            tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // ✅ IMPROVED DELETE BUTTON - Merges text sections
                                    IconButton(
                                        onClick = {
                                            val canvasIndex = sections.indexOf(section)

                                            // Find text sections before and after canvas
                                            val textBefore = if (canvasIndex > 0)
                                                sections.getOrNull(canvasIndex - 1) as? NoteSection.TextSection
                                            else null

                                            val textAfter = if (canvasIndex < sections.size - 1)
                                                sections.getOrNull(canvasIndex + 1) as? NoteSection.TextSection
                                            else null

                                            // Merge text sections if both exist
                                            if (textBefore != null && textAfter != null) {
                                                // Merge text after into text before
                                                textBefore.content = TextFieldValue(
                                                    text = textBefore.content.text + textAfter.content.text,
                                                    selection = androidx.compose.ui.text.TextRange(textBefore.content.text.length)
                                                )
                                                // Remove the text section after canvas
                                                sections.remove(textAfter)
                                            }

                                            // Remove canvas
                                            sections.remove(section)
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(32.dp)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Canvas",
                                            tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }

        if (showFolderSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showFolderSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                CategoryPickerSheet(
                    folders = folders,
                    onFolderSelected = {
                        selectedFolder = it
                        showFolderSheet = false
                    },
                    onCreateNew = {
                        showFolderSheet = false
                        showCreateCategorySheet = true
                    }
                )
            }
        }

        if (showCreateCategorySheet) {
            val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showCreateCategorySheet = false },
                sheetState = createSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                CreateCategorySheet(
                    onDismiss = { showCreateCategorySheet = false },
                    onCreate = { name, key, iconName ->
                        noteViewModel.createFolder(name, key.name, iconName)
                        pendingNewFolderName = name
                        scope.launch { noteViewModel.loadFolders() }
                        showCreateCategorySheet = false
                    }
                )
            }
        }

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
}

@Composable
private fun CategoryPickerSheet(
    folders: List<FolderEntity>,
    onFolderSelected: (FolderEntity) -> Unit,
    onCreateNew: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Select Label", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
            items(folders) { folder ->
                val (icon, bg, fg) = getFolderIconAndColors(folder)
                Surface(
                    onClick = { onFolderSelected(folder) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(bg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(folder.name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onCreateNew, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create New Label")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CreateCategorySheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, key: CategoryKey, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedKey by remember { mutableStateOf(CategoryKey.PERSONAL) }
    var selectedIcon by remember { mutableStateOf("Person") }

    val iconOptions = listOf(
        "Person" to Icons.Default.Person, "Work" to Icons.Default.Work, "Lightbulb" to Icons.Default.Lightbulb,
        "ShoppingCart" to Icons.Default.ShoppingCart, "School" to Icons.Default.School, "Code" to Icons.Default.Code
    )

    val categoryChoices = listOf(
        CategoryKey.PERSONAL, CategoryKey.WORK, CategoryKey.IDEAS,
        CategoryKey.CHECKLIST, CategoryKey.MINT, CategoryKey.SKY
    )

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Create Label", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        TextField(value = name, onValueChange = { name = it }, placeholder = { Text("Label name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))
        Text("Color", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categoryChoices) { key ->
                val color = categoryContainerColor(key)
                Surface(
                    onClick = { selectedKey = key },
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = color,
                    border = BorderStroke(
                        if (key == selectedKey) 3.dp else 1.dp,
                        if (key == selectedKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(0.3f)
                    )
                ) {}
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Icon", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(iconOptions) { (iconName, vector) ->
                Surface(
                    onClick = { selectedIcon = iconName },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(48.dp),
                    border = BorderStroke(
                        if (selectedIcon == iconName) 2.dp else 1.dp,
                        if (selectedIcon == iconName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(0.3f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(vector, iconName)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim(), selectedKey, selectedIcon) }, enabled = name.isNotBlank()) { Text("Create") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun getFolderIconAndColors(folder: FolderEntity?): Triple<ImageVector, Color, Color> {
    if (folder == null) {
        return Triple(Icons.Default.Folder, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val categoryKey = folder.colorKey?.let { try { CategoryKey.valueOf(it) } catch (_: Exception) { null } }
    val bg = if (categoryKey != null) categoryContainerColor(categoryKey) else categoryContainerColor(categoryKeyFromName(folder.name))
    val fg = if (categoryKey != null) categoryIconColor(categoryKey) else categoryIconColor(categoryKeyFromName(folder.name))
    val icon = folder.iconName?.let { iconNameToVector(it) } ?: Icons.Default.Folder
    return Triple(icon, bg, fg)
}

private fun iconNameToVector(name: String): ImageVector? = when (name) {
    "Person" -> Icons.Default.Person
    "Work" -> Icons.Default.Work
    "Lightbulb" -> Icons.Default.Lightbulb
    "ShoppingCart" -> Icons.Default.ShoppingCart
    "School" -> Icons.Default.School
    "Code" -> Icons.Default.Code
    else -> null
}

private fun categoryKeyFromName(name: String): CategoryKey {
    val n = name.lowercase()
    return when {
        listOf("work", "meeting").any { n.contains(it) } -> CategoryKey.WORK
        listOf("personal", "life").any { n.contains(it) } -> CategoryKey.PERSONAL
        listOf("idea", "ideas").any { n.contains(it) } -> CategoryKey.IDEAS
        listOf("check", "todo", "list").any { n.contains(it) } -> CategoryKey.CHECKLIST
        else -> CategoryKey.SLATE
    }
}

@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
    defaultColor: Color
) {
    val themeColors = listOf(
        defaultColor, Color(0xFFFFFDD0), Color(0xFFE6E6FA), Color(0xFFD4EDDA),
        Color(0xFFB0E0E6), Color(0xFFF08080), Color(0xFFFFFFFF)
    )
    var selectedColor by remember { mutableStateOf(currentColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Note Color", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                themeColors.chunked(4).forEach { rowColors ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        rowColors.forEach { color ->
                            Surface(
                                onClick = { selectedColor = color },
                                modifier = Modifier.size(48.dp).border(
                                    width = if (color == selectedColor) 3.dp else 1.dp,
                                    color = if (color == selectedColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                                shape = CircleShape,
                                color = color
                            ) {}
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onColorSelected(selectedColor) }) { Text("Select") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}