package com.moyu.rpc.exchange;

/**
 * 消息的监听器，由于拓展处理
 */
public interface MessageListener {

    void onReceive(Message message);

}
