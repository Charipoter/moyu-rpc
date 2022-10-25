package com.moyu.rpc.exchange;

import java.net.InetSocketAddress;

/**
 * 连接各种事件的监听器，由于拓展处理
 */
public interface ConnectionListener {

    /**
     * 收到了消息
     */
    void onReceived(Message received);
    /**
     * 与远程建立了连接
     */
    void onConnected(InetSocketAddress remoteAddress);
    /**
     * 与远程断开了连接
     */
    void onDisConnected(InetSocketAddress remoteAddress);
    /**
     * 发送了消息
     */
    void onSent(Message sent);
    /**
     * 连接可正常运作
     */
    void onOpen();

    void onException(Exception e);

}
