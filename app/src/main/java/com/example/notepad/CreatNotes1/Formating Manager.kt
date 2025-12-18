package com.example.notepad.CreatNotes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.example.notepad.CreatNotes1.*
import kotlin.math.max
import kotlin.math.min

/**
 * FormattingManager - FIXED VERSION
 * Handles all formatting logic and history management properly
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

    // Flag to prevent pushing duplicate history during restoration
    private var isRestoringHistory = false

    // Initialize history with empty state
    fun initializeHistory(initialContent: TextFieldValue) {
        historyList = listOf(
            TextHistory(
                value = initialContent.copy(),
                formatSpans = emptyList(),
                formatState = FormatState()
            )
        )
        historyIndex = 0
    }

    // Update format state
    fun updateFormatState(newState: FormatState) {
        formatState = newState
    }

    // Update format map
    fun updateFormatMap(newMap: List<FormatSpan>) {
        formatMap = newMap
    }

    // Push to history - only if not currently restoring
    fun pushHistory(value: TextFieldValue, spans: List<FormatSpan>, state: FormatState) {
        if (isRestoringHistory) return

        // Don't push duplicate history
        if (historyIndex >= 0 && historyIndex < historyList.size) {
            val current = historyList[historyIndex]
            if (current.value.text == value.text &&
                current.formatSpans == spans &&
                current.formatState == state) {
                return
            }
        }

        // Remove any history after current index (branching)
        val newHistory = historyList.take(historyIndex + 1).toMutableList()

        // Add new snapshot
        newHistory.add(
            TextHistory(
                value = value.copy(),
                formatSpans = spans.map { it.copy() },
                formatState = state.copy()
            )
        )

        // Keep only last 100 entries
        historyList = newHistory.takeLast(100)
        historyIndex = historyList.lastIndex
    }

    // Apply formatting to selected text - FIXED
    fun applyFormatting(
        content: TextFieldValue,
        start: Int,
        end: Int,
        newStyle: SpanStyle
    ) {
        if (start >= end) return

        val newFormatList = formatMap.toMutableList()

        // Remove overlapping spans in the selection
        newFormatList.removeAll { it.start < end && it.end > start }

        // Add new formatting span
        newFormatList.add(FormatSpan(start, end, newStyle))

        // Clean and sort
        val cleanedMap = newFormatList.filter { it.end > it.start }.sortedBy { it.start }

        // Update formatMap - this will trigger recomposition in FormattedTextEditor
        formatMap = cleanedMap

        // Push to history
        pushHistory(content, cleanedMap, formatState)
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

        // Clean and return
        val cleanedSpans = spans.filter { it.end > it.start }.sortedBy { it.start }

        // Update internal state
        formatMap = cleanedSpans

        return cleanedSpans
    }

    // Undo functionality - FIXED
    fun undo(): Triple<TextFieldValue, List<FormatSpan>, FormatState>? {
        if (historyIndex <= 0) return null

        isRestoringHistory = true
        historyIndex--

        val snapshot = historyList[historyIndex]

        // Update all states
        formatMap = snapshot.formatSpans.map { it.copy() }
        formatState = snapshot.formatState.copy()

        isRestoringHistory = false

        return Triple(
            snapshot.value.copy(),
            formatMap,
            formatState
        )
    }

    // Redo functionality - FIXED
    fun redo(): Triple<TextFieldValue, List<FormatSpan>, FormatState>? {
        if (historyIndex >= historyList.lastIndex) return null

        isRestoringHistory = true
        historyIndex++

        val snapshot = historyList[historyIndex]

        // Update all states
        formatMap = snapshot.formatSpans.map { it.copy() }
        formatState = snapshot.formatState.copy()

        isRestoringHistory = false

        return Triple(
            snapshot.value.copy(),
            formatMap,
            formatState
        )
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
            val style = SpanStyle(
                color = if (color == Color.Unspecified) textColor else color,
                fontWeight = if (formatState.isBold || formatState.isH1 || formatState.isH2)
                    FontWeight.Bold
                else FontWeight.Normal,
                fontStyle = if (formatState.isItalic)
                    FontStyle.Italic
                else FontStyle.Normal,
                textDecoration = when {
                    formatState.isUnderline && formatState.isStrikethrough ->
                        TextDecoration.combine(
                            listOf(
                                TextDecoration.Underline,
                                TextDecoration.LineThrough
                            )
                        )
                    formatState.isUnderline -> TextDecoration.Underline
                    formatState.isStrikethrough -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                },
                fontSize = when {
                    formatState.isH1 -> 28.sp
                    formatState.isH2 -> 22.sp
                    else -> 16.sp
                },
                background = if (formatState.highlightColor != Color.Unspecified)
                    formatState.highlightColor
                else
                    Color.Unspecified
            )
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
            val style = SpanStyle(
                color = if (formatState.textColor != Color.Unspecified)
                    formatState.textColor
                else textColor,
                fontWeight = if (formatState.isBold || formatState.isH1 || formatState.isH2)
                    FontWeight.Bold
                else FontWeight.Normal,
                fontStyle = if (formatState.isItalic)
                    FontStyle.Italic
                else FontStyle.Normal,
                textDecoration = when {
                    formatState.isUnderline && formatState.isStrikethrough ->
                        TextDecoration.combine(
                            listOf(
                                TextDecoration.Underline,
                                TextDecoration.LineThrough
                            )
                        )
                    formatState.isUnderline -> TextDecoration.Underline
                    formatState.isStrikethrough -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                },
                fontSize = when {
                    formatState.isH1 -> 28.sp
                    formatState.isH2 -> 22.sp
                    else -> 16.sp
                },
                background = if (color == Color.Unspecified)
                    Color.Unspecified
                else
                    color
            )
            applyFormatting(content, start, end, style)
        }

        formatState = formatState.copy(highlightColor = color)
    }
}