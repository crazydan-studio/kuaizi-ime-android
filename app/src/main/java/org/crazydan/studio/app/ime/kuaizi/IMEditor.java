/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import android.content.Context;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigChangeListener;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.InputFactory;
import org.crazydan.studio.app.ime.kuaizi.core.Inputboard;
import org.crazydan.studio.app.ime.kuaizi.core.InputboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.EditorKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.EmojiKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.LatinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.MathKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.NumberKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.PinyinCandidateKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.PinyinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.SymbolKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.ConfigUpdateMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputListCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.KeyboardHandModeSwitchMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.KeyboardSwitchMsgData;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Config_Update_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Input_Completion_Clean_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_Exit_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_HandMode_Switch_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_Hide_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_Start_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_Switch_Done;

/**
 * Input Method Editor (IME)
 * <p/>
 * 负责实现输入法的核心逻辑，由{@link Keyboard 键盘}和{@link Inputboard 输入面板}组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-03
 */
public class IMEditor implements InputMsgListener, UserMsgListener, ConfigChangeListener, PinyinDict.Listener {
    protected final Logger log = Logger.getLogger(getClass());

    /** 是否正在开启 {@link PinyinDict} */
    private boolean dictOpening;

    private Config.Mutable config;
    private PinyinDict dict;

    private InputList inputList;

    private Keyboard keyboard;
    private Inputboard inputboard;
    /** 切换前的主键盘类型 */
    private Keyboard.Type prevMasterKeyboardType;

    private InputMsgListener listener;

    IMEditor(Config.Mutable config, PinyinDict dict) {
        this.config = config;
        this.dict = dict;

        this.inputList = new InputList();

        this.inputboard = new Inputboard();
    }

    /** 获取键盘类型 */
    public Keyboard.Type getKeyboardType() {
        return this.keyboard != null ? this.keyboard.getType() : null;
    }

    // =============================== Start: 生命周期 ===================================

    /** 创建 {@link IMEditor} */
    public static IMEditor create(Config.Mutable config) {
        PinyinDict dict = PinyinDict.instance();

        return new IMEditor(config, dict);
    }

    /**
     * 启动 {@link IMEditor}
     *
     * @param keyboardType
     *         待使用的键盘类型
     */
    public void start(Context context, Keyboard.Type keyboardType, boolean resetInputting) {
        if (!this.config.bool(ConfigKey.disable_dict_db)) {
            this.dictOpening = true;
            // Note: 字典库是异步开启的，不会阻塞键盘视图的渲染
            this.dict.open(context, this);
        }

        // 先切换键盘
        switchKeyboardTo(keyboardType);

        // 再重置输入列表
        if (resetInputting) {
            withInputboardContext(this.inputboard::reset);
        }

        fire_InputMsg(Keyboard_Start_Done);
    }

    /** 隐藏 {@link IMEditor}，仅隐藏面板，但输入状态保持不变 */
    public void hide() {
        this.inputList.clearCompletions();
        fire_InputMsg(Input_Completion_Clean_Done);

        fire_InputMsg(Keyboard_Hide_Done);
    }

    /** 退出 {@link IMEditor}，即，重置输入状态 */
    public void exit() {
        // 重置输入面板
        withInputboardContext(this.inputboard::reset);

        // 重置键盘
        if (this.keyboard != null) {
            withKeyboardContext(this.keyboard::reset);
        }

        fire_InputMsg(Keyboard_Exit_Done);
    }

    /** 销毁 {@link IMEditor}，即，关闭并回收资源 */
    public void destroy() {
        // 确保拼音字典库能够被开启方及时关闭
        if (!this.config.bool(ConfigKey.disable_dict_db) //
            && this.keyboard != null // 已调用 #start 方法
        ) {
            this.dict.close();
        }

        this.config = null;
        this.dict = null;

        this.inputList = null;

        this.keyboard = null;
        this.inputboard = null;
        this.prevMasterKeyboardType = null;

        this.listener = null;
    }

    // =============================== End: 生命周期 ===================================

    // =============================== Start: 消息处理 ===================================

    public void setListener(InputMsgListener listener) {
        this.listener = listener;
    }

    // --------------------------------------

    /** {@link PinyinDict} 开启前 */
    @Override
    public void beforeOpen(PinyinDict dict) {
        // Note: 字典库异步开启，不会阻塞视图渲染，故而，无需显示提示信息
        //fire_InputMsg(Keyboard_Start_Doing);
    }

    /** {@link PinyinDict} 开启后 */
    @Override
    public void afterOpen(PinyinDict dict) {
        this.dictOpening = false;
    }

    // --------------------------------------

    /** 响应 {@link Config} 变更消息 */
    @Override
    public void onChanged(ConfigKey key, Object oldValue, Object newValue) {
        ConfigUpdateMsgData data = new ConfigUpdateMsgData(key, oldValue, newValue);
        fire_InputMsg(Config_Update_Done, data);
    }

    // --------------------------------------

    /** 响应视图的 {@link UserKeyMsg} 消息：向下传递消息给 {@link Keyboard} */
    @Override
    public void onMsg(UserKeyMsg msg) {
        // 字典还在开启中，不响应用户消息
        if (this.dictOpening) {
            return;
        }

        // TODO 记录用户按键消息所触发的输入消息，并优化合并输入消息：仅需最后一个消息触发按键的布局更新即可
        Key key = msg.data().key;
        KeyboardContext context = createKeyboardContext(key);

        this.keyboard.onMsg(context, msg);
    }

    /** 响应视图的 {@link UserInputMsg} 消息：向下传递消息给 {@link InputList} */
    @Override
    public void onMsg(UserInputMsg msg) {
        // 字典还在开启中，不响应用户消息
        if (this.dictOpening) {
            return;
        }

        withInputboardContext((context) -> this.inputboard.onMsg(context, msg));
    }

    // --------------------------------------

    /** 响应键盘的 {@link InputMsg} 消息：从键盘向上传递给外部监听者 */
    @Override
    public void onMsg(InputMsg msg) {
        // Note: 涉及消息的嵌套处理，可能会发生键盘切换，因此，不能定义 keyboard 的本地变量

        switch (msg.type) {
            case Keyboard_Switch_Doing: {
                on_Keyboard_Switch_Doing_Msg(msg.data());
                // Note: 在键盘切换过程中，不向上转发消息
                return;
            }
            case Keyboard_HandMode_Switch_Doing: {
                on_Keyboard_HandMode_Switch_Doing_Msg(msg.data());
                return;
            }
            // 向键盘派发 InputList 的消息
            case Input_Choose_Doing:
            case InputList_Clean_Done:
            case InputList_Cleaned_Cancel_Done: {
                withKeyboardContext((context) -> {
                    this.keyboard.onMsg(context, msg);
                });
                break;
            }
            case InputList_PairSymbol_Commit_Doing: {
                // 对于单独的配对符号提交，不保存已提交
                withInputboardContext(this.inputboard::reset);
                break;
            }
            case InputList_Commit_Doing: {
                InputListCommitMsgData data = msg.data();
                withInputboardContext((context) -> this.inputboard.storeCommitted(context, data.canBeRevoked));
                break;
            }
            case InputList_Committed_Revoke_Doing: {
                withInputboardContext(this.inputboard::restoreCommitted);
                break;
            }
            case Editor_Edit_Doing: {
                EditorEditMsgData data = msg.data();
                // 对编辑内容会造成修改的操作，需要清除 已提交 的恢复数据
                if (EditorAction.hasEffect(data.action)) {
                    this.inputboard.clearCommitted();
                }
                break;
            }
            default: {
                // 若产生新的输入，则需要清除 已删除/已提交 的恢复数据
                if (!this.inputList.isEmpty()) {
                    this.inputboard.clearCommitted();
                    this.inputboard.clearCleaned();
                }
            }
        }

        fire_InputMsg(msg.type, msg.data());
    }

    /** 发送 {@link InputMsg} 消息：附带空的消息数据 */
    private void fire_InputMsg(InputMsgType type) {
        fire_InputMsg(type, new InputMsgData());
    }

    /** 发送 {@link InputMsg} 消息 */
    private void fire_InputMsg(InputMsgType type, InputMsgData data) {
        KeyFactory keyFactory = createKeyFactory();
        InputFactory inputFactory = createInputFactory();

        InputMsg msg = new InputMsg(type,
                                    data,
                                    keyFactory,
                                    inputFactory,
                                    this.inputList.isEmpty(),
                                    this.inputboard.canRestoreCleaned());
        this.listener.onMsg(msg);
    }

    /** 处理 {@link InputMsgType#Keyboard_Switch_Doing} 消息 */
    private void on_Keyboard_Switch_Doing_Msg(KeyboardSwitchMsgData data) {
        Keyboard.Type newType = data.type != null ? data.type : this.prevMasterKeyboardType;
        assert newType != null;

        boolean prevMaster = this.keyboard != null && this.keyboard.isMaster();
        Keyboard.Type prevType = switchKeyboardTo(newType);
        if (prevMaster) {
            this.prevMasterKeyboardType = prevType;
        }

        data = new KeyboardSwitchMsgData(data.key, newType);
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
    private Keyboard.Type switchKeyboardTo(Keyboard.Type newType) {
        Keyboard current = this.keyboard;
        Keyboard.Type currentType = getKeyboardType();

        // 保持键盘不变，仅需重置键盘即可
        if (currentType != null //
            && (currentType == newType //
                || newType == Keyboard.Type.Keep_Current //
            ) //
        ) {
            withKeyboardContext(current::reset);

            return null;
        }

        switch (newType) {
            // 首次切换到本输入法时的情况
            case Keep_Current:
                // 切换本输入法到不同的系统键盘时的情况
            case By_ImeSubtype: {
                // Note: ImeService 将在每次 start 本键盘时更新该项配置
                IMESubtype imeSubtype = this.config.get(ConfigKey.ime_subtype);

                if (imeSubtype == IMESubtype.latin) {
                    newType = Keyboard.Type.Latin;
                } else {
                    newType = Keyboard.Type.Pinyin;
                }
                break;
            }
        }

        // 确保前序键盘完成退出清理工作
        if (current != null) {
            withKeyboardContext(current::stop);
        }
        this.config.set(ConfigKey.prev_keyboard_type, currentType);

        this.keyboard = createKeyboard(newType);
        withKeyboardContext(this.keyboard::start);

        return currentType;
    }

    private Keyboard createKeyboard(Keyboard.Type type) {
        switch (type) {
            case Number:
                return new NumberKeyboard();
            case Editor:
                return new EditorKeyboard();
            case Math:
                return new MathKeyboard(this.dict);
            case Symbol:
                return new SymbolKeyboard(this.dict);
            case Latin:
                return new LatinKeyboard(this.dict);
            case Emoji:
                return new EmojiKeyboard(this.dict);
            case Pinyin_Candidate:
                return new PinyinCandidateKeyboard(this.dict);
            default:
                return new PinyinKeyboard(this.dict);
        }
    }

    public void withKeyboardContext(Consumer<KeyboardContext> c) {
        withKeyboardContext((context) -> {
            c.accept(context);
            return null;
        });
    }

    public <T> T withKeyboardContext(Function<KeyboardContext, T> c) {
        KeyboardContext context = createKeyboardContext(null);
        return c.apply(context);
    }

    private KeyboardContext createKeyboardContext(Key key) {
        return KeyboardContext.build((b) -> b.config(this.config, this.inputboard)
                                             .key(key)
                                             .inputList(this.inputList)
                                             .listener(this));
    }

    protected void withInputboardContext(Consumer<InputboardContext> c) {
        InputboardContext context = InputboardContext.build((b) -> b.config(this.config)
                                                                    .inputList(this.inputList)
                                                                    .listener(this));
        c.accept(context);
    }

    /** 创建 {@link KeyFactory} 以使其携带{@link KeyFactory.NoAnimation 无动画}和{@link KeyFactory.LeftHandMode 左手模式}信息 */
    private KeyFactory createKeyFactory() {
        KeyFactory factory = this.keyboard != null ? withKeyboardContext(this.keyboard::buildKeyFactory) : null;

        boolean leftHandMode = this.config.get(ConfigKey.hand_mode) == Keyboard.HandMode.left;
        if (!leftHandMode || factory == null) {
            return factory;
        }

        if (factory instanceof KeyFactory.NoAnimation) {
            return (KeyFactory.LeftHandMode_NoAnimation) factory::getKeys;
        } else {
            return (KeyFactory.LeftHandMode) factory::getKeys;
        }
    }

    /** 创建 {@link InputFactory} */
    private InputFactory createInputFactory() {
        InputboardContext context = InputboardContext.build((b) -> b.config(this.config).inputList(this.inputList));
        return this.inputboard.buildInputFactory(context);
    }

    // =============================== Start: 自动化，用于模拟输入等 ===================================

    /** 更改最后一个输入的候选字 */
    public void changeLastInputWord(InputWord word) {
        this.inputList.getLastCharInput().setWord(word);
    }

    /**
     * 向输入列表做预备输入
     *
     * @param tuples
     *         其元素为 <code>["chars", "word value", "word spell"]</code> 三元数组
     */
    public void prepareInputs(List<String[]> tuples) {
        withInputboardContext(this.inputboard::reset);

        for (int i = 0; i < tuples.size(); i++) {
            String[] tuple = tuples.get(i);
            String chars = tuple[0];
            String wordValue = tuple[1];
            String wordSpell = tuple[2];

            CharKey key = CharKey.build((b) -> b.type(CharKey.Type.Alphabet).value(chars));
            CharInput input = CharInput.from(key);

            int wordId = 100 + i;
            PinyinWord word = PinyinWord.build((b) -> b.id(wordId).value(wordValue).spell(wordSpell));
            input.setWord(word);

            this.inputList.selectLast();
            this.inputList.withPending(input);
            this.inputList.confirmPending();
        }
    }

    // =============================== End: 自动化，用于模拟输入等 ===================================
}
