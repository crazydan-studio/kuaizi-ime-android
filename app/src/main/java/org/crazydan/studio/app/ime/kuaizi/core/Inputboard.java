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
import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.core.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputList;
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
 * {@link InputList} 的整体性处理（如，提交/清空输入列表、撤销提交/清空等）
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-01-04
 */
public class Inputboard implements UserInputMsgListener {
    public final InputList inputList = new InputList();

    private Stage stage;

    private InputboardConfig config;
    private InputMsgListener listener;

    public Inputboard() {
        resetWithStage(Stage.Type.none);
    }

    /**
     * 更新配置
     *
     * @return 若存在更新，则返回 true，否则，返回 false
     */
    public boolean updateConfig(InputboardConfig config) {
        boolean changed = !Objects.equals(this.config, config);
        this.config = config;

        if (config != null) {
            Input.Option inputOption = this.inputList.getInputOption();

            if (inputOption == null) {
                inputOption = new Input.Option(null, this.config.useCandidateVariantFirst);
            } else {
                inputOption = new Input.Option(inputOption.wordSpellUsedMode, this.config.useCandidateVariantFirst);
            }
            this.inputList.setInputOption(inputOption);
        }
        return changed;
    }

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

    // =============================== Start: 消息处理 ===================================

    public void setListener(InputMsgListener listener) {
        this.listener = listener;
    }

    /** 响应来自上层派发的 {@link UserInputMsg} 消息 */
    @Override
    public void onMsg(UserInputMsg msg) {
        switch (msg.type) {
            case SingleTap_Input: {
                Input input = msg.data().target;
                switch (msg.data().where) {
                    case head:
                        input = this.inputList.getFirstInput();
                        break;
                    case tail:
                        input = this.inputList.getLastInput();
                        break;
                }

                // Note: 在选择算术输入时，需先触发上层输入列表的选择消息，
                // 再触发算术输入列表的选择消息，从而确保先切换到算术键盘上
                fire_InputMsg(Input_Choose_Doing, input);
                break;
            }
            case SingleTap_CompletionInput: {
                CompletionInput completion = (CompletionInput) msg.data().target;

                this.inputList.applyCompletion(completion);
                // Note：待输入的补全数据将在 confirm 时清除
                this.inputList.confirmPendingAndSelectNext();

                fire_InputMsg(Input_Completion_Apply_Done, null);
                break;
            }
            case SingleTap_Btn_Clean_InputList: {
                storeCleaned(true);

                fire_InputMsg(InputList_Clean_Done, null);
                break;
            }
            case SingleTap_Btn_Cancel_Clean_InputList: {
                restoreCleaned();

                Input input = this.inputList.getSelected();
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

    /** 重置：{@link InputList#reset() 重置} {@link InputList}，并清空 {@link #stage} */
    public void reset() {
        storeCleaned(false);
    }

    /** 是否可恢复 已提交 */
    public boolean canRestoreCommitted() {
        return this.stage.type == Stage.Type.committed;
    }

    /** 保存 已提交：{@link #inputList} 将会被{@link InputList#reset() 重置} */
    public void storeCommitted(boolean canBeRestored) {
        resetWithStage(canBeRestored ? Stage.Type.committed : Stage.Type.none);
    }

    /** 恢复 已提交 */
    public void restoreCommitted() {
        if (canRestoreCommitted()) {
            this.stage = Stage.restore(this.inputList, this.stage);
        }
    }

    /** 清除 已提交 */
    public void clearCommitted() {
        if (canRestoreCommitted()) {
            this.stage = Stage.none();
        }
    }

    /** 是否可恢复 已清空 */
    public boolean canRestoreCleaned() {
        return this.stage.type == Stage.Type.cleaned;
    }

    /** 保存 已清空：{@link #inputList} 将会被{@link InputList#reset() 重置} */
    public void storeCleaned(boolean canBeRestored) {
        resetWithStage(canBeRestored ? Stage.Type.cleaned : Stage.Type.none);
    }

    /** 恢复 已清空 */
    public void restoreCleaned() {
        if (canRestoreCleaned()) {
            this.stage = Stage.restore(this.inputList, this.stage);
        }
    }

    /** 清除 已清空 */
    public void clearCleaned() {
        if (canRestoreCleaned()) {
            this.stage = Stage.none();
        }
    }

    /** 暂存 {@link InputList}，并对其进行{@link InputList#reset() 重置} */
    private void resetWithStage(Stage.Type stageType) {
        // Note: 在 Staged 中暂存 InputList 的副本
        this.stage = Stage.create(stageType, this.inputList.copy());

        this.inputList.reset();

        // 确保 InputList 的输入选项配置恢复为初始状态
        updateConfig(this.config);
    }

    // =============================== End: 生命周期 ===================================

    /** 用于支持撤销对输入列表的清空和提交 */
    static class Stage {
        public enum Type {
            /** 不暂存数据 */
            none,
            /** 暂存已清空数据 */
            cleaned,
            /** 暂存已提交数据 */
            committed,
        }

        public final Type type;

        private final InputList inputList;

        Stage(Type type, InputList inputList) {
            this.type = type;
            this.inputList = inputList;
        }

        /** 创建 {@link Type#none} 类型的 {@link Stage}，即，不存储任何 {@link InputList} 数据 */
        public static Stage none() {
            return new Stage(Type.none, null);
        }

        /** 创建指定 {@link Type} 的 {@link Stage} */
        public static Stage create(Type type, InputList inputList) {
            // 为空的输入列表的无需暂存
            if (type == Type.none || inputList.isEmpty()) {
                return none();
            }

            return new Stage(type, inputList);
        }

        /** 还原 {@link Stage} 中的数据到指定的 {@link InputList}，并返回 {@link #none()} */
        public static Stage restore(InputList inputList, Stage stage) {
            if (stage.type != Type.none) {
                inputList.replaceBy(stage.inputList);
            }

            return none();
        }
    }
}