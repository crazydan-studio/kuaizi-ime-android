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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class GsonUtils {

    /** 从 raw 资源中读取对象数据 */
    public static <T> T fromRawResourceJson(Context context, Class<T> cls, int rawResId) {
        try (
                InputStream input = context.getResources().openRawResource(rawResId);
                BufferedReader bf = new BufferedReader(new InputStreamReader(input))
        ) {
            StringBuilder sb = new StringBuilder();
            String line = bf.readLine();
            while (line != null) {
                sb.append(line);
                line = bf.readLine();
            }

            return fromJson(sb.toString(), cls);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param obj
     *         对象或集合
     * @return 若<code>obj</code>为null或字符串，则直接返回该值
     */
    public static String toJson(Object obj) {
        return toJson(obj, null, null);
    }

    /**
     * @param obj
     *         对象或集合
     * @param includes
     *         仅序列化该列表中的属性。为null时，不做过滤
     * @param excludes
     *         排除该列表中的属性。为null时，不做过滤
     * @return 若<code>obj</code>为null或字符串，则直接返回该值
     */
    public static String toJson(Object obj, String[] includes, String[] excludes) {
        if (obj == null || obj instanceof String) {
            return (String) obj;
        }

        Gson gson = createGson(includes, excludes);
        return gson.toJson(obj);
    }

    /**
     * @return 若<code>json</code>为null或空白字符串，则返回null
     */
    public static <T> T fromJson(String json, Class<T> cls) {
        return createGson().fromJson(json, cls);
    }

    /**
     * @return 若<code>json</code>为null或空白字符串，则返回null
     */
    public static <T> T fromJson(String json, Type type) {
        return createGson().fromJson(json, type);
    }

    /**
     * @return 若<code>json</code>为null或空白字符串，则返回null
     */
    public static <T> T fromJson(String json, TypeToken<T> ref) {
        return createGson().fromJson(json, ref.getType());
    }

    public static Gson createGson() {
        return createGson(null, null);
    }

    public static Gson createGson(String[] includes, String[] excludes) {
        return createGsonBuilder(includes, excludes).create();
    }

    public static GsonBuilder createGsonBuilder() {
        return createGsonBuilder(null, null);
    }

    public static GsonBuilder createGsonBuilder(String[] includes, String[] excludes) {
        // https://sites.google.com/site/gson/gson-user-guide#TOC-Serializing-and-Deserializing-Generic-Types
        return new GsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE)
                                .setExclusionStrategies(new FieldExclusionStrategy(includes, excludes));
    }
}
