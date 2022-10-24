package com.moyu.rpc.Myexchange;

public interface ListenableConnection extends Connection {

    void addListener(MessageListener listener);

}
