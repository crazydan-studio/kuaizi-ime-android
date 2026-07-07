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

package org.crazydan.studio.app.ime.kuaizi.dict.predict

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 维特比解码器，实现维特比算法求解最可能的隐藏状态序列（汉字序列）。
 *
 * 维特比算法是动态规划算法，时间复杂度为 O(n·k²)，其中 n 为观测序列长度，
 * k 为状态数量。对于拼音输入场景，n 通常为 2-6（词组长度），k 为候选字数量
 * （通常 < 50），计算开销可控。
 *
 * 算法三个阶段：
 * 1. 初始化：计算初始概率 × 发射概率
 * 2. 递推：动态规划计算最大概率路径
 * 3. 回溯：从终态回溯获取完整路径
 *
 * [decode] 方法在 [Dispatchers.Default] 上执行，避免阻塞主线程。
 *
 * @param model 隐马尔可夫模型，提供概率矩阵
 */
class ViterbiDecoder(private val model: HmmModel) {

    /**
     * 解码结果，包含最可能的隐藏状态序列及其概率。
     *
     * @property states 解码出的隐藏状态（汉字）序列
     * @property probability 该路径的概率值
     */
    data class DecodeResult(
        val states: List<HmmState>,
        val probability: Double,
    )

    /**
     * 对观测序列执行维特比解码，返回 Top-5 候选路径。
     *
     * @param observations 观测序列（拼音序列）
     * @return 按概率降序排列的解码结果列表，最多 5 条
     */
    suspend fun decode(observations: List<HmmObservation>): List<DecodeResult> =
        withContext(Dispatchers.Default) {
            if (observations.isEmpty()) return@withContext emptyList()

            val results = mutableListOf<DecodeResult>()
            // viterbi[t][state] = 到时刻 t 为止、以 state 结尾的最优路径概率
            val viterbi = Array(observations.size) { mutableMapOf<HmmState, Double>() }
            // backpointer[t][state] = 最优路径中 state 在时刻 t-1 的前驱状态
            val backpointer = Array(observations.size) { mutableMapOf<HmmState, HmmState>() }

            // === 阶段一：初始化 ===
            // 计算初始概率 × 发射概率，筛选出概率 > 0 的起始状态
            for (state in model.states) {
                val p = model.initialProb(state) * model.emissionProb(state, observations[0])
                if (p > 0.0) {
                    viterbi[0][state] = p
                }
            }

            // === 阶段二：递推 ===
            // 对每个后续时间步，计算前驱状态到当前状态的最大概率
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

            // === 阶段三：回溯 ===
            // 从终态按概率降序回溯，取 Top-5 完整路径
            val lastStates = viterbi.last().entries.sortedByDescending { it.value }
            for (entry in lastStates.take(5)) {
                val path = mutableListOf<HmmState>()
                var state: HmmState = entry.key
                for (t in observations.size - 1 downTo 1) {
                    path.add(0, state)
                    state = backpointer[t][state] ?: break
                }
                path.add(0, state)
                // 仅保留完整路径（防止回溯中断导致部分路径）
                if (path.size == observations.size) {
                    results.add(DecodeResult(path, entry.value))
                }
            }

            results
        }
}
