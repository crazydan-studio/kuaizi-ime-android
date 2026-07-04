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

package org.crazydan.studio.app.ime.kuaizi.ui.keyboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.OffsetF
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.RectF
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.InputKey

/**
 * 按键布局状态，含空间格网索引。
 *
 * 位置信息使用归一化坐标 [0,1]x[0,1] 存储，
 * 被 [GestureInputPanel]、[GestureFeedbackPanel]、
 * [ComposeInputActionPositionResolver] 等多个组件共同引用。
 *
 * @param keyPositions 按键归一化位置映射
 * @param spatialGrid 空间格网索引：将键盘划分为 gridCols×gridRows 个格子
 * @param panelSize 面板实际尺寸（像素），用于归一化坐标转换
 */
data class KeyLayoutState(
    val keyPositions: Map<InputKey, RectF> = emptyMap(),
    val spatialGrid: SpatialGrid = SpatialGrid(),
    val panelSize: Size = Size.Zero,
) {
    /**
     * 空间格网索引。
     *
     * 将归一化键盘区域划分为 gridCols × gridRows 个格子，
     * 每个格子预计算其覆盖的按键列表。
     * 触摸事件时先通过坐标定位到格子，再检测该格子内的少量按键，
     * 减少每次触摸事件的按键检测次数（从 30 次降至 3-5 次）。
     */
    class SpatialGrid(
        val gridCols: Int = 4,
        val gridRows: Int = 3,
    ) {
        // 二维格网数组，每个格子存储该区域覆盖的按键列表
        private val grid: Array<Array<MutableList<InputKey>>> =
            Array(gridRows) { Array(gridCols) { mutableListOf() } }

        /** 构建索引：将按键按归一化位置分配到对应格子 */
        fun buildIndex(keyPositions: Map<InputKey, RectF>) {
            grid.forEach { row -> row.forEach { it.clear() } }
            keyPositions.forEach { (key, rect) ->
                // 根据按键中心坐标计算所属格子
                val colStart = (rect.center.x * gridCols).toInt().coerceIn(0, gridCols - 1)
                val rowStart = (rect.center.y * gridRows).toInt().coerceIn(0, gridRows - 1)
                grid[rowStart][colStart].add(key)
            }
        }

        /** 查找指定归一化坐标所在的格子中的按键列表 */
        fun findCandidateKeys(normalizedPos: OffsetF): List<InputKey> {
            val col = (normalizedPos.x * gridCols).toInt().coerceIn(0, gridCols - 1)
            val row = (normalizedPos.y * gridRows).toInt().coerceIn(0, gridRows - 1)
            return grid[row][col]
        }
    }

    /** 将归一化矩形转换为指定面板尺寸下的像素矩形 */
    fun denormalize(normalized: RectF, targetSize: Size): Rect {
        return Rect(
            left = normalized.left * targetSize.width,
            top = normalized.top * targetSize.height,
            right = normalized.right * targetSize.width,
            bottom = normalized.bottom * targetSize.height,
        )
    }

    /** 使用空间格网索引查找指定像素坐标处的按键 */
    fun findKeyAt(position: Offset, targetSize: Size): InputKey? {
        // 先将像素坐标归一化
        val normalized = OffsetF(
            x = (position.x / targetSize.width).coerceIn(0f, 1f),
            y = (position.y / targetSize.height).coerceIn(0f, 1f),
        )
        // 通过格网定位候选按键（通常 3-5 个）
        val candidates = spatialGrid.findCandidateKeys(normalized)
        // 仅检测候选按键中的精确命中
        return candidates.firstOrNull { key ->
            val rect = keyPositions[key] ?: return@firstOrNull false
            denormalize(rect, targetSize).contains(position)
        }
    }
}

/**
 * 候选列表面板布局状态，用于坐标解析和输入动作播放期间指示器定位。
 *
 * @param candidatePositions 各候选项的位置矩形（屏幕坐标系）
 * @param panelSize 面板实际尺寸，用于归一化坐标转换
 */
data class CandidateListLayoutState(
    val candidatePositions: List<Rect>,
    val panelSize: Size = Size.Zero,
) {
    /** 查找指定候选项的中心坐标 */
    fun locateItem(index: Int): Offset? {
        return candidatePositions.getOrNull(index)?.center
    }
}

/**
 * 输入列表面板布局状态，用于坐标解析和输入动作播放期间指示器定位。
 *
 * @param itemPositions 各输入项的位置矩形（屏幕坐标系）
 * @param panelSize 面板实际尺寸，用于归一化坐标转换
 */
data class InputListLayoutState(
    val itemPositions: List<Rect>,
    val panelSize: Size = Size.Zero,
) {
    /** 查找指定输入项的中心坐标 */
    fun locateItem(index: Int): Offset? {
        return itemPositions.getOrNull(index)?.center
    }
}
