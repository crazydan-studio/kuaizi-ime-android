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

import java.util.ArrayList;
import java.util.List;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.os.PersistableBundle;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.core.input.clip.ClipInputData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.ClipInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserClipInputDataSingleTapMsgData;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.ClipInput_Data_Apply_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.ClipInput_Data_Create_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Editor_Edit_Doing;

/**
 * 剪贴板
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-02-21
 */
public class Clipboard {
    /** 多长间隔内的剪贴数据可以用于自动粘贴 */
    private final static long MAX_DURATION_TO_BE_AUTO_PASTED = 30 * 1000;

    protected final Logger log = Logger.getLogger(getClass());

    private final ClipboardManager manager;

    /** 最近的剪贴板数据 */
    private ClipInputData latest;

    public Clipboard(ClipboardManager manager) {
        this.manager = manager;
    }

    // =============================== Start: 消息处理 ===================================

    /** 响应来自上层派发的 {@link UserInputMsg} 消息 */
    public void onMsg(ClipboardContext context, UserInputMsg msg) {
        // TODO 显示剪贴板、粘贴选中内容、操作剪贴板

        switch (msg.type) {
            case SingleTap_ClipInputData: {
                UserClipInputDataSingleTapMsgData data = msg.data();

                if (data.clip.type == ClipInputData.Type.captcha) {
                    // TODO 逐字输入
                } else {
                    pastClipData(this.manager, data.clip, () -> {
                        InputMsgData msgData = new EditorEditMsgData(EditorAction.paste);

                        context.fireInputMsg(Editor_Edit_Doing, msgData);
                    });
                }

                context.fireInputMsg(ClipInput_Data_Apply_Done);
                break;
            }
            default: {
                this.log.warn("Ignore message %s", () -> new Object[] { msg.type });
            }
        }
    }

    // =============================== End: 消息处理 ===================================

    public void start(ClipboardContext context) {
        ClipInputData data = readClipData(this.manager);
        if (data == null) {
            return;
        }

        if (this.latest != null && this.latest.isSameWith(data)) {
            if (this.latest.usedAt > 0
                || Math.abs(this.latest.createdAt - data.createdAt) > MAX_DURATION_TO_BE_AUTO_PASTED) {
                return;
            }
        } else {
            this.latest = data;
        }

        List<ClipInputData> clips = new ArrayList<>();
        // - 正则表达式提取 6-8 位数字作为验证码，提示的验证码用星号脱敏
        // - 提供 将标点符号替换为下划线 后的剪贴结果
        clips.add(data);

        ClipInputMsgData msgData = new ClipInputMsgData(clips);

        context.fireInputMsg(ClipInput_Data_Create_Done, msgData);
    }

    private ClipInputData readClipData(ClipboardManager manager) {
        ClipData clip = manager.getPrimaryClip();
        ClipData.Item item = clip != null ? clip.getItemAt(0) : null;

        if (item == null) {
            return null;
        }

        PersistableBundle extras = clip.getDescription().getExtras();
        boolean sensitive = extras != null && extras.containsKey(ClipDescription.EXTRA_IS_SENSITIVE);

        // TODO 分析数据，拆分为多个可粘贴内容，交给上层显示
        ClipInputData data = ClipInputData.build((b) -> {
            b.sensitive(sensitive);

            if (item.getText() != null) {
                b.type(ClipInputData.Type.text).content(item.getText().toString());
            }
        });

        return data.type == null ? null : data;
    }

    private void pastClipData(ClipboardManager manager, ClipInputData data, Runnable cb) {
        ClipData oldClip = manager.getPrimaryClip();

        switch (data.type) {
            case html: {
                ClipData clip = ClipData.newHtmlText("", data.content, data.content);
                manager.setPrimaryClip(clip);
                break;
            }
            default: {
                ClipData clip = ClipData.newPlainText("", data.content);
                manager.setPrimaryClip(clip);
                break;
            }
        }

        cb.run();

        if (oldClip == null) {
            // 清空剪贴板
            oldClip = ClipData.newPlainText("", "");
        }
        manager.setPrimaryClip(oldClip);
    }
}
