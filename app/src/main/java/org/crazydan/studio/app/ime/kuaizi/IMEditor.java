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

package org.crazydan.studio.app.ime.kuaizi;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.SystemUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigChangeListener;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.BaseInputContext;
import org.crazydan.studio.app.ime.kuaizi.core.Favoriteboard;
import org.crazydan.studio.app.ime.kuaizi.core.FavoriteboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.InputFactory;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.Inputboard;
import org.crazydan.studio.app.ime.kuaizi.core.InputboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.EditorKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.EmojiKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.InputListCommitOptionKeyboard;
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
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputClipMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputListCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.KeyboardHandModeSwitchMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.KeyboardSwitchMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserEditorActionSingleTapMsgData;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Config_Update_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Editor_Edit_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputClip_Discard_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_Close_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_Close_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_Exit_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_HandMode_Switch_Done;
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
    private TaskHandler task;

    private InputList inputList;

    private Keyboard keyboard;
    private Favoriteboard favoriteboard;
    private Inputboard inputboard;
    /** 切换前的主键盘类型 */
    private Keyboard.Type prevMasterKeyboardType;

    private InputMsgListener listener;

    IMEditor(Config.Mutable config, PinyinDict dict) {
        this.config = config;
        this.dict = dict;

        this.task = new TaskHandler(this);

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

        if (this.favoriteboard == null) {
            ClipboardManager clipboard = SystemUtils.getClipboard(context);
            this.favoriteboard = new Favoriteboard(clipboard);
        }

        // 先切换键盘
        switchKeyboardTo(keyboardType);

        // 再重置输入面板
        if (resetInputting) {
            withInputboardContext(this.inputboard::reset);
        } else {
            withInputboardContext(this.inputboard::start);
        }

        fire_InputMsg(Keyboard_Start_Done);

        // 启动异步任务
        this.task.start();
    }

    /** 关闭 {@link IMEditor}，仅隐藏面板，但输入状态保持不变 */
    public void close() {
        withFavoriteboardContext(this.favoriteboard::close);

        fire_InputMsg(Keyboard_Close_Done);
    }

    /** 退出 {@link IMEditor} */
    public void exit() {
        if (this.favoriteboard != null) {
            withFavoriteboardContext(this.favoriteboard::close);
        }

        // Note: 不重置输入列表，以避免误操作导致输入被清空
        //withInputboardContext(this.inputboard::reset);

        // 重置键盘
        if (this.keyboard != null) {
            withKeyboardContext(this.keyboard::reset);
        }

        fire_InputMsg(Keyboard_Exit_Done);
    }

    /** 销毁 {@link IMEditor}，即，关闭并回收资源 */
    public void destroy() {
        this.task.stop();
        this.task = null;

        // 确保拼音字典库能够被开启方及时关闭
        if (!this.config.bool(ConfigKey.disable_dict_db) //
            && this.keyboard != null // 已调用 #start 方法
        ) {
            this.dict.close();
        }

        if (this.favoriteboard != null) {
            withFavoriteboardContext(this.favoriteboard::close);
            this.favoriteboard = null;
        }

        this.config = null;
        this.dict = null;

        this.inputList = null;

        this.keyboard = null;
        this.inputboard = null;
        this.prevMasterKeyboardType = null;

        this.listener = null;
    }

    /** {@link #start} 的异步后处理，避免阻塞主视图的布局更新 */
    private void afterStart() {
        withFavoriteboardContext(this.favoriteboard::start);
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
        switch (key) {
            case hand_mode: {
                // 若更改系统的左右手模式，则以系统的为准
                this.config.set(ConfigKey.hand_mode, newValue);
                break;
            }
        }

        withInputboardContext(this.inputboard::start);

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

        this.log.beginTreeLog("Handle %s", () -> new Object[] { msg.getClass() }) //
                .debug("Message Type: %s", () -> new Object[] { msg.type }) //
                .debug("Message Data: %s", () -> new Object[] { msg.data() });

        switch (msg.type) {
            // 直接处理不需要转发的消息
            case SingleTap_Btn_Close_Keyboard: {
                fire_InputMsg(Keyboard_Close_Doing);
                break;
            }
            case SingleTap_Btn_Editor_Action: {
                UserEditorActionSingleTapMsgData data = msg.data();
                InputMsgData msgData = new EditorEditMsgData(data.action);

                onMsg(InputMsg.build((b) -> b.type(Editor_Edit_Doing).data(msgData)));
                break;
            }
            default: {
                this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] {
                        msg.getClass(), this.inputboard.getClass()
                });

                withInputboardContext((context) -> this.inputboard.onMsg(context, msg));

                this.log.endTreeLog();
                /////////////////////////////////////////////////////////////////
                this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] {
                        msg.getClass(), this.favoriteboard.getClass()
                });

                withFavoriteboardContext((context) -> this.favoriteboard.onMsg(context, msg));

                this.log.endTreeLog();
            }
        }

        this.log.endTreeLog();
    }

    // --------------------------------------

    /** 响应键盘的 {@link InputMsg} 消息：从键盘向上传递给外部监听者 */
    @Override
    public void onMsg(InputMsg msg) {
        // Note: 涉及消息的嵌套处理，可能会发生键盘切换，因此，不能定义 keyboard 的本地变量

        this.log.beginTreeLog("Handle %s", () -> new Object[] { msg.getClass() }) //
                .debug("Message Type: %s", () -> new Object[] { msg.type }) //
                .debug("Message Data: %s", () -> new Object[] { msg.data() });

        on_InputClip_Related_Msg(msg);

        switch (msg.type) {
            case Keyboard_Switch_Doing: {
                on_Keyboard_Switch_Doing_Msg(msg.data());
                // Note: 在键盘切换过程中，不向上转发消息

                this.log.warn("Do not dispatch message %s", () -> new Object[] { msg.type }) //
                        .endTreeLog();
                return;
            }
            case Keyboard_HandMode_Switch_Doing: {
                on_Keyboard_HandMode_Switch_Doing_Msg(msg.data());

                this.log.warn("Do not dispatch message %s", () -> new Object[] { msg.type }) //
                        .endTreeLog();
                return;
            }
            // 向键盘派发 InputList 的消息
            case Input_Choose_Doing:
            case InputList_Clean_Done:
            case InputList_Cleaned_Cancel_Done: {
                this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] {
                        msg.getClass(), this.keyboard.getClass()
                });

                withKeyboardContext((context) -> {
                    this.keyboard.onMsg(context, msg);
                });

                this.log.endTreeLog();
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
                if (EditorAction.hasEditorEffect(data.action)) {
                    this.inputboard.clearCommitted();
                }
                break;
            }
            case Input_Pending_Drop_Done:
            case InputChars_Input_Doing:
            case InputChars_Input_Done:
            case InputCandidate_Choose_Done: {
                // 若产生新的输入，则需要清除 已删除/已提交 的恢复数据
                if (!this.inputList.isEmpty()) {
                    this.log.debug("Clear %s's committed/cleaned for message %s",
                                   () -> new Object[] { this.inputboard.getClass(), msg.type });

                    this.inputboard.clearCommitted();
                    this.inputboard.clearCleaned();
                }
                break;
            }
            default: {
                this.log.warn("Ignore message %s", () -> new Object[] { msg.type });
            }
        }
        this.log.endTreeLog();

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
        List<?> inputQuickList = getInputQuickList();

        InputMsg msg = InputMsg.build((b) -> b.type(type)
                                              .data(data)
                                              .keyFactory(keyFactory)
                                              .inputFactory(inputFactory)
                                              .inputQuickList(inputQuickList)
                                              .inputList(this.inputList, this.inputboard.canRestoreCleaned()));

        this.log.beginTreeLog("Dispatch %s to %s", () -> new Object[] {
                msg.getClass(), this.listener.getClass()
        });

        this.listener.onMsg(msg);

        this.log.endTreeLog();
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

    /** 处理与剪贴数据相关的消息 */
    private void on_InputClip_Related_Msg(InputMsg msg) {
        if (this.config.bool(ConfigKey.disable_input_clip_popup_tips)) {
            return;
        }

        switch (msg.type) {
            case InputList_Commit_Doing: {
                InputListCommitMsgData data = msg.data();
                // 仅换行和空格的输入不影响剪贴数据的处理
                if ("\n".contentEquals(data.text) || " ".contentEquals(data.text)) {
                    break;
                }
                // 仅非替换且可撤回的输入才可被收藏
                if (data.canBeRevoked && CollectionUtils.isEmpty(data.replacements)) {
                    // TODO 在输入提交后提示可收藏会遮挡对目标编辑器的操作，故而，暂时不做支持
                    //asyncSaveTextToFavorite(data.text);
                }
                break;
            }
            case InputList_PairSymbol_Commit_Doing:
            case InputChars_Input_Doing: {
                // 若正在输入，则废弃剪贴数据，且重新激活输入法也不再提示
                discardClips();
                break;
            }
            case InputClip_Apply_Done: {
                InputClipMsgData data = msg.data();
                // Note: 在该消息发送前，剪贴数据已被废弃，故而，这里无需再做处理
                disableClips(data.clip.code);
                break;
            }
        }
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
        // Note: 若切换到输入列表提交选项键盘，则保持前序键盘的输入状态不变，
        // 从而确保退出该键盘后，依然能够回到切换前的状态
        if (current != null && newType != Keyboard.Type.InputList_Commit_Option) {
            withKeyboardContext(current::stop);
        }
        this.config.set(ConfigKey.prev_keyboard_type, currentType);

        // Note: 对特定的键盘需冻结输入列表，以避免打断当前的键盘操作
        boolean frozen = CollectionUtils.contains(new Keyboard.Type[] {
                Keyboard.Type.Editor, Keyboard.Type.InputList_Commit_Option
        }, newType);
        this.inputList.freeze(frozen);

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
            case InputList_Commit_Option:
                return new InputListCommitOptionKeyboard();
            case Math:
                return new MathKeyboard();
            case Symbol:
                return new SymbolKeyboard();
            case Latin:
                return new LatinKeyboard();
            case Emoji:
                return new EmojiKeyboard();
            case Pinyin_Candidate:
                return new PinyinCandidateKeyboard();
            default:
                return new PinyinKeyboard();
        }
    }

    protected void withKeyboardContext(Consumer<KeyboardContext> c) {
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
        return KeyboardContext.build((b) -> withInputContextBuilder(b.config(this.config, this.inputboard).key(key)));
    }

    protected void withInputboardContext(Consumer<InputboardContext> c) {
        InputboardContext context = createInputboardContext();
        c.accept(context);
    }

    private InputboardContext createInputboardContext() {
        return InputboardContext.build((b) -> withInputContextBuilder(b.config(this.config)));
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
        InputboardContext context = createInputboardContext();
        return this.inputboard.buildInputFactory(context);
    }

    private void withInputContextBuilder(BaseInputContext.Builder<?, ?> builder) {
        builder.dict(this.dict).inputList(this.inputList).listener(this);
    }

    private List<?> getInputQuickList() {
        if (this.inputList.verifyCompletions()) {
            return this.inputList.getCompletionViewDataList();
        } else if ( //
                !this.config.bool(ConfigKey.disable_input_clip_popup_tips) //
                && this.favoriteboard.verifyClips() //
        ) {
            return this.favoriteboard.getClips();
        }
        return null;
    }

    protected void withFavoriteboardContext(Consumer<FavoriteboardContext> c) {
        String usedClipCode = this.config.get(ConfigKey.used_input_clip_code);
        boolean clipsDisabled = this.config.bool(ConfigKey.disable_input_clip_popup_tips);

        FavoriteboardContext context = FavoriteboardContext.build((b) -> withInputContextBuilder(b.usedClipCode(
                usedClipCode).clipsDisabled(clipsDisabled)));
        c.accept(context);
    }

    private void disableClips(String clipCode) {
        this.config.set(ConfigKey.used_input_clip_code, clipCode);
    }

    private void discardClips() {
        if (this.config.bool(ConfigKey.disable_input_clip_popup_tips) //
            || !this.favoriteboard.verifyClips() //
        ) {
            return;
        }

        String clipCode = this.favoriteboard.discardClips();
        disableClips(clipCode);

        fire_InputMsg(InputClip_Discard_Done);
    }

    private void asyncSaveTextToFavorite(CharSequence text) {
        this.task.removeMessages(TaskHandler.MSG_FAVORITE_TEXT);

        Message msg = new Message();
        msg.what = TaskHandler.MSG_FAVORITE_TEXT;
        msg.obj = text;

        this.task.sendMessage(msg);
    }

    private void saveTextToFavorite(CharSequence text) {
        withFavoriteboardContext((context) -> this.favoriteboard.trySave(context, text.toString()));
    }

    // =============================== Start: 自动化，用于模拟输入等 ===================================

    /** 更改最后一个输入的候选字 */
    public void changeLastInputWord(InputWord word) {
        CharInput input = this.inputList.getLastCharInput();
        input.setWord(word);
        input.confirmWord();
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

            this.inputList.selectLast();

            CharKey key = CharKey.build((b) -> b.type(CharKey.Type.Alphabet).value(chars));
            CharInput pending = this.inputList.newCharPending();
            pending.appendKey(key);

            int wordId = 100 + i;
            PinyinWord word = PinyinWord.build((b) -> b.id(wordId).value(wordValue).spell(wordSpell));
            pending.setWord(word);

            this.inputList.confirmPending();
        }
    }

    // =============================== End: 自动化，用于模拟输入等 ===================================

    private static class TaskHandler extends Handler {
        private static final int MSG_START = 0;
        private static final int MSG_FAVORITE_TEXT = 2;

        private final IMEditor editor;

        public TaskHandler(IMEditor editor) {
            super(Looper.getMainLooper());

            this.editor = editor;
        }

        public void start() {
            stop();
            sendEmptyMessage(MSG_START);
        }

        public void stop() {
            removeMessages(MSG_START);
            removeMessages(MSG_FAVORITE_TEXT);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START: {
                    this.editor.afterStart();
                    break;
                }
                case MSG_FAVORITE_TEXT: {
                    CharSequence text = (CharSequence) msg.obj;
                    this.editor.saveTextToFavorite(text);
                    break;
                }
            }
        }
    }
}
