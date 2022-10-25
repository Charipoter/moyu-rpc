package com.moyu.rpc.exchange;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * 网络中的客户端
 */
public interface Server extends EndPoint {

    /**
     * 如果已经建立了连接就发送，否则啥也不干
     */
    CompletableFuture<Object> sendTo(Object message, InetSocketAddress address);

    CompletableFuture<Object> broadcast(Object message);

}
