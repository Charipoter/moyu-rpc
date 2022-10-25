package com.moyu.rpc.exchange;

import java.net.InetSocketAddress;

/**
 * 代表着网络中一个抽象端点
 */
public interface EndPoint {

    boolean isOpen();

    boolean isClosed();

    /**
     * 每个端点由 ip:port 标识
     */
    InetSocketAddress getLocalAddress();

    /**
     * 端点开启
     */
    void open();

    /**
     * 端点关闭
     */
    void close();
}
