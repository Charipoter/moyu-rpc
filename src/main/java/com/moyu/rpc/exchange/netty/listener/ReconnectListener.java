package com.moyu.rpc.exchange.netty.listener;

import com.moyu.rpc.exchange.Client;
import com.moyu.rpc.exchange.ConnectionListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class ReconnectListener implements ConnectionListener {

    private final Client client;

    public ReconnectListener(Client client) {
        this.client = client;
    }

    @Override
    public void onDisConnected(InetSocketAddress remoteAddress) {

        if (!client.isClosed()) {
            client.reconnect();
        }

    }
}
