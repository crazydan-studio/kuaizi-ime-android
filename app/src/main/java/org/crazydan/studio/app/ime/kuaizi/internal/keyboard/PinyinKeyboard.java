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
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.pinyin.state.ChoosingInputCandidateData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.CommonInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputCommittingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputtingCharsMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.PlayingInputAudioMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.UserFingerMovingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.UserFingerSlippingMsgData;

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
    public void onUserMsg(UserMsg msg, UserMsgData data) {
        Key<?> key = data.target;
        if (key != null && key.isDisabled()) {
            return;
        }

        if (key instanceof CharKey) {
            onCharKeyMsg(msg, (CharKey) key, data);
        } else if (key instanceof CtrlKey) {
            if (this.state.type == State.Type.ChoosingInputCandidate) {
                onInputCandidatesCtrlKeyMsg(msg, (CtrlKey) key, data);
            } else {
                onCtrlKeyMsg(msg, (CtrlKey) key, data);
            }
        } else if (key instanceof InputWordKey) {
            if (this.state.type == State.Type.ChoosingInputCandidate) {
                onInputCandidateKeyMsg(msg, (InputWordKey) key, data);
            }
        } else if (key == null) {
            switch (this.state.type) {
                case SlippingInput:
                    // Note: 滑行输入过程中，消息体不一定带有 target 数据，故而单独执行响应
                    onCharKeyMsg(msg, null, data);
                    break;
                case ChoosingInputCandidate:
                    // Note: 手指滑动消息一定不带 target 数据，故而单独处理一次
                    if (msg == UserMsg.FingerSlipping) {
                        onInputCandidatesCtrlKeyMsg(msg, null, data);
                    }
                    break;
            }
        }
    }

    private void onCharKeyMsg(UserMsg msg, CharKey key, UserMsgData data) {
        switch (msg) {
            case KeyLongPressStart: {
                // 开始滑行输入
                if (key != null && key.getType() == CharKey.Type.Alphabet) {
                    this.state = new State(State.Type.SlippingInput);

                    CharInput input = getInputList().newPending();
                    input.setPinyin(true).appendKey(key);

                    onPlayingInputAudio_DoubleTick(key);

                    onContinuousInput(input, data.target, null, true);
                }
                break;
            }
            case KeyLongPressEnd: {
                // 选择候选字
                if (this.state.type == State.Type.SlippingInput) {
                    CharInput input = getInputList().getPending();

                    // 若无额外候选字，则直接确认输入
                    if (!input.hasExtraCandidates()) {
                        // 无候选字的输入，视为无效，直接丢弃
                        if (!input.hasWord()) {
                            getInputList().dropPending();
                        } else {
                            getInputList().confirmPending();
                        }
                        onInputtingCharsDone();
                    } else {
                        onChoosingInputCandidate(input, true);
                    }
                }
                break;
            }
            case FingerMoving: {
                // 添加拼音后继字母
                if (this.state.type == State.Type.SlippingInput) {
                    CharInput input = getInputList().getPending();
                    Key<?> closedKey = ((UserFingerMovingMsgData) data).closed;

                    // Note: 拼音不存在重复字母相邻的情况
                    if (key != null && !key.isSameWith(input.getCurrentKey())) {
                        input.appendKey(key);

                        onPlayingInputAudio_DoubleTick(key);
                    }

                    if (key != null || closedKey != null) {
                        onContinuousInput(input, key, closedKey, true);
                    }
                }
                break;
            }
            case KeySingleTap: {
                // 单字符输入
                if (key != null) {
                    onPlayingInputAudio_SingleTick(key);

                    doSingleKeyInput(key);
                }
                break;
            }
        }
    }

    private void onCtrlKeyMsg(UserMsg msg, CtrlKey key, UserMsgData data) {
        switch (msg) {
            case KeySingleTap: {
                if (key.getType() != CtrlKey.Type.Locator) {
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
                // TODO 长按定位按钮
                break;
            }
            case KeyLongPressTick: {
                switch (key.getType()) {
                    case Backspace:
                    case Space:
                    case Enter:
                        // 长按 tick 视为连续单击
                        onCtrlKeyMsg(UserMsg.KeySingleTap, key, data);
                        break;
                }
                break;
            }
        }
    }

    private void onInputCandidatesCtrlKeyMsg(UserMsg msg, CtrlKey key, UserMsgData data) {
        switch (msg) {
            case KeySingleTap: {
                onPlayingInputAudio_SingleTick(key);

                CharInput input = getInputList().getPending();
                String joinedInputChars = String.join("", input.getChars());

                // 丢弃或变更拼音
                switch (key.getType()) {
                    case DropInput: {
                        getInputList().dropPending();
                        onInputtingCharsDone();
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

                        // 重置状态，以确保候选字列表能被更新
                        this.state = new State(State.Type.InputWaiting);
                        prepareInputCandidates(input);

                        onChoosingInputCandidate(input, true);
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

                        // 重置状态，以确保候选字列表能被更新
                        this.state = new State(State.Type.InputWaiting);
                        prepareInputCandidates(input);

                        onChoosingInputCandidate(input, true);
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

                        // 重置状态，以确保候选字列表能被更新
                        this.state = new State(State.Type.InputWaiting);
                        prepareInputCandidates(input);

                        onChoosingInputCandidate(input, true);
                        break;
                    }
                }
                break;
            }
            case FingerSlipping: {
                // 候选字翻页
                boolean pageUp = ((UserFingerSlippingMsgData) data).upward;
                CharInput input = getInputList().getPending();

                // Note: 在该函数中根据实际是否有上下翻页来确定是否播放翻页音效
                onChoosingInputCandidate(input, pageUp);
            }
            break;
        }
    }

    private void onInputCandidateKeyMsg(UserMsg msg, InputWordKey key, UserMsgData data) {
        CharKey charKey = key.getCharKey();

        switch (msg) {
            case KeyLongPressStart: {
                if (charKey != null) {
                    // 长按标点，则由单击响应处理
                    if (charKey.isPunctuation()) {
                        onPlayingInputAudio_DoubleTick(key);

                        doSingleKeyInput(charKey);
                    }
                    // 开始滑行输入
                    else {
                        // Note: 在 onCharKeyMsg 中会播放长按的按键音，
                        // 故而，这里不再播放
                        onCharKeyMsg(msg, charKey, new UserMsgData(key));
                    }
                }
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
//                }
//                // 仅标点可在无候选字的按钮上单击，否则，仅长按才可输入该符号
//                else if (charKey != null && charKey.isPunctuation()) {
//                    onCharKeyMsg(msg, charKey, new UserMsgData(charKey));
                }
            }
            break;
        }
    }

    private void doSingleKeyInput(CharKey key) {
        if (!getInputList().hasPending()) {
            getInputList().newPending();
        }

        CharInput input = getInputList().getPending();
        onSingleKeyInput(input, key);
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

        InputMsgData data = new InputtingCharsMsgData(input.getKeys(), currentKey, closedKey, keyFactory);
        onInputMsg(InputMsg.InputtingChars, data);
    }

    private void onInputtingCharsDone() {
        // 当前输入已完成，等待新的输入
        this.state = new State(State.Type.InputWaiting);

        onInputMsg(InputMsg.InputtingCharsDone, new CommonInputMsgData(getKeyFactory()));
    }

    private void onChoosingInputCandidate(CharInput input, boolean pageUp) {
        // TODO 按本地常用字等信息，确定最佳候选字，并切换到该候选字所在的分页
        ChoosingInputCandidateData stateData;
        if (this.state.type == State.Type.ChoosingInputCandidate) {
            stateData = (ChoosingInputCandidateData) this.state.data;

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
            stateData = new ChoosingInputCandidateData(input.getCandidates().size(),
                                                       KeyTable.getInputCandidateKeysPageSize());
            this.state = new State(State.Type.ChoosingInputCandidate, stateData);
        }

        KeyFactory keyFactory = option -> KeyTable.createInputCandidateKeys(option,
                                                                            createKeyTableConfigure(),
                                                                            input,
                                                                            stateData.getPageStart());
        InputMsgData data = new InputtingCharsMsgData(input.getKeys(), null, null, keyFactory);

        onInputMsg(InputMsg.ChoosingInputCandidate, data);
    }

    private void onPlayingInputAudio_SingleTick(Key<?> key) {
        onPlayingInputAudio(key, PlayingInputAudioMsgData.AudioType.SingleTick);
    }

    private void onPlayingInputAudio_DoubleTick(Key<?> key) {
        onPlayingInputAudio(key, PlayingInputAudioMsgData.AudioType.DoubleTick);
    }

    private void onPlayingInputAudio_PageFlip() {
        InputMsgData data = new PlayingInputAudioMsgData(PlayingInputAudioMsgData.AudioType.PageFlip);

        onInputMsg(InputMsg.PlayingInputAudio, data);
    }

    private void onPlayingInputAudio(Key<?> key, PlayingInputAudioMsgData.AudioType audioType) {
        if (key == null //
            || (key instanceof CtrlKey && ((CtrlKey) key).isNoOp())) {
            return;
        }

        InputMsgData data = new PlayingInputAudioMsgData(audioType);

        onInputMsg(InputMsg.PlayingInputAudio, data);
    }

    private void onInputtingCommit() {
        // 输入已提交，等待新的输入
        this.state = new State(State.Type.InputWaiting);

        getInputList().confirmPending();

        StringBuilder text = getInputList().getText();
        getInputList().empty();

        InputMsgData data = new InputCommittingMsgData(getKeyFactory(), text);

        onInputMsg(InputMsg.InputCommitting, data);
    }

    private void onInputBackwardDeleting() {
        // 单次操作，直接重置为待输入状态
        this.state = new State(State.Type.InputWaiting);

        onInputMsg(InputMsg.InputBackwardDeleting, new CommonInputMsgData(getKeyFactory()));
    }

    private void onSwitchingIME() {
        // 单次操作，直接重置为待输入状态
        reset();

        onInputMsg(InputMsg.IMESwitching, new CommonInputMsgData(null));
    }

    private void prepareInputCandidates(CharInput input) {
        List<InputWord> candidateWords = this.pinyinCharTree.findCandidateWords(input.getChars())
                                                            .stream()
                                                            .map(InputWord::from)
                                                            .collect(Collectors.toList());
        input.setWord(candidateWords.isEmpty() ? null : candidateWords.get(0));
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
}
