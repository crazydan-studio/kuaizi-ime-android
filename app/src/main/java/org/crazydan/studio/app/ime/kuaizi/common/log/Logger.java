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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;

import android.util.Log;
import org.crazydan.studio.app.ime.kuaizi.BuildConfig;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-21
 */
public class Logger {
    /** 不做日志处理 */
    private static final Logger noop = new Logger(null) {
        @Override
        public void beginTreeLog(String title, Supplier<Object[]> argsGetter) {}

        @Override
        public void endTreeLog() {}

        @Override
        protected void log(int level, String msg, Supplier<Object[]> argsGetter) {}
    };

    private final String tag;

    public static Logger getLogger(Class<?> cls) {
        if (!BuildConfig.DEBUG) {
            return noop;
        }
        return new Logger(cls.getSimpleName());
    }

    Logger(String tag) {
        this.tag = tag;
    }

    public void beginTreeLog(String title) {
        beginTreeLog(title, null);
    }

    /** 通过 Lamdba 函数 延迟 获取格式化消息的参数，以避免发布版本中不必要代码的运行开销 */
    public void beginTreeLog(String title, Supplier<Object[]> argsGetter) {
        title = TreeLog.format(title, argsGetter);

        TreeLog.begin(this.tag, title);
    }

    public void endTreeLog() {
        TreeLog.end();
    }

    public void debug(String msg) {
        debug(msg, null);
    }

    /** 通过 Lamdba 函数 延迟 获取格式化消息的参数，以避免发布版本中不必要代码的运行开销 */
    public void debug(String msg, Supplier<Object[]> argsGetter) {
        log(Log.DEBUG, msg, argsGetter);
    }

    protected void log(int level, String msg, Supplier<Object[]> argsGetter) {
        TreeLog.log(level, this.tag, msg, argsGetter);
    }

    private static class TreeLog {
        private static final ThreadLocal<Stack<TreeLog>> tree = new ThreadLocal<>();

        private final int level;
        private final String tag;
        private final String msg;

        private final List<TreeLog> children = new ArrayList<>();

        TreeLog(int level, String tag, String msg) {
            this.level = level;
            this.tag = tag;
            this.msg = msg;
        }

        public static synchronized void begin(String tag, String title) {
            Stack<TreeLog> stack = tree.get();
            if (stack == null) {
                stack = new Stack<>();
                tree.set(stack);
            }

            TreeLog log = new TreeLog(-1, tag, title);

            TreeLog parent = stack.isEmpty() ? null : stack.peek();
            if (parent != null) {
                parent.children.add(log);
            }
            stack.push(log);
        }

        public static synchronized void end() {
            Stack<TreeLog> stack = tree.get();
            if (stack == null || stack.isEmpty()) {
                return;
            }

            TreeLog log = stack.pop();
            if (!stack.isEmpty()) {
                return;
            }

            StringBuffer sb = new StringBuffer();
            sb.append('\n');
            print(sb, log, 0);

            Log.i("Kuaizi_IME_TreeLog", sb.toString());
        }

        public static void log(int level, String tag, String msg, Supplier<Object[]> argsGetter) {
            Stack<TreeLog> stack = tree.get();

            msg = format(msg, argsGetter);
            if (stack == null || stack.isEmpty()) {
                Log.println(level, tag, msg);
            } else {
                TreeLog parent = stack.peek();
                TreeLog child = new TreeLog(level, tag, msg);

                parent.children.add(child);
            }
        }

        public static String format(String msg, Supplier<Object[]> argsGetter) {
            Object[] args = argsGetter != null ? argsGetter.get() : null;

            return args != null ? String.format(msg, args) : msg;
        }

        private static void print(StringBuffer sb, TreeLog log, int depth) {
            String indents = depth > 0 ? String.format("%" + (depth * 2) + "s", "") : "";

            String level = null;
            switch (log.level) {
                case Log.DEBUG: {
                    level = "DEBUG";
                    break;
                }
                case Log.INFO: {
                    level = "INFO";
                    break;
                }
                case Log.WARN: {
                    level = "WARN";
                    break;
                }
                case Log.ERROR: {
                    level = "ERROR";
                    break;
                }
            }

            sb.append(indents);
            if (level == null) {
                sb.append("+ [").append(log.tag).append("] ");
            } else {
                sb.append("|- [").append(level).append("][").append(log.tag).append("] ");
            }
            sb.append(log.msg).append('\n');

            log.children.forEach((child) -> print(sb, child, depth + 1));
        }
    }
}
