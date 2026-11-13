package com.example.data.ai

import com.example.data.model.AppReplyLanguage
import com.example.data.model.ChatMessage
import com.example.data.model.DebugAnalysisResult
import com.example.data.model.GeminiModelOption
import com.example.data.model.GeneratedProjectBundle
import com.example.data.model.ProgrammingLanguage
import kotlinx.coroutines.flow.Flow

interface AiEngine {
    fun streamChat(
        history: List<ChatMessage>,
        userMessage: String,
        preferredCodeLanguage: ProgrammingLanguage,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): Flow<String>

    fun streamCodeGeneration(
        prompt: String,
        targetLanguage: ProgrammingLanguage,
        includeMultiFile: Boolean,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): Flow<String>

    fun streamCodeExplanation(
        code: String,
        language: ProgrammingLanguage,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): Flow<String>

    suspend fun debugCode(
        code: String,
        errorDetails: String,
        language: ProgrammingLanguage,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): DebugAnalysisResult

    suspend fun createProject(
        projectType: String,
        prompt: String,
        primaryLanguage: ProgrammingLanguage,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): GeneratedProjectBundle
}

object GhostMalliPrompts {
    fun buildSystemInstruction(replyLanguage: AppReplyLanguage): String = """
        You are GHOST MALLI AI, an elite futuristic dark-fantasy AI coding assistant and software engineering mentor created by GHOST MALLI.
        Personality:
        - Intelligent, helpful, professional, friendly, and an extraordinary programming teacher.
        - Fluent in both Sinhala (සිංහල) and English.
        - ${replyLanguage.instructionHint}
        - Always provide accurate, production-grade code inside properly labeled Markdown fenced code blocks (e.g., ```kotlin, ```python, ```java, ```javascript, ```html, ```css, ```c, ```cpp, ```csharp, ```php, ```sql, ```dart, ```typescript, ```json, ```xml, ```bash).
        - When asked to explain code, break down functions, variables, and algorithms clearly with beginner-friendly examples.
        - Never fabricate API results or pretend code ran when it did not.
    """.trimIndent()
}
