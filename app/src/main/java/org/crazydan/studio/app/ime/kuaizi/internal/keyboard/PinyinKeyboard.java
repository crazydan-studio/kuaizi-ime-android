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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.BestCandidateWords;
import org.crazydan.studio.app.ime.kuaizi.internal.data.UserInputData;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.EmojiInputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.PinyinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.InputCandidateChooseDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.InputCharsFlipDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.InputCharsSlipDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.PagingStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCandidateChoosingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserFingerFlippingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserSingleTapMsgData;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;

/**
 * {@link Keyboard.Type#Pinyin 汉语拼音键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class PinyinKeyboard extends BaseKeyboard {

    @Override
    protected KeyFactory doGetKeyFactory() {
        InputList inputList = getInputList();
        PinyinKeyTable keyTable = PinyinKeyTable.create(createKeyTableConfigure());

        switch (this.state.type) {
            case InputChars_Slip_Doing: {
                InputCharsSlipDoingStateData stateData = ((InputCharsSlipDoingStateData) this.state.data);

                String level0Char = stateData.getLevel0Key() != null ? stateData.getLevel0Key().getText() : null;
                if (level0Char == null) {
                    return null;
                }

                String level1Char = stateData.getLevel1Key() != null ? stateData.getLevel1Key().getText() : null;
                String level2Char = stateData.getLevel2Key() != null ? stateData.getLevel2Key().getText() : null;

                return (NoAnimationKeyFactory) () -> keyTable.createNextCharKeys(level0Char,
                                                                                 level1Char,
                                                                                 level2Char,
                                                                                 stateData.getLevel1NextChars(),
                                                                                 stateData.getLevel2NextChars());
            }
            case InputChars_Flip_Doing: {
                InputCharsFlipDoingStateData stateData = ((InputCharsFlipDoingStateData) this.state.data);

                return (NoAnimationKeyFactory) () -> keyTable.createFullCharKeys(stateData.startChar,
                                                                                 stateData.restChars);
            }
            case InputChars_XPad_Input_Doing: {
                InputCharsSlipDoingStateData stateData = ((InputCharsSlipDoingStateData) this.state.data);

                String level0Char = stateData.getLevel0Key() != null ? stateData.getLevel0Key().getText() : null;
                if (level0Char == null) {
                    return null;
                }

                String level1Char = stateData.getLevel1Key() != null ? stateData.getLevel1Key().getText() : null;
                String level2Char = stateData.getLevel2Key() != null ? stateData.getLevel2Key().getText() : null;

                return () -> keyTable.createXPadNextCharKeys(level0Char,
                                                             level1Char,
                                                             level2Char,
                                                             stateData.getLevel1NextChars(),
                                                             stateData.getLevel2NextChars());
            }
            case InputCandidate_Choose_Doing: {
                InputCandidateChooseDoingStateData stateData = (InputCandidateChooseDoingStateData) this.state.data;
                CharInput input = stateData.getTarget();

                return () -> keyTable.createInputCandidateKeys(input,
                                                               stateData.getPagingData(),
                                                               stateData.getStrokes(),
                                                               stateData.getPageStart());
            }
            case InputList_Committing_Option_Choose_Doing: {
                boolean hasNotation = false;
                boolean hasVariant = false;

                for (CharInput input : inputList.getCharInputs()) {
                    InputWord word = input.getWord();
                    if (word == null) {
                        continue;
                    }

                    if (word.hasNotation()) {
                        hasNotation = true;
                    }
                    if (word.hasVariant()) {
                        hasVariant = true;
                    }
                }

                boolean finalHasNotation = hasNotation;
                boolean finalHasVariant = hasVariant;
                return () -> keyTable.createInputListCommittingOptionKeys(inputList.getOption(),
                                                                          finalHasNotation,
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

                            return (NoAnimationKeyFactory) keyTable::createKeys;
                    }
                }
                return keyTable::createKeys;
            }
        }
    }

    @Override
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        switch (this.state.type) {
            case InputCandidate_Choose_Doing:
                if (msg == UserInputMsg.Input_Choose_Doing) {
                    start_Input_Choosing(data.target);
                    return;
                }
            case InputChars_Flip_Doing: {
                if (msg == UserInputMsg.Inputs_Clean_Done) {
                    fire_InputList_Clean_Done();
                    change_State_to_Init();
                    return;
                }
                break;
            }
        }

        super.onUserInputMsg(msg, data);
    }

    @Override
    public void onUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data) {
        if (try_OnUserKeyMsg(msg, data)) {
            return;
        }

        Key<?> key = data.target;
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
            if (msg == UserKeyMsg.KeySingleTap) {
                play_SingleTick_InputAudio(key);

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
            case FingerMovingStart: {
                // 开始滑屏输入
                if (CharKey.isAlphabet(key)) {
                    play_DoubleTick_InputAudio(key);

                    confirm_or_New_InputList_Pending(inputList);

                    CharInput pending = inputList.getPending();

                    start_InputChars_Slipping(inputList, pending, key);
                }
                break;
            }
            case KeySingleTap: {
                // 单字符输入
                play_SingleTick_InputAudio(key);
                start_Single_Key_Inputting(inputList, key, (UserSingleTapMsgData) data, false);
                break;
            }
        }
    }

    private void onCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeyLongPressStart: {
                if (CtrlKey.is(key, CtrlKey.Type.Commit_InputList)) {
                    play_DoubleTick_InputAudio(key);
                    start_InputList_Committing_Option_Choosing(key);
                }
                break;
            }
            case KeySingleTap: {
                InputList inputList = getInputList();

                if (CtrlKey.is(key, CtrlKey.Type.Pinyin_End)) {
                    play_SingleTick_InputAudio(key);

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

                // Note: 拼音不存在重复字母相邻的情况
                Key<?> lastKey = pending.getLastKey();
                if (key instanceof CharKey //
                    && !key.isDisabled() //
                    && !key.isSameWith(lastKey)) {
                    play_DoubleTick_InputAudio(key);

                    do_InputChars_Slipping(inputList, pending, key);
                }
                break;
            }
            case FingerMovingEnd: {
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

                    start_InputChars_Flipping(inputList, firstKey);
                }
                break;
            }
        }
    }

    private void on_InputChars_Flip_Doing_UserKey_Msg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        InputList inputList = getInputList();

        if (msg == UserKeyMsg.KeySingleTap) {
            play_SingleTick_InputAudio(key);

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

        if (msg == UserKeyMsg.KeyPressEnd) {
            end_InputChars_Inputting(inputList, pending, key);
        } else if (msg != UserKeyMsg.KeySingleTap) {
            return;
        }

        play_SingleTick_InputAudio(key);

        // Note: 拼音不存在重复字母相邻的情况
        Key<?> lastKey = pending.getLastKey();
        if (key instanceof CharKey //
            && !key.isDisabled() //
            && !key.isSameWith(lastKey)) {
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

        if (msg == UserKeyMsg.KeySingleTap) {// 确认候选字
            play_SingleTick_InputAudio(key);

            InputWord word = key.getWord();

            // 候选字列表中的表情作为新增插入，不对当前候选字做替换
            if (word instanceof EmojiInputWord) {
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
        if (msg != UserKeyMsg.KeySingleTap) {
            return;
        }

        InputList inputList = getInputList();
        CharInput pending = inputList.getPending();
        // 丢弃或变更拼音
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
            case Filter_PinyinInputCandidate_stroke: {
                do_InputCandidate_Filtering_ByStroke(key, 1);
                break;
            }
        }
    }

    private void on_InputCandidate_Choose_Doing_PageFlipping_Msg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        UserFingerFlippingMsgData flippingData = (UserFingerFlippingMsgData) data;

        // 减去过滤的笔画数
        if (CtrlKey.is(key, CtrlKey.Type.Filter_PinyinInputCandidate_stroke)) {
            do_InputCandidate_Filtering_ByStroke((CtrlKey) key, -1);
            return;
        }

        // 翻页
        update_PagingStateData_by_UserKeyMsg((PagingStateData<?>) this.state.data, flippingData);

        fire_InputCandidate_Choose_Doing(((InputCandidateChooseDoingStateData) this.state.data).getTarget());
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

        // 添加后继字母，
        switch (currentKeyLevel) {
            case level_0: {
                pending.appendKey(currentKey);

                Collection<String> level1NextChars = this.pinyinDict.findPinyinNextChar(Key.Level.level_1,
                                                                                        currentKey.getText());

                stateData.setLevel0Key(currentKey);

                stateData.setLevel1Key(null);
                stateData.setLevel1NextChars(level1NextChars);

                stateData.setLevel2Key(null);
                stateData.setLevel2NextChars(new ArrayList<>());
                break;
            }
            case level_1: {
                pending.replaceKeyAfterLevel(Key.Level.level_0, currentKey);

                String startChar = stateData.getLevel0Key().getText();
                startChar += currentKey.getText();

                Collection<String> nextChars = this.pinyinDict.findPinyinNextChar(Key.Level.level_2, startChar);
                // 第二级后继字母先按字符顺序排列，再按字符长度升序排列
                List<String> level2NextChars = nextChars.stream()
                                                        .sorted(String::compareTo)
                                                        .sorted(Comparator.comparing(String::length))
                                                        .collect(Collectors.toList());

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
        determine_NotConfirmed_InputWord(inputList, pending);

        fire_InputChars_Input_Doing(currentKey, InputCharsInputtingMsgData.KeyInputType.slip);
    }

    private void end_InputChars_Slipping(InputList inputList, CharInput input, Key<?> key) {
        end_InputChars_Inputting(inputList, input, key);
    }

    private void end_InputChars_Inputting(InputList inputList, CharInput pending, Key<?> key) {
        // 无候选字的输入，视为无效输入，直接丢弃
        if (!pending.hasWord()) {
            drop_InputList_Pending(inputList, key);
        } else {
            determine_NotConfirmed_InputWords_Before(inputList, pending);
            confirm_InputList_Pending(inputList, key);
        }

        change_State_to_Init();
    }
    // >>>>>>>>>>>>

    // >>>>>>>>> 翻动输入
    private void start_InputChars_Flipping(InputList inputList, Key<?> key) {
        String startChar = key.getText();
        Map<String, List<String>> restChars = getRestChars(startChar);
        if (restChars.isEmpty()) {
            return;
        }

        InputCharsFlipDoingStateData stateData = new InputCharsFlipDoingStateData(startChar, restChars);
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
            determine_NotConfirmed_InputWord(inputList, input);
        }

        // 再结束输入
        end_InputChars_Inputting(inputList, input, key);
    }

    private Map<String, List<String>> getRestChars(String startChar) {
        Collection<String> filteredChars = this.pinyinDict.findPinyinCharsStartsWith(startChar);

        Map<String, List<String>> restChars = new HashMap<>();

        filteredChars.forEach((chars) -> {
            String sub = "";
            String prefix;

            switch (startChar) {
                case "a":
                case "e":
                case "o":
                case "i":
                case "u": {
                    sub = chars.substring(startChar.length());
                    prefix = startChar;
                    break;
                }
                default:
                    if (!chars.equals(startChar)) {
                        sub = chars.substring(startChar.length());
                        prefix = sub.substring(0, 1);
                    } else {
                        prefix = startChar;
                    }
            }

            restChars.computeIfAbsent(prefix, (k) -> new ArrayList<>()).add(sub);
        });

        restChars.forEach((k, list) -> list.sort(String::compareTo));

        return restChars;
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

        // 添加后继字母，
        switch (currentKeyLevel) {
            case level_0: {
                pending.appendKey(currentKey);

                Collection<String> level1NextChars = this.pinyinDict.findPinyinNextChar(Key.Level.level_1,
                                                                                        currentKey.getText());

                stateData.setLevel0Key(currentKey);

                stateData.setLevel1Key(null);
                stateData.setLevel1NextChars(level1NextChars);

                stateData.setLevel2Key(null);
                stateData.setLevel2NextChars(new ArrayList<>());
                break;
            }
            case level_1: {
                pending.replaceKeyAfterLevel(Key.Level.level_0, currentKey);

                String startChar = stateData.getLevel0Key().getText();
                startChar += currentKey.getText();

                Collection<String> nextChars = this.pinyinDict.findPinyinNextChar(Key.Level.level_2, startChar);
                // 第二级后继字母先按字符顺序排列，再按字符长度升序排列
                List<String> level2NextChars = nextChars.stream()
                                                        .sorted(String::compareTo)
                                                        .sorted(Comparator.comparing(String::length))
                                                        .collect(Collectors.toList());

                stateData.setLevel1Key(currentKey);

                stateData.setLevel2Key(null);
                stateData.setLevel2NextChars(level2NextChars);
                break;
            }
            case level_2: {
                // Note：第二级后继字母已包含第一级后继字母，故而，直接替换第 0 级之后的按键
                pending.replaceKeyAfterLevel(Key.Level.level_0, currentKey);
                break;
            }
        }

        // 并确定候选字
        determine_NotConfirmed_InputWord(inputList, pending);

        if (currentKeyLevel == Key.Level.level_2 //
            || (currentKeyLevel == Key.Level.level_1 //
                && stateData.getLevel2NextChars().isEmpty())) {
            end_InputChars_Inputting(inputList, pending, currentKey);
        } else {
            fire_InputChars_Input_Doing(currentKey, InputCharsInputtingMsgData.KeyInputType.circle);
        }
    }
    // >>>>>>>>>>>>

    // <<<<<<<<< 对输入候选字的操作

    /** 进入候选字选择状态，并处理候选字翻页 */
    private void start_InputCandidate_Choosing(InputList inputList, CharInput input, boolean inputPinyinChanged) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(createKeyTableConfigure(inputList));
        int pageSize = keyTable.getInputCandidateKeysPageSize();
        int bestCandidatesTop = 17;
        int bestEmojisTop = pageSize - bestCandidatesTop;

        Map<String, InputWord> candidateMap = getInputCandidateWords(inputList, input);
        List<InputWord> allCandidates = new ArrayList<>(candidateMap.values());
        List<InputWord> topBestCandidates = getTopBestInputCandidateWords(inputList, input, bestCandidatesTop);

        // 拼音修正后，需更新其自动确定的候选字
        if (inputPinyinChanged) {
            List<InputWord> finalTopBestCandidates = topBestCandidates;
            determine_NotConfirmed_InputWord(inputList, input, () -> finalTopBestCandidates);
        }

        // Note：以最新确定的输入候选字做为表情的关键字查询条件
        List<InputWord> topBestEmojis = getTopBestEmojis(inputList, input, bestEmojisTop);
        topBestCandidates.addAll(topBestEmojis);

        if (!topBestCandidates.isEmpty()) {
            // 若只有一页，则合并最佳候选字，并确保最佳候选字在最前面位置
            if (allCandidates.size() <= pageSize) {
                allCandidates.removeAll(topBestCandidates);
                allCandidates.addAll(0, topBestCandidates);

                allCandidates = reorder_TopBest_CandidateWords_and_Emojis(allCandidates, bestCandidatesTop, pageSize);
            } else {
                topBestCandidates = reorder_TopBest_CandidateWords_and_Emojis(topBestCandidates,
                                                                              bestCandidatesTop,
                                                                              pageSize);

                allCandidates.addAll(0, topBestCandidates);
            }
        }

        InputCandidateChooseDoingStateData stateData = new InputCandidateChooseDoingStateData(input,
                                                                                              allCandidates,
                                                                                              pageSize);
        State state = new State(State.Type.InputCandidate_Choose_Doing, stateData, createInitState());
        change_State_To(null, state);

        fire_InputCandidate_Choose_Doing(input);
    }

    private void do_InputCandidate_Filtering_ByStroke(CtrlKey key, int strokeIncrement) {
        InputCandidateChooseDoingStateData stateData = (InputCandidateChooseDoingStateData) this.state.data;

        CtrlKey.TextOption option = (CtrlKey.TextOption) key.getOption();
        if (stateData.addStroke(option.value(), strokeIncrement)) {
            play_SingleTick_InputAudio(key);
        }

        fire_InputCandidate_Choose_Doing(stateData.getTarget());
    }

    private void fire_InputCandidate_Choose_Doing(CharInput input) {
        InputMsgData data = new InputCandidateChoosingMsgData(getKeyFactory(), input);

        fireInputMsg(InputMsg.InputCandidate_Choose_Doing, data);
    }

    private void fire_InputCandidate_Choose_Done(CharInput input) {
        InputMsgData data = new InputCandidateChoosingMsgData(getKeyFactory(), input);

        fireInputMsg(InputMsg.InputCandidate_Choose_Done, data);
    }

    private void confirm_Selected_InputCandidate(InputList inputList) {
        CharInput pending = inputList.getPending();

        pending.getWord().setConfirmed(true);
        inputList.confirmPendingAndSelectNext();

        fire_InputCandidate_Choose_Done(pending);

        // 继续选择下一个拼音输入的候选字
        Input<?> selected = inputList.selectNextFirstMatched(Input::isPinyin);
        boolean hasNextPinyin = selected != null;

        // Note：前序已确认，则继续自动确认下一个拼音输入
        if (hasNextPinyin) {
            determine_NotConfirmed_InputWords_Before(inputList, (CharInput) selected);
        } else {
            selected = inputList.getSelected();
        }

        start_Input_Choosing(inputList, selected);
    }

    /**
     * 确定 <code>input</code> 的 前序 未确认输入 的最佳候选字
     * <p/>
     * <code>input</code> 也将根据前序确定的候选字而进行调整
     * <p/>
     * 在滑屏输入完成后，对未确认前序的候选字按匹配到的最佳短语进行调整，
     * 或者，在前序候选字确认后，自动对选中的下一个输入的候选字进行调整
     */
    private void determine_NotConfirmed_InputWords_Before(InputList inputList, CharInput input) {
        List<CharInput> inputs = CollectionUtils.last(inputList.getPinyinPhraseInputsBefore(input));
        if (inputs == null || inputs.isEmpty()) {
            return;
        }

        // Note: top 取 0 以避免查询单字
        BestCandidateWords best = getTopBestCandidateWords(inputList, input, 0);
        if (best.phrases.isEmpty()) {
            return;
        }

        inputs.add(input);
        // Note: phrase 为倒序匹配，故，前序 输入集 需倒置
        Collections.reverse(inputs);

        String[] bestMatchedPhrase = new String[0];
        for (String[] phrase : best.phrases) {
            int matchedSize = 0;
            for (int i = 0; i < phrase.length && i < inputs.size(); i++) {
                String phraseWordId = phrase[i];
                InputWord inputWord = inputs.get(i).getWord();

                // 对于已确认的字，需要做确切匹配
                if (inputWord.isConfirmed()) {
                    matchedSize += inputWord.getUid().equals(phraseWordId) ? 1 : 0;
                }
                // 对于根据短语自动确定的字，需要存在更长的匹配短语
                else if (inputWord.isFromPhrase()) {
                    matchedSize += i + 1 < phrase.length ? 1 : 0;
                } else {
                    matchedSize += 1;
                }
            }

            if (matchedSize > 1 && matchedSize > bestMatchedPhrase.length) {
                bestMatchedPhrase = phrase;
            }
        }

        for (int i = 0; i < bestMatchedPhrase.length && i < inputs.size(); i++) {
            CharInput target = inputs.get(i);
            if (target.getWord().isConfirmed()) {
                continue;
            }

            String wordId = bestMatchedPhrase[i];
            Map<String, InputWord> candidateMap = getInputCandidateWords(inputList, target);
            InputWord word = candidateMap.get(wordId);

            // Note：可能存在用户数据与内置字典数据不一致的情况
            if (word != null) {
                target.setWord(word);
                // Note：word 为缓存数据，不可直接修改其状态
                target.getWord().setSource(InputWord.Source.phrase);
            }
        }
    }

    @Override
    protected void do_InputList_Phrase_Completion_Updating(InputList inputList, Input<?> input) {
        Input<?> pending = inputList.getPendingOn(input);
        // 仅查找占位输入（且其待输入不能为拉丁文）之前的补全短语
        if (!input.isGap() || (pending != null && pending.isLatin())) {
            return;
        }

        // Note: InputList#getPinyinPhraseWordsBefore 获取的不是紧挨者 input 的短语，
        // 二者之间可能存在其他非拼音输入
        Input<?> before = inputList.getInputBefore(input);
        if (before == null || !before.isPinyin()) {
            return;
        }

        List<InputWord> words = CollectionUtils.last(inputList.getPinyinPhraseWordsBefore(input));
        if (words == null || words.isEmpty()) {
            return;
        }

        if (input.isPinyin()) {
            words.add(input.getWord());
        }

        List<List<InputWord>> topBestPhrases = this.pinyinDict.findTopBestMatchedPinyinPhrase(words, 5);
        List<CompletionInput> phraseCompletions = topBestPhrases.stream()
                                                                .map((phrase) -> createPhraseCompletion(words.size(),
                                                                                                        phrase))
                                                                .collect(Collectors.toList());
        inputList.setPhraseCompletions(phraseCompletions);
    }

    private CompletionInput createPhraseCompletion(int startIndex, List<InputWord> phrase) {
        CompletionInput completion = new CompletionInput(startIndex);

        for (int i = 0; i < phrase.size(); i++) {
            PinyinInputWord word = (PinyinInputWord) phrase.get(i);
            String pinyin = this.pinyinDict.getPinyinCharsById(word.getCharsId());

            CharInput charInput = CharInput.from(CharKey.from(pinyin));
            charInput.setWord(word);

            completion.add(charInput);
        }
        return completion;
    }

    /**
     * 确认 未确认输入 的候选字
     * <p/>
     * 在滑屏输入中实时调用
     */
    private void determine_NotConfirmed_InputWord(InputList inputList, CharInput input) {
        determine_NotConfirmed_InputWord(inputList, input, () -> getTopBestInputCandidateWords(inputList, input, 1));
    }

    /** 在滑屏输入中，以及拼音纠正切换中被调用 */
    private void determine_NotConfirmed_InputWord(
            InputList inputList, CharInput input, Supplier<List<InputWord>> topBestCandidatesGetter
    ) {
        Map<String, InputWord> candidateMap = getInputCandidateWords(inputList, input);

        if (!candidateMap.containsValue(input.getWord()) || !input.getWord().isConfirmed()) {
            List<InputWord> topBestCandidates = topBestCandidatesGetter.get();

            InputWord bestCandidate = CollectionUtils.first(topBestCandidates);
            // Note：无最佳候选字时，选择候选字列表中的第一个作为最佳候选字
            if (bestCandidate == null) {
                bestCandidate = CollectionUtils.first(candidateMap.values());
            }

            input.setWord(bestCandidate);
        }
    }

    private List<InputWord> getTopBestInputCandidateWords(InputList inputList, CharInput input, int top) {
        BestCandidateWords best = getTopBestCandidateWords(inputList, input, top);
        if (best.words.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, InputWord> candidateMap = getInputCandidateWords(inputList, input);

        return best.words.stream().map(candidateMap::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 获取输入的候选字列表
     * <p/>
     * 仅在首次获取时才查询拼音字典，后续的相同拼音都直接从缓存中取
     *
     * @return 不为 <code>null</code>
     */
    private Map<String, InputWord> getInputCandidateWords(InputList inputList, CharInput input) {
        Map<String, InputWord> words = inputList.getCachedCandidateWords(input);

        if (words == null) {
            List<InputWord> candidates = this.pinyinDict.getPinyinCandidateWords(input);
            inputList.cacheCandidateWords(input, candidates);

            words = inputList.getCachedCandidateWords(input);
        }
        return words;
    }

    private BestCandidateWords getTopBestCandidateWords(InputList inputList, CharInput input, int top) {
        // 根据当前位置之前的输入确定当前位置的最佳候选字
        List<InputWord> prevPhrase = CollectionUtils.last(inputList.getPinyinPhraseWordsBefore(input));

        return this.pinyinDict.findTopBestPinyinCandidateWords(input,
                                                               top,
                                                               prevPhrase,
                                                               getConfig().isUserInputDataDisabled());
    }

    private List<InputWord> getTopBestEmojis(InputList inputList, CharInput input, int top) {
        List<InputWord> prevPhrase = CollectionUtils.last(inputList.getPinyinPhraseWordsBefore(input));

        return this.pinyinDict.findTopBestEmojisMatchedPhrase(input, top, prevPhrase);
    }

    /**
     * 重新排序最佳候选字和表情符号列表，
     * 确保表情符号和候选字各自独占特定区域，
     * 并填充 <code>null</code> 以占满一页
     */
    private List<InputWord> reorder_TopBest_CandidateWords_and_Emojis(
            List<InputWord> candidates, int wordCount, int pageSize
    ) {
        List<InputWord> results = new ArrayList<>(pageSize);

        int emojiCount = pageSize - wordCount;
        List<InputWord> emojis = new ArrayList<>(emojiCount);
        candidates.forEach((candidate) -> {
            if (candidate instanceof EmojiInputWord) {
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
            case KeySingleTap: {
                if (CtrlKey.is(key, CtrlKey.Type.Commit_InputList_Option)) {
                    play_SingleTick_InputAudio(key);

                    CtrlKey.InputListCommitOption.Option option
                            = ((CtrlKey.InputListCommitOption) key.getOption()).value();

                    Input.Option oldInputOption = inputList.getOption();
                    if (oldInputOption == null) {
                        oldInputOption = new Input.Option(null, false);
                    }

                    Input.Option newInputOption = null;
                    switch (option) {
                        case only_pinyin: {
                            InputWord.NotationType notationType = oldInputOption.wordNotationType;
                            if (notationType == InputWord.NotationType.replacing) {
                                notationType = null;
                            } else {
                                notationType = InputWord.NotationType.replacing;
                            }

                            newInputOption = new Input.Option(notationType, oldInputOption.wordVariantUsed);
                            break;
                        }
                        case with_pinyin: {
                            InputWord.NotationType notationType = oldInputOption.wordNotationType;
                            if (notationType == InputWord.NotationType.following) {
                                notationType = null;
                            } else {
                                notationType = InputWord.NotationType.following;
                            }

                            newInputOption = new Input.Option(notationType, oldInputOption.wordVariantUsed);
                            break;
                        }
                        case switch_simple_trad: {
                            newInputOption = new Input.Option(oldInputOption.wordNotationType,
                                                              !oldInputOption.wordVariantUsed);
                            break;
                        }
                    }
                    inputList.setOption(newInputOption);

                    fire_InputChars_Input_Done(key);
                }
                break;
            }
            case KeyLongPressStart: {
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

        List<List<InputWord>> phrases = inputList.getPinyinPhraseWords();
        List<InputWord> emojis = inputList.getEmojis();
        Set<String> latins = inputList.getLatins();

        UserInputData data = new UserInputData(phrases, emojis, latins);

        handler.handle(data);
    }

    private interface UserInputDataHandler {
        void handle(UserInputData data);
    }
    // >>>>>>>>>>
}
