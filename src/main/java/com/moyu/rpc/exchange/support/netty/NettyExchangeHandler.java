package com.moyu.rpc.exchange.support.netty;

import com.moyu.rpc.exchange.Request;
import com.moyu.rpc.exchange.Response;
import com.moyu.rpc.exchange.support.AbstractExchangeHandler;

import java.net.InetSocketAddress;

public class NettyExchangeHandler extends AbstractExchangeHandler {

    @Override
    protected void doReceive(Request request) {
        System.out.println(request);
    }

    @Override
    protected void doReceive(Object message) {
        System.out.println(message);
    }

    @Override
    protected void doReceive(Response response) {
        System.out.println(response);
    }

    @Override
    public void connected(InetSocketAddress address) {
        System.out.println("连接上了" + address);
    }

    @Override
    public void caught(Throwable cause) {
        System.out.println(cause.toString());
    }
}
