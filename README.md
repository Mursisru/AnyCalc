# AnyCalc

Android-приложение «Google Translate для задач»: камера распознаёт условие задачи, поверх него появляется короткий ответ, по тапу открывается пошаговое решение.

## Возможности

- **Live-режим** — CameraX Preview + ImageAnalysis, on-device OCR ML Kit Text Recognition v2 с bounding boxes, debounce 550 мс и `STRATEGY_KEEP_ONLY_LATEST`.
- **Оверлей** — тонкие скруглённые рамки вокруг распознанного текста и «плавающая» стеклянная капсула с ответом.
- **BottomSheet** — короткий ответ, пошаговое решение, метки провайдера/источника/уверенности, редактор системного промпта, пересчёт, сохранение.
- **Гибридный калькулятор** — арифметика, функции, квадратные уравнения, проценты и базовые формулы физики считаются локально, без сети и ключей.
- **ИИ-провайдеры** — OpenCode Server (сессии: `POST /api/session`, `POST /session/{id}/prompt`, `GET /session/{id}/message`) и OpenAI-совместимые API (Gemini, Groq, OpenRouter, DeepSeek, Custom). Приоритет меняется drag-and-drop, есть проверка подключения и автоматический переход к резервному провайдеру.
- **Безопасность** — ключи в `EncryptedSharedPreferences`, мастер-ключ в Android Keystore.
- **Хранение** — Room (история, поиск, swipe-удаление) и DataStore (настройки, промпт, приоритеты).
- **Дизайн** — business-black (#050505 / #111214 / #1C1C1E), platinum-акцент, champagne для премиальных деталей, скругления 20–28 dp, пружинные анимации 200–350 мс, поддержка Reduce Motion и тактильной отдачи.
- **Ручной ввод** — fallback, если камера недоступна или модель OCR ещё не загружена.

## Сборка

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\openjdk\jdk-21.0.8"
.\gradlew.bat testDebugUnitTest assembleDebug assembleRelease
```

APK:
- debug — `app/build/outputs/apk/debug/app-debug.apk`
- release (подписан) — `app/build/outputs/apk/release/app-release.apk`

Ключ для release-сборки: `%USERPROFILE%\.android\anycalc-release.jks`, параметры в `keystore.properties` (файл в `.gitignore`). Пароль keystore: `anycalc2026`.

## Распознавание текста и локальные вычисления

- Цепочка OCR для фото/галереи в режиме `AUTO`: **Tesseract (eng+rus, LSTM)** → **ML Kit Text Recognition** → **AI-vision через выбранного провайдера**. Tesseract стабильнее читает печатные формулы; локальный калькулятор нормализует типичные ошибки OCR (`›‹` → `*`, `—:-` → `/`, `—` → `-`).
- Режим выбирается в «Настройки → Распознавание текста (OCR)»: `AUTO`, `TESSERACT`, `MLKIT`, `AI`.
- Live-камера использует ML Kit с debounce и кэшем; по тапу на капсулу открывается решение.
- Кнопка «Локально» на панели камеры запускает расширенный калькулятор **без сети и провайдеров**: арифметика, функции, квадратные уравнения, системы 2×2, проценты, производные и интегралы многочленов, перевод единиц, физические формулы и текстовые задачи про скорость/время.
- Кнопки «Фото» (CameraX ImageCapture) и «Галерея» (Android Photo Picker) распознают выбранное изображение и запускают решение.

## OpenCode Server

- URL по умолчанию: `http://127.0.0.1:28894` (сервер OpenCode Desktop). Пароль **не обязателен**: приложение сначала подключается без авторизации и использует Basic auth только если сервер ответил 401 и пароль задан в Настройках (логин `opencode`, хранится в Keystore).
- «Проверить подключение» обращается к `/api/session`, а список моделей грузится из `/api/model` кнопкой «Обновить список моделей OpenCode».
### Реальный телефон в локальной сети

`127.0.0.1` на телефоне — это сам телефон, поэтому по умолчанию подключение невозможно. Рабочая схема:

1. На ПК разрешить прослушивание всех интерфейсов (настройка Desktop CLI сохраняется):
   ```powershell
   opencode service set hostname 0.0.0.0
   opencode service restart
   opencode service status   # должно быть http://0.0.0.0:28894
   ```
2. В Windows Firewall должно быть разрешено входящее подключение для `opencode-cli.exe` (на этой машине правило уже есть; профиль сети Public).
3. В приложении: **Настройки → OpenCode Server в локальной сети → Найти сервер в LAN**. Приложение сканирует подсеть телефона на порт 28894 и само подставляет адрес ПК. Либо вручную укажите `http://<IP-ПК>:28894` в поле Base URL. На текущей машине IP — `10.15.1.11`, т.е. адрес `http://10.15.1.11:28894` (проверено: сервер отвечает 401 без пароля и 200 с Basic auth).
4. Для эмулятора по-прежнему работает `adb reverse tcp:28894 tcp:28894` с адресом `127.0.0.1:28894`.

Сервер с паролем отвечает 401 — сканер считает его найденным; Basic auth применяется, если пароль сохранён в приложении. Сервер без пароля отвечает 200 и работает сразу.
- Промпт отправляется в формате, совместимом со старым и новым API: `{"text":..., "prompt":{"text":...}}`.

## Провайдер NVIDIA

`https://integrate.api.nvidia.com/v1`, ключ — с [build.nvidia.com](https://build.nvidia.com/). Текстовая модель по умолчанию `nvidia/nemotron-3.5-lightning-30b-a3b`, vision-модель для OCR — `nvidia/nemotron-3.5-super-vl-120b-a12b`. Обе переопределяются в Настройках.

## Первый запуск

1. Пройдите онбординг.
2. Разрешите камеру. При первом запуске ML Kit загрузит модель OCR через Google Play services (нужен интернет).
3. Для ИИ откройте «Настройки» → введите ключ и проверьте подключение. Для эмулятора OpenCode Server уже настроен на `http://10.0.2.2:4096` (сервер должен быть запущен на хосте с `opencode serve`).
4. Без ключей и сервера приложение всё равно решает простые задачи локально.

### Подключение к OpenCode Server из эмулятора

По умолчанию используется `http://10.0.2.2:4096`. Если Windows Firewall блокирует входящие подключения из эмулятора, пробросьте порт через adb и укажите `http://127.0.0.1:4096` в настройках провайдера:

```powershell
adb reverse tcp:4096 tcp:4096
```

Сервер нужно поднимать с `opencode serve --port 4096 --hostname 0.0.0.0`. Адаптер отправляет промпт в формате, совместимом и со старым API (`text`), и с новым (`prompt: {text}`).

## Архитектура

```
ui/          Compose-экраны (camera, solution, history, settings, onboarding) и дизайн-система
camera/      OcrEngine (ML Kit) + TaskAnalyzer (debounce, hash-кэш)
ai/          AiRepository — OpenCode и OpenAI-совместимые провайдеры, fallback, парсинг JSON
calc/        LocalCalculator — безопасный парсер выражений, уравнения, физика
data/        Room, DataStore, EncryptedSharedPreferences
di/          Hilt-модуль
```

MVVM + Coroutines/Flow + Hilt. Ответ ИИ ожидается в формате `{"short": "...", "detailed": "...", "confidence": 0.0}`.

## Интерфейс

- Настройки разделены на: **Провайдеры**, **OCR**, **OpenCode Server в LAN**, **Оформление**, **Язык**, **Камера**, **Промпт**, **Общие**.
- Оформление: тёмная/светлая тема, по 8 палитр в каждой (4 бизнес + 4 пастель), масштаб текста, скругления, матовое стекло, Reduce Motion.
- Камера: язычок сетки, звук затвора, зеркало, debounce OCR.
- Нижняя панель камеры содержит 4 действия: «Галерея», «Вручную», «История», «Настройки». Кнопка фото — белая капсула с ободком над панелью; после съёмки фото показывается вместо live-превью, а крестик слева сверху возвращает в live-режим.
- Прокручиваемые страницы имеют плавное растворение Upper/lower edge (24 dp) вместо резкого обрезания.

Тестовое фото с задачей с доски: `tests/photos/chalkboard_task.jpg` (ожидаемый ответ `17`).

## Проверенная связка

AGP 9.5.2 · Gradle 9.9.2 (wrapper 9.6.0) · Kotlin/Compose plugin 2.4.21 · KSP 2.3.14 · Hilt 2.60.1 · Compose BOM 2026.11.01 · CameraX 1.7.0 · ML Kit text-recognition 16.0.1 · Tesseract (tess-two 9.1.0, `tessdata_fast` eng+rus) · Room 2.8.5 · minSdk 26 · compileSdk 37.2.

Иконки созданы в Figma (`zdUekDOd7pNiaPodpeXfwb`): секция `AnyCalc UI Icons` (13 line-иконок, 24×24, stroke 1.8) и бренд-символ Ghost, использованный как launcher foreground и в онбординге. SVG/pathData экспортированы в `app/src/main/res/drawable/ic_*.xml`.

Тестовые фото: `tests/photos/wikimedia_equations.png` (CC BY-SA 4.0, Vihaanapple, Wikimedia Commons) и `tests/photos/synthetic_task.png` (синтетическое, `2x^2 - 8x + 6 = 0`).
