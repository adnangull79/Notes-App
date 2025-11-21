package com.example.notepad.Drawing

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// IMPORT YOUR MODULES
import com.example.notepad.CreatNotes1.FormatMap
import com.example.notepad.CreatNotes1.FormatSpan
import com.example.notepad.CreatNotes1.FormatState
import com.example.notepad.CreatNotes1.FormattedTextEditor
import com.example.notepad.CreatNotes1.FormattingToolbar

sealed class Section {
    abstract val id: Long

    data class CanvasSection(
        override val id: Long,
        val canvasState: CanvasState = CanvasState()
    ) : Section()

    class TextSection(
        override val id: Long
    ) : Section() {
        var content by mutableStateOf(TextFieldValue(""))
        var formatState by mutableStateOf(FormatState())
        var formatMap by mutableStateOf<FormatMap>(emptyList())
    }
}


@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DrawingScreen(
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }


    // SECTION LIST (Canvas + optional text)
    val sections = remember {
        mutableStateListOf<Section>(
            Section.CanvasSection(id = System.currentTimeMillis())
        )
    }

    // UI & Canvas State
    var activeCanvasId by remember { mutableStateOf<Long?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Drawing Tool State
    var currentTool by remember { mutableStateOf(DrawingTool.PEN) }
    var drawColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(8f) }

    // Toolbar Panels (Drawing)
    var showToolOptions by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showCanvasOptions by remember { mutableStateOf(false) }
    var previousToolbarState by remember { mutableStateOf<String?>(null) }

    // Forced recompose for the canvas
    var recomposeTrigger by remember { mutableStateOf(0) }

    val isDrawingMode = activeCanvasId != null

    // Active canvas
    val activeCanvas = sections.firstOrNull {
        it is Section.CanvasSection && it.id == activeCanvasId
    } as? Section.CanvasSection

    // === TEXT FORMATTING STATE ===
    var activeTextSectionId by remember { mutableStateOf<Long?>(null) }
    var showTextFormattingBar by remember { mutableStateOf(false) }

    val baseTextColor = MaterialTheme.colorScheme.onSurface
    val bottomBarIconColor = MaterialTheme.colorScheme.onSurfaceVariant

    val isDarkTheme = isSystemInDarkTheme()
    val drawingManager = remember(activeCanvas) {
        activeCanvas?.let { DrawingManager(it.canvasState) }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Drawing Note",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isDrawingMode) {
                        IconButton(
                            onClick = {
                                activeCanvas?.canvasState?.hasDrawn = true
                                activeCanvasId = null
                                showToolOptions = false
                                showColorPicker = false
                                showCanvasOptions = false

                                // Add text section below canvas if it doesn't exist
                                if (activeCanvas != null && activeCanvas.canvasState.paths.isNotEmpty()) {
                                    val canvasIndex = sections.indexOfFirst { it.id == activeCanvas.id }
                                    if (canvasIndex != -1 &&
                                        (canvasIndex + 1 >= sections.size ||
                                                sections[canvasIndex + 1] !is Section.TextSection)
                                    ) {
                                        sections.add(
                                            canvasIndex + 1,
                                            Section.TextSection(id = System.currentTimeMillis())
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Done",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(onClick = { /* Share feature later */ }) {
                            Icon(Icons.Default.Share, "Share")
                        }

                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More")
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear All Canvas") },
                                onClick = {
                                    sections.forEach {
                                        if (it is Section.CanvasSection) {
                                            it.canvasState.paths.clear()
                                            it.canvasState.currentPath.clear()
                                            it.canvasState.redoStack.clear()
                                            it.canvasState.hasDrawn = false
                                        }
                                    }
                                    recomposeTrigger++
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Export as Image") },
                                onClick = {
                                    // TODO: implement
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Image, null) }
                            )
                        }
                    }
                }
            )
        },

        bottomBar = {
            when {
                // Drawing bottom bar
                isDrawingMode && activeCanvas != null -> {
                    DrawingBottomBar(
                        currentTool = currentTool,
                        drawColor = drawColor,
                        strokeWidth = strokeWidth,
                        canvasBgColor = activeCanvas.canvasState.bgColor,
                        showGridLines = activeCanvas.canvasState.showGrid,
                        showToolOptions = showToolOptions,
                        showColorPicker = showColorPicker,
                        showCanvasOptions = showCanvasOptions,
                        canUndo = activeCanvas.canvasState.paths.isNotEmpty()
                                || activeCanvas.canvasState.currentPath.isNotEmpty(),
                        canRedo = activeCanvas.canvasState.redoStack.isNotEmpty(),
                        previousToolbarState = previousToolbarState,
                        onToolChange = { tool ->
                            currentTool = tool
                            showToolOptions = true
                            showColorPicker = false
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
                            showColorPicker = true
                            showToolOptions = false
                            showCanvasOptions = false
                        },
                        onShowCanvasOptions = {
                            previousToolbarState = if (showToolOptions) "TOOL_OPTIONS" else null
                            showCanvasOptions = true
                            showToolOptions = false
                            showColorPicker = false
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
                            showColorPicker = false
                            showCanvasOptions = false
                            previousToolbarState = null
                        },
                        onBackToToolOptions = {
                            showToolOptions = true
                            showColorPicker = false
                            showCanvasOptions = false
                            previousToolbarState = null
                        }
                    )
                }

                // Text formatting bottom bar (only when not drawing and a text section is active)
                !isDrawingMode && activeTextSectionId != null -> {
                    val activeTextSection = sections.firstOrNull {
                        it is Section.TextSection && it.id == activeTextSectionId
                    } as? Section.TextSection

                    if (activeTextSection != null) {
                        FormattingToolbar(
                            showBottomBar = showTextFormattingBar,
                            onShowBottomBarChange = { isShown ->
                                if (isShown) {
                                    showTextFormattingBar = true   // allow showing the bar
                                }
                                // ignore hide requests from the cross button
                            },
                            contentText = activeTextSection.content,
                            onContentChange = { activeTextSection.content = it },
                            formatState = activeTextSection.formatState,
                            onFormatStateChange = { activeTextSection.formatState = it },
                            onApplyFormatting = { start, end, style ->
                                val currentMap = activeTextSection.formatMap.toMutableList()
                                // Remove overlapping spans
                                currentMap.removeAll { it.start < end && it.end > start }
                                currentMap.add(FormatSpan(start, end, style))
                                activeTextSection.formatMap = currentMap
                                    .filter { it.end > it.start }
                                    .sortedBy { it.start }
                            },
                            onPushHistory = {
                                // You can plug history here if you want later
                            },
                            onUndo = {
                                // No-op for now (can be wired to history)
                            },
                            onRedo = {
                                // No-op for now (can be wired to history)
                            },
                            canUndo = false,
                            canRedo = false,
                            onShowColorPicker = {
                                // Note color picker (not needed in drawing screen for now)
                            },
                            textColor = baseTextColor,
                            bottomBarIconColor = bottomBarIconColor,
                            onAddDrawing = {  }
                        )
                    }
                }
            }
        },

        floatingActionButton = {
            if (!isDrawingMode) {
                FloatingActionButton(
                    onClick = {
                        sections.add(Section.CanvasSection(id = System.currentTimeMillis()))
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Add Canvas")
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // TITLE FIELD
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title") },
                singleLine = true,
                enabled = !isDrawingMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            // RENDER SECTIONS
            sections.forEach { section ->
                when (section) {

                    is Section.CanvasSection -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(600.dp)
                                .padding(horizontal = 16.dp)
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
                                    }
                                }
                            )

                            // Edit Button
                            if (section.canvasState.hasDrawn && section.id != activeCanvasId) {
                                IconButton(
                                    onClick = { activeCanvasId = section.id },
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp)
                                        .size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Canvas",
                                        tint = if (isDarkTheme) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Delete Button
                            if (section.canvasState.hasDrawn && section.id != activeCanvasId) {
                                IconButton(
                                    onClick = { sections.remove(section) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Canvas",
                                        tint = if (isDarkTheme) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                    }

                    is Section.TextSection -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            FormattedTextEditor(
                                content = section.content,
                                onContentChange = { newValue ->
                                    section.content = newValue
                                },
                                formatState = section.formatState,
                                onFormatStateChange = { section.formatState = it },
                                formatMap = section.formatMap,
                                onFormatMapChange = { newMap ->
                                    section.formatMap = newMap
                                },
                                textColor = baseTextColor,
                                onFocusChanged = { focused ->
                                    if (focused) {
                                        activeTextSectionId = section.id
                                        showTextFormattingBar = true
                                    }
                                },
                                onCheckboxToggle = { toggledValue ->
                                    section.content = toggledValue
                                }
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

