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

package org.crazydan.studio.app.ime.kuaizi.common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-12-09
 */
public class Async {
    private final ThreadPoolExecutor executor;

    public Async(int corePoolSize, int maximumPoolSize) {
        this.executor = new ThreadPoolExecutor(corePoolSize,
                                               maximumPoolSize,
                                               0L,
                                               TimeUnit.MILLISECONDS,
                                               new LinkedBlockingQueue<>());
    }

    public void shutdown(long ms) {
        try {
            this.executor.awaitTermination(ms, TimeUnit.MILLISECONDS);
        } catch (Exception ignore) {
        }

        this.executor.shutdown();
    }

    public CompletableFuture<Void> future(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, this.executor);
    }

    public <T> CompletableFuture<T> future(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, this.executor);
    }
}
