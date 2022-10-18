package com.moyu.rpc.exchange;

import java.util.concurrent.CompletableFuture;

/**
 * 数据交换中的客户端角色（主动方）
 */
public interface ExchangeClient {
    /**
     * 发送数据，调用方决定是否接收响应
     */
    CompletableFuture<Object> send(Object message);

    void open();

    void connect();

    void close();

}
