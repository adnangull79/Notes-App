package com.example.notepad.Drawing


import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Reusable Drawing Manager for handling canvas operations
 * Use this instead of directly manipulating canvasState
 */
class DrawingManager(private val canvasState: CanvasState) {

    /**
     * Undo the last drawing action
     * Returns true if undo was successful, false if nothing to undo
     */
    fun undo(): Boolean {
        return if (canvasState.paths.isNotEmpty()) {
            val lastIndex = canvasState.paths.size - 1
            val removed = canvasState.paths.removeAt(lastIndex)
            canvasState.redoStack.add(removed)
            true
        } else {
            false
        }
    }

    /**
     * Redo the last undone action
     * Returns true if redo was successful, false if nothing to redo
     */
    fun redo(): Boolean {
        return if (canvasState.redoStack.isNotEmpty()) {
            val lastIndex = canvasState.redoStack.size - 1
            val restored = canvasState.redoStack.removeAt(lastIndex)
            canvasState.paths.add(restored)
            true
        } else {
            false
        }
    }

    /**
     * Check if undo is available
     */
    fun canUndo(): Boolean = canvasState.paths.isNotEmpty() || canvasState.currentPath.isNotEmpty()

    /**
     * Check if redo is available
     */
    fun canRedo(): Boolean = canvasState.redoStack.isNotEmpty()

    /**
     * Start a new drawing stroke
     */
    fun startDrawing(offset: Offset, color: Color, strokeWidth: Float, isEraser: Boolean) {
        canvasState.redoStack.clear()
        canvasState.currentPath.clear()
        canvasState.currentPath.add(offset)
        canvasState.currentColor = color
        canvasState.currentStrokeWidth = strokeWidth
        canvasState.currentIsEraser = isEraser
    }

    /**
     * Add point to current drawing stroke
     */
    fun addPoint(offset: Offset) {
        canvasState.currentPath.add(offset)
    }

    /**
     * Finish current drawing stroke
     */
    fun finishDrawing() {
        if (canvasState.currentPath.size > 1) {
            canvasState.paths.add(
                DrawPath(
                    points = canvasState.currentPath.toList(),
                    color = canvasState.currentColor,
                    strokeWidth = canvasState.currentStrokeWidth,
                    isEraser = canvasState.currentIsEraser
                )
            )
        }
        canvasState.currentPath.clear()
    }

    /**
     * Add a dot (single tap drawing)
     */
    fun addDot(position: Offset, color: Color, strokeWidth: Float) {
        canvasState.redoStack.clear()
        canvasState.paths.add(
            DrawPath(
                points = listOf(
                    position,
                    Offset(position.x + 0.1f, position.y + 0.1f)
                ),
                color = color,
                strokeWidth = strokeWidth,
                isEraser = false
            )
        )
    }

    /**
     * Clear all drawings
     */
    fun clearAll() {
        canvasState.paths.clear()
        canvasState.currentPath.clear()
        canvasState.redoStack.clear()
        canvasState.hasDrawn = false
    }

    /**
     * Change canvas background color
     */
    fun setBackgroundColor(color: Color) {
        canvasState.bgColor = color
    }

    /**
     * Toggle grid visibility
     */
    fun setGridVisible(visible: Boolean) {
        canvasState.showGrid = visible
    }
}

/**
 * Extension function to safely remove last item from list
 * Compatible with all SDK versions
 */
fun <T> MutableList<T>.removeLastSafe(): T? {
    return if (this.isNotEmpty()) {
        this.removeAt(this.size - 1)
    } else {
        null
    }
}

/**
 * Extension function for SnapshotStateList
 */
fun <T> SnapshotStateList<T>.removeLastSafe(): T? {
    return if (this.isNotEmpty()) {
        this.removeAt(this.size - 1)
    } else {
        null
    }
}