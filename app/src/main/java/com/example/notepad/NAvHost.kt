// AppNavigation.kt
package com.example.notepad

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notepad.CreatNotes1.CreateNoteScreen

import com.example.notepad.Drawing.DrawingScreen
import com.example.notepad.Screens.*

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val noteViewModel: NoteViewModel = viewModel()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        NavHost(
            navController = navController,
            startDestination = Screen.Notes.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {

            // Main Notes Screen
            composable(Screen.Notes.route) {
                NotesScreen(
                    navController = navController,
                    noteViewModel = noteViewModel
                )
            }
            composable(Screen.Drawing.route) {
                DrawingScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Other screens
            composable(Screen.Folders.route) {
                FoldersScreen(navController, viewModel())
            }

//            composable(Screen.Search.route) {
//                SearchScreen(navController, viewModel())
//            }

            composable(Screen.Settings.route) {
                SettingsScreen(navController)
            }

            composable(Screen.Archived.route) {
                ArchivedScreen(navController, noteViewModel)
            }

            composable(
                route = Screen.FolderNotes.routePattern,
                arguments = listOf(navArgument("folderId") { type = NavType.IntType })
            ) { backStackEntry ->
                val folderId = backStackEntry.arguments?.getInt("folderId") ?: -1
//                FolderNotesScreen(
//                    navController = navController,
//                    noteViewModel = noteViewModel,
//                    folderId = folderId
//                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(navController, noteViewModel)
            }

            composable(Screen.Trash.route) {
                TrashScreen(navController, noteViewModel)
            }

            // Create or Edit Note (TEXT, LIST, AUDIO, DRAWING)
            composable(
                route = Screen.CreateNote.routePattern,
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("noteType") {
                        type = NavType.StringType
                        defaultValue = NoteType.TEXT.name
                    }
                )
            ) { backStackEntry ->

                val rawId = backStackEntry.arguments?.getInt("noteId") ?: -1
                val noteId: Int? = if (rawId == -1) null else rawId

                val typeString = backStackEntry.arguments?.getString("noteType")
                    ?: NoteType.TEXT.name

                val noteType = runCatching { NoteType.valueOf(typeString) }
                    .getOrDefault(NoteType.TEXT)

                // 🔥 FINAL: Route to correct screen based on noteType
                when (noteType) {

                    NoteType.TEXT -> CreateNoteScreen(
                        noteId = noteId,
                        noteViewModel = noteViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )

                    NoteType.LIST -> ListNoteScreen(
                        noteId = noteId,
                        noteViewModel = noteViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )

                    NoteType.AUDIO -> AudioNoteScreen(
                        noteId = noteId,
                        noteViewModel = noteViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )

                    NoteType.DRAWING -> DrawingNoteScreen(
                        noteId = noteId,
                        noteViewModel = noteViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun DrawingNoteScreen(noteId: Int?, noteViewModel: NoteViewModel, onNavigateBack: () -> Boolean) {

}

@Composable
fun AudioNoteScreen(noteId: Int?, noteViewModel: NoteViewModel, onNavigateBack: () -> Boolean) {

}
