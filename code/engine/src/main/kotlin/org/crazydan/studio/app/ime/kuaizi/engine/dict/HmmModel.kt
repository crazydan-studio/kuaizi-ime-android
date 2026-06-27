package org.crazydan.studio.app.ime.kuaizi.engine.dict

@JvmInline
value class HmmState(val value: String)

@JvmInline
value class HmmObservation(val value: String)

data class HmmModel(
    val states: Set<HmmState> = emptySet(),
    val observations: Set<HmmObservation> = emptySet(),
    val initialProb: Map<HmmState, Double> = emptyMap(),
    val transitionProb: Map<HmmState, Map<HmmState, Double>> = emptyMap(),
    val emissionProb: Map<HmmState, Map<HmmObservation, Double>> = emptyMap(),
    val weight: Double = 0.3,
)
