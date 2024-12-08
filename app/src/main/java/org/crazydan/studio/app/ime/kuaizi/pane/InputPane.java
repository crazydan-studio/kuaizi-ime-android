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

package org.crazydan.studio.app.ime.kuaizi.pane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import android.content.Context;
import org.crazydan.studio.app.ime.kuaizi.ImeSubtype;
import org.crazydan.studio.app.ime.kuaizi.conf.Conf;
import org.crazydan.studio.app.ime.kuaizi.conf.Configuration;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.LatinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.MathKeyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.NumberKeyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.PinyinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputListMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputListMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputCommonMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.KeyboardSwitchingMsgData;

import static org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsg.Keyboard_Exit_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsg.Keyboard_Hide_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsg.Keyboard_Start_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsg.Keyboard_Switch_Done;

/**
 * 输入面板，由{@link Keyboard 键盘}和{@link InputList 输入列表}组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-03
 */
public class InputPane implements InputMsgListener, UserMsgListener {
    private PinyinDict dict;
    private Configuration conf;

    private Keyboard keyboard;
    private InputList inputList;

    private List<InputMsgListener> listeners;

    InputPane(PinyinDict dict) {
        this.dict = dict;

        this.conf = new Configuration();
        this.inputList = new InputList();
        this.listeners = new ArrayList<>();
    }

    // <<<<<<<<<<<<<<<<<<<<<< 生命周期

    /** 创建 {@link InputPane} */
    public static InputPane create(Context context) {
        // 确保拼音字典库保持就绪状态
        PinyinDict dict = PinyinDict.instance();
        dict.init(context);
        dict.open(context);

        return new InputPane(dict);
    }

    /**
     * 启动 {@link InputPane}
     *
     * @param keyboardType
     *         待启用的键盘类型
     */
    public void start(Keyboard.Type keyboardType, boolean resetInputting) {
        // 先切换键盘
        enableKeyboard(keyboardType);
        // 再重置输入列表
        if (resetInputting) {
            this.inputList.reset(false);
        }

        onMsg(this.keyboard, Keyboard_Start_Done, new InputCommonMsgData());
    }

    /** 隐藏 {@link InputPane}，仅隐藏面板，但输入状态保持不变 */
    public void hide() {
        onMsg(this.keyboard, Keyboard_Hide_Done, new InputCommonMsgData());
    }

    /** 退出 {@link InputPane}，即，重置输入状态 */
    public void exit() {
        // 清空输入列表
        this.inputList.clear();
        // 重置键盘
        if (this.keyboard != null) {
            this.keyboard.reset();
        }

        onMsg(this.keyboard, Keyboard_Exit_Done, new InputCommonMsgData());
    }

    /** 销毁 {@link InputPane}，即，关闭并回收资源 */
    public void destroy() {
        this.dict = null;
        this.conf = null;
        this.keyboard = null;
        this.inputList = null;
        this.listeners = null;

        // 确保拼音字典库能够被及时关闭
        PinyinDict.instance().close();
    }
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    // <<<<<<<<<<<<<<<<<<< 内部状态

    /** 获取键盘类型 */
    public Keyboard.Type getKeyboardType() {
        return this.keyboard != null ? this.keyboard.getType() : null;
    }

    /** 获取输入列表 */
    public InputList getInputList() {
        return this.inputList;
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

    public void addListener(InputMsgListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(InputMsgListener listener) {
        this.listeners.remove(listener);
    }

    /** 响应视图的 {@link UserKeyMsg} 消息：向下传递消息给 {@link Keyboard} */
    @Override
    public void onMsg(UserKeyMsg msg, UserKeyMsgData data) {
        this.keyboard.onMsg(msg, data);
    }

    /** 响应视图的 {@link UserInputMsg} 消息：向下传递消息给 {@link InputList} */
    @Override
    public void onMsg(UserInputMsg msg, UserInputMsgData data) {
        this.inputList.onMsg(msg, data);
    }

    /** 响应键盘的 {@link KeyboardMsg} 消息：从键盘向上传递给外部监听者 */
    @Override
    public void onMsg(Keyboard keyboard, KeyboardMsg msg, KeyboardMsgData msgData) {
        switch (msg) {
            case Keyboard_Switch_Doing: {
                Keyboard.Type target = ((KeyboardSwitchingMsgData) msgData).target;
                enableKeyboard(target);

                onMsg(this.keyboard, Keyboard_Switch_Done, msgData);
                return;
            }
        }

        this.listeners.forEach(listener -> listener.onMsg(keyboard, msg, msgData));
    }

    /** 响应输入列表的 {@link InputListMsg} 消息：从输入列表向上传递给外部监听者 */
    @Override
    public void onMsg(InputList inputList, InputListMsg msg, InputListMsgData msgData) {
        this.listeners.forEach(listener -> listener.onMsg(inputList, msg, msgData));
    }
    // >>>>>>>>>>>>>>>>>>>>

    /** 启用指定类型的键盘 */
    private void enableKeyboard(Keyboard.Type type) {
        Keyboard old = this.keyboard;
        Keyboard.Type oldType = getKeyboardType();

        // 保持键盘不变，则仅需重置键盘即可
        if (oldType != null //
            && (oldType == type //
                || type == Keyboard.Type.Keep_Current)) {
            old.reset();
            return;
        }

        switch (type) {
            // 首次切换到本输入法时的情况
            case Keep_Current:
                // 切换本输入法到不同的系统键盘时的情况
            case By_ImeSubtype: {
                // Note: ImeService 将在每次 start 本键盘时更新该项配置
                ImeSubtype imeSubtype = this.conf.get(Conf.ime_subtype);

                if (imeSubtype == ImeSubtype.latin) {
                    type = Keyboard.Type.Latin;
                } else {
                    type = Keyboard.Type.Pinyin;
                }
                break;
            }
        }

        if (old != null) {
            old.destroy();
        }

        Keyboard newKeyboard = createKeyboard(type, oldType);
//        newKeyboard.setInputList(this::getInputList);
//        newKeyboard.setConfig(this::getConfig);

        this.keyboard = newKeyboard;
        newKeyboard.start();
    }

    private Keyboard createKeyboard(Keyboard.Type type, Keyboard.Type oldType) {
        switch (type) {
            case Math:
                return new MathKeyboard(this, oldType);
            case Latin:
                return new LatinKeyboard(this, oldType);
            case Number:
                return new NumberKeyboard(this, oldType);
            default:
                return new PinyinKeyboard(this, oldType);
        }
    }
}
