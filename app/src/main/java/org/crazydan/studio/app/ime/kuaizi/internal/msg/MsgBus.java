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

/**
 * 消息总线，负责全局性地管理消息的发送和接收，
 * 使得消息收发对象之间完全解耦，
 * 从而避免对象删除后依然存在各种引用而造成消息收发混乱的问题
 * <p/>
 * 对于 Activity 等监听器，需要在其退出前{@link #unregister 注销}，
 * 在恢复后再重新{@link #register 注册}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-12-03
 */
public class MsgBus {
    private static final Map<Class<?>, Set<MsgListener<Msg, MsgData>>> listenersByType = new HashMap<>();

    private MsgBus() {
    }

    /** 发送消息 */
    public static <M extends Msg, D extends MsgData> void send(M msg, D data) {
        Set<MsgListener<Msg, MsgData>> listeners = listenersByType.get(msg.getClass());
        if (listeners != null) {
            listeners.forEach(listener -> listener.onMsg(msg, data));
        }
    }

    /** 注册指定类型消息的监听器 */
    public static <M extends Msg, D extends MsgData> void register(Class<M> msgType, MsgListener<M, D> listener) {
        listenersByType.computeIfAbsent(msgType, (k) -> new HashSet<>()).add((MsgListener<Msg, MsgData>) listener);
    }

    /** 移除指定类型消息的监听器 */
    public static <M extends Msg, D extends MsgData> void unregister(Class<M> msgType, MsgListener<M, D> listener) {
        Set<MsgListener<Msg, MsgData>> listeners = listenersByType.get(msgType);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /** 移除全部消息的监听器 */
    public static <M extends Msg, D extends MsgData> void unregister(MsgListener<M, D> listener) {
        listenersByType.forEach((type, listeners) -> listeners.remove(listener));
    }
}
