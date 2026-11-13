package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AssistantMode(
    val wireName: String,
    val displayTitle: String,
    val shortLabel: String,
    val subtitle: String
) {
    CHAT(
        wireName = "chat",
        displayTitle = "Shadow Nexus Chat",
        shortLabel = "Chat",
        subtitle = "Sinhala & English AI coding mentor with session memory"
    ),
    GENERATE(
        wireName = "generate",
        displayTitle = "Code Forge",
        shortLabel = "Generate",
        subtitle = "Generate clean multi-file code in 16 languages"
    ),
    DEBUG(
        wireName = "debug",
        displayTitle = "Void Debugger",
        shortLabel = "Debug",
        subtitle = "Analyze bugs, explain root causes, show before & after"
    ),
    EXPLAIN(
        wireName = "explain",
        displayTitle = "Sinhala Rune Explainer",
        shortLabel = "Explain",
        subtitle = "Simple Sinhala & English line-by-line explanations"
    ),
    PROJECT(
        wireName = "project",
        displayTitle = "Project Architect",
        shortLabel = "Project",
        subtitle = "Scaffold full Android, Web, Python, Java & Backend apps"
    );

    companion object {
        fun fromWireName(value: String): AssistantMode =
            entries.firstOrNull { it.wireName == value } ?: CHAT
    }
}

enum class ProgrammingLanguage(
    val displayName: String,
    val extension: String,
    val keywords: Set<String>
) {
    PYTHON("Python", "py", setOf("def", "class", "import", "from", "return", "if", "elif", "else", "for", "while", "async", "await", "try", "except", "with", "as", "True", "False", "None", "lambda", "yield", "pass")),
    JAVA("Java", "java", setOf("public", "private", "protected", "class", "interface", "extends", "implements", "void", "int", "String", "boolean", "return", "if", "else", "for", "while", "new", "static", "final", "try", "catch", "import", "package")),
    JAVASCRIPT("JavaScript", "js", setOf("const", "let", "var", "function", "return", "if", "else", "for", "while", "async", "await", "import", "export", "default", "class", "new", "try", "catch", "true", "false", "null")),
    HTML("HTML", "html", setOf("html", "head", "body", "div", "span", "section", "header", "footer", "script", "style", "link", "meta", "title", "button", "input", "form", "main", "nav")),
    CSS("CSS", "css", setOf("display", "flex", "grid", "color", "background", "border", "padding", "margin", "position", "width", "height", "font", "transition", "animation", "hover", "root")),
    C("C", "c", setOf("int", "char", "float", "double", "void", "struct", "typedef", "return", "if", "else", "for", "while", "switch", "case", "break", "include", "define", "sizeof", "NULL")),
    CPP("C++", "cpp", setOf("int", "auto", "class", "struct", "namespace", "using", "template", "typename", "public", "private", "virtual", "override", "const", "constexpr", "return", "std", "vector", "string", "nullptr")),
    CSHARP("C#", "cs", setOf("using", "namespace", "public", "private", "class", "interface", "record", "async", "await", "Task", "var", "string", "int", "bool", "return", "if", "else", "foreach", "new", "null")),
    KOTLIN("Kotlin", "kt", setOf("package", "import", "fun", "val", "var", "class", "data", "sealed", "interface", "object", "override", "suspend", "return", "if", "else", "when", "for", "while", "null", "true", "false", "companion")),
    PHP("PHP", "php", setOf("php", "echo", "function", "class", "public", "private", "protected", "return", "if", "else", "foreach", "while", "use", "namespace", "new", "array", "true", "false", "null")),
    SQL("SQL", "sql", setOf("SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE", "TABLE", "JOIN", "INNER", "LEFT", "ON", "GROUP", "BY", "ORDER", "LIMIT", "AND", "OR", "NOT", "NULL")),
    DART("Dart", "dart", setOf("import", "void", "class", "extends", "with", "final", "const", "var", "late", "Future", "async", "await", "return", "if", "else", "for", "while", "Widget", "build", "override", "null")),
    TYPESCRIPT("TypeScript", "ts", setOf("interface", "type", "enum", "const", "let", "function", "return", "async", "await", "import", "export", "implements", "extends", "public", "private", "readonly", "string", "number", "boolean")),
    JSON("JSON", "json", setOf("true", "false", "null")),
    XML("XML", "xml", setOf("xml", "version", "encoding", "xmlns", "android", "manifest", "application", "activity", "resources", "string", "color")),
    BASH("Bash", "sh", setOf("echo", "export", "if", "then", "else", "fi", "for", "do", "done", "while", "case", "esac", "function", "local", "exit", "cd", "grep", "awk", "sed", "curl"));

    companion object {
        fun fromName(name: String): ProgrammingLanguage =
            entries.firstOrNull {
                it.displayName.equals(name, ignoreCase = true) ||
                    it.extension.equals(name, ignoreCase = true)
            } ?: KOTLIN
    }
}

enum class AppReplyLanguage(val label: String, val instructionHint: String) {
    BILINGUAL(
        label = "Auto (Sinhala & English)",
        instructionHint = "Detect whether the user wrote in Sinhala (සිංහල), Singlish, or English, and reply naturally in that language. Support both Sinhala and English seamlessly."
    ),
    SINHALA(
        label = "Sinhala (සිංහල)",
        instructionHint = "Write all explanations and commentary in clear, friendly, simple Sinhala (සිංහල), while keeping code syntax and technical keywords accurate in English."
    ),
    ENGLISH(
        label = "English",
        instructionHint = "Reply in crisp, clear, professional English."
    )
}

enum class GeminiModelOption(
    val modelId: String,
    val displayName: String,
    val description: String
) {
    FLASH_38(
        modelId = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash",
        description = "Balanced frontier speed & coding intelligence (Default)"
    ),
    FLASH_LITE_35(
        modelId = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        description = "Ultra-fast responses for quick snippets & explanations"
    ),
    ARGON_4(
        modelId = "gemini-4-argon",
        displayName = "Gemini 4 Argon",
        description = "Deep architecture & complex debugging reasoning (Blaze)"
    );

    companion object {
        fun fromModelId(id: String): GeminiModelOption =
            entries.firstOrNull { it.modelId == id } ?: FLASH_38
    }
}

enum class BackendMode(val label: String) {
    FIREBASE_AI_LOGIC("Firebase AI Logic (Secure Managed)"),
    CUSTOM_BACKEND("Custom Backend API Server")
}

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val mode: String = AssistantMode.CHAT.wireName,
    val language: String = ProgrammingLanguage.KOTLIN.displayName,
    val modelName: String = GeminiModelOption.FLASH_38.modelId,
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val role: String = "user",
    val content: String = "",
    val language: String = "",
    val toolMode: String = AssistantMode.CHAT.wireName,
    val sequenceOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CodeSnippet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val fileName: String = "",
    val language: String = ProgrammingLanguage.KOTLIN.displayName,
    val code: String = "",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ProjectFileItem(
    val path: String,
    val language: String,
    val content: String,
    val purpose: String
)

data class GeneratedProjectBundle(
    val projectName: String,
    val architectureSummary: String,
    val setupSteps: List<String>,
    val files: List<ProjectFileItem>
)

data class DebugAnalysisResult(
    val rootCause: String,
    val explanation: String,
    val beforeCode: String,
    val afterCode: String,
    val improvements: List<String>
)
