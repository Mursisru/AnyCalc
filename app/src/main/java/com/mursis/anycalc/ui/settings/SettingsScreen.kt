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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mursis.anycalc.R
import com.mursis.anycalc.data.AppSettings
import com.mursis.anycalc.data.DARK_PALETTES
import com.mursis.anycalc.data.LIGHT_PALETTES
import com.mursis.anycalc.data.Palette
import com.mursis.anycalc.data.ProviderConfig
import com.mursis.anycalc.data.ProviderKind
import com.mursis.anycalc.ui.components.CapsuleButton
import com.mursis.anycalc.ui.components.GlassCard
import com.mursis.anycalc.ui.components.KeyField
import com.mursis.anycalc.ui.components.PlainField
import com.mursis.anycalc.ui.components.PromptField
import com.mursis.anycalc.ui.components.ScrollEdgeFade
import com.mursis.anycalc.ui.components.SectionLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class Category(val id: String, val title: String, val subtitle: String, val icon: Int)

private val CATEGORIES = listOf(
    Category("providers", "Провайдеры", "Ключи, модели, приоритеты", R.drawable.ic_sparkle),
    Category("ocr", "Распознавание", "Движок OCR и модель", R.drawable.ic_camera),
    Category("server", "OpenCode Server", "Подключение по сети", R.drawable.ic_settings),
    Category("appearance", "Оформление", "Тема, палитры, текст", R.drawable.ic_gallery),
    Category("language", "Язык", "Русский, English, авто", R.drawable.ic_chevron),
    Category("camera", "Камера", "Сетка, звук, зеркало", R.drawable.ic_camera),
    Category("prompt", "Системный промпт", "Инструкция для ИИ", R.drawable.ic_keyboard),
    Category("general", "Общие", "Отклик, анимации, сохранение", R.drawable.ic_check),
)

@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var category by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var checkState by remember { mutableStateOf<Pair<String, String>?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CATEGORIES.firstOrNull { it.id == category }?.title ?: "Настройки",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                CapsuleButton(
                    text = "Назад",
                    onClick = { if (category == null) onBack() else category = null },
                    primary = false,
                    modifier = Modifier.width(104.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        if (category == null) {
            CategoryList(onSelect = { category = it })
        } else {
            CategoryContent(
                id = category!!,
                settings = settings,
                viewModel = viewModel,
                scope = scope,
                expanded = expanded,
                onExpand = { expanded = if (expanded == it) null else it },
                checkState = checkState,
                onCheckState = { checkState = it },
            )
        }
    }
}

@Composable
private fun CategoryList(onSelect: (String) -> Unit) {
    val scroll = rememberScrollState()
    val canUp by remember { derivedStateOf { scroll.value > 0 } }
    val canDown by remember { derivedStateOf { scroll.value < scroll.maxValue - 1 } }
    ScrollEdgeFade(
        modifier = Modifier.fillMaxSize(),
        canScrollUp = canUp,
        canScrollDown = canDown,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CATEGORIES.forEach { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable { onSelect(cat.id) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(painterResource(cat.icon), contentDescription = null, modifier = Modifier.size(22.dp).alpha(0.8f))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(cat.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        Text(cat.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Image(painterResource(R.drawable.ic_chevron), contentDescription = null, modifier = Modifier.size(18.dp).alpha(0.6f))
                }
            }
        }
    }
}

@Composable
private fun CategoryContent(
    id: String,
    settings: AppSettings,
    viewModel: SettingsViewModel,
    scope: CoroutineScope,
    expanded: String?,
    onExpand: (String) -> Unit,
    checkState: Pair<String, String>?,
    onCheckState: (Pair<String, String>) -> Unit,
) {
    val scroll = rememberScrollState()
    val canUp by remember { derivedStateOf { scroll.value > 0 } }
    val canDown by remember { derivedStateOf { scroll.value < scroll.maxValue - 1 } }
    ScrollEdgeFade(
        modifier = Modifier.fillMaxSize(),
        canScrollUp = canUp,
        canScrollDown = canDown,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            when (id) {
                "providers" -> ProvidersContent(settings, viewModel, expanded, onExpand, checkState, scope, onCheckState)
                "ocr" -> OcrContent(settings, viewModel, scope, checkState, onCheckState)
                "server" -> ServerContent(settings, viewModel, scope, checkState, onCheckState)
                "appearance" -> AppearanceContent(settings, viewModel)
                "language" -> LanguageContent(settings, viewModel)
                "camera" -> CameraContent(settings, viewModel)
                "prompt" -> PromptContent(settings, viewModel)
                "general" -> GeneralContent(settings, viewModel)
            }
        }
    }
}

@Composable
private fun ProvidersContent(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    expanded: String?,
    onExpand: (String) -> Unit,
    checkState: Pair<String, String>?,
    scope: CoroutineScope,
    onCheckState: (Pair<String, String>) -> Unit,
) {
    SectionLabel("Порядок перетаскиванием")
    settings.orderedProviders.forEachIndexed { index, provider ->
        ProviderRow(
            provider = provider,
            index = index,
            last = settings.orderedProviders.lastIndex,
            expanded = expanded == provider.id,
            onToggleExpand = { onExpand(provider.id) },
            onMove = { from, to -> viewModel.move(from, to) },
            onUpdate = { url, model, enabled -> viewModel.updateProvider(provider.id, url, model, enabled) },
            onKey = { viewModel.setKey(provider.id, it) },
            onAuth = { user, password -> viewModel.setAuth(provider.id, user, password) },
            onVisionModel = { viewModel.setVisionModel(provider.id, it) },
            onCheck = {
                scope.launch { onCheckState(provider.id to viewModel.check(provider.id)) }
            },
            checkResult = checkState?.takeIf { it.first == provider.id }?.second,
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun OcrContent(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    scope: CoroutineScope,
    checkState: Pair<String, String>?,
    onCheckState: (Pair<String, String>) -> Unit,
) {
    SectionLabel("Движок распознавания")
    GlassCard {
        val modes = listOf("AUTO" to "Авто", "TESSERACT" to "Tess", "MLKIT" to "ML", "AI" to "AI")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            modes.forEach { (code, label) ->
                CapsuleButton(label, onClick = { viewModel.setOcrMode(code) }, primary = settings.ocrMode == code, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Авто: Tesseract → ML Kit → AI. Локальные движки работают без сети.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(12.dp))
    SectionLabel("Модели OpenCode")
    GlassCard {
        CapsuleButton(
            "Обновить список моделей",
            onClick = { scope.launch { onCheckState("models" to viewModel.refreshModels()) } },
            modifier = Modifier.fillMaxWidth(),
        )
        if (checkState?.first == "models") {
            Spacer(Modifier.height(8.dp))
            Text(checkState.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
private fun ServerContent(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    scope: CoroutineScope,
    checkState: Pair<String, String>?,
    onCheckState: (Pair<String, String>) -> Unit,
) {
    SectionLabel("OpenCode в локальной сети")
    GlassCard {
        Text(
            "127.0.0.1 — сам телефон. На ПК: opencode service set hostname 0.0.0.0 и opencode service restart.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Адрес ПК: http://<IP-ПК>:28894",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        CapsuleButton(
            "Найти сервер в LAN",
            onClick = { scope.launch { viewModel.discoverServer { onCheckState("lan" to it) } } },
            modifier = Modifier.fillMaxWidth(),
        )
        if (checkState?.first == "lan") {
            Spacer(Modifier.height(8.dp))
            Text(checkState.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
    Spacer(Modifier.height(12.dp))
    SectionLabel("Проверка")
    GlassCard {
        CapsuleButton(
            "Проверить подключение",
            onClick = { scope.launch { onCheckState("check" to viewModel.check("opencode")) } },
            modifier = Modifier.fillMaxWidth(),
        )
        if (checkState?.first == "check") {
            Spacer(Modifier.height(8.dp))
            Text(checkState.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppearanceContent(settings: AppSettings, viewModel: SettingsViewModel) {
    SectionLabel("Тема")
    GlassCard {
        val dark = settings.themeMode != "LIGHT"
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CapsuleButton("Тёмная", onClick = { viewModel.setThemeMode("DARK") }, primary = dark, modifier = Modifier.weight(1f))
            CapsuleButton("Светлая", onClick = { viewModel.setThemeMode("LIGHT") }, primary = !dark, modifier = Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(12.dp))
    SectionLabel("Палитра")
    GlassCard {
        val palettes = if (settings.themeMode != "LIGHT") DARK_PALETTES else LIGHT_PALETTES
        for (group in listOf("Бизнес", "Пастель")) {
            Text(group, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                palettes.filter { it.group == group }.forEach { p ->
                    PaletteSwatch(p, p.id == settings.paletteId, { viewModel.setPalette(p.id) }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    Spacer(Modifier.height(12.dp))
    SectionLabel("Типографика и поверхность")
    GlassCard {
        Text("Размер текста: ${(settings.fontScale * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Slider(value = settings.fontScale, onValueChange = viewModel::setFontScale, valueRange = 0.85f..1.25f)
        Text("Скругления: ${settings.cornerRadius} dp", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Slider(value = settings.cornerRadius.toFloat(), onValueChange = { viewModel.setCornerRadius(it.toInt()) }, valueRange = 12f..32f)
        ToggleRow("Матовое стекло", "Полупрозрачные поверхности", settings.glassEffect, viewModel::setGlassEffect)
        ToggleRow("Уменьшить анимации", "Плавность вместо движения", settings.reduceMotion, viewModel::setReduceMotion)
    }
}

@Composable
private fun LanguageContent(settings: AppSettings, viewModel: SettingsViewModel) {
    SectionLabel("Язык интерфейса")
    GlassCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("ru" to "Русский", "en" to "English", "system" to "Авто").forEach { (code, label) ->
                CapsuleButton(label, onClick = { viewModel.setLanguage(code) }, primary = settings.language == code, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Применяется к форматам дат и интерфейсу после перезапуска.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CameraContent(settings: AppSettings, viewModel: SettingsViewModel) {
    SectionLabel("Съёмка и превью")
    GlassCard {
        ToggleRow("Сетка", "Линии композиции", settings.cameraGrid, viewModel::setCameraGrid)
        ToggleRow("Звук затвора", "Щелчок при съёмке", settings.cameraSound, viewModel::setCameraSound)
        ToggleRow("Зеркало", "Показывать как селфи", settings.cameraMirror, viewModel::setCameraMirror)
    }
    Spacer(Modifier.height(12.dp))
    SectionLabel("Распознавание")
    GlassCard {
        Text("Debounce OCR: ${settings.debounceMs} мс", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Slider(value = settings.debounceMs.toFloat(), onValueChange = { viewModel.setDebounce(it.toInt()) }, valueRange = 200f..1200f)
    }
}

@Composable
private fun PromptContent(settings: AppSettings, viewModel: SettingsViewModel) {
    SectionLabel("Инструкция для ИИ")
    GlassCard {
        PromptField(value = settings.systemPrompt, onValueChange = viewModel::setPrompt)
        Spacer(Modifier.height(10.dp))
        CapsuleButton("Сбросить промпт", onClick = { viewModel.setPrompt(com.mursis.anycalc.data.DEFAULT_PROMPT) }, modifier = Modifier.fillMaxWidth(), primary = false)
    }
}

@Composable
private fun GeneralContent(settings: AppSettings, viewModel: SettingsViewModel) {
    SectionLabel("Поведение")
    GlassCard {
        ToggleRow("Тактильная отдача", "Отклик при действиях", settings.haptics, viewModel::setHaptics)
        ToggleRow("Уменьшить анимации", "Меньше движения", settings.reduceMotion, viewModel::setReduceMotion)
        ToggleRow("Автосохранение", "Сохранять ответы в историю", settings.autoSave, viewModel::setAutoSave)
    }
    Spacer(Modifier.height(20.dp))
    Text("AnyCalc 1.0 · локальные вычисления и ИИ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PaletteSwatch(palette: Palette, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
        Text(palette.name, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp), color = palette.text, maxLines = 1)
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

    GlassCard(radius = 22, padding = 14, modifier = Modifier.graphicsLayer { translationY = dragY }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(provider.id) {
                        detectDragGestures(
                            onDrag = { change, amount -> change.consume(); dragY += amount.y },
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
                Image(painterResource(R.drawable.ic_settings), contentDescription = "Перетащите", modifier = Modifier.size(20.dp).alpha(0.7f))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickableNoRipple(onToggleExpand)) {
                Text(provider.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(
                    if (provider.kind == ProviderKind.OPENCODE) "OpenCode Server" else "OpenAI API",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Switch(
                checked = provider.enabled,
                onCheckedChange = { onUpdate(url, model, it) },
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            )
        }
        if (expanded) {
            Spacer(Modifier.height(14.dp))
            PlainField(url, { url = it; onUpdate(url, model, provider.enabled) }, "Base URL")
            Spacer(Modifier.height(10.dp))
            PlainField(model, { model = it; onUpdate(url, model, provider.enabled) }, "Модель")
            if (provider.kind == ProviderKind.OPENAI) {
                Spacer(Modifier.height(10.dp))
                KeyField(apiKey, { apiKey = it; onKey(it) }, if (provider.hasKey) "Ключ сохранён" else "API-ключ")
                Spacer(Modifier.height(10.dp))
                PlainField(vision, { vision = it; onVisionModel(vision) }, "Vision-модель")
            } else {
                Spacer(Modifier.height(10.dp))
                PlainField(authUser, { authUser = it; onAuth(authUser, authPassword) }, "Логин")
                Spacer(Modifier.height(10.dp))
                KeyField(authPassword, { authPassword = it; onAuth(authUser, authPassword) }, if (provider.hasAuthPassword) "Пароль сохранён" else "Пароль")
            }
            Spacer(Modifier.height(12.dp))
            CapsuleButton("Проверить подключение", onClick = onCheck, modifier = Modifier.fillMaxWidth(), primary = false)
            if (checkResult != null) {
                Spacer(Modifier.height(8.dp))
                Text(checkResult, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
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
