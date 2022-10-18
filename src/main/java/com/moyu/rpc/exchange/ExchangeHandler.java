package com.moyu.rpc.exchange;

/**
 * 对于通讯中各类事件的处理
 */
public interface ExchangeHandler {

    void receive(Object message);

    void caught(Throwable cause);

}
