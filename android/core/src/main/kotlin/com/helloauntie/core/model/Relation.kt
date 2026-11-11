package com.helloauntie.core.model

/**
 * A directed or undirected family edge.
 *
 * PARENT: [fromId] is the parent of [toId].
 * SPOUSE: undirected marriage edge; [fromId] and [toId] are interchangeable.
 * SIBLING: undirected sibling edge; [siblingAge] describes [toId] relative to [fromId].
 */
data class Relation(
    val id: Long = 0,
    val fromId: Long,
    val toId: Long,
    val type: RelationType,
    val siblingAge: SiblingAge = SiblingAge.UNKNOWN,
    val confidence: Double = 1.0,
)

enum class RelationType {
    PARENT,
    SPOUSE,
    SIBLING;

    fun toStorage(): String = name

    companion object {
        fun fromStorage(value: String?): RelationType = when (value) {
            "PARENT" -> PARENT
            "SPOUSE" -> SPOUSE
            else -> SIBLING
        }
    }
}

enum class SiblingAge {
    ELDER,
    YOUNGER,
    UNKNOWN;

    fun toStorage(): String = name

    fun inverted(): SiblingAge = when (this) {
        ELDER -> YOUNGER
        YOUNGER -> ELDER
        UNKNOWN -> UNKNOWN
    }

    companion object {
        fun fromStorage(value: String?): SiblingAge = when (value) {
            "ELDER" -> ELDER
            "YOUNGER" -> YOUNGER
            else -> UNKNOWN
        }
    }
}
