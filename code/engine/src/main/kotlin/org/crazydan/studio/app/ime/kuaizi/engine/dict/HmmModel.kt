package org.crazydan.studio.app.ime.kuaizi.engine.dict

@JvmInline
value class HmmState(val value: String)

@JvmInline
value class HmmObservation(val value: String)

data class HmmModel(
    val states: Set<HmmState>,
    val observations: Set<HmmObservation>,
    val initialProb: Map<HmmState, Double>,
    val transitionProb: Map<HmmState, Map<HmmState, Double>>,
    val emissionProb: Map<HmmState, Map<HmmObservation, Double>>,
) {
    fun transitionProb(from: HmmState, to: HmmState): Double =
        transitionProb[from]?.get(to) ?: 0.0

    fun emissionProb(state: HmmState, observation: HmmObservation): Double =
        emissionProb[state]?.get(observation) ?: 0.0

    fun initialProb(state: HmmState): Double =
        initialProb[state] ?: 0.0
}
