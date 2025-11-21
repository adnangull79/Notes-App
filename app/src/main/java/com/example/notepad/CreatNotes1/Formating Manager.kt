package com.example.notepad.CreatNotes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.example.notepad.CreatNotes1.FormatSpan
import com.example.notepad.CreatNotes1.FormatState
import com.example.notepad.CreatNotes1.TextHistory
import com.example.notepad.CreatNotes1.adjustSpansForDeletion
import com.example.notepad.CreatNotes1.adjustSpansForInsertion
import com.example.notepad.CreatNotes1.commonPrefixLength
import com.example.notepad.CreatNotes1.commonSuffixLength
import com.example.notepad.CreatNotes1.composeActiveStyle
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp



/**
 * FormattingManager - Handles all formatting logic and history management
 * Keeps business logic separate from UI
 */
class FormattingManager(
    private val textColor: Color
) {
    // State
    var formatState by mutableStateOf(FormatState())
        private set

    var formatMap by mutableStateOf(listOf<FormatSpan>())
        private set

    // History for Undo/Redo
    var historyList by mutableStateOf(listOf<TextHistory>())
        private set

    var historyIndex by mutableStateOf(-1)
        private set

    // Initialize history with empty state
    fun initializeHistory(initialContent: TextFieldValue) {
        historyList = emptyList()
        historyIndex = -1
        pushHistory(initialContent, emptyList(), FormatState())
    }

    // Update format state
    fun updateFormatState(newState: FormatState) {
        formatState = newState
    }

    // Update format map
    fun updateFormatMap(newMap: List<FormatSpan>) {
        formatMap = newMap
    }

    // Push to history
    fun pushHistory(value: TextFieldValue, spans: List<FormatSpan>, state: FormatState) {
        val snapshot = TextHistory(value.copy(), spans.map { it.copy() }, state.copy())
        val newHistory = historyList.take(historyIndex + 1) + snapshot
        historyList = newHistory.takeLast(100)
        historyIndex = historyList.lastIndex
    }

    // Apply formatting to selected text
    fun applyFormatting(
        content: TextFieldValue,
        start: Int,
        end: Int,
        newStyle: SpanStyle
    ) {
        if (start >= end) return

        val newFormatList = formatMap.toMutableList()

        // Remove ALL overlapping spans first
        newFormatList.removeAll { it.start < end && it.end > start }

        // Add the new style
        newFormatList.add(FormatSpan(start, end, newStyle))

        formatMap = newFormatList.filter { it.end > it.start }.sortedBy { it.start }
        pushHistory(content, formatMap, formatState)
    }

    // Handle text changes and adjust spans
    fun handleTextChange(
        oldContent: TextFieldValue,
        newContent: TextFieldValue
    ): List<FormatSpan> {
        val oldText = oldContent.text
        val newText = newContent.text

        val textChanged = newText != oldText
        if (!textChanged) return formatMap

        // Calculate changes
        val prefixLen = commonPrefixLength(oldText, newText)
        val oldSuffixLen = commonSuffixLength(oldText, newText, prefixLen)

        val oldChangedEnd = oldText.length - oldSuffixLen
        val newChangedEnd = newText.length - oldSuffixLen

        val deletedLen = max(0, oldChangedEnd - prefixLen)
        val insertedLen = max(0, newChangedEnd - prefixLen)

        var spans = formatMap.toMutableList()

        // Adjust spans for deletion
        if (deletedLen > 0) {
            adjustSpansForDeletion(spans, prefixLen, oldChangedEnd)
        }

        // Adjust spans for insertion
        if (insertedLen > 0) {
            adjustSpansForInsertion(spans, prefixLen, insertedLen)
            // Apply currently active style to newly typed text
            composeActiveStyle(formatState, textColor)?.let { style ->
                spans.add(FormatSpan(prefixLen, prefixLen + insertedLen, style))
            }
        }

        spans = spans.filter { it.end > it.start }.sortedBy { it.start }.toMutableList()
        return spans
    }

    // Undo functionality
    fun undo(): Triple<TextFieldValue, List<FormatSpan>, FormatState>? {
        if (historyIndex > 0) {
            historyIndex--
            val snap = historyList[historyIndex]
            formatMap = snap.formatSpans.map { it.copy() }
            formatState = snap.formatState.copy()
            return Triple(snap.value, formatMap, formatState)
        }
        return null
    }

    // Redo functionality
    fun redo(): Triple<TextFieldValue, List<FormatSpan>, FormatState>? {
        if (historyIndex < historyList.lastIndex) {
            historyIndex++
            val snap = historyList[historyIndex]
            formatMap = snap.formatSpans.map { it.copy() }
            formatState = snap.formatState.copy()
            return Triple(snap.value, formatMap, formatState)
        }
        return null
    }

    // Check if undo is available
    fun canUndo(): Boolean = historyIndex > 0

    // Check if redo is available
    fun canRedo(): Boolean = historyIndex < historyList.lastIndex

    // Apply text color formatting
    fun applyTextColor(
        content: TextFieldValue,
        color: Color
    ) {
        val selection = content.selection
        val start = min(selection.start, selection.end)
        val end = max(selection.start, selection.end)

        if (start < end) {
            val style = if (color == Color.Unspecified) {
                // Reset to default with preserved formatting
                SpanStyle(
                    color = textColor,
                    fontWeight = if (formatState.isBold || formatState.isH1 || formatState.isH2)
                        androidx.compose.ui.text.font.FontWeight.Bold
                    else androidx.compose.ui.text.font.FontWeight.Normal,
                    fontStyle = if (formatState.isItalic)
                        androidx.compose.ui.text.font.FontStyle.Italic
                    else androidx.compose.ui.text.font.FontStyle.Normal,
                    textDecoration = when {
                        formatState.isUnderline && formatState.isStrikethrough ->
                            androidx.compose.ui.text.style.TextDecoration.combine(
                                listOf(
                                    androidx.compose.ui.text.style.TextDecoration.Underline,
                                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                            )
                        formatState.isUnderline -> androidx.compose.ui.text.style.TextDecoration.Underline
                        formatState.isStrikethrough -> androidx.compose.ui.text.style.TextDecoration.LineThrough
                        else -> androidx.compose.ui.text.style.TextDecoration.None
                    },
                    fontSize = when {
                        formatState.isH1 -> 28.sp
                        formatState.isH2 -> 22.sp
                        else -> 16.sp
                    }
                    ,
                    background = formatState.highlightColor
                )
            } else {
                SpanStyle(color = color)
            }
            applyFormatting(content, start, end, style)
        }
        formatState = formatState.copy(textColor = color)
    }

    // Apply highlight color formatting
    fun applyHighlightColor(
        content: TextFieldValue,
        color: Color
    ) {
        val selection = content.selection
        val start = min(selection.start, selection.end)
        val end = max(selection.start, selection.end)

        if (start < end) {
            val style = if (color == Color.Unspecified) {
                // Reset highlight with preserved formatting
                SpanStyle(
                    color = if (formatState.textColor != Color.Unspecified)
                        formatState.textColor
                    else textColor,
                    fontWeight = if (formatState.isBold || formatState.isH1 || formatState.isH2)
                        androidx.compose.ui.text.font.FontWeight.Bold
                    else androidx.compose.ui.text.font.FontWeight.Normal,
                    fontStyle = if (formatState.isItalic)
                        androidx.compose.ui.text.font.FontStyle.Italic
                    else androidx.compose.ui.text.font.FontStyle.Normal,
                    textDecoration = when {
                        formatState.isUnderline && formatState.isStrikethrough ->
                            androidx.compose.ui.text.style.TextDecoration.combine(
                                listOf(
                                    androidx.compose.ui.text.style.TextDecoration.Underline,
                                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                            )
                        formatState.isUnderline -> androidx.compose.ui.text.style.TextDecoration.Underline
                        formatState.isStrikethrough -> androidx.compose.ui.text.style.TextDecoration.LineThrough
                        else -> androidx.compose.ui.text.style.TextDecoration.None
                    },
                    fontSize = when {
                        formatState.isH1 -> 28.sp
                        formatState.isH2 -> 22.sp
                        else -> 16.sp

                    },
                    background = Color.Unspecified
                )
            } else {
                SpanStyle(background = color)
            }
            applyFormatting(content, start, end, style)
        }
        formatState = formatState.copy(highlightColor = color)
    }
}