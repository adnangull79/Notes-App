package com.example.notepad.Drawing

import androidx.compose.runtime.remember
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun DrawingCanvas(
    canvasState: CanvasState,
    drawColor: Color,
    strokeWidth: Float,
    currentTool: DrawingTool,
    isDrawingMode: Boolean,
    isCanvasLocked: Boolean,
    recomposeTrigger: Int,
    onRecompose: () -> Unit,
    onEnterDrawingMode: () -> Unit
) {
    val isEraser = currentTool == DrawingTool.ERASER

    val effectiveColor = when (currentTool) {
        DrawingTool.ERASER -> canvasState.bgColor
        DrawingTool.HIGHLIGHTER -> drawColor.copy(alpha = 0.3f)
        else -> drawColor
    }

    val effectiveWidth = when (currentTool) {
        DrawingTool.ERASER -> strokeWidth * 2.5f
        DrawingTool.MARKER -> strokeWidth * 1.3f
        DrawingTool.HIGHLIGHTER -> strokeWidth * 2f
        else -> strokeWidth
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(canvasState.bgColor)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .then(
                if (!isCanvasLocked) {
                    Modifier
                        .pointerInput(effectiveColor, effectiveWidth, isEraser, currentTool) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    onEnterDrawingMode()

                                    canvasState.redoStack.clear()
                                    canvasState.currentPath.clear()
                                    canvasState.currentPath.add(offset)
                                    canvasState.currentColor = effectiveColor
                                    canvasState.currentStrokeWidth = effectiveWidth
                                    canvasState.currentIsEraser = isEraser
                                    onRecompose()
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    canvasState.currentPath.add(change.position)
                                    onRecompose()
                                },
                                onDragEnd = {
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
                                    onRecompose()
                                }
                            )
                        }
                        .pointerInput(effectiveColor, effectiveWidth, isEraser, currentTool) {
                            awaitPointerEventScope {
                                while (true) {
                                    val downEvent = awaitPointerEvent()
                                    val downChange = downEvent.changes.firstOrNull()

                                    if (downChange != null && downChange.pressed && !downChange.previousPressed) {
                                        val downPos = downChange.position
                                        var hasMoved = false
                                        var totalMovement = 0f

                                        // Track if pointer moved
                                        while (true) {
                                            val moveEvent = awaitPointerEvent()
                                            val moveChange = moveEvent.changes.firstOrNull()

                                            if (moveChange == null || !moveChange.pressed) {
                                                // Pointer released
                                                break
                                            }

                                            // Calculate movement distance
                                            val movement = moveChange.positionChange()
                                            totalMovement += abs(movement.x) + abs(movement.y)

                                            // If moved more than 5px, consider it a drag
                                            if (totalMovement > 5f) {
                                                hasMoved = true
                                            }
                                        }

                                        // Only draw dot if:
                                        // 1. Didn't move (tap, not drag)
                                        // 2. Already in drawing mode
                                        // 3. NOT using eraser
                                        if (!hasMoved && isDrawingMode && currentTool != DrawingTool.ERASER) {
                                            canvasState.redoStack.clear()

                                            // Create dot with tool-specific properties
                                            canvasState.paths.add(
                                                DrawPath(
                                                    points = listOf(
                                                        downPos,
                                                        Offset(downPos.x + 0.1f, downPos.y + 0.1f)
                                                    ),
                                                    color = effectiveColor,
                                                    strokeWidth = effectiveWidth,
                                                    isEraser = false
                                                )
                                            )
                                            onRecompose()
                                        } else if (!isDrawingMode) {
                                            // Just activate drawing mode without drawing
                                            onEnterDrawingMode()
                                        }
                                    }
                                }
                            }
                        }
                } else Modifier
            )
    ) {
        key(recomposeTrigger) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
                // Draw grid
                if (canvasState.showGrid) {
                    val spacing = 50.dp.toPx()
                    val gridColor = Color.Gray.copy(alpha = 0.2f)

                    var x = spacing
                    while (x < size.width) {
                        drawLine(
                            gridColor,
                            Offset(x, 0f),
                            Offset(x, size.height),
                            1f
                        )
                        x += spacing
                    }

                    var y = spacing
                    while (y < size.height) {
                        drawLine(
                            gridColor,
                            Offset(0f, y),
                            Offset(size.width, y),
                            1f
                        )
                        y += spacing
                    }
                }

                // Draw saved paths
                canvasState.paths.forEach { pathObj ->
                    if (pathObj.points.size > 1) {
                        val p = Path()
                        pathObj.points.forEachIndexed { i, point ->
                            if (i == 0) p.moveTo(point.x, point.y)
                            else p.lineTo(point.x, point.y)
                        }

                        val renderColor = if (pathObj.isEraser) canvasState.bgColor else pathObj.color

                        drawPath(
                            p,
                            renderColor,
                            style = Stroke(
                                width = pathObj.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Draw current path
                if (canvasState.currentPath.size > 1) {
                    val p = Path()
                    canvasState.currentPath.forEachIndexed { i, point ->
                        if (i == 0) p.moveTo(point.x, point.y)
                        else p.lineTo(point.x, point.y)
                    }

                    val colorToUse =
                        if (canvasState.currentIsEraser) canvasState.bgColor
                        else canvasState.currentColor

                    drawPath(
                        p,
                        colorToUse,
                        style = Stroke(
                            width = canvasState.currentStrokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        // Show hint when empty
        if (!isDrawingMode && canvasState.paths.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onEnterDrawingMode() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Tap to start drawing",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ===== HELPER FUNCTIONS FOR UNDO/REDO (Add these at the bottom of the file) =====

/**
 * Safe removeLast for compatibility with older SDK versions
 */
