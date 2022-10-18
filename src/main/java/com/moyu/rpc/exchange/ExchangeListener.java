package com.moyu.rpc.exchange;

/**
 * 监听消息事件，扩展点
 */
public interface ExchangeListener {

    void onEvent(Object event);

}
