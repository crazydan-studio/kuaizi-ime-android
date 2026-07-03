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

package org.crazydan.studio.app.ime.kuaizi.engine.util

import android.content.Context
import android.content.res.Configuration
import android.view.inputmethod.InputMethodManager
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputMethodSubtype
import org.crazydan.studio.app.ime.kuaizi.engine.domain.ScreenOrientation

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-06-28
 */
object SystemHelper {

    /** 得到系统输入法子类型 */
    fun getInputMethodSubtype(context: Context): InputMethodSubtype {
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val subtype = manager.currentInputMethodSubtype

        return if ("en_US" == subtype?.locale || "en_US" == subtype?.languageTag) {
            InputMethodSubtype.Latin
        } else {
            InputMethodSubtype.Hans
        }
    }

    /** 确定键盘布局方向  */
    fun getScreenOrientation(context: Context): ScreenOrientation {
        return when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> ScreenOrientation.Landscape
            else -> ScreenOrientation.Portrait
        }
    }
}