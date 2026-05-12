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

import java.util.Iterator;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-08
 */
public class ChangelogUtils {

    public static Spanned forHtml(Context context) {
        String appVersion = cleanVersion(SystemUtils.getAppVersion(context));

        return doParseHtml(context, (changelogs) -> toHtml(changelogs, appVersion).toString());
    }

    public static Spanned forHtml(Context context, String version) {
        String cleanedVersion = cleanVersion(version);

        return doParseHtml(context, (changelogs) -> {
            JSONObject changelog = changelogs.getJSONObject(cleanedVersion);

            StringBuilder sb = new StringBuilder();
            toHtml(sb, version, changelog, false);

            return sb.toString();
        });
    }

    private static String cleanVersion(String version) {
        return version.replaceAll("-.*$", "");
    }

    private static Spanned doParseHtml(Context context, HtmlParser parser) {
        String appChangelog = ResourceUtils.readString(context, R.raw.text_about_changelog);

        try {
            JSONObject changelogs = new JSONObject(appChangelog);
            String html = parser.parse(changelogs);

            return Html.fromHtml(html, FROM_HTML_MODE_COMPACT);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static StringBuilder toHtml(JSONObject changelogs, String latestVersion) throws JSONException {
        StringBuilder sb = new StringBuilder();

        Iterator<String> it = changelogs.keys();
        while (it.hasNext()) {
            String version = it.next();
            boolean latest = version.equals(latestVersion);
            JSONObject changelog = changelogs.getJSONObject(version);

            if (sb.length() > 0) {
                sb.append("<br/>");
            }
            toHtml(sb, version, changelog, latest);
        }

        return sb;
    }

    private static void toHtml(StringBuilder sb, String version, JSONObject changelog, boolean latest)
            throws JSONException {
        String date = changelog.getString("date");
        sb.append("<p><b><big>v")
          .append(version)
          .append(" (")
          .append(date)
          .append(")")
          .append(latest ? " &lt;- 当前版本" : "")
          .append("</big></b></p><br /><ul>");

        JSONArray logs = changelog.getJSONArray("logs");
        for (int i = 0; i < logs.length(); i++) {
            JSONObject log = logs.getJSONObject(i);

            String text = log.getString("t");
            JSONArray details = log.optJSONArray("d");

            sb.append(i > 0 ? "<br/>" : "").append("<li>&nbsp;").append(text);

            if (details == null || details.length() == 0) {
                sb.append("；");
            } else {
                sb.append("：<ul>");

                for (int j = 0; j < details.length(); j++) {
                    String detail = details.getString(j);
                    sb.append("<li>&nbsp;&nbsp;&nbsp;&nbsp;").append(detail).append("；</li>");
                }

                sb.append("</ul>");
            }

            sb.append("</li>");
        }

        sb.append("</ul>");
    }

    private interface HtmlParser {
        String parse(JSONObject changelogs) throws JSONException;
    }
}
