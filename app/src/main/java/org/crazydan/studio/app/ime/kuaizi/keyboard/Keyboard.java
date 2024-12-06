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

package org.crazydan.studio.app.ime.kuaizi.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import android.content.Context;
import org.crazydan.studio.app.ime.kuaizi.ImeSubtype;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.keyboard.conf.Conf;
import org.crazydan.studio.app.ime.kuaizi.keyboard.conf.Configuration;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.KeyboardMsg;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.KeyboardMsgData;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.KeyboardMsgListener;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserKeyMsgListener;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.input.InputCommonMsgData;
import org.crazydan.studio.app.ime.kuaizi.keyboard.sub.LatinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.keyboard.sub.MathKeyboard;
import org.crazydan.studio.app.ime.kuaizi.keyboard.sub.NumberKeyboard;
import org.crazydan.studio.app.ime.kuaizi.keyboard.sub.PinyinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.keyboard.sub.SubKeyboard;

import static org.crazydan.studio.app.ime.kuaizi.keyboard.msg.KeyboardMsg.Keyboard_Hide_Done;
import static org.crazydan.studio.app.ime.kuaizi.keyboard.msg.KeyboardMsg.Keyboard_Start_Done;

/**
 * 键盘
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-03
 */
public class Keyboard implements KeyboardMsgListener, UserKeyMsgListener {

    /** 子键盘类型 */
    public enum Subtype {
        /** 汉语拼音键盘 */
        Pinyin,
        /** 算术键盘：支持数学计算 */
        Math,
        /** 拉丁文键盘：含字母、数字和英文标点（在内部切换按键），逐字直接录入目标输入组件 */
        Latin,
        /** 数字键盘：纯数字和 +、-、#、* 等符号 */
        Number,

        // 临时控制键盘切换
        /** 由 {@link org.crazydan.studio.app.ime.kuaizi.ImeSubtype ImeSubtype} 确定类型 */
        By_ImeSubtype,
        /** 保持当前键盘类型 */
        Keep_Current,
    }

    /** 键盘布局方向 */
    public enum Orientation {
        /** 纵向 */
        portrait,
        /** 横向 */
        landscape,
    }

    /** 左右手模式 */
    public enum HandMode {
        /** 左手模式 */
        left,
        /** 右手模式 */
        right,
    }

    /** 键盘主题样式 */
    public enum Theme {
        light(R.string.value_theme_light),
        night(R.string.value_theme_night),
        follow_system(R.string.value_theme_follow_system),
        ;

        private final int labelResId;

        Theme(int labelResId) {
            this.labelResId = labelResId;
        }

        public int getLabelResId() {
            return this.labelResId;
        }
    }

    private PinyinDict dict;
    private Configuration conf;

    private SubKeyboard sub;
    private InputList inputList;

    private List<KeyboardMsgListener> listeners;

    Keyboard(PinyinDict dict) {
        this.dict = dict;

        this.conf = new Configuration();
        this.listeners = new ArrayList<>();
    }

    // <<<<<<<<<<<<<<<<<<<<<< 生命周期

    /** 创建键盘 */
    public static Keyboard create(Context context) {
        // 确保拼音字典库保持就绪状态
        PinyinDict dict = PinyinDict.instance();
        dict.init(context);
        dict.open(context);

        return new Keyboard(dict);
    }

    /**
     * 启动键盘
     *
     * @param subtype
     *         待启用的子键盘类型
     */
    public void start(Subtype subtype, boolean resetInputList) {
        // 先切换子键盘
        enableSubKeyboard(subtype);
        // 再重置输入列表
        if (resetInputList) {
            this.inputList.reset(false);
        }

        onMsg(this, Keyboard_Start_Done, new InputCommonMsgData());
    }

    /** 隐藏键盘 */
    public void hide() {
        onMsg(this, Keyboard_Hide_Done, new InputCommonMsgData());
    }

    /** 退出键盘 */
    public void exit() {
        // 清空输入列表
        this.inputList.clear();
        // 重置子键盘
        if (this.sub != null) {
            this.sub.reset();
        }

        // TODO 发送键盘退出消息
    }

    /** 销毁键盘 */
    public void destroy() {
        this.dict = null;
        this.conf = null;
        this.sub = null;
        this.inputList = null;
        this.listeners = null;

        // 确保拼音字典库能够被及时关闭
        PinyinDict.instance().close();
    }
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    // <<<<<<<<<<<<<<<<<<< 内部状态

    /** 获取子键盘类型 */
    public Subtype getSubtype() {
        return this.sub != null ? this.sub.getType() : null;
    }

    /**
     * 更新配置
     * <p/>
     * 通过更新函数以支持批量更新，并便于一次性触发配置更新消息
     */
    public void updateConfig(Consumer<Configuration> updater) {
        updater.accept(this.conf);
    }
    // >>>>>>>>>>>>>>>>>>>>>>>

    // <<<<<<<<<<<<<<<<< 消息处理

    public void addListener(KeyboardMsgListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(KeyboardMsgListener listener) {
        this.listeners.remove(listener);
    }

    /** 响应子键盘的 {@link KeyboardMsg} 消息：从子键盘向上传递给外部监听者 */
    @Override
    public void onMsg(Keyboard keyboard, KeyboardMsg msg, KeyboardMsgData msgData) {
        this.listeners.forEach(listener -> listener.onMsg(this, msg, msgData));
    }

    /** 响应视图的 {@link UserKeyMsg} 消息：向下传递消息给子键盘 */
    @Override
    public void onMsg(UserKeyMsg msg, UserKeyMsgData data) {
        this.sub.onMsg(msg, data);
    }
    // >>>>>>>>>>>>>>>>>>>>

    /** 启用指定类型的子键盘 */
    private void enableSubKeyboard(Subtype subtype) {
        SubKeyboard oldSub = this.sub;
        Subtype oldSubtype = getSubtype();

        // 保持子键盘不变，则仅需重置子键盘即可
        if (oldSubtype != null //
            && (oldSubtype == subtype //
                || subtype == Subtype.Keep_Current)) {
            oldSub.reset();
            return;
        }

        switch (subtype) {
            // 首次切换到本输入法时的情况
            case Keep_Current:
                // 切换本输入法到不同的系统子键盘时的情况
            case By_ImeSubtype: {
                // Note: ImeService 将在每次 start 本键盘时更新该项配置
                ImeSubtype imeSubtype = this.conf.get(Conf.ime_subtype);

                if (imeSubtype == ImeSubtype.latin) {
                    subtype = Keyboard.Subtype.Latin;
                } else {
                    subtype = Keyboard.Subtype.Pinyin;
                }
                break;
            }
        }

        if (oldSub != null) {
            oldSub.destroy();
        }

        SubKeyboard newKeyboard = createKeyboard(subtype, oldSubtype);
//        newKeyboard.setInputList(this::getInputList);
//        newKeyboard.setConfig(this::getConfig);

        this.sub = newKeyboard;
        newKeyboard.start();
    }

    private SubKeyboard createKeyboard(Subtype subtype, Subtype oldSubtype) {
        switch (subtype) {
            case Math:
                return new MathKeyboard(this, oldSubtype);
            case Latin:
                return new LatinKeyboard(this, oldSubtype);
            case Number:
                return new NumberKeyboard(this, oldSubtype);
            default:
                return new PinyinKeyboard(this, oldSubtype);
        }
    }
}
