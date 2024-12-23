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

import org.crazydan.studio.app.ime.kuaizi.dict.Emojis;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.keytable.SymbolEmojiKeyTable;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.EmojiChooseStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgType;

/**
 * {@link Type#Emoji 表情键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-10
 */
public class EmojiKeyboard extends PagingKeysKeyboard {
    private final PinyinDict dict;

    public EmojiKeyboard(PinyinDict dict) {
        this.dict = dict;
    }

    @Override
    public Type getType() {
        return Type.Emoji;
    }

    @Override
    public void start(KeyboardContext context) {
        start_Emoji_Choosing(context);
    }

    private SymbolEmojiKeyTable createKeyTable(InputList inputList) {
        KeyTableConfig keyTableConf = createKeyTableConfig(inputList);

        return SymbolEmojiKeyTable.create(keyTableConf);
    }

    @Override
    public KeyFactory getKeyFactory(InputList inputList) {
        SymbolEmojiKeyTable keyTable = createKeyTable(inputList);

        EmojiChooseStateData stateData = (EmojiChooseStateData) this.state.data;

        return () -> keyTable.createEmojiKeys(stateData.getGroups(),
                                              stateData.getPagingData(),
                                              stateData.getGroup(),
                                              stateData.getPageStart());
    }

    @Override
    protected void on_InputCandidate_Choose_Doing_PagingKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        switch (msg.type) {
            case LongPress_Key_Tick:
            case SingleTap_Key: {
                play_SingleTick_InputAudio(context);
                show_InputChars_Input_Popup(context);

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
        if (CtrlKey.is(key, CtrlKey.Type.Toggle_Emoji_Group)) {
            play_SingleTick_InputAudio(context);

            CtrlKey.CodeOption option = (CtrlKey.CodeOption) key.getOption();
            do_Emoji_Choosing(context, option.value());
        }
    }

    /** 进入表情选择状态，并处理表情翻页 */
    private void start_Emoji_Choosing(KeyboardContext context) {
        InputList inputList = context.inputList;
        CharInput pending = inputList.getPending();

        SymbolEmojiKeyTable keyTable = createKeyTable(inputList);
        int pageSize = keyTable.getEmojiKeysPageSize();

        Emojis emojis = this.dict.getAllEmojis(pageSize / 2);

        EmojiChooseStateData stateData = new EmojiChooseStateData(pending, emojis, pageSize);
        this.state = new State(State.Type.Emoji_Choose_Doing, stateData);

        String group = null;
        // 若默认分组（常用）的数据为空，则切换到第二个分组
        if (stateData.getPagingData().isEmpty()) {
            group = stateData.getGroups().get(1);
        }

        do_Emoji_Choosing(context, group);
    }

    private void do_Emoji_Choosing(KeyboardContext context, String group) {
        EmojiChooseStateData stateData = (EmojiChooseStateData) this.state.data;
        stateData.setGroup(group);

        fire_InputCandidate_Choose_Doing(context);
    }

    private void do_Single_Emoji_Inputting(KeyboardContext context) {
        InputWordKey key = context.key();
        InputList inputList = context.inputList;
        boolean directInputting = inputList.isEmpty();

        if (!directInputting) {
            confirm_or_New_InputList_Pending(context);

            confirm_InputList_Input_with_SingleKey_Only(context);
            return;
        }

        InputWord word = key.getWord();
        CharInput pending = inputList.newPending();

        pending.appendKey(key);
        pending.setWord(word);

        // 直接提交输入
        commit_InputList(context, false, false);
    }
}
