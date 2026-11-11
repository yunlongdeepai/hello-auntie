package com.helloauntie.app.ml

import android.graphics.Bitmap
import android.os.SystemClock
import com.helloauntie.core.model.Box
import com.helloauntie.core.model.Detection
import kotlin.math.sin

/**
 * Demo detector used when no person model is bundled.
 *
 * It produces a person box that slowly grows and shrinks so the FAR/NEAR
 * state machine, overlay and enrollment UI can be exercised on a real phone
 * without downloading a model. It does not recognize anything.
 */
class DemoPersonDetector : PersonDetector {

    override val isDemo: Boolean = true

    private val startedAt = SystemClock.elapsedRealtime()

    override fun detect(bitmap: Bitmap): List<Detection> {
        if (bitmap.width <= 0 || bitmap.height <= 0) return emptyList()
        val seconds = (SystemClock.elapsedRealtime() - startedAt) / 1000.0
        val ratio = 0.45 + 0.30 * (0.5 + 0.5 * sin(seconds * 0.8))
        val height = bitmap.height * ratio.toFloat()
        val width = height * 0.42f
        val cx = bitmap.width * 0.5f
        val cy = bitmap.height * 0.56f
        val box = Box.fromCenter(cx, cy, width, height).clampTo(bitmap.width, bitmap.height)
        return listOf(Detection(box = box, score = 0.9f))
    }
}
