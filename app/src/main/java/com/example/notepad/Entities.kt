package com.example.notepad

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.notepad.CreatNotes1.ChecklistItem

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folderId")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val folderId: Int,
    val title: String,
    val content: String?,
    val checklistItems: List<ChecklistItem>?, // If list note type is used
    val audioPath: String?,
    val drawingData: String?,  // ✅ CHANGED: stores JSON string of drawing strokes
    val createdAt: Long = System.currentTimeMillis(),
    val lastEditedAt: Long = System.currentTimeMillis(),

    // NEW FIELDS FOR FAVORITE, ARCHIVE, TRASH
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false, // Soft delete - goes to Trash instead of permanent delete
    val noteType: NoteType = NoteType.TEXT
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorKey: String? = null,
    val iconName: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)