package com.helloauntie.app.ml

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import com.helloauntie.core.model.Box
import com.helloauntie.core.model.Detection
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * YOLOv8/YOLO11 person detector exported as a float32 TFLite model.
 *
 * Input:  [1, S, S, 3] RGB, normalized to 0..1; S is read from the model
 *         (320 or 640 are both supported).
 * Output: [1, 4+C, N] or [1, N, 4+C]; C is 1 (person-only) or 80 (COCO).
 *         Class 0 is "person"; other classes are ignored.
 */
class TflitePersonDetector(
    assets: AssetManager,
    modelPath: String = "models/person.tflite",
    inputSizeOverride: Int = 0,
    private val scoreThreshold: Float = 0.35f,
    private val iouThreshold: Float = 0.45f,
) : PersonDetector {

    private val interpreter: Interpreter
    private val inputSize: Int
    private val inputNchw: Boolean
    @Volatile
    private var firstResultLogged = false

    init {
        val bytes = assets.open(modelPath).use { it.readBytes() }
        val model = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply {
            put(bytes)
            rewind()
        }
        interpreter = Interpreter(model, Interpreter.Options().apply { setNumThreads(4) })
        val inputShape = interpreter.getInputTensor(0).shape()
        inputNchw = inputShape.size == 4 && inputShape[1] == 3
        val detectedSize = when {
            inputNchw && inputShape[2] == inputShape[3] -> inputShape[2]
            inputShape.size == 4 && inputShape[1] == inputShape[2] -> inputShape[1]
            else -> 320
        }
        inputSize = if (inputSizeOverride > 0) inputSizeOverride else detectedSize
        Log.i(
            "HelloAuntieML",
            "person input=${inputShape.contentToString()} nchw=$inputNchw size=$inputSize " +
                "output=${interpreter.getOutputTensor(0).shape().contentToString()}",
        )
    }

    override fun detect(bitmap: Bitmap): List<Detection> {
        if (bitmap.width <= 0 || bitmap.height <= 0) return emptyList()
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        if (inputNchw) {
            for (channel in 0..2) {
                for (pixel in pixels) {
                    val value = when (channel) {
                        0 -> (pixel shr 16) and 0xFF
                        1 -> (pixel shr 8) and 0xFF
                        else -> pixel and 0xFF
                    }
                    input.putFloat(value / 255f)
                }
            }
        } else {
            for (pixel in pixels) {
                input.putFloat(((pixel shr 16) and 0xFF) / 255f)
                input.putFloat(((pixel shr 8) and 0xFF) / 255f)
                input.putFloat((pixel and 0xFF) / 255f)
            }
        }
        input.rewind()
        if (scaled !== bitmap) scaled.recycle()

        val shape = interpreter.getOutputTensor(0).shape()
        if (shape.size != 3 || shape[0] != 1) return emptyList()
        val channelsFirst = shape[1] <= shape[2]
        val channels = if (channelsFirst) shape[1] else shape[2]
        val anchors = if (channelsFirst) shape[2] else shape[1]
        if (channels < 5) return emptyList()
        val candidates = if (channelsFirst) {
            val output = Array(1) { Array(channels) { FloatArray(anchors) } }
            interpreter.run(input, output)
            decodeChannelsFirst(output[0], anchors, bitmap)
        } else {
            val output = Array(1) { Array(anchors) { FloatArray(channels) } }
            interpreter.run(input, output)
            decodeAnchorsFirst(output[0], anchors, bitmap)
        }
        val kept = nms(candidates)
        if (!firstResultLogged) {
            firstResultLogged = true
            Log.i("HelloAuntieML", "person first result: candidates=${candidates.size} kept=${kept.size}")
        }
        return kept
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
