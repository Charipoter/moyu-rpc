package com.moyu.rpc.exchange;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * 网络中的一个客户端
 */
public interface Client extends EndPoint {

    boolean isConnected();

    void connect();

    void reconnect();

    void disconnect();

    InetSocketAddress getRemoteAddress();
    /**
     * 向连接的服务端发送数据
     */
    CompletableFuture<Object> send(Object message);

}
