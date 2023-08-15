/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2023 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.internal;

/**
 * {@link Keyboard 键盘}按键
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public interface Key<K extends Key<?>> {

    /** 是否为英文或数字 */
    boolean isLatin();

    /** 是否为数字 */
    boolean isNumber();

    /** 是否为标点符号 */
    boolean isSymbol();

    /** 是否为表情符号 */
    boolean isEmotion();

    /** 是否与指定的按键相同：即实例不同但按键类型和符号相同 */
    boolean isSameWith(Key<?> key);

    /** 是否已禁用 */
    boolean isDisabled();

    /** 设置为禁用 */
    K setDisabled(boolean disabled);

    /**
     * 获取按键对应的文本字符
     * <p/>
     * 若其不对应任何字符，则返回 <code>null</code>
     */
    String getText();

    /** 按键上显示的文字内容 */
    String getLabel();

    /** 设置按键上显示的文字内容 */
    K setLabel(String label);

    /** 按键上显示的图标资源 id */
    int getIconResId();

    /** 设置按键上显示的图标资源 id */
    K setIconResId(int iconResId);

    /** 获取前景色属性 id */
    int getFgColorAttrId();

    /** 设置前景色属性 id */
    K setFgColorAttrId(int fgColorAttrId);

    /** 获取背景色属性 id */
    int getBgColorAttrId();

    /** 设置背景色属性 id */
    K setBgColorAttrId(int bgColorAttrId);

    /** 按键级别 */
    enum Level {
        /** 第 0 级：初始布局的按键 */
        level_0,
        /** 第 1 级：拼音滑屏的第一级后继字母按键 */
        level_1,
        /** 第 2 级：拼音滑屏的第二级后继字母按键 */
        level_2,
    }
}
