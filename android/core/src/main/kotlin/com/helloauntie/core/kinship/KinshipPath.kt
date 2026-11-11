package com.helloauntie.core.kinship

import com.helloauntie.core.model.RelationType

/**
 * Primitive relations used by the kinship path algebra.
 * B/Z are used when the elder/younger relation is unknown (e.g. inferred siblings).
 */
enum class KinToken(val label: String) {
    F("爸爸"),
    M("妈妈"),
    H("丈夫"),
    W("妻子"),
    S("儿子"),
    D("女儿"),
    OB("哥哥"),
    OS("姐姐"),
    LB("弟弟"),
    LS("妹妹"),
    B("兄弟"),
    Z("姐妹"),
}

data class KinshipStep(
    val toPersonId: Long,
    val token: KinToken,
    val confidence: Double,
    val relationType: RelationType,
)

data class KinshipPath(
    val steps: List<KinshipStep>,
) {
    val tokens: List<KinToken> get() = steps.map { it.token }
    val selector: String get() = steps.joinToString("的") { it.token.label }
    val confidence: Double get() = steps.minOfOrNull { it.confidence } ?: 1.0
    val length: Int get() = steps.size
}
