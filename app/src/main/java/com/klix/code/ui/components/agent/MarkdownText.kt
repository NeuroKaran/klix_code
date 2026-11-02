package com.klix.code.ui.components.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.ui.theme.AccentPrimary
import com.klix.code.ui.theme.DarkBg
import com.klix.code.ui.theme.DarkBorder
import com.klix.code.ui.theme.DarkSurfaceVariant
import com.klix.code.ui.theme.TextMuted
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class MarkdownBlock {
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class HeaderBlock(val level: Int, val text: String) : MarkdownBlock()
    data class ListItemBlock(val prefix: String, val text: String) : MarkdownBlock()
    data class ParagraphBlock(val text: String) : MarkdownBlock()
}

/**
 * Parses markdown text into high-level structured blocks (code blocks, headers, lists, paragraphs).
 */
fun parseMarkdown(raw: String): List<MarkdownBlock> {
    if (raw.isBlank()) return emptyList()

    val blocks = mutableListOf<MarkdownBlock>()
    val lines = raw.lines()
    var index = 0

    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trimStart()

        // Check for start of code block
        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            index++
            while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                codeLines.add(lines[index])
                index++
            }
            if (index < lines.size && lines[index].trimStart().startsWith("```")) {
                index++ // skip closing ```
            }
            blocks.add(MarkdownBlock.CodeBlock(language = language, code = codeLines.joinToString("\n")))
            continue
        }

        // Check for headers
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            if (level in 1..4 && trimmed.length > level && trimmed[level] == ' ') {
                val headerText = trimmed.drop(level + 1).trim()
                blocks.add(MarkdownBlock.HeaderBlock(level = level, text = headerText))
                index++
                continue
            }
        }

        // Check for bullet list items
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
            val bulletText = trimmed.drop(2).trim()
            blocks.add(MarkdownBlock.ListItemBlock(prefix = "•", text = bulletText))
            index++
            continue
        }

        // Check for numbered list items (e.g. "1. ")
        val numberedMatch = Regex("""^(\d+)\.\s+(.*)""").find(trimmed)
        if (numberedMatch != null) {
            val num = numberedMatch.groupValues[1]
            val itemText = numberedMatch.groupValues[2]
            blocks.add(MarkdownBlock.ListItemBlock(prefix = "$num.", text = itemText))
            index++
            continue
        }

        // Regular paragraph or contiguous lines
        if (trimmed.isNotBlank()) {
            val paraLines = mutableListOf<String>()
            while (index < lines.size) {
                val nextTrimmed = lines[index].trimStart()
                if (nextTrimmed.isBlank() ||
                    nextTrimmed.startsWith("```") ||
                    (nextTrimmed.startsWith("#") && nextTrimmed.contains(' ')) ||
                    nextTrimmed.startsWith("- ") ||
                    nextTrimmed.startsWith("* ") ||
                    Regex("""^\d+\.\s+""").containsMatchIn(nextTrimmed)
                ) {
                    break
                }
                paraLines.add(lines[index])
                index++
            }
            blocks.add(MarkdownBlock.ParagraphBlock(text = paraLines.joinToString("\n")))
            continue
        }

        index++
    }

    return blocks
}

/**
 * Builds an AnnotatedString from markdown text, styling bold, italic, inline code, and @ tagged files.
 */
fun buildMarkdownAnnotatedString(
    text: String,
    baseColor: Color = TextPrimary
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val len = text.length

        while (cursor < len) {
            // Check for inline code: `code`
            if (text[cursor] == '`') {
                val end = text.indexOf('`', cursor + 1)
                if (end != -1) {
                    val codeContent = text.substring(cursor + 1, end)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF79C0FF),
                            background = Color(0x334B5563),
                            fontSize = 12.sp
                        )
                    )
                    append(" $codeContent ")
                    pop()
                    cursor = end + 1
                    continue
                }
            }

            // Check for bold: **bold**
            if (cursor + 1 < len && text[cursor] == '*' && text[cursor + 1] == '*') {
                val end = text.indexOf("**", cursor + 2)
                if (end != -1) {
                    val boldContent = text.substring(cursor + 2, end)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary))
                    append(boldContent)
                    pop()
                    cursor = end + 2
                    continue
                }
            }

            // Check for italic: *italic* (single asterisk)
            if (text[cursor] == '*' && (cursor == 0 || text[cursor - 1] != '*') && (cursor + 1 < len && text[cursor + 1] != '*')) {
                val end = text.indexOf('*', cursor + 1)
                if (end != -1 && (end + 1 >= len || text[end + 1] != '*')) {
                    val italicContent = text.substring(cursor + 1, end)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = TextPrimary))
                    append(italicContent)
                    pop()
                    cursor = end + 1
                    continue
                }
            }

            // Check for @ tag: @file_or_dir
            if (text[cursor] == '@') {
                var end = cursor + 1
                while (end < len && (text[end].isLetterOrDigit() || text[end] in "_-./")) {
                    end++
                }
                if (end > cursor + 1) {
                    val tagContent = text.substring(cursor, end)
                    pushStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = AccentPrimary,
                            background = AccentPrimary.copy(alpha = 0.15f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp
                        )
                    )
                    append(tagContent)
                    pop()
                    cursor = end
                    continue
                }
            }

            // Normal character
            append(text[cursor])
            cursor++
        }
    }
}

/**
 * Beautiful, readable Jetpack Compose Markdown Renderer with typography hierarchy and syntax code blocks.
 */
@Composable
fun MarkdownMessageView(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimary,
    onTagClicked: ((String) -> Unit)? = null
) {
    val blocks = remember(content) { parseMarkdown(content) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.HeaderBlock -> {
                    val fontSize = when (block.level) {
                        1 -> 17.sp
                        2 -> 15.5.sp
                        3 -> 14.5.sp
                        else -> 13.5.sp
                    }
                    val fontWeight = when (block.level) {
                        1 -> FontWeight.ExtraBold
                        2 -> FontWeight.Bold
                        else -> FontWeight.SemiBold
                    }
                    val color = if (block.level == 1) TextPrimary else if (block.level == 2) Color(0xFFF0F6FC) else AccentPrimary

                    Text(
                        text = buildMarkdownAnnotatedString(block.text, color),
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = color,
                        lineHeight = (fontSize.value * 1.35f).sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                is MarkdownBlock.ParagraphBlock -> {
                    Text(
                        text = buildMarkdownAnnotatedString(block.text, textColor),
                        fontSize = 14.sp,
                        color = textColor,
                        lineHeight = 21.sp,
                        letterSpacing = 0.2.sp
                    )
                }

                is MarkdownBlock.ListItemBlock -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = block.prefix,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPrimary,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        Text(
                            text = buildMarkdownAnnotatedString(block.text, textColor),
                            fontSize = 14.sp,
                            color = textColor,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCard(language = block.language, code = block.code)
                }
            }
        }
    }
}

/**
 * Modern code block container with language tag chip, horizontal scrolling, and copy button.
 */
@Composable
fun CodeBlockCard(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBg)
            .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
    ) {
        // Code Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (language.isNotBlank()) language.uppercase() else "CODE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        isCopied = true
                        scope.launch {
                            delay(2000)
                            isCopied = false
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (isCopied) Color(0xFF10B981) else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (isCopied) {
                    Text(
                        text = "Copied",
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Code Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(10.dp)
        ) {
            Text(
                text = code,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE6EDF3),
                lineHeight = 18.sp
            )
        }
    }
}
