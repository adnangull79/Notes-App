package com.example.notepad.Drawing

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import com.example.notepad.R

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
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
    val showAnyPanel = showToolOptions || showCanvasOptions

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = 8.dp)
    ) {
        // Tool options or canvas options panel (NOT color picker - it uses bottom sheet)
        AnimatedVisibility(
            visible = showAnyPanel,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
                    .zIndex(1f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onCloseSubPanel() },
                        modifier = Modifier
                            .size(40.dp)
                            .padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.width(8.dp))

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
        }

        Spacer(Modifier.height(if (showAnyPanel) 10.dp else 0.dp))

        // Main toolbar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left segment: tool buttons and canvas option
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                AppleToolButton(
                    selected = currentTool == DrawingTool.PEN,
                    onClick = { onToolChange(DrawingTool.PEN) }
                ) {
                    Icon(Icons.Default.BorderColor, contentDescription = "Pen", modifier = Modifier.size(20.dp))
                }

                AppleToolButton(
                    selected = currentTool == DrawingTool.MARKER,
                    onClick = { onToolChange(DrawingTool.MARKER) }
                ) {
                    Icon(Icons.Default.Brush, contentDescription = "Marker", modifier = Modifier.size(20.dp))
                }

                AppleToolButton(
                    selected = currentTool == DrawingTool.HIGHLIGHTER,
                    onClick = { onToolChange(DrawingTool.HIGHLIGHTER) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.highlighter),
                        contentDescription = "Highlighter",
                        modifier = Modifier.size(20.dp)
                    )
                }

                AppleToolButton(
                    selected = currentTool == DrawingTool.ERASER,
                    onClick = { onToolChange(DrawingTool.ERASER) }
                ) {
                    Icon(Icons.Default.AutoFixOff, contentDescription = "Eraser", modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.width(8.dp))

                AppleCanvasButton(
                    color = canvasBgColor,
                    onClick = onShowCanvasOptions
                )
            }

            // Right segment: undo/redo and color picker
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Default.Undo,
                        null,
                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Default.Redo,
                        null,
                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                AppleColorButton(
                    color = drawColor,
                    enabled = currentTool != DrawingTool.ERASER,
                    onClick = onShowColorPicker
                )
            }
        }

        // Color Picker Bottom Seet
        if (showColorPicker) {
            ModalBottomSheet(
                onDismissRequest = onCloseSubPanel,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ColorPickerBottomSheet(
                    currentColor = drawColor,
                    onColorChange = onColorChange,
                    onDismiss = onCloseSubPanel
                )
            }
        }
    }
}

@Composable
private fun AppleToolButton(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val targetScale by animateFloatAsState(if (selected) 1.06f else 1f, animationSpec = spring(stiffness = Spring.StiffnessLow))
    val bgColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    val elevation = if (selected) 8.dp else 0.dp
    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(targetScale)
            .shadow(elevation = elevation, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            content()
        }
    }
}

@Composable
private fun AppleCanvasButton(
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .shadow(6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
    }
}

@Composable
private fun AppleColorButton(
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alphaTint = if (enabled) 1f else 0.35f
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = alphaTint))
            .clickable(enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null) { if (enabled) onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // First row: Size, Preview, Color
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Size: ${strokeWidth.toInt()}px",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp)
            )

            // Brush preview in center
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                BrushPreview(
                    color = if (currentTool == DrawingTool.HIGHLIGHTER) drawColor.copy(alpha = 0.35f) else drawColor,
                    strokeWidth = when (currentTool) {
                        DrawingTool.ERASER -> strokeWidth * 2.5f
                        DrawingTool.MARKER -> strokeWidth * 1.3f
                        DrawingTool.HIGHLIGHTER -> strokeWidth * 2f
                        else -> strokeWidth
                    }
                )
            }

            // Color swatch on right
            if (currentTool != DrawingTool.ERASER) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onShowColorPicker() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(drawColor)
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }
            } else {
                Spacer(Modifier.width(44.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Second row: Full width slider
        Slider(
            value = strokeWidth,
            onValueChange = onStrokeWidthChange,
            valueRange = if (currentTool == DrawingTool.ERASER) 10f..60f else 2f..40f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BrushPreview(color: Color, strokeWidth: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height((strokeWidth / 5).coerceIn(3f, 20f).dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ColorPickerBottomSheet(
    currentColor: Color,
    onColorChange: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Grid", "Spectrum", "Sliders")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
            .padding(16.dp)
    ) {
        // Header with title and close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Colors",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Tab content with fixed height
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> GridColorPicker(currentColor, onColorChange)
                1 -> SpectrumColorPicker(currentColor, onColorChange)
                2 -> SlidersColorPicker(currentColor, onColorChange)
            }
        }
    }
}

@Composable
fun GridColorPicker(
    currentColor: Color,
    onColorChange: (Color) -> Unit
) {
    val colors = listOf(
        // Grays
        Color.White, Color(0xFFF5F5F5), Color(0xFFE0E0E0), Color(0xFFBDBDBD),
        Color(0xFF9E9E9E), Color(0xFF757575), Color(0xFF616161), Color(0xFF424242),
        Color(0xFF212121), Color.Black,

        // Reds
        Color(0xFFFFEBEE), Color(0xFFFFCDD2), Color(0xFFEF9A9A), Color(0xFFE57373),
        Color(0xFFEF5350), Color(0xFFF44336), Color(0xFFE53935), Color(0xFFD32F2F),
        Color(0xFFC62828), Color(0xFFB71C1C),

        // Pinks
        Color(0xFFFCE4EC), Color(0xFFF8BBD0), Color(0xFFF48FB1), Color(0xFFF06292),
        Color(0xFFEC407A), Color(0xFFE91E63), Color(0xFFD81B60), Color(0xFFC2185B),
        Color(0xFFAD1457), Color(0xFF880E4F),

        // Purples
        Color(0xFFF3E5F5), Color(0xFFE1BEE7), Color(0xFFCE93D8), Color(0xFFBA68C8),
        Color(0xFFAB47BC), Color(0xFF9C27B0), Color(0xFF8E24AA), Color(0xFF7B1FA2),
        Color(0xFF6A1B9A), Color(0xFF4A148C),

        // Deep Purples
        Color(0xFFEDE7F6), Color(0xFFD1C4E9), Color(0xFFB39DDB), Color(0xFF9575CD),
        Color(0xFF7E57C2), Color(0xFF673AB7), Color(0xFF5E35B1), Color(0xFF512DA8),
        Color(0xFF4527A0), Color(0xFF311B92),

        // Blues
        Color(0xFFE3F2FD), Color(0xFFBBDEFB), Color(0xFF90CAF9), Color(0xFF64B5F6),
        Color(0xFF42A5F5), Color(0xFF2196F3), Color(0xFF1E88E5), Color(0xFF1976D2),
        Color(0xFF1565C0), Color(0xFF0D47A1),

        // Cyans
        Color(0xFFE0F7FA), Color(0xFFB2EBF2), Color(0xFF80DEEA), Color(0xFF4DD0E1),
        Color(0xFF26C6DA), Color(0xFF00BCD4), Color(0xFF00ACC1), Color(0xFF0097A7),
        Color(0xFF00838F), Color(0xFF006064),

        // Greens
        Color(0xFFE8F5E9), Color(0xFFC8E6C9), Color(0xFFA5D6A7), Color(0xFF81C784),
        Color(0xFF66BB6A), Color(0xFF4CAF50), Color(0xFF43A047), Color(0xFF388E3C),
        Color(0xFF2E7D32), Color(0xFF1B5E20),

        // Light Greens
        Color(0xFFF1F8E9), Color(0xFFDCEDC8), Color(0xFFC5E1A5), Color(0xFFAED581),
        Color(0xFF9CCC65), Color(0xFF8BC34A), Color(0xFF7CB342), Color(0xFF689F38),
        Color(0xFF558B2F), Color(0xFF33691E),

        // Yellows
        Color(0xFFFFFDE7), Color(0xFFFFF9C4), Color(0xFFFFF59D), Color(0xFFFFF176),
        Color(0xFFFFEE58), Color(0xFFFFEB3B), Color(0xFFFDD835), Color(0xFFFBC02D),
        Color(0xFFF9A825), Color(0xFFF57F17),

        // Oranges
        Color(0xFFFFF3E0), Color(0xFFFFE0B2), Color(0xFFFFCC80), Color(0xFFFFB74D),
        Color(0xFFFFA726), Color(0xFFFF9800), Color(0xFFFB8C00), Color(0xFFF57C00),
        Color(0xFFEF6C00), Color(0xFFE65100),

        // Browns
        Color(0xFFEFEBE9), Color(0xFFD7CCC8), Color(0xFFBCAAA4), Color(0xFFA1887F),
        Color(0xFF8D6E63), Color(0xFF795548), Color(0xFF6D4C41), Color(0xFF5D4037),
        Color(0xFF4E342E), Color(0xFF3E2723)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(10),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(colors) { color ->
            val selected = color == currentColor
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .border(
                        width = if (selected) 3.dp else 0.5.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onColorChange(color) }
            ) {
                if (color == Color.White) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SpectrumColorPicker(
    currentColor: Color,
    onColorChange: (Color) -> Unit
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var brightness by remember { mutableStateOf(1f) }
    var alpha by remember { mutableStateOf(1f) }

    // Convert current color to HSV on first composition
    LaunchedEffect(Unit) {
        val hsv = FloatArray(3)
        val androidColor = android.graphics.Color.valueOf(
            currentColor.red,
            currentColor.green,
            currentColor.blue
        )
        android.graphics.Color.colorToHSV(
            android.graphics.Color.rgb(
                (currentColor.red * 255).toInt(),
                (currentColor.green * 255).toInt(),
                (currentColor.blue * 255).toInt()
            ),
            hsv
        )
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        alpha = currentColor.alpha
    }

    fun updateColor() {
        val hsv = floatArrayOf(hue, saturation, brightness)
        val color = android.graphics.Color.HSVToColor(hsv)
        onColorChange(
            Color(
                red = android.graphics.Color.red(color) / 255f,
                green = android.graphics.Color.green(color) / 255f,
                blue = android.graphics.Color.blue(color) / 255f,
                alpha = alpha
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hue spectrum bar
        Text("Hue", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF0000), // Red
                            Color(0xFFFFFF00), // Yellow
                            Color(0xFF00FF00), // Green
                            Color(0xFF00FFFF), // Cyan
                            Color(0xFF0000FF), // Blue
                            Color(0xFFFF00FF), // Magenta
                            Color(0xFFFF0000)  // Red
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        )
        Slider(
            value = hue,
            onValueChange = {
                hue = it
                updateColor()
            },
            valueRange = 0f..360f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Saturation slider
        Text("Saturation: ${(saturation * 100).toInt()}%", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White,
                            Color.hsv(hue, 1f, brightness)
                        )
                    )
                )
        )
        Slider(
            value = saturation,
            onValueChange = {
                saturation = it
                updateColor()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Brightness slider
        Text("Brightness: ${(brightness * 100).toInt()}%", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.hsv(hue, saturation, 1f)
                        )
                    )
                )
        )
        Slider(
            value = brightness,
            onValueChange = {
                brightness = it
                updateColor()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Opacity slider
        Text("Opacity: ${(alpha * 100).toInt()}%", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.hsv(hue, saturation, brightness, 0f),
                            Color.hsv(hue, saturation, brightness, 1f)
                        )
                    )
                )
        )
        Slider(
            value = alpha,
            onValueChange = {
                alpha = it
                updateColor()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // Color preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.hsv(hue, saturation, brightness, alpha))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        )
    }
}

@Composable
fun SlidersColorPicker(
    currentColor: Color,
    onColorChange: (Color) -> Unit
) {
    var red by remember { mutableStateOf(currentColor.red) }
    var green by remember { mutableStateOf(currentColor.green) }
    var blue by remember { mutableStateOf(currentColor.blue) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Red slider
        Text("Red: ${(red * 255).toInt()}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Slider(
            value = red,
            onValueChange = {
                red = it
                onColorChange(Color(red, green, blue))
            },
            colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
        )

        Spacer(Modifier.height(16.dp))

        // Green slider
        Text("Green: ${(green * 255).toInt()}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Slider(
            value = green,
            onValueChange = {
                green = it
                onColorChange(Color(red, green, blue))
            },
            colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green)
        )

        Spacer(Modifier.height(16.dp))

        // Blue slider
        Text("Blue: ${(blue * 255).toInt()}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Slider(
            value = blue,
            onValueChange = {
                blue = it
                onColorChange(Color(red, green, blue))
            },
            colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue)
        )

        Spacer(Modifier.height(24.dp))

        // Color preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(red, green, blue))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        )
    }
}

@Composable
fun RowScope.CanvasOptionsPanel(
    bgColor: Color,
    showGrid: Boolean,
    onBgColorChange: (Color) -> Unit,
    onGridToggle: (Boolean) -> Unit
) {
    val canvasColors = listOf(
        Color.White, Color(0xFFF5F5F5), Color(0xFFE8F5E9),
        Color(0xFFE3F2FD), Color(0xFFFFF8E1), Color(0xFFFCE4EC),
        Color(0xFFF3E5F5), Color.Black
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(canvasColors) { color ->
                val selected = color == bgColor
                Box(
                    modifier = Modifier
                        .size(if (selected) 44.dp else 36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onBgColorChange(color) }
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
        }

        Spacer(Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.GridOn, contentDescription = "Grid", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Text("Show Grid", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Switch(checked = showGrid, onCheckedChange = onGridToggle, modifier = Modifier.height(24.dp))
        }
    }
}