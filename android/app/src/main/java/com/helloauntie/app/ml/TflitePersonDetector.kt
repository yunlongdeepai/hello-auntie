package com.helloauntie.app.ml

import android.content.res.AssetManager
import android.graphics.Bitmap
import com.helloauntie.core.model.Box
import com.helloauntie.core.model.Detection
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * YOLOv8/YOLO11 person detector exported as a float32 TFLite model.
 *
 * Expected input:  [1, 320, 320, 3] RGB, normalized to 0..1
 * Expected output: [1, 84, N] (YOLOv8 export) or [1, N, 84]
 * Class 0 is "person"; the other 79 are COCO classes and ignored.
 *
 * If your model has a different layout, adjust [decode] or drop in another
 * PersonDetector implementation.
 */
class TflitePersonDetector(
    assets: AssetManager,
    modelPath: String = "models/person.tflite",
    private val inputSize: Int = 320,
    private val scoreThreshold: Float = 0.35f,
    private val iouThreshold: Float = 0.45f,
) : PersonDetector {

    private val interpreter: Interpreter

    init {
        val bytes = assets.open(modelPath).use { it.readBytes() }
        val model = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply {
            put(bytes)
            rewind()
        }
        interpreter = Interpreter(model, Interpreter.Options().apply { setNumThreads(4) })
    }

    override fun detect(bitmap: Bitmap): List<Detection> {
        if (bitmap.width <= 0 || bitmap.height <= 0) return emptyList()
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in pixels) {
            input.putFloat(((pixel shr 16) and 0xFF) / 255f)
            input.putFloat(((pixel shr 8) and 0xFF) / 255f)
            input.putFloat((pixel and 0xFF) / 255f)
        }
        input.rewind()
        if (scaled !== bitmap) scaled.recycle()

        val shape = interpreter.getOutputTensor(0).shape()
        if (shape.size != 3 || shape[0] != 1) return emptyList()
        val candidates = if (shape[1] == 84) {
            val anchors = shape[2]
            val output = Array(1) { Array(84) { FloatArray(anchors) } }
            interpreter.run(input, output)
            decodeChannelsFirst(output[0], anchors, bitmap)
        } else if (shape[2] == 84) {
            val anchors = shape[1]
            val output = Array(1) { Array(anchors) { FloatArray(84) } }
            interpreter.run(input, output)
            decodeAnchorsFirst(output[0], anchors, bitmap)
        } else {
            return emptyList()
        }
        return nms(candidates)
    }

    private fun decodeChannelsFirst(
        output: Array<FloatArray>,
        anchors: Int,
        bitmap: Bitmap,
    ): List<Detection> {
        val result = mutableListOf<Detection>()
        for (i in 0 until anchors) {
            val score = output[4][i]
            if (score < scoreThreshold) continue
            result += toDetection(
                cx = output[0][i],
                cy = output[1][i],
                w = output[2][i],
                h = output[3][i],
                score = score,
                bitmap = bitmap,
            )
        }
        return result
    }

    private fun decodeAnchorsFirst(
        output: Array<FloatArray>,
        anchors: Int,
        bitmap: Bitmap,
    ): List<Detection> {
        val result = mutableListOf<Detection>()
        for (i in 0 until anchors) {
            val row = output[i]
            val score = row[4]
            if (score < scoreThreshold) continue
            result += toDetection(
                cx = row[0],
                cy = row[1],
                w = row[2],
                h = row[3],
                score = score,
                bitmap = bitmap,
            )
        }
        return result
    }

    private fun toDetection(
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        score: Float,
        bitmap: Bitmap,
    ): Detection {
        val box = Box.fromCenter(
            cx = cx * bitmap.width,
            cy = cy * bitmap.height,
            width = w * bitmap.width,
            height = h * bitmap.height,
        ).clampTo(bitmap.width, bitmap.height)
        return Detection(box = box, score = score)
    }

    private fun nms(candidates: List<Detection>): List<Detection> {
        if (candidates.isEmpty()) return emptyList()
        val sorted = candidates.sortedByDescending { it.score }
        val kept = mutableListOf<Detection>()
        for (candidate in sorted) {
            if (kept.none { it.box.iou(candidate.box) > iouThreshold }) {
                kept += candidate
            }
            if (kept.size >= 20) break
        }
        return kept
    }
}
