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

package org.crazydan.studio.app.ime.kuaizi.ui.about;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.ui.common.HtmlSupportActivity;

/**
 * 赞助
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-01-10
 */
public class AboutDonation extends HtmlSupportActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_donation);

        String appName = getAppName();
        TextView alipayTextView = setHtmlText(R.id.donation_alipay_tips,
                                              R.raw.text_about_donation_alipay_tips,
                                              appName);
        TextView wechatTextView = setHtmlText(R.id.donation_wechat_tips,
                                              R.raw.text_about_donation_wechat_tips,
                                              appName);

        alipayTextView.setOnClickListener((v) -> copyToClipboard(appName));
        wechatTextView.setOnClickListener((v) -> copyToClipboard(appName));

        ImageView alipayImageView = setRawImage(R.id.donation_alipay, R.raw.donation_alipay);
        ImageView wechatImageView = setRawImage(R.id.donation_wechat, R.raw.donation_wechat);

        alipayImageView.setOnLongClickListener((v) -> {
            shareRawImage(R.raw.donation_alipay,
                          "donation/alipay.jpg",
                          R.string.title_about_donation_for_sharing_qrcode_to_alipay);
            return true;
        });
        wechatImageView.setOnLongClickListener((v) -> {
            shareRawImage(R.raw.donation_wechat,
                          "donation/wechat.jpg",
                          R.string.title_about_donation_for_sharing_qrcode_to_wechat);
            return true;
        });

    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", text);

        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "备注信息「" + text + "」已复制", Toast.LENGTH_SHORT).show();
    }
}
