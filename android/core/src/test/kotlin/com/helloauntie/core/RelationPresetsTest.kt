package com.helloauntie.core

import com.helloauntie.core.kinship.KinToken
import com.helloauntie.core.kinship.RelationPresets
import com.helloauntie.core.model.Gender
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationPresetsTest {

    @Test
    fun cousinPresetsExistForAllSides() {
        val ids = listOf(
            "paternal_cousin_male",
            "paternal_cousin_female",
            "paternal_aunt_cousin_male",
            "paternal_aunt_cousin_female",
            "maternal_uncle_cousin_male",
            "maternal_uncle_cousin_female",
            "maternal_aunt_cousin_male",
            "maternal_aunt_cousin_female",
        )
        ids.forEach { id ->
            val preset = RelationPresets.byId(id)
            assertTrue(preset.id == id, "missing cousin preset: $id")
            assertTrue(preset.label.contains("堂") || preset.label.contains("表"))
        }
    }

    @Test
    fun socialTitlesAreNotKinshipChains() {
        val expected = mapOf(
            "social_elder_uncle" to "大爷",
            "social_elder_aunt" to "大妈",
            "social_uncle" to "叔叔",
            "social_aunt" to "阿姨",
        )
        expected.forEach { (id, title) ->
            val preset = RelationPresets.byId(id)
            assertEquals(id, preset.id)
            assertEquals(title, preset.socialTitle)
            assertTrue(preset.steps.isEmpty())
        }
    }

    @Test
    fun paternalCousinChainGoesThroughFathersBrother() {
        val preset = RelationPresets.byId("paternal_cousin_male")
        assertEquals(
            listOf(KinToken.F, KinToken.OB, KinToken.S),
            preset.steps.map { it.token },
        )
        assertEquals(Gender.MALE, preset.steps.last().gender)
    }

    @Test
    fun maternalAuntCousinChainGoesThroughMothersSister() {
        val preset = RelationPresets.byId("maternal_aunt_cousin_female")
        assertEquals(
            listOf(KinToken.M, KinToken.OS, KinToken.D),
            preset.steps.map { it.token },
        )
        assertEquals(Gender.FEMALE, preset.steps.last().gender)
    }
}
