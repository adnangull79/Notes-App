package com.example.notepad.Drawing

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.TextUnit

@Composable
fun DrawingBottomBar(
    currentTool: DrawingTool,
    drawColor: Color,
    strokeWidth: Float,
    canvasBgColor: Color,
    showGridLines: Boolean,
    showToolOptions: Boolean,
    showColorPicker: Boolean,
    showCanvasOptions: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    previousToolbarState: String?,
    onToolChange: (DrawingTool) -> Unit,
    onColorChange: (Color) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onCanvasBgChange: (Color) -> Unit,
    onGridToggle: (Boolean) -> Unit,
    onShowColorPicker: () -> Unit,
    onShowCanvasOptions: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCloseSubPanel: () -> Unit,
    onBackToToolOptions: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        val showAnyPanel = showToolOptions || showColorPicker || showCanvasOptions

        if (showAnyPanel) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (showColorPicker || showCanvasOptions) {
                                when (previousToolbarState) {
                                    "TOOL_OPTIONS" -> onBackToToolOptions()
                                    else -> onCloseSubPanel()
                                }
                            } else {
                                onCloseSubPanel()
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (showColorPicker || showCanvasOptions) Icons.Default.ArrowBack
                            else Icons.Default.Close,
                            "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    when {
                        showToolOptions -> {
                            ToolOptionsPanel(
                                currentTool = currentTool,
                                strokeWidth = strokeWidth,
                                drawColor = drawColor,
                                onStrokeWidthChange = onStrokeWidthChange,
                                onShowColorPicker = onShowColorPicker
                            )
                        }

                        showColorPicker -> {
                            ColorPickerPanel(
                                currentColor = drawColor,
                                onColorChange = onColorChange
                            )
                        }

                        showCanvasOptions -> {
                            CanvasOptionsPanel(
                                bgColor = canvasBgColor,
                                showGrid = showGridLines,
                                onBgColorChange = onCanvasBgChange,
                                onGridToggle = onGridToggle
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { onToolChange(DrawingTool.PEN) },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (currentTool == DrawingTool.PEN)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.BorderColor, "Pen", modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { onToolChange(DrawingTool.MARKER) },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (currentTool == DrawingTool.MARKER)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Brush, "Marker", modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { onToolChange(DrawingTool.HIGHLIGHTER) },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (currentTool == DrawingTool.HIGHLIGHTER)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Highlight, "Highlighter", modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { onToolChange(DrawingTool.ERASER) },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (currentTool == DrawingTool.ERASER)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.AutoFixOff, "Eraser", modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = onShowCanvasOptions,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            "Canvas",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            "Undo",
                            tint = if (canUndo)
                                MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Redo,
                            "Redo",
                            tint = if (canRedo)
                                MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.ToolOptionsPanel(
    currentTool: DrawingTool,
    strokeWidth: Float,
    drawColor: Color,
    onStrokeWidthChange: (Float) -> Unit,
    onShowColorPicker: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .weight(1f)
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Text(
                when (currentTool) {
                    DrawingTool.PEN -> "Pen"
                    DrawingTool.MARKER -> "Marker"
                    DrawingTool.HIGHLIGHTER -> "Highlighter"
                    DrawingTool.ERASER -> "Eraser"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        item {
            Column(modifier = Modifier.width(200.dp)) {
                Text("Size: ${strokeWidth.toInt()}px", fontSize = 12.sp)
                Slider(
                    value = strokeWidth,
                    onValueChange = onStrokeWidthChange,
                    valueRange = if (currentTool == DrawingTool.ERASER) 10f..60f else 2f..40f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (currentTool != DrawingTool.ERASER) {
            item {
                IconButton(
                    onClick = onShowColorPicker,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(drawColor, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.ColorPickerPanel(
    currentColor: Color,
    onColorChange: (Color) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .weight(1f)
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val colors = listOf(
            Color.Black, Color.White, Color.Red, Color(0xFFE91E63),
            Color(0xFF9C27B0), Color(0xFF673AB7), Color.Blue, Color(0xFF03A9F4),
            Color(0xFF00BCD4), Color(0xFF009688), Color.Green, Color(0xFF8BC34A),
            Color(0xFFCDDC39), Color.Yellow, Color(0xFFFF9800), Color(0xFFFF5722),
            Color(0xFF795548), Color.Gray, Color.DarkGray
        )

        items(colors) { color ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onColorChange(color)
                    }
                    .border(
                        width = if (color == currentColor) 3.dp else 1.dp,
                        color = if (color == currentColor)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .padding(if (color == currentColor) 0.dp else 2.dp)
                    .background(color, CircleShape)
            ) {
                if (color == Color.White) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color.LightGray, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.CanvasOptionsPanel(
    bgColor: Color,
    showGrid: Boolean,
    onBgColorChange: (Color) -> Unit,
    onGridToggle: (Boolean) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .weight(1f)
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Text("Canvas", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        val canvasColors = listOf(
            Color.White, Color(0xFFF5F5F5), Color(0xFFE8F5E9),
            Color(0xFFE3F2FD), Color(0xFFFFF8E1), Color(0xFFFCE4EC),
            Color(0xFFF3E5F5), Color.Black
        )

        items(canvasColors) { color ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onBgColorChange(color)
                    }
                    .border(
                        width = if (color == bgColor) 3.dp else 1.dp,
                        color = if (color == bgColor)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .padding(if (color == bgColor) 0.dp else 2.dp)
                    .background(color, CircleShape)
            ) {
                if (color == Color.White || color == Color(0xFFF5F5F5)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color.LightGray, CircleShape)
                    )
                }
            }
        }

        item { Spacer(Modifier.width(8.dp)) }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.GridOn,
                    contentDescription = "Grid",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Show Grid",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = showGrid,
                    onCheckedChange = onGridToggle,
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}
