package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CodeSnippet
import com.example.data.model.ProgrammingLanguage
import com.example.ui.components.GlassRuneCard
import com.example.ui.components.SyntaxHighlightTransformation
import com.example.ui.theme.AbyssalCrimson
import com.example.ui.theme.AbyssalPurple
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LavenderText
import com.example.ui.theme.MutedRune
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ShadowSurface
import com.example.ui.theme.StarlightWhite
import com.example.ui.viewmodel.GhostMalliViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    viewModel: GhostMalliViewModel,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val code by viewModel.editorCode.collectAsStateWithLifecycle()
    val language by viewModel.editorLanguage.collectAsStateWithLifecycle()
    val fileName by viewModel.editorFileName.collectAsStateWithLifecycle()
    val snippets by viewModel.snippets.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var showLanguageMenu by remember { mutableStateOf(false) }
    var showVaultSheet by remember { mutableStateOf(false) }

    // Zero-permission Storage Access Framework file download/export
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(code.toByteArray(Charsets.UTF_8))
                }
                onShowSnackbar("Downloaded $fileName")
            } catch (e: Exception) {
                onShowSnackbar("Export failed: ${e.localizedMessage ?: "I/O error"}")
            }
        }
    }

    val lineCount = remember(code) { maxOf(1, code.count { it == '\n' } + 1) }
    val lineNumbersText = remember(lineCount) {
        (1..lineCount).joinToString("\n")
    }
    val syntaxTransformation = remember(language) { SyntaxHighlightTransformation(language) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Editor Header & Metadata
        GlassRuneCard(
            cornerRadius = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { viewModel.updateEditorFileName(it) },
                        label = { Text("File Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = NeonViolet.copy(alpha = 0.4f),
                            focusedTextColor = StarlightWhite,
                            unfocusedTextColor = StarlightWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        Surface(
                            onClick = { showLanguageMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1F1438),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("editor_language_picker")
                        ) {
                            Text(
                                text = language.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                            containerColor = ShadowSurface
                        ) {
                            ProgrammingLanguage.entries.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${lang.displayName} (.${lang.extension})",
                                            color = if (lang == language) ElectricCyan else StarlightWhite
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateEditorLanguage(lang)
                                        showLanguageMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Action Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorActionButton(
                        icon = Icons.Rounded.BookmarkAdd,
                        label = "Save",
                        tint = ElectricCyan,
                        onClick = { viewModel.saveEditorSnippet() }
                    )
                    EditorActionButton(
                        icon = Icons.Rounded.FolderOpen,
                        label = "Vault (${snippets.size})",
                        tint = NeonViolet,
                        onClick = { showVaultSheet = true }
                    )
                    EditorActionButton(
                        icon = Icons.Rounded.ContentCopy,
                        label = "Copy",
                        tint = StarlightWhite,
                        onClick = {
                            clipboard.setText(AnnotatedString(code))
                            onShowSnackbar("Code copied to clipboard")
                        }
                    )
                    EditorActionButton(
                        icon = Icons.Rounded.Download,
                        label = "Download",
                        tint = ElectricCyan,
                        onClick = {
                            val safeName = fileName.ifBlank { "code.${language.extension}" }
                            createDocumentLauncher.launch(safeName)
                        }
                    )
                    EditorActionButton(
                        icon = Icons.Rounded.Share,
                        label = "Share",
                        tint = StarlightWhite,
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, fileName)
                                putExtra(Intent.EXTRA_TEXT, code)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share $fileName"))
                        }
                    )
                    EditorActionButton(
                        icon = Icons.Rounded.CleaningServices,
                        label = "Clear",
                        tint = AbyssalCrimson,
                        onClick = { viewModel.clearEditor() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Syntax-Highlighted Code Editor Surface with Synchronized Line Numbers
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF090512),
            border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.5f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val verticalScroll = rememberScrollState()
            val horizontalScroll = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
            ) {
                // Line numbers gutter
                Box(
                    modifier = Modifier
                        .background(Color(0xFF120A24))
                        .padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = lineNumbersText,
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MutedRune,
                            textAlign = TextAlign.End
                        )
                    )
                }

                // Editable syntax-highlighted code surface
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScroll)
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = code,
                        onValueChange = { viewModel.updateEditorCode(it) },
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = StarlightWhite
                        ),
                        cursorBrush = SolidColor(ElectricCyan),
                        visualTransformation = syntaxTransformation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("code_editor_textfield")
                    )
                    if (code.isEmpty()) {
                        Text(
                            text = "// Write or paste ${language.displayName} code here...",
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MutedRune
                            )
                        )
                    }
                }
            }
        }

        // Status Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Lines: $lineCount  •  Chars: ${code.length}",
                style = MaterialTheme.typography.labelSmall,
                color = LavenderText
            )
            Text(
                text = "${language.displayName} • UTF-8",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan
            )
        }
    }

    if (showVaultSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showVaultSheet = false },
            sheetState = sheetState,
            containerColor = AbyssalPurple,
            contentColor = StarlightWhite
        ) {
            SnippetVaultSheet(
                snippets = snippets,
                onSelect = { snippet ->
                    viewModel.loadSnippetIntoEditor(snippet)
                    showVaultSheet = false
                },
                onDelete = { snippet ->
                    viewModel.deleteSnippet(snippet.id)
                }
            )
        }
    }
}

@Composable
private fun EditorActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1A1030),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = StarlightWhite
            )
        }
    }
}

@Composable
private fun SnippetVaultSheet(
    snippets: List<CodeSnippet>,
    onSelect: (CodeSnippet) -> Unit,
    onDelete: (CodeSnippet) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "CLOUD CODE VAULT",
            style = MaterialTheme.typography.titleLarge,
            color = StarlightWhite
        )
        if (snippets.isEmpty()) {
            Text(
                text = "No saved scripts yet. Tap Save in the editor to store files in your cloud vault.",
                style = MaterialTheme.typography.bodyMedium,
                color = LavenderText,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                items(snippets, key = { it.id }) { snippet ->
                    GlassRuneCard(
                        cornerRadius = 12.dp,
                        glowBorder = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(snippet) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = snippet.fileName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "${snippet.language} • ${snippet.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LavenderText
                                )
                            }
                            IconButton(onClick = { onDelete(snippet) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete snippet",
                                    tint = AbyssalCrimson
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
