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
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.BestCandidateWords;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.ChoosingInputCandidateStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.ChoosingSymbolEmojiStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.SlippingInputStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCommonMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserFingerSlippingMsgData;
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
        switch (this.state.type) {
            case Input_Candidate_Choosing: {
                ChoosingInputCandidateStateData stateData = (ChoosingInputCandidateStateData) this.state.data;
                CharInput input = stateData.getInput();

                return () -> KeyTable.createInputCandidateWordKeys(createKeyTableConfigure(),
                                                                   input,
                                                                   stateData.getCandidates(),
                                                                   stateData.getPageStart(),
                                                                   stateData.getPageSize());
            }
            case SymbolEmoji_Choosing: {
                ChoosingSymbolEmojiStateData stateData = (ChoosingSymbolEmojiStateData) this.state.data;

                return () -> KeyTable.createSymbolKeys(createKeyTableConfigure(),
                                                       stateData.getSymbols(),
                                                       stateData.getPageStart(),
                                                       stateData.getPageSize());
            }
            default: {
                return () -> KeyTable.createPinyinKeys(createKeyTableConfigure());
            }
        }
    }

    @Override
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        switch (this.state.type) {
            case Input_Waiting:
            case Input_Candidate_Choosing:
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
            case Input_Slipping: {
                onSlippingInputUserKeyMsg(msg, key, data);
                break;
            }
            case Input_Candidate_Choosing: {
                if (msg == UserKeyMsg.FingerSlipping) {
                    onInputCandidatesPageSlippingMsg(msg, key, data);
                } else if (key instanceof InputWordKey) {
                    onInputCandidatesKeyMsg(msg, (InputWordKey) key, data);
                } else if (key instanceof CtrlKey) {
                    onInputCandidatesCtrlKeyMsg(msg, (CtrlKey) key, data);
                }
                break;
            }
            case SymbolEmoji_Choosing: {
                if (msg == UserKeyMsg.FingerSlipping) {
                    on_ChoosingSymbolEmoji_PageSlippingMsg(msg, key, data);
                } else if (key instanceof CharKey) {
                    on_ChoosingSymbolEmoji_CharKeyMsg(msg, (CharKey) key, data);
                } else if (key instanceof CtrlKey) {
                    on_ChoosingSymbolEmoji_CtrlKeyMsg(msg, (CtrlKey) key, data);
                }
                break;
            }
            default: {
                if (key instanceof CharKey) {
                    onCharKeyMsg(msg, (CharKey) key, data);
                } else if (key instanceof CtrlKey) {
                    onCtrlKeyMsg(msg, (CtrlKey) key, data);
                }
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
        List<InputWord> emojis = getInputList().getEmojis();

        this.pinyinDict.saveUsedData(phrases, emojis);
    }

    private void onCharKeyMsg(UserKeyMsg msg, CharKey key, UserKeyMsgData data) {
        switch (msg) {
            case FingerMovingStart: {
                // 开始滑屏输入
                if (key.getType() == CharKey.Type.Alphabet) {
                    this.state = new State(State.Type.Input_Slipping, new SlippingInputStateData());

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
            case KeySingleTap: {
                switch (key.getType()) {
                    case SwitchToSymbolKeyboard:
                        play_InputtingSingleTick_Audio(key);

                        do_SymbolEmoji_Choosing(key);
                        break;
                }
                break;
            }
        }
    }

    private void onInputCandidatesKeyMsg(UserKeyMsg msg, InputWordKey key, UserKeyMsgData data) {
        switch (msg) {
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
            case KeyDoubleTap: // 双击继续触发第二次单击操作，以便于支持连续点击并执行对应操作
            case KeySingleTap: {
                play_InputtingSingleTick_Audio(key);

                String stroke = null;
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

                        onNewChoosingInputCandidate(input, true);
                        break;
                    }
                    case ToggleInputSpell_nl: {
                        input.toggle_Pinyin_NL_Starting();

                        onNewChoosingInputCandidate(input, true);
                        break;
                    }
                    case ToggleInputSpell_ng: {
                        input.toggle_Pinyin_NG_Ending();

                        onNewChoosingInputCandidate(input, true);
                        break;
                    }
                    case FilterInputCandidate_stroke_heng:
                        stroke = "1";
                        break;
                    case FilterInputCandidate_stroke_shu:
                        stroke = "2";
                        break;
                    case FilterInputCandidate_stroke_pie:
                        stroke = "3";
                        break;
                    case FilterInputCandidate_stroke_na:
                        stroke = "4";
                        break;
                    case FilterInputCandidate_stroke_zhe:
                        stroke = "5";
                        break;
                }

                if (stroke != null) {
                    onFilterInputCandidateByStroke(input, stroke);
                }
                break;
            }
        }
    }

    private void onInputCandidatesPageSlippingMsg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        // 候选字翻页
        Motion motion = ((UserFingerSlippingMsgData) data).motion;
        boolean pageUp = motion.direction == Motion.Direction.up || motion.direction == Motion.Direction.left;
        CharInput input = getInputList().getPending();

        // Note: 在该函数中根据实际是否有上下翻页来确定是否播放翻页音效
        onChoosingInputCandidate(input, pageUp, false);
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
            case Emoji:
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
    private void onNewChoosingInputCandidate(CharInput input, boolean inputChanged) {
        // Note: 复位状态，以便于做新状态的初始化，而不是利用原状态
        this.state = new State(State.Type.Input_Waiting);

        onChoosingInputCandidate(input, false, inputChanged);
    }

    /** 进入候选字选择状态，并处理候选字翻页 */
    private void onChoosingInputCandidate(CharInput input, boolean pageUp, boolean inputChanged) {
        ChoosingInputCandidateStateData stateData;
        if (this.state.type == State.Type.Input_Candidate_Choosing) {
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
            List<InputWord> topBestCandidates = getTopBestInputCandidateWords(input, 18);

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

            stateData = new ChoosingInputCandidateStateData(input, allCandidates, pageSize);
            this.state = new State(State.Type.Input_Candidate_Choosing, stateData);

            if (inputChanged) {
                determineNotConfirmedInputWord(input, () -> topBestCandidates);
            }
        }

        onFilterInputCandidateByStroke(input, null);
    }

    private void onFilterInputCandidateByStroke(CharInput input, String stroke) {
        ChoosingInputCandidateStateData stateData = (ChoosingInputCandidateStateData) this.state.data;
        stateData.addStroke(stroke);

        InputMsgData data = new InputCharsInputtingMsgData(getKeyFactory(), null);
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
     * 确定 <code>input</code> 的 前序 未确认输入 的最佳候选字
     * <p/>
     * <code>input</code> 也将根据前序确定的候选字而进行调整
     * <p/>
     * 在滑屏输入完成后，对未确认前序的候选字按匹配的最佳短语进行调整，
     * 或者，在前序候选字确认后，自动对选中的下一个输入的候选字进行调整
     */
    private void determineNotConfirmedInputWordsBefore(CharInput input) {
        // Note: top 取 0 以避免查询单字
        BestCandidateWords best = getTopBestCandidateWords(input, 0);

        String[] phrase = CollectionUtils.first(best.phrases);
        if (phrase == null || phrase.length < 2) {
            return;
        }

        List<CharInput> inputs = CollectionUtils.last(getInputList().getPinyinPhraseInputsBefore(input));
        if (inputs == null || inputs.size() + 1 < phrase.length) {
            return;
        }

        inputs.add(input);
        // Note: phrase 为倒序匹配，故，前序 输入集 需倒置
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

    /**
     * 确认 未确认输入 的候选字
     * <p/>
     * 在滑屏输入中实时调用
     */
    private void determineNotConfirmedInputWord(CharInput input) {
        determineNotConfirmedInputWord(input, () -> getTopBestInputCandidateWords(input, 1));
    }

    /** 在滑屏输入中，以及拼音纠正切换中被调用 */
    private void determineNotConfirmedInputWord(CharInput input, Supplier<List<InputWord>> topBestCandidatesGetter) {
        Map<String, InputWord> candidateMap = getInputCandidateWords(input);

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

    private List<InputWord> getTopBestInputCandidateWords(CharInput input, int top) {
        BestCandidateWords best = getTopBestCandidateWords(input, top);
        if (best.words.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, InputWord> candidateMap = getInputCandidateWords(input);
        return best.words.stream().map(candidateMap::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 获取输入的候选字列表
     * <p/>
     * 仅在首次获取时才查询拼音字典，后续的相同拼音都直接从缓存中取
     *
     * @return 不为 <code>null</code>
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

    private BestCandidateWords getTopBestCandidateWords(CharInput input, int top) {
        // 根据当前位置之前的输入确定当前位置的最佳候选字
        List<InputWord> prevPhrase = CollectionUtils.last(getInputList().getPinyinPhraseWordsBefore(input));

        return this.pinyinDict.findTopBestCandidateWords(input, top, prevPhrase, getConfig().isUserInputDataDisabled());
    }

    // <<<<<<<<< 对输入列表的操作

    /** 在输入列表中选中候选字 */
    private void onChoosingInputMsg(UserInputMsg msg, Input input, UserInputMsgData data) {
        getInputList().newPendingOn(input);

        CharInput pending = getInputList().getPending();
        // 仅当待输入为拼音时才进入候选字模式：输入过程中操作和处理的都是 pending
        if (pending.isPinyin()) {
            onNewChoosingInputCandidate(pending, false);
        }
        // 其余情况都是直接做替换输入
        else {
            confirm_InputChars();
        }
    }
    // >>>>>>>>>

    // <<<<<<<<<<< 对标点符号的操作
    private void do_SymbolEmoji_Choosing(CtrlKey key) {
        ChoosingSymbolEmojiStateData stateData = new ChoosingSymbolEmojiStateData(KeyTable.getSymbolKeysPageSize());
        stateData.setSymbols(SymbolKeyboard.chinese_symbols);

        this.state = new State(State.Type.SymbolEmoji_Choosing, stateData, this.state);

        do_Update_SymbolEmoji_Keys(false, false);
    }

    private void on_ChoosingSymbolEmoji_CharKeyMsg(UserKeyMsg msg, CharKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeySingleTap: {
                // 单字符输入，并切回原键盘
                play_InputtingSingleTick_Audio(key);

                do_Single_SymbolEmoji_Key_Inputting(key, true);
                break;
            }
            case KeyLongPressTick: {
                // 长按则连续输入
                play_InputtingSingleTick_Audio(key);

                do_Single_SymbolEmoji_Key_Inputting(key, false);
                break;
            }
        }
    }

    private void on_ChoosingSymbolEmoji_CtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        if (try_OnCtrlKeyMsg(msg, key, data)) {
            return;
        }

        switch (msg) {
            case KeySingleTap: {
                play_InputtingSingleTick_Audio(key);

                switch (key.getType()) {
                    case Exit: {
                        back_To_Previous_State();
                        break;
                    }
                    case ToggleSymbol_Locale_Zh_and_En: {
                        ChoosingSymbolEmojiStateData stateData = (ChoosingSymbolEmojiStateData) this.state.data;
                        if (stateData.getSymbols() == SymbolKeyboard.chinese_symbols) {
                            stateData.setSymbols(SymbolKeyboard.latin_symbols);
                        } else {
                            stateData.setSymbols(SymbolKeyboard.chinese_symbols);
                        }

                        do_Update_SymbolEmoji_Keys(false, false);
                        break;
                    }
                }
                break;
            }
        }
    }

    private void do_Update_SymbolEmoji_Keys(boolean doPaging, boolean pageUp) {
        ChoosingSymbolEmojiStateData stateData = (ChoosingSymbolEmojiStateData) this.state.data;
        if (doPaging) {
            boolean hasPage;
            if (pageUp) {
                hasPage = stateData.nextPage();
            } else {
                hasPage = stateData.prevPage();
            }

            if (hasPage) {
                play_InputPageFlipping_Audio();
            }
        }

        fireInputMsg(InputMsg.SymbolEmoji_Choosing, new InputCommonMsgData(getKeyFactory()));
    }

    private void do_Single_SymbolEmoji_Key_Inputting(CharKey key, boolean singleInputting) {
        boolean continuousInputting = !singleInputting || !getInputList().isEmpty();

        CharInput input = getInputList().newPending();
        if (key.isDoubleSymbol()) {
            // 拆分成两个 Key 插入到输入列表
            CharKey leftKey = KeyTable.symbolKey(key.getText().substring(0, 1));
            CharKey rightKey = KeyTable.symbolKey(key.getText().substring(1));

            input.appendKey(leftKey);
            input = getInputList().newPending();
            input.appendKey(rightKey);
            // 确认第二个 Key，并移动光标到其后的 Gap 位置
            getInputList().confirmPending();

            // 并将光标移动到成对标点之间的 Gap 位置
            getInputList().newPendingOn(getInputList().getSelectedIndex() - 2);

            // 再在成对标点之间等待新的输入
            continuousInputting = true;
        } else {
            input.appendKey(key);
        }

        if (continuousInputting) {
            if (singleInputting) {
                confirm_InputChars();
            } else {
                do_Update_SymbolEmoji_Keys(false, false);
            }
        } else {
            commit_InputList();
        }
    }

    private void on_ChoosingSymbolEmoji_PageSlippingMsg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        Motion motion = ((UserFingerSlippingMsgData) data).motion;
        boolean pageUp = motion.direction == Motion.Direction.up || motion.direction == Motion.Direction.left;

        do_Update_SymbolEmoji_Keys(true, pageUp);
    }
    // >>>>>>>>>>>
}
