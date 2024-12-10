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
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.PagingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.SymbolChooseDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.user.UserFingerFlippingMsgData;

/**
 * {@link Type#Symbol 符号键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-10
 */
public class SymbolKeyboard extends BaseKeyboard {

    @Override
    public Type getType() {
        return Type.Symbol;
    }

    @Override
    public void start(InputList inputList) {
        Input<?> input = inputList.getSelected();
        boolean hasPair = input != null && !input.isGap() && ((CharInput) input).hasPair();

        start_Symbol_Choosing(hasPair);
    }

    @Override
    protected KeyFactory doGetKeyFactory() {
        SymbolEmojiKeyTable keyTable = SymbolEmojiKeyTable.create(createKeyTableConfig());

        SymbolChooseDoingStateData stateData = (SymbolChooseDoingStateData) this.state.data;

        return () -> keyTable.createSymbolKeys(stateData.getGroup(), stateData.isOnlyPair(), stateData.getPageStart());
    }

    @Override
    public void onMsg(InputList inputList, UserKeyMsg msg, UserKeyMsgData data) {
        if (try_OnUserKeyMsg(inputList, msg, data)) {
            return;
        }

        Key<?> key = data.target;
        switch (msg) {
            case FingerFlipping: {
                on_Symbol_Choose_Doing_PageFlipping_Msg(key, data);
                break;
            }
            default: {
                if (key instanceof SymbolKey) {
                    on_Symbol_Choose_Doing_SymbolKey_Msg(inputList, msg, (SymbolKey) key);
                } else if (key instanceof CtrlKey) {
                    on_Symbol_Choose_Doing_CtrlKey_Msg(msg, (CtrlKey) key);
                }
            }
        }
    }

    private void start_Symbol_Choosing(boolean onlyPair) {
        SymbolEmojiKeyTable keyTable = SymbolEmojiKeyTable.create(createKeyTableConfig());
        int pageSize = keyTable.getSymbolKeysPageSize();

        SymbolChooseDoingStateData stateData = new SymbolChooseDoingStateData(pageSize, onlyPair);
        State state = new State(State.Type.Symbol_Choose_Doing, stateData, createInitState());
        change_State_To(null, state);

        SymbolGroup group = SymbolGroup.latin;
        // TODO 在 start 中获取原键盘类型
        if (getType() == Type.Pinyin) {
            group = SymbolGroup.han;
        }

        do_Symbol_Choosing(null, group);
    }

    private void on_Symbol_Choose_Doing_PageFlipping_Msg(Key<?> key, UserKeyMsgData data) {
        update_PagingStateData_by_UserKeyMsg((PagingStateData<?>) this.state.data, (UserFingerFlippingMsgData) data);

        fire_Symbol_Choose_Doing(key);
    }

    private void do_Symbol_Choosing(Key<?> key, SymbolGroup group) {
        SymbolChooseDoingStateData stateData = (SymbolChooseDoingStateData) this.state.data;
        stateData.setGroup(group);

        fire_Symbol_Choose_Doing(key);
    }

    private void on_Symbol_Choose_Doing_SymbolKey_Msg(InputList inputList, UserKeyMsg msg, SymbolKey key) {
        boolean continuous = false;

        switch (msg) {
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

    private void on_Symbol_Choose_Doing_CtrlKey_Msg(UserKeyMsg msg, CtrlKey key) {
        if (msg == UserKeyMsg.SingleTap_Key) {
            if (CtrlKey.is(key, CtrlKey.Type.Toggle_Symbol_Group)) {
                play_SingleTick_InputAudio(key);

                CtrlKey.SymbolGroupToggleOption option = (CtrlKey.SymbolGroupToggleOption) key.getOption();
                do_Symbol_Choosing(key, option.value());
            }
        }
    }

    private void fire_Symbol_Choose_Doing(Key<?> key) {
        fire_Common_InputMsg(KeyboardMsg.Symbol_Choose_Doing, key);
    }

    private void do_Single_Symbol_Inputting(InputList inputList, SymbolKey key, boolean continuousInput) {
        boolean isDirectInputting = inputList.isEmpty();
        boolean isPair = key.isPair();

        if (!isDirectInputting) {
            if (isPair) {
                prepare_for_PairSymbol_Inputting(inputList, (Symbol.Pair) key.getSymbol());

                // Note：配对符号输入后不再做连续输入，切换回原键盘
                confirm_InputList_Pending(inputList, key);
                switchTo_Previous_Keyboard(key);
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
            switchTo_Previous_Keyboard(key);
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
