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

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.crazydan.studio.app.ime.kuaizi.engine.ImeEngine
import org.crazydan.studio.app.ime.kuaizi.engine.log.ImeLog
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogStorage
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogWriter
import org.crazydan.studio.app.ime.kuaizi.engine.log.writer.FileLogWriter
import org.crazydan.studio.app.ime.kuaizi.log.LogcatWriter
import java.io.File

// -------------------------------------------------

/** Context 扩展属性，提供 IME 配置的 DataStore 实例。 */
private val Context.prefsDataStore: DataStore<Preferences> by preferencesDataStore("ime_config")

/** 顶层 object 延迟单例，以便于在 [IMEService] 和 Application 中共享配置。 */
private lateinit var configDataStore: ConfigDataStore

/** 初始化 [ConfigDataStore]：可多次调用，仅首次调用做真正的初始化，后续调用直接返回共享实例。 */
fun initConfigDataStore(context: Context): ConfigDataStore {
    if (!::configDataStore.isInitialized) {
        configDataStore = ConfigDataStore(context.prefsDataStore)
    }

    return configDataStore
}

/**
 * 监听 [ImeEngine] 的配置 [ImeConfig] 更新，并在其变更后，通过 [ConfigDataStore.updateConfig] 将该配置进行持久化保存。
 *
 * 注意，该函数将挂起当前协程，后续代码不会被执行。
 */
suspend inline fun updateConfigDataStoreWhenEngineConfigUpdated(configDataStore: ConfigDataStore, engine: ImeEngine) =
    engine.whenConfigUpdated { conf ->
        configDataStore.updateConfig { conf }
    }

// -------------------------------------------------

private var logInited: Boolean = false

/**
 * 初始化日志系统，并持续订阅配置变更：可多次调用，仅首次调用做真正的初始化。
 *
 * - Debug 构建：等级缺省为 DEBUG，使用 Logcat 输出，同时输出到文件
 * - Release 构建：等级缺省为 ERROR，输出到文件，并安装崩溃拦截器
 *
 * 注意，该函数将挂起当前协程，后续代码不会被执行，以持续监听配置变更。
 */
suspend fun initLog(configDataStore: ConfigDataStore, filesDir: File) {
    if (logInited) return
    logInited = true // 提前赋值，默认后续均不会发生异常

    val config = configDataStore.getConfig().engine

    // 缺省写入应用私有目录下的 logs/ 文件
    val dir = filesDir.resolve(config.logDir)
    val storage = LogStorage(dir)

    val writers = mutableListOf<LogWriter>(
        FileLogWriter(storage = storage)
    )

    if (BuildConfig.DEBUG) {
        writers.add(LogcatWriter())
    }

    ImeLog.setWriters(writers)
    ImeLog.enableLevel(config.logLevel!!)

    if (!BuildConfig.DEBUG) {
        ImeLog.CrashInterceptor(storage).install()
    }

    // --------------------
    // 仅在持久化配置更新后，再更新日志系统
    configDataStore.whenConfigUpdated { conf ->
        // Note：File#resolve 允许 logDir 为绝对路径
        val dir = filesDir.resolve(conf.engine.logDir)
        storage.changeDir(dir)

        ImeLog.enableLevel(conf.engine.logLevel!!)
    }
}
