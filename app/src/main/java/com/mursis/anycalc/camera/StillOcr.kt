package com.mursis.anycalc.camera

import android.graphics.Bitmap
import com.google.mlkit.vision.text.Text
import com.mursis.anycalc.ai.AiRepository
import com.mursis.anycalc.data.AppSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

enum class OcrMode { AUTO, TESSERACT, MLKIT, AI }

data class StillOcrResult(
    val text: String,
    val boxes: List<NormalizedBox> = emptyList(),
    val engine: String,
    val confidence: Float,
)

/** Распознавание неподвижного изображения (камера-снапшот или галерея) с цепочкой движков. */
@Singleton
class StillOcr @Inject constructor(
    private val tesseract: TesseractEngine,
    private val mlKit: OcrEngine,
    private val ai: AiRepository,
) {
    suspend fun recognize(
        bitmap: Bitmap,
        settings: AppSettings,
        onStatus: (String) -> Unit,
    ): StillOcrResult {
        val mode = runCatching { OcrMode.valueOf(settings.ocrMode) }.getOrDefault(OcrMode.AUTO)
        val order = when (mode) {
            OcrMode.AUTO -> listOf(OcrMode.TESSERACT, OcrMode.MLKIT, OcrMode.AI)
            OcrMode.TESSERACT -> listOf(OcrMode.TESSERACT, OcrMode.MLKIT, OcrMode.AI)
            OcrMode.MLKIT -> listOf(OcrMode.MLKIT, OcrMode.TESSERACT, OcrMode.AI)
            OcrMode.AI -> listOf(OcrMode.AI, OcrMode.MLKIT, OcrMode.TESSERACT)
        }
        var best: StillOcrResult? = null
        for (engine in order) {
            onStatus("OCR: ${engine.name.lowercase()}…")
            val candidate: StillOcrResult? = when (engine) {
                OcrMode.TESSERACT -> {
                    val r = tesseract.recognize(bitmap)
                    if (usable(r.text) && r.confidence >= 0.35f) {
                        StillOcrResult(r.text.trim(), emptyList(), "Tesseract", r.confidence)
                    } else null
                }
                OcrMode.MLKIT -> {
                    val text = mlKitBitmap(bitmap)
                    if (text != null && usable(text.text)) {
                        val t = text.text.trim()
                        val w = bitmap.width.coerceAtLeast(1)
                        val h = bitmap.height.coerceAtLeast(1)
                        val boxes = text.textBlocks.mapNotNull { it.boundingBox }.take(8).map { r ->
                            NormalizedBox(
                                r.left / w.toFloat(),
                                r.top / h.toFloat(),
                                r.right / w.toFloat(),
                                r.bottom / h.toFloat(),
                            )
                        }
                        StillOcrResult(t, boxes, "ML Kit", 0.8f)
                    } else null
                }
                OcrMode.AI -> {
                    if (best != null) null else {
                        val r = ai.ocrWithVision(bitmap, settings, onStatus)
                        if (usable(r.text)) StillOcrResult(r.text.trim(), emptyList(), "AI vision", r.confidence) else null
                    }
                }
                else -> null
            }
            if (candidate != null && (best == null || score(candidate) > score(best!!))) best = candidate
            // Tesseract читает печатные формулы лучше ML Kit — в AUTO доверяем ему первым.
            if (engine == OcrMode.TESSERACT && candidate != null) break
        }
        return best ?: StillOcrResult("", emptyList(), "—", 0f)
    }

    private fun usable(text: String): Boolean {
        val t = text.trim()
        return t.length >= 3 && t.any { it.isLetterOrDigit() } && t.count { it in "!@#$%^&*<>[]{}~`" } <= t.length / 3
    }

    private fun score(r: StillOcrResult): Float =
        r.confidence * 2f + minOf(r.text.length, 200).toFloat() / 100f + if (r.text.any { it.isDigit() }) 1f else 0f


    private suspend fun mlKitBitmap(bitmap: Bitmap): Text? = suspendCancellableCoroutine { cont ->
        mlKit.processBitmap(
            bitmap,
            onResult = { if (cont.isActive) cont.resume(it) },
            onError = { if (cont.isActive) cont.resume(null) },
        )
    }
}
