package com.example.notepad

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface NoteDao {

    // -----------------------------
    // FOLDERS
    // -----------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: Int): FolderEntity?

    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    suspend fun getAllFolders(): List<FolderEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE folderId = :folderId AND isDeleted = 0")
    suspend fun getNoteCountForFolder(folderId: Int): Int


    // -----------------------------
    // NOTE CRUD
    // -----------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    /** Permanent delete — Only used in Trash */
    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteEntity?


    // -----------------------------
    // MAIN NOTES LISTS (Filtered)
    // -----------------------------

    /** Default view: Exclude Trash */
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY lastEditedAt DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    /** Filter by folder and must not be deleted */
    @Query("""
        SELECT * FROM notes 
        WHERE folderId = :folderId AND isDeleted = 0 
        ORDER BY lastEditedAt DESC
    """)
    suspend fun getNotesByFolder(folderId: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isDeleted = 0 ORDER BY lastEditedAt DESC")
    suspend fun getFavoriteNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isDeleted = 0 ORDER BY lastEditedAt DESC")
    suspend fun getArchivedNotes(): List<NoteEntity>

    /** Trash screen shows only isDeleted = true */
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY lastEditedAt DESC")
    suspend fun getTrashNotes(): List<NoteEntity>


    // -----------------------------
    // TOGGLES
    // -----------------------------

    @Query("UPDATE notes SET isFavorite = :value, lastEditedAt = :ts WHERE id = :id")
    suspend fun setFavorite(id: Int, value: Boolean, ts: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isArchived = :value, lastEditedAt = :ts WHERE id = :id")
    suspend fun setArchived(id: Int, value: Boolean, ts: Long = System.currentTimeMillis())

    /** Move to Trash (Soft Delete) */
    @Query("UPDATE notes SET isDeleted = 1, lastEditedAt = :ts WHERE id = :id")
    suspend fun setDeleted(id: Int, ts: Long = System.currentTimeMillis())

    /** Restore from Trash */
    @Query("UPDATE notes SET isDeleted = 0, lastEditedAt = :ts WHERE id = :id")
    suspend fun restoreFromTrash(id: Int, ts: Long = System.currentTimeMillis())
}
