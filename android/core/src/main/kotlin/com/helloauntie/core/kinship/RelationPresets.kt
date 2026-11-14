package com.helloauntie.core.kinship

import com.helloauntie.core.model.Gender
import com.helloauntie.core.model.SiblingAge

/**
 * One step in a relation chain, from the current person towards the target.
 * Example: 爸爸的哥哥 = [F(爸爸), OB(伯父)].
 */
data class ChainStep(
    val token: KinToken,
    val gender: Gender,
    val label: String,
    val siblingAge: SiblingAge = SiblingAge.UNKNOWN,
)

data class RelationPreset(
    val id: String,
    val label: String,
    val steps: List<ChainStep>,
)

object RelationPresets {

    private val father = ChainStep(KinToken.F, Gender.MALE, "爸爸")
    private val mother = ChainStep(KinToken.M, Gender.FEMALE, "妈妈")

    val all: List<RelationPreset> = listOf(
        RelationPreset("father", "爸爸", listOf(father)),
        RelationPreset("mother", "妈妈", listOf(mother)),
        RelationPreset(
            "father_elder_brother",
            "爸爸的哥哥（伯父）",
            listOf(father, ChainStep(KinToken.OB, Gender.MALE, "伯父", SiblingAge.ELDER)),
        ),
        RelationPreset(
            "father_younger_brother",
            "爸爸的弟弟（叔叔）",
            listOf(father, ChainStep(KinToken.LB, Gender.MALE, "叔叔", SiblingAge.YOUNGER)),
        ),
        RelationPreset(
            "father_sister",
            "爸爸的姐妹（姑姑）",
            listOf(father, ChainStep(KinToken.OS, Gender.FEMALE, "姑姑", SiblingAge.ELDER)),
        ),
        RelationPreset(
            "mother_brother",
            "妈妈的兄弟（舅舅）",
            listOf(mother, ChainStep(KinToken.OB, Gender.MALE, "舅舅", SiblingAge.ELDER)),
        ),
        RelationPreset(
            "mother_sister",
            "妈妈的姐妹（姨妈）",
            listOf(mother, ChainStep(KinToken.OS, Gender.FEMALE, "姨妈", SiblingAge.ELDER)),
        ),
        RelationPreset(
            "paternal_cousin_male",
            "堂哥/堂弟（爸爸的兄弟的儿子）",
            listOf(
                father,
                ChainStep(KinToken.OB, Gender.MALE, "伯父", SiblingAge.ELDER),
                ChainStep(KinToken.S, Gender.MALE, "堂兄弟"),
            ),
        ),
        RelationPreset(
            "paternal_cousin_female",
            "堂姐/堂妹（爸爸的兄弟的女儿）",
            listOf(
                father,
                ChainStep(KinToken.OB, Gender.MALE, "伯父", SiblingAge.ELDER),
                ChainStep(KinToken.D, Gender.FEMALE, "堂姐妹"),
            ),
        ),
        RelationPreset(
            "paternal_aunt_cousin_male",
            "表哥/表弟（爸爸的姐妹的儿子）",
            listOf(
                father,
                ChainStep(KinToken.OS, Gender.FEMALE, "姑姑", SiblingAge.ELDER),
                ChainStep(KinToken.S, Gender.MALE, "表兄弟"),
            ),
        ),
        RelationPreset(
            "paternal_aunt_cousin_female",
            "表姐/表妹（爸爸的姐妹的女儿）",
            listOf(
                father,
                ChainStep(KinToken.OS, Gender.FEMALE, "姑姑", SiblingAge.ELDER),
                ChainStep(KinToken.D, Gender.FEMALE, "表姐妹"),
            ),
        ),
        RelationPreset(
            "maternal_uncle_cousin_male",
            "表哥/表弟（妈妈的兄弟的儿子）",
            listOf(
                mother,
                ChainStep(KinToken.OB, Gender.MALE, "舅舅", SiblingAge.ELDER),
                ChainStep(KinToken.S, Gender.MALE, "表兄弟"),
            ),
        ),
        RelationPreset(
            "maternal_uncle_cousin_female",
            "表姐/表妹（妈妈的兄弟的女儿）",
            listOf(
                mother,
                ChainStep(KinToken.OB, Gender.MALE, "舅舅", SiblingAge.ELDER),
                ChainStep(KinToken.D, Gender.FEMALE, "表姐妹"),
            ),
        ),
        RelationPreset(
            "maternal_aunt_cousin_male",
            "表哥/表弟（妈妈的姐妹的儿子）",
            listOf(
                mother,
                ChainStep(KinToken.OS, Gender.FEMALE, "姨妈", SiblingAge.ELDER),
                ChainStep(KinToken.S, Gender.MALE, "表兄弟"),
            ),
        ),
        RelationPreset(
            "maternal_aunt_cousin_female",
            "表姐/表妹（妈妈的姐妹的女儿）",
            listOf(
                mother,
                ChainStep(KinToken.OS, Gender.FEMALE, "姨妈", SiblingAge.ELDER),
                ChainStep(KinToken.D, Gender.FEMALE, "表姐妹"),
            ),
        ),
        RelationPreset(
            "uncle_wife",
            "伯父的妻子（伯母）",
            listOf(
                father,
                ChainStep(KinToken.OB, Gender.MALE, "伯父", SiblingAge.ELDER),
                ChainStep(KinToken.W, Gender.FEMALE, "伯母"),
            ),
        ),
        RelationPreset(
            "younger_uncle_wife",
            "叔叔的妻子（婶婶）",
            listOf(
                father,
                ChainStep(KinToken.LB, Gender.MALE, "叔叔", SiblingAge.YOUNGER),
                ChainStep(KinToken.W, Gender.FEMALE, "婶婶"),
            ),
        ),
        RelationPreset(
            "aunt_husband",
            "姑姑的丈夫（姑父）",
            listOf(
                father,
                ChainStep(KinToken.OS, Gender.FEMALE, "姑姑", SiblingAge.ELDER),
                ChainStep(KinToken.H, Gender.MALE, "姑父"),
            ),
        ),
        RelationPreset(
            "maternal_uncle_wife",
            "舅舅的妻子（舅妈）",
            listOf(
                mother,
                ChainStep(KinToken.OB, Gender.MALE, "舅舅", SiblingAge.ELDER),
                ChainStep(KinToken.W, Gender.FEMALE, "舅妈"),
            ),
        ),
        RelationPreset(
            "maternal_aunt_husband",
            "姨妈的丈夫（姨父）",
            listOf(
                mother,
                ChainStep(KinToken.OS, Gender.FEMALE, "姨妈", SiblingAge.ELDER),
                ChainStep(KinToken.H, Gender.MALE, "姨父"),
            ),
        ),
        RelationPreset(
            "elder_brother_wife",
            "哥哥的妻子（嫂子）",
            listOf(
                ChainStep(KinToken.OB, Gender.MALE, "哥哥", SiblingAge.ELDER),
                ChainStep(KinToken.W, Gender.FEMALE, "嫂子"),
            ),
        ),
        RelationPreset(
            "elder_sister_husband",
            "姐姐的丈夫（姐夫）",
            listOf(
                ChainStep(KinToken.OS, Gender.FEMALE, "姐姐", SiblingAge.ELDER),
                ChainStep(KinToken.H, Gender.MALE, "姐夫"),
            ),
        ),
        RelationPreset(
            "son_wife",
            "儿子的妻子（儿媳）",
            listOf(
                ChainStep(KinToken.S, Gender.MALE, "儿子"),
                ChainStep(KinToken.W, Gender.FEMALE, "儿媳"),
            ),
        ),
        RelationPreset(
            "daughter_husband",
            "女儿的丈夫（女婿）",
            listOf(
                ChainStep(KinToken.D, Gender.FEMALE, "女儿"),
                ChainStep(KinToken.H, Gender.MALE, "女婿"),
            ),
        ),
        RelationPreset("elder_brother", "哥哥", listOf(ChainStep(KinToken.OB, Gender.MALE, "哥哥", SiblingAge.ELDER))),
        RelationPreset("elder_sister", "姐姐", listOf(ChainStep(KinToken.OS, Gender.FEMALE, "姐姐", SiblingAge.ELDER))),
        RelationPreset("younger_brother", "弟弟", listOf(ChainStep(KinToken.LB, Gender.MALE, "弟弟", SiblingAge.YOUNGER))),
        RelationPreset("younger_sister", "妹妹", listOf(ChainStep(KinToken.LS, Gender.FEMALE, "妹妹", SiblingAge.YOUNGER))),
        RelationPreset("son", "儿子", listOf(ChainStep(KinToken.S, Gender.MALE, "儿子"))),
        RelationPreset("daughter", "女儿", listOf(ChainStep(KinToken.D, Gender.FEMALE, "女儿"))),
        RelationPreset("husband", "丈夫", listOf(ChainStep(KinToken.H, Gender.MALE, "丈夫"))),
        RelationPreset("wife", "妻子", listOf(ChainStep(KinToken.W, Gender.FEMALE, "妻子"))),
        RelationPreset(
            "paternal_grandfather",
            "爷爷",
            listOf(father, ChainStep(KinToken.F, Gender.MALE, "爷爷")),
        ),
        RelationPreset(
            "paternal_grandmother",
            "奶奶",
            listOf(father, ChainStep(KinToken.M, Gender.FEMALE, "奶奶")),
        ),
        RelationPreset(
            "maternal_grandfather",
            "外公",
            listOf(mother, ChainStep(KinToken.F, Gender.MALE, "外公")),
        ),
        RelationPreset(
            "maternal_grandmother",
            "外婆",
            listOf(mother, ChainStep(KinToken.M, Gender.FEMALE, "外婆")),
        ),
    )

    fun byId(id: String): RelationPreset = all.firstOrNull { it.id == id } ?: all.first()
}
