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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard;

import java.util.function.Supplier;

import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.SymbolKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.SymbolEmojiKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.state.SymbolChooseStateData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.dict.Symbol;
import org.crazydan.studio.app.ime.kuaizi.dict.SymbolGroup;

/**
 * {@link Type#Symbol 符号键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-10
 */
public class SymbolKeyboard extends InputCandidateKeyboard {

    public SymbolKeyboard(PinyinDict dict) {
        super(dict);
    }

    @Override
    public Type getType() {return Type.Symbol;}

    @Override
    public void start(KeyboardContext context) {
        InputList inputList = context.inputList;
        Input selected = inputList.getSelected();
        boolean onlyPair = selected != null && !selected.isGap() && ((CharInput) selected).hasPair();

        start_Symbol_Choosing(context, onlyPair);
    }

    private SymbolEmojiKeyTable createKeyTable(KeyboardContext context) {
        KeyTableConfig keyTableConf = createKeyTableConfig(context);

        return SymbolEmojiKeyTable.create(keyTableConf);
    }

    @Override
    public KeyFactory buildKeyFactory(KeyboardContext context) {
        SymbolEmojiKeyTable keyTable = createKeyTable(context);

        SymbolChooseStateData stateData = this.state.data();

        return () -> keyTable.createSymbolKeys(stateData.getGroup(), stateData.isOnlyPair(), stateData.getPageStart());
    }

    @Override
    protected void on_InputCandidate_Choose_Doing_PagingKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        boolean continuous = true;
        switch (msg.type) {
            case SingleTap_Key: {
                continuous = false;
                show_InputChars_Input_Popup(context);
            }
            // Note: 长按显示提示气泡有基类处理
            case LongPress_Key_Tick: {
                play_SingleTick_InputAudio(context);

                do_Single_Symbol_Inputting(context, continuous);
                break;
            }
        }
    }

    @Override
    protected void on_InputCandidate_Choose_Doing_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        CtrlKey key = context.key();
        if (CtrlKey.Type.Toggle_Symbol_Group.match(key)) {
            play_SingleTick_InputAudio(context);

            CtrlKey.Option<SymbolGroup> option = key.option();
            do_Symbol_Choosing(context, option.value);
        }
    }

    /** 进入符号选择状态，并处理符号翻页 */
    private void start_Symbol_Choosing(KeyboardContext context, boolean onlyPair) {
        InputList inputList = context.inputList;
        CharInput pending = inputList.getPending();

        SymbolEmojiKeyTable keyTable = createKeyTable(context);
        int pageSize = keyTable.getSymbolKeysPageSize();

        SymbolChooseStateData stateData = new SymbolChooseStateData(pending, pageSize, onlyPair);
        this.state = new State(State.Type.InputCandidate_Choose_Doing, stateData);

        SymbolGroup group = SymbolGroup.latin;
        if (context.keyboardPrevType == Type.Pinyin) {
            group = SymbolGroup.han;
        }

        do_Symbol_Choosing(context, group);
    }

    private void do_Symbol_Choosing(KeyboardContext context, SymbolGroup group) {
        SymbolChooseStateData stateData = this.state.data();
        stateData.setGroup(group);

        fire_InputCandidate_Choose_Doing(context);
    }

    private void do_Single_Symbol_Inputting(KeyboardContext context, boolean continuousInput) {
        SymbolKey key = context.key();
        InputList inputList = context.inputList;

        boolean directInputting = inputList.isEmpty();
        boolean isPairKey = key.symbol instanceof Symbol.Pair;

        if (!directInputting) {
            if (isPairKey) {
                prepare_for_PairSymbol_Inputting(context, (Symbol.Pair) key.symbol);

                confirm_InputList_Pending(context);

                // Note：配对符号输入后不再做连续输入，退出当前键盘
                exit_Keyboard(context);
            } else {
                do_Single_CharKey_Replace_or_NewPending_Inputting(context);
            }
            return;
        }

        CharInput pending = inputList.newPending();
        if (isPairKey) {
            Symbol.Pair symbol = (Symbol.Pair) key.symbol;

            prepare_for_PairSymbol_Inputting(context, symbol);
        } else {
            pending.appendKey(key);
            pending.clearPair();
        }

        // 直接提交输入
        commit_InputList(context, false, false, isPairKey);

        // Note：非连续输入的情况下，配对符号输入后不再做连续输入，退出当前键盘
        if (isPairKey && !continuousInput) {
            exit_Keyboard(context);
        }
    }

    private void prepare_for_PairSymbol_Inputting(KeyboardContext context, Symbol.Pair symbol) {
        InputList inputList = context.inputList;
        String left = symbol.left;
        String right = symbol.right;

        prepare_for_PairKey_Inputting(inputList,
                                      () -> SymbolKey.build(Symbol.single(left)),
                                      () -> SymbolKey.build(Symbol.single(right)));
    }

    public static void prepare_for_PairKey_Inputting(
            InputList inputList, Supplier<Key> left, Supplier<Key> right
    ) {
        Input selected = inputList.getSelected();

        Key leftKey = left.get();
        Key rightKey = right.get();

        // 用新的配对符号替换原配对符号
        if (!selected.isGap() && ((CharInput) selected).hasPair()) {
            CharInput pending = inputList.newPending();

            CharInput leftInput = pending;
            CharInput rightInput = ((CharInput) selected).getPair();

            int rightInputIndex = inputList.indexOf(rightInput);
            // 交换左右顺序
            if (inputList.getSelectedIndex() > rightInputIndex) {
                leftInput = rightInput;
                rightInput = pending;
            }

            leftInput.replaceLastKey(leftKey);
            rightInput.replaceLastKey(rightKey);
        } else {
            // 对于输入修改，若为非空的待输入，也不是符号输入，则对其做配对符号包裹
            boolean wrapSelected = !selected.isGap() && !inputList.hasEmptyPending() && !selected.isSymbol();
            if (wrapSelected) {
                // 选中被包裹输入的左侧 Gap
                inputList.confirmPendingAndSelectPrevious();
            } else {
                inputList.confirmPendingAndSelectNext();
            }

            CharInput leftInput = inputList.getPending();
            leftInput.appendKey(leftKey);

            if (wrapSelected) {
                // 选中被包裹输入的右侧 Gap：左符号+Gap+被包裹输入+右符号
                inputList.confirmPendingAndSelectByOffset(3);
            } else {
                inputList.confirmPendingAndSelectNext();
            }

            CharInput rightInput = inputList.getPending();
            rightInput.appendKey(rightKey);

            // 绑定配对符号的关联：由任意一方发起绑定即可
            rightInput.setPair(leftInput);

            // 确认右侧的配对输入，并将光标移动到 右配对输入 的左侧 Gap 位置以确保光标在配对符号的中间位置
            inputList.confirmPendingAndSelectPrevious();
        }
    }
}
