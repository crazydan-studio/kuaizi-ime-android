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

package org.crazydan.studio.app.ime.kuaizi.core.msg;

import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.core.Input;

/**
 * 用户操作 {@link Input} 所触发的消息
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-11
 */
public class UserInputMsg extends BaseMsg<UserInputMsgType, UserInputMsgData> {
    private final static Builder builder = new Builder();

    /** 构建 {@link UserInputMsg} */
    public static UserInputMsg build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    UserInputMsg(Builder builder) {
        super(builder);
    }

    /** {@link UserInputMsg} 的构建器 */
    public static class Builder extends BaseMsg.Builder<Builder, UserInputMsg, UserInputMsgType, UserInputMsgData> {

        // ===================== Start: 构建函数 ===================

        @Override
        protected UserInputMsg build() {
            return new UserInputMsg(this);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        // ===================== End: 构建配置 ===================
    }
}
