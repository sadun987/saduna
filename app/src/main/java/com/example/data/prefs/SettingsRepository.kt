package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppReplyLanguage
import com.example.data.model.BackendMode
import com.example.data.model.GeminiModelOption
import com.example.data.model.ProgrammingLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ghost_malli_settings")

enum class FantasyAccentTheme(val label: String) {
    NEON_VIOLET("Neon Violet & Cyan"),
    ARCANE_CRIMSON("Abyssal Crimson & Violet"),
    ASTRAL_BLUE("Electric Astral Blue")
}

data class UserSettings(
    val replyLanguage: AppReplyLanguage = AppReplyLanguage.BILINGUAL,
    val selectedModel: GeminiModelOption = GeminiModelOption.FLASH_38,
    val defaultCodeLanguage: ProgrammingLanguage = ProgrammingLanguage.KOTLIN,
    val backendMode: BackendMode = BackendMode.FIREBASE_AI_LOGIC,
    val customBackendUrl: String = "http://10.0.2.2:3000",
    val particlesEnabled: Boolean = true,
    val accentTheme: FantasyAccentTheme = FantasyAccentTheme.NEON_VIOLET
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val REPLY_LANGUAGE = stringPreferencesKey("reply_language")
        val MODEL_ID = stringPreferencesKey("model_id")
        val DEFAULT_CODE_LANGUAGE = stringPreferencesKey("default_code_language")
        val BACKEND_MODE = stringPreferencesKey("backend_mode")
        val CUSTOM_BACKEND_URL = stringPreferencesKey("custom_backend_url")
        val PARTICLES_ENABLED = booleanPreferencesKey("particles_enabled")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            replyLanguage = prefs[Keys.REPLY_LANGUAGE]?.let { name ->
                AppReplyLanguage.entries.firstOrNull { it.name == name }
            } ?: AppReplyLanguage.BILINGUAL,
            selectedModel = prefs[Keys.MODEL_ID]?.let { GeminiModelOption.fromModelId(it) }
                ?: GeminiModelOption.FLASH_38,
            defaultCodeLanguage = prefs[Keys.DEFAULT_CODE_LANGUAGE]?.let {
                ProgrammingLanguage.fromName(it)
            } ?: ProgrammingLanguage.KOTLIN,
            backendMode = prefs[Keys.BACKEND_MODE]?.let { name ->
                BackendMode.entries.firstOrNull { it.name == name }
            } ?: BackendMode.FIREBASE_AI_LOGIC,
            customBackendUrl = prefs[Keys.CUSTOM_BACKEND_URL] ?: "http://10.0.2.2:3000",
            particlesEnabled = prefs[Keys.PARTICLES_ENABLED] ?: true,
            accentTheme = prefs[Keys.ACCENT_THEME]?.let { name ->
                FantasyAccentTheme.entries.firstOrNull { it.name == name }
            } ?: FantasyAccentTheme.NEON_VIOLET
        )
    }

    suspend fun setReplyLanguage(language: AppReplyLanguage) {
        context.dataStore.edit { it[Keys.REPLY_LANGUAGE] = language.name }
    }

    suspend fun setSelectedModel(model: GeminiModelOption) {
        context.dataStore.edit { it[Keys.MODEL_ID] = model.modelId }
    }

    suspend fun setDefaultCodeLanguage(language: ProgrammingLanguage) {
        context.dataStore.edit { it[Keys.DEFAULT_CODE_LANGUAGE] = language.displayName }
    }

    suspend fun setBackendMode(mode: BackendMode) {
        context.dataStore.edit { it[Keys.BACKEND_MODE] = mode.name }
    }

    suspend fun setCustomBackendUrl(url: String) {
        context.dataStore.edit { it[Keys.CUSTOM_BACKEND_URL] = url.trim() }
    }

    suspend fun setParticlesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PARTICLES_ENABLED] = enabled }
    }

    suspend fun setAccentTheme(theme: FantasyAccentTheme) {
        context.dataStore.edit { it[Keys.ACCENT_THEME] = theme.name }
    }
}
