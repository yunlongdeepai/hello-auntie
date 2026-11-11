package com.helloauntie.app.ml

import android.graphics.Bitmap
import android.graphics.PointF
import com.helloauntie.core.model.Box
import com.helloauntie.core.model.FaceDetection

/**
 * Offline fallback face detector based on android.media.FaceDetector.
 * It only detects frontal faces, but requires no model and no GMS, which is
 * useful on ColorOS and for the first runnable build.
 */
class AndroidFaceDetector : FaceDetector {

    override fun detect(bitmap: Bitmap, roi: Box): List<FaceDetection> {
        val left = roi.left.toInt().coerceIn(0, bitmap.width - 1)
        val top = roi.top.toInt().coerceIn(0, bitmap.height - 1)
        val right = roi.right.toInt().coerceIn(left + 1, bitmap.width)
        val bottom = roi.bottom.toInt().coerceIn(top + 1, bitmap.height)
        val width = right - left
        val height = bottom - top
        if (width < 48 || height < 48) return emptyList()

        val evenWidth = width - (width % 2)
        val evenHeight = height - (height % 2)
        if (evenWidth < 48 || evenHeight < 48) return emptyList()
        var crop: Bitmap? = null
        var rgb565: Bitmap? = null
        return try {
            val cropBitmap = Bitmap.createBitmap(bitmap, left, top, evenWidth, evenHeight)
            crop = cropBitmap
            val rgbBitmap = cropBitmap.copy(Bitmap.Config.RGB_565, false) ?: return emptyList()
            rgb565 = rgbBitmap
            val detector = android.media.FaceDetector(evenWidth, evenHeight, MAX_FACES)
            val faces = arrayOfNulls<android.media.FaceDetector.Face>(MAX_FACES)
            val count = detector.findFaces(rgbBitmap, faces)
            if (count <= 0) return emptyList()

            val result = mutableListOf<FaceDetection>()
            for (i in 0 until count.coerceAtMost(MAX_FACES)) {
                val face = faces[i] ?: continue
                val mid = PointF()
                face.getMidPoint(mid)
                val eyes = face.eyesDistance().coerceAtLeast(6f)
                val box = Box.fromCenter(
                    cx = left + mid.x,
                    cy = top + mid.y,
                    width = eyes * 2.6f,
                    height = eyes * 3.2f,
                ).clampTo(bitmap.width, bitmap.height)
                result += FaceDetection(
                    box = box,
                    score = face.confidence().coerceIn(0f, 1f),
                )
            }
            result
        } catch (_: Throwable) {
            emptyList()
        } finally {
            if (rgb565 !== crop && rgb565?.isRecycled == false) rgb565.recycle()
            if (crop !== bitmap && crop?.isRecycled == false) crop.recycle()
        }
    }

    companion object {
        private const val MAX_FACES = 5
    }
}
