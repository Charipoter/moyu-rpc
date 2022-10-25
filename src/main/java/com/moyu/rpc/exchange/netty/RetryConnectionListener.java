package com.moyu.rpc.exchange.netty;

import com.moyu.rpc.exchange.AbstractListener;
import com.moyu.rpc.exchange.Client;

import java.net.InetSocketAddress;

public class RetryConnectionListener extends AbstractListener {

    private Client client;

    public RetryConnectionListener(Client client) {
        this.client = client;
    }

    @Override
    public void onDisConnected(InetSocketAddress remoteAddress) {
        // 尝试重连
        client.reConnect();
    }
}
