package com.mursis.anycalc.ui.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mursis.anycalc.ai.AiRepository
import com.mursis.anycalc.camera.NormalizedBox
import com.mursis.anycalc.camera.OcrEngine
import com.mursis.anycalc.camera.Recognition
import com.mursis.anycalc.camera.StillOcr
import com.mursis.anycalc.data.ScanDao
import com.mursis.anycalc.data.ScanEntity
import com.mursis.anycalc.data.SettingsRepository
import com.mursis.anycalc.model.Solution
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val recognizedText: String = "",
    val boxes: List<NormalizedBox> = emptyList(),
    val solution: Solution? = null,
    val status: String = "Наведите камеру на задачу",
    val isBusy: Boolean = false,
    val torch: Boolean = false,
    val showSheet: Boolean = false,
    val showManual: Boolean = false,
    val manualText: String = "",
    val promptDraft: String = "",
    val ocrEngine: String = "",
    val capturedBitmap: Bitmap? = null,
    val error: String? = null,
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val ai: AiRepository,
    private val settings: SettingsRepository,
    private val dao: ScanDao,
    private val ocrEngine: OcrEngine,
    private val stillOcr: StillOcr,
) : ViewModel() {
    private val _ui = MutableStateFlow(CameraUiState())
    val ui: StateFlow<CameraUiState> = _ui.asStateFlow()

    private var lastKey = ""
    private var debounce = 550

    init {
        viewModelScope.launch {
            settings.settings.collect { s ->
                debounce = s.debounceMs
                _ui.update { it.copy(promptDraft = it.promptDraft.ifBlank { s.systemPrompt }) }
            }
        }
    }

    fun currentDebounce(): Int = debounce

    fun ocrEngine(): OcrEngine = ocrEngine

    fun onRecognized(result: Recognition) {
        _ui.update { it.copy(recognizedText = result.text, boxes = result.boxes) }
        if (result.text.length >= 2 && result.text.any { it.isDigit() }) solve(result.text, showSheet = false)
    }

    fun submit(text: String, showSheet: Boolean = true) {
        val value = text.trim()
        if (value.isBlank()) return
        _ui.update { it.copy(recognizedText = value, manualText = value) }
        solve(value, showSheet = showSheet)
    }

    /** Снятый кадр или фото из галереи: OCR → решение. */
    fun onImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    isBusy = true,
                    status = "Распознаю изображение…",
                    error = null,
                    boxes = emptyList(),
                    solution = null,
                    capturedBitmap = bitmap,
                )
            }
            val current = settings.settings.first()
            val result = stillOcr.recognize(bitmap, current) { status ->
                _ui.update { it.copy(status = status) }
            }
            if (result.text.length < 2) {
                _ui.update {
                    it.copy(
                        isBusy = false,
                        status = "Текст не распознан — попробуйте другое фото или введите вручную",
                        ocrEngine = result.engine,
                    )
                }
                return@launch
            }
            _ui.update {
                it.copy(
                    recognizedText = result.text,
                    boxes = result.boxes,
                    ocrEngine = result.engine,
                    status = "",
                )
            }
            solve(result.text, showSheet = false)
        }
    }

    /** Снятое фото заменяет live-превью и сразу идёт в OCR. */
    fun capture(bitmap: Bitmap) {
        _ui.update { it.copy(capturedBitmap = bitmap) }
        onImage(bitmap)
    }

    fun closePhoto() = _ui.update {
        it.copy(
            capturedBitmap = null,
            recognizedText = "",
            boxes = emptyList(),
            solution = null,
            ocrEngine = "",
            status = "Наведите камеру на задачу",
            error = null,
        )
    }

    /** Продвинутый локальный калькулятор — работает без провайдеров и сети. */
    fun solveLocalNow() {
        val state = _ui.value
        val text = state.recognizedText.trim().ifBlank { state.manualText.trim() }
        if (text.isBlank()) {
            _ui.update { it.copy(status = "Сначала распознайте или введите задачу") }
            return
        }
        val result = ai.solveLocal(text)
        if (result == null) {
            _ui.update { it.copy(status = "Локально не решено — попробуйте ИИ или уточните задачу") }
            return
        }
        viewModelScope.launch {
            val current = settings.settings.first()
            if (current.autoSave) {
                dao.insert(
                    ScanEntity(
                        text = text.take(2000),
                        short = result.short,
                        detailed = result.detailed,
                        confidence = result.confidence,
                        provider = result.provider,
                    ),
                )
            }
        }
        _ui.update { it.copy(solution = result, status = "", showSheet = true, isBusy = false) }
    }

    fun recalculate() {
        val text = _ui.value.recognizedText
        if (text.isNotBlank()) solve(text, promptOverride = _ui.value.promptDraft, showSheet = true)
    }

    fun resetPrompt() {
        viewModelScope.launch {
            val prompt = settings.settings.first().systemPrompt
            _ui.update { it.copy(promptDraft = prompt) }
        }
    }

    fun openSheet() = _ui.update { it.copy(showSheet = true) }

    fun closeSheet() = _ui.update { it.copy(showSheet = false) }

    fun toggleTorch() = _ui.update { it.copy(torch = !it.torch) }

    fun toggleManual() = _ui.update { it.copy(showManual = !it.showManual, error = null) }

    fun onManualChange(value: String) = _ui.update { it.copy(manualText = value) }

    fun onPromptChange(value: String) = _ui.update { it.copy(promptDraft = value) }

    fun clearError() = _ui.update { it.copy(error = null) }

    fun onImageError(message: String) = _ui.update { it.copy(error = message, isBusy = false) }

    fun saveCurrent() {
        val state = _ui.value
        val solution = state.solution ?: return
        viewModelScope.launch {
            dao.insert(
                ScanEntity(
                    text = state.recognizedText.take(2000),
                    short = solution.short,
                    detailed = solution.detailed,
                    confidence = solution.confidence,
                    provider = solution.provider,
                ),
            )
        }
    }

    fun clear() {
        lastKey = ""
        _ui.update {
            it.copy(
                recognizedText = "",
                boxes = emptyList(),
                solution = null,
                status = "Наведите камеру на задачу",
                error = null,
                showSheet = false,
            )
        }
    }

    private fun solve(text: String, promptOverride: String? = null, showSheet: Boolean) {
        val key = text.trim() + "|" + (promptOverride ?: "")
        if (key == lastKey && _ui.value.solution != null && !showSheet) return
        viewModelScope.launch {
            _ui.update { it.copy(isBusy = true, status = "Анализирую задачу…", error = null, solution = null) }
            val current = settings.settings.first()
            val effectiveSettings = if (promptOverride == null) current
            else current.copy(systemPrompt = promptOverride)
            val result = ai.solve(text, effectiveSettings) { status ->
                _ui.update { it.copy(status = status) }
            }
            lastKey = key
            _ui.update {
                it.copy(
                    isBusy = false,
                    solution = result,
                    status = "",
                    showSheet = showSheet || it.showSheet,
                    promptDraft = it.promptDraft.ifBlank { effectiveSettings.systemPrompt },
                    error = if (result.confidence == 0f) result.detailed else null,
                )
            }
            if (current.autoSave) {
                dao.insert(
                    ScanEntity(
                        text = text.trim().take(2000),
                        short = result.short,
                        detailed = result.detailed,
                        confidence = result.confidence,
                        provider = result.provider,
                    ),
                )
            }
        }
    }
}
