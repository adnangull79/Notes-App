package com.example.notepad.CreatNotes1

import android.os.Build
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import com.example.notepad.Drawing.toJson
import com.example.notepad.Drawing.loadFromJson
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notepad.Audio.AudioData
import com.example.notepad.Checklist.*
import com.example.notepad.CreatNotes.FormattingManager
import com.example.notepad.Drawing.*
import com.example.notepad.FolderEntity
import com.example.notepad.NoteType
import com.example.notepad.NoteViewModel
import com.example.notepad.UI_theme.CategoryKey
import com.example.notepad.UI_theme.categoryContainerColor
import com.example.notepad.UI_theme.categoryIconColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

// ✅ Audio imports

import com.example.notepad.Audio.AudioRecordingBottomSheet
import com.example.notepad.Audio.AudioNoteItem
import com.example.notepad.audio_notes.AudioPlayer
import com.example.notepad.audio_notes.AudioRecorder


// --- Section Data Classes ---
sealed class NoteSection {
    abstract val id: Long

    data class CanvasSection(
        override val id: Long,
        val canvasState: CanvasState = CanvasState()
    ) : NoteSection()

    data class TextSection(
        override val id: Long
    ) : NoteSection() {
        var content by mutableStateOf(TextFieldValue(""))
        var formatState by mutableStateOf(FormatState())
        var formatMap by mutableStateOf<FormatMap>(emptyList())
        var focusRequester = FocusRequester()
    }

    data class ChecklistSection(
        override val id: Long,
        val stateManager: ChecklistStateManager
    ) : NoteSection()

    // ✅ NEW: Audio Section
    data class AudioSection(
        override val id: Long,
        val audioSection: com.example.notepad.Audio.AudioSection
    ) : NoteSection()
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteScreen(
    onNavigateBack: () -> Unit,
    noteViewModel: NoteViewModel = viewModel(),
    noteId: Int? = null,
    noteType: NoteType = NoteType.TEXT
) {
    val context = LocalContext.current
    val titleFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

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
    val isEditMode = noteId != null
    var isLoading by remember { mutableStateOf(noteId != null) }
    var isFavorite by remember { mutableStateOf(false) }

    val sections = remember {
        mutableStateListOf<NoteSection>(
            NoteSection.TextSection(id = System.currentTimeMillis())
        )
    }

    // ✅ AUDIO STATE VARIABLES
    val audioRecorder = remember { AudioRecorder(context) }
    val audioPlayer = remember { AudioPlayer() }
    var showAudioRecordingSheet by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) }
    var playingAudioId by remember { mutableStateOf<Long?>(null) }
    var currentAudioPosition by remember { mutableStateOf(0) }

    // Canvas state
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

    var activeTextSectionId by remember { mutableStateOf<Long?>((sections.firstOrNull() as? NoteSection.TextSection)?.id) }
    var activeChecklistSectionId by remember { mutableStateOf<Long?>(null) }

    val activeTextSection = sections.firstOrNull { it.id == activeTextSectionId } as? NoteSection.TextSection
    val activeChecklistSection = sections.firstOrNull { it.id == activeChecklistSectionId } as? NoteSection.ChecklistSection

    val formattingManager = remember { FormattingManager(textRenderColor) }
    val folders by noteViewModel.folders.collectAsState()
    var pendingNewFolderName by remember { mutableStateOf<String?>(null) }
    val drawingManager = remember(activeCanvas) { activeCanvas?.let { DrawingManager(it.canvasState) } }
    val itemHeightPx = with(LocalDensity.current) { 80.dp.toPx() }
    val dragDropState = remember { DragDropState() }

    val undoRedoHandler = rememberUndoRedoHandler(
        formattingManager = formattingManager,
        activeChecklistSection = activeChecklistSection,
        activeTextSection = activeTextSection,
        textRenderColor = textRenderColor
    )

    // ✅ AUDIO RECORDING TIMER
    LaunchedEffect(isRecording) {
        if (isRecording) {
            val startTime = System.currentTimeMillis()
            while (isActive && isRecording) {
                recordingDuration = System.currentTimeMillis() - startTime
                delay(100)
            }
        } else {
            recordingDuration = 0L
        }
    }

    // ✅ AUDIO PLAYBACK POSITION UPDATER
    LaunchedEffect(playingAudioId) {
        while (isActive && playingAudioId != null) {
            currentAudioPosition = audioPlayer.getCurrentPosition()
            delay(100)
        }
    }

    // ✅ AUDIO RECORDING FUNCTIONS
    fun startAudioRecording() {
        val filePath = audioRecorder.startRecording()
        if (filePath != null) {
            isRecording = true
        }
    }

    fun stopAndSaveAudio() {
        val filePath = audioRecorder.stopRecording()
        if (filePath != null) {
            val duration = audioPlayer.getAudioDuration(filePath)
            val newAudioSection = com.example.notepad.Audio.AudioSection(
                id = System.currentTimeMillis(),
                filePath = filePath,
                duration = duration,
                noteText = ""
            )

            val audioSectionWrapper = NoteSection.AudioSection(
                id = newAudioSection.id,
                audioSection = newAudioSection
            )

            val insertIndex = if (activeTextSection != null) {
                sections.indexOf(activeTextSection) + 1
            } else {
                sections.size
            }

            sections.add(insertIndex, audioSectionWrapper)

            // ✅ NEW: Add text section after audio so user can continue typing
            val newTextSection = NoteSection.TextSection(id = System.currentTimeMillis() + 1000)
            sections.add(insertIndex + 1, newTextSection)
            activeTextSectionId = newTextSection.id

            // ✅ NEW: Auto-focus the new text section
            scope.launch {
                delay(100)
                try {
                    newTextSection.focusRequester.requestFocus()
                    keyboardController?.show()
                } catch (e: Exception) {
                    // Focus request failed, ignore
                }
            }
        }
        isRecording = false
        showAudioRecordingSheet = false
    }

    fun cancelAudioRecording() {
        audioRecorder.cancelRecording()
        isRecording = false
        showAudioRecordingSheet = false
    }

    // ✅ UPDATED SAVE AND EXIT WITH AUDIO SUPPORT
    val saveAndExit = {
        if (title.isNotBlank() || sections.size > 1 || (sections.firstOrNull() as? NoteSection.TextSection)?.content?.text?.isNotBlank() == true) {

            val sectionDataList = sections.mapNotNull { section ->
                when (section) {
                    is NoteSection.TextSection -> {
                        if (section.content.text.isNotBlank()) {
                            NoteSectionData.Text(content = section.content.text)
                        } else null
                    }
                    is NoteSection.CanvasSection -> {
                        if (section.canvasState.paths.isNotEmpty()) {
                            NoteSectionData.Canvas(
                                drawingData = DrawingData(
                                    strokes = section.canvasState.paths.map { path ->
                                        StrokeData(
                                            id = "${System.currentTimeMillis()}_${Math.random()}",
                                            points = path.points.map { PointData(it.x, it.y) },
                                            color = colorToHex(path.color),
                                            strokeWidth = path.strokeWidth,
                                            toolType = if (path.isEraser) "ERASER" else "PEN"
                                        )
                                    },
                                    bgColor = colorToHex(section.canvasState.bgColor),
                                    showGrid = section.canvasState.showGrid
                                )
                            )
                        } else null
                    }
                    is NoteSection.ChecklistSection -> {
                        val checklistString = section.stateManager.itemsToString()
                        if (checklistString.isNotBlank()) {
                            NoteSectionData.Checklist(items = checklistString)
                        } else null
                    }
                    // ✅ NEW: Handle Audio Section
                    is NoteSection.AudioSection -> {
                        NoteSectionData.Audio(
                            audioData = AudioData(
                                filePath = section.audioSection.filePath,
                                duration = section.audioSection.duration,
                                timestamp = section.audioSection.id,
                                noteText = section.audioSection.noteText
                            )
                        )
                    }
                }
            }

            val createNoteStructure = CreateNoteStructure(sections = sectionDataList)
            val structuredData = if (sectionDataList.isNotEmpty()) {
                Json.encodeToString(createNoteStructure)
            } else null

            val folderToUse = selectedFolder ?: folders.firstOrNull()

            if (folderToUse != null) {
                if (isEditMode && noteId != null) {
                    scope.launch {
                        val existing = noteViewModel.getNoteById(noteId)
                        if (existing != null) {
                            noteViewModel.updateNote(
                                existing.copy(
                                    folderId = folderToUse.id,
                                    title = title,
                                    content = null,
                                    drawingData = structuredData,
                                    noteType = NoteType.TEXT,
                                    lastEditedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                } else {
                    noteViewModel.createNote(
                        folderId = folderToUse.id,
                        title = title,
                        content = null,
                        drawingData = structuredData,
                        noteType = NoteType.TEXT
                    )
                }
            }
        }
        audioPlayer.release()
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

    // ✅ UPDATED LOAD NOTE WITH AUDIO SUPPORT
    LaunchedEffect(noteId) {
        if (noteId != null) {
            scope.launch {
                val existingNote = noteViewModel.getNoteById(noteId)
                if (existingNote != null) {

                    title = existingNote.title
                    isFavorite = existingNote.isFavorite

                    sections.clear()

                    existingNote.drawingData?.let { jsonData ->
                        try {
                            val createNoteStructure = Json.decodeFromString<CreateNoteStructure>(jsonData)

                            createNoteStructure.sections.forEachIndexed { index, sectionData ->
                                when (sectionData) {

                                    is NoteSectionData.Text -> {
                                        val textSection = NoteSection.TextSection(
                                            id = System.currentTimeMillis() + index
                                        )
                                        textSection.content = TextFieldValue(sectionData.content)
                                        sections.add(textSection)
                                    }

                                    is NoteSectionData.Canvas -> {
                                        val canvasSection = NoteSection.CanvasSection(
                                            id = System.currentTimeMillis() + index + 10000
                                        )

                                        canvasSection.canvasState.paths.clear()
                                        canvasSection.canvasState.paths.addAll(
                                            sectionData.drawingData.strokes.map { stroke ->
                                                DrawPath(
                                                    points = stroke.points.map {
                                                        androidx.compose.ui.geometry.Offset(it.x, it.y)
                                                    },
                                                    color = hexToColor(stroke.color),
                                                    strokeWidth = stroke.strokeWidth,
                                                    isEraser = stroke.toolType == "ERASER"
                                                )
                                            }
                                        )

                                        canvasSection.canvasState.bgColor = hexToColor(sectionData.drawingData.bgColor)
                                        canvasSection.canvasState.showGrid = sectionData.drawingData.showGrid
                                        canvasSection.canvasState.hasDrawn = true

                                        sections.add(canvasSection)
                                    }

                                    is NoteSectionData.Checklist -> {
                                        val checklistManager = ChecklistStateManager(
                                            scope,
                                            ChecklistConfig(
                                                showDragHandles = true,
                                                showAddButton = false,
                                                enableDragReorder = true,
                                                autoFocusNewItems = true
                                            )
                                        )
                                        checklistManager.parseAndLoadItems(sectionData.items)

                                        val checklistSection = NoteSection.ChecklistSection(
                                            id = System.currentTimeMillis() + index + 20000,
                                            stateManager = checklistManager
                                        )

                                        sections.add(checklistSection)
                                    }

                                    // ✅ NEW: Load Audio Section
                                    is NoteSectionData.Audio -> {
                                        val audioSec = com.example.notepad.Audio.AudioSection(
                                            id = sectionData.audioData.timestamp,
                                            filePath = sectionData.audioData.filePath,
                                            duration = sectionData.audioData.duration,
                                            noteText = sectionData.audioData.noteText
                                        )

                                        val audioSectionWrapper = NoteSection.AudioSection(
                                            id = audioSec.id,
                                            audioSection = audioSec
                                        )

                                        sections.add(audioSectionWrapper)
                                    }
                                }
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (sections.isEmpty()) {
                        sections.add(NoteSection.TextSection(id = System.currentTimeMillis()))
                    }

                    val last = sections.lastOrNull()
                    if (last is NoteSection.CanvasSection) {
                        val newText = NoteSection.TextSection(id = System.currentTimeMillis() + 99999)
                        sections.add(newText)
                        scope.launch {
                            delay(150)
                            newText.focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    }

                    val firstText = sections.firstOrNull { it is NoteSection.TextSection } as? NoteSection.TextSection
                    firstText?.let { formattingManager.initializeHistory(it.content) }
                }

                isLoading = false
            }
        } else {
            val firstText = sections.firstOrNull() as? NoteSection.TextSection
            firstText?.let { formattingManager.initializeHistory(it.content) }
        }
    }

    LaunchedEffect(Unit) { noteViewModel.loadFolders() }

    LaunchedEffect(noteId) {
        delay(150)

        if (noteId == null) {
            titleFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            val lastTextSection = sections.lastOrNull { it is NoteSection.TextSection } as? NoteSection.TextSection

            if (lastTextSection != null) {
                val text = lastTextSection.content.text
                lastTextSection.content = TextFieldValue(
                    text = text,
                    selection = androidx.compose.ui.text.TextRange(text.length)
                )

                lastTextSection.focusRequester.requestFocus()
                keyboardController?.hide()
            }
        }
    }

    LaunchedEffect(folders, noteId) {
        if (noteId != null) {
            scope.launch {
                val existingNote = noteViewModel.getNoteById(noteId)
                existingNote?.let { n -> selectedFolder = folders.find { it.id == n.folderId } }
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
        val insertIndex = if (activeTextSection != null) sections.indexOf(activeTextSection) + 1 else sections.size
        sections.add(insertIndex, newCanvas)
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
            val alreadyTextBelow = sections.getOrNull(nextIndex) is NoteSection.TextSection

            if (!alreadyTextBelow) {
                val newTextSection = NoteSection.TextSection(id = System.currentTimeMillis())
                sections.add(nextIndex, newTextSection)
                activeTextSectionId = newTextSection.id

                scope.launch {
                    delay(100)
                    try {
                        newTextSection.focusRequester.requestFocus()
                        keyboardController?.show()
                    } catch (e: Exception) { }
                }
            }
        }
    }

    fun exitChecklistToText(sectionIndex: Int, itemText: String, itemIdToRemove: String? = null) {
        val checklistSection = sections[sectionIndex] as? NoteSection.ChecklistSection ?: return
        if (itemIdToRemove != null) checklistSection.stateManager.deleteItem(itemIdToRemove)
        val newTextSection = NoteSection.TextSection(System.currentTimeMillis()).apply { content = TextFieldValue(itemText) }
        sections.add(sectionIndex + 1, newTextSection)
        activeChecklistSectionId = null
        activeTextSectionId = newTextSection.id
        scope.launch {
            delay(50)
            newTextSection.focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    fun convertLineToChecklist() {
        val section = activeTextSection ?: return
        val text = section.content.text
        val cursor = section.content.selection.start
        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val textBefore = text.substring(0, lineStart).trimEnd('\n')
        val lineText = text.substring(lineStart, lineEnd)
        val textAfter = text.substring(lineEnd).trimStart('\n')
        val index = sections.indexOf(section)
        sections.removeAt(index)

        if (textAfter.isNotEmpty()) {
            sections.add(index, NoteSection.TextSection(System.currentTimeMillis() + 2).apply { content = TextFieldValue(textAfter) })
        }
        val checklistManager = ChecklistStateManager(scope, ChecklistConfig(showDragHandles = true, showAddButton = false, enableDragReorder = true, autoFocusNewItems = true))
        checklistManager.addItem(text = lineText, formatState = section.formatState, autoFocus = true)
        val newChecklist = NoteSection.ChecklistSection(System.currentTimeMillis() + 1, checklistManager)
        sections.add(index, newChecklist)
        if (textBefore.isNotEmpty()) {
            sections.add(index, NoteSection.TextSection(System.currentTimeMillis()).apply { content = TextFieldValue(textBefore) })
        }
        activeChecklistSectionId = newChecklist.id
        activeTextSectionId = null
    }

    Scaffold(
        containerColor = noteBackgroundColor,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(text = if (isEditMode) "Edit Note" else "Create Note", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = {
                    IconButton(onClick = { if (isDrawingMode) finishDrawing() else saveAndExit() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (isDrawingMode) {
                        IconButton(onClick = { finishDrawing() }) {
                            Icon(Icons.Default.Check, "Done Drawing", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = { undoRedoHandler.undo() }, enabled = undoRedoHandler.canUndo()) {
                            Icon(Icons.Default.Undo, "Undo", tint = if (undoRedoHandler.canUndo()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                        }
                        IconButton(onClick = { undoRedoHandler.redo() }, enabled = undoRedoHandler.canRedo()) {
                            Icon(Icons.Default.Redo, "Redo", tint = if (undoRedoHandler.canRedo()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                        }
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More Options", tint = MaterialTheme.colorScheme.onBackground)
                        }
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
                    onCanvasBgChange = { activeCanvas.canvasState.bgColor = it; recomposeTrigger++ },
                    onGridToggle = { activeCanvas.canvasState.showGrid = it; recomposeTrigger++ },
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
                    onUndo = { if (drawingManager?.undo() == true) recomposeTrigger++ },
                    onRedo = { if (drawingManager?.redo() == true) recomposeTrigger++ },
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
                            section.content = newValue
                        }
                    },
                    formatState = activeTextSection?.formatState ?: activeChecklistSection?.let { FormatState() } ?: FormatState(),
                    onFormatStateChange = { newState ->
                        activeTextSection?.let {
                            it.formatState = newState
                            formattingManager.updateFormatState(newState)
                        }
                    },
                    onApplyFormatting = { start, end, style -> activeTextSection?.let { formattingManager.applyFormatting(it.content, start, end, style) } },
                    onPushHistory = { activeTextSection?.let { formattingManager.pushHistory(it.content, formattingManager.formatMap, formattingManager.formatState) } },
                    onUndo = { undoRedoHandler.undo() },
                    onRedo = { undoRedoHandler.redo() },
                    canUndo = if (activeChecklistSection != null) activeChecklistSection.stateManager.canUndo() else formattingManager.canUndo(),
                    canRedo = if (activeChecklistSection != null) activeChecklistSection.stateManager.canRedo() else formattingManager.canRedo(),
                    onShowColorPicker = { showColorPicker = true },
                    textColor = textRenderColor,
                    bottomBarIconColor = bottomBarIconColor,
                    onAddDrawing = { addDrawingCanvas() },
                    onCheckboxClick = { convertLineToChecklist() },
                    isInChecklistMode = activeChecklistSection != null,
                    // ✅ WIRE UP MICROPHONE CALLBACK
                    onMicrophoneClick = { showAudioRecordingSheet = true }
                )
            }
        },
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
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
                    placeholder = { Text("Title", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    enabled = !isDrawingMode,
                    modifier = Modifier.fillMaxWidth().offset(y = (-12).dp).focusRequester(titleFocusRequester),
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

                sections.forEachIndexed { index, section ->
                    when (section) {
                        is NoteSection.TextSection -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).focusRequester(section.focusRequester)) {
                                FormattedTextEditor(
                                    content = section.content,
                                    onContentChange = { newValue ->
                                        if (!isDrawingMode) {
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
                                        }
                                    },
                                    textColor = textRenderColor,
                                    onFocusChanged = { focused ->
                                        if (focused && !isDrawingMode) {
                                            activeTextSectionId = section.id
                                            activeChecklistSectionId = null
                                        }
                                    },
                                    onCheckboxToggle = { },
                                    formattingManager = formattingManager
                                )
                            }
                        }
                        is NoteSection.ChecklistSection -> {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                section.stateManager.items.forEachIndexed { itemIndex, item ->
                                    key(item.id) {
                                        ChecklistItemRow(
                                            item = item,
                                            index = itemIndex,
                                            dragDropState = dragDropState,
                                            itemHeight = itemHeightPx,
                                            itemCount = section.stateManager.items.size,
                                            focusRequester = section.stateManager.getFocusRequester(item.id),
                                            formatState = section.stateManager.items.find { it.id == section.stateManager.focusedItemId }?.let { FormatState(isBold = it.isBold) } ?: FormatState(),
                                            textRenderColor = textRenderColor,
                                            isFocused = section.stateManager.focusedItemId == item.id,
                                            config = ChecklistConfig(showDragHandles = true),
                                            onFocusChanged = { focused -> if (focused) { section.stateManager.setFocusedItem(item.id); activeChecklistSectionId = section.id; activeTextSectionId = null } },
                                            onCheckedChange = { section.stateManager.toggleItemChecked(item.id) },
                                            onTextChange = { newText ->
                                                val cleanText = newText.replace("\n", "")
                                                val isNowEmpty = cleanText.isEmpty()
                                                val wasEmpty = item.text.isEmpty()
                                                val hasNewline = newText.contains("\n")
                                                if (hasNewline && wasEmpty) { exitChecklistToText(index, "", item.id); return@ChecklistItemRow }
                                                if (hasNewline) { section.stateManager.updateItemText(item.id, cleanText, FormatState()); section.stateManager.addItem("", FormatState(), itemIndex, true); return@ChecklistItemRow }
                                                section.stateManager.updateItemText(item.id, cleanText, FormatState())
                                            },
                                            onDelete = {
                                                if (itemIndex == 0 && section.stateManager.items.size == 1) exitChecklistToText(index, "", item.id)
                                                else section.stateManager.deleteItem(item.id)
                                            },
                                            onReorder = { from, to -> section.stateManager.reorderItems(from, to) },
                                            onFormatChange = { updated -> section.stateManager.updateItemFormatting(item.id, updated) }
                                        )
                                    }
                                }
                            }
                        }
                        is NoteSection.CanvasSection -> {
                            Box(modifier = Modifier.fillMaxWidth().height(500.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
                                DrawingCanvas(
                                    canvasState = section.canvasState,
                                    drawColor = drawColor,
                                    strokeWidth = strokeWidth,
                                    currentTool = currentTool,
                                    isDrawingMode = section.id == activeCanvasId,
                                    isCanvasLocked = section.canvasState.hasDrawn && section.id != activeCanvasId,
                                    recomposeTrigger = recomposeTrigger,
                                    onRecompose = { recomposeTrigger++ },
                                    onEnterDrawingMode = { if (!section.canvasState.hasDrawn || section.id == activeCanvasId) { activeCanvasId = section.id; keyboardController?.hide() } }
                                )
                                if (section.canvasState.hasDrawn && section.id != activeCanvasId) {
                                    IconButton(
                                        onClick = { activeCanvasId = section.id; keyboardController?.hide() },
                                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(32.dp)
                                    ) { Icon(Icons.Default.Edit, "Edit Canvas", tint = Color.Black, modifier = Modifier.size(18.dp)) }
                                    IconButton(
                                        onClick = {
                                            val canvasIndex = sections.indexOf(section)
                                            val textBefore = if (canvasIndex > 0) sections.getOrNull(canvasIndex - 1) as? NoteSection.TextSection else null
                                            val textAfter = if (canvasIndex < sections.size - 1) sections.getOrNull(canvasIndex + 1) as? NoteSection.TextSection else null
                                            if (textBefore != null && textAfter != null) {
                                                textBefore.content = TextFieldValue(text = textBefore.content.text + textAfter.content.text, selection = androidx.compose.ui.text.TextRange(textBefore.content.text.length))
                                                sections.remove(textAfter)
                                            }
                                            sections.remove(section)
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp)
                                    ) { Icon(Icons.Default.Delete, "Delete Canvas", tint = Color.Black, modifier = Modifier.size(18.dp)) }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        // ✅ NEW: Render Audio Section
                        is NoteSection.AudioSection -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                AudioNoteItem(
                                    filePath = section.audioSection.filePath,
                                    duration = section.audioSection.duration,
                                    isPlaying = playingAudioId == section.audioSection.id && audioPlayer.isPlaying(),
                                    currentPosition = if (playingAudioId == section.audioSection.id) currentAudioPosition else 0,
                                    onPlayPause = {
                                        if (playingAudioId == section.audioSection.id) {
                                            if (audioPlayer.isPlaying()) {
                                                audioPlayer.pause()
                                            } else {
                                                audioPlayer.resume()
                                            }
                                        } else {
                                            audioPlayer.stop()
                                            currentAudioPosition = 0
                                            playingAudioId = section.audioSection.id
                                            audioPlayer.play(section.audioSection.filePath)
                                            audioPlayer.setOnCompletionListener {
                                                playingAudioId = null
                                                currentAudioPosition = 0
                                            }
                                        }
                                    },
                                    onSeek = { position ->
                                        audioPlayer.seekTo(position)
                                        currentAudioPosition = position
                                    },
                                    onDelete = {
                                        audioRecorder.deleteAudioFile(section.audioSection.filePath)
                                        sections.remove(section)
                                        if (playingAudioId == section.audioSection.id) {
                                            audioPlayer.stop()
                                            playingAudioId = null
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(100.dp))
            }
        }

        // ✅ AUDIO RECORDING BOTTOM SHEET
        if (showAudioRecordingSheet) {
            AudioRecordingBottomSheet(
                isRecording = isRecording,
                recordingDuration = recordingDuration,
                onStartRecording = { startAudioRecording() },
                onStopAndSave = { stopAndSaveAudio() },
                onCancel = { cancelAudioRecording() },
                onDismiss = {
                    if (!isRecording) {
                        showAudioRecordingSheet = false
                    }
                }
            )
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
            onPageColor = { showColorPicker = true },
            onDelete = { /* TODO: Implement delete */ }
        )

        if (showFolderSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(onDismissRequest = { showFolderSheet = false }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
                CategoryPickerSheet(folders = folders, onFolderSelected = { selectedFolder = it; showFolderSheet = false }, onCreateNew = { showFolderSheet = false; showCreateCategorySheet = true })
            }
        }

        if (showCreateCategorySheet) {
            val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(onDismissRequest = { showCreateCategorySheet = false }, sheetState = createSheetState, containerColor = MaterialTheme.colorScheme.surface) {
                CreateCategorySheet(onDismiss = { showCreateCategorySheet = false }, onCreate = { name, key, iconName -> noteViewModel.createFolder(name, key.name, iconName); pendingNewFolderName = name; scope.launch { noteViewModel.loadFolders() }; showCreateCategorySheet = false })
            }
        }

        if (showColorPicker) {
            ColorPickerDialog(currentColor = noteBackgroundColor, onColorSelected = { noteBackgroundColor = it; showColorPicker = false }, onDismiss = { showColorPicker = false }, defaultColor = defaultNoteBgColor)
        }
    }

    FormattingFAB(showBottomBar = showBottomBar, onShowBottomBar = { showBottomBar = true })

    // ✅ CLEANUP AUDIO PLAYER ON DISPOSE
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
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