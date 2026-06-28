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

/**
 * 隐马尔可夫模型的隐藏状态（汉字），使用 [@JvmInline] value class 避免装箱开销。
 */
@JvmInline
value class HmmState(val value: String)

/**
 * 隐马尔可夫模型的观测值（拼音），使用 [@JvmInline] value class 避免装箱开销。
 */
@JvmInline
value class HmmObservation(val value: String)

/**
 * 隐马尔可夫模型，用于上下文感知的短语预测。
 *
 * 将拼音序列视为观测序列，将汉字序列视为隐藏状态序列，通过转移概率和发射概率
 * 建模拼音到汉字的映射关系。核心应用场景是：用户输入一个字后，引擎根据上下文
 * 预测下一个可能的字或词组，将预测结果排序后插入候选列表前端。
 *
 * 三个概率矩阵使用不可变 [Map] 存储，查询操作为纯函数。
 *
 * @property states 所有可能的隐藏状态（汉字）集合
 * @property observations 所有可能的观测值（拼音）集合
 * @property initialProb 初始状态概率分布
 * @property transitionProb 状态转移概率矩阵：从 [HmmState] 到 [HmmState] 的概率映射
 * @property emissionProb 发射概率矩阵：从 [HmmState] 到 [HmmObservation] 的概率映射
 *
 * @see ViterbiDecoder 维特比解码器，基于此模型求解最可能的汉字序列
 */
data class HmmModel(
    val states: Set<HmmState>,
    val observations: Set<HmmObservation>,
    val initialProb: Map<HmmState, Double>,
    val transitionProb: Map<HmmState, Map<HmmState, Double>>,
    val emissionProb: Map<HmmState, Map<HmmObservation, Double>>,
) {
    /** 获取从 [from] 状态转移到 [to] 状态的概率，不存在时返回 0.0。 */
    fun transitionProb(from: HmmState, to: HmmState): Double =
        transitionProb[from]?.get(to) ?: 0.0

    /** 获取 [state] 状态发射 [observation] 观测值的概率，不存在时返回 0.0。 */
    fun emissionProb(state: HmmState, observation: HmmObservation): Double =
        emissionProb[state]?.get(observation) ?: 0.0

    /** 获取 [state] 的初始概率，不存在时返回 0.0。 */
    fun initialProb(state: HmmState): Double =
        initialProb[state] ?: 0.0
}
