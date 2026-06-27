package org.crazydan.studio.app.ime.kuaizi.engine.dict

class ViterbiDecoder(private val model: HmmModel) {
    data class Result(
        val states: List<HmmState>,
        val probability: Double,
    )

    fun decode(observations: List<HmmObservation>, topN: Int = 5): List<Result> {
        if (observations.isEmpty() || model.states.isEmpty()) return emptyList()

        var currentPaths: Map<List<HmmState>, Double> = model.initialProb.mapKeys {
            listOf(it.key)
        }

        for (obs in observations) {
            val newPaths = mutableMapOf<List<HmmState>, Double>()

            for ((path, prob) in currentPaths) {
                val lastState = path.last()
                val transitions = model.transitionProb[lastState] ?: continue

                for ((nextState, transProb) in transitions) {
                    val emission = model.emissionProb[nextState]?.get(obs) ?: continue
                    val newProb = prob + transProb + emission
                    val newPath = path + nextState

                    val currentBest = newPaths[newPath]
                    if (currentBest == null || newProb > currentBest) {
                        newPaths[newPath] = newProb
                    }
                }
            }

            currentPaths = newPaths.entries
                .sortedByDescending { it.value }
                .take(topN)
                .associate { it.toPair() }
        }

        return currentPaths.entries
            .sortedByDescending { it.value }
            .take(topN)
            .map { (path, prob) -> Result(path, prob) }
    }
}
