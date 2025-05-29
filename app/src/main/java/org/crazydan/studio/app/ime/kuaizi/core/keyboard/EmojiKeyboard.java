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

import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.SymbolEmojiKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.state.EmojiChooseStateData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.dict.Emojis;
import org.crazydan.studio.app.ime.kuaizi.dict.UserInputDataDict;

/**
 * {@link Type#Emoji 表情键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-10
 */
public class EmojiKeyboard extends InputCandidateKeyboard {

    @Override
    public Type getType() {return Type.Emoji;}

    @Override
    public void start(KeyboardContext context) {
        start_Emoji_Choosing(context);
    }

    private SymbolEmojiKeyTable createKeyTable(KeyboardContext context) {
        KeyTableConfig keyTableConf = createKeyTableConfig(context);

        return SymbolEmojiKeyTable.create(keyTableConf);
    }

    @Override
    public KeyFactory do_BuildKeyFactory(KeyboardContext context) {
        if (this.state.type != State.Type.InputCandidate_Choose_Doing) {
            return null;
        }

        SymbolEmojiKeyTable keyTable = createKeyTable(context);

        EmojiChooseStateData stateData = this.state.data();

        return () -> keyTable.createEmojiGrid(stateData.getGroups(),
                                              stateData.getPagingData(),
                                              stateData.getGroup(),
                                              stateData.getPageStart());
    }

    @Override
    protected void on_InputCandidate_Choose_Doing_PagingKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        switch (msg.type) {
            case SingleTap_Key: {
                show_InputChars_Input_Popup(context);
            }
            // Note: 长按显示提示气泡有基类处理
            case LongPress_Key_Tick: {
                play_SingleTick_InputAudio(context);

                do_Single_Emoji_Inputting(context);
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
        if (CtrlKey.Type.Toggle_Emoji_Group.match(key)) {
            play_SingleTick_InputAudio(context);

            CtrlKey.Option<String> option = key.option();
            do_Emoji_Choosing(context, option.value);
        }
    }

    /** 进入表情选择状态，并处理表情翻页 */
    private void start_Emoji_Choosing(KeyboardContext context) {
        InputList inputList = context.inputList;
        CharInput pending = inputList.getCharPending();

        SymbolEmojiKeyTable keyTable = createKeyTable(context);
        int pageSize = keyTable.getEmojiGridPageSize();

        UserInputDataDict dict = context.dict.useUserInputDataDict();
        Emojis emojis = dict.getAllEmojis(pageSize / 2);

        EmojiChooseStateData stateData = new EmojiChooseStateData(pending, emojis, pageSize);
        this.state = new State(State.Type.InputCandidate_Choose_Doing, stateData);

        String group = null;
        // 若默认分组（常用）的数据为空，则切换到第二个分组
        if (stateData.getPagingData().isEmpty()) {
            group = stateData.getGroups().get(1);
        }

        do_Emoji_Choosing(context, group);
    }

    private void do_Emoji_Choosing(KeyboardContext context, String group) {
        EmojiChooseStateData stateData = this.state.data();
        stateData.setGroup(group);

        fire_InputCandidate_Choose_Doing(context);
    }

    private void do_Single_Emoji_Inputting(KeyboardContext context) {
        InputWordKey key = context.key();
        InputList inputList = context.inputList;

        boolean directInputting = inputList.isEmpty();
        if (!directInputting) {
            do_Single_CharKey_Replace_or_NewPending_Inputting(context);
            return;
        }

        InputWord word = key.word;
        CharInput pending = inputList.newCharPending();

        pending.appendKey(key);
        pending.setWord(word);

        // 直接提交输入
        commit_InputList(context, false, false);
    }
}
