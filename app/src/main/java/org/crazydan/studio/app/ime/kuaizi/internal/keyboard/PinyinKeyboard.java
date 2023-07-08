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

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinCharTree;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.pinyin.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.pinyin.State;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.KeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.KeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.FingerMoveMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.InputtingCharsMsgData;

/**
 * 汉语拼音{@link Keyboard 键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class PinyinKeyboard extends BaseKeyboard {
    private final Random random = new Random(new Date().getTime());

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
    public Key[][] keys(Orientation orientation) {
        return KeyTable.keys(orientation, handMode());
    }

    @Override
    public void onKeyMsg(KeyMsg msg, KeyMsgData data) {
        if (data.target instanceof CharKey) {
            onCharKeyMsg(msg, data);
        } else if (data.target instanceof CtrlKey) {
            onCtrlKeyMsg(msg, data);
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
                    onInputMsg(InputMsg.InputtingCharsDone, null);
                }
                break;
            }
        }
    }

    private void onCtrlKeyMsg(KeyMsg msg, KeyMsgData data) {
    }

    private void onInputtingChars(CharInput input, CharKey currentKey, Key closedKey) {
        List<InputWord> candidateWords = this.pinyinCharTree.findCandidateWords(input.chars())
                                                            .stream()
                                                            .map(w -> new InputWord(w.getValue(), w.getNotation()))
                                                            .collect(Collectors.toList());
        input.word(candidateWords.isEmpty() ? null : candidateWords.get(this.random.nextInt(candidateWords.size())));
        input.candidates(candidateWords);

        List<String> nextChars = this.pinyinCharTree.findNextChars(input.chars());
        InputMsgData data = new InputtingCharsMsgData(input.keys(), currentKey, closedKey, nextChars);

        onInputMsg(InputMsg.InputtingChars, data);
    }
}
