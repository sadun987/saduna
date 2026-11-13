package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiEngine
import com.example.data.ai.CustomBackendAiEngine
import com.example.data.ai.FirebaseAiEngine
import com.example.data.model.AppReplyLanguage
import com.example.data.model.AssistantMode
import com.example.data.model.BackendMode
import com.example.data.model.ChatMessage
import com.example.data.model.CodeSnippet
import com.example.data.model.Conversation
import com.example.data.model.DebugAnalysisResult
import com.example.data.model.GeminiModelOption
import com.example.data.model.GeneratedProjectBundle
import com.example.data.model.ProgrammingLanguage
import com.example.data.prefs.FantasyAccentTheme
import com.example.data.prefs.SettingsRepository
import com.example.data.prefs.UserSettings
import com.example.data.repository.GhostMalliRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiStreamState {
    data object Idle : AiStreamState
    data object Loading : AiStreamState
    data class Streaming(val text: String) : AiStreamState
    data class Error(val message: String) : AiStreamState
}

@OptIn(ExperimentalCoroutinesApi::class)
class GhostMalliViewModel(
    private val repository: GhostMalliRepository,
    private val settingsRepository: SettingsRepository,
    private val firebaseAiEngine: FirebaseAiEngine,
    private val customBackendEngine: CustomBackendAiEngine,
    val currentUserId: String
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserSettings()
        )

    private val _firestoreError = MutableStateFlow<String?>(null)
    val firestoreError: StateFlow<String?> = _firestoreError.asStateFlow()

    val conversations: StateFlow<List<Conversation>> = repository.observeConversations()
        .catch { error ->
            val firestoreErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreErr == null && error is CancellationException) throw error
            val root = firestoreErr ?: error
            Log.w(TAG, "Error observing conversations", root)
            _firestoreError.value = root.localizedMessage ?: "Failed to load chat history."
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val snippets: StateFlow<List<CodeSnippet>> = repository.observeSnippets()
        .catch { error ->
            val firestoreErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreErr == null && error is CancellationException) throw error
            val root = firestoreErr ?: error
            Log.w(TAG, "Error observing snippets", root)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeMessages(convId).catch { error ->
                    val firestoreErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing messages", firestoreErr ?: error)
                    emit(emptyList())
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _chatStreamState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val chatStreamState: StateFlow<AiStreamState> = _chatStreamState.asStateFlow()

    private val _selectedCodeLanguage = MutableStateFlow(ProgrammingLanguage.KOTLIN)
    val selectedCodeLanguage: StateFlow<ProgrammingLanguage> = _selectedCodeLanguage.asStateFlow()

    private val _activeChatMode = MutableStateFlow(AssistantMode.CHAT)
    val activeChatMode: StateFlow<AssistantMode> = _activeChatMode.asStateFlow()

    // Studio Tool States
    private val _forgeState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val forgeState: StateFlow<AiStreamState> = _forgeState.asStateFlow()

    private val _explainState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val explainState: StateFlow<AiStreamState> = _explainState.asStateFlow()

    private val _debugLoading = MutableStateFlow(false)
    val debugLoading: StateFlow<Boolean> = _debugLoading.asStateFlow()
    private val _debugResult = MutableStateFlow<DebugAnalysisResult?>(null)
    val debugResult: StateFlow<DebugAnalysisResult?> = _debugResult.asStateFlow()
    private val _debugError = MutableStateFlow<String?>(null)
    val debugError: StateFlow<String?> = _debugError.asStateFlow()

    private val _projectLoading = MutableStateFlow(false)
    val projectLoading: StateFlow<Boolean> = _projectLoading.asStateFlow()
    private val _projectBundle = MutableStateFlow<GeneratedProjectBundle?>(null)
    val projectBundle: StateFlow<GeneratedProjectBundle?> = _projectBundle.asStateFlow()
    private val _projectError = MutableStateFlow<String?>(null)
    val projectError: StateFlow<String?> = _projectError.asStateFlow()

    // Interactive Code Editor States
    private val _editorCode = MutableStateFlow(
        "// Welcome to Ghost Malli AI Code Editor\nfun summonShadowCore() {\n    println(\"Ghost Malli AI Ready\")\n}"
    )
    val editorCode: StateFlow<String> = _editorCode.asStateFlow()
    private val _editorLanguage = MutableStateFlow(ProgrammingLanguage.KOTLIN)
    val editorLanguage: StateFlow<ProgrammingLanguage> = _editorLanguage.asStateFlow()
    private val _editorFileName = MutableStateFlow("ShadowCore.kt")
    val editorFileName: StateFlow<String> = _editorFileName.asStateFlow()
    private val _editorTitle = MutableStateFlow("ShadowCore")
    val editorTitle: StateFlow<String> = _editorTitle.asStateFlow()
    private val _editorSnippetId = MutableStateFlow<String?>(null)
    val editorSnippetId: StateFlow<String?> = _editorSnippetId.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _backendTestStatus = MutableStateFlow<String?>(null)
    val backendTestStatus: StateFlow<String?> = _backendTestStatus.asStateFlow()

    private var activeChatJob: Job? = null
    private var activeToolJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { userSettings ->
                customBackendEngine.updateBaseUrl(userSettings.customBackendUrl)
            }
        }
    }

    private fun activeEngine(): AiEngine =
        if (settings.value.backendMode == BackendMode.CUSTOM_BACKEND) {
            customBackendEngine
        } else {
            firebaseAiEngine
        }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun selectCodeLanguage(language: ProgrammingLanguage) {
        _selectedCodeLanguage.value = language
    }

    fun selectChatMode(mode: AssistantMode) {
        _activeChatMode.value = mode
    }

    fun startNewChat() {
        activeChatJob?.cancel()
        _chatStreamState.value = AiStreamState.Idle
        _activeConversationId.value = null
    }

    fun selectConversation(conversationId: String) {
        activeChatJob?.cancel()
        _chatStreamState.value = AiStreamState.Idle
        _activeConversationId.value = conversationId
        conversations.value.firstOrNull { it.id == conversationId }?.let { conv ->
            _selectedCodeLanguage.value = ProgrammingLanguage.fromName(conv.language)
            _activeChatMode.value = AssistantMode.fromWireName(conv.mode)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            val res = repository.deleteConversation(conversationId)
            if (res.isSuccess) {
                if (_activeConversationId.value == conversationId) {
                    _activeConversationId.value = null
                    _chatStreamState.value = AiStreamState.Idle
                }
                _statusMessage.value = "Conversation deleted."
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete conversation."
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            activeChatJob?.cancel()
            val res = repository.clearAllConversations()
            if (res.isSuccess) {
                _activeConversationId.value = null
                _chatStreamState.value = AiStreamState.Idle
                _statusMessage.value = "All chat history cleared."
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to clear chat history."
            }
        }
    }

    fun sendChatMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) return

        activeChatJob?.cancel()
        activeChatJob = viewModelScope.launch {
            val currentSettings = settings.value
            val lang = _selectedCodeLanguage.value
            val mode = _activeChatMode.value
            val existingMessages = messages.value

            _chatStreamState.value = AiStreamState.Loading

            val convId = _activeConversationId.value ?: run {
                val title = prompt.replace('\n', ' ').take(48)
                val created = repository.createConversation(
                    title = title,
                    mode = mode,
                    language = lang.displayName,
                    modelName = currentSettings.selectedModel.modelId
                )
                if (created.isFailure) {
                    val errMsg = created.exceptionOrNull()?.localizedMessage
                        ?: "Unable to initialize conversation in cloud database."
                    _chatStreamState.value = AiStreamState.Error(errMsg)
                    return@launch
                }
                created.getOrThrow().also { _activeConversationId.value = it }
            }

            val nextOrder = (existingMessages.maxOfOrNull { it.sequenceOrder } ?: -1L) + 1L
            val userSaveResult = repository.appendMessage(
                conversationId = convId,
                role = "user",
                content = prompt,
                language = lang.displayName,
                mode = mode,
                sequenceOrder = nextOrder,
                newTitleIfFirst = if (existingMessages.isEmpty()) prompt.take(48) else null
            )
            if (userSaveResult.isFailure) {
                _chatStreamState.value = AiStreamState.Error(
                    userSaveResult.exceptionOrNull()?.localizedMessage ?: "Failed to save message."
                )
                return@launch
            }

            streamAiReplyIntoConversation(
                convId = convId,
                history = existingMessages,
                userPrompt = prompt,
                lang = lang,
                mode = mode,
                sequenceOrder = nextOrder + 1L,
                currentSettings = currentSettings
            )
        }
    }

    fun regenerateLastResponse() {
        val convId = _activeConversationId.value ?: return
        val currentMsgs = messages.value
        val lastUserMsg = currentMsgs.lastOrNull { it.role == "user" } ?: return

        activeChatJob?.cancel()
        activeChatJob = viewModelScope.launch {
            _chatStreamState.value = AiStreamState.Loading
            val trailingModel = currentMsgs.lastOrNull()
            val historyBeforeLastUser = if (trailingModel != null && trailingModel.role == "model") {
                repository.deleteMessage(convId, trailingModel.id)
                currentMsgs.dropLast(1).dropLast(1)
            } else {
                currentMsgs.dropLast(1)
            }
            val seq = lastUserMsg.sequenceOrder + 1L
            streamAiReplyIntoConversation(
                convId = convId,
                history = historyBeforeLastUser,
                userPrompt = lastUserMsg.content,
                lang = _selectedCodeLanguage.value,
                mode = _activeChatMode.value,
                sequenceOrder = seq,
                currentSettings = settings.value
            )
        }
    }

    private suspend fun streamAiReplyIntoConversation(
        convId: String,
        history: List<ChatMessage>,
        userPrompt: String,
        lang: ProgrammingLanguage,
        mode: AssistantMode,
        sequenceOrder: Long,
        currentSettings: UserSettings
    ) {
        var latestAccumulated = ""
        try {
            activeEngine().streamChat(
                history = history,
                userMessage = userPrompt,
                preferredCodeLanguage = lang,
                replyLanguage = currentSettings.replyLanguage,
                modelOption = currentSettings.selectedModel
            ).collect { partial ->
                latestAccumulated = partial
                _chatStreamState.value = AiStreamState.Streaming(partial)
            }

            if (latestAccumulated.isNotBlank()) {
                repository.appendMessage(
                    conversationId = convId,
                    role = "model",
                    content = latestAccumulated,
                    language = lang.displayName,
                    mode = mode,
                    sequenceOrder = sequenceOrder
                )
                _chatStreamState.value = AiStreamState.Idle
            } else {
                _chatStreamState.value = AiStreamState.Error("AI returned an empty response.")
            }
        } catch (e: CancellationException) {
            // If user tapped Stop mid-stream and we already received partial tokens, preserve them
            if (latestAccumulated.isNotBlank()) {
                viewModelScope.launch {
                    repository.appendMessage(
                        conversationId = convId,
                        role = "model",
                        content = "$latestAccumulated\n\n_[Generation stopped]_",
                        language = lang.displayName,
                        mode = mode,
                        sequenceOrder = sequenceOrder
                    )
                }
            }
            _chatStreamState.value = AiStreamState.Idle
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI chat error", e)
            _chatStreamState.value = AiStreamState.Error(
                e.localizedMessage ?: "AI backend connection error."
            )
        }
    }

    fun stopChatGeneration() {
        activeChatJob?.cancel()
        _chatStreamState.value = AiStreamState.Idle
    }

    // --- Studio Tools ---

    fun generateCode(prompt: String, language: ProgrammingLanguage, multiFile: Boolean) {
        if (prompt.isBlank()) return
        activeToolJob?.cancel()
        activeToolJob = viewModelScope.launch {
            _forgeState.value = AiStreamState.Loading
            val currentSettings = settings.value
            try {
                activeEngine().streamCodeGeneration(
                    prompt = prompt.trim(),
                    targetLanguage = language,
                    includeMultiFile = multiFile,
                    replyLanguage = currentSettings.replyLanguage,
                    modelOption = currentSettings.selectedModel
                ).collect { text ->
                    _forgeState.value = AiStreamState.Streaming(text)
                }
            } catch (e: CancellationException) {
                _forgeState.value = AiStreamState.Idle
                throw e
            } catch (e: Exception) {
                _forgeState.value = AiStreamState.Error(
                    e.localizedMessage ?: "Code generation failed."
                )
            }
        }
    }

    fun debugCode(code: String, errorDetails: String, language: ProgrammingLanguage) {
        if (code.isBlank()) return
        activeToolJob?.cancel()
        activeToolJob = viewModelScope.launch {
            _debugLoading.value = true
            _debugError.value = null
            val currentSettings = settings.value
            try {
                val res = activeEngine().debugCode(
                    code = code,
                    errorDetails = errorDetails.trim(),
                    language = language,
                    replyLanguage = currentSettings.replyLanguage,
                    modelOption = currentSettings.selectedModel
                )
                _debugResult.value = res
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _debugError.value = e.localizedMessage ?: "Debug analysis failed."
            } finally {
                _debugLoading.value = false
            }
        }
    }

    fun explainCodeInSinhala(code: String, language: ProgrammingLanguage) {
        if (code.isBlank()) return
        activeToolJob?.cancel()
        activeToolJob = viewModelScope.launch {
            _explainState.value = AiStreamState.Loading
            val currentSettings = settings.value
            try {
                activeEngine().streamCodeExplanation(
                    code = code,
                    language = language,
                    replyLanguage = currentSettings.replyLanguage,
                    modelOption = currentSettings.selectedModel
                ).collect { text ->
                    _explainState.value = AiStreamState.Streaming(text)
                }
            } catch (e: CancellationException) {
                _explainState.value = AiStreamState.Idle
                throw e
            } catch (e: Exception) {
                _explainState.value = AiStreamState.Error(
                    e.localizedMessage ?: "Explanation failed."
                )
            }
        }
    }

    fun createProject(projectType: String, prompt: String, language: ProgrammingLanguage) {
        if (prompt.isBlank()) return
        activeToolJob?.cancel()
        activeToolJob = viewModelScope.launch {
            _projectLoading.value = true
            _projectError.value = null
            val currentSettings = settings.value
            try {
                val bundle = activeEngine().createProject(
                    projectType = projectType,
                    prompt = prompt.trim(),
                    primaryLanguage = language,
                    replyLanguage = currentSettings.replyLanguage,
                    modelOption = currentSettings.selectedModel
                )
                _projectBundle.value = bundle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _projectError.value = e.localizedMessage ?: "Project generation failed."
            } finally {
                _projectLoading.value = false
            }
        }
    }

    fun stopToolGeneration() {
        activeToolJob?.cancel()
        _forgeState.value = AiStreamState.Idle
        _explainState.value = AiStreamState.Idle
        _debugLoading.value = false
        _projectLoading.value = false
    }

    // --- Interactive Code Editor ---

    fun updateEditorCode(code: String) {
        _editorCode.value = code
    }

    fun updateEditorLanguage(language: ProgrammingLanguage) {
        _editorLanguage.value = language
        val currentName = _editorFileName.value
        val base = currentName.substringBeforeLast('.', currentName).ifBlank { "script" }
        _editorFileName.value = "$base.${language.extension}"
    }

    fun updateEditorFileName(name: String) {
        _editorFileName.value = name
    }

    fun updateEditorTitle(title: String) {
        _editorTitle.value = title
    }

    fun openCodeInEditor(
        code: String,
        language: ProgrammingLanguage,
        title: String = "Generated ${language.displayName}",
        fileName: String = "main.${language.extension}"
    ) {
        _editorSnippetId.value = null
        _editorCode.value = code
        _editorLanguage.value = language
        _editorTitle.value = title
        _editorFileName.value = fileName
    }

    fun loadSnippetIntoEditor(snippet: CodeSnippet) {
        _editorSnippetId.value = snippet.id
        _editorCode.value = snippet.code
        _editorLanguage.value = ProgrammingLanguage.fromName(snippet.language)
        _editorTitle.value = snippet.title
        _editorFileName.value = snippet.fileName
    }

    fun clearEditor() {
        _editorSnippetId.value = null
        _editorCode.value = ""
        _editorTitle.value = "Untitled"
        _editorFileName.value = "script.${_editorLanguage.value.extension}"
    }

    fun saveEditorSnippet() {
        viewModelScope.launch {
            val res = repository.saveSnippet(
                snippetId = _editorSnippetId.value,
                title = _editorTitle.value.ifBlank { _editorFileName.value },
                fileName = _editorFileName.value,
                language = _editorLanguage.value.displayName,
                code = _editorCode.value,
                summary = "Saved from Ghost Malli AI Code Editor"
            )
            if (res.isSuccess) {
                _editorSnippetId.value = res.getOrThrow()
                _statusMessage.value = "Saved to Cloud Vault!"
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save snippet."
            }
        }
    }

    fun deleteSnippet(snippetId: String) {
        viewModelScope.launch {
            val res = repository.deleteSnippet(snippetId)
            if (res.isSuccess) {
                if (_editorSnippetId.value == snippetId) {
                    _editorSnippetId.value = null
                }
                _statusMessage.value = "Snippet deleted."
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Delete failed."
            }
        }
    }

    // --- Settings ---

    fun setReplyLanguage(language: AppReplyLanguage) {
        viewModelScope.launch { settingsRepository.setReplyLanguage(language) }
    }

    fun setSelectedModel(model: GeminiModelOption) {
        viewModelScope.launch { settingsRepository.setSelectedModel(model) }
    }

    fun setDefaultCodeLanguage(language: ProgrammingLanguage) {
        viewModelScope.launch { settingsRepository.setDefaultCodeLanguage(language) }
    }

    fun setBackendMode(mode: BackendMode) {
        viewModelScope.launch { settingsRepository.setBackendMode(mode) }
    }

    fun setCustomBackendUrl(url: String) {
        viewModelScope.launch { settingsRepository.setCustomBackendUrl(url) }
    }

    fun setParticlesEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setParticlesEnabled(enabled) }
    }

    fun setAccentTheme(theme: FantasyAccentTheme) {
        viewModelScope.launch { settingsRepository.setAccentTheme(theme) }
    }

    fun testCustomBackendConnection() {
        viewModelScope.launch {
            _backendTestStatus.value = "Pinging backend..."
            val result = customBackendEngine.testConnection()
            _backendTestStatus.value = result.fold(
                onSuccess = { it },
                onFailure = { "Connection failed: ${it.localizedMessage ?: "Unreachable"}" }
            )
        }
    }

    private companion object {
        const val TAG = "GhostMalliVM"
    }
}
