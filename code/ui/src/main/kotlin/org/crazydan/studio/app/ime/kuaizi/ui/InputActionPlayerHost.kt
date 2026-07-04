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

package org.crazydan.studio.app.ime.kuaizi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.UseMode
import org.crazydan.studio.app.ime.kuaizi.ui.input_action.InputActionPlayer
import org.crazydan.studio.app.ime.kuaizi.ui.input_action.InputActionPlayerState

/**
 * 输入动作播放集成组件。
 *
 * 在 [KeyboardHost] 基础上叠加输入动作播放引擎，支持 [UseMode.Animation] 和 [UseMode.DirectInput] 两种使用模式。
 * Animation 模式下显示播放控制面板和指示器，DirectInput 模式下仅叠加指示器不显示控制面板。
 *
 * @param viewModel 键盘视图模型
 * @param useMode 使用模式
 * @param modifier 修饰符
 */
@Composable
fun InputActionPlayerHost(
    viewModel: KeyboardViewModel,
    useMode: UseMode,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        KeyboardHost(
            viewModel = viewModel,
            showIndicator = useMode != UseMode.DirectInput,
        )

        // Animation 模式下显示播放控制面板
        if (useMode == UseMode.Animation) {
            InputActionPlayerPanel(
                player = viewModel.actionPlayer,
            )
        }
    }
}

/**
 * 播放控制面板，提供播放/暂停、进度显示等控件。
 *
 * 根据播放器的不同状态呈现不同的操作按钮：
 * - [InputActionPlayerState.Idle]：显示"播放"按钮
 * - [InputActionPlayerState.Ready]：显示"开始"按钮
 * - [InputActionPlayerState.Playing]：显示"暂停"按钮和进度信息
 * - [InputActionPlayerState.Paused]：显示"继续"和"停止"按钮
 * - [InputActionPlayerState.Finished]：显示"重播"按钮
 *
 * @param player 输入动作播放器
 * @param modifier 修饰符
 */
@Composable
fun InputActionPlayerPanel(
    player: InputActionPlayer,
    modifier: Modifier = Modifier,
) {
    val playbackState by player.playbackState.collectAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (playbackState) {
            is InputActionPlayerState.Idle -> {
                Button(onClick = { player.play() }) { Text("播放") }
            }

            is InputActionPlayerState.Ready -> {
                Button(onClick = { player.play() }) { Text("开始") }
            }

            is InputActionPlayerState.Playing -> {
                Button(onClick = { player.pause() }) { Text("暂停") }
                val state = playbackState as InputActionPlayerState.Playing
                Text("${state.currentIndex}/${state.totalActions}")
            }

            is InputActionPlayerState.Paused -> {
                Button(onClick = { player.resume() }) { Text("继续") }
                Button(onClick = { player.stop() }) { Text("停止") }
            }

            is InputActionPlayerState.Finished -> {
                Button(onClick = { player.load(player.script ?: return@Button); player.play() }) { Text("重播") }
            }
        }
    }
}
