package com.helloauntie.app.ml

import android.content.Context

object ModelAssets {
    const val PERSON = "models/person.tflite"
    const val FACE = "models/face.tflite"
    const val EMBEDDER = "models/mobileface.tflite"

    fun exists(context: Context, path: String): Boolean = runCatching {
        context.assets.open(path).use { }
        true
    }.getOrDefault(false)
}
