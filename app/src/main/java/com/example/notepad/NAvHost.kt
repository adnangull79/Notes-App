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
import com.example.notepad.Audio.AudioNotesScreen

import com.example.notepad.CreatNotes1.CreateNoteScreen
import com.example.notepad.Drawing.DrawingScreen
import com.example.notepad.Screens.*
// ✅ ADD THIS IMPORT


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

            // Drawing route with optional noteId
            composable(
                route = "drawing?noteId={noteId}",
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val rawId = backStackEntry.arguments?.getInt("noteId") ?: -1
                val noteId: Int? = if (rawId == -1) null else rawId

                DrawingScreen(
                    onNavigateBack = { navController.popBackStack() },
                    noteViewModel = noteViewModel,
                    noteId = noteId
                )
            }

            // Other screens
            composable(Screen.Folders.route) {
                FoldersScreen(navController, viewModel())
            }

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

                // 🔥 Route to correct screen based on noteType
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

                    // ✅ UPDATED: Route to AudioNotesScreen
                    NoteType.AUDIO -> AudioNotesScreen(
                        noteId = noteId,
                        noteViewModel = noteViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )

                    NoteType.DRAWING -> DrawingScreen(
                        noteId = noteId,
                        noteViewModel = noteViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}