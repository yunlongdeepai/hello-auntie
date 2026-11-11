package com.helloauntie.core.kinship

import com.helloauntie.core.model.Person
import com.helloauntie.core.model.Relation
import com.helloauntie.core.model.RelationType
import com.helloauntie.core.model.SiblingAge

data class InferredRelation(
    val fromId: Long,
    val toId: Long,
    val type: RelationType,
    val siblingAge: SiblingAge = SiblingAge.UNKNOWN,
    val confidence: Double = 0.75,
    val reason: String = "",
)

/**
 * Conservative graph completion. Only the high-confidence rule is applied automatically:
 * two people who share a parent are siblings. The result is marked with a lower confidence
 * so the UI can ask the user to confirm it.
 */
object KinshipInference {

    fun inferSiblings(
        persons: List<Person>,
        relations: List<Relation>,
    ): List<InferredRelation> {
        val byId = persons.associateBy { it.id }
        val childrenByParent = relations
            .asSequence()
            .filter { it.type == RelationType.PARENT }
            .groupBy({ it.fromId }, { it.toId })

        val existing = relations
            .filter { it.type == RelationType.SIBLING }
            .map { pairKey(it.fromId, it.toId) }
            .toHashSet()

        val inferred = mutableListOf<InferredRelation>()
        for ((_, children) in childrenByParent) {
            val unique = children.distinct()
            if (unique.size < 2) continue
            for (i in unique.indices) {
                for (j in i + 1 until unique.size) {
                    val a = unique[i]
                    val b = unique[j]
                    if (a == b) continue
                    if (byId[a] == null || byId[b] == null) continue
                    val key = pairKey(a, b)
                    if (key in existing) continue
                    existing.add(key)
                    inferred += InferredRelation(
                        fromId = a,
                        toId = b,
                        type = RelationType.SIBLING,
                        siblingAge = SiblingAge.UNKNOWN,
                        confidence = 0.75,
                        reason = "共同父母（推断，需确认）",
                    )
                }
            }
        }
        return inferred
    }

    private fun pairKey(a: Long, b: Long): String =
        if (a < b) "$a:$b" else "$b:$a"
}
