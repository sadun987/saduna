package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProgrammingLanguage
import com.example.ui.theme.AmberRune
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldMana
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MutedRune
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StarlightWhite

private val KeywordColor = Color(0xFFD580FF)
private val StringColor = EmeraldMana
private val CommentColor = MutedRune
private val NumberColor = AmberRune
private val TagOrTypeColor = ElectricCyan

fun highlightCode(code: String, language: ProgrammingLanguage): AnnotatedString {
    return buildAnnotatedString {
        append(code)

        // Comments (//..., #..., --..., /* ... */)
        val commentRegex = Regex("""(//[^\n]*|#[^\n]*|--[^\n]*|/\*[\s\S]*?\*/)""")
        commentRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = CommentColor), match.range.first, match.range.last + 1)
        }

        // Strings ("...", '...', `...`)
        val stringRegex = Regex("""("([^"\\]|\\.)*"|'([^'\\]|\\.)*'|`([^`\\]|\\.)*`)""")
        stringRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = StringColor), match.range.first, match.range.last + 1)
        }

        // Numbers
        val numberRegex = Regex("""\b\d+(\.\d+)?\b""")
        numberRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = NumberColor), match.range.first, match.range.last + 1)
        }

        // Tags or @Annotations
        val annotationOrTagRegex = Regex("""(@[A-Za-z_][A-Za-z0-9_]*|</?[A-Za-z][A-Za-z0-9_:-]*\b|>)""")
        annotationOrTagRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = TagOrTypeColor), match.range.first, match.range.last + 1)
        }

        // Language keywords
        if (language.keywords.isNotEmpty()) {
            val wordRegex = Regex("""\b[A-Za-z_][A-Za-z0-9_]*\b""")
            wordRegex.findAll(code).forEach { match ->
                if (match.value in language.keywords || match.value.uppercase() in language.keywords) {
                    addStyle(
                        SpanStyle(color = KeywordColor, fontWeight = FontWeight.SemiBold),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }
        }
    }
}

class SyntaxHighlightTransformation(
    private val language: ProgrammingLanguage
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = highlightCode(text.text, language),
            offsetMapping = OffsetMapping.Identity
        )
    }
}

sealed interface MessageSegment {
    data class PlainText(val text: String) : MessageSegment
    data class CodeBlock(val languageHint: String, val code: String) : MessageSegment
}

fun parseMarkdownSegments(input: String): List<MessageSegment> {
    if (input.isEmpty()) return emptyList()
    val segments = mutableListOf<MessageSegment>()
    val fenceRegex = Regex("""```([A-Za-z0-9_+#-]*)\n([\s\S]*?)```""")
    var lastIndex = 0

    for (match in fenceRegex.findAll(input)) {
        if (match.range.first > lastIndex) {
            val textPart = input.substring(lastIndex, match.range.first).trim()
            if (textPart.isNotEmpty()) segments.add(MessageSegment.PlainText(textPart))
        }
        val langHint = match.groupValues[1].trim()
        val codeBody = match.groupValues[2].trimEnd()
        segments.add(MessageSegment.CodeBlock(langHint, codeBody))
        lastIndex = match.range.last + 1
    }

    if (lastIndex < input.length) {
        val tail = input.substring(lastIndex)
        // Handle unclosed streaming fence gracefully
        val openFenceIdx = tail.indexOf("```")
        if (openFenceIdx >= 0) {
            val beforeFence = tail.substring(0, openFenceIdx).trim()
            if (beforeFence.isNotEmpty()) segments.add(MessageSegment.PlainText(beforeFence))
            val rest = tail.substring(openFenceIdx + 3)
            val nl = rest.indexOf('\n')
            if (nl >= 0) {
                val hint = rest.substring(0, nl).trim()
                val body = rest.substring(nl + 1)
                segments.add(MessageSegment.CodeBlock(hint, body))
            } else {
                segments.add(MessageSegment.PlainText(tail))
            }
        } else if (tail.isNotBlank()) {
            segments.add(MessageSegment.PlainText(tail.trim()))
        }
    }
    return segments
}

@Composable
fun MarkdownMessageContent(
    markdown: String,
    fallbackLanguage: ProgrammingLanguage,
    onCopyCode: (String) -> Unit,
    onOpenInEditor: (String, ProgrammingLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = remember(markdown) { parseMarkdownSegments(markdown) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (segments.isEmpty()) {
            Text(
                text = markdown,
                style = MaterialTheme.typography.bodyLarge,
                color = StarlightWhite
            )
        } else {
            segments.forEach { segment ->
                when (segment) {
                    is MessageSegment.PlainText -> {
                        Text(
                            text = segment.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = StarlightWhite
                        )
                    }
                    is MessageSegment.CodeBlock -> {
                        val detectedLang = remember(segment.languageHint, fallbackLanguage) {
                            if (segment.languageHint.isNotBlank()) {
                                ProgrammingLanguage.fromName(segment.languageHint)
                            } else {
                                fallbackLanguage
                            }
                        }
                        CodeBlockCard(
                            code = segment.code,
                            languageLabel = segment.languageHint.ifBlank { detectedLang.displayName },
                            language = detectedLang,
                            onCopy = { onCopyCode(segment.code) },
                            onOpenInEditor = { onOpenInEditor(segment.code, detectedLang) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    code: String,
    languageLabel: String,
    language: ProgrammingLanguage,
    onCopy: () -> Unit,
    onOpenInEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highlighted = remember(code, language) { highlightCode(code, language) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0A0614),
        border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.45f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF160E2B))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = languageLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onOpenInEditor,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = "Open in Code Editor",
                        tint = NeonViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy code",
                        tint = StarlightWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = highlighted,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = StarlightWhite
                )
            }
        }
    }
}
