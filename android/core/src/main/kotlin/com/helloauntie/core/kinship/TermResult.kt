package com.helloauntie.core.kinship

enum class Region(val label: String) {
    NORTH("北方"),
    SOUTH("南方"),
    CANTONESE("粤语"),
}

data class TermResult(
    val primary: String,
    val alternatives: List<String> = emptyList(),
    val selector: String,
    val confidence: Double,
    val known: Boolean,
    val explanation: String = "",
    val pathPersonIds: List<Long> = emptyList(),
)
