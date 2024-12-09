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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinCharsTree;
import org.crazydan.studio.app.ime.kuaizi.dict.UserInputData;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.EmojiWord;
import org.crazydan.studio.app.ime.kuaizi.pane.input.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.keytable.PinyinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.CandidatePinyinWordAdvanceFilterDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.CandidatePinyinWordChooseDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.InputCharsFlipDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.InputCharsSlipDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.PagingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputListMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputListMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputCandidateChoosingMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.user.UserFingerFlippingMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.user.UserSingleTapMsgData;

/**
 * {@link Type#Pinyin 汉语拼音键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class PinyinKeyboard extends BaseKeyboard {

    public PinyinKeyboard(KeyboardMsgListener listener, Type prevType) {super(listener, prevType);}

    @Override
    public Type getType() {
        return Type.Pinyin;
    }

    @Override
    protected KeyFactory doGetKeyFactory() {
        InputList inputList = getInputList();
        PinyinKeyTable keyTable = PinyinKeyTable.create(createKeyTableConfig());

        switch (this.state.type) {
            case InputChars_Slip_Doing: {
                InputCharsSlipDoingStateData stateData = ((InputCharsSlipDoingStateData) this.state.data);

                String level0Char = stateData.getLevel0Key() != null ? stateData.getLevel0Key().getText() : null;
                if (level0Char == null) {
                    return null;
                }

                PinyinCharsTree charsTree = this.pinyinDict.getPinyinCharsTree();
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
                PinyinCharsTree charsTree = this.pinyinDict.getPinyinCharsTree();

                return (KeyFactory.NoAnimation) () -> keyTable.createFullCharKeys(charsTree, stateData.startChar);
            }
            case InputChars_XPad_Input_Doing: {
                InputCharsSlipDoingStateData stateData = ((InputCharsSlipDoingStateData) this.state.data);

                String level0Char = stateData.getLevel0Key() != null ? stateData.getLevel0Key().getText() : null;
                if (level0Char == null) {
                    return null;
                }

                PinyinCharsTree charsTree = this.pinyinDict.getPinyinCharsTree();
                String level1Char = stateData.getLevel1Key() != null ? stateData.getLevel1Key().getText() : null;

                return () -> keyTable.createXPadNextCharKeys(charsTree,
                                                             level0Char,
                                                             level1Char,
                                                             stateData.getLevel2NextChars());
            }
            case InputCandidate_Choose_Doing: {
                CandidatePinyinWordChooseDoingStateData stateData
                        = (CandidatePinyinWordChooseDoingStateData) this.state.data;
                CharInput input = stateData.getTarget();
                PinyinCharsTree charsTree = this.pinyinDict.getPinyinCharsTree();

                return () -> keyTable.createInputCandidateKeys(charsTree,
                                                               input,
                                                               stateData.getSpells(),
                                                               stateData.getPagingData(),
                                                               stateData.getPageStart(),
                                                               stateData.getFilter());
            }
            case InputCandidate_AdvanceFilter_Doing: {
                CandidatePinyinWordAdvanceFilterDoingStateData stateData
                        = (CandidatePinyinWordAdvanceFilterDoingStateData) this.state.data;

                return () -> keyTable.createInputCandidateAdvanceFilterKeys(stateData.getSpells(),
                                                                            stateData.getPagingData(),
                                                                            stateData.getPageStart(),
                                                                            stateData.getFilter());
            }
            case InputList_Committing_Option_Choose_Doing: {
                boolean hasSpell = false;
                boolean hasVariant = false;

                // TODO 若是未启用繁体优先配置，则为全部的拼音字补充其繁/简形式
                for (CharInput input : inputList.getCharInputs()) {
                    InputWord word = input.getWord();
                    if (word == null) {
                        continue;
                    }

                    if (word.hasSpell()) {
                        hasSpell = true;
                    }
                    if (word.hasVariant()) {
                        hasVariant = true;
                    }
                }

                boolean finalHasSpell = hasSpell;
                boolean finalHasVariant = hasVariant;
                return () -> keyTable.createInputListCommittingOptionKeys(inputList.getOption(),
                                                                          finalHasSpell,
                                                                          finalHasVariant);
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
    public void onMsg(UserKeyMsg msg, UserKeyMsgData data) {
        Key<?> key = data.target;
        if (try_OnUserKeyMsg(msg, data) //
            // Note：被禁用的部分按键也需要接受处理
            && (!CtrlKey.is(key, CtrlKey.Type.Filter_PinyinInputCandidate_by_Spell) //
                && !CtrlKey.is(key, CtrlKey.Type.Filter_PinyinInputCandidate_by_Radical) //
                && !CtrlKey.is(key, CtrlKey.Type.Commit_InputList_Option) //
            ) //
        ) {
            return;
        }

        switch (this.state.type) {
            case InputChars_Slip_Doing: {
                on_InputChars_Slip_Doing_UserKey_Msg(msg, key, data);
                break;
            }
            case InputChars_Flip_Doing: {
                on_InputChars_Flip_Doing_UserKey_Msg(msg, key, data);
                break;
            }
            case InputChars_XPad_Input_Doing: {
                on_InputChars_XPad_Input_Doing_UserKey_Msg(msg, key, data);
                break;
            }
            case InputCandidate_Choose_Doing: {
                if (msg == UserKeyMsg.FingerFlipping) {
                    on_InputCandidate_Choose_Doing_PageFlipping_Msg(msg, key, data);
                } else if (key instanceof InputWordKey) {
                    on_InputCandidate_Choose_Doing_InputWordKey_Msg(msg, (InputWordKey) key, data);
                } else if (key instanceof CtrlKey) {
                    on_InputCandidate_Choose_Doing_CtrlKey_Msg(msg, (CtrlKey) key, data);
                }
                break;
            }
            case InputCandidate_AdvanceFilter_Doing: {
                if (msg == UserKeyMsg.FingerFlipping) {
                    on_InputCandidate_AdvanceFilter_Doing_PageFlipping_Msg(msg, key, data);
                } else if (key instanceof CtrlKey) {
                    on_InputCandidate_AdvanceFilter_Doing_CtrlKey_Msg(msg, (CtrlKey) key, data);
                }
                break;
            }
            case InputList_Committing_Option_Choose_Doing: {
                if (key instanceof CtrlKey) {
                    on_InputList_Committing_Option_Choose_Doing_CtrlKey_Msg(msg, (CtrlKey) key, data);
                }
                break;
            }
            default: {
                if (key instanceof CharKey && !key.isDisabled()) {
                    onCharKeyMsg(msg, (CharKey) key, data);
                } else if (key instanceof CtrlKey) {
                    onCtrlKeyMsg(msg, (CtrlKey) key, data);
                }
            }
        }
    }

    private void onCharKeyMsg(UserKeyMsg msg, CharKey key, UserKeyMsgData data) {
        InputList inputList = getInputList();

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

                    start_InputChars_Slipping(inputList, pending, key);
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

    private void onCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case LongPress_Key_Start: {
                if (CtrlKey.is(key, CtrlKey.Type.Commit_InputList)) {
                    play_DoubleTick_InputAudio(key);

                    start_InputList_Committing_Option_Choosing(key);
                }
                break;
            }
            case SingleTap_Key: {
                InputList inputList = getInputList();

                if (CtrlKey.is(key, CtrlKey.Type.Pinyin_End)) {
                    play_SingleTick_InputAudio(key);
                    show_InputChars_Input_Popup(key);

                    CharInput pending = inputList.getPending();
                    end_InputChars_Inputting(inputList, pending, key);
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
            end_InputChars_Inputting(inputList, inputList.getPending(), key);
        }

        super.confirm_InputList_Input_Enter_or_Space(inputList, key);
    }

    @Override
    protected void before_Commit_InputList(InputList inputList) {
        doWithUserInputData(inputList, this.pinyinDict::saveUserInputData);
    }

    @Override
    protected void after_Revoke_Committed_InputList(InputList inputList) {
        doWithUserInputData(inputList, this.pinyinDict::revokeSavedUserInputData);
    }

    private void on_InputChars_Slip_Doing_UserKey_Msg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        InputList inputList = getInputList();

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

                    do_InputChars_Slipping(inputList, pending, key);
                }
                break;
            }
            case FingerMoving_End: {
                CharInput pending = inputList.getPending();

                end_InputChars_Slipping(inputList, pending, key);
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

    private void on_InputChars_Flip_Doing_UserKey_Msg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        InputList inputList = getInputList();

        if (msg == UserKeyMsg.SingleTap_Key) {
            play_SingleTick_InputAudio(key);
            show_InputChars_Input_Popup(key);

            CharInput pending = inputList.getPending();
            end_InputChars_Flipping(inputList, pending, key);
        }
    }

    private void on_InputChars_XPad_Input_Doing_UserKey_Msg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        if (key instanceof CtrlKey && !CtrlKey.isNoOp(key)) {
            onCtrlKeyMsg(msg, (CtrlKey) key, data);
            return;
        }

        InputList inputList = getInputList();
        // 添加拼音后继字母
        CharInput pending = inputList.getPending();

        if (msg == UserKeyMsg.Press_Key_End) {
            end_InputChars_Inputting(inputList, pending, key);
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

    private void on_InputCandidate_Choose_Doing_InputWordKey_Msg(
            UserKeyMsg msg, InputWordKey key, UserKeyMsgData data
    ) {
        if (key.isDisabled()) {
            return;
        }

        InputList inputList = getInputList();

        // 确认候选字
        if (msg == UserKeyMsg.SingleTap_Key) {
            play_SingleTick_InputAudio(key);
            show_InputChars_Input_Popup(key);

            InputWord word = key.getWord();

            // 候选字列表中的表情作为新增插入，不对当前候选字做替换
            if (word instanceof EmojiWord) {
                inputList.confirmPendingAndSelectNext();

                // Note：补充输入按键，以避免待输入为空
                InputWordKey wordKey = InputWordKey.create(word);
                inputList.getPending().appendKey(wordKey);
            }

            inputList.getPending().setWord(word);

            confirm_Selected_InputCandidate(inputList);
        }
    }

    private void on_InputCandidate_Choose_Doing_CtrlKey_Msg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        if (msg != UserKeyMsg.SingleTap_Key) {
            return;
        }

        InputList inputList = getInputList();
        CharInput pending = inputList.getPending();
        switch (key.getType()) {
            case DropInput: {
                play_SingleTick_InputAudio(key);

                delete_InputList_Selected(inputList, key);
                change_State_to_Init();
                break;
            }
            case ConfirmInput: {
                play_SingleTick_InputAudio(key);
                confirm_Selected_InputCandidate(inputList);
                break;
            }
            case Toggle_PinyinInput_spell: {
                play_SingleTick_InputAudio(key);

                CtrlKey.PinyinSpellToggleOption option = (CtrlKey.PinyinSpellToggleOption) key.getOption();
                switch (option.value()) {
                    case ng:
                        pending.toggle_Pinyin_NG_Ending();
                        break;
                    case nl:
                        pending.toggle_Pinyin_NL_Starting();
                        break;
                    case zcs_h:
                        pending.toggle_Pinyin_SCZ_Starting();
                        break;
                }

                start_InputCandidate_Choosing(inputList, pending, true);
                break;
            }
            case Filter_PinyinInputCandidate_by_Spell: {
                play_SingleTick_InputAudio(key);
                show_InputChars_Input_Popup(key);

                CandidatePinyinWordChooseDoingStateData stateData
                        = (CandidatePinyinWordChooseDoingStateData) this.state.data;

                CtrlKey.Option<?> option = key.getOption();
                PinyinWord.Spell value = (PinyinWord.Spell) option.value();
                PinyinWord.Filter filter = stateData.getFilter();

                filter.clear();
                if (!key.isDisabled()) {
                    filter.spells.add(value);
                }

                stateData.setFilter(filter);

                fire_InputCandidate_Choose_Doing(stateData.getTarget());
                break;
            }
            case Filter_PinyinInputCandidate_advance: {
                play_SingleTick_InputAudio(key);

                start_InputCandidate_Advance_Filtering(inputList, pending, key);
                break;
            }
        }
    }

    private void on_InputCandidate_Choose_Doing_PageFlipping_Msg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        UserFingerFlippingMsgData flippingData = (UserFingerFlippingMsgData) data;

        // 翻页
        update_PagingStateData_by_UserKeyMsg((PagingStateData<?>) this.state.data, flippingData);

        fire_InputCandidate_Choose_Doing(((CandidatePinyinWordChooseDoingStateData) this.state.data).getTarget());
    }

    // >>>>>>>>> 滑屏输入
    private void start_InputChars_Slipping(InputList inputList, CharInput pending, Key<?> key) {
        State state = new State(State.Type.InputChars_Slip_Doing,
                                new InputCharsSlipDoingStateData(),
                                createInitState());
        change_State_To(key, state);

        do_InputChars_Slipping(inputList, pending, key);
    }

    private void do_InputChars_Slipping(InputList inputList, CharInput pending, Key<?> currentKey) {
        InputCharsSlipDoingStateData stateData = ((InputCharsSlipDoingStateData) this.state.data);
        Key.Level currentKeyLevel = currentKey.getLevel();
        PinyinCharsTree charsTree = this.pinyinDict.getPinyinCharsTree();

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
        determine_NotConfirmed_InputWord(pending);

        fire_InputChars_Input_Doing(currentKey, InputCharsInputtingMsgData.KeyInputType.slip);
    }

    private void end_InputChars_Slipping(InputList inputList, CharInput input, Key<?> key) {
        hide_InputChars_Input_Popup();

        end_InputChars_Inputting(inputList, input, key);
    }

    private void end_InputChars_Inputting(InputList inputList, CharInput pending, Key<?> key) {
        end_InputChars_Inputting(inputList, pending, key, true);
    }

    private void end_InputChars_Inputting(InputList inputList, CharInput pending, Key<?> key, boolean reset) {
        // 非拼音输入，视为无效输入，直接丢弃
        if (!this.pinyinDict.getPinyinCharsTree().isPinyinCharsInput(pending)) {
            drop_InputList_Pending(inputList, key);
        } else {
            predict_NotConfirmed_Phrase_InputWords(inputList, pending);
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
        if (this.pinyinDict.getPinyinCharsTree().getChild(startChar) == null) {
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

    private void end_InputChars_Flipping(InputList inputList, CharInput input, Key<?> key) {
        // 先确认当前输入的候选字
        if (!CharKey.isAlphabet(key)) {
            input.setWord(null);
        } else {
            input.appendKey(key);
            determine_NotConfirmed_InputWord(input);
        }

        // 再结束输入
        end_InputChars_Inputting(inputList, input, key);
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

        PinyinCharsTree charsTree = this.pinyinDict.getPinyinCharsTree();
        boolean needToEndInputting = false;
        boolean needToContinueInputting = false;

        // 添加后继字母，
        switch (currentKeyLevel) {
            case level_0: {
                // 当前 pending 为有效拼音，则结束当前输入，并创建新输入
                if (this.pinyinDict.getPinyinCharsTree().isPinyinCharsInput(pending)) {
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
        determine_NotConfirmed_InputWord(pending);

        if (needToEndInputting) {
            end_InputChars_Inputting(inputList, pending, currentKey, !needToContinueInputting);

            if (needToContinueInputting) {
                start_InputChars_XPad_Inputting(inputList, inputList.newPending(), currentKey);
            }
        } else {
            fire_InputChars_Input_Doing(currentKey, InputCharsInputtingMsgData.KeyInputType.circle);
        }
    }
    // >>>>>>>>>>>>

    // <<<<<<<<< 对输入候选字的高级过滤
    private void start_InputCandidate_Advance_Filtering(InputList inputList, CharInput input, Key<?> key) {
        CandidatePinyinWordChooseDoingStateData prevStateData
                = (CandidatePinyinWordChooseDoingStateData) this.state.data;
        PinyinWord.Filter filter = prevStateData.getFilter();

        PinyinKeyTable keyTable = PinyinKeyTable.create(createKeyTableConfig(inputList));
        int pageSize = keyTable.getInputCandidateAdvanceFilterKeysPageSize();

        CandidatePinyinWordAdvanceFilterDoingStateData stateData = new CandidatePinyinWordAdvanceFilterDoingStateData(
                input,
                prevStateData.getCandidates(),
                pageSize);
        stateData.setFilter(filter);

        State state = new State(State.Type.InputCandidate_AdvanceFilter_Doing, stateData, this.state);
        change_State_To(key, state);

        fire_InputCandidate_Choose_Doing(input);
    }

    private void on_InputCandidate_AdvanceFilter_Doing_CtrlKey_Msg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        if (msg != UserKeyMsg.SingleTap_Key) {
            return;
        }

        CandidatePinyinWordAdvanceFilterDoingStateData stateData
                = (CandidatePinyinWordAdvanceFilterDoingStateData) this.state.data;
        switch (key.getType()) {
            case Filter_PinyinInputCandidate_by_Spell:
            case Filter_PinyinInputCandidate_by_Radical: {
                play_SingleTick_InputAudio(key);
                show_InputChars_Input_Popup(key);

                CtrlKey.Option<?> option = key.getOption();
                PinyinWord.Filter filter = stateData.getFilter();

                switch (key.getType()) {
                    case Filter_PinyinInputCandidate_by_Spell: {
                        PinyinWord.Spell value = (PinyinWord.Spell) option.value();

                        filter.clear();
                        if (!key.isDisabled()) {
                            filter.spells.add(value);
                        }
                        break;
                    }
                    case Filter_PinyinInputCandidate_by_Radical: {
                        PinyinWord.Radical value = (PinyinWord.Radical) option.value();
                        if (key.isDisabled()) {
                            filter.radicals.remove(value);
                        } else {
                            filter.radicals.add(value);
                        }
                        break;
                    }
                }

                stateData.setFilter(filter);

                fire_InputCandidate_Choose_Doing(stateData.getTarget());
                break;
            }
            case Confirm_PinyinInputCandidate_Filters: {
                play_SingleTick_InputAudio(key);

                CandidatePinyinWordChooseDoingStateData prevStateData
                        = (CandidatePinyinWordChooseDoingStateData) this.state.previous.data;
                prevStateData.setFilter(stateData.getFilter());

                exit(key);
                break;
            }
        }
    }

    private void on_InputCandidate_AdvanceFilter_Doing_PageFlipping_Msg(
            UserKeyMsg msg, Key<?> key, UserKeyMsgData data
    ) {
        UserFingerFlippingMsgData flippingData = (UserFingerFlippingMsgData) data;
        CandidatePinyinWordAdvanceFilterDoingStateData stateData
                = (CandidatePinyinWordAdvanceFilterDoingStateData) this.state.data;

        // 翻页
        update_PagingStateData_by_UserKeyMsg(stateData, flippingData);

        fire_InputCandidate_Choose_Doing(stateData.getTarget());
    }
    // >>>>>>>>>>>>

    // <<<<<<<<< 对输入候选字的操作

    /** 进入候选字选择状态，并处理候选字翻页 */
    private void start_InputCandidate_Choosing(InputList inputList, CharInput input, boolean inputPinyinChanged) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(createKeyTableConfig(inputList));
        int pageSize = keyTable.getInputCandidateKeysPageSize();
        int bestCandidatesTop = keyTable.getBestCandidatesCount();
        int bestEmojisTop = pageSize - bestCandidatesTop;

        Map<Integer, InputWord> candidateMap = getInputCandidateWords(input);
        List<InputWord> allCandidates = new ArrayList<>(candidateMap.values());

        List<Integer> topBestCandidateIds = this.pinyinDict.getTopBestCandidatePinyinWordIds(input, bestCandidatesTop);
        List<InputWord> topBestCandidates = topBestCandidateIds.stream()
                                                               .map(candidateMap::get)
                                                               .collect(Collectors.toList());

        // 拼音修正后，需更新其自动确定的候选字
        if (inputPinyinChanged) {
            determine_NotConfirmed_InputWord(input);
        }

        // 当前输入确定的拼音字放在最前面
        if (input.isPinyin() //
            && !topBestCandidates.contains(input.getWord()) //
        ) {
            topBestCandidates.add(0, input.getWord());

            topBestCandidates = CollectionUtils.subList(topBestCandidates, 0, bestCandidatesTop);
        }

        // Note：以最新确定的输入候选字做为表情的关键字查询条件
        List<InputWord> topBestEmojis = getTopBestEmojis(inputList, input, bestEmojisTop);
        topBestCandidates.addAll(topBestEmojis);

        if (!topBestCandidates.isEmpty()) {
            int allCandidatesCount = allCandidates.size();

            // 最佳候选字不再重复出现在普通候选字列表
            allCandidates.removeAll(topBestCandidates);

            // 若候选字列表总共只有一页，则合并最佳候选字，并确保最佳候选字在最前面位置
            if (allCandidatesCount <= pageSize) {
                allCandidates.addAll(0, topBestCandidates);

                allCandidates = reorder_TopBest_CandidateWords_and_Emojis_In_Page(allCandidates,
                                                                                  bestCandidatesTop,
                                                                                  pageSize);
            } else {
                topBestCandidates = reorder_TopBest_CandidateWords_and_Emojis_In_Page(topBestCandidates,
                                                                                      bestCandidatesTop,
                                                                                      pageSize);
                allCandidates.addAll(0, topBestCandidates);
            }
        }

        CandidatePinyinWordChooseDoingStateData stateData = new CandidatePinyinWordChooseDoingStateData(input,
                                                                                                        allCandidates,
                                                                                                        pageSize);
        State state = new State(State.Type.InputCandidate_Choose_Doing, stateData, createInitState());
        change_State_To(null, state);

        fire_InputCandidate_Choose_Doing(input);
    }

    private void fire_InputCandidate_Choose_Doing(CharInput input) {
        KeyboardMsgData data = new InputCandidateChoosingMsgData(getKeyFactory(), input);

        fire_InputMsg(KeyboardMsg.InputCandidate_Choose_Doing, data);
    }

    private void fire_InputCandidate_Choose_Done(CharInput input) {
        KeyboardMsgData data = new InputCandidateChoosingMsgData(getKeyFactory(), input);

        fire_InputMsg(KeyboardMsg.InputCandidate_Choose_Done, data);
    }

    private void confirm_Selected_InputCandidate(InputList inputList) {
        CharInput pending = inputList.getPending();
        pending.markWordConfirmed();

        inputList.confirmPendingAndSelectNext();

        fire_InputCandidate_Choose_Done(pending);

        // 继续选择下一个拼音输入的候选字
        Input<?> selected = inputList.selectNextFirstMatched(Input::isPinyin);
        boolean hasNextPinyin = selected != null;

        if (hasNextPinyin) {
            predict_NotConfirmed_Phrase_InputWords(inputList, (CharInput) selected);
        } else {
            selected = inputList.getSelected();
        }

        start_Input_Choosing(inputList, selected);
    }

    /**
     * 预测 <code>input</code> 所在拼音短语中 未确认输入 的字
     * <p/>
     * <code>input</code> 也将根据预测的短语结果而进行调整
     */
    private void predict_NotConfirmed_Phrase_InputWords(InputList inputList, CharInput input) {
        List<CharInput> inputs = inputList.getPinyinPhraseInputWhichContains(input);
        List<List<InputWord>> bestPhrases = this.pinyinDict.findTopBestMatchedPhrase(inputs, 1);

        List<InputWord> bestPhrase = CollectionUtils.first(bestPhrases);
        if (bestPhrase == null) {
            return;
        }

        for (int i = 0; i < bestPhrase.size(); i++) {
            CharInput target = inputs.get(i);
            // 已确认的输入，则不做处理
            if (target.isWordConfirmed()) {
                continue;
            }

            InputWord word = bestPhrase.get(i);
            // Note: 非拼音位置的输入，其拼音字为 null，不覆盖其 word
            if (word != null) {
                target.setWord(word);
            }
        }
    }

    @Override
    protected void do_InputList_Phrase_Completion_Updating(InputList inputList, Input<?> input) {
    }

    /**
     * 确认 未确认输入 的候选字
     * <p/>
     * 在滑屏输入中实时调用
     */
    private void determine_NotConfirmed_InputWord(CharInput input) {
        Integer pinyinCharsId = this.pinyinDict.getPinyinCharsTree().getCharsId(input);

        InputWord word = null;
        if (pinyinCharsId != null //
            && (!input.isWordConfirmed() || !input.hasWord() //
                || !Objects.equals(input.getWord().getSpell().charsId, pinyinCharsId) //
            ) //
        ) {
            word = this.pinyinDict.getFirstBestCandidatePinyinWord(pinyinCharsId);
        }
        input.setWord(word);
    }

    /**
     * 获取输入的候选字列表
     *
     * @return 不为 <code>null</code>
     */
    private Map<Integer, InputWord> getInputCandidateWords(CharInput input) {
        String inputChars = input.getJoinedChars();
        return this.pinyinDict.getCandidatePinyinWords(inputChars);
    }

    private List<InputWord> getTopBestEmojis(InputList inputList, CharInput input, int top) {
        // TODO 在 InputList 发送 Input 选中消息时附带短语
        List<PinyinWord> phraseWords = inputList.getPinyinPhraseWordsFrom(input);

        return this.pinyinDict.findTopBestEmojisMatchedPhrase(phraseWords, top);
    }

    /**
     * 在一页内重新排序最佳候选字和表情符号列表，
     * 确保表情符号和候选字各自独占特定区域，
     * 并填充 <code>null</code> 以占满一页
     */
    private List<InputWord> reorder_TopBest_CandidateWords_and_Emojis_In_Page(
            List<InputWord> candidates, int wordCount, int pageSize
    ) {
        List<InputWord> results = new ArrayList<>(pageSize);

        int emojiCount = pageSize - wordCount;
        List<InputWord> emojis = new ArrayList<>(emojiCount);
        candidates.forEach((candidate) -> {
            if (candidate instanceof EmojiWord) {
                emojis.add(candidate);
            } else {
                results.add(candidate);
            }
        });

        CollectionUtils.fillToSize(results, null, wordCount);

        // Note：在候选字不够一页时，最佳候选字会与剩余的候选字合并，
        // 故而可能会超过最佳候选字的显示数量，因此，需更新表情符号可填充数
        emojiCount = pageSize - results.size();
        CollectionUtils.fillToSize(emojis, null, emojiCount);

        results.addAll(emojis);

        return results;
    }
    // >>>>>>>>>>

    // >>>>>>>>> 对输入列表 提交选项 的操作
    private void start_InputList_Committing_Option_Choosing(Key<?> key) {
        State state = new State(State.Type.InputList_Committing_Option_Choose_Doing, createInitState());
        change_State_To(key, state);
    }

    private void on_InputList_Committing_Option_Choose_Doing_CtrlKey_Msg(
            UserKeyMsg msg, CtrlKey key, UserKeyMsgData data
    ) {
        InputList inputList = getInputList();

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

    // <<<<<<<<< 对输入列表的操作

    /** 在输入列表中选中拼音输入的候选字 */
    @Override
    protected boolean do_Input_Choosing(InputList inputList, CharInput input) {
        // 仅当待输入为拼音时才进入候选字模式
        if (input.isPinyin()) {
            start_InputCandidate_Choosing(inputList, input, false);
            return true;
        }
        return false;
    }
    // >>>>>>>>>

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
