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

package org.crazydan.studio.app.ime.kuaizi.core.input;

import java.util.Objects;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.common.Immutable;

/**
 * 剪贴板数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class InputClip extends Immutable {
    private final static Builder builder = new Builder();

    /** 类型（文本、HTML、URL。。。） */
    public final Type type;
    /** 唯一标识，用于判断数据是否已使用 */
    public final int code;

    /** 文本内容 */
    public final String text;
    /** HTML 内容，用于支持富文本，其与 {@link #text} 必须成对出现 */
    public final String html;

    /** 构建 {@link InputClip} */
    public static InputClip build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    /** 创建副本 */
    public InputClip copy(Consumer<Builder> c) {
        return Builder.copy(builder, this, c);
    }

    protected InputClip(Builder builder) {
        super(builder);

        this.type = builder.type;
        this.code = builder.code;

        this.text = builder.text;
        this.html = builder.html;
    }

    /** {@link InputClip} 的类型 */
    public enum Type {
        /** 文本 */
        text,
        /** 链接文本 */
        url,
        /** 验证码 */
        captcha,
        /** 手机/电话号码 */
        phone,
        /** 邮箱 */
        email,
    }

    /** {@link InputClip} 的构建器 */
    public static class Builder extends Immutable.Builder<InputClip> {
        private Type type;
        private int code;

        private String text;
        private String html;

        // ===================== Start: 构建函数 ===================

        @Override
        protected InputClip build() {
            return new InputClip(this);
        }

        @Override
        protected void doCopy(InputClip source) {
            super.doCopy(source);

            this.type = source.type;
            this.code = source.code;

            this.text = source.text;
            this.html = source.html;
        }

        @Override
        protected void reset() {
            this.type = null;
            this.code = 0;

            this.text = null;
            this.html = null;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.type, this.code, this.text, this.html);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see InputClip#type */
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /** @see InputClip#code */
        public Builder code(int code) {
            this.code = code;
            return this;
        }

        /** @see InputClip#text */
        public Builder text(String text) {
            this.text = text;
            return this;
        }

        /** @see InputClip#html */
        public Builder html(String html) {
            this.html = html;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
