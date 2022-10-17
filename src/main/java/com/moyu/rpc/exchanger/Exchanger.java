package com.moyu.rpc.exchanger;

import java.util.concurrent.CompletableFuture;

/**
 * 交换器，负责数据的传输和接收（后台）
 */
public interface Exchanger {

    /**
     * 发送数据，调用方决定要不要异步
     */
    CompletableFuture<Object> send(Object message);

    /**
     * 开启一个通讯器，默认与指定 url 建立长连接
     */
    void open(String host, int port);

    /**
     * 关闭
     */
    void close();


}
