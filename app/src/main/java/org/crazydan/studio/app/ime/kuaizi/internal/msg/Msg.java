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

package org.crazydan.studio.app.ime.kuaizi.internal.msg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.util.Log;

/**
 * 负责全局性地管理消息的发送和接收，
 * 使得消息收发对象之间完全解耦，
 * 从而避免对象删除后依然存在各种引用而造成消息收发混乱的问题
 * <p/>
 * 对于 Activity 等监听器，需要在其退出前{@link Registry#unregister 注销}，
 * 在恢复后再重新{@link Registry#register 注册}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-12-03
 */
public interface Msg {

    /** 发送消息 */
    default <S, D extends MsgData> void send(S sender, D msgData) {
        Registry.send(sender, this, msgData);
    }

    class Registry {
        private static final Map<Class<?>, Set<MsgListener<Object, Msg, MsgData>>> listenersByType = new HashMap<>();

        private Registry() {}

        /**
         * 发送消息
         * <p/>
         * 在发送者存在多实例的情况下，需要通过 <code>sender</code> 参数区分消息是否来自于所关注的对象，
         * 对于非关注对象的消息应该直接忽略
         */
        private static <S, M extends Msg, D extends MsgData> void send(S sender, M msg, D msgData) {
            Set<MsgListener<Object, Msg, MsgData>> listeners = listenersByType.get(msg.getClass());
            if (listeners == null) {
                return;
            }

            Set<MsgListener<Object, Msg, MsgData>> oldListeners = new HashSet<>(listeners);
            for (MsgListener<Object, Msg, MsgData> listener : oldListeners) {
                // 若有被提前移除的监听，则无需执行
                if (listeners.contains(listener)) {
                    listener.onMsg(sender, msg, msgData);
                }
            }
        }

        /** 注册指定类型消息的监听器 */
        public static <S, M extends Msg, D extends MsgData> void register(
                Class<M> msgType, MsgListener<S, M, D> listener
        ) {
            listenersByType.computeIfAbsent(msgType, (k) -> new HashSet<>())
                           .add((MsgListener<Object, Msg, MsgData>) listener);
        }

        /** 移除指定类型消息的监听器 */
        public static <S, M extends Msg, D extends MsgData> void unregister(
                Class<M> msgType, MsgListener<S, M, D> listener
        ) {
            Log.i(Msg.Registry.class.getSimpleName(), "unregister listener - " + msgType + ":" + listener);

            Set<MsgListener<Object, Msg, MsgData>> listeners = listenersByType.get(msgType);
            if (listeners != null) {
                listeners.remove(listener);
            }
        }

        /** 移除指定类型的监听器 */
        public static void unregister(Class<?> listenerType) {
            Log.i(Msg.Registry.class.getSimpleName(), "unregister listeners - " + listenerType);

            listenersByType.forEach((msgType, listeners) -> listeners.removeIf(listener -> listener.getClass()
                                                                                           == listenerType));
        }

        /** 移除全部消息的监听器 */
        public static void unregister(MsgListener<?, ?, ?> listener) {
            Log.i(Msg.Registry.class.getSimpleName(), "unregister listener - " + listener);

            listenersByType.forEach((type, listeners) -> listeners.remove(listener));
        }
    }
}
