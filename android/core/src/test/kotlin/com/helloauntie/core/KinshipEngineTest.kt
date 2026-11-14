package com.helloauntie.core

import com.helloauntie.core.kinship.KinshipEngine
import com.helloauntie.core.kinship.KinshipGraph
import com.helloauntie.core.kinship.Region
import com.helloauntie.core.model.Gender
import com.helloauntie.core.model.Person
import com.helloauntie.core.model.Relation
import com.helloauntie.core.model.RelationType
import com.helloauntie.core.model.SiblingAge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KinshipEngineTest {

    private fun person(
        id: Long,
        name: String,
        gender: Gender,
        birthYear: Int? = null,
        birthOrder: Int? = null,
        isSelf: Boolean = false,
    ) = Person(id, name, gender, birthYear, birthOrder, isSelf)

    private fun parent(from: Long, to: Long) =
        Relation(fromId = from, toId = to, type = RelationType.PARENT)

    private fun spouse(a: Long, b: Long) =
        Relation(fromId = a, toId = b, type = RelationType.SPOUSE)

    private fun sibling(a: Long, b: Long, age: SiblingAge) =
        Relation(fromId = a, toId = b, type = RelationType.SIBLING, siblingAge = age)

    private fun family(): KinshipGraph {
        val people = listOf(
            person(1, "我", Gender.MALE, 2000, isSelf = true),
            person(2, "爸爸", Gender.MALE, 1970),
            person(3, "妈妈", Gender.FEMALE, 1972),
            person(4, "伯父", Gender.MALE, 1968),
            person(5, "伯母", Gender.FEMALE, 1969),
            person(6, "堂弟", Gender.MALE, 2010),
            person(7, "姨妈", Gender.FEMALE, 1974),
            person(8, "姨父", Gender.MALE, 1973),
            person(9, "表姐", Gender.FEMALE, 1990),
            person(10, "外婆", Gender.FEMALE, 1948),
            person(11, "哥哥", Gender.MALE, 1998),
            person(12, "侄子", Gender.MALE, 2020),
        )
        val relations = listOf(
            parent(2, 1),
            parent(3, 1),
            sibling(2, 4, SiblingAge.ELDER),
            spouse(4, 5),
            parent(4, 6),
            parent(5, 6),
            parent(10, 3),
            parent(10, 7),
            spouse(7, 8),
            parent(7, 9),
            sibling(1, 11, SiblingAge.ELDER),
            parent(11, 12),
        )
        return KinshipGraph(people, relations)
    }

    @Test
    fun directAndInLawTerms() {
        val engine = KinshipEngine(family(), Region.SOUTH)
        assertEquals("爸爸", engine.term(1, 2).primary)
        assertEquals("妈妈", engine.term(1, 3).primary)
        assertEquals("伯父", engine.term(1, 4).primary)
        assertEquals("伯母", engine.term(1, 5).primary)
        assertEquals("爸爸的哥哥", engine.term(1, 4).selector)
    }

    @Test
    fun cousinAndNiblingTermsUseAgeAndSide() {
        val engine = KinshipEngine(family(), Region.SOUTH)
        assertEquals("堂弟", engine.term(1, 6).primary)
        assertEquals("哥哥", engine.term(1, 11).primary)
        assertEquals("侄子", engine.term(1, 12).primary)
    }

    @Test
    fun inferredSiblingFromSharedParent() {
        val graph = family()
        val engine = KinshipEngine(graph, Region.SOUTH)
        assertTrue(graph.inferredRelations.isNotEmpty())
        assertEquals("姨妈", engine.term(1, 7).primary)
        assertEquals("姨丈", engine.term(1, 8).primary)
        assertEquals("表姐", engine.term(1, 9).primary)
        assertEquals("外婆", engine.term(1, 10).primary)
    }

    @Test
    fun regionVariants() {
        val north = KinshipEngine(family(), Region.NORTH)
        assertEquals("姥姥", north.term(1, 10).primary)
        val south = KinshipEngine(family(), Region.SOUTH)
        assertEquals("外婆", south.term(1, 10).primary)
    }

    @Test
    fun rankByBirthOrder() {
        val people = listOf(
            person(1, "我", Gender.MALE, 2000, isSelf = true),
            person(2, "爸爸", Gender.MALE, 1970),
            person(4, "二伯", Gender.MALE, 1968, birthOrder = 2),
        )
        val relations = listOf(
            parent(2, 1),
            sibling(2, 4, SiblingAge.ELDER),
        )
        val engine = KinshipEngine(KinshipGraph(people, relations), Region.SOUTH)
        assertEquals("二伯", engine.term(1, 4).primary)
        assertTrue(engine.term(1, 4).alternatives.contains("伯父"))
    }

    @Test
    fun socialTitleIsUsedForNonRelatives() {
        val people = listOf(
            person(1, "我", Gender.MALE, 2000, isSelf = true),
            Person(2, "王阿姨", Gender.FEMALE, socialTitle = "阿姨"),
            Person(3, "李大爷", Gender.MALE, socialTitle = "大爷"),
        )
        val engine = KinshipEngine(KinshipGraph(people, emptyList()), Region.SOUTH)
        assertEquals("阿姨", engine.term(1, 2).primary)
        assertTrue(engine.term(1, 2).known)
        assertEquals("大爷", engine.term(1, 3).primary)
        assertEquals("非亲属常规称呼", engine.term(1, 2).selector)
    }

    @Test
    fun unknownRelationIsNotGuessed() {
        val people = listOf(
            person(1, "我", Gender.MALE, 2000, isSelf = true),
            person(20, "陌生人", Gender.UNKNOWN),
        )
        val engine = KinshipEngine(KinshipGraph(people, emptyList()), Region.SOUTH)
        val result = engine.term(1, 20)
        assertFalse(result.known)
        assertEquals("亲戚", result.primary)
    }
}
