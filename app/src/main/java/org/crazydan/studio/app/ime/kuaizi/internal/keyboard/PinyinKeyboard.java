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
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.BestCandidateWords;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDictDB;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.ChoosingInputCandidateStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.SlippingInputStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputtingMsgData;
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
                onSlippingInputUserKeyMsg(msg, key, data);
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
        if (getConfig().isUserInputDataDisabled()) {
            return;
        }

        // 本地更新用户选字频率
        List<List<InputWord>> phrases = getInputList().getPinyinPhraseWords();
        this.pinyinDict.saveUsedPhrases(phrases);
    }

    private void onCharKeyMsg(UserKeyMsg msg, CharKey key, UserKeyMsgData data) {
        switch (msg) {
            case FingerMovingStart: {
                // 开始滑屏输入
                if (key.getType() == CharKey.Type.Alphabet) {
                    this.state = new State(State.Type.SlippingInput, new SlippingInputStateData());

                    CharInput input = getInputList().newPending();

                    play_InputtingDoubleTick_Audio(key);

                    onSlippingInput(input, key);
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

    private void onSlippingInputUserKeyMsg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        switch (msg) {
            case FingerMoving: {
                // 添加拼音后继字母
                CharInput input = getInputList().getPending();

                // Note: 拼音不存在重复字母相邻的情况
                Key<?> lastKey = input.getLastKey();
                if (key instanceof CharKey && !key.isSameWith(lastKey)) {
                    play_InputtingDoubleTick_Audio(key);

                    onSlippingInput(input, key);
                }
                break;
            }
            case FingerMovingEnd: {
                CharInput input = getInputList().getPending();

                // 无候选字的输入，视为无效输入，直接丢弃
                if (!input.hasWord()) {
                    getInputList().dropPending();
                } else {
                    determineNotConfirmedInputWordsBefore(input);
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
                // 丢弃或变更拼音
                switch (key.getType()) {
                    case CommitInputList: {
                        on_CtrlKey_CommitInputList(key);
                        break;
                    }
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
                        input.toggle_Pinyin_SCZ_Starting();

                        onNewChoosingInputCandidate(input);
                        break;
                    }
                    case ToggleInputSpell_nl: {
                        input.toggle_Pinyin_NL_Starting();

                        onNewChoosingInputCandidate(input);
                        break;
                    }
                    case ToggleInputSpell_ng: {
                        input.toggle_Pinyin_NG_Ending();

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

        Key<?> lastKey = input.getLastKey();
        if (!key.canReplaceTheKey(lastKey)) {
            return;
        }

        CharKey lastCharKey = (CharKey) lastKey;
        // Note: 在 Input 中的 key 可能不携带 replacement 信息，只能通过当前按键做判断
        String newKeyText = key.nextReplacement(lastCharKey.getText());

        CharKey newKey = KeyTable.alphabetKey(newKeyText);
        input.replaceLatestKey(lastCharKey, newKey);

        fire_and_Waiting_Continuous_InputChars_Inputting(key);
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
                input.appendKey(key);

                fire_and_Waiting_Continuous_InputChars_Inputting(key);
                break;
            }
        }
    }

    /** 滑屏输入 */
    private void onSlippingInput(CharInput input, Key<?> currentKey) {
        SlippingInputStateData stateData = ((SlippingInputStateData) this.state.data);
        Key.Level currentKeyLevel = stateData.getKeyLevel();

        if (currentKeyLevel == Key.Level.level_0) {
            input.appendKey(currentKey);

            Collection<String> level1NextChars = this.pinyinDict.findNextChar(Key.Level.level_1, currentKey.getText());
            stateData.setLevel1NextChars(level1NextChars);
            stateData.setLevel0Key(currentKey);

            stateData.nextKeyLevel();
        } else {
            // Note: 第 1 级后继按键仅在首次添加时采用追加方式，后续均为替换输入中的最后一个按键，
            // 即第 1/2 级按键均可相互替换，确保输入始终最多保留两个按键（第 0 级 + 第 1/2 级）
            if (input.getKeys().size() == 1) {
                input.appendKey(currentKey);
            } else {
                Key<?> lastKey = input.getLastKey();
                input.replaceLatestKey(lastKey, currentKey);
            }
        }

        List<String> level1NextChars = new ArrayList<>(stateData.getLevel1NextChars());
        List<String> level2NextChars = new ArrayList<>(stateData.getLevel2NextChars());

        Key<?> lastKey = input.getLastKey();
        if (currentKeyLevel != Key.Level.level_0) {
            // 输入的最后一个按键处于第 1 级，则查找并更新第 2 级后继字母按键
            if (level1NextChars.contains(lastKey.getText())) {
                String startChar = stateData.getLevel0Key().getText();
                startChar += lastKey.getText();

                Collection<String> nextChars = this.pinyinDict.findNextChar(Key.Level.level_2, startChar);

                level2NextChars = nextChars.stream().sorted().collect(Collectors.toList());
                stateData.setLevel2NextChars(level2NextChars);
            }

            // 第 1 级按键均不显示
            level1NextChars.clear();

            // 保持第 2 级字母按键（其按键动态生成）的位置不变，以避免出现闪动
            int lastKeyIndex = level2NextChars.indexOf(lastKey.getText());
            if (lastKeyIndex >= 0) {
                level2NextChars.set(lastKeyIndex, null);
            }
        }
        determineNotConfirmedInputWord(input);

        Key<?>[][] keys = KeyTable.createPinyinNextCharKeys(createKeyTableConfigure(),
                                                            stateData.getLevel0Key().getText(),
                                                            level1NextChars,
                                                            level2NextChars);
        fire_InputChars_Inputting(() -> keys, currentKey);
    }

    /** 重新进入候选字状态 */
    private void onNewChoosingInputCandidate(CharInput input) {
        // Note: 复位状态，以便于做新状态的初始化，而不是利用原状态
        this.state = new State(State.Type.Input_Waiting);

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
            Map<String, InputWord> candidateMap = getInputCandidateWords(input);

            List<InputWord> allCandidates = new ArrayList<>(candidateMap.values());
            List<InputWord> topBestCandidates = getTopBestInputCandidateWords(input);

            int pageSize = KeyTable.getInputCandidateKeysPageSize();
            if (!topBestCandidates.isEmpty() && topBestCandidates.size() < candidateMap.size()) {
                // 最佳 top 候选字独立占用第一页，不够一页时以 null 占位
                for (int i = topBestCandidates.size(); i < pageSize; i++) {
                    topBestCandidates.add(null);
                }
                allCandidates.addAll(0, topBestCandidates);
            } else if (topBestCandidates.size() == candidateMap.size()) {
                allCandidates = topBestCandidates;
            }

            stateData = new ChoosingInputCandidateStateData(allCandidates, pageSize);
            this.state = new State(State.Type.ChoosingInputCandidate, stateData);

            determineNotConfirmedInputWord(input, candidateMap, () -> topBestCandidates);
        }

        KeyFactory keyFactory = () -> KeyTable.createInputCandidateWordKeys(createKeyTableConfigure(),
                                                                            input,
                                                                            stateData.getCandidates(),
                                                                            stateData.getPageStart(),
                                                                            stateData.getPageSize());
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

        // Note：前序已确认，则继续自动确认下一个拼音输入
        if (selected != null && selected.isPinyin()) {
            determineNotConfirmedInputWordsBefore((CharInput) selected);
        }

        onChoosingInputMsg(null, selected, null);
    }

    /**
     * 确定 指定输入 的 前序 未确认输入 的最佳候选字
     * <p/>
     * 指定输入 也将根据前序确定的候选字而进行调整
     */
    private void determineNotConfirmedInputWordsBefore(CharInput input) {
        BestCandidateWords best = getTopBestCandidateWords(input);

        String[] phrase = CollectionUtils.first(best.phrases);
        if (phrase == null || phrase.length < 2) {
            return;
        }

        List<CharInput> inputs = CollectionUtils.last(getInputList().getPinyinPhraseInputsBefore(input));
        if (inputs == null || inputs.size() + 1 < phrase.length) {
            return;
        }

        inputs.add(input);
        // Note: phrase 为倒序匹配，故，前序输入集需倒置
        Collections.reverse(inputs);

        for (int i = 0; i < phrase.length; i++) {
            CharInput target = inputs.get(i);
            if (target.getWord().isConfirmed()) {
                continue;
            }

            String pinyinWordId = phrase[i];
            Map<String, InputWord> candidateMap = getInputCandidateWords(target);
            InputWord word = candidateMap.get(pinyinWordId);

            target.setWord(word);
        }
    }

    /** 确认 未确认输入 的候选字 */
    private void determineNotConfirmedInputWord(CharInput input) {
        Map<String, InputWord> candidateMap = getInputCandidateWords(input);

        determineNotConfirmedInputWord(input, candidateMap, () -> getTopBestInputCandidateWords(input));
    }

    private void determineNotConfirmedInputWord(
            CharInput input, Map<String, InputWord> candidateMap, Supplier<List<InputWord>> topBestCandidatesGetter
    ) {
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

    private List<InputWord> getTopBestInputCandidateWords(CharInput input) {
        Map<String, InputWord> candidateMap = getInputCandidateWords(input);
        BestCandidateWords best = getTopBestCandidateWords(input);

        return best.words.stream().map(candidateMap::get).collect(Collectors.toList());
    }

    /**
     * 获取输入的候选字列表
     * <p/>
     * 仅在首次获取时才查询拼音字典，后续的相同拼音都直接从缓存中取
     */
    private Map<String, InputWord> getInputCandidateWords(CharInput input) {
        Map<String, InputWord> words = getInputList().getCachedCandidateWords(input);

        if (words == null) {
            List<InputWord> candidates = this.pinyinDict.getCandidateWords(input);
            getInputList().cacheCandidateWords(input, candidates);

            words = getInputList().getCachedCandidateWords(input);
        }
        return words;
    }

    private BestCandidateWords getTopBestCandidateWords(CharInput input) {
        // 根据当前位置之前的输入确定当前位置的最佳候选字
        List<InputWord> prevInputWords = CollectionUtils.last(getInputList().getPinyinPhraseWordsBefore(input));

        return this.pinyinDict.findTopBestCandidateWords(input,
                                                         18,
                                                         prevInputWords,
                                                         getConfig().isUserInputDataDisabled());
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
