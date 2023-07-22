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
import java.util.List;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinCharTree;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.pinyin.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.pinyin.State;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.pinyin.state.ChoosingInputCandidateStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.pinyin.state.LocatingInputCursorStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.CommonInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputCommittingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputCursorLocatingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputtingCharsMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.PlayingInputAudioMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.UserFingerMovingKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.UserFingerSlippingKeyMsgData;

/**
 * 汉语拼音{@link Keyboard 键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class PinyinKeyboard extends BaseKeyboard {
    private final PinyinCharTree pinyinCharTree;
    private State state = new State(State.Type.InputWaiting);

    public PinyinKeyboard(PinyinCharTree pinyinCharTree) {
        this.pinyinCharTree = pinyinCharTree;
    }

    @Override
    public void reset() {
        super.reset();

        onInputtingCharsDone();
    }

    @Override
    public KeyFactory getKeyFactory() {
        return option -> KeyTable.createKeys(option, createKeyTableConfigure());
    }

    @Override
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        switch (this.state.type) {
            case InputWaiting:
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
        Key<?> key = data.target;
        if (key != null && key.isDisabled()) {
            return;
        }

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
            case LocatingInputCursor:
                onLocatingInputCursorCtrlKeyMsg(msg, (CtrlKey) key, data);
                break;
            default:
                if (key instanceof CharKey) {
                    onCharKeyMsg(msg, (CharKey) key, data);
                } else if (key instanceof CtrlKey) {
                    onCtrlKeyMsg(msg, (CtrlKey) key, data);
                }
        }
    }

    private void onCharKeyMsg(UserKeyMsg msg, CharKey key, UserKeyMsgData data) {
        switch (msg) {
            case FingerMovingStart: {
                // 开始滑行输入
                if (key.getType() == CharKey.Type.Alphabet) {
                    this.state = new State(State.Type.SlippingInput);

                    CharInput input = getInputList().newPending();
                    input.appendKey(key);

                    onPlayingInputAudio_DoubleTick(key);

                    onContinuousInput(input, data.target, null, true);
                }
                break;
            }
            case KeySingleTap: {
                // 单字符输入
                onPlayingInputAudio_SingleTick(key);

                doSingleKeyInput(key);
                break;
            }
            case KeyDoubleTap: {
                // 双击字符，则替换前一个相同的输入字符：
                // 因为双击会先触发单击，而单击时会添加一次该字符
                onPlayingInputAudio_SingleTick(key);

                onReplacementKeyInput(key);
                break;
            }
        }
    }

    private void onSlippingInputCharKeyMsg(UserKeyMsg msg, CharKey key, UserKeyMsgData data) {
        switch (msg) {
            case FingerMoving: {
                // 添加拼音后继字母
                CharInput input = getInputList().getPending();
                Key<?> closedKey = ((UserFingerMovingKeyMsgData) data).closed;

                // Note: 拼音不存在重复字母相邻的情况
                if (key != null && !key.isSameWith(input.getLatestKey())) {
                    input.appendKey(key);

                    onPlayingInputAudio_DoubleTick(key);
                }

                if (key != null || closedKey != null) {
                    // Note: 第二个参数传事件触发的实际 key，而不是当前的 charkey
                    onContinuousInput(input, data.target, closedKey, true);
                }
                break;
            }
            case FingerMovingEnd: {
                CharInput input = getInputList().getPending();

                // 无候选字的输入，视为无效输入，直接丢弃
                if (!input.hasWord()) {
                    getInputList().dropPending();
                } else {
                    getInputList().confirmPending();
                }
                onInputtingCharsDone();
                break;
            }
        }
    }

    private void onCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeyDoubleTap: // 双击继续触发第二次单击操作
            case KeySingleTap: {
                if (key.getType() != CtrlKey.Type.LocateInputCursor) {
                    onPlayingInputAudio_SingleTick(key);
                }

                switch (key.getType()) {
                    case CommitInput: {
                        onInputtingCommit();
                        break;
                    }
                    case Backspace: {
                        if (!getInputList().isEmpty()) {
                            if (getInputList().hasPending()) {
                                getInputList().dropPending();
                            } else {
                                getInputList().backwardDelete();
                            }
                            onInputtingCharsDone();
                        } else {
                            onInputBackwardDeleting();
                        }
                        break;
                    }
                    case Space:
                    case Enter: {
                        boolean isEmpty = getInputList().isEmpty();
                        // 单独输入并确认 空格/换行
                        getInputList().newPending().appendKey(key);

                        if (!isEmpty) {
                            getInputList().confirmPending();
                            onInputtingCharsDone();
                        } else {
                            // 单个 空格/换行 则直接提交输入
                            onInputtingCommit();
                        }
                        break;
                    }
                    case SwitchIME: {
                        onSwitchingIME();
                        break;
                    }
                }
                break;
            }
            case KeyLongPressStart: {
                if (key.getType() == CtrlKey.Type.LocateInputCursor) {
                    onPlayingInputAudio_DoubleTick(key);

                    onLocatingInputCursor(key, null);
                }
                break;
            }
            case KeyLongPressTick: {
                switch (key.getType()) {
                    case Backspace:
                    case Space:
                    case Enter:
                        // 长按 tick 视为连续单击
                        onCtrlKeyMsg(UserKeyMsg.KeySingleTap, key, data);
                        break;
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
                    onPlayingInputAudio_SingleTick(key);

                    InputWord word = key.getWord();
                    getInputList().getPending().setWord(word);
                    getInputList().confirmPending();

                    onInputtingCharsDone();
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
                onPlayingInputAudio_SingleTick(key);

                CharInput input = getInputList().getPending();
                String joinedInputChars = String.join("", input.getChars());

                // 丢弃或变更拼音
                switch (key.getType()) {
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
                Motion motion = ((UserFingerSlippingKeyMsgData) data).motion;
                boolean pageUp = motion.direction == Motion.Direction.up || motion.direction == Motion.Direction.left;
                CharInput input = getInputList().getPending();

                // Note: 在该函数中根据实际是否有上下翻页来确定是否播放翻页音效
                onChoosingInputCandidate(input, pageUp);
                break;
            }
        }
    }

    private void doSingleKeyInput(CharKey key) {
        if (!getInputList().hasPending()) {
            getInputList().newPending();
        }

        CharInput input = getInputList().getPending();
        onSingleKeyInput(input, key);
    }

    private void onReplacementKeyInput(CharKey key) {
        CharInput input;

        if (!getInputList().hasPending()) {
            // Note: 标点是单个输入的，故，需向后替换已输入的标点
            Input preInput = getInputList().getInputBeforeSelected();
            if (preInput != null && key.isPunctuation() && preInput.isPunctuation()) {
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

        this.state = new State(State.Type.InputWaiting);
        onContinuousInput(input, key, null, false);
    }

    private void onSingleKeyInput(CharInput input, CharKey key) {
        switch (key.getType()) {
            // 若为标点、表情符号，则直接确认输入，不支持连续输入其他字符
            case Emotion:
            case Punctuation: {
                boolean isEmpty = getInputList().isEmpty();
                getInputList().newPending().appendKey(key);

                if (!isEmpty) {
                    getInputList().confirmPending();
                    onInputtingCharsDone();
                } else {
                    // 单个标点、表情则直接提交输入
                    onInputtingCommit();
                }
                break;
            }
            // 字母、数字可连续输入
            case Number:
            case Alphabet: {
                this.state = new State(State.Type.InputWaiting);

                input.appendKey(key);
                onContinuousInput(input, key, null, false);
                break;
            }
        }
    }

    private void onContinuousInput(CharInput input, Key<?> currentKey, Key<?> closedKey, boolean isPinyin) {
        List<String> nextChars = null;
        if (isPinyin) {
            nextChars = this.pinyinCharTree.findNextChars(input.getChars());

            if (nextChars.size() == 1 && patchUniquePinyin(input)) {
                nextChars = new ArrayList<>();
            }
            prepareInputCandidates(input);
        } else {
            input.setWord(null);
            input.setCandidates(new ArrayList<>());
        }

        List<String> finalNextChars = nextChars;
        KeyFactory keyFactory = option -> KeyTable.createNextCharKeys(option,
                                                                      createKeyTableConfigure(),
                                                                      finalNextChars);

        InputMsgData data = new InputtingCharsMsgData(keyFactory, input.getKeys(), currentKey, closedKey);
        fireInputMsg(InputMsg.InputtingChars, data);
    }

    /** 当前输入已完成，等待新的输入 */
    private void onInputtingCharsDone() {
        this.state = new State(State.Type.InputWaiting);

        fireInputMsg(InputMsg.InputtingCharsDone, new CommonInputMsgData(getKeyFactory()));
    }

    /** 重新进入候选字状态 */
    private void onNewChoosingInputCandidate(CharInput input) {
        // Note: 复位状态，以便于做新状态的初始化，而不是利用原状态
        this.state = new State(State.Type.InputWaiting);
        prepareInputCandidates(input);

        onChoosingInputCandidate(input, true);
    }

    private void onChoosingInputCandidate(CharInput input, boolean pageUp) {
        // TODO 按本地常用字等信息，确定最佳候选字，并切换到该候选字所在的分页
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
                onPlayingInputAudio_PageFlip();
            }
        } else {
            stateData = new ChoosingInputCandidateStateData(input.getCandidates().size(),
                                                            KeyTable.getInputCandidateKeysPageSize());
            this.state = new State(State.Type.ChoosingInputCandidate, stateData);
        }

        KeyFactory keyFactory = option -> KeyTable.createInputCandidateKeys(option,
                                                                            createKeyTableConfigure(),
                                                                            input,
                                                                            stateData.getPageStart());
        InputMsgData data = new InputtingCharsMsgData(keyFactory, input.getKeys(), null, null);

        fireInputMsg(InputMsg.ChoosingInputCandidate, data);
    }

    private void onPlayingInputAudio_SingleTick(Key<?> key) {
        onPlayingInputAudio(key, PlayingInputAudioMsgData.AudioType.SingleTick);
    }

    private void onPlayingInputAudio_DoubleTick(Key<?> key) {
        onPlayingInputAudio(key, PlayingInputAudioMsgData.AudioType.DoubleTick);
    }

    private void onPlayingInputAudio_PageFlip() {
        InputMsgData data = new PlayingInputAudioMsgData(PlayingInputAudioMsgData.AudioType.PageFlip);

        fireInputMsg(InputMsg.PlayingInputAudio, data);
    }

    private void onPlayingInputAudio(Key<?> key, PlayingInputAudioMsgData.AudioType audioType) {
        if (key == null //
            || (key instanceof CtrlKey && ((CtrlKey) key).isNoOp())) {
            return;
        }

        InputMsgData data = new PlayingInputAudioMsgData(audioType);

        fireInputMsg(InputMsg.PlayingInputAudio, data);
    }

    private void onInputtingCommit() {
        // 输入已提交，等待新的输入
        this.state = new State(State.Type.InputWaiting);

        getInputList().confirmPending();

        StringBuilder text = getInputList().getText();
        getInputList().empty();

        InputMsgData data = new InputCommittingMsgData(getKeyFactory(), text);

        fireInputMsg(InputMsg.InputCommitting, data);
    }

    private void onInputBackwardDeleting() {
        // 单次操作，直接重置为待输入状态
        this.state = new State(State.Type.InputWaiting);

        fireInputMsg(InputMsg.InputBackwardDeleting, new CommonInputMsgData(getKeyFactory()));
    }

    private void onSwitchingIME() {
        // 单次操作，直接重置为待输入状态
        reset();

        fireInputMsg(InputMsg.IMESwitching, new CommonInputMsgData(null));
    }

    private void prepareInputCandidates(CharInput input) {
        List<InputWord> candidateWords = this.pinyinCharTree.findCandidateWords(input.getChars())
                                                            .stream()
                                                            .map(InputWord::from)
                                                            .collect(Collectors.toList());
        // Note: 在拼音未改变的情况下，保留原来的已选字
        if (!candidateWords.contains(input.getWord())) {
            input.setWord(candidateWords.isEmpty() ? null : candidateWords.get(0));
        }
        input.setCandidates(candidateWords);
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
            List<?> candidates = this.pinyinCharTree.findCandidateWords(chars);
            List<String> nextChars = this.pinyinCharTree.findNextChars(chars);

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
                String ch = nextChars.get(0);
                patchedChars.add(ch);
                chars.add(ch);
            }
        } while (true);

        for (String ch : patchedChars) {
            input.appendKey(KeyTable.alphabetKey(ch));
        }

        return true;
    }

    private KeyTable.Configure createKeyTableConfigure() {
        return new KeyTable.Configure(getHandMode(), !getInputList().isEmpty());
    }

    // <<<<<< 输入定位逻辑
    private void onLocatingInputCursorCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeySingleTap: {
                switch (key.getType()) {
                    // 点击 退出 按钮，则退回到输入状态
                    case Exit:
                        onPlayingInputAudio_SingleTick(key);

                        onInputtingCharsDone();
                        break;
                }
                break;
            }
            case FingerSlipping:
                // Note: 仅在 按键 上的滑动才有效
                if (key != null) {
                    Motion motion = ((UserFingerSlippingKeyMsgData) data).motion;
                    switch (key.getType()) {
                        case LocateInputCursor_Locator:
                            onPlayingInputAudio_SingleTick(key);

                            onLocatingInputCursor(key, motion);
                            break;
                        case LocateInputCursor_Selector:
                            onPlayingInputAudio_SingleTick(key);

                            onSelectingInputText(key, motion);
                            break;
                    }
                }
                break;
        }
    }

    private void onLocatingInputCursor(CtrlKey key, Motion motion) {
        KeyFactory keyFactory = null;
        LocatingInputCursorStateData stateData;

        if (this.state.type != State.Type.LocatingInputCursor) {
            stateData = new LocatingInputCursorStateData();
            keyFactory = option -> KeyTable.createLocatorKeys(option, createKeyTableConfigure());

            this.state = new State(State.Type.LocatingInputCursor, stateData);
        } else {
            stateData = (LocatingInputCursorStateData) this.state.data;
            stateData.updateLocator(motion);
        }

        InputMsgData data = new InputCursorLocatingMsgData(keyFactory, key, stateData.getLocator());
        fireInputMsg(InputMsg.LocatingInputCursor, data);
    }

    private void onSelectingInputText(CtrlKey key, Motion motion) {
        LocatingInputCursorStateData stateData = (LocatingInputCursorStateData) this.state.data;
        stateData.updateSelector(motion);

        InputMsgData data = new InputCursorLocatingMsgData(null, key, stateData.getSelector());
        fireInputMsg(InputMsg.SelectingInputText, data);
    }
    // >>>>>>>>

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
            getInputList().confirmPending();
            onInputtingCharsDone();
        }
    }
    // >>>>>>>>>
}
