package com.helloauntie.core.kinship

import com.helloauntie.core.model.Gender
import com.helloauntie.core.model.Person
import com.helloauntie.core.model.Relation
import com.helloauntie.core.model.RelationType
import com.helloauntie.core.model.SiblingAge

/**
 * In-memory family graph. The graph is intentionally small (dozens to hundreds of people),
 * so BFS over adjacency lists is sufficient; no graph database is needed.
 */
class KinshipGraph(
    persons: List<Person>,
    relations: List<Relation>,
    inferSiblings: Boolean = true,
) {
    private val personMap: Map<Long, Person> = persons.associateBy { it.id }
    private val allRelations: MutableList<Relation> = relations.toMutableList()

    val inferredRelations: List<InferredRelation>

    init {
        inferredRelations = if (inferSiblings) {
            KinshipInference.inferSiblings(persons, relations).also { inferred ->
                inferred.forEach { item ->
                    allRelations += Relation(
                        fromId = item.fromId,
                        toId = item.toId,
                        type = item.type,
                        siblingAge = item.siblingAge,
                        confidence = item.confidence,
                    )
                }
            }
        } else {
            emptyList()
        }
    }

    fun person(id: Long): Person? = personMap[id]

    fun persons(): List<Person> = personMap.values.toList()

    fun relations(): List<Relation> = allRelations.toList()

    fun shortestPath(fromId: Long, toId: Long, maxDepth: Int = 8): KinshipPath? {
        if (personMap[fromId] == null || personMap[toId] == null) return null
        if (fromId == toId) return KinshipPath(emptyList())

        val queue = ArrayDeque<PathNode>()
        val visited = hashSetOf(fromId)
        queue.add(PathNode(fromId, emptyList()))

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.steps.size >= maxDepth) continue
            for (step in neighbors(node.personId)) {
                if (!visited.add(step.toPersonId)) continue
                val steps = node.steps + step
                if (step.toPersonId == toId) return KinshipPath(steps)
                queue.add(PathNode(step.toPersonId, steps))
            }
        }
        return null
    }

    /**
     * One-hop steps from [currentId]. Tokens are computed from the direction of travel,
     * so the same edge produces "爸爸" when walking child -> parent and "儿子" when
     * walking parent -> child.
     */
    fun neighbors(currentId: Long): List<KinshipStep> {
        val current = personMap[currentId] ?: return emptyList()
        val result = LinkedHashMap<String, KinshipStep>()

        fun put(step: KinshipStep) {
            val key = "${step.toPersonId}:${step.token.name}"
            val old = result[key]
            if (old == null || step.confidence > old.confidence) result[key] = step
        }

        for (relation in allRelations) {
            if (relation.fromId != currentId && relation.toId != currentId) continue
            when (relation.type) {
                RelationType.PARENT -> {
                    if (relation.fromId == currentId) {
                        val child = personMap[relation.toId] ?: continue
                        put(
                            KinshipStep(
                                toPersonId = child.id,
                                token = childToken(child),
                                confidence = relation.confidence,
                                relationType = RelationType.PARENT,
                            ),
                        )
                    } else {
                        val parent = personMap[relation.fromId] ?: continue
                        put(
                            KinshipStep(
                                toPersonId = parent.id,
                                token = parentToken(parent),
                                confidence = relation.confidence,
                                relationType = RelationType.PARENT,
                            ),
                        )
                    }
                }

                RelationType.SPOUSE -> {
                    val otherId = if (relation.fromId == currentId) relation.toId else relation.fromId
                    val other = personMap[otherId] ?: continue
                    put(
                        KinshipStep(
                            toPersonId = other.id,
                            token = spouseToken(current, other),
                            confidence = relation.confidence,
                            relationType = RelationType.SPOUSE,
                        ),
                    )
                }

                RelationType.SIBLING -> {
                    val otherId = if (relation.fromId == currentId) relation.toId else relation.fromId
                    val other = personMap[otherId] ?: continue
                    val age = if (relation.fromId == currentId) {
                        relation.siblingAge
                    } else {
                        relation.siblingAge.inverted()
                    }
                    put(
                        KinshipStep(
                            toPersonId = other.id,
                            token = siblingToken(other, age),
                            confidence = relation.confidence,
                            relationType = RelationType.SIBLING,
                        ),
                    )
                }
            }
        }
        return result.values.toList()
    }

    private fun parentToken(parent: Person): KinToken = when (parent.gender) {
        Gender.FEMALE -> KinToken.M
        else -> KinToken.F
    }

    private fun childToken(child: Person): KinToken = when (child.gender) {
        Gender.FEMALE -> KinToken.D
        else -> KinToken.S
    }

    private fun spouseToken(current: Person, other: Person): KinToken = when (other.gender) {
        Gender.MALE -> KinToken.H
        Gender.FEMALE -> KinToken.W
        Gender.UNKNOWN -> if (current.gender == Gender.MALE) KinToken.W else KinToken.H
    }

    private fun siblingToken(other: Person, age: SiblingAge): KinToken = when (age) {
        SiblingAge.ELDER -> if (other.gender == Gender.FEMALE) KinToken.OS else KinToken.OB
        SiblingAge.YOUNGER -> if (other.gender == Gender.FEMALE) KinToken.LS else KinToken.LB
        SiblingAge.UNKNOWN -> if (other.gender == Gender.FEMALE) KinToken.Z else KinToken.B
    }

    private data class PathNode(
        val personId: Long,
        val steps: List<KinshipStep>,
    )
}
