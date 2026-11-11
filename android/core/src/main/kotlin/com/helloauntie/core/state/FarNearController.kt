package com.helloauntie.core.state

enum class FarNearMode {
    FAR,
    NEAR,
}

/**
 * Per-track mode controller with hysteresis.
 *
 * The threshold is based on the person box height divided by the frame height.
 * At 1080p and a 60-degree horizontal field of view, 65% is roughly 4 m;
 * 55% is roughly 4.7 m. These values must be recalibrated for other focal lengths.
 */
class FarNearController(
    private val enterRatio: Float = 0.65f,
    private val exitRatio: Float = 0.55f,
) {
    var mode: FarNearMode = FarNearMode.FAR
        private set

    init {
        require(exitRatio < enterRatio) { "exitRatio must be smaller than enterRatio" }
    }

    fun update(personHeightRatio: Float): FarNearMode {
        when (mode) {
            FarNearMode.FAR -> if (personHeightRatio >= enterRatio) mode = FarNearMode.NEAR
            FarNearMode.NEAR -> if (personHeightRatio < exitRatio) mode = FarNearMode.FAR
        }
        return mode
    }

    fun reset() {
        mode = FarNearMode.FAR
    }
}
