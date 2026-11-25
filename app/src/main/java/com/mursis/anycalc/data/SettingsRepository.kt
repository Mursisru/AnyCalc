package com.mursis.anycalc.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "anycalc_settings")

enum class ProviderKind { OPENCODE, OPENAI }

data class ProviderConfig(
    val id: String,
    val name: String,
    val kind: ProviderKind,
    val baseUrl: String,
    val model: String,
    val enabled: Boolean = true,
    val hasKey: Boolean = false,
    val authUser: String = "",
    val hasAuthPassword: Boolean = false,
    val visionModel: String = "",
)

data class AppSettings(
    val onboardingDone: Boolean = false,
    val providerOrder: List<String> = DEFAULT_ORDER,
    val providers: Map<String, ProviderConfig> = emptyMap(),
    val systemPrompt: String = DEFAULT_PROMPT,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val autoSave: Boolean = true,
    val debounceMs: Int = 550,
    val ocrMode: String = "AUTO",
    val remoteModels: List<String> = emptyList(),
    val themeMode: String = "DARK",
    val paletteId: String = "dark_obsidian",
    val language: String = "ru",
    val fontScale: Float = 1f,
    val cornerRadius: Int = 24,
    val glassEffect: Boolean = true,
    val cameraGrid: Boolean = false,
    val cameraSound: Boolean = false,
    val cameraMirror: Boolean = false,
) {
    val orderedProviders: List<ProviderConfig>
        get() = providerOrder.mapNotNull { providers[it] }
}

private val DEFAULT_ORDER = listOf("opencode", "nvidia", "groq", "gemini", "openrouter", "deepseek", "custom")

const val DEFAULT_PROMPT = """Ты — эксперт-репетитор. Реши задачу, которую я пришлю.
Ответь ТОЛЬКО валидным JSON без обёрток и комментариев:
{"short":"короткий ответ, максимум 12 слов","detailed":"пошаговое решение с формулами","confidence":0.0}
confidence — число от 0 до 1. detailed — список с нумерацией шагов, каждый шаг с новой строки."""

val PROVIDER_PRESETS = listOf(
    ProviderConfig(
        "opencode", "OpenCode Server", ProviderKind.OPENCODE,
        "http://127.0.0.1:28894", "opencode", authUser = "opencode",
    ),
    ProviderConfig("nvidia", "NVIDIA NIM", ProviderKind.OPENAI, "https://integrate.api.nvidia.com/v1", "nvidia/nemotron-3.5-lightning-30b-a3b", visionModel = "nvidia/nemotron-3.5-super-vl-120b-a12b"),
    ProviderConfig("groq", "Groq", ProviderKind.OPENAI, "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
    ProviderConfig("gemini", "Gemini", ProviderKind.OPENAI, "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.5-flash", visionModel = "gemini-2.5-flash"),
    ProviderConfig("openrouter", "OpenRouter", ProviderKind.OPENAI, "https://openrouter.ai/api/v1", "openai/gpt-4o-mini", visionModel = "openai/gpt-4o-mini"),
    ProviderConfig("deepseek", "DeepSeek", ProviderKind.OPENAI, "https://api.deepseek.com/v1", "deepseek-chat"),
    ProviderConfig("custom", "Custom API", ProviderKind.OPENAI, "", ""),
)

private val K_ONBOARDING = booleanPreferencesKey("onboarding_done")
private val K_ORDER = stringPreferencesKey("provider_order")
private val K_PROMPT = stringPreferencesKey("system_prompt")
private val K_HAPTICS = booleanPreferencesKey("haptics")
private val K_REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
private val K_AUTO_SAVE = booleanPreferencesKey("auto_save")
private val K_DEBOUNCE = intPreferencesKey("debounce_ms")
private val K_OCR_MODE = stringPreferencesKey("ocr_mode")
private val K_REMOTE_MODELS = stringPreferencesKey("remote_models")
private val K_THEME_MODE = stringPreferencesKey("theme_mode")
private val K_PALETTE = stringPreferencesKey("palette_id")
private val K_LANGUAGE = stringPreferencesKey("language")
private val K_FONT_SCALE = androidx.datastore.preferences.core.floatPreferencesKey("font_scale")
private val K_CORNER = intPreferencesKey("corner_radius")
private val K_GLASS = booleanPreferencesKey("glass_effect")
private val K_CAM_GRID = booleanPreferencesKey("camera_grid")
private val K_CAM_SOUND = booleanPreferencesKey("camera_sound")
private val K_CAM_MIRROR = booleanPreferencesKey("camera_mirror")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureKeys: SecureKeyStore,
) {
    private val store = context.settingsDataStore
    private val keyChanges = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }

    val settings: Flow<AppSettings> = combine(store.data, keyChanges) { prefs, _ ->
        val order = prefs[K_ORDER]?.takeIf { it.isNotBlank() }?.split(',')?.filter { it.isNotBlank() }
            ?: DEFAULT_ORDER
        val providers = PROVIDER_PRESETS.associate { preset ->
            val id = preset.id
            preset.id to preset.copy(
                baseUrl = prefs[stringPreferencesKey("base_$id")] ?: preset.baseUrl,
                model = prefs[stringPreferencesKey("model_$id")] ?: preset.model,
                enabled = prefs[booleanPreferencesKey("enabled_$id")] != false,
                hasKey = secureKeys.hasKey(id),
                authUser = prefs[stringPreferencesKey("authuser_$id")] ?: preset.authUser,
                hasAuthPassword = secureKeys.hasKey("auth_$id"),
                visionModel = prefs[stringPreferencesKey("vision_$id")] ?: preset.visionModel,
            )
        }
        AppSettings(
            onboardingDone = prefs[K_ONBOARDING] == true,
            providerOrder = order,
            providers = providers,
            systemPrompt = prefs[K_PROMPT] ?: DEFAULT_PROMPT,
            haptics = prefs[K_HAPTICS] != false,
            reduceMotion = prefs[K_REDUCE_MOTION] == true,
            autoSave = prefs[K_AUTO_SAVE] != false,
            debounceMs = prefs[K_DEBOUNCE] ?: 550,
            ocrMode = prefs[K_OCR_MODE] ?: "AUTO",
            remoteModels = prefs[K_REMOTE_MODELS]?.split('|')?.filter { it.isNotBlank() } ?: emptyList(),
            themeMode = prefs[K_THEME_MODE] ?: "DARK",
            paletteId = prefs[K_PALETTE] ?: "dark_obsidian",
            language = prefs[K_LANGUAGE] ?: "ru",
            fontScale = prefs[K_FONT_SCALE] ?: 1f,
            cornerRadius = prefs[K_CORNER] ?: 24,
            glassEffect = prefs[K_GLASS] != false,
            cameraGrid = prefs[K_CAM_GRID] == true,
            cameraSound = prefs[K_CAM_SOUND] == true,
            cameraMirror = prefs[K_CAM_MIRROR] == true,
        )
    }

    suspend fun setOnboardingDone() = store.edit { it[K_ONBOARDING] = true }

    suspend fun setProviderOrder(order: List<String>) = store.edit { it[K_ORDER] = order.joinToString(",") }

    suspend fun updateProvider(id: String, baseUrl: String, model: String, enabled: Boolean) = store.edit { prefs ->
        prefs[stringPreferencesKey("base_$id")] = baseUrl.trim()
        prefs[stringPreferencesKey("model_$id")] = model.trim()
        prefs[booleanPreferencesKey("enabled_$id")] = enabled
    }

    suspend fun setKey(id: String, value: String) {
        secureKeys.putKey(id, value)
        keyChanges.emit(Unit)
    }

    suspend fun setPrompt(value: String) = store.edit { it[K_PROMPT] = value }

    suspend fun setHaptics(value: Boolean) = store.edit { it[K_HAPTICS] = value }

    suspend fun setReduceMotion(value: Boolean) = store.edit { it[K_REDUCE_MOTION] = value }

    suspend fun setAutoSave(value: Boolean) = store.edit { it[K_AUTO_SAVE] = value }

    suspend fun setDebounce(value: Int) = store.edit { it[K_DEBOUNCE] = value.coerceIn(200, 1200) }

    suspend fun setOcrMode(value: String) = store.edit { it[K_OCR_MODE] = value }

    suspend fun setRemoteModels(models: List<String>) = store.edit { it[K_REMOTE_MODELS] = models.joinToString("|") }

    suspend fun setAuth(id: String, user: String, password: String) {
        store.edit { it[stringPreferencesKey("authuser_$id")] = user.trim() }
        secureKeys.putKey("auth_$id", password)
        keyChanges.emit(Unit)
    }

    suspend fun setVisionModel(id: String, value: String) =
        store.edit { it[stringPreferencesKey("vision_$id")] = value.trim() }

    suspend fun setThemeMode(value: String) = store.edit {
        it[K_THEME_MODE] = value
        val prefix = if (value == "DARK") "light_" else "dark_"
        if (it[K_PALETTE]?.startsWith(prefix) == true) {
            it[K_PALETTE] = if (value == "DARK") "dark_obsidian" else "light_paper"
        }
    }

    suspend fun setPalette(value: String) = store.edit { it[K_PALETTE] = value }

    suspend fun setLanguage(value: String) = store.edit { it[K_LANGUAGE] = value }

    suspend fun setFontScale(value: Float) = store.edit { it[K_FONT_SCALE] = value.coerceIn(0.85f, 1.25f) }

    suspend fun setCornerRadius(value: Int) = store.edit { it[K_CORNER] = value.coerceIn(12, 32) }

    suspend fun setGlassEffect(value: Boolean) = store.edit { it[K_GLASS] = value }

    suspend fun setCameraGrid(value: Boolean) = store.edit { it[K_CAM_GRID] = value }

    suspend fun setCameraSound(value: Boolean) = store.edit { it[K_CAM_SOUND] = value }

    suspend fun setCameraMirror(value: Boolean) = store.edit { it[K_CAM_MIRROR] = value }
}
