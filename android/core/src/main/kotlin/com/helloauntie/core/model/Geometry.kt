package com.helloauntie.core.model

import kotlin.math.max
import kotlin.math.min

/** Axis-aligned box in image pixel coordinates. */
data class Box(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = max(0f, right - left)
    val height: Float get() = max(0f, bottom - top)
    val area: Float get() = width * height
    val centerX: Float get() = (left + right) * 0.5f
    val centerY: Float get() = (top + bottom) * 0.5f

    fun iou(other: Box): Float {
        val x1 = max(left, other.left)
        val y1 = max(top, other.top)
        val x2 = min(right, other.right)
        val y2 = min(bottom, other.bottom)
        val inter = max(0f, x2 - x1) * max(0f, y2 - y1)
        val union = area + other.area - inter
        return if (union <= 0f) 0f else inter / union
    }

    fun expand(ratio: Float): Box {
        val dx = width * ratio * 0.5f
        val dy = height * ratio * 0.5f
        return Box(left - dx, top - dy, right + dx, bottom + dy)
    }

    fun clampTo(width: Int, height: Int): Box = Box(
        left.coerceIn(0f, width.toFloat()),
        top.coerceIn(0f, height.toFloat()),
        right.coerceIn(0f, width.toFloat()),
        bottom.coerceIn(0f, height.toFloat()),
    )

    companion object {
        fun fromCenter(cx: Float, cy: Float, width: Float, height: Float): Box =
            Box(cx - width / 2f, cy - height / 2f, cx + width / 2f, cy + height / 2f)
    }
}

data class Detection(
    val box: Box,
    val score: Float,
    val classId: Int = 0,
)

data class Point(val x: Float, val y: Float)

data class FaceDetection(
    val box: Box,
    val score: Float = 0.8f,
    val landmarks: List<Point> = emptyList(),
)
