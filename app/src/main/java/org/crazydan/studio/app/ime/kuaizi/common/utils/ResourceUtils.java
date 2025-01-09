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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-28
 */
public class ResourceUtils {

    public static String readString(Context context, int rawResId, Object... args) {
        try (
                InputStream input = context.getResources().openRawResource(rawResId);
                OutputStream output = new ByteArrayOutputStream();
        ) {
            int length;
            byte[] buffer = new byte[1024];
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            return String.format(output.toString(), args);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void copy(Context context, int rawResId, OutputStream output) throws IOException {
        try (
                InputStream input = context.getResources().openRawResource(rawResId);
        ) {
            copy(input, output);
        }
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        int length;
        byte[] buffer = new byte[1024];
        while ((length = input.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }

        output.flush();
    }
}
