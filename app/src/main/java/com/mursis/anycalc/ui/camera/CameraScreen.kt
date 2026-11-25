package com.mursis.anycalc.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mursis.anycalc.R
import com.mursis.anycalc.camera.TaskAnalyzer
import com.mursis.anycalc.ui.components.CapsuleButton
import com.mursis.anycalc.ui.components.FloatingCapsule
import com.mursis.anycalc.ui.components.GlassCard
import com.mursis.anycalc.ui.components.ShimmerLines
import com.mursis.anycalc.ui.components.motionMs
import com.mursis.anycalc.ui.solution.SolutionSheet
import com.mursis.anycalc.ui.theme.BusinessBlack
import com.mursis.anycalc.ui.theme.Champagne
import com.mursis.anycalc.ui.theme.Glass
import com.mursis.anycalc.ui.theme.Hairline
import com.mursis.anycalc.ui.theme.HairlineStrong
import com.mursis.anycalc.ui.theme.LocalReduceMotion
import com.mursis.anycalc.ui.theme.Platinum
import com.mursis.anycalc.ui.theme.PrimaryText
import com.mursis.anycalc.ui.theme.SecondaryText
import com.mursis.anycalc.ui.theme.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CameraRoute(
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
    }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    val analyzer = remember(viewModel) {
        TaskAnalyzer(viewModel.ocrEngine(), { viewModel.currentDebounce() }, viewModel::onRecognized)
    }
    val imageCapture = remember { ImageCapture.Builder().build() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) loadBitmap(context, uri)?.let { viewModel.onImage(it) }
    }

    fun takePhoto() {
        val file = File(context.cacheDir, "anycalc_capture.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    BitmapFactory.decodeFile(file.absolutePath)?.let { viewModel.onImage(it) }
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModel.onImageError(exception.message ?: "Не удалось снять фото")
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(BusinessBlack)) {
        if (granted) {
            CameraPreview(
                analyzer = analyzer,
                imageCapture = imageCapture,
                torch = ui.torch,
                onCamera = { camera = it },
                modifier = Modifier.fillMaxSize(),
            )
            DetectionOverlay(ui, Modifier.fillMaxSize())
        } else {
            PermissionPlaceholder(onRequest = { launcher.launch(Manifest.permission.CAMERA) })
        }

        // Верхняя панель
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            BusinessBlack.copy(alpha = 0.72f),
                            androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    ),
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("AnyCalc", style = MaterialTheme.typography.titleLarge, color = PrimaryText)
                Text("распознай · реши · пойми", style = MaterialTheme.typography.bodySmall)
            }
            StatusPill(active = ui.solution != null && !ui.isBusy)
            Spacer(Modifier.width(10.dp))
            GlassPill(
                icon = if (ui.torch) R.drawable.ic_flash_on else R.drawable.ic_flash_off,
                onClick = { viewModel.toggleTorch(); haptic(context) },
            )
        }

        // Ответ / загрузка
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val scale by animateFloatAsState(
                targetValue = if (ui.solution != null) 1f else 0.94f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 260f),
                label = "answer-scale",
            )
            AnimatedVisibility(
                visible = ui.isBusy && ui.solution == null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                GlassCard(radius = 26) { ShimmerLines(3) }
            }
            AnimatedVisibility(
                visible = ui.solution != null && !ui.isBusy,
                enter = fadeIn() + slideInVertically { it / 4 },
                exit = fadeOut() + slideOutVertically { it / 4 },
            ) {
                ui.solution?.let { solution ->
                    FloatingCapsule(
                        text = solution.short,
                        modifier = Modifier
                            .scale(scale)
                            .clickable { viewModel.openSheet(); haptic(context) },
                    )
                }
            }
            if (ui.ocrEngine.isNotBlank()) {
                Text("OCR: ${ui.ocrEngine}", style = MaterialTheme.typography.labelMedium, color = Champagne)
            }
            if (ui.status.isNotBlank() && ui.solution == null && !ui.isBusy) {
                Text(
                    ui.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Glass.copy(alpha = 0.86f))
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }

        // Нижняя панель
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            AnimatedVisibility(
                visible = ui.showManual,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut(),
            ) {
                GlassCard(modifier = Modifier.padding(bottom = 12.dp), radius = 26) {
                    Text("Ввести задачу вручную", style = MaterialTheme.typography.labelMedium, color = Champagne)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ui.manualText,
                        onValueChange = viewModel::onManualChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Например: 2x^2 - 8x + 6 = 0", color = SecondaryText) },
                        minLines = 1,
                        maxLines = 3,
                        shape = RoundedCornerShape(18.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.submit(ui.manualText) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedBorderColor = HairlineStrong,
                            unfocusedBorderColor = Hairline,
                            focusedContainerColor = BusinessBlack,
                            unfocusedContainerColor = BusinessBlack,
                            cursorColor = Platinum,
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    CapsuleButton("Решить", onClick = { viewModel.submit(ui.manualText); haptic(context) }, modifier = Modifier.fillMaxWidth())
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Glass.copy(alpha = 0.88f))
                    .border(1.dp, Hairline, RoundedCornerShape(28.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlPill(R.drawable.ic_camera, "Фото", false) { takePhoto() }
                ControlPill(R.drawable.ic_gallery, "Галерея", false) {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }
                ControlPill(R.drawable.ic_keyboard, "Ввод", ui.showManual) { viewModel.toggleManual() }
                ControlPill(R.drawable.ic_sparkle, "Локально", false) { viewModel.solveLocalNow(); haptic(context) }
                ControlPill(R.drawable.ic_clear, "Очистить", false) { viewModel.clear() }
                ControlPill(R.drawable.ic_history, "История", false) { onHistory() }
                ControlPill(R.drawable.ic_settings, "Настройки", false) { onSettings() }
            }
        }

        ui.error?.let { error ->
            FloatingCapsule(
                text = error,
                isError = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 72.dp)
                    .clickable { viewModel.clearError() },
            )
        }

        SolutionSheet(
            solution = if (ui.showSheet) ui.solution else null,
            prompt = ui.promptDraft,
            onPromptChange = viewModel::onPromptChange,
            onDismiss = viewModel::closeSheet,
            onRecalculate = { viewModel.recalculate(); haptic(context) },
            onResetPrompt = viewModel::resetPrompt,
            onSave = viewModel::saveCurrent,
            reduceMotion = LocalReduceMotion.current,
        )
    }
    DisposableEffect(Unit) { onDispose { camera?.cameraControl?.enableTorch(false) } }
}

@Composable
private fun CameraPreview(
    analyzer: TaskAnalyzer,
    imageCapture: ImageCapture,
    torch: Boolean,
    onCamera: (Camera) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    AndroidView(factory = { previewView }, modifier = modifier)

    LaunchedEffect(Unit) {
        val provider = withContext(Dispatchers.IO) { ProcessCameraProvider.getInstance(context).get() }
        val preview = Preview.Builder().build().apply { surfaceProvider = previewView.surfaceProvider }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(previewView.display.rotation)
            .build()
        analysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer)
        provider.unbindAll()
        val camera = provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis,
            imageCapture,
        )
        boundCamera = camera
        onCamera(camera)
    }

    LaunchedEffect(torch) {
        boundCamera?.cameraControl?.enableTorch(torch)
    }
}

@Composable
private fun DetectionOverlay(state: CameraUiState, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        state.boxes.forEach { box ->
            val left = box.left * w
            val top = box.top * h
            val right = box.right * w
            val bottom = box.bottom * h
            drawRoundRect(
                color = Champagne.copy(alpha = 0.85f),
                topLeft = Offset(left - 8f, top - 8f),
                size = Size(right - left + 16f, bottom - top + 16f),
                cornerRadius = CornerRadius(20.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
        val glow = Brush.radialGradient(
            colors = listOf(Champagne.copy(alpha = 0.08f), androidx.compose.ui.graphics.Color.Transparent),
            center = Offset(w / 2, h / 2),
            radius = minOf(w, h) * 0.55f,
        )
        drawRect(glow)
    }
}

@Composable
private fun PermissionPlaceholder(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassCard {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Доступ к камере", style = MaterialTheme.typography.titleLarge, color = PrimaryText)
                Spacer(Modifier.height(8.dp))
                Text("Нужен для live-распознавания задач", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(18.dp))
                CapsuleButton("Разрешить", onClick = onRequest)
            }
        }
    }
}

@Composable
private fun StatusPill(active: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Glass)
            .border(1.dp, Hairline, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(if (active) Success else SecondaryText))
        Spacer(Modifier.width(7.dp))
        Text(if (active) "LIVE" else "READY", style = MaterialTheme.typography.labelMedium, color = if (active) Success else SecondaryText)
    }
}

@Composable
private fun GlassPill(icon: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(50))
            .background(Glass)
            .border(1.dp, Hairline, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = "Вспышка", tint = PrimaryText, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ControlPill(icon: Int, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) Platinum.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = if (active) Platinum else SecondaryText,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(label, color = if (active) Platinum else SecondaryText, style = MaterialTheme.typography.labelMedium)
    }
}

private fun loadBitmap(context: Context, uri: Uri): Bitmap? = try {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    var bitmap = ImageDecoder.decodeBitmap(source)
    val input = context.contentResolver.openInputStream(uri)
    if (input != null) {
        val exif = ExifInterface(input)
        val degrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        input.close()
        if (degrees != 0f) {
            val matrix = Matrix().apply { postRotate(degrees) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }
    if (bitmap.width > 2400 || bitmap.height > 2400) {
        val scale = 2400f / maxOf(bitmap.width, bitmap.height)
        Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    } else bitmap
} catch (_: Exception) {
    null
}

@SuppressLint("MissingPermission")
private fun haptic(context: Context) {
    val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(18, 90))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(18)
    }
}
