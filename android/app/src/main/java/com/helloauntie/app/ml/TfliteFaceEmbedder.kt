package com.helloauntie.app.ml

import android.content.res.AssetManager
import android.graphics.Bitmap
import com.helloauntie.core.model.FaceDetection
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * MobileFaceNet-style embedder.
 *
 * Expected input:  [1, 112, 112, 3] RGB, (pixel / 127.5) - 1
 * Expected output: [1, 512] (or [1, 128]); the vector is L2-normalized.
 */
class TfliteFaceEmbedder(
    assets: AssetManager,
    modelPath: String = "models/mobileface.tflite",
    private val inputSize: Int = 112,
) : FaceEmbedder {

    private val interpreter: Interpreter
    private val outputDim: Int

    init {
        val bytes = assets.open(modelPath).use { it.readBytes() }
        val model = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply {
            put(bytes)
            rewind()
        }
        interpreter = Interpreter(model, Interpreter.Options().apply { setNumThreads(4) })
        outputDim = interpreter.getOutputTensor(0).shape().lastOrNull() ?: 512
    }

    override fun embed(bitmap: Bitmap, face: FaceDetection): FloatArray? {
        val expanded = face.box.expand(0.25f).clampTo(bitmap.width, bitmap.height)
        if (expanded.width < 20f || expanded.height < 20f) return null
        val left = expanded.left.toInt()
        val top = expanded.top.toInt()
        val width = expanded.width.toInt().coerceAtLeast(1)
        val height = expanded.height.toInt().coerceAtLeast(1)
        if (left < 0 || top < 0 || left + width > bitmap.width || top + height > bitmap.height) return null

        var crop: Bitmap? = null
        var scaled: Bitmap? = null
        return try {
            val cropBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
            crop = cropBitmap
            val scaledBitmap = Bitmap.createScaledBitmap(cropBitmap, inputSize, inputSize, true)
            scaled = scaledBitmap
            val input = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4)
                .order(ByteOrder.nativeOrder())
            val pixels = IntArray(inputSize * inputSize)
            scaledBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
            for (pixel in pixels) {
                input.putFloat(((pixel shr 16) and 0xFF) / 127.5f - 1f)
                input.putFloat(((pixel shr 8) and 0xFF) / 127.5f - 1f)
                input.putFloat((pixel and 0xFF) / 127.5f - 1f)
            }
            input.rewind()
            val output = Array(1) { FloatArray(outputDim) }
            interpreter.run(input, output)
            l2Normalize(output[0])
        } catch (_: Throwable) {
            null
        } finally {
            if (scaled !== crop && scaled?.isRecycled == false) scaled.recycle()
            if (crop !== bitmap && crop?.isRecycled == false) crop.recycle()
        }
    }

    private fun l2Normalize(values: FloatArray): FloatArray {
        var sum = 0f
        for (value in values) sum += value * value
        val norm = sqrt(sum)
        if (norm <= 1e-6f) return values
        for (i in values.indices) values[i] /= norm
        return values
    }
}
