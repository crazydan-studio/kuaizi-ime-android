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

import java.util.function.Supplier;

import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
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
        // Note: 若选中输入为配对符号输入，则仅显示可选的配对符号列表
        boolean onlyPair = selected instanceof CharInput && ((CharInput) selected).hasPair();

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
            // Note: 长按显示提示气泡由基类处理
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
        CharInput pending = inputList.getCharPending();

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

        if (isPairKey) {
            Symbol.Pair symbol = (Symbol.Pair) key.symbol;
            prepare_for_PairSymbol_Inputting(context, symbol);

            confirm_InputList_Pending(context);

            // Note：非连续输入的情况下，配对符号输入后不再做连续输入，退出当前键盘
            if (!continuousInput) {
                exit_Keyboard(context);
            }
        }
        // 非直输，做替换输入
        else if (!directInputting) {
            do_Single_CharKey_Replace_or_NewPending_Inputting(context);
        }
        // 直输
        else {
            CharInput pending = inputList.newCharPending();
            pending.appendKey(key);
            pending.clearPair();

            commit_InputList(context, false, false, false);
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
        if (selected instanceof CharInput && ((CharInput) selected).hasPair()) {
            CharInput pending = inputList.newCharPending();

            CharInput leftInput = pending;
            CharInput rightInput = ((CharInput) selected).getPair();

            int rightInputIndex = inputList.getInputIndex(rightInput);
            // 交换左右顺序
            if (inputList.getSelectedIndex() > rightInputIndex) {
                leftInput = rightInput;
                rightInput = pending;
            }

            leftInput.replaceLastKey(leftKey);
            rightInput.replaceLastKey(rightKey);
        } else {
            // 若待输入非空，且不是符号输入，则对其做配对符号包裹
            Input pending = inputList.getPending();
            boolean wrapSelected = !Input.isEmpty(pending) && !pending.isSymbol();
            if (wrapSelected) {
                // 选中被包裹输入的左侧 Gap
                inputList.confirmPendingAndSelectPrevious();
            } else {
                inputList.confirmPendingAndSelectNext();
            }

            CharInput leftInput = inputList.getCharPending();
            leftInput.appendKey(leftKey);

            if (wrapSelected) {
                // 选中被包裹输入的右侧 Gap：左符号+Gap+被包裹输入+右符号
                inputList.confirmPendingAndSelectByOffset(3);
            } else {
                inputList.confirmPendingAndSelectNext();
            }

            CharInput rightInput = inputList.getCharPending();
            rightInput.appendKey(rightKey);

            // 绑定配对符号的关联：由任意一方发起绑定即可
            rightInput.setPair(leftInput);

            // 确认右侧的配对输入，并将光标移动到 右配对输入 的左侧 Gap 位置以确保光标在配对符号的中间位置
            inputList.confirmPendingAndSelectPrevious();
        }
    }
}
