package com.mursis.anycalc.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mursis.anycalc.data.ProviderConfig
import com.mursis.anycalc.data.ProviderKind
import com.mursis.anycalc.ui.components.CapsuleButton
import com.mursis.anycalc.ui.components.GlassCard
import com.mursis.anycalc.ui.components.KeyField
import com.mursis.anycalc.ui.components.PlainField
import com.mursis.anycalc.ui.components.PromptField
import com.mursis.anycalc.ui.components.SectionLabel
import com.mursis.anycalc.ui.components.ScrollEdgeFade
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf<String?>(null) }
    var checkState by remember { mutableStateOf<Pair<String, String>?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Настройки", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                CapsuleButton("Назад", onClick = onBack, primary = false, modifier = Modifier.width(110.dp))
            }
            Spacer(Modifier.height(12.dp))
        }

        ScrollEdgeFade(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            SectionLabel("Провайдеры · порядок перетаскиванием")
            val list = settings.orderedProviders
            list.forEachIndexed { index, provider ->
                ProviderRow(
                    provider = provider,
                    index = index,
                    last = list.lastIndex,
                    expanded = expanded == provider.id,
                    onToggleExpand = { expanded = if (expanded == provider.id) null else provider.id },
                    onMove = { from, to -> viewModel.move(from, to) },
                    onUpdate = { url, model, enabled -> viewModel.updateProvider(provider.id, url, model, enabled) },
                    onKey = { viewModel.setKey(provider.id, it) },
                    onAuth = { user, password -> viewModel.setAuth(provider.id, user, password) },
                    onVisionModel = { viewModel.setVisionModel(provider.id, it) },
                    onCheck = {
                        scope.launch {
                            val result = viewModel.check(provider.id)
                            expanded = provider.id
                            checkState = provider.id to result
                        }
                    },
                    checkResult = checkState?.takeIf { it.first == provider.id }?.second,
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("OpenCode Server в локальной сети")
            GlassCard {
                Text(
                    "127.0.0.1 — это сам телефон. На ПК: opencode service set hostname 0.0.0.0 && opencode service restart. Затем найдите сервер в Wi‑Fi или введите http://IP-ПК:28894 в Base URL.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                CapsuleButton(
                    "Найти сервер в LAN",
                    onClick = { scope.launch { viewModel.discoverServer { checkState = "lan" to it } } },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (checkState?.first == "lan") {
                    Spacer(Modifier.height(8.dp))
                    Text(checkState?.second.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Распознавание текста (OCR)")
            GlassCard {
                val modes = listOf("AUTO", "TESSERACT", "MLKIT", "AI")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEach { mode ->
                        CapsuleButton(
                            text = mode,
                            onClick = { viewModel.setOcrMode(mode) },
                            modifier = Modifier.weight(1f),
                            primary = settings.ocrMode == mode,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                CapsuleButton(
                    "Обновить список моделей OpenCode",
                    onClick = {
                        scope.launch { checkState = "models" to viewModel.refreshModels() }
                    },
                    primary = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (checkState?.first == "models") {
                    Spacer(Modifier.height(8.dp))
                    Text(checkState?.second.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (settings.remoteModels.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        settings.remoteModels.take(12).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Оформление")
            GlassCard {
                val dark = settings.themeMode != "LIGHT"
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CapsuleButton("Тёмная", onClick = { viewModel.setThemeMode("DARK") }, primary = dark, modifier = Modifier.weight(1f))
                    CapsuleButton("Светлая", onClick = { viewModel.setThemeMode("LIGHT") }, primary = !dark, modifier = Modifier.weight(1f))
                }
                val palettes = if (dark) com.mursis.anycalc.data.DARK_PALETTES else com.mursis.anycalc.data.LIGHT_PALETTES
                for (group in listOf("Бизнес", "Пастель")) {
                    Spacer(Modifier.height(14.dp))
                    Text(group, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        palettes.filter { it.group == group }.forEach { p ->
                            PaletteSwatch(
                                palette = p,
                                selected = p.id == settings.paletteId,
                                onClick = { viewModel.setPalette(p.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Размер текста: ${(settings.fontScale * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Slider(value = settings.fontScale, onValueChange = viewModel::setFontScale, valueRange = 0.85f..1.25f)
                Text("Скругления: ${settings.cornerRadius} dp", style = MaterialTheme.typography.bodyMedium)
                Slider(value = settings.cornerRadius.toFloat(), onValueChange = { viewModel.setCornerRadius(it.toInt()) }, valueRange = 12f..32f)
                ToggleRow("Матовое стекло", "Полупрозрачные поверхности", settings.glassEffect, viewModel::setGlassEffect)
                ToggleRow("Уменьшить анимации", "Плавность вместо движения", settings.reduceMotion, viewModel::setReduceMotion)
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Язык")
            GlassCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("ru" to "Русский", "en" to "English", "system" to "Системный").forEach { (code, label) ->
                        CapsuleButton(label, onClick = { viewModel.setLanguage(code) }, primary = settings.language == code, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Язык применяется к форматам дат и интерфейсу после перезапуска.", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Камера")
            GlassCard {
                ToggleRow("Сетка", "Линии композиции на预览", settings.cameraGrid, viewModel::setCameraGrid)
                ToggleRow("Звук затвора", "Короткий щелчок при съёмке", settings.cameraSound, viewModel::setCameraSound)
                ToggleRow("Зеркало", "Отображать как селфи-камеру", settings.cameraMirror, viewModel::setCameraMirror)
                Text("Debounce OCR: ${settings.debounceMs} мс", style = MaterialTheme.typography.bodyMedium)
                Slider(value = settings.debounceMs.toFloat(), onValueChange = { viewModel.setDebounce(it.toInt()) }, valueRange = 200f..1200f)
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Системный промпт")
            GlassCard {
                PromptField(value = settings.systemPrompt, onValueChange = viewModel::setPrompt)
                Spacer(Modifier.height(8.dp))
                CapsuleButton("Сбросить промпт", onClick = { viewModel.setPrompt(com.mursis.anycalc.data.DEFAULT_PROMPT) }, primary = false, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Интерфейс и live-режим")
            GlassCard {
                ToggleRow("Тактильная отдача", "Лёгкий отклик при захвате и ответе", settings.haptics, viewModel::setHaptics)
                ToggleRow("Уменьшить анимации", "Отключает движение для доступности", settings.reduceMotion, viewModel::setReduceMotion)
                ToggleRow("Автосохранение", "Сохранять ответы в историю", settings.autoSave, viewModel::setAutoSave)
                Spacer(Modifier.height(14.dp))
                Text("Debounce OCR: ${settings.debounceMs} мс", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Slider(
                    value = settings.debounceMs.toFloat(),
                    onValueChange = { viewModel.setDebounce(it.roundToInt()) },
                    valueRange = 200f..1200f,
                    steps = 9,
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "AnyCalc 1.0 · локальные вычисления и ИИ-провайдеры",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        }
    }
}

@Composable
private fun ProviderRow(
    provider: ProviderConfig,
    index: Int,
    last: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onUpdate: (String, String, Boolean) -> Unit,
    onKey: (String) -> Unit,
    onAuth: (String, String) -> Unit,
    onVisionModel: (String) -> Unit,
    onCheck: () -> Unit,
    checkResult: String?,
) {
    var dragY by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 78.dp.toPx() }
    var url by remember(provider.id) { mutableStateOf(provider.baseUrl) }
    var model by remember(provider.id) { mutableStateOf(provider.model) }
    var authUser by remember(provider.id) { mutableStateOf(provider.authUser) }
    var authPassword by remember(provider.id) { mutableStateOf("") }
    var apiKey by remember(provider.id) { mutableStateOf("") }
    var vision by remember(provider.id) { mutableStateOf(provider.visionModel) }

    GlassCard(
        radius = 22,
        padding = 14,
        modifier = Modifier.graphicsLayer { translationY = dragY },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    .pointerInput(provider.id) {
                        detectDragGestures(
                            onDrag = { change, amount ->
                                change.consume()
                                dragY += amount.y
                            },
                            onDragEnd = {
                                val steps = (dragY / rowHeightPx).roundToInt()
                                if (steps != 0) onMove(index, (index + steps).coerceIn(0, last))
                                dragY = 0f
                            },
                            onDragCancel = { dragY = 0f },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(com.mursis.anycalc.R.drawable.ic_settings),
                    contentDescription = "Перетащите",
                    modifier = Modifier.size(20.dp).alpha(0.7f),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickableNoRipple(onToggleExpand)) {
                Text(provider.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (provider.kind == ProviderKind.OPENCODE) "OpenCode Server" else "OpenAI-совместимый API",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = provider.enabled,
                onCheckedChange = { onUpdate(url, model, it) },
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondary, checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
            )
        }

        if (expanded) {
            Spacer(Modifier.height(14.dp))
            PlainField(url, { url = it; onUpdate(url, model, provider.enabled) }, "Base URL")
            Spacer(Modifier.height(10.dp))
            PlainField(model, { model = it; onUpdate(url, model, provider.enabled) }, "Модель")
            if (provider.kind == ProviderKind.OPENAI) {
                Spacer(Modifier.height(10.dp))
                KeyField(apiKey, { apiKey = it; onKey(it) }, if (provider.hasKey) "API-ключ (сохранён)" else "API-ключ")
                Spacer(Modifier.height(10.dp))
                PlainField(vision, { vision = it; onVisionModel(vision) }, "Vision-модель для OCR")
            } else {
                Spacer(Modifier.height(10.dp))
                PlainField(authUser, { authUser = it; onAuth(authUser, authPassword) }, "Логин сервера")
                Spacer(Modifier.height(10.dp))
                KeyField(
                    authPassword,
                    { authPassword = it; onAuth(authUser, authPassword) },
                    if (provider.hasAuthPassword) "Пароль (сохранён)" else "Пароль OpenCode Server",
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CapsuleButton("Проверить подключение", onClick = onCheck, primary = false, modifier = Modifier.weight(1f))
            }
            if (checkResult != null) {
                Spacer(Modifier.height(8.dp))
                Text(checkResult, style = MaterialTheme.typography.bodySmall, color = if (checkResult.startsWith("OK")) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    palette: com.mursis.anycalc.data.Palette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .border(1.dp, if (selected) palette.accent else palette.border, RoundedCornerShape(16.dp))
            .clickableNoRipple(onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(palette.background, palette.surfaceVariant, palette.accent, palette.accentSoft).forEach { color ->
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            palette.name,
            style = MaterialTheme.typography.labelMedium,
            color = palette.text,
            maxLines = 1,
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondary, checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
        )
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    clickable(
        onClick = onClick,
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
    ),
)
