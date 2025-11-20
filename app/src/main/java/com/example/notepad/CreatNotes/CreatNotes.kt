//
//package com.example.notepad.CreatNotes
//
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.animation.*
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.focus.FocusRequester
//import androidx.compose.ui.focus.focusRequester
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.platform.LocalSoftwareKeyboardController
//import androidx.compose.ui.text.SpanStyle
//import androidx.compose.ui.text.TextRange
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.TextFieldValue
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.notepad.FolderEntity
//import com.example.notepad.NoteType
//import com.example.notepad.NoteViewModel
//import com.example.notepad.UI_theme.CategoryKey
//import com.example.notepad.UI_theme.categoryContainerColor
//import com.example.notepad.UI_theme.categoryIconColor
//import kotlinx.coroutines.launch
//import kotlin.math.max
//import kotlin.math.min
//
//// -----------------------------------------
//// --- Main Screen Composable ---
//// -----------------------------------------
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CreateNoteScreen(
//    onNavigateBack: () -> Unit,
//    noteViewModel: NoteViewModel = viewModel(),
//    noteId: Int? = null,
//    noteType: NoteType = NoteType.TEXT
//) {
//    val titleFocusRequester = remember { FocusRequester() }
//    val keyboardController = LocalSoftwareKeyboardController.current
//
//    val defaultNoteBgColor = MaterialTheme.colorScheme.background
//    var noteBackgroundColor by remember { mutableStateOf(defaultNoteBgColor) }
//    val textRenderColor = MaterialTheme.colorScheme.onSurface
//    val bottomBarIconColor = MaterialTheme.colorScheme.onSurface
//
//    var title by remember { mutableStateOf("") }
//    var contentText by remember { mutableStateOf(TextFieldValue("")) }
//    var selectedFolder by remember { mutableStateOf<FolderEntity?>(null) }
//
//    var showFolderSheet by remember { mutableStateOf(false) }
//    var showCreateCategorySheet by remember { mutableStateOf(false) }
//    var showColorPicker by remember { mutableStateOf(false) }
//    var showMoreMenu by remember { mutableStateOf(false) }
//    var showAddSheet by remember { mutableStateOf(false) }
//    var showTextFormatOptions by remember { mutableStateOf(false) }
//    var showParagraphFormatOptions by remember { mutableStateOf(false) }
//    var showBottomBar by remember { mutableStateOf(true) }
//
//    var showTextColorBar by remember { mutableStateOf(false) }
//    var showHighlightColorBar by remember { mutableStateOf(false) }
//    var previousToolbarState by remember { mutableStateOf<String?>(null) }
//
//    var currentNoteType by remember { mutableStateOf(noteType) }
//    var isContentFocused by remember { mutableStateOf(false) }
//    val isEditMode = noteId != null
//    var isLoading by remember { mutableStateOf(noteId != null) }
//    var isFavorite by remember { mutableStateOf(false) }
//
//    var formatState by remember { mutableStateOf(FormatState()) }
//    var formatMap by remember { mutableStateOf(listOf<FormatSpan>()) }
//
//    var checklistItems by remember { mutableStateOf(listOf<ChecklistItem>()) }
//
//    // Undo/Redo history
//    var historyList by remember { mutableStateOf(listOf<TextHistory>()) }
//    var historyIndex by remember { mutableStateOf(-1) }
//
//    val folders by noteViewModel.folders.collectAsState()
//    val scope = rememberCoroutineScope()
//    var pendingNewFolderName by remember { mutableStateOf<String?>(null) }
//
//    fun pushHistory(value: TextFieldValue, spans: List<FormatSpan>, state: FormatState) {
//        val snapshot = TextHistory(value.copy(), spans.map { it.copy() }, state.copy())
//        val newHistory = historyList.take(historyIndex + 1) + snapshot
//        historyList = newHistory.takeLast(100)
//        historyIndex = historyList.lastIndex
//    }
//
//    fun pushContentHistory() {
//        pushHistory(contentText, formatMap, formatState)
//    }
//
//    fun applyFormatting(start: Int, end: Int, newStyle: SpanStyle) {
//        if (start >= end) return
//
//        val newFormatList = formatMap.toMutableList()
//
//        // ✅ Remove ALL overlapping spans first
//        newFormatList.removeAll { it.start < end && it.end > start }
//
//        // ✅ Add the new style
//        newFormatList.add(FormatSpan(start, end, newStyle))
//
//        formatMap = newFormatList.filter { it.end > it.start }.sortedBy { it.start }
//        pushContentHistory()
//    }
//
//    // Load note
//    LaunchedEffect(noteId) {
//        if (noteId != null) {
//            scope.launch {
//                val existingNote = noteViewModel.getNoteById(noteId)
//                if (existingNote != null) {
//                    title = existingNote.title
//                    val initialContent = TextFieldValue(existingNote.content ?: "")
//                    contentText = initialContent
//                    isFavorite = existingNote.isFavorite
//
//                    historyList = emptyList()
//                    historyIndex = -1
//                    pushContentHistory()
//                }
//                isLoading = false
//            }
//        } else {
//            historyList = emptyList()
//            historyIndex = -1
//            pushContentHistory()
//        }
//    }
//
//    LaunchedEffect(Unit) { noteViewModel.loadFolders() }
//    LaunchedEffect(Unit) {
//        kotlinx.coroutines.delay(150)
//        titleFocusRequester.requestFocus()
//        keyboardController?.show()
//    }
//
//    LaunchedEffect(folders, noteId) {
//        if (noteId != null) {
//            scope.launch {
//                val existingNote = noteViewModel.getNoteById(noteId)
//                existingNote?.let { n ->
//                    selectedFolder = folders.find { it.id == n.folderId }
//                    if (currentNoteType == NoteType.LIST) {
//                        checklistItems = parseChecklistFromContent(n.content ?: "")
//                    }
//                }
//            }
//        }
//    }
//
//    LaunchedEffect(folders) {
//        if (!isEditMode && selectedFolder == null && folders.isNotEmpty()) {
//            selectedFolder = folders.first()
//        }
//        pendingNewFolderName?.let { name ->
//            val match = folders.lastOrNull { it.name == name } ?: folders.find { it.name == name }
//            if (match != null) {
//                selectedFolder = match
//                pendingNewFolderName = null
//            }
//        }
//    }
//
//    Scaffold(
//        containerColor = noteBackgroundColor,
//        topBar = {
//            TopAppBar(
//                windowInsets = WindowInsets(0, 0, 0, 0),
//                title = {
//                    Text(
//                        text = if (isEditMode) "Edit Note" else "Create Note",
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        color = MaterialTheme.colorScheme.onBackground
//                    )
//                },
//                navigationIcon = {
//                    IconButton(onClick = onNavigateBack) {
//                        Icon(
//                            Icons.Default.ArrowBack,
//                            contentDescription = "Back",
//                            tint = MaterialTheme.colorScheme.onBackground
//                        )
//                    }
//                },
//                actions = {
//                    IconButton(onClick = { /* Share functionality */ }) {
//                        Icon(
//                            Icons.Default.Share,
//                            contentDescription = "Share",
//                            tint = MaterialTheme.colorScheme.onBackground
//                        )
//                    }
//
//                    IconButton(onClick = { showMoreMenu = true }) {
//                        Icon(
//                            Icons.Default.MoreVert,
//                            contentDescription = "More Options",
//                            tint = MaterialTheme.colorScheme.onBackground
//                        )
//                    }
//
//                    DropdownMenu(
//                        expanded = showMoreMenu,
//                        onDismissRequest = { showMoreMenu = false }
//                    ) {
//                        DropdownMenuItem(
//                            text = { Text("Pin") },
//                            onClick = { showMoreMenu = false },
//                            leadingIcon = { Icon(Icons.Default.PushPin, null) }
//                        )
//                        DropdownMenuItem(
//                            text = { Text("Add to Folder") },
//                            onClick = { showMoreMenu = false },
//                            leadingIcon = { Icon(Icons.Default.Folder, null) }
//                        )
//                        DropdownMenuItem(
//                            text = { Text("Add a Label") },
//                            onClick = {
//                                showMoreMenu = false
//                                showFolderSheet = true
//                            },
//                            leadingIcon = { Icon(Icons.Default.Label, null) }
//                        )
//                        DropdownMenuItem(
//                            text = { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites") },
//                            onClick = {
//                                showMoreMenu = false
//                                isFavorite = !isFavorite
//                            },
//                            leadingIcon = { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null) }
//                        )
//                        DropdownMenuItem(
//                            text = { Text("Print") },
//                            onClick = { showMoreMenu = false },
//                            leadingIcon = { Icon(Icons.Default.Print, null) }
//                        )
//                        DropdownMenuItem(
//                            text = { Text("Delete") },
//                            onClick = { showMoreMenu = false },
//                            leadingIcon = { Icon(Icons.Default.Delete, null) }
//                        )
//                    }
//
//                    IconButton(
//                        onClick = {
//                            if (title.isNotBlank()) {
//                                val contentToSave = contentText.text
//                                val folderToUse = selectedFolder ?: folders.firstOrNull()
//
//                                if (folderToUse != null) {
//                                    if (isEditMode && noteId != null) {
//                                        noteViewModel.updateNote(
//                                            id = noteId,
//                                            folderId = folderToUse.id,
//                                            title = title,
//                                            content = contentToSave
//                                        )
//                                    } else {
//                                        noteViewModel.createNote(
//                                            folderId = folderToUse.id,
//                                            title = title,
//                                            content = contentToSave
//                                        )
//                                    }
//                                    onNavigateBack()
//                                }
//                            }
//                        },
//                        enabled = title.isNotBlank() && !isLoading
//                    ) {
//                        Icon(
//                            Icons.Default.Check,
//                            contentDescription = "Save",
//                            tint = MaterialTheme.colorScheme.onBackground
//                        )
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = Color.Transparent
//                )
//            )
//        },
//        bottomBar = {
//            if (showBottomBar) {
//                Surface(
//                    color = MaterialTheme.colorScheme.surface,
//                    tonalElevation = 3.dp,
//                    shadowElevation = 8.dp
//                ) {
//                    val showAnyFormatOptions = showTextFormatOptions || showParagraphFormatOptions || showTextColorBar || showHighlightColorBar
//
//                    if (showAnyFormatOptions) {
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(vertical = 8.dp),
//                            horizontalArrangement = Arrangement.Start,
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            // Close Button
//                            IconButton(
//                                onClick = {
//                                    if (showTextColorBar || showHighlightColorBar) {
//                                        showTextColorBar = false
//                                        showHighlightColorBar = false
//                                        when (previousToolbarState) {
//                                            "TEXT_FORMAT" -> showTextFormatOptions = true
//                                            "PARAGRAPH_FORMAT" -> showParagraphFormatOptions = true
//                                            else -> {
//                                                showTextFormatOptions = false
//                                                showParagraphFormatOptions = false
//                                            }
//                                        }
//                                        previousToolbarState = null
//                                    } else {
//                                        showTextFormatOptions = false
//                                        showParagraphFormatOptions = false
//                                        previousToolbarState = null
//                                    }
//                                },
//                                modifier = Modifier.size(40.dp)
//                            ) {
//                                Icon(Icons.Default.Close, "Close", tint = bottomBarIconColor)
//                            }
//
//                            // Scrollable options content
//                            LazyRow(
//                                modifier = Modifier
//                                    .weight(1f)
//                                    .height(40.dp),
//                                horizontalArrangement = Arrangement.spacedBy(2.dp),
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                if (showTextFormatOptions) {
//                                    // TEXT FORMAT OPTIONS
//                                    val onTextFormatClick: (SpanStyle, (SpanStyle) -> Boolean, Boolean) -> Unit = { style, checkFn, isCharStyle ->
//                                        val selection = contentText.selection
//                                        val start = min(selection.start, selection.end)
//                                        val end = max(selection.start, selection.end)
//
//                                        val isCurrentlyActive = if (isCharStyle) {
//                                            when {
//                                                style.fontWeight == FontWeight.Bold -> formatState.isBold
//                                                style.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic -> formatState.isItalic
//                                                style.textDecoration == androidx.compose.ui.text.style.TextDecoration.Underline -> formatState.isUnderline
//                                                style.textDecoration == androidx.compose.ui.text.style.TextDecoration.LineThrough -> formatState.isStrikethrough
//                                                else -> false
//                                            }
//                                        } else {
//                                            (formatState.isH1 && style.fontSize == 28.sp) || (formatState.isH2 && style.fontSize == 22.sp)
//                                        }
//
//                                        val styleToApply = if (isCurrentlyActive) {
//                                            SpanStyle(
//                                                fontWeight = FontWeight.Normal,
//                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
//                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.None,
//                                                fontSize = 16.sp,
//                                                color = textRenderColor,
//                                                background = Color.Unspecified
//                                            )
//                                        } else {
//                                            style
//                                        }
//
//                                        if (start < end) {
//                                            applyFormatting(start, end, styleToApply)
//                                        }
//
//                                        if (isCharStyle) {
//                                            formatState = when {
//                                                style.fontWeight == FontWeight.Bold -> formatState.copy(isBold = !formatState.isBold, isH1 = false, isH2 = false)
//                                                style.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic -> formatState.copy(isItalic = !formatState.isItalic)
//                                                style.textDecoration == androidx.compose.ui.text.style.TextDecoration.Underline -> formatState.copy(isUnderline = !formatState.isUnderline)
//                                                style.textDecoration == androidx.compose.ui.text.style.TextDecoration.LineThrough -> formatState.copy(isStrikethrough = !formatState.isStrikethrough)
//                                                else -> formatState
//                                            }
//                                        } else {
//                                            formatState = formatState.copy(
//                                                isH1 = !isCurrentlyActive && style.fontSize == 28.sp,
//                                                isH2 = !isCurrentlyActive && style.fontSize == 22.sp,
//                                                isBold = false,
//                                                isItalic = false,
//                                                isUnderline = false
//                                            )
//                                        }
//                                    }
//
//                                    // Bold
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                onTextFormatClick(SpanStyle(fontWeight = FontWeight.Bold, color = textRenderColor), { it.fontWeight == FontWeight.Bold }, true)
//                                            },
//                                            colors = IconButtonDefaults.iconButtonColors(
//                                                contentColor = if (formatState.isBold) MaterialTheme.colorScheme.primary else bottomBarIconColor
//                                            ),
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.FormatBold, "Bold", modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                    // Italic
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                onTextFormatClick(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = textRenderColor), { it.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic }, true)
//                                            },
//                                            colors = IconButtonDefaults.iconButtonColors(
//                                                contentColor = if (formatState.isItalic) MaterialTheme.colorScheme.primary else bottomBarIconColor
//                                            ),
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.FormatItalic, "Italic", modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                    // Underline
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                onTextFormatClick(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline, color = textRenderColor), { it.textDecoration == androidx.compose.ui.text.style.TextDecoration.Underline }, true)
//                                            },
//                                            colors = IconButtonDefaults.iconButtonColors(
//                                                contentColor = if (formatState.isUnderline) MaterialTheme.colorScheme.primary else bottomBarIconColor
//                                            ),
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.FormatUnderlined, "Underline", modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                    // Strikethrough
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                onTextFormatClick(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough, color = textRenderColor), { it.textDecoration == androidx.compose.ui.text.style.TextDecoration.LineThrough }, true)
//                                            },
//                                            colors = IconButtonDefaults.iconButtonColors(
//                                                contentColor = if (formatState.isStrikethrough) MaterialTheme.colorScheme.primary else bottomBarIconColor
//                                            ),
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.StrikethroughS, "Strike", modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                    // Text Color
//                                    // Text Color
//                                    item {
//                                        Box(
//                                            modifier = Modifier
//                                                .size(40.dp)
//                                                .padding(2.dp),
//                                            contentAlignment = Alignment.Center
//                                        ) {
//                                            IconButton(
//                                                onClick = {
//                                                    previousToolbarState = "TEXT_FORMAT"
//                                                    showTextColorBar = true
//                                                    showHighlightColorBar = false
//                                                    showTextFormatOptions = false
//                                                    showParagraphFormatOptions = false
//                                                },
//                                                modifier = Modifier.fillMaxSize()
//                                            ) {
//                                                Column(
//                                                    horizontalAlignment = Alignment.CenterHorizontally,
//                                                    verticalArrangement = Arrangement.Center,
//                                                    modifier = Modifier.fillMaxSize()
//                                                ) {
//                                                    Icon(
//                                                        Icons.Default.FormatColorText,
//                                                        "Text color",
//                                                        tint = bottomBarIconColor,
//                                                        modifier = Modifier.size(20.dp)
//                                                    )
//                                                    Spacer(modifier = Modifier.height(2.dp))
//                                                    // Colored underline indicator
//                                                    Box(
//                                                        modifier = Modifier
//                                                            .width(20.dp)
//                                                            .height(2.dp)
//                                                            .background(
//                                                                color = if (formatState.textColor != Color.Unspecified)
//                                                                    formatState.textColor
//                                                                else
//                                                                    bottomBarIconColor
//                                                            )
//                                                    )
//                                                }
//                                            }
//                                        }
//                                    }
//
//
//                                    // Highlight
//                                    item {
//                                        Box(
//                                            modifier = Modifier
//                                                .size(40.dp)
//                                                .padding(2.dp),
//                                            contentAlignment = Alignment.Center
//                                        ) {
//                                            IconButton(
//                                                onClick = {
//                                                    previousToolbarState = "TEXT_FORMAT"
//                                                    showHighlightColorBar = true
//                                                    showTextColorBar = false
//                                                    showTextFormatOptions = false
//                                                    showParagraphFormatOptions = false
//                                                },
//                                                modifier = Modifier.fillMaxSize()
//                                            ) {
//                                                // Custom highlighter pen with colored tip
//                                                Box(
//                                                    modifier = Modifier.size(24.dp),
//                                                    contentAlignment = Alignment.Center
//                                                ) {
//                                                    // Main pen body - rotated to look like a highlighter
//                                                    Icon(
//                                                        Icons.Default.Highlight, // Pen icon
//                                                        contentDescription = "Highlight",
//                                                        tint = bottomBarIconColor,
//                                                        modifier = Modifier
//                                                            .size(20.dp)
//                                                            .offset(x = (-1).dp, y = (-1).dp)
//                                                    )
//
//                                                    // Colored tip/cap at the bottom
//                                                    Box(
//                                                        modifier = Modifier
//                                                            .size(8.dp)
//                                                            .align(Alignment.BottomEnd)
//                                                            .offset(x = 2.dp, y = 2.dp)
//                                                            .background(
//                                                                color = if (formatState.highlightColor != Color.Unspecified)
//                                                                    formatState.highlightColor
//                                                                else
//                                                                    Color(0xFFFFEB3B).copy(alpha = 0.4f), // Default yellow
//                                                                shape = RoundedCornerShape(2.dp)
//                                                            )
//                                                            .border(
//                                                                width = 0.5.dp,
//                                                                color = bottomBarIconColor.copy(alpha = 0.3f),
//                                                                shape = RoundedCornerShape(2.dp)
//                                                            )
//                                                    )
//                                                }
//                                            }
//                                        }
//                                    }
//
//                                    // H1
//                                    item {
//                                        TextButton(
//                                            onClick = { onTextFormatClick(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textRenderColor), { it.fontSize == 28.sp }, false) },
//                                            colors = ButtonDefaults.textButtonColors(
//                                                contentColor = if (formatState.isH1) MaterialTheme.colorScheme.primary else bottomBarIconColor
//                                            ),
//                                            modifier = Modifier.height(36.dp),
//                                            contentPadding = PaddingValues(horizontal = 2.dp)
//                                        ) { Text("H1", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
//                                    }
//
//                                    // H2
//                                    item {
//                                        TextButton(
//                                            onClick = { onTextFormatClick(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textRenderColor), { it.fontSize == 22.sp }, false) },
//                                            colors = ButtonDefaults.textButtonColors(
//                                                contentColor = if (formatState.isH2) MaterialTheme.colorScheme.primary else bottomBarIconColor
//                                            ),
//                                            modifier = Modifier.height(36.dp),
//                                            contentPadding = PaddingValues(horizontal = 2.dp)
//                                        ) { Text("H2", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
//                                    }
//
//                                } else if (showParagraphFormatOptions) {
//                                    // PARAGRAPH FORMAT OPTIONS
//
//                                    // Bullet List
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                formatState = formatState.copy(
//                                                    isBulletList = !formatState.isBulletList,
//                                                    isNumberedList = false
//                                                )
//                                                if (formatState.isBulletList) {
//                                                    val cursor = contentText.selection.start
//                                                    val lineStart = findLineStart(contentText.text, cursor)
//                                                    val lineEnd = contentText.text.indexOf('\n', lineStart).let { if (it == -1) contentText.text.length else it }
//                                                    val line = contentText.text.substring(lineStart, lineEnd)
//
//                                                    if (!line.startsWith("• ")) {
//                                                        val newText = contentText.text.substring(0, lineStart) + "• " + contentText.text.substring(lineStart)
//                                                        contentText = TextFieldValue(newText, TextRange(cursor + 2))
//                                                        pushContentHistory()
//                                                    }
//                                                }
//                                            },
//                                            colors = IconButtonDefaults.iconButtonColors(
//                                                contentColor = if (formatState.isBulletList) MaterialTheme.colorScheme.primary else bottomBarIconColor
//                                            ),
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.FormatListBulleted, "Bullets", modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                    // Numbered List
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                formatState = formatState.copy(
//                                                    isNumberedList = !formatState.isNumberedList,
//                                                    isBulletList = false,
//                                                    listCounter = 1
//                                                )
//                                                if (formatState.isNumberedList) {
//                                                    val cursor = contentText.selection.start
//                                                    val lineStart = findLineStart(contentText.text, cursor)
//                                                    val lineEnd = contentText.text.indexOf('\n', lineStart).let { if (it == -1) contentText.text.length else it }
//                                                    val line = contentText.text.substring(lineStart, lineEnd)
//
//                                                    if (!line.matches(Regex("^\\d+\\.\\s.*"))) {
//                                                        val newText = contentText.text.substring(0, lineStart) + "1. " + contentText.text.substring(lineStart)
//                                                        contentText = TextFieldValue(newText, TextRange(cursor + 3))
//                                                        pushContentHistory()
//                                                    }
//                                                }
//                                            },
//                                            colors = IconButtonDefaults.iconButtonColors(
//                                                contentColor = if (formatState.isNumberedList) MaterialTheme.colorScheme.primary else bottomBarIconColor
//                                            ),
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.FormatListNumbered, "Numbers", modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                    // Align Left
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                formatState = formatState.copy(textAlign = TextAlign.Start)
//                                            },
//                                            colors = IconButtonDefaults.iconButtonColors(
//                                                contentColor = if (formatState.textAlign == TextAlign.Start) MaterialTheme.colorScheme.primary else bottomBarIconColor
//                                            ),
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.FormatAlignLeft, "Align Left", modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                    // Align Center
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                formatState = formatState.copy(textAlign = TextAlign.Center)
//                                            },
//                                            colors = IconButtonDefaults.iconButtonColors(
//                                                contentColor = if (formatState.textAlign == TextAlign.Center) MaterialTheme.colorScheme.primary else bottomBarIconColor
//                                            ),
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.FormatAlignCenter, "Align Center", modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                    // Align Right
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                formatState = formatState.copy(textAlign = TextAlign.End)
//                                            },
//                                            colors = IconButtonDefaults.iconButtonColors(
//                                                contentColor = if (formatState.textAlign == TextAlign.End) MaterialTheme.colorScheme.primary else bottomBarIconColor
//                                            ),
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.FormatAlignRight, "Align Right", modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                    // Align Justify
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                formatState = formatState.copy(textAlign = TextAlign.Justify)
//                                            },
//                                            colors = IconButtonDefaults.iconButtonColors(
//                                                contentColor = if (formatState.textAlign == TextAlign.Justify) MaterialTheme.colorScheme.primary else bottomBarIconColor
//                                            ),
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.FormatAlignJustify, "Align Justify", modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                    // Indent Increase
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                formatState = formatState.copy(indentLevel = min(formatState.indentLevel + 1, 5))
//                                            },
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.FormatIndentIncrease, "Indent +", tint = bottomBarIconColor, modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                    // Indent Decrease
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                formatState = formatState.copy(indentLevel = max(formatState.indentLevel - 1, 0))
//                                            },
//                                            enabled = formatState.indentLevel > 0,
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.FormatIndentDecrease, "Indent -", tint = if (formatState.indentLevel > 0) bottomBarIconColor else bottomBarIconColor.copy(0.3f), modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                    // Blockquote
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                formatState = formatState.copy(isBlockquote = !formatState.isBlockquote)
//                                            },
//                                            colors = IconButtonDefaults.iconButtonColors(
//                                                contentColor = if (formatState.isBlockquote) MaterialTheme.colorScheme.primary else bottomBarIconColor
//                                            ),
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.FormatQuote, "Blockquote", modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                    // Line Spacing
//                                    item {
//                                        IconButton(
//                                            onClick = {
//                                                val newSpacing = when (formatState.lineSpacing) {
//                                                    1.0f -> 1.5f
//                                                    1.5f -> 2.0f
//                                                    else -> 1.0f
//                                                }
//                                                formatState = formatState.copy(lineSpacing = newSpacing)
//                                            },
//                                            modifier = Modifier.size(36.dp)
//                                        ) { Icon(Icons.Default.FormatLineSpacing, "Line Spacing", tint = bottomBarIconColor, modifier = Modifier.size(20.dp)) }
//                                    }
//
//                                } else if (showTextColorBar) {
//                                    // TEXT COLOR BAR - Reset/Default Color
//                                    item {
//                                        Box(
//                                            modifier = Modifier
//                                                .size(36.dp)
//                                                .clickable(
//                                                    interactionSource = remember { MutableInteractionSource() },
//                                                    indication = null
//                                                ) {
//                                                    val selection = contentText.selection
//                                                    val start = min(selection.start, selection.end)
//                                                    val end = max(selection.start, selection.end)
//
//                                                    if (start < end) {
//                                                        // ✅ Build a complete style preserving ALL formatting except color
//                                                        val resetStyle = SpanStyle(
//                                                            color = textRenderColor, // ← Reset to theme color
//                                                            fontWeight = if (formatState.isBold || formatState.isH1 || formatState.isH2) FontWeight.Bold else FontWeight.Normal,
//                                                            fontStyle = if (formatState.isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
//                                                            textDecoration = when {
//                                                                formatState.isUnderline && formatState.isStrikethrough ->
//                                                                    androidx.compose.ui.text.style.TextDecoration.combine(
//                                                                        listOf(
//                                                                            androidx.compose.ui.text.style.TextDecoration.Underline,
//                                                                            androidx.compose.ui.text.style.TextDecoration.LineThrough
//                                                                        )
//                                                                    )
//                                                                formatState.isUnderline -> androidx.compose.ui.text.style.TextDecoration.Underline
//                                                                formatState.isStrikethrough -> androidx.compose.ui.text.style.TextDecoration.LineThrough
//                                                                else -> androidx.compose.ui.text.style.TextDecoration.None
//                                                            },
//                                                            fontSize = when {
//                                                                formatState.isH1 -> 28.sp
//                                                                formatState.isH2 -> 22.sp
//                                                                else -> 16.sp
//                                                            },
//                                                            background = formatState.highlightColor // ← Preserve highlight!
//                                                        )
//                                                        applyFormatting(start, end, resetStyle)
//                                                    }
//                                                    formatState = formatState.copy(textColor = Color.Unspecified)
//                                                }
//                                                .border(
//                                                    width = if (formatState.textColor == Color.Unspecified) 2.dp else 1.dp,
//                                                    color = if (formatState.textColor == Color.Unspecified) {
//                                                        MaterialTheme.colorScheme.primary
//                                                    } else {
//                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
//                                                    },
//                                                    shape = CircleShape
//                                                )
//                                                .background(Color.Transparent, CircleShape),
//                                            contentAlignment = Alignment.Center
//                                        ) {
//                                            Icon(
//                                                Icons.Default.FormatColorReset,
//                                                "Default Color",
//                                                tint = bottomBarIconColor,
//                                                modifier = Modifier.size(18.dp)
//                                            )
//                                        }
//                                    }
//                                    item { Spacer(Modifier.width(4.dp)) }
//
//                                    val textColors = listOf(
//                                        Color.Black,
//                                        Color(0xFFE53935),
//                                        Color(0xFF1E88E5),
//                                        Color(0xFF43A047),
//                                        Color(0xFFFFB300),
//                                        Color(0xFF8E24AA),
//                                        Color(0xFF00ACC1),
//                                        Color(0xFFFF6F00)
//                                    )
//
//                                    items(textColors) { color ->
//                                        Box(
//                                            modifier = Modifier
//                                                .size(28.dp)
//                                                .clickable(
//                                                    interactionSource = remember { MutableInteractionSource() },
//                                                    indication = null
//                                                ) {
//                                                    val selection = contentText.selection
//                                                    val start = min(selection.start, selection.end)
//                                                    val end = max(selection.start, selection.end)
//                                                    if (start < end) {
//                                                        applyFormatting(start, end, SpanStyle(color = color))
//                                                    }
//                                                    formatState = formatState.copy(textColor = color)
//                                                }
//                                                .border(
//                                                    width = if (color == formatState.textColor) 2.dp else 0.dp,
//                                                    color = if (color == formatState.textColor) {
//                                                        MaterialTheme.colorScheme.primary
//                                                    } else {
//                                                        Color.Transparent
//                                                    },
//                                                    shape = CircleShape
//                                                )
//                                                .background(color, CircleShape)
//                                        )
//                                    }
//
//                                } else if (showHighlightColorBar) {
//
//                                    // HIGHLIGHT COLOR BAR - Reset/No Highlight
//                                    item {
//                                        Box(
//                                            modifier = Modifier
//                                                .size(36.dp)
//                                                .clickable(
//                                                    interactionSource = remember { MutableInteractionSource() },
//                                                    indication = null
//                                                ) {
//                                                    val selection = contentText.selection
//                                                    val start = min(selection.start, selection.end)
//                                                    val end = max(selection.start, selection.end)
//
//                                                    if (start < end) {
//                                                        // ✅ Build a complete style preserving ALL formatting except background
//                                                        val resetStyle = SpanStyle(
//                                                            color = if (formatState.textColor != Color.Unspecified) formatState.textColor else textRenderColor, // ← Preserve text color!
//                                                            fontWeight = if (formatState.isBold || formatState.isH1 || formatState.isH2) FontWeight.Bold else FontWeight.Normal,
//                                                            fontStyle = if (formatState.isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
//                                                            textDecoration = when {
//                                                                formatState.isUnderline && formatState.isStrikethrough ->
//                                                                    androidx.compose.ui.text.style.TextDecoration.combine(
//                                                                        listOf(
//                                                                            androidx.compose.ui.text.style.TextDecoration.Underline,
//                                                                            androidx.compose.ui.text.style.TextDecoration.LineThrough
//                                                                        )
//                                                                    )
//                                                                formatState.isUnderline -> androidx.compose.ui.text.style.TextDecoration.Underline
//                                                                formatState.isStrikethrough -> androidx.compose.ui.text.style.TextDecoration.LineThrough
//                                                                else -> androidx.compose.ui.text.style.TextDecoration.None
//                                                            },
//                                                            fontSize = when {
//                                                                formatState.isH1 -> 28.sp
//                                                                formatState.isH2 -> 22.sp
//                                                                else -> 16.sp
//                                                            },
//                                                            background = Color.Unspecified // ← Remove highlight!
//                                                        )
//                                                        applyFormatting(start, end, resetStyle)
//                                                    }
//                                                    formatState = formatState.copy(highlightColor = Color.Unspecified)
//                                                }
//                                                .border(
//                                                    width = if (formatState.highlightColor == Color.Unspecified) 2.dp else 1.dp,
//                                                    color = if (formatState.highlightColor == Color.Unspecified) {
//                                                        MaterialTheme.colorScheme.primary
//                                                    } else {
//                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
//                                                    },
//                                                    shape = CircleShape
//                                                )
//                                                .background(Color.Transparent, CircleShape),
//                                            contentAlignment = Alignment.Center
//                                        ) {
//                                            Icon(
//                                                Icons.Default.FormatColorReset,
//                                                "No Highlight",
//                                                tint = bottomBarIconColor,
//                                                modifier = Modifier.size(18.dp)
//                                            )
//                                        }
//                                    }
//
//                                    item { Spacer(Modifier.width(4.dp)) }
//
//                                    val highlightColors = listOf(
//                                        Color(0xFFFFEB3B).copy(alpha = 0.4f),
//                                        Color(0xFF4CAF50).copy(alpha = 0.3f),
//                                        Color(0xFF2196F3).copy(alpha = 0.3f),
//                                        Color(0xFFFF9800).copy(alpha = 0.4f),
//                                        Color(0xFFE91E63).copy(alpha = 0.3f),
//                                        Color(0xFF9C27B0).copy(alpha = 0.3f),
//                                        Color(0xFF00BCD4).copy(alpha = 0.3f),
//                                        Color(0xFFCDDC39).copy(alpha = 0.4f)
//                                    )
//
//                                    items(highlightColors) { color ->
//                                        Box(
//                                            modifier = Modifier
//                                                .size(28.dp)
//                                                .clickable(
//                                                    interactionSource = remember { MutableInteractionSource() },
//                                                    indication = null
//                                                ) {
//                                                    val selection = contentText.selection
//                                                    val start = min(selection.start, selection.end)
//                                                    val end = max(selection.start, selection.end)
//                                                    if (start < end) {
//                                                        applyFormatting(start, end, SpanStyle(background = color))
//                                                    }
//                                                    formatState = formatState.copy(highlightColor = color)
//                                                }
//                                                .border(
//                                                    width = if (color == formatState.highlightColor) 2.dp else 0.dp,
//                                                    color = if (color == formatState.highlightColor) {
//                                                        MaterialTheme.colorScheme.primary
//                                                    } else {
//                                                        Color.Transparent
//                                                    },
//                                                    shape = CircleShape
//                                                )
//                                                .background(color, CircleShape)
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    } else {
//                        // MAIN TOOLBAR
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(horizontal = 2.dp, vertical = 8.dp),
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            // LEFT SIDE
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.spacedBy(2.dp),
//                                modifier = Modifier.weight(1f)
//                            ) {
//                                // Add Media
//                                IconButton(
//                                    onClick = { showAddSheet = true },
//                                    modifier = Modifier.size(36.dp)
//                                ) {
//                                    Icon(Icons.Default.AddCircleOutline, "Add Media", tint = bottomBarIconColor, modifier = Modifier.size(20.dp))
//                                }
//
//                                // Text Format
//                                IconButton(
//                                    onClick = { showTextFormatOptions = true; showParagraphFormatOptions = false },
//                                    modifier = Modifier.size(36.dp)
//                                ) {
//                                    Icon(Icons.Default.TextFields, "Text format", tint = bottomBarIconColor, modifier = Modifier.size(20.dp))
//                                }
//
//                                // Paragraph Format
//                                IconButton(
//                                    onClick = { showParagraphFormatOptions = true; showTextFormatOptions = false },
//                                    modifier = Modifier.size(36.dp)
//                                ) {
//                                    Icon(Icons.Default.FormatAlignLeft, "Paragraph format", tint = bottomBarIconColor, modifier = Modifier.size(20.dp))
//                                }
//
//                                // Note Color
//                                IconButton(
//                                    onClick = { showColorPicker = true },
//                                    modifier = Modifier.size(36.dp)
//                                ) {
//                                    Icon(Icons.Default.Palette, "Note Color", tint = bottomBarIconColor, modifier = Modifier.size(20.dp))
//                                }
//
//                                // Checkbox Insert
//                                IconButton(
//                                    onClick = {
//                                        contentText = insertCheckboxAtCursor(contentText)
//                                        pushContentHistory()
//                                    },
//                                    modifier = Modifier.size(36.dp)
//                                ) {
//                                    Icon(Icons.Default.CheckBoxOutlineBlank, "Checkbox", tint = bottomBarIconColor, modifier = Modifier.size(20.dp))
//                                }
//                            }
//
//                            // RIGHT SIDE
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.spacedBy(2.dp)
//                            ) {
//                                // Undo
//                                IconButton(
//                                    onClick = {
//                                        if (historyIndex > 0) {
//                                            historyIndex--
//                                            val snap = historyList[historyIndex]
//                                            contentText = snap.value
//                                            formatMap = snap.formatSpans.map { it.copy() }
//                                            formatState = snap.formatState.copy()
//                                        }
//                                    },
//                                    enabled = historyIndex > 0,
//                                    modifier = Modifier.size(36.dp)
//                                ) {
//                                    Icon(Icons.Default.Undo, "Undo", tint = if (historyIndex > 0) bottomBarIconColor else bottomBarIconColor.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
//                                }
//
//                                // Redo
//                                IconButton(
//                                    onClick = {
//                                        if (historyIndex < historyList.lastIndex) {
//                                            historyIndex++
//                                            val snap = historyList[historyIndex]
//                                            contentText = snap.value
//                                            formatMap = snap.formatSpans.map { it.copy() }
//                                            formatState = snap.formatState.copy()
//                                        }
//                                    },
//                                    enabled = historyIndex < historyList.lastIndex,
//                                    modifier = Modifier.size(36.dp)
//                                ) {
//                                    Icon(Icons.Default.Redo, "Redo", tint = if (historyIndex < historyList.lastIndex) bottomBarIconColor else bottomBarIconColor.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
//                                }
//
//                                // Hide bar
//                                IconButton(
//                                    onClick = { showBottomBar = false },
//                                    modifier = Modifier.size(36.dp)
//                                ) {
//                                    Icon(Icons.Default.Close, "Hide Bar", tint = bottomBarIconColor, modifier = Modifier.size(20.dp))
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        },
//        content = { paddingValues ->
//            if (isLoading) {
//                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    CircularProgressIndicator()
//                }
//            } else {
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(paddingValues)
//                        .padding(horizontal = 16.dp)
//                ) {
//                    TextField(
//                        value = title,
//                        onValueChange = { title = it },
//                        placeholder = {
//                            Text(
//                                "Title",
//                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
//                            )
//                        },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .focusRequester(titleFocusRequester),
//                        colors = TextFieldDefaults.colors(
//                            focusedContainerColor = Color.Transparent,
//                            unfocusedContainerColor = Color.Transparent,
//                            focusedIndicatorColor = Color.Transparent,
//                            unfocusedIndicatorColor = Color.Transparent,
//                            focusedTextColor = textRenderColor,
//                            unfocusedTextColor = textRenderColor
//                        ),
//                        textStyle = LocalTextStyle.current.copy(
//                            fontSize = 22.sp,
//                            fontWeight = FontWeight.SemiBold
//                        ),
//                        singleLine = true
//                    )
//
//                    HorizontalDivider(
//                        modifier = Modifier.padding(vertical = 8.dp),
//                        thickness = 1.dp,
//                        color = MaterialTheme.colorScheme.outlineVariant
//                    )
//
//                    Box(modifier = Modifier.weight(1f)) {
//                        when (currentNoteType) {
//                            NoteType.TEXT -> FormattedTextEditor(
//                                content = contentText,
//                                onContentChange = { newValue ->
//                                    contentText = newValue
//                                },
//                                formatState = formatState,
//                                onFormatStateChange = { formatState = it },
//                                formatMap = formatMap,
//                                onFormatMapChange = { formatMap = it; pushContentHistory() },
//                                textColor = textRenderColor,
//                                onFocusChanged = { isContentFocused = it },
//                                onCheckboxToggle = { toggledValue ->
//                                    contentText = toggledValue
//                                    pushContentHistory()
//                                }
//                            )
//                            NoteType.LIST, NoteType.AUDIO, NoteType.DRAWING ->
//                                Text("Note Type not fully supported for rich text yet.", color = MaterialTheme.colorScheme.error)
//                        }
//                    }
//                }
//            }
//
//            // Animated FAB
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(bottom = 0.dp, end = 16.dp),
//                contentAlignment = Alignment.BottomEnd
//            ) {
//                androidx.compose.animation.AnimatedVisibility(
//                    visible = !showBottomBar,
//                    enter = slideInVertically(
//                        initialOffsetY = { 80 },
//                        animationSpec = spring(
//                            dampingRatio = Spring.DampingRatioMediumBouncy,
//                            stiffness = Spring.StiffnessLow
//                        )
//                    ) + fadeIn(
//                        animationSpec = tween(durationMillis = 400)
//                    ) + scaleIn(
//                        initialScale = 0.8f,
//                        animationSpec = spring(
//                            dampingRatio = Spring.DampingRatioMediumBouncy,
//                            stiffness = Spring.StiffnessLow
//                        )
//                    ),
//                    exit = slideOutVertically(
//                        targetOffsetY = { 80 },
//                        animationSpec = spring(
//                            dampingRatio = Spring.DampingRatioNoBouncy,
//                            stiffness = Spring.StiffnessMedium
//                        )
//                    ) + fadeOut(
//                        animationSpec = tween(durationMillis = 250)
//                    ) + scaleOut(
//                        targetScale = 0.8f,
//                        animationSpec = tween(durationMillis = 250)
//                    )
//                ) {
//                    FloatingActionButton(
//                        onClick = { showBottomBar = true },
//                        shape = CircleShape,
//                        containerColor = MaterialTheme.colorScheme.primary,
//                        elevation = FloatingActionButtonDefaults.elevation(
//                            defaultElevation = 8.dp,
//                            pressedElevation = 12.dp,
//                            hoveredElevation = 10.dp
//                        ),
//                        modifier = Modifier
//                            .padding(bottom = 16.dp)
//                            .size(56.dp)
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Edit,
//                            contentDescription = "Show Formatting Bar",
//                            tint = MaterialTheme.colorScheme.onPrimary,
//                            modifier = Modifier.size(24.dp)
//                        )
//                    }
//                }
//            }
//
//            // Bottom Sheets and Dialogs
//            if (showFolderSheet) {
//                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
//                ModalBottomSheet(
//                    onDismissRequest = { showFolderSheet = false },
//                    sheetState = sheetState,
//                    containerColor = MaterialTheme.colorScheme.surface
//                ) {
//                    CategoryPickerSheet(
//                        folders = folders,
//                        onFolderSelected = {
//                            selectedFolder = it
//                            showFolderSheet = false
//                        },
//                        onCreateNew = {
//                            showFolderSheet = false
//                            showCreateCategorySheet = true
//                        }
//                    )
//                }
//            }
//
//            if (showCreateCategorySheet) {
//                val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
//                ModalBottomSheet(
//                    onDismissRequest = { showCreateCategorySheet = false },
//                    sheetState = createSheetState,
//                    containerColor = MaterialTheme.colorScheme.surface
//                ) {
//                    CreateCategorySheet(
//                        onDismiss = { showCreateCategorySheet = false },
//                        onCreate = { name, key, iconName ->
//                            noteViewModel.createFolder(name, key.name, iconName)
//                            pendingNewFolderName = name
//                            scope.launch { noteViewModel.loadFolders() }
//                            showCreateCategorySheet = false
//                        }
//                    )
//                }
//            }
//
//            if (showAddSheet) {
//                val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
//                ModalBottomSheet(
//                    onDismissRequest = { showAddSheet = false },
//                    sheetState = addSheetState,
//                    containerColor = MaterialTheme.colorScheme.surface
//                ) {
//                    Column(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(16.dp)
//                    ) {
//                        Text(
//                            "Add to Note",
//                            fontSize = 18.sp,
//                            fontWeight = FontWeight.SemiBold,
//                            color = MaterialTheme.colorScheme.onSurface
//                        )
//                        Spacer(Modifier.height(16.dp))
//
//                        ListItem(
//                            headlineContent = { Text("Image") },
//                            supportingContent = { Text("Add photo from gallery (Dummy)") },
//                            leadingContent = { Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary) },
//                            modifier = Modifier.clickable(
//                                interactionSource = remember { MutableInteractionSource() },
//                                indication = null
//                            ) { showAddSheet = false }
//                        )
//                        ListItem(
//                            headlineContent = { Text("Voice Recording") },
//                            supportingContent = { Text("Record audio note (Dummy)") },
//                            leadingContent = { Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary) },
//                            modifier = Modifier.clickable(
//                                interactionSource = remember { MutableInteractionSource() },
//                                indication = null
//                            ) { showAddSheet = false }
//                        )
//                        ListItem(
//                            headlineContent = { Text("Drawing") },
//                            supportingContent = { Text("Create sketch or drawing (Dummy)") },
//                            leadingContent = { Icon(Icons.Default.Brush, null, tint = MaterialTheme.colorScheme.primary) },
//                            modifier = Modifier.clickable(
//                                interactionSource = remember { MutableInteractionSource() },
//                                indication = null
//                            ) { showAddSheet = false }
//                        )
//                        ListItem(
//                            headlineContent = { Text("File Attachment") },
//                            supportingContent = { Text("Attach document or file (Dummy)") },
//                            leadingContent = { Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary) },
//                            modifier = Modifier.clickable(
//                                interactionSource = remember { MutableInteractionSource() },
//                                indication = null
//                            ) { showAddSheet = false }
//                        )
//                        ListItem(
//                            headlineContent = { Text("Camera") },
//                            supportingContent = { Text("Take photo with camera (Dummy)") },
//                            leadingContent = { Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.primary) },
//                            modifier = Modifier.clickable(
//                                interactionSource = remember { MutableInteractionSource() },
//                                indication = null
//                            ) { showAddSheet = false }
//                        )
//
//                        Spacer(Modifier.height(16.dp))
//                    }
//                }
//            }
//
//            if (showColorPicker) {
//                ColorPickerDialog(
//                    currentColor = noteBackgroundColor,
//                    onColorSelected = {
//                        noteBackgroundColor = it
//                        showColorPicker = false
//                    },
//                    onDismiss = { showColorPicker = false },
//                    defaultColor = defaultNoteBgColor
//                )
//            }
//        }
//    )
//}
//
//// -----------------------------------------
//// --- Support Composables ---
//// -----------------------------------------
//
//@Composable
//private fun CategoryPickerSheet(
//    folders: List<FolderEntity>,
//    onFolderSelected: (FolderEntity) -> Unit,
//    onCreateNew: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp, vertical = 12.dp)
//    ) {
//        Text(
//            "Select Label",
//            fontWeight = FontWeight.SemiBold,
//            fontSize = 18.sp,
//            color = MaterialTheme.colorScheme.onSurface
//        )
//        Spacer(Modifier.height(12.dp))
//
//        LazyColumn(
//            verticalArrangement = Arrangement.spacedBy(8.dp),
//            modifier = Modifier
//                .fillMaxWidth()
//                .heightIn(max = 300.dp)
//        ) {
//            items(folders) { folder ->
//                val (icon, bg, fg) = getFolderIconAndColors(folder)
//                Surface(
//                    onClick = { onFolderSelected(folder) },
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(10.dp),
//                    color = MaterialTheme.colorScheme.surfaceVariant
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(12.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(40.dp)
//                                .background(bg, RoundedCornerShape(10.dp)),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
//                        }
//                        Spacer(Modifier.width(12.dp))
//                        Text(
//                            folder.name,
//                            fontSize = 16.sp,
//                            color = MaterialTheme.colorScheme.onSurface,
//                            fontWeight = FontWeight.Medium
//                        )
//                    }
//                }
//            }
//            item {
//                Spacer(Modifier.height(8.dp))
//                OutlinedButton(
//                    onClick = onCreateNew,
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(10.dp)
//                ) {
//                    Icon(Icons.Default.Add, null)
//                    Spacer(Modifier.width(8.dp))
//                    Text("Create New Label")
//                }
//                Spacer(Modifier.height(12.dp))
//            }
//        }
//    }
//}
//
//@Composable
//private fun CreateCategorySheet(
//    onDismiss: () -> Unit,
//    onCreate: (name: String, key: CategoryKey, iconName: String) -> Unit
//) {
//    var name by remember { mutableStateOf("") }
//    var selectedKey by remember { mutableStateOf(CategoryKey.PERSONAL) }
//    var selectedIcon by remember { mutableStateOf("Person") }
//
//    val iconOptions = listOf(
//        "Person" to Icons.Default.Person,
//        "Work" to Icons.Default.Work,
//        "Lightbulb" to Icons.Default.Lightbulb,
//        "ShoppingCart" to Icons.Default.ShoppingCart,
//        "School" to Icons.Default.School,
//        "Code" to Icons.Default.Code
//    )
//
//    val categoryChoices = listOf(
//        CategoryKey.PERSONAL, CategoryKey.WORK, CategoryKey.IDEAS,
//        CategoryKey.CHECKLIST, CategoryKey.MINT, CategoryKey.SKY
//    )
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(16.dp)
//    ) {
//        Text("Create Label", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
//        Spacer(Modifier.height(12.dp))
//
//        TextField(
//            value = name,
//            onValueChange = { name = it },
//            placeholder = { Text("Label name") },
//            singleLine = true,
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(Modifier.height(12.dp))
//        Text("Color", fontSize = 13.sp, fontWeight = FontWeight.Medium)
//        Spacer(Modifier.height(8.dp))
//
//        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//            items(categoryChoices) { key ->
//                val color = categoryContainerColor(key)
//                Surface(
//                    onClick = { selectedKey = key },
//                    modifier = Modifier.size(44.dp),
//                    shape = CircleShape,
//                    color = color,
//                    border = BorderStroke(
//                        if (key == selectedKey) 3.dp else 1.dp,
//                        if (key == selectedKey) MaterialTheme.colorScheme.primary
//                        else MaterialTheme.colorScheme.outline.copy(0.3f)
//                    )
//                ) {}
//            }
//        }
//
//        Spacer(Modifier.height(12.dp))
//        Text("Icon", fontSize = 13.sp, fontWeight = FontWeight.Medium)
//        Spacer(Modifier.height(8.dp))
//
//        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//            items(iconOptions) { (iconName, vector) ->
//                Surface(
//                    onClick = { selectedIcon = iconName },
//                    shape = RoundedCornerShape(10.dp),
//                    color = MaterialTheme.colorScheme.surfaceVariant,
//                    modifier = Modifier.size(48.dp),
//                    border = BorderStroke(
//                        if (selectedIcon == iconName) 2.dp else 1.dp,
//                        if (selectedIcon == iconName) MaterialTheme.colorScheme.primary
//                        else MaterialTheme.colorScheme.outline.copy(0.3f)
//                    )
//                ) {
//                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
//                        Icon(vector, iconName)
//                    }
//                }
//            }
//        }
//
//        Spacer(Modifier.height(16.dp))
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.End
//        ) {
//            TextButton(onClick = onDismiss) { Text("Cancel") }
//            Spacer(Modifier.width(8.dp))
//            TextButton(
//                onClick = { if (name.isNotBlank()) onCreate(name.trim(), selectedKey, selectedIcon) },
//                enabled = name.isNotBlank()
//            ) { Text("Create") }
//        }
//        Spacer(Modifier.height(8.dp))
//    }
//}
//
//@Composable
//private fun getFolderIconAndColors(folder: FolderEntity?): Triple<ImageVector, Color, Color> {
//    if (folder == null) {
//        return Triple(Icons.Default.Folder, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
//    }
//
//    val categoryKey = folder.colorKey?.let {
//        try { CategoryKey.valueOf(it) } catch (_: Exception) { null }
//    }
//
//    val bg = if (categoryKey != null) categoryContainerColor(categoryKey)
//    else categoryContainerColor(categoryKeyFromName(folder.name))
//
//    val fg = if (categoryKey != null) categoryIconColor(categoryKey)
//    else categoryIconColor(categoryKeyFromName(folder.name))
//
//    val icon = folder.iconName?.let { iconNameToVector(it) } ?: Icons.Default.Folder
//
//    return Triple(icon, bg, fg)
//}
//
//private fun iconNameToVector(name: String): ImageVector? = when (name) {
//    "Person" -> Icons.Default.Person
//    "Work" -> Icons.Default.Work
//    "Lightbulb" -> Icons.Default.Lightbulb
//    "ShoppingCart" -> Icons.Default.ShoppingCart
//    "School" -> Icons.Default.School
//    "Code" -> Icons.Default.Code
//    else -> null
//}
//
//private fun categoryKeyFromName(name: String): CategoryKey {
//    val n = name.lowercase()
//    return when {
//        listOf("work", "meeting").any { n.contains(it) } -> CategoryKey.WORK
//        listOf("personal", "life").any { n.contains(it) } -> CategoryKey.PERSONAL
//        listOf("idea", "ideas").any { n.contains(it) } -> CategoryKey.IDEAS
//        listOf("check", "todo", "list").any { n.contains(it) } -> CategoryKey.CHECKLIST
//        else -> CategoryKey.SLATE
//    }
//}
//
//@Composable
//fun AudioNoteContent() {
//    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            Icon(Icons.Default.Mic, "Audio", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
//            Text("Audio recording feature", fontSize = 18.sp, fontWeight = FontWeight.Medium)
//            Text("Coming soon...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
//        }
//    }
//}
//
//@Composable
//fun DrawingNoteContent() {
//    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            Icon(Icons.Default.Brush, "Drawing", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
//            Text("Drawing canvas feature", fontSize = 18.sp, fontWeight = FontWeight.Medium)
//            Text("Coming soon...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
//        }
//    }
//}
//
//@Composable
//fun ColorPickerDialog(
//    currentColor: Color,
//    onColorSelected: (Color) -> Unit,
//    onDismiss: () -> Unit,
//    defaultColor: Color
//) {
//    val themeColors = listOf(
//        defaultColor,
//        Color(0xFFFFFDD0),
//        Color(0xFFE6E6FA),
//        Color(0xFFD4EDDA),
//        Color(0xFFB0E0E6),
//        Color(0xFFF08080),
//        Color(0xFFFFFFFF)
//    )
//
//    var selectedColor by remember { mutableStateOf(currentColor) }
//
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text("Select Note Color", fontWeight = FontWeight.SemiBold) },
//        text = {
//            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
//                themeColors.chunked(4).forEach { rowColors ->
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceEvenly
//                    ) {
//                        rowColors.forEach { color ->
//                            Surface(
//                                onClick = { selectedColor = color },
//                                modifier = Modifier
//                                    .size(48.dp)
//                                    .border(
//                                        width = if (color == selectedColor) 3.dp else 1.dp,
//                                        color = if (color == selectedColor) {
//                                            MaterialTheme.colorScheme.primary
//                                        } else {
//                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
//                                        },
//                                        shape = CircleShape
//                                    ),
//                                shape = CircleShape,
//                                color = color
//                            ) {}
//                        }
//                    }
//                }
//            }
//        },
//        confirmButton = {
//            TextButton(onClick = { onColorSelected(selectedColor) }) {
//                Text("Select")
//            }
//        },
//        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
//        containerColor = MaterialTheme.colorScheme.surface
//    )
//}
