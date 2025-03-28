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

package org.crazydan.studio.app.ime.kuaizi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.common.Async;
import org.crazydan.studio.app.ime.kuaizi.common.utils.FileUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ResourceUtils;
import org.crazydan.studio.app.ime.kuaizi.dict.DictDBType;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinCharsTree;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.dict.UserInputDataDict;
import org.crazydan.studio.app.ime.kuaizi.dict.UserInputFavoriteDict;
import org.crazydan.studio.app.ime.kuaizi.dict.upgrade.From_v0;
import org.crazydan.studio.app.ime.kuaizi.dict.upgrade.From_v2_to_v3;
import org.crazydan.studio.app.ime.kuaizi.dict.upgrade.From_v3_to_v4;
import org.crazydan.studio.app.ime.kuaizi.dict.upgrade.Upgrader;

import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.closeSQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.openSQLite;
import static org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict.createPinyinCharsTree;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.UserInputDataDBHelper.enableAllPrintableEmojis;

/**
 * {@link IMEditor} 的字典
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-24
 */
public class IMEditorDict {
    public static final String VERSION_V0 = "v0";
    public static final String VERSION_V2 = "v2";
    public static final String VERSION_V3 = "v3";
    public static final String VERSION_V4 = "v4";

    /** 字典首次安装版本号 */
    public static final String FIRST_INSTALL_VERSION = VERSION_V0;
    /** 连续的字典版本号，用于递进升级 */
    public static final String[] CONTINUOUS_VERSIONS = new String[] { VERSION_V2, VERSION_V3, VERSION_V4 };
    /** 字典最新版本号 */
    public static final String LATEST_VERSION = CONTINUOUS_VERSIONS[CONTINUOUS_VERSIONS.length - 1];

    /** 字典版本文件名 */
    private static final String version_filename = "ime_user_dict.version";

    private static final IMEditorDict instance = new IMEditorDict();

    /** 字典 {@link #open} 的引用计数 */
    private int openedRefs;
    private boolean opened;
    /** 异步 */
    private Async async;

    private String version;
    private SQLiteDatabase db;

    // <<<<<<<<<<<<< 缓存常量数据
    private PinyinCharsTree pinyinCharsTree;
    private Map<Class<?>, Object> deriveDicts;
    // >>>>>>>>>>>>>

    IMEditorDict() {
    }

    public static IMEditorDict instance() {
        return instance;
    }

    public boolean isOpened() {
        return this.opened;
    }

    // =================== Start: 生命周期 ==================

    /**
     * 在使用前开启字典：由开启方负责 {@link #close 关闭}
     * <p/>
     * 开启为异步操作，需通过 {@link CompletableFuture#thenRun} 确定真实的开启完毕时机
     */
    public synchronized CompletableFuture<Void> open(Context context) {
        this.openedRefs += 1;
        if (isOpened()) {
            return CompletableFuture.completedFuture(null);
        }

        this.async = new Async(1, 4);

        return this.async.future(() -> {
            doUpgrade(context);
            doOpen(context);

            this.opened = true;
        });
    }

    /** 在资源回收前关闭字典：由 {@link #open 开启} 方负责关闭 */
    public synchronized void close() {
        this.openedRefs -= 1;
        if (this.openedRefs > 0) {
            return;
        }

        if (isOpened()) {
            doClose();
        }
        this.opened = false;
    }

    // =================== End: 生命周期 ==================

    // =================== Start: 派生字典 ==================

    public PinyinDict usePinyinDict() {
        return deriveDict(PinyinDict.class, () -> new PinyinDict(getDB(), this.async, this.pinyinCharsTree));
    }

    public UserInputDataDict useUserInputDataDict() {
        return deriveDict(UserInputDataDict.class, () -> new UserInputDataDict(getDB(), this.async));
    }

    public UserInputFavoriteDict useUserInputFavoriteDict() {
        return deriveDict(UserInputFavoriteDict.class, () -> new UserInputFavoriteDict(getDB(), this.async));
    }

    private <T> T deriveDict(Class<T> cls, Supplier<T> supplier) {
        return (T) this.deriveDicts.computeIfAbsent(cls, (k) -> supplier.get());
    }

    // =================== End: 派生字典 ==================

    // =================== Start: 数据库管理 ==================

    public SQLiteDatabase getDB() {
        return isOpened() ? this.db : null;
    }

    private void doOpen(Context context) {
        File userDBFile = getUserDBFile(context);

        this.db = openSQLite(userDBFile, false);
        execSQLite(this.db, /*"pragma cache_size = 200;",*/ "pragma temp_store = memory;");

        // 启用系统支持的可显示的表情
        enableAllPrintableEmojis(this.db);

        this.deriveDicts = new HashMap<>();
        if (this.pinyinCharsTree == null) {
            this.pinyinCharsTree = createPinyinCharsTree(this.db);
        }
    }

    private void doClose() {
        this.async.shutdown(1500);

        closeSQLite(this.db);

        this.db = null;
        this.async = null;
        this.pinyinCharsTree = null;
        this.deriveDicts = null;
    }

    public File getUserDBFile(Context context) {
        return getDBFile(context, DictDBType.user);
    }

    public File getDBFile(Context context, DictDBType dbType) {
        return getFile(context, dbType.filename);
    }

    public void saveUserDB(Context context, OutputStream output) throws IOException {
        File userDBFile = getUserDBFile(context);

        try (InputStream input = FileUtils.newInput(userDBFile)) {
            ResourceUtils.copy(input, output);
        }
    }

    // =================== End: 数据库管理 ==================

    // =================== Start: 数据版本升级 ==================

    /** 升级数据版本：已成功升级的，将不会重复处理 */
    private void doUpgrade(Context context) {
        // <<<<<<<<<< 修改文件名 @v4
        // Note: FileUtils#moveFile 在源文件不存在时，将不会做任何操作
        File oldUserDBFile = getFile(context, "pinyin_user_dict.db");
        FileUtils.moveFile(oldUserDBFile, getUserDBFile(context));

        File oldVersionFile = getFile(context, "pinyin_user_dict.version");
        FileUtils.moveFile(oldVersionFile, getVersionFile(context));
        // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

        String currentVersion = getVersion(context);

        // Note: 字典没有 v1 版本
        if (FIRST_INSTALL_VERSION.equals(currentVersion)) {
            new From_v0().upgrade(context, this);
        } else {
            String fromVersion = null;
            Map<String, Upgrader> upgraders = getUpgraders();
            // 按照版本号递进升级
            for (String targetVersion : CONTINUOUS_VERSIONS) {
                if (targetVersion.equals(currentVersion)) {
                    fromVersion = targetVersion;
                    continue;
                }

                if (fromVersion != null) {
                    String code = fromVersion + "-" + targetVersion;
                    Upgrader upgrader = upgraders.get(code);
                    assert upgrader != null;
                    upgrader.upgrade(context, this);

                    fromVersion = targetVersion;
                }
            }
        }

        updateVersionToLatest(context);
    }

    private Map<String, Upgrader> getUpgraders() {
        return new HashMap<String, Upgrader>() {{
            put(VERSION_V2 + "-" + VERSION_V3, new From_v2_to_v3());
            put(VERSION_V3 + "-" + VERSION_V4, new From_v3_to_v4());
        }};
    }

    /** 获取应用本地的用户数据的版本 */
    private String getVersion(Context context) {
        if (this.version == null) {
            File userDBFile = getUserDBFile(context);
            File versionFile = getVersionFile(context);

            if (!versionFile.exists()) {
                if (userDBFile.exists()) { // 应用 HMM 算法之前的版本
                    this.version = VERSION_V2;
                } else { // 首次安装
                    this.version = FIRST_INSTALL_VERSION;
                }
            } else { // 实际记录的版本号
                this.version = FileUtils.read(versionFile, true);
            }
        }

        return this.version;
    }

    private void updateVersionToLatest(Context context) {
        if (LATEST_VERSION.equals(this.version)) {
            return;
        }

        File file = getVersionFile(context);
        try {
            FileUtils.write(file, LATEST_VERSION);
            this.version = LATEST_VERSION;
        } catch (IOException ignore) {
        }
    }

    private File getVersionFile(Context context) {
        return getFile(context, version_filename);
    }

    private File getFile(Context context, String filename) {
        return new File(context.getFilesDir(), filename);
    }

    // =================== End: 数据版本升级 ==================
}
