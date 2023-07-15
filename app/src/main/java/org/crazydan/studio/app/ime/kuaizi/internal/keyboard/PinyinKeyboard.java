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

        this.state = new State(State.Type.InputWaiting);
        onInputMsg(InputMsg.InputtingCharsDone, new CommonInputMsgData(getKeyFactory()));
    }

    @Override
    public KeyFactory getKeyFactory() {
        return option -> KeyTable.createKeys(option, createKeyTableConfigure());
    }

    @Override
    public void onUserMsg(UserMsg msg, UserMsgData data) {
        Key<?> key = data.target;

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
                    input.appendKey(key);

                    onContinuousInput(input, key, null, true);
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
                    if (!getInputList().hasPending()) {
                        getInputList().newPending();
                    }

                    CharInput input = getInputList().getPending();
                    onSingleKeyInput(input, key);
                }
                break;
            }
        }
    }

    private void onCtrlKeyMsg(UserMsg msg, CtrlKey key, UserMsgData data) {
        switch (msg) {
            case KeySingleTap: {
                switch (key.getType()) {
                    case CommitInput: {
                        onInputtingCommit();
                        break;
                    }
                    case Backspace: {
                        if (getInputList().hasPending()) {
                            getInputList().dropPending();
                        } else {
                            getInputList().backwardDelete();
                        }
                        onInputtingCharsDone();
                        break;
                    }
                    case Space: {
                        // 单独输入并确认空格
                        getInputList().newPending().appendKey(key);
                        getInputList().confirmPending();

                        onInputtingCharsDone();
                    }
                }
                break;
            }
            case KeyLongPressStart:
                // TODO 长按定位按钮
                break;
        }
    }

    private void onInputCandidatesCtrlKeyMsg(UserMsg msg, CtrlKey key, UserMsgData data) {
        switch (msg) {
            case KeySingleTap: {
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
                        onCharKeyMsg(UserMsg.KeySingleTap, charKey, new UserMsgData(charKey));
                    }
                    // 开始滑行输入
                    else {
                        onCharKeyMsg(msg, charKey, new UserMsgData(charKey));
                    }
                }
                break;
            }
            case KeySingleTap: {
                // 确认候选字
                if (key.hasWord()) {
                    InputWord word = key.getWord();
                    getInputList().getPending().setWord(word);
                    getInputList().confirmPending();

                    onInputtingCharsDone();
                }
                // 仅标点可在无候选字的按钮上单击，否则，仅长按才可输入该符号
                else if (charKey != null && charKey.isPunctuation()) {
                    onCharKeyMsg(msg, charKey, new UserMsgData(charKey));
                }
            }
            break;
        }
    }

    private void onSingleKeyInput(CharInput input, CharKey key) {
        this.state = new State(State.Type.InputWaiting);

        switch (key.getType()) {
            // 若为标点、表情符号，则直接确认输入，不支持连续输入其他字符
            case Emotion:
            case Punctuation:
                getInputList().newPending().appendKey(key);
                getInputList().confirmPending();

                onInputMsg(InputMsg.InputtingCharsDone, new CommonInputMsgData(getKeyFactory()));
                break;
            // 字母、数字可连续输入
            case Number:
            case Alphabet:
                input.appendKey(key);
                onContinuousInput(input, key, null, false);
                break;
        }
    }

    private void onContinuousInput(CharInput input, Key<?> currentKey, Key<?> closedKey, boolean isPinyin) {
        List<String> nextChars = null;
        if (isPinyin) {
            nextChars = this.pinyinCharTree.findNextChars(input.getChars());

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
        this.state = new State(State.Type.InputWaiting);

        onInputMsg(InputMsg.InputtingCharsDone, new CommonInputMsgData(getKeyFactory()));
    }

    private void onChoosingInputCandidate(CharInput input, boolean pageUp) {
        // TODO 按本地常用字等信息，确定最佳候选字，并切换到该候选字所在的分页
        ChoosingInputCandidateData stateData;
        if (this.state.type == State.Type.ChoosingInputCandidate) {
            stateData = (ChoosingInputCandidateData) this.state.data;

            if (pageUp) {
                stateData.nextPage();
            } else {
                stateData.prevPage();
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

    private void onInputtingCommit() {
        StringBuilder text = getInputList().getText();
        InputMsgData data = new InputCommittingMsgData(text);

        onInputMsg(InputMsg.InputCommitting, data);
    }

    private void prepareInputCandidates(CharInput input) {
        List<InputWord> candidateWords = this.pinyinCharTree.findCandidateWords(input.getChars())
                                                            .stream()
                                                            .map(InputWord::from)
                                                            .collect(Collectors.toList());
        input.setWord(candidateWords.isEmpty() ? null : candidateWords.get(0));
        input.setCandidates(candidateWords);
    }

    private KeyTable.Configure createKeyTableConfigure() {
        return new KeyTable.Configure(getHandMode(), !getInputList().isEmpty());
    }
}
