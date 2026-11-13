package com.example.data.ai

import android.util.Log
import com.example.data.model.AppReplyLanguage
import com.example.data.model.ChatMessage
import com.example.data.model.DebugAnalysisResult
import com.example.data.model.GeminiModelOption
import com.example.data.model.GeneratedProjectBundle
import com.example.data.model.ProgrammingLanguage
import com.example.data.model.ProjectFileItem
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "FirebaseAiEngine"
private const val AI_TIMEOUT_MS = 60_000L

class FirebaseAiEngine : AiEngine {

    private val textModels = ConcurrentHashMap<String, GenerativeModel>()

    private fun getTextModel(
        modelOption: GeminiModelOption,
        replyLanguage: AppReplyLanguage
    ): GenerativeModel {
        val key = "${modelOption.modelId}_${replyLanguage.name}"
        return textModels.getOrPut(key) {
            Firebase.ai.generativeModel(
                modelName = modelOption.modelId,
                systemInstruction = content {
                    text(GhostMalliPrompts.buildSystemInstruction(replyLanguage))
                }
            )
        }
    }

    private val debugSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "rootCause" to Schema.string(description = "Concise diagnosis of the bug or error"),
                "explanation" to Schema.string(description = "Clear explanation of why the issue occurred and how the fix works"),
                "beforeCode" to Schema.string(description = "Original buggy code snippet"),
                "afterCode" to Schema.string(description = "Complete corrected code snippet"),
                "improvements" to Schema.array(
                    items = Schema.string(),
                    description = "List of key improvements or best-practice tips"
                )
            )
        )
    }

    private val projectSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "projectName" to Schema.string(description = "Short project title"),
                "architectureSummary" to Schema.string(description = "Overview of architecture and tech stack"),
                "setupSteps" to Schema.array(
                    items = Schema.string(),
                    description = "Step-by-step instructions to build and run"
                ),
                "files" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "path" to Schema.string(description = "Relative file path e.g. src/main.py"),
                            "language" to Schema.string(description = "Language name"),
                            "purpose" to Schema.string(description = "Short purpose of this file"),
                            "content" to Schema.string(description = "Complete source code for this file")
                        )
                    ),
                    description = "Source files forming the project structure"
                )
            )
        )
    }

    override fun streamChat(
        history: List<ChatMessage>,
        userMessage: String,
        preferredCodeLanguage: ProgrammingLanguage,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): Flow<String> = flow {
        val model = getTextModel(modelOption, replyLanguage)
        val chatHistory = history.takeLast(16).map { msg ->
            content(role = if (msg.role == "model") "model" else "user") {
                text(msg.content)
            }
        }
        val chat = model.startChat(history = chatHistory)
        val promptWithContext = "[Active Code Language Preference: ${preferredCodeLanguage.displayName}]\n$userMessage"
        val accumulated = StringBuilder()

        try {
            withTimeout(AI_TIMEOUT_MS) {
                chat.sendMessageStream(promptWithContext).collect { chunk ->
                    val delta = chunk.text.orEmpty()
                    if (delta.isNotEmpty()) {
                        accumulated.append(delta)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI model returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat streaming timed out", e)
            throw IllegalStateException("Connection timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chat streaming failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to connect to Gemini AI backend.")
        }
    }

    override fun streamCodeGeneration(
        prompt: String,
        targetLanguage: ProgrammingLanguage,
        includeMultiFile: Boolean,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): Flow<String> = flow {
        val model = getTextModel(modelOption, replyLanguage)
        val fullPrompt = buildString {
            appendLine("Task: Generate complete, production-ready ${targetLanguage.displayName} code.")
            if (includeMultiFile) {
                appendLine("Include a clean project folder structure and multiple well-organized source files with file path headers.")
            }
            appendLine("After the code block(s), explain how the generated code works clearly.")
            appendLine()
            appendLine("User Requirement:")
            appendLine(prompt)
        }
        streamSinglePrompt(model, fullPrompt) { emit(it) }
    }

    override fun streamCodeExplanation(
        code: String,
        language: ProgrammingLanguage,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): Flow<String> = flow {
        val effectiveReplyLanguage = if (replyLanguage == AppReplyLanguage.ENGLISH) {
            AppReplyLanguage.ENGLISH
        } else {
            AppReplyLanguage.SINHALA
        }
        val model = getTextModel(modelOption, effectiveReplyLanguage)
        val fullPrompt = buildString {
            appendLine("Explain the following ${language.displayName} code step-by-step and line-by-line.")
            if (effectiveReplyLanguage != AppReplyLanguage.ENGLISH) {
                appendLine("Write the explanation in simple, beginner-friendly Sinhala (සිංහල) with clear examples.")
            }
            appendLine("Cover: 1) Overall purpose, 2) Functions & variables, 3) Algorithm / logic flow, 4) Beginner-friendly example.")
            appendLine()
            appendLine("```${language.extension}")
            appendLine(code)
            appendLine("```")
        }
        streamSinglePrompt(model, fullPrompt) { emit(it) }
    }

    override suspend fun debugCode(
        code: String,
        errorDetails: String,
        language: ProgrammingLanguage,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): DebugAnalysisResult {
        val model by lazy {
            Firebase.ai.generativeModel(
                modelName = modelOption.modelId,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = debugSchema
                },
                systemInstruction = content {
                    text(GhostMalliPrompts.buildSystemInstruction(replyLanguage))
                }
            )
        }

        val prompt = buildString {
            appendLine("Analyze and fix the following ${language.displayName} code.")
            if (errorDetails.isNotBlank()) {
                appendLine("Error / Symptom:")
                appendLine(errorDetails)
            }
            appendLine("Code:")
            appendLine(code)
        }

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                model.generateContent(prompt)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("Debugger returned an empty response.")
            val json = JSONObject(jsonText)
            val improvementsArray = json.optJSONArray("improvements")
            val improvements = buildList {
                if (improvementsArray != null) {
                    for (i in 0 until improvementsArray.length()) {
                        add(improvementsArray.getString(i))
                    }
                }
            }
            DebugAnalysisResult(
                rootCause = json.optString("rootCause", "Unknown issue"),
                explanation = json.optString("explanation", ""),
                beforeCode = json.optString("beforeCode", code),
                afterCode = json.optString("afterCode", code),
                improvements = improvements
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Debug analysis timed out", e)
            throw IllegalStateException("Debug analysis timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Debug analysis failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to debug code.")
        }
    }

    override suspend fun createProject(
        projectType: String,
        prompt: String,
        primaryLanguage: ProgrammingLanguage,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): GeneratedProjectBundle {
        val model by lazy {
            Firebase.ai.generativeModel(
                modelName = modelOption.modelId,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = projectSchema
                },
                systemInstruction = content {
                    text(GhostMalliPrompts.buildSystemInstruction(replyLanguage))
                }
            )
        }

        val fullPrompt = """
            Create a complete, well-structured $projectType project primarily using ${primaryLanguage.displayName}.
            Generate 4 to 6 essential, complete, runnable source and configuration files (no placeholders or TODO stubs).
            User specification: $prompt
        """.trimIndent()

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                model.generateContent(fullPrompt)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("Project Creator returned an empty response.")
            val json = JSONObject(jsonText)
            val stepsArray = json.optJSONArray("setupSteps")
            val setupSteps = buildList {
                if (stepsArray != null) {
                    for (i in 0 until stepsArray.length()) {
                        add(stepsArray.getString(i))
                    }
                }
            }
            val filesArray = json.optJSONArray("files")
            val files = buildList {
                if (filesArray != null) {
                    for (i in 0 until filesArray.length()) {
                        val obj = filesArray.getJSONObject(i)
                        add(
                            ProjectFileItem(
                                path = obj.optString("path", "file_$i.${primaryLanguage.extension}"),
                                language = obj.optString("language", primaryLanguage.displayName),
                                purpose = obj.optString("purpose", ""),
                                content = obj.optString("content", "")
                            )
                        )
                    }
                }
            }
            GeneratedProjectBundle(
                projectName = json.optString("projectName", "GhostMalliProject"),
                architectureSummary = json.optString("architectureSummary", ""),
                setupSteps = setupSteps,
                files = files
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Project creation timed out", e)
            throw IllegalStateException("Project generation timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Project creation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to create project.")
        }
    }

    private suspend fun streamSinglePrompt(
        model: GenerativeModel,
        prompt: String,
        onAccumulated: suspend (String) -> Unit
    ) {
        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                model.generateContentStream(prompt).collect { chunk ->
                    val delta = chunk.text.orEmpty()
                    if (delta.isNotEmpty()) {
                        accumulated.append(delta)
                        onAccumulated(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Model returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Generation timed out", e)
            throw IllegalStateException("Generation timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "AI backend request failed.")
        }
    }
}
