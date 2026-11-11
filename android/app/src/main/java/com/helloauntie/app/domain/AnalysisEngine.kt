package com.helloauntie.app.domain

import android.graphics.Bitmap
import com.helloauntie.app.ml.FaceDetector
import com.helloauntie.app.ml.FaceEmbedder
import com.helloauntie.app.ml.PersonDetector
import com.helloauntie.core.face.FaceMatcher
import com.helloauntie.core.face.FaceTemplate
import com.helloauntie.core.face.QualityGate
import com.helloauntie.core.model.Box
import com.helloauntie.core.model.FaceDetection
import com.helloauntie.core.state.FarNearController
import com.helloauntie.core.state.FarNearMode
import com.helloauntie.core.track.ByteTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiTrack(
    val id: Int,
    val personBox: Box,
    val faceBox: Box?,
    val mode: FarNearMode,
    val label: String,
    val confidence: Float,
    val selector: String,
)

data class FrameUiState(
    val tracks: List<UiTrack> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val demo: Boolean = false,
    val hint: String = "",
)

/**
 * Frame-level orchestration:
 *   person detection -> tracking -> per-track FAR/NEAR mode -> face recognition only in NEAR.
 *
 * The face model is not called at all in FAR mode; this is the main difference
 * from a conventional always-on face recognition pipeline.
 */
class AnalysisEngine(
    private val personDetector: PersonDetector,
    private val faceDetector: FaceDetector,
    private val faceEmbedder: FaceEmbedder?,
    private val faceMatcher: FaceMatcher,
    private val galleryProvider: () -> List<FaceTemplate>,
    private val labelProvider: (Long) -> String?,
) {
    private val tracker = ByteTracker()
    private val controllers = mutableMapOf<Int, FarNearController>()
    private val _state = MutableStateFlow(FrameUiState())
    val state: StateFlow<FrameUiState> = _state.asStateFlow()

    private var latestFrame: Bitmap? = null
    private var latestFace: FaceDetection? = null

    @Synchronized
    fun analyze(bitmap: Bitmap) {
        if (bitmap.width <= 0 || bitmap.height <= 0) return
        if (latestFrame !== bitmap) {
            latestFrame?.recycle()
            latestFrame = bitmap
        }
        latestFace = null
        val persons = personDetector.detect(bitmap)
        val tracks = tracker.update(persons)

        val activeIds = tracks.map { it.id }.toHashSet()
        controllers.keys.retainAll(activeIds)

        val uiTracks = tracks.map { track ->
            val controller = controllers.getOrPut(track.id) { FarNearController() }
            val mode = controller.update(track.heightRatio(bitmap.height))
            var faceBox: Box? = null
            var label = if (personDetector.isDemo) "演示目标" else "未知"
            var confidence = 0f
            var selector = ""

            if (mode == FarNearMode.NEAR) {
                val faces = faceDetector.detect(bitmap, track.box)
                val face = faces.maxByOrNull { it.score }
                if (face != null) {
                    faceBox = face.box
                    latestFace = face
                    val quality = QualityGate.evaluate(
                        faceWidthPx = face.box.width,
                        frameWidth = bitmap.width,
                    )
                    if (!quality.ok) {
                        label = "看不清"
                    } else {
                        val embedder = faceEmbedder
                        if (embedder != null) {
                            val embedding = embedder.embed(bitmap, face)
                            if (embedding != null) {
                                val match = faceMatcher.match(embedding, galleryProvider())
                                val matchedId = match.personId
                                if (match.accepted && matchedId != null) {
                                    label = labelProvider(matchedId) ?: "已识别"
                                    confidence = match.top1
                                }
                            }
                        }
                    }
                }
            }

            UiTrack(
                id = track.id,
                personBox = track.box,
                faceBox = faceBox,
                mode = mode,
                label = label,
                confidence = confidence,
                selector = selector,
            )
        }

        _state.value = FrameUiState(
            tracks = uiTracks,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            demo = personDetector.isDemo,
        )
    }

    /**
     * Captures a face from the latest analyzed frame for enrollment.
     * Must be called after at least one frame has produced a face in NEAR mode.
     */
    @Synchronized
    fun captureFaceSample(): CaptureOutcome {
        val frame = latestFrame
        if (frame == null || frame.isRecycled) return CaptureOutcome.NoFrame

        val face = latestFace ?: faceDetector.detect(
            frame,
            Box(0f, 0f, frame.width.toFloat(), frame.height.toFloat()),
        ).maxByOrNull { it.score } ?: return CaptureOutcome.NoFace

        val quality = QualityGate.evaluate(
            faceWidthPx = face.box.width,
            frameWidth = frame.width,
        )
        if (!quality.ok) return CaptureOutcome.LowQuality(quality.reasons)

        val embedder = faceEmbedder ?: return CaptureOutcome.NoModel
        val embedding = embedder.embed(frame, face) ?: return CaptureOutcome.NoFace
        val thumb = runCatching { thumbnail(frame, face.box) }.getOrElse {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        return CaptureOutcome.Success(
            FaceSampleCapture(
                faceBox = face.box,
                embedding = embedding,
                quality = quality.score,
                thumbnail = thumb,
            ),
        )
    }

    private fun thumbnail(frame: Bitmap, box: Box): Bitmap {
        val expanded = box.expand(0.35f).clampTo(frame.width, frame.height)
        val left = expanded.left.toInt().coerceIn(0, frame.width - 1)
        val top = expanded.top.toInt().coerceIn(0, frame.height - 1)
        val width = expanded.width.toInt().coerceIn(1, frame.width - left)
        val height = expanded.height.toInt().coerceIn(1, frame.height - top)
        val crop = Bitmap.createBitmap(frame, left, top, width, height)
        val scaled = Bitmap.createScaledBitmap(crop, 192, 192, true)
        if (scaled !== crop) crop.recycle()
        return scaled
    }

    @Synchronized
    fun reset() {
        latestFrame?.recycle()
        latestFrame = null
        latestFace = null
        controllers.clear()
        _state.value = FrameUiState(demo = personDetector.isDemo)
    }
}
