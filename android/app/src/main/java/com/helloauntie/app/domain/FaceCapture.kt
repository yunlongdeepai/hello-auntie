package com.helloauntie.app.domain

import android.graphics.Bitmap
import com.helloauntie.core.model.Box

data class FaceSampleCapture(
    val faceBox: Box,
    val embedding: FloatArray,
    val quality: Float,
    val thumbnail: Bitmap,
)

sealed interface CaptureOutcome {
    data class Success(val sample: FaceSampleCapture) : CaptureOutcome
    data object NoFrame : CaptureOutcome
    data object NoFace : CaptureOutcome
    data class LowQuality(val reasons: List<String>) : CaptureOutcome
    data object NoModel : CaptureOutcome
}
