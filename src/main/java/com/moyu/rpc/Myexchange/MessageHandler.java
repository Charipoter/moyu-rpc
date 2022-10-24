package com.moyu.rpc.Myexchange;

/**
 * 收到消息后的处理
 */
public interface MessageHandler {

    /**
     * 处理消息接收
     */
    void handleReceived(Message received);

}
