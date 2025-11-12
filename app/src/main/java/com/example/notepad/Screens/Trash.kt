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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.notepad.*

enum class TrashViewMode { LIST, GRID }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(navController: NavController, noteViewModel: NoteViewModel) {

    var viewMode by remember { mutableStateOf(TrashViewMode.LIST) }
    var sheetNote by remember { mutableStateOf<NoteEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val folders by noteViewModel.folders.collectAsState()

    // ✅ Local state to avoid blinking
    var trashNotes by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }

    // Load trash when screen shows
    LaunchedEffect(Unit) {
        noteViewModel.loadTrashNotes()
    }

    // Update UI when notes change
    val allNotes by noteViewModel.notes.collectAsState()
    LaunchedEffect(allNotes) {
        trashNotes = allNotes.filter { it.isDeleted }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewMode = if (viewMode == TrashViewMode.LIST) TrashViewMode.GRID else TrashViewMode.LIST
                    }) {
                        Icon(
                            imageVector = if (viewMode == TrashViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewAgenda,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->

        if (trashNotes.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Trash is empty")
            }
        } else {


            if (viewMode == TrashViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trashNotes) { note ->
                        NoteCard(
                            note = note,
                            folders = folders,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = {}, // Cannot open from trash
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
                    items(trashNotes) { note ->
                        NoteCardGrid(
                            note = note,
                            folders = folders,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = {},
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
                    icon = Icons.Default.Restore,
                    label = "Restore",
                    onClick = {
                        noteViewModel.restoreFromTrash(n.id)
                        sheetNote = null
                    }
                )

                SheetActionRow(
                    icon = Icons.Default.DeleteForever,
                    label = "Delete Permanently",
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        noteViewModel.permanentlyDelete(n.id)
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
