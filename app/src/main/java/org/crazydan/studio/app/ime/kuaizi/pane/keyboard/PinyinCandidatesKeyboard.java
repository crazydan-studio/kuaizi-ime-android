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

package org.crazydan.studio.app.ime.kuaizi.pane.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinCharsTree;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.EmojiWord;
import org.crazydan.studio.app.ime.kuaizi.pane.input.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.keytable.PinyinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.CandidatePinyinWordAdvanceFilterDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.CandidatePinyinWordChooseDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.PagingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputCandidateChoosingMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.user.UserFingerFlippingMsgData;

/**
 * {@link Type#Pinyin_Candidates 拼音候选字键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-10
 */
public class PinyinCandidatesKeyboard extends BaseKeyboard {
    private final PinyinDict dict;

    public PinyinCandidatesKeyboard(PinyinDict dict) {
        this.dict = dict;
    }

    @Override
    public Type getType() {
        return Type.Pinyin_Candidates;
    }

    @Override
    public void start(InputList inputList) {
        start_InputCandidate_Choosing(inputList, inputList.getPending(), false);
    }

    @Override
    protected KeyFactory doGetKeyFactory() {
        PinyinKeyTable keyTable = PinyinKeyTable.create(createKeyTableConfig());

        switch (this.state.type) {
            case InputCandidate_Choose_Doing: {
                CandidatePinyinWordChooseDoingStateData stateData
                        = (CandidatePinyinWordChooseDoingStateData) this.state.data;
                CharInput input = stateData.getTarget();
                PinyinCharsTree charsTree = this.dict.getPinyinCharsTree();

                return () -> keyTable.createInputCandidateKeys(charsTree,
                                                               input,
                                                               stateData.getSpells(),
                                                               stateData.getPagingData(),
                                                               stateData.getPageStart(),
                                                               stateData.getFilter());
            }
            case InputCandidate_AdvanceFilter_Doing: {
                CandidatePinyinWordAdvanceFilterDoingStateData stateData
                        = (CandidatePinyinWordAdvanceFilterDoingStateData) this.state.data;

                return () -> keyTable.createInputCandidateAdvanceFilterKeys(stateData.getSpells(),
                                                                            stateData.getPagingData(),
                                                                            stateData.getPageStart(),
                                                                            stateData.getFilter());
            }
        }
        return null;
    }

    @Override
    public void onMsg(InputList inputList, UserKeyMsg msg) {
        if (try_OnUserKeyMsg(inputList, msg)) {
            return;
        }

        Key<?> key = msg.data.target;
        switch (this.state.type) {
            case InputCandidate_Choose_Doing: {
                if (msg.type == UserKeyMsgType.FingerFlipping) {
                    on_InputCandidate_Choose_Doing_PageFlipping_Msg(msg.data);
                } else if (key instanceof InputWordKey) {
                    on_InputCandidate_Choose_Doing_InputWordKey_Msg(inputList, msg, (InputWordKey) key);
                } else if (key instanceof CtrlKey) {
                    on_InputCandidate_Choose_Doing_CtrlKey_Msg(inputList, msg, (CtrlKey) key);
                }
                break;
            }
            case InputCandidate_AdvanceFilter_Doing: {
                if (msg.type == UserKeyMsgType.FingerFlipping) {
                    on_InputCandidate_AdvanceFilter_Doing_PageFlipping_Msg(msg.data);
                } else if (key instanceof CtrlKey) {
                    on_InputCandidate_AdvanceFilter_Doing_CtrlKey_Msg(msg, (CtrlKey) key);
                }
                break;
            }
        }
    }

    private void on_InputCandidate_Choose_Doing_InputWordKey_Msg(
            InputList inputList, UserKeyMsg msg, InputWordKey key
    ) {
        if (key.isDisabled()) {
            return;
        }

        // 确认候选字
        if (msg.type == UserKeyMsgType.SingleTap_Key) {
            play_SingleTick_InputAudio(key);
            show_InputChars_Input_Popup(key);

            InputWord word = key.getWord();

            // 候选字列表中的表情作为新增插入，不对当前候选字做替换
            if (word instanceof EmojiWord) {
                inputList.confirmPendingAndSelectNext();

                // Note：补充输入按键，以避免待输入为空
                InputWordKey wordKey = InputWordKey.create(word);
                inputList.getPending().appendKey(wordKey);
            }

            inputList.getPending().setWord(word);

            confirm_Selected_InputCandidate(inputList);
        }
    }

    private void on_InputCandidate_Choose_Doing_CtrlKey_Msg(InputList inputList, UserKeyMsg msg, CtrlKey key) {
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        CharInput pending = inputList.getPending();
        switch (key.getType()) {
            case DropInput: {
                play_SingleTick_InputAudio(key);

                delete_InputList_Selected(inputList, key);
                change_State_to_Init();
                break;
            }
            case ConfirmInput: {
                play_SingleTick_InputAudio(key);
                confirm_Selected_InputCandidate(inputList);
                break;
            }
            case Toggle_PinyinInput_spell: {
                play_SingleTick_InputAudio(key);

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

                start_InputCandidate_Choosing(inputList, pending, true);
                break;
            }
            case Filter_PinyinInputCandidate_by_Spell: {
                play_SingleTick_InputAudio(key);
                show_InputChars_Input_Popup(key);

                CandidatePinyinWordChooseDoingStateData stateData
                        = (CandidatePinyinWordChooseDoingStateData) this.state.data;

                CtrlKey.Option<?> option = key.getOption();
                PinyinWord.Spell value = (PinyinWord.Spell) option.value();
                PinyinWord.Filter filter = stateData.getFilter();

                filter.clear();
                if (!key.isDisabled()) {
                    filter.spells.add(value);
                }

                stateData.setFilter(filter);

                fire_InputCandidate_Choose_Doing(stateData.getTarget());
                break;
            }
            case Filter_PinyinInputCandidate_advance: {
                play_SingleTick_InputAudio(key);

                start_InputCandidate_Advance_Filtering(inputList, pending, key);
                break;
            }
        }
    }

    private void on_InputCandidate_Choose_Doing_PageFlipping_Msg(UserKeyMsgData data) {
        UserFingerFlippingMsgData flippingData = (UserFingerFlippingMsgData) data;

        // 翻页
        update_PagingStateData_by_UserKeyMsg((PagingStateData<?>) this.state.data, flippingData);

        fire_InputCandidate_Choose_Doing(((CandidatePinyinWordChooseDoingStateData) this.state.data).getTarget());
    }

    // <<<<<<<<< 对输入候选字的高级过滤
    private void start_InputCandidate_Advance_Filtering(InputList inputList, CharInput input, Key<?> key) {
        CandidatePinyinWordChooseDoingStateData prevStateData
                = (CandidatePinyinWordChooseDoingStateData) this.state.data;
        PinyinWord.Filter filter = prevStateData.getFilter();

        PinyinKeyTable keyTable = PinyinKeyTable.create(createKeyTableConfig(inputList));
        int pageSize = keyTable.getInputCandidateAdvanceFilterKeysPageSize();

        CandidatePinyinWordAdvanceFilterDoingStateData stateData = new CandidatePinyinWordAdvanceFilterDoingStateData(
                input,
                prevStateData.getCandidates(),
                pageSize);
        stateData.setFilter(filter);

        State state = new State(State.Type.InputCandidate_AdvanceFilter_Doing, stateData, this.state);
        change_State_To(key, state);

        fire_InputCandidate_Choose_Doing(input);
    }

    private void on_InputCandidate_AdvanceFilter_Doing_CtrlKey_Msg(UserKeyMsg msg, CtrlKey key) {
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        CandidatePinyinWordAdvanceFilterDoingStateData stateData
                = (CandidatePinyinWordAdvanceFilterDoingStateData) this.state.data;
        switch (key.getType()) {
            case Filter_PinyinInputCandidate_by_Spell:
            case Filter_PinyinInputCandidate_by_Radical: {
                play_SingleTick_InputAudio(key);
                show_InputChars_Input_Popup(key);

                CtrlKey.Option<?> option = key.getOption();
                PinyinWord.Filter filter = stateData.getFilter();

                switch (key.getType()) {
                    case Filter_PinyinInputCandidate_by_Spell: {
                        PinyinWord.Spell value = (PinyinWord.Spell) option.value();

                        filter.clear();
                        if (!key.isDisabled()) {
                            filter.spells.add(value);
                        }
                        break;
                    }
                    case Filter_PinyinInputCandidate_by_Radical: {
                        PinyinWord.Radical value = (PinyinWord.Radical) option.value();
                        if (key.isDisabled()) {
                            filter.radicals.remove(value);
                        } else {
                            filter.radicals.add(value);
                        }
                        break;
                    }
                }

                stateData.setFilter(filter);

                fire_InputCandidate_Choose_Doing(stateData.getTarget());
                break;
            }
            case Confirm_PinyinInputCandidate_Filters: {
                play_SingleTick_InputAudio(key);

                CandidatePinyinWordChooseDoingStateData prevStateData
                        = (CandidatePinyinWordChooseDoingStateData) this.state.previous.data;
                prevStateData.setFilter(stateData.getFilter());

                exit_Keyboard(key);
                break;
            }
        }
    }

    private void on_InputCandidate_AdvanceFilter_Doing_PageFlipping_Msg(UserKeyMsgData data) {
        UserFingerFlippingMsgData flippingData = (UserFingerFlippingMsgData) data;
        CandidatePinyinWordAdvanceFilterDoingStateData stateData
                = (CandidatePinyinWordAdvanceFilterDoingStateData) this.state.data;

        // 翻页
        update_PagingStateData_by_UserKeyMsg(stateData, flippingData);

        fire_InputCandidate_Choose_Doing(stateData.getTarget());
    }
    // >>>>>>>>>>>>

    private void fire_InputCandidate_Choose_Doing(CharInput input) {
        InputMsgData data = new InputCandidateChoosingMsgData(input);

        fire_InputMsg(InputMsgType.InputCandidate_Choose_Doing, data);
    }

    private void fire_InputCandidate_Choose_Done(CharInput input) {
        InputMsgData data = new InputCandidateChoosingMsgData(input);

        fire_InputMsg(InputMsgType.InputCandidate_Choose_Done, data);
    }

    private void confirm_Selected_InputCandidate(InputList inputList) {
        CharInput pending = inputList.getPending();
        pending.markWordConfirmed();

        inputList.confirmPendingAndSelectNext();

        fire_InputCandidate_Choose_Done(pending);

        // 继续选择下一个拼音输入的候选字
        Input<?> selected = inputList.selectNextFirstMatched(Input::isPinyin);
        boolean hasNextPinyin = selected != null;

        if (hasNextPinyin) {
            predict_NotConfirmed_Phrase_InputWords(this.dict, inputList, (CharInput) selected);
        } else {
            selected = inputList.getSelected();
        }

        start_Input_Choosing(inputList, selected);
    }

    /** 进入候选字选择状态，并处理候选字翻页 */
    private void start_InputCandidate_Choosing(InputList inputList, CharInput input, boolean inputPinyinChanged) {
        PinyinKeyTable keyTable = PinyinKeyTable.create(createKeyTableConfig(inputList));
        int pageSize = keyTable.getInputCandidateKeysPageSize();
        int bestCandidatesTop = keyTable.getBestCandidatesCount();
        int bestEmojisTop = pageSize - bestCandidatesTop;

        Map<Integer, InputWord> candidateMap = getInputCandidateWords(input);
        List<InputWord> allCandidates = new ArrayList<>(candidateMap.values());

        List<Integer> topBestCandidateIds = this.dict.getTopBestCandidatePinyinWordIds(input, bestCandidatesTop);
        List<InputWord> topBestCandidates = topBestCandidateIds.stream()
                                                               .map(candidateMap::get)
                                                               .collect(Collectors.toList());

        // 拼音修正后，需更新其自动确定的候选字
        if (inputPinyinChanged) {
            determine_NotConfirmed_InputWord(this.dict, input);
        }

        // 当前输入确定的拼音字放在最前面
        if (input.isPinyin() //
            && !topBestCandidates.contains(input.getWord()) //
        ) {
            topBestCandidates.add(0, input.getWord());

            topBestCandidates = CollectionUtils.subList(topBestCandidates, 0, bestCandidatesTop);
        }

        // Note：以最新确定的输入候选字做为表情的关键字查询条件
        List<InputWord> topBestEmojis = getTopBestEmojis(inputList, input, bestEmojisTop);
        topBestCandidates.addAll(topBestEmojis);

        if (!topBestCandidates.isEmpty()) {
            int allCandidatesCount = allCandidates.size();

            // 最佳候选字不再重复出现在普通候选字列表
            allCandidates.removeAll(topBestCandidates);

            // 若候选字列表总共只有一页，则合并最佳候选字，并确保最佳候选字在最前面位置
            if (allCandidatesCount <= pageSize) {
                allCandidates.addAll(0, topBestCandidates);

                allCandidates = reorder_TopBest_CandidateWords_and_Emojis_In_Page(allCandidates,
                                                                                  bestCandidatesTop,
                                                                                  pageSize);
            } else {
                topBestCandidates = reorder_TopBest_CandidateWords_and_Emojis_In_Page(topBestCandidates,
                                                                                      bestCandidatesTop,
                                                                                      pageSize);
                allCandidates.addAll(0, topBestCandidates);
            }
        }

        CandidatePinyinWordChooseDoingStateData stateData = new CandidatePinyinWordChooseDoingStateData(input,
                                                                                                        allCandidates,
                                                                                                        pageSize);
        this.state = new State(State.Type.InputCandidate_Choose_Doing, stateData);

        fire_InputCandidate_Choose_Doing(input);
    }

    /**
     * 获取输入的候选字列表
     *
     * @return 不为 <code>null</code>
     */
    private Map<Integer, InputWord> getInputCandidateWords(CharInput input) {
        String inputChars = input.getJoinedChars();
        return this.dict.getCandidatePinyinWords(inputChars);
    }

    private List<InputWord> getTopBestEmojis(InputList inputList, CharInput input, int top) {
        List<PinyinWord> phraseWords = inputList.getPinyinPhraseWordsFrom(input);

        return this.dict.findTopBestEmojisMatchedPhrase(phraseWords, top);
    }

    /**
     * 在一页内重新排序最佳候选字和表情符号列表，
     * 确保表情符号和候选字各自独占特定区域，
     * 并填充 <code>null</code> 以占满一页
     */
    private List<InputWord> reorder_TopBest_CandidateWords_and_Emojis_In_Page(
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

    /**
     * 预测 <code>input</code> 所在拼音短语中 未确认输入 的字
     * <p/>
     * <code>input</code> 也将根据预测的短语结果而进行调整
     */
    protected static void predict_NotConfirmed_Phrase_InputWords(
            PinyinDict dict, InputList inputList, CharInput input
    ) {
        List<CharInput> inputs = inputList.getPinyinPhraseInputWhichContains(input);
        List<List<InputWord>> bestPhrases = dict.findTopBestMatchedPhrase(inputs, 1);

        List<InputWord> bestPhrase = CollectionUtils.first(bestPhrases);
        if (bestPhrase == null) {
            return;
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
            && (!input.isWordConfirmed() || !input.hasWord() //
                || !Objects.equals(input.getWord().getSpell().charsId, pinyinCharsId) //
            ) //
        ) {
            word = dict.getFirstBestCandidatePinyinWord(pinyinCharsId);
        }
        input.setWord(word);
    }
}
