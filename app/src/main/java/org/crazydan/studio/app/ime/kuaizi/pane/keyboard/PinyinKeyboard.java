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

package org.crazydan.studio.app.ime.kuaizi.pane.keyboard;

import java.util.ArrayList;
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.dict.PinyinCharsTree;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.dict.UserInputData;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.keytable.PinyinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.CommittingOptionChooseStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.InputCharsFlipStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.InputCharsSlipStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputCharsInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.user.UserSingleTapMsgData;

import static org.crazydan.studio.app.ime.kuaizi.pane.keyboard.PinyinCandidatesKeyboard.determine_NotConfirmed_InputWord;
import static org.crazydan.studio.app.ime.kuaizi.pane.keyboard.PinyinCandidatesKeyboard.predict_NotConfirmed_Phrase_InputWords;

/**
 * {@link Type#Pinyin 拼音键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class PinyinKeyboard extends BaseKeyboard {
    private final PinyinDict dict;

    public PinyinKeyboard(PinyinDict dict) {
        this.dict = dict;
    }

    @Override
    public Type getType() {
        return Type.Pinyin;
    }

    @Override
    public KeyFactory getKeyFactory(InputList inputList) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(createKeyTableConfig(inputList));

        switch (this.state.type) {
            case InputChars_Slip_Doing: {
                InputCharsSlipStateData stateData = ((InputCharsSlipStateData) this.state.data);

                String level0Char = stateData.getLevel0Key() != null ? stateData.getLevel0Key().getText() : null;
                if (level0Char == null) {
                    return null;
                }

                PinyinCharsTree charsTree = this.dict.getPinyinCharsTree();
                String level1Char = stateData.getLevel1Key() != null ? stateData.getLevel1Key().getText() : null;
                String level2Char = stateData.getLevel2Key() != null ? stateData.getLevel2Key().getText() : null;

                return (KeyFactory.NoAnimation) () -> keyTable.createNextCharKeys(charsTree,
                                                                                  level0Char,
                                                                                  level1Char,
                                                                                  level2Char,
                                                                                  stateData.getLevel2NextChars());
            }
            case InputChars_Flip_Doing: {
                InputCharsFlipStateData stateData = ((InputCharsFlipStateData) this.state.data);
                PinyinCharsTree charsTree = this.dict.getPinyinCharsTree();

                return (KeyFactory.NoAnimation) () -> keyTable.createFullCharKeys(charsTree, stateData.startChar);
            }
            case InputChars_XPad_Input_Doing: {
                InputCharsSlipStateData stateData = ((InputCharsSlipStateData) this.state.data);

                String level0Char = stateData.getLevel0Key() != null ? stateData.getLevel0Key().getText() : null;
                if (level0Char == null) {
                    return null;
                }

                PinyinCharsTree charsTree = this.dict.getPinyinCharsTree();
                String level1Char = stateData.getLevel1Key() != null ? stateData.getLevel1Key().getText() : null;

                return () -> keyTable.createXPadNextCharKeys(charsTree,
                                                             level0Char,
                                                             level1Char,
                                                             stateData.getLevel2NextChars());
            }
            case InputList_Committing_Option_Choose_Doing: {
                CommittingOptionChooseStateData stateData = (CommittingOptionChooseStateData) this.state.data;

                return () -> keyTable.createInputListCommittingOptionKeys(stateData.getOption(),
                                                                          stateData.hasSpell(),
                                                                          stateData.hasVariant());
            }
            default: {
                State previous = this.state.previous;

                // Note：滑屏/翻动输入结束后，恢复按键布局也需禁用动效
                if (previous != null) {
                    switch (previous.type) {
                        case InputChars_Slip_Doing:
                        case InputChars_Flip_Doing:
                            // Note：将前序状态置空，以确保按键动画仅在前后状态变化时被禁用一次
                            this.state = new State(this.state.type, this.state.data);

                            return (KeyFactory.NoAnimation) keyTable::createKeys;
                    }
                }
                return keyTable::createKeys;
            }
        }
    }

    // ====================== Start: 消息处理 ======================

    @Override
    public void onMsg(InputList inputList, InputMsg msg) {
        switch (this.state.type) {
            case InputCandidate_Choose_Doing:
                // Note：其余消息，继续向后处理
                if (msg.type == InputMsgType.Input_Choose_Doing) {
                    choose_InputList_Input(inputList, msg.data.input);
                    return;
                }
            case InputCandidate_AdvanceFilter_Doing:
            case InputChars_Flip_Doing: {
                if (msg.type == InputMsgType.InputList_Clean_Done) {
                    change_State_to_Init();
                    return;
                }
                break;
            }
        }

        super.onMsg(inputList, msg);
    }

    @Override
    public void onMsg(InputList inputList, UserKeyMsg msg) {
        Key<?> key = msg.data.key;

        if (try_Common_OnUserKeyMsg(inputList, msg)) {
            // Note：被禁用的部分按键也需要接受处理
            if (!CtrlKey.isAny(key,
                               CtrlKey.Type.Filter_PinyinCandidate_by_Spell,
                               CtrlKey.Type.Filter_PinyinCandidate_by_Radical,
                               CtrlKey.Type.Commit_InputList_Option)) {
                return;
            }
        }

        switch (this.state.type) {
            case InputChars_Slip_Doing: {
                on_InputChars_Slip_Doing_UserKey_Msg(inputList, msg, key);
                break;
            }
            case InputChars_Flip_Doing: {
                on_InputChars_Flip_Doing_UserKey_Msg(inputList, msg, key);
                break;
            }
            case InputChars_XPad_Input_Doing: {
                on_InputChars_XPad_Input_Doing_UserKey_Msg(inputList, msg, key);
                break;
            }
            case InputList_Committing_Option_Choose_Doing: {
                if (key instanceof CtrlKey) {
                    on_InputList_Committing_Option_Choose_Doing_CtrlKey_Msg(inputList, msg, (CtrlKey) key);
                }
                break;
            }
            default: {
                if (key instanceof CharKey) {
                    on_CharKey_Msg(inputList, msg, (CharKey) key);
                } else if (key instanceof CtrlKey) {
                    on_CtrlKey_Msg(inputList, msg, (CtrlKey) key);
                }
            }
        }
    }

    /** 响应 {@link CharKey} 的消息 */
    private void on_CharKey_Msg(InputList inputList, UserKeyMsg msg, CharKey key) {
        if (key.isDisabled()) {
            return;
        }

        if (isXInputPadEnabled()) {
            if (msg.type == UserKeyMsgType.SingleTap_Key) {
                if (CharKey.isAlphabet(key)) {
                    confirm_or_New_InputList_Pending(inputList);

                    start_InputChars_XPad_Inputting(inputList, key);
                } else {
                    start_Single_Key_Inputting(inputList, key, (UserSingleTapMsgData) msg.data, false);
                }
            }
            return;
        }

        switch (msg.type) {
            case FingerMoving_Start: {
                // 开始滑屏输入：仅针对字母按键
                if (CharKey.isAlphabet(key)) {
                    confirm_or_New_InputList_Pending(inputList);

                    start_InputChars_Slipping(inputList, key);
                }
                break;
            }
            case SingleTap_Key: {
                // 单字符输入
                start_Single_Key_Inputting(inputList, key, (UserSingleTapMsgData) msg.data, false);
                break;
            }
        }
    }

    /** 响应 {@link CtrlKey} 的消息 */
    private void on_CtrlKey_Msg(InputList inputList, UserKeyMsg msg, CtrlKey key) {
        switch (msg.type) {
            case LongPress_Key_Start: {
                if (CtrlKey.is(key, CtrlKey.Type.Commit_InputList)) {
                    play_DoubleTick_InputAudio(key);

                    start_InputList_Committing_Option_Choosing(inputList, key);
                }
                break;
            }
            case SingleTap_Key: {
                if (CtrlKey.is(key, CtrlKey.Type.Pinyin_End)) {
                    play_SingleTick_InputAudio(key);
                    show_InputChars_Input_Popup(key);

                    stop_InputChars_Inputting(inputList, key);
                }
                break;
            }
        }
    }

    // ====================== End: 消息处理 ======================

    // ================== Start: 滑屏输入 =====================

    /** 开始滑屏输入 */
    private void start_InputChars_Slipping(InputList inputList, Key<?> key) {
        State state = new State(State.Type.InputChars_Slip_Doing,
                                new InputCharsSlipStateData(),
                                createInitState());
        change_State_To(key, state);

        CharInput pending = inputList.getPending();
        do_InputChars_Slipping_Input_Key(pending, key);
    }

    /** 进入 {@link State.Type#InputChars_Slip_Doing} 状态后的按键消息处理 */
    private void on_InputChars_Slip_Doing_UserKey_Msg(InputList inputList, UserKeyMsg msg, Key<?> currentKey) {
        switch (msg.type) {
            case FingerMoving: {
                CharInput pending = inputList.getPending();
                Key<?> lastKey = pending.getLastKey();

                if (currentKey instanceof CharKey //
                    && !currentKey.isDisabled() //
                    // 拼音的后继不会是相同字母
                    && !currentKey.isSameWith(lastKey) //
                ) {
                    do_InputChars_Slipping_Input_Key(pending, currentKey);
                }
                break;
            }
            case FingerMoving_Stop: {
                stop_InputChars_Slipping(inputList, currentKey);
                break;
            }
            // Note：翻动手势发生在 FingerMovingStart（滑屏输入开始）之后，
            // FingerMovingEnd（滑屏输入结束）之前，
            // 故而，需要在滑屏输入状态下开始翻动输入
            case FingerFlipping: {
                // Note：翻动发生在滑动之后，在滑动开始时已处理输入替换还是新增，故，这里无需继续处理
                CharInput pending = inputList.getPending();

                // 开始翻动输入。注意，若滑屏输入在较短时间内完成也会触发翻动事件，
                // 故，仅针对录入了一个字符的情况
                if (pending.getKeys().size() == 1) {
                    CharKey firstKey = (CharKey) pending.getFirstKey();

                    show_InputChars_Input_Popup(firstKey);
                    start_InputChars_Flipping(inputList, firstKey);
                }
                break;
            }
        }
    }

    /** 添加输入按键：播放提示音，显示输入按键 */
    private void do_InputChars_Slipping_Input_Key(CharInput pending, Key<?> currentKey) {
        play_DoubleTick_InputAudio(currentKey);
        show_InputChars_Input_Popup(currentKey, false);

        InputCharsSlipStateData stateData = ((InputCharsSlipStateData) this.state.data);

        // 添加输入拼音的后继字母
        Key.Level currentKeyLevel = currentKey.getLevel();
        switch (currentKeyLevel) {
            case level_0: {
                pending.appendKey(currentKey);

                stateData.setLevel0Key(currentKey);
                stateData.setLevel1Key(null);

                stateData.setLevel2Key(null);
                stateData.setLevel2NextChars(new ArrayList<>());
                break;
            }
            case level_1: {
                pending.replaceKeyAfterLevel(Key.Level.level_0, currentKey);

                String level0Char = stateData.getLevel0Key().getText();
                String level1Char = currentKey.getText();

                PinyinCharsTree charsTree = this.dict.getPinyinCharsTree();
                PinyinCharsTree level1CharsTree = charsTree.getChild(level0Char).getChild(level1Char);

                List<String> level2NextChars = level1CharsTree != null
                                               ? level1CharsTree.getNextChars()
                                               : new ArrayList<>();

                stateData.setLevel1Key(currentKey);

                stateData.setLevel2Key(null);
                stateData.setLevel2NextChars(level2NextChars);
                break;
            }
            case level_2: {
                // Note：第二级后继字母已包含第一级后继字母，故而，直接替换第 0 级之后的按键
                pending.replaceKeyAfterLevel(Key.Level.level_0, currentKey);

                stateData.setLevel2Key(currentKey);
                break;
            }
        }

        // 确定拼音的候选字
        determine_NotConfirmed_InputWord(this.dict, pending);

        fire_InputChars_Input_Doing(currentKey, pending, InputCharsInputMsgData.InputMode.slip);
    }

    /** 结束滑屏输入：始终针对 {@link InputList#getPending() 待输入} */
    private void stop_InputChars_Slipping(InputList inputList, Key<?> key) {
        hide_InputChars_Input_Popup();

        stop_InputChars_Inputting(inputList, key);
    }

    // ================== End: 滑屏输入 =====================

    // ================== Start: 翻动输入，即，直接显示以输入按键开头的全部拼音 =====================

    private void start_InputChars_Flipping(InputList inputList, Key<?> key) {
        String startChar = key.getText();
        // 若无以输入按键开头的拼音，则不进入该状态
        if (this.dict.getPinyinCharsTree().getChild(startChar) == null) {
            return;
        }

        InputCharsFlipStateData stateData = new InputCharsFlipStateData(startChar);
        State state = new State(State.Type.InputChars_Flip_Doing, stateData, createInitState());
        change_State_To(key, state);

        // Note：单字母的滑屏输入与翻动输入的触发按键是相同的，以此对先触发的滑屏输入做替换
        CharInput pending = inputList.newPending();
        pending.appendKey(key);

        fire_InputChars_Input_Doing(key, pending, InputCharsInputMsgData.InputMode.flip);
    }

    private void on_InputChars_Flip_Doing_UserKey_Msg(InputList inputList, UserKeyMsg msg, Key<?> key) {
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        play_SingleTick_InputAudio(key);
        show_InputChars_Input_Popup(key);

        stop_InputChars_Flipping(inputList, key);
    }

    private void stop_InputChars_Flipping(InputList inputList, Key<?> key) {
        CharInput pending = inputList.getPending();

        // 先确认当前输入的候选字
        if (!CharKey.isAlphabet(key)) {
            pending.setWord(null);
        } else {
            pending.appendKey(key);

            determine_NotConfirmed_InputWord(this.dict, pending);
        }

        // 再结束输入
        stop_InputChars_Inputting(inputList, key);
    }

    // ================== End: 翻动输入 =====================

    // ================== Start: X 型面板输入 =====================

    private void start_InputChars_XPad_Inputting(InputList inputList, Key<?> key) {
        State state = new State(State.Type.InputChars_XPad_Input_Doing,
                                new InputCharsSlipStateData(),
                                createInitState());
        change_State_To(key, state);

        CharInput pending = inputList.getPending();
        do_InputChars_XPad_Inputting(inputList, pending, key);
    }

    private void do_InputChars_XPad_Inputting(InputList inputList, CharInput pending, Key<?> currentKey) {
        play_SingleTick_InputAudio(currentKey);
        show_InputChars_Input_Popup(currentKey);

        InputCharsSlipStateData stateData = ((InputCharsSlipStateData) this.state.data);
        Key.Level currentKeyLevel = currentKey.getLevel();

        PinyinCharsTree charsTree = this.dict.getPinyinCharsTree();
        boolean needToEndInputting = false;
        boolean needToContinueInputting = false;

        // 添加后继字母，
        switch (currentKeyLevel) {
            case level_0: {
                // 当前 pending 为有效拼音，则结束当前输入，并创建新输入
                if (this.dict.getPinyinCharsTree().isPinyinCharsInput(pending)) {
                    needToEndInputting = true;
                    needToContinueInputting = true;
                    break;
                }

                pending.appendKey(currentKey);

                String level0Char = currentKey.getText();
                PinyinCharsTree level0CharsTree = charsTree.getChild(level0Char);

                stateData.setLevel0Key(currentKey);
                stateData.setLevel1Key(null);

                stateData.setLevel2Key(null);
                stateData.setLevel2NextChars(new ArrayList<>());

                needToEndInputting = level0CharsTree == null || !level0CharsTree.hasChild();
                break;
            }
            case level_1: {
                pending.replaceKeyAfterLevel(Key.Level.level_0, currentKey);

                String level0Char = stateData.getLevel0Key().getText();
                String level1Char = currentKey.getText();
                PinyinCharsTree level1CharsTree = charsTree.getChild(level0Char).getChild(level1Char);

                List<String> level2NextChars = level1CharsTree.getNextChars();

                stateData.setLevel1Key(currentKey);

                stateData.setLevel2Key(null);
                stateData.setLevel2NextChars(level2NextChars);

                needToEndInputting = level2NextChars.isEmpty();
                break;
            }
            case level_2: {
                // Note：第二级后继字母已包含第一级后继字母，故而，直接替换第 0 级之后的按键
                pending.replaceKeyAfterLevel(Key.Level.level_0, currentKey);

                needToEndInputting = true;
                break;
            }
            case level_final: {
                pending.dropKeys();
                pending.appendKey(currentKey);

                needToEndInputting = true;
                break;
            }
        }

        // 并确定候选字
        determine_NotConfirmed_InputWord(this.dict, pending);

        if (needToEndInputting) {
            stop_InputChars_Inputting(inputList, currentKey, !needToContinueInputting);

            if (needToContinueInputting) {
                inputList.newPending();

                start_InputChars_XPad_Inputting(inputList, currentKey);
            }
        } else {
            fire_InputChars_Input_Doing(currentKey, pending, InputCharsInputMsgData.InputMode.circle);
        }
    }

    private void on_InputChars_XPad_Input_Doing_UserKey_Msg(InputList inputList, UserKeyMsg msg, Key<?> key) {
        if (key instanceof CtrlKey && !CtrlKey.isNoOp(key)) {
            on_CtrlKey_Msg(inputList, msg, (CtrlKey) key);
            return;
        }

        // 添加拼音后继字母
        CharInput pending = inputList.getPending();

        if (msg.type == UserKeyMsgType.Press_Key_Stop) {
            stop_InputChars_Inputting(inputList, key);
            return;
        } else if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        if (key instanceof CharKey && !key.isDisabled()) {
            do_InputChars_XPad_Inputting(inputList, pending, key);
        }
    }

    // ================== End: X 型面板输入 =====================

    // ======================== Start: 输入补全 ========================

    @Override
    protected List<String> getTopBestMatchedLatins(String text) {
        return this.dict.findTopBestMatchedLatins(text, 5);
    }

    // ======================== End: 输入补全 ========================

    // ================== Start: 对输入列表 提交选项 的操作 =====================

    private void start_InputList_Committing_Option_Choosing(InputList inputList, Key<?> key) {
        CommittingOptionChooseStateData stateData = new CommittingOptionChooseStateData(inputList);
        State state = new State(State.Type.InputList_Committing_Option_Choose_Doing, stateData, createInitState());

        change_State_To(key, state);
    }

    private void on_InputList_Committing_Option_Choose_Doing_CtrlKey_Msg(
            InputList inputList, UserKeyMsg msg, CtrlKey key
    ) {
        switch (msg.type) {
            case SingleTap_Key: {
                if (CtrlKey.is(key, CtrlKey.Type.Commit_InputList_Option)) {
                    CtrlKey.InputListCommitOption.Option option
                            = ((CtrlKey.InputListCommitOption) key.getOption()).value();

                    Input.Option oldInputOption = inputList.getOption();
                    Input.Option newInputOption = null;
                    switch (option) {
                        case only_pinyin: {
                            InputWord.SpellUsedType spellUsedType = oldInputOption.wordSpellUsedType;
                            if (spellUsedType == InputWord.SpellUsedType.replacing) {
                                spellUsedType = null;
                            } else {
                                spellUsedType = InputWord.SpellUsedType.replacing;
                            }

                            newInputOption = new Input.Option(spellUsedType, oldInputOption.wordVariantUsed);
                            break;
                        }
                        case with_pinyin: {
                            InputWord.SpellUsedType spellUsedType = oldInputOption.wordSpellUsedType;
                            if (spellUsedType == InputWord.SpellUsedType.following) {
                                spellUsedType = null;
                            } else {
                                spellUsedType = InputWord.SpellUsedType.following;
                            }

                            newInputOption = new Input.Option(spellUsedType, oldInputOption.wordVariantUsed);
                            break;
                        }
                        case switch_trad_to_simple:
                        case switch_simple_to_trad: {
                            // 被禁用的繁简转换按钮不做响应
                            if (!key.isDisabled()) {
                                newInputOption = new Input.Option(oldInputOption.wordSpellUsedType,
                                                                  !oldInputOption.wordVariantUsed);
                            }
                            break;
                        }
                    }

                    if (newInputOption == null) {
                        return;
                    }

                    inputList.setOption(newInputOption);
                    ((CommittingOptionChooseStateData) this.state.data).update(inputList);

                    play_SingleTick_InputAudio(key);
                    fire_InputChars_Input_Done(key, null);
                }
                break;
            }
            case LongPress_Key_Start: {
                if (CtrlKey.is(key, CtrlKey.Type.Commit_InputList)) {
                    play_DoubleTick_InputAudio(key);

                    inputList.setOption(null);

                    change_State_to_Init(key);
                }
                break;
            }
        }
    }

    // ================== End: 对输入列表 提交选项 的操作 =====================

    // <<<<<<<<<< 对用户输入数据的处理
    private void doWithUserInputData(InputList inputList, UserInputDataHandler handler) {
        if (getConfig().isUserInputDataDisabled()) {
            return;
        }

        List<List<PinyinWord>> phrases = inputList.getPinyinPhraseWords();
        List<InputWord> emojis = inputList.getEmojis();
        List<String> latins = inputList.getLatins();

        UserInputData data = new UserInputData(phrases, emojis, latins);

        handler.handle(data);
    }

    private interface UserInputDataHandler {
        void handle(UserInputData data);
    }
    // >>>>>>>>>>

    /** 结束输入：始终针对 {@link InputList#getPending() 待输入}，并做状态复位 */
    private void stop_InputChars_Inputting(InputList inputList, Key<?> key) {
        stop_InputChars_Inputting(inputList, key, true);
    }

    /** 结束输入：始终针对 {@link InputList#getPending() 待输入} */
    private void stop_InputChars_Inputting(InputList inputList, Key<?> key, boolean resetState) {
        CharInput pending = inputList.getPending();

        // 若为无效的拼音输入，则直接丢弃
        if (!this.dict.getPinyinCharsTree().isPinyinCharsInput(pending)) {
            drop_InputList_Pending(inputList, key);
        } else {
            predict_NotConfirmed_Phrase_InputWords(this.dict, inputList, pending);

            confirm_InputList_Pending(inputList, key);
        }

        if (resetState) {
            change_State_to_Init(key);
        }
    }

    @Override
    protected void do_InputList_Backspacing(InputList inputList, Key<?> key) {
        super.do_InputList_Backspacing(inputList, key);

        // Note：在 X 型输入状态下只支持输入拼音，故而，若做回删必然会删除当前拼音，
        // 所以，可以直接回到初始状态以便于继续输入其他拼音
        if (this.state.type == State.Type.InputChars_XPad_Input_Doing) {
            change_State_to_Init();
        }
    }

    @Override
    protected void confirm_InputList_Input_Enter_or_Space(InputList inputList, CtrlKey key) {
        // Note：在 X 型输入状态下输入空格，则先结束当前拼音输入再录入空格
        if (this.state.type == State.Type.InputChars_XPad_Input_Doing //
            && CtrlKey.is(key, CtrlKey.Type.Space)) {
            stop_InputChars_Inputting(inputList, key);
        }

        super.confirm_InputList_Input_Enter_or_Space(inputList, key);
    }

    @Override
    protected void before_Commit_InputList(InputList inputList) {
        doWithUserInputData(inputList, this.dict::saveUserInputData);
    }

    @Override
    protected void after_Revoke_Committed_InputList(InputList inputList) {
        doWithUserInputData(inputList, this.dict::revokeSavedUserInputData);
    }
}
