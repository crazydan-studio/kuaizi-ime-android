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

package org.crazydan.studio.app.ime.kuaizi.ui.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.OffsetF
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.RectF

/** 将绝对像素坐标 [Offset] 归一化到 [0,1] 范围 */
fun Offset.normalize(sourceSize: Size): OffsetF {
    require(sourceSize.width > 0f && sourceSize.height > 0f) {
        "Source size must be positive: $sourceSize"
    }
    return OffsetF(
        x = (x / sourceSize.width).coerceIn(0f, 1f),
        y = (y / sourceSize.height).coerceIn(0f, 1f),
    )
}

/** 将归一化坐标 [OffsetF] 反归一化为绝对像素坐标 [Offset] */
fun OffsetF.denormalize(targetSize: Size): Offset {
    return Offset(
        x = x * targetSize.width,
        y = y * targetSize.height,
    )
}

/** 将归一化矩形 [RectF] 反归一化为绝对像素矩形 [Rect] */
fun RectF.denormalize(targetSize: Size): Rect {
    return Rect(
        left = left * targetSize.width,
        top = top * targetSize.height,
        right = right * targetSize.width,
        bottom = bottom * targetSize.height,
    )
}

/**
 * 坐标归一化工具。
 *
 * 在绝对像素坐标与归一化坐标 [0,1]x[0,1] 之间进行双向转换。
 * 归一化坐标使得手势反馈数据可以跨面板、跨 Zone 使用，无需关心面板的实际像素尺寸。
 *
 * 归一化坐标的约定：
 * - (0, 0) 对应面板左上角
 * - (1, 1) 对应面板右下角
 * - X 轴向右为正，Y 轴向下为正
 */
object CoordinateNormalizer {
    /**
     * 将绝对坐标归一化
     * @param offset 绝对像素坐标
     * @param sourceSize 坐标来源面板的尺寸
     * @return 归一化坐标 [0,1] x [0,1]
     */
    fun normalize(offset: Offset, sourceSize: Size): OffsetF {
        require(sourceSize.width > 0f && sourceSize.height > 0f) {
            "Source size must be positive: $sourceSize"
        }
        return OffsetF(
            x = (offset.x / sourceSize.width).coerceIn(0f, 1f),
            y = (offset.y / sourceSize.height).coerceIn(0f, 1f),
        )
    }

    /**
     * 将归一化坐标反归一化为绝对坐标
     * @param normalized 归一化坐标
     * @param targetSize 目标面板的尺寸
     * @return 绝对像素坐标
     */
    fun denormalize(normalized: OffsetF, targetSize: Size): Offset {
        return Offset(
            x = normalized.x * targetSize.width,
            y = normalized.y * targetSize.height,
        )
    }

    /**
     * 将归一化矩形反归一化为绝对矩形
     * @param normalized 归一化矩形
     * @param targetSize 目标面板的尺寸
     * @return 绝对像素矩形
     */
    fun denormalize(normalized: RectF, targetSize: Size): Rect {
        return Rect(
            left = normalized.left * targetSize.width,
            top = normalized.top * targetSize.height,
            right = normalized.right * targetSize.width,
            bottom = normalized.bottom * targetSize.height,
        )
    }
}
