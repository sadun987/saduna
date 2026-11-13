package com.example.data.ai

import android.util.Log
import com.example.data.model.AppReplyLanguage
import com.example.data.model.ChatMessage
import com.example.data.model.DebugAnalysisResult
import com.example.data.model.GeminiModelOption
import com.example.data.model.GeneratedProjectBundle
import com.example.data.model.ProgrammingLanguage
import com.example.data.model.ProjectFileItem
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

private const val TAG = "CustomBackendAiEngine"
private const val REQUEST_TIMEOUT_MS = 60_000L

@JsonClass(generateAdapter = true)
data class BackendHistoryMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class BackendChatRequest(
    val message: String,
    val history: List<BackendHistoryMessage>,
    val model: String,
    val language: String,
    val replyLanguage: String,
    val mode: String
)

@JsonClass(generateAdapter = true)
data class BackendChatResponse(
    val reply: String? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class BackendHealthResponse(
    val status: String? = null,
    val service: String? = null
)

interface GhostBackendApi {
    @GET("health")
    suspend fun health(): BackendHealthResponse

    @POST("api/chat")
    suspend fun chat(@Body request: BackendChatRequest): BackendChatResponse
}

class CustomBackendAiEngine(
    initialBaseUrl: String = "http://10.0.2.2:3000"
) : AiEngine {

    @Volatile
    private var baseUrl: String = initialBaseUrl

    fun updateBaseUrl(url: String) {
        baseUrl = url.trim()
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun createApi(): GhostBackendApi {
        val raw = baseUrl.trim().ifBlank {
            throw IllegalStateException("Custom Backend URL is empty. Configure it in Settings.")
        }
        val normalized = if (raw.endsWith("/")) raw else "$raw/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GhostBackendApi::class.java)
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val res = withTimeout(10_000L) { createApi().health() }
            Result.success("Connected: ${res.service ?: "Ghost Malli Backend"} (${res.status ?: "ok"})")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun streamChat(
        history: List<ChatMessage>,
        userMessage: String,
        preferredCodeLanguage: ProgrammingLanguage,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): Flow<String> = flow {
        val response = executeBackendCall(
            message = userMessage,
            history = history.takeLast(16).map { BackendHistoryMessage(it.role, it.content) },
            modelOption = modelOption,
            language = preferredCodeLanguage,
            replyLanguage = replyLanguage,
            mode = "chat"
        )
        emit(response)
    }

    override fun streamCodeGeneration(
        prompt: String,
        targetLanguage: ProgrammingLanguage,
        includeMultiFile: Boolean,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): Flow<String> = flow {
        val fullPrompt = if (includeMultiFile) {
            "Generate a multi-file ${targetLanguage.displayName} solution with file headers and explanations:\n$prompt"
        } else {
            "Generate ${targetLanguage.displayName} code and explain it:\n$prompt"
        }
        emit(
            executeBackendCall(
                message = fullPrompt,
                history = emptyList(),
                modelOption = modelOption,
                language = targetLanguage,
                replyLanguage = replyLanguage,
                mode = "generate"
            )
        )
    }

    override fun streamCodeExplanation(
        code: String,
        language: ProgrammingLanguage,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): Flow<String> = flow {
        val prompt = "Explain this ${language.displayName} code line-by-line in simple Sinhala/English:\n```\n$code\n```"
        emit(
            executeBackendCall(
                message = prompt,
                history = emptyList(),
                modelOption = modelOption,
                language = language,
                replyLanguage = replyLanguage,
                mode = "explain"
            )
        )
    }

    override suspend fun debugCode(
        code: String,
        errorDetails: String,
        language: ProgrammingLanguage,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): DebugAnalysisResult {
        val prompt = """
            Return ONLY raw JSON with keys: rootCause, explanation, beforeCode, afterCode, improvements (array of strings).
            Debug this ${language.displayName} code. Error: $errorDetails
            Code:
            $code
        """.trimIndent()
        val raw = executeBackendCall(prompt, emptyList(), modelOption, language, replyLanguage, "debug")
        val cleaned = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(cleaned)
        val arr = json.optJSONArray("improvements")
        val improvements = buildList {
            if (arr != null) for (i in 0 until arr.length()) add(arr.getString(i))
        }
        return DebugAnalysisResult(
            rootCause = json.optString("rootCause", "Analysis complete"),
            explanation = json.optString("explanation", raw),
            beforeCode = json.optString("beforeCode", code),
            afterCode = json.optString("afterCode", code),
            improvements = improvements
        )
    }

    override suspend fun createProject(
        projectType: String,
        prompt: String,
        primaryLanguage: ProgrammingLanguage,
        replyLanguage: AppReplyLanguage,
        modelOption: GeminiModelOption
    ): GeneratedProjectBundle {
        val reqPrompt = """
            Return ONLY raw JSON with keys: projectName, architectureSummary, setupSteps (string array), files (array of {path, language, purpose, content}).
            Create a complete $projectType project in ${primaryLanguage.displayName}: $prompt
        """.trimIndent()
        val raw = executeBackendCall(reqPrompt, emptyList(), modelOption, primaryLanguage, replyLanguage, "project")
        val cleaned = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(cleaned)
        val stepsArr = json.optJSONArray("setupSteps")
        val steps = buildList {
            if (stepsArr != null) for (i in 0 until stepsArr.length()) add(stepsArr.getString(i))
        }
        val filesArr = json.optJSONArray("files")
        val files = buildList {
            if (filesArr != null) {
                for (i in 0 until filesArr.length()) {
                    val f = filesArr.getJSONObject(i)
                    add(
                        ProjectFileItem(
                            path = f.optString("path", "file_$i.${primaryLanguage.extension}"),
                            language = f.optString("language", primaryLanguage.displayName),
                            purpose = f.optString("purpose", ""),
                            content = f.optString("content", "")
                        )
                    )
                }
            }
        }
        return GeneratedProjectBundle(
            projectName = json.optString("projectName", "GhostMalliProject"),
            architectureSummary = json.optString("architectureSummary", ""),
            setupSteps = steps,
            files = files
        )
    }

    private suspend fun executeBackendCall(
        message: String,
        history: List<BackendHistoryMessage>,
        modelOption: GeminiModelOption,
        language: ProgrammingLanguage,
        replyLanguage: AppReplyLanguage,
        mode: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                createApi().chat(
                    BackendChatRequest(
                        message = message,
                        history = history,
                        model = modelOption.modelId,
                        language = language.displayName,
                        replyLanguage = replyLanguage.name,
                        mode = mode
                    )
                )
            }
            if (!response.error.isNullOrBlank()) {
                throw IllegalStateException(response.error)
            }
            response.reply?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Backend returned an empty response.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Custom backend call failed", e)
            throw IllegalStateException(
                "Backend connection error: ${e.localizedMessage ?: "Check server URL and connectivity."}"
            )
        }
    }
}
