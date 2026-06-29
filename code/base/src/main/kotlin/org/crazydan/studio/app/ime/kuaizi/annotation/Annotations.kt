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

package org.crazydan.studio.app.ime.kuaizi.annotation

/**
 * 标记一个 DataStore 配置类。
 *
 * 被此注解标注的类将由 DataStore 配置的代码生成处理器自动生成
 * 对应的 DataStore Keys 对象和 read/write 方法。
 * 生成的代码包含：
 * - 一个 object（命名格式：`{ClassName}DataStore`）
 * - 每个属性对应的 Preferences.Key 常量
 * - `readConfig(prefs: Preferences): {ClassName}` 读取方法
 * - `writeConfig(prefs: MutablePreferences, old: {ClassName}, new: {ClassName})` 写入方法
 *
 * @param prefix Preferences Key 的前缀，默认使用类名首字母小写
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DataStoreConfig(
    val prefix: String = "",
)

/**
 * 标记 DataStore 配置类中的属性。
 *
 * 可自定义属性在 Preferences 中的 Key 名称，
 * 默认使用属性名的蛇形命名（snake_case）作为 Key。
 *
 * @param name 自定义的 Preferences Key 名称，空字符串表示使用默认命名
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class DataStoreConfigKey(
    val name: String = "",
)