package com.mursis.anycalc.camera

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Локальный OCR на базе Tesseract LSTM (eng+rus), модели лежат в assets/tessdata. */
@Singleton
class TesseractEngine @Inject constructor(@ApplicationContext private val context: Context) {
    private val dataPath: File = File(context.filesDir, "tessdata")

    init {
        if (!dataPath.exists()) dataPath.mkdirs()
        for (lang in listOf("eng", "rus")) {
            val target = File(dataPath, "$lang.traineddata")
            if (!target.exists()) {
                context.assets.open("tessdata/$lang.traineddata").use { input ->
                    FileOutputStream(target).use { input.copyTo(it) }
                }
            }
        }
    }

    suspend fun recognize(bitmap: Bitmap): OcrText = withContext(Dispatchers.IO) {
        val api = TessBaseAPI()
        try {
            val ok = api.init(dataPath.parent, "eng+rus") ||
                api.init(dataPath.parent, "eng") ||
                api.init(dataPath.parent, "rus")
            if (!ok) {
                return@withContext OcrText("", 0f)
            }
            val compatible = if (bitmap.config == android.graphics.Bitmap.Config.ARGB_8888) {
                bitmap
            } else {
                bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
            }
            api.setImage(compatible)
            val text = api.getUTF8Text()?.trim().orEmpty()
            OcrText(text, api.meanConfidence() / 100f)
        } catch (e: Exception) {
            android.util.Log.d("TesseractEngine", "recognize failed: ${e.message}")
            OcrText("", 0f)
        } finally {
            api.end()
        }
    }
}

data class OcrText(val text: String, val confidence: Float)
