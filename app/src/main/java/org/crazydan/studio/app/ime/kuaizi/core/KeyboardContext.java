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

import org.crazydan.studio.app.ime.kuaizi.IMESubtype;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;

/**
 * {@link Keyboard} 的上下文，以参数形式向键盘传递上下文信息
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-23
 */
public class KeyboardContext extends BaseInputContext {
    private final static Builder builder = new Builder();

    /** 与当前上下文直接关联的 {@link Key}，一般为触发 {@link UserKeyMsg} 消息所对应的按键，可能为 null */
    public final Key key;

    // <<<<<<<<<<<<<<<<<<<<<<<<< 配置信息
    /** 原键盘类型 */
    public final Keyboard.Type keyboardPrevType;
    /** 左右手使用模式 */
    public final Keyboard.HandMode keyboardHandMode;
    /** 是否采用单行输入模式 */
    public final boolean useSingleLineInputMode;

    /** 是否已启用 X 输入面板 */
    public final boolean xInputPadEnabled;
    /** 是否已启用在 X 输入面板中让拉丁文输入共用拼音输入的按键布局 */
    public final boolean latinUsePinyinKeysInXInputPadEnabled;
    /** 是否已禁用对用户输入数据的保存 */
    public final boolean userInputDataDisabled;

    /** 是否有可撤回的输入提交 */
    public final boolean hasRevokableInputsCommit;
    /** 是否有可取消的输入清空 */
    public final boolean hasCancellableInputsClean;
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    /** 构建 {@link KeyboardContext} */
    public static KeyboardContext build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    KeyboardContext(Builder builder) {
        super(builder);

        this.key = builder.key;

        this.keyboardPrevType = builder.keyboardPrevType;
        this.keyboardHandMode = builder.keyboardHandMode;
        this.useSingleLineInputMode = builder.useSingleLineInputMode;

        this.xInputPadEnabled = builder.xInputPadEnabled;
        this.latinUsePinyinKeysInXInputPadEnabled = builder.latinUsePinyinKeysInXInputPadEnabled;
        this.userInputDataDisabled = builder.userInputDataDisabled;

        this.hasRevokableInputsCommit = builder.hasRevokableInputsCommit;
        this.hasCancellableInputsClean = builder.hasCancellableInputsClean;
    }

    /** 创建副本 */
    public KeyboardContext copy(Consumer<Builder> c) {
        return Builder.copy(builder, this, c);
    }

    /** {@link #key} 的泛型转换接口，避免编写显式的类型转换代码 */
    public <T extends Key> T key() {
        return (T) this.key;
    }

    /** {@link KeyboardContext} 的构建器 */
    public static class Builder extends BaseInputContext.Builder<Builder, KeyboardContext> {
        private Key key;

        private Keyboard.Type keyboardPrevType;
        private Keyboard.HandMode keyboardHandMode;
        private boolean useSingleLineInputMode;

        private boolean xInputPadEnabled;
        private boolean latinUsePinyinKeysInXInputPadEnabled;
        private boolean userInputDataDisabled;

        private boolean hasRevokableInputsCommit;
        private boolean hasCancellableInputsClean;

        protected Builder() {
            super(2);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected KeyboardContext doBuild() {
            return new KeyboardContext(this);
        }

        @Override
        protected void doCopy(KeyboardContext source) {
            super.doCopy(source);

            key(source.key);

            this.keyboardPrevType = source.keyboardPrevType;
            this.keyboardHandMode = source.keyboardHandMode;
            this.useSingleLineInputMode = source.useSingleLineInputMode;

            this.xInputPadEnabled = source.xInputPadEnabled;
            this.latinUsePinyinKeysInXInputPadEnabled = source.latinUsePinyinKeysInXInputPadEnabled;
            this.userInputDataDisabled = source.userInputDataDisabled;

            this.hasRevokableInputsCommit = source.hasRevokableInputsCommit;
            this.hasCancellableInputsClean = source.hasCancellableInputsClean;
        }

        @Override
        protected void reset() {
            super.reset();

            this.key = null;

            this.keyboardPrevType = null;
            this.keyboardHandMode = null;
            this.useSingleLineInputMode = false;

            this.xInputPadEnabled = false;
            this.latinUsePinyinKeysInXInputPadEnabled = false;
            this.userInputDataDisabled = false;

            this.hasRevokableInputsCommit = false;
            this.hasCancellableInputsClean = false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(),
                                this.key,
                                this.keyboardPrevType,
                                this.keyboardHandMode,
                                this.useSingleLineInputMode,
                                this.xInputPadEnabled,
                                this.latinUsePinyinKeysInXInputPadEnabled,
                                this.userInputDataDisabled,
                                this.hasRevokableInputsCommit,
                                this.hasCancellableInputsClean);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** 设置配置信息 */
        public Builder config(Config config, Inputboard inputboard) {
            this.keyboardPrevType = config.get(ConfigKey.prev_keyboard_type);

            this.keyboardHandMode = config.get(ConfigKey.hand_mode);
            this.useSingleLineInputMode = config.bool(ConfigKey.single_line_input);

            this.xInputPadEnabled = config.bool(ConfigKey.enable_x_input_pad);
            this.latinUsePinyinKeysInXInputPadEnabled
                    = config.bool(ConfigKey.enable_latin_use_pinyin_keys_in_x_input_pad)
                      // Note: 仅汉字输入环境才支持将拉丁文键盘与拼音键盘的按键布局设置为相同的
                      && config.get(ConfigKey.ime_subtype) == IMESubtype.hans;
            this.userInputDataDisabled = config.bool(ConfigKey.disable_user_input_data);

            this.hasRevokableInputsCommit = inputboard.canRestoreCommitted();
            this.hasCancellableInputsClean = inputboard.canRestoreCleaned();

            return this;
        }

        /** @see KeyboardContext#key */
        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
