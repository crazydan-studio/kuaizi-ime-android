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

import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.SymbolEmojiKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.state.EmojiChooseStateData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.dict.Emojis;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;

/**
 * {@link Type#Emoji 表情键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-10
 */
public class EmojiKeyboard extends InputCandidateKeyboard {

    public EmojiKeyboard(PinyinDict dict) {
        super(dict);
    }

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
    public KeyFactory buildKeyFactory(KeyboardContext context) {
        SymbolEmojiKeyTable keyTable = createKeyTable(context);

        EmojiChooseStateData stateData = this.state.data();

        return () -> keyTable.createEmojiKeys(stateData.getGroups(),
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
        CharInput pending = inputList.getPending();

        SymbolEmojiKeyTable keyTable = createKeyTable(context);
        int pageSize = keyTable.getEmojiKeysPageSize();

        Emojis emojis = this.dict.getAllEmojis(pageSize / 2);

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
        CharInput pending = inputList.newPending();

        pending.appendKey(key);
        pending.setWord(word);

        // 直接提交输入
        commit_InputList(context, false, false);
    }
}
