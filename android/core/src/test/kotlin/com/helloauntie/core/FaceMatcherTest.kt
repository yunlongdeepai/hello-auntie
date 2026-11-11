package com.helloauntie.core

import com.helloauntie.core.face.FaceMatcher
import com.helloauntie.core.face.FaceTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FaceMatcherTest {

    @Test
    fun acceptsClearBestMatch() {
        val matcher = FaceMatcher(threshold = 0.38f, margin = 0.05f)
        val result = matcher.match(
            query = floatArrayOf(1f, 0f, 0f),
            gallery = listOf(
                FaceTemplate(10L, floatArrayOf(1f, 0f, 0f)),
                FaceTemplate(20L, floatArrayOf(0f, 1f, 0f)),
            ),
        )
        assertTrue(result.accepted)
        assertEquals(10L, result.personId)
    }

    @Test
    fun multipleTemplatesOfSamePersonDoNotCauseRejection() {
        val matcher = FaceMatcher(threshold = 0.38f, margin = 0.05f)
        val result = matcher.match(
            query = floatArrayOf(1f, 0f, 0f),
            gallery = listOf(
                FaceTemplate(10L, floatArrayOf(1f, 0f, 0f)),
                FaceTemplate(10L, floatArrayOf(0.9f, 0.1f, 0f)),
                FaceTemplate(20L, floatArrayOf(0f, 1f, 0f)),
            ),
        )
        assertTrue(result.accepted)
        assertEquals(10L, result.personId)
    }

    @Test
    fun rejectsAmbiguousOrUnknownFace() {
        val matcher = FaceMatcher(threshold = 0.38f, margin = 0.05f)
        val ambiguous = matcher.match(
            query = floatArrayOf(1f, 1f, 0f),
            gallery = listOf(
                FaceTemplate(10L, floatArrayOf(1f, 0f, 0f)),
                FaceTemplate(20L, floatArrayOf(0f, 1f, 0f)),
            ),
        )
        assertFalse(ambiguous.accepted)
        assertEquals(null, ambiguous.personId)

        val unknown = matcher.match(
            query = floatArrayOf(0f, 0f, 1f),
            gallery = listOf(FaceTemplate(10L, floatArrayOf(1f, 0f, 0f))),
        )
        assertFalse(unknown.accepted)
    }
}
