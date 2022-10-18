package com.moyu.rpc.exchange;

/**
 * 数据交换中的服务端角色（被动方）
 */
public interface ExchangeServer {
    void open();
    void close();

}
