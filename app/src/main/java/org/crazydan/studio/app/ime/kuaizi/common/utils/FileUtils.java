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

package org.crazydan.studio.app.ime.kuaizi.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.zip.ZipOutputStream;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-18
 */
public class FileUtils {

    /** 将压缩文件存放到 Download 目录 */
    public static void saveZipToDownload(Context context, String fileName, ZipFileSaver fileSaver) {
        saveToDownload(context, fileName, (output) -> {
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                fileSaver.saveTo(zip);
            }
        });
    }

    /** 将文件存放到 Download 目录 */
    public static void saveToDownload(Context context, String fileName, FileSaver fileSaver) {
        // https://stackoverflow.com/questions/59103133/how-to-directly-download-a-file-to-download-directory-on-android-q-android-10#answer-64357198
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            ContentResolver resolver = context.getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

            try (OutputStream output = resolver.openOutputStream(uri)) {
                fileSaver.saveTo(output);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/" + fileName);

            try (OutputStream output = FileUtils.newOutput(dir)) {
                fileSaver.saveTo(output);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static String read(File file, boolean trim) {
        if (!file.exists()) {
            return null;
        }

        return read(() -> newInput(file), trim);
    }

    public static String read(Context context, int rawResId, boolean trim) {
        if (rawResId <= 0) {
            return null;
        }

        return read(() -> context.getResources().openRawResource(rawResId), trim);
    }

    public static void write(File file, String text) throws IOException {
        try (OutputStream output = newOutput(file);) {
            write(output, text);

            output.flush();
        }
    }

    public static void write(OutputStream output, String text) throws IOException {
        byte[] bytes = text.getBytes();
        output.write(bytes, 0, bytes.length);
    }

    public static void copy(Context context, int rawResId, File target) throws IOException {
        try (OutputStream output = newOutput(target);) {
            ResourceUtils.copy(context, rawResId, output);
        }
    }

    private static String read(Callable<InputStream> inputGetter, boolean trim) {
        try (InputStream input = inputGetter.call()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            return trim ? sb.toString().replaceAll("^\\s+|\\s+$", "") : sb.toString();
        } catch (Exception ignore) {
        }
        return null;
    }

    public static OutputStream newOutput(File file) throws IOException {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return Files.newOutputStream(file.toPath());
        } else {
            return new FileOutputStream(file);
        }
    }

    public static InputStream newInput(File file) throws IOException {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return Files.newInputStream(file.toPath());
        } else {
            return new FileInputStream(file);
        }
    }

    public static void deleteFile(File file) {
        if (file.exists() && file.isFile()) {
            file.delete();
        }
    }

    /** 若 <code>source</code> 不存在，则不做任何操作 */
    public static void moveFile(File source, File target) {
        if (!source.exists()) {
            return;
        }

        if (target.exists()) {
            deleteFile(target);
        }
        source.renameTo(target);
    }

    public interface FileSaver {
        void saveTo(OutputStream output) throws Exception;
    }

    public interface ZipFileSaver {
        void saveTo(ZipOutputStream output) throws Exception;
    }
}
