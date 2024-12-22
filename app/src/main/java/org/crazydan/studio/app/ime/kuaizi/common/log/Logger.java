/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
