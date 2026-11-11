package com.helloauntie.core.kinship

/**
 * Facade for kinship term calculation.
 *
 * Usage:
 *   val engine = KinshipEngine(graph, Region.SOUTH)
 *   val result = engine.term(egoId = 1, targetId = 5)
 */
class KinshipEngine(
    val graph: KinshipGraph,
    val region: Region = Region.SOUTH,
) {

    fun term(egoId: Long, targetId: Long): TermResult {
        val ego = graph.person(egoId)
            ?: return TermResult("未知", selector = "", confidence = 0.0, known = false, explanation = "找不到‘我’")
        val target = graph.person(targetId)
            ?: return TermResult("未知", selector = "", confidence = 0.0, known = false, explanation = "找不到目标人物")

        if (egoId == targetId) {
            return TermResult(
                primary = "我",
                selector = "我",
                confidence = 1.0,
                known = true,
                explanation = "当前使用者",
                pathPersonIds = listOf(egoId),
            )
        }

        val path = graph.shortestPath(egoId, targetId)
            ?: return TermResult(
                primary = "亲戚",
                selector = "",
                confidence = 0.0,
                known = false,
                explanation = "关系未连接：请先录入关系链",
            )

        return TermBook.resolve(path, ego, target, region)
            ?: TermResult(
                primary = "亲戚",
                selector = path.selector,
                confidence = path.confidence,
                known = false,
                explanation = "关系链超出内置词典，可自定义叫法",
                pathPersonIds = path.steps.map { it.toPersonId },
            )
    }

    fun inferredRelations(): List<InferredRelation> = graph.inferredRelations
}
