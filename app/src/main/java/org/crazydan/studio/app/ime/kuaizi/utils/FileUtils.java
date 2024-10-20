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

package org.crazydan.studio.app.ime.kuaizi.utils;

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

import android.content.Context;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-18
 */
public class FileUtils {

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
            byte[] bytes = text.getBytes();
            output.write(bytes, 0, bytes.length);

            output.flush();
        }
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

    public static void moveFile(File source, File target) {
        if (!source.exists()) {
            return;
        }

        if (target.exists()) {
            deleteFile(target);
        }
        source.renameTo(target);
    }
}
