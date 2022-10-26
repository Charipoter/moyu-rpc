package com.moyu.rpc.exchange;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * 网络中一条连接，用于发送数据
 */
public interface Connection {

    CompletableFuture<Object> send(Object message);
    /**
     * 关闭连接
     */
    void close();
    /**
     * 连接是否有效
     */
    boolean isActive();
    /**
     * 收到数据
     * 需要往具体的连接器注册消息接收监听
     */
    void receive(Message received);
    /**
     * 获取源 url
     */
    InetSocketAddress getLocalAddress();
    /**
     * 获取目标 url
     */
    InetSocketAddress getRemoteAddress();

}
