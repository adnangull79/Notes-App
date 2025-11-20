//package com.example.notepad.CreatNotes
//
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.BasicTextField
//import androidx.compose.material3.LocalTextStyle
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.focus.onFocusChanged
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.SolidColor
//import androidx.compose.ui.text.TextRange
//import androidx.compose.ui.text.input.TextFieldValue
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import kotlin.math.max
//
//@Composable
//fun FormattedTextEditor(
//    content: TextFieldValue,
//    onContentChange: (TextFieldValue) -> Unit,
//    formatState: FormatState,
//    onFormatStateChange: (FormatState) -> Unit,
//    formatMap: FormatMap,
//    onFormatMapChange: (FormatMap) -> Unit,
//    textColor: Color,
//    onFocusChanged: (Boolean) -> Unit,
//    onCheckboxToggle: (TextFieldValue) -> Unit
//) {
//    val annotatedText = decorateText(
//        text = content.text,
//        formatMap = formatMap,
//        baseColor = textColor,
//        primaryColor = MaterialTheme.colorScheme.primary,
//        grayColor = Color.Gray,
//        formatState = formatState
//    )
//
//    // Using AnnotatedString constructor for TextFieldValue
//    val styledContent = content.copy(annotatedString = annotatedText)
//
//    BasicTextField(
//        value = styledContent,
//        onValueChange = { newValue ->
//            // ✅ Toggle only when user TAPs (selection changed + text same)
//            if (newValue.text == content.text && newValue.selection != content.selection) {
//                val toggled = toggleCheckbox(content)
//                if (toggled.text != content.text) {
//                    onCheckboxToggle(toggled)
//                    return@BasicTextField
//                }
//            }
//
//            var processedValue = newValue
//            val oldText = content.text
//            val newText = processedValue.text
//
//            val textChanged = newText != oldText
//            val isDeletion = newText.length < oldText.length
//
//            if (textChanged) {
//                // --- 1. Span adjustment (Rich Text Logic) ---
//                val prefixLen = commonPrefixLength(oldText, newText)
//                val oldSuffixLen = commonSuffixLength(oldText, newText, prefixLen)
//                val newSuffixLen = oldSuffixLen
//
//                val oldChangedEnd = oldText.length - oldSuffixLen
//                val newChangedEnd = newText.length - newSuffixLen
//
//                val deletedLen = max(0, oldChangedEnd - prefixLen)
//                val insertedLen = max(0, newChangedEnd - prefixLen)
//
//                var spans = formatMap.toMutableList()
//
//                if (deletedLen > 0) {
//                    adjustSpansForDeletion(spans, prefixLen, oldChangedEnd)
//                }
//                if (insertedLen > 0) {
//                    adjustSpansForInsertion(spans, prefixLen, insertedLen)
//                    // Apply currently active style to newly typed text
//                    composeActiveStyle(formatState, textColor)?.let { st ->
//                        spans.add(FormatSpan(prefixLen, prefixLen + insertedLen, st))
//                    }
//                }
//
//                spans = spans.filter { it.end > it.start }.sortedBy { it.start }.toMutableList()
//                onFormatMapChange(spans) // Trigger history push
//
//                // --- 2. Handle Enter key for Lists ---
//                if (!isDeletion && newText.length > oldText.length && newText.getOrNull(newValue.selection.start - 1) == '\n') {
//
//                    val cursorPos = newValue.selection.start
//                    val lineBeforeNewlineStart = findLineStart(oldText, content.selection.start - 1)
//                    val lineBeforeNewline = oldText.substring(lineBeforeNewlineStart, content.selection.start)
//                    val lineBeforeNewlineTrimmed = lineBeforeNewline.trim()
//
//                    when {
//                        // ✅ Checklist continuation first, stops other formatting
//                        lineBeforeNewlineTrimmed.startsWith(CHECKED_PREFIX) ||
//                                lineBeforeNewlineTrimmed.startsWith(UNCHECKED_PREFIX) -> {
//
//                            val marker = UNCHECKED_PREFIX
//                            val newTextWithMarker = newText.substring(0, cursorPos) + marker + newText.substring(cursorPos)
//                            processedValue = processedValue.copy(text = newTextWithMarker, selection = TextRange(cursorPos + marker.length))
//                        }
//                        // Bullet List Logic
//                        formatState.isBulletList -> {
//                            val marker = "• "
//                            val lineIsEmpty = lineBeforeNewlineTrimmed == marker.trim()
//
//                            if (lineIsEmpty) {
//                                // If line was just the marker, remove it and disable list
//                                val t = oldText.substring(0, lineBeforeNewlineStart) + newText.substring(cursorPos)
//                                processedValue = TextFieldValue(text = t, selection = TextRange(lineBeforeNewlineStart))
//                                onFormatStateChange(formatState.copy(isBulletList = false))
//                            } else if (lineBeforeNewlineTrimmed.startsWith(marker.trim())) {
//                                // If line contained content, continue the list
//                                val newTextWithMarker = newText.substring(0, cursorPos) + marker + newText.substring(cursorPos)
//                                processedValue = processedValue.copy(text = newTextWithMarker, selection = TextRange(cursorPos + marker.length))
//                            }
//                        }
//
//                        // Numbered List Logic
//                        formatState.isNumberedList -> {
//                            val numberRegex = Regex("^(\\d+)\\.\\s*$")
//                            val lastNumberMatch = numberRegex.find(lineBeforeNewlineTrimmed)
//
//                            val currentNumber = lastNumberMatch?.groupValues?.get(1)?.toIntOrNull() ?: formatState.listCounter
//                            val lineWasOnlyMarker = lineBeforeNewlineTrimmed.matches(Regex("^\\d+\\.$"))
//
//                            if (lineWasOnlyMarker) {
//                                // If line was just the marker, remove it and disable list
//                                val t = oldText.substring(0, lineBeforeNewlineStart) + newText.substring(cursorPos)
//                                processedValue = TextFieldValue(text = t, selection = TextRange(lineBeforeNewlineStart))
//                                onFormatStateChange(formatState.copy(isNumberedList = false, listCounter = 1))
//                            } else {
//                                // If line had content, continue the list
//                                val nextNumber = currentNumber + 1
//                                val marker = "$nextNumber. "
//                                val newTextWithMarker = newText.substring(0, cursorPos) + marker + newText.substring(cursorPos)
//                                processedValue = processedValue.copy(text = newTextWithMarker, selection = TextRange(cursorPos + marker.length))
//                                onFormatStateChange(formatState.copy(listCounter = nextNumber))
//                            }
//                        }
//
//                        // Checklist Continuation
//                        lineBeforeNewlineTrimmed.startsWith(CHECKED_PREFIX) || lineBeforeNewlineTrimmed.startsWith(
//                            UNCHECKED_PREFIX
//                        ) -> {
//                            // Automatically start a new line with an UNCHECKED marker
//                            val marker = UNCHECKED_PREFIX
//                            val newTextWithMarker = newText.substring(0, cursorPos) + marker + newText.substring(cursorPos)
//                            processedValue = processedValue.copy(text = newTextWithMarker, selection = TextRange(cursorPos + marker.length))
//                        }
//                    }
//                }
//            }
//
//            onContentChange(processedValue)
//        },
//        modifier = Modifier
//            .fillMaxWidth()
//            .onFocusChanged { onFocusChanged(it.isFocused) },
//        textStyle = LocalTextStyle.current.copy(
//            fontSize = 16.sp,
//            color = textColor,
//            textAlign = formatState.textAlign,
//            lineHeight = (16.sp.value * formatState.lineSpacing).sp // Apply line spacing
//        ),
//        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
//        decorationBox = { innerTextField ->
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(
//                        top = 4.dp,
//                        start = (formatState.indentLevel * 16).dp // Apply indent
//                    )
//                    .then(
//                        if (formatState.isBlockquote) {
//                            Modifier
//                                .border(
//                                    width = 3.dp,
//                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
//                                    shape = RoundedCornerShape(4.dp)
//                                )
//                                .background(
//                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
//                                    RoundedCornerShape(4.dp)
//                                )
//                                .padding(8.dp)
//                        } else Modifier
//                    )
//            ) {
//                if (content.text.isEmpty()) {
//                    Text(
//                        "Start writing...",
//                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
//                        fontSize = 16.sp
//                    )
//                }
//                innerTextField()
//            }
//        }
//    )
//}