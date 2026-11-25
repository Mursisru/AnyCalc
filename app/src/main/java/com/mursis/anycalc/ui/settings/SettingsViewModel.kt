package com.mursis.anycalc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mursis.anycalc.ai.AiRepository
import com.mursis.anycalc.data.AppSettings
import com.mursis.anycalc.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val ai: AiRepository,
) : ViewModel() {
    val settings = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun completeOnboarding() = viewModelScope.launch { repo.setOnboardingDone() }

    fun setKey(id: String, value: String) = viewModelScope.launch { repo.setKey(id, value) }

    fun updateProvider(id: String, baseUrl: String, model: String, enabled: Boolean) =
        viewModelScope.launch { repo.updateProvider(id, baseUrl, model, enabled) }

    fun move(from: Int, to: Int) = viewModelScope.launch {
        val list = settings.value.providerOrder.toMutableList()
        if (from !in list.indices || to !in list.indices || from == to) return@launch
        val item = list.removeAt(from)
        list.add(to, item)
        repo.setProviderOrder(list)
    }

    fun setPrompt(value: String) = viewModelScope.launch { repo.setPrompt(value) }

    fun setHaptics(value: Boolean) = viewModelScope.launch { repo.setHaptics(value) }

    fun setReduceMotion(value: Boolean) = viewModelScope.launch { repo.setReduceMotion(value) }

    fun setAutoSave(value: Boolean) = viewModelScope.launch { repo.setAutoSave(value) }

    fun setDebounce(value: Int) = viewModelScope.launch { repo.setDebounce(value) }

    suspend fun check(id: String): String {
        val provider = settings.value.providers[id] ?: return "Провайдер не найден"
        return ai.checkConnection(provider, settings.value)
    }

    suspend fun refreshModels(): String {
        val current = settings.value
        val opencode = current.orderedProviders.firstOrNull { it.kind == com.mursis.anycalc.data.ProviderKind.OPENCODE }
            ?: return "OpenCode не настроен"
        val models = ai.loadOpenCodeModels(opencode, current)
        repo.setRemoteModels(models)
        return if (models.isEmpty()) "Модели не загружены — проверьте подключение"
        else "Загружено моделей: ${models.size}"
    }

    fun setAuth(id: String, user: String, password: String) = viewModelScope.launch { repo.setAuth(id, user, password) }

    fun setVisionModel(id: String, value: String) = viewModelScope.launch { repo.setVisionModel(id, value) }

    fun setOcrMode(value: String) = viewModelScope.launch { repo.setOcrMode(value) }
}
