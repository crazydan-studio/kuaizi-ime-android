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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard;

import java.util.ArrayList;
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.PinyinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.state.InputCharsFlipStateData;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.state.InputCharsSlipStateData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputCharsInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinCharsTree;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;

import static org.crazydan.studio.app.ime.kuaizi.core.keyboard.PinyinCandidateKeyboard.determine_NotConfirmed_InputWord;
import static org.crazydan.studio.app.ime.kuaizi.core.keyboard.PinyinCandidateKeyboard.predict_NotConfirmed_Phrase_InputWords;

/**
 * {@link Type#Pinyin 拼音键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class PinyinKeyboard extends BaseKeyboard {

    public PinyinKeyboard(PinyinDict dict) {
        super(dict);
    }

    @Override
    public Type getType() {return Type.Pinyin;}

    @Override
    public boolean isMaster() {return true;}

    @Override
    public KeyFactory buildKeyFactory(KeyboardContext context) {
        KeyTableConfig keyTableConf = createKeyTableConfig(context);
        PinyinKeyTable keyTable = PinyinKeyTable.create(keyTableConf);

        PinyinCharsTree charsTree = this.dict.getPinyinCharsTree();
        switch (this.state.type) {
            case InputChars_Slip_Doing: {
                InputCharsSlipStateData stateData = this.state.data();

                String level0Char = stateData.getLevel0Key() != null ? stateData.getLevel0Key().value : null;
                if (level0Char == null) {
                    return null;
                }

                String level1Char = stateData.getLevel1Key() != null ? stateData.getLevel1Key().value : null;
                String level2Char = stateData.getLevel2Key() != null ? stateData.getLevel2Key().value : null;

                return (KeyFactory.NoAnimation) () -> keyTable.createNextCharKeys(charsTree,
                                                                                  level0Char,
                                                                                  level1Char,
                                                                                  level2Char,
                                                                                  stateData.getLevel2NextChars());
            }
            case InputChars_Flip_Doing: {
                InputCharsFlipStateData stateData = this.state.data();

                return (KeyFactory.NoAnimation) () -> keyTable.createFullCharKeys(charsTree, stateData.startChar);
            }
            case InputChars_XPad_Input_Doing: {
                InputCharsSlipStateData stateData = this.state.data();

                String level0Char = stateData.getLevel0Key() != null ? stateData.getLevel0Key().value : null;
                if (level0Char == null) {
                    return null;
                }

                String level1Char = stateData.getLevel1Key() != null ? stateData.getLevel1Key().value : null;

                return () -> keyTable.createXPadNextCharKeys(charsTree,
                                                             level0Char,
                                                             level1Char,
                                                             stateData.getLevel2NextChars());
            }
            default: {
                State previous = this.state.previous;

                // Note：滑屏/翻动输入结束后，恢复按键布局也需禁用动效
                if (previous != null) {
                    switch (previous.type) {
                        case InputChars_Slip_Doing:
                        case InputChars_Flip_Doing:
                            // Note：将前序状态置空，以确保按键动画仅在前后状态变化时被禁用一次
                            this.state = new State(this.state.type, (State.Data) this.state.data());

                            return (KeyFactory.NoAnimation) keyTable::createKeys;
                    }
                }
                return keyTable::createKeys;
            }
        }
    }

    // ====================== Start: 消息处理 ======================

    @Override
    public void onMsg(KeyboardContext context, UserKeyMsg msg) {
        Key key = context.key();
        // Note: 在滑动结束时可能无关联按键，此时需继续由不同的状态处理该结束消息
        if (key != null && try_On_Common_UserKey_Msg(context, msg)) {
            return;
        }

        switch (this.state.type) {
            case InputChars_Slip_Doing: {
                on_InputChars_Slip_Doing_UserKey_Msg(context, msg);
                break;
            }
            case InputChars_Flip_Doing: {
                on_InputChars_Flip_Doing_UserKey_Msg(context, msg);
                break;
            }
            case InputChars_XPad_Input_Doing: {
                on_InputChars_XPad_Input_Doing_UserKey_Msg(context, msg);
                break;
            }
            default: {
                if (key instanceof CharKey) {
                    on_CharKey_Msg(context, msg);
                } else if (key instanceof CtrlKey) {
                    on_CtrlKey_Msg(context, msg);
                }
            }
        }
    }

    /** 响应 {@link CharKey} 的消息 */
    private void on_CharKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        CharKey key = context.key();
        if (key.disabled) {
            return;
        }

        if (context.xInputPadEnabled) {
            on_CharKey_Msg_Over_XPad(context, msg);
            return;
        }

        switch (msg.type) {
            case FingerMoving_Start: {
                // 开始滑屏输入：仅针对字母按键
                if (CharKey.Type.Alphabet.match(key)) {
                    confirm_or_New_InputList_Pending(context);

                    start_InputChars_Slipping(context);
                }
                break;
            }
            case SingleTap_Key: {
                InputList inputList = context.inputList;
                boolean directInputting = false;
                switch (key.type) {
                    case Emoji:
                    case Symbol: {
                        directInputting = inputList.isEmpty();
                        break;
                    }
                }

                // 单字符输入
                start_Single_CharKey_Inputting(context, msg.data(), directInputting);
                break;
            }
            case LongPress_Key_Tick: {
                // 字符、表情、符号连续输入
                try_On_LongPress_Tick_as_SingleTap_Msg(context, msg, this::on_CharKey_Msg);
                break;
            }
        }
    }

    /** 响应 {@link CtrlKey} 的消息 */
    private void on_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        // Note: 无单独的控制按键处理逻辑
    }

    // ====================== End: 消息处理 ======================

    // ================== Start: 滑屏输入 =====================

    /** 开始滑屏输入 */
    private void start_InputChars_Slipping(KeyboardContext context) {
        State state = new State(State.Type.InputChars_Slip_Doing, new InputCharsSlipStateData(), createInitState());
        change_State_To(context, state);

        InputList inputList = context.inputList;
        CharInput pending = inputList.getCharPending();

        do_InputChars_Slipping_Input_CharKey(context, pending);
    }

    /** 进入 {@link State.Type#InputChars_Slip_Doing} 状态后的按键消息处理 */
    private void on_InputChars_Slip_Doing_UserKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        Key key = context.key();
        InputList inputList = context.inputList;

        switch (msg.type) {
            case FingerMoving: {
                CharInput pending = inputList.getCharPending();
                Key lastKey = pending.getLastKey();

                if (key instanceof CharKey //
                    && !key.disabled //
                    // 拼音的后继不会是相同字母
                    && !(key.value.equals(lastKey.value) //
                         && ((CharKey) key).type.match(lastKey)) //
                ) {
                    do_InputChars_Slipping_Input_CharKey(context, pending);
                }
                break;
            }
            case FingerMoving_Stop: {
                stop_InputChars_Slipping(context);
                break;
            }
            // Note：翻动手势发生在 FingerMovingStart（滑屏输入开始）之后，
            // FingerMovingEnd（滑屏输入结束）之前，
            // 故而，需要在滑屏输入状态下开始翻动输入
            case FingerFlipping: {
                // Note：翻动发生在滑动之后，在滑动开始时已处理输入替换还是新增，故，这里无需继续处理
                CharInput pending = inputList.getCharPending();

                // 开始翻动输入。注意，若滑屏输入在较短时间内完成也会触发翻动事件，
                // 故，仅针对录入了一个字符的情况
                if (pending.getKeys().size() == 1) {
                    CharKey firstKey = (CharKey) pending.getFirstKey();

                    context = context.copy((b) -> b.key(firstKey));

                    show_InputChars_Input_Popup(context);
                    start_InputChars_Flipping(context);
                }
                break;
            }
        }
    }

    /** 添加输入按键：播放提示音，显示输入按键 */
    private void do_InputChars_Slipping_Input_CharKey(KeyboardContext context, CharInput pending) {
        CharKey key = context.key();

        play_DoubleTick_InputAudio(context);
        show_InputChars_Input_Popup(context, false);

        InputCharsSlipStateData stateData = this.state.data();

        // 添加输入拼音的后继字母
        CharKey.Level currentKeyLevel = key.level;
        switch (currentKeyLevel) {
            case level_0: {
                pending.appendKey(key);

                stateData.setLevel0Key(key);
                stateData.setLevel1Key(null);

                stateData.setLevel2Key(null);
                stateData.setLevel2NextChars(new ArrayList<>());
                break;
            }
            case level_1: {
                pending.replaceKeyAfterLevel(CharKey.Level.level_0, key);

                String level0Char = stateData.getLevel0Key().value;
                String level1Char = key.value;

                PinyinCharsTree charsTree = this.dict.getPinyinCharsTree();
                PinyinCharsTree level1CharsTree = charsTree.getChild(level0Char).getChild(level1Char);

                List<String> level2NextChars = level1CharsTree != null
                                               ? level1CharsTree.getNextChars()
                                               : new ArrayList<>();

                stateData.setLevel1Key(key);

                stateData.setLevel2Key(null);
                stateData.setLevel2NextChars(level2NextChars);
                break;
            }
            case level_2: {
                // Note：第二级后继字母已包含第一级后继字母，故而，直接替换第 0 级之后的按键
                pending.replaceKeyAfterLevel(CharKey.Level.level_0, key);

                stateData.setLevel2Key(key);
                break;
            }
        }

        // 确定拼音的候选字
        determine_NotConfirmed_InputWord(this.dict, pending);

        fire_InputChars_Input_Doing(context, pending, InputCharsInputMsgData.InputMode.slip);
    }

    /** 结束滑屏输入：始终针对 {@link InputList#getCharPending() 待输入} */
    private void stop_InputChars_Slipping(KeyboardContext context) {
        hide_InputChars_Input_Popup(context);

        stop_InputChars_Inputting(context);
    }

    // ================== End: 滑屏输入 =====================

    // ================== Start: 翻动输入，即，直接显示以输入按键开头的全部拼音 =====================

    private void start_InputChars_Flipping(KeyboardContext context) {
        Key key = context.key();
        InputList inputList = context.inputList;

        String startChar = key.value;
        // 若无以输入按键开头的拼音，则不进入该状态
        if (this.dict.getPinyinCharsTree().getChild(startChar) == null) {
            return;
        }

        InputCharsFlipStateData stateData = new InputCharsFlipStateData(startChar);
        State state = new State(State.Type.InputChars_Flip_Doing, stateData, createInitState());
        change_State_To(context, state);

        // Note：单字母的滑屏输入与翻动输入的触发按键是相同的，因此，对先触发的滑屏输入做替换
        CharInput pending = inputList.newCharPending();
        pending.appendKey(key);

        fire_InputChars_Input_Doing(context, pending, InputCharsInputMsgData.InputMode.flip);
    }

    private void on_InputChars_Flip_Doing_UserKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        play_SingleTick_InputAudio(context);
        show_InputChars_Input_Popup(context);

        stop_InputChars_Flipping(context);
    }

    private void stop_InputChars_Flipping(KeyboardContext context) {
        Key key = context.key();
        InputList inputList = context.inputList;
        CharInput pending = inputList.getCharPending();

        // 先确认当前输入的候选字
        if (!CharKey.Type.Alphabet.match(key)) {
            pending.setWord(null);
        } else {
            pending.appendKey(key);

            determine_NotConfirmed_InputWord(this.dict, pending);
        }

        // 再结束输入
        stop_InputChars_Inputting(context);
    }

    // ================== End: 翻动输入 =====================

    // ================== Start: X 型面板输入 =====================

    private void on_CharKey_Msg_Over_XPad(KeyboardContext context, UserKeyMsg msg) {
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        CharKey key = context.key();
        if (CharKey.Type.Alphabet.match(key)) {
            confirm_or_New_InputList_Pending(context);

            start_InputChars_XPad_Inputting(context);
        } else {
            start_Single_CharKey_Inputting(context, msg.data(), false);
        }
    }

    private void on_InputChars_XPad_Input_Doing_UserKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        Key key = context.key();
        InputList inputList = context.inputList;

        if (key instanceof CtrlKey && !CtrlKey.Type.NoOp.match(key)) {
            on_CtrlKey_Msg(context, msg);
            return;
        }

        // 添加拼音后继字母
        switch (msg.type) {
            case SingleTap_Key: {
                if (key instanceof CharKey && !key.disabled) {
                    CharInput pending = inputList.getCharPending();

                    do_InputChars_XPad_Input_CharKey(context, pending);
                }
                break;
            }
            case Press_Key_Stop: {
                stop_InputChars_Inputting(context);
                break;
            }
        }
    }

    private void start_InputChars_XPad_Inputting(KeyboardContext context) {
        State state = new State(State.Type.InputChars_XPad_Input_Doing,
                                new InputCharsSlipStateData(),
                                createInitState());
        change_State_To(context, state);

        InputList inputList = context.inputList;
        CharInput pending = inputList.getCharPending();

        do_InputChars_XPad_Input_CharKey(context, pending);
    }

    private void do_InputChars_XPad_Input_CharKey(KeyboardContext context, CharInput pending) {
        CharKey key = context.key();

        InputCharsSlipStateData stateData = this.state.data();
        CharKey.Level currentKeyLevel = key.level;

        PinyinCharsTree charsTree = this.dict.getPinyinCharsTree();
        boolean needToEndInputting = false;
        boolean needToContinueInputting = false;

        // 添加输入拼音的后继字母
        switch (currentKeyLevel) {
            case level_0: {
                // 若当前 pending 为有效拼音，则结束当前输入，并创建新输入
                if (this.dict.getPinyinCharsTree().isPinyinCharsInput(pending)) {
                    needToEndInputting = true;
                    needToContinueInputting = true;
                    break;
                }

                pending.appendKey(key);

                String level0Char = key.value;
                PinyinCharsTree level0CharsTree = charsTree.getChild(level0Char);

                stateData.setLevel0Key(key);
                stateData.setLevel1Key(null);

                stateData.setLevel2Key(null);
                stateData.setLevel2NextChars(new ArrayList<>());

                needToEndInputting = level0CharsTree == null || !level0CharsTree.hasChild();
                break;
            }
            case level_1: {
                pending.replaceKeyAfterLevel(CharKey.Level.level_0, key);

                String level0Char = stateData.getLevel0Key().value;
                String level1Char = key.value;
                PinyinCharsTree level1CharsTree = charsTree.getChild(level0Char).getChild(level1Char);

                List<String> level2NextChars = level1CharsTree.getNextChars();

                stateData.setLevel1Key(key);

                stateData.setLevel2Key(null);
                stateData.setLevel2NextChars(level2NextChars);

                needToEndInputting = level2NextChars.isEmpty();
                break;
            }
            case level_2: {
                // Note：第二级后继字母已包含第一级后继字母，故而，直接替换第 0 级之后的按键
                pending.replaceKeyAfterLevel(CharKey.Level.level_0, key);

                needToEndInputting = true;
                break;
            }
            case level_final: {
                pending.dropKeys();
                pending.appendKey(key);

                needToEndInputting = true;
                break;
            }
        }

        // Note: 若需要连续输入下一个拼音，则等到下一次输入调用时再播放音效
        if (!needToContinueInputting) {
            play_SingleTick_InputAudio(context);
            show_InputChars_Input_Popup(context);
        }

        // 确定候选字
        determine_NotConfirmed_InputWord(this.dict, pending);

        InputList inputList = context.inputList;
        if (needToEndInputting) {
            stop_InputChars_Inputting(context, !needToContinueInputting);

            if (needToContinueInputting) {
                inputList.newCharPending();

                start_InputChars_XPad_Inputting(context);
            }
        } else {
            fire_InputChars_Input_Doing(context, pending, InputCharsInputMsgData.InputMode.circle);
        }
    }

    // ================== End: X 型面板输入 =====================

    // ======================== Start: 输入补全 ========================

    @Override
    protected List<String> getTopBestMatchedLatins(String text) {
        return this.dict.findTopBestMatchedLatins(text, 5);
    }

    // ======================== End: 输入补全 ========================

    /** 结束输入：始终针对 {@link InputList#getCharPending() 待输入}，并做状态复位 */
    private void stop_InputChars_Inputting(KeyboardContext context) {
        stop_InputChars_Inputting(context, true);
    }

    /** 结束输入：始终针对 {@link InputList#getCharPending() 待输入} */
    private void stop_InputChars_Inputting(KeyboardContext context, boolean resetState) {
        InputList inputList = context.inputList;
        CharInput pending = inputList.getCharPending();

        // 若为无效的拼音输入，则直接丢弃
        if (!this.dict.getPinyinCharsTree().isPinyinCharsInput(pending)) {
            drop_InputList_Pending(context);
        } else {
            predict_NotConfirmed_Phrase_InputWords(this.dict, inputList, pending, true);

            confirm_InputList_Pending(context);
        }

        if (resetState) {
            change_State_to_Init(context);
        }
    }

    @Override
    protected void do_InputList_Backspacing(KeyboardContext context) {
        super.do_InputList_Backspacing(context);

        // Note：在 X 型输入状态下只支持输入拼音，故而，若做回删必然会删除当前拼音，
        // 所以，可以直接回到初始状态以便于继续输入其他拼音
        if (this.state.type == State.Type.InputChars_XPad_Input_Doing) {
            change_State_to_Init(context);
        }
    }

    @Override
    protected void after_InputList_Backspacing(KeyboardContext context) {
        InputList inputList = context.inputList;
        CharInput pending = inputList.getCharPending();

        // 在回删输入列表后，也做输入预测
        predict_NotConfirmed_Phrase_InputWords(this.dict, inputList, pending, true);
    }

    @Override
    protected void confirm_InputList_Input_Enter_or_Space(KeyboardContext context) {
        Key key = context.key();

        // Note：在 X 型输入状态下输入空格，则先结束当前拼音输入再录入空格
        if (this.state.type == State.Type.InputChars_XPad_Input_Doing //
            && CtrlKey.Type.Space.match(key)) {
            stop_InputChars_Inputting(context);
        }

        super.confirm_InputList_Input_Enter_or_Space(context);
    }
}
