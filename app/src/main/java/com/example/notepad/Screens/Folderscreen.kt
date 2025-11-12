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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.notepad.*
import com.example.notepad.UI_theme.CategoryKey
import com.example.notepad.UI_theme.categoryContainerColor
import com.example.notepad.UI_theme.categoryIconColor
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(navController: NavController, noteViewModel: NoteViewModel) {
    val folders by noteViewModel.folders.collectAsState()
    val allNotes by noteViewModel.notes.collectAsState()

    LaunchedEffect(Unit) {
        noteViewModel.loadFolders()
        noteViewModel.loadAllNotes()
    }

    // Count notes per folder
    val folderNoteCounts = remember(folders, allNotes) {
        folders.associateWith { folder ->
            allNotes.count { it.folderId == folder.id }
        }.filter { it.value > 0 } // Only show folders with notes
    }

    // Check if there are checklist notes
    val checklistNotes = allNotes.filter { note ->
        note.checklistItems != null && note.checklistItems.isNotEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar - Transparent
        TopAppBar(
            title = {
                Text(
                    "Folders",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    // TODO: Add create folder dialog
                }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Folder",
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

        // Folder List
        if (folderNoteCounts.isEmpty() && checklistNotes.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Folders",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Folders Yet",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create notes to see folders here",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Regular folders
                items(folderNoteCounts.toList()) { (folder, noteCount) ->
                    FolderCard(
                        folder = folder,
                        noteCount = noteCount,
                        onClick = {
                            navController.navigate(Screen.FolderNotes.route(folder.id))

                            // TODO: Navigate to folder notes view
                            // navController.navigate("folderNotes/${folder.id}")
                        }
                    )
                }

                // Checklist folder (if there are checklist notes)
                if (checklistNotes.isNotEmpty()) {
                    item {
                        FolderCard(
                            folder = FolderEntity(
                                id = -1,
                                name = "Checklists",
                                createdAt = System.currentTimeMillis()
                            ),
                            noteCount = checklistNotes.size,
                            onClick = {
                                navController.navigate(Screen.FolderNotes.route(-1))

                                // TODO: Navigate to checklist notes view
                                // navController.navigate("checklistNotes")
                            }
                        )
                    }
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
fun FolderCard(
    folder: FolderEntity,
    noteCount: Int,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5
    val pureSurface = if (isDark) Color.Black else Color.White

    val (icon, bg, fg) = getFolderIconAndColors(folder)  // Get the icon and colors for the folder

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
            // Folder Icon
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

            // Folder Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = folder.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$noteCount ${if (noteCount == 1) "note" else "notes"}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Arrow Icon
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open folder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}



@Composable
private fun getFolderIconAndColors(
    folder: FolderEntity
): Triple<androidx.compose.ui.graphics.vector.ImageVector, Color, Color> {
    // Use folder's iconName and colorKey from the FolderEntity
    val iconName = folder.iconName ?: "Folder"  // Default to "Folder" if iconName is null
    val colorKey = folder.colorKey ?: CategoryKey.PERSONAL.name  // Default to "PERSONAL" if colorKey is null

    val icon = getFolderIcon(iconName)  // Get the icon based on the folder's icon name
    val bg = categoryContainerColor(CategoryKey.valueOf(colorKey))  // Get the background color based on the category key
    val fg = categoryIconColor(CategoryKey.valueOf(colorKey))  // Get the foreground color based on the category key

    return Triple(icon, bg, fg)
}

// Helper function to get the icon based on the folder's iconName
private fun getFolderIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Person" -> Icons.Default.Person
        "Work" -> Icons.Default.Work
        "Lightbulb" -> Icons.Default.Lightbulb
        "ShoppingCart" -> Icons.Default.ShoppingCart
        "School" -> Icons.Default.School
        "Code" -> Icons.Default.Code
        "FitnessCenter" -> Icons.Default.FitnessCenter
        "Flight" -> Icons.Default.Flight
        "AccountBalance" -> Icons.Default.AccountBalance
        "AccountTree" -> Icons.Default.AccountTree
        "Restaurant" -> Icons.Default.Restaurant
        "MenuBook" -> Icons.Default.MenuBook
        else -> Icons.Default.Folder  // Fallback to "Folder" if no match
    }
}
