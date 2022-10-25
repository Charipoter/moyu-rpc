package com.moyu.rpc.exchange;

import java.net.InetSocketAddress;

/**
 * 默认无实现
 */
public abstract class AbstractListener implements ConnectionListener {
    @Override
    public void onReceived(Message received) {
    }

    @Override
    public void onConnected(InetSocketAddress remoteAddress) {
    }

    @Override
    public void onDisConnected(InetSocketAddress remoteAddress) {
    }

    @Override
    public void onSent(Message sent) {
    }

    @Override
    public void onOpen() {

    }

    @Override
    public void onException(Exception e) {

    }
}
