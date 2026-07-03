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

package org.crazydan.studio.app.ime.kuaizi

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig
import org.crazydan.studio.app.ime.kuaizi.engine.datastore.EngineConfigDataStore
import org.crazydan.studio.app.ime.kuaizi.engine.datastore.UiConfigDataStore
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogLevel

/**
 * 基于 DataStore 的配置持久化实现。
 *
 * 将 [ImeConfig] 中的 [ImeConfig.Engine] 和 [ImeConfig.Ui] 持久化到 Preferences DataStore。
 * 支持增量更新：仅将变化的部分写入 DataStore，避免全量写入。
 *
 * [ImeConfig.Runtime] 不做持久化，应用重启时根据 [ImeConfig.Startup] 重新初始化。
 */
class ConfigDataStore(private val dataStore: DataStore<Preferences>) {
    /**
     * 配置的响应式 Flow。
     *
     * 从 DataStore 读取原始 Preferences，映射为 [ImeConfig] 实例。
     * UI 层通过订阅此 Flow 实时响应配置变更。
     *
     * 注意，[updateConfig] 被调用后，将自动同步该配置数据。
     */
    val config: Flow<ImeConfig> = dataStore.data.map { prefs ->
        ImeConfig(
            engine = EngineConfigDataStore.readConfig(prefs).let {
                if (it.logLevel == null)
                    it.copy(
                        logLevel =
                            if (BuildConfig.DEBUG) LogLevel.DEBUG
                            else LogLevel.ERROR
                    )
                else it
            },
            ui = UiConfigDataStore.readConfig(prefs),
        )
    }

    suspend inline fun getConfig(): ImeConfig =
        config.first() // 取值并取消 collect 处理

    /**
     * 增量更新配置。
     *
     * 通过比较新旧配置的差异，仅将发生变化的字段写入 DataStore，
     * 避免每次 updateConfig 都全量写入所有 key。
     * 适用于高频配置切换场景（主题、音效开关的快速连续切换）。
     */
    suspend fun updateConfig(transform: (ImeConfig) -> ImeConfig) {
        val old = getConfig()
        val new = transform(old)

        if (old.engine == new.engine && old.ui == new.ui) return

        dataStore.edit { prefs ->
            EngineConfigDataStore.writeConfig(prefs, old.engine, new.engine)
            UiConfigDataStore.writeConfig(prefs, old.ui, new.ui)
        }
    }

    /** 监听持久化配置的更新。注意，该函数将阻塞当前线程。 */
    suspend inline fun whenConfigUpdated(collector: FlowCollector<ImeConfig>) =
        config
            .drop(1)  // 跳过订阅时的状态，仅关注后续的变化
            .collect(collector)
}