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

package org.crazydan.studio.app.ime.kuaizi.core;

import org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData;
import org.crazydan.studio.app.ime.kuaizi.core.input.MathExprInput;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputCompletionSingleTapMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputSingleTapMsgData;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputList_Clean_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputList_Cleaned_Cancel_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Input_Choose_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Input_Completion_Apply_Done;

/**
 * 输入面板
 * <p/>
 * 负责处理与 {@link Input} 相关的 {@link UserInputMsg} 消息
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-01-04
 */
public class Inputboard {
    private Stage stage;

    public Inputboard() {
        this.stage = Stage.none();
    }

    /** 构建 {@link InputFactory} */
    public InputFactory buildInputFactory(InputboardContext context) {
        InputList inputList = context.inputList;
        Input.Option inputOption = inputList.getInputOption();

        return () -> InputViewData.build(inputList, inputOption);
    }

    // =============================== Start: 消息处理 ===================================

    /** 响应来自上层派发的 {@link UserInputMsg} 消息 */
    public void onMsg(InputboardContext context, UserInputMsg msg) {
        InputList inputList = context.inputList;

        switch (msg.type) {
            case SingleTap_Input: {
                UserInputSingleTapMsgData data = msg.data();

                Input input;
                if (data.positionInParent < 0) {
                    input = getInputAt(inputList, data.position);
                }
                // 处理输入列表被嵌套的情况
                else {
                    input = getInputAt(inputList, data.positionInParent);

                    if (input instanceof MathExprInput) {
                        InputList subInputList = ((MathExprInput) input).getInputList();
                        input = getInputAt(subInputList, data.position);
                    }
                }

                // Note: 在选择算术输入时，需先触发上层输入列表的选择消息，
                // 再触发算术输入列表的选择消息，从而确保先切换到算术键盘上
                fire_InputMsg(context, Input_Choose_Doing, input);
                break;
            }
            case SingleTap_InputCompletion: {
                UserInputCompletionSingleTapMsgData data = msg.data();

                inputList.applyCompletion(data.position);

                fire_InputMsg(context, Input_Completion_Apply_Done, null);
                break;
            }
            case SingleTap_Btn_Clean_InputList: {
                storeCleaned(context, true);

                fire_InputMsg(context, InputList_Clean_Done, null);
                break;
            }
            case SingleTap_Btn_Cancel_Clean_InputList: {
                restoreCleaned(context);

                Input input = inputList.getSelected();
                fire_InputMsg(context, InputList_Cleaned_Cancel_Done, input);
                break;
            }
        }
    }

    /** 发送 {@link InputMsg} 消息 */
    private void fire_InputMsg(InputboardContext context, InputMsgType type, Input input) {
        context.fireInputMsg(type, input);
    }

    private Input getInputAt(InputList inputList, int position) {
        return position < 0 //
               ? inputList.getLastInput() //
               : inputList.getInput(position);
    }

    // =============================== End: 消息处理 ===================================

    // =============================== Start: 生命周期 ===================================

    public void start(InputboardContext context) {
        InputList inputList = context.inputList;
        Input.Option inputOption = context.createInputOption();

        inputList.setInputOption(inputOption);
    }

    /** 重置：{@link InputList#reset() 重置} {@link InputList}，并清空 {@link #stage} */
    public void reset(InputboardContext context) {
        storeCleaned(context, false);
    }

    /** 是否可恢复 已提交 */
    public boolean canRestoreCommitted() {
        return this.stage.type == Stage.Type.committed;
    }

    /** 保存 已提交：{@link InputboardContext#inputList} 将会被{@link InputList#reset() 重置} */
    public void storeCommitted(InputboardContext context, boolean canBeRestored) {
        resetWithStage(context, canBeRestored ? Stage.Type.committed : Stage.Type.none);

        // 提交输入后，需清空只读数据构建器的缓存，以降低内存占用
        InputViewData.clearCachedBuilds();
    }

    /** 恢复 已提交 */
    public void restoreCommitted(InputboardContext context) {
        if (canRestoreCommitted()) {
            this.stage = Stage.restore(context.inputList, this.stage);
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

    /** 保存 已清空：{@link InputboardContext#inputList} 将会被{@link InputList#reset() 重置} */
    public void storeCleaned(InputboardContext context, boolean canBeRestored) {
        resetWithStage(context, canBeRestored ? Stage.Type.cleaned : Stage.Type.none);

        // 清空输入后，需清空只读数据构建器的缓存，以降低内存占用
        InputViewData.clearCachedBuilds();
    }

    /** 恢复 已清空 */
    public void restoreCleaned(InputboardContext context) {
        if (canRestoreCleaned()) {
            this.stage = Stage.restore(context.inputList, this.stage);
        }
    }

    /** 清除 已清空 */
    public void clearCleaned() {
        if (canRestoreCleaned()) {
            this.stage = Stage.none();
        }
    }

    /** 暂存 {@link InputList}，并对其进行{@link InputList#reset() 重置} */
    private void resetWithStage(InputboardContext context, Stage.Type stageType) {
        InputList inputList = context.inputList;

        // Note: 在 Staged 中暂存 InputList 的副本
        this.stage = Stage.create(stageType, inputList.copy());

        inputList.reset();

        start(context);
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
