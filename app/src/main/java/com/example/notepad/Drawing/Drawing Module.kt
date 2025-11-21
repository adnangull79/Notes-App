package com.example.notepad.Drawing



import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class DrawPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

enum class DrawingTool {
    PEN, MARKER, HIGHLIGHTER, ERASER
}

data class CanvasState(
    val paths: MutableList<DrawPath> = mutableListOf(),
    var currentPath: MutableList<Offset> = mutableListOf(),
    var currentColor: Color = Color.Black,
    var currentStrokeWidth: Float = 8f,
    var currentIsEraser: Boolean = false,
    var redoStack: MutableList<DrawPath> = mutableListOf(),
    var bgColor: Color = Color.White,
    var showGrid: Boolean = false,
    var hasDrawn: Boolean = false
)
