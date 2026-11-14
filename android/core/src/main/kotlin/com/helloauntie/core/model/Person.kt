package com.helloauntie.core.model

/**
 * A person in the local family graph. The person with [isSelf] = true is the ego:
 * all kinship terms are calculated from this node.
 */
data class Person(
    val id: Long,
    val name: String,
    val gender: Gender = Gender.UNKNOWN,
    val birthYear: Int? = null,
    /** 1 = eldest among siblings; used for 大伯/二伯, 大姨/二姨. */
    val birthOrder: Int? = null,
    val isSelf: Boolean = false,
    /** Non-kinship courtesy title such as 大爷/大妈/叔叔/阿姨. */
    val socialTitle: String? = null,
)

enum class Gender {
    MALE,
    FEMALE,
    UNKNOWN;

    fun toStorage(): String = when (this) {
        MALE -> "M"
        FEMALE -> "F"
        UNKNOWN -> "U"
    }

    companion object {
        fun fromStorage(value: String?): Gender = when (value) {
            "M" -> MALE
            "F" -> FEMALE
            else -> UNKNOWN
        }
    }
}
