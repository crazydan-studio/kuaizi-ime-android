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

import org.crazydan.studio.app.ime.kuaizi.R;

/** 输入文本的类型 */
public enum InputTextType {
    /** 文本 */
    text(R.string.value_text_type_normal),
    /** 链接文本 */
    url(R.string.value_text_type_url),
    /** 验证码 */
    captcha(R.string.value_text_type_captcha),
    /** 手机/电话号码 */
    phone(R.string.value_text_type_phone),
    /** 邮箱 */
    email(R.string.value_text_type_email),
    /** 身份证号 */
    id_card(R.string.value_text_type_id_card),
    /** 银行卡号 */
    credit_card(R.string.value_text_type_credit_card),
    /** 地址 */
    address(R.string.value_text_type_address),
    ;

    public final int labelResId;

    InputTextType(int labelResId) {
        this.labelResId = labelResId;
    }
}
