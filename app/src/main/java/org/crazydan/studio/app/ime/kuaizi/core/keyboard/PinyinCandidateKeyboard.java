/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.core.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.completion.InputCompletion;
import org.crazydan.studio.app.ime.kuaizi.core.input.completion.InputCompletions;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.EmojiWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.PinyinCandidateKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.state.PinyinCandidateAdvanceFilterStateData;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.state.PinyinCandidateChooseStateData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinCharsTree;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;

/**
 * {@link Type#Pinyin_Candidate 拼音候选字键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-10
 */
public class PinyinCandidateKeyboard extends InputCandidateKeyboard {

    @Override
    public Type getType() {return Type.Pinyin_Candidate;}

    @Override
    public void start(KeyboardContext context) {
        start_InputCandidate_Choosing(context, false);
    }

    private PinyinCandidateKeyTable createKeyTable(KeyboardContext context) {
        KeyTableConfig keyTableConf = createKeyTableConfig(context);

        return PinyinCandidateKeyTable.create(keyTableConf);
    }

    @Override
    public KeyFactory buildKeyFactory(KeyboardContext context) {
        PinyinCandidateKeyTable keyTable = createKeyTable(context);

        switch (this.state.type) {
            case InputCandidate_Choose_Doing: {
                PinyinCandidateChooseStateData stateData = this.state.data();

                PinyinDict dict = context.dict.usePinyinDict();
                PinyinCharsTree charsTree = dict.getPinyinCharsTree();

                return () -> keyTable.createKeys(charsTree,
                                                 stateData.input,
                                                 stateData.getSpells(),
                                                 stateData.getPagingData(),
                                                 stateData.getPageStart(),
                                                 stateData.getFilter());
            }
            case InputCandidate_Advance_Filter_Doing: {
                PinyinCandidateAdvanceFilterStateData stateData = this.state.data();

                return () -> keyTable.createAdvanceFilterKeys(stateData.getSpells(),
                                                              stateData.getPagingData(),
                                                              stateData.getPageStart(),
                                                              stateData.getFilter());
            }
        }
        return null;
    }

    @Override
    protected boolean try_On_Common_UserKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        if (super.try_On_Common_UserKey_Msg(context, msg)) {
            return true;
        }

        // 仅需处理高级过滤状态的操作
        if (this.state.type != State.Type.InputCandidate_Advance_Filter_Doing) {
            return false;
        }

        Key key = context.key();
        if (msg.type == UserKeyMsgType.FingerFlipping) {
            // Note: 高级过滤的翻页与普通翻页共享逻辑代码
            on_InputCandidate_Choose_Doing_FingerFlipping_Msg(context, msg);
        }
        // Note: 部首和拼音过滤均为 CtrlKey 类型
        else if (key instanceof CtrlKey) {
            on_InputCandidate_Advance_Filter_Doing_CtrlKey_Msg(context, msg);
        }
        return true;
    }

    @Override
    protected void on_InputCandidate_Choose_Doing_PagingKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        // Note: 先做消息过滤，因为，不同的消息会携带不同类型的按键
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        InputWordKey key = context.key();
        InputList inputList = context.inputList;

        if (key.disabled) {
            return;
        }

        play_SingleTick_InputAudio(context);
        show_InputChars_Input_Popup(context);

        InputWord word = key.word;
        // 候选字列表中的表情作为新增插入，不对当前候选字做替换
        if (word instanceof EmojiWord) {
            inputList.confirmPendingAndSelectNext();

            // Note：补充输入按键，以避免待输入为空
            InputWordKey wordKey = InputWordKey.build(word);
            CharInput pending = inputList.getCharPending();

            pending.appendKey(wordKey);
        }

        // 更新最新的待输入
        CharInput pending = inputList.getCharPending();
        pending.setWord(word);

        // 确认候选字
        confirm_InputList_Pending_InputCandidate(context);
    }

    @Override
    protected void on_InputCandidate_Choose_Doing_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        CtrlKey key = context.key();
        switch (key.type) {
            case ConfirmInput: {
                play_SingleTick_InputAudio(context);

                confirm_InputList_Pending_InputCandidate(context);
                break;
            }
            case Toggle_Pinyin_Spell: {
                play_SingleTick_InputAudio(context);

                InputList inputList = context.inputList;
                CharInput pending = inputList.getCharPending();

                CtrlKey.Option<CtrlKey.PinyinToggleMode> option = key.option();
                switch (option.value) {
                    case ng_end:
                        pending.toggle_Pinyin_NG_Ending();
                        break;
                    case nl_start:
                        pending.toggle_Pinyin_NL_Starting();
                        break;
                    case zcs_start:
                        pending.toggle_Pinyin_SCZ_Starting();
                        break;
                }

                start_InputCandidate_Choosing(context, true);
                break;
            }
            case Filter_PinyinCandidate_by_Spell: {
                play_SingleTick_InputAudio(context);
                show_InputChars_Input_Popup(context);

                PinyinCandidateChooseStateData stateData = this.state.data();

                PinyinWord.Filter filter = stateData.getFilter();
                filter.addSpellByKey(key);

                stateData.updateFilter(filter);

                fire_InputCandidate_Choose_Doing(context);
                break;
            }
            case Filter_PinyinCandidate_advance: {
                play_SingleTick_InputAudio(context);

                start_InputCandidate_Advance_Filtering(context);
                break;
            }
        }
    }

    // ===================== Start: 候选字选择 =====================

    /** 进入候选字选择状态，并处理候选字翻页 */
    private void start_InputCandidate_Choosing(KeyboardContext context, boolean pinyinChanged) {
        InputList inputList = context.inputList;
        CharInput pending = inputList.getCharPending();

        PinyinCandidateKeyTable keyTable = createKeyTable(context);
        int pageSize = keyTable.getKeysPageSize();
        int bestCandidatesTop = keyTable.getBestCandidatesCount();
        int bestEmojisTop = pageSize - bestCandidatesTop;

        PinyinDict dict = context.dict.usePinyinDict();
        Map<Integer, InputWord> candidateMap = dict.getCandidates(pending);
        List<InputWord> allCandidates = new ArrayList<>(candidateMap.values());

        List<Integer> topBestCandidateIds = dict.getTopBestCandidateIds(pending, bestCandidatesTop);
        List<InputWord> topBestCandidates = topBestCandidateIds.stream()
                                                               .map(candidateMap::get)
                                                               .collect(Collectors.toList());

        // 拼音修正后，需更新其自动确定的候选字
        if (pinyinChanged) {
            determine_NotConfirmed_InputWord(dict, pending);
        }

        // 当前输入确定的拼音字放在最前面
        if (!topBestCandidates.contains(pending.getWord())) {
            topBestCandidates.add(0, pending.getWord());

            topBestCandidates = CollectionUtils.subList(topBestCandidates, 0, bestCandidatesTop);
        }

        // Note：以最新确定的输入候选字做为表情的关键字查询条件
        List<PinyinWord> emojiKeywords = inputList.getPinyinPhraseWordsFrom(pending);
        List<InputWord> topBestEmojis = dict.findTopBestEmojisMatchedPhrase(emojiKeywords, bestEmojisTop);
        topBestCandidates.addAll(topBestEmojis);

        if (!topBestCandidates.isEmpty()) {
            int allCandidatesCount = allCandidates.size();

            // 最佳候选字不再重复出现在普通候选字列表
            allCandidates.removeAll(topBestCandidates);

            // 若候选字列表总共只有一页，则合并最佳候选字，并确保最佳候选字在最前面位置
            if (allCandidatesCount <= pageSize) {
                allCandidates.addAll(0, topBestCandidates);

                allCandidates = reorder_TopBestCandidates_in_One_Page(allCandidates, bestCandidatesTop, pageSize);
            } else {
                topBestCandidates = reorder_TopBestCandidates_in_One_Page(topBestCandidates,
                                                                          bestCandidatesTop,
                                                                          pageSize);
                allCandidates.addAll(0, topBestCandidates);
            }
        }

        PinyinCandidateChooseStateData stateData = new PinyinCandidateChooseStateData(pending, allCandidates, pageSize);
        this.state = new State(State.Type.InputCandidate_Choose_Doing, stateData);

        fire_InputCandidate_Choose_Doing(context);
    }

    /** 确认待输入的候选字。若存在下一个拼音输入，则自动切换到对该输入的候选字选择，否则，选中相邻的输入 */
    private void confirm_InputList_Pending_InputCandidate(KeyboardContext context) {
        InputList inputList = context.inputList;

        CharInput pending = inputList.getCharPending();
        pending.confirmWord();

        inputList.confirmPendingAndSelectNext();

        fire_InputCandidate_Choose_Done(context, pending);

        // 继续选择下一个拼音输入的候选字
        Input selected = inputList.selectNextFirstMatched(CharInput::isPinyin);
        boolean hasNextPinyin = selected != null;

        if (hasNextPinyin) {
            PinyinDict dict = context.dict.usePinyinDict();
            predict_NotConfirmed_Phrase_InputWords(dict, inputList, (CharInput) selected, 1, false);
        } else {
            selected = inputList.getSelected();
        }

        if (!CharInput.isPinyin(selected)) {
            exit_Keyboard(context);
        } else {
            choose_InputList_Input(context, selected);
        }
    }

    /**
     * 在一页内重新排序最佳候选字和表情符号列表，
     * 确保表情符号和候选字各自独占特定区域，
     * 并填充 <code>null</code> 以占满一页
     */
    private List<InputWord> reorder_TopBestCandidates_in_One_Page(
            List<InputWord> candidates, int wordCount, int pageSize
    ) {
        List<InputWord> results = new ArrayList<>(pageSize);

        int emojiCount = pageSize - wordCount;
        List<InputWord> emojis = new ArrayList<>(emojiCount);
        candidates.forEach((candidate) -> {
            if (candidate instanceof EmojiWord) {
                emojis.add(candidate);
            } else {
                results.add(candidate);
            }
        });

        CollectionUtils.fillToSize(results, null, wordCount);

        // Note：在候选字不够一页时，最佳候选字会与剩余的候选字合并，
        // 故而可能会超过最佳候选字的显示数量，因此，需更新表情符号可填充数
        emojiCount = pageSize - results.size();
        CollectionUtils.fillToSize(emojis, null, emojiCount);

        results.addAll(emojis);

        return results;
    }

    // ===================== End: 候选字选择 =====================

    // ====================== Start: 候选字的高级过滤 =========================

    private void start_InputCandidate_Advance_Filtering(KeyboardContext context) {
        InputList inputList = context.inputList;
        CharInput pending = inputList.getCharPending();

        PinyinCandidateChooseStateData prevStateData = this.state.data();
        PinyinWord.Filter filter = prevStateData.getFilter();

        PinyinCandidateKeyTable keyTable = createKeyTable(context);
        int pageSize = keyTable.getAdvanceFilterKeysPageSize();

        PinyinCandidateAdvanceFilterStateData stateData = new PinyinCandidateAdvanceFilterStateData(pending,
                                                                                                    prevStateData.getCandidates(),
                                                                                                    pageSize);
        stateData.updateFilter(filter);

        State state = new State(State.Type.InputCandidate_Advance_Filter_Doing, stateData, this.state);
        change_State_To(context, state);

        fire_InputCandidate_Choose_Doing(context);
    }

    /** 高级过滤状态对 {@link CtrlKey} 的处理 */
    private void on_InputCandidate_Advance_Filter_Doing_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        CtrlKey key = context.key();
        PinyinCandidateAdvanceFilterStateData stateData = this.state.data();
        switch (key.type) {
            case Filter_PinyinCandidate_by_Spell:
            case Filter_PinyinCandidate_by_Radical: {
                play_SingleTick_InputAudio(context);
                show_InputChars_Input_Popup(context);

                update_InputCandidate_Advance_Filter(stateData, key);

                fire_InputCandidate_Choose_Doing(context);
                break;
            }
            case Confirm_PinyinCandidate_Filter: {
                play_SingleTick_InputAudio(context);

                PinyinCandidateChooseStateData prevStateData = this.state.previous.data();
                prevStateData.updateFilter(stateData.getFilter());

                exit_Keyboard(context);
                break;
            }
        }
    }

    /** 更新高级过滤的过滤条件 */
    private void update_InputCandidate_Advance_Filter(PinyinCandidateAdvanceFilterStateData stateData, CtrlKey key) {
        PinyinWord.Filter filter = stateData.getFilter();

        boolean resetPage = true;
        switch (key.type) {
            case Filter_PinyinCandidate_by_Spell: {
                filter.addSpellByKey(key);
                break;
            }
            case Filter_PinyinCandidate_by_Radical: {
                // Note: 部首条件的增减不会影响部首的分页结果，故而，不重置分页
                resetPage = false;
                filter.addRadicalByKey(key);
                break;
            }
        }

        stateData.updateFilter(filter, resetPage);
    }

    // ====================== End: 候选字的高级过滤 =========================

    @Override
    protected void after_InputList_Selected_Deleted(KeyboardContext context) {
        predict_NotConfirmed_Phrase_InputWords_with_Completions(context, null, this::fire_Input_Completion_Create_Done);
    }

    // ================================ Start: 共用静态接口 ==================================

    /** {@link #predict_NotConfirmed_Phrase_InputWords 输入短语预测}并{@link #create_Phrase_InputWord_Completions 构造输入补全} */
    protected static void predict_NotConfirmed_Phrase_InputWords_with_Completions(
            KeyboardContext context, Consumer<KeyboardContext> beforeCompletions,
            Consumer<KeyboardContext> afterCompletions
    ) {
        InputList inputList = context.inputList;
        CharInput pending = inputList.getCharPending();

        // TODO 需要更好的输入预测机制，当前的方案对预测输入并无很大帮助，仅用于做输入补全的功能测试
        // Note: top 参数大于 1 时，可启用输入补全
        PinyinDict dict = context.dict.usePinyinDict();
        List<List<InputWord>> bestPhrases = predict_NotConfirmed_Phrase_InputWords(dict, inputList, pending, 1, true);

        if (beforeCompletions != null) {
            beforeCompletions.accept(context);
        }

        create_Phrase_InputWord_Completions(inputList, bestPhrases, () -> afterCompletions.accept(context));
    }

    /**
     * 预测 <code>input</code> 所在拼音短语中 未确认输入 的字
     * <p/>
     * <code>input</code> 也将根据预测的短语结果而进行调整
     *
     * @return 返回应用最佳预测结果后的剩余（<code>top - 1</code>）的短语预测结果
     */
    protected static List<List<InputWord>> predict_NotConfirmed_Phrase_InputWords(
            PinyinDict dict, InputList inputList, CharInput currentInput, int top, boolean forInputting
    ) {
        List<CharInput> inputs = inputList.getPinyinPhraseInputWhichContains(currentInput);
        List<List<InputWord>> bestPhrases = dict.findTopBestMatchedPhrase(inputs,
                                                                          forInputting ? null : currentInput,
                                                                          top);

        List<InputWord> bestPhrase = CollectionUtils.first(bestPhrases);
        if (bestPhrase == null) {
            return List.of();
        }

        for (int i = 0; i < bestPhrase.size(); i++) {
            CharInput target = inputs.get(i);
            // 已确认的输入，则不做处理
            if (target.isWordConfirmed()) {
                continue;
            }

            InputWord word = bestPhrase.get(i);
            // Note: 非拼音位置的输入，其拼音字为 null，不覆盖其 word
            if (word != null) {
                target.setWord(word);
            }
        }

        bestPhrases.remove(0);
        return bestPhrases;
    }

    /**
     * 确认 未确认输入 的候选字
     * <p/>
     * 在滑屏输入中实时调用
     */
    protected static void determine_NotConfirmed_InputWord(PinyinDict dict, CharInput input) {
        Integer pinyinCharsId = dict.getPinyinCharsTree().getCharsId(input);

        InputWord word = null;
        if (pinyinCharsId != null //
            && (!input.isWordConfirmed() //
                || !input.hasWord() //
                || (input.getWord() instanceof PinyinWord
                    && !Objects.equals(((PinyinWord) input.getWord()).spell.charsId, pinyinCharsId)) //
            ) //
        ) {
            word = dict.getFirstBestCandidate(pinyinCharsId);
        }

        input.setWord(word);
    }

    /**
     * 创建短语的输入补全
     * <p/>
     * 仅针对待输入确认后，光标位置依然在 {@link #predict_NotConfirmed_Phrase_InputWords}
     * 所处理的{@link InputList#getPinyinPhraseInputWhichContains 短语内}
     */
    protected static void create_Phrase_InputWord_Completions(
            InputList inputList, List<List<InputWord>> bestPhrases, Runnable fireCompletions
    ) {
        if (bestPhrases.isEmpty() || !inputList.isGapSelected()) {
            return;
        }

        Input gap = inputList.getSelected();
        // 在短语预测后会确认待输入，因此，需以新的待输入重新查找其所在的输入短语
        List<CharInput> inputs = inputList.getPinyinPhraseInputWhichContains(gap);

        Input first = CollectionUtils.first(inputs);
        Input last = CollectionUtils.last(inputs);
        int firstIndex = inputList.getInputIndex(first);
        int lastIndex = inputList.getInputIndex(last);

        // 若光标在预测短语的首尾，则需更新输入短语的起止位置
        int gapIndex = inputList.getInputIndex(gap);
        if (gapIndex < firstIndex) {
            first = gap;
        } else if (gapIndex > lastIndex) {
            last = gap;
        }

        InputCompletions completions = inputList.newPhraseWordCompletions(first, last);

        bestPhrases.forEach(phrase -> {
            InputCompletion completion = new InputCompletion();
            for (int i = 0; i < phrase.size(); i++) {
                CharInput target = inputs.get(i);
                target = (CharInput) target.copy();

                InputWord word = phrase.get(i);
                if (word != null) {
                    target.setWord(word);
                }

                completion.inputs.add(target);
            }

            completions.add(completion);
        });

        fireCompletions.run();
    }

    // ================================ End: 共用静态接口 ==================================
}
