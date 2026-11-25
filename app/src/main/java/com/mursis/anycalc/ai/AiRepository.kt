package com.mursis.anycalc.ai

import android.graphics.Bitmap
import android.util.Base64
import com.mursis.anycalc.calc.LocalCalculator
import com.mursis.anycalc.camera.OcrText
import com.mursis.anycalc.data.AppSettings
import com.mursis.anycalc.data.ProviderConfig
import com.mursis.anycalc.data.ProviderKind
import com.mursis.anycalc.data.ScanDao
import com.mursis.anycalc.data.SecureKeyStore
import com.mursis.anycalc.model.Solution
import com.mursis.anycalc.model.SolutionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val client: OkHttpClient,
    private val keys: SecureKeyStore,
    private val dao: ScanDao,
    private val local: LocalCalculator,
) {
    /** Локальный расчёт всегда пробуем первым — провайдеры не нужны. */
    fun solveLocal(text: String): Solution? = local.solve(text.trim())

    suspend fun solve(text: String, settings: AppSettings, onStatus: (String) -> Unit): Solution =
        withContext(Dispatchers.IO) {
            val started = System.currentTimeMillis()
            val task = text.trim()
            if (task.isBlank()) return@withContext errorSolution("Пустая задача")

            local.solve(task)?.let { return@withContext it }

            dao.findByText(task)?.let { cached ->
                if (cached.short.isNotBlank()) {
                    return@withContext Solution(
                        cached.short,
                        cached.detailed,
                        cached.confidence,
                        "Кэш",
                        SolutionSource.CACHE,
                        System.currentTimeMillis() - started,
                    )
                }
            }

            var lastError = ""
            for (provider in settings.orderedProviders) {
                if (!provider.enabled || provider.baseUrl.isBlank()) continue
                if (provider.kind != ProviderKind.OPENCODE && keys.getKey(provider.id).isBlank()) continue

                onStatus("Подключаюсь к ${provider.name}…")
                try {
                    val content = when (provider.kind) {
                        ProviderKind.OPENCODE -> askOpenCode(provider, settings.systemPrompt, task, settings, null)
                        else -> askOpenAi(provider, provider.model, settings.systemPrompt, task, settings)
                    }
                    val parsed = parseSolution(content, provider.name, started)
                    onStatus("Ответ получен")
                    return@withContext parsed
                } catch (e: Exception) {
                    lastError = e.message?.take(160).orEmpty().ifBlank { e::class.java.simpleName }
                }
            }

            errorSolution(
                if (lastError.isBlank()) "Нет доступных провайдеров. Проверьте ключи в Настройках."
                else "Провайдеры недоступны: $lastError",
                started,
            )
        }

    /** Проверка именно OpenCode Server: /api/session + список моделей. */
    suspend fun checkConnection(provider: ProviderConfig, settings: AppSettings): String =
        withContext(Dispatchers.IO) {
            if (provider.baseUrl.isBlank()) return@withContext "Не задан base URL"
            if (provider.kind == ProviderKind.OPENCODE) {
                val password = keys.getKey("auth_${provider.id}")
                if (password.isBlank()) return@withContext "Введите пароль OpenCode Server"
                val url = provider.baseUrl.trimEnd('/') + "/api/session"
                return@withContext try {
                    val request = applyAuth(Request.Builder().url(url).get(), provider, settings).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            return@withContext "HTTP ${response.code} · ${response.message}"
                        }
                        val count = loadOpenCodeModels(provider, settings).size
                        "OK · HTTP ${response.code} · моделей: $count"
                    }
                } catch (e: Exception) {
                    "Ошибка: ${e.message?.take(100).orEmpty()}"
                }
            }
            val url = provider.baseUrl.trimEnd('/') + "/models"
            val key = keys.getKey(provider.id)
            if (key.isBlank()) return@withContext "Нет API-ключа"
            try {
                val request = Request.Builder().url(url).header("Authorization", "Bearer $key").get().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) "OK · HTTP ${response.code}" else "HTTP ${response.code} · ${response.message}"
                }
            } catch (e: Exception) {
                "Ошибка: ${e.message?.take(100).orEmpty()}"
            }
        }

    /** Автозагрузка всех моделей, доступных на OpenCode Server. */
    suspend fun loadOpenCodeModels(provider: ProviderConfig, settings: AppSettings): List<String> =
        withContext(Dispatchers.IO) {
            val url = provider.baseUrl.trimEnd('/') + "/api/model"
            val request = applyAuth(Request.Builder().url(url).get(), provider, settings).build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val root = JSONObject(response.body?.string().orEmpty())
                    val arr = root.optJSONArray("data") ?: return@withContext emptyList()
                    (0 until arr.length()).mapNotNull { i ->
                        val o = arr.optJSONObject(i) ?: return@mapNotNull null
                        o.optString("id").ifBlank { null }
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    /** OCR через vision-модель: изображение уходит выбранному провайдеру. */
    suspend fun ocrWithVision(bitmap: Bitmap, settings: AppSettings, onStatus: (String) -> Unit): OcrText =
        withContext(Dispatchers.IO) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val dataUrl = "data:image/jpeg;base64," +
                Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            val prompt = "Распознай текст задачи на изображении. Сохрани цифры, формулы и знаки. " +
                "Верни ТОЛЬКО JSON: {\"text\":\"...\",\"confidence\":0.0}"

            var lastError = ""
            for (provider in settings.orderedProviders) {
                if (!provider.enabled || provider.baseUrl.isBlank()) continue
                if (provider.kind != ProviderKind.OPENCODE && keys.getKey(provider.id).isBlank()) continue
                onStatus("OCR через ${provider.name}…")
                try {
                    val content = when (provider.kind) {
                        ProviderKind.OPENCODE -> askOpenCode(provider, prompt, dataUrl, settings, dataUrl)
                        else -> {
                            val model = provider.visionModel.ifBlank { provider.model }
                            askOpenAiVision(provider, model, prompt, dataUrl, settings)
                        }
                    }
                    val cleaned = content.replace(Regex("^```(?:json)?\\s*", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("\\s*```$"), "").trim()
                    val start = cleaned.indexOf('{')
                    val end = cleaned.lastIndexOf('}')
                    if (start >= 0 && end > start) {
                        val obj = JSONObject(cleaned.substring(start, end + 1))
                        val text = obj.optString("text").ifBlank { obj.optString("short") }
                        if (text.isNotBlank()) {
                            return@withContext OcrText(text.trim(), obj.optDouble("confidence", 0.8).toFloat().coerceIn(0f, 1f))
                        }
                    }
                    if (cleaned.isNotBlank()) return@withContext OcrText(cleaned.take(4000), 0.6f)
                } catch (e: Exception) {
                    lastError = e.message?.take(120).orEmpty()
                }
            }
            onStatus(if (lastError.isBlank()) "AI OCR недоступен" else "AI OCR: $lastError")
            OcrText("", 0f)
        }

    private fun applyAuth(builder: Request.Builder, provider: ProviderConfig, settings: AppSettings): Request.Builder {
        if (provider.kind == ProviderKind.OPENCODE) {
            if (settings.providers[provider.id]?.hasAuthPassword == true) {
                val pass = keys.getKey("auth_${provider.id}")
                val user = settings.providers[provider.id]?.authUser?.ifBlank { "opencode" } ?: "opencode"
                val token = Base64.encodeToString("$user:$pass".toByteArray(), Base64.NO_WRAP)
                builder.header("Authorization", "Basic $token")
            }
        } else {
            val key = keys.getKey(provider.id)
            if (key.isNotBlank()) builder.header("Authorization", "Bearer $key")
        }
        return builder
    }

    private fun askOpenAi(
        provider: ProviderConfig,
        model: String,
        systemPrompt: String,
        task: String,
        settings: AppSettings,
    ): String {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
            .put(JSONObject().put("role", "user").put("content", task))
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.2)
            .toString()
        val request = applyAuth(
            Request.Builder()
                .url(provider.baseUrl.trimEnd('/') + "/chat/completions")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType())),
            provider,
            settings,
        ).build()
        return client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}: ${raw.take(120)}")
            val choices = JSONObject(raw).optJSONArray("choices") ?: throw IllegalStateException("Нет choices")
            choices.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
                .ifBlank { throw IllegalStateException("Пустой content") }
        }
    }

    private fun askOpenAiVision(
        provider: ProviderConfig,
        model: String,
        prompt: String,
        dataUrl: String,
        settings: AppSettings,
    ): String {
        val content = JSONArray()
            .put(JSONObject().put("type", "text").put("text", prompt))
            .put(
                JSONObject().put("type", "image_url").put(
                    "image_url",
                    JSONObject().put("url", dataUrl),
                ),
            )
        val messages = JSONArray().put(JSONObject().put("role", "user").put("content", content))
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.1)
            .toString()
        val request = applyAuth(
            Request.Builder()
                .url(provider.baseUrl.trimEnd('/') + "/chat/completions")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType())),
            provider,
            settings,
        ).build()
        return client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}: ${raw.take(120)}")
            val choices = JSONObject(raw).optJSONArray("choices") ?: throw IllegalStateException("Нет choices")
            choices.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
                .ifBlank { throw IllegalStateException("Пустой content") }
        }
    }

    private fun askOpenCode(
        provider: ProviderConfig,
        promptText: String,
        task: String,
        settings: AppSettings,
        imageDataUrl: String?,
    ): String {
        val base = provider.baseUrl.trimEnd('/')
        val json = "application/json".toMediaType()

        val createRequest = applyAuth(
            Request.Builder()
                .url("$base/api/session")
                .header("Content-Type", "application/json")
                .post("{\"title\":\"AnyCalc\"}".toRequestBody(json)),
            provider,
            settings,
        ).build()
        val sessionId = client.newCall(createRequest).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}: ${raw.take(120)}")
            val obj = JSONObject(raw)
            val data = obj.optJSONObject("data") ?: obj
            data.optString("id").ifBlank { throw IllegalStateException("Нет id сессии") }
        }

        val promptObject = JSONObject().put("text", task)
        if (imageDataUrl != null) {
            promptObject.put(
                "files",
                JSONArray().put(
                    JSONObject()
                        .put("uri", imageDataUrl)
                        .put("source", JSONObject().put("type", "inline"))
                        .put("name", "task.jpg"),
                ),
            )
        }
        val combined = "$promptText\n\nЗАДАЧА:\n$task"
        val promptRequest = applyAuth(
            Request.Builder()
                .url("$base/api/session/$sessionId/prompt")
                .header("Content-Type", "application/json")
                .post(
                    JSONObject()
                        .put("text", combined)
                        .put("prompt", promptObject.put("text", combined))
                        .toString()
                        .toRequestBody(json),
                ),
            provider,
            settings,
        ).build()
        client.newCall(promptRequest).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}: ${raw.take(120)}")
        }

        val messagesRequest = applyAuth(
            Request.Builder().url("$base/api/session/$sessionId/message?limit=10&order=desc").get(),
            provider,
            settings,
        ).build()
        return client.newCall(messagesRequest).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}: ${raw.take(120)}")
            val root = JSONObject(raw)
            val list = root.optJSONArray("data") ?: root.optJSONArray("messages")
                ?: runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
            var result = ""
            for (i in 0 until list.length()) {
                val msg = list.optJSONObject(i) ?: continue
                if (msg.optString("role") != "assistant") continue
                val parts = msg.optJSONArray("parts")
                if (parts != null) {
                    val sb = StringBuilder()
                    for (j in 0 until parts.length()) {
                        val part = parts.optJSONObject(j) ?: continue
                        if (part.optString("type") == "text" || part.has("text")) sb.append(part.optString("text"))
                    }
                    result = sb.toString()
                } else {
                    result = msg.optString("content").ifBlank { msg.optString("text") }
                }
                if (result.isNotBlank()) break
            }
            result.ifBlank { throw IllegalStateException("Пустой ответ ассистента") }
        }
    }

    private fun parseSolution(content: String, provider: String, started: Long): Solution {
        val cleaned = content
            .replace(Regex("^```(?:json)?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```$"), "")
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start >= 0 && end > start) {
            try {
                val obj = JSONObject(cleaned.substring(start, end + 1))
                val short = obj.optString("short").ifBlank { obj.optString("answer") }
                val detailed = obj.optString("detailed").ifBlank { obj.optString("steps") }
                if (short.isNotBlank()) {
                    return Solution(
                        short.take(300),
                        detailed.ifBlank { short },
                        obj.optDouble("confidence", 0.8).coerceIn(0.0, 1.0).toFloat(),
                        provider,
                        SolutionSource.AI,
                        System.currentTimeMillis() - started,
                    )
                }
            } catch (_: Exception) {
            }
        }
        val lines = cleaned.lines().filter { it.isNotBlank() }
        return Solution(
            lines.firstOrNull().orEmpty().take(300).ifBlank { "Ответ получен" },
            cleaned.take(4000),
            0.6f,
            provider,
            SolutionSource.AI,
            System.currentTimeMillis() - started,
        )
    }

    private fun errorSolution(message: String, started: Long = 0) = Solution(
        "Ответ недоступен",
        message,
        0f,
        "Ошибка",
        SolutionSource.AI,
        System.currentTimeMillis() - started,
    )
}
