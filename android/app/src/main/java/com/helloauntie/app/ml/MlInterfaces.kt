package com.helloauntie.app.ml

import android.graphics.Bitmap
import com.helloauntie.core.model.Box
import com.helloauntie.core.model.Detection
import com.helloauntie.core.model.FaceDetection

interface PersonDetector {
    val isDemo: Boolean get() = false
    fun detect(bitmap: Bitmap): List<Detection>
}

interface FaceDetector {
    fun detect(bitmap: Bitmap, roi: Box): List<FaceDetection>
}

interface FaceEmbedder {
    fun embed(bitmap: Bitmap, face: FaceDetection): FloatArray?
}
