/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

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
