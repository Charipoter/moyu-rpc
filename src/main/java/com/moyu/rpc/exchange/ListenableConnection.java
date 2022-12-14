package com.moyu.rpc.exchange;

import java.net.InetSocketAddress;

/**
 * 可被监听的连接，提供扩展，根据事件调用监听器
 */
public interface ListenableConnection extends Connection {

    void addListener(ConnectionListener listener);

    void onReceived(Message received);

    void onConnected(InetSocketAddress remoteAddress);

    void onDisConnected(InetSocketAddress remoteAddress);

    void onSent(Message sent);

    void onOpen();

    void onException(Exception e);

}
