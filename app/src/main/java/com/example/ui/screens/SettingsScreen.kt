package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.signOutUser
import com.example.data.model.AppReplyLanguage
import com.example.data.model.BackendMode
import com.example.data.model.GeminiModelOption
import com.example.data.prefs.FantasyAccentTheme
import com.example.ui.components.GhostAvatarOrb
import com.example.ui.components.GlassRuneCard
import com.example.ui.theme.AbyssalCrimson
import com.example.ui.theme.AbyssalPurple
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldMana
import com.example.ui.theme.LavenderText
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.StarlightWhite
import com.example.ui.viewmodel.GhostMalliViewModel

@Composable
fun SettingsScreen(
    viewModel: GhostMalliViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backendTestStatus by viewModel.backendTestStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var backendUrlInput by remember(settings.customBackendUrl) {
        mutableStateOf(settings.customBackendUrl)
    }
    var showClearDialog by remember { mutableStateOf(false) }
    var showSetupGuideDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SYSTEM CONFIGURATION",
            style = MaterialTheme.typography.headlineMedium,
            color = StarlightWhite,
            modifier = Modifier.padding(top = 4.dp)
        )

        // 1. Dark Fantasy Theme & Visuals
        SettingsSectionCard(icon = Icons.Rounded.Palette, title = "Dark Fantasy Theme") {
            FantasyAccentTheme.entries.forEach { theme ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.accentTheme == theme,
                        onClick = { viewModel.setAccentTheme(theme) },
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                    )
                    Text(
                        text = theme.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StarlightWhite
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Magical Glowing Particles",
                        style = MaterialTheme.typography.titleSmall,
                        color = StarlightWhite
                    )
                    Text(
                        text = "Animated arcane energy particles in background",
                        style = MaterialTheme.typography.bodySmall,
                        color = LavenderText
                    )
                }
                Switch(
                    checked = settings.particlesEnabled,
                    onCheckedChange = { viewModel.setParticlesEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan, checkedTrackColor = NeonViolet)
                )
            }
        }

        // 2. Language Settings
        SettingsSectionCard(icon = Icons.Rounded.Translate, title = "Language Settings (සිංහල & English)") {
            AppReplyLanguage.entries.forEach { lang ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.replyLanguage == lang,
                        onClick = { viewModel.setReplyLanguage(lang) },
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                    )
                    Text(
                        text = lang.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StarlightWhite
                    )
                }
            }
        }

        // 3. AI Model Configuration
        SettingsSectionCard(icon = Icons.Rounded.AutoAwesome, title = "AI Model Configuration") {
            GeminiModelOption.entries.forEach { model ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.selectedModel == model,
                        onClick = { viewModel.setSelectedModel(model) },
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                    )
                    Column {
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = StarlightWhite
                        )
                        Text(
                            text = model.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = LavenderText
                        )
                    }
                }
            }
        }

        // 4. Secure Backend Settings
        SettingsSectionCard(icon = Icons.Rounded.Security, title = "Backend Connection Settings") {
            BackendMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.backendMode == mode,
                        onClick = { viewModel.setBackendMode(mode) },
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                    )
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StarlightWhite
                    )
                }
            }

            if (settings.backendMode == BackendMode.CUSTOM_BACKEND) {
                OutlinedTextField(
                    value = backendUrlInput,
                    onValueChange = { backendUrlInput = it },
                    label = { Text("Custom Backend URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = NeonViolet.copy(alpha = 0.4f),
                        focusedTextColor = StarlightWhite,
                        unfocusedTextColor = StarlightWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.setCustomBackendUrl(backendUrlInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save URL")
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.setCustomBackendUrl(backendUrlInput)
                            viewModel.testCustomBackendConnection()
                        },
                        border = BorderStroke(1.dp, ElectricCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Ping", color = ElectricCyan)
                    }
                }
                if (!backendTestStatus.isNullOrBlank()) {
                    Text(
                        text = backendTestStatus.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (backendTestStatus!!.startsWith("Connected")) EmeraldMana else AbyssalCrimson
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = EmeraldMana, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Protected by Firebase App Check. Zero API keys in APK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldMana
                    )
                }
            }

            TextButton(onClick = { showSetupGuideDialog = true }) {
                Text("View Gemini API & Backend Setup Guide", color = ElectricCyan)
            }
        }

        // 5. Data & Account Actions
        SettingsSectionCard(icon = Icons.Rounded.DeleteForever, title = "History & Account") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    border = BorderStroke(1.dp, AbyssalCrimson),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clear_history_button")
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = AbyssalCrimson, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear History", color = AbyssalCrimson)
                }

                OutlinedButton(
                    onClick = {
                        signOutUser(credentialManager = credentialManager, scope = scope)
                    },
                    border = BorderStroke(1.dp, NeonViolet),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.Rounded.Logout, contentDescription = null, tint = StarlightWhite, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out", color = StarlightWhite)
                }
            }
        }

        // 6. App & Developer Info
        SettingsSectionCard(icon = Icons.Rounded.Info, title = "App & Developer Info") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GhostAvatarOrb(isThinking = false, size = 50.dp)
                Column {
                    Text(
                        text = "GHOST MALLI AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = StarlightWhite
                    )
                    Text(
                        text = "Version 1.0.0 • Premium Neon Dark Fantasy AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = LavenderText
                    )
                    Text(
                        text = "DEVELOPED BY GHOST MALLI",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = AbyssalPurple,
            titleContentColor = StarlightWhite,
            textContentColor = LavenderText,
            title = { Text("Clear All Chat History?") },
            text = { Text("This will permanently delete all saved conversations and messages from your cloud archive.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAllHistory()
                    }
                ) {
                    Text("Clear All", color = AbyssalCrimson)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = ElectricCyan)
                }
            }
        )
    }

    if (showSetupGuideDialog) {
        AlertDialog(
            onDismissRequest = { showSetupGuideDialog = false },
            containerColor = AbyssalPurple,
            titleContentColor = ElectricCyan,
            textContentColor = StarlightWhite,
            title = { Text("Gemini API & Secure Backend Guide") },
            text = {
                Text(
                    text = "1. Default Managed Mode uses Firebase AI Logic + App Check, so no Gemini API key is ever stored in Android code.\n\n" +
                        "2. Optional Self-Hosted Mode: Run the included backend/server.js Express proxy, set GEMINI_API_KEY in backend/.env on your server, and enter your HTTPS or emulator URL (http://10.0.2.2:3000) in Settings.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showSetupGuideDialog = false }) {
                    Text("Got it", color = ElectricCyan)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    GlassRuneCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan
                )
            }
            content()
        }
    }
}
