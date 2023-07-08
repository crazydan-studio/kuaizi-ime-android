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
import java.util.Arrays;
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
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.KeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.KeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.CommonInputMsgData;
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
    private State state = State.Init;
    private boolean slidingInput;

    public PinyinKeyboard(PinyinCharTree pinyinCharTree) {
        this.pinyinCharTree = pinyinCharTree;
    }

    @Override
    public void reset() {
        this.state = State.Init;
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
        } else if (data.target instanceof CtrlKey) {
            onCtrlKeyMsg(msg, data);
        } else if (data.target instanceof InputWordKey) {
            onInputWordKeyMsg(msg, data);
        }
    }

    private void onCharKeyMsg(KeyMsg msg, KeyMsgData data) {
        CharKey key = (CharKey) data.target;

        switch (msg) {
            case KeyLongPress: {
                this.state = State.Inputting;
                this.slidingInput = true;
                inputList().initPending();

                CharInput input = (CharInput) inputList().cursor().pending();
                input.append(key);

                onInputtingChars(input, key, null);
                break;
            }
            case FingerMove: {
                if (this.state == State.Inputting && this.slidingInput) {
                    CharInput input = (CharInput) inputList().cursor().pending();
                    if (key != input.currentKey()) {
                        input.append(key);

                        Key closed = ((FingerMoveMsgData) data).closed;
                        onInputtingChars(input, key, closed);
                    }
                }
                break;
            }
            case KeyLongPressEnd: {
                if (this.state == State.Inputting) {
                    this.slidingInput = false;
                    inputList().confirmPending();
                    onInputMsg(InputMsg.InputtingCharsDone, new CommonInputMsgData(keyFactory()));
                }
                break;
            }
        }
    }

    private void onCtrlKeyMsg(KeyMsg msg, KeyMsgData data) {
        CtrlKey key = (CtrlKey) data.target;

        switch (msg) {
            case FingerMove: {
                if (this.state == State.Inputting && this.slidingInput //
                    && key.type() == CtrlKey.Type.ChooseWord) {
                    CharInput input = (CharInput) inputList().cursor().pending();
                    Key closed = ((FingerMoveMsgData) data).closed;

                    KeyFactory keyFactory = option -> switchToChoosingInputCandidate(option, input);
                    InputMsgData idata = new InputtingCharsMsgData(input.keys(), null, closed, keyFactory);

                    onInputMsg(InputMsg.InputtingChars, idata);
                }
                break;
            }
        }
    }

    private void onInputWordKeyMsg(KeyMsg msg, KeyMsgData data) {
    }

    private void onInputtingChars(CharInput input, CharKey currentKey, Key closedKey) {
        List<InputWord> candidateWords = this.pinyinCharTree.findCandidateWords(input.chars())
                                                            .stream()
                                                            .map(w -> new InputWord(w.getValue(), w.getNotation()))
                                                            .collect(Collectors.toList());
        if (candidateWords.isEmpty()) {
            input.word(null);
            input.candidates(candidateWords);
        } else if (candidateWords.size() == 1) {
            input.word(candidateWords.get(0));
            input.candidates(new ArrayList<>());
        } else {
            input.word(candidateWords.get(0));
            input.candidates(candidateWords);
        }

        KeyFactory keyFactory = createKeyFactoryByInput(input);
        InputMsgData data = new InputtingCharsMsgData(input.keys(), currentKey, closedKey, keyFactory);

        onInputMsg(InputMsg.InputtingChars, data);
    }

    private KeyFactory createKeyFactoryByInput(CharInput input) {
        List<String> nextChars = this.pinyinCharTree.findNextChars(input.chars());

        return option -> {
            Key[][] keys = keyFactory().create(option);
            // 有后继字母：1. 拼音有效但可继续补充后继字母；2. 拼音无效且需要补充后继字母；
            if (!nextChars.isEmpty()) {
                // 仅显示后继字母按键
                KeyTable.traverseKeys(keys, key -> {
                    key.hide();
                    if (key instanceof CharKey && nextChars.contains(((CharKey) key).text())) {
                        key.show();
                    }
                });

                if (input.hasWord() && !input.candidates().isEmpty()) {
                    keys = Arrays.stream(keys).map(Key[]::clone).toArray(Key[][]::new);
                    // 增加显示“选字”按键，以提前结束拼音录入，在滑入“选字”按键后，进入选字模式
                    // TODO 根据左右手模式等调整按键坐标
                    keys[1][6] = KeyTable.ctrl_key_choose_word;
                }
            }
            // 无后继字母：1. 拼音有效且完整；2. 拼音无效；
            else if (input.hasWord() && !input.candidates().isEmpty()) {
                // 进入候选字模式
                keys = switchToChoosingInputCandidate(option, input);
            } else {
                // 输入结束，显示原始键盘
            }

            return keys;
        };
    }

    private Key[][] switchToChoosingInputCandidate(Keyboard.KeyFactory.Option option, CharInput input) {
        this.state = State.ChoosingInputCandidate;

        return KeyTable.inputCandidateKeys(option, handMode(), input.candidates());
    }
}
