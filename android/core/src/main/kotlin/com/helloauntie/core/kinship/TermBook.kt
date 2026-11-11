package com.helloauntie.core.kinship

import com.helloauntie.core.model.Gender
import com.helloauntie.core.model.Person

/**
 * Chinese kinship term lookup.
 *
 * This is a curated subset of the relationship.js idea: a path of primitive relations
 * (father, mother, older brother, wife, son ...) is mapped to a standard term plus
 * regional variants. Dynamic rules handle rank (大伯/二伯) and cousin/nibling cases
 * where the term depends on the target's gender or age relative to the ego.
 */
object TermBook {

    private data class TermEntry(
        val standard: String,
        val north: String? = null,
        val south: String? = null,
        val cantonese: String? = null,
        val alternatives: List<String> = emptyList(),
    )

    private val terms: Map<String, TermEntry> = mapOf(
        "F" to TermEntry("爸爸", alternatives = listOf("父亲", "老爸")),
        "M" to TermEntry("妈妈", alternatives = listOf("母亲", "老妈")),

        "F-F" to TermEntry("爷爷", cantonese = "阿公", alternatives = listOf("祖父")),
        "F-M" to TermEntry("奶奶", cantonese = "阿婆", alternatives = listOf("祖母")),
        "M-F" to TermEntry("外公", north = "姥爷", cantonese = "阿公", alternatives = listOf("姥爷")),
        "M-M" to TermEntry("外婆", north = "姥姥", cantonese = "阿婆", alternatives = listOf("姥姥")),

        "OB" to TermEntry("哥哥", alternatives = listOf("兄长")),
        "OS" to TermEntry("姐姐", alternatives = listOf("姊姊")),
        "LB" to TermEntry("弟弟"),
        "LS" to TermEntry("妹妹"),
        "B" to TermEntry("兄弟"),
        "Z" to TermEntry("姐妹"),

        "F-OB-W" to TermEntry("伯母", north = "大娘", cantonese = "伯娘", alternatives = listOf("大娘", "伯娘")),
        "F-LB-W" to TermEntry("婶婶", south = "婶娘", cantonese = "婶母", alternatives = listOf("婶娘", "婶母")),
        "F-OS-H" to TermEntry("姑父", south = "姑丈", alternatives = listOf("姑丈")),
        "F-LS-H" to TermEntry("姑父", south = "姑丈", alternatives = listOf("姑丈")),
        "M-OB-W" to TermEntry("舅妈", south = "舅母", alternatives = listOf("舅母", "妗子")),
        "M-LB-W" to TermEntry("舅妈", south = "舅母", alternatives = listOf("舅母", "妗子")),
        "M-OS-H" to TermEntry("姨父", south = "姨丈", alternatives = listOf("姨丈")),
        "M-LS-H" to TermEntry("姨父", south = "姨丈", alternatives = listOf("姨丈")),

        "OB-W" to TermEntry("嫂子", alternatives = listOf("嫂嫂")),
        "LB-W" to TermEntry("弟媳", alternatives = listOf("弟妹")),
        "OS-H" to TermEntry("姐夫"),
        "LS-H" to TermEntry("妹夫"),

        "S" to TermEntry("儿子"),
        "D" to TermEntry("女儿"),
        "S-W" to TermEntry("儿媳", alternatives = listOf("媳妇")),
        "D-H" to TermEntry("女婿"),

        "OB-S" to TermEntry("侄子", alternatives = listOf("侄儿")),
        "OB-D" to TermEntry("侄女"),
        "LB-S" to TermEntry("侄子", alternatives = listOf("侄儿")),
        "LB-D" to TermEntry("侄女"),
        "OS-S" to TermEntry("外甥"),
        "OS-D" to TermEntry("外甥女"),
        "LS-S" to TermEntry("外甥"),
        "LS-D" to TermEntry("外甥女"),

        "W-F" to TermEntry("岳父", alternatives = listOf("丈人", "老丈人")),
        "W-M" to TermEntry("岳母", alternatives = listOf("丈母娘")),
        "H-F" to TermEntry("公公", south = "家公", alternatives = listOf("家公")),
        "H-M" to TermEntry("婆婆", south = "家婆", alternatives = listOf("家婆")),

        "W-OB" to TermEntry("大舅子"),
        "W-LB" to TermEntry("小舅子"),
        "W-OS" to TermEntry("大姨子"),
        "W-LS" to TermEntry("小姨子"),
        "H-OB" to TermEntry("大伯子", alternatives = listOf("大伯哥")),
        "H-LB" to TermEntry("小叔子"),
        "H-OS" to TermEntry("大姑子"),
        "H-LS" to TermEntry("小姑子"),
    )

    fun resolve(
        path: KinshipPath,
        ego: Person,
        target: Person,
        region: Region,
    ): TermResult? {
        val tokens = path.tokens
        if (tokens.isEmpty()) return null

        parentSibling(tokens, target, region, path)?.let { return it }
        parentSiblingSpouse(tokens, region, path)?.let { return it }
        cousin(tokens, ego, target, path)?.let { return it }
        nibling(tokens, target, path)?.let { return it }

        if (tokens.size == 1 && tokens[0] in setOf(KinToken.OB, KinToken.OS, KinToken.LB, KinToken.LS, KinToken.B, KinToken.Z)) {
            return entryResult(tokens[0].label, emptyList(), path, region, "同辈")
        }

        val key = tokens.joinToString("-")
        val entry = terms[key] ?: return null
        return entryResult(entry, path, region)
    }

    private fun parentSibling(
        tokens: List<KinToken>,
        target: Person,
        region: Region,
        path: KinshipPath,
    ): TermResult? {
        if (tokens.size != 2) return null
        val parent = tokens[0]
        val sibling = tokens[1]
        if (parent != KinToken.F && parent != KinToken.M) return null
        if (sibling !in setOf(KinToken.OB, KinToken.OS, KinToken.LB, KinToken.LS, KinToken.B, KinToken.Z)) return null

        val male = sibling in setOf(KinToken.OB, KinToken.LB, KinToken.B)
        val base: String
        val prefix: String
        val explanation: String
        when {
            parent == KinToken.F && male -> {
                base = if (sibling == KinToken.LB) "叔叔" else "伯父"
                prefix = if (sibling == KinToken.LB) "叔" else "伯"
                explanation = if (sibling == KinToken.LB) "父亲的弟弟" else "父亲的哥哥/兄弟"
            }
            parent == KinToken.F -> {
                base = "姑姑"; prefix = "姑"; explanation = "父亲的姐妹"
            }
            male -> {
                base = "舅舅"; prefix = "舅"; explanation = "母亲的兄弟"
            }
            else -> {
                base = "姨妈"; prefix = "姨"; explanation = "母亲的姐妹"
            }
        }

        val primary = rankTerm(prefix, base, target.birthOrder)
        val alternatives = buildList {
            add(base)
            if (primary != base) add(primary)
            when {
                parent == KinToken.F && male && base == "伯父" -> addAll(listOf("大伯", "伯伯"))
                parent == KinToken.F && male -> addAll(listOf("叔父"))
                parent == KinToken.F -> addAll(listOf("姑妈", "姑姐"))
                male -> addAll(listOf("舅父"))
                else -> addAll(listOf("阿姨", "姨姨"))
            }
            if (sibling == KinToken.B || sibling == KinToken.Z) {
                add(if (sibling == KinToken.B) "叔叔" else "姑姑")
            }
        }
        return TermResult(
            primary = primary,
            alternatives = alternatives.distinct().filter { it != primary },
            selector = path.selector,
            confidence = path.confidence,
            known = true,
            explanation = explanation,
            pathPersonIds = path.steps.map { it.toPersonId },
        )
    }

    private fun parentSiblingSpouse(
        tokens: List<KinToken>,
        region: Region,
        path: KinshipPath,
    ): TermResult? {
        if (tokens.size != 3) return null
        if (tokens[2] != KinToken.W && tokens[2] != KinToken.H) return null
        val parent = tokens[0]
        val sibling = tokens[1]
        if (parent != KinToken.F && parent != KinToken.M) return null
        if (sibling !in setOf(KinToken.OB, KinToken.OS, KinToken.LB, KinToken.LS, KinToken.B, KinToken.Z)) return null

        val base = when {
            parent == KinToken.F && sibling in setOf(KinToken.OB, KinToken.B) -> "伯母"
            parent == KinToken.F && sibling == KinToken.LB -> "婶婶"
            parent == KinToken.F -> "姑父"
            sibling in setOf(KinToken.OB, KinToken.LB, KinToken.B) -> "舅妈"
            else -> "姨父"
        }
        return entryResult(base, terms[tokens.joinToString("-")]?.alternatives ?: emptyList(), path, region, "父/母的兄弟姐妹的配偶")
    }

    private fun cousin(
        tokens: List<KinToken>,
        ego: Person,
        target: Person,
        path: KinshipPath,
    ): TermResult? {
        if (tokens.size < 3) return null
        val last = tokens.last()
        if (last != KinToken.S && last != KinToken.D) return null
        val prefix = tokens.dropLast(1)
        val parentSibling = prefix.last()
        if (parentSibling !in setOf(KinToken.OB, KinToken.OS, KinToken.LB, KinToken.LS, KinToken.B, KinToken.Z)) return null

        val tang = prefix.first() == KinToken.F && parentSibling in setOf(KinToken.OB, KinToken.LB, KinToken.B)
        val older = when {
            ego.birthYear != null && target.birthYear != null -> target.birthYear < ego.birthYear
            else -> true
        }
        val base = if (tang) "堂" else "表"
        val primary: String
        val alt: String
        when {
            target.gender == Gender.MALE && older -> { primary = "${base}哥"; alt = "${base}弟" }
            target.gender == Gender.MALE -> { primary = "${base}弟"; alt = "${base}哥" }
            target.gender == Gender.FEMALE && older -> { primary = "${base}姐"; alt = "${base}妹" }
            target.gender == Gender.FEMALE -> { primary = "${base}妹"; alt = "${base}姐" }
            else -> { primary = "${base}兄弟"; alt = "${base}姐妹" }
        }
        return TermResult(
            primary = primary,
            alternatives = listOf(alt),
            selector = path.selector,
            confidence = path.confidence,
            known = true,
            explanation = if (tang) "父亲的兄弟的子女（堂）" else "表亲（父系姐妹/母系兄弟姐妹的子女）",
            pathPersonIds = path.steps.map { it.toPersonId },
        )
    }

    private fun nibling(
        tokens: List<KinToken>,
        target: Person,
        path: KinshipPath,
    ): TermResult? {
        if (tokens.size != 2) return null
        val sibling = tokens[0]
        val child = tokens[1]
        if (sibling !in setOf(KinToken.OB, KinToken.LB, KinToken.B, KinToken.OS, KinToken.LS, KinToken.Z)) return null
        if (child != KinToken.S && child != KinToken.D) return null

        val brother = sibling in setOf(KinToken.OB, KinToken.LB, KinToken.B)
        val base = when {
            brother && child == KinToken.S -> "侄子"
            brother && child == KinToken.D -> "侄女"
            child == KinToken.S -> "外甥"
            else -> "外甥女"
        }
        return TermResult(
            primary = base,
            alternatives = if (base == "侄子") listOf("侄儿") else emptyList(),
            selector = path.selector,
            confidence = path.confidence,
            known = true,
            explanation = if (brother) "兄弟的子女" else "姐妹的子女",
            pathPersonIds = path.steps.map { it.toPersonId },
        )
    }

    private fun entryResult(
        entry: TermEntry,
        path: KinshipPath,
        region: Region,
        explanation: String = "",
    ): TermResult {
        val primary = variant(entry, region)
        val alternatives = buildList {
            add(entry.standard)
            entry.north?.let(::add)
            entry.south?.let(::add)
            entry.cantonese?.let(::add)
            addAll(entry.alternatives)
        }.distinct().filter { it != primary }
        return TermResult(
            primary = primary,
            alternatives = alternatives,
            selector = path.selector,
            confidence = path.confidence,
            known = true,
            explanation = explanation,
            pathPersonIds = path.steps.map { it.toPersonId },
        )
    }

    private fun entryResult(
        base: String,
        alternatives: List<String>,
        path: KinshipPath,
        region: Region,
        explanation: String,
    ): TermResult {
        val entry = terms.entries.firstOrNull { it.value.standard == base }?.value
        val primary = entry?.let { variant(it, region) } ?: base
        return TermResult(
            primary = primary,
            alternatives = (alternatives + base).distinct().filter { it != primary },
            selector = path.selector,
            confidence = path.confidence,
            known = true,
            explanation = explanation,
            pathPersonIds = path.steps.map { it.toPersonId },
        )
    }

    private fun variant(entry: TermEntry, region: Region): String = when (region) {
        Region.NORTH -> entry.north ?: entry.standard
        Region.SOUTH -> entry.south ?: entry.standard
        Region.CANTONESE -> entry.cantonese ?: entry.standard
    }

    private fun rankTerm(prefix: String, base: String, order: Int?): String {
        if (order == null || order <= 0) return base
        val rank = when (order) {
            1 -> "大"
            2 -> "二"
            3 -> "三"
            4 -> "四"
            5 -> "五"
            else -> order.toString()
        }
        return "$rank$prefix"
    }
}
