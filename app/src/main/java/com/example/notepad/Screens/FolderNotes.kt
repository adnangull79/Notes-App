package com.example.notepad.Screens


import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.notepad.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderNotesScreen(
    navController: NavController,
    noteViewModel: NoteViewModel,
    folderId: Int
) {
    val folders by noteViewModel.folders.collectAsState()
    val allNotes by noteViewModel.notes.collectAsState()

    LaunchedEffect(Unit) {
        noteViewModel.loadFolders()
        noteViewModel.loadAllNotes()
    }

    // Get the current folder
    val currentFolder = remember(folders, folderId) {
        if (folderId == -1) {
            // Checklist folder
            FolderEntity(id = -1, name = "Checklists", createdAt = System.currentTimeMillis())
        } else {
            folders.find { it.id == folderId }
        }
    }

    // Filter notes for this folder
    val folderNotes = remember(allNotes, folderId) {
        if (folderId == -1) {
            // Show checklist notes
            allNotes.filter { note ->
                note.checklistItems != null && note.checklistItems.isNotEmpty()
            }
        } else {
            allNotes.filter { it.folderId == folderId }
        }.sortedByDescending { it.lastEditedAt }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = currentFolder?.name ?: "Folder",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    // TODO: Add filter/sort options
                }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        // Notes count
        Text(
            text = "${folderNotes.size} ${if (folderNotes.size == 1) "note" else "notes"}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Notes List
        if (folderNotes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "No notes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notes in this folder",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Create a note to get started",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(folderNotes) { note ->
                    FolderNoteCard(
                        note = note,
                        folders = folders,
                        onClick = {
                            // Navigate to edit note
                            val noteType = if (note.checklistItems != null && note.checklistItems.isNotEmpty()) {
                                NoteType.LIST
                            } else {
                                detectNoteTypeFromContent(note.content)
                            }
                            navController.navigate(Screen.CreateNote.route(note.id, noteType))
                        }
                    )
                }

                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun FolderNoteCard(
    note: NoteEntity,
    folders: List<FolderEntity>,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5
    val pureSurface = if (isDark) Color.Black else Color.White

    val folder = folders.find { it.id == note.folderId }

    // Check if it's a checklist note
    val isChecklistNote = note.checklistItems != null && note.checklistItems.isNotEmpty()
    val noteType = if (isChecklistNote) NoteType.LIST else detectNoteTypeFromContent(note.content)

    val (icon, bg, fg) = getFolderNoteIconAndColors(note, folder, noteType)
    val absoluteDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.lastEditedAt))
    val editedRelative = formatDateRelative(note.lastEditedAt)

    Surface(
        color = pureSurface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Note Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(bg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Note Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = absoluteDate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Display checklist preview or content preview
                if (isChecklistNote) {
                    val items = note.checklistItems!!
                    val checkedCount = items.count { it.isChecked }
                    val totalCount = items.size
                    Text(
                        text = "$checkedCount/$totalCount completed",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (!note.content.isNullOrBlank()) {
                    Text(
                        text = note.content,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Edited • $editedRelative",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Helper function to detect note type from content
fun detectNoteTypeFromContent(content: String?): NoteType {
    if (content.isNullOrBlank()) return NoteType.TEXT

    val lines = content.lines()
    val checklistLines = lines.count {
        it.trim().startsWith("[x]") || it.trim().startsWith("[ ]")
    }

    val nonEmptyLines = lines.count { it.isNotBlank() }
    return if (nonEmptyLines > 0 && checklistLines.toFloat() / nonEmptyLines > 0.5f) {
        NoteType.LIST
    } else {
        NoteType.TEXT
    }
}

@Composable
private fun getFolderNoteIconAndColors(
    note: NoteEntity,
    folder: FolderEntity?,
    noteType: NoteType
): Triple<androidx.compose.ui.graphics.vector.ImageVector, Color, Color> {
    // If it's a list note, always show checklist icon
    if (noteType == NoteType.LIST) {
        return Triple(
            Icons.Default.Checklist,
            Color(0xFFE8F5E9), // Light green pastel
            Color(0xFF4CAF50)  // Green
        )
    }

    val name = (folder?.name ?: note.title).lowercase(Locale.getDefault())

    val (icon, base) = when {
        listOf("grocery", "shopping").any { name.contains(it) } ->
            Icons.Default.ShoppingCart to Color(0xFFEFF6FF)
        listOf("brainstorm", "idea", "ideas").any { name.contains(it) } ->
            Icons.Default.Lightbulb to Color(0xFFFFF9C4)
        listOf("anniversary", "love", "personal").any { name.contains(it) } ->
            Icons.Default.Favorite to Color(0xFFFFEEF1)
        listOf("code", "snippet", "dev").any { name.contains(it) } ->
            Icons.Default.Code to Color(0xFFF4F0FF)
        listOf("meeting", "recap", "docs", "note", "work").any { name.contains(it) } ->
            Icons.Default.Description to Color(0xFFF2F6FF)
        else ->
            Icons.Default.Description to Color(0xFFF5F7FA)
    }

    val fg = when (icon) {
        Icons.Default.Favorite -> Color(0xFFEF476F)
        Icons.Default.Lightbulb -> Color(0xFFFBC02D)
        Icons.Default.Code -> Color(0xFF7C3AED)
        Icons.Default.ShoppingCart -> Color(0xFF2563EB)
        else -> MaterialTheme.colorScheme.primary
    }

    return Triple(icon, base, fg)
}

fun formatDateRelative(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} minutes ago"
        diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
        diff < 604_800_000 -> "${diff / 86_400_000} days ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}