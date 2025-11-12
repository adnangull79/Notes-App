package com.example.notepad.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.notepad.*

enum class ArchivedViewMode { LIST, GRID }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedScreen(navController: NavController, noteViewModel: NoteViewModel) {

    var viewMode by remember { mutableStateOf(ArchivedViewMode.LIST) }
    var sheetNote by remember { mutableStateOf<NoteEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val folders by noteViewModel.folders.collectAsState()

    // ✅ Local stable archived list (fixes blinking)
    var archivedNotes by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }

    // Load only archived notes when screen opens
    LaunchedEffect(Unit) {
        noteViewModel.loadArchivedNotes()
    }

    // Update local list when ViewModel updates
    val allNotes by noteViewModel.notes.collectAsState()
    LaunchedEffect(allNotes) {
        archivedNotes = allNotes.filter { it.isArchived && !it.isDeleted }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archived") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewMode = if (viewMode == ArchivedViewMode.LIST) ArchivedViewMode.GRID else ArchivedViewMode.LIST
                    }) {
                        Icon(
                            imageVector = if (viewMode == ArchivedViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewAgenda,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->

        if (archivedNotes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No archived notes")
            }
        } else {

            if (viewMode == ArchivedViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(archivedNotes) { note ->
                        NoteCard(
                            note = note,
                            folders = folders,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = {
                                val type = detectNoteType(note.content)
                                navController.navigate(Screen.CreateNote.route(note.id, type))
                            },
                            onLongClick = { sheetNote = note }
                        )
                    }
                }

            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(archivedNotes) { note ->
                        NoteCardGrid(
                            note = note,
                            folders = folders,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = {
                                val type = detectNoteType(note.content)
                                navController.navigate(Screen.CreateNote.route(note.id, type))
                            },
                            onLongClick = { sheetNote = note }
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheet Actions
    if (sheetNote != null) {
        ModalBottomSheet(
            onDismissRequest = { sheetNote = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val n = sheetNote!!
            Column(Modifier.padding(bottom = 20.dp)) {

                SheetActionRow(
                    icon = Icons.Default.Unarchive,
                    label = "Unarchive",
                    onClick = {
                        noteViewModel.toggleArchive(n.id, true)
                        sheetNote = null
                    }
                )

                SheetActionRow(
                    icon = Icons.Default.Delete,
                    label = "Move to Trash",
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        noteViewModel.moveToTrash(n.id)
                        sheetNote = null
                    }
                )

                Divider(Modifier.padding(vertical = 8.dp))

                SheetActionRow(
                    icon = Icons.Default.Close,
                    label = "Cancel",
                    onClick = { sheetNote = null }
                )
            }
        }
    }
}
@Composable
fun SheetActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(Modifier.width(14.dp))
            Text(label, color = contentColor, fontSize = 16.sp)
        }
    }
}
