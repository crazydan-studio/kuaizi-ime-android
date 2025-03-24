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
import java.util.Arrays;
import java.util.Date;
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
        public Logger beginTreeLog(String title, Supplier<Object[]> argsGetter) {return this;}

        @Override
        public void endTreeLog() {}

        @Override
        protected Logger log(int level, String msg, Supplier<Object[]> argsGetter) {return this;}
    };

    private static final LogCache cache = new LogCache();

    private final String tag;

    public static Logger getLogger(Class<?> cls) {
        if (!BuildConfig.DEBUG) {
            return noop;
        }
        return new Logger(cls.getSimpleName());
    }

    public static void enableLogCache(boolean enabled) {
        cache.enabled = enabled;
        if (!enabled) {
            cache.logs.clear();
        }
    }

    public static List<String> getCachedLogs() {
        return new ArrayList<>(cache.logs);
    }

    Logger(String tag) {
        this.tag = tag;
    }

    public Logger beginTreeLog(String title) {
        return beginTreeLog(title, null);
    }

    /** 通过 Lamdba 函数 延迟 获取格式化消息的参数，以避免发布版本中不必要代码的运行开销 */
    public Logger beginTreeLog(String title, Supplier<Object[]> argsGetter) {
        title = TreeLog.format(title, argsGetter);
        TreeLog.begin(this.tag, title);

        return this;
    }

    public void endTreeLog() {
        TreeLog.end();
    }

    public Logger debug(String msg) {
        return debug(msg, null);
    }

    /** 通过 Lamdba 函数 延迟 获取格式化消息的参数，以避免发布版本中不必要代码的运行开销 */
    public Logger debug(String msg, Supplier<Object[]> argsGetter) {
        return log(Log.DEBUG, msg, argsGetter);
    }

    public Logger warn(String msg) {
        return warn(msg, null);
    }

    /** 通过 Lamdba 函数 延迟 获取格式化消息的参数，以避免发布版本中不必要代码的运行开销 */
    public Logger warn(String msg, Supplier<Object[]> argsGetter) {
        return log(Log.WARN, msg, argsGetter);
    }

    public Logger info(String msg) {
        return info(msg, null);
    }

    /** 通过 Lamdba 函数 延迟 获取格式化消息的参数，以避免发布版本中不必要代码的运行开销 */
    public Logger info(String msg, Supplier<Object[]> argsGetter) {
        return log(Log.INFO, msg, argsGetter);
    }

    public Logger error(String msg) {
        return error(msg, null);
    }

    /** 通过 Lamdba 函数 延迟 获取格式化消息的参数，以避免发布版本中不必要代码的运行开销 */
    public Logger error(String msg, Supplier<Object[]> argsGetter) {
        return log(Log.ERROR, msg, argsGetter);
    }

    protected Logger log(int level, String msg, Supplier<Object[]> argsGetter) {
        TreeLog.log(level, this.tag, msg, argsGetter);

        return this;
    }

    private static class TreeLog {
        private static final ThreadLocal<Stack<TreeLog>> tree = new ThreadLocal<>();

        private final int level;
        private final String tag;
        private final String msg;
        private final long timestamp;

        private final List<TreeLog> children = new ArrayList<>();

        TreeLog(int level, String tag, String msg) {
            this.level = level;
            this.tag = tag;
            this.msg = msg;
            this.timestamp = new Date().getTime();
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
            sb.append("⤹˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜˜").append('\n');
            print(sb, log, 0, false);

            String msg = sb.toString();
            cache.cache(msg);

            Log.i("Kuaizi_IME_TreeLog", msg);
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

            if (args != null) {
                args = Arrays.stream(args)
                             .map(arg -> arg instanceof Class ? ((Class<?>) arg).getSimpleName() : arg)
                             .toArray();
                return String.format(msg, args);
            }
            return msg;
        }

        private static void print(StringBuffer sb, TreeLog log, int depth, boolean last) {
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
                sb.append("+ [").append(log.timestamp).append("][").append(log.tag).append("] ");
            } else {
                if (last) {
                    sb.append("└──");
                } else {
                    sb.append("├──");
                }
                sb.append(" [")
                  .append(log.timestamp)
                  .append("][")
                  .append(level)
                  .append("][")
                  .append(log.tag)
                  .append("] ");
            }
            sb.append(log.msg).append('\n');

            int total = log.children.size();
            for (int i = 0; i < total; i++) {
                TreeLog child = log.children.get(i);
                print(sb, child, depth + 1, i == total - 1);
            }
        }
    }

    private static class LogCache {
        private boolean enabled;
        private final List<String> logs = new ArrayList<>();

        public void cache(String log) {
            if (this.enabled) {
                this.logs.add(log);
            }
        }
    }
}
