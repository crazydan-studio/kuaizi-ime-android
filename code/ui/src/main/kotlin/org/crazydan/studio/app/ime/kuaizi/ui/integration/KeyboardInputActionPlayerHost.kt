package org.crazydan.studio.app.ime.kuaizi.ui.integration

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
import org.crazydan.studio.app.ime.kuaizi.ui.player.InputActionPlayer
import org.crazydan.studio.app.ime.kuaizi.ui.player.InputActionPlayerState
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.KeyboardViewModel

@Composable
fun KeyboardInputActionPlayerHost(
    viewModel: KeyboardViewModel,
    useMode: UseMode,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        KeyboardHost(
            viewModel = viewModel,
            showIndicator = useMode != UseMode.DirectInput,
        )
        if (useMode == UseMode.Animation) {
            InputActionPlayerPanel(
                player = viewModel.actionPlayer,
            )
        }
    }
}

@Composable
fun InputActionPlayerPanel(
    player: InputActionPlayer,
    modifier: Modifier = Modifier,
) {
    val playbackState by player.playbackState.collectAsState()

    Row(
        modifier = modifier.fillMaxWidth().padding(8.dp),
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
