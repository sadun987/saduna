package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProgrammingLanguage
import com.example.ui.components.ArcaneTypingIndicator
import com.example.ui.components.CodeBlockCard
import com.example.ui.components.GlassRuneCard
import com.example.ui.components.MarkdownMessageContent
import com.example.ui.theme.AbyssalCrimson
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldMana
import com.example.ui.theme.LavenderText
import com.example.ui.theme.MutedRune
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ShadowSurface
import com.example.ui.theme.StarlightWhite
import com.example.ui.viewmodel.AiStreamState
import com.example.ui.viewmodel.GhostMalliViewModel

@Composable
fun StudioScreen(
    viewModel: GhostMalliViewModel,
    onOpenCodeInEditor: (String, ProgrammingLanguage, String, String) -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Forge", "Debugger", "Sinhala", "Projects")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = "ARCANE CODING STUDIO",
            style = MaterialTheme.typography.headlineMedium,
            color = StarlightWhite,
            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ShadowSurface,
            contentColor = ElectricCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ElectricCyan
                )
            }
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedTab == idx) ElectricCyan else LavenderText
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedTab) {
            0 -> CodeForgeTab(viewModel, onOpenCodeInEditor, onShowSnackbar)
            1 -> CodeDebuggerTab(viewModel, onOpenCodeInEditor, onShowSnackbar)
            2 -> SinhalaExplainerTab(viewModel, onOpenCodeInEditor, onShowSnackbar)
            else -> ProjectCreatorTab(viewModel, onOpenCodeInEditor, onShowSnackbar)
        }
    }
}

@Composable
private fun LanguagePickerStrip(
    selected: ProgrammingLanguage,
    onSelect: (ProgrammingLanguage) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ProgrammingLanguage.entries.forEach { lang ->
            val isSelected = lang == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(lang) },
                label = { Text(lang.displayName, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonViolet.copy(alpha = 0.25f),
                    selectedLabelColor = ElectricCyan,
                    containerColor = ShadowSurface,
                    labelColor = LavenderText
                )
            )
        }
    }
}

@Composable
private fun CodeForgeTab(
    viewModel: GhostMalliViewModel,
    onOpenCodeInEditor: (String, ProgrammingLanguage, String, String) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val forgeState by viewModel.forgeState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var prompt by rememberSaveable { mutableStateOf("") }
    var targetLanguage by rememberSaveable { mutableStateOf(ProgrammingLanguage.KOTLIN) }
    var multiFile by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassRuneCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "A. AI Code Generator",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan
                )
                LanguagePickerStrip(selected = targetLanguage, onSelect = { targetLanguage = it })
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Describe the code or module you want...") },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = NeonViolet.copy(alpha = 0.45f),
                        focusedTextColor = StarlightWhite,
                        unfocusedTextColor = StarlightWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forge_prompt_input")
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = multiFile,
                        onCheckedChange = { multiFile = it },
                        colors = CheckboxDefaults.colors(checkedColor = NeonViolet)
                    )
                    Text(
                        text = "Include multi-file project structure",
                        style = MaterialTheme.typography.bodySmall,
                        color = LavenderText,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { viewModel.generateCode(prompt, targetLanguage, multiFile) },
                        enabled = prompt.isNotBlank() && forgeState !is AiStreamState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                        modifier = Modifier.testTag("forge_generate_button")
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Forge Code")
                    }
                }
            }
        }

        when (val state = forgeState) {
            is AiStreamState.Loading -> ArcaneTypingIndicator("Forging ${targetLanguage.displayName} code...")
            is AiStreamState.Streaming -> {
                GlassRuneCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Generated Output",
                                style = MaterialTheme.typography.titleSmall,
                                color = ElectricCyan,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(state.text))
                                onShowSnackbar("Output copied")
                            }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = StarlightWhite)
                            }
                        }
                        MarkdownMessageContent(
                            markdown = state.text,
                            fallbackLanguage = targetLanguage,
                            onCopyCode = {
                                clipboard.setText(AnnotatedString(it))
                                onShowSnackbar("Code copied")
                            },
                            onOpenInEditor = { code, lang ->
                                onOpenCodeInEditor(code, lang, "Forged ${lang.displayName}", "forged.${lang.extension}")
                            }
                        )
                    }
                }
            }
            is AiStreamState.Error -> StudioErrorBanner(state.message)
            is AiStreamState.Idle -> Unit
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CodeDebuggerTab(
    viewModel: GhostMalliViewModel,
    onOpenCodeInEditor: (String, ProgrammingLanguage, String, String) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val isLoading by viewModel.debugLoading.collectAsStateWithLifecycle()
    val result by viewModel.debugResult.collectAsStateWithLifecycle()
    val error by viewModel.debugError.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    var buggyCode by rememberSaveable { mutableStateOf("") }
    var errorLog by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf(ProgrammingLanguage.PYTHON) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassRuneCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "B. Void Code Debugger (Before & After)",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan
                )
                LanguagePickerStrip(selected = language, onSelect = { language = it })
                OutlinedTextField(
                    value = buggyCode,
                    onValueChange = { buggyCode = it },
                    label = { Text("Paste code with bug...") },
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = NeonViolet.copy(alpha = 0.45f),
                        focusedTextColor = StarlightWhite,
                        unfocusedTextColor = StarlightWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debugger_code_input")
                )
                OutlinedTextField(
                    value = errorLog,
                    onValueChange = { errorLog = it },
                    label = { Text("Optional error message / stack trace") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = NeonViolet.copy(alpha = 0.35f),
                        focusedTextColor = StarlightWhite,
                        unfocusedTextColor = StarlightWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.debugCode(buggyCode, errorLog, language) },
                    enabled = buggyCode.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debugger_analyze_button")
                ) {
                    Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze & Fix Bug")
                }
            }
        }

        if (isLoading) {
            ArcaneTypingIndicator("Inspecting stack traces & generating fix...")
        }
        if (!error.isNullOrBlank()) {
            StudioErrorBanner(error.orEmpty())
        }
        result?.let { debug ->
            GlassRuneCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Root Cause",
                        style = MaterialTheme.typography.titleSmall,
                        color = AbyssalCrimson
                    )
                    Text(
                        text = debug.rootCause,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StarlightWhite
                    )
                    Text(
                        text = debug.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LavenderText
                    )

                    Text(
                        text = "BEFORE (Buggy)",
                        style = MaterialTheme.typography.labelMedium,
                        color = AbyssalCrimson
                    )
                    CodeBlockCard(
                        code = debug.beforeCode,
                        languageLabel = "${language.displayName} • BEFORE",
                        language = language,
                        onCopy = {
                            clipboard.setText(AnnotatedString(debug.beforeCode))
                            onShowSnackbar("Original code copied")
                        },
                        onOpenInEditor = {
                            onOpenCodeInEditor(debug.beforeCode, language, "Buggy Code", "before.${language.extension}")
                        }
                    )

                    Text(
                        text = "AFTER (Fixed)",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldMana
                    )
                    CodeBlockCard(
                        code = debug.afterCode,
                        languageLabel = "${language.displayName} • FIXED",
                        language = language,
                        onCopy = {
                            clipboard.setText(AnnotatedString(debug.afterCode))
                            onShowSnackbar("Fixed code copied")
                        },
                        onOpenInEditor = {
                            onOpenCodeInEditor(debug.afterCode, language, "Fixed Code", "fixed.${language.extension}")
                        }
                    )

                    if (debug.improvements.isNotEmpty()) {
                        Text(
                            text = "Suggested Improvements",
                            style = MaterialTheme.typography.titleSmall,
                            color = ElectricCyan
                        )
                        debug.improvements.forEach { tip ->
                            Text(
                                text = "• $tip",
                                style = MaterialTheme.typography.bodySmall,
                                color = StarlightWhite
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onOpenCodeInEditor(debug.afterCode, language, "Fixed ${language.displayName}", "fixed.${language.extension}")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF04131A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.EditNote, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Fixed Code to Editor")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SinhalaExplainerTab(
    viewModel: GhostMalliViewModel,
    onOpenCodeInEditor: (String, ProgrammingLanguage, String, String) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val explainState by viewModel.explainState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var codeInput by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf(ProgrammingLanguage.KOTLIN) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassRuneCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "C. Code Explainer (Simple Sinhala / English)",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan
                )
                Text(
                    text = "ඕනෑම code එකක් paste කරන්න. Functions, variables, algorithms සහ උදාහරණ සරල සිංහලෙන් පැහැදිලි කර දෙයි.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LavenderText
                )
                LanguagePickerStrip(selected = language, onSelect = { language = it })
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("Paste code to explain...") },
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = NeonViolet.copy(alpha = 0.45f),
                        focusedTextColor = StarlightWhite,
                        unfocusedTextColor = StarlightWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explainer_code_input")
                )
                Button(
                    onClick = { viewModel.explainCodeInSinhala(codeInput, language) },
                    enabled = codeInput.isNotBlank() && explainState !is AiStreamState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explainer_run_button")
                ) {
                    Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Explain Line-by-Line")
                }
            }
        }

        when (val state = explainState) {
            is AiStreamState.Loading -> ArcaneTypingIndicator("Translating logic into simple Sinhala...")
            is AiStreamState.Streaming -> {
                GlassRuneCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Explanation",
                                style = MaterialTheme.typography.titleSmall,
                                color = ElectricCyan,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(state.text))
                                onShowSnackbar("Explanation copied")
                            }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = StarlightWhite)
                            }
                        }
                        MarkdownMessageContent(
                            markdown = state.text,
                            fallbackLanguage = language,
                            onCopyCode = {
                                clipboard.setText(AnnotatedString(it))
                                onShowSnackbar("Example copied")
                            },
                            onOpenInEditor = { code, lang ->
                                onOpenCodeInEditor(code, lang, "Explainer Example", "example.${lang.extension}")
                            }
                        )
                    }
                }
            }
            is AiStreamState.Error -> StudioErrorBanner(state.message)
            is AiStreamState.Idle -> Unit
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProjectCreatorTab(
    viewModel: GhostMalliViewModel,
    onOpenCodeInEditor: (String, ProgrammingLanguage, String, String) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val isLoading by viewModel.projectLoading.collectAsStateWithLifecycle()
    val bundle by viewModel.projectBundle.collectAsStateWithLifecycle()
    val error by viewModel.projectError.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    val projectTypes = listOf(
        "Android App",
        "Website (HTML/CSS/JS)",
        "Python App",
        "Java App",
        "CSS Design System",
        "Backend API"
    )
    var selectedType by rememberSaveable { mutableStateOf(projectTypes.first()) }
    var primaryLanguage by rememberSaveable { mutableStateOf(ProgrammingLanguage.KOTLIN) }
    var prompt by rememberSaveable { mutableStateOf("") }
    var selectedFileIndex by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassRuneCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "D. Multi-File Project Architect",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    projectTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricCyan,
                                containerColor = ShadowSurface,
                                labelColor = LavenderText
                            )
                        )
                    }
                }
                LanguagePickerStrip(selected = primaryLanguage, onSelect = { primaryLanguage = it })
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Describe the project features...") },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = NeonViolet.copy(alpha = 0.45f),
                        focusedTextColor = StarlightWhite,
                        unfocusedTextColor = StarlightWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_prompt_input")
                )
                Button(
                    onClick = {
                        selectedFileIndex = 0
                        viewModel.createProject(selectedType, prompt, primaryLanguage)
                    },
                    enabled = prompt.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_create_button")
                ) {
                    Icon(Icons.Rounded.FolderSpecial, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scaffold Complete Project")
                }
            }
        }

        if (isLoading) {
            ArcaneTypingIndicator("Architecting project files & dependencies...")
        }
        if (!error.isNullOrBlank()) {
            StudioErrorBanner(error.orEmpty())
        }

        bundle?.let { proj ->
            GlassRuneCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = proj.projectName,
                        style = MaterialTheme.typography.titleLarge,
                        color = ElectricCyan
                    )
                    Text(
                        text = proj.architectureSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StarlightWhite
                    )
                    if (proj.setupSteps.isNotEmpty()) {
                        Text(
                            text = "Setup Steps",
                            style = MaterialTheme.typography.titleSmall,
                            color = NeonViolet
                        )
                        proj.setupSteps.forEachIndexed { idx, step ->
                            Text(
                                text = "${idx + 1}. $step",
                                style = MaterialTheme.typography.bodySmall,
                                color = LavenderText
                            )
                        }
                    }

                    if (proj.files.isNotEmpty()) {
                        val safeIdx = selectedFileIndex.coerceIn(0, proj.files.lastIndex)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            proj.files.forEachIndexed { index, file ->
                                FilterChip(
                                    selected = safeIdx == index,
                                    onClick = { selectedFileIndex = index },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    label = { Text(file.path, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonViolet.copy(alpha = 0.25f),
                                        selectedLabelColor = ElectricCyan,
                                        containerColor = ShadowSurface,
                                        labelColor = StarlightWhite
                                    )
                                )
                            }
                        }

                        val activeFile = proj.files[safeIdx]
                        val fileLang = ProgrammingLanguage.fromName(activeFile.language)
                        if (activeFile.purpose.isNotBlank()) {
                            Text(
                                text = activeFile.purpose,
                                style = MaterialTheme.typography.bodySmall,
                                color = LavenderText
                            )
                        }
                        CodeBlockCard(
                            code = activeFile.content,
                            languageLabel = activeFile.path,
                            language = fileLang,
                            onCopy = {
                                clipboard.setText(AnnotatedString(activeFile.content))
                                onShowSnackbar("Copied ${activeFile.path}")
                            },
                            onOpenInEditor = {
                                onOpenCodeInEditor(
                                    activeFile.content,
                                    fileLang,
                                    proj.projectName,
                                    activeFile.path.substringAfterLast('/')
                                )
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StudioErrorBanner(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AbyssalCrimson.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, AbyssalCrimson),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = StarlightWhite,
            modifier = Modifier.padding(12.dp)
        )
    }
}
