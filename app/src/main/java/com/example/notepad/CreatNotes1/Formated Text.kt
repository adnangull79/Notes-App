package com.example.notepad.CreatNotes1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notepad.CreatNotes.FormattingManager
import kotlin.math.max

@Composable
fun FormattedTextEditor(
    content: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    formatState: FormatState,
    onFormatStateChange: (FormatState) -> Unit,
    formatMap: FormatMap,
    onFormatMapChange: (FormatMap) -> Unit,
    textColor: Color,
    onFocusChanged: (Boolean) -> Unit,
    onCheckboxToggle: (TextFieldValue) -> Unit,
    formattingManager: FormattingManager   // ⭐ REQUIRED for Undo/Redo
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val grayColor = Color.Gray

    var previousFormatMap by remember { mutableStateOf(formatMap) }
    var internalContent by remember { mutableStateOf(content) }

    LaunchedEffect(formatMap) {
        if (formatMap != previousFormatMap) {
            previousFormatMap = formatMap
            val annotated = decorateText(
                text = content.text,
                formatMap = formatMap,
                baseColor = textColor,
                primaryColor = primaryColor,
                grayColor = grayColor,
                formatState = formatState
            )
            internalContent = content.copy(annotatedString = annotated)
        }
    }

    LaunchedEffect(content.text, content.selection) {
        if (content.text != internalContent.text || content.selection != internalContent.selection) {
            val annotated = decorateText(
                text = content.text,
                formatMap = formatMap,
                baseColor = textColor,
                primaryColor = primaryColor,
                grayColor = grayColor,
                formatState = formatState
            )
            internalContent = content.copy(annotatedString = annotated)
        }
    }

    BasicTextField(
        value = internalContent,
        onValueChange = { newValue ->

            if (newValue.text == internalContent.text && newValue.selection != internalContent.selection) {
                val toggled = toggleCheckbox(internalContent)
                if (toggled.text != internalContent.text) {
                    onCheckboxToggle(toggled)
                    return@BasicTextField
                }
            }

            val oldText = internalContent.text
            val newText = newValue.text
            val textChanged = newText != oldText

            if (!textChanged) {
                internalContent = newValue
                return@BasicTextField
            }

            val isDeletion = newText.length < oldText.length
            var processedValue = newValue

            val prefixLen = commonPrefixLength(oldText, newText)
            val oldSuffix = commonSuffixLength(oldText, newText, prefixLen)
            val newSuffix = oldSuffix

            val oldChangedEnd = oldText.length - oldSuffix
            val newChangedEnd = newText.length - newSuffix

            val deletedLen = max(0, oldChangedEnd - prefixLen)
            val insertedLen = max(0, newChangedEnd - prefixLen)

            val spans = formatMap.toMutableList()

            if (deletedLen > 0) {
                adjustSpansForDeletion(spans, prefixLen, oldChangedEnd)
            }

            if (insertedLen > 0) {
                adjustSpansForInsertion(spans, prefixLen, insertedLen)

                val activeStyle = composeActiveStyle(formatState, textColor)
                if (activeStyle != null) {
                    spans.add(FormatSpan(prefixLen, prefixLen + insertedLen, activeStyle))
                } else {
                    spans.add(
                        FormatSpan(
                            prefixLen,
                            prefixLen + insertedLen,
                            SpanStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                fontStyle = FontStyle.Normal,
                                textDecoration = TextDecoration.None,
                                color = textColor,
                                background = Color.Unspecified
                            )
                        )
                    )
                }
            }

            val cleanedSpans = spans.filter { it.end > it.start }.sortedBy { it.start }.toMutableList()

            // ⭐ LIST HANDLING (unchanged)
            if (!isDeletion &&
                newText.length > oldText.length &&
                newText.getOrNull(newValue.selection.start - 1) == '\n'
            ) {
                val cursor = newValue.selection.start
                val lineStart = findLineStart(oldText, internalContent.selection.start - 1)
                val lineBefore = oldText.substring(lineStart, internalContent.selection.start)
                val trimmed = lineBefore.trim()

                if (trimmed.startsWith(CHECKED_PREFIX) || trimmed.startsWith(UNCHECKED_PREFIX)) {
                    val marker = UNCHECKED_PREFIX
                    val updated = newText.substring(0, cursor) + marker + newText.substring(cursor)
                    processedValue = processedValue.copy(text = updated, selection = TextRange(cursor + marker.length))
                } else if (formatState.isBulletList) {
                    val marker = "• "
                    val empty = trimmed == marker.trim()
                    if (empty) {
                        val t = oldText.substring(0, lineStart) + newText.substring(cursor)
                        processedValue = TextFieldValue(t, TextRange(lineStart))
                        onFormatStateChange(formatState.copy(isBulletList = false))
                    } else if (trimmed.startsWith(marker.trim())) {
                        val updated = newText.substring(0, cursor) + marker + newText.substring(cursor)
                        processedValue = processedValue.copy(text = updated, selection = TextRange(cursor + marker.length))
                    }
                } else if (formatState.isNumberedList) {
                    val regex = Regex("^(\\d+)\\.\\s*$")
                    val match = regex.find(trimmed)
                    val currentNum = match?.groupValues?.get(1)?.toIntOrNull() ?: formatState.listCounter
                    val onlyMarker = trimmed.matches(Regex("^\\d+\\.$"))
                    if (onlyMarker) {
                        val t = oldText.substring(0, lineStart) + newText.substring(cursor)
                        processedValue = TextFieldValue(t, TextRange(lineStart))
                        onFormatStateChange(formatState.copy(isNumberedList = false, listCounter = 1))
                    } else {
                        val next = currentNum + 1
                        val marker = "$next. "
                        val updated = newText.substring(0, cursor) + marker + newText.substring(cursor)
                        processedValue = processedValue.copy(text = updated, selection = TextRange(cursor + marker.length))
                        onFormatStateChange(formatState.copy(listCounter = next))
                    }
                }
            }

            // ⭐ UPDATE SPANS
            onFormatMapChange(cleanedSpans)

            // ⭐ *** THE TWO MISSING UNDO/REDO LINES ***
            formattingManager.handleTextChange(content, processedValue)
            formattingManager.pushHistory(processedValue, cleanedSpans, formatState)
            // ⭐ END

            val decoratedText = decorateText(
                text = processedValue.text,
                formatMap = cleanedSpans,
                baseColor = textColor,
                primaryColor = primaryColor,
                grayColor = grayColor,
                formatState = formatState
            )

            internalContent = processedValue.copy(annotatedString = decoratedText)
            onContentChange(processedValue)
        },

        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChanged(it.isFocused) },

        textStyle = LocalTextStyle.current.copy(
            fontSize = 16.sp,
            color = textColor,
            textAlign = formatState.textAlign,
            lineHeight = 22.sp
        ),

        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),

        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp, start = (formatState.indentLevel * 16).dp)
                    .then(
                        if (formatState.isBlockquote) {
                            Modifier
                                .border(
                                    3.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                        } else Modifier
                    )
            ) {
                inner()
            }
        }
    )
}