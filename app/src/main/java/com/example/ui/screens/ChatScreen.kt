package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AssistantMode
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.ProgrammingLanguage
import com.example.ui.components.ArcaneTypingIndicator
import com.example.ui.components.GhostAvatarOrb
import com.example.ui.components.GlassRuneCard
import com.example.ui.components.MarkdownMessageContent
import com.example.ui.theme.AbyssalCrimson
import com.example.ui.theme.AbyssalPurple
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LavenderText
import com.example.ui.theme.MutedRune
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ShadowSurface
import com.example.ui.theme.StarlightWhite
import com.example.ui.viewmodel.AiStreamState
import com.example.ui.viewmodel.GhostMalliViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: GhostMalliViewModel,
    onOpenCodeInEditor: (String, ProgrammingLanguage) -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConvId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val streamState by viewModel.chatStreamState.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedCodeLanguage.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeChatMode.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val clipboard = LocalClipboardManager.current
    var inputText by rememberSaveable { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isGenerating = streamState is AiStreamState.Loading || streamState is AiStreamState.Streaming

    LaunchedEffect(messages.size, streamState) {
        val totalItems = messages.size + if (streamState !is AiStreamState.Idle) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Header Bar
        GlassRuneCard(
            cornerRadius = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GhostAvatarOrb(isThinking = isGenerating, size = 44.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GHOST MALLI AI",
                        style = MaterialTheme.typography.titleLarge,
                        color = StarlightWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${settings.selectedModel.displayName} • ${selectedLanguage.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { showHistorySheet = true },
                    modifier = Modifier.testTag("chat_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = "Chat history",
                        tint = StarlightWhite
                    )
                }
                IconButton(
                    onClick = { viewModel.startNewChat() },
                    modifier = Modifier.testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddComment,
                        contentDescription = "New chat",
                        tint = ElectricCyan
                    )
                }
            }
        }

        // Language quick selector strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProgrammingLanguage.entries.forEach { lang ->
                val selected = lang == selectedLanguage
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.selectCodeLanguage(lang) },
                    label = {
                        Text(
                            text = lang.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonViolet.copy(alpha = 0.25f),
                        selectedLabelColor = ElectricCyan,
                        containerColor = ShadowSurface,
                        labelColor = LavenderText
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = NeonViolet.copy(alpha = 0.25f),
                        selectedBorderColor = ElectricCyan
                    )
                )
            }
        }

        // Chat Feed
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && streamState is AiStreamState.Idle) {
                EmptyChatHero(
                    selectedLanguage = selectedLanguage,
                    onStarterPrompt = { starter ->
                        viewModel.sendChatMessage(starter)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isLastModel = msg == messages.lastOrNull() && msg.role == "model"
                        ChatBubbleItem(
                            message = msg,
                            fallbackLanguage = selectedLanguage,
                            showRegenerate = isLastModel && !isGenerating,
                            onCopyResponse = {
                                clipboard.setText(AnnotatedString(msg.content))
                                onShowSnackbar("Response copied to clipboard")
                            },
                            onRegenerate = { viewModel.regenerateLastResponse() },
                            onCopyCode = { code ->
                                clipboard.setText(AnnotatedString(code))
                                onShowSnackbar("Code copied")
                            },
                            onOpenInEditor = onOpenCodeInEditor
                        )
                    }

                    when (val state = streamState) {
                        is AiStreamState.Loading -> {
                            item(key = "streaming_loading") {
                                StreamingAiBubble(
                                    text = "",
                                    isLoadingOnly = true,
                                    fallbackLanguage = selectedLanguage,
                                    onCopyCode = {},
                                    onOpenInEditor = onOpenCodeInEditor
                                )
                            }
                        }
                        is AiStreamState.Streaming -> {
                            item(key = "streaming_active") {
                                StreamingAiBubble(
                                    text = state.text,
                                    isLoadingOnly = false,
                                    fallbackLanguage = selectedLanguage,
                                    onCopyCode = { code ->
                                        clipboard.setText(AnnotatedString(code))
                                        onShowSnackbar("Code copied")
                                    },
                                    onOpenInEditor = onOpenCodeInEditor
                                )
                            }
                        }
                        is AiStreamState.Error -> {
                            item(key = "stream_error") {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = AbyssalCrimson.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, AbyssalCrimson),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ErrorOutline,
                                            contentDescription = null,
                                            tint = AbyssalCrimson
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Connection / AI Error",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = StarlightWhite
                                            )
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LavenderText
                                            )
                                        }
                                        TextButton(onClick = { viewModel.regenerateLastResponse() }) {
                                            Text("Retry", color = ElectricCyan)
                                        }
                                    }
                                }
                            }
                        }
                        is AiStreamState.Idle -> Unit
                    }
                }
            }
        }

        // Stop Generation Bar
        AnimatedVisibility(visible = isGenerating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    onClick = { viewModel.stopChatGeneration() },
                    shape = RoundedCornerShape(20.dp),
                    color = AbyssalCrimson.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, AbyssalCrimson),
                    modifier = Modifier.testTag("stop_generation_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.StopCircle,
                            contentDescription = "Stop generation",
                            tint = AbyssalCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Stop Generation",
                            style = MaterialTheme.typography.labelMedium,
                            color = StarlightWhite
                        )
                    }
                }
            }
        }

        // Multiline Input Composer
        GlassRuneCard(
            cornerRadius = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Ask in Sinhala (සිංහල) or English...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedRune
                        )
                    },
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = StarlightWhite,
                        unfocusedTextColor = StarlightWhite,
                        cursorColor = ElectricCyan
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    onClick = {
                        if (inputText.isNotBlank() && !isGenerating) {
                            val prompt = inputText
                            inputText = ""
                            viewModel.sendChatMessage(prompt)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    shape = CircleShape,
                    color = if (inputText.isNotBlank() && !isGenerating) NeonViolet else MutedRune.copy(alpha = 0.25f),
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .size(48.dp)
                        .testTag("send_message_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send message",
                            tint = StarlightWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState,
            containerColor = AbyssalPurple,
            contentColor = StarlightWhite
        ) {
            ConversationHistorySheet(
                conversations = conversations,
                activeConversationId = activeConvId,
                onSelect = { conv ->
                    viewModel.selectConversation(conv.id)
                    showHistorySheet = false
                },
                onDelete = { conv ->
                    viewModel.deleteConversation(conv.id)
                },
                onNewChat = {
                    viewModel.startNewChat()
                    showHistorySheet = false
                }
            )
        }
    }
}

@Composable
private fun EmptyChatHero(
    selectedLanguage: ProgrammingLanguage,
    onStarterPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(18.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_dark_fantasy_banner),
                contentDescription = "Dark Fantasy Nexus",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x4406030D), Color(0xEE06030D))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = "SHADOW CODING NEXUS",
                    style = MaterialTheme.typography.titleLarge,
                    color = StarlightWhite
                )
                Text(
                    text = "සිංහල සහ English භාෂා දෙකෙන්ම programming ප්‍රශ්න අහන්න.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricCyan
                )
            }
        }

        Text(
            text = "QUICK SUMMON PROMPTS",
            style = MaterialTheme.typography.labelMedium,
            color = LavenderText
        )

        val prompts = listOf(
            "Build a clean ${selectedLanguage.displayName} REST API client with error handling",
            "Kotlin Jetpack Compose Login screen එකක් සිංහලෙන් පැහැදිලි කරලා හදලා දෙන්න",
            "Explain recursion and binary search in simple Sinhala with a Python example",
            "Design a responsive dark neon CSS card component with glassmorphism"
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(prompts) { prompt ->
                GlassRuneCard(
                    cornerRadius = 14.dp,
                    glowBorder = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStarterPrompt(prompt) }
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StarlightWhite,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    fallbackLanguage: ProgrammingLanguage,
    showRegenerate: Boolean,
    onCopyResponse: () -> Unit,
    onRegenerate: () -> Unit,
    onCopyCode: (String) -> Unit,
    onOpenInEditor: (String, ProgrammingLanguage) -> Unit
) {
    val isUser = message.role == "user"
    val timeText = remember(message.createdAt) {
        val date = message.createdAt?.toDate() ?: Date()
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            GhostAvatarOrb(isThinking = false, size = 34.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 520.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 18.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                ),
                color = if (isUser) Color(0xFF28144A) else Color(0xCC130C24),
                border = BorderStroke(
                    1.dp,
                    if (isUser) NeonViolet.copy(alpha = 0.65f) else ElectricCyan.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (isUser) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = StarlightWhite
                        )
                    } else {
                        MarkdownMessageContent(
                            markdown = message.content,
                            fallbackLanguage = fallbackLanguage,
                            onCopyCode = onCopyCode,
                            onOpenInEditor = onOpenInEditor
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedRune
                )
                if (!isUser) {
                    IconButton(
                        onClick = onCopyResponse,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy response",
                            tint = LavenderText,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    if (showRegenerate) {
                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("regenerate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Regenerate response",
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = NeonViolet.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, NeonViolet),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = "User Avatar",
                        tint = StarlightWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingAiBubble(
    text: String,
    isLoadingOnly: Boolean,
    fallbackLanguage: ProgrammingLanguage,
    onCopyCode: (String) -> Unit,
    onOpenInEditor: (String, ProgrammingLanguage) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        GhostAvatarOrb(isThinking = true, size = 34.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = Color(0xCC130C24),
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.65f)),
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArcaneTypingIndicator(
                    label = if (isLoadingOnly) "Summoning Ghost Malli AI..." else "Streaming arcane response..."
                )
                if (text.isNotBlank()) {
                    MarkdownMessageContent(
                        markdown = text,
                        fallbackLanguage = fallbackLanguage,
                        onCopyCode = onCopyCode,
                        onOpenInEditor = onOpenInEditor
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationHistorySheet(
    conversations: List<Conversation>,
    activeConversationId: String?,
    onSelect: (Conversation) -> Unit,
    onDelete: (Conversation) -> Unit,
    onNewChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONVERSATION ARCHIVES",
                style = MaterialTheme.typography.titleLarge,
                color = StarlightWhite,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onNewChat) {
                Icon(Icons.Rounded.AddComment, contentDescription = null, tint = ElectricCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Chat", color = ElectricCyan)
            }
        }

        if (conversations.isEmpty()) {
            Text(
                text = "No saved conversations yet. Send a message to begin!",
                style = MaterialTheme.typography.bodyMedium,
                color = LavenderText,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                items(conversations, key = { it.id }) { conv ->
                    val isSelected = conv.id == activeConversationId
                    GlassRuneCard(
                        cornerRadius = 14.dp,
                        glowBorder = isSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(conv) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conv.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) ElectricCyan else StarlightWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${conv.language} • ${conv.messageCount} msgs • ${conv.lastMessagePreview}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LavenderText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onDelete(conv) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete conversation",
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
