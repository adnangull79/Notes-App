package com.example.notepad.CreatNotes1



import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import kotlin.math.min

// --- Global constants for Checkbox prefixes ---
const val UNCHECKED_PREFIX = "☐ "
const val CHECKED_PREFIX = "☑ "

// ==============================================
// DATA CLASSES (MODELS)
// ==============================================

// Data class to store formatting information for a text span
data class FormatSpan(
    var start: Int,
    var end: Int,
    val style: SpanStyle
)

data class ChecklistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val checked: Boolean = false
)

data class FormatState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isH1: Boolean = false,
    val isH2: Boolean = false,
    val isBulletList: Boolean = false,
    val isNumberedList: Boolean = false,
    var listCounter: Int = 1,
    val textColor: Color = Color.Unspecified,
    val highlightColor: Color = Color.Unspecified,
    val textAlign: TextAlign = TextAlign.Start,
    val indentLevel: Int = 0,
    val isBlockquote: Boolean = false,
    val lineSpacing: Float = 1.0f
)

data class TextHistory(
    val value: TextFieldValue,
    val formatSpans: List<FormatSpan>,
    val formatState: FormatState,
    val timestamp: Long = System.currentTimeMillis()
)

// A map to store formatting information per text range
typealias FormatMap = List<FormatSpan>

// ==============================================
// UTILITY FUNCTIONS
// ==============================================

fun commonPrefixLength(a: String, b: String): Int {
    val max = min(a.length, b.length)
    var i = 0
    while (i < max && a[i] == b[i]) i++
    return i
}

fun commonSuffixLength(a: String, b: String, prefixLen: Int): Int {
    val aRemain = a.length - prefixLen
    val bRemain = b.length - prefixLen
    val max = min(aRemain, bRemain)
    var i = 0
    while (i < max && a[a.length - 1 - i] == b[b.length - 1 - i]) i++
    return i
}

fun findLineStart(text: String, cursorPos: Int): Int {
    return text.lastIndexOf('\n', cursorPos - 1).let {
        if (it == -1) 0 else it + 1
    }
}

fun insertCheckboxAtCursor(textFieldValue: TextFieldValue): TextFieldValue {
    val text = textFieldValue.text
    val cursorPos = textFieldValue.selection.start
    val lineStart = findLineStart(text, cursorPos)
    val lineText = text.substring(lineStart, cursorPos).trim()

    val checkboxPrefix = UNCHECKED_PREFIX

    val insertLine = if (lineStart == cursorPos) "" else "\n"
    val insertIndex = if (lineText.isEmpty()) lineStart else cursorPos

    val newText = text.substring(0, insertIndex) + insertLine + checkboxPrefix + text.substring(insertIndex)
    val newCursorPosition = insertIndex + insertLine.length + checkboxPrefix.length

    return TextFieldValue(
        text = newText,
        selection = TextRange(newCursorPosition)
    )
}

fun toggleCheckbox(textFieldValue: TextFieldValue): TextFieldValue {
    val text = textFieldValue.text
    val cursor = textFieldValue.selection.start

    val lineStart = findLineStart(text, cursor)
    val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
    val line = text.substring(lineStart, lineEnd)

    val isChecked = line.startsWith(CHECKED_PREFIX)
    val isUnchecked = line.startsWith(UNCHECKED_PREFIX)

    if (!isChecked && !isUnchecked) return textFieldValue

    val oldPrefix = if (isChecked) CHECKED_PREFIX else UNCHECKED_PREFIX
    val newPrefix = if (isChecked) UNCHECKED_PREFIX else CHECKED_PREFIX

    val newLine = newPrefix + line.removePrefix(oldPrefix)
    val newText = text.replaceRange(lineStart, lineEnd, newLine)

    val newCursor = cursor + (newPrefix.length - oldPrefix.length)

    return TextFieldValue(
        text = newText,
        selection = TextRange(newCursor)
    )
}

fun adjustSpansForInsertion(spans: MutableList<FormatSpan>, index: Int, insLen: Int) {
    for (span in spans) {
        if (span.start >= index) span.start += insLen
        if (span.end >= index) span.end += insLen
    }
}

fun adjustSpansForDeletion(spans: MutableList<FormatSpan>, delStart: Int, delEnd: Int) {
    val delLen = delEnd - delStart
    val toRemove = mutableListOf<FormatSpan>()
    for (span in spans) {
        when {
            span.end <= delStart -> { /* no-op */ }
            span.start >= delEnd -> {
                span.start -= delLen
                span.end -= delLen
            }
            span.start >= delStart && span.end <= delEnd -> {
                toRemove.add(span)
            }
            span.start < delStart && span.end in (delStart + 1)..delEnd -> {
                span.end = delStart
            }
            span.start in delStart until delEnd && span.end > delEnd -> {
                span.start = delStart
                span.end -= delLen
            }
            span.start < delStart && span.end > delEnd -> {
                span.end -= delLen
            }
        }
    }
    spans.removeAll(toRemove)
}

fun composeActiveStyle(formatState: FormatState, baseColor: Color): SpanStyle? {
    val size = when {
        formatState.isH1 -> 28.sp
        formatState.isH2 -> 22.sp
        else -> 16.sp
    }
    val weight = when {
        formatState.isH1 || formatState.isH2 || formatState.isBold -> FontWeight.Bold
        else -> FontWeight.Normal
    }
    val italic = if (formatState.isItalic) FontStyle.Italic else FontStyle.Normal

    val decorationList = mutableListOf<TextDecoration>()
    if (formatState.isUnderline) decorationList.add(TextDecoration.Underline)
    if (formatState.isStrikethrough) decorationList.add(TextDecoration.LineThrough)
    val textDecoration = if (decorationList.isNotEmpty()) TextDecoration.combine(decorationList) else TextDecoration.None

    val textColor = if (formatState.textColor != Color.Unspecified) formatState.textColor else baseColor
    val bgColor = if (formatState.highlightColor != Color.Unspecified) formatState.highlightColor else Color.Unspecified

    val isDefault = weight == FontWeight.Normal &&
            italic == FontStyle.Normal &&
            textDecoration == TextDecoration.None &&
            size == 16.sp &&
            formatState.textColor == Color.Unspecified &&
            formatState.highlightColor == Color.Unspecified

    return if (isDefault) null else SpanStyle(
        fontSize = size,
        fontWeight = weight,
        fontStyle = italic,
        textDecoration = textDecoration,
        color = textColor,
        background = bgColor
    )
}

fun decorateText(
    text: String,
    formatMap: FormatMap,
    baseColor: Color,
    primaryColor: Color,
    grayColor: Color,
    formatState: FormatState
): AnnotatedString {
    val builder = AnnotatedString.Builder(text)

    // Apply base style
    builder.addStyle(
        style = SpanStyle(color = baseColor, fontSize = 16.sp, fontWeight = FontWeight.Normal),
        start = 0,
        end = text.length
    )

    // Apply rich text spans
    formatMap.forEach { span ->
        val s = span.start.coerceIn(0, text.length)
        val e = span.end.coerceIn(0, text.length)
        if (e > s) {
            builder.addStyle(
                style = span.style,
                start = s,
                end = e
            )
        }
    }

    val lines = text.split('\n')
    var currentOffset = 0
    val BULLET = "•"

    // Apply paragraph/prefix styles (Checklists, Bullet, Numbered)
    lines.forEach { line ->
        if (line.startsWith(UNCHECKED_PREFIX) || line.startsWith(CHECKED_PREFIX)) {
            val isChecked = line.startsWith(CHECKED_PREFIX)

            // Style for the Checkbox character itself (☐ or ☑)
            builder.addStyle(
                style = SpanStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isChecked) primaryColor else grayColor
                ),
                start = currentOffset,
                end = currentOffset + 1
            )
            // Style for the content of the checked item (strikethrough and dimmed)
            if (isChecked && line.length > UNCHECKED_PREFIX.length - 1) {
                builder.addStyle(
                    style = SpanStyle(
                        textDecoration = TextDecoration.LineThrough,
                        color = baseColor.copy(alpha = 0.6f)
                    ),
                    start = currentOffset + UNCHECKED_PREFIX.length,
                    end = currentOffset + line.length
                )
            }
        } else if (line.startsWith(BULLET + " ")) {
            // Style for Bullet (•)
            builder.addStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                ),
                start = currentOffset,
                end = currentOffset + 1
            )
        } else if (line.matches(Regex("^\\d+\\.\\s*.*"))) {
            // Style for Numbered List
            val numberEnd = line.indexOf(". ").let { if (it == -1) line.indexOf(' ') else it }
            if (numberEnd != -1) {
                builder.addStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    ),
                    start = currentOffset,
                    end = currentOffset + numberEnd + 1
                )
            }
        }

        currentOffset += line.length + 1
    }

    return builder.toAnnotatedString()
}

fun parseChecklistFromContent(content: String): List<ChecklistItem> {
    if (content.isBlank()) return emptyList()
    return content.lines()
        .map { it.trim() }
        .filter { it.startsWith("[x]") || it.startsWith("[ ]") }
        .map { line ->
            val checked = line.startsWith("[x]")
            val text = line.removePrefix(if (checked) "[x]" else "[ ]").trim()
            ChecklistItem(text = text, checked = checked)
        }
}