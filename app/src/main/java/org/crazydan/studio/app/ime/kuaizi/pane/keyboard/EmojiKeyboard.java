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

import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.keytable.SymbolEmojiKeyTable;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.EmojiChooseDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.PagingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.user.UserFingerFlippingMsgData;

/**
 * {@link Type#Emoji 表情键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-10
 */
public class EmojiKeyboard extends BaseKeyboard {
    private final PinyinDict dict;

    public EmojiKeyboard(PinyinDict dict) {
        this.dict = dict;
    }

    @Override
    public Type getType() {
        return Type.Emoji;
    }

    @Override
    protected KeyFactory doGetKeyFactory() {
        SymbolEmojiKeyTable keyTable = SymbolEmojiKeyTable.create(createKeyTableConfig());

        EmojiChooseDoingStateData stateData = (EmojiChooseDoingStateData) this.state.data;

        return () -> keyTable.createEmojiKeys(stateData.getGroups(),
                                              stateData.getPagingData(),
                                              stateData.getGroup(),
                                              stateData.getPageStart());
    }

    @Override
    public void onMsg(InputList inputList, UserKeyMsg msg, UserKeyMsgData data) {
        if (try_OnUserKeyMsg(inputList, msg, data)) {
            return;
        }

        Key<?> key = data.target;
        switch (msg) {
            case FingerFlipping: {
                on_Emoji_Choose_Doing_PageFlipping_Msg(key, data);
                break;
            }
            default: {
                if (key instanceof InputWordKey) {
                    on_Emoji_Choose_Doing_InputWordKey_Msg(inputList, msg, (InputWordKey) key);
                }
                //
                else if (key instanceof CtrlKey) {
                    on_Emoji_Choose_Doing_CtrlKey_Msg(msg, (CtrlKey) key);
                }
            }
        }
    }

    private void on_Emoji_Choose_Doing_PageFlipping_Msg(Key<?> key, UserKeyMsgData data) {
        update_PagingStateData_by_UserKeyMsg((PagingStateData<?>) this.state.data, (UserFingerFlippingMsgData) data);

        fire_Emoji_Choose_Doing(key);
    }

    private void on_Emoji_Choose_Doing_InputWordKey_Msg(InputList inputList, UserKeyMsg msg, InputWordKey key) {
        switch (msg) {
            case LongPress_Key_Tick:
            case SingleTap_Key: {
                play_SingleTick_InputAudio(key);
                // TODO 显示表情的名字（或者最短关键字）
                show_InputChars_Input_Popup(key);

                do_Single_Emoji_Inputting(inputList, key);
                break;
            }
        }
    }

    private void on_Emoji_Choose_Doing_CtrlKey_Msg(UserKeyMsg msg, CtrlKey key) {
        switch (msg) {
            case SingleTap_Key: {
                if (CtrlKey.is(key, CtrlKey.Type.Toggle_Emoji_Group)) {
                    play_SingleTick_InputAudio(key);

                    CtrlKey.CodeOption option = (CtrlKey.CodeOption) key.getOption();
                    do_Emoji_Choosing(key, option.value());
                }
                break;
            }
        }
    }

    private void do_Single_Emoji_Inputting(InputList inputList, InputWordKey key) {
        if (try_Single_Key_Inputting(inputList, key)) {
            return;
        }

        boolean isDirectInputting = inputList.isEmpty();
        CharInput pending = inputList.newPending();

        InputWord word = key.getWord();
        pending.appendKey(key);
        pending.setWord(word);

        // 直接提交输入
        if (isDirectInputting) {
            commit_InputList(inputList, false, false);
        } else {
            confirm_InputList_Pending(inputList, key);
        }
    }
}
