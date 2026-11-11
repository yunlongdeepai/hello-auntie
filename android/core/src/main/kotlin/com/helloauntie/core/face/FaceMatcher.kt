package com.helloauntie.core.face

import kotlin.math.sqrt

data class FaceTemplate(
    val personId: Long,
    val embedding: FloatArray,
)

data class MatchResult(
    val personId: Long?,
    val top1: Float,
    val top2: Float,
    val accepted: Boolean,
)

/**
 * Open-set 1:N matcher. A result is accepted only when the best score is above
 * the threshold AND the margin to the second best is large enough. This is the
 * key protection against confidently calling the wrong relative.
 */
class FaceMatcher(
    private val threshold: Float = 0.38f,
    private val margin: Float = 0.05f,
) {
    fun match(query: FloatArray, gallery: List<FaceTemplate>): MatchResult {
        if (gallery.isEmpty() || query.isEmpty()) {
            return MatchResult(personId = null, top1 = -1f, top2 = -1f, accepted = false)
        }
        // Multiple templates may belong to one person. Compare people, not templates:
        // otherwise two photos of the same relative would shrink the margin and be rejected.
        val bestByPerson = HashMap<Long, Float>()
        for (item in gallery) {
            val score = cosine(query, item.embedding)
            val old = bestByPerson[item.personId]
            if (old == null || score > old) bestByPerson[item.personId] = score
        }
        val ranked = bestByPerson.entries.sortedByDescending { it.value }
        val top1Entry = ranked.first()
        val top1 = top1Entry.value
        val top2 = if (ranked.size > 1) ranked[1].value else -1f
        val accepted = top1 >= threshold && (top1 - top2) >= margin
        return MatchResult(
            personId = if (accepted) top1Entry.key else null,
            top1 = top1,
            top2 = top2,
            accepted = accepted,
        )
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in 0 until n) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        if (na <= 0f || nb <= 0f) return 0f
        return dot / (sqrt(na) * sqrt(nb))
    }
}
