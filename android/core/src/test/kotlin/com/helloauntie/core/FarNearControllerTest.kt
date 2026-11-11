package com.helloauntie.core

import com.helloauntie.core.state.FarNearController
import com.helloauntie.core.state.FarNearMode
import kotlin.test.Test
import kotlin.test.assertEquals

class FarNearControllerTest {

    @Test
    fun hysteresisAvoidsFlickerAtBoundary() {
        val controller = FarNearController(enterRatio = 0.65f, exitRatio = 0.55f)
        assertEquals(FarNearMode.FAR, controller.update(0.50f))
        assertEquals(FarNearMode.NEAR, controller.update(0.66f))
        assertEquals(FarNearMode.NEAR, controller.update(0.60f))
        assertEquals(FarNearMode.NEAR, controller.update(0.55f))
        assertEquals(FarNearMode.FAR, controller.update(0.54f))
    }

    @Test
    fun resetReturnsToFar() {
        val controller = FarNearController()
        controller.update(0.80f)
        assertEquals(FarNearMode.NEAR, controller.mode)
        controller.reset()
        assertEquals(FarNearMode.FAR, controller.mode)
    }
}
