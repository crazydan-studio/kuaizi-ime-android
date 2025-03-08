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

package org.crazydan.studio.app.ime.kuaizi.ui.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.Spanned;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ResourceUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-07
 */
public abstract class HtmlSupportActivity extends FollowSystemThemeActivity {

    protected void setText(int viewResId, int textResId, Object... args) {
        // https://developer.android.com/guide/topics/resources/string-resource#formatting-strings
        String viewText = getResources().getString(textResId, args);

        TextView view = findViewById(viewResId);
        view.setText(viewText);
    }

    protected TextView setHtmlText(int viewResId, int htmlRawResId, Object... args) {
        String text = ResourceUtils.readString(getApplicationContext(), htmlRawResId, args);
        TextView view = findViewById(viewResId);

        ViewUtils.setHtmlText(view, text);

        return view;
    }

    protected void setIcon(int viewResId, int iconResId) {
        ImageView view = findViewById(viewResId);
        view.setImageResource(iconResId);
    }

    protected ImageView setRawImage(int viewResId, int rawResId) {
        try (InputStream input = getResources().openRawResource(rawResId)) {
            Bitmap bitmap = BitmapFactory.decodeStream(input);

            ImageView view = findViewById(viewResId);
            view.setImageBitmap(bitmap);

            return view;
        } catch (IOException ignored) {
        }
        return null;
    }

    protected void shareRawImage(int rawResId, String filename, int titleResId) {
        File file = new File(getCacheDir(), filename);
        file.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            ResourceUtils.copy(getApplicationContext(), rawResId, fos);
        } catch (IOException ignored) {
        }

        Uri uri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", file);

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String title = getResources().getString(titleResId);
        intent = Intent.createChooser(intent, title);
        startActivity(intent);
    }
}
