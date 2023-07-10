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
import org.crazydan.studio.app.ime.kuaizi.internal.msg.KeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.KeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.CommonInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.FingerFlingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.FingerMoveMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputtingCharsMsgData;

/**
 * 汉语拼音{@link Keyboard 键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class PinyinKeyboard extends BaseKeyboard {
    private final PinyinCharTree pinyinCharTree;
    private State state = new State(State.Type.Init);
    private boolean slidingInput;

    public PinyinKeyboard(PinyinCharTree pinyinCharTree) {
        this.pinyinCharTree = pinyinCharTree;
    }

    @Override
    public void reset() {
        this.state = new State(State.Type.Init);
        super.reset();
    }

    @Override
    public KeyFactory keyFactory() {
        return option -> KeyTable.showKeys(KeyTable.keys(option, handMode()));
    }

    @Override
    public void onKeyMsg(KeyMsg msg, KeyMsgData data) {
        if (data.target instanceof CharKey) {
            onCharKeyMsg(msg, data);
        } else if (data.target instanceof CtrlKey || msg == KeyMsg.FingerFling) {
            onCtrlKeyMsg(msg, data);
        } else if (data.target instanceof InputWordKey) {
            onInputWordKeyMsg(msg, data);
        }
    }

    private void onCharKeyMsg(KeyMsg msg, KeyMsgData data) {
        CharKey key = (CharKey) data.target;

        switch (msg) {
            case KeyLongPress: {
                this.state = new State(State.Type.Inputting);
                this.slidingInput = true;
                inputList().initPending();

                CharInput input = (CharInput) inputList().cursor().pending();
                input.append(key);

                onInputtingChars(input, key, null);
                break;
            }
            case FingerMove: {
                if (this.state.type == State.Type.Inputting && this.slidingInput) {
                    CharInput input = (CharInput) inputList().cursor().pending();
                    if (key != input.currentKey()) {
                        input.append(key);

                        Key<?> closed = ((FingerMoveMsgData) data).closed;
                        onInputtingChars(input, key, closed);
                    }
                }
                break;
            }
            case KeyLongPressEnd: {
                if (this.state.type == State.Type.Inputting) {
                    this.slidingInput = false;

                    CharInput input = (CharInput) inputList().cursor().pending();
                    if (!input.hasExtraCandidates()) {
                        confirmInputPending();
                    } else {
                        KeyFactory keyFactory = option -> switchToChoosingInputCandidate(option, input, true);
                        InputMsgData idata = new InputtingCharsMsgData(input.keys(), key, null, keyFactory);

                        onInputMsg(InputMsg.InputtingChars, idata);
                    }
                }
                break;
            }
        }
    }

    private void onCtrlKeyMsg(KeyMsg msg, KeyMsgData data) {
        CtrlKey key = (CtrlKey) data.target;

        switch (msg) {
            case KeyClick: {
                if (this.state.type == State.Type.ChoosingInputCandidate //
                    && key.type() == CtrlKey.Type.Confirm) {
                    confirmInputPending();
                }
                break;
            }
            case FingerFling: {
                if (this.state.type == State.Type.ChoosingInputCandidate) {
                    CharInput input = (CharInput) inputList().cursor().pending();
                    KeyFactory keyFactory = option -> switchToChoosingInputCandidate(option,
                                                                                     input,
                                                                                     ((FingerFlingMsgData) data).up);
                    InputMsgData idata = new InputtingCharsMsgData(input.keys(), key, null, keyFactory);

                    onInputMsg(InputMsg.InputtingChars, idata);
                }
                break;
            }
        }
    }

    private void onInputWordKeyMsg(KeyMsg msg, KeyMsgData data) {
        InputWordKey key = (InputWordKey) data.target;

        switch (msg) {
            case KeyClick: {
                if (this.state.type == State.Type.ChoosingInputCandidate) {
                    InputWord word = key.word();
                    CharInput input = (CharInput) inputList().cursor().pending();
                    input.word(word);

                    confirmInputPending();
                }
                break;
            }
        }
    }

    private void onInputtingChars(CharInput input, CharKey currentKey, Key<?> closedKey) {
        List<InputWord> candidateWords = this.pinyinCharTree.findCandidateWords(input.chars())
                                                            .stream()
                                                            .map(InputWord::from)
                                                            .sorted()
                                                            .collect(Collectors.toList());
        input.word(candidateWords.isEmpty() ? null : candidateWords.get(0));
        input.candidates(candidateWords);

        KeyFactory keyFactory = createKeyFactoryByInput(input);
        InputMsgData data = new InputtingCharsMsgData(input.keys(), currentKey, closedKey, keyFactory);

        onInputMsg(InputMsg.InputtingChars, data);
    }

    private void confirmInputPending() {
        this.state = new State(State.Type.Inputting);
        this.slidingInput = false;

        CharInput input = (CharInput) inputList().cursor().pending();
        if (input == null || !input.hasWord()) {
            // 无有效拼音，则丢弃输入
            inputList().dropPending();
        } else {
            inputList().confirmPending();
        }
        onInputMsg(InputMsg.InputtingCharsDone, new CommonInputMsgData(keyFactory()));
    }

    private KeyFactory createKeyFactoryByInput(CharInput input) {
        List<String> nextChars = this.pinyinCharTree.findNextChars(input.chars());

        return option -> {
            Key<?>[][] keys = keyFactory().create(option);
            // 有后继字母，则仅显示后继字母按键
            if (!nextChars.isEmpty()) {
                KeyTable.traverseKeys(keys, key -> {
                    key.hide();
                    if (key instanceof CharKey && nextChars.contains(((CharKey) key).text())) {
                        key.show();
                    }
                });
            }
            // 无后继字母，但有额外的候选字，则进入候选字模式
            else if (input.hasExtraCandidates()) {
                keys = switchToChoosingInputCandidate(option, input, true);
            }

            return keys;
        };
    }

    private Key<?>[][] switchToChoosingInputCandidate(
            Keyboard.KeyFactory.Option option, CharInput input, boolean pageUp
    ) {
        ChoosingInputCandidateData data;
        if (this.state.type == State.Type.ChoosingInputCandidate) {
            data = (ChoosingInputCandidateData) this.state.data;

            if (pageUp) {
                data.nextPage();
            } else {
                data.prevPage();
            }
        } else {
            data = new ChoosingInputCandidateData(input.candidates().size(), KeyTable.inputCandidateKeysPageSize());
            this.state = new State(State.Type.ChoosingInputCandidate, data);
        }

        return KeyTable.inputCandidateKeys(option, handMode(), data.getPageStart(), input.candidates());
    }
}
