/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
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

package org.crazydan.studio.app.ime.kuaizi.common.log;

import android.util.Log;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-21
 */
public class Logger {
    private final String tag;

    public static Logger getLogger(Class<?> cls) {
        return new Logger(cls.getSimpleName());
    }

    public Logger(String tag) {
        this.tag = tag;
    }

    public void debug(String s, Object... args) {
        log(Log.DEBUG, s, args);
    }

    public void info(String s, Object... args) {
        log(Log.INFO, s, args);
    }

    public void error(String s, Object... args) {
        log(Log.ERROR, s, args);
    }

    private void log(int level, String s, Object... args) {
        String msg = String.format(s, args);
        Log.println(level, this.tag, msg);
    }
}
