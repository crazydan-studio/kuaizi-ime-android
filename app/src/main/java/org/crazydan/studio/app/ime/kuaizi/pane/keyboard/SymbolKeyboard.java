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

import java.util.function.Supplier;

import org.crazydan.studio.app.ime.kuaizi.dict.Symbol;
import org.crazydan.studio.app.ime.kuaizi.dict.SymbolGroup;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.SymbolKey;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.keytable.SymbolEmojiKeyTable;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.SymbolChooseStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgType;

/**
 * {@link Type#Symbol 符号键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-10
 */
public class SymbolKeyboard extends PagingKeysKeyboard {

    @Override
    public Type getType() {
        return Type.Symbol;
    }

    @Override
    public void start(InputList inputList) {
        Input<?> input = inputList.getSelected();
        boolean hasPair = input != null && !input.isGap() && ((CharInput) input).hasPair();

        start_Symbol_Choosing(inputList, hasPair);
    }

    @Override
    public KeyFactory getKeyFactory(InputList inputList) {
        SymbolEmojiKeyTable keyTable = SymbolEmojiKeyTable.create(createKeyTableConfig(inputList));

        SymbolChooseStateData stateData = (SymbolChooseStateData) this.state.data;

        return () -> keyTable.createSymbolKeys(stateData.getGroup(), stateData.isOnlyPair(), stateData.getPageStart());
    }

    private void start_Symbol_Choosing(InputList inputList, boolean onlyPair) {
        CharInput pending = inputList.getPending();

        SymbolEmojiKeyTable keyTable = SymbolEmojiKeyTable.create(createKeyTableConfig(inputList));
        int pageSize = keyTable.getSymbolKeysPageSize();

        SymbolChooseStateData stateData = new SymbolChooseStateData(pending, pageSize, onlyPair);
        this.state = new State(State.Type.Symbol_Choose_Doing, stateData);

        SymbolGroup group = SymbolGroup.latin;
        // TODO 在 start 中获取原键盘类型
        if (getType() == Type.Pinyin) {
            group = SymbolGroup.han;
        }

        do_Symbol_Choosing(null, group);
    }

    private void do_Symbol_Choosing(Key<?> key, SymbolGroup group) {
        SymbolChooseStateData stateData = (SymbolChooseStateData) this.state.data;
        stateData.setGroup(group);

        fire_InputCandidate_Choose_Doing(stateData.input, key);
    }

    @Override
    protected void on_Choose_Doing_PagingKey_Msg(InputList inputList, UserKeyMsg msg) {
        boolean continuous = false;

        SymbolKey key = (SymbolKey) msg.data.key;
        switch (msg.type) {
            case LongPress_Key_Tick:
                continuous = true;
            case SingleTap_Key: {
                play_SingleTick_InputAudio(key);
                // TODO 符号放大、加粗显示，以便于识别
                show_InputChars_Input_Popup(key);

                do_Single_Symbol_Inputting(inputList, key, continuous);
                break;
            }
        }
    }

    @Override
    protected void on_Choose_Doing_CtrlKey_Msg(InputList inputList, UserKeyMsg msg, CtrlKey key) {
        if (msg.type == UserKeyMsgType.SingleTap_Key) {
            if (CtrlKey.is(key, CtrlKey.Type.Toggle_Symbol_Group)) {
                play_SingleTick_InputAudio(key);

                CtrlKey.SymbolGroupToggleOption option = (CtrlKey.SymbolGroupToggleOption) key.getOption();
                do_Symbol_Choosing(key, option.value());
            }
        }
    }

    private void do_Single_Symbol_Inputting(InputList inputList, SymbolKey key, boolean continuousInput) {
        boolean isDirectInputting = inputList.isEmpty();
        boolean isPair = key.isPair();

        if (!isDirectInputting) {
            if (isPair) {
                prepare_for_PairSymbol_Inputting(inputList, (Symbol.Pair) key.getSymbol());

                // Note：配对符号输入后不再做连续输入，切换回原键盘
                confirm_InputList_Pending(inputList, key);
                switch_Keyboard_to_Previous(key);
            } else {
                confirm_or_New_InputList_Pending(inputList);

                confirm_InputList_Input_with_SingleKey_Only(inputList, key);
            }
            return;
        }

        CharInput pending = inputList.newPending();
        if (isPair) {
            Symbol.Pair symbol = (Symbol.Pair) key.getSymbol();

            prepare_for_PairSymbol_Inputting(inputList, symbol);
        } else {
            pending.appendKey(key);
            pending.clearPair();
        }

        // 直接提交输入
        commit_InputList(inputList, false, false, isPair);

        // Note：非连续输入的情况下，配对符号输入后不再做连续输入，切换回原键盘
        if (isPair && !continuousInput) {
            switch_Keyboard_to_Previous(key);
        }
    }

    private void prepare_for_PairSymbol_Inputting(InputList inputList, Symbol.Pair symbol) {
        String left = symbol.left;
        String right = symbol.right;

        prepare_for_PairKey_Inputting(inputList,
                                      () -> SymbolKey.create(Symbol.single(left)),
                                      () -> SymbolKey.create(Symbol.single(right)));
    }

    private void prepare_for_PairKey_Inputting(InputList inputList, Supplier<Key<?>> left, Supplier<Key<?>> right) {
        Input<?> selected = inputList.getSelected();

        Key<?> leftKey = left.get();
        Key<?> rightKey = right.get();

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
            // 对于输入修改，若为非空的待输入，则对其做配对符号包裹
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
