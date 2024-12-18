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

package org.crazydan.studio.app.ime.kuaizi.common.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-12-09
 */
public class Async {

    public static void waitAndShutdown(ExecutorService executor, long ms) {
        try {
            executor.awaitTermination(ms, TimeUnit.MILLISECONDS);
        } catch (Exception ignore) {
        }

        executor.shutdown();
    }

    public static <T> T value(Future<T> f) {
        return value(f, null);
    }

    public static <T> T value(Future<T> f, T defaultVale) {
        try {
            return f != null ? f.get() : defaultVale;
        } catch (Exception e) {
            return defaultVale;
        }
    }
}
