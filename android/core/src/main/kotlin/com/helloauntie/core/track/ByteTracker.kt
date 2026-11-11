package com.helloauntie.core.track

import com.helloauntie.core.model.Box
import com.helloauntie.core.model.Detection

enum class TrackState {
    TRACKED,
    LOST,
    REMOVED,
}

/**
 * A tracked person. The motion filter is a lightweight alpha-beta predictor;
 * it keeps IDs stable for the short occlusions typical of a family gathering.
 * For production, this class can be replaced by a full Kalman filter without
 * changing the tracker API.
 */
class Track internal constructor(
    val id: Int,
    var box: Box,
    var score: Float,
) {
    var state: TrackState = TrackState.TRACKED
        internal set
    var timeSinceUpdate: Int = 0
        internal set
    var hits: Int = 1
        internal set

    private var velocityX: Float = 0f
    private var velocityY: Float = 0f

    internal fun predict() {
        if (velocityX != 0f || velocityY != 0f) {
            box = box.translate(velocityX, velocityY)
        }
    }

    internal fun update(newBox: Box) {
        val dx = newBox.centerX - box.centerX
        val dy = newBox.centerY - box.centerY
        velocityX = 0.6f * velocityX + 0.4f * dx
        velocityY = 0.6f * velocityY + 0.4f * dy
        box = newBox
        timeSinceUpdate = 0
        state = TrackState.TRACKED
        hits += 1
    }

    internal fun markLost() {
        timeSinceUpdate += 1
        state = TrackState.LOST
    }

    internal fun remove() {
        state = TrackState.REMOVED
    }

    fun heightRatio(frameHeight: Int): Float =
        if (frameHeight <= 0) 0f else box.height / frameHeight
}

private fun Box.translate(dx: Float, dy: Float): Box =
    Box(left + dx, top + dy, right + dx, bottom + dy)

/**
 * ByteTrack-style two-stage association:
 * 1. high-score detections are matched to all tracks;
 * 2. low-score detections get a second chance with the remaining tracks.
 *
 * Matching is greedy by IoU (fast and adequate for < 30 tracks). A Hungarian
 * assignment can be dropped in later without changing the public API.
 */
class ByteTracker(
    private val highThreshold: Float = 0.5f,
    private val lowThreshold: Float = 0.1f,
    private val matchIou: Float = 0.3f,
    private val lowMatchIou: Float = 0.15f,
    private val maxLostFrames: Int = 30,
) {
    private val tracks = mutableListOf<Track>()
    private var nextId = 1

    fun update(detections: List<Detection>): List<Track> {
        tracks.forEach { it.predict() }

        val available = tracks.filter { it.state != TrackState.REMOVED }.toMutableList()
        val high = detections.filter { it.score >= highThreshold }.sortedByDescending { it.score }
        val low = detections
            .filter { it.score >= lowThreshold && it.score < highThreshold }
            .sortedByDescending { it.score }

        val highMatches = greedyMatch(available, high, matchIou)
        val usedHigh = BooleanArray(high.size)
        applyMatches(highMatches, high, usedHigh)

        val usedTracks = highMatches.map { it.track }.toHashSet()
        val remainingTracks = available.filter { it !in usedTracks }
        val lowMatches = greedyMatch(remainingTracks, low, lowMatchIou)
        val usedLow = BooleanArray(low.size)
        applyMatches(lowMatches, low, usedLow)

        // Tracks that were not matched: keep them for a short grace period.
        val matchedIds = (highMatches.map { it.track } + lowMatches.map { it.track }).toHashSet()
        for (track in available) {
            if (track in matchedIds) continue
            track.markLost()
            if (track.timeSinceUpdate > maxLostFrames) track.remove()
        }

        // Unmatched high-score detections start new tracks.
        for (i in high.indices) {
            if (usedHigh[i]) continue
            val detection = high[i]
            tracks += Track(nextId++, detection.box, detection.score)
        }

        tracks.removeAll { it.state == TrackState.REMOVED }
        return tracks.toList()
    }

    private fun applyMatches(
        matches: List<Match>,
        detections: List<Detection>,
        used: BooleanArray,
    ) {
        for (match in matches) {
            match.track.update(match.detection.box)
            match.track.score = match.detection.score
            val index = detections.indexOfFirst { it === match.detection }
            if (index >= 0) used[index] = true
        }
    }

    private fun greedyMatch(
        tracks: List<Track>,
        detections: List<Detection>,
        threshold: Float,
    ): List<Match> {
        if (tracks.isEmpty() || detections.isEmpty()) return emptyList()
        val candidates = mutableListOf<Candidate>()
        for (track in tracks) {
            for (index in detections.indices) {
                val iou = track.box.iou(detections[index].box)
                if (iou >= threshold) {
                    candidates += Candidate(track, index, iou)
                }
            }
        }
        candidates.sortByDescending { it.iou }

        val usedTracks = hashSetOf<Track>()
        val usedDetections = hashSetOf<Int>()
        val result = mutableListOf<Match>()
        for (candidate in candidates) {
            if (candidate.track in usedTracks || candidate.detectionIndex in usedDetections) continue
            usedTracks += candidate.track
            usedDetections += candidate.detectionIndex
            result += Match(candidate.track, detections[candidate.detectionIndex])
        }
        return result
    }

    private data class Candidate(
        val track: Track,
        val detectionIndex: Int,
        val iou: Float,
    )

    private data class Match(
        val track: Track,
        val detection: Detection,
    )
}
