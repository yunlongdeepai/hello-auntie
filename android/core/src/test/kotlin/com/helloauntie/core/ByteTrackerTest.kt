package com.helloauntie.core

import com.helloauntie.core.model.Box
import com.helloauntie.core.model.Detection
import com.helloauntie.core.track.ByteTracker
import com.helloauntie.core.track.TrackState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ByteTrackerTest {

    private fun det(x: Float, score: Float = 0.9f) = Detection(
        box = Box(x, 10f, x + 40f, 110f),
        score = score,
    )

    @Test
    fun keepsIdAcrossFramesAndShortOcclusion() {
        val tracker = ByteTracker()
        val first = tracker.update(listOf(det(10f)))
        assertEquals(1, first.size)
        val id = first.first().id

        val second = tracker.update(listOf(det(12f)))
        assertEquals(id, second.first().id)

        val lost = tracker.update(emptyList())
        assertEquals(TrackState.LOST, lost.first().state)

        val recovered = tracker.update(listOf(det(15f)))
        assertEquals(id, recovered.first().id)
        assertEquals(TrackState.TRACKED, recovered.first().state)
    }

    @Test
    fun lowScoreDetectionCanKeepTrackAlive() {
        val tracker = ByteTracker()
        val id = tracker.update(listOf(det(10f))).first().id
        val tracks = tracker.update(listOf(det(11f, score = 0.2f)))
        assertEquals(id, tracks.first().id)
        assertTrue(tracks.first().hits >= 2)
    }
}
