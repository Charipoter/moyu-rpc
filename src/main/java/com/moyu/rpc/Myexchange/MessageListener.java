package com.moyu.rpc.Myexchange;

/**
 * 消息的监听器，由于拓展处理
 */
public interface MessageListener {

    void onReceive(Message message);

}
