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

package org.crazydan.studio.app.ime.kuaizi.ui.viewmodel

import kotlinx.coroutines.flow.*
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.InputActionFingerIndicator
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.OffsetF

/**
 * 手势反馈状态，使用归一化坐标，纯视觉反馈。
 *
 * 所有坐标数据以归一化形式 [0,1]x[0,1] 存储，
 * 绘制时由 [GestureFeedbackPanel] 根据面板实际尺寸转换为像素坐标。
 *
 * 采用生产者-消费者模式：
 * - 生产者：[GestureInputPanel]（用户手势）、[InputActionPlayer]（程序化播放）
 * - 消费者：[GestureFeedbackPanel]（渲染轨迹和指示器）
 */
class GestureFeedbackState {
    /** 触摸轨迹点（归一化坐标），由手势过程中实时积累或播放器插值生成 */
    private val _touchTrailPoints = MutableStateFlow<List<OffsetF>>(emptyList())
    val touchTrailPoints: StateFlow<List<OffsetF>> = _touchTrailPoints.asStateFlow()

    /** 当前按下的按键集合（临时高亮），手势结束后清除 */
    private val _pressedKeys = MutableStateFlow<Set<InputKey>>(emptySet())
    val pressedKeys: StateFlow<Set<InputKey>> = _pressedKeys.asStateFlow()

    /** 手指指示器状态（归一化坐标），由 InputActionPlayer 驱动 */
    private val _fingerIndicator = MutableStateFlow<InputActionFingerIndicator?>(null)
    val fingerIndicator: StateFlow<InputActionFingerIndicator?> = _fingerIndicator.asStateFlow()

    /** 添加单个归一化轨迹点 */
    fun addTouchTrailPoint(normalizedPoint: OffsetF) {
        _touchTrailPoints.update { list ->
            val mutable = list.toMutableList()
            mutable.add(normalizedPoint)
            mutable
        }
    }

    /** 设置完整轨迹（含按键间路径的插值点） */
    fun setTouchTrailPoints(points: List<OffsetF>) {
        _touchTrailPoints.value = points
    }

    /** 清除触摸轨迹 */
    fun clearTouchTrail() {
        _touchTrailPoints.value = emptyList()
    }

    /** 设置按下的按键集合 */
    fun setPressedKeys(keys: Set<InputKey>) {
        _pressedKeys.value = keys
    }

    /** 清除按下的按键集合 */
    fun clearPressedKeys() {
        _pressedKeys.value = emptySet()
    }

    /** 设置手指指示器状态 */
    fun setFingerIndicator(state: InputActionFingerIndicator?) {
        _fingerIndicator.value = state
    }

    /** 手势结束，清除所有临时反馈 */
    fun clearAll() {
        clearTouchTrail()
        clearPressedKeys()
    }

    /** 清理所有状态（含手指指示器） */
    fun clear() {
        clearAll()
        _fingerIndicator.value = null
    }
}
