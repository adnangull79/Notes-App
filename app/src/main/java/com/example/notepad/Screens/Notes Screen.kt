// NotesScreen.kt
package com.example.notepad.Screens

import android.app.Activity
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext // <-- ADDED THIS IMPORT
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.notepad.FolderEntity
import com.example.notepad.NoteEntity
import com.example.notepad.NoteType
import com.example.notepad.NoteViewModel
import com.example.notepad.Screen
import com.example.notepad.UI_theme.CategoryKey
import com.example.notepad.UI_theme.categoryContainerColor
import com.example.notepad.UI_theme.categoryIconColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ViewMode { LIST, GRID }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(navController: NavController, noteViewModel: NoteViewModel) {
    var selectedFilter by remember { mutableStateOf<FolderEntity?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedNotes by remember { mutableStateOf(setOf<Int>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }

    val folders by noteViewModel.folders.collectAsState()
    val allNotes by noteViewModel.notes.collectAsState()

    LaunchedEffect(Unit) {
        noteViewModel.loadFolders()
        noteViewModel.loadAllNotes()
    }

    // Filter + search
    val filteredNotes = remember(selectedFilter, allNotes, searchQuery) {
        // Exclude archived and trashed from main "All Notes" view
        val baseRaw = if (selectedFilter == null) allNotes else allNotes.filter { it.folderId == selectedFilter!!.id }
        val base = baseRaw.filter { !it.isArchived && !it.isDeleted }
        val searched = if (searchQuery.isBlank()) base else base.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.content?.contains(searchQuery, ignoreCase = true) == true
        }
        searched.sortedByDescending { it.lastEditedAt }
    }

    // Exit selection mode when filter changes
    LaunchedEffect(selectedFilter) {
        isSelectionMode = false
        selectedNotes = setOf()
    }

    // Exit selection mode when all selected notes are deselected
    LaunchedEffect(selectedNotes) {
        if (isSelectionMode && selectedNotes.isEmpty()) {
            isSelectionMode = false
        }
    }


    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()


    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NotesSideDrawer(
                navController = navController,
                folders = folders,
                folderCounts = remember(allNotes) {
                    folders.associate { f -> f.id to allNotes.count { it.folderId == f.id && !it.isDeleted } }
                },
                onItemClick = { scope.launch { drawerState.close() } },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Settings.route)
                }
            )

        }
    ) {
        Scaffold(
            topBar = {
                NotesTopAppBar(
                    isSelectionMode = isSelectionMode,
                    selectedNotesCount = selectedNotes.size,
                    selectedFolder = selectedFilter,
                    searchQuery = searchQuery,
                    viewMode = viewMode,
                    onSearchQueryChange = { searchQuery = it },
                    onViewModeChange = { viewMode = it },
                    onExitSelectionMode = {
                        isSelectionMode = false
                        selectedNotes = setOf()
                    },
                    onDeleteSelected = { showDeleteDialog = true },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    selectedNotes = selectedNotes,
                    noteViewModel = noteViewModel,
                    allNotes = allNotes
                )
            },
            floatingActionButton = {
                if (!isSelectionMode) {
                    ExpandableFab(navController)
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            NotesContent(
                paddingValues = paddingValues,
                selectedFolder = selectedFilter,
                folders = folders,
                notes = filteredNotes,
                isSelectionMode = isSelectionMode,
                selectedNotes = selectedNotes,
                viewMode = viewMode,
                onNoteClick = { note ->
                    // Toggle selection if in selection mode, otherwise navigate
                    if (isSelectionMode) {
                        selectedNotes = if (selectedNotes.contains(note.id)) selectedNotes - note.id else selectedNotes + note.id
                        if (!isSelectionMode) isSelectionMode = true // Enter selection mode on first click
                    } else {
                        val noteType = detectNoteType(note.content)
                        navController.navigate(Screen.CreateNote.route(note.id, noteType))
                    }
                },
                onNoteLongClick = { note ->
                    if (!isSelectionMode) {
                        isSelectionMode = true
                        selectedNotes = setOf(note.id)
                    }
                }
            )
        }
    }

    // Multi-select confirmation: move to trash (soft delete)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(text = "Move to Trash", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    text = "Move ${selectedNotes.size} note${if (selectedNotes.size > 1) "s" else ""} to Trash?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedNotes.forEach { id -> noteViewModel.moveToTrash(id) }
                        showDeleteDialog = false
                        isSelectionMode = false
                        selectedNotes = setOf()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Move") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesTopAppBar(
    isSelectionMode: Boolean,
    selectedNotesCount: Int,
    selectedFolder: FolderEntity?,
    searchQuery: String,
    viewMode: ViewMode,
    onSearchQueryChange: (String) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onExitSelectionMode: () -> Unit,
    onDeleteSelected: () -> Unit,
    onOpenDrawer: () -> Unit,
    selectedNotes: Set<Int>,
    noteViewModel: NoteViewModel,
    allNotes: List<NoteEntity>
) {
    val singleSelectedNote = remember(selectedNotes, allNotes) {
        if (selectedNotes.size == 1) allNotes.find { it.id == selectedNotes.first() } else null
    }

    val isFavorite = singleSelectedNote?.isFavorite ?: false
    val isArchived = singleSelectedNote?.isArchived ?: false

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
                Text(
                    text = if (isSelectionMode) "${selectedNotesCount} selected" else selectedFolder?.name ?: "All Notes",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                if (isSelectionMode) {
                    IconButton(onClick = onExitSelectionMode) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Selection", tint = MaterialTheme.colorScheme.onBackground)
                    }
                } else {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Menu", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            },
            actions = {
                if (isSelectionMode) {
                    if (selectedNotesCount == 1) {
                        // Single-note actions (moved from bottom sheet)
                        IconButton(onClick = {
                            singleSelectedNote?.let { note ->
                                noteViewModel.toggleFavorite(note.id, note.isFavorite)
                                // Deselect the note after action, simulating action completion
                                onExitSelectionMode()
                            }
                        }) {
                            Icon(
                                if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = {
                            singleSelectedNote?.let { note ->
                                noteViewModel.toggleArchive(note.id, note.isArchived)
                                // Deselect the note after action
                                onExitSelectionMode()
                            }
                        }) {
                            Icon(
                                if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (isArchived) "Unarchive" else "Archive",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    // Multi-select and single-select delete
                    IconButton(onClick = onDeleteSelected, enabled = selectedNotes.isNotEmpty()) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Move to Trash",
                            tint = if (selectedNotes.isNotEmpty()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    IconButton(onClick = {
                        onViewModeChange(if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
                    }) {
                        Icon(
                            imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewAgenda,
                            contentDescription = if (viewMode == ViewMode.LIST) "Grid View" else "List View",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        // Search Bar (remains when not in selection mode)
        if (!isSelectionMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Search notes...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/* ---------------- Drawer UI ---------------- */

@Composable
private fun NotesSideDrawer(
    navController: NavController,
    folders: List<FolderEntity>,
    folderCounts: Map<Int, Int>,
    onItemClick: () -> Unit,
    onSettingsClick: () -> Unit
)
{
    val drawerContainer = MaterialTheme.colorScheme.surface
    val headerColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
    val selectedContent = MaterialTheme.colorScheme.onPrimaryContainer
    val normalContent = MaterialTheme.colorScheme.onSurface

    ModalDrawerSheet(
        drawerContainerColor = drawerContainer,
        drawerContentColor = normalContent,
        modifier = Modifier
            .fillMaxWidth(0.8f) // 70% width

    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            // Quick list
            DrawerRow(
                icon = Icons.Default.Notes,
                label = "All Notes",
                selected = true,
                onClick = onItemClick,
                selectedBg = selectedBg,
                selectedContent = selectedContent
            )
            DrawerRow(
                icon = Icons.Default.Star,
                label = "Favorites",
                onClick = {
                    onItemClick()
                    navController.navigate(Screen.Favorites.route)
                }
            )
            DrawerRow(
                icon = Icons.Default.Archive,
                label = "Archived",
                onClick = {
                    onItemClick()
                    navController.navigate(Screen.Archived.route)
                }
            )

            DrawerRow(
                icon = Icons.Default.Delete,
                label = "Trash",
                onClick = {
                    onItemClick()
                    navController.navigate(Screen.Trash.route)
                }
            )


            Divider(Modifier.padding(vertical = 8.dp))

            // Folders section (empty for now per your request)
            SectionHeader(title = "Folders", trailingIcon = Icons.Default.Add,
                onTrailingClick = onItemClick, headerColor = headerColor)
            // No folder rows here for now.

            Divider(Modifier.padding(top = 8.dp, bottom = 8.dp))

            // Tags section (dummy)
            SectionHeader(title = "Tags", trailingIcon = Icons.Default.Add, onTrailingClick = onItemClick, headerColor = headerColor)
            DrawerRow(Icons.Default.Label, "#urgent", count = 3, onClick = onItemClick)
            DrawerRow(Icons.Default.Label, "#ideas", count = 8, onClick = onItemClick)
        }
        Divider(Modifier.padding(vertical = 8.dp))

        // Sticky bottom: Settings
        //Spacer(Modifier.height(4.dp))
        DrawerRow(
            icon = Icons.Default.Settings,
            label = "Settings",
            onClick = onSettingsClick,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailingIcon: ImageVector,
    onTrailingClick: () -> Unit,
    headerColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = headerColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onTrailingClick) {
            Icon(trailingIcon, contentDescription = null, tint = headerColor)
        }
    }
}

@Composable
private fun DrawerRow(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    count: Int? = null,
    onClick: () -> Unit,
    selectedBg: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
    selectedContent: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) selectedBg else Color.Transparent
    val contentColor = if (selected) selectedContent else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(Modifier.width(12.dp))
            Text(label, color = contentColor, modifier = Modifier.weight(1f))
            if (count != null) {
                Text("$count", color = contentColor.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}

/* ---------------- Helpers ---------------- */

fun detectNoteType(content: String?): NoteType {
    if (content.isNullOrBlank()) return NoteType.TEXT
    val lines = content.lines()
    val checklistLines = lines.count { it.trim().startsWith("[x]") || it.trim().startsWith("[ ]") }
    val nonEmptyLines = lines.count { it.isNotBlank() }
    return if (nonEmptyLines > 0 && checklistLines.toFloat() / nonEmptyLines > 0.5f) NoteType.LIST else NoteType.TEXT
}

@Composable
fun ExpandableFab(navController: NavController) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "rotation"
    )

    Box(contentAlignment = Alignment.BottomEnd) {
        Column(horizontalAlignment = Alignment.End) {
            val options = listOf(
                Triple("Audio", Icons.Default.Mic, NoteType.AUDIO),
                Triple("Drawing", Icons.Default.Brush, NoteType.DRAWING),
                Triple("List", Icons.Default.List, NoteType.LIST),
                Triple("Text", Icons.Default.TextFields, NoteType.TEXT)
            )
            options.forEachIndexed { index, (label, icon, noteType) ->
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(expanded) {
                    if (expanded) { delay(index * 50L); isVisible = true }
                    else { delay((3 - index) * 50L); isVisible = false }
                }
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + scaleIn(initialScale = 0.3f),
                    exit = fadeOut() + scaleOut(targetScale = 0.3f)
                ) {
                    FabOption(label = label, icon = icon) {
                        expanded = false

                        when (label) {
                            "Drawing" -> {
                                navController.navigate(Screen.Drawing.route)
                            }

                            "Text" -> {
                                navController.navigate(Screen.CreateNote.route(noteId = null, noteType = NoteType.TEXT))
                            }

                            "List" -> {
                                navController.navigate(Screen.CreateNote.route(noteId = null, noteType = NoteType.LIST))
                            }

                        }
                    }

                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            ) { Icon(Icons.Default.Add, contentDescription = "Toggle Options") }
        }
    }
}

@Composable
fun FabOption(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Surface(
            onClick = onClick,
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) { Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesContent(
    paddingValues: PaddingValues,
    selectedFolder: FolderEntity?,
    folders: List<FolderEntity>,
    notes: List<NoteEntity>,
    isSelectionMode: Boolean,
    selectedNotes: Set<Int>,
    viewMode: ViewMode,
    onNoteClick: (NoteEntity) -> Unit, // Handles selection toggle OR navigation (when not in selection mode)
    onNoteLongClick: (NoteEntity) -> Unit,
) {
    // The NavController access is now handled in NotesScreen and passed down via onNoteClick.
    // However, if onNoteClick's implementation requires knowing *if* selection mode is active
    // to determine whether to navigate or select, we adjust the usage here.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = paddingValues.calculateTopPadding()) // Apply TopApp bar padding
    ) {
        // Notes Display
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "No notes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notes yet",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Tap + to create your first note",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            if (viewMode == ViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = paddingValues.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes) { note ->
                        NoteCard(
                            note = note,
                            folders = folders,
                            isSelected = selectedNotes.contains(note.id),
                            isSelectionMode = isSelectionMode,
                            onClick = { onNoteClick(note) }, // Use the passed-in lambda
                            onLongClick = { onNoteLongClick(note) }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = paddingValues.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes) { note ->
                        NoteCardGrid(
                            note = note,
                            folders = folders,
                            isSelected = selectedNotes.contains(note.id),
                            isSelectionMode = isSelectionMode,
                            onClick = { onNoteClick(note) }, // Use the passed-in lambda
                            onLongClick = { onNoteLongClick(note) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteEntity,
    folders: List<FolderEntity>,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val folder = folders.find { it.id == note.folderId }
    val noteType = detectNoteType(note.content)
    val (icon, bg, fg) = noteIconAndColors(note, folder, noteType)

    val absoluteDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.lastEditedAt))
    val editedRelative = formatDate(note.lastEditedAt)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(bg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(imageVector = icon, contentDescription = null, tint = fg) }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (!note.content.isNullOrBlank()) {
                    if (noteType == NoteType.LIST) {
                        val lines = note.content.lines()
                        val checkedCount = lines.count { it.trim().startsWith("[x]") }
                        val totalCount = lines.count { it.trim().startsWith("[x]") || it.trim().startsWith("[ ]") }
                        Text(
                            text = "$checkedCount/$totalCount completed",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = note.content ?: "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = absoluteDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "• $editedRelative",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCardGrid(
    note: NoteEntity,
    folders: List<FolderEntity>,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val folder = folders.find { it.id == note.folderId }
    val noteType = detectNoteType(note.content)
    val (icon, bg, fg) = noteIconAndColors(note, folder, noteType)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(bg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (note.isFavorite) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = note.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!note.content.isNullOrBlank()) {
                if (noteType == NoteType.LIST) {
                    val lines = note.content.lines()
                    val checkedCount = lines.count { it.trim().startsWith("[x]") }
                    val totalCount = lines.count { it.trim().startsWith("[x]") || it.trim().startsWith("[ ]") }
                    Text(
                        text = "$checkedCount/$totalCount completed",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = note.content ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = formatDate(note.lastEditedAt),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}


@Composable
private fun noteIconAndColors(
    note: NoteEntity,
    folder: FolderEntity?,
    noteType: NoteType
): Triple<ImageVector, Color, Color> {
    if (noteType == NoteType.LIST) {
        val bg = categoryContainerColor(CategoryKey.CHECKLIST)
        val fg = categoryIconColor(CategoryKey.CHECKLIST)
        return Triple(Icons.Default.Checklist, bg, fg)
    }

    val iconName = folder?.iconName ?: "Folder"
    val colorKey = folder?.colorKey ?: CategoryKey.PERSONAL.name

    val icon = getFolderIcon(iconName)
    val bg = categoryContainerColor(CategoryKey.valueOf(colorKey))
    val fg = categoryIconColor(CategoryKey.valueOf(colorKey))

    return Triple(icon, bg, fg)
}

private fun getFolderIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Person" -> Icons.Default.Person
        "Work" -> Icons.Default.Work
        "Lightbulb" -> Icons.Default.Lightbulb
        "ShoppingCart" -> Icons.Default.ShoppingCart
        "School" -> Icons.Default.School
        "Code" -> Icons.Default.Code
        "FitnessCenter" -> Icons.Default.FitnessCenter
        "Flight" -> Icons.Default.Flight
        "AccountBalance" -> Icons.Default.AccountBalance
        "AccountTree" -> Icons.Default.AccountTree
        "Restaurant" -> Icons.Default.Restaurant
        "MenuBook" -> Icons.Default.MenuBook
        else -> Icons.Default.Folder
    }
}

fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        diff < 86_400_000 -> "${diff / 3_600_000} hrs ago"
        diff < 604_800_000 -> "${diff / 86_400_000} days ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}