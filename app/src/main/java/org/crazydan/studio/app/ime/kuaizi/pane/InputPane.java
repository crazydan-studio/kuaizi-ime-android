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

import java.util.Stack;

import android.content.Context;
import org.crazydan.studio.app.ime.kuaizi.ImeSubtype;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigChangeListener;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.EditorKeyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.EmojiKeyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.LatinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.NumberKeyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.PinyinCandidatesKeyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.PinyinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.SymbolKeyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.KeyboardHandModeSwitchMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.KeyboardSwitchMsgData;

import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputList_Config_Update_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Input_Completion_Clean_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_Config_Update_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_Exit_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_HandMode_Switch_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_Hide_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_Start_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_Switch_Done;

/**
 * 输入面板，由{@link Keyboard 键盘}和{@link InputList 输入列表}组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-03
 */
public class InputPane implements InputMsgListener, UserMsgListener, ConfigChangeListener {
    private PinyinDict dict;
    private Config.Mutable config;

    private Keyboard keyboard;
    private InputList inputList;
    private Stack<Keyboard.Type> keyboardSwitches;

    private InputMsgListener listener;

    InputPane(PinyinDict dict) {
        this.dict = dict;

        this.inputList = new InputList();
        this.inputList.setListener(this);

        this.keyboardSwitches = new Stack<>();
    }

    // =============================== Start: 生命周期 ===================================

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
     *         待使用的键盘类型
     */
    public void start(Keyboard.Type keyboardType, boolean resetInputting) {
        // 全新的切换，故而需清空键盘切换记录
        this.keyboardSwitches.clear();

        // 先切换键盘
        switchKeyboardTo(keyboardType);

        // 再重置输入列表
        this.inputList.updateConfig(createInputListConfig());
        if (resetInputting) {
            this.inputList.reset(false);
        }

        fire_InputMsg(Keyboard_Start_Done);
    }

    /** 隐藏 {@link InputPane}，仅隐藏面板，但输入状态保持不变 */
    public void hide() {
        this.inputList.clearCompletions();
        fire_InputMsg(Input_Completion_Clean_Done);

        fire_InputMsg(Keyboard_Hide_Done);
    }

    /** 退出 {@link InputPane}，即，重置输入状态 */
    public void exit() {
        // 清空输入列表
        this.inputList.clear();
        // 重置键盘
        if (this.keyboard != null) {
            this.keyboard.reset();
        }

        fire_InputMsg(Keyboard_Exit_Done);
    }

    /** 销毁 {@link InputPane}，即，关闭并回收资源 */
    public void destroy() {
        this.dict = null;
        this.config = null;
        this.keyboard = null;
        this.inputList = null;
        this.keyboardSwitches = null;
        this.listener = null;

        // 确保拼音字典库能够被及时关闭
        PinyinDict.instance().close();
    }

    // =============================== End: 生命周期 ===================================

    // =============================== Start: 内部状态 ===================================

    public void setConfig(Config.Mutable config) {
        this.config = config;
    }

    /** 获取键盘类型 */
    public Keyboard.Type getKeyboardType() {
        return this.keyboard != null ? this.keyboard.getType() : null;
    }

    /** 获取输入列表 */
    public InputList getInputList() {
        return this.inputList;
    }

    // =============================== End: 内部状态 ===================================

    // =============================== Start: 消息处理 ===================================

    public void setListener(InputMsgListener listener) {
        this.listener = listener;
    }

    // --------------------------------------

    /** 响应 {@link Config} 变更消息 */
    @Override
    public void onChanged(ConfigKey key, Object oldValue, Object newValue) {
        if (this.inputList.updateConfig(createInputListConfig())) {
            fire_InputMsg(InputList_Config_Update_Done);
        }

        if (this.keyboard.updateConfig(createKeyboardConfig())) {
            fire_InputMsg(Keyboard_Config_Update_Done);
        }
    }

    // --------------------------------------

    /** 响应视图的 {@link UserKeyMsg} 消息：向下传递消息给 {@link Keyboard} */
    @Override
    public void onMsg(UserKeyMsg msg) {
        this.keyboard.onMsg(this.inputList, msg);
    }

    /** 响应视图的 {@link UserInputMsg} 消息：向下传递消息给 {@link InputList} */
    @Override
    public void onMsg(UserInputMsg msg) {
        this.inputList.onMsg(msg);
    }

    // --------------------------------------

    /** 响应键盘的 {@link InputMsg} 消息：从键盘向上传递给外部监听者 */
    @Override
    public void onMsg(InputMsg msg) {
        InputList inputList = this.inputList;
        Keyboard keyboard = this.keyboard;

        switch (msg.type) {
            case Keyboard_Switch_Doing: {
                on_Keyboard_Switch_Doing_Msg((KeyboardSwitchMsgData) msg.data);
                // Note: 在键盘切换过程中，不向上转发消息
                return;
            }
            case Keyboard_HandMode_Switch_Doing: {
                on_Keyboard_HandMode_Switch_Doing_Msg((KeyboardHandModeSwitchMsgData) msg.data);
                return;
            }
            // 向键盘派发 InputList 的消息
            case Input_Choose_Doing:
            case InputList_Clean_Done:
            case InputList_Cleaned_Cancel_Done: {
                keyboard.onMsg(inputList, msg);
                break;
            }
            default: {
                // TODO 若有新输入，则清空 删除撤销数据
                if (!inputList.isEmpty()) {
                    inputList.clearDeleteCancels();
                }
            }
        }

        fire_InputMsg(msg.type, msg.data);
    }

    /** 发送 {@link InputMsg} 消息：附带空的消息数据 */
    private void fire_InputMsg(InputMsgType type) {
        fire_InputMsg(type, new InputMsgData());
    }

    /** 发送 {@link InputMsg} 消息 */
    private void fire_InputMsg(InputMsgType type, InputMsgData data) {
        InputList inputList = this.inputList;
        Keyboard keyboard = this.keyboard;

        // TODO 附件输入状态数据，如，InputList 是否为空、是否可撤销删除、CompletionInputFactory 等
        InputMsg newMsg = new InputMsg(type, data, keyboard.getKeyFactory(inputList), inputList.getInputFactory());
        this.listener.onMsg(newMsg);
    }

    /** 处理 {@link InputMsgType#Keyboard_Switch_Doing} 消息 */
    private void on_Keyboard_Switch_Doing_Msg(KeyboardSwitchMsgData data) {
        Keyboard.Type type = data.type;
        if (type == null && !this.keyboardSwitches.isEmpty()) {
            type = this.keyboardSwitches.pop();
        }

        Keyboard.Type oldType = switchKeyboardTo(type);
        if (oldType != null) {
            this.keyboardSwitches.push(oldType);
        }

        fire_InputMsg(Keyboard_Switch_Done, data);
    }

    /** 处理 {@link InputMsgType#Keyboard_HandMode_Switch_Doing} 消息 */
    private void on_Keyboard_HandMode_Switch_Doing_Msg(KeyboardHandModeSwitchMsgData data) {
        Keyboard.HandMode mode = data.mode;
        this.config.set(ConfigKey.hand_mode, mode);

        fire_InputMsg(Keyboard_HandMode_Switch_Done, data);
    }

    // =============================== End: 消息处理 ===================================

    /**
     * 切换到指定类型的键盘
     *
     * @return 切换前的键盘类型，若为 null，则表示为首次创建键盘，或者，未发生键盘切换
     */
    private Keyboard.Type switchKeyboardTo(Keyboard.Type type) {
        Keyboard old = this.keyboard;
        Keyboard.Type oldType = getKeyboardType();

        // 保持键盘不变，仅需重置键盘即可
        if (oldType != null //
            && (oldType == type //
                || type == Keyboard.Type.Keep_Current //
            ) //
        ) {
            KeyboardConfig config = createKeyboardConfig();
            old.updateConfig(config);

            old.reset();
            return null;
        }

        switch (type) {
            // 首次切换到本输入法时的情况
            case Keep_Current:
                // 切换本输入法到不同的系统键盘时的情况
            case By_ImeSubtype: {
                // Note: ImeService 将在每次 start 本键盘时更新该项配置
                ImeSubtype imeSubtype = this.config.get(ConfigKey.ime_subtype);

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
        this.config.set(ConfigKey.prev_keyboard_type, oldType);

        KeyboardConfig config = createKeyboardConfig();
        this.keyboard = createKeyboard(type);
        this.keyboard.updateConfig(config);
        this.keyboard.setListener(this);

        this.keyboard.start(this.inputList);

        return oldType;
    }

    private Keyboard createKeyboard(Keyboard.Type type) {
        switch (type) {
//            case Math:
//                return new MathKeyboard();
            case Number:
                return new NumberKeyboard();
            case Symbol:
                return new SymbolKeyboard();
            case Editor:
                return new EditorKeyboard();
            case Latin:
                return new LatinKeyboard(this.dict);
            case Emoji:
                return new EmojiKeyboard(this.dict);
            case Pinyin_Candidates:
                return new PinyinCandidatesKeyboard(this.dict);
            default:
                return new PinyinKeyboard(this.dict);
        }
    }

    private InputListConfig createInputListConfig() {
        return InputListConfig.from(this.config);
    }

    private KeyboardConfig createKeyboardConfig() {
        return KeyboardConfig.from(this.config);
    }
}
