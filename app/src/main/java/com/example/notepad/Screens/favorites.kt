package com.example.notepad.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.notepad.*

enum class FavViewMode { LIST, GRID }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController, noteViewModel: NoteViewModel) {

    var viewMode by remember { mutableStateOf(FavViewMode.LIST) }
    val folders by noteViewModel.folders.collectAsState()

    // Local favorites state to avoid blinking
    var favNotes by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }

    // Load favorites initially
    LaunchedEffect(Unit) {
        noteViewModel.loadFavoriteNotes()
    }

    // React to ViewModel notes update
    val allNotes by noteViewModel.notes.collectAsState()
    LaunchedEffect(allNotes) {
        favNotes = allNotes.filter { it.isFavorite && !it.isDeleted }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewMode = if (viewMode == FavViewMode.LIST) FavViewMode.GRID else FavViewMode.LIST
                    }) {
                        Icon(
                            imageVector = if (viewMode == FavViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewAgenda,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->

        if (favNotes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No favorite notes")
            }
        } else {

            if (viewMode == FavViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favNotes) { note ->
                        NoteCard(
                            note = note,
                            folders = folders,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = {
                                val type = detectNoteType(note.content)
                                navController.navigate(Screen.CreateNote.route(note.id, type))
                            },
                            onLongClick = { }
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
                    items(favNotes) { note ->
                        NoteCardGrid(
                            note = note,
                            folders = folders,
                            isSelected = false,
                            isSelectionMode = false,
                            onClick = {
                                val type = detectNoteType(note.content)
                                navController.navigate(Screen.CreateNote.route(note.id, type))
                            },
                            onLongClick = { }
                        )
                    }
                }
            }
        }
    }
}
