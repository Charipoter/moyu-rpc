package com.moyu.rpc.exchange.support;

import com.moyu.rpc.exchange.ExchangeClient;
import com.moyu.rpc.exchange.FutureReception;
import com.moyu.rpc.exchange.Request;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

@Getter
@AllArgsConstructor
public abstract class AbstractExchangeClient implements ExchangeClient {

    private InetSocketAddress remoteAddress;

    private int connectTimeout;

    @Override
    public CompletableFuture<Object> send(Object message) {
        Request request = new Request(message);
        FutureReception future = FutureReception.addFuture(request);
        doSend(request);
        return future;
    }

    protected abstract void doSend(Request request);
}
