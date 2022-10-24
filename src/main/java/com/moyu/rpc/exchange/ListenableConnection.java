package com.moyu.rpc.exchange;

public interface ListenableConnection extends Connection {

    void addListener(MessageListener listener);

}
