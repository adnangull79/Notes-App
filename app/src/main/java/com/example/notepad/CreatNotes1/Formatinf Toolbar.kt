package com.example.notepad.CreatNotes1

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.max
import kotlin.math.min

/**
 * Main Formatting Toolbar - Shows at bottom of screen
 * Handles switching between different toolbar modes
 */
@Composable
fun FormattingToolbar(
    showBottomBar: Boolean,
    onShowBottomBarChange: (Boolean) -> Unit,
    contentText: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    formatState: FormatState,
    onFormatStateChange: (FormatState) -> Unit,
    onApplyFormatting: (Int, Int, SpanStyle) -> Unit,
    onPushHistory: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onShowColorPicker: () -> Unit,
    textColor: Color,
    bottomBarIconColor: Color,
    onAddDrawing: () -> Unit
) {
    var showTextFormatOptions by remember { mutableStateOf(false) }
    var showParagraphFormatOptions by remember { mutableStateOf(false) }
    var showImagePopup by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showTextColorBar by remember { mutableStateOf(false) }
    var showHighlightColorBar by remember { mutableStateOf(false) }
    var previousToolbarState by remember { mutableStateOf<String?>(null) }

    Box {
        if (showBottomBar) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                val showAnyFormatOptions = showTextFormatOptions || showParagraphFormatOptions ||
                        showTextColorBar || showHighlightColorBar

                if (showAnyFormatOptions) {
                    // Format Options Toolbar (Text/Paragraph/Colors)
                    FormatOptionsToolbarContent(
                        showTextFormatOptions = showTextFormatOptions,
                        showParagraphFormatOptions = showParagraphFormatOptions,
                        showTextColorBar = showTextColorBar,
                        showHighlightColorBar = showHighlightColorBar,
                        formatState = formatState,
                        onFormatStateChange = onFormatStateChange,
                        contentText = contentText,
                        onContentChange = onContentChange,
                        onApplyFormatting = onApplyFormatting,
                        onPushHistory = onPushHistory,
                        textColor = textColor,
                        bottomBarIconColor = bottomBarIconColor,
                        onClose = {
                            if (showTextColorBar || showHighlightColorBar) {
                                showTextColorBar = false
                                showHighlightColorBar = false
                                when (previousToolbarState) {
                                    "TEXT_FORMAT" -> showTextFormatOptions = true
                                    "PARAGRAPH_FORMAT" -> showParagraphFormatOptions = true
                                    else -> {
                                        showTextFormatOptions = false
                                        showParagraphFormatOptions = false
                                    }
                                }
                                previousToolbarState = null
                            } else {
                                showTextFormatOptions = false
                                showParagraphFormatOptions = false
                                previousToolbarState = null
                            }
                        },
                        onShowTextColorBar = {
                            previousToolbarState = "TEXT_FORMAT"
                            showTextColorBar = true
                            showHighlightColorBar = false
                            showTextFormatOptions = false
                            showParagraphFormatOptions = false
                        },
                        onShowHighlightColorBar = {
                            previousToolbarState = "TEXT_FORMAT"
                            showHighlightColorBar = true
                            showTextColorBar = false
                            showTextFormatOptions = false
                            showParagraphFormatOptions = false
                        }
                    )
                } else {
                    // Main Toolbar (default view)
                    MainToolbarContent(
                        onTextFormatClick = {
                            showTextFormatOptions = true
                            showParagraphFormatOptions = false
                        },
                        onParagraphFormatClick = {
                            showParagraphFormatOptions = true
                            showTextFormatOptions = false
                        },
                        onCheckboxClick = {
                            onContentChange(insertCheckboxAtCursor(contentText))
                            onPushHistory()
                        },
                        onMicrophoneClick = {
                            // Empty for now
                        },
                        onImageClick = {
                            showImagePopup = !showImagePopup
                        },
                        onMoreClick = {
                            showMoreSheet = true
                        },
                        onUndoClick = onUndo,
                        onRedoClick = onRedo,
                        canUndo = canUndo,
                        canRedo = canRedo,
                        onHideBar = { onShowBottomBarChange(false) },
                        bottomBarIconColor = bottomBarIconColor
                    )
                }
            }
        }

        // More Bottom Sheet
        if (showMoreSheet) {
            MoreOptionsSheet(
                onDismiss = { showMoreSheet = false },
                onShowColorPicker = onShowColorPicker,
                onAddDrawing = onAddDrawing
            )



        }
    }

    // Image Popup (separate from main Box, positioned independently)
    if (showImagePopup) {
        ImageOptionsPopup(
            onDismiss = { showImagePopup = false }
        )
    }
}

/**
 * Main Toolbar Content - Default view with primary actions (NO SCROLLING)
 */
@Composable
private fun MainToolbarContent(
    onTextFormatClick: () -> Unit,
    onParagraphFormatClick: () -> Unit,
    onCheckboxClick: () -> Unit,
    onMicrophoneClick: () -> Unit,
    onImageClick: () -> Unit,
    onMoreClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onHideBar: () -> Unit,
    bottomBarIconColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // LEFT SIDE - Format Options
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Text Format
            IconButton(
                onClick = onTextFormatClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.TextFields,
                    "Text format",
                    tint = bottomBarIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Paragraph Format
            IconButton(
                onClick = onParagraphFormatClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.FormatAlignLeft,
                    "Paragraph format",
                    tint = bottomBarIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Checkbox Insert
            IconButton(
                onClick = onCheckboxClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.CheckBoxOutlineBlank,
                    "Checkbox",
                    tint = bottomBarIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Microphone (Audio)
            IconButton(
                onClick = onMicrophoneClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Mic,
                    "Audio",
                    tint = bottomBarIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Image
            IconButton(
                onClick = onImageClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Image,
                    "Image",
                    tint = bottomBarIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // More Options
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    "More",
                    tint = bottomBarIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // RIGHT SIDE - Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Undo
            IconButton(
                onClick = onUndoClick,
                enabled = canUndo,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Undo,
                    "Undo",
                    tint = if (canUndo) bottomBarIconColor else bottomBarIconColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Redo
            IconButton(
                onClick = onRedoClick,
                enabled = canRedo,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Redo,
                    "Redo",
                    tint = if (canRedo) bottomBarIconColor else bottomBarIconColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Hide bar
            IconButton(
                onClick = onHideBar,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    "Hide Bar",
                    tint = bottomBarIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Format Options Toolbar - Shows Text/Paragraph/Color options
 */
@Composable
private fun FormatOptionsToolbarContent(
    showTextFormatOptions: Boolean,
    showParagraphFormatOptions: Boolean,
    showTextColorBar: Boolean,
    showHighlightColorBar: Boolean,
    formatState: FormatState,
    onFormatStateChange: (FormatState) -> Unit,
    contentText: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    onApplyFormatting: (Int, Int, SpanStyle) -> Unit,
    onPushHistory: () -> Unit,
    textColor: Color,
    bottomBarIconColor: Color,
    onClose: () -> Unit,
    onShowTextColorBar: () -> Unit,
    onShowHighlightColorBar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Default.Close, "Close", tint = bottomBarIconColor)
        }

        // Scrollable options content
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                showTextFormatOptions -> {
                    item {
                        TextFormatOptions(
                            formatState = formatState,
                            onFormatStateChange = onFormatStateChange,
                            contentText = contentText,
                            onApplyFormatting = onApplyFormatting,
                            textColor = textColor,
                            bottomBarIconColor = bottomBarIconColor,
                            onShowTextColorBar = onShowTextColorBar,
                            onShowHighlightColorBar = onShowHighlightColorBar
                        )
                    }
                }
                showParagraphFormatOptions -> {
                    item {
                        ParagraphFormatOptions(
                            formatState = formatState,
                            onFormatStateChange = onFormatStateChange,
                            contentText = contentText,
                            onContentChange = onContentChange,
                            onPushHistory = onPushHistory,
                            bottomBarIconColor = bottomBarIconColor
                        )
                    }
                }
                showTextColorBar -> {
                    item {
                        TextColorBar(
                            formatState = formatState,
                            onFormatStateChange = onFormatStateChange,
                            contentText = contentText,
                            onApplyFormatting = onApplyFormatting,
                            textColor = textColor,
                            bottomBarIconColor = bottomBarIconColor
                        )
                    }
                }
                showHighlightColorBar -> {
                    item {
                        HighlightColorBar(
                            formatState = formatState,
                            onFormatStateChange = onFormatStateChange,
                            contentText = contentText,
                            onApplyFormatting = onApplyFormatting,
                            textColor = textColor,
                            bottomBarIconColor = bottomBarIconColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Text Format Options - Bold, Italic, Underline, etc.
 * FIXED: Properly toggles formatting on/off
 */
@Composable
private fun TextFormatOptions(
    formatState: FormatState,
    onFormatStateChange: (FormatState) -> Unit,
    contentText: TextFieldValue,
    onApplyFormatting: (Int, Int, SpanStyle) -> Unit,
    textColor: Color,
    bottomBarIconColor: Color,
    onShowTextColorBar: () -> Unit,
    onShowHighlightColorBar: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bold
        IconButton(
            onClick = {
                val selection = contentText.selection
                val start = min(selection.start, selection.end)
                val end = max(selection.start, selection.end)

                val newBoldState = !formatState.isBold

                if (start < end) {
                    val style = SpanStyle(
                        fontWeight = if (newBoldState) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (formatState.isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = buildTextDecoration(formatState.isUnderline, formatState.isStrikethrough),
                        color = if (formatState.textColor != Color.Unspecified) formatState.textColor else textColor,
                        background = if (formatState.highlightColor != Color.Unspecified) formatState.highlightColor else Color.Unspecified,
                        fontSize = when {
                            formatState.isH1 -> 28.sp
                            formatState.isH2 -> 22.sp
                            else -> 16.sp
                        }
                    )
                    onApplyFormatting(start, end, style)
                }

                onFormatStateChange(formatState.copy(isBold = newBoldState))
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (formatState.isBold) MaterialTheme.colorScheme.primary else bottomBarIconColor
            ),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.FormatBold, "Bold", modifier = Modifier.size(20.dp))
        }

        // Italic
        IconButton(
            onClick = {
                val selection = contentText.selection
                val start = min(selection.start, selection.end)
                val end = max(selection.start, selection.end)

                val newItalicState = !formatState.isItalic

                if (start < end) {
                    val style = SpanStyle(
                        fontWeight = if (formatState.isBold || formatState.isH1 || formatState.isH2) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (newItalicState) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = buildTextDecoration(formatState.isUnderline, formatState.isStrikethrough),
                        color = if (formatState.textColor != Color.Unspecified) formatState.textColor else textColor,
                        background = if (formatState.highlightColor != Color.Unspecified) formatState.highlightColor else Color.Unspecified,
                        fontSize = when {
                            formatState.isH1 -> 28.sp
                            formatState.isH2 -> 22.sp
                            else -> 16.sp
                        }
                    )
                    onApplyFormatting(start, end, style)
                }

                onFormatStateChange(formatState.copy(isItalic = newItalicState))
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (formatState.isItalic) MaterialTheme.colorScheme.primary else bottomBarIconColor
            ),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.FormatItalic, "Italic", modifier = Modifier.size(20.dp))
        }

        // Underline
        IconButton(
            onClick = {
                val selection = contentText.selection
                val start = min(selection.start, selection.end)
                val end = max(selection.start, selection.end)

                val newUnderlineState = !formatState.isUnderline

                if (start < end) {
                    val style = SpanStyle(
                        fontWeight = if (formatState.isBold || formatState.isH1 || formatState.isH2) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (formatState.isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = buildTextDecoration(newUnderlineState, formatState.isStrikethrough),
                        color = if (formatState.textColor != Color.Unspecified) formatState.textColor else textColor,
                        background = if (formatState.highlightColor != Color.Unspecified) formatState.highlightColor else Color.Unspecified,
                        fontSize = when {
                            formatState.isH1 -> 28.sp
                            formatState.isH2 -> 22.sp
                            else -> 16.sp
                        }
                    )
                    onApplyFormatting(start, end, style)
                }

                onFormatStateChange(formatState.copy(isUnderline = newUnderlineState))
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (formatState.isUnderline) MaterialTheme.colorScheme.primary else bottomBarIconColor
            ),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.FormatUnderlined, "Underline", modifier = Modifier.size(20.dp))
        }

        // Strikethrough
        IconButton(
            onClick = {
                val selection = contentText.selection
                val start = min(selection.start, selection.end)
                val end = max(selection.start, selection.end)

                val newStrikethroughState = !formatState.isStrikethrough

                if (start < end) {
                    val style = SpanStyle(
                        fontWeight = if (formatState.isBold || formatState.isH1 || formatState.isH2) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (formatState.isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = buildTextDecoration(formatState.isUnderline, newStrikethroughState),
                        color = if (formatState.textColor != Color.Unspecified) formatState.textColor else textColor,
                        background = if (formatState.highlightColor != Color.Unspecified) formatState.highlightColor else Color.Unspecified,
                        fontSize = when {
                            formatState.isH1 -> 28.sp
                            formatState.isH2 -> 22.sp
                            else -> 16.sp
                        }
                    )
                    onApplyFormatting(start, end, style)
                }

                onFormatStateChange(formatState.copy(isStrikethrough = newStrikethroughState))
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (formatState.isStrikethrough) MaterialTheme.colorScheme.primary else bottomBarIconColor
            ),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.StrikethroughS, "Strike", modifier = Modifier.size(20.dp))
        }

        // Text Color
        Box(
            modifier = Modifier
                .size(40.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onShowTextColorBar,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.FormatColorText,
                        "Text color",
                        tint = bottomBarIconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(
                                color = if (formatState.textColor != Color.Unspecified)
                                    formatState.textColor
                                else
                                    bottomBarIconColor
                            )
                    )
                }
            }
        }

        // Highlight
        Box(
            modifier = Modifier
                .size(40.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onShowHighlightColorBar,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Highlight,
                        contentDescription = "Highlight",
                        tint = bottomBarIconColor,
                        modifier = Modifier
                            .size(20.dp)
                            .offset(x = (-1).dp, y = (-1).dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .background(
                                color = if (formatState.highlightColor != Color.Unspecified)
                                    formatState.highlightColor
                                else
                                    Color(0xFFFFEB3B).copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = bottomBarIconColor.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }

        // H1
        TextButton(
            onClick = {
                val selection = contentText.selection
                val start = min(selection.start, selection.end)
                val end = max(selection.start, selection.end)

                val newH1State = !formatState.isH1

                if (start < end) {
                    val style = SpanStyle(
                        fontSize = if (newH1State) 28.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = if (formatState.isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = buildTextDecoration(formatState.isUnderline, formatState.isStrikethrough),
                        color = if (formatState.textColor != Color.Unspecified) formatState.textColor else textColor,
                        background = if (formatState.highlightColor != Color.Unspecified) formatState.highlightColor else Color.Unspecified
                    )
                    onApplyFormatting(start, end, style)
                }

                onFormatStateChange(formatState.copy(
                    isH1 = newH1State,
                    isH2 = false,
                    isBold = true
                ))
            },
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (formatState.isH1) MaterialTheme.colorScheme.primary else bottomBarIconColor
            ),
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            Text("H1", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        // H2
        TextButton(
            onClick = {
                val selection = contentText.selection
                val start = min(selection.start, selection.end)
                val end = max(selection.start, selection.end)

                val newH2State = !formatState.isH2

                if (start < end) {
                    val style = SpanStyle(
                        fontSize = if (newH2State) 22.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = if (formatState.isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = buildTextDecoration(formatState.isUnderline, formatState.isStrikethrough),
                        color = if (formatState.textColor != Color.Unspecified) formatState.textColor else textColor,
                        background = if (formatState.highlightColor != Color.Unspecified) formatState.highlightColor else Color.Unspecified
                    )
                    onApplyFormatting(start, end, style)
                }

                onFormatStateChange(formatState.copy(
                    isH2 = newH2State,
                    isH1 = false,
                    isBold = true
                ))
            },
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (formatState.isH2) MaterialTheme.colorScheme.primary else bottomBarIconColor
            ),
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            Text("H2", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

/**
 * Helper function to build TextDecoration based on underline and strikethrough states
 */
private fun buildTextDecoration(isUnderline: Boolean, isStrikethrough: Boolean): TextDecoration {
    return when {
        isUnderline && isStrikethrough -> TextDecoration.combine(
            listOf(TextDecoration.Underline, TextDecoration.LineThrough)
        )
        isUnderline -> TextDecoration.Underline
        isStrikethrough -> TextDecoration.LineThrough
        else -> TextDecoration.None
    }
}

/**
 * Paragraph Format Options - Bullets, Alignment, Indent, etc.
 * REMOVED: Blockquote option
 */
@Composable
private fun ParagraphFormatOptions(
    formatState: FormatState,
    onFormatStateChange: (FormatState) -> Unit,
    contentText: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    onPushHistory: () -> Unit,
    bottomBarIconColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bullet List
        IconButton(
            onClick = {
                onFormatStateChange(formatState.copy(
                    isBulletList = !formatState.isBulletList,
                    isNumberedList = false
                ))
                if (!formatState.isBulletList) {
                    val cursor = contentText.selection.start
                    val lineStart = findLineStart(contentText.text, cursor)
                    val lineEnd = contentText.text.indexOf('\n', lineStart).let {
                        if (it == -1) contentText.text.length else it
                    }
                    val line = contentText.text.substring(lineStart, lineEnd)

                    if (!line.startsWith("• ")) {
                        val newText = contentText.text.substring(0, lineStart) + "• " +
                                contentText.text.substring(lineStart)
                        onContentChange(TextFieldValue(newText, androidx.compose.ui.text.TextRange(cursor + 2)))
                        onPushHistory()
                    }
                }
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (formatState.isBulletList) MaterialTheme.colorScheme.primary else bottomBarIconColor
            ),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.FormatListBulleted, "Bullets", modifier = Modifier.size(20.dp))
        }

        // Numbered List
        IconButton(
            onClick = {
                onFormatStateChange(formatState.copy(
                    isNumberedList = !formatState.isNumberedList,
                    isBulletList = false,
                    listCounter = 1
                ))
                if (!formatState.isNumberedList) {
                    val cursor = contentText.selection.start
                    val lineStart = findLineStart(contentText.text, cursor)
                    val lineEnd = contentText.text.indexOf('\n', lineStart).let {
                        if (it == -1) contentText.text.length else it
                    }
                    val line = contentText.text.substring(lineStart, lineEnd)

                    if (!line.matches(Regex("^\\d+\\.\\s.*"))) {
                        val newText = contentText.text.substring(0, lineStart) + "1. " +
                                contentText.text.substring(lineStart)
                        onContentChange(TextFieldValue(newText, androidx.compose.ui.text.TextRange(cursor + 3)))
                        onPushHistory()
                    }
                }
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (formatState.isNumberedList) MaterialTheme.colorScheme.primary else bottomBarIconColor
            ),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.FormatListNumbered, "Numbers", modifier = Modifier.size(20.dp))
        }

        // Align Left
        IconButton(
            onClick = {
                onFormatStateChange(formatState.copy(textAlign = TextAlign.Start))
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (formatState.textAlign == TextAlign.Start)
                    MaterialTheme.colorScheme.primary else bottomBarIconColor
            ),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.FormatAlignLeft, "Align Left", modifier = Modifier.size(20.dp))
        }

        // Align Center
        IconButton(
            onClick = {
                onFormatStateChange(formatState.copy(textAlign = TextAlign.Center))
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (formatState.textAlign == TextAlign.Center)
                    MaterialTheme.colorScheme.primary else bottomBarIconColor
            ),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.FormatAlignCenter, "Align Center", modifier = Modifier.size(20.dp))
        }

        // Align Right
        IconButton(
            onClick = {
                onFormatStateChange(formatState.copy(textAlign = TextAlign.End))
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (formatState.textAlign == TextAlign.End)
                    MaterialTheme.colorScheme.primary else bottomBarIconColor
            ),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.FormatAlignRight, "Align Right", modifier = Modifier.size(20.dp))
        }

        // Align Justify
        IconButton(
            onClick = {
                onFormatStateChange(formatState.copy(textAlign = TextAlign.Justify))
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (formatState.textAlign == TextAlign.Justify)
                    MaterialTheme.colorScheme.primary else bottomBarIconColor
            ),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.FormatAlignJustify, "Align Justify", modifier = Modifier.size(20.dp))
        }

        // Indent Increase
        IconButton(
            onClick = {
                onFormatStateChange(formatState.copy(indentLevel = min(formatState.indentLevel + 1, 5)))
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.FormatIndentIncrease,
                "Indent +",
                tint = bottomBarIconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // Indent Decrease
        IconButton(
            onClick = {
                onFormatStateChange(formatState.copy(indentLevel = max(formatState.indentLevel - 1, 0)))
            },
            enabled = formatState.indentLevel > 0,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.FormatIndentDecrease,
                "Indent -",
                tint = if (formatState.indentLevel > 0) bottomBarIconColor else bottomBarIconColor.copy(0.3f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Line Spacing
        IconButton(
            onClick = {
                val newSpacing = when (formatState.lineSpacing) {
                    1.0f -> 1.5f
                    1.5f -> 2.0f
                    else -> 1.0f
                }
                onFormatStateChange(formatState.copy(lineSpacing = newSpacing))
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.FormatLineSpacing,
                "Line Spacing",
                tint = bottomBarIconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Text Color Bar - Color picker for text color
 * FIXED: Reset to default color now works properly
 */
@Composable
private fun TextColorBar(
    formatState: FormatState,
    onFormatStateChange: (FormatState) -> Unit,
    contentText: TextFieldValue,
    onApplyFormatting: (Int, Int, SpanStyle) -> Unit,
    textColor: Color,
    bottomBarIconColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset/Default Color
        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val selection = contentText.selection
                    val start = min(selection.start, selection.end)
                    val end = max(selection.start, selection.end)

                    if (start < end) {
                        val resetStyle = SpanStyle(
                            color = textColor,
                            fontWeight = if (formatState.isBold || formatState.isH1 || formatState.isH2)
                                FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (formatState.isItalic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = buildTextDecoration(formatState.isUnderline, formatState.isStrikethrough),
                            fontSize = when {
                                formatState.isH1 -> 28.sp
                                formatState.isH2 -> 22.sp
                                else -> 16.sp
                            },
                            background = if (formatState.highlightColor != Color.Unspecified) formatState.highlightColor else Color.Unspecified
                        )
                        onApplyFormatting(start, end, resetStyle)
                    }
                    onFormatStateChange(formatState.copy(textColor = Color.Unspecified))
                }
                .border(
                    width = if (formatState.textColor == Color.Unspecified) 2.dp else 1.dp,
                    color = if (formatState.textColor == Color.Unspecified) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                )
                .background(Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.FormatColorReset,
                "Default Color",
                tint = bottomBarIconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(4.dp))

        val textColors = listOf(
            Color.Black,
            Color(0xFFE53935),
            Color(0xFF1E88E5),
            Color(0xFF43A047),
            Color(0xFFFFB300),
            Color(0xFF8E24AA),
            Color(0xFF00ACC1),
            Color(0xFFFF6F00)
        )

        textColors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val selection = contentText.selection
                        val start = min(selection.start, selection.end)
                        val end = max(selection.start, selection.end)
                        if (start < end) {
                            val style = SpanStyle(
                                color = color,
                                fontWeight = if (formatState.isBold || formatState.isH1 || formatState.isH2)
                                    FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (formatState.isItalic) FontStyle.Italic else FontStyle.Normal,
                                textDecoration = buildTextDecoration(formatState.isUnderline, formatState.isStrikethrough),
                                fontSize = when {
                                    formatState.isH1 -> 28.sp
                                    formatState.isH2 -> 22.sp
                                    else -> 16.sp
                                },
                                background = if (formatState.highlightColor != Color.Unspecified) formatState.highlightColor else Color.Unspecified
                            )
                            onApplyFormatting(start, end, style)
                        }
                        onFormatStateChange(formatState.copy(textColor = color))
                    }
                    .border(
                        width = if (color == formatState.textColor) 2.dp else 0.dp,
                        color = if (color == formatState.textColor) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                        shape = CircleShape
                    )
                    .background(color, CircleShape)
            )
        }
    }
}

/**
 * Highlight Color Bar - Color picker for highlight/background color
 * FIXED: Reset to no highlight now works properly
 */
@Composable
private fun HighlightColorBar(
    formatState: FormatState,
    onFormatStateChange: (FormatState) -> Unit,
    contentText: TextFieldValue,
    onApplyFormatting: (Int, Int, SpanStyle) -> Unit,
    textColor: Color,
    bottomBarIconColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset/No Highlight
        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val selection = contentText.selection
                    val start = min(selection.start, selection.end)
                    val end = max(selection.start, selection.end)

                    if (start < end) {
                        val resetStyle = SpanStyle(
                            color = if (formatState.textColor != Color.Unspecified)
                                formatState.textColor else textColor,
                            fontWeight = if (formatState.isBold || formatState.isH1 || formatState.isH2)
                                FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (formatState.isItalic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = buildTextDecoration(formatState.isUnderline, formatState.isStrikethrough),
                            fontSize = when {
                                formatState.isH1 -> 28.sp
                                formatState.isH2 -> 22.sp
                                else -> 16.sp
                            },
                            background = Color.Unspecified
                        )
                        onApplyFormatting(start, end, resetStyle)
                    }
                    onFormatStateChange(formatState.copy(highlightColor = Color.Unspecified))
                }
                .border(
                    width = if (formatState.highlightColor == Color.Unspecified) 2.dp else 1.dp,
                    color = if (formatState.highlightColor == Color.Unspecified) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                )
                .background(Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.FormatColorReset,
                "No Highlight",
                tint = bottomBarIconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(4.dp))

        val highlightColors = listOf(
            Color(0xFFFFEB3B).copy(alpha = 0.4f),
            Color(0xFF4CAF50).copy(alpha = 0.3f),
            Color(0xFF2196F3).copy(alpha = 0.3f),
            Color(0xFFFF9800).copy(alpha = 0.4f),
            Color(0xFFE91E63).copy(alpha = 0.3f),
            Color(0xFF9C27B0).copy(alpha = 0.3f),
            Color(0xFF00BCD4).copy(alpha = 0.3f),
            Color(0xFFCDDC39).copy(alpha = 0.4f)
        )

        highlightColors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val selection = contentText.selection
                        val start = min(selection.start, selection.end)
                        val end = max(selection.start, selection.end)
                        if (start < end) {
                            val style = SpanStyle(
                                color = if (formatState.textColor != Color.Unspecified)
                                    formatState.textColor else textColor,
                                fontWeight = if (formatState.isBold || formatState.isH1 || formatState.isH2)
                                    FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (formatState.isItalic) FontStyle.Italic else FontStyle.Normal,
                                textDecoration = buildTextDecoration(formatState.isUnderline, formatState.isStrikethrough),
                                fontSize = when {
                                    formatState.isH1 -> 28.sp
                                    formatState.isH2 -> 22.sp
                                    else -> 16.sp
                                },
                                background = color
                            )
                            onApplyFormatting(start, end, style)
                        }
                        onFormatStateChange(formatState.copy(highlightColor = color))
                    }
                    .border(
                        width = if (color == formatState.highlightColor) 2.dp else 0.dp,
                        color = if (color == formatState.highlightColor) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                        shape = CircleShape
                    )
                    .background(color, CircleShape)
            )
        }
    }
}

/**
 * Image Options Popup - Camera or Gallery
 * FIXED: Now shows as popup above toolbar, doesn't close keyboard
 */
@Composable
private fun ImageOptionsPopup(
    onDismiss: () -> Unit
) {
    Popup(
        alignment = Alignment.BottomCenter,
        offset = androidx.compose.ui.unit.IntOffset(0, -180),
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .width(280.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                // Camera Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Empty for now
                            onDismiss()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Take Photo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Capture from camera",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Gallery Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Empty for now
                            onDismiss()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Choose from Gallery",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Select existing photo",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * More Options Bottom Sheet - Attachment, Drawing, and Background Color
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreOptionsSheet(
    onDismiss: () -> Unit,
    onShowColorPicker: () -> Unit,
    onAddDrawing: () -> Unit // ⭐ NEW
)
 {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "More Options",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Attachment Option
            ListItem(
                headlineContent = { Text("Attachment", fontSize = 16.sp) },
                supportingContent = { Text("Attach files or documents", fontSize = 14.sp) },
                leadingContent = {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Empty for now
                        onDismiss()
                    }
            )

            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Drawing", fontSize = 16.sp) },
                supportingContent = { Text("Create sketch or drawing", fontSize = 14.sp) },
                leadingContent = {
                    Icon(
                        Icons.Default.Brush,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDismiss()
                        onAddDrawing()  // ⭐ CALL THE PASSED-IN FUNCTION
                    }
            )




            Spacer(modifier = Modifier.height(8.dp))

            // Background Color Option
            ListItem(
                headlineContent = { Text("Background Color", fontSize = 16.sp) },
                supportingContent = { Text("Change note background", fontSize = 14.sp) },
                leadingContent = {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDismiss()
                        onShowColorPicker()
                    }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Floating Action Button - Shows when toolbar is hidden
 */
@Composable
fun FormattingFAB(
    showBottomBar: Boolean,
    onShowBottomBar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 0.dp, end = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = !showBottomBar,
            enter = slideInVertically(
                initialOffsetY = { 80 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(
                animationSpec = tween(durationMillis = 400)
            ) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { 80 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(
                animationSpec = tween(durationMillis = 250)
            ) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(durationMillis = 250)
            )
        ) {
            FloatingActionButton(
                onClick = onShowBottomBar,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp,
                    hoveredElevation = 10.dp
                ),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Show Formatting Bar",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}