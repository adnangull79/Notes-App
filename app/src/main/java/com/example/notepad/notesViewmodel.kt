package com.example.notepad

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository

    // ------------- FOLDERS -------------
    private val _folders = MutableStateFlow<List<FolderEntity>>(emptyList())
    val folders: StateFlow<List<FolderEntity>> = _folders

    // ------------- NOTES (bound to UI) -------------
    private val _notes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val notes: StateFlow<List<NoteEntity>> = _notes

    // Selected folder (null = show all)
    private val _selectedFolderId = MutableStateFlow<Int?>(null)
    val selectedFolderId: StateFlow<Int?> = _selectedFolderId

    init {
        val dao = NotesDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(dao)

        viewModelScope.launch {
            initializeDefaultFolders()
            loadFolders()
            loadAllNotes()
        }
    }

    // Create default folders on first run
    private suspend fun initializeDefaultFolders() {
        val existing = repository.getAllFolders()
        val defaults = listOf(
            Triple("Personal", "PERSONAL", "Person"),
            Triple("Work", "WORK", "Work"),
            Triple("Ideas", "IDEAS", "Lightbulb")
        )
        for ((name, colorKey, iconName) in defaults) {
            if (existing.none { it.name == name && it.isDefault }) {
                repository.insertFolder(
                    FolderEntity(
                        name = name,
                        colorKey = colorKey,
                        iconName = iconName,
                        isDefault = true
                    )
                )
            }
        }
    }

    // ------------- FOLDER ACTIONS -------------
    fun loadFolders() = viewModelScope.launch {
        _folders.value = repository.getAllFolders()
    }

    fun createFolder(name: String, colorKey: String? = null, iconName: String? = null) {
        viewModelScope.launch {
            repository.insertFolder(FolderEntity(name = name, colorKey = colorKey, iconName = iconName))
            loadFolders()
        }
    }

    fun updateFolderMeta(folderId: Int, name: String? = null, colorKey: String? = null, iconName: String? = null) {
        viewModelScope.launch {
            val existing = repository.getFolderById(folderId) ?: return@launch
            repository.updateFolder(
                existing.copy(
                    name = name ?: existing.name,
                    colorKey = colorKey ?: existing.colorKey,
                    iconName = iconName ?: existing.iconName
                )
            )
            loadFolders()
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            if (!folder.isDefault) {
                repository.deleteFolder(folder)
                loadFolders()
                loadAllNotes() // refresh notes view
                if (_selectedFolderId.value == folder.id) _selectedFolderId.value = null
            }
        }
    }

    fun selectFolder(folderId: Int?) {
        _selectedFolderId.value = folderId
        if (folderId == null) loadAllNotes() else loadNotesByFolder(folderId)
    }

    // ------------- NOTE LOADING -------------
    fun loadAllNotes() = viewModelScope.launch {
        _notes.value = repository.getAllNotes()
    }

    fun loadNotesByFolder(folderId: Int) = viewModelScope.launch {
        _notes.value = repository.getNotesByFolder(folderId)
    }

    fun loadFavoriteNotes() = viewModelScope.launch {
        _notes.value = repository.getFavoriteNotes()
    }

    fun loadArchivedNotes() = viewModelScope.launch {
        _notes.value = repository.getArchivedNotes()
    }

    fun loadTrashNotes() = viewModelScope.launch {
        _notes.value = repository.getTrashNotes()
    }

    // ✅ **Missing function added here**
    suspend fun getNoteById(noteId: Int): NoteEntity? {
        return repository.getNoteById(noteId)
    }

    // ------------- CREATE / UPDATE NOTES -------------
    fun createNote(folderId: Int, title: String, content: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.insertNote(
                NoteEntity(
                    folderId = folderId,
                    title = title,
                    content = content,
                    checklistItems = null,
                    audioPath = null,
                    drawingPath = null,
                    createdAt = now,
                    lastEditedAt = now
                )
            )
            refreshAfterChange()
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(lastEditedAt = System.currentTimeMillis()))
            refreshAfterChange()
        }
    }

    fun updateNote(id: Int, folderId: Int? = null, title: String? = null, content: String? = null) {
        viewModelScope.launch {
            val existing = repository.getNoteById(id) ?: return@launch
            repository.updateNote(
                existing.copy(
                    folderId = folderId ?: existing.folderId,
                    title = title ?: existing.title,
                    content = content ?: existing.content,
                    lastEditedAt = System.currentTimeMillis()
                )
            )
            refreshAfterChange()
        }
    }

    private fun refreshAfterChange() {
        _selectedFolderId.value?.let { loadNotesByFolder(it) } ?: loadAllNotes()
    }

    // ------------- FAVORITE / ARCHIVE / TRASH -------------
    fun toggleFavorite(noteId: Int, currentState: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(noteId, !currentState)
            refreshAfterChange()
        }
    }

    fun toggleArchive(noteId: Int, currentState: Boolean) {
        viewModelScope.launch {
            repository.setArchived(noteId, !currentState)
            refreshAfterChange()
        }
    }

    fun moveToTrash(noteId: Int) {
        viewModelScope.launch {
            repository.setDeleted(noteId, true)
            refreshAfterChange()
        }
    }

    fun restoreFromTrash(noteId: Int) {
        viewModelScope.launch {
            repository.restoreFromTrash(noteId)
            refreshAfterChange()
        }
    }

    fun permanentlyDelete(noteId: Int) {
        viewModelScope.launch {
            repository.getNoteById(noteId)?.let { repository.deleteNote(it) }
            refreshAfterChange()
        }
    }

    // ------------- TEXT FORMAT STATE -------------
    data class TextFormatting(
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderline: Boolean = false,
        val textColor: Color = Color.Black
    ) {
        fun toSpanStyle(): SpanStyle = SpanStyle(
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None,
            color = textColor
        )
    }
}
