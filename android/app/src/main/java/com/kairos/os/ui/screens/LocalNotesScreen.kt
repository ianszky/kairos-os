package com.kairos.os.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kairos.os.data.db.LocalNote
import com.kairos.os.domain.tools.LocalNotesController
import com.kairos.os.ui.googleSansFont
import com.kairos.os.ui.parseMarkdownToAnnotatedString
import kotlinx.coroutines.launch

enum class NoteSaveState {
    GRAY_CHECK,
    ORANGE_SAVE,
    ORANGE_CHECK
}

@Composable
fun LocalNotesScreen(
    notesController: LocalNotesController,
    onBack: () -> Unit,
    onNoteEditorStateChanged: (isEditing: Boolean, saveState: NoteSaveState, onSave: (() -> Unit)?, onCancelEdit: (() -> Unit)?) -> Unit = { _, _, _, _ -> }
) {
    val coroutineScope = rememberCoroutineScope()
    var notes by remember { mutableStateOf<List<LocalNote>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedNote by remember { mutableStateOf<LocalNote?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    fun refreshNotes() {
        coroutineScope.launch {
            notes = if (searchQuery.isBlank()) {
                notesController.getAllNotes()
            } else {
                notesController.searchNotes(searchQuery)
            }
        }
    }

    LaunchedEffect(searchQuery) {
        refreshNotes()
    }

    LaunchedEffect(isEditing) {
        if (!isEditing) {
            onNoteEditorStateChanged(false, NoteSaveState.GRAY_CHECK, null, null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp)
            .padding(horizontal = 24.dp)
    ) {
        if (isEditing) {
            NoteEditor(
                note = selectedNote,
                onSave = { title, content ->
                    coroutineScope.launch {
                        notesController.createNote(title, content)
                        refreshNotes()
                    }
                },
                onStateChange = { saveState, onSaveAction ->
                    onNoteEditorStateChanged(true, saveState, onSaveAction, {
                        isEditing = false
                        selectedNote = null
                    })
                }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Full-width Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search notes...", fontFamily = googleSansFont) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )

                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "No Notes",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "No Local Notes" else "No Notes Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = googleSansFont),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "Tap '+' floating button or prompt @kainotes in chat." else "Try a different search query.",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = {
                                    selectedNote = note
                                    isEditing = true
                                },
                                onDelete = {
                                    coroutineScope.launch {
                                        notesController.deleteNote(note.id)
                                        refreshNotes()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Floating Circular '+' Button at Bottom Right with sufficient margins
            FloatingActionButton(
                onClick = {
                    selectedNote = null
                    isEditing = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 8.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Note",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun NoteCard(
    note: LocalNote,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val codeText = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = googleSansFont,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = parseMarkdownToAnnotatedString(note.content, codeBg, codeText),
                    fontSize = 14.sp,
                    fontFamily = googleSansFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Note",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun NoteEditor(
    note: LocalNote?,
    onSave: (String, String) -> Unit,
    onStateChange: (NoteSaveState, (() -> Unit)?) -> Unit
) {
    var title by remember(note) { mutableStateOf(note?.title ?: "") }
    var content by remember(note) { mutableStateOf(note?.content ?: "") }

    val initialTitle = remember(note) { note?.title ?: "" }
    val initialContent = remember(note) { note?.content ?: "" }

    var isContentEditing by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }

    val hasChanges = title != initialTitle || content != initialContent

    val currentSaveState = when {
        isSaved -> NoteSaveState.ORANGE_CHECK
        hasChanges -> NoteSaveState.ORANGE_SAVE
        else -> NoteSaveState.GRAY_CHECK
    }

    val executeSave = {
        if (hasChanges) {
            onSave(title, content)
            isSaved = true
        }
    }

    LaunchedEffect(currentSaveState, title, content) {
        onStateChange(currentSaveState, executeSave)
    }

    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val codeText = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp)
    ) {
        // Borderless Title with 24.sp bold and ONLY a bottom border line (no full box outline)
        TextField(
            value = title,
            onValueChange = {
                title = it
                isSaved = false
            },
            placeholder = {
                Text(
                    "Note Title",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = googleSansFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = googleSansFont,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Note Content: Markdown Preview when not clicked, Edit Mode when clicked
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            if (!isContentEditing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isContentEditing = true }
                        .padding(8.dp)
                ) {
                    if (content.isBlank()) {
                        Text(
                            text = "Tap to write note content...",
                            fontSize = 16.sp,
                            fontFamily = googleSansFont,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    } else {
                        Text(
                            text = parseMarkdownToAnnotatedString(content, codeBg, codeText),
                            fontSize = 16.sp,
                            fontFamily = googleSansFont,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            } else {
                TextField(
                    value = content,
                    onValueChange = {
                        content = it
                        isSaved = false
                    },
                    placeholder = { Text("Note Content (Markdown supported)...", fontFamily = googleSansFont) },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        fontFamily = googleSansFont,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
