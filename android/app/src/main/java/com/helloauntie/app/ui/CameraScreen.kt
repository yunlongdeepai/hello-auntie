package com.helloauntie.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint as AndroidPaint
import android.graphics.YuvImage
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helloauntie.app.domain.AnalysisEngine
import com.helloauntie.app.domain.CaptureOutcome
import com.helloauntie.app.domain.FaceSampleCapture
import com.helloauntie.app.domain.FrameUiState
import com.helloauntie.app.domain.UiTrack
import com.helloauntie.app.vm.AuntieViewModel
import com.helloauntie.app.vm.PersonWithTerm
import com.helloauntie.core.model.Box
import com.helloauntie.core.state.FarNearMode
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.min

@Composable
fun CameraScreen(
    engine: AnalysisEngine,
    viewModel: AuntieViewModel,
    onAddPerson: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val frame by engine.state.collectAsStateWithLifecycle()
    val people by viewModel.people.collectAsStateWithLifecycle()
    val near = frame.tracks.any { it.mode == FarNearMode.NEAR }
    var capture by remember { mutableStateOf<FaceSampleCapture?>(null) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0C1E2E))) {
        if (hasPermission) {
            CameraPreview(engine = engine, modifier = Modifier.fillMaxSize())
        } else {
            PermissionPanel(
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        CameraOverlay(state = frame, modifier = Modifier.fillMaxSize())

        Surface(
            color = Color(0xCC0C1E2E),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (near) "近距模式 · 人脸识别" else "远距模式 · 检测 + 跟踪",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
                if (frame.demo) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "演示",
                        color = Color(0xFFFFD18A),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = {
                    when (val outcome = engine.captureFaceSample()) {
                        is CaptureOutcome.Success -> capture = outcome.sample
                        CaptureOutcome.NoFrame -> toast(context, "相机还没准备好，请稍等")
                        CaptureOutcome.NoFace -> toast(context, "没有检测到正脸，请靠近一点")
                        is CaptureOutcome.LowQuality -> toast(
                            context,
                            "人脸质量不合格：${outcome.reasons.joinToString("、")}",
                        )
                        CaptureOutcome.NoModel -> toast(
                            context,
                            "未安装 mobileface.tflite，无法提取人脸特征",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(0.62f),
            ) {
                Text("录入当前人脸")
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (frame.tracks.isEmpty()) {
                    "把手机对准亲戚：远距先发现，走近再识别"
                } else {
                    "远距只做检测与跟踪；走近约 4 m 内才识别人脸"
                },
                color = Color(0xFFD6E4EC),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(10.dp))
        }

        FloatingActionButton(
            onClick = onAddPerson,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp),
            containerColor = Color(0xFF2E7D5A),
            contentColor = Color.White,
        ) {
            Text("+", fontSize = 26.sp)
        }
    }

    capture?.let { sample ->
        AssignFaceDialog(
            sample = sample,
            people = people.filter { !it.person.isSelf },
            onAssign = { person ->
                viewModel.addFaceSample(person.person.id, sample.embedding, sample.quality)
                toast(context, "已为 ${person.person.name} 录入 1 张人脸")
                capture = null
            },
            onDismiss = { capture = null },
        )
    }
}

@Composable
private fun AssignFaceDialog(
    sample: FaceSampleCapture,
    people: List<PersonWithTerm>,
    onAssign: (PersonWithTerm) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("这张脸是谁？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(
                    bitmap = sample.thumbnail.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                )
                if (people.isEmpty()) {
                    Text("还没有可选的亲戚，请先在“亲戚”页添加。")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(people, key = { it.person.id }) { person ->
                            TextButton(
                                onClick = { onAssign(person) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("${person.term.primary} · ${person.person.name}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

private fun toast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@Composable
private fun PermissionPanel(
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color(0xFF16324F),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.padding(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("需要相机权限", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(
                "相机只用于在本机识别亲戚，画面不会上传。",
                color = Color(0xFFC8D8E4),
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = onRequest) {
                Text("允许相机")
            }
        }
    }
}

@Composable
private fun CameraPreview(
    engine: AnalysisEngine,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            provider?.unbindAll()
            executor.shutdown()
            engine.reset()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        update = { previewView ->
            if (previewView.tag != "bound") {
                previewView.tag = "bound"
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener(
                    {
                        runCatching {
                            val cameraProvider = future.get()
                            provider = cameraProvider
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            analysis.setAnalyzer(executor, AuntieAnalyzer(engine))
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis,
                            )
                        }.onFailure { previewView.tag = null }
                    },
                    ContextCompat.getMainExecutor(context),
                )
            }
        },
    )
}

@Composable
private fun CameraOverlay(
    state: FrameUiState,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (state.imageWidth <= 0 || state.imageHeight <= 0) return@Canvas
        val scale = min(
            size.width / state.imageWidth.toFloat(),
            size.height / state.imageHeight.toFloat(),
        )
        val offsetX = (size.width - state.imageWidth * scale) / 2f
        val offsetY = (size.height - state.imageHeight * scale) / 2f

        state.tracks.forEach { track ->
            val color = if (track.mode == FarNearMode.NEAR) {
                Color(0xFF2E7D5A)
            } else {
                Color(0xFF8A98A8)
            }
            drawTrackBox(track.personBox, color, scale, offsetX, offsetY, 4f)
            track.faceBox?.let {
                drawTrackBox(it, Color(0xFF39A9C8), scale, offsetX, offsetY, 3f)
            }
            drawTrackLabel(track, scale, offsetX, offsetY)
        }
    }
}

private fun DrawScope.drawTrackBox(
    box: Box,
    color: Color,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    stroke: Float,
) {
    drawRect(
        color = color,
        topLeft = Offset(offsetX + box.left * scale, offsetY + box.top * scale),
        size = Size(box.width * scale, box.height * scale),
        style = Stroke(width = stroke),
    )
}

private fun DrawScope.drawTrackLabel(
    track: UiTrack,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
) {
    val text = when (track.mode) {
        FarNearMode.FAR -> "有人（远距）"
        FarNearMode.NEAR -> when {
            track.label == "未知" -> "未知（近距）"
            track.confidence <= 0f -> track.label
            else -> "${track.label} ${(track.confidence * 100).toInt()}%"
        }
    }
    val x = offsetX + track.personBox.left * scale
    val y = offsetY + track.personBox.top * scale
    val paint = AndroidPaint().apply {
        color = android.graphics.Color.WHITE
        textSize = 34f
        isAntiAlias = true
        setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
    }
    val textWidth = paint.measureText(text)
    val labelTop = (y - 46f).coerceAtLeast(0f)
    drawRect(
        color = Color(0x99000000),
        topLeft = Offset(x, labelTop),
        size = Size(textWidth + 24f, 44f),
    )
    drawContext.canvas.nativeCanvas.drawText(text, x + 12f, labelTop + 32f, paint)
}

private class AuntieAnalyzer(
    private val engine: AnalysisEngine,
) : ImageAnalysis.Analyzer {

    private var lastFrameAt = 0L

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastFrameAt < 80) {
            image.close()
            return
        }
        lastFrameAt = now

        var raw: Bitmap? = null
        var upright: Bitmap? = null
        try {
            val bitmap = image.toBitmapFromYuv() ?: return
            raw = bitmap
            val rotated = rotate(bitmap, image.imageInfo.rotationDegrees)
            upright = rotated
            engine.analyze(rotated)
        } catch (_: Throwable) {
            // A bad frame must not kill the analyzer.
        } finally {
            if (upright !== raw && raw?.isRecycled == false) raw.recycle()
            // AnalysisEngine takes ownership of the upright bitmap and recycles it
            // on the next frame or when the camera screen is disposed.
            image.close()
        }
    }

    private fun ImageProxy.toBitmapFromYuv(): Bitmap? {
        if (planes.size < 3) return null
        val evenWidth = width and 1.inv()
        val evenHeight = height and 1.inv()
        if (evenWidth <= 0 || evenHeight <= 0) return null

        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]
        val chromaWidth = evenWidth / 2
        val chromaHeight = evenHeight / 2
        val nv21 = ByteArray(evenWidth * evenHeight + 2 * chromaWidth * chromaHeight)
        var position = 0
        for (row in 0 until evenHeight) {
            for (col in 0 until evenWidth) {
                nv21[position++] = yPlane.buffer.get(row * yPlane.rowStride + col * yPlane.pixelStride)
            }
        }
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val u = uPlane.buffer.get(row * uPlane.rowStride + col * uPlane.pixelStride)
                val v = vPlane.buffer.get(row * vPlane.rowStride + col * vPlane.pixelStride)
                nv21[position++] = v
                nv21[position++] = u
            }
        }

        val output = ByteArrayOutputStream()
        YuvImage(nv21, ImageFormat.NV21, evenWidth, evenHeight, null)
            .compressToJpeg(android.graphics.Rect(0, 0, evenWidth, evenHeight), 85, output)
        val bytes = output.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
