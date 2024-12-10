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
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.CommittingOptionChooseDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.InputCharsFlipDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.InputCharsSlipDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputListMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputListMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputCharsInputtingMsgData;
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
    protected KeyFactory doGetKeyFactory() {
        PinyinKeyTable keyTable = PinyinKeyTable.create(createKeyTableConfig());

        switch (this.state.type) {
            case InputChars_Slip_Doing: {
                InputCharsSlipDoingStateData stateData = ((InputCharsSlipDoingStateData) this.state.data);

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
                InputCharsFlipDoingStateData stateData = ((InputCharsFlipDoingStateData) this.state.data);
                PinyinCharsTree charsTree = this.dict.getPinyinCharsTree();

                return (KeyFactory.NoAnimation) () -> keyTable.createFullCharKeys(charsTree, stateData.startChar);
            }
            case InputChars_XPad_Input_Doing: {
                InputCharsSlipDoingStateData stateData = ((InputCharsSlipDoingStateData) this.state.data);

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
                CommittingOptionChooseDoingStateData stateData = (CommittingOptionChooseDoingStateData) this.state.data;

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

    @Override
    public void onMsg(InputList inputList, InputListMsg msg, InputListMsgData msgData) {
        switch (this.state.type) {
            case InputCandidate_Choose_Doing:
                // Note：其余消息，继续向后处理
                if (msg == InputListMsg.Input_Choose_Doing) {
                    start_Input_Choosing(inputList, msgData.target);
                    return;
                }
            case InputCandidate_AdvanceFilter_Doing:
            case InputChars_Flip_Doing: {
                if (msg == InputListMsg.Inputs_Clean_Done) {
                    change_State_to_Init();
                    return;
                }
                break;
            }
        }

        super.onMsg(inputList, msg, msgData);
    }

    @Override
    public void onMsg(InputList inputList, UserKeyMsg msg, UserKeyMsgData data) {
        Key<?> key = data.target;
        if (try_OnUserKeyMsg(inputList, msg, data) //
            // Note：被禁用的部分按键也需要接受处理
            && !CtrlKey.isAny(key,
                              CtrlKey.Type.Filter_PinyinInputCandidate_by_Spell,
                              CtrlKey.Type.Filter_PinyinInputCandidate_by_Radical,
                              CtrlKey.Type.Commit_InputList_Option) //
        ) {
            return;
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
                if (key instanceof CharKey && !key.isDisabled()) {
                    on_CharKey_Msg(inputList, msg, (CharKey) key, data);
                } else if (key instanceof CtrlKey) {
                    on_CtrlKey_Msg(inputList, msg, (CtrlKey) key);
                }
            }
        }
    }

    private void on_CharKey_Msg(InputList inputList, UserKeyMsg msg, CharKey key, UserKeyMsgData data) {
        if (isXInputPadEnabled()) {
            if (msg == UserKeyMsg.SingleTap_Key) {
                play_SingleTick_InputAudio(key);
                show_InputChars_Input_Popup(key);

                if (CharKey.isAlphabet(key)) {
                    confirm_or_New_InputList_Pending(inputList);

                    CharInput pending = inputList.getPending();

                    start_InputChars_XPad_Inputting(inputList, pending, key);
                } else {
                    start_Single_Key_Inputting(inputList, key, (UserSingleTapMsgData) data, false);
                }
            }
            return;
        }

        switch (msg) {
            case FingerMoving_Start: {
                // 开始滑屏输入
                if (CharKey.isAlphabet(key)) {
                    play_DoubleTick_InputAudio(key);
                    show_InputChars_Input_Popup(key, false);

                    confirm_or_New_InputList_Pending(inputList);

                    CharInput pending = inputList.getPending();

                    start_InputChars_Slipping(pending, key);
                }
                break;
            }
            case SingleTap_Key: {
                // 单字符输入
                play_SingleTick_InputAudio(key);
                show_InputChars_Input_Popup(key);

                start_Single_Key_Inputting(inputList, key, (UserSingleTapMsgData) data, false);
                break;
            }
        }
    }

    private void on_CtrlKey_Msg(InputList inputList, UserKeyMsg msg, CtrlKey key) {
        switch (msg) {
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

                    CharInput pending = inputList.getPending();
                    stop_InputChars_Inputting(inputList, pending, key);
                }
                break;
            }
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
            stop_InputChars_Inputting(inputList, inputList.getPending(), key);
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

    private void on_InputChars_Slip_Doing_UserKey_Msg(InputList inputList, UserKeyMsg msg, Key<?> key) {
        switch (msg) {
            case FingerMoving: {
                // 添加拼音后继字母
                CharInput pending = inputList.getPending();

                Key<?> lastKey = pending.getLastKey();
                if (key instanceof CharKey //
                    && !key.isDisabled() //
                    // 拼音的后继不会是相同字母
                    && !key.isSameWith(lastKey) //
                ) {
                    play_DoubleTick_InputAudio(key);
                    show_InputChars_Input_Popup(key, false);

                    do_InputChars_Slipping(pending, key);
                }
                break;
            }
            case FingerMoving_Stop: {
                CharInput pending = inputList.getPending();

                stop_InputChars_Slipping(inputList, pending, key);
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

    private void on_InputChars_Flip_Doing_UserKey_Msg(InputList inputList, UserKeyMsg msg, Key<?> key) {
        if (msg == UserKeyMsg.SingleTap_Key) {
            play_SingleTick_InputAudio(key);
            show_InputChars_Input_Popup(key);

            CharInput pending = inputList.getPending();
            stop_InputChars_Flipping(inputList, pending, key);
        }
    }

    private void on_InputChars_XPad_Input_Doing_UserKey_Msg(InputList inputList, UserKeyMsg msg, Key<?> key) {
        if (key instanceof CtrlKey && !CtrlKey.isNoOp(key)) {
            on_CtrlKey_Msg(inputList, msg, (CtrlKey) key);
            return;
        }

        // 添加拼音后继字母
        CharInput pending = inputList.getPending();

        if (msg == UserKeyMsg.Press_Key_Stop) {
            stop_InputChars_Inputting(inputList, pending, key);
            return;
        } else if (msg != UserKeyMsg.SingleTap_Key) {
            return;
        }

        play_SingleTick_InputAudio(key);
        show_InputChars_Input_Popup(key);

        if (key instanceof CharKey && !key.isDisabled()) {
            do_InputChars_XPad_Inputting(inputList, pending, key);
        }
    }

    // >>>>>>>>> 滑屏输入
    private void start_InputChars_Slipping(CharInput pending, Key<?> key) {
        State state = new State(State.Type.InputChars_Slip_Doing,
                                new InputCharsSlipDoingStateData(),
                                createInitState());
        change_State_To(key, state);

        do_InputChars_Slipping(pending, key);
    }

    private void do_InputChars_Slipping(CharInput pending, Key<?> currentKey) {
        InputCharsSlipDoingStateData stateData = ((InputCharsSlipDoingStateData) this.state.data);
        Key.Level currentKeyLevel = currentKey.getLevel();
        PinyinCharsTree charsTree = this.dict.getPinyinCharsTree();

        // 添加后继字母，
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

        // 并确定候选字
        determine_NotConfirmed_InputWord(this.dict, pending);

        fire_InputChars_Input_Doing(currentKey, InputCharsInputtingMsgData.KeyInputType.slip);
    }

    private void stop_InputChars_Slipping(InputList inputList, CharInput input, Key<?> key) {
        hide_InputChars_Input_Popup();

        stop_InputChars_Inputting(inputList, input, key);
    }

    private void stop_InputChars_Inputting(InputList inputList, CharInput pending, Key<?> key) {
        stop_InputChars_Inputting(inputList, pending, key, true);
    }

    private void stop_InputChars_Inputting(InputList inputList, CharInput pending, Key<?> key, boolean reset) {
        // 非拼音输入，视为无效输入，直接丢弃
        if (!this.dict.getPinyinCharsTree().isPinyinCharsInput(pending)) {
            drop_InputList_Pending(inputList, key);
        } else {
            predict_NotConfirmed_Phrase_InputWords(this.dict, inputList, pending);

            confirm_InputList_Pending(inputList, key);
        }

        if (reset) {
            // Note：将最后的输入按键附加到消息中，以便于识别哪个按键触发了状态变化
            change_State_to_Init(key);
        }
    }
    // >>>>>>>>>>>>

    // >>>>>>>>> 翻动输入
    private void start_InputChars_Flipping(InputList inputList, Key<?> key) {
        String startChar = key.getText();
        // 若无以输入按键开头的拼音，则不进入该状态
        if (this.dict.getPinyinCharsTree().getChild(startChar) == null) {
            return;
        }

        InputCharsFlipDoingStateData stateData = new InputCharsFlipDoingStateData(startChar);
        State state = new State(State.Type.InputChars_Flip_Doing, stateData, createInitState());
        change_State_To(key, state);

        // Note：单字母的滑屏输入与翻动输入的触发按键是相同的，以此对先触发的滑屏输入做替换
        CharInput pending = inputList.newPending();
        pending.appendKey(key);

        fire_InputChars_Input_Doing(key, InputCharsInputtingMsgData.KeyInputType.flip);
    }

    private void stop_InputChars_Flipping(InputList inputList, CharInput input, Key<?> key) {
        // 先确认当前输入的候选字
        if (!CharKey.isAlphabet(key)) {
            input.setWord(null);
        } else {
            input.appendKey(key);
            determine_NotConfirmed_InputWord(this.dict, input);
        }

        // 再结束输入
        stop_InputChars_Inputting(inputList, input, key);
    }
    // >>>>>>>>>>>>

    // >>>>>>>>> X 型面板输入
    private void start_InputChars_XPad_Inputting(InputList inputList, CharInput pending, Key<?> key) {
        State state = new State(State.Type.InputChars_XPad_Input_Doing,
                                new InputCharsSlipDoingStateData(),
                                createInitState());
        change_State_To(key, state);

        do_InputChars_XPad_Inputting(inputList, pending, key);
    }

    private void do_InputChars_XPad_Inputting(InputList inputList, CharInput pending, Key<?> currentKey) {
        InputCharsSlipDoingStateData stateData = ((InputCharsSlipDoingStateData) this.state.data);
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
            stop_InputChars_Inputting(inputList, pending, currentKey, !needToContinueInputting);

            if (needToContinueInputting) {
                start_InputChars_XPad_Inputting(inputList, inputList.newPending(), currentKey);
            }
        } else {
            fire_InputChars_Input_Doing(currentKey, InputCharsInputtingMsgData.KeyInputType.circle);
        }
    }
    // >>>>>>>>>>>>

    @Override
    protected void do_InputList_Phrase_Completion_Updating(InputList inputList, Input<?> input) {
    }

    // >>>>>>>>>>

    // >>>>>>>>> 对输入列表 提交选项 的操作
    private void start_InputList_Committing_Option_Choosing(InputList inputList, Key<?> key) {
        CommittingOptionChooseDoingStateData stateData = new CommittingOptionChooseDoingStateData(inputList);
        State state = new State(State.Type.InputList_Committing_Option_Choose_Doing, stateData, createInitState());

        change_State_To(key, state);
    }

    private void on_InputList_Committing_Option_Choose_Doing_CtrlKey_Msg(
            InputList inputList, UserKeyMsg msg, CtrlKey key
    ) {
        switch (msg) {
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
                    ((CommittingOptionChooseDoingStateData) this.state.data).update(inputList);

                    play_SingleTick_InputAudio(key);
                    fire_InputChars_Input_Done(key);
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
    // <<<<<<<<<

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
}
