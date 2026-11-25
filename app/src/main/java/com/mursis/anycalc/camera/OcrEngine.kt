package com.mursis.anycalc.camera

import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton

data class NormalizedBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class Recognition(val text: String, val boxes: List<NormalizedBox>)

/** Обёртка над on-device ML Kit Text Recognition v2. */
@Singleton
class OcrEngine @Inject constructor() {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun process(image: InputImage, onResult: (Text) -> Unit, onError: (Exception) -> Unit) {
        recognizer.process(image)
            .addOnSuccessListener(onResult)
            .addOnFailureListener(onError)
    }

    fun processBitmap(bitmap: android.graphics.Bitmap, onResult: (Text) -> Unit, onError: (Exception) -> Unit) {
        process(InputImage.fromBitmap(bitmap, 0), onResult, onError)
    }

    fun close() = recognizer.close()
}

/** Анализатор кадров: debounce, KEEP_ONLY_LATEST и кэш по хэшу текста. */
class TaskAnalyzer(
    private val engine: OcrEngine,
    private val debounceMs: () -> Int,
    private val onResult: (Recognition) -> Unit,
) : ImageAnalysis.Analyzer {
    private var lastRun = 0L
    private var lastHash = ""

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastRun < debounceMs()) {
            proxy.close()
            return
        }
        val mediaImage = proxy.image
        if (mediaImage == null) {
            proxy.close()
            return
        }
        lastRun = now
        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
        val w = mediaImage.width.coerceAtLeast(1)
        val h = mediaImage.height.coerceAtLeast(1)
        engine.process(
            image,
            onResult = { text ->
                val value = text.text.trim()
                val hash = value.hashCode().toString()
                if (value.isNotBlank() && hash != lastHash) {
                    lastHash = hash
                    val boxes = text.textBlocks.mapNotNull { it.boundingBox }.take(8).map { r: Rect ->
                        NormalizedBox(
                            r.left / w.toFloat(),
                            r.top / h.toFloat(),
                            r.right / w.toFloat(),
                            r.bottom / h.toFloat(),
                        )
                    }
                    onResult(Recognition(value, boxes))
                }
                proxy.close()
            },
            onError = { proxy.close() },
        )
    }
}
