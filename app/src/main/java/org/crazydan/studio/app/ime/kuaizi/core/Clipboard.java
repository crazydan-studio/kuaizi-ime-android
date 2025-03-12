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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ClipData;
import android.content.ClipboardManager;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputClip;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputClipMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputClipTextCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputClipSingleTapMsgData;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Editor_Edit_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputClip_Data_Apply_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputClip_Text_Commit_Doing;

/**
 * 剪贴板
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-02-21
 */
public class Clipboard {
    private static final Pattern REGEX_CAPTCHA = Pattern.compile("^.*?\\D(\\d{6,8})\\D.*$",
                                                                 Pattern.DOTALL | Pattern.MULTILINE);
    /**
     * 匹配：
     * - http://example.com
     * - https://www.example.com/path?query=string#fragment
     * - ftp://ftp.example.com:21/files
     */
    private static final Pattern REGEX_URL = Pattern.compile(
            "^.*?((https?|ftp)://([\\w-]+\\.)+\\w{2,}(:\\d+)?(/[^\\s?#]*)?(\\?[^#]*)?(#\\S*)?).*$",
            Pattern.DOTALL | Pattern.MULTILINE);
    /**
     * 匹配：
     * - +86-13812345678, 086-13812345678
     * - 13812345678
     * - 010-12345678, 021 12345678
     * - (0755)1234567
     */
    private static final Pattern REGEX_PHONE = Pattern.compile(
            "^.*?((\\+?0?86-?)?1[3-9]\\d{9}|(\\+?0?86-?)?(\\(\\d{3,4}\\)|\\d{3,4})[- ]?\\d{7,8}).*$",
            Pattern.DOTALL | Pattern.MULTILINE);
    /**
     * 匹配：
     * - user@example.com
     * - user.name+tag@mail.co.uk
     * - john_doe123@sub.domain.org
     */
    private static final Pattern REGEX_EMAIL = Pattern.compile("^.*?([\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}).*$",
                                                               Pattern.DOTALL | Pattern.MULTILINE);

    protected final Logger log = Logger.getLogger(getClass());

    private final ClipboardManager manager;

    private String latestUsedClipCode;
    private List<InputClip> latestClips;

    public Clipboard(ClipboardManager manager) {
        this.manager = manager;
    }

    // =============================== Start: 消息处理 ===================================

    /** 响应来自上层派发的 {@link UserInputMsg} 消息 */
    public void onMsg(ClipboardContext context, UserInputMsg msg) {
        switch (msg.type) {
            case SingleTap_InputClip: {
                // 提前废弃数据，以避免下面的数据粘贴消息更新气泡提示
                discardClips();

                UserInputClipSingleTapMsgData data = msg.data();
                switch (data.clip.type) {
                    case captcha:
                    case phone: {
                        commitClipText(context, data.clip, true);
                        break;
                    }
                    default: {
                        if (data.position > 0) {
                            // 从原始内容中提取的数据需单独提交
                            commitClipText(context, data.clip, false);
                        } else {
                            // 直接粘贴剪贴板原始内容
                            InputMsgData msgData = new EditorEditMsgData(EditorAction.paste);
                            context.fireInputMsg(Editor_Edit_Doing, msgData);
                        }
                    }
                }

                InputClipMsgData msgData = new InputClipMsgData(data.clip);
                context.fireInputMsg(InputClip_Data_Apply_Done, msgData);
                break;
            }
            default: {
                this.log.warn("Ignore message %s", () -> new Object[] { msg.type });
            }
        }
    }

    private void commitClipText(ClipboardContext context, InputClip clip, boolean oneByOne) {
        InputClipTextCommitMsgData msgData = new InputClipTextCommitMsgData(clip.text, oneByOne);

        context.fireInputMsg(InputClip_Text_Commit_Doing, msgData);
    }

    // =============================== End: 消息处理 ===================================

    // =============================== Start: 生命周期 ===================================

    /** 检查剪贴板，并构造{@link #verifyClips() 可使用的}剪贴数据 */
    public void start(ClipboardContext context) {
        this.latestUsedClipCode = context.usedClipCode;

        updateClips(false);
    }

    // =============================== End: 生命周期 ===================================

    /**
     * 废弃剪贴数据，不再使用
     * <p/>
     * 一般在有新的输入或超过一定的时间后，需废弃剪贴数据
     */
    public void discardClips() {
        if (this.latestClips == null) {
            return;
        }
        this.log.debug("Discard Clips");

        this.latestClips = null;
    }

    /** 检查是否有有效的剪贴数据 */
    public boolean verifyClips() {
        return !CollectionUtils.isEmpty(this.latestClips);
    }

    /** 返回剪贴数据 */
    public List<InputClip> getClips() {
        return this.latestClips;
    }

    /** 更新剪贴数据，用于剪贴板发生变化时调用 */
    public void updateClips(boolean force) {
        InputClip primary = readClip(this.manager);
        if (primary == null) {
            discardClips();
            return;
        }

        String clipCode = primary.code;
        // 若剪贴板数据已使用，则不再处理
        if (!force && clipCode.equals(this.latestUsedClipCode)) {
            discardClips();
            return;
        }

        String primaryText = cleanClipText(primary);
        InputClip other = extractOtherClip(primaryText, clipCode);

        List<InputClip> clips = new ArrayList<>();
        // Note: 确保原始内容在第一的位置
        clips.add(primary);

        if (other != null) {
            if (primaryText.equals(other.text)) {
                // 仅更改类型，以保留首尾的空白字符
                clips.set(0, primary.copy((b) -> b.type(other.type)));
            } else {
                clips.add(other);
            }
        }

        this.latestClips = Collections.unmodifiableList(clips);
    }

    // =============================== Start: 内部方法 ===================================

    private String cleanClipText(InputClip clip) {
        return clip.text.trim();
    }

    private InputClip readClip(ClipboardManager manager) {
        ClipData data = manager.getPrimaryClip();
        if (data == null) {
            return null;
        }

        InputClip clip = InputClip.build((b) -> {
            String text = null;
            String html = null;

            for (int i = 0; i < data.getItemCount(); i++) {
                ClipData.Item item = data.getItemAt(i);
                if (item.getText() != null) {
                    text = item.getText().toString();
                } else if (item.getHtmlText() != null) {
                    html = item.getHtmlText();
                }
            }

            if (!CharUtils.isBlank(text)) {
                b.type(InputClip.Type.text).text(text).html(html);
            }
        });

        return clip.type != null ? clip.copy((b) -> b.code(clip.hashCode() + "")) : null;
    }

    private InputClip extractOtherClip(String primaryText, String code) {
        Map<InputClip.Type, Matcher> matchers = new LinkedHashMap<InputClip.Type, Matcher>() {{
            put(InputClip.Type.url, REGEX_URL.matcher(primaryText));
            put(InputClip.Type.email, REGEX_EMAIL.matcher(primaryText));
            put(InputClip.Type.phone, REGEX_PHONE.matcher(primaryText));
            put(InputClip.Type.captcha, REGEX_CAPTCHA.matcher(primaryText));
        }};

        for (Map.Entry<InputClip.Type, Matcher> entry : matchers.entrySet()) {
            Matcher matcher = entry.getValue();
            if (!matcher.matches()) {
                continue;
            }

            String text = matcher.group(1);
            InputClip.Type type = entry.getKey();

            return InputClip.build((b) -> b.type(type).code(code).text(text));
        }

        return null;
    }

    private void pastClip(ClipboardManager manager, InputClip data, Runnable cb) {
        ClipData oldClip = manager.getPrimaryClip();

        switch (data.type) {
            default: {
                ClipData clip = ClipData.newPlainText("", data.text);
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

    // =============================== End: 内部方法 ===================================
}
