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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ViterbiDecoder(private val model: HmmModel) {

    data class DecodeResult(
        val states: List<HmmState>,
        val probability: Double,
    )

    suspend fun decode(observations: List<HmmObservation>): List<DecodeResult> =
        withContext(Dispatchers.Default) {
            if (observations.isEmpty()) return@withContext emptyList()

            val results = mutableListOf<DecodeResult>()
            val viterbi = Array(observations.size) { mutableMapOf<HmmState, Double>() }
            val backpointer = Array(observations.size) { mutableMapOf<HmmState, HmmState>() }

            for (state in model.states) {
                val p = model.initialProb(state) * model.emissionProb(state, observations[0])
                if (p > 0.0) {
                    viterbi[0][state] = p
                }
            }

            for (t in 1 until observations.size) {
                for (currState in model.states) {
                    var maxProb = 0.0
                    var bestPrev: HmmState? = null
                    for (prevState in viterbi[t - 1].keys) {
                        val p = viterbi[t - 1][prevState]!! *
                            model.transitionProb(prevState, currState) *
                            model.emissionProb(currState, observations[t])
                        if (p > maxProb) {
                            maxProb = p
                            bestPrev = prevState
                        }
                    }
                    if (maxProb > 0.0 && bestPrev != null) {
                        viterbi[t][currState] = maxProb
                        backpointer[t][currState] = bestPrev
                    }
                }
            }

            val lastStates = viterbi.last().entries.sortedByDescending { it.value }
            for (entry in lastStates.take(5)) {
                val path = mutableListOf<HmmState>()
                var state: HmmState = entry.key
                for (t in observations.size - 1 downTo 1) {
                    path.add(0, state)
                    state = backpointer[t][state] ?: break
                }
                path.add(0, state)
                if (path.size == observations.size) {
                    results.add(DecodeResult(path, entry.value))
                }
            }

            results
        }
}
