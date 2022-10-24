package com.moyu.rpc.exchange;

import java.net.InetSocketAddress;

/**
 * 对于通讯中各类事件的处理
 */
public interface ExchangeHandler {

    void receive(Object message);

    void connected(InetSocketAddress address);

    void caught(Throwable cause);

}
