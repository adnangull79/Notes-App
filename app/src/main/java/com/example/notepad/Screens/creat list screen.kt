package com.example.notepad.Screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notepad.CreatNotes1.ColorPickerDialog
import com.example.notepad.CreatNotes1.FormatState

import com.example.notepad.FolderEntity
import com.example.notepad.NoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Drag & Drop State - manages dragging operations
 */
class DragDropState {
    var draggedIndex by mutableStateOf<Int?>(null)
    var dragOffset by mutableStateOf(0f)
    var targetIndex by mutableStateOf<Int?>(null)
    private var isDragging by mutableStateOf(false)

    fun startDrag(index: Int) {
        draggedIndex = index
        dragOffset = 0f
        targetIndex = index
        isDragging = true
    }

    fun updateDrag(offset: Float, itemHeight: Float, itemCount: Int) {
        if (!isDragging) return
        draggedIndex?.let { currentIndex ->
            dragOffset = offset
            val offsetInItems = (offset / itemHeight).toInt()
            val newTarget = (currentIndex + offsetInItems).coerceIn(0, itemCount - 1)
            targetIndex = newTarget
        }
    }

    fun endDrag(): Pair<Int, Int>? {
        if (!isDragging) return null
        val from = draggedIndex
        val to = targetIndex
        reset()
        return if (from != null && to != null && from != to) {
            Pair(from, to)
        } else null
    }

    fun reset() {
        isDragging = false
        draggedIndex = null
        dragOffset = 0f
        targetIndex = null
    }

    fun isCurrentlyDragging() = isDragging
}

/**
 * Enhanced ChecklistItem with formatting
 */
data class ChecklistItemWithFormat(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val checked: Boolean = false,
    val order: Long = 0L,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val textColor: Color = Color.Unspecified,
    val highlightColor: Color = Color.Unspecified
)

/**
 * History for undo/redo - tracks TEXT CHANGES only
 */
data class ChecklistHistory(
    val itemId: String,
    val previousText: String,
    val newText: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Parse checklist items from saved content
 */
private fun parseChecklistItems(content: String): List<ChecklistItemWithFormat> {
    return content.lines()
        .filter { it.isNotBlank() }
        .mapIndexed { index, line ->
            val checked = line.trim().startsWith("[x]") || line.trim().startsWith("[X]")
            val text = line.trim()
                .removePrefix("[x]")
                .removePrefix("[X]")
                .removePrefix("[ ]")
                .trim()
            ChecklistItemWithFormat(
                text = text,
                checked = checked,
                order = index.toLong()
            )
        }
}

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

    var title by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<ChecklistItemWithFormat>() }
    val itemFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }

    var isLoading by remember { mutableStateOf(noteId != null) }
    var isFavorite by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<FolderEntity?>(null) }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }

    // Formatting state
    var showTextFormatOptions by remember { mutableStateOf(false) }
    var showTextColorBar by remember { mutableStateOf(false) }
    var showHighlightColorBar by remember { mutableStateOf(false) }
    var previousToolbarState by remember { mutableStateOf<String?>(null) }
    var formatState by remember { mutableStateOf(FormatState()) }

    // Bottom bar visibility
    var showBottomBar by remember { mutableStateOf(true) }

    // Focus tracking - to know which item is currently focused
    var focusedItemId by remember { mutableStateOf<String?>(null) }

    // Background color
    val defaultNoteBgColor = MaterialTheme.colorScheme.background
    var noteBackgroundColor by remember { mutableStateOf(defaultNoteBgColor) }

    val textRenderColor = MaterialTheme.colorScheme.onSurface
    val bottomBarIconColor = MaterialTheme.colorScheme.onSurface

    val dragDropState = remember { DragDropState() }
    val itemHeightPx = with(LocalDensity.current) { 80.dp.toPx() }

    // Undo/Redo history - TEXT CHANGES ONLY
    var historyList by remember { mutableStateOf(listOf<ChecklistHistory>()) }
    var historyIndex by remember { mutableStateOf(-1) }

    fun pushHistory(itemId: String, oldText: String, newText: String) {
        if (oldText == newText) return

        val snapshot = ChecklistHistory(
            itemId = itemId,
            previousText = oldText,
            newText = newText
        )
        val newHistory = historyList.take(historyIndex + 1) + snapshot
        historyList = newHistory.takeLast(50)
        historyIndex = historyList.lastIndex
    }

    // Auto-save functionality with proper order tracking
    LaunchedEffect(title, items.toList()) {
        if (!isLoading && noteId != null && title.isNotBlank()) {
            delay(1000)
            items.forEachIndexed { index, item ->
                items[index] = item.copy(order = index.toLong())
            }
            val content = items.joinToString("\n") {
                if (it.checked) "[x] ${it.text.trim()}" else "[ ] ${it.text.trim()}"
            }
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

                    val parsed = parseChecklistItems(n.content ?: "")
                    items.clear()
                    items.addAll(parsed)
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

    // Handle reordering
    fun handleReorder(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)

        items.forEachIndexed { index, checklistItem ->
            items[index] = checklistItem.copy(order = index.toLong())
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
                            items.forEachIndexed { index, item ->
                                items[index] = item.copy(order = index.toLong())
                            }
                            val content = items.joinToString("\n") {
                                if (it.checked) "[x] ${it.text.trim()}" else "[ ] ${it.text.trim()}"
                            }
                            val folderId = selectedFolder?.id ?: folders.firstOrNull()?.id ?: 1
                            noteViewModel.createNote(
                                folderId = folderId,
                                title = title,
                                content = content
                            )
                        }
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }

                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
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
                            leadingIcon = {
                                Icon(
                                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Print") },
                            onClick = { showMoreMenu = false },
                            leadingIcon = { Icon(Icons.Default.Print, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMoreMenu = false
                                if (noteId != null) {
                                    noteViewModel.moveToTrash(noteId)
                                    onNavigateBack()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    val showAnyFormatOptions = showTextFormatOptions || showTextColorBar || showHighlightColorBar

                    if (showAnyFormatOptions) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (showTextColorBar || showHighlightColorBar) {
                                        showTextColorBar = false
                                        showHighlightColorBar = false
                                        when (previousToolbarState) {
                                            "TEXT_FORMAT" -> showTextFormatOptions = true
                                            else -> showTextFormatOptions = false
                                        }
                                        previousToolbarState = null
                                    } else {
                                        showTextFormatOptions = false
                                        previousToolbarState = null
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Close, "Close", tint = bottomBarIconColor)
                            }

                            LazyRow(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showTextFormatOptions) {
                                    item {
                                        IconButton(
                                            onClick = {
                                                formatState = formatState.copy(isBold = !formatState.isBold)
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                contentColor = if (formatState.isBold) MaterialTheme.colorScheme.primary else bottomBarIconColor
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.FormatBold, "Bold", modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    item {
                                        IconButton(
                                            onClick = {
                                                formatState = formatState.copy(isItalic = !formatState.isItalic)
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                contentColor = if (formatState.isItalic) MaterialTheme.colorScheme.primary else bottomBarIconColor
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.FormatItalic, "Italic", modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    item {
                                        IconButton(
                                            onClick = {
                                                formatState = formatState.copy(isUnderline = !formatState.isUnderline)
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                contentColor = if (formatState.isUnderline) MaterialTheme.colorScheme.primary else bottomBarIconColor
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.FormatUnderlined, "Underline", modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    item {
                                        IconButton(
                                            onClick = {
                                                formatState = formatState.copy(isStrikethrough = !formatState.isStrikethrough)
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                contentColor = if (formatState.isStrikethrough) MaterialTheme.colorScheme.primary else bottomBarIconColor
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.StrikethroughS, "Strike", modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    item {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    previousToolbarState = "TEXT_FORMAT"
                                                    showTextColorBar = true
                                                    showHighlightColorBar = false
                                                    showTextFormatOptions = false
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Icon(
                                                        Icons.Default.FormatColorText,
                                                        "Text color",
                                                        tint = bottomBarIconColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .width(20.dp)
                                                            .height(2.dp)
                                                            .background(
                                                                color = if (formatState.textColor != Color.Unspecified)
                                                                    formatState.textColor
                                                                else
                                                                    bottomBarIconColor
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    previousToolbarState = "TEXT_FORMAT"
                                                    showHighlightColorBar = true
                                                    showTextColorBar = false
                                                    showTextFormatOptions = false
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(24.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Highlight,
                                                        contentDescription = "Highlight",
                                                        tint = bottomBarIconColor,
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .offset(x = (-1).dp, y = (-1).dp)
                                                    )

                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .align(Alignment.BottomEnd)
                                                            .offset(x = 2.dp, y = 2.dp)
                                                            .background(
                                                                color = if (formatState.highlightColor != Color.Unspecified)
                                                                    formatState.highlightColor
                                                                else
                                                                    Color(0xFFFFEB3B).copy(alpha = 0.4f),
                                                                shape = RoundedCornerShape(2.dp)
                                                            )
                                                            .border(
                                                                width = 0.5.dp,
                                                                color = bottomBarIconColor.copy(alpha = 0.3f),
                                                                shape = RoundedCornerShape(2.dp)
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                } else if (showTextColorBar) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    formatState = formatState.copy(textColor = Color.Unspecified)
                                                }
                                                .border(
                                                    width = if (formatState.textColor == Color.Unspecified) 2.dp else 1.dp,
                                                    color = if (formatState.textColor == Color.Unspecified) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                                    },
                                                    shape = CircleShape
                                                )
                                                .background(Color.Transparent, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.FormatColorReset,
                                                "Default Color",
                                                tint = bottomBarIconColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    item { Spacer(Modifier.width(4.dp)) }

                                    val textColors = listOf(
                                        Color.Black,
                                        Color(0xFFE53935),
                                        Color(0xFF1E88E5),
                                        Color(0xFF43A047),
                                        Color(0xFFFFB300),
                                        Color(0xFF8E24AA),
                                        Color(0xFF00ACC1),
                                        Color(0xFFFF6F00)
                                    )

                                    items(textColors) { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    formatState = formatState.copy(textColor = color)
                                                }
                                                .border(
                                                    width = if (color == formatState.textColor) 2.dp else 0.dp,
                                                    color = if (color == formatState.textColor) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                    shape = CircleShape
                                                )
                                                .background(color, CircleShape)
                                        )
                                    }

                                } else if (showHighlightColorBar) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    formatState = formatState.copy(highlightColor = Color.Unspecified)
                                                }
                                                .border(
                                                    width = if (formatState.highlightColor == Color.Unspecified) 2.dp else 1.dp,
                                                    color = if (formatState.highlightColor == Color.Unspecified) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                                    },
                                                    shape = CircleShape
                                                )
                                                .background(Color.Transparent, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.FormatColorReset,
                                                "No Highlight",
                                                tint = bottomBarIconColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    item { Spacer(Modifier.width(4.dp)) }

                                    val highlightColors = listOf(
                                        Color(0xFFFFEB3B).copy(alpha = 0.4f),
                                        Color(0xFF4CAF50).copy(alpha = 0.3f),
                                        Color(0xFF2196F3).copy(alpha = 0.3f),
                                        Color(0xFFFF9800).copy(alpha = 0.4f),
                                        Color(0xFFE91E63).copy(alpha = 0.3f),
                                        Color(0xFF9C27B0).copy(alpha = 0.3f),
                                        Color(0xFF00BCD4).copy(alpha = 0.3f),
                                        Color(0xFFCDDC39).copy(alpha = 0.4f)
                                    )

                                    items(highlightColors) { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    formatState = formatState.copy(highlightColor = color)
                                                }
                                                .border(
                                                    width = if (color == formatState.highlightColor) 2.dp else 0.dp,
                                                    color = if (color == formatState.highlightColor) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                    shape = CircleShape
                                                )
                                                .background(color, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = { showAddSheet = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, "Add options", tint = bottomBarIconColor, modifier = Modifier.size(20.dp))
                                }

                                IconButton(
                                    onClick = { showTextFormatOptions = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.TextFields, "Text format", tint = bottomBarIconColor, modifier = Modifier.size(20.dp))
                                }

                                IconButton(
                                    onClick = { showColorPicker = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Palette, "Note Color", tint = bottomBarIconColor, modifier = Modifier.size(20.dp))
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (historyIndex > 0) {
                                            historyIndex--
                                            val snapshot = historyList[historyIndex]
                                            val itemIndex = items.indexOfFirst { it.id == snapshot.itemId }
                                            if (itemIndex != -1) {
                                                items[itemIndex] = items[itemIndex].copy(text = snapshot.previousText)
                                            }
                                        }
                                    },
                                    enabled = historyIndex > 0,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Undo,
                                        "Undo",
                                        tint = if (historyIndex > 0) bottomBarIconColor else bottomBarIconColor.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (historyIndex < historyList.lastIndex) {
                                            historyIndex++
                                            val snapshot = historyList[historyIndex]
                                            val itemIndex = items.indexOfFirst { it.id == snapshot.itemId }
                                            if (itemIndex != -1) {
                                                items[itemIndex] = items[itemIndex].copy(text = snapshot.newText)
                                            }
                                        }
                                    },
                                    enabled = historyIndex < historyList.lastIndex,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Redo,"Redo",
                                        tint = if (historyIndex < historyList.lastIndex) bottomBarIconColor else bottomBarIconColor.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showBottomBar = false },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Hide Bar", tint = bottomBarIconColor, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
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
                                items = items,
                                key = { _, item -> item.id }
                            ) { index, item ->
                                if (!itemFocusRequesters.containsKey(item.id)) {
                                    itemFocusRequesters[item.id] = FocusRequester()
                                }

                                ChecklistItemRow(
                                    item = item,
                                    index = index,
                                    dragDropState = dragDropState,
                                    itemHeight = itemHeightPx,
                                    itemCount = items.size,
                                    focusRequester = itemFocusRequesters[item.id]!!,
                                    formatState = formatState,
                                    textRenderColor = textRenderColor,
                                    isFocused = focusedItemId == item.id,
                                    onFocusChanged = { focused ->
                                        focusedItemId = if (focused) item.id else null
                                    },
                                    onCheckedChange = { checked ->
                                        items[index] = item.copy(checked = checked)
                                    },
                                    onTextChange = { newText ->
                                        val oldText = item.text

                                        if (newText.contains("\n")) {
                                            val cleanText = newText.replace("\n", "").trim()
                                            items[index] = item.copy(
                                                text = cleanText,
                                                isBold = formatState.isBold,
                                                isItalic = formatState.isItalic,
                                                isUnderline = formatState.isUnderline,
                                                isStrikethrough = formatState.isStrikethrough,
                                                textColor = formatState.textColor,
                                                highlightColor = formatState.highlightColor
                                            )

                                            if (oldText != cleanText) {
                                                pushHistory(item.id, oldText, cleanText)
                                            }

                                            val newItem = ChecklistItemWithFormat(
                                                text = "",
                                                checked = false,
                                                order = (index + 1).toLong(),
                                                isBold = formatState.isBold,
                                                isItalic = formatState.isItalic,
                                                isUnderline = formatState.isUnderline,
                                                isStrikethrough = formatState.isStrikethrough,
                                                textColor = formatState.textColor,
                                                highlightColor = formatState.highlightColor
                                            )
                                            val insertIndex = index + 1
                                            items.add(insertIndex, newItem)

                                            for (i in insertIndex until items.size) {
                                                items[i] = items[i].copy(order = i.toLong())
                                            }

                                            itemFocusRequesters[newItem.id] = FocusRequester()

                                            scope.launch {
                                                delay(50)
                                                itemFocusRequesters[newItem.id]?.requestFocus()
                                                listState.animateScrollToItem(
                                                    index = (insertIndex).coerceAtMost(items.size - 1)
                                                )
                                            }
                                        } else {
                                            items[index] = item.copy(
                                                text = newText,
                                                isBold = formatState.isBold,
                                                isItalic = formatState.isItalic,
                                                isUnderline = formatState.isUnderline,
                                                isStrikethrough = formatState.isStrikethrough,
                                                textColor = formatState.textColor,
                                                highlightColor = formatState.highlightColor
                                            )

                                            if (oldText != newText) {
                                                pushHistory(item.id, oldText, newText)
                                            }
                                        }
                                    },
                                    onDelete = {
                                        items.removeAt(index)
                                        itemFocusRequesters.remove(item.id)
                                    },
                                    onReorder = ::handleReorder,
                                    onFormatChange = { updatedItem ->
                                        items[index] = updatedItem
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
                                            val newItem = ChecklistItemWithFormat(
                                                text = "",
                                                checked = false,
                                                order = items.size.toLong(),
                                                isBold = formatState.isBold,
                                                isItalic = formatState.isItalic,
                                                isUnderline = formatState.isUnderline,
                                                isStrikethrough = formatState.isStrikethrough,
                                                textColor = formatState.textColor,
                                                highlightColor = formatState.highlightColor
                                            )
                                            items.add(newItem)
                                            itemFocusRequesters[newItem.id] = FocusRequester()

                                            scope.launch {
                                                delay(100)
                                                itemFocusRequesters[newItem.id]?.requestFocus()
                                                listState.animateScrollToItem(items.size - 1)
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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    AnimatedVisibility(
                        visible = !showBottomBar,
                        enter = slideInVertically(
                            initialOffsetY = { 80 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 400)
                        ) + scaleIn(
                            initialScale = 0.8f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { 80 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 250)
                        ) + scaleOut(
                            targetScale = 0.8f,
                            animationSpec = tween(durationMillis = 250)
                        )
                    ) {
                        FloatingActionButton(
                            onClick = { showBottomBar = true },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 12.dp,
                                hoveredElevation = 10.dp
                            ),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Show Formatting Bar",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
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

@Composable
private fun ChecklistItemRow(
    item: ChecklistItemWithFormat,
    index: Int,
    dragDropState: DragDropState,
    itemHeight: Float,
    itemCount: Int,
    focusRequester: FocusRequester,
    formatState: FormatState,
    textRenderColor: Color,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    onFormatChange: (ChecklistItemWithFormat) -> Unit
) {
    val isDragging = dragDropState.draggedIndex == index
    val draggedIdx = dragDropState.draggedIndex
    val targetIdx = dragDropState.targetIndex

    // Apply formatting ONLY when THIS item is focused
    LaunchedEffect(
        isFocused,
        formatState.isBold,
        formatState.isItalic,
        formatState.isUnderline,
        formatState.isStrikethrough,
        formatState.textColor,
        formatState.highlightColor
    ) {
        if (isFocused && item.text.isNotEmpty() && !isDragging) {
            val updated = item.copy(
                isBold = formatState.isBold,
                isItalic = formatState.isItalic,
                isUnderline = formatState.isUnderline,
                isStrikethrough = formatState.isStrikethrough,
                textColor = formatState.textColor,
                highlightColor = formatState.highlightColor
            )
            if (updated != item) {
                onFormatChange(updated)
            }
        }
    }

    val shouldDisplace = draggedIdx != null && targetIdx != null && !isDragging
    val displacementValue = remember(draggedIdx, targetIdx, index) {
        when {
            !shouldDisplace -> 0f
            draggedIdx != null && targetIdx != null && draggedIdx < targetIdx &&
                    index > draggedIdx && index <= targetIdx -> -itemHeight
            draggedIdx != null && targetIdx != null && draggedIdx > targetIdx &&
                    index >= targetIdx && index < draggedIdx -> itemHeight
            else -> 0f
        }
    }

    val displacement by animateFloatAsState(
        targetValue = displacementValue,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "displacement"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (isDragging) dragDropState.dragOffset else 0f,
        animationSpec = if (isDragging) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "offsetY"
    )

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "elevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isDragging) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "alpha"
    )

    val textStyle = LocalTextStyle.current.copy(
        fontSize = 16.sp,
        fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (item.isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = when {
            item.checked -> TextDecoration.LineThrough
            item.isUnderline && item.isStrikethrough -> TextDecoration.combine(
                listOf(TextDecoration.Underline, TextDecoration.LineThrough)
            )
            item.isUnderline -> TextDecoration.Underline
            item.isStrikethrough -> TextDecoration.LineThrough
            else -> TextDecoration.None
        },
        color = when {
            item.checked -> textRenderColor.copy(alpha = 0.5f)
            item.textColor != Color.Unspecified -> item.textColor
            else -> textRenderColor
        },
        background = if (item.highlightColor != Color.Unspecified) item.highlightColor else Color.Transparent
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = if (isDragging) offsetY else displacement
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .zIndex(if (isDragging) 1f else 0f),
        color = when {
            isDragging -> MaterialTheme.colorScheme.surfaceVariant
            targetIdx == index && draggedIdx != null -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(8.dp),
        shadowElevation = elevation
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (isDragging) 1f else 0.5f
                ),
                modifier = Modifier
                    .size(32.dp)
                    .padding(4.dp)
                    .pointerInput(index, item.id) {
                        detectDragGestures(
                            onDragStart = {
                                dragDropState.startDrag(index)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (dragDropState.isCurrentlyDragging()) {
                                    dragDropState.updateDrag(
                                        dragDropState.dragOffset + dragAmount.y,
                                        itemHeight,
                                        itemCount
                                    )
                                }
                            },
                            onDragEnd = {
                                val result = dragDropState.endDrag()
                                result?.let { (from, to) ->
                                    onReorder(from, to)
                                }
                            },
                            onDragCancel = {
                                dragDropState.reset()
                            }
                        )
                    }
            )

            Spacer(modifier = Modifier.width(4.dp))

            Checkbox(
                checked = item.checked,
                onCheckedChange = { onCheckedChange(it) },
                enabled = !isDragging,
                colors = CheckboxDefaults.colors(
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 120.dp)
            ) {
                TextField(
                    value = item.text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .verticalScroll(rememberScrollState())
                        .onFocusChanged { focusState ->
                            onFocusChanged(focusState.isFocused)
                        },
                    enabled = !isDragging,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            onTextChange(item.text + "\n")
                        }
                    ),
                    placeholder = {
                        Text(
                            "List item",
                            style = LocalTextStyle.current.copy(
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    },
                    textStyle = textStyle,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                enabled = !isDragging,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove item",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
