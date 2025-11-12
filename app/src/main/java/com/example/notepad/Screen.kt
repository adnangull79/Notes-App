package com.example.notepad

// Note type enum
enum class NoteType {
    TEXT,
    AUDIO,
    DRAWING,
    LIST;

    companion object {
        fun fromString(value: String?): NoteType {
            return try {
                valueOf(value ?: "TEXT")
            } catch (e: IllegalArgumentException) {
                TEXT
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Notes : Screen("notes")
    object Folders : Screen("folders")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object Favorites : Screen("favorites")
    object Archived : Screen("archived")
    object Trash : Screen("trash")




    object FolderNotes : Screen("folderNotes") {
        val routePattern = "folderNotes/{folderId}"
        fun route(folderId: Int) = "folderNotes/$folderId"
    }

    object CreateNote {
        const val base = "create_note"
        // Pattern includes both noteId and noteType
        const val routePattern = "$base?noteId={noteId}&noteType={noteType}"

        // Helper for navigation with optional noteId and noteType
        fun route(noteId: Int? = null, noteType: NoteType = NoteType.TEXT): String {
            return if (noteId == null) {
                "$base?noteType=${noteType.name}"
            } else {
                "$base?noteId=$noteId&noteType=${noteType.name}"
            }
        }
    }
}