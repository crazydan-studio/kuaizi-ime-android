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
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDictDB;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.ChoosingInputCandidateStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserFingerMovingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserFingerSlippingMsgData;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;

/**
 * {@link Keyboard.Type#Pinyin 汉语拼音键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class PinyinKeyboard extends BaseKeyboard {
    private final PinyinDictDB pinyinDict = PinyinDictDB.getInstance();

    @Override
    public KeyFactory getKeyFactory() {
        return () -> KeyTable.createPinyinKeys(createKeyTableConfigure());
    }

    @Override
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        switch (this.state.type) {
            case Input_Waiting:
            case ChoosingInputCandidate:
                switch (msg) {
                    case ChoosingInput:
                        onChoosingInputMsg(msg, data.target, data);
                        break;
                }
                break;
        }
    }

    @Override
    public void onUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data) {
        if (try_OnUserKeyMsg(msg, data)) {
            return;
        }

        Key<?> key = data.target;
        switch (this.state.type) {
            case SlippingInput:
                onSlippingInputCharKeyMsg(msg, (CharKey) key, data);
                break;
            case ChoosingInputCandidate:
                if (key instanceof InputWordKey) {
                    onInputCandidatesKeyMsg(msg, (InputWordKey) key, data);
                } else if (key instanceof CtrlKey) {
                    onInputCandidatesCtrlKeyMsg(msg, (CtrlKey) key, data);
                } else {
                    onInputCandidatesPageSlippingMsg(msg, key, data);
                }
                break;
            default:
                if (key instanceof CharKey) {
                    onCharKeyMsg(msg, (CharKey) key, data);
                } else if (key instanceof CtrlKey) {
                    onCtrlKeyMsg(msg, (CtrlKey) key, data);
                }
        }
    }

    @Override
    protected void before_Commit_InputList() {
        // 本地更新用户选字频率
        List<List<InputWord>> phrases = getInputList().getPinyinPhrases(false);
        this.pinyinDict.saveUsedPhrases(phrases);
    }

    private void onCharKeyMsg(UserKeyMsg msg, CharKey key, UserKeyMsgData data) {
        switch (msg) {
            case FingerMovingStart: {
                // 开始滑行输入
                if (key.getType() == CharKey.Type.Alphabet) {
                    this.state = new State(State.Type.SlippingInput);

                    CharInput input = getInputList().newPending();
                    input.appendKey(key);

                    play_InputtingDoubleTick_Audio(key);

                    onContinuousInput(input, data.target, true);
                }
                break;
            }
            case KeySingleTap: {
                // 单字符输入
                play_InputtingSingleTick_Audio(key);

                doSingleKeyInputting(key);
                break;
            }
            case KeyDoubleTap: {
                // 双击字符，则替换前一个相同的输入字符：
                // 因为双击会先触发单击，而单击时已添加了一次该双击的字符
                play_InputtingSingleTick_Audio(key);

                doReplacementKeyInputting(key);
                break;
            }
        }
    }

    private void onSlippingInputCharKeyMsg(UserKeyMsg msg, CharKey key, UserKeyMsgData data) {
        switch (msg) {
            case FingerMoving: {
                // 添加拼音后继字母
                CharInput input = getInputList().getPending();
                Key<?> closedKey = ((UserFingerMovingMsgData) data).closed;

                // Note: 拼音不存在重复字母相邻的情况
                if (key != null && !key.isSameWith(input.getLatestKey())) {
                    input.appendKey(key);

                    play_InputtingDoubleTick_Audio(key);
                }

                if (key != null || closedKey != null) {
                    // Note: 第二个参数传事件触发的实际 key，而不是当前的 charkey
                    onContinuousInput(input, data.target, true);
                }
                break;
            }
            case FingerMovingEnd: {
                CharInput input = getInputList().getPending();

                // 无候选字的输入，视为无效输入，直接丢弃
                if (!input.hasWord()) {
                    getInputList().dropPending();
                }
                confirm_InputChars();
                break;
            }
        }
    }

    private void onCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeyDoubleTap: // 双击继续触发第二次单击操作
            case KeySingleTap: {
                play_InputtingSingleTick_Audio(key);

                // Note: 切换至拉丁文输入法的逻辑在基类中处理
                switch (key.getType()) {
                    case SwitchToSymbolKeyboard: {
                        switch_Keyboard(Type.Symbol);
                        break;
                    }
                    case SwitchToMathKeyboard: {
                        switch_Keyboard(Type.Math);
                        break;
                    }
                }
                break;
            }
        }
    }

    private void onInputCandidatesKeyMsg(UserKeyMsg msg, InputWordKey key, UserKeyMsgData data) {
        switch (msg) {
            case FingerSlipping: {
                onInputCandidatesPageSlippingMsg(msg, key, data);
                break;
            }
            case KeySingleTap: {
                // 确认候选字
                if (key.hasWord()) {
                    play_InputtingSingleTick_Audio(key);

                    InputWord word = key.getWord();
                    getInputList().getPending().setWord(word);

                    onConfirmSelectedInputCandidate();
                }
            }
            break;
        }
    }

    private void onInputCandidatesCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case FingerSlipping: {
                onInputCandidatesPageSlippingMsg(msg, key, data);
                break;
            }
            case KeyDoubleTap: // 双击继续触发第二次单击操作，以便于支持连续点击并执行对应操作
            case KeySingleTap: {
                play_InputtingSingleTick_Audio(key);

                CharInput input = getInputList().getPending();
                String joinedInputChars = String.join("", input.getChars());

                // 丢弃或变更拼音
                switch (key.getType()) {
                    case DropInput: {
                        getInputList().deleteSelected();

                        confirm_InputChars();
                        break;
                    }
                    case ConfirmInput: {
                        onConfirmSelectedInputCandidate();
                        break;
                    }
                    case ToggleInputSpell_zcs_h: {
                        if (joinedInputChars.startsWith("sh")
                            || joinedInputChars.startsWith("ch")
                            || joinedInputChars.startsWith("zh")) {
                            input.getKeys().remove(1);
                        } else if (joinedInputChars.startsWith("s")
                                   || joinedInputChars.startsWith("c")
                                   || joinedInputChars.startsWith("z")) {
                            input.getKeys().add(1, KeyTable.alphabetKey("h"));
                        }

                        onNewChoosingInputCandidate(input);
                        break;
                    }
                    case ToggleInputSpell_nl: {
                        if (joinedInputChars.startsWith("n")) {
                            input.getKeys().remove(0);
                            input.getKeys().add(0, KeyTable.alphabetKey("l"));
                        } else if (joinedInputChars.startsWith("l")) {
                            input.getKeys().remove(0);
                            input.getKeys().add(0, KeyTable.alphabetKey("n"));
                        }

                        onNewChoosingInputCandidate(input);
                        break;
                    }
                    case ToggleInputSpell_ng: {
                        if (joinedInputChars.endsWith("eng")
                            || joinedInputChars.endsWith("ing")
                            || joinedInputChars.endsWith("ang")) {
                            input.getKeys().remove(input.getKeys().size() - 1);
                        } else if (joinedInputChars.endsWith("en")
                                   || joinedInputChars.endsWith("in")
                                   || joinedInputChars.endsWith("an")) {
                            input.getKeys().add(input.getKeys().size(), KeyTable.alphabetKey("g"));
                        }

                        onNewChoosingInputCandidate(input);
                        break;
                    }
                }
                break;
            }
        }
    }

    private void onInputCandidatesPageSlippingMsg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        switch (msg) {
            case FingerSlipping: {
                // 候选字翻页
                Motion motion = ((UserFingerSlippingMsgData) data).motion;
                boolean pageUp = motion.direction == Motion.Direction.up || motion.direction == Motion.Direction.left;
                CharInput input = getInputList().getPending();

                // Note: 在该函数中根据实际是否有上下翻页来确定是否播放翻页音效
                onChoosingInputCandidate(input, pageUp);
                break;
            }
        }
    }

    private void doSingleKeyInputting(CharKey key) {
        if (getInputList().hasEmptyPending()) {
            getInputList().newPending();
        }

        CharInput input = getInputList().getPending();
        doSingleKeyInputting(input, key);
    }

    private void doReplacementKeyInputting(CharKey key) {
        CharInput input;

        if (getInputList().hasEmptyPending()) {
            Input selected = getInputList().getSelected();
            // Note: 标点是单个输入的，故，需向前替换已输入的标点。
            // 若当前选中的是标点，则也支持双击切换标点
            Input preInput = selected.isSymbol() ? selected : getInputList().getInputBeforeSelected();

            if (preInput != null && key.isSymbol() && preInput.isSymbol()) {
                input = (CharInput) preInput;
            } else {
                return;
            }
        } else {
            input = getInputList().getPending();
        }

        Key<?> latestKey = input.getLatestKey();
        if (!key.canReplaceTheKey(latestKey)) {
            return;
        }

        CharKey latestCharKey = (CharKey) latestKey;
        // Note: 在 Input 中的 key 可能不携带 replacement 信息，只能通过当前按键做判断
        String newKeyText = key.nextReplacement(latestCharKey.getText());

        CharKey newKey = KeyTable.alphabetKey(newKeyText);
        input.replaceLatestKey(latestCharKey, newKey);

        this.state = new State(State.Type.Input_Waiting);
        onContinuousInput(input, key, false);
    }

    private void doSingleKeyInputting(CharInput input, CharKey key) {
        switch (key.getType()) {
            // 若为标点、表情符号，则直接确认输入，不支持连续输入其他字符
            case Emotion:
            case DoubleSymbol:
            case Symbol: {
                boolean isEmpty = getInputList().isEmpty();
                getInputList().newPending().appendKey(key);

                if (!isEmpty) {
                    confirm_InputChars();
                } else {
                    // 单个标点、表情则直接提交输入
                    commit_InputList();
                }
                break;
            }
            // 字母、数字可连续输入
            case Number:
            case Alphabet: {
                this.state = new State(State.Type.Input_Waiting);

                input.appendKey(key);
                onContinuousInput(input, key, false);
                break;
            }
        }
    }

    private void onContinuousInput(CharInput input, Key<?> currentKey, boolean isPinyin) {
        Collection<String> nextChars = null;
        if (isPinyin) {
            nextChars = this.pinyinDict.findNextChar(input.getChars());

//            if (nextChars.size() == 1 && patchUniquePinyin(input)) {
//                nextChars = new ArrayList<>();
//            }
            prepareInputCandidates(input);
        } else {
            input.setWord(null);
        }
        // Note: 连续输入过程中不处理候选字，故而，直接置空该输入的候选字列表，避免浪费内存
        input.setCandidates(new ArrayList<>());

        Collection<String> finalNextChars = nextChars;
        KeyFactory keyFactory = () -> KeyTable.createPinyinNextCharKeys(createKeyTableConfigure(), finalNextChars);

        do_InputChars_Inputting(keyFactory, currentKey);
    }

    /** 重新进入候选字状态 */
    private void onNewChoosingInputCandidate(CharInput input) {
        // Note: 复位状态，以便于做新状态的初始化，而不是利用原状态
        this.state = new State(State.Type.Input_Waiting);
        prepareInputCandidates(input);

        onChoosingInputCandidate(input, true);
    }

    private void onChoosingInputCandidate(CharInput input, boolean pageUp) {
        ChoosingInputCandidateStateData stateData;
        if (this.state.type == State.Type.ChoosingInputCandidate) {
            stateData = (ChoosingInputCandidateStateData) this.state.data;

            boolean hasPage;
            if (pageUp) {
                hasPage = stateData.nextPage();
            } else {
                hasPage = stateData.prevPage();
            }

            if (hasPage) {
                play_InputPageFlipping_Audio();
            }
        } else {
            int dataSize = input.getCandidates().size();
            int pageSize = KeyTable.getInputCandidateKeysPageSize();
            stateData = new ChoosingInputCandidateStateData(dataSize, pageSize);

            this.state = new State(State.Type.ChoosingInputCandidate, stateData);
        }

        KeyFactory keyFactory = () -> KeyTable.createInputCandidateKeys(createKeyTableConfigure(),
                                                                        input,
                                                                        stateData.getPageStart());
        InputMsgData data = new InputCharsInputtingMsgData(keyFactory, null);

        fireInputMsg(InputMsg.InputCandidate_Choosing, data);
    }

    private void onConfirmSelectedInputCandidate() {
        getInputList().getPending().getWord().setConfirmed(true);
        getInputList().confirmPending();

        // 继续选择下一个拼音输入的候选字
        Input selected;
        do {
            getInputList().moveToNextCharInput();
            selected = getInputList().getSelected();
        } while (selected != null && !selected.isPinyin() && selected != getInputList().getLastInput());

        onChoosingInputMsg(null, selected, null);
    }

    private void prepareInputCandidates(CharInput input) {
        List<String> pinyinChars = input.getChars();
        List<InputWord> candidateWords = this.pinyinDict.getCandidateWords(pinyinChars);
        input.setCandidates(candidateWords);

        if (!candidateWords.contains(input.getWord()) || !input.getWord().isConfirmed()) {
            // 根据当前位置之前的输入确定当前位置的最佳候选字
            List<InputWord> preInputWords = CollectionUtils.last(getInputList().getPinyinPhrases(true));

            InputWord bestCandidate = this.pinyinDict.findBestCandidateWord(candidateWords, preInputWords);
            input.setWord(bestCandidate);
        }
    }

    /**
     * 若指定输入仅有唯一的可选拼音，
     * 则直接补全该拼音，并返回 <code>true</code>，
     * 否则，不做处理并直接返回 <code>false</code>
     */
    private boolean patchUniquePinyin(CharInput input) {
        List<String> chars = new ArrayList<>(input.getChars());
        List<String> patchedChars = new ArrayList<>();

        do {
            List<?> candidates = this.pinyinDict.getCandidateWords(chars);
            Collection<String> nextChars = this.pinyinDict.findNextChar(chars);

            if (nextChars.isEmpty()) {
                // 无效拼音：无候选字
                if (candidates.isEmpty()) {
                    return false;
                }
                // 查找结束，有唯一拼音
                else {
                    break;
                }
            }
            // 存在后继字母，且当前有后选字，则不存在唯一拼音，则不做处理
            else if (!candidates.isEmpty() || nextChars.size() > 1) {
                return false;
            }
            // 继续沿唯一的后继字母进行检查
            else {
                String ch = nextChars.iterator().next();
                patchedChars.add(ch);
                chars.add(ch);
            }
        } while (true);

        for (String ch : patchedChars) {
            input.appendKey(KeyTable.alphabetKey(ch));
        }

        return true;
    }

    // <<<<<<<<< 对输入列表的操作
    private void onChoosingInputMsg(UserInputMsg msg, Input input, UserInputMsgData data) {
        getInputList().newPendingOn(input);

        CharInput pending = getInputList().getPending();
        // 仅当待输入为拼音时才进入候选字模式：输入过程中操作和处理的都是待输入
        if (pending.isPinyin()) {
            onNewChoosingInputCandidate(pending);
        }
        // 其余情况都是直接做替换输入
        else {
            confirm_InputChars();
        }
    }
    // >>>>>>>>>
}
