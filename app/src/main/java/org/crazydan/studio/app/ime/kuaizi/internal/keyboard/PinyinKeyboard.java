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
    public KeyFactory getKeyFactory() {
        return option -> KeyTable.getKeys(option, getHandMode());
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
                getInputList().initPending();

                CharInput input = (CharInput) getInputList().getCursor().getPending();
                input.appendKey(key);

                onInputtingChars(input, key, null, true);
                break;
            }
            case FingerMove: {
                if (this.state.type == State.Type.Inputting && this.slidingInput) {
                    CharInput input = (CharInput) getInputList().getCursor().getPending();
                    if (key != input.getCurrentKey()) {
                        input.appendKey(key);

                        Key<?> closed = ((FingerMoveMsgData) data).closed;
                        onInputtingChars(input, key, closed, true);
                    }
                }
                break;
            }
            case KeyLongPressEnd: {
                if (this.state.type == State.Type.Inputting) {
                    this.slidingInput = false;

                    CharInput input = (CharInput) getInputList().getCursor().getPending();
                    if (!input.hasExtraCandidates()) {
                        confirmInputPending();
                    } else {
                        KeyFactory keyFactory = option -> switchToChoosingInputCandidate(option, input, true);
                        InputMsgData idata = new InputtingCharsMsgData(input.getKeys(), key, null, keyFactory);

                        onInputMsg(InputMsg.InputtingChars, idata);
                    }
                }
                break;
            }
            case KeyClick: {
                if (this.state.type != State.Type.Inputting) {
                    this.state = new State(State.Type.Inputting);
                    getInputList().initPending();
                } else if (getInputList().getCursor().getPending() == null) {
                    getInputList().initPending();
                }

                CharInput input = (CharInput) getInputList().getCursor().getPending();
                input.appendKey(key);

                onInputtingChars(input, key, null, false);
                break;
            }
        }
    }

    private void onCtrlKeyMsg(KeyMsg msg, KeyMsgData data) {
        CtrlKey key = (CtrlKey) data.target;

        switch (msg) {
            case KeyClick: {
                if (this.state.type == State.Type.ChoosingInputCandidate) {
                    switch (key.getType()) {
                        case DropInput: {
                            getInputList().dropPending();
                            confirmInputPending();
                            break;
                        }
                        case ToggleInputTongue: {
                            CharInput input = (CharInput) getInputList().getCursor().getPending();
                            String s = String.join("", input.getChars());
                            if (s.startsWith("sh") || s.startsWith("ch") || s.startsWith("zh")) {
                                input.getKeys().remove(1);
                            } else if (s.startsWith("s") || s.startsWith("c") || s.startsWith("z")) {
                                input.getKeys().add(1, KeyTable.charKey("h"));
                            }

                            List<InputWord> candidateWords = this.pinyinCharTree.findCandidateWords(input.getChars())
                                                                                .stream()
                                                                                .map(InputWord::from)
                                                                                .sorted()
                                                                                .collect(Collectors.toList());
                            input.word(candidateWords.isEmpty() ? null : candidateWords.get(0));
                            input.candidates(candidateWords);

                            this.state = new State(State.Type.Init);
                            KeyFactory keyFactory = option -> switchToChoosingInputCandidate(option, input, true);
                            InputMsgData idata = new InputtingCharsMsgData(input.getKeys(), key, null, keyFactory);

                            onInputMsg(InputMsg.InputtingChars, idata);
                            break;
                        }
                        case ToggleInputRhyme: {
                            CharInput input = (CharInput) getInputList().getCursor().getPending();
                            String s = String.join("", input.getChars());
                            if (s.endsWith("eng") || s.endsWith("ing") || s.endsWith("ang")) {
                                input.getKeys().remove(input.getKeys().size() - 1);
                            } else if (s.endsWith("en") || s.endsWith("in") || s.endsWith("an")) {
                                input.getKeys().add(input.getKeys().size(), KeyTable.charKey("g"));
                            }

                            List<InputWord> candidateWords = this.pinyinCharTree.findCandidateWords(input.getChars())
                                                                                .stream()
                                                                                .map(InputWord::from)
                                                                                .sorted()
                                                                                .collect(Collectors.toList());
                            input.word(candidateWords.isEmpty() ? null : candidateWords.get(0));
                            input.candidates(candidateWords);

                            this.state = new State(State.Type.Init);
                            KeyFactory keyFactory = option -> switchToChoosingInputCandidate(option, input, true);
                            InputMsgData idata = new InputtingCharsMsgData(input.getKeys(), key, null, keyFactory);

                            onInputMsg(InputMsg.InputtingChars, idata);
                            break;
                        }
                        case ToggleInputNL: {
                            CharInput input = (CharInput) getInputList().getCursor().getPending();
                            String s = String.join("", input.getChars());
                            if (s.startsWith("n")) {
                                input.getKeys().remove(0);
                                input.getKeys().add(0, KeyTable.charKey("l"));
                            } else if (s.startsWith("l")) {
                                input.getKeys().remove(0);
                                input.getKeys().add(0, KeyTable.charKey("n"));
                            }

                            List<InputWord> candidateWords = this.pinyinCharTree.findCandidateWords(input.getChars())
                                                                                .stream()
                                                                                .map(InputWord::from)
                                                                                .sorted()
                                                                                .collect(Collectors.toList());
                            input.word(candidateWords.isEmpty() ? null : candidateWords.get(0));
                            input.candidates(candidateWords);

                            this.state = new State(State.Type.Init);
                            KeyFactory keyFactory = option -> switchToChoosingInputCandidate(option, input, true);
                            InputMsgData idata = new InputtingCharsMsgData(input.getKeys(), key, null, keyFactory);

                            onInputMsg(InputMsg.InputtingChars, idata);
                            break;
                        }
                    }
                }
                break;
            }
            case FingerFling: {
                if (this.state.type == State.Type.ChoosingInputCandidate) {
                    CharInput input = (CharInput) getInputList().getCursor().getPending();
                    KeyFactory keyFactory = option -> switchToChoosingInputCandidate(option,
                                                                                     input,
                                                                                     ((FingerFlingMsgData) data).up);
                    InputMsgData idata = new InputtingCharsMsgData(input.getKeys(), key, null, keyFactory);

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
                    InputWord word = key.getWord();
                    CharInput input = (CharInput) getInputList().getCursor().getPending();
                    input.word(word);

                    confirmInputPending();
                }
                break;
            }
        }
    }

    private void onInputtingChars(CharInput input, CharKey currentKey, Key<?> closedKey, boolean needToProcessPinyin) {
        List<String> nextChars = new ArrayList<>();
        if (needToProcessPinyin) {
            nextChars = this.pinyinCharTree.findNextChars(input.getChars());

            List<InputWord> candidateWords = this.pinyinCharTree.findCandidateWords(input.getChars())
                                                                .stream()
                                                                .map(InputWord::from)
                                                                .sorted()
                                                                .collect(Collectors.toList());
            input.word(candidateWords.isEmpty() ? null : candidateWords.get(0));
            input.candidates(candidateWords);
        } else {
            input.word(null);
            input.candidates(new ArrayList<>());
        }

        KeyFactory keyFactory = createKeyFactoryByInput(input, nextChars);
        InputMsgData data = new InputtingCharsMsgData(input.getKeys(), currentKey, closedKey, keyFactory);

        onInputMsg(InputMsg.InputtingChars, data);
    }

    private void confirmInputPending() {
        this.state = new State(State.Type.Inputting);
        this.slidingInput = false;

        CharInput input = (CharInput) getInputList().getCursor().getPending();
        if (input == null || !input.hasWord()) {
            // 无有效拼音，则丢弃输入
            getInputList().dropPending();
        } else {
            getInputList().confirmPending();
        }
        onInputMsg(InputMsg.InputtingCharsDone, new CommonInputMsgData(getKeyFactory()));
    }

    private KeyFactory createKeyFactoryByInput(CharInput input, List<String> nextChars) {
        return option -> {
            Key<?>[][] keys = getKeyFactory().create(option);
            // 有后继字母，则仅显示后继字母按键
            if (!nextChars.isEmpty()) {
                KeyTable.traverseKeys(keys, key -> {
                    key.hide();
                    if (key instanceof CharKey && nextChars.contains(((CharKey) key).getText())) {
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
            data = new ChoosingInputCandidateData(input.getWordCandidates().size(),
                                                  KeyTable.getInputCandidateKeysPageSize());
            this.state = new State(State.Type.ChoosingInputCandidate, data);
        }

        return KeyTable.getInputCandidateKeys(option, getHandMode(), data.getPageStart(), input);
    }
}
