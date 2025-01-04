/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.core;

import java.util.ArrayList;
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.core.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.GapInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgListener;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputList_Clean_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputList_Cleaned_Cancel_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Input_Choose_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Input_Completion_Apply_Done;

/**
 * 输入面板
 * <p/>
 * 负责处理 {@link UserInputMsg} 消息，以及对
 * {@link InputList} 的整体性处理（如，提交输入列表、撤销输入列表提交等）
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-01-04
 */
public class Inputboard implements UserInputMsgListener {
    public final InputList inputList = new InputList();

    private InputListConfig config;
    private InputMsgListener listener;

    /** 暂存器，用于临时记录已删除、已提交输入，以支持撤销删除和提交操作 */
    private Staged staged;
    private Input.Option option;

    public Inputboard() {
        this.staged = doReset(Inputboard.Staged.Type.none);
    }

    // =============================== Start: 消息处理 ===================================

    public void setListener(InputMsgListener listener) {
        this.listener = listener;
    }

    // -----------------------------

    /** 响应来自上层派发的 {@link UserInputMsg} 消息 */
    @Override
    public void onMsg(UserInputMsg msg) {
        switch (msg.type) {
            case SingleTap_Input: {
                Input input = msg.data().target;
                switch (msg.data().where) {
                    case head:
                        input = getFirstInput();
                        break;
                    case tail:
                        input = getLastInput();
                        break;
                }

                // Note: 在选择算术输入时，需先触发上层输入列表的选择消息，
                // 再触发算术输入列表的选择消息，从而确保先切换到算术键盘上
                fire_InputMsg(Input_Choose_Doing, input);
                break;
            }
            case SingleTap_CompletionInput: {
                CompletionInput completion = (CompletionInput) msg.data().target;

                applyCompletion(completion);
                // Note：待输入的补全数据将在 confirm 时清除
                confirmPendingAndSelectNext();

                fire_InputMsg(Input_Completion_Apply_Done, null);
                break;
            }
            case SingleTap_Btn_Clean_InputList: {
                reset(true);

                fire_InputMsg(InputList_Clean_Done, null);
                break;
            }
            case SingleTap_Btn_Cancel_Clean_InputList: {
                cancelDelete();

                Input input = getSelected();
                fire_InputMsg(InputList_Cleaned_Cancel_Done, input);
                break;
            }
        }
    }

    /** 发送 {@link InputMsg} 消息 */
    private void fire_InputMsg(InputMsgType type, Input input) {
        InputMsg msg = new InputMsg(type, new InputMsgData(input));
        this.listener.onMsg(msg);
    }

    // =============================== End: 消息处理 ===================================

    // =============================== Start: 生命周期 ===================================

    /** 重置输入列表 */
    public void reset(boolean canBeCanceled) {
        this.staged = doReset(canBeCanceled ? Inputboard.Staged.Type.deleted : Inputboard.Staged.Type.none);
    }

    /** 清空输入列表 */
    public void clear() {
        clearCommitRevokes();
        clearDeleteCancels();
    }

    /**
     * 提交输入列表
     * <p/>
     * 返回{@link #getText() 输入文本}，并{@link #reset 重置}
     */
    public StringBuilder commit(boolean canBeRevoked) {
        StringBuilder text = getText();

        this.staged = doReset(canBeRevoked ? Inputboard.Staged.Type.committed : Inputboard.Staged.Type.none);

        return text;
    }

    /** 是否可撤回已提交输入 */
    public boolean canRevokeCommit() {
        return this.staged.type == Inputboard.Staged.Type.committed;
    }

    /** 撤回已提交的输入 */
    public void revokeCommit() {
        if (canRevokeCommit()) {
            Inputboard.Staged.restore(this, this.staged);
        }
    }

    /** 清除 提交撤回数据 */
    public void clearCommitRevokes() {
        if (canRevokeCommit()) {
            this.staged = Inputboard.Staged.none();
        }
    }

    /** 是否可撤销已删除输入 */
    public boolean canCancelDelete() {
        return this.staged.type == Inputboard.Staged.Type.deleted;
    }

    /** 撤销已删除输入 */
    public void cancelDelete() {
        if (canCancelDelete()) {
            Inputboard.Staged.restore(this, this.staged);
        }
    }

    /** 清除 删除撤销数据 */
    public void clearDeleteCancels() {
        if (canCancelDelete()) {
            this.staged = Inputboard.Staged.none();
        }
    }

    /** 重置列表，并返回指定类型的暂存器（存储重置前的输入数据） */
    private Inputboard.Staged doReset(Inputboard.Staged.Type stagedType) {
        Inputboard.Staged staged = Inputboard.Staged.store(stagedType, this);

        this.inputs.clear();
        this.cursor.reset();

        Input gap = new GapInput();
        this.inputs.add(gap);
        doSelect(gap);

        resetOption();
        this.staged = Inputboard.Staged.none();

        return staged;
    }

    // =============================== End: 生命周期 ===================================

    public InputFactory getInputFactory() {
        return () -> {
            List<InputViewData> dataList = new ArrayList<>();

            for (int i = 0; i < this.inputs.size(); i++) {
                InputViewData data = InputViewData.create(this, getOption(), i);

                dataList.add(data);
            }
            return dataList;
        };
    }

    static class Staged {
        public final Type type;
        public final List<Input> inputs;
        public final InputList.Cursor cursor;

        private Staged(Type type, List<Input> inputs, InputList.Cursor cursor) {
            this.type = type;
            this.inputs = inputs;
            this.cursor = cursor;
        }

        public static Staged none() {
            return new Staged(Type.none, null, null);
        }

        public static Staged store(Type type, InputList inputList) {
            // 为空的输入列表的无需暂存
            if (type == Type.none || inputList.isEmpty()) {
                return none();
            }

            return new Staged(type, new ArrayList<>(inputList.inputs), inputList.cursor.copy());
        }

        public static void restore(InputList inputList, Staged staged) {
            if (staged.type == Type.none) {
                return;
            }

            inputList.doReset(Type.none);

            inputList.inputs.clear();
            inputList.inputs.addAll(staged.inputs);

            staged.cursor.copyTo(inputList.cursor);
        }

        public enum Type {
            none,
            deleted,
            committed,
        }
    }
}
