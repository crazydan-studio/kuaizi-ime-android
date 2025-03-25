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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ClipData;
import android.content.ClipboardManager;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputClip;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputFavorite;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputTextType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputClipMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputFavoriteMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputTextCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputClipMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputDeleteMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputFavoriteMsgData;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Editor_Edit_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputClip_Apply_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputClip_CanBe_Favorite;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputClip_Create_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputClip_Text_Commit_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputFavorite_Be_Ready;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputFavorite_Delete_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputFavorite_Paste_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputFavorite_Save_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputFavorite_Text_Commit_Doing;

/**
 * 收藏面板
 * <p/>
 * 负责处理 {@link InputClip} 与 {@link InputFavorite} 的管理
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-02-21
 */
public class Favoriteboard {
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
    /** 匹配：18位新版（带校验码） */
    private static final Pattern REGEX_ID_CARD = Pattern.compile(
            "^.*?([1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]).*$",
            Pattern.DOTALL | Pattern.MULTILINE);
    /**
     * 匹配：
     * - Visa（13或16位）
     * - MasterCard（16位）
     * - American Express（15位）
     * - 银联卡（16-19位）
     * - Discover（16-19位）
     */
    private static final Pattern REGEX_CREDIT_CARD = Pattern.compile(
            "^.*?((4\\d{12,15})|(5[1-5]\\d{14})|(3[47]\\d{13})|(62\\d{14,17})|(6(?:011|5[0-9]{2})\\d{12,15})).*$",
            Pattern.DOTALL | Pattern.MULTILINE);
    /** 匹配： */
    private static final Pattern REGEX_ADDRESS = Pattern.compile(
            "^.*?([1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]).*$",
            Pattern.DOTALL | Pattern.MULTILINE);

    protected final Logger log = Logger.getLogger(getClass());

    private final ClipboardManager clipboard;
    private final List<InputFavorite> favorites = new ArrayList<>();

    private ClipboardManager.OnPrimaryClipChangedListener clipChangedListener;
    private String latestUsedClipCode;
    private List<InputClip> latestClips;

    public Favoriteboard(ClipboardManager clipboard) {
        this.clipboard = clipboard;
    }

    // =============================== Start: 生命周期 ===================================

    public void start(FavoriteboardContext context) {
        if (context.clipsDisabled) {
            this.latestClips = null;
            this.latestUsedClipCode = null;
        } else {
            this.latestUsedClipCode = context.usedClipCode;

            updateClips(context);
            bindClipboard(context);
        }
    }

    public void close(FavoriteboardContext context) {
        unbindClipboard();
    }

    private void bindClipboard(FavoriteboardContext context) {
        unbindClipboard();

        this.clipChangedListener = () -> {
            // Note: 剪贴板监听仅输入法处于前台时才会触发，因此，复制/剪切均将由输入法触发，
            // 故而，不提示可粘贴
            InputClip clip = readClip(this.clipboard);
            trySave(context, clip, InputClipMsgData.ClipSourceType.copy_cut);
        };
        this.clipboard.addPrimaryClipChangedListener(this.clipChangedListener);
    }

    private void unbindClipboard() {
        if (this.clipChangedListener != null) {
            this.clipboard.removePrimaryClipChangedListener(this.clipChangedListener);
            this.clipChangedListener = null;
        }
    }

    // =============================== End: 生命周期 ===================================

    // =============================== Start: 消息处理 ===================================

    /** 响应来自上层派发的 {@link UserInputMsg} 消息 */
    public void onMsg(FavoriteboardContext context, UserInputMsg msg) {
        switch (msg.type) {
            case SingleTap_InputClip: {
                on_SingleTap_InputClip_Msg(context, msg);
                break;
            }
            case SingleTap_Btn_Paste_InputFavorite: {
                on_SingleTap_Btn_Paste_InputFavorite_Msg(context, msg);
                break;
            }
            case SingleTap_Btn_Open_Favoriteboard: {
                on_SingleTap_Btn_Open_Favoriteboard_Msg(context);
                break;
            }
            case SingleTap_Btn_Save_As_Favorite: {
                on_SingleTap_Btn_Save_As_Favorite_Msg(context, msg);
                break;
            }
            case SingleTap_Btn_Delete_Selected_InputFavorite: {
                on_SingleTap_Btn_Delete_Selected_InputFavorite_Msg(context, msg);
                break;
            }
            case SingleTap_Btn_Clear_All_InputFavorite: {
                on_SingleTap_Btn_Clear_All_InputFavorite_Msg(context);
                break;
            }
            default: {
                this.log.warn("Ignore message %s", () -> new Object[] { msg.type });
            }
        }
    }

    private void on_SingleTap_InputClip_Msg(FavoriteboardContext context, UserInputMsg msg) {
        // 提前废弃数据，以避免后面的数据粘贴消息更新气泡提示
        discardClips();

        UserInputClipMsgData data = msg.data();
        switch (data.clip.type) {
            case captcha: {
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
        context.fireInputMsg(InputClip_Apply_Done, msgData);

        trySave(context, data.clip, InputClipMsgData.ClipSourceType.paste);
    }

    private void on_SingleTap_Btn_Paste_InputFavorite_Msg(FavoriteboardContext context, UserInputMsg msg) {
        UserInputFavoriteMsgData data = msg.data();

        InputMsgData msgData = new InputTextCommitMsgData(data.favorite.text, false);
        context.fireInputMsg(InputFavorite_Text_Commit_Doing, msgData);

        // TODO 更新使用次数及时间

        for (int i = 0; i < this.favorites.size(); i++) {
            InputFavorite f = this.favorites.get(i);

            if (Objects.equals(f.id, data.favorite.id)) {
                this.favorites.set(i, f.copy((b) -> b.usedCount(f.usedCount + 1).usedAt(new Date())));
                break;
            }
        }

        msgData = new InputFavoriteMsgData(new ArrayList<>(this.favorites));
        context.fireInputMsg(InputFavorite_Paste_Done, msgData);
    }

    private void on_SingleTap_Btn_Open_Favoriteboard_Msg(FavoriteboardContext context) {
        // TODO 从数据库中查询已收藏，按创建时间、最近使用、使用次数降序排序

        InputFavoriteMsgData msgData = new InputFavoriteMsgData(new ArrayList<>(this.favorites));
        context.fireInputMsg(InputFavorite_Be_Ready, msgData);
    }

    private void on_SingleTap_Btn_Save_As_Favorite_Msg(FavoriteboardContext context, UserInputMsg msg) {
        UserInputClipMsgData data = msg.data();

        InputClip clip = data.clip;
        InputFavorite favorite = InputFavorite.from(clip);

        // TODO 保存已收藏到数据库

        this.favorites.add(0, favorite.copy((b) -> b.id((int) new Date().getTime())));

        InputFavoriteMsgData msgData = new InputFavoriteMsgData(new ArrayList<>(this.favorites));
        context.fireInputMsg(InputFavorite_Save_Done, msgData);
    }

    private void on_SingleTap_Btn_Delete_Selected_InputFavorite_Msg(FavoriteboardContext context, UserInputMsg msg) {
        UserInputDeleteMsgData data = msg.data();
        // TODO 从数据库中删除已收藏

        for (Integer id : data.selected) {
            InputFavorite favorite = this.favorites.stream()
                                                   .filter((f) -> Objects.equals(f.id, id))
                                                   .findFirst()
                                                   .orElse(null);
            this.favorites.remove(favorite);
        }

        InputFavoriteMsgData msgData = new InputFavoriteMsgData(new ArrayList<>(this.favorites));
        context.fireInputMsg(InputFavorite_Delete_Done, msgData);
    }

    private void on_SingleTap_Btn_Clear_All_InputFavorite_Msg(FavoriteboardContext context) {
        // TODO 从数据库中清空全部已收藏

        this.favorites.clear();

        InputFavoriteMsgData msgData = new InputFavoriteMsgData(new ArrayList<>(this.favorites));
        context.fireInputMsg(InputFavorite_Delete_Done, msgData);
    }

    private void commitClipText(FavoriteboardContext context, InputClip clip, boolean oneByOne) {
        InputTextCommitMsgData msgData = new InputTextCommitMsgData(clip.text, oneByOne);

        context.fireInputMsg(InputClip_Text_Commit_Doing, msgData);
    }

    private void trySave(FavoriteboardContext context, InputClip clip, InputClipMsgData.ClipSourceType source) {
        if (!canBeSave(clip)) {
            return;
        }

        // Note: 对于未确定类型或来自复制/剪切的数据，需要做类型检测
        if (clip.type == null || source == InputClipMsgData.ClipSourceType.copy_cut) {
            clip = deeplyCollectClips(clip).get(0);
        }

        InputClipMsgData msgData = new InputClipMsgData(source, clip);
        context.fireInputMsg(InputClip_CanBe_Favorite, msgData);
    }

    // =============================== End: 消息处理 ===================================

    /** 尝试收藏用户输入的文本 <code>text</code>，并发送 {@link InputMsgType#InputClip_CanBe_Favorite} 消息 */
    public void trySave(FavoriteboardContext context, String text) {
        InputClip clip = InputClip.build((b) -> b.text(text));

        trySave(context, clip, InputClipMsgData.ClipSourceType.user_input);
    }

    /**
     * 检查指定的 {@link InputClip} 是否可被收藏
     * <p/>
     * 已收藏的 {@link InputClip} 将返回 <code>false</code>
     * <p/>
     * 只有可收藏的 {@link InputClip} 才弹出收藏提示
     */
    public boolean canBeSave(InputClip clip) {
        if (clip == null || CharUtils.isBlank(clip.text) || clip.text.length() < 3) {
            return false;
        }

        // TODO 查询数据，检查是否有相同内容的收藏

        return true;
    }

    /**
     * 废弃剪贴数据，不再使用
     * <p/>
     * 一般在有新的输入或超过一定的时间后，需废弃剪贴数据
     */
    public String discardClips() {
        if (!verifyClips()) {
            return null;
        }
        this.log.debug("Discard Clips");

        String clipCode = this.latestClips.get(0).code;
        this.latestClips = null;

        return clipCode;
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
    private void updateClips(FavoriteboardContext context) {
        InputClip primary = readClip(this.clipboard);
        if (primary == null) {
            discardClips();
            return;
        }

        // 若剪贴板数据已使用，则不再处理
        if (primary.code.equals(this.latestUsedClipCode)) {
            discardClips();
            return;
        }

        List<InputClip> clips = deeplyCollectClips(primary);

        this.latestClips = Collections.unmodifiableList(clips);

        InputClipMsgData msgData = new InputClipMsgData(primary);
        context.fireInputMsg(InputClip_Create_Done, msgData);
    }

    // =============================== Start: 内部方法 ===================================

    /** 从指定的剪贴数据中掘取不同{@link InputTextType 类型}的数据 */
    private List<InputClip> deeplyCollectClips(InputClip primary) {
        if (primary.type == null) {
            primary = primary.copy((b) -> b.type(InputTextType.text));
        }

        List<InputClip> clips = new ArrayList<>();
        // Note: 确保原始内容在第一的位置
        clips.add(primary);

        if (primary.type == InputTextType.html) {
            return clips;
        }

        String primaryText = cleanClipText(primary);
        List<InputClip> others = extractOtherClips(primaryText, primary.code);

        if (!others.isEmpty()) {
            InputClip first = others.get(0);

            // 仅更改类型，以保留首尾的空白字符
            if (primaryText.equals(first.text)) {
                clips.set(0, primary.copy((b) -> b.type(first.type)));
                others.remove(0);
            }

            clips.addAll(others);
        }

        return clips;
    }

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
                }
                if (item.getHtmlText() != null) {
                    // TODO 暂时没有好的方法粘贴 html，故而，不做识别
                    //html = item.getHtmlText();
                }
            }

            if (!CharUtils.isBlank(text)) {
                if (!CharUtils.isBlank(html)) {
                    b.type(InputTextType.html);
                } else {
                    b.type(InputTextType.text);
                }

                b.text(text).html(html);
            }
        });

        return clip.type != null ? clip.copy((b) -> b.code(clip.hashCode() + "")) : null;
    }

    private List<InputClip> extractOtherClips(String primaryText, String code) {
        Map<InputTextType, Matcher> matchers = new LinkedHashMap<InputTextType, Matcher>() {{
            // <<<<<<< 可重复匹配的类型，但每种类型仅支持匹配到一条数据
            put(InputTextType.url, REGEX_URL.matcher(primaryText));
            put(InputTextType.email, REGEX_EMAIL.matcher(primaryText));
            put(InputTextType.phone, REGEX_PHONE.matcher(primaryText));
            put(InputTextType.id_card, REGEX_ID_CARD.matcher(primaryText));
            put(InputTextType.credit_card, REGEX_CREDIT_CARD.matcher(primaryText));
            put(InputTextType.address, REGEX_ADDRESS.matcher(primaryText));
            // >>>>>>>
            // <<<<<<< 不可重复匹配的类型：在有其他类型的匹配数据时，不再匹配这些类型的数据
            put(InputTextType.captcha, REGEX_CAPTCHA.matcher(primaryText));
            // >>>>>>>
        }};

        List<InputClip> clips = new ArrayList<>();
        for (Map.Entry<InputTextType, Matcher> entry : matchers.entrySet()) {
            Matcher matcher = entry.getValue();
            if (!matcher.matches()) {
                continue;
            }

            InputTextType type = entry.getKey();
            if (!clips.isEmpty() && type == InputTextType.captcha) {
                continue;
            }

            String text = matcher.group(1);
            InputClip clip = InputClip.build((b) -> b.type(type).code(code).text(text));

            clips.add(clip);
        }

        return clips;
    }

    // =============================== End: 内部方法 ===================================
}
