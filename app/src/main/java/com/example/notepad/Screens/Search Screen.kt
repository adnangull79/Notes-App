//package com.example.notepad.Screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.text.SpanStyle
//import androidx.compose.ui.text.buildAnnotatedString
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.text.withStyle
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.example.notepad.*
//import kotlinx.coroutines.delay
//import java.text.SimpleDateFormat
//import java.util.*
//
///* ---------------------------------- Search ---------------------------------- */
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun SearchScreen(
//    navController: NavController,
//    noteViewModel: NoteViewModel
//) {
//    val folders by noteViewModel.folders.collectAsState()
//    val notes by noteViewModel.notes.collectAsState()  // Directly using notes from ViewModel
//
//    var searchQuery by remember { mutableStateOf("") }
//    var debouncedQuery by remember { mutableStateOf("") }
//
//    // Debounce typing (fast + smooth)
//    LaunchedEffect(searchQuery) {
//        delay(300)
//        debouncedQuery = searchQuery.trim() // Update the query after a small delay
//    }
//
//    // Build filtered results from actual data
//    val results = remember(debouncedQuery, notes, folders) {
//        if (debouncedQuery.isBlank()) emptyList()
//        else {
//            val q = debouncedQuery.lowercase()
//            notes.asSequence()
//                .mapNotNull { note ->
//                    val folder = folders.find { it.id == note.folderId }
//                    val inTitle = note.title.contains(q, ignoreCase = true)
//                    val inContent = (note.content ?: "").contains(q, ignoreCase = true)
//                    val inChecklist =
//                        (note.checklistItems?.any { it.text.contains(q, ignoreCase = true) } == true)
//
//                    if (inTitle || inContent || inChecklist) {
//                        val lastModified = formatRelative(note.lastEditedAt)
//                        val noteType = when {
//                            note.checklistItems?.isNotEmpty() == true -> NoteType.LIST
//                            else -> detectNoteTypeFromContent(note.content)
//                        }
//
//                        val (icon, _) = iconForNote(noteType, folder)
//                        val snippet = when {
//                            inTitle -> note.title
//                            inChecklist -> // build a tiny snippet from the first matching checklist line
//                                note.checklistItems?.firstOrNull { it.text.contains(q, true) }?.text
//                                    ?: (note.content ?: "")
//                            else -> note.content ?: ""
//                        }
//
//                        SearchResult(
//                            id = note.id,
//                            title = note.title,
//                            description = snippet,
//                            folder = folder?.name,
//                            lastModified = lastModified,
//                            icon = icon,
//                            noteType = noteType
//                        )
//                    } else null
//                }
//                // Light ranking: title hits first, then recent
//                .sortedWith(
//                    compareByDescending<SearchResult> { it.title.contains(q, true) }
//                        .thenByDescending { result ->
//                            // parse relative isn’t sortable; instead sort by original note lastEditedAt:
//                            notes.find { it.id == result.id }?.lastEditedAt ?: 0L
//                        }
//                )
//                .toList()
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(MaterialTheme.colorScheme.background)
//    ) {
//        /* Top App Bar */
//        TopAppBar(
//            title = {
//                Text(
//                    "Search",
//                    fontSize = 20.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    color = MaterialTheme.colorScheme.onBackground
//                )
//            },
//            colors = TopAppBarDefaults.topAppBarColors(
//                containerColor = Color.Transparent,
//                titleContentColor = MaterialTheme.colorScheme.onBackground
//            )
//        )
//
//        /* Search Bar */
//        Surface(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//                .border(
//                    width = 2.dp,
//                    color = MaterialTheme.colorScheme.primary,
//                    shape = RoundedCornerShape(12.dp)
//                ),
//            color = MaterialTheme.colorScheme.surface,
//            shape = RoundedCornerShape(12.dp)
//        ) {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 12.dp, vertical = 4.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Search,
//                    contentDescription = "Search",
//                    tint = MaterialTheme.colorScheme.primary,
//                    modifier = Modifier.size(22.dp)
//                )
//                Spacer(modifier = Modifier.width(12.dp))
//
//                TextField(
//                    value = searchQuery,
//                    onValueChange = { searchQuery = it },
//                    placeholder = {
//                        Text(
//                            "Search notes, checklists…",
//                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
//                        )
//                    },
//                    colors = TextFieldDefaults.colors(
//                        focusedContainerColor = MaterialTheme.colorScheme.surface,
//                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
//                        disabledContainerColor = MaterialTheme.colorScheme.surface,
//                        focusedIndicatorColor = Color.Transparent,
//                        unfocusedIndicatorColor = Color.Transparent,
//                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
//                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
//                    ),
//                    modifier = Modifier.weight(1f),
//                    singleLine = true
//                )
//
//                if (searchQuery.isNotEmpty()) {
//                    IconButton(
//                        onClick = { searchQuery = "" },
//                        modifier = Modifier.size(28.dp)
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Close,
//                            contentDescription = "Clear",
//                            tint = MaterialTheme.colorScheme.primary,
//                            modifier = Modifier.size(20.dp)
//                        )
//                    }
//                }
//            }
//        }
//
//        /* Result count */
//        if (debouncedQuery.isNotEmpty()) {
//            Text(
//                text = "${results.size} result${if (results.size == 1) "" else "s"} for ‘$debouncedQuery’",
//                fontSize = 12.sp,
//                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
//            )
//        }
//
//        /* Body */
//        when {
//            debouncedQuery.isEmpty() -> {
//                // Calm empty state
//                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                        Icon(
//                            imageVector = Icons.Default.Search,
//                            contentDescription = null,
//                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
//                            modifier = Modifier.size(64.dp)
//                        )
//                        Spacer(Modifier.height(16.dp))
//                        Text(
//                            "Search your notes",
//                            fontSize = 16.sp,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            fontWeight = FontWeight.Medium
//                        )
//                        Text(
//                            "Type a keyword to find Notes",
//                            fontSize = 14.sp,
//
//                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
//                        )
//                    }
//                }
//            }
//            results.isEmpty() -> {
//                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                        Icon(
//                            imageVector = Icons.Default.SearchOff,
//                            contentDescription = null,
//                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
//                            modifier = Modifier.size(64.dp)
//                        )
//                        Spacer(Modifier.height(16.dp))
//                        Text(
//                            "No results",
//                            fontSize = 16.sp,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            fontWeight = FontWeight.Medium
//                        )
//                        Text(
//                            "Try different keywords",
//                            fontSize = 14.sp,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
//                        )
//                    }
//                }
//            }
//            else -> {
//                LazyColumn(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(horizontal = 16.dp),
//                    verticalArrangement = Arrangement.spacedBy(12.dp),
//                    contentPadding = PaddingValues(bottom = 80.dp)
//                ) {
//                    items(results) { result ->
//                        SearchResultCard(
//                            result = result,
//                            searchQuery = debouncedQuery,
//                            onOpen = {
//                                navController.navigate(
//                                    Screen.CreateNote.route(
//                                        noteId = result.id,
//                                        noteType = result.noteType
//                                    )
//                                )
//                            }
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
///* ---------------------------- UI bits & helpers ---------------------------- */
//
//private data class SearchResult(
//    val id: Int,
//    val title: String,
//    val description: String,  // may be title, content, or checklist line
//    val folder: String?,
//    val lastModified: String,
//    val icon: ImageVector,
//    val noteType: NoteType
//)
//
//@Composable
//private fun SearchResultCard(
//    result: SearchResult,
//    searchQuery: String,
//    onOpen: () -> Unit
//) {
//    Surface(
//        onClick = onOpen, // Surface handles interaction without the ripple crash
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(12.dp),
//        color = MaterialTheme.colorScheme.surface,
//        shadowElevation = 1.dp
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                imageVector = result.icon,
//                contentDescription = null,
//                tint = MaterialTheme.colorScheme.onSurfaceVariant,
//                modifier = Modifier.size(24.dp)
//            )
//
//            Spacer(modifier = Modifier.width(16.dp))
//
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = highlightSearchTerm(result.title, searchQuery),
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    color = MaterialTheme.colorScheme.onSurface,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//
//                Spacer(modifier = Modifier.height(4.dp))
//
//                if (result.description.isNotBlank()) {
//                    Text(
//                        text = highlightSearchTerm(result.description, searchQuery),
//                        fontSize = 14.sp,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        maxLines = 2,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    if (!result.folder.isNullOrBlank()) {
//                        Text(
//                            text = "Folder: ${result.folder}",
//                            fontSize = 11.sp,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
//                        )
//                        Text(
//                            text = " • ",
//                            fontSize = 11.sp,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
//                        )
//                    }
//                    Text(
//                        text = "Last edited: ${result.lastModified}",
//                        fontSize = 11.sp,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
//                    )
//                }
//            }
//
//            Icon(
//                imageVector = Icons.Default.ChevronRight,
//                contentDescription = "Open",
//                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
//                modifier = Modifier.size(24.dp)
//            )
//        }
//    }
//}
//
//@Composable
//private fun highlightSearchTerm(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
//    if (query.isBlank()) return buildAnnotatedString { append(text) }
//
//    val lowerText = text.lowercase()
//    val lowerQuery = query.lowercase()
//    var currentIndex = 0
//
//    return buildAnnotatedString {
//        while (currentIndex < text.length) {
//            val idx = lowerText.indexOf(lowerQuery, startIndex = currentIndex)
//            if (idx == -1) {
//                append(text.substring(currentIndex))
//                break
//            }
//            append(text.substring(currentIndex, idx))
//            withStyle(
//                style = SpanStyle(
//                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
//                    fontWeight = FontWeight.Bold
//                )
//            ) {
//                append(text.substring(idx, idx + query.length))
//            }
//            currentIndex = idx + query.length
//        }
//    }
//}
//
//private fun iconForNote(
//    type: NoteType,
//    folder: FolderEntity?
//): Pair<ImageVector, Color> {
//    return when (type) {
//        NoteType.LIST -> Icons.Default.Checklist to Color(0xFFE8F5E9)
//        NoteType.AUDIO -> Icons.Default.Mic to Color(0xFFE3F2FD)
//        NoteType.DRAWING -> Icons.Default.Brush to Color(0xFFFFF3E0)
//        NoteType.TEXT -> {
//            val name = (folder?.name ?: "").lowercase(Locale.getDefault())
//            when {
//                listOf("code", "dev", "snippet").any { name.contains(it) } ->
//                    Icons.Default.Code to Color(0xFFF4F0FF)
//                listOf("idea", "ideas", "brainstorm").any { name.contains(it) } ->
//                    Icons.Default.Lightbulb to Color(0xFFFFF9C4)
//                listOf("work", "meeting", "docs", "note").any { name.contains(it) } ->
//                    Icons.Default.Description to Color(0xFFF2F6FF)
//                else -> Icons.Default.Description to Color(0xFFF5F7FA)
//            }
//        }
//    }
//}
//
//
//
//private fun formatRelative(ts: Long): String {
//    val now = System.currentTimeMillis()
//    val diff = now - ts
//    return when {
//        diff < 60_000 -> "Just now"
//        diff < 3_600_000 -> "${diff / 60_000} min ago"
//        diff < 86_400_000 -> "${diff / 3_600_000} h ago"
//        diff < 604_800_000 -> "${diff / 86_400_000} d ago"
//        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(ts))
//    }
//}
