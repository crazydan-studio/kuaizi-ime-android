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
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.PagingStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.SlippingInputStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserFingerFlippingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserLongPressTickMsgData;
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
            case InputCandidate_Choosing: {
                ChoosingInputCandidateStateData stateData = (ChoosingInputCandidateStateData) this.state.data;
                CharInput input = stateData.getInput();

                return () -> KeyTable.createPinyinInputWordKeys(createKeyTableConfigure(),
                                                                input,
                                                                stateData.getPagingData(),
                                                                stateData.getPageStart(),
                                                                stateData.getPageSize(),
                                                                stateData.getStrokes());
            }
            default: {
                return () -> KeyTable.createPinyinKeys(createKeyTableConfigure());
            }
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
                on_SlippingInput_UserKeyMsg(msg, key, data);
                break;
            }
            case InputCandidate_Choosing: {
                if (msg == UserKeyMsg.FingerFlipping) {
                    on_InputCandidates_PageFlippingMsg(msg, key, data);
                } else if (key instanceof InputWordKey) {
                    on_InputCandidates_InputWordKeyMsg(msg, (InputWordKey) key, data);
                } else if (key instanceof CtrlKey) {
                    on_InputCandidates_CtrlKeyMsg(msg, (CtrlKey) key, data);
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
                    play_InputtingDoubleTick_Audio(key);

                    this.state = new State(State.Type.Input_Slipping, new SlippingInputStateData());

                    CharInput input = getInputList().newPending();
                    on_SlippingInput(input, key);
                }
                break;
            }
            case KeySingleTap: {
                // 单字符输入
                play_InputtingSingleTick_Audio(key);

                do_SingleKey_Inputting(key);
                break;
            }
            case KeyDoubleTap: {
                // 双击前已触发两次单击，故，不需再播放按键提示音
                //play_InputtingSingleTick_Audio(key);

                // TODO 双击切换字母按键的策略需调整
                do_ReplacementKey_Inputting(key);
                break;
            }
        }
    }

    private void on_SlippingInput_UserKeyMsg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        switch (msg) {
            case FingerMoving: {
                // 添加拼音后继字母
                CharInput input = getInputList().getPending();

                // Note: 拼音不存在重复字母相邻的情况
                Key<?> lastKey = input.getLastKey();
                if (key instanceof CharKey && !key.isSameWith(lastKey)) {
                    play_InputtingDoubleTick_Audio(key);

                    on_SlippingInput(input, key);
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

                confirm_InputChars_and_Waiting_Input();
                break;
            }
        }
    }

    private void onCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
    }

    private void on_InputCandidates_InputWordKeyMsg(UserKeyMsg msg, InputWordKey key, UserKeyMsgData data) {
        if (msg == UserKeyMsg.KeySingleTap) {// 确认候选字
            play_InputtingSingleTick_Audio(key);

            InputWord word = key.getWord();
            getInputList().getPending().setWord(word);

            onConfirmSelectedInputCandidate();
        }
    }

    private void on_InputCandidates_CtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeyLongPressTick:
                // 过滤的笔画数 -1
                if (key.getType() == CtrlKey.Type.Filter_PinyinInputCandidate_stroke) {
                    if (((UserLongPressTickMsgData) data).tick % 4 == 0) {
                        CtrlKey.TextOption option = (CtrlKey.TextOption) key.getOption();

                        do_InputCandidate_Filtering_ByStroke(key, option.value(), -1);
                    }
                    break;
                }
            case KeySingleTap: {
                // Note：笔画过滤按键音效由过滤接口处理
                if (key.getType() != CtrlKey.Type.Filter_PinyinInputCandidate_stroke) {
                    play_InputtingSingleTick_Audio(key);
                }

                CharInput input = getInputList().getPending();
                // 丢弃或变更拼音
                switch (key.getType()) {
                    case DropInput: {
                        getInputList().deleteSelected();
                        confirm_InputChars_and_Waiting_Input();
                        break;
                    }
                    case ConfirmInput: {
                        onConfirmSelectedInputCandidate();
                        break;
                    }
                    case Toggle_PinyinInput_spell: {
                        CtrlKey.PinyinSpellToggleOption option = (CtrlKey.PinyinSpellToggleOption) key.getOption();
                        switch (option.value()) {
                            case ng:
                                input.toggle_Pinyin_NG_Ending();
                                break;
                            case nl:
                                input.toggle_Pinyin_NL_Starting();
                                break;
                            case zcs_h:
                                input.toggle_Pinyin_SCZ_Starting();
                                break;
                        }

                        start_InputCandidate_Choosing(input, true);
                        break;
                    }
                    case Filter_PinyinInputCandidate_stroke: {
                        CtrlKey.TextOption option = (CtrlKey.TextOption) key.getOption();

                        do_InputCandidate_Filtering_ByStroke(key, option.value(), 1);
                        break;
                    }
                }
                break;
            }
        }
    }

    private void on_InputCandidates_PageFlippingMsg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        flip_Page_for_PagingState((PagingStateData<?>) this.state.data, (UserFingerFlippingMsgData) data);

        do_InputCandidate_Choosing();
    }

    private void do_SingleKey_Inputting(CharKey key) {
        if (getInputList().hasEmptyPending()) {
            getInputList().newPending();
        }

        CharInput input = getInputList().getPending();
        do_SingleKey_Inputting(input, key);
    }

    private void do_ReplacementKey_Inputting(CharKey key) {
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

    private void do_SingleKey_Inputting(CharInput input, CharKey key) {
        switch (key.getType()) {
            // 若为标点、表情符号，则直接确认输入，不支持连续输入其他字符
            case Emoji:
            case Symbol: {
                boolean isEmpty = getInputList().isEmpty();
                getInputList().newPending().appendKey(key);

                if (!isEmpty) {
                    confirm_InputChars_and_Waiting_Input();
                } else {
                    // 单个标点、表情则直接提交输入
                    commit_InputList_and_Waiting_Input();
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
    private void on_SlippingInput(CharInput input, Key<?> currentKey) {
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

    // <<<<<<<<< 对输入候选字的操作

    /** 进入候选字选择状态，并处理候选字翻页 */
    private void start_InputCandidate_Choosing(CharInput input, boolean inputChanged) {
        Map<String, InputWord> candidateMap = getInputCandidateWords(input);

        List<InputWord> allCandidates = new ArrayList<>(candidateMap.values());
        List<InputWord> topBestCandidates = getTopBestInputCandidateWords(input, 18);

        int pageSize = KeyTable.getPinyinInputKeysPageSize();
        if (!topBestCandidates.isEmpty() && topBestCandidates.size() < candidateMap.size()) {
            // 最佳 top 候选字独立占用第一页，不够一页时以 null 占位
            CollectionUtils.fillToSize(topBestCandidates, null, pageSize);

            allCandidates.addAll(0, topBestCandidates);
        } else if (topBestCandidates.size() == candidateMap.size()) {
            allCandidates = topBestCandidates;
        }

        if (inputChanged) {
            determineNotConfirmedInputWord(input, () -> topBestCandidates);
        }

        ChoosingInputCandidateStateData stateData = new ChoosingInputCandidateStateData(input, allCandidates, pageSize);
        this.state = new State(State.Type.InputCandidate_Choosing, stateData);

        do_InputCandidate_Choosing();
    }

    private void do_InputCandidate_Filtering_ByStroke(Key<?> key, String strokeCode, int strokeIncrement) {
        ChoosingInputCandidateStateData stateData = (ChoosingInputCandidateStateData) this.state.data;
        if (stateData.addStroke(strokeCode, strokeIncrement)) {
            play_InputtingSingleTick_Audio(key);
        }

        do_InputCandidate_Choosing();
    }

    private void do_InputCandidate_Choosing() {
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

        onChoosingInputMsg(selected);
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

            // Note：可能存在用户数据与内置字典数据不一致的情况
            if (word != null) {
                target.setWord(word);
            }
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
    // >>>>>>>>>>

    // <<<<<<<<< 对输入列表的操作

    /** 在输入列表中选中拼音输入的候选字 */
    @Override
    protected boolean do_Choosing_Input_in_InputList(CharInput input) {
        // 仅当待输入为拼音时才进入候选字模式
        if (input.isPinyin()) {
            start_InputCandidate_Choosing(input, false);
            return true;
        }
        return false;
    }
    // >>>>>>>>>
}
