package com.helloauntie.core.face

data class FaceQuality(
    val ok: Boolean,
    val score: Float,
    val reasons: List<String>,
)

/**
 * Cheap quality gate applied before feature extraction. It is deliberately
 * conservative: a low-quality face is shown as "看不清" rather than matched.
 */
object QualityGate {

    fun evaluate(
        faceWidthPx: Float,
        frameWidth: Int,
        yawDegrees: Float = 0f,
        pitchDegrees: Float = 0f,
        brightness: Float = 0.5f,
    ): FaceQuality {
        val reasons = mutableListOf<String>()
        var score = 1f

        val minWidth = maxOf(60f, frameWidth * 0.05f)
        if (faceWidthPx < minWidth) {
            reasons += "人脸太小"
            score -= 0.4f
        }
        if (kotlin.math.abs(yawDegrees) > 30f) {
            reasons += "侧脸角度过大"
            score -= 0.25f
        }
        if (kotlin.math.abs(pitchDegrees) > 20f) {
            reasons += "俯仰角度过大"
            score -= 0.2f
        }
        if (brightness < 0.2f || brightness > 0.85f) {
            reasons += "光照不佳"
            score -= 0.2f
        }

        val finalScore = score.coerceIn(0f, 1f)
        return FaceQuality(
            ok = reasons.isEmpty() && finalScore >= 0.6f,
            score = finalScore,
            reasons = reasons,
        )
    }
}
