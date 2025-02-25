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

package org.crazydan.studio.app.ime.kuaizi.core;

import java.util.Objects;
import java.util.function.Consumer;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.os.PersistableBundle;
import org.crazydan.studio.app.ime.kuaizi.common.Immutable;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;

/**
 * 剪贴板
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-02-21
 */
public class Clipboard {
    /** 多长间隔内的剪贴数据可以用于自动粘贴 */
    private final static long MAX_DURATION_TO_BE_AUTO_PASTED = 30 * 1000;

    protected final Logger log = Logger.getLogger(getClass());

    private final ClipboardManager manager;

    /** 最近的剪贴板数据 */
    private Data latest;

    public Clipboard(ClipboardManager manager) {
        this.manager = manager;
    }

    // =============================== Start: 消息处理 ===================================

    /** 响应来自上层派发的 {@link UserInputMsg} 消息 */
    public void onMsg(ClipboardContext context, UserInputMsg msg) {
        // TODO 显示剪贴板、粘贴选中内容、操作剪贴板

        switch (msg.type) {
            default: {
                this.log.warn("Do not handle message %s", () -> new Object[] { msg.type });
            }
        }
    }

    // =============================== End: 消息处理 ===================================

    public void start(ClipboardContext context) {
        Data data = readClipData(this.manager);
        if (data == null) {
            return;
        }

        if (this.latest != null && this.latest.isSameWith(data)) {
            if (this.latest.usedAt > 0
                || Math.abs(this.latest.createdAt - data.createdAt) > MAX_DURATION_TO_BE_AUTO_PASTED) {
                return;
            }
        } else {
            this.latest = data;
        }

        // TODO 分析数据，拆分为多个可粘贴内容，交给上层显示
        // - 仅「收藏」的复制内容才会被保存
        // - 内容需完全或部分脱敏
        // - 正则表达式提取 6-8 位数字作为验证码，提示的验证码用星号脱敏
        // - 提供 将标点符号替换为下划线 后的剪贴结果
    }

    private Data readClipData(ClipboardManager manager) {
        ClipData clip = manager.getPrimaryClip();
        ClipData.Item item = clip != null ? clip.getItemAt(0) : null;

        if (item == null) {
            return null;
        }

        PersistableBundle extras = clip.getDescription().getExtras();
        boolean sensitive = extras != null && extras.containsKey(ClipDescription.EXTRA_IS_SENSITIVE);

        Data data = Data.build((b) -> {
            b.sensitive(sensitive);

            if (item.getText() != null) {
                b.type(Data.Type.text).content(item.getText().toString());
            } else if (item.getUri() != null) {
                b.type(Data.Type.uri).content(item.getUri().toString());
            }
        });
        return data.type == null ? null : data;
    }

    /** {@link Clipboard} 的数据 */
    public static class Data extends Immutable {
        private final static Builder builder = new Builder();

        /** 类型（文本、图像。。。） */
        public final Type type;
        /** 内容 */
        public final String content;

        /** 创建时间 */
        public final long createdAt;
        /** 最近粘贴时间 */
        public final long usedAt;
        /** 粘贴次数 */
        public final long usedCount;
        /**
         * 是否敏感内容：密码、银行卡号等，由系统确定得出。
         * 检测得到的验证码等也将标记为敏感
         */
        public final boolean sensitive;

        /** 构建 {@link Data} */
        public static Data build(Consumer<Builder> c) {
            return Builder.build(builder, c);
        }

        /** 创建副本 */
        public Data copy(Consumer<Builder> c) {
            return Builder.copy(builder, this, c);
        }

        protected Data(Builder builder) {
            super(builder);

            this.type = builder.type;
            this.content = builder.content;

            this.createdAt = builder.createdAt;
            this.usedAt = builder.usedAt;
            this.usedCount = builder.usedCount;
            this.sensitive = builder.sensitive;
        }

        public boolean isSameWith(Data data) {
            return Objects.equals(this.type, data.type) && Objects.equals(this.content, data.content);
        }

        /** {@link Data} 的类型 */
        public enum Type {
            /** 文本 */
            text,
            /** 文件、图片等的 URI 地址 */
            uri,
        }

        /** {@link Data} 的构建器 */
        public static class Builder extends Immutable.Builder<Data> {
            private Type type;
            private String content;

            private long createdAt;
            private long usedAt;
            private long usedCount;
            private boolean sensitive;

            // ===================== Start: 构建函数 ===================

            @Override
            protected Data build() {
                return new Data(this);
            }

            @Override
            protected void doCopy(Data source) {
                super.doCopy(source);

                this.type = source.type;
                this.content = source.content;
                this.createdAt = source.createdAt;
                this.usedAt = source.usedAt;
                this.usedCount = source.usedCount;
                this.sensitive = source.sensitive;
            }

            @Override
            protected void reset() {
                this.type = null;
                this.content = null;

                this.createdAt = 0;
                this.usedAt = 0;
                this.usedCount = 0;
                this.sensitive = false;
            }

            @Override
            public int hashCode() {
                return Objects.hash(this.type,
                                    this.content,
                                    this.createdAt,
                                    this.usedAt,
                                    this.usedCount,
                                    this.sensitive);
            }

            // ===================== End: 构建函数 ===================

            // ===================== Start: 构建配置 ===================

            /** @see Data#type */
            public Builder type(Type type) {
                this.type = type;
                return this;
            }

            /** @see Data#content */
            public Builder content(String content) {
                this.content = content;
                return this;
            }

            /** @see Data#createdAt */
            public Builder createdAt(long createdAt) {
                this.createdAt = createdAt;
                return this;
            }

            /** @see Data#usedAt */
            public Builder usedAt(long usedAt) {
                this.usedAt = usedAt;
                return this;
            }

            /** @see Data#usedCount */
            public Builder usedCount(long usedCount) {
                this.usedCount = usedCount;
                return this;
            }

            /** @see Data#sensitive */
            public Builder sensitive(boolean sensitive) {
                this.sensitive = sensitive;
                return this;
            }

            // ===================== End: 构建配置 ===================
        }
    }
}
