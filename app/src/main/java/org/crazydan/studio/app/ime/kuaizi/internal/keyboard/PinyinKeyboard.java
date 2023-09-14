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
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.PinyinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.ChoosingInputCandidateStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.PagingStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.SlippingInputStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserFingerFlippingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserSingleTapMsgData;
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
        PinyinKeyTable keyTable = PinyinKeyTable.create(createKeyTableConfigure());

        switch (this.state.type) {
            case Input_Slipping: {
                SlippingInputStateData stateData = ((SlippingInputStateData) this.state.data);

                return () -> keyTable.createNextCharKeys(stateData.getLevel0Key().getText(),
                                                         stateData.getLevel1Key() != null ? stateData.getLevel1Key()
                                                                                                     .getText() : null,
                                                         stateData.getLevel2Key() != null ? stateData.getLevel2Key()
                                                                                                     .getText() : null,
                                                         stateData.getLevel1NextChars(),
                                                         stateData.getLevel2NextChars());
            }
            case InputCandidate_Choosing: {
                ChoosingInputCandidateStateData stateData = (ChoosingInputCandidateStateData) this.state.data;
                CharInput input = stateData.getInput();

                return () -> keyTable.createInputCandidateKeys(input,
                                                               stateData.getPagingData(),
                                                               stateData.getStrokes(),
                                                               stateData.getPageStart());
            }
            case InputList_Committing_Option_Choosing: {
                return keyTable::createInputListCommittingOptionKeys;
            }
            default: {
                return keyTable::createKeys;
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
                } else if (key instanceof InputWordKey && !key.isDisabled()) {
                    on_InputCandidates_InputWordKeyMsg(msg, (InputWordKey) key, data);
                } else if (key instanceof CtrlKey) {
                    on_InputCandidates_CtrlKeyMsg(msg, (CtrlKey) key, data);
                }
                break;
            }
            case InputList_Committing_Option_Choosing: {
                if (key instanceof CtrlKey) {
                    on_InputList_Committing_Option_CtrlKeyMsg(msg, (CtrlKey) key, data);
                }
                break;
            }
            default: {
                if (key instanceof CharKey && !key.isDisabled()) {
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

                    start_SlippingInput(key);
                }
                break;
            }
            case KeySingleTap: {
                // 单字符输入
                play_InputtingSingleTick_Audio(key);

                start_SingleKey_Inputting(key, (UserSingleTapMsgData) data, false);
                break;
            }
        }
    }

    private void onCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        if (msg == UserKeyMsg.KeyLongPressStart) {
            if (key.getType() == CtrlKey.Type.Commit_InputList) {
                play_InputtingDoubleTick_Audio(key);

                start_InputList_Committing_Option_Choosing();
            }
        }
    }

    private void on_SlippingInput_UserKeyMsg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        switch (msg) {
            case FingerMoving: {
                // 添加拼音后继字母
                CharInput pending = getInputList().getPending();

                // Note: 拼音不存在重复字母相邻的情况
                Key<?> lastKey = pending.getLastKey();
                if (key instanceof CharKey //
                    && !key.isDisabled() //
                    && !key.isSameWith(lastKey)) {
                    play_InputtingDoubleTick_Audio(key);

                    do_SlippingInput(pending, key);
                }
                break;
            }
            case FingerMovingEnd: {
                CharInput pending = getInputList().getPending();

                // 无候选字的输入，视为无效输入，直接丢弃
                if (!pending.hasWord()) {
                    getInputList().dropPending();
                } else {
                    determineNotConfirmedInputWordsBefore(pending);
                }

                // 修改字符输入后，光标需后移以继续输入其他字符
                confirm_CharInput_Pending_and_MoveTo_NextGapInput_then_Waiting_Input();
                break;
            }
        }
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
        if (msg != UserKeyMsg.KeySingleTap) {
            return;
        }

        CharInput pending = getInputList().getPending();
        // 丢弃或变更拼音
        switch (key.getType()) {
            case DropInput: {
                play_InputtingSingleTick_Audio(key);

                getInputList().deleteSelected();
                confirm_Pending_and_Waiting_Input();
                break;
            }
            case ConfirmInput: {
                play_InputtingSingleTick_Audio(key);
                onConfirmSelectedInputCandidate();
                break;
            }
            case Toggle_PinyinInput_spell: {
                play_InputtingSingleTick_Audio(key);

                CtrlKey.PinyinSpellToggleOption option = (CtrlKey.PinyinSpellToggleOption) key.getOption();
                switch (option.value()) {
                    case ng:
                        pending.toggle_Pinyin_NG_Ending();
                        break;
                    case nl:
                        pending.toggle_Pinyin_NL_Starting();
                        break;
                    case zcs_h:
                        pending.toggle_Pinyin_SCZ_Starting();
                        break;
                }

                start_InputCandidate_Choosing(pending, true);
                break;
            }
            case Filter_PinyinInputCandidate_stroke: {
                do_InputCandidate_Filtering_ByStroke(key, 1);
                break;
            }
        }
    }

    private void on_InputCandidates_PageFlippingMsg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        UserFingerFlippingMsgData flippingData = (UserFingerFlippingMsgData) data;

        // 减去过滤的笔画数
        if (key instanceof CtrlKey && ((CtrlKey) key).getType() == CtrlKey.Type.Filter_PinyinInputCandidate_stroke) {
            do_InputCandidate_Filtering_ByStroke((CtrlKey) key, -1);
            return;
        }

        // 翻页
        flip_Page_for_PagingState((PagingStateData<?>) this.state.data, flippingData);
        do_InputCandidate_Choosing();
    }

    // >>>>>>>>> 滑屏输入
    private void start_SlippingInput(Key<?> key) {
        this.state = new State(State.Type.Input_Slipping, new SlippingInputStateData());

        CharInput pending = getInputList().newPending();
        do_SlippingInput(pending, key);
    }

    private void do_SlippingInput(CharInput input, Key<?> currentKey) {
        SlippingInputStateData stateData = ((SlippingInputStateData) this.state.data);
        Key.Level currentKeyLevel = currentKey.getLevel();

        // 添加后继字母，
        switch (currentKeyLevel) {
            case level_0: {
                input.appendKey(currentKey);

                Collection<String> level1NextChars = this.pinyinDict.findNextChar(Key.Level.level_1,
                                                                                  currentKey.getText());

                stateData.setLevel0Key(currentKey);

                stateData.setLevel1Key(null);
                stateData.setLevel1NextChars(level1NextChars);

                stateData.setLevel2Key(null);
                stateData.setLevel2NextChars(new ArrayList<>());
                break;
            }
            case level_1: {
                input.replaceKeyAfterLevel(Key.Level.level_0, currentKey);

                String startChar = stateData.getLevel0Key().getText();
                startChar += currentKey.getText();

                Collection<String> nextChars = this.pinyinDict.findNextChar(Key.Level.level_2, startChar);
                List<String> level2NextChars = nextChars.stream().sorted().collect(Collectors.toList());

                stateData.setLevel1Key(currentKey);

                stateData.setLevel2Key(null);
                stateData.setLevel2NextChars(level2NextChars);
                break;
            }
            case level_2: {
                // Note：第二级后继字母已包含第一级后继字母，故而，直接替换第 0 级之后的按键
                input.replaceKeyAfterLevel(Key.Level.level_0, currentKey);

                stateData.setLevel2Key(currentKey);
                break;
            }
        }

        // 并确定候选字
        determineNotConfirmedInputWord(input);

        fire_InputChars_Inputting(getKeyFactory(), currentKey);
    }
    // >>>>>>>>>>>>

    // <<<<<<<<< 对输入候选字的操作

    /** 进入候选字选择状态，并处理候选字翻页 */
    private void start_InputCandidate_Choosing(CharInput input, boolean inputChanged) {
        Map<String, InputWord> candidateMap = getInputCandidateWords(input);

        List<InputWord> allCandidates = new ArrayList<>(candidateMap.values());
        List<InputWord> topBestCandidates = getTopBestInputCandidateWords(input, 18);

        PinyinKeyTable keyTable = PinyinKeyTable.create(createKeyTableConfigure());
        int pageSize = keyTable.getInputCandidateKeysPageSize();

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

    private void do_InputCandidate_Filtering_ByStroke(CtrlKey key, int strokeIncrement) {
        ChoosingInputCandidateStateData stateData = (ChoosingInputCandidateStateData) this.state.data;

        CtrlKey.TextOption option = (CtrlKey.TextOption) key.getOption();
        if (stateData.addStroke(option.value(), strokeIncrement)) {
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

    // >>>>>>>>> 对输入列表 提交选项 的操作
    private void start_InputList_Committing_Option_Choosing() {
        this.state = new State(State.Type.InputList_Committing_Option_Choosing);

        end_InputChars_Inputting();
    }

    private void on_InputList_Committing_Option_CtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeySingleTap: {
                if (key.getType() == CtrlKey.Type.Commit_InputList_Option) {
                    play_InputtingSingleTick_Audio(key);

                    CtrlKey.CommitInputListOption.Option option
                            = ((CtrlKey.CommitInputListOption) key.getOption()).value();

                    Input.Option oldInputOption = getInputList().getOption();
                    oldInputOption = oldInputOption != null ? oldInputOption : new Input.Option(null, false);

                    Input.Option newInputOption = null;
                    switch (option) {
                        case only_pinyin: {
                            InputWord.NotationType notationType = oldInputOption.wordNotationType;
                            if (notationType == InputWord.NotationType.replacing) {
                                notationType = null;
                            } else {
                                notationType = InputWord.NotationType.replacing;
                            }

                            newInputOption = new Input.Option(notationType, oldInputOption.wordVariantUsed);
                            break;
                        }
                        case with_pinyin: {
                            InputWord.NotationType notationType = oldInputOption.wordNotationType;
                            if (notationType == InputWord.NotationType.following) {
                                notationType = null;
                            } else {
                                notationType = InputWord.NotationType.following;
                            }

                            newInputOption = new Input.Option(notationType, oldInputOption.wordVariantUsed);
                            break;
                        }
                        case switch_simple_trad: {
                            newInputOption = new Input.Option(oldInputOption.wordNotationType,
                                                              !oldInputOption.wordVariantUsed);
                            break;
                        }
                    }
                    getInputList().setOption(newInputOption);

                    end_InputChars_Inputting();
                }
                break;
            }
            case KeyLongPressStart: {
                if (key.getType() == CtrlKey.Type.Commit_InputList) {
                    play_InputtingDoubleTick_Audio(key);

                    getInputList().setOption(null);

                    end_InputChars_Inputting_and_Waiting_Input();
                }
                break;
            }
        }
    }
    // <<<<<<<<<

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
