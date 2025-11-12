package com.example.notepad

class NoteRepository(private val noteDao: NoteDao) {

    // -----------------------------
    // Folders
    // -----------------------------

    suspend fun insertFolder(folder: FolderEntity): Int =
        noteDao.insertFolder(folder).toInt()

    suspend fun updateFolder(folder: FolderEntity) =
        noteDao.updateFolder(folder)

    suspend fun deleteFolder(folder: FolderEntity) =
        noteDao.deleteFolder(folder)

    suspend fun getFolderById(id: Int): FolderEntity? =
        noteDao.getFolderById(id)

    suspend fun getAllFolders(): List<FolderEntity> =
        noteDao.getAllFolders()

    suspend fun getNoteCountForFolder(folderId: Int): Int =
        noteDao.getNoteCountForFolder(folderId)




    // -----------------------------
    // Notes
    // -----------------------------

    suspend fun insertNote(note: NoteEntity): Int =
        noteDao.insertNote(note).toInt()

    suspend fun updateNote(note: NoteEntity) =
        noteDao.updateNote(note)

    /** Permanent delete — Only used in the Trash screen */
    suspend fun deleteNote(note: NoteEntity) =
        noteDao.deleteNote(note)

    suspend fun getNoteById(id: Int): NoteEntity? =
        noteDao.getNoteById(id)

    suspend fun getNotesByFolder(folderId: Int): List<NoteEntity> =
        noteDao.getNotesByFolder(folderId)

    suspend fun getAllNotes(): List<NoteEntity> =
        noteDao.getAllNotes()

    suspend fun getFavoriteNotes(): List<NoteEntity> =
        noteDao.getFavoriteNotes()

    suspend fun getArchivedNotes(): List<NoteEntity> =
        noteDao.getArchivedNotes()

    suspend fun getTrashNotes(): List<NoteEntity> =
        noteDao.getTrashNotes()


    // -----------------------------
    // State Toggles (Favorite / Archive / Trash)
    // -----------------------------

    suspend fun setFavorite(id: Int, value: Boolean) =
        noteDao.setFavorite(id, value)

    suspend fun setArchived(id: Int, value: Boolean) =
        noteDao.setArchived(id, value)

    /** Soft delete (Move to Trash) */
    suspend fun setDeleted(id: Int, value: Boolean = true) =
        noteDao.setDeleted(id)

    suspend fun restoreFromTrash(id: Int) =
        noteDao.restoreFromTrash(id)
}
