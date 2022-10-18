package com.moyu.rpc.exchange.support;

import com.moyu.rpc.exchange.*;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractExchangeHandler implements ExchangeHandler {

    private final List<ExchangeListener> listeners = new ArrayList<>();
    @Override
    public void receive(Object message) {
        if (message instanceof Request) {
            doReceive((Request) message);
        } else if (message instanceof Response) {
            FutureReception.receive((Response) message);
            doReceive((Response) message);
        } else {
            doReceive(message);
        }
        listeners.forEach(exchangeListener -> exchangeListener.onEvent(message));
    }

    public void addListener(ExchangeListener listener) {
        listeners.add(listener);
    }

    protected abstract void doReceive(Request request);

    protected abstract void doReceive(Object message);

    protected abstract void doReceive(Response response);
}
