package com.moyu.rpc.exchange;

import java.net.InetSocketAddress;

/**
 * 连接各种事件的监听器，由于拓展处理
 */
public interface ConnectionListener {

    /**
     * 收到了消息
     */
    default void onReceived(Message received) {
    }
    /**
     * 与远程建立了连接
     */
     default void onConnected(InetSocketAddress remoteAddress) {
     }
    /**
     * 与远程断开了连接
     */
    default void onDisConnected(InetSocketAddress remoteAddress) {
    }
    /**
     * 发送了消息
     */
    default void onSent(Message sent) {
    }
    /**
     * 连接可正常运作
     */
    default void onOpen() {
    }

    default void onException(Exception e) {
    }

}
