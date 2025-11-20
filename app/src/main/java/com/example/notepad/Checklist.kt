package com.example.notepad

import java.util.UUID

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val checked: Boolean,
    val order: Long = System.currentTimeMillis() // Added for initial sorting
)