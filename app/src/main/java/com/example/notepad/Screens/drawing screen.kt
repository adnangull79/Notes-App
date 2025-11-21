//package com.example.notepad.Screens
//
//import android.annotation.SuppressLint
//import androidx.compose.animation.*
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.gestures.detectDragGestures
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.BasicTextField
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Path
//import androidx.compose.ui.graphics.StrokeCap
//import androidx.compose.ui.graphics.StrokeJoin
//import androidx.compose.ui.graphics.drawscope.Stroke
//import androidx.compose.ui.graphics.SolidColor
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//// ----------------------------
////        DATA MODELS
//// ----------------------------
//
//data class DrawPath(
//    val points: List<Offset>,
//    val color: Color,
//    val strokeWidth: Float,
//    val isEraser: Boolean = false
//)
//
//enum class DrawingTool {
//    PEN, MARKER, HIGHLIGHTER, ERASER
//}
//
//data class CanvasState(
//    val paths: MutableList<DrawPath> = mutableListOf(),
//    var currentPath: MutableList<Offset> = mutableListOf(),
//    var currentColor: Color = Color.Black,
//    var currentStrokeWidth: Float = 8f,
//    var currentIsEraser: Boolean = false,
//    var redoStack: MutableList<DrawPath> = mutableListOf(),
//    var bgColor: Color = Color.White,
//    var showGrid: Boolean = false,
//    var hasDrawn: Boolean = false
//)
//
//sealed class Section {
//    abstract val id: Long
//
//    data class CanvasSection(
//        override val id: Long,
//        val canvasState: CanvasState = CanvasState()
//    ) : Section()
//
//    class TextSection(
//        override val id: Long
//    ) : Section() {
//        var content by mutableStateOf("")
//    }}
//
//// ----------------------------
////        MAIN SCREEN
//// ----------------------------
//
//    @OptIn(ExperimentalMaterial3Api::class)
//    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
//    @Composable
//    fun DrawingScreen(
//        onNavigateBack: () -> Unit
//    ) {
//        var title by remember { mutableStateOf("") }
//        val sections = remember { mutableStateListOf<Section>(
//            Section.CanvasSection(id = System.currentTimeMillis())
//        ) }
//
//        // UI state
//        var activeCanvasId by remember { mutableStateOf<Long?>(null) }
//        var showMoreMenu by remember { mutableStateOf(false) }
//
//        // Tool state
//        var currentTool by remember { mutableStateOf(DrawingTool.PEN) }
//
//        // Drawing properties
//        var drawColor by remember { mutableStateOf(Color.Black) }
//        var strokeWidth by remember { mutableStateOf(8f) }
//
//        // Sub-panel state
//        var showToolOptions by remember { mutableStateOf(false) }
//        var showColorPicker by remember { mutableStateOf(false) }
//        var showCanvasOptions by remember { mutableStateOf(false) }
//        var previousToolbarState by remember { mutableStateOf<String?>(null) }
//
//        // Force recomposition trigger
//        var recomposeTrigger by remember { mutableStateOf(0) }
//
//        val isDrawingMode = activeCanvasId != null
//
//        // Get active canvas state
//        val activeCanvas = sections.firstOrNull {
//            it is Section.CanvasSection && it.id == activeCanvasId
//        } as? Section.CanvasSection
//
//        Scaffold(
//            containerColor = MaterialTheme.colorScheme.background,
//            contentWindowInsets = WindowInsets(0, 0, 0, 0), // Prevent FAB from moving with keyboard
//            topBar = {
//                TopAppBar(
//                    title = {
//                        Text(
//                            "Drawing Note",
//                            fontSize = 18.sp,
//                            fontWeight = FontWeight.SemiBold
//                        )
//                    },
//                    navigationIcon = {
//                        IconButton(onClick = onNavigateBack) {
//                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                        }
//                    },
//                    actions = {
//                        if (isDrawingMode) {
//                            // Show Done button in drawing mode
//                            IconButton(
//                                onClick = {
//                                    // Mark canvas as drawn
//                                    activeCanvas?.canvasState?.hasDrawn = true
//                                    activeCanvasId = null
//                                    showToolOptions = false
//                                    showColorPicker = false
//                                    showCanvasOptions = false
//
//                                    // Add text section after this canvas ONLY if canvas has drawings
//                                    if (activeCanvas != null && activeCanvas.canvasState.paths.isNotEmpty()) {
//                                        val canvasIndex = sections.indexOfFirst { it.id == activeCanvas.id }
//                                        if (canvasIndex != -1 &&
//                                            (canvasIndex + 1 >= sections.size ||
//                                                    sections[canvasIndex + 1] !is Section.TextSection)) {
//                                            sections.add(
//                                                canvasIndex + 1,
//                                                Section.TextSection(id = System.currentTimeMillis())
//                                            )
//                                        }
//                                    }
//                                }
//                            ) {
//                                Icon(
//                                    Icons.Default.Check,
//                                    contentDescription = "Done Drawing",
//                                    tint = MaterialTheme.colorScheme.primary
//                                )
//                            }
//                        } else {
//                            // Show normal actions when not drawing
//                            IconButton(onClick = { /* Share */ }) {
//                                Icon(Icons.Default.Share, contentDescription = "Share")
//                            }
//
//                            IconButton(onClick = { showMoreMenu = true }) {
//                                Icon(Icons.Default.MoreVert, contentDescription = "More")
//                            }
//
//                            DropdownMenu(
//                                expanded = showMoreMenu,
//                                onDismissRequest = { showMoreMenu = false }
//                            ) {
//                                DropdownMenuItem(
//                                    text = { Text("Clear All Canvas") },
//                                    onClick = {
//                                        sections.forEach { section ->
//                                            if (section is Section.CanvasSection) {
//                                                section.canvasState.paths.clear()
//                                                section.canvasState.currentPath.clear()
//                                                section.canvasState.redoStack.clear()
//                                                section.canvasState.hasDrawn = false
//                                            }
//                                        }
//                                        recomposeTrigger++
//                                        showMoreMenu = false
//                                    },
//                                    leadingIcon = { Icon(Icons.Default.Delete, null) }
//                                )
//                                DropdownMenuItem(
//                                    text = { Text("Export as Image") },
//                                    onClick = {
//                                        showMoreMenu = false
//                                    },
//                                    leadingIcon = { Icon(Icons.Default.Image, null) }
//                                )
//                            }
//                        }
//                    },
//                    colors = TopAppBarDefaults.topAppBarColors(
//                        containerColor = MaterialTheme.colorScheme.surface
//                    )
//                )
//            },
//            bottomBar = {
//                if (isDrawingMode && activeCanvas != null) {
//                    DrawingBottomBar(
//                        currentTool = currentTool,
//                        drawColor = drawColor,
//                        strokeWidth = strokeWidth,
//                        canvasBgColor = activeCanvas.canvasState.bgColor,
//                        showGridLines = activeCanvas.canvasState.showGrid,
//                        showToolOptions = showToolOptions,
//                        showColorPicker = showColorPicker,
//                        showCanvasOptions = showCanvasOptions,
//                        canUndo = activeCanvas.canvasState.paths.isNotEmpty() ||
//                                activeCanvas.canvasState.currentPath.isNotEmpty(),
//                        canRedo = activeCanvas.canvasState.redoStack.isNotEmpty(),
//                        previousToolbarState = previousToolbarState,
//                        onToolChange = { tool ->
//                            currentTool = tool
//                            showToolOptions = true
//                            showColorPicker = false
//                            showCanvasOptions = false
//                            previousToolbarState = null
//                        },
//                        onColorChange = { drawColor = it },
//                        onStrokeWidthChange = { strokeWidth = it },
//                        onCanvasBgChange = {
//                            activeCanvas.canvasState.bgColor = it
//                            recomposeTrigger++
//                        },
//                        onGridToggle = {
//                            activeCanvas.canvasState.showGrid = it
//                            recomposeTrigger++
//                        },
//                        onShowColorPicker = {
//                            previousToolbarState = if (showToolOptions) "TOOL_OPTIONS" else null
//                            showColorPicker = true
//                            showToolOptions = false
//                            showCanvasOptions = false
//                        },
//                        onShowCanvasOptions = {
//                            previousToolbarState = if (showToolOptions) "TOOL_OPTIONS" else null
//                            showCanvasOptions = true
//                            showToolOptions = false
//                            showColorPicker = false
//                        },
//                        onUndo = {
//                            if (activeCanvas.canvasState.paths.isNotEmpty()) {
//                                val removed = activeCanvas.canvasState.paths.removeAt(
//                                    activeCanvas.canvasState.paths.lastIndex
//                                )
//                                activeCanvas.canvasState.redoStack.add(removed)
//                                recomposeTrigger++
//                            }
//                        },
//                        onRedo = {
//                            if (activeCanvas.canvasState.redoStack.isNotEmpty()) {
//                                val restored = activeCanvas.canvasState.redoStack.removeAt(
//                                    activeCanvas.canvasState.redoStack.lastIndex
//                                )
//                                activeCanvas.canvasState.paths.add(restored)
//                                recomposeTrigger++
//                            }
//                        },
//                        onCloseSubPanel = {
//                            showToolOptions = false
//                            showColorPicker = false
//                            showCanvasOptions = false
//                            previousToolbarState = null
//                        },
//                        onBackToToolOptions = {
//                            showToolOptions = true
//                            showColorPicker = false
//                            showCanvasOptions = false
//                            previousToolbarState = null
//                        }
//                    )
//                }
//            },
//            floatingActionButton = {
//                if (!isDrawingMode) {
//                    FloatingActionButton(
//                        onClick = {
//                            sections.add(
//                                Section.CanvasSection(id = System.currentTimeMillis())
//                            )
//                        },
//                        containerColor = MaterialTheme.colorScheme.primary
//                    ) {
//                        Icon(Icons.Default.Add, "Add Canvas")
//                    }
//                }
//            }
//        ) { padding ->
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding)
//                    .verticalScroll(rememberScrollState())
//            ) {
//                // Title Field (Always visible at top)
//                OutlinedTextField(
//                    value = title,
//                    onValueChange = { title = it },
//                    placeholder = { Text("Title") },
//                    singleLine = true,
//                    enabled = !isDrawingMode, // Disable when drawing
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 16.dp)
//                        .padding(top = 16.dp),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedBorderColor = MaterialTheme.colorScheme.primary,
//                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
//                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
//                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
//                    )
//                )
//
//                Spacer(Modifier.height(12.dp))
//
//                // Dynamic Sections
//                sections.forEachIndexed { index, section ->
//                    when (section) {
//                        is Section.CanvasSection -> {
//                            // Calculate canvas height
//                            val canvasHeight = if (section.id == activeCanvasId ||
//                                section.canvasState.hasDrawn) {
//                                600.dp
//                            } else {
//                                // Full screen height for first empty canvas
//                                if (index == 0 && !section.canvasState.hasDrawn) {
//                                    600.dp
//                                } else {
//                                    600.dp
//                                }
//                            }
//
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .height(canvasHeight)
//                                    .padding(horizontal = 16.dp)
//                            ) {
//                                DrawingCanvas(
//                                    canvasState = section.canvasState,
//                                    drawColor = drawColor,
//                                    strokeWidth = strokeWidth,
//                                    currentTool = currentTool,
//                                    isDrawingMode = section.id == activeCanvasId,
//                                    isCanvasLocked = section.canvasState.hasDrawn && section.id != activeCanvasId,
//                                    recomposeTrigger = recomposeTrigger,
//                                    onRecompose = { recomposeTrigger++ },
//                                    onEnterDrawingMode = {
//                                        // Only enter drawing mode if not locked
//                                        if (!section.canvasState.hasDrawn || section.id == activeCanvasId) {
//                                            activeCanvasId = section.id
//                                        }
//                                    }
//                                )
//
//                                // Edit icon on top-left if canvas is locked (has been drawn and completed)
//                                if (section.canvasState.hasDrawn && section.id != activeCanvasId) {
//                                    IconButton(
//                                        onClick = {
//                                            activeCanvasId = section.id
//                                        },
//                                        modifier = Modifier
//                                            .align(Alignment.TopStart)
//                                            .padding(12.dp)
//                                            .size(24.dp)
//                                            .background(
//                                                Color.LightGray.copy(alpha = 0.4f),
//                                                RoundedCornerShape(6.dp)
//                                            )
//                                    ) {
//                                        Icon(
//                                            Icons.Default.Edit,
//                                            "Edit Canvas",
//                                            tint = Color.Black,
//                                            modifier = Modifier.size(18.dp)
//                                        )
//                                    }
//                                }
//
//                                // Delete icon on top-right if canvas has drawings
//                                if (section.canvasState.hasDrawn && section.id != activeCanvasId) {
//                                    IconButton(
//                                        onClick = {
//                                            sections.remove(section)
//                                        },
//                                        modifier = Modifier
//                                            .align(Alignment.TopEnd)
//                                            .padding(12.dp)
//                                            .size(24.dp)
//                                            .background(
//                                                Color.LightGray.copy(alpha = 0.4f),
//                                                RoundedCornerShape(6.dp)
//                                            )
//                                    ) {
//                                        Icon(
//                                            Icons.Default.Delete,
//                                            "Delete Canvas",
//                                            tint = Color.Black,
//                                            modifier = Modifier.size(18.dp)
//                                        )
//                                    }
//                                }
//                            }
//
//                            Spacer(Modifier.height(12.dp))
//                        }
//
//                        is Section.TextSection -> {
//                            // Borderless text field - ALWAYS VISIBLE, not animated
//                            Column(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(horizontal = 16.dp)
//                            ) {
//                                BasicTextField(
//                                    value = section.content,
//                                    onValueChange = { newValue ->
//                                        section.content = newValue
//                                    },
//                                    enabled = !isDrawingMode, // Disable when drawing
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .wrapContentHeight()
//                                        .background(
//                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
//                                            RoundedCornerShape(8.dp)
//                                        )
//                                        .padding(12.dp),
//                                    textStyle = TextStyle(
//                                        fontSize = 16.sp,
//                                        color = if (isDrawingMode)
//                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
//                                        else
//                                            MaterialTheme.colorScheme.onSurface,
//                                        lineHeight = 24.sp
//                                    ),
//                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
//                                    decorationBox = { innerTextField ->
//                                        Box(
//                                            modifier = Modifier.fillMaxWidth()
//                                        ) {
//                                            if (section.content.isEmpty() && !isDrawingMode) {
//                                                Text(
//                                                    "Add notes here...",
//                                                    style = TextStyle(
//                                                        fontSize = 16.sp,
//                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
//                                                            alpha = 0.5f
//                                                        )
//                                                    )
//                                                )
//                                            }
//                                            innerTextField()
//                                        }
//                                    }
//                                )
//                            }
//
//                            Spacer(Modifier.height(12.dp))
//                        }
//                    }
//                }
//
//                Spacer(Modifier.height(80.dp))
//            }
//        }
//    }
//
//// ----------------------------
////        DRAWING CANVAS
//// ----------------------------
//
//    @Composable
//    fun DrawingCanvas(
//        canvasState: CanvasState,
//        drawColor: Color,
//        strokeWidth: Float,
//        currentTool: DrawingTool,
//        isDrawingMode: Boolean,
//        isCanvasLocked: Boolean,
//        recomposeTrigger: Int,
//        onRecompose: () -> Unit,
//        onEnterDrawingMode: () -> Unit
//    ) {
//        val isEraser = currentTool == DrawingTool.ERASER
//
//        val effectiveColor = when (currentTool) {
//            DrawingTool.ERASER -> canvasState.bgColor
//            DrawingTool.HIGHLIGHTER -> drawColor.copy(alpha = 0.3f)
//            else -> drawColor
//        }
//
//        val effectiveWidth = when (currentTool) {
//            DrawingTool.ERASER -> strokeWidth * 2.5f
//            DrawingTool.MARKER -> strokeWidth * 1.3f
//            DrawingTool.HIGHLIGHTER -> strokeWidth * 2f
//            else -> strokeWidth
//        }
//
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .clip(RoundedCornerShape(8.dp))
//                .background(canvasState.bgColor)
//                .border(
//                    1.dp,
//                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
//                    RoundedCornerShape(8.dp)
//                )
//                .then(
//                    if (!isCanvasLocked) {
//                        Modifier.pointerInput(effectiveColor, effectiveWidth, isEraser, canvasState.bgColor,
//                            canvasState.showGrid, isDrawingMode) {
//                            detectDragGestures(
//                                onDragStart = { offset ->
//                                    // Enter drawing mode when user starts drawing
//                                    onEnterDrawingMode()
//
//                                    canvasState.redoStack.clear()
//                                    canvasState.currentPath.clear()
//                                    canvasState.currentPath.add(offset)
//                                    canvasState.currentColor = effectiveColor
//                                    canvasState.currentStrokeWidth = effectiveWidth
//                                    canvasState.currentIsEraser = isEraser
//                                    onRecompose()
//                                },
//                                onDrag = { change, _ ->
//                                    change.consume()
//                                    canvasState.currentPath.add(change.position)
//                                    onRecompose()
//                                },
//                                onDragEnd = {
//                                    if (canvasState.currentPath.size > 1) {
//                                        canvasState.paths.add(
//                                            DrawPath(
//                                                points = canvasState.currentPath.toList(),
//                                                color = canvasState.currentColor,
//                                                strokeWidth = canvasState.currentStrokeWidth,
//                                                isEraser = canvasState.currentIsEraser
//                                            )
//                                        )
//                                    }
//                                    canvasState.currentPath.clear()
//                                    onRecompose()
//                                }
//                            )
//                        }.pointerInput(effectiveColor, effectiveWidth, isEraser) {
//                            awaitPointerEventScope {
//                                while (true) {
//                                    val event = awaitPointerEvent()
//                                    event.changes.forEach { change ->
//                                        if (change.pressed && change.previousPressed.not()) {
//                                            // Single tap detected - draw a dot
//                                            onEnterDrawingMode()
//
//                                            val dotPosition = change.position
//                                            canvasState.redoStack.clear()
//
//                                            // Create a small circle as a dot
//                                            val dotPoints = listOf(
//                                                dotPosition,
//                                                Offset(dotPosition.x + 0.1f, dotPosition.y + 0.1f)
//                                            )
//
//                                            canvasState.paths.add(
//                                                DrawPath(
//                                                    points = dotPoints,
//                                                    color = effectiveColor,
//                                                    strokeWidth = effectiveWidth,
//                                                    isEraser = isEraser
//                                                )
//                                            )
//                                            onRecompose()
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    } else {
//                        Modifier
//                    }
//                )
//        ) {
//            key(recomposeTrigger) {
//                Canvas(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(Color.Transparent)
//                ) {
//                    // Draw grid if enabled
//                    if (canvasState.showGrid) {
//                        val gridSpacing = 50.dp.toPx()
//                        val gridColor = Color.Gray.copy(alpha = 0.2f)
//
//                        var x = gridSpacing
//                        while (x < size.width) {
//                            drawLine(
//                                color = gridColor,
//                                start = Offset(x, 0f),
//                                end = Offset(x, size.height),
//                                strokeWidth = 1f
//                            )
//                            x += gridSpacing
//                        }
//
//                        var y = gridSpacing
//                        while (y < size.height) {
//                            drawLine(
//                                color = gridColor,
//                                start = Offset(0f, y),
//                                end = Offset(size.width, y),
//                                strokeWidth = 1f
//                            )
//                            y += gridSpacing
//                        }
//                    }
//
//                    // Draw all saved paths
//                    canvasState.paths.forEach { pathObj ->
//                        if (pathObj.points.size > 1) {
//                            val path = Path()
//                            pathObj.points.forEachIndexed { i, point ->
//                                if (i == 0) {
//                                    path.moveTo(point.x, point.y)
//                                } else {
//                                    path.lineTo(point.x, point.y)
//                                }
//                            }
//
//                            val renderColor = if (pathObj.isEraser) {
//                                canvasState.bgColor
//                            } else {
//                                pathObj.color
//                            }
//
//                            drawPath(
//                                path = path,
//                                color = renderColor,
//                                style = Stroke(
//                                    width = pathObj.strokeWidth,
//                                    cap = StrokeCap.Round,
//                                    join = StrokeJoin.Round
//                                )
//                            )
//                        }
//                    }
//
//                    // Draw current path (real-time feedback)
//                    if (canvasState.currentPath.size > 1) {
//                        val path = Path()
//                        canvasState.currentPath.forEachIndexed { i, point ->
//                            if (i == 0) {
//                                path.moveTo(point.x, point.y)
//                            } else {
//                                path.lineTo(point.x, point.y)
//                            }
//                        }
//
//                        val currentRenderColor = if (canvasState.currentIsEraser) {
//                            canvasState.bgColor
//                        } else {
//                            canvasState.currentColor
//                        }
//
//                        drawPath(
//                            path = path,
//                            color = currentRenderColor,
//                            style = Stroke(
//                                width = canvasState.currentStrokeWidth,
//                                cap = StrokeCap.Round,
//                                join = StrokeJoin.Round
//                            )
//                        )
//                    }
//                }
//            }
//
//            // Show hint when canvas is empty and not in drawing mode
//            if (!isDrawingMode && canvasState.paths.isEmpty()) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .clickable(
//                            interactionSource = remember { MutableInteractionSource() },
//                            indication = null
//                        ) {
//                            onEnterDrawingMode()
//                        },
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        "Tap to start drawing",
//                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
//                        fontSize = 16.sp
//                    )
//                }
//            }
//        }
//    }
//
//// ----------------------------
////        BOTTOM TOOLBAR
//// ----------------------------
//
//    @Composable
//    fun DrawingBottomBar(
//        currentTool: DrawingTool,
//        drawColor: Color,
//        strokeWidth: Float,
//        canvasBgColor: Color,
//        showGridLines: Boolean,
//        showToolOptions: Boolean,
//        showColorPicker: Boolean,
//        showCanvasOptions: Boolean,
//        canUndo: Boolean,
//        canRedo: Boolean,
//        previousToolbarState: String?,
//        onToolChange: (DrawingTool) -> Unit,
//        onColorChange: (Color) -> Unit,
//        onStrokeWidthChange: (Float) -> Unit,
//        onCanvasBgChange: (Color) -> Unit,
//        onGridToggle: (Boolean) -> Unit,
//        onShowColorPicker: () -> Unit,
//        onShowCanvasOptions: () -> Unit,
//        onUndo: () -> Unit,
//        onRedo: () -> Unit,
//        onCloseSubPanel: () -> Unit,
//        onBackToToolOptions: () -> Unit
//    ) {
//        Surface(
//            color = MaterialTheme.colorScheme.surface,
//            tonalElevation = 3.dp,
//            shadowElevation = 8.dp
//        ) {
//            val showAnyOptions = showToolOptions || showColorPicker || showCanvasOptions
//
//            if (showAnyOptions) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 8.dp)
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = 4.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        IconButton(
//                            onClick = {
//                                if (showColorPicker || showCanvasOptions) {
//                                    when (previousToolbarState) {
//                                        "TOOL_OPTIONS" -> onBackToToolOptions()
//                                        else -> onCloseSubPanel()
//                                    }
//                                } else {
//                                    onCloseSubPanel()
//                                }
//                            },
//                            modifier = Modifier.size(40.dp)
//                        ) {
//                            Icon(
//                                if (showColorPicker || showCanvasOptions) Icons.Default.ArrowBack
//                                else Icons.Default.Close,
//                                "Close",
//                                tint = MaterialTheme.colorScheme.onSurface
//                            )
//                        }
//
//                        when {
//                            showToolOptions -> {
//                                ToolOptionsPanel(
//                                    currentTool = currentTool,
//                                    strokeWidth = strokeWidth,
//                                    drawColor = drawColor,
//                                    onStrokeWidthChange = onStrokeWidthChange,
//                                    onShowColorPicker = onShowColorPicker
//                                )
//                            }
//                            showColorPicker -> {
//                                ColorPickerPanel(
//                                    currentColor = drawColor,
//                                    onColorChange = onColorChange
//                                )
//                            }
//                            showCanvasOptions -> {
//                                CanvasOptionsPanel(
//                                    bgColor = canvasBgColor,
//                                    showGrid = showGridLines,
//                                    onBgColorChange = onCanvasBgChange,
//                                    onGridToggle = onGridToggle
//                                )
//                            }
//                        }
//                    }
//                }
//            } else {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 2.dp, vertical = 8.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(2.dp),
//                        modifier = Modifier.weight(1f)
//                    ) {
//                        IconButton(
//                            onClick = { onToolChange(DrawingTool.PEN) },
//                            colors = IconButtonDefaults.iconButtonColors(
//                                contentColor = if (currentTool == DrawingTool.PEN)
//                                    MaterialTheme.colorScheme.primary
//                                else MaterialTheme.colorScheme.onSurface
//                            ),
//                            modifier = Modifier.size(36.dp)
//                        ) {
//                            Icon(Icons.Default.BorderColor, "Pen", modifier = Modifier.size(20.dp))
//                        }
//
//                        IconButton(
//                            onClick = { onToolChange(DrawingTool.MARKER) },
//                            colors = IconButtonDefaults.iconButtonColors(
//                                contentColor = if (currentTool == DrawingTool.MARKER)
//                                    MaterialTheme.colorScheme.primary
//                                else MaterialTheme.colorScheme.onSurface
//                            ),
//                            modifier = Modifier.size(36.dp)
//                        ) {
//                            Icon(Icons.Default.Brush, "Marker", modifier = Modifier.size(20.dp))
//                        }
//
//                        IconButton(
//                            onClick = { onToolChange(DrawingTool.HIGHLIGHTER) },
//                            colors = IconButtonDefaults.iconButtonColors(
//                                contentColor = if (currentTool == DrawingTool.HIGHLIGHTER)
//                                    MaterialTheme.colorScheme.primary
//                                else MaterialTheme.colorScheme.onSurface
//                            ),
//                            modifier = Modifier.size(36.dp)
//                        ) {
//                            Icon(Icons.Default.Highlight, "Highlighter", modifier = Modifier.size(20.dp))
//                        }
//
//                        IconButton(
//                            onClick = { onToolChange(DrawingTool.ERASER) },
//                            colors = IconButtonDefaults.iconButtonColors(
//                                contentColor = if (currentTool == DrawingTool.ERASER)
//                                    MaterialTheme.colorScheme.primary
//                                else MaterialTheme.colorScheme.onSurface
//                            ),
//                            modifier = Modifier.size(36.dp)
//                        ) {
//                            Icon(Icons.Default.AutoFixOff, "Eraser", modifier = Modifier.size(20.dp))
//                        }
//
//                        IconButton(
//                            onClick = onShowCanvasOptions,
//                            modifier = Modifier.size(36.dp)
//                        ) {
//                            Icon(
//                                Icons.Default.Palette,
//                                "Canvas",
//                                tint = MaterialTheme.colorScheme.onSurface,
//                                modifier = Modifier.size(20.dp)
//                            )
//                        }
//                    }
//
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(2.dp)
//                    ) {
//                        IconButton(
//                            onClick = onUndo,
//                            enabled = canUndo,
//                            modifier = Modifier.size(36.dp)
//                        ) {
//                            Icon(
//                                Icons.Default.Undo,
//                                "Undo",
//                                tint = if (canUndo)
//                                    MaterialTheme.colorScheme.onSurface
//                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
//                                modifier = Modifier.size(20.dp)
//                            )
//                        }
//
//                        IconButton(
//                            onClick = onRedo,
//                            enabled = canRedo,
//                            modifier = Modifier.size(36.dp)
//                        ) {
//                            Icon(
//                                Icons.Default.Redo,
//                                "Redo",
//                                tint = if (canRedo)
//                                    MaterialTheme.colorScheme.onSurface
//                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
//                                modifier = Modifier.size(20.dp)
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//// ----------------------------
////        TOOL OPTIONS PANEL
//// ----------------------------
//
//    @Composable
//    fun RowScope.ToolOptionsPanel(
//        currentTool: DrawingTool,
//        strokeWidth: Float,
//        drawColor: Color,
//        onStrokeWidthChange: (Float) -> Unit,
//        onShowColorPicker: () -> Unit
//    ) {
//        LazyRow(
//            modifier = Modifier
//                .weight(1f)
//                .height(48.dp),
//            horizontalArrangement = Arrangement.spacedBy(12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            item {
//                Text(
//                    when (currentTool) {
//                        DrawingTool.PEN -> "Pen"
//                        DrawingTool.MARKER -> "Marker"
//                        DrawingTool.HIGHLIGHTER -> "Highlighter"
//                        DrawingTool.ERASER -> "Eraser"
//                    },
//                    fontSize = 14.sp,
//                    fontWeight = FontWeight.Medium
//                )
//            }
//
//            item {
//                Column(modifier = Modifier.width(200.dp)) {
//                    Text("Size: ${strokeWidth.toInt()}px", fontSize = 12.sp)
//                    Slider(
//                        value = strokeWidth,
//                        onValueChange = onStrokeWidthChange,
//                        valueRange = if (currentTool == DrawingTool.ERASER) 10f..60f else 2f..40f,
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                }
//            }
//
//            if (currentTool != DrawingTool.ERASER) {
//                item {
//                    IconButton(
//                        onClick = onShowColorPicker,
//                        modifier = Modifier.size(40.dp)
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(32.dp)
//                                .background(drawColor, CircleShape)
//                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
//                        )
//                    }
//                }
//            }
//        }
//    }
//
//// ----------------------------
////        COLOR PICKER PANEL
//// ----------------------------
//
//    @Composable
//    fun RowScope.ColorPickerPanel(
//        currentColor: Color,
//        onColorChange: (Color) -> Unit
//    ) {
//        LazyRow(
//            modifier = Modifier
//                .weight(1f)
//                .height(48.dp),
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            val colors = listOf(
//                Color.Black, Color.White, Color.Red, Color(0xFFE91E63),
//                Color(0xFF9C27B0), Color(0xFF673AB7), Color.Blue, Color(0xFF03A9F4),
//                Color(0xFF00BCD4), Color(0xFF009688), Color.Green, Color(0xFF8BC34A),
//                Color(0xFFCDDC39), Color.Yellow, Color(0xFFFF9800), Color(0xFFFF5722),
//                Color(0xFF795548), Color.Gray, Color.DarkGray
//            )
//
//            items(colors) { color ->
//                Box(
//                    modifier = Modifier
//                        .size(36.dp)
//                        .clickable(
//                            interactionSource = remember { MutableInteractionSource() },
//                            indication = null
//                        ) {
//                            onColorChange(color)
//                        }
//                        .border(
//                            width = if (color == currentColor) 3.dp else 1.dp,
//                            color = if (color == currentColor)
//                                MaterialTheme.colorScheme.primary
//                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
//                            shape = CircleShape
//                        )
//                        .padding(if (color == currentColor) 0.dp else 2.dp)
//                        .background(color, CircleShape)
//                ) {
//                    if (color == Color.White) {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .border(1.dp, Color.LightGray, CircleShape)
//                        )
//                    }
//                }
//            }
//        }
//    }
//
//// ----------------------------
////        CANVAS OPTIONS PANEL
//// ----------------------------
//
//    @Composable
//    fun RowScope.CanvasOptionsPanel(
//        bgColor: Color,
//        showGrid: Boolean,
//        onBgColorChange: (Color) -> Unit,
//        onGridToggle: (Boolean) -> Unit
//    ) {
//        LazyRow(
//            modifier = Modifier
//                .weight(1f)
//                .height(48.dp),
//            horizontalArrangement = Arrangement.spacedBy(12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            item {
//                Text("Canvas", fontSize = 14.sp, fontWeight = FontWeight.Medium)
//            }
//
//            val canvasColors = listOf(
//                Color.White, Color(0xFFF5F5F5), Color(0xFFE8F5E9),
//                Color(0xFFE3F2FD), Color(0xFFFFF8E1), Color(0xFFFCE4EC),
//                Color(0xFFF3E5F5), Color.Black
//            )
//
//            items(canvasColors) { color ->
//                Box(
//                    modifier = Modifier
//                        .size(36.dp)
//                        .clickable(
//                            interactionSource = remember { MutableInteractionSource() },
//                            indication = null
//                        ) {
//                            onBgColorChange(color)
//                        }
//                        .border(
//                            width = if (color == bgColor) 3.dp else 1.dp,
//                            color = if (color == bgColor)
//                                MaterialTheme.colorScheme.primary
//                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
//                            shape = CircleShape
//                        )
//                        .padding(if (color == bgColor) 0.dp else 2.dp)
//                        .background(color, CircleShape)
//                ) {
//                    if (color == Color.White || color == Color(0xFFF5F5F5)) {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .border(1.dp, Color.LightGray, CircleShape)
//                        )
//                    }
//                }
//            }
//
//            item {
//                Spacer(Modifier.width(8.dp))
//            }
//
//            item {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(8.dp),
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(8.dp))
//                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
//                        .padding(horizontal = 12.dp, vertical = 8.dp)
//                ) {
//                    Icon(
//                        Icons.Default.GridOn,
//                        contentDescription = "Grid",
//                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
//                        modifier = Modifier.size(20.dp)
//                    )
//                    Text(
//                        "Show Grid",
//                        fontSize = 13.sp,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                    Switch(
//                        checked = showGrid,
//                        onCheckedChange = onGridToggle,
//                        modifier = Modifier.height(24.dp)
//                    )
//                }
//            }
//        }
//    }